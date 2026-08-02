package com.erp.saas.control.service.snapshot;

import com.erp.saas.contract.model.SaasEntitlementSnapshot;

public interface SaasEntitlementSnapshotService {
    SaasEntitlementSnapshot load(String tenantId, String operator);
}
