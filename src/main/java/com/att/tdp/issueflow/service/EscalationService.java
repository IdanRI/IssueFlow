package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.enums.Priority;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class EscalationService {

    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    @Transactional
    public void escalateOverdueTickets() {
        List<Ticket> overdueTickets = ticketRepository
                .findByDueDateBeforeAndStatusNotAndDeletedFalseAndDueDateIsNotNull(
                        Instant.now(), TicketStatus.DONE);

        for (Ticket ticket : overdueTickets) {
            if (ticket.getPriority().canEscalate()) {
                Priority oldPriority = ticket.getPriority();
                ticket.setPriority(ticket.getPriority().escalate());
                log.info("Escalated ticket {} priority from {} to {}",
                        ticket.getId(), oldPriority, ticket.getPriority());

                if (ticket.getPriority() == Priority.CRITICAL) {
                    ticket.setOverdue(true);
                }

                ticketRepository.save(ticket);
                auditLogService.logSystemAction(AuditAction.AUTO_ESCALATE, EntityType.TICKET, ticket.getId());
            } else if (ticket.getPriority() == Priority.CRITICAL && !ticket.isOverdue()) {
                ticket.setOverdue(true);
                ticketRepository.save(ticket);
                auditLogService.logSystemAction(AuditAction.AUTO_ESCALATE, EntityType.TICKET, ticket.getId());
            }
        }
    }
}
