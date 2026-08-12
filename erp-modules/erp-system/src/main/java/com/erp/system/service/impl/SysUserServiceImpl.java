package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.service.ISysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.SysUserPost;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.saas.SaasLocalQuotaService;
import com.erp.system.service.ISysUserPostService;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final PasswordEncoder passwordEncoder;
    private final ISysUserRoleService userRoleService;
    private final ISysUserPostService userPostService;
    private final SaasLocalQuotaService quotaService;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder,
            ISysUserRoleService userRoleService,
            ISysUserPostService userPostService,
            SaasLocalQuotaService quotaService) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleService = userRoleService;
        this.userPostService = userPostService;
        this.quotaService = quotaService;
    }

    @Override
    public SysUser selectUserByUserName(String userName) {
        LambdaQueryWrapper<SysUser> queryWrapper = new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName);
        String tenantId = TenantContextHolder.getTenantId();
        if (StringUtils.hasText(tenantId)) {
            queryWrapper.eq(SysUser::getTenantId, tenantId.trim());
        }
        return getOne(queryWrapper);
    }

    /**
     * 更新当前用户个人资料。
     *
     * @param profile 包含 userId 与可更新资料字段的用户对象
     * @return 更新是否成功
     */
    @Override
    @Transactional
    public boolean updateProfileByUserId(SysUser profile) {
        if (profile == null || profile.getUserId() == null) {
            return false;
        }
        return baseMapper.updateById(profile) > 0;
    }

    /**
     * 更新当前用户密码。
     *
     * @param userId          用户ID
     * @param encodedPassword 已加密的新密码
     * @return 更新是否成功
     */
    @Override
    @Transactional
    public boolean updatePasswordByUserId(Long userId, String encodedPassword) {
        if (userId == null) {
            return false;
        }
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setPassword(encodedPassword);
        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean activateProvisionedUser(Long userId, String encodedPassword) {
        if (userId == null || !StringUtils.hasText(encodedPassword)) {
            return false;
        }
        SysUser existedUser = getById(userId);
        if (existedUser == null || !"1".equals(existedUser.getStatus())
                || !"0".equals(existedUser.getDelFlag())) {
            return false;
        }
        String tenantId = resolveTenantId(null, existedUser.getTenantId());
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String quotaReference = reserveUserQuota(tenantId);
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setTenantId(tenantId);
        updateEntity.setStatus("0");
        updateEntity.setPassword(encodedPassword);
        boolean success = super.updateById(updateEntity);
        if (!success) {
            releaseUserQuota(tenantId, quotaReference);
            return false;
        }
        settleUserQuota(tenantId, quotaReference);
        return true;
    }

    @Override
    @Transactional
    public boolean save(SysUser entity) {
        if (entity == null) {
            return false;
        }
        String tenantId = resolveTenantId(entity.getTenantId(), null);
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        entity.setTenantId(tenantId);
        if (StringUtils.hasText(entity.getUserName())) {
            entity.setUserName(entity.getUserName().trim());
        }
        if (StringUtils.hasText(entity.getNickName())) {
            entity.setNickName(entity.getNickName().trim());
        }
        if (entity.getTokenVersion() == null) {
            entity.setTokenVersion(0);
        }
        // 密码加密
        if (entity.getPassword() != null) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        String quotaReference = isCountedOnCreate(entity) ? reserveUserQuota(tenantId) : null;
        boolean success = super.save(entity);
        if (!success && quotaReference != null) {
            releaseUserQuota(tenantId, quotaReference);
        }
        if (success && entity.getRoleIds() != null && !entity.getRoleIds().isEmpty()) {
            insertUserRole(entity);
        }
        if (success && entity.getPostIds() != null && !entity.getPostIds().isEmpty()) {
            insertUserPost(entity);
        }
        if (success && quotaReference != null) {
            settleUserQuota(tenantId, quotaReference);
        }
        return success;
    }

    @Override
    @Transactional
    public boolean updateById(SysUser entity) {
        if (entity == null || entity.getUserId() == null) {
            return false;
        }
        SysUser existedUser = getById(entity.getUserId());
        if (existedUser == null) {
            return false;
        }
        String tenantId = resolveTenantId(entity.getTenantId(), existedUser.getTenantId());
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        entity.setTenantId(tenantId);
        boolean countedBefore = isCounted(existedUser.getStatus(), existedUser.getDelFlag());
        boolean countedAfter = isCounted(
                valueOrExisting(entity.getStatus(), existedUser.getStatus()),
                valueOrExisting(entity.getDelFlag(), existedUser.getDelFlag()));
        String quotaReference = !countedBefore && countedAfter ? reserveUserQuota(tenantId) : null;
        if (StringUtils.hasText(entity.getUserName())) {
            entity.setUserName(entity.getUserName().trim());
        }
        if (StringUtils.hasText(entity.getNickName())) {
            entity.setNickName(entity.getNickName().trim());
        }
        // 先删除原有关联
        userRoleService.remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, entity.getUserId()));
        userPostService.remove(new LambdaQueryWrapper<SysUserPost>().eq(SysUserPost::getUserId, entity.getUserId()));
        // 插入新关联
        if (entity.getRoleIds() != null && !entity.getRoleIds().isEmpty()) {
            insertUserRole(entity);
        }
        if (entity.getPostIds() != null && !entity.getPostIds().isEmpty()) {
            insertUserPost(entity);
        }
        boolean success = super.updateById(entity);
        if (!success && quotaReference != null) {
            releaseUserQuota(tenantId, quotaReference);
        }
        if (success && quotaReference != null) {
            settleUserQuota(tenantId, quotaReference);
        } else if (success && countedBefore && !countedAfter) {
            quotaService.decreaseUsed(SaasQuotaKeys.USER_COUNT, 1L, "user-service");
        }
        return success;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeUserById(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser existedUser = getById(userId);
        if (existedUser == null) {
            return false;
        }
        boolean success = baseMapper.deleteById(userId) > 0;
        if (success && isCounted(existedUser.getStatus(), existedUser.getDelFlag())) {
            quotaService.decreaseUsed(SaasQuotaKeys.USER_COUNT, 1L, "user-service");
        }
        return success;
    }

    /**
     * 递增用户 Token 版本号，使既有令牌失效。
     *
     * @param userId 用户ID
     * @return 更新是否成功
     */
    @Override
    @Transactional
    public boolean incrementTokenVersion(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser existedUser = getById(userId);
        if (existedUser == null) {
            return false;
        }
        SysUser updateEntity = new SysUser();
        updateEntity.setUserId(userId);
        updateEntity.setTokenVersion((existedUser.getTokenVersion() == null ? 0 : existedUser.getTokenVersion()) + 1);
        return baseMapper.updateById(updateEntity) > 0;
    }

    /**
     * 新增用户角色信息
     */
    private void insertUserRole(SysUser user) {
        String tenantId = resolveTenantId(user.getTenantId(), null);
        if (!StringUtils.hasText(tenantId)) {
            return;
        }
        List<SysUserRole> list = user.getRoleIds().stream().map(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setTenantId(tenantId);
            ur.setUserId(user.getUserId());
            ur.setRoleId(roleId);
            return ur;
        }).collect(Collectors.toList());
        userRoleService.saveBatch(list);
    }

    /**
     * 新增用户岗位信息
     */
    private void insertUserPost(SysUser user) {
        String tenantId = resolveTenantId(user.getTenantId(), null);
        if (!StringUtils.hasText(tenantId)) {
            return;
        }
        List<SysUserPost> list = user.getPostIds().stream().map(postId -> {
            SysUserPost up = new SysUserPost();
            up.setTenantId(tenantId);
            up.setUserId(user.getUserId());
            up.setPostId(postId);
            return up;
        }).collect(Collectors.toList());
        userPostService.saveBatch(list);
    }

    /**
     * 解析用户租户编号，优先使用租户上下文并校验请求值一致性。
     *
     * @param tenantId        请求中传入的租户编号
     * @param fallbackTenantId 已存在实体的租户编号
     * @return 租户编号
     */
    private String resolveTenantId(String tenantId, String fallbackTenantId) {
        String contextTenantId = normalizeTenantId(TenantContextHolder.getTenantId());
        String normalizedTenantId = normalizeTenantId(tenantId);
        String normalizedFallbackTenantId = normalizeTenantId(fallbackTenantId);
        if (StringUtils.hasText(contextTenantId)) {
            if (StringUtils.hasText(normalizedTenantId) && !contextTenantId.equals(normalizedTenantId)) {
                return null;
            }
            if (StringUtils.hasText(normalizedFallbackTenantId) && !contextTenantId.equals(normalizedFallbackTenantId)) {
                return null;
            }
            return contextTenantId;
        }
        if (StringUtils.hasText(normalizedTenantId)) {
            return normalizedTenantId;
        }
        if (StringUtils.hasText(normalizedFallbackTenantId)) {
            return normalizedFallbackTenantId;
        }
        return null;
    }

    /**
     * 规范化租户编号。
     *
     * @param tenantId 原始租户编号
     * @return 规范化租户编号
     */
    private String normalizeTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    private String reserveUserQuota(String tenantId) {
        String reference = "user-" + UUID.randomUUID();
        quotaService.apply(quotaEvent(tenantId, reference, SaasUsageOperation.RESERVE, 1L));
        return reference;
    }

    private void settleUserQuota(String tenantId, String reference) {
        quotaService.apply(quotaEvent(tenantId, reference, SaasUsageOperation.SETTLE, 1L));
    }

    private void releaseUserQuota(String tenantId, String reference) {
        quotaService.apply(quotaEvent(tenantId, reference, SaasUsageOperation.RELEASE, null));
    }

    private SaasUsageEvent quotaEvent(String tenantId, String reference,
            SaasUsageOperation operation, Long amount) {
        long now = System.currentTimeMillis();
        return new SaasUsageEvent(operation.name().toLowerCase() + "-" + UUID.randomUUID(),
                tenantId, SaasQuotaKeys.USER_COUNT, operation, reference, amount, null, now);
    }

    private boolean isCountedOnCreate(SysUser user) {
        return isCounted(valueOrExisting(user.getStatus(), "0"), valueOrExisting(user.getDelFlag(), "0"));
    }

    private boolean isCounted(String status, String delFlag) {
        return "0".equals(status) && "0".equals(delFlag);
    }

    private String valueOrExisting(String value, String existing) {
        return value == null ? existing : value;
    }
}
