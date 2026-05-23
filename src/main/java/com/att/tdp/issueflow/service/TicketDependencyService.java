package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.dto.response.DependencyResponse;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.TicketDependency;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.exception.CrossProjectDependencyException;
import com.att.tdp.issueflow.exception.DuplicateResourceException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TicketDependencyService {

    private final TicketDependencyRepository ticketDependencyRepository;
    private final TicketService ticketService;
    private final AuditLogService auditLogService;

    @Transactional
    public void addDependency(Long ticketId, AddDependencyRequest request, Long performedBy) {
        Ticket ticket = ticketService.findTicketOrThrow(ticketId);
        Ticket blocker = ticketService.findTicketOrThrow(request.getBlockedBy());

        if (ticketId.equals(request.getBlockedBy())) {
            throw new IllegalArgumentException("A ticket cannot depend on itself");
        }

        if (!ticket.getProjectId().equals(blocker.getProjectId())) {
            throw new CrossProjectDependencyException(
                    "Both tickets must belong to the same project");
        }

        if (ticketDependencyRepository.existsByTicketIdAndBlockedById(ticketId, request.getBlockedBy())) {
            throw new DuplicateResourceException("This dependency already exists");
        }

        TicketDependency dependency = TicketDependency.builder()
                .ticketId(ticketId)
                .blockedById(request.getBlockedBy())
                .build();

        ticketDependencyRepository.save(dependency);
        auditLogService.logUserAction(AuditAction.CREATE, EntityType.DEPENDENCY, dependency.getId(), performedBy);
    }

    public List<DependencyResponse> listDependencies(Long ticketId) {
        ticketService.findTicketOrThrow(ticketId);
        return ticketDependencyRepository.findByTicketId(ticketId).stream()
                .map(dep -> {
                    Ticket blocker = ticketService.findTicketOrThrow(dep.getBlockedById());
                    return DependencyResponse.builder()
                            .id(blocker.getId())
                            .title(blocker.getTitle())
                            .status(blocker.getStatus())
                            .build();
                })
                .toList();
    }

    @Transactional
    public void removeDependency(Long ticketId, Long blockerId, Long performedBy) {
        TicketDependency dependency = ticketDependencyRepository
                .findByTicketIdAndBlockedById(ticketId, blockerId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Dependency not found between ticket " + ticketId + " and blocker " + blockerId));

        ticketDependencyRepository.delete(dependency);
        auditLogService.logUserAction(AuditAction.DELETE, EntityType.DEPENDENCY, dependency.getId(), performedBy);
    }
}
