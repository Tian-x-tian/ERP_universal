package com.erp.saas.control.service.provisioning.impl;

import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.domain.DomainVerificationState;
import com.erp.saas.control.domain.SaasProvisioningStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.service.domain.SaasDomainService;
import com.erp.saas.control.service.domain.model.RegisterDomainCommand;
import com.erp.saas.control.service.domain.model.SaasDomainView;
import com.erp.saas.control.service.domain.model.VerifyDomainCommand;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.lifecycle.model.StartTrialCommand;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import com.erp.saas.control.service.provisioning.SaasProvisioningException;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningGateway;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningService;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningStateService;
import com.erp.saas.control.service.provisioning.model.SaasProvisioningPreparation;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningResult;
import org.springframework.stereotype.Service;

import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class SaasTenantProvisioningServiceImpl implements SaasTenantProvisioningService {
    private final SaasTenantProvisioningStateService stateService;
    private final SaasTenantProvisioningGateway provisioningGateway;
    private final SaasTenantLifecycleService lifecycleService;
    private final SaasDomainService domainService;

    public SaasTenantProvisioningServiceImpl(SaasTenantProvisioningStateService stateService,
            SaasTenantProvisioningGateway provisioningGateway,
            SaasTenantLifecycleService lifecycleService,
            SaasDomainService domainService) {
        this.stateService = stateService;
        this.provisioningGateway = provisioningGateway;
        this.lifecycleService = lifecycleService;
        this.domainService = domainService;
    }

    @Override
    public SaasTenantProvisioningResult provision(SaasTenantProvisioningCommand command, String operator) {
        Objects.requireNonNull(command, "command must not be null");
        SaasProvisioningPreparation preparation = stateService.prepare(command, operator);
        SaasProvisioningTaskEntity task = preparation.task();
        if (task.getStatus() == SaasProvisioningStatus.SUCCEEDED) {
            return result(task, preparation.tenant().getLifecycleState(), null, true);
        }
        if (task.getStatus() == SaasProvisioningStatus.PROVISIONING) {
            return result(task, preparation.tenant().getLifecycleState(), null, true);
        }
        if (task.getStatus() == SaasProvisioningStatus.INITIALIZED) {
            SaasTenantInitializationResult replacement = ensureActivationToken(command,
                    preparation.deployment(), initializationResult(task));
            return completeInitialized(preparation, operator, replacement, true);
        }

        try {
            SaasTenantLifecycleView provisioning = beginProvisioning(preparation, operator);
            ensureVerifiedDomain(command, operator);
            task = stateService.markProcessing(command.requestId(), task.getVersionNo(), operator);
            SaasTenantInitializationResult initialized = provisioningGateway.initialize(
                    preparation.deployment(), initializationRequest(command));
            initialized = ensureActivationToken(command, preparation.deployment(), initialized);
            task = stateService.markInitialized(command.requestId(), task.getVersionNo(), initialized, operator);
            SaasProvisioningPreparation initializedPreparation = new SaasProvisioningPreparation(
                    task, preparation.tenant(), preparation.deployment(), preparation.replayed());
            return completeInitialized(initializedPreparation, operator, initialized, false,
                    provisioning.tenantVersion());
        } catch (RuntimeException error) {
            fail(command.requestId(), operator, error);
            throw error;
        }
    }

    private SaasTenantLifecycleView beginProvisioning(SaasProvisioningPreparation preparation, String operator) {
        TenantLifecycleState state = preparation.tenant().getLifecycleState();
        if (state == TenantLifecycleState.PROVISIONING) {
            return lifecycle(preparation.tenant().getTenantId(), state, preparation.tenant().getVersionNo());
        }
        if (state != TenantLifecycleState.DRAFT && state != TenantLifecycleState.PROVISION_FAILED) {
            throw new IllegalStateException("Tenant cannot resume provisioning from state " + state);
        }
        return lifecycleService.beginProvisioning(new TenantVersionCommand(
                preparation.tenant().getTenantId(), preparation.tenant().getVersionNo(), operator));
    }

    private void ensureVerifiedDomain(SaasTenantProvisioningCommand command, String operator) {
        SaasDomainView domain = domainService.register(new RegisterDomainCommand(
                command.tenantId(), command.host(), DomainVerificationMethod.PLATFORM_MANUAL, operator));
        if (domain.verificationState() == DomainVerificationState.PENDING) {
            domainService.verify(new VerifyDomainCommand(domain.domainId(), domain.versionNo(), operator));
        }
    }

    private SaasTenantProvisioningResult completeInitialized(SaasProvisioningPreparation preparation,
            String operator, SaasTenantInitializationResult initialized, boolean replayed) {
        return completeInitialized(preparation, operator, initialized, replayed,
                preparation.tenant().getVersionNo());
    }

    private SaasTenantProvisioningResult completeInitialized(SaasProvisioningPreparation preparation,
            String operator, SaasTenantInitializationResult initialized, boolean replayed,
            Long expectedTenantVersion) {
        TenantLifecycleState current = preparation.tenant().getLifecycleState();
        SaasTenantLifecycleView lifecycle;
        if (current == TenantLifecycleState.PROVISIONING || current == TenantLifecycleState.DRAFT
                || current == TenantLifecycleState.PROVISION_FAILED) {
            lifecycle = lifecycleService.startTrial(new StartTrialCommand(
                    preparation.task().getTenantId(), preparation.task().getPlanId(),
                    expectedTenantVersion, operator));
        } else {
            lifecycle = lifecycle(preparation.task().getTenantId(), current,
                    preparation.tenant().getVersionNo());
        }
        SaasProvisioningTaskEntity succeeded = stateService.markSucceeded(
                preparation.task().getRequestId(), preparation.task().getVersionNo(), operator);
        return result(succeeded, lifecycle.lifecycleState(),
                initialized == null ? null : initialized.getActivationToken(), replayed);
    }

    private void fail(String requestId, String operator, RuntimeException original) {
        try {
            SaasProvisioningPreparation latest = stateService.load(requestId);
            if (latest.tenant().getLifecycleState() == TenantLifecycleState.PROVISIONING) {
                lifecycleService.markProvisionFailed(new TenantVersionCommand(
                        latest.tenant().getTenantId(), latest.tenant().getVersionNo(), operator));
            }
            if (latest.task().getStatus() != SaasProvisioningStatus.INITIALIZED) {
                stateService.markFailed(requestId, original.getClass().getSimpleName(), operator);
            }
        } catch (RuntimeException cleanupError) {
            original.addSuppressed(cleanupError);
        }
    }

    private SaasTenantInitializationRequest initializationRequest(SaasTenantProvisioningCommand command) {
        SaasTenantInitializationRequest request = new SaasTenantInitializationRequest();
        request.setRequestId(command.requestId());
        request.setTenantId(command.tenantId());
        request.setTenantName(command.tenantName());
        request.setCompanyCode(command.companyCode());
        request.setCompanyName(command.companyName());
        request.setAdminUsername(command.adminUsername());
        request.setAdminDisplayName(command.adminDisplayName());
        request.setAdminEmail(command.adminEmail());
        return request;
    }

    private SaasTenantInitializationResult initializationResult(SaasProvisioningTaskEntity task) {
        SaasTenantInitializationResult result = new SaasTenantInitializationResult();
        result.setRequestId(task.getRequestId());
        result.setTenantId(task.getTenantId());
        result.setTenantRecordId(task.getTenantRecordId());
        result.setCompanyId(task.getCompanyId());
        result.setDeptId(task.getDeptId());
        result.setRoleId(task.getRoleId());
        result.setUserId(task.getUserId());
        result.setActivationExpiresAtEpochMs(task.getActivationExpiresAt() == null ? 0L
                : task.getActivationExpiresAt().toInstant(ZoneOffset.UTC).toEpochMilli());
        result.setReplayed(true);
        return result;
    }

    private SaasTenantInitializationResult ensureActivationToken(SaasTenantProvisioningCommand command,
            SaasDeploymentEntity deployment,
            SaasTenantInitializationResult initialized) {
        if (initialized != null && initialized.getActivationToken() != null
                && !initialized.getActivationToken().isBlank()) {
            return initialized;
        }
        if (initialized == null) {
            throw provisioningConflict("System tenant initialization returned no result");
        }
        SaasTenantActivationReissueResult replacement = provisioningGateway.reissueActivation(deployment,
                new SaasTenantActivationReissueRequest(command.requestId(), command.tenantId()));
        if (replacement == null
                || !command.requestId().equals(replacement.getRequestId())
                || !command.tenantId().equals(replacement.getTenantId())
                || !Objects.equals(initialized.getUserId(), replacement.getUserId())
                || replacement.getActivationToken() == null
                || replacement.getActivationToken().isBlank()
                || replacement.getActivationExpiresAtEpochMs() <= 0L) {
            throw provisioningConflict("Activation reissue result does not match the provisioning request");
        }
        initialized.setActivationToken(replacement.getActivationToken());
        initialized.setActivationExpiresAtEpochMs(replacement.getActivationExpiresAtEpochMs());
        return initialized;
    }

    private SaasProvisioningException provisioningConflict(String message) {
        return new SaasProvisioningException(SaasProvisioningException.ErrorCode.CONFLICT, message);
    }

    private SaasTenantProvisioningResult result(SaasProvisioningTaskEntity task,
            TenantLifecycleState lifecycleState, String activationToken, boolean replayed) {
        Long expiresAt = task.getActivationExpiresAt() == null ? null
                : task.getActivationExpiresAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        return new SaasTenantProvisioningResult(task.getRequestId(), task.getTenantId(), task.getStatus(),
                lifecycleState, task.getTenantRecordId(), task.getCompanyId(), task.getDeptId(),
                task.getRoleId(), task.getUserId(), activationToken, expiresAt, replayed);
    }

    private SaasTenantLifecycleView lifecycle(String tenantId, TenantLifecycleState state, Long version) {
        return new SaasTenantLifecycleView(tenantId, state, null, version,
                null, null, null, null, null, null, false, null, null, null);
    }
}
