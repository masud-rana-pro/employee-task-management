package com.fifotech.taskmanager.dto;

import com.fifotech.taskmanager.enums.TaskPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/** Body of POST /api/tasks and PUT /api/tasks/{id}. */
public record TaskRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @Size(max = 2000, message = "Description must be at most 2000 characters")
        String description,

        /** Optional. Format yyyy-MM-dd. */
        LocalDate deadline,

        /** Optional - defaults to MEDIUM. */
        TaskPriority priority,

        @NotNull(message = "Assignee is required")
        Long assignedTo) {
}
