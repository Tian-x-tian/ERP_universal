package com.erp.saas.control.service;

import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.domain.entity.*;
import com.erp.saas.control.mapper.*;
import com.erp.saas.control.service.impl.SaasTenantEntitlementServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaasEffectiveEntitlementsTest {
    private static final Instant INSTANT = Instant.parse("2026-08-01T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private SaasTenantMapper tenantMapper;
    private SaasFeatureMapper featureMapper;
    private SaasSubscriptionMapper subscriptionMapper;
    private SaasPlanMapper planMapper;
    private SaasPlanFeatureMapper planFeatureMapper;
    private SaasPlanQuotaMapper planQuotaMapper;
    private SaasTenantFeatureOverrideMapper featureOverrideMapper;
    private SaasTenantQuotaOverrideMapper quotaOverrideMapper;
    private SaasTenantEntitlementService service;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SaasTenantMapper.class);
        featureMapper = mock(SaasFeatureMapper.class);
        subscriptionMapper = mock(SaasSubscriptionMapper.class);
        planMapper = mock(SaasPlanMapper.class);
        planFeatureMapper = mock(SaasPlanFeatureMapper.class);
        planQuotaMapper = mock(SaasPlanQuotaMapper.class);
        featureOverrideMapper = mock(SaasTenantFeatureOverrideMapper.class);
        quotaOverrideMapper = mock(SaasTenantQuotaOverrideMapper.class);
        service = new SaasTenantEntitlementServiceImpl(tenantMapper, featureMapper, subscriptionMapper,
                planMapper, planFeatureMapper, planQuotaMapper, featureOverrideMapper, quotaOverrideMapper,
                new ControlUtcTime(Clock.fixed(INSTANT, ZoneOffset.UTC)));
        when(tenantMapper.findByTenantId("tenant_1")).thenReturn(tenant());
        when(featureMapper.findAllOrdered()).thenReturn(List.of(
                feature(1L, "reports.edit", FeatureStatus.INACTIVE),
                feature(2L, "reports.view", FeatureStatus.ACTIVE)));
        when(featureOverrideMapper.findByTenantId("tenant_1")).thenReturn(List.of());
        when(quotaOverrideMapper.findByTenantId("tenant_1")).thenReturn(List.of());
    }

    @Test
    void shouldFailClosedWithoutCurrentSubscription() {
        when(subscriptionMapper.findCurrentByTenantId("tenant_1")).thenReturn(null);
        var result = service.effectiveEntitlements("tenant_1");
        assertThat(result.subscriptionId()).isNull();
        assertThat(result.features()).containsExactlyEntriesOf(new java.util.TreeMap<>(java.util.Map.of(
                "reports.edit", false, "reports.view", false)));
        assertThat(result.quotas()).allSatisfy((key, value) -> {
            assertThat(value.unlimited()).isFalse();
            assertThat(value.limitValue()).isZero();
        }).hasSize(4);
    }

    @Test
    void shouldApplyActivePlanAndCurrentOverridesWithHalfOpenWindows() {
        SaasSubscriptionEntity subscription = new SaasSubscriptionEntity();
        subscription.setSubscriptionId(10L);
        subscription.setPlanId(20L);
        when(subscriptionMapper.findCurrentByTenantId("tenant_1")).thenReturn(subscription);
        when(planMapper.selectById(20L)).thenReturn(plan(20L));
        when(planFeatureMapper.findByPlanId(20L)).thenReturn(List.of(grant(1L, true), grant(2L, true)));
        when(planQuotaMapper.findByPlanId(20L)).thenReturn(List.of(
                quota(SaasQuotaKeys.USER_COUNT, 10L), quota(SaasQuotaKeys.STORAGE_BYTES, null)));
        when(featureOverrideMapper.findByTenantId("tenant_1")).thenReturn(List.of(
                featureOverride(2L, FeatureOverrideState.DENY, NOW, null),
                featureOverride(1L, FeatureOverrideState.GRANT, NOW.minusHours(1), NOW.plusHours(1))));
        when(quotaOverrideMapper.findByTenantId("tenant_1")).thenReturn(List.of(
                quotaOverride(SaasQuotaKeys.USER_COUNT, 15L, NOW.minusHours(1), NOW),
                quotaOverride(SaasQuotaKeys.AI_INPUT_TOKENS, null, NOW, null)));

        var result = service.effectiveEntitlements("tenant_1");

        assertThat(result.isFeatureEnabled("reports.view")).isFalse();
        assertThat(result.isFeatureEnabled("reports.edit")).isFalse();
        assertThat(result.quotaLimit(SaasQuotaKeys.USER_COUNT).limitValue()).isEqualTo(10L);
        assertThat(result.quotaLimit(SaasQuotaKeys.STORAGE_BYTES).unlimited()).isTrue();
        assertThat(result.quotaLimit(SaasQuotaKeys.AI_INPUT_TOKENS).unlimited()).isTrue();
        assertThat(result.quotaLimit(SaasQuotaKeys.AI_OUTPUT_TOKENS).limitValue()).isZero();
    }

    @Test
    void shouldRejectDuplicateCurrentlyEffectiveControlData() {
        SaasSubscriptionEntity subscription = new SaasSubscriptionEntity();
        subscription.setSubscriptionId(10L);
        subscription.setPlanId(20L);
        when(subscriptionMapper.findCurrentByTenantId("tenant_1")).thenReturn(subscription);
        when(planMapper.selectById(20L)).thenReturn(plan(20L));
        when(planFeatureMapper.findByPlanId(20L)).thenReturn(List.of());
        when(planQuotaMapper.findByPlanId(20L)).thenReturn(List.of());
        when(quotaOverrideMapper.findByTenantId("tenant_1")).thenReturn(List.of(
                quotaOverride(SaasQuotaKeys.USER_COUNT, 10L, NOW.minusHours(2), null),
                quotaOverride(SaasQuotaKeys.USER_COUNT, 20L, NOW.minusHours(1), null)));

        assertThatThrownBy(() -> service.effectiveEntitlements("tenant_1"))
                .isInstanceOf(SaasCatalogException.class)
                .extracting(error -> ((SaasCatalogException) error).getErrorCode())
                .isEqualTo(SaasCatalogException.ErrorCode.DUPLICATE);
    }

    @Test
    void shouldKeepLegacyFullAccessUnlimitedAndEnableEveryActiveFeature() {
        SaasSubscriptionEntity subscription = new SaasSubscriptionEntity();
        subscription.setSubscriptionId(10L);
        subscription.setPlanId(20L);
        SaasPlanEntity legacyPlan = plan(20L);
        legacyPlan.setPlanCode("legacy-full-access");
        when(subscriptionMapper.findCurrentByTenantId("tenant_1")).thenReturn(subscription);
        when(planMapper.selectById(20L)).thenReturn(legacyPlan);

        var result = service.effectiveEntitlements("tenant_1");

        assertThat(result.isFeatureEnabled("reports.view")).isTrue();
        assertThat(result.isFeatureEnabled("reports.edit")).isFalse();
        assertThat(result.quotas()).allSatisfy((key, value) -> assertThat(value.unlimited()).isTrue());
    }

    @Test
    void shouldExposeReadOnlyTransaction() throws Exception {
        Transactional annotation = SaasTenantEntitlementServiceImpl.class
                .getDeclaredMethod("effectiveEntitlements", String.class).getAnnotation(Transactional.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.readOnly()).isTrue();
    }

    private static SaasTenantEntity tenant() {
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId("tenant_1");
        return tenant;
    }

    private static SaasFeatureEntity feature(Long id, String key, FeatureStatus status) {
        SaasFeatureEntity feature = new SaasFeatureEntity();
        feature.setFeatureId(id);
        feature.setFeatureKey(key);
        feature.setStatus(status);
        return feature;
    }

    private static SaasPlanEntity plan(Long id) {
        SaasPlanEntity plan = new SaasPlanEntity();
        plan.setPlanId(id);
        return plan;
    }

    private static SaasPlanFeatureEntity grant(Long featureId, boolean granted) {
        SaasPlanFeatureEntity row = new SaasPlanFeatureEntity();
        row.setFeatureId(featureId);
        row.setGranted(granted);
        return row;
    }

    private static SaasPlanQuotaEntity quota(String key, Long limit) {
        SaasPlanQuotaEntity row = new SaasPlanQuotaEntity();
        row.setQuotaKey(key);
        row.setLimitValue(limit);
        return row;
    }

    private static SaasTenantFeatureOverrideEntity featureOverride(Long featureId, FeatureOverrideState state,
            LocalDateTime start, LocalDateTime end) {
        SaasTenantFeatureOverrideEntity row = new SaasTenantFeatureOverrideEntity();
        row.setFeatureId(featureId);
        row.setOverrideState(state);
        row.setEffectiveFrom(start);
        row.setEffectiveUntil(end);
        return row;
    }

    private static SaasTenantQuotaOverrideEntity quotaOverride(String key, Long limit,
            LocalDateTime start, LocalDateTime end) {
        SaasTenantQuotaOverrideEntity row = new SaasTenantQuotaOverrideEntity();
        row.setQuotaKey(key);
        row.setLimitValue(limit);
        row.setEffectiveFrom(start);
        row.setEffectiveUntil(end);
        return row;
    }
}
