package com.erp.saas.control.service;

import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.QuotaPeriodType;
import com.erp.saas.control.service.model.EffectiveTenantEntitlements;
import com.erp.saas.control.service.model.FeatureDefinitionCommand;
import com.erp.saas.control.service.model.FeatureOverrideCommand;
import com.erp.saas.control.service.model.PlanDraftCommand;
import com.erp.saas.control.service.model.PlanFeatureGrantCommand;
import com.erp.saas.control.service.model.PlanQuotaCommand;
import com.erp.saas.control.service.model.PublishPlanCommand;
import com.erp.saas.control.service.model.QuotaEntitlement;
import com.erp.saas.control.service.model.QuotaOverrideCommand;
import com.erp.saas.control.service.model.SaasFeatureView;
import com.erp.saas.control.service.model.SaasPlanView;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SaasPlanCatalogContractTest {

    @Test
    void shouldNormalizeAndValidateCatalogCommands() {
        PlanDraftCommand plan = new PlanDraftCommand(" starter ", 1, " Starter ", 14, 7, " entry plan ");
        assertEquals("starter", plan.planCode());
        assertEquals("Starter", plan.planName());
        assertEquals("entry plan", plan.description());

        FeatureDefinitionCommand feature = new FeatureDefinitionCommand(
                " reports.view ", " Reports ", FeatureStatus.ACTIVE, null);
        assertEquals("reports.view", feature.featureKey());
        assertEquals("Reports", feature.featureName());

        PlanFeatureGrantCommand grant = new PlanFeatureGrantCommand(" reports.view ", true);
        assertEquals("reports.view", grant.featureKey());

        assertInvalid(() -> new PlanDraftCommand("x", 1, "Plan", 0, 0, null));
        assertInvalid(() -> new PlanDraftCommand("plan", 0, "Plan", 0, 0, null));
        assertInvalid(() -> new PlanDraftCommand("plan", 1, " ", 0, 0, null));
        assertInvalid(() -> new PlanDraftCommand("plan", 1, "Plan", -1, 0, null));
        assertInvalid(() -> new PlanDraftCommand("plan", 1, "Plan", 0, 3651, null));
        assertInvalid(() -> new FeatureDefinitionCommand("Feature", "Feature", FeatureStatus.ACTIVE, null));
        assertInvalid(() -> new FeatureDefinitionCommand("feature", "Feature", null, null));
        assertInvalid(() -> new PlanFeatureGrantCommand("feature", null));
    }

    @Test
    void shouldAcceptOnlyKnownQuotaKeysWithTheirFrozenPeriods() {
        assertEquals(QuotaPeriodType.CURRENT,
                new PlanQuotaCommand(SaasQuotaKeys.USER_COUNT, null, QuotaPeriodType.CURRENT).periodType());
        assertEquals(QuotaPeriodType.CURRENT,
                new PlanQuotaCommand(SaasQuotaKeys.STORAGE_BYTES, 0L, QuotaPeriodType.CURRENT).periodType());
        assertEquals(QuotaPeriodType.MONTHLY,
                new PlanQuotaCommand(SaasQuotaKeys.AI_INPUT_TOKENS, 1L, QuotaPeriodType.MONTHLY).periodType());
        assertEquals(QuotaPeriodType.MONTHLY,
                new PlanQuotaCommand(SaasQuotaKeys.AI_OUTPUT_TOKENS, 1L, QuotaPeriodType.MONTHLY).periodType());

        assertCode(SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY,
                () -> new PlanQuotaCommand("custom_quota", 1L, QuotaPeriodType.CURRENT));
        assertInvalid(() -> new PlanQuotaCommand(SaasQuotaKeys.USER_COUNT, 1L, QuotaPeriodType.MONTHLY));
        assertInvalid(() -> new PlanQuotaCommand(SaasQuotaKeys.AI_INPUT_TOKENS, 1L, QuotaPeriodType.CURRENT));
        assertInvalid(() -> new PlanQuotaCommand(SaasQuotaKeys.STORAGE_BYTES, -1L, QuotaPeriodType.CURRENT));
    }

    @Test
    void shouldValidateTenantOverrideCommands() {
        LocalDateTime effectiveFrom = LocalDateTime.of(2026, 8, 1, 10, 0);
        FeatureOverrideCommand feature = new FeatureOverrideCommand(
                " tenant_1 ", " reports.view ", FeatureOverrideState.GRANT,
                effectiveFrom, null, " approved ");
        assertEquals("tenant_1", feature.tenantId());
        assertEquals("reports.view", feature.featureKey());
        assertEquals("approved", feature.reason());

        QuotaOverrideCommand quota = new QuotaOverrideCommand(
                "tenant-1", SaasQuotaKeys.USER_COUNT, null, effectiveFrom, null, null);
        assertNull(quota.limitValue());

        assertInvalid(() -> new FeatureOverrideCommand(
                "tenant.id", "reports.view", FeatureOverrideState.GRANT, effectiveFrom, null, null));
        assertInvalid(() -> new FeatureOverrideCommand(
                "tenant", "reports.view", null, effectiveFrom, null, null));
        assertInvalid(() -> new FeatureOverrideCommand(
                "tenant", "reports.view", FeatureOverrideState.GRANT, null, null, null));
        assertCode(SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY, () -> new QuotaOverrideCommand(
                "tenant", "unknown", 1L, effectiveFrom, null, null));
        assertInvalid(() -> new QuotaOverrideCommand(
                "tenant", SaasQuotaKeys.USER_COUNT, -1L, effectiveFrom, null, null));
    }

    @Test
    void shouldRequirePublishActivePlanExpectationAsAPair() {
        PublishPlanCommand withoutActive = new PublishPlanCommand(10L, 3L, null, null);
        PublishPlanCommand withActive = new PublishPlanCommand(10L, 3L, 9L, 5L);
        assertNull(withoutActive.expectedActivePlanId());
        assertEquals(9L, withActive.expectedActivePlanId());

        assertInvalid(() -> new PublishPlanCommand(null, 3L, null, null));
        assertInvalid(() -> new PublishPlanCommand(10L, null, null, null));
        assertInvalid(() -> new PublishPlanCommand(10L, 3L, 9L, null));
        assertInvalid(() -> new PublishPlanCommand(10L, 3L, null, 5L));
    }

    @Test
    void shouldCanonicalizeQuotaEntitlementAndRejectNegativeLimits() {
        QuotaEntitlement unlimited = new QuotaEntitlement(true, 99L);
        assertTrue(unlimited.unlimited());
        assertEquals(0L, unlimited.limitValue());

        QuotaEntitlement limited = new QuotaEntitlement(false, 12L);
        assertFalse(limited.unlimited());
        assertEquals(12L, limited.limitValue());
        assertInvalid(() -> new QuotaEntitlement(false, -1L));
    }

    @Test
    void shouldExposeImmutableSortedEffectiveEntitlements() {
        TreeMap<String, Boolean> features = new TreeMap<>();
        features.put("z.feature", true);
        features.put("a.feature", false);
        TreeMap<String, QuotaEntitlement> quotas = new TreeMap<>();
        quotas.put(SaasQuotaKeys.USER_COUNT, new QuotaEntitlement(false, 10L));

        EffectiveTenantEntitlements entitlements = new EffectiveTenantEntitlements(
                " tenant_1 ", 20L, 10L, features, quotas);
        features.put("later.feature", true);
        quotas.clear();

        assertEquals("tenant_1", entitlements.tenantId());
        assertEquals(List.of("a.feature", "z.feature"), List.copyOf(entitlements.features().keySet()));
        assertEquals(false, entitlements.isFeatureEnabled("a.feature"));
        assertEquals(10L, entitlements.quotaLimit(SaasQuotaKeys.USER_COUNT).limitValue());
        assertThrows(UnsupportedOperationException.class,
                () -> entitlements.features().put("new.feature", true));
        assertThrows(UnsupportedOperationException.class,
                () -> entitlements.quotas().put(SaasQuotaKeys.STORAGE_BYTES, new QuotaEntitlement(false, 1L)));
        assertNotSame(features, entitlements.features());
        assertNotSame(quotas, entitlements.quotas());

        SaasCatalogException missingFeature = assertThrows(SaasCatalogException.class,
                () -> entitlements.isFeatureEnabled("missing.feature"));
        assertEquals(SaasCatalogException.ErrorCode.UNKNOWN_FEATURE_KEY, missingFeature.getErrorCode());
        SaasCatalogException missingQuota = assertThrows(SaasCatalogException.class,
                () -> entitlements.quotaLimit(SaasQuotaKeys.STORAGE_BYTES));
        assertEquals(SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY, missingQuota.getErrorCode());

        assertInvalid(() -> new EffectiveTenantEntitlements("tenant", null, 10L, features, quotas));
        assertInvalid(() -> new EffectiveTenantEntitlements("tenant", 20L, null, features, quotas));
    }

    @Test
    void shouldFreezeViewsErrorsAndServiceInterfaces() throws Exception {
        SaasPlanView planView = new SaasPlanView(
                1L, "starter", 1, "Starter", PlanStatus.DRAFT, 14, 7, null, 2L);
        SaasFeatureView featureView = new SaasFeatureView(
                2L, "reports.view", "Reports", FeatureStatus.ACTIVE, null, 3L);
        assertEquals(PlanStatus.DRAFT, planView.status());
        assertEquals(FeatureStatus.ACTIVE, featureView.status());

        assertEquals(List.of("NOT_FOUND", "DUPLICATE", "INVALID_INPUT", "IMMUTABLE_PUBLISHED_PLAN",
                        "VERSION_CONFLICT", "OVERLAPPING_OVERRIDE", "UNKNOWN_FEATURE_KEY", "UNKNOWN_QUOTA_KEY"),
                List.of(SaasCatalogException.ErrorCode.values()).stream().map(Enum::name).toList());

        assertMethod(SaasPlanCatalogService.class, "createDraft", SaasPlanView.class,
                PlanDraftCommand.class, String.class);
        assertMethod(SaasPlanCatalogService.class, "updateDraft", SaasPlanView.class,
                Long.class, Long.class, PlanDraftCommand.class, String.class);
        assertMethod(SaasPlanCatalogService.class, "defineFeature", SaasFeatureView.class,
                FeatureDefinitionCommand.class, String.class);
        assertMethod(SaasPlanCatalogService.class, "updateFeature", SaasFeatureView.class,
                Long.class, Long.class, FeatureDefinitionCommand.class, String.class);
        assertMethod(SaasPlanCatalogService.class, "replaceDraftFeatures", SaasPlanView.class,
                Long.class, Long.class, List.class, String.class);
        assertMethod(SaasPlanCatalogService.class, "replaceDraftQuotas", SaasPlanView.class,
                Long.class, Long.class, List.class, String.class);
        assertMethod(SaasPlanCatalogService.class, "publish", SaasPlanView.class,
                PublishPlanCommand.class, String.class);
        assertEquals(7, SaasPlanCatalogService.class.getDeclaredMethods().length);

        assertMethod(SaasTenantEntitlementService.class, "addFeatureOverride", Long.class,
                FeatureOverrideCommand.class, String.class);
        assertMethod(SaasTenantEntitlementService.class, "addQuotaOverride", Long.class,
                QuotaOverrideCommand.class, String.class);
        assertMethod(SaasTenantEntitlementService.class, "deleteFutureFeatureOverride", void.class,
                Long.class, Long.class, String.class);
        assertMethod(SaasTenantEntitlementService.class, "deleteFutureQuotaOverride", void.class,
                Long.class, Long.class, String.class);
        assertMethod(SaasTenantEntitlementService.class, "effectiveEntitlements", EffectiveTenantEntitlements.class,
                String.class);
        assertEquals(5, SaasTenantEntitlementService.class.getDeclaredMethods().length);
    }

    private static void assertMethod(Class<?> type, String name, Class<?> returnType, Class<?>... parameterTypes)
            throws Exception {
        Method method = type.getDeclaredMethod(name, parameterTypes);
        assertEquals(returnType, method.getReturnType());
    }

    private static void assertInvalid(Runnable action) {
        SaasCatalogException exception = assertThrows(SaasCatalogException.class, action::run);
        assertEquals(SaasCatalogException.ErrorCode.INVALID_INPUT, exception.getErrorCode());
    }

    private static void assertCode(SaasCatalogException.ErrorCode code, Runnable action) {
        SaasCatalogException exception = assertThrows(SaasCatalogException.class, action::run);
        assertEquals(code, exception.getErrorCode());
    }
}
