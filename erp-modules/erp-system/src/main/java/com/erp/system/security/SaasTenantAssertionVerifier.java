package com.erp.system.security;

import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Verifies the gateway-signed tenant context for public system endpoints.
 */
@Component
public class SaasTenantAssertionVerifier {
    private static final String INVALID_MESSAGE = "租户上下文无效或已过期";
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private final String signatureSecret;
    private final Clock clock;

    public SaasTenantAssertionVerifier(
            @Value("${erp.saas.tenant-assertion-signature-secret:}") String signatureSecret) {
        this(signatureSecret, Clock.systemUTC());
    }

    public SaasTenantAssertionVerifier(String signatureSecret, Clock clock) {
        this.signatureSecret = secret(signatureSecret);
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    public ResolvedTenantAssertion verify(HttpServletRequest request) {
        if (request == null) throw invalid();
        try {
            String tenantId = header(request, TenantAssertionHeaders.TENANT_ID, 20);
            if (!TENANT_ID.matcher(tenantId).matches()) throw invalid();
            String host = header(request, TenantAssertionHeaders.HOST, 300);
            String method = header(request, TenantAssertionHeaders.METHOD, 16);
            String path = header(request, TenantAssertionHeaders.PATH, 2048);
            long issuedAt = Long.parseLong(header(request, TenantAssertionHeaders.ISSUED_AT, 20));
            String nonce = header(request, TenantAssertionHeaders.NONCE, 128);
            String signature = header(request, TenantAssertionHeaders.SIGNATURE, 128);
            ResolvedTenantAssertion assertion = new ResolvedTenantAssertion(
                    tenantId, host, method, path, issuedAt, nonce);
            ResolvedTenantAssertion actualRequest = new ResolvedTenantAssertion(
                    tenantId, host, request.getMethod(), request.getRequestURI(), issuedAt, nonce);
            if (!assertion.getMethod().equals(actualRequest.getMethod())
                    || !assertion.getPath().equals(actualRequest.getPath())
                    || !ResolvedTenantAssertionSignatureUtils.verify(
                            signatureSecret, assertion, signature, clock.millis())) {
                throw invalid();
            }
            return assertion;
        } catch (IllegalArgumentException error) {
            if (INVALID_MESSAGE.equals(error.getMessage())) throw error;
            throw invalid();
        }
    }

    private static String header(HttpServletRequest request, String name, int maximumLength) {
        String value = request.getHeader(name);
        if (!StringUtils.hasText(value)) throw invalid();
        String normalized = value.trim();
        if (normalized.length() > maximumLength) throw invalid();
        return normalized;
    }

    private static String secret(String value) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException("erp.saas.tenant-assertion-signature-secret must not be blank");
        }
        String normalized = value.trim();
        if (normalized.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "erp.saas.tenant-assertion-signature-secret must contain at least 32 UTF-8 bytes");
        }
        return normalized;
    }

    private static IllegalArgumentException invalid() {
        return new IllegalArgumentException(INVALID_MESSAGE);
    }
}
