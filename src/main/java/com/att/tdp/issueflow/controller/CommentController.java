package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @GetMapping
    public ResponseEntity<List<CommentResponse>> getComments(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.getCommentsForTicket(ticketId));
    }

    @PostMapping
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long ticketId,
                                                       @Valid @RequestBody CreateCommentRequest request,
                                                       Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(commentService.addComment(ticketId, request, user.getId()));
    }

    @PatchMapping("/{commentId}")
    public ResponseEntity<Void> updateComment(@PathVariable Long ticketId,
                                              @PathVariable Long commentId,
                                              @Valid @RequestBody UpdateCommentRequest request,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        commentService.updateComment(ticketId, commentId, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long ticketId,
                                              @PathVariable Long commentId,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        commentService.deleteComment(ticketId, commentId, user.getId());
        return ResponseEntity.ok().build();
    }
}
