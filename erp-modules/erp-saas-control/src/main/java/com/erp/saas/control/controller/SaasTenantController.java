package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningService;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningResult;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saas")
public class SaasTenantController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasTenantProvisioningService provisioningService;

    public SaasTenantController(PlatformSaasAdminGuard adminGuard,
            SaasTenantProvisioningService provisioningService) {
        this.adminGuard = adminGuard;
        this.provisioningService = provisioningService;
    }

    @PostMapping("/tenants")
    public R<SaasTenantProvisioningResult> createTenant(
            @RequestBody SaasTenantProvisioningCommand command,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(provisioningService.provision(command, operator));
    }
}
