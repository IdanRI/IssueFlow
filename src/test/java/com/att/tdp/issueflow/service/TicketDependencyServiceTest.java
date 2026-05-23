package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.exception.CrossProjectDependencyException;
import com.att.tdp.issueflow.exception.DuplicateResourceException;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketDependencyServiceTest {

    @Mock private TicketDependencyRepository ticketDependencyRepository;
    @Mock private TicketService ticketService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private TicketDependencyService ticketDependencyService;

    @Test
    void addDependency_crossProject_throws() {
        Ticket t1 = Ticket.builder().id(1L).projectId(1L).status(TicketStatus.TODO).build();
        Ticket t2 = Ticket.builder().id(2L).projectId(2L).status(TicketStatus.TODO).build();

        when(ticketService.findTicketOrThrow(1L)).thenReturn(t1);
        when(ticketService.findTicketOrThrow(2L)).thenReturn(t2);

        assertThatThrownBy(() -> ticketDependencyService.addDependency(
                1L, AddDependencyRequest.builder().blockedBy(2L).build(), 1L))
                .isInstanceOf(CrossProjectDependencyException.class);
    }

    @Test
    void addDependency_selfReference_throws() {
        Ticket t1 = Ticket.builder().id(1L).projectId(1L).status(TicketStatus.TODO).build();
        when(ticketService.findTicketOrThrow(1L)).thenReturn(t1);

        assertThatThrownBy(() -> ticketDependencyService.addDependency(
                1L, AddDependencyRequest.builder().blockedBy(1L).build(), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itself");
    }

    @Test
    void addDependency_duplicate_throws() {
        Ticket t1 = Ticket.builder().id(1L).projectId(1L).build();
        Ticket t2 = Ticket.builder().id(2L).projectId(1L).build();

        when(ticketService.findTicketOrThrow(1L)).thenReturn(t1);
        when(ticketService.findTicketOrThrow(2L)).thenReturn(t2);
        when(ticketDependencyRepository.existsByTicketIdAndBlockedById(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> ticketDependencyService.addDependency(
                1L, AddDependencyRequest.builder().blockedBy(2L).build(), 1L))
                .isInstanceOf(DuplicateResourceException.class);
    }
}
