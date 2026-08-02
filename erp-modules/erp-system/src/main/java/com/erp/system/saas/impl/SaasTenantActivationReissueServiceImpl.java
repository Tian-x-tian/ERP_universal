package com.erp.system.saas.impl;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;
import com.erp.system.domain.SysSaasProvisioningTask;
import com.erp.system.domain.SysUserActivation;
import com.erp.system.mapper.SysSaasProvisioningTaskMapper;
import com.erp.system.mapper.SysUserActivationMapper;
import com.erp.system.saas.SaasSecureTokenService;
import com.erp.system.saas.SaasTenantActivationReissueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Service
public class SaasTenantActivationReissueServiceImpl implements SaasTenantActivationReissueService {
    private static final String PLATFORM_TENANT_ID = "000000";

    private final SysSaasProvisioningTaskMapper taskMapper;
    private final SysUserActivationMapper activationMapper;
    private final SaasSecureTokenService tokenService;
    private final Clock clock;
    private final long activationTtlHours;

    public SaasTenantActivationReissueServiceImpl(SysSaasProvisioningTaskMapper taskMapper,
            SysUserActivationMapper activationMapper, SaasSecureTokenService tokenService,
            Clock clock, @Value("${erp.saas.activation.ttl-hours:24}") long activationTtlHours) {
        this.taskMapper = Objects.requireNonNull(taskMapper);
        this.activationMapper = Objects.requireNonNull(activationMapper);
        this.tokenService = Objects.requireNonNull(tokenService);
        this.clock = Objects.requireNonNull(clock);
        if (activationTtlHours < 1 || activationTtlHours > 168) {
            throw new IllegalArgumentException("activationTtlHours must be between 1 and 168");
        }
        this.activationTtlHours = activationTtlHours;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantActivationReissueResult reissue(SaasTenantActivationReissueRequest request) {
        requirePlatformContext();
        String requestId = required(request == null ? null : request.getRequestId(), "requestId", 128);
        String tenantId = required(request == null ? null : request.getTenantId(), "tenantId", 20);
        if (PLATFORM_TENANT_ID.equals(tenantId)) {
            throw validation("Platform tenant activation cannot be reissued");
        }
        String originalTenant = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(tenantId);
            return reissue(requestId, tenantId);
        } finally {
            TenantContextHolder.setTenantId(originalTenant);
        }
    }

    private SaasTenantActivationReissueResult reissue(String requestId, String tenantId) {
        SysSaasProvisioningTask task = taskMapper.lock(tenantId, requestId);
        if (task == null || !"SUCCEEDED".equals(task.getStatus()) || task.getUserId() == null) {
            throw conflict("Completed provisioning task is required");
        }
        SysUserActivation activation = activationMapper.lockByUser(tenantId, task.getUserId());
        if (activation == null || !"PENDING".equals(activation.getStatus())
                || activation.getActivationId() == null || activation.getVersionNo() == null
                || !tenantId.equals(activation.getTenantId())
                || !task.getUserId().equals(activation.getUserId())) {
            throw conflict("Activation token cannot be reissued");
        }

        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDateTime expiresAt = now.plusHours(activationTtlHours);
        SaasSecureTokenService.SecureToken token = tokenService.generate();
        if (activationMapper.reissue(tenantId, activation.getActivationId(), task.getUserId(),
                activation.getVersionNo(), token.tokenHash(), expiresAt, now) != 1) {
            throw conflict("Activation token reissue conflicted with another request");
        }
        if (taskMapper.updateActivationExpiry(tenantId, requestId, task.getUserId(), expiresAt, now) != 1) {
            throw conflict("Provisioning activation expiry could not be updated");
        }
        return new SaasTenantActivationReissueResult(requestId, tenantId, task.getUserId(),
                token.rawToken(), expiresAt.toInstant(ZoneOffset.UTC).toEpochMilli());
    }

    private void requirePlatformContext() {
        if (!PLATFORM_TENANT_ID.equals(TenantContextHolder.getTenantId())) {
            throw new ServiceException("Platform tenant context is required",
                    (int) ResultCode.FORBIDDEN.getCode());
        }
    }

    private String required(String value, String field, int maxLength) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > maxLength) {
            throw validation(field + " must contain 1 to " + maxLength + " characters");
        }
        return value.trim();
    }

    private ServiceException validation(String message) {
        return new ServiceException(message, (int) ResultCode.VALIDATE_FAILED.getCode());
    }

    private ServiceException conflict(String message) {
        return new ServiceException(message, (int) ResultCode.CONFLICT.getCode());
    }
}
