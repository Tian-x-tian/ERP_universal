package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.common.core.domain.ResultCode;
import com.erp.system.domain.SysCompany;
import com.erp.system.domain.vo.DataPermissionScope;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IDataPermissionService;
import com.erp.system.service.ISysCompanyService;
import com.erp.system.support.StatusFieldSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Collections;

/**
 * 公司管理控制层
 */
@Tag(name = "公司管理")
@RestController
@RequestMapping("/system/company")
@RequiredArgsConstructor
public class SysCompanyController {

    private final ISysCompanyService companyService;
    private final IDataPermissionService dataPermissionService;
    private final SecurityUserResolver securityUserResolver;

    @Operation(summary = "查询公司列表")
    @PreAuthorize("@ss.hasPermi('system:company:list')")
    @GetMapping("/list")
    public R<List<SysCompany>> list() {
        DataPermissionScope dataScope = resolveCompanyDataScope();
        if (dataScope == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        if (dataScope.isAllData()) {
            return R.success(normalizeCompanyList(companyService.list()));
        }
        if (dataScope.getCompanyIds().isEmpty()) {
            return R.success(Collections.emptyList());
        }
        List<SysCompany> companyList = companyService.list(new LambdaQueryWrapper<SysCompany>()
                .in(SysCompany::getCompanyId, dataScope.getCompanyIds()));
        return R.success(normalizeCompanyList(companyList));
    }

    @Operation(summary = "查询公司树")
    @PreAuthorize("@ss.hasPermi('system:company:list')")
    @GetMapping("/tree")
    public R<List<SysCompany>> tree() {
        DataPermissionScope dataScope = resolveCompanyDataScope();
        if (dataScope == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        List<SysCompany> companyList;
        if (dataScope.isAllData()) {
            companyList = normalizeCompanyList(companyService.list());
        } else if (dataScope.getCompanyIds().isEmpty()) {
            companyList = Collections.emptyList();
        } else {
            companyList = normalizeCompanyList(companyService.list(new LambdaQueryWrapper<SysCompany>()
                    .in(SysCompany::getCompanyId, dataScope.getCompanyIds())));
        }
        return R.success(companyService.buildCompanyTree(companyList));
    }

    @Operation(summary = "查询公司详情")
    @PreAuthorize("@ss.hasPermi('system:company:query')")
    @GetMapping("/{companyId}")
    public R<SysCompany> getInfo(@PathVariable("companyId") Long companyId) {
        DataPermissionScope dataScope = resolveCompanyDataScope();
        if (dataScope == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        if (!dataScope.isAllData() && (companyId == null || !dataScope.getCompanyIds().contains(companyId))) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        return R.success(normalizeCompany(companyService.getById(companyId)));
    }

    @Operation(summary = "新增公司")
    @PreAuthorize("@ss.hasPermi('system:company:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysCompany company) {
        if (company == null) {
            return R.failed("公司参数不能为空");
        }
        DataPermissionScope dataScope = resolveCompanyDataScope();
        if (dataScope == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        if (!canWriteCompany(dataScope, company, null)) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        company.setStatus(StatusFieldSupport.normalizeBinaryStatus(company.getStatus()));
        boolean success = companyService.createCompany(company);
        return success ? R.success(true) : R.failed("新增公司失败，请检查公司编码、名称和上级公司配置");
    }

    @Operation(summary = "修改公司")
    @PreAuthorize("@ss.hasPermi('system:company:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysCompany company) {
        if (company == null || company.getCompanyId() == null) {
            return R.failed("公司ID不能为空");
        }
        DataPermissionScope dataScope = resolveCompanyDataScope();
        if (dataScope == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        SysCompany existedCompany = companyService.getById(company.getCompanyId());
        if (existedCompany == null) {
            return R.failed("公司不存在");
        }
        if (!canAccessCompany(dataScope, existedCompany.getCompanyId())
                || !canWriteCompany(dataScope, company, existedCompany)) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        boolean success = companyService.updateCompany(company);
        return success ? R.success(true) : R.failed("修改公司失败，请检查上级公司配置");
    }

    @Operation(summary = "删除公司")
    @PreAuthorize("@ss.hasPermi('system:company:remove')")
    @DeleteMapping("/{companyId}")
    public R<Boolean> remove(@PathVariable("companyId") Long companyId) {
        DataPermissionScope dataScope = resolveCompanyDataScope();
        if (dataScope == null) {
            return R.failed(ResultCode.UNAUTHORIZED);
        }
        if (!canAccessCompany(dataScope, companyId)) {
            return R.failed(ResultCode.FORBIDDEN);
        }
        long childCount = companyService.count(new LambdaQueryWrapper<SysCompany>()
                .eq(SysCompany::getParentCompanyId, companyId));
        if (childCount > 0) {
            return R.failed("存在下级公司，不能直接删除");
        }
        return R.success(companyService.removeById(companyId));
    }

    /**
     * 规范公司列表中的状态字段，避免前端出现空白状态。
     *
     * @param companyList 公司列表
     * @return 状态字段已规范化的公司列表
     */
    private List<SysCompany> normalizeCompanyList(List<SysCompany> companyList) {
        if (companyList == null || companyList.isEmpty()) {
            return companyList;
        }
        for (SysCompany company : companyList) {
            normalizeCompany(company);
        }
        return companyList;
    }

    /**
     * 规范公司状态字段。
     *
     * @param company 公司对象
     * @return 规范化后的公司对象
     */
    private SysCompany normalizeCompany(SysCompany company) {
        if (company != null) {
            company.setStatus(StatusFieldSupport.normalizeBinaryStatus(company.getStatus()));
        }
        return company;
    }

    /**
     * 解析当前用户可访问的公司数据范围。
     *
     * @return 数据权限范围，未登录时返回 null
     */
    private DataPermissionScope resolveCompanyDataScope() {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return null;
        }
        return dataPermissionService.resolveDataScope(currentUserId);
    }

    /**
     * 校验公司是否在当前数据权限范围内。
     *
     * @param dataScope 数据权限范围
     * @param companyId 公司ID
     * @return true 表示允许访问
     */
    private boolean canAccessCompany(DataPermissionScope dataScope, Long companyId) {
        if (companyId == null || dataScope == null) {
            return false;
        }
        return dataScope.isAllData() || dataScope.getCompanyIds().contains(companyId);
    }

    /**
     * 校验公司写操作是否落在当前数据权限范围内。
     *
     * @param dataScope      数据权限范围
     * @param requestCompany 请求公司对象
     * @param existedCompany 已存在公司对象
     * @return true 表示允许写入
     */
    private boolean canWriteCompany(DataPermissionScope dataScope, SysCompany requestCompany, SysCompany existedCompany) {
        if (dataScope == null || requestCompany == null) {
            return false;
        }
        if (dataScope.isAllData()) {
            return true;
        }
        Long parentCompanyId = requestCompany.getParentCompanyId();
        if (parentCompanyId == null && existedCompany != null) {
            parentCompanyId = existedCompany.getParentCompanyId();
        }
        if (parentCompanyId == null || parentCompanyId == 0L) {
            return false;
        }
        return dataScope.getCompanyIds().contains(parentCompanyId);
    }
}
