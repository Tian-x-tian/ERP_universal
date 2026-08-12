package com.erp.system.saas.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.saas.contract.model.SaasTenantInitializationRequest;
import com.erp.saas.contract.model.SaasTenantInitializationResult;
import com.erp.system.domain.SysCompany;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysSaasProvisioningTask;
import com.erp.system.domain.SysTenant;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserActivation;
import com.erp.system.mapper.SysSaasProvisioningTaskMapper;
import com.erp.system.mapper.SysTenantMapper;
import com.erp.system.mapper.SysUserActivationMapper;
import com.erp.system.saas.SaasSecureTokenService;
import com.erp.system.saas.SaasTenantInitializationService;
import com.erp.system.service.ISysCompanyService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysUserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;

@Service
public class SaasTenantInitializationServiceImpl implements SaasTenantInitializationService {
    private static final String PLATFORM_TENANT_ID = "000000";
    private static final String OPERATOR = "saas-provisioning";
    private static final String PROCESSING = "PROCESSING";
    private static final String SUCCEEDED = "SUCCEEDED";

    private final SysTenantMapper tenantMapper;
    private final SysSaasProvisioningTaskMapper taskMapper;
    private final SysUserActivationMapper activationMapper;
    private final ISysCompanyService companyService;
    private final ISysDeptService deptService;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;
    private final ISysUserService userService;
    private final SaasSecureTokenService tokenService;
    private final Clock clock;
    private final long activationTtlHours;

    public SaasTenantInitializationServiceImpl(SysTenantMapper tenantMapper,
            SysSaasProvisioningTaskMapper taskMapper, SysUserActivationMapper activationMapper,
            ISysCompanyService companyService, ISysDeptService deptService,
            ISysRoleService roleService, ISysMenuService menuService, ISysUserService userService,
            SaasSecureTokenService tokenService, Clock clock,
            @Value("${erp.saas.activation.ttl-hours:24}") long activationTtlHours) {
        this.tenantMapper = tenantMapper;
        this.taskMapper = taskMapper;
        this.activationMapper = activationMapper;
        this.companyService = companyService;
        this.deptService = deptService;
        this.roleService = roleService;
        this.menuService = menuService;
        this.userService = userService;
        this.tokenService = tokenService;
        this.clock = clock;
        if (activationTtlHours < 1 || activationTtlHours > 168) {
            throw new IllegalArgumentException("activationTtlHours must be between 1 and 168");
        }
        this.activationTtlHours = activationTtlHours;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantInitializationResult initialize(SaasTenantInitializationRequest request) {
        requirePlatformContext();
        NormalizedRequest normalized = normalize(request);
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
        String requestHash = requestHash(normalized);
        String originalTenant = TenantContextHolder.getTenantId();
        try {
            TenantContextHolder.setTenantId(normalized.tenantId());
            SysSaasProvisioningTask task = processingTask(normalized, requestHash, now);
            if (task != null) {
                return replay(task, requestHash);
            }
            return create(normalized, now);
        } finally {
            TenantContextHolder.setTenantId(originalTenant);
        }
    }

    private SysSaasProvisioningTask processingTask(NormalizedRequest request,
            String requestHash, LocalDateTime now) {
        SysSaasProvisioningTask task = new SysSaasProvisioningTask();
        task.setTenantId(request.tenantId());
        task.setRequestId(request.requestId());
        task.setRequestHash(requestHash);
        task.setStatus(PROCESSING);
        task.setCreateBy(OPERATOR);
        task.setCreateTime(now);
        task.setUpdateBy(OPERATOR);
        task.setUpdateTime(now);
        if (taskMapper.insertProcessing(task) == 1) {
            return null;
        }
        SysSaasProvisioningTask existing = taskMapper.lock(request.tenantId(), request.requestId());
        if (existing == null) {
            throw conflict("Provisioning task could not be locked");
        }
        return existing;
    }

    private SaasTenantInitializationResult replay(SysSaasProvisioningTask task, String requestHash) {
        if (!requestHash.equals(task.getRequestHash())) {
            throw conflict("Provisioning request id was reused with different tenant data");
        }
        if (!SUCCEEDED.equals(task.getStatus())) {
            throw conflict("Provisioning request is already processing");
        }
        return result(task, null, true);
    }

    private SaasTenantInitializationResult create(NormalizedRequest request, LocalDateTime now) {
        if (tenantMapper.findByTenantIdForUpdate(request.tenantId()) != null) {
            throw conflict("Tenant identifier already exists");
        }
        SysTenant tenant = tenant(request);
        requireInserted(tenantMapper.insert(tenant), "tenant");

        SysCompany company = company(request);
        requireSaved(companyService.createCompany(company), "company");

        SysDept dept = department(request, company.getCompanyId());
        requireSaved(deptService.createDept(dept), "root department");

        SysRole role = administratorRole(request, activeMenuIds());
        requireSaved(roleService.save(role), "tenant administrator role");

        SaasSecureTokenService.SecureToken activationToken = tokenService.generate();
        SaasSecureTokenService.SecureToken placeholderPassword = tokenService.generate();
        SysUser user = administrator(request, dept.getDeptId(), role.getRoleId(), placeholderPassword.rawToken());
        requireSaved(userService.save(user), "tenant administrator user");

        LocalDateTime activationExpiresAt = now.plusHours(activationTtlHours);
        requireInserted(activationMapper.insert(activation(request.tenantId(), user.getUserId(),
                activationToken.tokenHash(), activationExpiresAt, now)), "activation record");
        requireInserted(taskMapper.markSucceeded(request.tenantId(), request.requestId(), tenant.getId(),
                company.getCompanyId(), dept.getDeptId(), role.getRoleId(), user.getUserId(),
                activationExpiresAt, now), "provisioning completion");

        SysSaasProvisioningTask completed = new SysSaasProvisioningTask();
        completed.setTenantId(request.tenantId());
        completed.setRequestId(request.requestId());
        completed.setStatus(SUCCEEDED);
        completed.setTenantRecordId(tenant.getId());
        completed.setCompanyId(company.getCompanyId());
        completed.setDeptId(dept.getDeptId());
        completed.setRoleId(role.getRoleId());
        completed.setUserId(user.getUserId());
        completed.setActivationExpiresAt(activationExpiresAt);
        return result(completed, activationToken.rawToken(), false);
    }

    private SysTenant tenant(NormalizedRequest request) {
        SysTenant tenant = new SysTenant();
        tenant.setTenantId(request.tenantId());
        tenant.setName(request.tenantName());
        tenant.setContactUser(request.adminDisplayName());
        tenant.setStatus("0");
        tenant.setDelFlag("0");
        tenant.setCreateBy(OPERATOR);
        tenant.setUpdateBy(OPERATOR);
        tenant.setRemark("Created by SaaS provisioning");
        return tenant;
    }

    private SysCompany company(NormalizedRequest request) {
        SysCompany company = new SysCompany();
        company.setTenantId(request.tenantId());
        company.setCompanyCode(request.companyCode());
        company.setCompanyName(request.companyName());
        company.setParentCompanyId(0L);
        company.setLeader(request.adminDisplayName());
        company.setStatus("0");
        company.setDelFlag("0");
        company.setCreateBy(OPERATOR);
        company.setUpdateBy(OPERATOR);
        return company;
    }

    private SysDept department(NormalizedRequest request, Long companyId) {
        SysDept dept = new SysDept();
        dept.setTenantId(request.tenantId());
        dept.setCompanyId(companyId);
        dept.setParentId(0L);
        dept.setDeptName("总部");
        dept.setOrderNum(0);
        dept.setLeader(request.adminDisplayName());
        dept.setEmail(request.adminEmail());
        dept.setStatus("0");
        dept.setDelFlag("0");
        dept.setCreateBy(OPERATOR);
        dept.setUpdateBy(OPERATOR);
        return dept;
    }

    private SysRole administratorRole(NormalizedRequest request, List<Long> menuIds) {
        SysRole role = new SysRole();
        role.setTenantId(request.tenantId());
        role.setRoleName("Tenant Administrator");
        role.setRoleKey("tenant_admin");
        role.setRoleSort(1);
        role.setDataScope("1");
        role.setStatus("0");
        role.setDelFlag("0");
        role.setMenuIds(menuIds);
        role.setCreateBy(OPERATOR);
        role.setUpdateBy(OPERATOR);
        return role;
    }

    private List<Long> activeMenuIds() {
        return menuService.list(new LambdaQueryWrapper<SysMenu>().eq(SysMenu::getStatus, "0")).stream()
                .map(SysMenu::getMenuId).filter(java.util.Objects::nonNull).distinct().toList();
    }

    private SysUser administrator(NormalizedRequest request, Long deptId,
            Long roleId, String placeholderPassword) {
        SysUser user = new SysUser();
        user.setTenantId(request.tenantId());
        user.setDeptId(deptId);
        user.setRoleIds(List.of(roleId));
        user.setUserName(request.adminUsername());
        user.setNickName(request.adminDisplayName());
        user.setUserType("00");
        user.setEmail(request.adminEmail());
        user.setSex("2");
        user.setPassword(placeholderPassword);
        user.setTokenVersion(0);
        user.setStatus("1");
        user.setDelFlag("0");
        user.setCreateBy(OPERATOR);
        user.setUpdateBy(OPERATOR);
        user.setRemark("Activation required");
        return user;
    }

    private SysUserActivation activation(String tenantId, Long userId, String tokenHash,
            LocalDateTime expiresAt, LocalDateTime now) {
        SysUserActivation activation = new SysUserActivation();
        activation.setTenantId(tenantId);
        activation.setUserId(userId);
        activation.setTokenHash(tokenHash);
        activation.setExpiresAt(expiresAt);
        activation.setStatus("PENDING");
        activation.setCreateBy(OPERATOR);
        activation.setCreateTime(now);
        activation.setUpdateBy(OPERATOR);
        activation.setUpdateTime(now);
        activation.setVersionNo(0L);
        return activation;
    }

    private SaasTenantInitializationResult result(SysSaasProvisioningTask task,
            String activationToken, boolean replayed) {
        long expiresAt = task.getActivationExpiresAt() == null ? 0L
                : task.getActivationExpiresAt().toInstant(ZoneOffset.UTC).toEpochMilli();
        return new SaasTenantInitializationResult(task.getRequestId(), task.getTenantId(),
                task.getTenantRecordId(), task.getCompanyId(), task.getDeptId(), task.getRoleId(),
                task.getUserId(), activationToken, expiresAt, replayed);
    }

    private NormalizedRequest normalize(SaasTenantInitializationRequest request) {
        if (request == null) throw validation("Initialization request is required");
        String tenantId = required(request.getTenantId(), "tenantId", 20);
        if (PLATFORM_TENANT_ID.equals(tenantId)) throw validation("Platform tenant cannot be provisioned");
        String email = required(request.getAdminEmail(), "adminEmail", 50);
        if (!email.matches("^[^@\\s]+@[^@\\s]+$")) throw validation("adminEmail is invalid");
        return new NormalizedRequest(required(request.getRequestId(), "requestId", 128), tenantId,
                required(request.getTenantName(), "tenantName", 50),
                required(request.getCompanyCode(), "companyCode", 64),
                required(request.getCompanyName(), "companyName", 128),
                required(request.getAdminUsername(), "adminUsername", 30),
                required(request.getAdminDisplayName(), "adminDisplayName", 30), email);
    }

    private String requestHash(NormalizedRequest request) {
        String canonical = String.join("\u0000", request.requestId(), request.tenantId(), request.tenantName(),
                request.companyCode(), request.companyName(), request.adminUsername(),
                request.adminDisplayName(), request.adminEmail());
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
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

    private void requireSaved(boolean saved, String entity) {
        if (!saved) throw conflict("Failed to create " + entity);
    }

    private void requireInserted(int affected, String entity) {
        if (affected != 1) throw conflict("Failed to create " + entity);
    }

    private ServiceException validation(String message) {
        return new ServiceException(message, (int) ResultCode.VALIDATE_FAILED.getCode());
    }

    private ServiceException conflict(String message) {
        return new ServiceException(message, (int) ResultCode.CONFLICT.getCode());
    }

    private record NormalizedRequest(String requestId, String tenantId, String tenantName,
            String companyCode, String companyName, String adminUsername,
            String adminDisplayName, String adminEmail) { }
}
