package com.erp.saas.control.service.lifecycle.model;

import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;

import java.time.LocalDateTime;

public record SaasTenantLifecycleView(
        String tenantId,
        TenantLifecycleState lifecycleState,
        TenantLifecycleState suspendedFromState,
        Long tenantVersion,
        Long subscriptionId,
        SubscriptionState subscriptionState,
        Long planId,
        LocalDateTime startAt,
        LocalDateTime endAt,
        LocalDateTime graceEndAt,
        boolean nonExpiring,
        Long subscriptionVersion,
        LocalDateTime archivedAt,
        LocalDateTime purgeEligibleAt) {
}
