package com.erp.system.saas;

import com.erp.common.client.internal.InternalSaasClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasEntitlementSnapshotSignatureUtils;
import com.erp.saas.contract.model.SaasFeatureGrant;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasQuotaLimit;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.system.saas.impl.SaasRuntimeSnapshotServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasRuntimeSnapshotServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private InternalSaasClient client;
    private SaasLocalSnapshotStore store;
    private SaasRuntimeSnapshotService service;

    @BeforeEach
    void setUp() {
        client = mock(InternalSaasClient.class);
        store = mock(SaasLocalSnapshotStore.class);
        service = serviceAt(NOW);
        TenantContextHolder.setTenantId("tenant_1");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldVerifyRemoteSnapshotBeforeSavingAndAllowWritesWhileFreshActive() {
        SaasEntitlementSnapshot snapshot = snapshot(TenantLifecycleState.ACTIVE,
                NOW.minusSeconds(60), NOW.plus(Duration.ofHours(24)));
        when(client.loadEntitlementSnapshot("tenant_1")).thenReturn(snapshot);

        SaasRuntimeEntitlements result = service.refresh("tenant_1");

        verify(store).save(snapshot, "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(result.source()).isEqualTo(SaasRuntimeSource.REMOTE_REFRESH);
        assertThat(result.loginAllowed()).isTrue();
        assertThat(result.writeAllowed()).isTrue();
        assertThat(result.readOnly()).isFalse();
        assertThat(result.featureEnabled("orders.read")).isTrue();
        assertThat(result.quotaLimit(SaasQuotaKeys.STORAGE_BYTES)).isNull();
    }

    @Test
    void shouldUseExpiredLocalSnapshotAsReadOnlyAndStillPermitEligibleLogin() {
        SaasEntitlementSnapshot snapshot = snapshot(TenantLifecycleState.ACTIVE,
                NOW.minus(Duration.ofHours(25)), NOW);
        when(store.load("tenant_1")).thenReturn(snapshot);

        SaasRuntimeEntitlements result = service.current("tenant_1");

        assertThat(result.source()).isEqualTo(SaasRuntimeSource.EXPIRED_CACHE);
        assertThat(result.stale()).isTrue();
        assertThat(result.loginAllowed()).isTrue();
        assertThat(result.writeAllowed()).isFalse();
        assertThat(result.readOnly()).isTrue();
        assertThat(result.featureEnabled("orders.read")).isTrue();
    }

    @Test
    void shouldDenyLoginForSuspendedTenantAndFailClosedWithoutAnySnapshot() {
        when(store.load("tenant_1")).thenReturn(snapshot(TenantLifecycleState.SUSPENDED,
                NOW.minusSeconds(60), NOW.plus(Duration.ofHours(24))));
        assertThat(service.current("tenant_1").loginAllowed()).isFalse();

        when(store.load("tenant_1")).thenReturn(null);
        SaasRuntimeEntitlements missing = service.current("tenant_1");
        assertThat(missing.source()).isEqualTo(SaasRuntimeSource.MISSING);
        assertThat(missing.loginAllowed()).isFalse();
        assertThat(missing.writeAllowed()).isFalse();
    }

    @Test
    void shouldRejectTamperingWrongTenantUnknownKeyAndExpiredRemoteLease() {
        SaasEntitlementSnapshot tampered = snapshot(TenantLifecycleState.ACTIVE,
                NOW.minusSeconds(60), NOW.plus(Duration.ofHours(24)));
        tampered.setPlanCode("enterprise");
        when(client.loadEntitlementSnapshot("tenant_1")).thenReturn(tampered);
        assertCode(SaasRuntimeSnapshotException.ErrorCode.INVALID_SIGNATURE,
                () -> service.refresh("tenant_1"));

        SaasEntitlementSnapshot expired = snapshot(TenantLifecycleState.ACTIVE,
                NOW.minus(Duration.ofHours(25)), NOW);
        when(client.loadEntitlementSnapshot("tenant_1")).thenReturn(expired);
        assertCode(SaasRuntimeSnapshotException.ErrorCode.INVALID_LEASE,
                () -> service.refresh("tenant_1"));
    }

    @Test
    void shouldRejectWeakVerificationConfigurationAndFutureIssuedLease() {
        assertThatThrownBy(() -> serviceWith("primary", "short", NOW))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32");
        SaasEntitlementSnapshot future = snapshot(TenantLifecycleState.ACTIVE,
                NOW.plusSeconds(61), NOW.plus(Duration.ofHours(24)));
        when(client.loadEntitlementSnapshot("tenant_1")).thenReturn(future);
        assertCode(SaasRuntimeSnapshotException.ErrorCode.INVALID_LEASE,
                () -> service.refresh("tenant_1"));
    }

    private SaasRuntimeSnapshotService serviceAt(Instant instant) {
        return serviceWith("primary", SECRET, instant);
    }

    private SaasRuntimeSnapshotService serviceWith(String keyId, String secret, Instant instant) {
        SaasSnapshotVerificationProperties properties = new SaasSnapshotVerificationProperties();
        properties.setKeyId(keyId);
        properties.setSecret(secret);
        properties.setClockSkew(Duration.ofMinutes(1));
        return new SaasRuntimeSnapshotServiceImpl(client, store, properties, Clock.fixed(instant, ZoneOffset.UTC));
    }

    private static SaasEntitlementSnapshot snapshot(TenantLifecycleState lifecycle,
            Instant issuedAt, Instant expiresAt) {
        SaasEntitlementSnapshot snapshot = new SaasEntitlementSnapshot();
        snapshot.setTenantId("tenant_1");
        snapshot.setLifecycleState(lifecycle);
        snapshot.setDeploymentMode(DeploymentMode.SHARED);
        snapshot.setSubscriptionState(SubscriptionState.ACTIVE);
        snapshot.setPlanCode("starter");
        snapshot.setVersion(7L);
        snapshot.setIssuedAtEpochMs(issuedAt.toEpochMilli());
        snapshot.setExpiresAtEpochMs(expiresAt.toEpochMilli());
        snapshot.setFeatureGrants(List.of(new SaasFeatureGrant("orders.read", true)));
        snapshot.setQuotaLimits(List.of(
                new SaasQuotaLimit(SaasQuotaKeys.USER_COUNT, 10L),
                new SaasQuotaLimit(SaasQuotaKeys.STORAGE_BYTES, null)));
        snapshot.setSignatureKeyId("primary");
        snapshot.setSignature(SaasEntitlementSnapshotSignatureUtils.sign(
                SECRET.getBytes(StandardCharsets.UTF_8), snapshot));
        return snapshot;
    }

    private static void assertCode(SaasRuntimeSnapshotException.ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(SaasRuntimeSnapshotException.class)
                .extracting(error -> ((SaasRuntimeSnapshotException) error).getErrorCode()).isEqualTo(code);
    }
}
