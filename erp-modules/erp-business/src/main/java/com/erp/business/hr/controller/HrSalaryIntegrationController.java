package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrSalaryFieldMapping;
import com.erp.business.hr.domain.HrSalarySyncLog;
import com.erp.business.hr.domain.vo.HrSalaryCallbackBody;
import com.erp.business.hr.domain.vo.HrSalaryPushBody;
import com.erp.business.hr.service.IHrSalaryIntegrationService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 薪酬核算同步控制层。
 */
@RestController
@RequestMapping({"/business/hr/integration/salary", "/business/hr/payroll"})
public class HrSalaryIntegrationController {
    private final IHrSalaryIntegrationService salaryIntegrationService;

    public HrSalaryIntegrationController(IHrSalaryIntegrationService salaryIntegrationService) {
        this.salaryIntegrationService = salaryIntegrationService;
    }

    /**
     * 查询薪酬字段映射。
     *
     * @return 映射列表
     */
    @GetMapping("/mapping")
    @PreAuthorize("@ss.hasAnyPermi('business:hr:payroll:list','business:hr:integration:salary')")
    public R<List<HrSalaryFieldMapping>> mapping() {
        return R.success(salaryIntegrationService.listMappings());
    }

    /**
     * 保存薪酬字段映射。
     *
     * @param mappings 映射列表
     * @return 最新配置
     */
    @PutMapping("/mapping")
    @PreAuthorize("@ss.hasAnyPermi('business:hr:payroll:config','business:hr:integration:salary')")
    public R<List<HrSalaryFieldMapping>> saveMapping(@RequestBody List<HrSalaryFieldMapping> mappings) {
        return R.success(salaryIntegrationService.saveMappings(mappings));
    }

    /**
     * 发起薪酬推送。
     *
     * @param body 推送参数
     * @return 同步日志
     */
    @PostMapping("/push")
    @PreAuthorize("@ss.hasAnyPermi('business:hr:payroll:push','business:hr:integration:salary')")
    public R<List<HrSalarySyncLog>> push(@RequestBody HrSalaryPushBody body) {
        return R.success(salaryIntegrationService.pushSalary(body));
    }

    /**
     * 处理薪酬回传。
     *
     * @param body 回传参数
     * @return 同步日志
     */
    @PostMapping("/callback")
    public R<HrSalarySyncLog> callback(@RequestBody HrSalaryCallbackBody body) {
        return R.success(salaryIntegrationService.callback(body));
    }

    /**
     * 分页查询薪酬同步日志。
     *
     * @param periodCode 期间
     * @param syncStatus 状态
     * @param employeeId 员工ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/log/list")
    @PreAuthorize("@ss.hasAnyPermi('business:hr:payroll:list','business:hr:integration:salary')")
    public R<PageData<HrSalarySyncLog>> logList(@RequestParam(value = "periodCode", required = false) String periodCode,
            @RequestParam(value = "syncStatus", required = false) String syncStatus,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<HrSalarySyncLog> page = salaryIntegrationService.selectLogPage(periodCode, syncStatus, employeeId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 重试薪酬同步。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    @PostMapping("/retry/{logId}")
    @PreAuthorize("@ss.hasAnyPermi('business:hr:payroll:retry','business:hr:integration:salary')")
    public R<HrSalarySyncLog> retry(@PathVariable("logId") Long logId) {
        return R.success(salaryIntegrationService.retry(logId));
    }
}
