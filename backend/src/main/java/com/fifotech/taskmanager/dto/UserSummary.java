package com.fifotech.taskmanager.dto;

/** Lightweight user data used to fill drop-downs (assignee / manager pickers). */
public record UserSummary(Long id, String fullName, String email) {
}
