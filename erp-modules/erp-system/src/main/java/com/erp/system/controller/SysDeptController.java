package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysDept;
import com.erp.system.service.ISysDeptService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制层
 */
@RestController
@RequestMapping("/system/dept")
public class SysDeptController {

    private final ISysDeptService deptService;

    public SysDeptController(ISysDeptService deptService) {
        this.deptService = deptService;
    }

    /**
     * 查询部门列表
     */
    @GetMapping("/list")
    public R<List<SysDept>> list(SysDept dept) {
        return R.success(deptService.list());
    }

    /**
     * 查询部门树
     */
    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        return R.success(deptService.buildDeptTree(deptService.list()));
    }

    /**
     * 获取部门详细信息
     */
    @GetMapping("/{deptId}")
    public R<SysDept> getInfo(@PathVariable("deptId") Long deptId) {
        return R.success(deptService.getById(deptId));
    }

    /**
     * 新增部门
     */
    @PostMapping
    public R<Boolean> add(@RequestBody SysDept dept) {
        return R.success(deptService.save(dept));
    }

    /**
     * 修改部门
     */
    @PutMapping
    public R<Boolean> edit(@RequestBody SysDept dept) {
        return R.success(deptService.updateById(dept));
    }

    /**
     * 删除部门
     */
    @DeleteMapping("/{deptId}")
    public R<Boolean> remove(@PathVariable("deptId") Long deptId) {
        return R.success(deptService.removeById(deptId));
    }
}
