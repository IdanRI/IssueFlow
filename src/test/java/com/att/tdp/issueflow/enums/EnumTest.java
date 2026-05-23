package com.att.tdp.issueflow.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class EnumTest {

    @Test
    void ticketStatus_forwardTransitions() {
        assertThat(TicketStatus.TODO.canTransitionTo(TicketStatus.IN_PROGRESS)).isTrue();
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.IN_REVIEW)).isTrue();
        assertThat(TicketStatus.IN_REVIEW.canTransitionTo(TicketStatus.DONE)).isTrue();
    }

    @Test
    void ticketStatus_backwardTransitions_rejected() {
        assertThat(TicketStatus.IN_PROGRESS.canTransitionTo(TicketStatus.TODO)).isFalse();
        assertThat(TicketStatus.DONE.canTransitionTo(TicketStatus.IN_REVIEW)).isFalse();
        assertThat(TicketStatus.IN_REVIEW.canTransitionTo(TicketStatus.IN_PROGRESS)).isFalse();
    }

    @Test
    void ticketStatus_sameStatus_rejected() {
        assertThat(TicketStatus.TODO.canTransitionTo(TicketStatus.TODO)).isFalse();
    }

    @Test
    void priority_escalation() {
        assertThat(Priority.LOW.escalate()).isEqualTo(Priority.MEDIUM);
        assertThat(Priority.MEDIUM.escalate()).isEqualTo(Priority.HIGH);
        assertThat(Priority.HIGH.escalate()).isEqualTo(Priority.CRITICAL);
        assertThat(Priority.CRITICAL.escalate()).isEqualTo(Priority.CRITICAL);
    }

    @Test
    void priority_canEscalate() {
        assertThat(Priority.LOW.canEscalate()).isTrue();
        assertThat(Priority.MEDIUM.canEscalate()).isTrue();
        assertThat(Priority.HIGH.canEscalate()).isTrue();
        assertThat(Priority.CRITICAL.canEscalate()).isFalse();
    }

    @Test
    void ticketStatus_skipTransitions_allowed() {
        assertThat(TicketStatus.TODO.canTransitionTo(TicketStatus.DONE)).isTrue();
        assertThat(TicketStatus.TODO.canTransitionTo(TicketStatus.IN_REVIEW)).isTrue();
    }
}
