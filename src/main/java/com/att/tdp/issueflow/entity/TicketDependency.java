package com.att.tdp.issueflow.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "ticket_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_id", "blocked_by_id"}))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ticket_id", nullable = false)
    private Long ticketId;

    @Column(name = "blocked_by_id", nullable = false)
    private Long blockedById;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
