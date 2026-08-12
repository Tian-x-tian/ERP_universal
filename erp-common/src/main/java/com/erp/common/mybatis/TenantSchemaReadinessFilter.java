package com.erp.common.mybatis;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Blocks schema-dependent traffic until startup tenant schema validation succeeds.
 */
public class TenantSchemaReadinessFilter extends OncePerRequestFilter implements Ordered {
    private static final String LIVENESS_PATH = "/actuator/health/liveness";

    private final TenantSchemaReadinessGate readinessGate;

    /**
     * Creates the readiness filter.
     *
     * @param readinessGate tenant schema readiness gate
     */
    public TenantSchemaReadinessFilter(TenantSchemaReadinessGate readinessGate) {
        this.readinessGate = readinessGate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (readinessGate.isOpen() || isLivenessRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }
        response.sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
                "Tenant schema validation has not completed.");
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }

    private boolean isLivenessRequest(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isEmpty() && requestUri.startsWith(contextPath)) {
            requestUri = requestUri.substring(contextPath.length());
        }
        return LIVENESS_PATH.equals(requestUri);
    }
}
