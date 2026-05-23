package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.entity.Attachment;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.exception.InvalidFileTypeException;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.repository.AttachmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AttachmentService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/png", "image/jpeg", "application/pdf", "text/plain"
    );
    private static final long MAX_SIZE = 10 * 1024 * 1024;

    private final AttachmentRepository attachmentRepository;
    private final TicketService ticketService;
    private final AuditLogService auditLogService;

    @Transactional
    public AttachmentResponse uploadAttachment(Long ticketId, MultipartFile file, Long performedBy) {
        ticketService.findTicketOrThrow(ticketId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new InvalidFileTypeException(
                    "File type '" + contentType + "' is not allowed. Allowed types: " + ALLOWED_TYPES);
        }

        if (file.getSize() > MAX_SIZE) {
            throw new IllegalArgumentException("File size exceeds the maximum allowed size of 10MB");
        }

        try {
            Attachment attachment = Attachment.builder()
                    .ticketId(ticketId)
                    .filename(file.getOriginalFilename())
                    .contentType(contentType)
                    .data(file.getBytes())
                    .size(file.getSize())
                    .build();

            Attachment saved = attachmentRepository.save(attachment);
            auditLogService.logUserAction(AuditAction.CREATE, EntityType.ATTACHMENT, saved.getId(), performedBy);

            return toResponse(saved);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read file data", e);
        }
    }

    @Transactional
    public void deleteAttachment(Long ticketId, Long attachmentId, Long performedBy) {
        ticketService.findTicketOrThrow(ticketId);
        Attachment attachment = attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Attachment", attachmentId));

        if (!attachment.getTicketId().equals(ticketId)) {
            throw new ResourceNotFoundException(
                    "Attachment " + attachmentId + " does not belong to ticket " + ticketId);
        }

        attachmentRepository.delete(attachment);
        auditLogService.logUserAction(AuditAction.DELETE, EntityType.ATTACHMENT, attachmentId, performedBy);
    }

    private AttachmentResponse toResponse(Attachment attachment) {
        return AttachmentResponse.builder()
                .id(attachment.getId())
                .ticketId(attachment.getTicketId())
                .filename(attachment.getFilename())
                .contentType(attachment.getContentType())
                .build();
    }
}
