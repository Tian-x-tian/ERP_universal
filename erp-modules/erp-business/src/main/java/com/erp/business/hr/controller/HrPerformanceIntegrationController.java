package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrPerformanceFieldMapping;
import com.erp.business.hr.domain.HrPerformanceSyncLog;
import com.erp.business.hr.domain.vo.HrPerformanceCallbackBody;
import com.erp.business.hr.domain.vo.HrPerformancePushBody;
import com.erp.business.hr.service.IHrPerformanceIntegrationService;
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
 * 绩效考核同步控制层。
 */
@RestController
@RequestMapping({"/business/hr/performance", "/business/hr/integration/performance"})
public class HrPerformanceIntegrationController {
    private final IHrPerformanceIntegrationService performanceIntegrationService;

    public HrPerformanceIntegrationController(IHrPerformanceIntegrationService performanceIntegrationService) {
        this.performanceIntegrationService = performanceIntegrationService;
    }

    /**
     * 查询字段映射。
     *
     * @return 映射列表
     */
    @GetMapping("/mapping")
    @PreAuthorize("@ss.hasPermi('business:hr:performance:list')")
    public R<List<HrPerformanceFieldMapping>> mapping() {
        return R.success(performanceIntegrationService.listMappings());
    }

    /**
     * 保存字段映射。
     *
     * @param mappings 映射列表
     * @return 最新配置
     */
    @PutMapping("/mapping")
    @PreAuthorize("@ss.hasPermi('business:hr:performance:config')")
    public R<List<HrPerformanceFieldMapping>> saveMapping(@RequestBody List<HrPerformanceFieldMapping> mappings) {
        return R.success(performanceIntegrationService.saveMappings(mappings));
    }

    /**
     * 发起绩效推送。
     *
     * @param body 推送参数
     * @return 同步日志
     */
    @PostMapping("/push")
    @PreAuthorize("@ss.hasPermi('business:hr:performance:push')")
    public R<List<HrPerformanceSyncLog>> push(@RequestBody HrPerformancePushBody body) {
        return R.success(performanceIntegrationService.pushPerformance(body));
    }

    /**
     * 处理绩效回传。
     *
     * @param body 回传参数
     * @return 同步日志
     */
    @PostMapping("/callback")
    public R<HrPerformanceSyncLog> callback(@RequestBody HrPerformanceCallbackBody body) {
        return R.success(performanceIntegrationService.callback(body));
    }

    /**
     * 分页查询同步日志。
     *
     * @param periodCode 期间
     * @param syncStatus 状态
     * @param employeeId 员工ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/log/list")
    @PreAuthorize("@ss.hasPermi('business:hr:performance:list')")
    public R<PageData<HrPerformanceSyncLog>> logList(@RequestParam(value = "periodCode", required = false) String periodCode,
            @RequestParam(value = "syncStatus", required = false) String syncStatus,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<HrPerformanceSyncLog> page = performanceIntegrationService.selectLogPage(periodCode, syncStatus, employeeId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 重试绩效同步。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    @PostMapping("/retry/{logId}")
    @PreAuthorize("@ss.hasPermi('business:hr:performance:retry')")
    public R<HrPerformanceSyncLog> retry(@PathVariable("logId") Long logId) {
        return R.success(performanceIntegrationService.retry(logId));
    }
}

