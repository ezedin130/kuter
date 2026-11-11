package com.kuter.health;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Health service implementation verifying database readiness.
 */
@Service
public class HealthServiceImpl implements HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthServiceImpl.class);
    private static final String INITIAL_STATUS_KEY = "DATABASE_INITIALIZED";

    private final SystemStatusRepository systemStatusRepository;
    private final Environment environment;
    private final String appName;

    public HealthServiceImpl(SystemStatusRepository systemStatusRepository,
                             Environment environment,
                             @Value("${app.name:Kuter Inventory SaaS}") String appName) {
        this.systemStatusRepository = systemStatusRepository;
        this.environment = environment;
        this.appName = appName;
    }

    @Override
    @Transactional(readOnly = true)
    public HealthResponse getHealthStatus() {
        String dbStatus = checkDatabaseConnectivity();
        String activeProfile = getActiveProfile();

        log.debug("Health check executed: dbStatus={}, profile={}", dbStatus, activeProfile);
        return HealthResponse.of(appName, dbStatus, activeProfile);
    }

    private String checkDatabaseConnectivity() {
        try {
            boolean initialized = systemStatusRepository.findByStatusName(INITIAL_STATUS_KEY).isPresent();
            return initialized ? "CONNECTED" : "CONNECTED_UNINITIALIZED";
        } catch (Exception ex) {
            log.warn("Database connectivity check failed: {}", ex.getMessage());
            return "UNAVAILABLE";
        }
    }

    private String getActiveProfile() {
        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        return profiles.length > 0 ? String.join(", ", profiles) : "default";
    }
}
