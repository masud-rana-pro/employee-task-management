package com.fifotech.taskmanager.dto;

import com.fifotech.taskmanager.enums.TaskPriority;
import com.fifotech.taskmanager.enums.TaskStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        TaskPriority priority,
        LocalDate deadline,
        Long assignedToId,
        String assignedToName,
        Long assignedById,
        String assignedByName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
