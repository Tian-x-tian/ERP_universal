package com.erp.auth.security;

import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ResolvedTenantAssertionVerifierTest {
    private static final String SECRET = "gateway-tenant-assertion-secret-32-bytes";
    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private ResolvedTenantAssertionVerifier verifier;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        verifier = new ResolvedTenantAssertionVerifier(redisTemplate, SECRET,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldVerifyFreshGatewayAssertionAndConsumeNonceOnce() {
        ResolvedTenantAssertion assertion = assertion(NOW.toEpochMilli());

        ResolvedTenantAssertion verified = verifier.verify(request(assertion));

        assertThat(verified.getTenantId()).isEqualTo("tenant-a");
        assertThat(verified.getHost()).isEqualTo("acme.example");
    }

    @Test
    void shouldRejectTamperedMethodExpiredAssertionAndReplay() {
        ResolvedTenantAssertion assertion = assertion(NOW.toEpochMilli());
        MockHttpServletRequest tamperedMethod = request(assertion);
        tamperedMethod.setMethod("GET");
        assertThatThrownBy(() -> verifier.verify(tamperedMethod))
                .isInstanceOf(ResolvedTenantAssertionException.class);

        ResolvedTenantAssertion expired = assertion(NOW.minusSeconds(31).toEpochMilli());
        assertThatThrownBy(() -> verifier.verify(request(expired)))
                .isInstanceOf(ResolvedTenantAssertionException.class);

        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        assertThatThrownBy(() -> verifier.verify(request(assertion)))
                .isInstanceOf(ResolvedTenantAssertionException.class);
    }

    @Test
    void shouldFailStartupForWeakTenantAssertionSecret() {
        assertThatThrownBy(() -> new ResolvedTenantAssertionVerifier(redisTemplate, "short",
                Clock.fixed(NOW, ZoneOffset.UTC)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    private ResolvedTenantAssertion assertion(long issuedAt) {
        return new ResolvedTenantAssertion("tenant-a", "acme.example", "POST", "/login",
                issuedAt, "nonce-123");
    }

    private MockHttpServletRequest request(ResolvedTenantAssertion assertion) {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/login");
        request.addHeader(TenantAssertionHeaders.TENANT_ID, assertion.getTenantId());
        request.addHeader(TenantAssertionHeaders.HOST, assertion.getHost());
        request.addHeader(TenantAssertionHeaders.METHOD, assertion.getMethod());
        request.addHeader(TenantAssertionHeaders.PATH, assertion.getPath());
        request.addHeader(TenantAssertionHeaders.ISSUED_AT, String.valueOf(assertion.getIssuedAt()));
        request.addHeader(TenantAssertionHeaders.NONCE, assertion.getNonce());
        request.addHeader(TenantAssertionHeaders.SIGNATURE,
                ResolvedTenantAssertionSignatureUtils.sign(SECRET, assertion));
        return request;
    }
}
