package com.att.tdp.issueflow.mapper;

import com.att.tdp.issueflow.dto.response.AuditLogResponse;
import com.att.tdp.issueflow.entity.AuditLog;

public final class AuditLogMapper {

    private AuditLogMapper() {}

    public static AuditLogResponse toResponse(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction().name())
                .entityType(log.getEntityType().name())
                .entityId(log.getEntityId())
                .performedBy(log.getPerformedBy())
                .actor(log.getActor().name())
                .timestamp(log.getTimestamp())
                .build();
    }
}
