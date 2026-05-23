package com.att.tdp.issueflow.controller;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.dto.response.WorkloadResponse;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.Role;
import com.att.tdp.issueflow.exception.AccessDeniedException;
import com.att.tdp.issueflow.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{projectId}")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectById(projectId));
    }

    @PostMapping
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request,
                                                          Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        return ResponseEntity.ok(projectService.createProject(request, user.getId()));
    }

    @PatchMapping("/{projectId}")
    public ResponseEntity<Void> updateProject(@PathVariable Long projectId,
                                              @RequestBody UpdateProjectRequest request,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        projectService.updateProject(projectId, request, user.getId());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{projectId}")
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId,
                                              Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        projectService.softDeleteProject(projectId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/deleted")
    public ResponseEntity<List<ProjectResponse>> getDeletedProjects(Authentication authentication) {
        requireAdmin(authentication);
        return ResponseEntity.ok(projectService.getDeletedProjects());
    }

    @PostMapping("/{projectId}/restore")
    public ResponseEntity<Void> restoreProject(@PathVariable Long projectId,
                                               Authentication authentication) {
        requireAdmin(authentication);
        User user = (User) authentication.getPrincipal();
        projectService.restoreProject(projectId, user.getId());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/workload")
    public ResponseEntity<List<WorkloadResponse>> getProjectWorkload(@PathVariable Long projectId) {
        return ResponseEntity.ok(projectService.getProjectWorkload(projectId));
    }

    private void requireAdmin(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        if (user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only ADMIN users can perform this action");
        }
    }
}
