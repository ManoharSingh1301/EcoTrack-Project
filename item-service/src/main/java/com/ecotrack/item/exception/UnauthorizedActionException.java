package com.ecotrack.item.exception;

/**
 * Thrown when an authenticated user attempts to modify a resource they do not own.
 * Maps to HTTP 403 Forbidden via GlobalExceptionHandler.
 */
public class UnauthorizedActionException extends RuntimeException {

    public UnauthorizedActionException(String message) {
        super(message);
    }
}
