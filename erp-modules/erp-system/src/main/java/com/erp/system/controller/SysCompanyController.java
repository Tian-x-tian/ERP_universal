package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysCompany;
import com.erp.system.service.ISysCompanyService;
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
        return R.success(companyService.list());
    }

    @Operation(summary = "查询公司详情")
    @PreAuthorize("@ss.hasPermi('system:company:query')")
    @GetMapping("/{companyId}")
    public R<SysCompany> getInfo(@PathVariable("companyId") Long companyId) {
        return R.success(companyService.getById(companyId));
    }

    @Operation(summary = "新增公司")
    @PreAuthorize("@ss.hasPermi('system:company:add')")
    @PostMapping
    public R<Boolean> add(@RequestBody SysCompany company) {
        return R.success(companyService.save(company));
    }

    @Operation(summary = "修改公司")
    @PreAuthorize("@ss.hasPermi('system:company:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody SysCompany company) {
        return R.success(companyService.updateById(company));
    }

    @Operation(summary = "删除公司")
    @PreAuthorize("@ss.hasPermi('system:company:remove')")
    @DeleteMapping("/{companyId}")
    public R<Boolean> remove(@PathVariable("companyId") Long companyId) {
        return R.success(companyService.removeById(companyId));
    }
}
