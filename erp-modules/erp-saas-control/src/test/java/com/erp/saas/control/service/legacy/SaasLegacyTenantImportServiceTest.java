package com.erp.saas.control.service.legacy;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.saas.control.service.legacy.impl.SaasLegacyTenantImportServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasLegacyTenantImportServiceTest {
    @Test
    void shouldImportActiveCustomerTenantsAndSkipPlatformTenantIdempotently() {
        InternalSystemClient systemClient = mock(InternalSystemClient.class);
        SaasLegacyTenantImportStateService stateService = mock(SaasLegacyTenantImportStateService.class);
        SaasLegacyTenantImportService service = new SaasLegacyTenantImportServiceImpl(systemClient, stateService);
        PlatformTenantView platform = tenant("000000", "Platform");
        PlatformTenantView first = tenant("tenant-a", "Tenant A");
        PlatformTenantView existing = tenant("tenant-b", "Tenant B");
        when(systemClient.listActiveTenants()).thenReturn(List.of(platform, first, existing));
        when(stateService.importTenant(first, "admin")).thenReturn(true);
        when(stateService.importTenant(existing, "admin")).thenReturn(false);

        SaasLegacyImportResult result = service.importActiveTenants("admin");

        assertThat(result.discovered()).isEqualTo(3);
        assertThat(result.imported()).isEqualTo(1);
        assertThat(result.skipped()).isEqualTo(2);
        verify(stateService).importTenant(first, "admin");
        verify(stateService).importTenant(existing, "admin");
    }

    private PlatformTenantView tenant(String tenantId, String name) {
        PlatformTenantView tenant = new PlatformTenantView();
        tenant.setTenantId(tenantId);
        tenant.setTenantName(name);
        tenant.setStatus("0");
        tenant.setDelFlag("0");
        return tenant;
    }
}
