package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysMenu;
import com.erp.system.domain.vo.SysMenuSyncNode;
import com.erp.system.service.ISysMenuService;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 菜单管理控制层
 */
@RestController
@RequestMapping("/system/menu")
public class SysMenuController {

    private final ISysMenuService menuService;

    public SysMenuController(ISysMenuService menuService) {
        this.menuService = menuService;
    }

    /**
     * 查询菜单列表
     */
    @PreAuthorize("@ss.hasPermi('system:menu:list')")
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.success(menuService.list());
    }

    /**
     * 获取菜单详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:menu:query')")
    @GetMapping("/{menuId}")
    public R<SysMenu> getInfo(@PathVariable("menuId") Long menuId) {
        return R.success(menuService.getById(menuId));
    }

    /**
     * 新增菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysMenu menu) {
        if (!TenantWriteGuard.canWriteGlobalData()) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(menuService.save(menu));
    }

    /**
     * 修改菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysMenu menu) {
        if (!TenantWriteGuard.canWriteGlobalData()) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(menuService.updateById(menu));
    }

    /**
     * 同步当前页面菜单结构。
     *
     * @param menuTree 菜单同步树
     * @return 同步处理记录数
     */
    @PreAuthorize("@ss.hasPermi('system:menu:edit')")
    @PostMapping("/sync")
    public R<Integer> sync(@RequestBody List<SysMenuSyncNode> menuTree) {
        if (!TenantWriteGuard.canWriteGlobalData()) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        if (menuTree == null || menuTree.isEmpty()) {
            return R.failed(ResultCode.PARAM_ERROR, "同步菜单数据不能为空");
        }
        return R.success(menuService.syncMenuTree(menuTree));
    }

    /**
     * 删除菜单
     */
    @PreAuthorize("@ss.hasPermi('system:menu:remove')")
    @DeleteMapping("/{menuId}")
    public R<Boolean> remove(@PathVariable("menuId") Long menuId) {
        if (!TenantWriteGuard.canWriteGlobalData()) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(menuService.removeById(menuId));
    }
}
