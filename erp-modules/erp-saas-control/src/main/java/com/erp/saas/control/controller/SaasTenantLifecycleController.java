package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.ActivateSubscriptionCommand;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/saas/tenants/{tenantId}")
public class SaasTenantLifecycleController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasTenantLifecycleService lifecycleService;

    public SaasTenantLifecycleController(PlatformSaasAdminGuard adminGuard,
            SaasTenantLifecycleService lifecycleService) {
        this.adminGuard = adminGuard;
        this.lifecycleService = lifecycleService;
    }

    @PostMapping("/activate")
    public R<SaasTenantLifecycleView> activate(@PathVariable String tenantId,
            @RequestBody ActivateRequest request, Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(lifecycleService.activate(new ActivateSubscriptionCommand(
                tenantId, request.planId(), request.expectedTenantVersion(), request.endAt(),
                request.nonExpiring(), operator)));
    }

    @PostMapping("/suspend")
    public R<SaasTenantLifecycleView> suspend(@PathVariable String tenantId,
            @RequestBody VersionRequest request, Authentication authentication) {
        return R.success(lifecycleService.suspend(command(tenantId, request, authentication)));
    }

    @PostMapping("/resume")
    public R<SaasTenantLifecycleView> resume(@PathVariable String tenantId,
            @RequestBody VersionRequest request, Authentication authentication) {
        return R.success(lifecycleService.resume(command(tenantId, request, authentication)));
    }

    @PostMapping("/archive")
    public R<SaasTenantLifecycleView> archive(@PathVariable String tenantId,
            @RequestBody VersionRequest request, Authentication authentication) {
        return R.success(lifecycleService.archive(command(tenantId, request, authentication)));
    }

    @PostMapping("/purge-pending")
    public R<SaasTenantLifecycleView> markPurgePending(@PathVariable String tenantId,
            @RequestBody VersionRequest request, Authentication authentication) {
        return R.success(lifecycleService.markPurgePending(command(tenantId, request, authentication)));
    }

    private TenantVersionCommand command(String tenantId, VersionRequest request,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return new TenantVersionCommand(tenantId, request.expectedTenantVersion(), operator);
    }

    public record VersionRequest(Long expectedTenantVersion) {
    }

    public record ActivateRequest(Long planId, Long expectedTenantVersion,
            LocalDateTime endAt, boolean nonExpiring) {
    }
}
