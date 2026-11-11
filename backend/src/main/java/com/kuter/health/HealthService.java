package com.kuter.health;

/**
 * Service interface for retrieving system health status.
 */
public interface HealthService {

    HealthResponse getHealthStatus();
}
