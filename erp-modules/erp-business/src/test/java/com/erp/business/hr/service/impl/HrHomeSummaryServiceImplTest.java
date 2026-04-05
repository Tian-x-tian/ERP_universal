package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.domain.HrWarningRecord;
import com.erp.business.hr.domain.vo.HrWarningHomeSummaryVO;
import com.erp.business.hr.mapper.HrWarningRecordMapper;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.PermissionService;
import com.erp.business.security.service.SecurityUserResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * HR 首页汇总服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrHomeSummaryServiceImplTest {

    @Mock
    private HrWarningRecordMapper warningRecordMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    /**
     * 验证具备权限时会正确统计异常与紧急预警。
     */
    @Test
    void shouldBuildWarningSummaryWhenPermissionGranted() {
        when(permissionService.hasPermi("business:hr:warning:list")).thenReturn(true);
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");
        when(warningRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(
                buildWarning(11L, daysOffset(1), HrEmployeeSupport.WARNING_STATUS_NEW),
                buildWarning(12L, daysOffset(2), HrEmployeeSupport.WARNING_STATUS_NEW),
                buildWarning(12L, daysOffset(5), HrEmployeeSupport.WARNING_STATUS_NEW),
                buildWarning(13L, daysOffset(1), HrEmployeeSupport.WARNING_STATUS_HANDLED)));

        HrHomeSummaryServiceImpl service = new HrHomeSummaryServiceImpl(
                warningRecordMapper,
                permissionService,
                securityUserResolver);

        HrWarningHomeSummaryVO summary = service.buildWarningSummary();

        Assertions.assertEquals(2L, summary.getAbnormalEmployeeCount());
        Assertions.assertEquals(2L, summary.getUrgentWarningCount());
    }

    /**
     * 验证无权限时返回空安全值。
     */
    @Test
    void shouldReturnSafeSummaryWhenNoPermission() {
        when(permissionService.hasPermi("business:hr:warning:list")).thenReturn(false);

        HrHomeSummaryServiceImpl service = new HrHomeSummaryServiceImpl(
                warningRecordMapper,
                permissionService,
                securityUserResolver);

        HrWarningHomeSummaryVO summary = service.buildWarningSummary();

        Assertions.assertEquals(0L, summary.getAbnormalEmployeeCount());
        Assertions.assertEquals(0L, summary.getUrgentWarningCount());
    }

    /**
     * 验证已过期预警不会计入紧急预警。
     */
    @Test
    void shouldIgnoreExpiredWarningForUrgentCount() {
        when(permissionService.hasPermi("business:hr:warning:list")).thenReturn(true);
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");
        when(warningRecordMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(
                buildWarning(21L, daysOffset(-1), HrEmployeeSupport.WARNING_STATUS_NEW),
                buildWarning(22L, daysOffset(1), HrEmployeeSupport.WARNING_STATUS_NEW)));

        HrHomeSummaryServiceImpl service = new HrHomeSummaryServiceImpl(
                warningRecordMapper,
                permissionService,
                securityUserResolver);

        HrWarningHomeSummaryVO summary = service.buildWarningSummary();

        Assertions.assertEquals(2L, summary.getAbnormalEmployeeCount());
        Assertions.assertEquals(1L, summary.getUrgentWarningCount());
    }

    /**
     * 构造预警对象。
     *
     * @param employeeId 员工ID
     * @param expireDate 到期时间
     * @param status 预警状态
     * @return 预警对象
     */
    private HrWarningRecord buildWarning(Long employeeId, Date expireDate, String status) {
        HrWarningRecord warningRecord = new HrWarningRecord();
        warningRecord.setEmployeeId(employeeId);
        warningRecord.setExpireDate(expireDate);
        warningRecord.setStatus(status);
        return warningRecord;
    }

    /**
     * 构造当前日期偏移天数后的时间。
     *
     * @param days 天数偏移
     * @return 时间
     */
    private Date daysOffset(int days) {
        return new Date(System.currentTimeMillis() + days * 24L * 60L * 60L * 1000L);
    }
}
