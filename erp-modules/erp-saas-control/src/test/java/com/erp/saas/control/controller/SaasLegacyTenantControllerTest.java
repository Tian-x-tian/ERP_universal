package com.erp.saas.control.controller;

import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.legacy.SaasLegacyImportResult;
import com.erp.saas.control.service.legacy.SaasLegacyTenantImportService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasLegacyTenantControllerTest {
    @Test
    void shouldAuthorizeAndImportActiveLegacyTenants() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasLegacyTenantImportService service = mock(SaasLegacyTenantImportService.class);
        SaasLegacyTenantController controller = new SaasLegacyTenantController(guard, service);
        Authentication authentication = mock(Authentication.class);
        SaasLegacyImportResult expected = new SaasLegacyImportResult(3, 2, 1);
        when(guard.requireAdmin(authentication)).thenReturn("admin");
        when(service.importActiveTenants("admin")).thenReturn(expected);

        assertThat(controller.importLegacy(authentication).getData()).isSameAs(expected);

        verify(guard).requireAdmin(authentication);
        verify(service).importActiveTenants("admin");
    }
}
