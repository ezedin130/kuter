package com.kuter.tenancy;

import org.springframework.security.access.AccessDeniedException;

/**
 * Thrown when an authenticated user attempts to access or modify data belonging
 * to a tenant for which they have no authorized membership.
 */
public class TenantAccessDeniedException extends AccessDeniedException {

    public TenantAccessDeniedException(String message) {
        super(message);
    }
}
