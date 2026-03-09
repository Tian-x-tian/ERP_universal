package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysDept;
import com.erp.system.domain.vo.DataPermissionScope;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IDataPermissionService;
import com.erp.system.service.ISysDeptService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
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
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        DataPermissionScope dataScope = dataPermissionService.resolveDataScope(currentUserId);
        if (dataScope.isAllData()) {
            return R.success(normalizeDeptList(deptService.list()));
        }
        if (dataScope.getDeptIds().isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<SysDept> deptList = deptService.list(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getDeptId, dataScope.getDeptIds()));
        return R.success(normalizeDeptList(deptList));
    }

    /**
     * 查询部门树
     */
    @PreAuthorize("@ss.hasPermi('system:dept:list')")
    @GetMapping("/tree")
    public R<List<SysDept>> tree() {
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        DataPermissionScope dataScope = dataPermissionService.resolveDataScope(currentUserId);
        if (dataScope.isAllData()) {
            List<SysDept> deptList = normalizeDeptList(deptService.list());
            return R.success(deptService.buildDeptTree(deptList));
        }
        if (dataScope.getDeptIds().isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<SysDept> deptList = normalizeDeptList(deptService.list(new LambdaQueryWrapper<SysDept>()
                .in(SysDept::getDeptId, dataScope.getDeptIds())));
        return R.success(deptService.buildDeptTree(deptList));
    }

    /**
     * 获取部门详细信息
     */
    @PreAuthorize("@ss.hasPermi('system:dept:query')")
    @GetMapping("/{deptId}")
    public R<SysDept> getInfo(@PathVariable("deptId") Long deptId) {
        Long currentUserId = resolveCurrentUserId();
        if (currentUserId == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        DataPermissionScope dataScope = dataPermissionService.resolveDataScope(currentUserId);
        if (!canAccessDept(dataScope, deptId)) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(normalizeDept(deptService.getById(deptId)));
    }

    /**
     * 新增部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysDept dept) {
        if (dept == null || !StringUtils.hasText(dept.getDeptName())) {
            return R.failed("部门名称不能为空");
        }
        if (dept.getParentId() == null) {
            return R.failed("上级部门不能为空");
        }
        dept.setStatus(StatusFieldSupport.normalizeBinaryStatus(dept.getStatus()));
        boolean success = deptService.createDept(dept);
        return success ? R.success(true) : R.failed("新增部门失败，请检查上级部门是否存在");
    }

    /**
     * 修改部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysDept dept) {
        if (dept == null || dept.getDeptId() == null) {
            return R.failed("部门ID不能为空");
        }
        boolean success = deptService.updateDept(dept);
        return success ? R.success(true) : R.failed("修改部门失败，请检查上级部门配置");
    }

    /**
     * 删除部门
     */
    @PreAuthorize("@ss.hasPermi('system:dept:remove')")
    @DeleteMapping("/{deptId}")
    public R<Boolean> remove(@PathVariable("deptId") Long deptId) {
        long childCount = deptService.count(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getParentId, deptId));
        if (childCount > 0) {
            return R.failed("存在下级部门，不能直接删除");
        }
        return R.success(deptService.removeById(deptId));
    }

    /**
     * 获取当前登录用户ID。
     *
     * @return 当前用户ID，未登录时返回 null
     */
    private Long resolveCurrentUserId() {
        return securityUserResolver.getCurrentUserId();
    }

    /**
     * 校验目标部门是否在当前数据权限范围内。
     *
     * @param dataScope 数据权限范围
     * @param deptId    部门ID
     * @return true 表示允许访问
     */
    private boolean canAccessDept(DataPermissionScope dataScope, Long deptId) {
        if (deptId == null) {
            return false;
        }
        if (dataScope == null) {
            return false;
        }
        if (dataScope.isAllData()) {
            return true;
        }
        return dataScope.getDeptIds().contains(deptId);
    }

    /**
     * 规范部门列表中的状态字段，避免前端出现空白状态。
     *
     * @param deptList 部门列表
     * @return 状态字段已规范化的部门列表
     */
    private List<SysDept> normalizeDeptList(List<SysDept> deptList) {
        if (deptList == null || deptList.isEmpty()) {
            return deptList;
        }
        for (SysDept dept : deptList) {
            normalizeDept(dept);
        }
        return deptList;
    }

    /**
     * 规范部门状态字段。
     *
     * @param dept 部门对象
     * @return 规范化后的部门对象
     */
    private SysDept normalizeDept(SysDept dept) {
        if (dept != null) {
            dept.setStatus(StatusFieldSupport.normalizeBinaryStatus(dept.getStatus()));
        }
        return dept;
    }
}
