package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.legacy.SaasLegacyImportResult;
import com.erp.saas.control.service.legacy.SaasLegacyTenantImportService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saas/tenants")
public class SaasLegacyTenantController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasLegacyTenantImportService importService;

    public SaasLegacyTenantController(PlatformSaasAdminGuard adminGuard,
            SaasLegacyTenantImportService importService) {
        this.adminGuard = adminGuard;
        this.importService = importService;
    }

    @PostMapping("/import-legacy")
    public R<SaasLegacyImportResult> importLegacy(Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(importService.importActiveTenants(operator));
    }
}
