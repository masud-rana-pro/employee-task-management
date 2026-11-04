package com.fifotech.taskmanager.dto;

import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.util.Map;

/**
 * Uniform error body returned for every failed request:
 * <pre>
 * { "timestamp": "...", "status": 400, "error": "Bad Request",
 *   "message": "Validation failed", "path": "/api/users",
 *   "fieldErrors": { "email": "must be a well-formed email address" } }
 * </pre>
 * "fieldErrors" is only present for validation failures (null values are not serialized).
 */
public record ApiError(
        String timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors) {

    public static ApiError of(HttpStatus status, String message, String path) {
        return new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, path, null);
    }

    public static ApiError of(HttpStatus status, String message, String path, Map<String, String> fieldErrors) {
        return new ApiError(Instant.now().toString(), status.value(), status.getReasonPhrase(), message, path, fieldErrors);
    }
}
