package com.fifotech.taskmanager.dto;

/** Response of POST /api/auth/login, exactly as required by the specification: { token, role, name }. */
public record LoginResponse(String token, String role, String name) {
}
