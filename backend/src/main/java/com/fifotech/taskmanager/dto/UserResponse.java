package com.fifotech.taskmanager.dto;

import com.fifotech.taskmanager.enums.Role;

import java.time.LocalDateTime;

/** What the API exposes about a user. The password hash is deliberately absent. */
public record UserResponse(
        Long id,
        String fullName,
        String email,
        Role role,
        Long managerId,
        String managerName,
        boolean active,
        LocalDateTime createdAt) {
}
