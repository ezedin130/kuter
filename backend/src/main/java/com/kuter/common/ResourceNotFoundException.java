package com.kuter.common;

/**
 * Thrown when a requested resource (tenant, user, onboarding request, etc.) is not found.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
