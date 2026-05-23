package com.att.tdp.issueflow.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotNull(message = "Author ID is required")
    private Long authorId;

    @NotBlank(message = "Content is required")
    @Size(min = 1, max = 5000, message = "Content must be between 1 and 5000 characters")
    private String content;
}
