package com.erp.system.saas;

import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;

public interface SaasTenantDatabasePurgeExecutor {
    SaasTenantPurgeResult purgeDatabase(SaasTenantPurgeRequest request);
}
