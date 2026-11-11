package com.kuter.common;

/**
 * Global constant definitions for headers, MDC keys, and API paths.
 */
public final class Constants {

    private Constants() {
        // Prevent instantiation
    }

    // Correlation and Request Tracing Headers
    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String TENANT_ID_HEADER = "X-Tenant-ID";
    public static final String CORRELATION_ID_MDC_KEY = "correlationId";
    public static final String REQUEST_ID_MDC_KEY = "requestId";

    // Base API Paths
    public static final String API_BASE_PATH = "/api/v1";
    public static final String HEALTH_ENDPOINT = API_BASE_PATH + "/health";
    public static final String INFO_ENDPOINT = API_BASE_PATH + "/info";
}
