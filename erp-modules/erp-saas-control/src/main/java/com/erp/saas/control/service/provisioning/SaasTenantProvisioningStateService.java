package com.erp.saas.control.service.provisioning;

import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.service.provisioning.model.SaasProvisioningPreparation;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;

public interface SaasTenantProvisioningStateService {
    SaasProvisioningPreparation prepare(SaasTenantProvisioningCommand command, String operator);

    SaasProvisioningPreparation load(String requestId);

    SaasProvisioningTaskEntity markProcessing(String requestId, Long expectedVersion, String operator);

    SaasProvisioningTaskEntity markInitialized(String requestId, Long expectedVersion,
            SaasTenantInitializationResult result, String operator);

    SaasProvisioningTaskEntity markSucceeded(String requestId, Long expectedVersion, String operator);

    void markFailed(String requestId, String errorType, String operator);
}
