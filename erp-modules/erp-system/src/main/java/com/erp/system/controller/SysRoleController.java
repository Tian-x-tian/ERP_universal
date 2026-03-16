package com.erp.system.controller;

import com.erp.common.core.context.TenantContextHolder;
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
    private static final String PLATFORM_TENANT_ID = "000000";
    private static final String SUPER_ADMIN_ROLE_KEY = "admin";
    private static final String DEFAULT_DATA_SCOPE = "3";
    private static final String DEFAULT_STATUS = "0";
    private static final int DEFAULT_ROLE_SORT = 1;

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
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        applyRoleCreateDefaults(role);
        R<Boolean> validationResult = validateRoleWrite(role, null, currentUserId);
        if (validationResult != null) {
            return validationResult;
        }
        boolean success = roleService.save(role);
        return success ? R.success(true) : R.failed("新增角色失败");
    }

    /**
     * 修改角色
     */
    @PreAuthorize("@ss.hasPermi('system:role:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysRole role) {
        if (role == null || role.getRoleId() == null) {
            return R.failed("角色ID不能为空");
        }
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        SysRole existedRole = roleService.getById(role.getRoleId());
        if (existedRole == null) {
            return R.failed("角色不存在");
        }
        R<Boolean> validationResult = validateRoleWrite(role, existedRole, currentUserId);
        if (validationResult != null) {
            return validationResult;
        }
        boolean success = roleService.updateById(role);
        return success ? R.success(true) : R.failed("修改角色失败");
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
        if (roleId == null) {
            return R.failed("角色ID不能为空");
        }
        SysRole role = roleService.getById(roleId);
        if (role == null) {
            return R.failed("角色不存在");
        }
        if (isReservedPlatformAdminRole(resolveRoleKey(role, null), resolveTenantId(role, null))) {
            return R.failed("平台超级管理员角色不允许删除");
        }
        return R.success(roleService.removeById(roleId));
    }

    /**
     * 校验角色新增/修改请求，防止越权分配保留角色编码与数据权限。
     *
     * @param role          请求角色对象
     * @param existedRole   已存在角色对象
     * @param currentUserId 当前用户ID
     * @return 校验失败结果；通过时返回 null
     */
    private R<Boolean> validateRoleWrite(SysRole role, SysRole existedRole, Long currentUserId) {
        if (role == null) {
            return R.failed("角色参数不能为空");
        }
        String roleName = StringUtils.hasText(role.getRoleName())
                ? role.getRoleName().trim()
                : existedRole == null ? null : existedRole.getRoleName();
        if (!StringUtils.hasText(roleName)) {
            return R.failed("角色名称不能为空");
        }
        String tenantId = resolveTenantId(role, existedRole);
        String roleKey = resolveRoleKey(role, existedRole);
        if (SUPER_ADMIN_ROLE_KEY.equals(roleKey) && !PLATFORM_TENANT_ID.equals(tenantId)) {
            return R.failed("admin 角色编码仅允许平台租户使用");
        }
        DataPermissionScope currentScope = dataPermissionService.resolveDataScope(currentUserId);
        return validateRoleDataScope(buildRoleForValidation(role, existedRole), currentScope);
    }

    /**
     * 解析角色最终租户编号。
     *
     * @param role        请求角色对象
     * @param existedRole 已存在角色对象
     * @return 角色租户编号
     */
    private String resolveTenantId(SysRole role, SysRole existedRole) {
        String tenantId = role == null ? null : role.getTenantId();
        if (!StringUtils.hasText(tenantId) && existedRole != null) {
            tenantId = existedRole.getTenantId();
        }
        if (!StringUtils.hasText(tenantId)) {
            tenantId = TenantContextHolder.getTenantId();
        }
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 解析角色最终编码。
     *
     * @param role        请求角色对象
     * @param existedRole 已存在角色对象
     * @return 角色编码
     */
    private String resolveRoleKey(SysRole role, SysRole existedRole) {
        String roleKey = role == null ? null : role.getRoleKey();
        if (!StringUtils.hasText(roleKey) && existedRole != null) {
            roleKey = existedRole.getRoleKey();
        }
        return StringUtils.hasText(roleKey) ? roleKey.trim() : null;
    }

    /**
     * 判断是否为保留的平台超级管理员角色。
     *
     * @param roleKey  角色编码
     * @param tenantId 租户编号
     * @return true 表示平台超级管理员角色
     */
    private boolean isReservedPlatformAdminRole(String roleKey, String tenantId) {
        return SUPER_ADMIN_ROLE_KEY.equals(roleKey) && PLATFORM_TENANT_ID.equals(tenantId);
    }

    /**
     * 构建用于权限校验的角色对象，补齐编辑场景下未传的现有字段。
     *
     * @param role        请求角色对象
     * @param existedRole 已存在角色对象
     * @return 用于校验的角色对象
     */
    private SysRole buildRoleForValidation(SysRole role, SysRole existedRole) {
        if (existedRole == null) {
            return role;
        }
        SysRole roleForValidation = new SysRole();
        roleForValidation.setRoleId(role.getRoleId());
        roleForValidation.setTenantId(resolveTenantId(role, existedRole));
        roleForValidation.setRoleKey(resolveRoleKey(role, existedRole));
        roleForValidation.setRoleName(StringUtils.hasText(role.getRoleName()) ? role.getRoleName() : existedRole.getRoleName());
        roleForValidation.setDataScope(StringUtils.hasText(role.getDataScope()) ? role.getDataScope() : existedRole.getDataScope());
        roleForValidation.setDeptIds(role.getDeptIds() == null ? existedRole.getDeptIds() : role.getDeptIds());
        return roleForValidation;
    }

    /**
     * 为新增角色请求补齐默认值，兼容前端未传字段场景。
     *
     * @param role 角色请求对象
     */
    private void applyRoleCreateDefaults(SysRole role) {
        if (role == null) {
            return;
        }
        if (!StringUtils.hasText(role.getDataScope())) {
            role.setDataScope(DEFAULT_DATA_SCOPE);
        }
        if (!StringUtils.hasText(role.getStatus())) {
            role.setStatus(DEFAULT_STATUS);
        }
        if (role.getRoleSort() == null) {
            role.setRoleSort(DEFAULT_ROLE_SORT);
        }
    }
}
