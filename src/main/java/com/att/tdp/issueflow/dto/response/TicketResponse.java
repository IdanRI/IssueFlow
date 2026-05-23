package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.enums.Priority;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.enums.TicketType;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponse {
    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private Priority priority;
    private TicketType type;
    private Long projectId;
    private Long assigneeId;
    private Instant dueDate;

    @JsonProperty("isOverdue")
    private boolean isOverdue;
}
