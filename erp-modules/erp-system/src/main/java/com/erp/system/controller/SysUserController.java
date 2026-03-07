package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.SysUser;
import com.erp.system.service.ISysUserService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.erp.system.service.ISysMenuService;
import com.erp.system.service.ISysRoleService;
import java.util.HashMap;
import java.util.Map;

/**
 * 用户管理控制层
 */
@RestController
@RequestMapping("/system/user")
public class SysUserController {

    private final ISysUserService userService;
    private final ISysRoleService roleService;
    private final ISysMenuService menuService;

    public SysUserController(ISysUserService userService, ISysRoleService roleService, ISysMenuService menuService) {
        this.userService = userService;
        this.roleService = roleService;
        this.menuService = menuService;
    }

    /**
     * 获取当前用户信息（包含角色与权限）
     * 实际应用中应从 SecurityContext 获取 userId，此处暂用写死或参数模拟
     */
    @GetMapping("/getInfo")
    public R<Map<String, Object>> getInfo() {
        // TODO: 从安全上下文获取当前用户ID
        Long userId = 1L;
        SysUser user = userService.getById(userId);

        Map<String, Object> ajax = new HashMap<>();
        ajax.put("user", user);
        ajax.put("roles", roleService.selectRoleKeysByUserId(userId));
        ajax.put("permissions", menuService.selectMenuPermsByUserId(userId));
        return R.success(ajax);
    }

    /**
     * 获取路由信息
     */
    @GetMapping("/getRouters")
    public R<List<SysMenu>> getRouters() {
        // TODO: 从安全上下文获取当前用户ID
        Long userId = 1L;
        return R.success(menuService.selectMenuTreeByUserId(userId));
    }

    /**
     * 查询用户列表
     */
    @GetMapping("/list")
    public R<List<SysUser>> list() {
        return R.success(userService.list());
    }

    /**
     * 获取用户详细信息
     */
    @GetMapping("/{userId}")
    public R<SysUser> getInfo(@PathVariable("userId") Long userId) {
        return R.success(userService.getById(userId));
    }

    /**
     * 新增用户
     */
    @PostMapping
    public R<Boolean> add(@RequestBody SysUser user) {
        return R.success(userService.save(user));
    }

    /**
     * 修改用户
     */
    @PutMapping
    public R<Boolean> edit(@RequestBody SysUser user) {
        return R.success(userService.updateById(user));
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{userId}")
    public R<Boolean> remove(@PathVariable("userId") Long userId) {
        return R.success(userService.removeById(userId));
    }
}
