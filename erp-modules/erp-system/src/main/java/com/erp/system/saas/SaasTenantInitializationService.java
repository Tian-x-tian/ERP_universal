package com.erp.system.saas;

import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;

public interface SaasTenantInitializationService {
    SaasTenantInitializationResult initialize(SaasTenantInitializationRequest request);
}
