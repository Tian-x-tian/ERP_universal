package com.erp.saas.control.service.management;

import com.erp.saas.control.service.management.model.SaasDeploymentManagementView;
import com.erp.saas.control.service.management.model.SaasDomainManagementView;
import com.erp.saas.control.service.management.model.SaasFeatureManagementView;
import com.erp.saas.control.service.management.model.SaasPlanCatalogDetailView;
import com.erp.saas.control.service.management.model.SaasPlanManagementView;
import com.erp.saas.control.service.management.model.SaasTenantManagementView;
import com.erp.saas.control.service.management.model.SaasUsageManagementView;

import java.util.List;

public interface SaasManagementQueryService {
    List<SaasTenantManagementView> listTenants();
    List<SaasPlanManagementView> listPlans();
    List<SaasFeatureManagementView> listFeatures();
    SaasPlanCatalogDetailView getPlan(Long planId);
    List<SaasDomainManagementView> listDomains();
    List<SaasDeploymentManagementView> listDeployments();
    List<SaasUsageManagementView> listUsage();
}
