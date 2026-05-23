package com.att.tdp.issueflow.service;

import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.enums.ActorType;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import com.att.tdp.issueflow.mapper.AuditLogMapper;
import com.att.tdp.issueflow.repository.AuditLogRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Transactional
    public void logUserAction(AuditAction action, EntityType entityType, Long entityId, Long performedBy) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .performedBy(performedBy)
                .actor(ActorType.USER)
                .build();
        auditLogRepository.save(log);
    }

    @Transactional
    public void logSystemAction(AuditAction action, EntityType entityType, Long entityId) {
        AuditLog log = AuditLog.builder()
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .actor(ActorType.SYSTEM)
                .build();
        auditLogRepository.save(log);
    }

    public List<AuditLogResponse> getAuditLogs(String entityType, Long entityId, String action, String actor) {
        List<AuditLog> logs = auditLogRepository.findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (entityType != null) {
                predicates.add(cb.equal(root.get("entityType"), EntityType.valueOf(entityType)));
            }
            if (entityId != null) {
                predicates.add(cb.equal(root.get("entityId"), entityId));
            }
            if (action != null) {
                predicates.add(cb.equal(root.get("action"), AuditAction.valueOf(action)));
            }
            if (actor != null) {
                predicates.add(cb.equal(root.get("actor"), ActorType.valueOf(actor)));
            }

            query.orderBy(cb.desc(root.get("timestamp")));
            return cb.and(predicates.toArray(new Predicate[0]));
        });

        return logs.stream().map(AuditLogMapper::toResponse).toList();
    }
}
