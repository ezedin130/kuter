package com.kuter.common;

/**
 * Thrown when an authenticated user attempts an operation outside their authorized boundaries.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
