package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.request.CreateProjectRequest;
import com.att.tdp.issueflow.dto.request.UpdateProjectRequest;
import com.att.tdp.issueflow.dto.response.ProjectResponse;
import com.att.tdp.issueflow.dto.response.WorkloadResponse;
import com.att.tdp.issueflow.entity.Project;
import com.att.tdp.issueflow.entity.User;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.enums.Role;
import com.att.tdp.issueflow.enums.TicketStatus;
import com.att.tdp.issueflow.exception.ResourceNotFoundException;
import com.att.tdp.issueflow.mapper.ProjectMapper;
import com.att.tdp.issueflow.repository.ProjectRepository;
import com.att.tdp.issueflow.repository.TicketRepository;
import com.att.tdp.issueflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;
    private final TicketRepository ticketRepository;
    private final AuditLogService auditLogService;

    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findByDeletedFalse().stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    public ProjectResponse getProjectById(Long id) {
        Project project = findProjectOrThrow(id);
        return ProjectMapper.toResponse(project);
    }

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request, Long performedBy) {
        userRepository.findById(request.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("User", request.getOwnerId()));

        Project project = ProjectMapper.toEntity(request);
        Project saved = projectRepository.save(project);

        auditLogService.logUserAction(AuditAction.CREATE, EntityType.PROJECT, saved.getId(), performedBy);
        return ProjectMapper.toResponse(saved);
    }

    @Transactional
    public void updateProject(Long id, UpdateProjectRequest request, Long performedBy) {
        Project project = findProjectOrThrow(id);

        if (request.getName() != null) {
            project.setName(request.getName());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        projectRepository.save(project);
        auditLogService.logUserAction(AuditAction.UPDATE, EntityType.PROJECT, id, performedBy);
    }

    @Transactional
    public void softDeleteProject(Long id, Long performedBy) {
        Project project = findProjectOrThrow(id);
        project.setDeleted(true);
        projectRepository.save(project);
        auditLogService.logUserAction(AuditAction.DELETE, EntityType.PROJECT, id, performedBy);
    }

    @Transactional
    public void restoreProject(Long id, Long performedBy) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (!project.isDeleted()) {
            throw new IllegalArgumentException("Project is not deleted");
        }

        project.setDeleted(false);
        projectRepository.save(project);
        auditLogService.logUserAction(AuditAction.RESTORE, EntityType.PROJECT, id, performedBy);
    }

    public List<ProjectResponse> getDeletedProjects() {
        return projectRepository.findByDeletedTrue().stream()
                .map(ProjectMapper::toResponse)
                .toList();
    }

    public List<WorkloadResponse> getProjectWorkload(Long projectId) {
        findProjectOrThrow(projectId);

        List<Object[]> counts = ticketRepository.countOpenTicketsByAssigneeInProject(projectId);
        Map<Long, Long> workloadMap = new HashMap<>();
        for (Object[] row : counts) {
            workloadMap.put((Long) row[0], (Long) row[1]);
        }

        List<User> developers = userRepository.findByRole(Role.DEVELOPER);
        List<WorkloadResponse> result = new ArrayList<>();

        for (User dev : developers) {
            long openCount = workloadMap.getOrDefault(dev.getId(), 0L);
            result.add(WorkloadResponse.builder()
                    .userId(dev.getId())
                    .username(dev.getUsername())
                    .openTicketCount(openCount)
                    .build());
        }

        result.sort((a, b) -> Long.compare(a.getOpenTicketCount(), b.getOpenTicketCount()));
        return result;
    }

    public Project findProjectOrThrow(Long id) {
        return projectRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));
    }
}
