package com.erp.system.controller;

import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SaasRuntimeAccess;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.saas.SaasRuntimeEntitlements;
import com.erp.system.saas.SaasRuntimeSnapshotService;
import com.erp.system.saas.SaasTenantPurgeService;
import com.erp.system.saas.SaasTenantActivationReissueService;
import com.erp.system.saas.SaasTenantInitializationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/internal/saas")
public class SystemSaasInternalController {
    private final SaasTenantInitializationService initializationService;
    private final SaasTenantActivationReissueService activationReissueService;
    private final SaasRuntimeSnapshotService snapshotService;
    private final SaasTenantPurgeService purgeService;

    public SystemSaasInternalController(SaasTenantInitializationService initializationService,
            SaasTenantActivationReissueService activationReissueService,
            SaasRuntimeSnapshotService snapshotService, SaasTenantPurgeService purgeService) {
        this.initializationService = initializationService;
        this.activationReissueService = activationReissueService;
        this.snapshotService = snapshotService;
        this.purgeService = purgeService;
    }

    @PostMapping("/tenants/initialize")
    public SaasTenantInitializationResult initializeTenant(
            @RequestBody SaasTenantInitializationRequest request) {
        return initializationService.initialize(request);
    }

    @PostMapping("/tenants/activation/reissue")
    public SaasTenantActivationReissueResult reissueActivation(
            @RequestBody SaasTenantActivationReissueRequest request) {
        return activationReissueService.reissue(request);
    }

    @GetMapping("/runtime-access")
    public SaasRuntimeAccess runtimeAccess() {
        String tenantId = TenantContextHolder.getTenantId();
        SaasRuntimeEntitlements entitlements = snapshotService.current(tenantId);
        return new SaasRuntimeAccess(entitlements.tenantId(), entitlements.lifecycleState(),
                entitlements.stale(), entitlements.loginAllowed(), entitlements.writeAllowed(),
                entitlements.features());
    }

    @PostMapping("/tenants/purge")
    public SaasTenantPurgeResult purgeTenant(@RequestBody SaasTenantPurgeRequest request) {
        return purgeService.purge(request);
    }
}
