package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysRole;
import com.erp.system.service.ISysRoleService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制层
 */
@RestController
@RequestMapping("/system/role")
public class SysRoleController {

    private final ISysRoleService roleService;

    public SysRoleController(ISysRoleService roleService) {
        this.roleService = roleService;
    }

    /**
     * 查询角色列表
     */
    @PreAuthorize("@ss.hasPermi('system:role:list')")
    @GetMapping("/list")
    public R<List<SysRole>> list() {
        return R.success(roleService.list());
    }

    /**
     * 获取角色详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:role:query')")
    @GetMapping("/{roleId}")
    public R<SysRole> getInfo(@PathVariable("roleId") Long roleId) {
        return R.success(roleService.getRoleWithPermissions(roleId));
    }

    /**
     * 新增角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysRole role) {
        return R.success(roleService.save(role));
    }

    /**
     * 修改角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysRole role) {
        return R.success(roleService.updateById(role));
    }

    /**
     * 分配角色数据权限。
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping("/dataScope")
    public R<Boolean> dataScope(@RequestBody SysRole role) {
        if (role == null || role.getRoleId() == null || !StringUtils.hasText(role.getDataScope())) {
            return R.failed("角色ID和数据权限范围不能为空");
        }
        boolean success = roleService.updateDataScope(role);
        return success ? R.success(true) : R.failed("分配数据权限失败");
    }

    /**
     * 删除角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:remove')")
    @DeleteMapping("/{roleId}")
    public R<Boolean> remove(@PathVariable("roleId") Long roleId) {
        return R.success(roleService.removeById(roleId));
    }
}
