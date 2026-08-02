package com.erp.saas.contract.model;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

public final class SaasEntitlementSnapshotSignatureUtils {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private SaasEntitlementSnapshotSignatureUtils() { }

    public static String sign(byte[] secret, SaasEntitlementSnapshot snapshot) {
        requireSecret(secret);
        validateLease(snapshot);
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret, HMAC_ALGORITHM));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(signedBytes(snapshot)));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("HMAC-SHA256 is unavailable", exception);
        }
    }

    public static boolean verify(byte[] secret, SaasEntitlementSnapshot snapshot) {
        try {
            if (snapshot == null || blank(snapshot.getSignature())) {
                return false;
            }
            byte[] actual = Base64.getUrlDecoder().decode(snapshot.getSignature());
            byte[] expected = Base64.getUrlDecoder().decode(sign(secret, snapshot));
            return MessageDigest.isEqual(expected, actual);
        } catch (RuntimeException exception) {
            return false;
        }
    }

    public static String contentDigest(SaasEntitlementSnapshot snapshot) {
        byte[] digest;
        try {
            digest = MessageDigest.getInstance("SHA-256").digest(contentBytes(snapshot));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
        StringBuilder hex = new StringBuilder(digest.length * 2);
        for (byte value : digest) {
            hex.append(Character.forDigit((value >>> 4) & 0x0f, 16));
            hex.append(Character.forDigit(value & 0x0f, 16));
        }
        return hex.toString();
    }

    private static byte[] signedBytes(SaasEntitlementSnapshot snapshot) {
        return write(out -> {
            out.writeInt(1);
            out.writeLong(snapshot.getVersion());
            out.writeLong(snapshot.getIssuedAtEpochMs());
            out.writeLong(snapshot.getExpiresAtEpochMs());
            writeText(out, snapshot.getSignatureKeyId());
            byte[] content = contentBytes(snapshot);
            out.writeInt(content.length);
            out.write(content);
        });
    }

    private static byte[] contentBytes(SaasEntitlementSnapshot snapshot) {
        LogicalContent content = logicalContent(snapshot);
        return write(out -> {
            out.writeInt(1);
            writeText(out, content.tenantId());
            writeText(out, content.lifecycleState());
            writeText(out, content.deploymentMode());
            writeNullableText(out, content.subscriptionState());
            writeNullableText(out, content.planCode());
            out.writeInt(content.features().size());
            for (Map.Entry<String, Boolean> entry : content.features().entrySet()) {
                writeText(out, entry.getKey());
                out.writeBoolean(entry.getValue());
            }
            out.writeInt(content.quotas().size());
            for (Map.Entry<String, Long> entry : content.quotas().entrySet()) {
                writeText(out, entry.getKey());
                out.writeBoolean(entry.getValue() != null);
                if (entry.getValue() != null) out.writeLong(entry.getValue());
            }
        });
    }

    private static LogicalContent logicalContent(SaasEntitlementSnapshot snapshot) {
        if (snapshot == null) throw invalid("snapshot must not be null");
        String tenantId = text(snapshot.getTenantId(), "tenantId", 20);
        if (!TENANT_ID.matcher(tenantId).matches()) throw invalid("tenantId has an invalid format");
        if (snapshot.getLifecycleState() == null) throw invalid("lifecycleState must not be null");
        if (snapshot.getDeploymentMode() == null) throw invalid("deploymentMode must not be null");
        String planCode = optional(snapshot.getPlanCode(), "planCode", 64);
        TreeMap<String, Boolean> features = new TreeMap<>();
        List<SaasFeatureGrant> featureGrants = snapshot.getFeatureGrants();
        if (featureGrants == null) throw invalid("featureGrants must not be null");
        for (SaasFeatureGrant grant : featureGrants) {
            if (grant == null) throw invalid("feature grant must not be null");
            String key = text(grant.getFeatureKey(), "featureKey", 128);
            if (features.putIfAbsent(key, grant.isGranted()) != null) {
                throw invalid("duplicate featureKey: " + key);
            }
        }
        TreeMap<String, Long> quotas = new TreeMap<>();
        List<SaasQuotaLimit> quotaLimits = snapshot.getQuotaLimits();
        if (quotaLimits == null) throw invalid("quotaLimits must not be null");
        for (SaasQuotaLimit quota : quotaLimits) {
            if (quota == null) throw invalid("quota limit must not be null");
            String key = text(quota.getQuotaKey(), "quotaKey", 64);
            if (quota.getLimit() != null && quota.getLimit() < 0) {
                throw invalid("quota limit must not be negative");
            }
            if (quotas.containsKey(key)) throw invalid("duplicate quotaKey: " + key);
            quotas.put(key, quota.getLimit());
        }
        return new LogicalContent(tenantId, snapshot.getLifecycleState().name(),
                snapshot.getDeploymentMode().name(),
                snapshot.getSubscriptionState() == null ? null : snapshot.getSubscriptionState().name(),
                planCode, features, quotas);
    }

    private static void validateLease(SaasEntitlementSnapshot snapshot) {
        logicalContent(snapshot);
        if (snapshot.getVersion() <= 0) throw invalid("version must be positive");
        if (snapshot.getIssuedAtEpochMs() <= 0
                || snapshot.getExpiresAtEpochMs() <= snapshot.getIssuedAtEpochMs()) {
            throw invalid("snapshot lease timestamps are invalid");
        }
        text(snapshot.getSignatureKeyId(), "signatureKeyId", 64);
    }

    private static void requireSecret(byte[] secret) {
        if (secret == null || secret.length == 0) throw invalid("secret must not be empty");
    }

    private static byte[] write(IoWriter writer) {
        try {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(buffer)) {
                writer.write(out);
            }
            return buffer.toByteArray();
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Unable to canonicalize entitlement snapshot", exception);
        }
    }

    private static void writeNullableText(DataOutputStream out, String value) throws java.io.IOException {
        out.writeBoolean(value != null);
        if (value != null) writeText(out, value);
    }

    private static void writeText(DataOutputStream out, String value) throws java.io.IOException {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        out.writeInt(bytes.length);
        out.write(bytes);
    }

    private static String text(String value, String field, int maxLength) {
        if (blank(value)) throw invalid(field + " must not be blank");
        String normalized = value.trim();
        if (normalized.length() > maxLength) throw invalid(field + " is too long");
        return normalized;
    }

    private static String optional(String value, String field, int maxLength) {
        return blank(value) ? null : text(value, field, maxLength);
    }

    private static boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static IllegalArgumentException invalid(String message) {
        return new IllegalArgumentException(message);
    }

    private record LogicalContent(String tenantId, String lifecycleState, String deploymentMode,
            String subscriptionState, String planCode, TreeMap<String, Boolean> features,
            TreeMap<String, Long> quotas) { }

    @FunctionalInterface
    private interface IoWriter {
        void write(DataOutputStream out) throws java.io.IOException;
    }
}
