package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysTenant;
import com.erp.system.service.ISysTenantService;
import com.erp.system.support.StatusFieldSupport;
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
        return R.success(normalizeTenantList(tenantService.list()));
    }

    /**
     * 获取租户详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:query')")
    @GetMapping("/{id}")
    public R<SysTenant> getInfo(@PathVariable("id") Long id) {
        return R.success(normalizeTenant(tenantService.getById(id)));
    }

    /**
     * 新增租户
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysTenant tenant) {
        if (tenant == null) {
            return R.failed("租户参数不能为空");
        }
        tenant.setStatus(StatusFieldSupport.normalizeBinaryStatus(tenant.getStatus()));
        return R.success(tenantService.save(tenant));
    }

    /**
     * 修改租户
     */
    @PreAuthorize("@ss.hasPermi('system:tenant:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysTenant tenant) {
        if (tenant == null || tenant.getId() == null) {
            return R.failed("租户ID不能为空");
        }
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

    /**
     * 规范租户列表中的状态字段，避免前端出现空白状态。
     *
     * @param tenantList 租户列表
     * @return 状态字段已规范化的租户列表
     */
    private List<SysTenant> normalizeTenantList(List<SysTenant> tenantList) {
        if (tenantList == null || tenantList.isEmpty()) {
            return tenantList;
        }
        for (SysTenant tenant : tenantList) {
            normalizeTenant(tenant);
        }
        return tenantList;
    }

    /**
     * 规范租户状态字段。
     *
     * @param tenant 租户对象
     * @return 规范化后的租户对象
     */
    private SysTenant normalizeTenant(SysTenant tenant) {
        if (tenant != null) {
            tenant.setStatus(StatusFieldSupport.normalizeBinaryStatus(tenant.getStatus()));
        }
        return tenant;
    }
}
