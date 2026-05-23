package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddDependencyRequest {

    @NotNull(message = "blockedBy ticket ID is required")
    private Long blockedBy;
}
