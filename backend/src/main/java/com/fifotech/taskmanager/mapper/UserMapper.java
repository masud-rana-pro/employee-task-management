package com.fifotech.taskmanager.mapper;

import com.fifotech.taskmanager.dto.UserResponse;
import com.fifotech.taskmanager.dto.UserSummary;
import com.fifotech.taskmanager.entity.User;

/** Entity -> DTO conversion. Call it inside a transaction (the manager is lazily loaded). */
public final class UserMapper {

    private UserMapper() {
    }

    public static UserResponse toResponse(User user) {
        User manager = user.getManager();
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole(),
                manager != null ? manager.getId() : null,
                manager != null ? manager.getFullName() : null,
                user.isActive(),
                user.getCreatedAt());
    }

    public static UserSummary toSummary(User user) {
        return new UserSummary(user.getId(), user.getFullName(), user.getEmail());
    }
}
