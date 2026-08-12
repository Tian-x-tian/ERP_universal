package com.erp.common.security;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Signs and verifies the gateway-resolved tenant assertion.
 */
public final class ResolvedTenantAssertionSignatureUtils {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String DOMAIN_VERSION = "ERP-SAAS-TENANT-ASSERTION-V1";
    private static final long FRESHNESS_WINDOW_MS = 30_000L;

    private ResolvedTenantAssertionSignatureUtils() {
    }

    public static String sign(String secret, ResolvedTenantAssertion assertion) {
        byte[] signature = calculate(secret, assertion);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
    }

    public static boolean verify(String secret, ResolvedTenantAssertion assertion, String signature, long nowEpochMs) {
        requireSecret(secret);
        if (assertion == null || signature == null
                || assertion.getIssuedAt() < nowEpochMs - FRESHNESS_WINDOW_MS
                || assertion.getIssuedAt() > nowEpochMs + FRESHNESS_WINDOW_MS) {
            return false;
        }
        try {
            byte[] expected = calculate(secret, assertion);
            byte[] supplied = Base64.getUrlDecoder().decode(signature);
            return MessageDigest.isEqual(expected, supplied);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    private static byte[] calculate(String secret, ResolvedTenantAssertion assertion) {
        requireSecret(secret);
        if (assertion == null) {
            throw new IllegalArgumentException("assertion must not be null");
        }
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return mac.doFinal(canonicalPayload(assertion).getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Failed to calculate tenant assertion signature", ex);
        }
    }

    private static void requireSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "erp.saas.tenant-assertion-signature-secret must not be blank");
        }
    }

    private static String canonicalPayload(ResolvedTenantAssertion assertion) {
        return DOMAIN_VERSION
                + "\n" + assertion.getTenantId()
                + "\n" + assertion.getHost()
                + "\n" + assertion.getMethod()
                + "\n" + assertion.getPath()
                + "\n" + assertion.getIssuedAt()
                + "\n" + assertion.getNonce();
    }
}
