package com.erp.saas.control.service.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaasDomainHostNormalizerTest {
    private final SaasDomainHostNormalizer normalizer = new SaasDomainHostNormalizer();

    @Test
    void shouldNormalizeCaseIdnTrailingDotAndValidPort() {
        assertThat(normalizer.normalize("  Customer.Example.COM.:443 "))
                .isEqualTo("customer.example.com");
        assertThat(normalizer.normalize("例子.测试"))
                .isEqualTo("xn--fsqu00a.xn--0zwm56d");
        assertThat(normalizer.normalize("例子。测试"))
                .isEqualTo("xn--fsqu00a.xn--0zwm56d");
    }

    @Test
    void shouldRejectUrlsCredentialsPathsWildcardsAndControls() {
        assertInvalid("https://tenant.example.com");
        assertInvalid("user@tenant.example.com");
        assertInvalid("tenant.example.com/path");
        assertInvalid("tenant.example.com?x=1");
        assertInvalid("tenant.example.com#fragment");
        assertInvalid("*.example.com");
        assertInvalid("tenant\n.example.com");
    }

    @Test
    void shouldRejectAmbiguousPortsIpLiteralsAndInvalidLabels() {
        assertInvalid("tenant.example.com:0");
        assertInvalid("tenant.example.com:65536");
        assertInvalid("tenant.example.com:not-a-port");
        assertInvalid("tenant.example.com::443");
        assertInvalid("127.0.0.1");
        assertInvalid("[::1]");
        assertInvalid("-tenant.example.com");
        assertInvalid("tenant..example.com");
        assertInvalid("tenant.example.com..");
    }

    @Test
    void shouldEnforceDnsLengthLimits() {
        assertInvalid("a".repeat(64) + ".example.com");
        String oversized = String.join(".", "a".repeat(63), "b".repeat(63),
                "c".repeat(63), "d".repeat(62), "com");
        assertInvalid(oversized);
    }

    private void assertInvalid(String value) {
        assertThatThrownBy(() -> normalizer.normalize(value))
                .isInstanceOf(SaasDomainException.class)
                .extracting(error -> ((SaasDomainException) error).getErrorCode())
                .isEqualTo(SaasDomainException.ErrorCode.INVALID_HOST);
    }
}
