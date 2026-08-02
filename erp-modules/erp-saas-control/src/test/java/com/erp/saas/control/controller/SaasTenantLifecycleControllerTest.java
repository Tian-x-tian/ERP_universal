package com.erp.saas.control.controller;

import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantLifecycleControllerTest {
    @Test
    void shouldUseAuthenticatedOperatorWhenSuspendingTenant() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasTenantLifecycleService service = mock(SaasTenantLifecycleService.class);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("platform-admin");
        SaasTenantLifecycleController controller = new SaasTenantLifecycleController(guard, service);

        controller.suspend("tenant-a", new SaasTenantLifecycleController.VersionRequest(3L), authentication);

        verify(service).suspend(new TenantVersionCommand("tenant-a", 3L, "platform-admin"));
    }
}
