package com.kuter.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kuter.common.Constants;
import com.kuter.common.ErrorResponse;
import com.kuter.identity.KuterUserPrincipal;
import com.kuter.tenancy.Tenant;
import com.kuter.tenancy.TenantContext;
import com.kuter.tenancy.TenantRepository;
import com.kuter.tenancy.TenantStatus;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Filter that securely resolves and enforces tenant context.
 * Validates requested X-Tenant-ID header against the authenticated user's authorized memberships.
 * Enforces tenant lifecycle status: rejects requests for SUSPENDED tenants.
 * Never trusts a client-supplied tenant ID without cryptographic/membership verification.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final ObjectMapper objectMapper;
    private final TenantRepository tenantRepository;

    public TenantContextFilter(ObjectMapper objectMapper, TenantRepository tenantRepository) {
        this.objectMapper = objectMapper;
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String tenantHeaderValue = request.getHeader(Constants.TENANT_ID_HEADER);

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();

            if (StringUtils.hasText(tenantHeaderValue)) {
                UUID requestedTenantId;
                try {
                    requestedTenantId = UUID.fromString(tenantHeaderValue.trim());
                } catch (IllegalArgumentException ex) {
                    writeForbiddenError(request, response, "Invalid X-Tenant-ID header format. Must be a valid UUID.");
                    return;
                }

                if (auth != null && auth.getPrincipal() instanceof KuterUserPrincipal principal) {
                    // Enforce membership verification: user must be an active member of the tenant.
                    // Platform administration operations must not automatically grant access to tenant data.
                    if (!principal.hasMembershipIn(requestedTenantId)) {
                        log.warn("Access denied: User {} attempted to access unauthorized tenant {} [CID: {}]",
                                principal.getEmail(), requestedTenantId, MDC.get(Constants.CORRELATION_ID_MDC_KEY));
                        writeForbiddenError(request, response,
                                "Access denied: you do not hold an active membership in the requested tenant shop");
                        return;
                    }

                    // Enforce tenant lifecycle status: suspended tenants cannot be accessed
                    Tenant tenant = tenantRepository.findById(requestedTenantId).orElse(null);
                    if (tenant != null && tenant.getStatus() == TenantStatus.SUSPENDED) {
                        log.warn("Access denied: Tenant shop {} is suspended [CID: {}]",
                                requestedTenantId, MDC.get(Constants.CORRELATION_ID_MDC_KEY));
                        writeForbiddenError(request, response,
                                "Tenant shop is suspended. Please contact platform administration.");
                        return;
                    }

                    principal.setCurrentTenantId(requestedTenantId);
                    TenantContext.setTenantId(requestedTenantId);
                }
            } else {
                // If header is omitted, check if authenticated user belongs to exactly one tenant
                if (auth != null && auth.getPrincipal() instanceof KuterUserPrincipal principal) {
                    if (principal.getAuthorizedTenantIds().size() == 1) {
                        UUID singleTenantId = principal.getAuthorizedTenantIds().iterator().next();

                        // Enforce tenant status check for auto-activated single-tenant context
                        Tenant tenant = tenantRepository.findById(singleTenantId).orElse(null);
                        if (tenant != null && tenant.getStatus() == TenantStatus.SUSPENDED) {
                            log.warn("Access denied: Auto-activated single tenant shop {} is suspended [CID: {}]",
                                    singleTenantId, MDC.get(Constants.CORRELATION_ID_MDC_KEY));
                            writeForbiddenError(request, response,
                                    "Tenant shop is suspended. Please contact platform administration.");
                            return;
                        }

                        principal.setCurrentTenantId(singleTenantId);
                        TenantContext.setTenantId(singleTenantId);
                    }
                }
            }

            filterChain.doFilter(request, response);
        } finally {
            // Guarantee ThreadLocal cleanup to prevent connection pool leakage
            TenantContext.clear();
        }
    }

    private void writeForbiddenError(HttpServletRequest request,
                                     HttpServletResponse response,
                                     String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        String correlationId = MDC.get(Constants.CORRELATION_ID_MDC_KEY);
        ErrorResponse error = ErrorResponse.builder()
                .status(HttpStatus.FORBIDDEN.value())
                .error(HttpStatus.FORBIDDEN.getReasonPhrase())
                .message(message)
                .path(request.getRequestURI())
                .correlationId(correlationId != null ? correlationId : "none")
                .build();

        objectMapper.writeValue(response.getWriter(), error);
    }
}
