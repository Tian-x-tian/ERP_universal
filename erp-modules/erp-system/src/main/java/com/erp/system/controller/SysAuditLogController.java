package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysAuditLog;
import com.erp.system.service.ISysAuditLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 审计日志控制层
 */
@RestController
@RequestMapping("/system/audit/log")
public class SysAuditLogController {

    private final ISysAuditLogService auditLogService;

    public SysAuditLogController(ISysAuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * 查询审计日志列表。
     *
     * @param operator   操作人账号，支持模糊查询
     * @param requestUri 请求 URI，支持模糊查询
     * @param success    是否成功（1 成功，0 失败）
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 审计日志列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:audit:list')")
    public R<List<SysAuditLog>> list(
            @RequestParam(value = "operator", required = false) String operator,
            @RequestParam(value = "requestUri", required = false) String requestUri,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        LambdaQueryWrapper<SysAuditLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operator)) {
            queryWrapper.like(SysAuditLog::getOperator, operator.trim());
        }
        if (StringUtils.hasText(requestUri)) {
            queryWrapper.like(SysAuditLog::getRequestUri, requestUri.trim());
        }
        if (StringUtils.hasText(success)) {
            queryWrapper.eq(SysAuditLog::getSuccessFlag, success.trim());
        }
        if (startTime != null) {
            queryWrapper.ge(SysAuditLog::getOperationTime, toDate(startTime));
        }
        if (endTime != null) {
            queryWrapper.le(SysAuditLog::getOperationTime, toDate(endTime));
        }
        queryWrapper.orderByDesc(SysAuditLog::getOperationTime);
        return R.success(auditLogService.list(queryWrapper));
    }

    /**
     * 查询审计日志详情。
     *
     * @param logId 日志ID
     * @return 审计日志详情
     */
    @GetMapping("/{logId}")
    @PreAuthorize("@ss.hasPermi('system:audit:query')")
    public R<SysAuditLog> getInfo(@PathVariable("logId") Long logId) {
        return R.success(auditLogService.getById(logId));
    }

    /**
     * 删除审计日志。
     *
     * @param logIds 日志ID集合
     * @return 删除结果
     */
    @DeleteMapping("/{logIds}")
    @PreAuthorize("@ss.hasPermi('system:audit:remove')")
    public R<Boolean> remove(@PathVariable("logIds") List<Long> logIds) {
        return R.success(auditLogService.removeByIds(logIds));
    }

    /**
     * 将 LocalDateTime 转换为 Date。
     *
     * @param localDateTime 本地时间
     * @return Date 时间
     */
    private Date toDate(LocalDateTime localDateTime) {
        return java.sql.Timestamp.valueOf(localDateTime);
    }
}
