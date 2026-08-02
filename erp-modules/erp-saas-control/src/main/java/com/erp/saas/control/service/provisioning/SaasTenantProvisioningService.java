package com.erp.saas.control.service.provisioning;

import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningResult;

public interface SaasTenantProvisioningService {
    SaasTenantProvisioningResult provision(SaasTenantProvisioningCommand command, String operator);
}
