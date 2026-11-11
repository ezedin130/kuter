package com.kuter.tenancy;

/**
 * Thrown when an expected tenant context or tenant entity is missing.
 */
public class TenantNotFoundException extends RuntimeException {

    public TenantNotFoundException(String message) {
        super(message);
    }
}
