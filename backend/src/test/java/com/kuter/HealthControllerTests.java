package com.kuter;

import com.kuter.common.Constants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class HealthControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/health returns 200 OK with UP status and database connectivity")
    void healthEndpointReturnsSuccess() throws Exception {
        mockMvc.perform(get(Constants.HEALTH_ENDPOINT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().exists(Constants.CORRELATION_ID_HEADER))
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("health check successful")))
                .andExpect(jsonPath("$.data.status", is("UP")))
                .andExpect(jsonPath("$.data.database", is("CONNECTED")))
                .andExpect(jsonPath("$.data.application", notNullValue()))
                .andExpect(jsonPath("$.correlationId", notNullValue()))
                .andExpect(jsonPath("$.timestamp", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/health echoes provided X-Correlation-ID")
    void healthEndpointEchoesCorrelationId() throws Exception {
        String customCorrelationId = "test-corr-id-998877";

        mockMvc.perform(get(Constants.HEALTH_ENDPOINT)
                        .header(Constants.CORRELATION_ID_HEADER, customCorrelationId)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(header().string(Constants.CORRELATION_ID_HEADER, customCorrelationId))
                .andExpect(jsonPath("$.correlationId", is(customCorrelationId)));
    }

    @Test
    @DisplayName("GET /api/v1/info returns application information metadata")
    void infoEndpointReturnsSuccess() throws Exception {
        mockMvc.perform(get(Constants.INFO_ENDPOINT)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.application", notNullValue()))
                .andExpect(jsonPath("$.data.version", notNullValue()))
                .andExpect(jsonPath("$.data.profiles", notNullValue()));
    }
}
