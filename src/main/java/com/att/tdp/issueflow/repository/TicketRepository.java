package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.Ticket;
import com.att.tdp.issueflow.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByProjectIdAndDeletedFalse(Long projectId);

    Optional<Ticket> findByIdAndDeletedFalse(Long id);

    List<Ticket> findByDeletedTrueAndProjectId(Long projectId);

    @Query("SELECT t.assigneeId, COUNT(t) FROM Ticket t " +
            "WHERE t.projectId = :projectId AND t.status <> 'DONE' AND t.deleted = false AND t.assigneeId IS NOT NULL " +
            "GROUP BY t.assigneeId")
    List<Object[]> countOpenTicketsByAssigneeInProject(@Param("projectId") Long projectId);

    List<Ticket> findByDueDateBeforeAndStatusNotAndDeletedFalseAndDueDateIsNotNull(
            Instant now, TicketStatus excludeStatus);

    long countByAssigneeIdAndProjectIdAndStatusNotAndDeletedFalse(
            Long assigneeId, Long projectId, TicketStatus excludeStatus);
}
