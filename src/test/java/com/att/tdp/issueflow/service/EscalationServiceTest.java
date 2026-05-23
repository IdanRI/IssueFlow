package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.enums.Priority;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EscalationServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private EscalationService escalationService;

    @Test
    void escalateOverdueTickets_bumpsLowToMedium() {
        Ticket ticket = Ticket.builder()
                .id(1L).priority(Priority.LOW).status(TicketStatus.TODO)
                .dueDate(Instant.now().minusSeconds(3600)).overdue(false).build();

        when(ticketRepository.findByDueDateBeforeAndStatusNotAndDeletedFalseAndDueDateIsNotNull(
                any(Instant.class), eq(TicketStatus.DONE))).thenReturn(List.of(ticket));

        escalationService.escalateOverdueTickets();

        assertThat(ticket.getPriority()).isEqualTo(Priority.MEDIUM);
        verify(ticketRepository).save(ticket);
    }

    @Test
    void escalateOverdueTickets_highToCritical_setsOverdue() {
        Ticket ticket = Ticket.builder()
                .id(2L).priority(Priority.HIGH).status(TicketStatus.IN_PROGRESS)
                .dueDate(Instant.now().minusSeconds(7200)).overdue(false).build();

        when(ticketRepository.findByDueDateBeforeAndStatusNotAndDeletedFalseAndDueDateIsNotNull(
                any(Instant.class), eq(TicketStatus.DONE))).thenReturn(List.of(ticket));

        escalationService.escalateOverdueTickets();

        assertThat(ticket.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(ticket.isOverdue()).isTrue();
    }

    @Test
    void escalateOverdueTickets_criticalAlready_setsOverdueFlag() {
        Ticket ticket = Ticket.builder()
                .id(3L).priority(Priority.CRITICAL).status(TicketStatus.TODO)
                .dueDate(Instant.now().minusSeconds(3600)).overdue(false).build();

        when(ticketRepository.findByDueDateBeforeAndStatusNotAndDeletedFalseAndDueDateIsNotNull(
                any(Instant.class), eq(TicketStatus.DONE))).thenReturn(List.of(ticket));

        escalationService.escalateOverdueTickets();

        assertThat(ticket.getPriority()).isEqualTo(Priority.CRITICAL);
        assertThat(ticket.isOverdue()).isTrue();
    }

    @Test
    void escalateOverdueTickets_criticalAlreadyOverdue_noChange() {
        Ticket ticket = Ticket.builder()
                .id(4L).priority(Priority.CRITICAL).status(TicketStatus.TODO)
                .dueDate(Instant.now().minusSeconds(3600)).overdue(true).build();

        when(ticketRepository.findByDueDateBeforeAndStatusNotAndDeletedFalseAndDueDateIsNotNull(
                any(Instant.class), eq(TicketStatus.DONE))).thenReturn(List.of(ticket));

        escalationService.escalateOverdueTickets();

        verify(ticketRepository, never()).save(any());
    }
}
