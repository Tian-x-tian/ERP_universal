package com.erp.system.saas;

import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;

public interface SaasTenantPurgeService {
    SaasTenantPurgeResult purge(SaasTenantPurgeRequest request);
}
