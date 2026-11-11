package com.kuter.health;

import java.time.Instant;

/**
 * Health check response DTO.
 */
public record HealthResponse(
        String status,
        String application,
        String timestamp,
        String database,
        String environment
) {
    public static HealthResponse of(String application, String database, String environment) {
        String overallStatus = "CONNECTED".equalsIgnoreCase(database) ? "UP" : "DOWN";
        return new HealthResponse(
                overallStatus,
                application,
                Instant.now().toString(),
                database,
                environment
        );
    }
}
