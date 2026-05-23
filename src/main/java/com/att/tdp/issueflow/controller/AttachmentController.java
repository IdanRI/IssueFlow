package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.response.AttachmentResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.service.AttachmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/tickets/{ticketId}/attachments")
@RequiredArgsConstructor
public class AttachmentController {

    private final AttachmentService attachmentService;

    @PostMapping
    public ResponseEntity<AttachmentResponse> uploadAttachment(@PathVariable Long ticketId,
                                                                @RequestParam("file") MultipartFile file,
                                                                Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(attachmentService.uploadAttachment(ticketId, file, user.getId()));
    }

    @DeleteMapping("/{attachmentId}")
    public ResponseEntity<Void> deleteAttachment(@PathVariable Long ticketId,
                                                 @PathVariable Long attachmentId,
                                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        attachmentService.deleteAttachment(ticketId, attachmentId, user.getId());
        return ResponseEntity.ok().build();
    }
}
