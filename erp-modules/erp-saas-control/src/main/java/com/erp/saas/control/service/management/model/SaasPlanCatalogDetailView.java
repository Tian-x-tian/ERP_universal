package com.erp.saas.control.service.management.model;

import java.util.List;

public record SaasPlanCatalogDetailView(SaasPlanManagementView plan,
        List<SaasPlanFeatureManagementView> features,
        List<SaasPlanQuotaManagementView> quotas) {
}
