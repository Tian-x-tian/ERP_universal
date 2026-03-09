package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.SysRoleDept;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysUserRole;
import com.erp.system.domain.vo.DataPermissionScope;
import com.erp.system.service.IDataPermissionService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysRoleDeptService;
import com.erp.system.service.ISysRoleService;
import com.erp.system.service.ISysUserRoleService;
import com.erp.system.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 数据权限服务实现
 */
@Service
public class DataPermissionServiceImpl implements IDataPermissionService {

    private final ISysUserService userService;
    private final ISysUserRoleService userRoleService;
    private final ISysRoleService roleService;
    private final ISysRoleDeptService roleDeptService;
    private final ISysDeptService deptService;

    public DataPermissionServiceImpl(ISysUserService userService,
            ISysUserRoleService userRoleService,
            ISysRoleService roleService,
            ISysRoleDeptService roleDeptService,
            ISysDeptService deptService) {
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.roleService = roleService;
        this.roleDeptService = roleDeptService;
        this.deptService = deptService;
    }

    /**
     * 解析用户的数据权限范围。
     *
     * @param userId 用户ID
     * @return 数据权限范围
     */
    @Override
    public DataPermissionScope resolveDataScope(Long userId) {
        DataPermissionScope scope = new DataPermissionScope();
        if (userId == null) {
            return scope;
        }

        SysUser user = userService.getById(userId);
        if (user == null) {
            return scope;
        }

        List<Long> roleIds = userRoleService.list(new LambdaQueryWrapper<SysUserRole>()
                .eq(SysUserRole::getUserId, userId))
                .stream()
                .map(SysUserRole::getRoleId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (roleIds.isEmpty()) {
            addCurrentDept(scope, user.getDeptId());
            appendCompanyIds(scope);
            return scope;
        }

        List<SysRole> roles = roleService.listByIds(roleIds);
        if (roles.stream().anyMatch(this::isAllDataRole)) {
            scope.setAllData(true);
            return scope;
        }

        for (SysRole role : roles) {
            fillScopeByRole(scope, role, user.getDeptId());
        }

        if (scope.getDeptIds().isEmpty()) {
            addCurrentDept(scope, user.getDeptId());
        }
        appendCompanyIds(scope);
        return scope;
    }

    /**
     * 判断角色是否拥有全部数据权限。
     *
     * @param role 角色对象
     * @return true 表示全部数据权限
     */
    private boolean isAllDataRole(SysRole role) {
        if (role == null) {
            return false;
        }
        return "1".equals(role.getDataScope()) || "admin".equals(role.getRoleKey());
    }

    /**
     * 按角色配置填充可访问的部门范围。
     *
     * @param scope          数据权限范围对象
     * @param role           角色对象
     * @param currentDeptId  当前用户部门ID
     */
    private void fillScopeByRole(DataPermissionScope scope, SysRole role, Long currentDeptId) {
        if (role == null || !StringUtils.hasText(role.getDataScope())) {
            addCurrentDept(scope, currentDeptId);
            return;
        }
        switch (role.getDataScope()) {
            case "2":
                addCustomDeptScope(scope, role.getRoleId());
                break;
            case "3":
                addCurrentDept(scope, currentDeptId);
                break;
            case "4":
                addCurrentAndChildren(scope, currentDeptId);
                break;
            default:
                addCurrentDept(scope, currentDeptId);
                break;
        }
    }

    /**
     * 添加自定义角色部门数据范围。
     *
     * @param scope  数据权限范围对象
     * @param roleId 角色ID
     */
    private void addCustomDeptScope(DataPermissionScope scope, Long roleId) {
        if (roleId == null) {
            return;
        }
        Set<Long> customDeptIds = roleDeptService.list(new LambdaQueryWrapper<SysRoleDept>()
                .eq(SysRoleDept::getRoleId, roleId))
                .stream()
                .map(SysRoleDept::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        scope.getDeptIds().addAll(customDeptIds);
    }

    /**
     * 添加当前部门及其子部门的数据范围。
     *
     * @param scope         数据权限范围对象
     * @param currentDeptId 当前部门ID
     */
    private void addCurrentAndChildren(DataPermissionScope scope, Long currentDeptId) {
        if (currentDeptId == null) {
            return;
        }
        List<SysDept> allDepts = deptService.list();
        for (SysDept dept : allDepts) {
            if (dept.getDeptId() == null) {
                continue;
            }
            if (currentDeptId.equals(dept.getDeptId()) || containsAncestor(dept.getAncestors(), currentDeptId)) {
                scope.getDeptIds().add(dept.getDeptId());
            }
        }
    }

    /**
     * 添加当前部门数据范围。
     *
     * @param scope         数据权限范围对象
     * @param currentDeptId 当前部门ID
     */
    private void addCurrentDept(DataPermissionScope scope, Long currentDeptId) {
        if (currentDeptId != null) {
            scope.getDeptIds().add(currentDeptId);
        }
    }

    /**
     * 将可访问部门映射为可访问公司范围。
     *
     * @param scope 数据权限范围对象
     */
    private void appendCompanyIds(DataPermissionScope scope) {
        if (scope.getDeptIds().isEmpty()) {
            return;
        }
        List<SysDept> depts = deptService.listByIds(scope.getDeptIds());
        Set<Long> companyIds = depts.stream()
                .map(SysDept::getCompanyId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        scope.getCompanyIds().addAll(companyIds);
    }

    /**
     * 判断指定祖级列表中是否包含某个部门ID。
     *
     * @param ancestors 祖级列表
     * @param deptId    部门ID
     * @return true 表示包含
     */
    private boolean containsAncestor(String ancestors, Long deptId) {
        if (!StringUtils.hasText(ancestors) || deptId == null) {
            return false;
        }
        List<String> ancestorList = Arrays.asList(ancestors.split(","));
        return ancestorList.contains(String.valueOf(deptId));
    }
}
