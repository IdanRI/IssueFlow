package com.att.tdp.issueflow.enums;

public enum TicketStatus {
    TODO,
    IN_PROGRESS,
    IN_REVIEW,
    DONE;

    public boolean canTransitionTo(TicketStatus target) {
        return target.ordinal() > this.ordinal();
    }
}
