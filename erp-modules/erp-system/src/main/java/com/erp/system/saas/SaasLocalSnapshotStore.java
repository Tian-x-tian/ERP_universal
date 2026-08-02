package com.erp.system.saas;

import com.erp.saas.contract.model.SaasEntitlementSnapshot;

import java.time.LocalDateTime;

public interface SaasLocalSnapshotStore {
    SaasEntitlementSnapshot load(String tenantId);

    void save(SaasEntitlementSnapshot snapshot, String operator, LocalDateTime now);
}
