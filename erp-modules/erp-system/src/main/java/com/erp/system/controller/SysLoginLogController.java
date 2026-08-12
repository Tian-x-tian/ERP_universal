package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysLoginLog;
import com.erp.system.service.ISysLoginLogService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * 登录日志控制层
 */
@RestController
@RequestMapping("/system/login/log")
public class SysLoginLogController {

    private final ISysLoginLogService loginLogService;

    public SysLoginLogController(ISysLoginLogService loginLogService) {
        this.loginLogService = loginLogService;
    }

    /**
     * 查询登录日志列表。
     *
     * @param userName  登录账号
     * @param status    登录状态（0成功 1失败）
     * @param startTime 开始时间
     * @param endTime   结束时间
     * @param pageNum   当前页码
     * @param pageSize  每页条数
     * @return 登录日志列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:loginLog:list')")
    public R<PageData<SysLoginLog>> list(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        LambdaQueryWrapper<SysLoginLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userName)) {
            queryWrapper.like(SysLoginLog::getUserName, userName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(SysLoginLog::getStatus, status.trim());
        }
        if (startTime != null) {
            queryWrapper.ge(SysLoginLog::getLoginTime, toDate(startTime));
        }
        if (endTime != null) {
            queryWrapper.le(SysLoginLog::getLoginTime, toDate(endTime));
        }
        queryWrapper.orderByDesc(SysLoginLog::getLoginTime);
        Page<SysLoginLog> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        Page<SysLoginLog> resultPage = loginLogService.page(page, queryWrapper);
        return R.page(resultPage.getRecords(), resultPage.getCurrent(), resultPage.getSize(), resultPage.getTotal());
    }

    /**
     * 删除登录日志。
     *
     * @param infoIds 日志主键集合
     * @return 删除结果
     */
    @DeleteMapping("/{infoIds}")
    @PreAuthorize("@ss.hasPermi('system:loginLog:remove')")
    public R<Boolean> remove(@PathVariable("infoIds") List<Long> infoIds) {
        return R.success(loginLogService.removeByIds(infoIds));
    }

    /**
     * 将 LocalDateTime 转为 Date。
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
