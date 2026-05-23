package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.AuditLog;
import com.att.tdp.issueflow.enums.ActorType;
import com.att.tdp.issueflow.enums.AuditAction;
import com.att.tdp.issueflow.enums.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {

    List<AuditLog> findByEntityType(EntityType entityType);

    List<AuditLog> findByEntityId(Long entityId);

    List<AuditLog> findByAction(AuditAction action);

    List<AuditLog> findByActor(ActorType actor);

    List<AuditLog> findByEntityTypeAndEntityId(EntityType entityType, Long entityId);
}
