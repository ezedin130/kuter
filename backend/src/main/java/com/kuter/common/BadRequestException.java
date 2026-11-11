package com.kuter.common;

/**
 * Thrown when an invalid operation or state transition is requested.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
