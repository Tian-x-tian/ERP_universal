package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysRole;
import com.erp.system.domain.vo.DataPermissionScope;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IDataPermissionService;
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
    private final IDataPermissionService dataPermissionService;
    private final SecurityUserResolver securityUserResolver;

    public SysRoleController(ISysRoleService roleService,
            IDataPermissionService dataPermissionService,
            SecurityUserResolver securityUserResolver) {
        this.roleService = roleService;
        this.dataPermissionService = dataPermissionService;
        this.securityUserResolver = securityUserResolver;
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
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        DataPermissionScope currentScope = dataPermissionService.resolveDataScope(currentUserId);
        R<Boolean> validationResult = validateRoleDataScope(role, currentScope);
        if (validationResult != null) {
            return validationResult;
        }
        boolean success = roleService.updateDataScope(role);
        return success ? R.success(true) : R.failed("分配数据权限失败");
    }

    /**
     * 校验角色数据权限分配是否超出当前用户可授权范围。
     *
     * @param role         角色数据权限参数
     * @param currentScope 当前用户数据权限范围
     * @return 校验失败返回失败结果，校验通过返回 null
     */
    private R<Boolean> validateRoleDataScope(SysRole role, DataPermissionScope currentScope) {
        if (currentScope == null) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        if (currentScope.isAllData()) {
            return null;
        }
        String dataScope = role.getDataScope();
        if ("1".equals(dataScope)) {
            return R.failed("当前账号不能分配全部数据权限");
        }
        if ("2".equals(dataScope)) {
            if (role.getDeptIds() == null || role.getDeptIds().isEmpty()) {
                return null;
            }
            boolean hasOutOfScopeDept = role.getDeptIds().stream()
                    .anyMatch(deptId -> deptId == null || !currentScope.getDeptIds().contains(deptId));
            if (hasOutOfScopeDept) {
                return R.failed("包含超出当前账号数据范围的部门");
            }
            return null;
        }
        if ("3".equals(dataScope) || "4".equals(dataScope)) {
            return null;
        }
        return R.failed("无效的数据权限范围");
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
