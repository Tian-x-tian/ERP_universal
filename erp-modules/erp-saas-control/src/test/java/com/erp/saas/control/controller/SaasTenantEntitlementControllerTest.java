package com.erp.saas.control.controller;

import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.SaasTenantEntitlementService;
import com.erp.saas.control.service.model.FeatureOverrideCommand;
import com.erp.saas.control.service.model.QuotaOverrideCommand;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.time.LocalDateTime;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantEntitlementControllerTest {
    @Test
    void shouldApplyTenantOverridesWithAuthenticatedOperator() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasTenantEntitlementService service = mock(SaasTenantEntitlementService.class);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("platform-admin");
        SaasTenantEntitlementController controller = new SaasTenantEntitlementController(guard, service);
        LocalDateTime startsAt = LocalDateTime.of(2026, 8, 3, 0, 0);

        controller.addFeatureOverride("tenant-a",
                new SaasTenantEntitlementController.FeatureOverrideRequest(
                        "business.order", FeatureOverrideState.GRANT, startsAt, null, "contract"),
                authentication);
        controller.addQuotaOverride("tenant-a",
                new SaasTenantEntitlementController.QuotaOverrideRequest(
                        "user_count", 50L, startsAt, null, "contract"), authentication);
        controller.effective("tenant-a", authentication);

        verify(service).addFeatureOverride(new FeatureOverrideCommand("tenant-a", "business.order",
                FeatureOverrideState.GRANT, startsAt, null, "contract"), "platform-admin");
        verify(service).addQuotaOverride(new QuotaOverrideCommand("tenant-a", "user_count",
                50L, startsAt, null, "contract"), "platform-admin");
        verify(service).effectiveEntitlements("tenant-a");
        verify(guard, org.mockito.Mockito.times(3)).requireAdmin(authentication);
    }
}
