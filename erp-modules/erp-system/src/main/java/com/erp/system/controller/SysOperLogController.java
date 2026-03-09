package com.erp.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.core.domain.R;
import com.erp.system.domain.SysOperLog;
import com.erp.system.service.ISysOperLogService;
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
 * 操作日志控制层
 */
@RestController
@RequestMapping("/system/oper/log")
public class SysOperLogController {

    private final ISysOperLogService operLogService;

    public SysOperLogController(ISysOperLogService operLogService) {
        this.operLogService = operLogService;
    }

    /**
     * 查询操作日志列表。
     *
     * @param operator   操作人账号
     * @param requestUri 请求URI
     * @param success    是否成功（1 成功，0 失败）
     * @param startTime  开始时间
     * @param endTime    结束时间
     * @return 操作日志列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:oper:list')")
    public R<List<SysOperLog>> list(
            @RequestParam(value = "operator", required = false) String operator,
            @RequestParam(value = "requestUri", required = false) String requestUri,
            @RequestParam(value = "success", required = false) String success,
            @RequestParam(value = "startTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(value = "endTime", required = false)
            @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime) {
        LambdaQueryWrapper<SysOperLog> queryWrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(operator)) {
            queryWrapper.like(SysOperLog::getOperator, operator.trim());
        }
        if (StringUtils.hasText(requestUri)) {
            queryWrapper.like(SysOperLog::getRequestUri, requestUri.trim());
        }
        if (StringUtils.hasText(success)) {
            queryWrapper.eq(SysOperLog::getSuccessFlag, success.trim());
        }
        if (startTime != null) {
            queryWrapper.ge(SysOperLog::getOperationTime, toDate(startTime));
        }
        if (endTime != null) {
            queryWrapper.le(SysOperLog::getOperationTime, toDate(endTime));
        }
        queryWrapper.orderByDesc(SysOperLog::getOperationTime);
        return R.success(operLogService.list(queryWrapper));
    }

    /**
     * 查询操作日志详情。
     *
     * @param operId 日志ID
     * @return 日志详情
     */
    @GetMapping("/{operId}")
    @PreAuthorize("@ss.hasPermi('system:oper:query')")
    public R<SysOperLog> getInfo(@PathVariable("operId") Long operId) {
        return R.success(operLogService.getById(operId));
    }

    /**
     * 删除操作日志。
     *
     * @param operIds 日志ID集合
     * @return 删除结果
     */
    @DeleteMapping("/{operIds}")
    @PreAuthorize("@ss.hasPermi('system:oper:remove')")
    public R<Boolean> remove(@PathVariable("operIds") List<Long> operIds) {
        return R.success(operLogService.removeByIds(operIds));
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
