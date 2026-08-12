package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.SaasPlanCatalogService;
import com.erp.saas.control.service.model.FeatureDefinitionCommand;
import com.erp.saas.control.service.model.PlanDraftCommand;
import com.erp.saas.control.service.model.PlanFeatureGrantCommand;
import com.erp.saas.control.service.model.PlanQuotaCommand;
import com.erp.saas.control.service.model.PublishPlanCommand;
import com.erp.saas.control.service.model.SaasFeatureView;
import com.erp.saas.control.service.model.SaasPlanView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/saas")
public class SaasPlanManagementController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasPlanCatalogService catalogService;

    public SaasPlanManagementController(PlatformSaasAdminGuard adminGuard,
            SaasPlanCatalogService catalogService) {
        this.adminGuard = adminGuard;
        this.catalogService = catalogService;
    }

    @PostMapping("/plans/drafts")
    public R<SaasPlanView> createDraft(@RequestBody PlanDraftCommand command,
            Authentication authentication) {
        return R.success(catalogService.createDraft(command, adminGuard.requireAdmin(authentication)));
    }

    @PutMapping("/plans/{planId}/drafts/{expectedVersion}")
    public R<SaasPlanView> updateDraft(@PathVariable Long planId, @PathVariable Long expectedVersion,
            @RequestBody PlanDraftCommand command, Authentication authentication) {
        return R.success(catalogService.updateDraft(
                planId, expectedVersion, command, adminGuard.requireAdmin(authentication)));
    }

    @PutMapping("/plans/{planId}/features")
    public R<SaasPlanView> replaceFeatures(@PathVariable Long planId,
            @RequestBody ReplaceFeaturesRequest request, Authentication authentication) {
        return R.success(catalogService.replaceDraftFeatures(planId, request.expectedVersion(),
                request.grants(), adminGuard.requireAdmin(authentication)));
    }

    @PutMapping("/plans/{planId}/quotas")
    public R<SaasPlanView> replaceQuotas(@PathVariable Long planId,
            @RequestBody ReplaceQuotasRequest request, Authentication authentication) {
        return R.success(catalogService.replaceDraftQuotas(planId, request.expectedVersion(),
                request.quotas(), adminGuard.requireAdmin(authentication)));
    }

    @PostMapping("/plans/{planId}/publish")
    public R<SaasPlanView> publish(@PathVariable Long planId, @RequestBody PublishRequest request,
            Authentication authentication) {
        return R.success(catalogService.publish(new PublishPlanCommand(planId,
                request.expectedPlanVersion(), request.expectedActivePlanId(),
                request.expectedActivePlanVersion()), adminGuard.requireAdmin(authentication)));
    }

    @PostMapping("/features")
    public R<SaasFeatureView> defineFeature(@RequestBody FeatureDefinitionCommand command,
            Authentication authentication) {
        return R.success(catalogService.defineFeature(command, adminGuard.requireAdmin(authentication)));
    }

    @PutMapping("/features/{featureId}/{expectedVersion}")
    public R<SaasFeatureView> updateFeature(@PathVariable Long featureId,
            @PathVariable Long expectedVersion, @RequestBody FeatureDefinitionCommand command,
            Authentication authentication) {
        return R.success(catalogService.updateFeature(
                featureId, expectedVersion, command, adminGuard.requireAdmin(authentication)));
    }

    public record ReplaceFeaturesRequest(Long expectedVersion, List<PlanFeatureGrantCommand> grants) {
    }

    public record ReplaceQuotasRequest(Long expectedVersion, List<PlanQuotaCommand> quotas) {
    }

    public record PublishRequest(Long expectedPlanVersion, Long expectedActivePlanId,
            Long expectedActivePlanVersion) {
    }
}
