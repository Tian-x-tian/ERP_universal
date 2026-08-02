package com.erp.business.controller.internal;

import com.erp.business.saas.service.SaasTenantStoragePurgeService;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantStoragePurgeResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business/internal/saas")
public class BusinessSaasInternalController {
    private final SaasTenantStoragePurgeService purgeService;

    public BusinessSaasInternalController(SaasTenantStoragePurgeService purgeService) {
        this.purgeService = purgeService;
    }

    @PostMapping("/tenants/purge-storage")
    public SaasTenantStoragePurgeResult purgeStorage(@RequestBody SaasTenantPurgeRequest request) {
        return purgeService.purge(request);
    }
}
