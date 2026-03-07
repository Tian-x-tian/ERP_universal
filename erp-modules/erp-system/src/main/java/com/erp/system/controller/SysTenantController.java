package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysTenant;
import com.erp.system.service.ISysTenantService;
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
    @GetMapping("/list")
    public R<List<SysTenant>> list() {
        return R.success(tenantService.list());
    }

    /**
     * 获取租户详细信息
     */
    @GetMapping("/{id}")
    public R<SysTenant> getInfo(@PathVariable("id") Long id) {
        return R.success(tenantService.getById(id));
    }

    /**
     * 新增租户
     */
    @PostMapping
    public R<Boolean> add(@RequestBody SysTenant tenant) {
        return R.success(tenantService.save(tenant));
    }

    /**
     * 修改租户
     */
    @PutMapping
    public R<Boolean> edit(@RequestBody SysTenant tenant) {
        return R.success(tenantService.updateById(tenant));
    }

    /**
     * 删除租户
     */
    @DeleteMapping("/{id}")
    public R<Boolean> remove(@PathVariable("id") Long id) {
        return R.success(tenantService.removeById(id));
    }
}
