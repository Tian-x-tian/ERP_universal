package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
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
        SysRole updateEntity = new SysRole();
        updateEntity.setRoleId(role.getRoleId());
        updateEntity.setDataScope(role.getDataScope());
        updateEntity.setUpdateBy(role.getUpdateBy());
        updateEntity.setUpdateTime(new Date());
        boolean updated = super.updateById(updateEntity);
        if (!updated) {
            return false;
        }

        roleDeptService.remove(new LambdaQueryWrapper<SysRoleDept>().eq(SysRoleDept::getRoleId, role.getRoleId()));
        if ("2".equals(role.getDataScope()) && role.getDeptIds() != null && !role.getDeptIds().isEmpty()) {
            SysRole roleForDept = new SysRole();
            roleForDept.setRoleId(role.getRoleId());
            roleForDept.setTenantId(existedRole.getTenantId());
            roleForDept.setDeptIds(role.getDeptIds());
            insertRoleDept(roleForDept);
        }
        return true;
    }

    @Override
    @Transactional
    public boolean save(SysRole entity) {
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
     * 新增角色菜单信息
     */
    private void insertRoleMenu(SysRole role) {
        String tenantId = StringUtils.hasText(role.getTenantId()) ? role.getTenantId() : "000000";
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
        String tenantId = StringUtils.hasText(role.getTenantId()) ? role.getTenantId() : "000000";
        List<SysRoleDept> list = role.getDeptIds().stream().map(deptId -> {
            SysRoleDept rd = new SysRoleDept();
            rd.setTenantId(tenantId);
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            return rd;
        }).collect(Collectors.toList());
        roleDeptService.saveBatch(list);
    }
}
