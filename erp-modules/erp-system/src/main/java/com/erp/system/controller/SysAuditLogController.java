package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
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
     * @param keyword    操作人或请求地址关键字
     * @param pageNum    当前页码
     * @param pageSize   每页条数
     * @return 审计日志列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:audit:list')")
    public R<PageData<SysAuditLog>> list(
            @RequestParam(value = "operator", required = false) String operator,
            @RequestParam(value = "requestUri", required = false) String requestUri,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
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
        if (StringUtils.hasText(keyword)) {
            String normalizedKeyword = keyword.trim();
            queryWrapper.and(wrapper -> wrapper.like(SysAuditLog::getOperator, normalizedKeyword)
                    .or()
                    .like(SysAuditLog::getRequestUri, normalizedKeyword));
        }
        queryWrapper.orderByDesc(SysAuditLog::getOperationTime);
        Page<SysAuditLog> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<SysAuditLog> resultPage = auditLogService.page(page, queryWrapper);
        return R.page(resultPage.getRecords(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
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

    /**
     * 规范化页码，避免非法页码导致分页异常。
     *
     * @param pageNum 原始页码
     * @return 规范化后的页码
     */
    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化分页大小，统一限制最大页长。
     *
     * @param pageSize 原始分页大小
     * @return 规范化后的分页大小
     */
    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }
}
