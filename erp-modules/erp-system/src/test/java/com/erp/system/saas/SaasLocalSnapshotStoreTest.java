package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasEntitlementSnapshotSignatureUtils;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.system.domain.SysSaasEntitlementSnapshot;
import com.erp.system.mapper.SysSaasEntitlementSnapshotMapper;
import com.erp.system.saas.impl.SaasLocalSnapshotStoreImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasLocalSnapshotStoreTest {
    private static final Instant NOW = Instant.parse("2026-08-02T03:00:00Z");
    private static final byte[] SECRET = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private SysSaasEntitlementSnapshotMapper mapper;
    private SaasLocalSnapshotStore store;

    @BeforeEach
    void setUp() {
        mapper = mock(SysSaasEntitlementSnapshotMapper.class);
        store = new SaasLocalSnapshotStoreImpl(mapper, new ObjectMapper());
        TenantContextHolder.setTenantId("tenant_1");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldInsertAndDeserializeTenantBoundSnapshot() {
        when(mapper.findForUpdate("tenant_1")).thenReturn(null);
        when(mapper.insert(any())).thenReturn(1);
        SaasEntitlementSnapshot snapshot = snapshot(1L, TenantLifecycleState.ACTIVE);

        store.save(snapshot, "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));

        ArgumentCaptor<SysSaasEntitlementSnapshot> captor =
                ArgumentCaptor.forClass(SysSaasEntitlementSnapshot.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo("tenant_1");
        assertThat(captor.getValue().getSnapshotJson()).contains("\"signature\"");
        when(mapper.findByTenantId("tenant_1")).thenReturn(captor.getValue());
        assertThat(store.load("tenant_1").getVersion()).isEqualTo(1L);
    }

    @Test
    void shouldRejectRollbackAndVersionCollisionButAllowStrictIdempotence() {
        SaasEntitlementSnapshot existingSnapshot = snapshot(5L, TenantLifecycleState.ACTIVE);
        SysSaasEntitlementSnapshot existing = entity(existingSnapshot, 3L);
        when(mapper.findForUpdate("tenant_1")).thenReturn(existing);

        assertThatThrownBy(() -> store.save(snapshot(4L, TenantLifecycleState.ACTIVE),
                "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .isInstanceOf(SaasRuntimeSnapshotException.class)
                .extracting(error -> ((SaasRuntimeSnapshotException) error).getErrorCode())
                .isEqualTo(SaasRuntimeSnapshotException.ErrorCode.VERSION_ROLLBACK);

        store.save(existingSnapshot, "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        verify(mapper, never()).updateVersioned(any(), any(), any(), any());

        assertThatThrownBy(() -> store.save(snapshot(5L, TenantLifecycleState.READ_ONLY),
                "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC)))
                .isInstanceOf(SaasRuntimeSnapshotException.class)
                .extracting(error -> ((SaasRuntimeSnapshotException) error).getErrorCode())
                .isEqualTo(SaasRuntimeSnapshotException.ErrorCode.VERSION_COLLISION);
    }

    @Test
    void shouldUpdateOnlyNewerSnapshotWithCasAndRequireMatchingTenantContext() {
        SysSaasEntitlementSnapshot existing = entity(snapshot(5L, TenantLifecycleState.ACTIVE), 3L);
        when(mapper.findForUpdate("tenant_1")).thenReturn(existing);
        when(mapper.updateVersioned(any(), eq(3L), eq("saas-refresh"), any())).thenReturn(1);
        store.save(snapshot(6L, TenantLifecycleState.READ_ONLY),
                "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        verify(mapper).updateVersioned(any(), eq(3L), eq("saas-refresh"), any());

        TenantContextHolder.setTenantId("tenant_2");
        assertThatThrownBy(() -> store.load("tenant_1"))
                .isInstanceOf(SaasRuntimeSnapshotException.class)
                .extracting(error -> ((SaasRuntimeSnapshotException) error).getErrorCode())
                .isEqualTo(SaasRuntimeSnapshotException.ErrorCode.TENANT_CONTEXT_MISMATCH);
    }

    @Test
    void shouldRepairCorruptedPayloadWhenRemoteSnapshotHasSameVersionAndSignature() {
        SaasEntitlementSnapshot snapshot = snapshot(5L, TenantLifecycleState.ACTIVE);
        SysSaasEntitlementSnapshot existing = entity(snapshot, 3L);
        existing.setSnapshotJson("{corrupted");
        when(mapper.findForUpdate("tenant_1")).thenReturn(existing);
        when(mapper.updateVersioned(any(), eq(3L), eq("saas-refresh"), any())).thenReturn(1);

        store.save(snapshot, "saas-refresh", LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));

        ArgumentCaptor<SysSaasEntitlementSnapshot> captor =
                ArgumentCaptor.forClass(SysSaasEntitlementSnapshot.class);
        verify(mapper).updateVersioned(captor.capture(), eq(3L), eq("saas-refresh"), any());
        assertThat(captor.getValue().getSnapshotJson()).contains("\"tenantId\":\"tenant_1\"");
    }

    private static SaasEntitlementSnapshot snapshot(long version, TenantLifecycleState lifecycle) {
        SaasEntitlementSnapshot snapshot = new SaasEntitlementSnapshot();
        snapshot.setTenantId("tenant_1");
        snapshot.setLifecycleState(lifecycle);
        snapshot.setDeploymentMode(DeploymentMode.SHARED);
        snapshot.setSubscriptionState(SubscriptionState.ACTIVE);
        snapshot.setPlanCode("starter");
        snapshot.setVersion(version);
        snapshot.setIssuedAtEpochMs(NOW.toEpochMilli());
        snapshot.setExpiresAtEpochMs(NOW.plusSeconds(86400).toEpochMilli());
        snapshot.setSignatureKeyId("primary");
        snapshot.setSignature(SaasEntitlementSnapshotSignatureUtils.sign(SECRET, snapshot));
        return snapshot;
    }

    private static SysSaasEntitlementSnapshot entity(SaasEntitlementSnapshot snapshot, long rowVersion) {
        SysSaasEntitlementSnapshot row = new SysSaasEntitlementSnapshot();
        row.setTenantId(snapshot.getTenantId());
        row.setSnapshotVersion(snapshot.getVersion());
        try {
            row.setSnapshotJson(new ObjectMapper().writeValueAsString(snapshot));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        row.setIssuedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(snapshot.getIssuedAtEpochMs()), ZoneOffset.UTC));
        row.setExpiresAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(snapshot.getExpiresAtEpochMs()), ZoneOffset.UTC));
        row.setSignatureKeyId(snapshot.getSignatureKeyId());
        row.setSignature(snapshot.getSignature());
        row.setVersionNo(rowVersion);
        return row;
    }
}
