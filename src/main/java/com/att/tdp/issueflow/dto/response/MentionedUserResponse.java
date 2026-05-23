package com.att.tdp.issueflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MentionedUserResponse {
    private Long id;
    private String username;
    private String fullName;
}
