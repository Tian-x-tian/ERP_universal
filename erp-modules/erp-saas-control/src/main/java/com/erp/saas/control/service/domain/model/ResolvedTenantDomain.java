package com.erp.saas.control.service.domain.model;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.TenantLifecycleState;

public record ResolvedTenantDomain(Long domainId, String tenantId, String host,
        DeploymentMode deploymentMode, TenantLifecycleState lifecycleState) { }
