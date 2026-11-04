package com.fifotech.taskmanager.dto;

import com.fifotech.taskmanager.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Body of PUT /api/users/{id}.
 * The password is optional here: when it is null the current password is kept.
 */
public record UpdateUserRequest(
        @NotBlank(message = "Full name is required")
        @Size(max = 100, message = "Full name must be at most 100 characters")
        String fullName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a well-formed address")
        @Size(max = 150, message = "Email must be at most 150 characters")
        String email,

        @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
        String password,

        @NotNull(message = "Role is required")
        Role role,

        Long managerId) {
}
