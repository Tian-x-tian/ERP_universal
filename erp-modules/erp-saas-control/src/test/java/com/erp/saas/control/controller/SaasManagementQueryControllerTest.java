package com.erp.saas.control.controller;

import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.management.SaasManagementQueryService;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasManagementQueryControllerTest {
    @Test
    void shouldAuthorizeEveryManagementQuery() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasManagementQueryService service = mock(SaasManagementQueryService.class);
        SaasManagementQueryController controller = new SaasManagementQueryController(guard, service);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("admin");
        when(service.listTenants()).thenReturn(List.of());

        assertThat(controller.tenants(authentication).getData()).isEmpty();

        verify(guard).requireAdmin(authentication);
        verify(service).listTenants();
    }

    @Test
    void shouldAuthorizePlanCatalogDetailQueries() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasManagementQueryService service = mock(SaasManagementQueryService.class);
        SaasManagementQueryController controller = new SaasManagementQueryController(guard, service);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("admin");
        when(service.listFeatures()).thenReturn(List.of());

        assertThat(controller.features(authentication).getData()).isEmpty();
        controller.plan(10L, authentication);

        verify(guard, org.mockito.Mockito.times(2)).requireAdmin(authentication);
        verify(service).listFeatures();
        verify(service).getPlan(10L);
    }
}
