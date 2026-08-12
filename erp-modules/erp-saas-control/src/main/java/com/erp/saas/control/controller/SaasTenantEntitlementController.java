package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.SaasTenantEntitlementService;
import com.erp.saas.control.service.model.EffectiveTenantEntitlements;
import com.erp.saas.control.service.model.FeatureOverrideCommand;
import com.erp.saas.control.service.model.QuotaOverrideCommand;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/saas/tenants/{tenantId}/entitlements")
public class SaasTenantEntitlementController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasTenantEntitlementService entitlementService;

    public SaasTenantEntitlementController(PlatformSaasAdminGuard adminGuard,
            SaasTenantEntitlementService entitlementService) {
        this.adminGuard = adminGuard;
        this.entitlementService = entitlementService;
    }

    @GetMapping
    public R<EffectiveTenantEntitlements> effective(@PathVariable String tenantId,
            Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(entitlementService.effectiveEntitlements(tenantId));
    }

    @PostMapping("/feature-overrides")
    public R<Long> addFeatureOverride(@PathVariable String tenantId,
            @RequestBody FeatureOverrideRequest request, Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(entitlementService.addFeatureOverride(new FeatureOverrideCommand(
                tenantId, request.featureKey(), request.overrideState(), request.effectiveFrom(),
                request.effectiveUntil(), request.reason()), operator));
    }

    @PostMapping("/quota-overrides")
    public R<Long> addQuotaOverride(@PathVariable String tenantId,
            @RequestBody QuotaOverrideRequest request, Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(entitlementService.addQuotaOverride(new QuotaOverrideCommand(
                tenantId, request.quotaKey(), request.limitValue(), request.effectiveFrom(),
                request.effectiveUntil(), request.reason()), operator));
    }

    @DeleteMapping("/feature-overrides/{overrideId}/{expectedVersion}")
    public R<Void> deleteFeatureOverride(@PathVariable String tenantId,
            @PathVariable Long overrideId, @PathVariable Long expectedVersion,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        entitlementService.deleteFutureFeatureOverride(overrideId, expectedVersion, operator);
        return R.success();
    }

    @DeleteMapping("/quota-overrides/{overrideId}/{expectedVersion}")
    public R<Void> deleteQuotaOverride(@PathVariable String tenantId,
            @PathVariable Long overrideId, @PathVariable Long expectedVersion,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        entitlementService.deleteFutureQuotaOverride(overrideId, expectedVersion, operator);
        return R.success();
    }

    public record FeatureOverrideRequest(String featureKey, FeatureOverrideState overrideState,
            LocalDateTime effectiveFrom, LocalDateTime effectiveUntil, String reason) {
    }

    public record QuotaOverrideRequest(String quotaKey, Long limitValue,
            LocalDateTime effectiveFrom, LocalDateTime effectiveUntil, String reason) {
    }
}
