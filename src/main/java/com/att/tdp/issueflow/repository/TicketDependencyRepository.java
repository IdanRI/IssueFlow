package com.att.tdp.issueflow.repository;

import com.att.tdp.issueflow.entity.TicketDependency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {

    List<TicketDependency> findByTicketId(Long ticketId);

    Optional<TicketDependency> findByTicketIdAndBlockedById(Long ticketId, Long blockedById);

    boolean existsByTicketIdAndBlockedById(Long ticketId, Long blockedById);
}
