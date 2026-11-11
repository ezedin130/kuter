package com.kuter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3.0 configuration providing interactive Swagger UI and REST API documentation.
 */
@Configuration
public class OpenApiConfig {

    public static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI kuterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Kuter Multi-Tenant Building Materials SaaS API")
                        .description("Production REST API for multi-tenant building materials inventory management, dual-UOM conversions, moving weighted-average costing, counter checkout, subscriptions, and platform administration.")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Kuter Engineering")
                                .email("engineering@kuterplatform.com"))
                        .license(new License()
                                .name("Proprietary")))
                .servers(List.of(
                        new Server().url("/").description("Current Server (Relative)"),
                        new Server().url("http://localhost:8080").description("Local Development Server")))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME,
                                new SecurityScheme()
                                        .name(SECURITY_SCHEME_NAME)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("Enter JWT Bearer token issued by /api/v1/auth/login")));
    }
}
