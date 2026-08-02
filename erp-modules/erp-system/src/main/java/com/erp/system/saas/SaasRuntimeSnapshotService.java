package com.erp.system.saas;

public interface SaasRuntimeSnapshotService {
    SaasRuntimeEntitlements refresh(String tenantId);

    SaasRuntimeEntitlements current(String tenantId);
}
