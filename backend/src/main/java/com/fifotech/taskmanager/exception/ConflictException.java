package com.fifotech.taskmanager.exception;

/** The request conflicts with the current state (duplicate email, manager still has a team...). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
