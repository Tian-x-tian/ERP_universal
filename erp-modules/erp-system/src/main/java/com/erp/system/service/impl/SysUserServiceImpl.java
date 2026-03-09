package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.service.ISysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.SysUserPost;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.service.ISysUserPostService;
import org.springframework.transaction.annotation.Transactional;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;

/**
 * 用户服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private static final String DEFAULT_TENANT_ID = "000000";

    private final PasswordEncoder passwordEncoder;
    private final ISysUserRoleService userRoleService;
    private final ISysUserPostService userPostService;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder,
            ISysUserRoleService userRoleService,
            ISysUserPostService userPostService) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleService = userRoleService;
        this.userPostService = userPostService;
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
        profile.setUpdateTime(new Date());
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
        updateEntity.setUpdateTime(new Date());
        return baseMapper.updateById(updateEntity) > 0;
    }

    @Override
    @Transactional
    public boolean save(SysUser entity) {
        if (entity == null) {
            return false;
        }
        entity.setTenantId(resolveTenantId(entity.getTenantId(), null));
        if (StringUtils.hasText(entity.getUserName())) {
            entity.setUserName(entity.getUserName().trim());
        }
        if (StringUtils.hasText(entity.getNickName())) {
            entity.setNickName(entity.getNickName().trim());
        }
        entity.setCreateTime(new Date());
        // 密码加密
        if (entity.getPassword() != null) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        boolean success = super.save(entity);
        if (success && entity.getRoleIds() != null && !entity.getRoleIds().isEmpty()) {
            insertUserRole(entity);
        }
        if (success && entity.getPostIds() != null && !entity.getPostIds().isEmpty()) {
            insertUserPost(entity);
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
        entity.setTenantId(resolveTenantId(entity.getTenantId(), existedUser.getTenantId()));
        if (StringUtils.hasText(entity.getUserName())) {
            entity.setUserName(entity.getUserName().trim());
        }
        if (StringUtils.hasText(entity.getNickName())) {
            entity.setNickName(entity.getNickName().trim());
        }
        entity.setUpdateTime(new Date());
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
        return super.updateById(entity);
    }

    /**
     * 新增用户角色信息
     */
    private void insertUserRole(SysUser user) {
        String tenantId = StringUtils.hasText(user.getTenantId()) ? user.getTenantId() : "000000";
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
        String tenantId = StringUtils.hasText(user.getTenantId()) ? user.getTenantId() : "000000";
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
     * 解析用户租户编号，优先使用入参，其次使用租户上下文，最后回退默认租户。
     *
     * @param tenantId        请求中传入的租户编号
     * @param fallbackTenantId 已存在实体的租户编号
     * @return 租户编号
     */
    private String resolveTenantId(String tenantId, String fallbackTenantId) {
        if (StringUtils.hasText(tenantId)) {
            return tenantId.trim();
        }
        if (StringUtils.hasText(fallbackTenantId)) {
            return fallbackTenantId.trim();
        }
        String contextTenantId = TenantContextHolder.getTenantId();
        if (StringUtils.hasText(contextTenantId)) {
            return contextTenantId.trim();
        }
        return DEFAULT_TENANT_ID;
    }
}
