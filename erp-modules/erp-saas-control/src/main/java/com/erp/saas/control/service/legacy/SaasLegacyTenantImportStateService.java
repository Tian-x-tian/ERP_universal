package com.erp.saas.control.service.legacy;

import com.erp.platform.contract.model.PlatformTenantView;

public interface SaasLegacyTenantImportStateService {
    boolean importTenant(PlatformTenantView source, String operator);
}
