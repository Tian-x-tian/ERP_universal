package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
     * @return 登录日志列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:loginLog:list')")
    public R<List<SysLoginLog>> list(
            @RequestParam(value = "userName", required = false) String userName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
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
        return R.success(loginLogService.list(queryWrapper));
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
}
