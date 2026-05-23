package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.ImportResultResponse;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.enums.Priority;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.enums.TicketType;
import com.att.tdp.issueflow.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvServiceTest {

    @Mock private TicketRepository ticketRepository;
    @Mock private ProjectService projectService;
    @Mock private AuditLogService auditLogService;

    @InjectMocks
    private CsvService csvService;

    @Test
    void exportTickets_generatesValidCsv() {
        when(projectService.findProjectOrThrow(1L)).thenReturn(Project.builder().id(1L).build());
        Ticket ticket = Ticket.builder()
                .id(1L).title("Test Bug").description("A bug with, commas")
                .status(TicketStatus.TODO).priority(Priority.HIGH).type(TicketType.BUG)
                .projectId(1L).assigneeId(2L).build();
        when(ticketRepository.findByProjectIdAndDeletedFalse(1L)).thenReturn(List.of(ticket));

        byte[] csv = csvService.exportTickets(1L);
        String csvString = new String(csv, StandardCharsets.UTF_8);

        assertThat(csvString).contains("id,title,description,status,priority,type,assigneeId");
        assertThat(csvString).contains("Test Bug");
        assertThat(csvString).contains("\"A bug with, commas\"");
    }

    @Test
    void importTickets_validCsv_createsTickets() {
        when(projectService.findProjectOrThrow(1L)).thenReturn(Project.builder().id(1L).build());
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        String csv = "title,description,status,priority,type,assigneeId\n" +
                "Bug1,Desc1,TODO,HIGH,BUG,1\n" +
                "Feature1,Desc2,TODO,LOW,FEATURE,\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "tickets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        ImportResultResponse result = csvService.importTickets(file, 1L, 1L);

        assertThat(result.getCreated()).isEqualTo(2);
        assertThat(result.getFailed()).isEqualTo(0);
    }

    @Test
    void importTickets_invalidRow_reportsError() {
        when(projectService.findProjectOrThrow(1L)).thenReturn(Project.builder().id(1L).build());

        String csv = "title,description,status,priority,type,assigneeId\n" +
                "Good,Desc,TODO,HIGH,BUG,1\n" +
                "Bad,Desc,INVALID_STATUS,HIGH,BUG,1\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "tickets.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> {
            Ticket t = inv.getArgument(0);
            t.setId(1L);
            return t;
        });

        ImportResultResponse result = csvService.importTickets(file, 1L, 1L);

        assertThat(result.getCreated()).isEqualTo(1);
        assertThat(result.getFailed()).isEqualTo(1);
        assertThat(result.getErrors()).isNotEmpty();
    }
}
