package com.erp.saas.control.controller;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.SaasProvisioningStatus;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningService;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningResult;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantControllerTest {
    @Test
    void shouldAuthorizeAndStartIdempotentTenantProvisioning() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasTenantProvisioningService service = mock(SaasTenantProvisioningService.class);
        SaasTenantController controller = new SaasTenantController(guard, service);
        Authentication authentication = mock(Authentication.class);
        SaasTenantProvisioningCommand command = new SaasTenantProvisioningCommand(
                "req-1", "tenant-a", "tenant-a", "Tenant A", "COMP-A", "Company A",
                "admin", "Tenant Admin", "admin@example.com", DeploymentMode.SHARED,
                "standard", "acme.example", "http://erp-system", null);
        SaasTenantProvisioningResult expected = new SaasTenantProvisioningResult(
                "req-1", "tenant-a", SaasProvisioningStatus.SUCCEEDED, TenantLifecycleState.TRIAL,
                1L, 2L, 3L, 4L, 5L, "activation", 10L, false);
        when(guard.requireAdmin(authentication)).thenReturn("platform-admin");
        when(service.provision(command, "platform-admin")).thenReturn(expected);

        var response = controller.createTenant(command, authentication);

        assertThat(response.getData()).isSameAs(expected);
        verify(guard).requireAdmin(authentication);
        verify(service).provision(command, "platform-admin");
    }
}
