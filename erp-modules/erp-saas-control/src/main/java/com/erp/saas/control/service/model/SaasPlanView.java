package com.erp.saas.control.service.model;

import com.erp.saas.control.domain.PlanStatus;

public record SaasPlanView(
        Long planId,
        String planCode,
        Integer planVersion,
        String planName,
        PlanStatus status,
        Integer trialDays,
        Integer graceDays,
        String description,
        Long versionNo) {
}
