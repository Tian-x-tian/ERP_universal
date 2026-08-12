package com.erp.saas.control.service.legacy.impl;

import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.saas.control.service.legacy.SaasLegacyImportResult;
import com.erp.saas.control.service.legacy.SaasLegacyTenantImportService;
import com.erp.saas.control.service.legacy.SaasLegacyTenantImportStateService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class SaasLegacyTenantImportServiceImpl implements SaasLegacyTenantImportService {
    private static final String PLATFORM_TENANT_ID = "000000";

    private final InternalSystemClient systemClient;
    private final SaasLegacyTenantImportStateService stateService;

    public SaasLegacyTenantImportServiceImpl(InternalSystemClient systemClient,
            SaasLegacyTenantImportStateService stateService) {
        this.systemClient = Objects.requireNonNull(systemClient);
        this.stateService = Objects.requireNonNull(stateService);
    }

    @Override
    public SaasLegacyImportResult importActiveTenants(String operator) {
        List<PlatformTenantView> sources = systemClient.listActiveTenants();
        int discovered = sources.size();
        int imported = 0;
        for (PlatformTenantView source : sources) {
            if (source != null && !PLATFORM_TENANT_ID.equals(source.getTenantId())
                    && stateService.importTenant(source, operator)) {
                imported++;
            }
        }
        return new SaasLegacyImportResult(discovered, imported, discovered - imported);
    }
}
