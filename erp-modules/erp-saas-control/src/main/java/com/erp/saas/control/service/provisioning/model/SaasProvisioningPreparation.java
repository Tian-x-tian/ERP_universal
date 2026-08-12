package com.erp.saas.control.service.provisioning.model;

import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;

public record SaasProvisioningPreparation(
        SaasProvisioningTaskEntity task,
        SaasTenantEntity tenant,
        SaasDeploymentEntity deployment,
        boolean replayed) {
}
