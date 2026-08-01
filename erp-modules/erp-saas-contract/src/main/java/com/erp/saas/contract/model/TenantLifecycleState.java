package com.erp.saas.contract.model;

public enum TenantLifecycleState {
    DRAFT,
    PROVISIONING,
    TRIAL,
    ACTIVE,
    GRACE,
    READ_ONLY,
    ARCHIVED,
    PURGE_PENDING,
    PURGED,
    SUSPENDED,
    PROVISION_FAILED
}
