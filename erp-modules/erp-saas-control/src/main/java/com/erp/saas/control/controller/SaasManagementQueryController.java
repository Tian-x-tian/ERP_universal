package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.management.SaasManagementQueryService;
import com.erp.saas.control.service.management.model.SaasDeploymentManagementView;
import com.erp.saas.control.service.management.model.SaasDomainManagementView;
import com.erp.saas.control.service.management.model.SaasFeatureManagementView;
import com.erp.saas.control.service.management.model.SaasPlanCatalogDetailView;
import com.erp.saas.control.service.management.model.SaasPlanManagementView;
import com.erp.saas.control.service.management.model.SaasTenantManagementView;
import com.erp.saas.control.service.management.model.SaasUsageManagementView;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/saas")
public class SaasManagementQueryController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasManagementQueryService queryService;

    public SaasManagementQueryController(PlatformSaasAdminGuard adminGuard,
            SaasManagementQueryService queryService) {
        this.adminGuard = adminGuard;
        this.queryService = queryService;
    }

    @GetMapping("/tenants")
    public R<List<SaasTenantManagementView>> tenants(Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.listTenants());
    }

    @GetMapping("/plans")
    public R<List<SaasPlanManagementView>> plans(Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.listPlans());
    }

    @GetMapping("/features")
    public R<List<SaasFeatureManagementView>> features(Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.listFeatures());
    }

    @GetMapping("/plans/{planId}")
    public R<SaasPlanCatalogDetailView> plan(@PathVariable Long planId,
            Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.getPlan(planId));
    }

    @GetMapping("/domains")
    public R<List<SaasDomainManagementView>> domains(Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.listDomains());
    }

    @GetMapping("/deployments")
    public R<List<SaasDeploymentManagementView>> deployments(Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.listDeployments());
    }

    @GetMapping("/usage")
    public R<List<SaasUsageManagementView>> usage(Authentication authentication) {
        adminGuard.requireAdmin(authentication);
        return R.success(queryService.listUsage());
    }
}
