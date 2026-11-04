package com.fifotech.taskmanager.exception;

/** A business rule was violated by the request data. Mapped to HTTP 400. */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
