package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrAttendanceFieldMapping;
import com.erp.business.hr.domain.HrAttendanceSyncLog;
import com.erp.business.hr.domain.vo.HrAttendanceCallbackBody;
import com.erp.business.hr.domain.vo.HrAttendancePushBody;
import com.erp.business.hr.service.IHrAttendanceIntegrationService;
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
 * 出勤管理同步控制层。
 */
@RestController
@RequestMapping({"/business/hr/attendance", "/business/hr/integration/attendance"})
public class HrAttendanceIntegrationController {
    private final IHrAttendanceIntegrationService attendanceIntegrationService;

    public HrAttendanceIntegrationController(IHrAttendanceIntegrationService attendanceIntegrationService) {
        this.attendanceIntegrationService = attendanceIntegrationService;
    }

    /**
     * 查询字段映射。
     *
     * @return 映射列表
     */
    @GetMapping("/mapping")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:list')")
    public R<List<HrAttendanceFieldMapping>> mapping() {
        return R.success(attendanceIntegrationService.listMappings());
    }

    /**
     * 保存字段映射。
     *
     * @param mappings 映射列表
     * @return 最新配置
     */
    @PutMapping("/mapping")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:config')")
    public R<List<HrAttendanceFieldMapping>> saveMapping(@RequestBody List<HrAttendanceFieldMapping> mappings) {
        return R.success(attendanceIntegrationService.saveMappings(mappings));
    }

    /**
     * 发起出勤推送。
     *
     * @param body 推送参数
     * @return 同步日志
     */
    @PostMapping("/push")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:push')")
    public R<List<HrAttendanceSyncLog>> push(@RequestBody HrAttendancePushBody body) {
        return R.success(attendanceIntegrationService.pushAttendance(body));
    }

    /**
     * 处理出勤回传。
     *
     * @param body 回传参数
     * @return 同步日志
     */
    @PostMapping("/callback")
    public R<HrAttendanceSyncLog> callback(@RequestBody HrAttendanceCallbackBody body) {
        return R.success(attendanceIntegrationService.callback(body));
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
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:list')")
    public R<PageData<HrAttendanceSyncLog>> logList(@RequestParam(value = "periodCode", required = false) String periodCode,
            @RequestParam(value = "syncStatus", required = false) String syncStatus,
            @RequestParam(value = "employeeId", required = false) Long employeeId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<HrAttendanceSyncLog> page = attendanceIntegrationService.selectLogPage(periodCode, syncStatus, employeeId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 重试出勤同步。
     *
     * @param logId 日志ID
     * @return 最新日志
     */
    @PostMapping("/retry/{logId}")
    @PreAuthorize("@ss.hasPermi('business:hr:attendance:retry')")
    public R<HrAttendanceSyncLog> retry(@PathVariable("logId") Long logId) {
        return R.success(attendanceIntegrationService.retry(logId));
    }
}

