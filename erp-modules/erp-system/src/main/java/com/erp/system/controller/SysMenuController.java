package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysMenu;
import com.erp.system.service.ISysMenuService;
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
    @GetMapping("/list")
    public R<List<SysMenu>> list() {
        return R.success(menuService.list());
    }

    /**
     * 获取菜单详细信息
     */
    @GetMapping("/{menuId}")
    public R<SysMenu> getInfo(@PathVariable("menuId") Long menuId) {
        return R.success(menuService.getById(menuId));
    }

    /**
     * 新增菜单
     */
    @PostMapping
    public R<Boolean> add(@RequestBody SysMenu menu) {
        return R.success(menuService.save(menu));
    }

    /**
     * 修改菜单
     */
    @PutMapping
    public R<Boolean> edit(@RequestBody SysMenu menu) {
        return R.success(menuService.updateById(menu));
    }

    /**
     * 删除菜单
     */
    @DeleteMapping("/{menuId}")
    public R<Boolean> remove(@PathVariable("menuId") Long menuId) {
        return R.success(menuService.removeById(menuId));
    }
}
