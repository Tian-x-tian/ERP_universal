package com.erp.saas.control.service.purge;

import com.erp.saas.control.service.purge.model.SaasTenantPurgeCommand;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeOutcome;

public interface SaasTenantPurgeOrchestrator {
    SaasTenantPurgeOutcome purge(SaasTenantPurgeCommand command);
}
