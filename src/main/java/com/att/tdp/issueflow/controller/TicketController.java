package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateTicketRequest;
import com.att.tdp.issueflow.dto.request.UpdateTicketRequest;
import com.att.tdp.issueflow.dto.response.ImportResultResponse;
import com.att.tdp.issueflow.dto.response.TicketResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.Role;
import com.att.tdp.issueflow.exception.AccessDeniedException;
import com.att.tdp.issueflow.service.CsvService;
import com.att.tdp.issueflow.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;
    private final CsvService csvService;

    @GetMapping
    public ResponseEntity<List<TicketResponse>> getTicketsByProject(@RequestParam Long projectId) {
        return ResponseEntity.ok(ticketService.getTicketsByProject(projectId));
    }

    @GetMapping("/{ticketId}")
    public ResponseEntity<TicketResponse> getTicketById(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketService.getTicketById(ticketId));
    }

    @PostMapping
    public ResponseEntity<TicketResponse> createTicket(@Valid @RequestBody CreateTicketRequest request,
                                                        Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(ticketService.createTicket(request, user.getId()));
    }

    @PatchMapping("/{ticketId}")
    public ResponseEntity<Void> updateTicket(@PathVariable Long ticketId,
                                             @RequestBody UpdateTicketRequest request,
                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ticketService.updateTicket(ticketId, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{ticketId}")
    public ResponseEntity<Void> deleteTicket(@PathVariable Long ticketId,
                                             Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ticketService.softDeleteTicket(ticketId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<TicketResponse>> getDeletedTickets(@RequestParam Long projectId,
                                                                   Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(ticketService.getDeletedTickets(projectId));
    }

    @PostMapping("/{ticketId}/restore")
    public ResponseEntity<Void> restoreTicket(@PathVariable Long ticketId,
                                              Authentication authentication) {
        requireAdmin(authentication);
        User user = (User) authentication.getPrincipal();
        ticketService.restoreTicket(ticketId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTickets(@RequestParam Long projectId) {
        byte[] csv = csvService.exportTickets(projectId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"tickets.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }

    @PostMapping("/import")
    public ResponseEntity<ImportResultResponse> importTickets(
            @RequestParam("file") MultipartFile file,
            @RequestParam("projectId") Long projectId,
            Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(csvService.importTickets(file, projectId, user.getId()));
    }

    private void requireAdmin(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN users can perform this action");
        }
    }
}
