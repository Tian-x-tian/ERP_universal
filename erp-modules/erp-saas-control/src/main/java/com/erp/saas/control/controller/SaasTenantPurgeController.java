package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.purge.SaasTenantPurgeOrchestrator;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeCommand;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeOutcome;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saas/tenants/{tenantId}/purge")
public class SaasTenantPurgeController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasTenantPurgeOrchestrator orchestrator;

    public SaasTenantPurgeController(PlatformSaasAdminGuard adminGuard,
            SaasTenantPurgeOrchestrator orchestrator) {
        this.adminGuard = adminGuard;
        this.orchestrator = orchestrator;
    }

    @PostMapping
    public R<SaasTenantPurgeOutcome> purge(@PathVariable String tenantId,
            @RequestBody PurgeRequest request, Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(orchestrator.purge(new SaasTenantPurgeCommand(request.requestId(),
                tenantId, request.expectedTenantVersion(), request.confirmationTenantId(), operator)));
    }

    public record PurgeRequest(String requestId, Long expectedTenantVersion,
            String confirmationTenantId) {
    }
}
