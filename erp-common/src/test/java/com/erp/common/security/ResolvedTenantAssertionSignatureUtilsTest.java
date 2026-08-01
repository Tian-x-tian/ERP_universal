package com.erp.common.security;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

class ResolvedTenantAssertionSignatureUtilsTest {
    private static final String SECRET = "a-separate-tenant-assertion-secret";
    private static final long NOW = 1_722_470_400_000L;

    @Test
    void shouldSignAndVerifyResolvedTenantAssertion() {
        ResolvedTenantAssertion assertion = assertion(NOW);

        String signature = ResolvedTenantAssertionSignatureUtils.sign(SECRET, assertion);

        Assertions.assertTrue(ResolvedTenantAssertionSignatureUtils.verify(SECRET, assertion, signature, NOW));
        Assertions.assertFalse(signature.contains("="));
        Assertions.assertEquals(List.of(
                "X-Resolved-Tenant-Id",
                "X-Resolved-Tenant-Host",
                "X-Resolved-Tenant-Method",
                "X-Resolved-Tenant-Path",
                "X-Resolved-Tenant-Issued-At",
                "X-Resolved-Tenant-Nonce",
                "X-Resolved-Tenant-Signature"), TenantAssertionHeaders.INTERNAL_HEADERS);
        Assertions.assertThrows(UnsupportedOperationException.class,
                () -> TenantAssertionHeaders.INTERNAL_HEADERS.add("X-Other"));
    }

    @Test
    void shouldRejectModifiedHost() {
        ResolvedTenantAssertion assertion = assertion(NOW);
        String signature = ResolvedTenantAssertionSignatureUtils.sign(SECRET, assertion);
        ResolvedTenantAssertion modified = new ResolvedTenantAssertion("tenant-a", "other.example.com", "GET",
                "/auth/login", NOW, "nonce-a");

        Assertions.assertFalse(ResolvedTenantAssertionSignatureUtils.verify(SECRET, modified, signature, NOW));
    }

    @Test
    void shouldRejectExpiredAssertion() {
        ResolvedTenantAssertion assertion = assertion(NOW - 30_001L);
        String signature = ResolvedTenantAssertionSignatureUtils.sign(SECRET, assertion);

        Assertions.assertFalse(ResolvedTenantAssertionSignatureUtils.verify(SECRET, assertion, signature, NOW));
        ResolvedTenantAssertion boundary = assertion(NOW - 30_000L);
        Assertions.assertTrue(ResolvedTenantAssertionSignatureUtils.verify(SECRET, boundary,
                ResolvedTenantAssertionSignatureUtils.sign(SECRET, boundary), NOW));
    }

    @Test
    void shouldRejectFutureAssertion() {
        ResolvedTenantAssertion assertion = assertion(NOW + 30_001L);
        String signature = ResolvedTenantAssertionSignatureUtils.sign(SECRET, assertion);

        Assertions.assertFalse(ResolvedTenantAssertionSignatureUtils.verify(SECRET, assertion, signature, NOW));
        ResolvedTenantAssertion boundary = assertion(NOW + 30_000L);
        Assertions.assertTrue(ResolvedTenantAssertionSignatureUtils.verify(SECRET, boundary,
                ResolvedTenantAssertionSignatureUtils.sign(SECRET, boundary), NOW));
    }

    @Test
    void shouldRejectMalformedSignature() {
        Assertions.assertFalse(ResolvedTenantAssertionSignatureUtils.verify(SECRET, assertion(NOW), "%%%", NOW));
        Assertions.assertFalse(ResolvedTenantAssertionSignatureUtils.verify(SECRET, assertion(NOW), null, NOW));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ResolvedTenantAssertionSignatureUtils.sign("  ", assertion(NOW)));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> ResolvedTenantAssertionSignatureUtils.verify(null, assertion(NOW), "ignored", NOW));
    }

    @Test
    void shouldRejectCrLfInSignedFields() {
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResolvedTenantAssertion("tenant-a\nforged", "example.com", "GET", "/auth/login",
                        NOW, "nonce-a"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResolvedTenantAssertion("tenant-a", "example.com", "GET", "/auth/login",
                        NOW, "nonce\rforged"));
    }

    @Test
    void shouldNormalizeHostMethodAndPath() {
        ResolvedTenantAssertion assertion = new ResolvedTenantAssertion(" tenant-a ", " EXAMPLE.COM.:8443 ",
                " post ", "/auth/../auth//login/%2F", NOW, " nonce-a ");

        Assertions.assertEquals("tenant-a", assertion.getTenantId());
        Assertions.assertEquals("example.com", assertion.getHost());
        Assertions.assertEquals("POST", assertion.getMethod());
        Assertions.assertEquals("/auth//login/%2F", assertion.getPath());
        Assertions.assertEquals("nonce-a", assertion.getNonce());
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResolvedTenantAssertion("tenant-a", "https://example.com/path", "GET", "/", NOW, "n"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResolvedTenantAssertion("tenant-a", "example.com:bad", "GET", "/", NOW, "n"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResolvedTenantAssertion("tenant-a", "example.com:65536", "GET", "/", NOW, "n"));
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> new ResolvedTenantAssertion("tenant-a", "example.com", "GET", "/path?q=1", NOW, "n"));
    }

    private ResolvedTenantAssertion assertion(long issuedAt) {
        return new ResolvedTenantAssertion("tenant-a", "example.com", "GET", "/auth/login", issuedAt, "nonce-a");
    }
}
