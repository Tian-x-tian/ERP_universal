package com.erp.saas.control.service.provisioning;

import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;

public interface SaasTenantProvisioningGateway {
    SaasTenantInitializationResult initialize(SaasDeploymentEntity deployment,
            SaasTenantInitializationRequest request);

    SaasTenantActivationReissueResult reissueActivation(SaasDeploymentEntity deployment,
            SaasTenantActivationReissueRequest request);

    SaasTenantPurgeResult purge(SaasDeploymentEntity deployment, SaasTenantPurgeRequest request);
}
