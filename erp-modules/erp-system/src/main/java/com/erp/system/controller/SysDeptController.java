package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.vo.DataPermissionScope;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IDataPermissionService;
import com.erp.system.service.ISysDeptService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * 部门管理控制层
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final ISysDeptService deptService;
    private final IDataPermissionService dataPermissionService;
    private final SecurityUserResolver securityUserResolver;

    public SysDeptController(ISysDeptService deptService,
            IDataPermissionService dataPermissionService,
            SecurityUserResolver securityUserResolver) {
        this.deptService = deptService;
        this.dataPermissionService = dataPermissionService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询部门列表
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/list")
    public R<List<SysDept>> list(SysDept dept) {
        DataPermissionScope dataScope = dataPermissionService.resolveDataScope(resolveCurrentUserId());
        if (dataScope.isAllData()) {
            return R.success(deptService.list());
        }
        if (dataScope.getDeptIds().isEmpty()) {
            return R.success(Collections.emptyList());
        }
        return R.success(deptService.list(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getDeptId, dataScope.getDeptIds())));
    }

    /**
     * 查询部门树
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        DataPermissionScope dataScope = dataPermissionService.resolveDataScope(resolveCurrentUserId());
        if (dataScope.isAllData()) {
            return R.success(deptService.buildDeptTree(deptService.list()));
        }
        if (dataScope.getDeptIds().isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<SysDept> deptList = deptService.list(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getDeptId, dataScope.getDeptIds()));
        return R.success(deptService.buildDeptTree(deptList));
    }

    /**
     * 获取部门详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping("/{deptId}")
    public R<SysDept> getInfo(@PathVariable("deptId") Long deptId) {
        return R.success(deptService.getById(deptId));
    }

    /**
     * 新增部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysDept dept) {
        return R.success(deptService.save(dept));
    }

    /**
     * 修改部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysDept dept) {
        return R.success(deptService.updateById(dept));
    }

    /**
     * 删除部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public R<Boolean> remove(@PathVariable("deptId") Long deptId) {
        return R.success(deptService.removeById(deptId));
    }

    /**
     * 获取当前登录用户ID，解析失败时回退为默认管理员。
     *
     * @return 当前用户ID
     */
    private Long resolveCurrentUserId() {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        return currentUserId != null ? currentUserId : 1L;
    }
}
