package com.erp.saas.control.controller;

import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.purge.SaasTenantPurgeOrchestrator;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeCommand;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantPurgeControllerTest {
    @Test
    void shouldRequirePlatformAdminAndTypedTenantConfirmation() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasTenantPurgeOrchestrator orchestrator = mock(SaasTenantPurgeOrchestrator.class);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("platform-admin");
        SaasTenantPurgeController controller = new SaasTenantPurgeController(guard, orchestrator);

        controller.purge("tenant-a", new SaasTenantPurgeController.PurgeRequest(
                "purge-001", 7L, "tenant-a"), authentication);

        verify(orchestrator).purge(new SaasTenantPurgeCommand(
                "purge-001", "tenant-a", 7L, "tenant-a", "platform-admin"));
    }
}
