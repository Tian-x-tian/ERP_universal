package com.erp.saas.control.service;

import com.erp.saas.control.service.model.FeatureDefinitionCommand;
import com.erp.saas.control.service.model.PlanDraftCommand;
import com.erp.saas.control.service.model.PlanFeatureGrantCommand;
import com.erp.saas.control.service.model.PlanQuotaCommand;
import com.erp.saas.control.service.model.PublishPlanCommand;
import com.erp.saas.control.service.model.SaasFeatureView;
import com.erp.saas.control.service.model.SaasPlanView;

import java.util.List;

public interface SaasPlanCatalogService {
    SaasPlanView createDraft(PlanDraftCommand command, String operator);

    SaasPlanView updateDraft(Long planId, Long expectedVersion, PlanDraftCommand command, String operator);

    SaasFeatureView defineFeature(FeatureDefinitionCommand command, String operator);

    SaasFeatureView updateFeature(
            Long featureId, Long expectedVersion, FeatureDefinitionCommand command, String operator);

    SaasPlanView replaceDraftFeatures(
            Long planId, Long expectedVersion, List<PlanFeatureGrantCommand> grants, String operator);

    SaasPlanView replaceDraftQuotas(
            Long planId, Long expectedVersion, List<PlanQuotaCommand> quotas, String operator);

    SaasPlanView publish(PublishPlanCommand command, String operator);
}
