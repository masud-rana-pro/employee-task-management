package com.fifotech.taskmanager.mapper;

import com.fifotech.taskmanager.dto.TaskResponse;
import com.fifotech.taskmanager.entity.Task;

/** Entity -> DTO conversion. Call it inside a transaction (assignee / assigner are lazily loaded). */
public final class TaskMapper {

    private TaskMapper() {
    }

    public static TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getPriority(),
                task.getDeadline(),
                task.getAssignedTo().getId(),
                task.getAssignedTo().getFullName(),
                task.getAssignedBy().getId(),
                task.getAssignedBy().getFullName(),
                task.getCreatedAt(),
                task.getUpdatedAt());
    }
}
