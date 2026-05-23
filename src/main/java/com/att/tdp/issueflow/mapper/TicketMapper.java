package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.entity.Ticket;

public final class TicketMapper {

    private TicketMapper() {}

    public static TicketResponse toResponse(Ticket ticket) {
        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .type(ticket.getType())
                .projectId(ticket.getProjectId())
                .assigneeId(ticket.getAssigneeId())
                .dueDate(ticket.getDueDate())
                .isOverdue(ticket.isOverdue())
                .build();
    }

    public static Ticket toEntity(CreateTicketRequest request) {
        return Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus())
                .priority(request.getPriority())
                .type(request.getType())
                .projectId(request.getProjectId())
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .build();
    }
}
