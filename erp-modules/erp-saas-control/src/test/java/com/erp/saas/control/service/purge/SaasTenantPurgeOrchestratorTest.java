package com.erp.saas.control.service.purge;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningGateway;
import com.erp.saas.control.service.purge.impl.SaasTenantPurgeOrchestratorImpl;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeCommand;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;

class SaasTenantPurgeOrchestratorTest {
    @Test
    void shouldDeleteLocalDataBeforeMarkingControlTenantPurged() {
        SaasTenantMapper tenantMapper = mock(SaasTenantMapper.class);
        SaasDeploymentMapper deploymentMapper = mock(SaasDeploymentMapper.class);
        SaasTenantProvisioningGateway gateway = mock(SaasTenantProvisioningGateway.class);
        SaasTenantLifecycleService lifecycleService = mock(SaasTenantLifecycleService.class);
        SaasTenantPurgeOrchestrator orchestrator = new SaasTenantPurgeOrchestratorImpl(
                tenantMapper, deploymentMapper, gateway, lifecycleService);
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId("tenant-a");
        tenant.setLifecycleState(TenantLifecycleState.PURGE_PENDING);
        tenant.setVersionNo(7L);
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setMode(DeploymentMode.SHARED);
        when(tenantMapper.findByTenantId("tenant-a")).thenReturn(tenant);
        when(deploymentMapper.findByTenantId("tenant-a")).thenReturn(deployment);
        SaasTenantPurgeRequest request = new SaasTenantPurgeRequest("purge-001", "tenant-a", "tenant-a");
        when(gateway.purge(eq(deployment), any(SaasTenantPurgeRequest.class))).thenReturn(
                new SaasTenantPurgeResult("purge-001", "tenant-a", 12, 34L, false));
        SaasTenantLifecycleView lifecycle = new SaasTenantLifecycleView("tenant-a",
                TenantLifecycleState.PURGED, null, 8L, null, null, null,
                null, null, null, false, null, null, null);
        TenantVersionCommand versionCommand = new TenantVersionCommand("tenant-a", 7L, "admin");
        when(lifecycleService.completePurge(versionCommand)).thenReturn(lifecycle);

        var outcome = orchestrator.purge(new SaasTenantPurgeCommand(
                "purge-001", "tenant-a", 7L, "tenant-a", "admin"));

        assertThat(outcome.rowsDeleted()).isEqualTo(34L);
        assertThat(outcome.lifecycle().lifecycleState()).isEqualTo(TenantLifecycleState.PURGED);
        var ordered = inOrder(gateway, lifecycleService);
        ordered.verify(gateway).purge(eq(deployment), argThat(actual ->
                request.getRequestId().equals(actual.getRequestId())
                        && request.getTenantId().equals(actual.getTenantId())
                        && request.getConfirmationTenantId().equals(actual.getConfirmationTenantId())));
        ordered.verify(lifecycleService).completePurge(versionCommand);
    }
}
