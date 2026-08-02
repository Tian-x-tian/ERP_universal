package com.erp.saas.control.service.purge.impl;

import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.lifecycle.SaasLifecycleException;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningGateway;
import com.erp.saas.control.service.purge.SaasTenantPurgeOrchestrator;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeCommand;
import com.erp.saas.control.service.purge.model.SaasTenantPurgeOutcome;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SaasTenantPurgeOrchestratorImpl implements SaasTenantPurgeOrchestrator {
    private static final String PLATFORM_TENANT = "000000";

    private final SaasTenantMapper tenantMapper;
    private final SaasDeploymentMapper deploymentMapper;
    private final SaasTenantProvisioningGateway gateway;
    private final SaasTenantLifecycleService lifecycleService;

    public SaasTenantPurgeOrchestratorImpl(SaasTenantMapper tenantMapper,
            SaasDeploymentMapper deploymentMapper, SaasTenantProvisioningGateway gateway,
            SaasTenantLifecycleService lifecycleService) {
        this.tenantMapper = tenantMapper;
        this.deploymentMapper = deploymentMapper;
        this.gateway = gateway;
        this.lifecycleService = lifecycleService;
    }

    @Override
    public SaasTenantPurgeOutcome purge(SaasTenantPurgeCommand command) {
        if (PLATFORM_TENANT.equals(command.tenantId())) {
            throw invalid("The platform tenant cannot be purged");
        }
        SaasTenantEntity tenant = tenantMapper.findByTenantId(command.tenantId());
        if (tenant == null) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.NOT_FOUND,
                    "Tenant not found");
        }
        if (tenant.getLifecycleState() == TenantLifecycleState.PURGED) {
            return new SaasTenantPurgeOutcome(command.requestId(), command.tenantId(),
                    0, 0L, 0, true, lifecycleView(tenant));
        }
        if (!Objects.equals(tenant.getVersionNo(), command.expectedTenantVersion())) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.VERSION_CONFLICT,
                    "The expected tenant version no longer matches");
        }
        if (tenant.getLifecycleState() != TenantLifecycleState.PURGE_PENDING) {
            throw invalid("Only purge-pending tenants can be deleted");
        }
        SaasDeploymentEntity deployment = deploymentMapper.findByTenantId(command.tenantId());
        if (deployment == null) {
            throw invalid("Tenant deployment is not registered");
        }

        SaasTenantPurgeResult local = gateway.purge(deployment, new SaasTenantPurgeRequest(
                command.requestId(), command.tenantId(), command.confirmationTenantId()));
        SaasTenantLifecycleView lifecycle = lifecycleService.completePurge(new TenantVersionCommand(
                command.tenantId(), command.expectedTenantVersion(), command.operator()));
        return new SaasTenantPurgeOutcome(local.getRequestId(), local.getTenantId(),
                local.getTablesProcessed(), local.getRowsDeleted(), local.getObjectsDeleted(),
                local.isReplayed(), lifecycle);
    }

    private SaasTenantLifecycleView lifecycleView(SaasTenantEntity tenant) {
        return new SaasTenantLifecycleView(tenant.getTenantId(), tenant.getLifecycleState(),
                tenant.getSuspendedFromState(), tenant.getVersionNo(), null, null, null,
                null, null, null, false, null, tenant.getArchivedAt(), tenant.getPurgeEligibleAt());
    }

    private SaasLifecycleException invalid(String message) {
        return new SaasLifecycleException(SaasLifecycleException.ErrorCode.INVALID_TRANSITION, message);
    }
}
