package com.erp.saas.control.service.legacy;

import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.legacy.impl.SaasLegacyTenantImportStateServiceImpl;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasLegacyTenantImportStateServiceTest {
    @Test
    void shouldCreateActiveTenantUnlimitedSubscriptionAndSharedDeploymentAtomically() {
        SaasTenantMapper tenantMapper = mock(SaasTenantMapper.class);
        SaasPlanMapper planMapper = mock(SaasPlanMapper.class);
        SaasSubscriptionMapper subscriptionMapper = mock(SaasSubscriptionMapper.class);
        SaasDeploymentMapper deploymentMapper = mock(SaasDeploymentMapper.class);
        ControlUtcTime time = mock(ControlUtcTime.class);
        LocalDateTime now = LocalDateTime.of(2026, 8, 2, 12, 0);
        when(time.operator("admin")).thenReturn("admin");
        when(time.now()).thenReturn(now);
        SaasPlanEntity plan = new SaasPlanEntity();
        plan.setPlanId(99L);
        plan.setStatus(PlanStatus.ACTIVE);
        when(planMapper.findActiveByCode("legacy-full-access")).thenReturn(plan);
        when(tenantMapper.insert(any())).thenReturn(1);
        when(subscriptionMapper.insert(any())).thenReturn(1);
        when(deploymentMapper.insert(any())).thenReturn(1);
        SaasLegacyTenantImportStateService service = new SaasLegacyTenantImportStateServiceImpl(
                tenantMapper, planMapper, subscriptionMapper, deploymentMapper, time,
                "http://erp-system");

        assertThat(service.importTenant(tenant(), "admin")).isTrue();

        ArgumentCaptor<SaasTenantEntity> tenant = ArgumentCaptor.forClass(SaasTenantEntity.class);
        ArgumentCaptor<SaasSubscriptionEntity> subscription = ArgumentCaptor.forClass(SaasSubscriptionEntity.class);
        ArgumentCaptor<SaasDeploymentEntity> deployment = ArgumentCaptor.forClass(SaasDeploymentEntity.class);
        verify(tenantMapper).insert(tenant.capture());
        verify(subscriptionMapper).insert(subscription.capture());
        verify(deploymentMapper).insert(deployment.capture());
        assertThat(tenant.getValue().getLifecycleState()).isEqualTo(TenantLifecycleState.ACTIVE);
        assertThat(tenant.getValue().getSlug()).isEqualTo("legacy-tenant-a");
        assertThat(subscription.getValue().getState()).isEqualTo(SubscriptionState.ACTIVE);
        assertThat(subscription.getValue().getNonExpiring()).isTrue();
        assertThat(subscription.getValue().getEndAt()).isNull();
        assertThat(deployment.getValue().getMode()).isEqualTo(DeploymentMode.SHARED);
        assertThat(deployment.getValue().getStatus()).isEqualTo(DeploymentStatus.REGISTERED);
        assertThat(deployment.getValue().getSecretRef()).isNull();
    }

    private PlatformTenantView tenant() {
        PlatformTenantView tenant = new PlatformTenantView();
        tenant.setTenantId("tenant-a");
        tenant.setTenantName("Tenant A");
        tenant.setStatus("0");
        tenant.setDelFlag("0");
        return tenant;
    }
}
