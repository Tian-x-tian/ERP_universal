package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysTenant;
import com.erp.system.service.ISysTenantService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 租户管理控制层
 */
@RestController
@RequestMapping("/system/tenant")
public class SysTenantController {

    private final ISysTenantService tenantService;

    public SysTenantController(ISysTenantService tenantService) {
        this.tenantService = tenantService;
    }

    /**
     * 查询租户列表
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:list')")
    @GetMapping("/list")
    public R<List<SysTenant>> list() {
        return R.success(tenantService.list());
    }

    /**
     * 获取租户详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:query')")
    @GetMapping("/{id}")
    public R<SysTenant> getInfo(@PathVariable("id") Long id) {
        return R.success(tenantService.getById(id));
    }

    /**
     * 新增租户
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysTenant tenant) {
        return R.success(tenantService.save(tenant));
    }

    /**
     * 修改租户
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysTenant tenant) {
        return R.success(tenantService.updateById(tenant));
    }

    /**
     * 删除租户
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:remove')")
    @DeleteMapping("/{id}")
    public R<Boolean> remove(@PathVariable("id") Long id) {
        return R.success(tenantService.removeById(id));
    }
}
