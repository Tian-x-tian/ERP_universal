package com.erp.saas.control.controller;

import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasHostResolution;
import com.erp.common.security.AuthenticatedUserPrincipal;
import com.erp.saas.control.service.domain.SaasDomainService;
import com.erp.saas.control.service.snapshot.SaasEntitlementSnapshotService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/saas")
public class InternalSaasController {
    private final SaasDomainService domainService;
    private final SaasEntitlementSnapshotService snapshotService;

    public InternalSaasController(SaasDomainService domainService,
            SaasEntitlementSnapshotService snapshotService) {
        this.domainService = domainService;
        this.snapshotService = snapshotService;
    }

    @GetMapping("/hosts/resolve")
    public ResponseEntity<SaasHostResolution> resolveHost(@RequestParam("host") String host) {
        return domainService.resolve(host)
                .map(resolved -> ResponseEntity.ok(new SaasHostResolution(
                        resolved.host(), resolved.tenantId(), resolved.deploymentMode(),
                        resolved.lifecycleState(), true)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/tenants/{tenantId}/entitlement-snapshot")
    public SaasEntitlementSnapshot entitlementSnapshot(@PathVariable String tenantId,
            Authentication authentication) {
        String operator = "internal-service";
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUserPrincipal principal) {
            operator = principal.getUserName();
        } else if (authentication != null) {
            operator = authentication.getName();
        }
        return snapshotService.load(tenantId, operator);
    }
}
