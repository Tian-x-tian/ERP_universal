package com.erp.business.hr.attendance.core.service.impl;

import com.erp.business.hr.attendance.core.domain.HrAttendanceDaySummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLocationRule;
import com.erp.business.hr.attendance.core.domain.HrAttendanceRecord;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceLeaveBody;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalDayVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendancePersonalSignBody;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceDaySummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceExceptionMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLeaveOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLocationRuleMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceMonthSummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceOvertimeOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceRecordMapper;
import com.erp.business.hr.attendance.core.service.AttendanceWorkflowGateway;
import com.erp.business.hr.attendance.core.service.IHrAttendanceAggregationService;
import com.erp.business.hr.attendance.core.support.HrAttendanceSupport;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalPlatformClient;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 出勤核心服务单元测试。
 */
class HrAttendanceServiceImplTest {

    private HrAttendanceRecordMapper recordMapper;
    private HrAttendanceDaySummaryMapper daySummaryMapper;
    private HrAttendanceMonthSummaryMapper monthSummaryMapper;
    private HrAttendanceExceptionMapper exceptionMapper;
    private HrAttendanceLocationRuleMapper locationRuleMapper;
    private HrAttendanceLeaveOrderMapper leaveOrderMapper;
    private HrAttendanceOvertimeOrderMapper overtimeOrderMapper;
    private HrEmployeeCoreMapper employeeCoreMapper;
    private SecurityUserResolver securityUserResolver;
    private InternalPlatformClient internalPlatformClient;
    private IHrAttendanceAggregationService aggregationService;
    private AttendanceWorkflowGateway workflowGateway;
    private HrAttendanceServiceImpl attendanceService;

    /**
     * 初始化测试依赖。
     */
    @BeforeEach
    void setUp() {
        recordMapper = mock(HrAttendanceRecordMapper.class);
        daySummaryMapper = mock(HrAttendanceDaySummaryMapper.class);
        monthSummaryMapper = mock(HrAttendanceMonthSummaryMapper.class);
        exceptionMapper = mock(HrAttendanceExceptionMapper.class);
        locationRuleMapper = mock(HrAttendanceLocationRuleMapper.class);
        leaveOrderMapper = mock(HrAttendanceLeaveOrderMapper.class);
        overtimeOrderMapper = mock(HrAttendanceOvertimeOrderMapper.class);
        employeeCoreMapper = mock(HrEmployeeCoreMapper.class);
        securityUserResolver = mock(SecurityUserResolver.class);
        internalPlatformClient = mock(InternalPlatformClient.class);
        aggregationService = mock(IHrAttendanceAggregationService.class);
        workflowGateway = mock(AttendanceWorkflowGateway.class);
        attendanceService = new HrAttendanceServiceImpl(recordMapper, daySummaryMapper, monthSummaryMapper,
                exceptionMapper, locationRuleMapper, leaveOrderMapper, overtimeOrderMapper, employeeCoreMapper,
                securityUserResolver, internalPlatformClient, aggregationService, workflowGateway);
        when(securityUserResolver.getCurrentTenantId()).thenReturn("000000");
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        when(securityUserResolver.getCurrentUserId()).thenReturn(88L);
    }

    /**
     * 验证个人签到会写入内部原子记录并触发汇总重算。
     */
    @Test
    void shouldCreateInternalSignInRecordAndTriggerRecalculation() {
        when(employeeCoreMapper.selectOne(any())).thenReturn(buildEmployee());
        when(locationRuleMapper.selectList(any())).thenReturn(Collections.singletonList(buildRule()));
        when(recordMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            HrAttendanceRecord record = invocation.getArgument(0);
            record.setRecordId(9001L);
            return 1;
        }).when(recordMapper).insert(any(HrAttendanceRecord.class));
        when(recordMapper.selectList(any())).thenReturn(Collections.singletonList(buildInternalRecord()));
        when(daySummaryMapper.selectOne(any())).thenReturn(buildDaySummary());
        when(exceptionMapper.selectList(any())).thenReturn(List.of());

        HrAttendancePersonalSignBody body = new HrAttendancePersonalSignBody();
        body.setLatitude(new BigDecimal("30.000000"));
        body.setLongitude(new BigDecimal("120.000000"));
        body.setAddress("办公楼一层");
        body.setDeviceSource("MOBILE");
        body.setSignTime(LocalDateTime.of(2026, 4, 10, 8, 55));

        HrAttendancePersonalDayVO result = attendanceService.signIn(body);

        Assertions.assertNotNull(result);
        ArgumentCaptor<HrAttendanceRecord> captor = ArgumentCaptor.forClass(HrAttendanceRecord.class);
        verify(recordMapper).insert(captor.capture());
        Assertions.assertEquals("INTERNAL", captor.getValue().getSourceType());
        Assertions.assertEquals("Y", captor.getValue().getSignInInRange());
        verify(aggregationService).recalculateEmployeeDay(10L, body.getSignTime().toLocalDate(), "tester");
        verify(aggregationService).recalculateEmployeeMonth(10L, "2026-04", "tester");
    }

    /**
     * 验证请假单提交审批时会更新状态并调用流程网关。
     */
    @Test
    void shouldSubmitLeaveWorkflow() {
        HrAttendanceLeaveOrder draftOrder = new HrAttendanceLeaveOrder();
        draftOrder.setOrderId(3001L);
        draftOrder.setTenantId("000000");
        draftOrder.setEmployeeId(10L);
        draftOrder.setOrgId(1L);
        draftOrder.setDeptId(2L);
        draftOrder.setOrderNo("AL20260410001");
        draftOrder.setStatus(HrAttendanceSupport.ORDER_STATUS_DRAFT);
        when(employeeCoreMapper.selectOne(any())).thenReturn(buildEmployee());
        doAnswer(invocation -> {
            HrAttendanceLeaveOrder order = invocation.getArgument(0);
            order.setOrderId(3001L);
            order.setOrderNo("AL20260410001");
            return 1;
        }).when(leaveOrderMapper).insert(any(HrAttendanceLeaveOrder.class));
        when(leaveOrderMapper.selectOne(any())).thenReturn(draftOrder);
        when(leaveOrderMapper.selectById(3001L)).thenReturn(draftOrder);
        when(workflowGateway.startLeaveWorkflow(any(HrAttendanceLeaveOrder.class))).thenReturn(true);

        HrAttendanceLeaveBody body = new HrAttendanceLeaveBody();
        body.setStartTime(LocalDateTime.of(2026, 4, 10, 9, 0));
        body.setEndTime(LocalDateTime.of(2026, 4, 10, 18, 0));
        body.setLeaveType("ANNUAL");
        body.setReason("年假");
        attendanceService.saveLeave(body);
        attendanceService.submitLeave(3001L);

        ArgumentCaptor<HrAttendanceLeaveOrder> updateCaptor = ArgumentCaptor.forClass(HrAttendanceLeaveOrder.class);
        verify(leaveOrderMapper).updateById(updateCaptor.capture());
        Assertions.assertEquals(HrAttendanceSupport.ORDER_STATUS_SUBMITTED, updateCaptor.getValue().getStatus());
        verify(workflowGateway).startLeaveWorkflow(any(HrAttendanceLeaveOrder.class));
    }

    /**
     * 构造员工主档。
     *
     * @return 员工对象
     */
    private HrEmployeeCore buildEmployee() {
        HrEmployeeCore employee = new HrEmployeeCore();
        employee.setEmployeeId(10L);
        employee.setTenantId("000000");
        employee.setUserId(88L);
        employee.setEmpCode("E0001");
        employee.setEmpName("张三");
        employee.setOrgId(1L);
        employee.setDeptId(2L);
        employee.setStatus(HrEmployeeSupport.STATUS_ACTIVE);
        employee.setDelFlag(HrEmployeeSupport.EXIST_DEL_FLAG);
        return employee;
    }

    /**
     * 构造默认定位规则。
     *
     * @return 定位规则
     */
    private HrAttendanceLocationRule buildRule() {
        HrAttendanceLocationRule rule = new HrAttendanceLocationRule();
        rule.setRuleId(1L);
        rule.setTenantId("000000");
        rule.setDeptId(2L);
        rule.setRuleName("总部");
        rule.setCenterLatitude(new BigDecimal("30.000000"));
        rule.setCenterLongitude(new BigDecimal("120.000000"));
        rule.setRadiusMeters(300);
        rule.setEnabledFlag("Y");
        return rule;
    }

    /**
     * 构造内部签到记录。
     *
     * @return 出勤记录
     */
    private HrAttendanceRecord buildInternalRecord() {
        HrAttendanceRecord record = new HrAttendanceRecord();
        record.setRecordId(9001L);
        record.setTenantId("000000");
        record.setEmployeeId(10L);
        record.setOrgId(1L);
        record.setDeptId(2L);
        record.setSourceType(HrAttendanceSupport.SOURCE_INTERNAL);
        record.setAuthorityFlag("N");
        record.setSignInInRange("Y");
        record.setSignInTime(java.sql.Timestamp.valueOf(LocalDateTime.of(2026, 4, 10, 8, 55)));
        return record;
    }

    /**
     * 构造个人日汇总。
     *
     * @return 日汇总
     */
    private HrAttendanceDaySummary buildDaySummary() {
        HrAttendanceDaySummary summary = new HrAttendanceDaySummary();
        summary.setSummaryId(1L);
        summary.setTenantId("000000");
        summary.setEmployeeId(10L);
        summary.setAttendanceDays(BigDecimal.ONE);
        summary.setMonthCode("2026-04");
        summary.setActualMinutes(0);
        return summary;
    }
}
