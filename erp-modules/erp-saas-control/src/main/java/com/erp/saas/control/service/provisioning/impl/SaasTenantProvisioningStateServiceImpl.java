package com.erp.saas.control.service.provisioning.impl;

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
import com.erp.saas.control.service.provisioning.SaasProvisioningException;
import com.erp.saas.control.service.provisioning.SaasTenantProvisioningStateService;
import com.erp.saas.control.service.provisioning.model.SaasProvisioningPreparation;
import com.erp.saas.control.service.provisioning.model.SaasTenantProvisioningCommand;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.Objects;

@Service
public class SaasTenantProvisioningStateServiceImpl implements SaasTenantProvisioningStateService {
    private static final long LEASE_MINUTES = 5L;

    private final SaasProvisioningTaskMapper taskMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasPlanMapper planMapper;
    private final SaasDeploymentMapper deploymentMapper;
    private final ControlUtcTime time;

    public SaasTenantProvisioningStateServiceImpl(SaasProvisioningTaskMapper taskMapper,
            SaasTenantMapper tenantMapper, SaasPlanMapper planMapper,
            SaasDeploymentMapper deploymentMapper, ControlUtcTime time) {
        this.taskMapper = taskMapper;
        this.tenantMapper = tenantMapper;
        this.planMapper = planMapper;
        this.deploymentMapper = deploymentMapper;
        this.time = time;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasProvisioningPreparation prepare(SaasTenantProvisioningCommand command, String operator) {
        Objects.requireNonNull(command, "command must not be null");
        operator = time.operator(operator);
        String requestHash = requestHash(command);
        SaasProvisioningTaskEntity existing = taskMapper.lockByRequestId(command.requestId());
        if (existing != null) {
            if (!requestHash.equals(existing.getRequestHash())) {
                throw conflict("requestId was already used with a different payload", null);
            }
            reclaimExpired(existing, operator);
            return loadExisting(existing, true);
        }
        if (tenantMapper.lockByTenantId(command.tenantId()) != null) {
            throw conflict("tenantId is already registered", null);
        }
        if (tenantMapper.findBySlugForUpdate(command.slug()) != null) {
            throw conflict("slug is already registered", null);
        }
        SaasPlanEntity plan = planMapper.findActiveByCode(command.planCode());
        if (plan == null || plan.getStatus() != PlanStatus.ACTIVE) {
            throw new SaasProvisioningException(SaasProvisioningException.ErrorCode.PLAN_NOT_ACTIVE,
                    "Active plan not found");
        }
        LocalDateTime now = time.now();
        SaasTenantEntity tenant = tenant(command, operator, now);
        SaasDeploymentEntity deployment = deployment(command, operator, now);
        SaasProvisioningTaskEntity task = task(command, plan.getPlanId(), requestHash, operator, now);
        try {
            insert(tenantMapper.insert(tenant), "tenant");
            insert(deploymentMapper.insert(deployment), "deployment");
            insert(taskMapper.insert(task), "provisioning task");
        } catch (DuplicateKeyException error) {
            throw conflict("Provisioning identifiers are already registered", error);
        }
        return new SaasProvisioningPreparation(task, tenant, deployment, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasProvisioningPreparation load(String requestId) {
        SaasProvisioningTaskEntity task = taskMapper.lockByRequestId(requestId);
        if (task == null) throw notFound();
        return loadExisting(task, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasProvisioningTaskEntity markProcessing(String requestId, Long expectedVersion, String operator) {
        operator = time.operator(operator);
        SaasProvisioningTaskEntity task = locked(requestId, expectedVersion);
        if (task.getStatus() != SaasProvisioningStatus.PENDING
                && task.getStatus() != SaasProvisioningStatus.FAILED) {
            throw versionConflict();
        }
        LocalDateTime now = time.now();
        cas(taskMapper.markProcessing(requestId, expectedVersion, now.plusMinutes(LEASE_MINUTES), operator, now));
        task.setStatus(SaasProvisioningStatus.PROVISIONING);
        task.setAttemptCount((task.getAttemptCount() == null ? 0 : task.getAttemptCount()) + 1);
        task.setLeaseUntil(now.plusMinutes(LEASE_MINUTES));
        task.setLastErrorType(null);
        task.setTenantRecordId(null);
        task.setCompanyId(null);
        task.setDeptId(null);
        task.setRoleId(null);
        task.setUserId(null);
        task.setActivationExpiresAt(null);
        task.setVersionNo(task.getVersionNo() + 1);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasProvisioningTaskEntity markInitialized(String requestId, Long expectedVersion,
            SaasTenantInitializationResult result, String operator) {
        operator = time.operator(operator);
        SaasProvisioningTaskEntity task = locked(requestId, expectedVersion);
        if (task.getStatus() != SaasProvisioningStatus.PROVISIONING) throw versionConflict();
        validateResult(task, result);
        LocalDateTime expiresAt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(result.getActivationExpiresAtEpochMs()), ZoneOffset.UTC);
        LocalDateTime now = time.now();
        cas(taskMapper.markInitialized(requestId, expectedVersion, result.getTenantRecordId(),
                result.getCompanyId(), result.getDeptId(), result.getRoleId(), result.getUserId(),
                expiresAt, operator, now));
        task.setStatus(SaasProvisioningStatus.INITIALIZED);
        task.setLeaseUntil(null);
        task.setTenantRecordId(result.getTenantRecordId());
        task.setCompanyId(result.getCompanyId());
        task.setDeptId(result.getDeptId());
        task.setRoleId(result.getRoleId());
        task.setUserId(result.getUserId());
        task.setActivationExpiresAt(expiresAt);
        task.setVersionNo(task.getVersionNo() + 1);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasProvisioningTaskEntity markSucceeded(String requestId, Long expectedVersion, String operator) {
        operator = time.operator(operator);
        SaasProvisioningTaskEntity task = locked(requestId, expectedVersion);
        if (task.getStatus() == SaasProvisioningStatus.SUCCEEDED) return task;
        if (task.getStatus() != SaasProvisioningStatus.INITIALIZED) throw versionConflict();
        SaasDeploymentEntity deployment = deploymentMapper.lockByTenantId(task.getTenantId());
        if (deployment == null || deployment.getVersionNo() == null) {
            throw conflict("Tenant deployment is not registered", null);
        }
        LocalDateTime now = time.now();
        cas(deploymentMapper.updateStatus(task.getTenantId(), deployment.getVersionNo(),
                DeploymentStatus.HEALTHY, operator, now));
        cas(taskMapper.markSucceeded(requestId, expectedVersion, operator, now));
        deployment.setStatus(DeploymentStatus.HEALTHY);
        deployment.setVersionNo(deployment.getVersionNo() + 1);
        task.setStatus(SaasProvisioningStatus.SUCCEEDED);
        task.setLeaseUntil(null);
        task.setLastErrorType(null);
        task.setVersionNo(task.getVersionNo() + 1);
        return task;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(String requestId, String errorType, String operator) {
        operator = time.operator(operator);
        SaasProvisioningTaskEntity task = taskMapper.lockByRequestId(requestId);
        if (task == null) throw notFound();
        if (task.getStatus() == SaasProvisioningStatus.FAILED
                || task.getStatus() == SaasProvisioningStatus.SUCCEEDED) return;
        String normalizedError = errorType == null ? "RuntimeException" : errorType.trim();
        if (normalizedError.isEmpty() || normalizedError.length() > 128) normalizedError = "RuntimeException";
        cas(taskMapper.markFailed(requestId, task.getVersionNo(), normalizedError, operator, time.now()));
    }

    private SaasProvisioningPreparation loadExisting(SaasProvisioningTaskEntity task, boolean replayed) {
        SaasTenantEntity tenant = tenantMapper.lockByTenantId(task.getTenantId());
        SaasDeploymentEntity deployment = deploymentMapper.findByTenantId(task.getTenantId());
        if (tenant == null || deployment == null) {
            throw new SaasProvisioningException(SaasProvisioningException.ErrorCode.CONFLICT,
                    "Provisioning state is incomplete");
        }
        return new SaasProvisioningPreparation(task, tenant, deployment, replayed);
    }

    private void reclaimExpired(SaasProvisioningTaskEntity task, String operator) {
        if (task.getStatus() != SaasProvisioningStatus.PROVISIONING
                || task.getLeaseUntil() == null) {
            return;
        }
        LocalDateTime now = time.now();
        if (task.getLeaseUntil().isAfter(now)) {
            return;
        }
        cas(taskMapper.reclaimExpired(task.getRequestId(), task.getVersionNo(), now, operator));
        task.setStatus(SaasProvisioningStatus.FAILED);
        task.setLeaseUntil(null);
        task.setLastErrorType("ProvisioningLeaseExpired");
        task.setVersionNo(task.getVersionNo() + 1);
    }

    private SaasProvisioningTaskEntity locked(String requestId, Long expectedVersion) {
        SaasProvisioningTaskEntity task = taskMapper.lockByRequestId(requestId);
        if (task == null) throw notFound();
        if (!Objects.equals(task.getVersionNo(), expectedVersion)) throw versionConflict();
        return task;
    }

    private SaasTenantEntity tenant(SaasTenantProvisioningCommand command, String operator, LocalDateTime now) {
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId(command.tenantId());
        tenant.setSlug(command.slug());
        tenant.setTenantName(command.tenantName());
        tenant.setLifecycleState(TenantLifecycleState.DRAFT);
        tenant.setCreateBy(operator);
        tenant.setCreateTime(now);
        tenant.setUpdateBy(operator);
        tenant.setUpdateTime(now);
        tenant.setVersionNo(0L);
        return tenant;
    }

    private SaasDeploymentEntity deployment(SaasTenantProvisioningCommand command,
            String operator, LocalDateTime now) {
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setTenantId(command.tenantId());
        deployment.setMode(command.deploymentMode());
        deployment.setStatus(DeploymentStatus.REGISTERED);
        deployment.setDeploymentRef(command.deploymentRef());
        deployment.setSecretRef(command.secretRef());
        deployment.setCreateBy(operator);
        deployment.setCreateTime(now);
        deployment.setUpdateBy(operator);
        deployment.setUpdateTime(now);
        deployment.setVersionNo(0L);
        return deployment;
    }

    private SaasProvisioningTaskEntity task(SaasTenantProvisioningCommand command, Long planId,
            String requestHash, String operator, LocalDateTime now) {
        SaasProvisioningTaskEntity task = new SaasProvisioningTaskEntity();
        task.setRequestId(command.requestId());
        task.setRequestHash(requestHash);
        task.setTenantId(command.tenantId());
        task.setPlanId(planId);
        task.setStatus(SaasProvisioningStatus.PENDING);
        task.setAttemptCount(0);
        task.setCreateBy(operator);
        task.setCreateTime(now);
        task.setUpdateBy(operator);
        task.setUpdateTime(now);
        task.setVersionNo(0L);
        return task;
    }

    private void validateResult(SaasProvisioningTaskEntity task, SaasTenantInitializationResult result) {
        if (result == null || !task.getRequestId().equals(result.getRequestId())
                || !task.getTenantId().equals(result.getTenantId())
                || !positive(result.getTenantRecordId()) || !positive(result.getCompanyId())
                || !positive(result.getDeptId()) || !positive(result.getRoleId())
                || !positive(result.getUserId()) || result.getActivationExpiresAtEpochMs() <= 0) {
            throw new SaasProvisioningException(SaasProvisioningException.ErrorCode.INVALID_RESULT,
                    "System tenant initialization returned an invalid result");
        }
    }

    private String requestHash(SaasTenantProvisioningCommand command) {
        String canonical = String.join("\u0000",
                command.requestId(), command.tenantId(), command.slug(), command.tenantName(),
                command.companyCode(), command.companyName(), command.adminUsername(),
                command.adminDisplayName(), command.adminEmail(), command.deploymentMode().name(),
                command.planCode(), command.host(), command.deploymentRef(),
                command.secretRef() == null ? "" : command.secretRef());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException("SHA-256 is unavailable", error);
        }
    }

    private boolean positive(Long value) {
        return value != null && value > 0;
    }

    private void insert(int affected, String target) {
        if (affected != 1) throw conflict("Failed to create " + target, null);
    }

    private void cas(int affected) {
        if (affected != 1) throw versionConflict();
    }

    private SaasProvisioningException notFound() {
        return new SaasProvisioningException(SaasProvisioningException.ErrorCode.NOT_FOUND,
                "Provisioning request not found");
    }

    private SaasProvisioningException versionConflict() {
        return new SaasProvisioningException(SaasProvisioningException.ErrorCode.VERSION_CONFLICT,
                "Provisioning state changed concurrently");
    }

    private SaasProvisioningException conflict(String message, Throwable cause) {
        return cause == null
                ? new SaasProvisioningException(SaasProvisioningException.ErrorCode.CONFLICT, message)
                : new SaasProvisioningException(SaasProvisioningException.ErrorCode.CONFLICT, message, cause);
    }
}
