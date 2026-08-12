package com.erp.saas.control.service.lifecycle;

import com.erp.saas.control.service.lifecycle.model.ActivateSubscriptionCommand;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.lifecycle.model.StartTrialCommand;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;

public interface SaasTenantLifecycleService {
    SaasTenantLifecycleView beginProvisioning(TenantVersionCommand command);

    SaasTenantLifecycleView markProvisionFailed(TenantVersionCommand command);

    SaasTenantLifecycleView startTrial(StartTrialCommand command);

    SaasTenantLifecycleView activate(ActivateSubscriptionCommand command);

    SaasTenantLifecycleView reconcile(String tenantId, String operator);

    SaasTenantLifecycleView suspend(TenantVersionCommand command);

    SaasTenantLifecycleView resume(TenantVersionCommand command);

    SaasTenantLifecycleView archive(TenantVersionCommand command);

    SaasTenantLifecycleView markPurgePending(TenantVersionCommand command);

    SaasTenantLifecycleView completePurge(TenantVersionCommand command);
}
