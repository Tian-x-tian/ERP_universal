package com.erp.saas.control.service.management;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.SaasProvisioningStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasDomainMapper;
import com.erp.saas.control.mapper.SaasFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasPlanQuotaMapper;
import com.erp.saas.control.mapper.SaasProvisioningTaskMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.mapper.SaasUsageSummaryMapper;
import com.erp.saas.control.service.management.impl.SaasManagementQueryServiceImpl;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SaasManagementQueryServiceTest {
    @Test
    void shouldAggregateTenantSubscriptionDeploymentAndProvisioningStatus() {
        SaasTenantMapper tenantMapper = mock(SaasTenantMapper.class);
        SaasSubscriptionMapper subscriptionMapper = mock(SaasSubscriptionMapper.class);
        SaasPlanMapper planMapper = mock(SaasPlanMapper.class);
        SaasDeploymentMapper deploymentMapper = mock(SaasDeploymentMapper.class);
        SaasProvisioningTaskMapper taskMapper = mock(SaasProvisioningTaskMapper.class);
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId("tenant-a");
        tenant.setSlug("tenant-a");
        tenant.setTenantName("Tenant A");
        tenant.setLifecycleState(TenantLifecycleState.TRIAL);
        when(tenantMapper.selectList(null)).thenReturn(List.of(tenant));
        SaasSubscriptionEntity subscription = new SaasSubscriptionEntity();
        subscription.setPlanId(10L);
        subscription.setState(SubscriptionState.TRIAL);
        when(subscriptionMapper.findLatestByTenantId("tenant-a")).thenReturn(subscription);
        SaasPlanEntity plan = new SaasPlanEntity();
        plan.setPlanId(10L);
        plan.setPlanCode("standard");
        plan.setPlanName("Standard");
        plan.setStatus(PlanStatus.ACTIVE);
        when(planMapper.selectById(10L)).thenReturn(plan);
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setMode(DeploymentMode.SHARED);
        deployment.setStatus(DeploymentStatus.HEALTHY);
        when(deploymentMapper.findByTenantId("tenant-a")).thenReturn(deployment);
        SaasProvisioningTaskEntity task = new SaasProvisioningTaskEntity();
        task.setStatus(SaasProvisioningStatus.SUCCEEDED);
        when(taskMapper.findByTenantId("tenant-a")).thenReturn(task);
        SaasManagementQueryService service = new SaasManagementQueryServiceImpl(
                tenantMapper, subscriptionMapper, planMapper,
                mock(SaasFeatureMapper.class), mock(SaasPlanFeatureMapper.class),
                mock(SaasPlanQuotaMapper.class), deploymentMapper, taskMapper,
                mock(SaasDomainMapper.class), mock(SaasUsageSummaryMapper.class));

        var result = service.listTenants();

        assertThat(result).singleElement().satisfies(view -> {
            assertThat(view.tenantId()).isEqualTo("tenant-a");
            assertThat(view.planCode()).isEqualTo("standard");
            assertThat(view.subscriptionState()).isEqualTo(SubscriptionState.TRIAL);
            assertThat(view.deploymentMode()).isEqualTo(DeploymentMode.SHARED);
            assertThat(view.provisioningStatus()).isEqualTo(SaasProvisioningStatus.SUCCEEDED);
        });
    }
}
