package com.erp.saas.control.service.purge.model;

import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;

public record SaasTenantPurgeOutcome(String requestId, String tenantId,
        Integer tablesProcessed, Long rowsDeleted, Integer objectsDeleted, Boolean replayed,
        SaasTenantLifecycleView lifecycle) {
}
