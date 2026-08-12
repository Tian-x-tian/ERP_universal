package com.erp.saas.control.service.provisioning;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.SaasProvisioningStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasProvisioningTaskMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.provisioning.impl.SaasTenantProvisioningStateServiceImpl;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantProvisioningStateServiceTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 8, 2, 12, 0);
    private SaasProvisioningTaskMapper taskMapper;
    private SaasTenantMapper tenantMapper;
    private SaasPlanMapper planMapper;
    private SaasDeploymentMapper deploymentMapper;
    private SaasTenantProvisioningStateService service;

    @BeforeEach
    void setUp() {
        taskMapper = mock(SaasProvisioningTaskMapper.class);
        tenantMapper = mock(SaasTenantMapper.class);
        planMapper = mock(SaasPlanMapper.class);
        deploymentMapper = mock(SaasDeploymentMapper.class);
        ControlUtcTime time = mock(ControlUtcTime.class);
        when(time.now()).thenReturn(NOW);
        when(time.operator("admin")).thenReturn("admin");
        service = new SaasTenantProvisioningStateServiceImpl(
                taskMapper, tenantMapper, planMapper, deploymentMapper, time);
    }

    @Test
    void shouldCreateDraftTenantDeploymentAndPendingTaskAtomically() {
        SaasPlanEntity plan = new SaasPlanEntity();
        plan.setPlanId(10L);
        plan.setStatus(PlanStatus.ACTIVE);
        when(planMapper.findActiveByCode("standard")).thenReturn(plan);
        when(tenantMapper.lockByTenantId("tenant-a")).thenReturn(null);
        when(tenantMapper.findBySlugForUpdate("tenant-a")).thenReturn(null);
        doAnswer(invocation -> { ((SaasTenantEntity) invocation.getArgument(0)).setId(1L); return 1; })
                .when(tenantMapper).insert(any(SaasTenantEntity.class));
        doAnswer(invocation -> { ((SaasDeploymentEntity) invocation.getArgument(0)).setDeploymentId(2L); return 1; })
                .when(deploymentMapper).insert(any(SaasDeploymentEntity.class));
        doAnswer(invocation -> { ((SaasProvisioningTaskEntity) invocation.getArgument(0)).setTaskId(3L); return 1; })
                .when(taskMapper).insert(any(SaasProvisioningTaskEntity.class));

        var prepared = service.prepare(command("Company A"), "admin");

        assertThat(prepared.replayed()).isFalse();
        assertThat(prepared.tenant().getLifecycleState()).isEqualTo(TenantLifecycleState.DRAFT);
        assertThat(prepared.deployment().getMode()).isEqualTo(DeploymentMode.SHARED);
        assertThat(prepared.task().getStatus()).isEqualTo(SaasProvisioningStatus.PENDING);
        assertThat(prepared.task().getRequestHash()).hasSize(64);
        assertThat(prepared.task().getPlanId()).isEqualTo(10L);
    }

    @Test
    void shouldReplaySamePayloadAndRejectRequestIdReuseWithDifferentPayload() {
        SaasProvisioningTaskEntity existing = new SaasProvisioningTaskEntity();
        existing.setRequestId("req-1");
        existing.setTenantId("tenant-a");
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId("tenant-a");
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setTenantId("tenant-a");

        SaasPlanEntity plan = new SaasPlanEntity();
        plan.setPlanId(10L);
        plan.setStatus(PlanStatus.ACTIVE);
        when(planMapper.findActiveByCode("standard")).thenReturn(plan);
        when(tenantMapper.lockByTenantId("tenant-a")).thenReturn(null);
        when(tenantMapper.findBySlugForUpdate("tenant-a")).thenReturn(null);
        when(tenantMapper.insert(any())).thenReturn(1);
        when(deploymentMapper.insert(any())).thenReturn(1);
        doAnswer(invocation -> {
            SaasProvisioningTaskEntity task = invocation.getArgument(0);
            existing.setRequestHash(task.getRequestHash());
            existing.setStatus(task.getStatus());
            existing.setPlanId(task.getPlanId());
            existing.setVersionNo(task.getVersionNo());
            return 1;
        }).when(taskMapper).insert(any());
        service.prepare(command("Company A"), "admin");

        when(taskMapper.lockByRequestId("req-1")).thenReturn(existing);
        when(tenantMapper.lockByTenantId("tenant-a")).thenReturn(tenant);
        when(deploymentMapper.findByTenantId("tenant-a")).thenReturn(deployment);
        assertThat(service.prepare(command("Company A"), "admin").replayed()).isTrue();
        assertThatThrownBy(() -> service.prepare(command("Changed Company"), "admin"))
                .isInstanceOf(SaasProvisioningException.class)
                .hasMessageContaining("different payload");
        verify(planMapper, org.mockito.Mockito.times(1)).findActiveByCode("standard");
    }

    @Test
    void shouldPersistOnlyInitializationIdentifiersAndExpiry() {
        SaasProvisioningTaskEntity processing = new SaasProvisioningTaskEntity();
        processing.setRequestId("req-1");
        processing.setTenantId("tenant-a");
        processing.setStatus(SaasProvisioningStatus.PROVISIONING);
        processing.setVersionNo(1L);
        when(taskMapper.lockByRequestId("req-1")).thenReturn(processing);
        when(taskMapper.markInitialized("req-1", 1L, 1L, 2L, 3L, 4L, 5L,
                LocalDateTime.of(2027, 1, 15, 8, 0), "admin", NOW)).thenReturn(1);

        SaasTenantInitializationResult result = new SaasTenantInitializationResult();
        result.setRequestId("req-1");
        result.setTenantId("tenant-a");
        result.setTenantRecordId(1L);
        result.setCompanyId(2L);
        result.setDeptId(3L);
        result.setRoleId(4L);
        result.setUserId(5L);
        result.setActivationToken("must-not-be-persisted");
        result.setActivationExpiresAtEpochMs(1_800_000_000_000L);

        SaasProvisioningTaskEntity updated = service.markInitialized("req-1", 1L, result, "admin");

        assertThat(updated.getStatus()).isEqualTo(SaasProvisioningStatus.INITIALIZED);
        assertThat(updated.getActivationExpiresAt()).isEqualTo(LocalDateTime.of(2027, 1, 15, 8, 0));
        verify(taskMapper, never()).updateById(any());
    }

    @Test
    void shouldReclaimExpiredProvisioningLeaseBeforeReplay() {
        SaasPlanEntity plan = new SaasPlanEntity();
        plan.setPlanId(10L);
        plan.setStatus(PlanStatus.ACTIVE);
        when(planMapper.findActiveByCode("standard")).thenReturn(plan);
        when(tenantMapper.lockByTenantId("tenant-a")).thenReturn(null);
        when(tenantMapper.findBySlugForUpdate("tenant-a")).thenReturn(null);
        when(tenantMapper.insert(any())).thenReturn(1);
        when(deploymentMapper.insert(any())).thenReturn(1);
        java.util.concurrent.atomic.AtomicReference<SaasProvisioningTaskEntity> inserted =
                new java.util.concurrent.atomic.AtomicReference<>();
        doAnswer(invocation -> { inserted.set(invocation.getArgument(0)); return 1; })
                .when(taskMapper).insert(any());
        var first = service.prepare(command("Company A"), "admin");
        SaasProvisioningTaskEntity stale = inserted.get();
        stale.setStatus(SaasProvisioningStatus.PROVISIONING);
        stale.setLeaseUntil(NOW.minusSeconds(1));
        stale.setVersionNo(1L);
        when(taskMapper.lockByRequestId("req-1")).thenReturn(stale);
        when(tenantMapper.lockByTenantId("tenant-a")).thenReturn(first.tenant());
        when(deploymentMapper.findByTenantId("tenant-a")).thenReturn(first.deployment());
        when(taskMapper.reclaimExpired("req-1", 1L, NOW, "admin")).thenReturn(1);

        var replay = service.prepare(command("Company A"), "admin");

        assertThat(replay.task().getStatus()).isEqualTo(SaasProvisioningStatus.FAILED);
        assertThat(replay.task().getLeaseUntil()).isNull();
        assertThat(replay.task().getLastErrorType()).isEqualTo("ProvisioningLeaseExpired");
        assertThat(replay.task().getVersionNo()).isEqualTo(2L);
        verify(taskMapper).reclaimExpired("req-1", 1L, NOW, "admin");
    }

    @Test
    void shouldMarkDeploymentHealthyWhenProvisioningSucceeds() {
        SaasProvisioningTaskEntity initialized = new SaasProvisioningTaskEntity();
        initialized.setRequestId("req-1");
        initialized.setTenantId("tenant-a");
        initialized.setStatus(SaasProvisioningStatus.INITIALIZED);
        initialized.setVersionNo(2L);
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setTenantId("tenant-a");
        deployment.setStatus(DeploymentStatus.REGISTERED);
        deployment.setVersionNo(0L);
        when(taskMapper.lockByRequestId("req-1")).thenReturn(initialized);
        when(deploymentMapper.lockByTenantId("tenant-a")).thenReturn(deployment);
        when(deploymentMapper.updateStatus("tenant-a", 0L, DeploymentStatus.HEALTHY,
                "admin", NOW)).thenReturn(1);
        when(taskMapper.markSucceeded("req-1", 2L, "admin", NOW)).thenReturn(1);

        SaasProvisioningTaskEntity succeeded = service.markSucceeded("req-1", 2L, "admin");

        assertThat(succeeded.getStatus()).isEqualTo(SaasProvisioningStatus.SUCCEEDED);
        verify(deploymentMapper).updateStatus("tenant-a", 0L, DeploymentStatus.HEALTHY,
                "admin", NOW);
    }

    private SaasTenantProvisioningCommand command(String companyName) {
        return new SaasTenantProvisioningCommand("req-1", "tenant-a", "tenant-a", "Tenant A",
                "COMP-A", companyName, "admin", "Tenant Admin", "admin@example.com",
                DeploymentMode.SHARED, "standard", "acme.example", "http://erp-system", null);
    }
}
