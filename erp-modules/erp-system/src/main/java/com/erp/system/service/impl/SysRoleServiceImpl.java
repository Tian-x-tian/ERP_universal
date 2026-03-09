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
        List<SysRoleMenu> list = role.getMenuIds().stream().map(menuId -> {
            SysRoleMenu rm = new SysRoleMenu();
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
        List<SysRoleDept> list = role.getDeptIds().stream().map(deptId -> {
            SysRoleDept rd = new SysRoleDept();
            rd.setTenantId(role.getTenantId());
            rd.setRoleId(role.getRoleId());
            rd.setDeptId(deptId);
            return rd;
        }).collect(Collectors.toList());
        roleDeptService.saveBatch(list);
    }
}
