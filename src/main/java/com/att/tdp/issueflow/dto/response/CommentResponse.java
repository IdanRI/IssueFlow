package com.att.tdp.issueflow.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {
    private Long id;
    private Long ticketId;
    private Long authorId;
    private String content;
    private List<MentionedUserResponse> mentionedUsers;
}
