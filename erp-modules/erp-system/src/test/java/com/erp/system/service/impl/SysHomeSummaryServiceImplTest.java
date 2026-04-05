package com.erp.system.service.impl;

import com.erp.system.domain.vo.SystemHomeHealthSummaryVO;
import com.erp.system.security.service.PermissionService;
import com.erp.system.service.ISysLoginLogService;
import com.erp.system.service.ISysOperLogService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 系统首页汇总服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysHomeSummaryServiceImplTest {

    @Mock
    private ISysLoginLogService loginLogService;

    @Mock
    private ISysOperLogService operLogService;

    @Mock
    private PermissionService permissionService;

    /**
     * 验证具备权限时会正确计算 24 小时健康指数。
     */
    @Test
    void shouldBuildHealthSummaryWhenPermissionGranted() {
        when(permissionService.hasPermi("system:loginLog:list")).thenReturn(true);
        when(permissionService.hasPermi("system:oper:list")).thenReturn(true);
        when(loginLogService.count(any())).thenReturn(100L, 80L);
        when(operLogService.count(any())).thenReturn(50L, 45L);

        SysHomeSummaryServiceImpl service = new SysHomeSummaryServiceImpl(
                loginLogService,
                operLogService,
                permissionService);

        SystemHomeHealthSummaryVO summary = service.buildHealthSummary();

        Assertions.assertEquals(150L, summary.getTotalEventCount24h());
        Assertions.assertEquals(25L, summary.getFailedEventCount24h());
        Assertions.assertEquals(80.00D, summary.getLoginSuccessRate24h());
        Assertions.assertEquals(90.00D, summary.getOperSuccessRate24h());
        Assertions.assertEquals(83.33D, summary.getSuccessRate24h());
    }

    /**
     * 验证无权限时返回空安全值。
     */
    @Test
    void shouldReturnSafeSummaryWhenNoPermission() {
        when(permissionService.hasPermi("system:loginLog:list")).thenReturn(false);
        when(permissionService.hasPermi("system:oper:list")).thenReturn(false);

        SysHomeSummaryServiceImpl service = new SysHomeSummaryServiceImpl(
                loginLogService,
                operLogService,
                permissionService);

        SystemHomeHealthSummaryVO summary = service.buildHealthSummary();

        Assertions.assertEquals(0L, summary.getTotalEventCount24h());
        Assertions.assertEquals(0L, summary.getFailedEventCount24h());
        Assertions.assertEquals(0D, summary.getLoginSuccessRate24h());
        Assertions.assertEquals(0D, summary.getOperSuccessRate24h());
        Assertions.assertEquals(0D, summary.getSuccessRate24h());
    }

    /**
     * 验证仅具备登录日志权限时仅统计登录维度数据。
     */
    @Test
    void shouldBuildSummaryWhenOnlyLoginPermissionGranted() {
        when(permissionService.hasPermi("system:loginLog:list")).thenReturn(true);
        when(permissionService.hasPermi("system:oper:list")).thenReturn(false);
        when(loginLogService.count(any())).thenReturn(20L, 10L);

        SysHomeSummaryServiceImpl service = new SysHomeSummaryServiceImpl(
                loginLogService,
                operLogService,
                permissionService);

        SystemHomeHealthSummaryVO summary = service.buildHealthSummary();

        Assertions.assertEquals(20L, summary.getTotalEventCount24h());
        Assertions.assertEquals(10L, summary.getFailedEventCount24h());
        Assertions.assertEquals(50.00D, summary.getLoginSuccessRate24h());
        Assertions.assertEquals(0D, summary.getOperSuccessRate24h());
        Assertions.assertEquals(50.00D, summary.getSuccessRate24h());
    }
}
