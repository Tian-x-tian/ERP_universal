package com.erp.saas.control.service.snapshot;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasEntitlementSnapshotSignatureUtils;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasEntitlementSnapshotEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasEntitlementSnapshotMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.SaasTenantEntitlementService;
import com.erp.saas.control.service.model.EffectiveTenantEntitlements;
import com.erp.saas.control.service.model.QuotaEntitlement;
import com.erp.saas.control.service.snapshot.impl.SaasEntitlementSnapshotServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasEntitlementSnapshotServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-08-02T02:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private static final String SECRET = "0123456789abcdef0123456789abcdef";
    private SaasTenantMapper tenantMapper;
    private SaasDeploymentMapper deploymentMapper;
    private SaasSubscriptionMapper subscriptionMapper;
    private SaasPlanMapper planMapper;
    private SaasEntitlementSnapshotMapper snapshotMapper;
    private SaasTenantEntitlementService entitlementService;
    private SaasEntitlementSnapshotService service;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SaasTenantMapper.class);
        deploymentMapper = mock(SaasDeploymentMapper.class);
        subscriptionMapper = mock(SaasSubscriptionMapper.class);
        planMapper = mock(SaasPlanMapper.class);
        snapshotMapper = mock(SaasEntitlementSnapshotMapper.class);
        entitlementService = mock(SaasTenantEntitlementService.class);
        service = serviceAt(INSTANT, "primary");
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant());
        when(deploymentMapper.findByTenantId("tenant_1")).thenReturn(deployment());
        when(subscriptionMapper.findLatestByTenantId("tenant_1")).thenReturn(subscription());
        when(planMapper.selectById(10L)).thenReturn(plan());
        when(entitlementService.effectiveEntitlements("tenant_1")).thenReturn(entitlements(true));
    }

    @Test
    void shouldCreateSignedTwentyFourHourSnapshotWithSortedEntitlements() {
        when(snapshotMapper.findForUpdate("tenant_1")).thenReturn(null);
        when(snapshotMapper.insert(any())).thenReturn(1);

        var snapshot = service.load("tenant_1", "saas-gateway");

        assertThat(snapshot.getVersion()).isEqualTo(1L);
        assertThat(snapshot.getIssuedAtEpochMs()).isEqualTo(INSTANT.toEpochMilli());
        assertThat(snapshot.getExpiresAtEpochMs()).isEqualTo(INSTANT.plus(Duration.ofHours(24)).toEpochMilli());
        assertThat(snapshot.getFeatureGrants()).extracting("featureKey")
                .containsExactly("orders.read", "reports.export");
        assertThat(snapshot.getQuotaLimits()).extracting("quotaKey")
                .containsExactly(SaasQuotaKeys.AI_INPUT_TOKENS, SaasQuotaKeys.AI_OUTPUT_TOKENS,
                        SaasQuotaKeys.STORAGE_BYTES, SaasQuotaKeys.USER_COUNT);
        assertThat(SaasEntitlementSnapshotSignatureUtils.verify(
                SECRET.getBytes(StandardCharsets.UTF_8), snapshot)).isTrue();

        ArgumentCaptor<SaasEntitlementSnapshotEntity> captor =
                ArgumentCaptor.forClass(SaasEntitlementSnapshotEntity.class);
        verify(snapshotMapper).insert(captor.capture());
        assertThat(captor.getValue().getSnapshotJson()).contains("\"signature\"");
        assertThat(captor.getValue().getCreateBy()).isEqualTo("saas-gateway");
        assertThat(captor.getValue().getIssuedAt()).isEqualTo(NOW);
    }

    @Test
    void shouldReuseUnchangedLeaseAndVersionChangedContentWithCas() {
        AtomicReference<SaasEntitlementSnapshotEntity> stored = new AtomicReference<>();
        when(snapshotMapper.findForUpdate("tenant_1")).thenReturn(null);
        doAnswer(invocation -> {
            stored.set(invocation.getArgument(0));
            return 1;
        }).when(snapshotMapper).insert(any());
        var first = service.load("tenant_1", "system");

        when(snapshotMapper.findForUpdate("tenant_1")).thenReturn(stored.get());
        var reused = service.load("tenant_1", "system");
        assertThat(reused.getVersion()).isEqualTo(first.getVersion());
        verify(snapshotMapper, never()).updateVersioned(any(), any(), any(), any());

        when(entitlementService.effectiveEntitlements("tenant_1")).thenReturn(entitlements(false));
        when(snapshotMapper.updateVersioned(any(), eq(0L), eq("system"), eq(NOW))).thenReturn(1);
        var changed = service.load("tenant_1", "system");
        assertThat(changed.getVersion()).isEqualTo(2L);
        assertThat(changed.getFeatureGrants()).filteredOn(grant -> grant.getFeatureKey().equals("orders.read"))
                .extracting("granted").containsExactly(false);
    }

    @Test
    void shouldRotateNearExpiryAndRejectMissingOrDisabledDeployment() {
        when(snapshotMapper.findForUpdate("tenant_1")).thenReturn(null);
        when(snapshotMapper.insert(any())).thenReturn(1);
        SaasEntitlementSnapshotEntity expiring = persisted(service.load("tenant_1", "system"));
        expiring.setExpiresAt(NOW.plusMinutes(4));
        expiring.setVersionNo(3L);
        when(snapshotMapper.findForUpdate("tenant_1")).thenReturn(expiring);
        when(snapshotMapper.updateVersioned(any(), eq(3L), eq("system"), eq(NOW))).thenReturn(1);
        var rotated = service.load("tenant_1", "system");
        assertThat(rotated.getVersion()).isEqualTo(expiring.getSnapshotVersion() + 1);

        when(deploymentMapper.findByTenantId("tenant_1")).thenReturn(null);
        assertThatThrownBy(() -> service.load("tenant_1", "system"))
                .isInstanceOf(SaasSnapshotException.class)
                .extracting(error -> ((SaasSnapshotException) error).getErrorCode())
                .isEqualTo(SaasSnapshotException.ErrorCode.DEPLOYMENT_NOT_ELIGIBLE);
    }

    @Test
    void shouldRequireStrongSigningConfigurationAndTransactionalLoad() throws Exception {
        assertThatThrownBy(() -> serviceWith("primary", "short", INSTANT))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("32");
        Transactional annotation = SaasEntitlementSnapshotServiceImpl.class
                .getDeclaredMethod("load", String.class, String.class).getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.rollbackFor()).contains(Exception.class);
    }

    private SaasEntitlementSnapshotService serviceAt(Instant instant, String keyId) {
        return serviceWith(keyId, SECRET, instant);
    }

    private SaasEntitlementSnapshotService serviceWith(String keyId, String secret, Instant instant) {
        SaasSnapshotSigningProperties properties = new SaasSnapshotSigningProperties();
        properties.setKeyId(keyId);
        properties.setSecret(secret);
        properties.setValidity(Duration.ofHours(24));
        properties.setRenewBefore(Duration.ofMinutes(5));
        return new SaasEntitlementSnapshotServiceImpl(tenantMapper, deploymentMapper, subscriptionMapper,
                planMapper, snapshotMapper, entitlementService, new ObjectMapper(), properties,
                new ControlUtcTime(Clock.fixed(instant, ZoneOffset.UTC)));
    }

    private static SaasTenantEntity tenant() {
        SaasTenantEntity row = new SaasTenantEntity();
        row.setTenantId("tenant_1");
        row.setLifecycleState(TenantLifecycleState.ACTIVE);
        return row;
    }

    private static SaasDeploymentEntity deployment() {
        SaasDeploymentEntity row = new SaasDeploymentEntity();
        row.setTenantId("tenant_1");
        row.setMode(DeploymentMode.SHARED);
        row.setStatus(DeploymentStatus.HEALTHY);
        return row;
    }

    private static SaasSubscriptionEntity subscription() {
        SaasSubscriptionEntity row = new SaasSubscriptionEntity();
        row.setSubscriptionId(100L);
        row.setTenantId("tenant_1");
        row.setPlanId(10L);
        row.setState(SubscriptionState.ACTIVE);
        return row;
    }

    private static SaasPlanEntity plan() {
        SaasPlanEntity row = new SaasPlanEntity();
        row.setPlanId(10L);
        row.setPlanCode("starter");
        row.setStatus(PlanStatus.ACTIVE);
        return row;
    }

    private static EffectiveTenantEntitlements entitlements(boolean ordersRead) {
        TreeMap<String, Boolean> features = new TreeMap<>();
        features.put("reports.export", false);
        features.put("orders.read", ordersRead);
        TreeMap<String, QuotaEntitlement> quotas = new TreeMap<>();
        quotas.put(SaasQuotaKeys.USER_COUNT, new QuotaEntitlement(false, 10));
        quotas.put(SaasQuotaKeys.STORAGE_BYTES, new QuotaEntitlement(true, 0));
        quotas.put(SaasQuotaKeys.AI_INPUT_TOKENS, new QuotaEntitlement(false, 1000));
        quotas.put(SaasQuotaKeys.AI_OUTPUT_TOKENS, new QuotaEntitlement(false, 2000));
        return new EffectiveTenantEntitlements("tenant_1", 100L, 10L, features, quotas);
    }

    private static SaasEntitlementSnapshotEntity persisted(com.erp.saas.contract.model.SaasEntitlementSnapshot value) {
        SaasEntitlementSnapshotEntity row = new SaasEntitlementSnapshotEntity();
        row.setTenantId(value.getTenantId());
        row.setSnapshotVersion(value.getVersion());
        row.setPayloadHash(SaasEntitlementSnapshotSignatureUtils.contentDigest(value));
        try {
            row.setSnapshotJson(new ObjectMapper().writeValueAsString(value));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        row.setIssuedAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(value.getIssuedAtEpochMs()), ZoneOffset.UTC));
        row.setExpiresAt(LocalDateTime.ofInstant(Instant.ofEpochMilli(value.getExpiresAtEpochMs()), ZoneOffset.UTC));
        row.setSignatureKeyId(value.getSignatureKeyId());
        row.setSignature(value.getSignature());
        row.setVersionNo(0L);
        return row;
    }
}
