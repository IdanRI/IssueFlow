package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.*;
import com.att.tdp.issueflow.exception.InvalidStatusTransitionException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.exception.TicketDoneException;
import com.att.tdp.issueflow.exception.UnresolvedDependencyException;
import com.att.tdp.issueflow.mapper.TicketMapper;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;
    private final AuditLogService auditLogService;
    private final TicketDependencyRepository ticketDependencyRepository;

    public List<TicketResponse> getTicketsByProject(Long projectId) {
        projectService.findProjectOrThrow(projectId);
        return ticketRepository.findByProjectIdAndDeletedFalse(projectId).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    public TicketResponse getTicketById(Long id) {
        Ticket ticket = findTicketOrThrow(id);
        return TicketMapper.toResponse(ticket);
    }

    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, Long performedBy) {
        projectService.findProjectOrThrow(request.getProjectId());

        if (request.getAssigneeId() != null) {
            userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssigneeId()));
        }

        Ticket ticket = TicketMapper.toEntity(request);
        Ticket saved = ticketRepository.save(ticket);

        auditLogService.logUserAction(AuditAction.CREATE, EntityType.TICKET, saved.getId(), performedBy);

        if (saved.getAssigneeId() == null) {
            autoAssign(saved);
        }

        return TicketMapper.toResponse(saved);
    }

    @Transactional
    public void updateTicket(Long id, UpdateTicketRequest request, Long performedBy) {
        Ticket ticket = findTicketOrThrow(id);

        if (ticket.getStatus() == TicketStatus.DONE) {
            throw new TicketDoneException();
        }

        if (request.getStatus() != null && request.getStatus() != ticket.getStatus()) {
            validateStatusTransition(ticket, request.getStatus());
        }

        boolean priorityManuallyChanged = request.getPriority() != null
                && request.getPriority() != ticket.getPriority();

        if (request.getTitle() != null) {
            ticket.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            ticket.setDescription(request.getDescription());
        }
        if (request.getStatus() != null) {
            ticket.setStatus(request.getStatus());
        }
        if (request.getPriority() != null) {
            ticket.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", request.getAssigneeId()));
            ticket.setAssigneeId(request.getAssigneeId());
        }
        if (request.getDueDate() != null) {
            ticket.setDueDate(request.getDueDate());
        }

        if (priorityManuallyChanged) {
            ticket.setOverdue(false);
        }

        ticketRepository.save(ticket);
        auditLogService.logUserAction(AuditAction.UPDATE, EntityType.TICKET, id, performedBy);
    }

    @Transactional
    public void softDeleteTicket(Long id, Long performedBy) {
        Ticket ticket = findTicketOrThrow(id);
        ticket.setDeleted(true);
        ticketRepository.save(ticket);
        auditLogService.logUserAction(AuditAction.DELETE, EntityType.TICKET, id, performedBy);
    }

    @Transactional
    public void restoreTicket(Long id, Long performedBy) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));

        if (!ticket.isDeleted()) {
            throw new IllegalArgumentException("Ticket is not deleted");
        }

        ticket.setDeleted(false);
        ticketRepository.save(ticket);
        auditLogService.logUserAction(AuditAction.RESTORE, EntityType.TICKET, id, performedBy);
    }

    public List<TicketResponse> getDeletedTickets(Long projectId) {
        return ticketRepository.findByDeletedTrueAndProjectId(projectId).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }

    public Ticket findTicketOrThrow(Long id) {
        return ticketRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket", id));
    }

    private void validateStatusTransition(Ticket ticket, TicketStatus newStatus) {
        if (!ticket.getStatus().canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    "Cannot transition from " + ticket.getStatus() + " to " + newStatus);
        }

        if (newStatus == TicketStatus.DONE) {
            boolean hasUnresolvedBlockers = ticketDependencyRepository
                    .findByTicketId(ticket.getId()).stream()
                    .anyMatch(dep -> {
                        Ticket blocker = ticketRepository.findByIdAndDeletedFalse(dep.getBlockedById()).orElse(null);
                        return blocker != null && blocker.getStatus() != TicketStatus.DONE;
                    });
            if (hasUnresolvedBlockers) {
                throw new UnresolvedDependencyException(
                        "Cannot mark ticket as DONE: it has unresolved blocking dependencies");
            }
        }
    }

    private void autoAssign(Ticket ticket) {
        List<User> developers = userRepository.findByRole(Role.DEVELOPER);
        if (developers.isEmpty()) {
            return;
        }

        User leastLoaded = developers.stream()
                .min(Comparator
                        .comparingLong((User dev) -> ticketRepository
                                .countByAssigneeIdAndProjectIdAndStatusNotAndDeletedFalse(
                                        dev.getId(), ticket.getProjectId(), TicketStatus.DONE))
                        .thenComparing(User::getCreatedAt))
                .orElse(null);

        if (leastLoaded != null) {
            ticket.setAssigneeId(leastLoaded.getId());
            ticketRepository.save(ticket);
            auditLogService.logSystemAction(AuditAction.AUTO_ASSIGN, EntityType.TICKET, ticket.getId());
        }
    }
}
