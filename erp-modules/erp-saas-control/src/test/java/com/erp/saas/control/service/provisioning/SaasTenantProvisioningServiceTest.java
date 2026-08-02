package com.erp.saas.control.service.provisioning;

import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.domain.DomainVerificationState;
import com.erp.saas.control.domain.SaasProvisioningStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.service.domain.SaasDomainService;
import com.erp.saas.control.service.domain.model.SaasDomainView;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.provisioning.impl.SaasTenantProvisioningServiceImpl;
import com.erp.saas.control.service.provisioning.model.SaasProvisioningPreparation;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantProvisioningServiceTest {
    private SaasTenantProvisioningStateService stateService;
    private SaasTenantProvisioningGateway provisioningGateway;
    private SaasTenantLifecycleService lifecycleService;
    private SaasDomainService domainService;
    private SaasTenantProvisioningService service;

    @BeforeEach
    void setUp() {
        stateService = mock(SaasTenantProvisioningStateService.class);
        provisioningGateway = mock(SaasTenantProvisioningGateway.class);
        lifecycleService = mock(SaasTenantLifecycleService.class);
        domainService = mock(SaasDomainService.class);
        service = new SaasTenantProvisioningServiceImpl(
                stateService, provisioningGateway, lifecycleService, domainService);
    }

    @Test
    void shouldProvisionTenantStartTrialAndReturnActivationTokenOnce() {
        SaasTenantProvisioningCommand command = command();
        SaasProvisioningTaskEntity pending = task(SaasProvisioningStatus.PENDING, 0L);
        SaasTenantEntity tenant = tenant(TenantLifecycleState.DRAFT, 0L);
        SaasDeploymentEntity deployment = deployment();
        when(stateService.prepare(command, "admin"))
                .thenReturn(new SaasProvisioningPreparation(pending, tenant, deployment, false));
        when(lifecycleService.beginProvisioning(any())).thenReturn(lifecycle(
                TenantLifecycleState.PROVISIONING, 1L, null));
        when(domainService.register(any())).thenReturn(new SaasDomainView(
                20L, "tenant-a", "acme.example", DomainVerificationState.PENDING,
                DomainVerificationMethod.PLATFORM_MANUAL, null, null, 0L));
        when(domainService.verify(any())).thenReturn(new SaasDomainView(
                20L, "tenant-a", "acme.example", DomainVerificationState.VERIFIED,
                DomainVerificationMethod.PLATFORM_MANUAL, LocalDateTime.now(), null, 1L));
        SaasProvisioningTaskEntity processing = task(SaasProvisioningStatus.PROVISIONING, 1L);
        when(stateService.markProcessing("req-1", 0L, "admin")).thenReturn(processing);
        SaasTenantInitializationResult initialized = initializationResult("activation-raw");
        when(provisioningGateway.initialize(eq(deployment), any(SaasTenantInitializationRequest.class)))
                .thenReturn(initialized);
        SaasProvisioningTaskEntity initializedTask = task(SaasProvisioningStatus.INITIALIZED, 2L);
        copyResult(initializedTask);
        when(stateService.markInitialized("req-1", 1L, initialized, "admin"))
                .thenReturn(initializedTask);
        when(lifecycleService.startTrial(any())).thenReturn(lifecycle(
                TenantLifecycleState.TRIAL, 2L, SubscriptionState.TRIAL));
        SaasProvisioningTaskEntity succeeded = task(SaasProvisioningStatus.SUCCEEDED, 3L);
        copyResult(succeeded);
        when(stateService.markSucceeded("req-1", 2L, "admin")).thenReturn(succeeded);

        var result = service.provision(command, "admin");

        assertThat(result.taskStatus()).isEqualTo(SaasProvisioningStatus.SUCCEEDED);
        assertThat(result.lifecycleState()).isEqualTo(TenantLifecycleState.TRIAL);
        assertThat(result.activationToken()).isEqualTo("activation-raw");
        assertThat(result.replayed()).isFalse();
        verify(provisioningGateway).initialize(any(), any());
    }

    @Test
    void shouldReplayCompletedRequestWithoutRemoteCallOrStoredSecret() {
        SaasProvisioningTaskEntity succeeded = task(SaasProvisioningStatus.SUCCEEDED, 3L);
        copyResult(succeeded);
        when(stateService.prepare(command(), "admin")).thenReturn(new SaasProvisioningPreparation(
                succeeded, tenant(TenantLifecycleState.TRIAL, 2L), deployment(), true));

        var result = service.provision(command(), "admin");

        assertThat(result.activationToken()).isNull();
        assertThat(result.replayed()).isTrue();
        verify(provisioningGateway, never()).initialize(any(), any());
    }

    @Test
    void shouldReissueActivationWhenInitializationReplayCannotReturnOriginalSecret() {
        SaasTenantProvisioningCommand command = command();
        SaasProvisioningTaskEntity pending = task(SaasProvisioningStatus.PENDING, 0L);
        SaasTenantEntity tenant = tenant(TenantLifecycleState.DRAFT, 0L);
        SaasDeploymentEntity deployment = deployment();
        when(stateService.prepare(command, "admin"))
                .thenReturn(new SaasProvisioningPreparation(pending, tenant, deployment, false));
        when(lifecycleService.beginProvisioning(any())).thenReturn(lifecycle(
                TenantLifecycleState.PROVISIONING, 1L, null));
        when(domainService.register(any())).thenReturn(new SaasDomainView(
                20L, "tenant-a", "acme.example", DomainVerificationState.VERIFIED,
                DomainVerificationMethod.PLATFORM_MANUAL, LocalDateTime.now(), null, 1L));
        when(stateService.markProcessing("req-1", 0L, "admin"))
                .thenReturn(task(SaasProvisioningStatus.PROVISIONING, 1L));
        SaasTenantInitializationResult initialized = initializationResult(null);
        initialized.setReplayed(true);
        when(provisioningGateway.initialize(eq(deployment), any(SaasTenantInitializationRequest.class)))
                .thenReturn(initialized);
        when(provisioningGateway.reissueActivation(eq(deployment),
                any(SaasTenantActivationReissueRequest.class)))
                .thenReturn(new SaasTenantActivationReissueResult(
                        "req-1", "tenant-a", 5L, "replacement-token", 1_900_000_000_000L));
        SaasProvisioningTaskEntity initializedTask = task(SaasProvisioningStatus.INITIALIZED, 2L);
        copyResult(initializedTask);
        initializedTask.setActivationExpiresAt(LocalDateTime.of(2030, 3, 17, 17, 46, 40));
        when(stateService.markInitialized(eq("req-1"), eq(1L), any(), eq("admin")))
                .thenReturn(initializedTask);
        when(lifecycleService.startTrial(any())).thenReturn(lifecycle(
                TenantLifecycleState.TRIAL, 2L, SubscriptionState.TRIAL));
        SaasProvisioningTaskEntity succeeded = task(SaasProvisioningStatus.SUCCEEDED, 3L);
        copyResult(succeeded);
        succeeded.setActivationExpiresAt(initializedTask.getActivationExpiresAt());
        when(stateService.markSucceeded("req-1", 2L, "admin")).thenReturn(succeeded);

        var result = service.provision(command, "admin");

        assertThat(result.activationToken()).isEqualTo("replacement-token");
        verify(provisioningGateway).reissueActivation(eq(deployment),
                any(SaasTenantActivationReissueRequest.class));
    }

    @Test
    void shouldReissueActivationWhenResumingCentrallyInitializedTask() {
        SaasProvisioningTaskEntity initialized = task(SaasProvisioningStatus.INITIALIZED, 2L);
        copyResult(initialized);
        SaasTenantEntity tenant = tenant(TenantLifecycleState.PROVISIONING, 1L);
        SaasDeploymentEntity deployment = deployment();
        when(stateService.prepare(command(), "admin")).thenReturn(new SaasProvisioningPreparation(
                initialized, tenant, deployment, true));
        when(provisioningGateway.reissueActivation(eq(deployment),
                any(SaasTenantActivationReissueRequest.class)))
                .thenReturn(new SaasTenantActivationReissueResult(
                        "req-1", "tenant-a", 5L, "resumed-token", 1_900_000_000_000L));
        when(lifecycleService.startTrial(any())).thenReturn(lifecycle(
                TenantLifecycleState.TRIAL, 2L, SubscriptionState.TRIAL));
        SaasProvisioningTaskEntity succeeded = task(SaasProvisioningStatus.SUCCEEDED, 3L);
        copyResult(succeeded);
        when(stateService.markSucceeded("req-1", 2L, "admin")).thenReturn(succeeded);

        var result = service.provision(command(), "admin");

        assertThat(result.activationToken()).isEqualTo("resumed-token");
        assertThat(result.replayed()).isTrue();
        verify(provisioningGateway).reissueActivation(eq(deployment),
                any(SaasTenantActivationReissueRequest.class));
        verify(provisioningGateway, never()).initialize(any(), any());
    }

    @Test
    void shouldMarkTenantAndTaskFailedWhenLocalInitializationFails() {
        SaasProvisioningTaskEntity pending = task(SaasProvisioningStatus.PENDING, 0L);
        SaasTenantEntity tenant = tenant(TenantLifecycleState.DRAFT, 0L);
        when(stateService.prepare(command(), "admin")).thenReturn(new SaasProvisioningPreparation(
                pending, tenant, deployment(), false));
        when(lifecycleService.beginProvisioning(any())).thenReturn(lifecycle(
                TenantLifecycleState.PROVISIONING, 1L, null));
        when(domainService.register(any())).thenReturn(new SaasDomainView(
                20L, "tenant-a", "acme.example", DomainVerificationState.VERIFIED,
                DomainVerificationMethod.PLATFORM_MANUAL, LocalDateTime.now(), null, 1L));
        when(stateService.markProcessing("req-1", 0L, "admin"))
                .thenReturn(task(SaasProvisioningStatus.PROVISIONING, 1L));
        when(provisioningGateway.initialize(any(), any())).thenThrow(new IllegalStateException("unavailable"));
        when(stateService.load("req-1")).thenReturn(new SaasProvisioningPreparation(
                task(SaasProvisioningStatus.PROVISIONING, 1L),
                tenant(TenantLifecycleState.PROVISIONING, 1L), deployment(), true));

        assertThatThrownBy(() -> service.provision(command(), "admin"))
                .isInstanceOf(IllegalStateException.class);

        verify(lifecycleService).markProvisionFailed(any());
        verify(stateService).markFailed("req-1", "IllegalStateException", "admin");
    }

    @Test
    void shouldKeepInitializedCheckpointWhenCompletionFailsAfterTrialStarted() {
        SaasProvisioningTaskEntity pending = task(SaasProvisioningStatus.PENDING, 0L);
        SaasDeploymentEntity deployment = deployment();
        when(stateService.prepare(command(), "admin")).thenReturn(new SaasProvisioningPreparation(
                pending, tenant(TenantLifecycleState.DRAFT, 0L), deployment, false));
        when(lifecycleService.beginProvisioning(any())).thenReturn(lifecycle(
                TenantLifecycleState.PROVISIONING, 1L, null));
        when(domainService.register(any())).thenReturn(new SaasDomainView(
                20L, "tenant-a", "acme.example", DomainVerificationState.VERIFIED,
                DomainVerificationMethod.PLATFORM_MANUAL, LocalDateTime.now(), null, 1L));
        when(stateService.markProcessing("req-1", 0L, "admin"))
                .thenReturn(task(SaasProvisioningStatus.PROVISIONING, 1L));
        SaasTenantInitializationResult initialized = initializationResult("activation-raw");
        when(provisioningGateway.initialize(eq(deployment), any(SaasTenantInitializationRequest.class)))
                .thenReturn(initialized);
        SaasProvisioningTaskEntity checkpoint = task(SaasProvisioningStatus.INITIALIZED, 2L);
        copyResult(checkpoint);
        when(stateService.markInitialized("req-1", 1L, initialized, "admin"))
                .thenReturn(checkpoint);
        when(lifecycleService.startTrial(any())).thenReturn(lifecycle(
                TenantLifecycleState.TRIAL, 2L, SubscriptionState.TRIAL));
        when(stateService.markSucceeded("req-1", 2L, "admin"))
                .thenThrow(new IllegalStateException("concurrent deployment update"));
        when(stateService.load("req-1")).thenReturn(new SaasProvisioningPreparation(
                checkpoint, tenant(TenantLifecycleState.TRIAL, 2L), deployment, true));

        assertThatThrownBy(() -> service.provision(command(), "admin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("concurrent deployment update");

        verify(stateService, never()).markFailed(any(), any(), any());
    }

    private SaasTenantProvisioningCommand command() {
        return new SaasTenantProvisioningCommand("req-1", "tenant-a", "tenant-a", "Tenant A",
                "COMP-A", "Company A", "admin", "Tenant Admin", "admin@example.com",
                DeploymentMode.SHARED, "standard", "acme.example", "http://erp-system", null);
    }

    private SaasProvisioningTaskEntity task(SaasProvisioningStatus status, long version) {
        SaasProvisioningTaskEntity task = new SaasProvisioningTaskEntity();
        task.setTaskId(100L);
        task.setRequestId("req-1");
        task.setTenantId("tenant-a");
        task.setPlanId(10L);
        task.setStatus(status);
        task.setVersionNo(version);
        return task;
    }

    private SaasTenantEntity tenant(TenantLifecycleState state, long version) {
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId("tenant-a");
        tenant.setLifecycleState(state);
        tenant.setVersionNo(version);
        return tenant;
    }

    private SaasDeploymentEntity deployment() {
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setTenantId("tenant-a");
        deployment.setMode(DeploymentMode.SHARED);
        deployment.setStatus(DeploymentStatus.REGISTERED);
        deployment.setDeploymentRef("http://erp-system");
        return deployment;
    }

    private SaasTenantInitializationResult initializationResult(String token) {
        SaasTenantInitializationResult result = new SaasTenantInitializationResult();
        result.setRequestId("req-1");
        result.setTenantId("tenant-a");
        result.setTenantRecordId(1L);
        result.setCompanyId(2L);
        result.setDeptId(3L);
        result.setRoleId(4L);
        result.setUserId(5L);
        result.setActivationToken(token);
        result.setActivationExpiresAtEpochMs(1_800_000_000_000L);
        return result;
    }

    private void copyResult(SaasProvisioningTaskEntity task) {
        task.setTenantRecordId(1L);
        task.setCompanyId(2L);
        task.setDeptId(3L);
        task.setRoleId(4L);
        task.setUserId(5L);
        task.setActivationExpiresAt(LocalDateTime.of(2027, 1, 15, 8, 0));
    }

    private SaasTenantLifecycleView lifecycle(TenantLifecycleState state, long version,
            SubscriptionState subscriptionState) {
        return new SaasTenantLifecycleView("tenant-a", state, null, version,
                subscriptionState == null ? null : 30L, subscriptionState,
                subscriptionState == null ? null : 10L, null, null, null,
                false, subscriptionState == null ? null : 0L, null, null);
    }
}
