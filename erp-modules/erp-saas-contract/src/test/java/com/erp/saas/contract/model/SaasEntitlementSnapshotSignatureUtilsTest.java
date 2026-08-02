package com.erp.saas.contract.model;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaasEntitlementSnapshotSignatureUtilsTest {
    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);

    @Test
    void shouldSignEverySecurityRelevantFieldAndVerifyInConstantFormat() {
        SaasEntitlementSnapshot snapshot = snapshot();
        snapshot.setSignature(SaasEntitlementSnapshotSignatureUtils.sign(SECRET, snapshot));

        assertTrue(SaasEntitlementSnapshotSignatureUtils.verify(SECRET, snapshot));
        assertFalse(snapshot.getSignature().contains("="));

        snapshot.setLifecycleState(TenantLifecycleState.READ_ONLY);
        assertFalse(SaasEntitlementSnapshotSignatureUtils.verify(SECRET, snapshot));
    }

    @Test
    void shouldCanonicalizeGrantOrderButRejectDuplicateKeys() {
        SaasEntitlementSnapshot snapshot = snapshot();
        snapshot.setSignature(SaasEntitlementSnapshotSignatureUtils.sign(SECRET, snapshot));
        snapshot.setFeatureGrants(List.of(
                new SaasFeatureGrant("reports.export", false),
                new SaasFeatureGrant("orders.read", true)));
        assertTrue(SaasEntitlementSnapshotSignatureUtils.verify(SECRET, snapshot));

        snapshot.setFeatureGrants(List.of(
                new SaasFeatureGrant("orders.read", true),
                new SaasFeatureGrant("orders.read", false)));
        assertThrows(IllegalArgumentException.class,
                () -> SaasEntitlementSnapshotSignatureUtils.sign(SECRET, snapshot));
    }

    @Test
    void shouldProduceStableLogicalContentDigestIndependentOfLeaseMetadata() {
        SaasEntitlementSnapshot first = snapshot();
        SaasEntitlementSnapshot second = snapshot();
        second.setVersion(99L);
        second.setIssuedAtEpochMs(99L);
        second.setExpiresAtEpochMs(100L);
        second.setSignatureKeyId("rotated");

        assertTrue(SaasEntitlementSnapshotSignatureUtils.contentDigest(first)
                .equals(SaasEntitlementSnapshotSignatureUtils.contentDigest(second)));
        second.setPlanCode("enterprise");
        assertFalse(SaasEntitlementSnapshotSignatureUtils.contentDigest(first)
                .equals(SaasEntitlementSnapshotSignatureUtils.contentDigest(second)));
    }

    private static SaasEntitlementSnapshot snapshot() {
        SaasEntitlementSnapshot snapshot = new SaasEntitlementSnapshot();
        snapshot.setTenantId("tenant_1");
        snapshot.setLifecycleState(TenantLifecycleState.ACTIVE);
        snapshot.setDeploymentMode(DeploymentMode.SHARED);
        snapshot.setSubscriptionState(SubscriptionState.ACTIVE);
        snapshot.setPlanCode("starter");
        snapshot.setVersion(7L);
        snapshot.setIssuedAtEpochMs(1000L);
        snapshot.setExpiresAtEpochMs(2000L);
        snapshot.setFeatureGrants(List.of(
                new SaasFeatureGrant("orders.read", true),
                new SaasFeatureGrant("reports.export", false)));
        snapshot.setQuotaLimits(List.of(
                new SaasQuotaLimit(SaasQuotaKeys.USER_COUNT, 10L),
                new SaasQuotaLimit(SaasQuotaKeys.STORAGE_BYTES, null)));
        snapshot.setSignatureKeyId("primary");
        return snapshot;
    }
}
