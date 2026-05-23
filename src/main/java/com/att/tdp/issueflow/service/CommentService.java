package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateCommentRequest;
import com.att.tdp.issueflow.dto.request.UpdateCommentRequest;
import com.att.tdp.issueflow.dto.response.CommentResponse;
import com.att.tdp.issueflow.entity.Comment;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.CommentRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final TicketService ticketService;
    private final MentionService mentionService;
    private final AuditLogService auditLogService;

    public List<CommentResponse> getCommentsForTicket(Long ticketId) {
        ticketService.findTicketOrThrow(ticketId);
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request, Long performedBy) {
        ticketService.findTicketOrThrow(ticketId);
        userRepository.findById(request.getAuthorId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getAuthorId()));

        Comment comment = Comment.builder()
                .ticketId(ticketId)
                .authorId(request.getAuthorId())
                .content(request.getContent())
                .build();

        Comment saved = commentRepository.save(comment);
        mentionService.syncMentions(saved.getId(), saved.getContent());
        auditLogService.logUserAction(AuditAction.CREATE, EntityType.COMMENT, saved.getId(), performedBy);

        return toResponse(saved);
    }

    @Transactional
    public void updateComment(Long ticketId, Long commentId, UpdateCommentRequest request, Long performedBy) {
        ticketService.findTicketOrThrow(ticketId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.getTicketId().equals(ticketId)) {
            throw new ResourceNotFoundException("Comment " + commentId + " does not belong to ticket " + ticketId);
        }

        comment.setContent(request.getContent());
        commentRepository.save(comment);
        mentionService.syncMentions(commentId, request.getContent());
        auditLogService.logUserAction(AuditAction.UPDATE, EntityType.COMMENT, commentId, performedBy);
    }

    @Transactional
    public void deleteComment(Long ticketId, Long commentId, Long performedBy) {
        ticketService.findTicketOrThrow(ticketId);
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", commentId));

        if (!comment.getTicketId().equals(ticketId)) {
            throw new ResourceNotFoundException("Comment " + commentId + " does not belong to ticket " + ticketId);
        }

        commentRepository.delete(comment);
        auditLogService.logUserAction(AuditAction.DELETE, EntityType.COMMENT, commentId, performedBy);
    }

    private CommentResponse toResponse(Comment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicketId())
                .authorId(comment.getAuthorId())
                .content(comment.getContent())
                .mentionedUsers(mentionService.getMentionedUsers(comment.getId()))
                .build();
    }
}
