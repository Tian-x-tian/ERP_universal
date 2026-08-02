package com.erp.auth.security;

import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Verifies the short-lived tenant context asserted by the gateway and rejects replayed nonces.
 */
@Component
public class ResolvedTenantAssertionVerifier {
    private static final String INVALID_MESSAGE = "租户上下文无效或已过期";
    private static final String NONCE_KEY_PREFIX = "erp:auth:tenant-assertion:nonce:";
    private static final Duration NONCE_TTL = Duration.ofSeconds(60);
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private final StringRedisTemplate redisTemplate;
    private final String signatureSecret;
    private final Clock clock;

    @Autowired
    public ResolvedTenantAssertionVerifier(StringRedisTemplate redisTemplate,
            @Value("${erp.saas.tenant-assertion-signature-secret:}") String signatureSecret) {
        this(redisTemplate, signatureSecret, Clock.systemUTC());
    }

    ResolvedTenantAssertionVerifier(StringRedisTemplate redisTemplate, String signatureSecret, Clock clock) {
        this.redisTemplate = Objects.requireNonNull(redisTemplate, "redisTemplate must not be null");
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
            String issuedAtValue = header(request, TenantAssertionHeaders.ISSUED_AT, 20);
            String nonce = header(request, TenantAssertionHeaders.NONCE, 128);
            String signature = header(request, TenantAssertionHeaders.SIGNATURE, 128);
            long issuedAt = Long.parseLong(issuedAtValue);
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
            Boolean firstUse = redisTemplate.opsForValue().setIfAbsent(
                    nonceKey(assertion), "1", NONCE_TTL);
            if (!Boolean.TRUE.equals(firstUse)) throw invalid();
            return assertion;
        } catch (ResolvedTenantAssertionException error) {
            throw error;
        } catch (RuntimeException error) {
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

    private static String nonceKey(ResolvedTenantAssertion assertion) {
        String payload = assertion.getTenantId() + "\n" + assertion.getNonce();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(payload.getBytes(StandardCharsets.UTF_8));
            return NONCE_KEY_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
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

    private static ResolvedTenantAssertionException invalid() {
        return new ResolvedTenantAssertionException(INVALID_MESSAGE);
    }
}
