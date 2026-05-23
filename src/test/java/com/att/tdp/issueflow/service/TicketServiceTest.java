package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.*;
import com.att.tdp.issueflow.exception.InvalidStatusTransitionException;
import com.att.tdp.issueflow.exception.TicketDoneException;
import com.att.tdp.issueflow.repository.TicketDependencyRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProjectService projectService;
    @Mock private AuditLogService auditLogService;
    @Mock private TicketDependencyRepository ticketDependencyRepository;

    @InjectMocks
    private TicketService ticketService;

    @Test
    void createTicket_success() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Bug fix")
                .status(TicketStatus.TODO)
                .priority(Priority.HIGH)
                .type(TicketType.BUG)
                .projectId(1L)
                .assigneeId(2L)
                .build();

        when(projectService.findProjectOrThrow(1L)).thenReturn(Project.builder().id(1L).build());
        when(userRepository.findById(2L)).thenReturn(Optional.of(User.builder().id(2L).build()));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        TicketResponse response = ticketService.createTicket(request, 1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getTitle()).isEqualTo("Bug fix");
        assertThat(response.getStatus()).isEqualTo(TicketStatus.TODO);
    }

    @Test
    void createTicket_withoutAssignee_triggersAutoAssign() {
        CreateTicketRequest request = CreateTicketRequest.builder()
                .title("Task").status(TicketStatus.TODO).priority(Priority.LOW)
                .type(TicketType.FEATURE).projectId(1L).build();

        when(projectService.findProjectOrThrow(1L)).thenReturn(Project.builder().id(1L).build());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        User dev1 = User.builder().id(5L).username("dev1").role(Role.DEVELOPER)
                .createdAt(Instant.now().minusSeconds(100)).build();
        User dev2 = User.builder().id(6L).username("dev2").role(Role.DEVELOPER)
                .createdAt(Instant.now()).build();
        when(userRepository.findByRole(Role.DEVELOPER)).thenReturn(List.of(dev1, dev2));
        when(ticketRepository.countByAssigneeIdAndProjectIdAndStatusNotAndDeletedFalse(
                anyLong(), anyLong(), any())).thenReturn(0L);

        ticketService.createTicket(request, 1L);

        verify(ticketRepository, atLeast(2)).save(any(Ticket.class));
        verify(auditLogService).logSystemAction(AuditAction.AUTO_ASSIGN, EntityType.TICKET, 1L);
    }

    @Test
    void updateTicket_doneTicket_throws() {
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.DONE).build();
        when(ticketRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() ->
                ticketService.updateTicket(1L, UpdateTicketRequest.builder().title("new").build(), 1L))
                .isInstanceOf(TicketDoneException.class);
    }

    @Test
    void updateTicket_backwardTransition_throws() {
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.IN_PROGRESS).build();
        when(ticketRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() ->
                ticketService.updateTicket(1L, UpdateTicketRequest.builder().status(TicketStatus.TODO).build(), 1L))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    @Test
    void updateTicket_forwardTransition_success() {
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.TODO).projectId(1L).build();
        when(ticketRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        ticketService.updateTicket(1L,
                UpdateTicketRequest.builder().status(TicketStatus.IN_PROGRESS).build(), 1L);

        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    void updateTicket_manualPriorityChange_resetsOverdue() {
        Ticket ticket = Ticket.builder().id(1L).status(TicketStatus.TODO).priority(Priority.CRITICAL)
                .overdue(true).projectId(1L).build();
        when(ticketRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        ticketService.updateTicket(1L, UpdateTicketRequest.builder().priority(Priority.LOW).build(), 1L);

        assertThat(ticket.isOverdue()).isFalse();
        assertThat(ticket.getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void softDeleteTicket_success() {
        Ticket ticket = Ticket.builder().id(1L).deleted(false).build();
        when(ticketRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(ticket));
        when(ticketRepository.save(any())).thenReturn(ticket);

        ticketService.softDeleteTicket(1L, 1L);

        assertThat(ticket.isDeleted()).isTrue();
    }
}
