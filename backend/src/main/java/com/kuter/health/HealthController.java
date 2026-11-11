package com.kuter.health;

import com.kuter.common.ApiResponse;
import com.kuter.common.Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST controller providing health check and application information endpoints.
 */
@RestController
@RequestMapping(Constants.API_BASE_PATH)
public class HealthController {

    private final HealthService healthService;
    private final Environment environment;
    private final String appName;
    private final String appVersion;

    public HealthController(HealthService healthService,
                            Environment environment,
                            @Value("${app.name:Kuter Inventory SaaS}") String appName,
                            @Value("${app.version:0.0.1-SNAPSHOT}") String appVersion) {
        this.healthService = healthService;
        this.environment = environment;
        this.appName = appName;
        this.appVersion = appVersion;
    }

    /**
     * Endpoint: GET /api/v1/health
     * Returns application health status and database connectivity.
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<HealthResponse>> getHealth() {
        HealthResponse response = healthService.getHealthStatus();
        return ResponseEntity.ok(ApiResponse.ok("System health check successful", response));
    }

    /**
     * Endpoint: GET /api/v1/info
     * Returns application metadata and active environment.
     */
    @GetMapping("/info")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInfo() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("application", appName);
        info.put("version", appVersion);
        info.put("timestamp", Instant.now().toString());

        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        info.put("profiles", profiles);

        return ResponseEntity.ok(ApiResponse.ok("Application information", info));
    }
}
