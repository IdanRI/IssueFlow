package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.ImportResultResponse;
import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.enums.Priority;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.enums.TicketType;
import com.att.tdp.issueflow.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CsvService {

    private final TicketRepository ticketRepository;
    private final ProjectService projectService;
    private final AuditLogService auditLogService;

    public byte[] exportTickets(Long projectId) {
        projectService.findProjectOrThrow(projectId);
        List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedFalse(projectId);

        try (StringWriter writer = new StringWriter();
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT
                     .builder()
                     .setHeader("id", "title", "description", "status", "priority", "type", "assigneeId")
                     .build())) {

            for (Ticket ticket : tickets) {
                printer.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getType(),
                        ticket.getAssigneeId()
                );
            }

            printer.flush();
            return writer.toString().getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("Failed to export tickets to CSV", e);
        }
    }

    @Transactional
    public ImportResultResponse importTickets(MultipartFile file, Long projectId, Long performedBy) {
        projectService.findProjectOrThrow(projectId);

        int created = 0;
        int failed = 0;
        List<String> errors = new ArrayList<>();

        try (Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT
                     .builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .setTrim(true)
                     .build()
                     .parse(reader)) {

            int rowNum = 1;
            for (CSVRecord record : parser) {
                rowNum++;
                try {
                    Ticket ticket = parseTicketFromCsv(record, projectId);
                    Ticket saved = ticketRepository.save(ticket);
                    auditLogService.logUserAction(AuditAction.CREATE, EntityType.TICKET, saved.getId(), performedBy);
                    created++;
                } catch (Exception e) {
                    failed++;
                    errors.add("Row " + rowNum + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse CSV file", e);
        }

        return ImportResultResponse.builder()
                .created(created)
                .failed(failed)
                .errors(errors)
                .build();
    }

    private Ticket parseTicketFromCsv(CSVRecord record, Long projectId) {
        String title = record.get("title");
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Title is required");
        }

        TicketStatus status = TicketStatus.valueOf(record.get("status").trim());
        Priority priority = Priority.valueOf(record.get("priority").trim());
        TicketType type = TicketType.valueOf(record.get("type").trim());

        Long assigneeId = null;
        String assigneeIdStr = record.get("assigneeId");
        if (assigneeIdStr != null && !assigneeIdStr.isBlank()) {
            assigneeId = Long.parseLong(assigneeIdStr.trim());
        }

        return Ticket.builder()
                .title(title)
                .description(record.get("description"))
                .status(status)
                .priority(priority)
                .type(type)
                .projectId(projectId)
                .assigneeId(assigneeId)
                .build();
    }
}
