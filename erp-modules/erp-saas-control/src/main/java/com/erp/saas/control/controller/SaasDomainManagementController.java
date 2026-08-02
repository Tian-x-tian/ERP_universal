package com.erp.saas.control.controller;

import com.erp.common.core.domain.R;
import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.domain.SaasDomainService;
import com.erp.saas.control.service.domain.model.RegisterDomainCommand;
import com.erp.saas.control.service.domain.model.RevokeDomainCommand;
import com.erp.saas.control.service.domain.model.SaasDomainView;
import com.erp.saas.control.service.domain.model.VerifyDomainCommand;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/saas/domains")
public class SaasDomainManagementController {
    private final PlatformSaasAdminGuard adminGuard;
    private final SaasDomainService domainService;

    public SaasDomainManagementController(PlatformSaasAdminGuard adminGuard, SaasDomainService domainService) {
        this.adminGuard = adminGuard;
        this.domainService = domainService;
    }

    @PostMapping
    public R<SaasDomainView> register(@RequestBody RegisterDomainRequest request,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(domainService.register(new RegisterDomainCommand(
                request.tenantId(), request.host(), request.verificationMethod(), operator)));
    }

    @PostMapping("/{domainId}/verify")
    public R<SaasDomainView> verify(@PathVariable Long domainId, @RequestBody VersionRequest request,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(domainService.verify(new VerifyDomainCommand(domainId, request.expectedVersion(), operator)));
    }

    @PostMapping("/{domainId}/revoke")
    public R<SaasDomainView> revoke(@PathVariable Long domainId, @RequestBody VersionRequest request,
            Authentication authentication) {
        String operator = adminGuard.requireAdmin(authentication);
        return R.success(domainService.revoke(new RevokeDomainCommand(domainId, request.expectedVersion(), operator)));
    }

    public record RegisterDomainRequest(String tenantId, String host,
            DomainVerificationMethod verificationMethod) {
    }

    public record VersionRequest(Long expectedVersion) {
    }
}
