package com.erp.saas.control.service.management.model;

import com.erp.saas.control.domain.PlanStatus;

import java.time.LocalDateTime;

public record SaasPlanManagementView(Long planId, String planCode, Integer planVersion,
        String planName, PlanStatus status, Integer trialDays, Integer graceDays,
        String description, Long versionNo, LocalDateTime updateTime) {
}
