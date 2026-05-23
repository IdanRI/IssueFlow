package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.AddDependencyRequest;
import com.att.tdp.issueflow.dto.response.DependencyResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.service.TicketDependencyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets/{ticketId}/dependencies")
@RequiredArgsConstructor
public class TicketDependencyController {

    private final TicketDependencyService ticketDependencyService;

    @PostMapping
    public ResponseEntity<Void> addDependency(@PathVariable Long ticketId,
                                              @Valid @RequestBody AddDependencyRequest request,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ticketDependencyService.addDependency(ticketId, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<List<DependencyResponse>> listDependencies(@PathVariable Long ticketId) {
        return ResponseEntity.ok(ticketDependencyService.listDependencies(ticketId));
    }

    @DeleteMapping("/{blockerId}")
    public ResponseEntity<Void> removeDependency(@PathVariable Long ticketId,
                                                 @PathVariable Long blockerId,
                                                 Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        ticketDependencyService.removeDependency(ticketId, blockerId, user.getId());
        return ResponseEntity.ok().build();
    }
}
