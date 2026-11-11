package com.kuter.common;

/**
 * Thrown when a unique constraint or conflicting state is encountered.
 */
public class ConflictException extends RuntimeException {

    public ConflictException(String message) {
        super(message);
    }
}
