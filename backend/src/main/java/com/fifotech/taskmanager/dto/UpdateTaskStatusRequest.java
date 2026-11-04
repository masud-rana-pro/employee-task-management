package com.fifotech.taskmanager.dto;

import com.fifotech.taskmanager.enums.TaskStatus;
import jakarta.validation.constraints.NotNull;

/** Body of PUT /api/tasks/{id}/status. */
public record UpdateTaskStatusRequest(
        @NotNull(message = "Status is required")
        TaskStatus status) {
}
