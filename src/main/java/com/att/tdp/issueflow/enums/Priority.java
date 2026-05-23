package com.att.tdp.issueflow.enums;

public enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL;

    public Priority escalate() {
        return switch (this) {
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH -> CRITICAL;
            case CRITICAL -> CRITICAL;
        };
    }

    public boolean canEscalate() {
        return this != CRITICAL;
    }
}
