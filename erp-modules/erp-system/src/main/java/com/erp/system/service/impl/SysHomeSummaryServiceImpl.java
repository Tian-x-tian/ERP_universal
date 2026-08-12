package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.system.domain.SysLoginLog;
import com.erp.system.domain.SysOperLog;
import com.erp.system.domain.vo.SystemHomeHealthSummaryVO;
import com.erp.system.security.service.PermissionService;
import com.erp.system.service.ISysHomeSummaryService;
import com.erp.system.service.ISysLoginLogService;
import com.erp.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

/**
 * 系统首页汇总服务实现。
 */
@Service
public class SysHomeSummaryServiceImpl implements ISysHomeSummaryService {
    private static final String LOGIN_STATUS_SUCCESS = "0";
    private static final String OPER_STATUS_SUCCESS = "1";

    private final ISysLoginLogService loginLogService;
    private final ISysOperLogService operLogService;
    private final PermissionService permissionService;

    public SysHomeSummaryServiceImpl(ISysLoginLogService loginLogService,
            ISysOperLogService operLogService,
            PermissionService permissionService) {
        this.loginLogService = loginLogService;
        this.operLogService = operLogService;
        this.permissionService = permissionService;
    }

    /**
     * 构建系统首页健康汇总数据。
     *
     * @return 健康汇总
     */
    @Override
    public SystemHomeHealthSummaryVO buildHealthSummary() {
        boolean canReadLoginLog = permissionService.hasPermi("system:loginLog:list");
        boolean canReadOperLog = permissionService.hasPermi("system:oper:list");
        if (!canReadLoginLog && !canReadOperLog) {
            return emptySummary();
        }
        Date timeRangeStart = new Date(System.currentTimeMillis() - 24L * 60L * 60L * 1000L);
        long loginTotalCount = 0L;
        long loginSuccessCount = 0L;
        if (canReadLoginLog) {
            loginTotalCount = loginLogService.count(new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, timeRangeStart));
            loginSuccessCount = loginLogService.count(new LambdaQueryWrapper<SysLoginLog>()
                    .ge(SysLoginLog::getLoginTime, timeRangeStart)
                    .eq(SysLoginLog::getStatus, LOGIN_STATUS_SUCCESS));
        }
        long operTotalCount = 0L;
        long operSuccessCount = 0L;
        if (canReadOperLog) {
            operTotalCount = operLogService.count(new LambdaQueryWrapper<SysOperLog>()
                    .ge(SysOperLog::getOperationTime, timeRangeStart));
            operSuccessCount = operLogService.count(new LambdaQueryWrapper<SysOperLog>()
                    .ge(SysOperLog::getOperationTime, timeRangeStart)
                    .eq(SysOperLog::getSuccessFlag, OPER_STATUS_SUCCESS));
        }
        long totalEventCount = loginTotalCount + operTotalCount;
        long successEventCount = loginSuccessCount + operSuccessCount;
        SystemHomeHealthSummaryVO summaryVO = new SystemHomeHealthSummaryVO();
        summaryVO.setTotalEventCount24h(totalEventCount);
        summaryVO.setFailedEventCount24h(Math.max(totalEventCount - successEventCount, 0L));
        summaryVO.setLoginSuccessRate24h(calculateRate(loginSuccessCount, loginTotalCount));
        summaryVO.setOperSuccessRate24h(calculateRate(operSuccessCount, operTotalCount));
        summaryVO.setSuccessRate24h(calculateRate(successEventCount, totalEventCount));
        return summaryVO;
    }

    /**
     * 构建空安全汇总对象。
     *
     * @return 汇总对象
     */
    private SystemHomeHealthSummaryVO emptySummary() {
        SystemHomeHealthSummaryVO summaryVO = new SystemHomeHealthSummaryVO();
        summaryVO.setSuccessRate24h(0D);
        summaryVO.setTotalEventCount24h(0L);
        summaryVO.setFailedEventCount24h(0L);
        summaryVO.setLoginSuccessRate24h(0D);
        summaryVO.setOperSuccessRate24h(0D);
        return summaryVO;
    }

    /**
     * 计算百分比并统一保留两位小数。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比
     */
    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
