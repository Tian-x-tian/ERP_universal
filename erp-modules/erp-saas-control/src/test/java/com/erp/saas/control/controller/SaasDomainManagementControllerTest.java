package com.erp.saas.control.controller;

import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.domain.SaasDomainService;
import com.erp.saas.control.service.domain.model.RegisterDomainCommand;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasDomainManagementControllerTest {
    @Test
    void shouldUseAuthenticatedOperatorWhenRegisteringDomain() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasDomainService service = mock(SaasDomainService.class);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("platform-admin");
        SaasDomainManagementController controller = new SaasDomainManagementController(guard, service);

        controller.register(new SaasDomainManagementController.RegisterDomainRequest(
                "tenant-a", "acme.example", DomainVerificationMethod.PLATFORM_MANUAL), authentication);

        verify(service).register(new RegisterDomainCommand(
                "tenant-a", "acme.example", DomainVerificationMethod.PLATFORM_MANUAL, "platform-admin"));
    }
}
