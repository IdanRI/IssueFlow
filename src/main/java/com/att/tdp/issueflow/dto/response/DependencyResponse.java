package com.att.tdp.issueflow.dto.response;

import com.att.tdp.issueflow.enums.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyResponse {
    private Long id;
    private String title;
    private TicketStatus status;
}
