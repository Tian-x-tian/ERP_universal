package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysRoleDept;
import com.erp.system.domain.SysRoleMenu;
import com.erp.system.mapper.SysRoleMapper;
import com.erp.system.service.ISysRoleDeptService;
import com.erp.system.service.ISysRoleMenuService;
import com.erp.system.service.ISysRoleService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.erp.system.domain.SysUserRole;
import com.erp.system.service.ISysUserRoleService;
import java.util.Collections;
import java.util.Set;

/**
 * 角色服务实现
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements ISysRoleService {
    private static final String PLATFORM_TENANT_ID = "000000";
    private static final String SUPER_ADMIN_ROLE_KEY = "admin";
    private static final String DEFAULT_DATA_SCOPE = "3";
    private static final String DEFAULT_STATUS = "0";
    private static final String DEFAULT_DEL_FLAG = "0";
    private static final int DEFAULT_ROLE_SORT = 1;

    private final ISysRoleMenuService roleMenuService;
    private final ISysUserRoleService userRoleService;
    private final ISysRoleDeptService roleDeptService;

    public SysRoleServiceImpl(ISysRoleMenuService roleMenuService,
            ISysUserRoleService userRoleService,
            ISysRoleDeptService roleDeptService) {
        this.roleMenuService = roleMenuService;
        this.userRoleService = userRoleService;
        this.roleDeptService = roleDeptService;
    }

    @Override
    public Set<String> selectRoleKeysByUserId(Long userId) {
        List<Long> roleIds = userRoleService
                .list(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream().map(SysUserRole::getRoleId).collect(Collectors.toList());
        if (roleIds.isEmpty())
            return Collections.emptySet();

        return listByIds(roleIds).stream()
                .map(SysRole::getRoleKey)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    /**
     * 判断用户是否拥有平台超级管理员角色。
     *
     * @param userId 用户ID
     * @return true 表示平台超级管理员
     */
    @Override
    public boolean isPlatformSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<Long> roleIds = userRoleService
                .list(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return false;
        }
        return listByIds(roleIds).stream().anyMatch(this::isPlatformSuperAdminRole);
    }

    /**
     * 查询角色详情并回填菜单与部门权限。
     *
     * @param roleId 角色ID
     * @return 角色详情
     */
    @Override
    public SysRole getRoleWithPermissions(Long roleId) {
        if (roleId == null) {
            return null;
        }
        SysRole role = getById(roleId);
        if (role == null) {
            return null;
        }
        List<Long> menuIds = roleMenuService.list(new LambdaQueryWrapper<SysRoleMenu>()
                .eq(SysRoleMenu::getRoleId, roleId))
                .stream()
                .map(SysRoleMenu::getMenuId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        role.setMenuIds(menuIds);

        List<Long> deptIds = roleDeptService.list(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, roleId))
                .stream()
                .map(SysRoleDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        role.setDeptIds(deptIds);
        return role;
    }

    /**
     * 更新角色数据权限范围。
     *
     * @param role 角色对象
     * @return 更新结果
     */
    @Override
    @Transactional
    public boolean updateDataScope(SysRole role) {
        if (role == null || role.getRoleId() == null || !StringUtils.hasText(role.getDataScope())) {
            return false;
        }
        SysRole existedRole = getById(role.getRoleId());
        if (existedRole == null) {
            return false;
        }
        String tenantId = resolveTenantId(role.getTenantId(), existedRole.getTenantId());
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        SysRole updateEntity = new SysRole();
        updateEntity.setRoleId(role.getRoleId());
        updateEntity.setDataScope(role.getDataScope());
        updateEntity.setUpdateBy(role.getUpdateBy());
        boolean updated = super.updateById(updateEntity);
        if (!updated) {
            return false;
        }

        roleDeptService.remove(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        if ("2".equals(role.getDataScope()) && role.getDeptIds() != null && !role.getDeptIds().isEmpty()) {
            SysRole roleForDept = new SysRole();
            roleForDept.setRoleId(role.getRoleId());
            roleForDept.setTenantId(tenantId);
            roleForDept.setDeptIds(role.getDeptIds());
            insertRoleDept(roleForDept);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean save(SysRole entity) {
        if (entity == null) {
            return false;
        }
        applyCreateDefaults(entity);
        String tenantId = resolveTenantId(entity.getTenantId(), null);
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        entity.setTenantId(tenantId);
        entity.setRoleKey(normalizeRoleKey(entity.getRoleKey()));
        if (!canUseRoleKey(tenantId, entity.getRoleKey())) {
            return false;
        }
        boolean success = super.save(entity);
        if (success && entity.getMenuIds() != null && !entity.getMenuIds().isEmpty()) {
            insertRoleMenu(entity);
        }
        if (success && "2".equals(entity.getDataScope()) && entity.getDeptIds() != null && !entity.getDeptIds().isEmpty()) {
            insertRoleDept(entity);
        }
        return success;
    }

    @Override
    @Transactional
    public boolean updateById(SysRole entity) {
        if (entity == null || entity.getRoleId() == null) {
            return false;
        }
        SysRole existedRole = getById(entity.getRoleId());
        if (existedRole == null) {
            return false;
        }
        String tenantId = resolveTenantId(entity.getTenantId(), existedRole.getTenantId());
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        entity.setTenantId(tenantId);
        entity.setRoleKey(normalizeRoleKey(entity.getRoleKey()));
        String targetRoleKey = StringUtils.hasText(entity.getRoleKey()) ? entity.getRoleKey() : existedRole.getRoleKey();
        if (!canUseRoleKey(tenantId, targetRoleKey)) {
            return false;
        }
        // 删除旧关联
        roleMenuService.remove(new LambdaQueryWrapper<SysRoleMenu>().eq(SysRoleMenu::getRoleId, entity.getRoleId()));
        roleDeptService.remove(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, entity.getRoleId()));
        // 插入新关联
        if (entity.getMenuIds() != null && !entity.getMenuIds().isEmpty()) {
            insertRoleMenu(entity);
        }
        if ("2".equals(entity.getDataScope()) && entity.getDeptIds() != null && !entity.getDeptIds().isEmpty()) {
            insertRoleDept(entity);
        }
        return super.updateById(entity);
    }

    /**
     * 为新增角色补齐默认字段，避免前端未传字段导致保存失败。
     *
     * @param entity 角色对象
     */
    private void applyCreateDefaults(SysRole entity) {
        if (!StringUtils.hasText(entity.getDataScope())) {
            entity.setDataScope(DEFAULT_DATA_SCOPE);
        }
        if (!StringUtils.hasText(entity.getStatus())) {
            entity.setStatus(DEFAULT_STATUS);
        }
        if (!StringUtils.hasText(entity.getDelFlag())) {
            entity.setDelFlag(DEFAULT_DEL_FLAG);
        }
        if (entity.getRoleSort() == null) {
            entity.setRoleSort(DEFAULT_ROLE_SORT);
        }
    }

    /**
     * 新增角色菜单信息
     */
    private void insertRoleMenu(SysRole role) {
        String tenantId = resolveTenantId(role.getTenantId(), null);
        if (!StringUtils.hasText(tenantId)) {
            return;
        }
        List<SysRoleMenu> list = role.getMenuIds().stream().map(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
            rm.setTenantId(tenantId);
            rm.setRoleId(role.getRoleId());
            rm.setMenuId(menuId);
            return rm;
        }).collect(Collectors.toList());
        roleMenuService.saveBatch(list);
    }

    /**
     * 新增角色部门信息
     */
    private void insertRoleDept(SysRole role) {
        String tenantId = resolveTenantId(role.getTenantId(), null);
        if (!StringUtils.hasText(tenantId)) {
            return;
        }
        List<SysRoleDept> list = role.getDeptIds().stream().map(deptId -> {
            SysRoleDept rd = new SysRoleDept();
            rd.setTenantId(tenantId);
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            return rd;
        }).collect(Collectors.toList());
        roleDeptService.saveBatch(list);
    }

    /**
     * 解析角色租户编号，优先租户上下文并校验请求值一致性。
     *
     * @param tenantId         当前租户编号
     * @param fallbackTenantId 回退租户编号
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
     * @return 规范化后的租户编号
     */
    private String normalizeTenantId(String tenantId) {
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 判断角色是否为平台超级管理员角色。
     *
     * @param role 角色对象
     * @return true 表示平台超级管理员角色
     */
    private boolean isPlatformSuperAdminRole(SysRole role) {
        if (role == null) {
            return false;
        }
        return PLATFORM_TENANT_ID.equals(normalizeTenantId(role.getTenantId()))
                && SUPER_ADMIN_ROLE_KEY.equals(normalizeRoleKey(role.getRoleKey()));
    }

    /**
     * 校验角色编码是否允许在目标租户下使用。
     *
     * @param tenantId 目标租户编号
     * @param roleKey  角色编码
     * @return true 表示允许保存
     */
    private boolean canUseRoleKey(String tenantId, String roleKey) {
        String normalizedRoleKey = normalizeRoleKey(roleKey);
        if (!SUPER_ADMIN_ROLE_KEY.equals(normalizedRoleKey)) {
            return true;
        }
        return PLATFORM_TENANT_ID.equals(normalizeTenantId(tenantId));
    }

    /**
     * 规范化角色编码。
     *
     * @param roleKey 原始角色编码
     * @return 去空白后的角色编码
     */
    private String normalizeRoleKey(String roleKey) {
        return StringUtils.hasText(roleKey) ? roleKey.trim() : null;
    }
}
