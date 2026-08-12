package com.erp.system.security;

import com.erp.common.security.ResolvedTenantAssertion;
import com.erp.common.security.ResolvedTenantAssertionSignatureUtils;
import com.erp.common.security.TenantAssertionHeaders;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.support.TestPropertySourceUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaasTenantAssertionVerifierTest {
    private static final String SECRET = "gateway-tenant-assertion-secret-32-bytes";
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");

    @Test
    void shouldCreateVerifierThroughSpringConstructorInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(context,
                    "erp.saas.tenant-assertion-signature-secret=" + SECRET);
            context.register(SaasTenantAssertionVerifier.class);

            context.refresh();

            assertThat(context.getBean(SaasTenantAssertionVerifier.class)).isNotNull();
        }
    }

    @Test
    void shouldVerifyGatewayAssertionBoundToActivationRequest() {
        SaasTenantAssertionVerifier verifier = new SaasTenantAssertionVerifier(
                SECRET, Clock.fixed(NOW, ZoneOffset.UTC));
        ResolvedTenantAssertion assertion = assertion("POST", "/system/public/saas/activation",
                NOW.toEpochMilli());

        assertThat(verifier.verify(request(assertion)).getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void shouldRejectExpiredOrPathMismatchedAssertion() {
        SaasTenantAssertionVerifier verifier = new SaasTenantAssertionVerifier(
                SECRET, Clock.fixed(NOW, ZoneOffset.UTC));
        ResolvedTenantAssertion expired = assertion("POST", "/system/public/saas/activation",
                NOW.minusSeconds(31).toEpochMilli());
        assertThatThrownBy(() -> verifier.verify(request(expired)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("租户上下文无效或已过期");

        ResolvedTenantAssertion mismatched = assertion("POST", "/login", NOW.toEpochMilli());
        MockHttpServletRequest request = request(mismatched);
        request.setRequestURI("/system/public/saas/activation");
        assertThatThrownBy(() -> verifier.verify(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("租户上下文无效或已过期");
    }

    private ResolvedTenantAssertion assertion(String method, String path, long issuedAt) {
        return new ResolvedTenantAssertion("tenant-a", "acme.example", method, path,
                issuedAt, "nonce-1");
    }

    private MockHttpServletRequest request(ResolvedTenantAssertion assertion) {
        MockHttpServletRequest request = new MockHttpServletRequest(assertion.getMethod(), assertion.getPath());
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
