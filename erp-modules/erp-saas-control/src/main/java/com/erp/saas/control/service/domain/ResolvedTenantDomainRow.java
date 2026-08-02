package com.erp.saas.control.service.domain;

import com.erp.saas.contract.model.TenantLifecycleState;
import lombok.Data;

@Data
public class ResolvedTenantDomainRow {
    private Long domainId;
    private String tenantId;
    private String host;
    private TenantLifecycleState lifecycleState;
}
