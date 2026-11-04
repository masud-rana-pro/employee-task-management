package com.fifotech.taskmanager.enums;

/**
 * Task life cycle: PENDING -> IN_PROGRESS -> DONE.
 * The workflow only moves forward, one step at a time.
 */
public enum TaskStatus {
    PENDING,
    IN_PROGRESS,
    DONE;

    /** @return the only status this status may move to, or null when it is final. */
    public TaskStatus next() {
        return switch (this) {
            case PENDING -> IN_PROGRESS;
            case IN_PROGRESS -> DONE;
            case DONE -> null;
        };
    }

    public boolean canMoveTo(TaskStatus target) {
        return target != null && target == next();
    }
}
