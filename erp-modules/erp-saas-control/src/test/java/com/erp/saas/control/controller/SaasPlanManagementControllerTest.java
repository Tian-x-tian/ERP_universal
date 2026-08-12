package com.erp.saas.control.controller;

import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.security.PlatformSaasAdminGuard;
import com.erp.saas.control.service.SaasPlanCatalogService;
import com.erp.saas.control.service.model.FeatureDefinitionCommand;
import com.erp.saas.control.service.model.PlanDraftCommand;
import com.erp.saas.control.service.model.PlanFeatureGrantCommand;
import com.erp.saas.control.service.model.PlanQuotaCommand;
import com.erp.saas.control.service.model.PublishPlanCommand;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasPlanManagementControllerTest {
    @Test
    void shouldUseAuthenticatedOperatorForCatalogChanges() {
        PlatformSaasAdminGuard guard = mock(PlatformSaasAdminGuard.class);
        SaasPlanCatalogService service = mock(SaasPlanCatalogService.class);
        Authentication authentication = mock(Authentication.class);
        when(guard.requireAdmin(authentication)).thenReturn("platform-admin");
        SaasPlanManagementController controller = new SaasPlanManagementController(guard, service);

        PlanDraftCommand draft = new PlanDraftCommand("standard", 1, "Standard", 14, 7, "Default plan");
        controller.createDraft(draft, authentication);
        controller.replaceFeatures(10L,
                new SaasPlanManagementController.ReplaceFeaturesRequest(0L,
                        List.of(new PlanFeatureGrantCommand("business.order", true))),
                authentication);
        controller.replaceQuotas(10L,
                new SaasPlanManagementController.ReplaceQuotasRequest(1L,
                        List.of(new PlanQuotaCommand("user_count", 20L,
                                com.erp.saas.control.domain.QuotaPeriodType.CURRENT))),
                authentication);
        controller.publish(10L,
                new SaasPlanManagementController.PublishRequest(2L, null, null), authentication);
        FeatureDefinitionCommand feature = new FeatureDefinitionCommand(
                "business.order", "Order management", FeatureStatus.ACTIVE, null);
        controller.defineFeature(feature, authentication);

        verify(service).createDraft(draft, "platform-admin");
        verify(service).replaceDraftFeatures(10L, 0L,
                List.of(new PlanFeatureGrantCommand("business.order", true)), "platform-admin");
        verify(service).replaceDraftQuotas(10L, 1L,
                List.of(new PlanQuotaCommand("user_count", 20L,
                        com.erp.saas.control.domain.QuotaPeriodType.CURRENT)), "platform-admin");
        verify(service).publish(new PublishPlanCommand(10L, 2L, null, null), "platform-admin");
        verify(service).defineFeature(feature, "platform-admin");
    }
}
