package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysCompany;
import com.erp.system.service.ISysCompanyService;
import com.erp.system.support.StatusFieldSupport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 公司管理控制层
 */
@Tag(name = "公司管理")
@RestController
@RequestMapping("/system/company")
@RequiredArgsConstructor
public class SysCompanyController {

    private final ISysCompanyService companyService;

    @Operation(summary = "查询公司列表")
    @PreAuthorize("@ss.hasPermi('system:company:list')")
    @GetMapping("/list")
    public R<List<SysCompany>> list() {
        return R.success(normalizeCompanyList(companyService.list()));
    }

    @Operation(summary = "查询公司树")
    @PreAuthorize("@ss.hasPermi('system:company:list')")
    @GetMapping("/tree")
    public R<List<SysCompany>> tree() {
        List<SysCompany> companyList = normalizeCompanyList(companyService.list());
        return R.success(companyService.buildCompanyTree(companyList));
    }

    @Operation(summary = "查询公司详情")
    @PreAuthorize("@ss.hasPermi('system:company:query')")
    @GetMapping("/{companyId}")
    public R<SysCompany> getInfo(@PathVariable("companyId") Long companyId) {
        return R.success(normalizeCompany(companyService.getById(companyId)));
    }

    @Operation(summary = "新增公司")
    @PreAuthorize("@ss.hasPermi('system:company:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysCompany company) {
        if (company == null) {
            return R.failed("公司参数不能为空");
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
        boolean success = companyService.updateCompany(company);
        return success ? R.success(true) : R.failed("修改公司失败，请检查上级公司配置");
    }

    @Operation(summary = "删除公司")
    @PreAuthorize("@ss.hasPermi('system:company:remove')")
    @DeleteMapping("/{companyId}")
    public R<Boolean> remove(@PathVariable("companyId") Long companyId) {
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
}
