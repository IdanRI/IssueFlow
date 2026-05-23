package com.att.tdp.issueflow.dto.request;

import com.att.tdp.issueflow.enums.Priority;
import com.att.tdp.issueflow.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTicketRequest {

    private String title;
    private String description;
    private TicketStatus status;
    private Priority priority;
    private Long assigneeId;
    private Instant dueDate;
}
