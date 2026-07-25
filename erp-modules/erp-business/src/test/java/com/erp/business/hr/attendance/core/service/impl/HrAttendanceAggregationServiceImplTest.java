package com.erp.business.hr.attendance.core.service.impl;

import com.erp.business.hr.attendance.core.domain.HrAttendanceDaySummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceException;
import com.erp.business.hr.attendance.core.domain.HrAttendanceMonthSummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceRecord;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceDaySummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceExceptionMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceMonthSummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceOvertimeOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLeaveOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceRecordMapper;
import com.erp.business.hr.attendance.core.support.HrAttendanceSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 出勤聚合服务单元测试。
 */
class HrAttendanceAggregationServiceImplTest {

    /**
     * 验证日汇总会优先采用第三方权威记录，并同步生成异常数据。
     */
    @Test
    void shouldPreferIntegrationRecordWhenRecalculateDaySummary() {
        HrAttendanceRecordMapper recordMapper = mock(HrAttendanceRecordMapper.class);
        HrAttendanceDaySummaryMapper daySummaryMapper = mock(HrAttendanceDaySummaryMapper.class);
        HrAttendanceMonthSummaryMapper monthSummaryMapper = mock(HrAttendanceMonthSummaryMapper.class);
        HrAttendanceExceptionMapper exceptionMapper = mock(HrAttendanceExceptionMapper.class);
        HrAttendanceLeaveOrderMapper leaveOrderMapper = mock(HrAttendanceLeaveOrderMapper.class);
        HrAttendanceOvertimeOrderMapper overtimeOrderMapper = mock(HrAttendanceOvertimeOrderMapper.class);
        HrAttendanceAggregationServiceImpl aggregationService = new HrAttendanceAggregationServiceImpl(
                recordMapper, daySummaryMapper, monthSummaryMapper, exceptionMapper, leaveOrderMapper, overtimeOrderMapper);

        LocalDate workDate = LocalDate.of(2026, 4, 10);
        HrAttendanceRecord internalRecord = buildRecord(11L, HrAttendanceSupport.SOURCE_INTERNAL,
                LocalDateTime.of(2026, 4, 10, 9, 5),
                LocalDateTime.of(2026, 4, 10, 18, 0), "Y", "Y", "N");
        HrAttendanceRecord integrationRecord = buildRecord(12L, HrAttendanceSupport.SOURCE_INTEGRATION,
                LocalDateTime.of(2026, 4, 10, 9, 30),
                LocalDateTime.of(2026, 4, 10, 18, 0), "N", "Y", "Y");
        when(recordMapper.selectList(any())).thenReturn(List.of(internalRecord, integrationRecord));
        when(leaveOrderMapper.selectList(any())).thenReturn(List.of());
        when(overtimeOrderMapper.selectList(any())).thenReturn(List.of());
        when(daySummaryMapper.selectOne(any())).thenReturn(null);
        doAnswer(invocation -> {
            HrAttendanceDaySummary summary = invocation.getArgument(0);
            summary.setSummaryId(1001L);
            return 1;
        }).when(daySummaryMapper).insert(any(HrAttendanceDaySummary.class));

        aggregationService.recalculateEmployeeDay(10L, workDate, "tester");

        ArgumentCaptor<HrAttendanceDaySummary> summaryCaptor = ArgumentCaptor.forClass(HrAttendanceDaySummary.class);
        verify(daySummaryMapper).insert(summaryCaptor.capture());
        HrAttendanceDaySummary inserted = summaryCaptor.getValue();
        Assertions.assertEquals(HrAttendanceSupport.SOURCE_INTEGRATION, inserted.getPrimarySourceType());
        Assertions.assertEquals(12L, inserted.getAuthorityRecordId());
        Assertions.assertEquals(510, inserted.getActualMinutes());
        Assertions.assertEquals(1, inserted.getLateCount());
        Assertions.assertEquals(0, inserted.getMissingCardCount());
        ArgumentCaptor<HrAttendanceException> exceptionCaptor = ArgumentCaptor.forClass(HrAttendanceException.class);
        verify(exceptionMapper, org.mockito.Mockito.atLeastOnce()).insert(exceptionCaptor.capture());
        List<String> exceptionTypes = exceptionCaptor.getAllValues().stream()
                .map(HrAttendanceException::getExceptionType)
                .toList();
        Assertions.assertTrue(exceptionTypes.contains(HrAttendanceSupport.EXCEPTION_LATE));
        Assertions.assertTrue(exceptionTypes.contains(HrAttendanceSupport.EXCEPTION_OUT_OF_RANGE));
    }

    /**
     * 验证月汇总会按日汇总累计统计值。
     */
    @Test
    void shouldRollupMonthSummaryFromDaySummaries() {
        HrAttendanceRecordMapper recordMapper = mock(HrAttendanceRecordMapper.class);
        HrAttendanceDaySummaryMapper daySummaryMapper = mock(HrAttendanceDaySummaryMapper.class);
        HrAttendanceMonthSummaryMapper monthSummaryMapper = mock(HrAttendanceMonthSummaryMapper.class);
        HrAttendanceExceptionMapper exceptionMapper = mock(HrAttendanceExceptionMapper.class);
        HrAttendanceLeaveOrderMapper leaveOrderMapper = mock(HrAttendanceLeaveOrderMapper.class);
        HrAttendanceOvertimeOrderMapper overtimeOrderMapper = mock(HrAttendanceOvertimeOrderMapper.class);
        HrAttendanceAggregationServiceImpl aggregationService = new HrAttendanceAggregationServiceImpl(
                recordMapper, daySummaryMapper, monthSummaryMapper, exceptionMapper, leaveOrderMapper, overtimeOrderMapper);

        HrAttendanceDaySummary summary1 = new HrAttendanceDaySummary();
        summary1.setEmployeeId(10L);
        summary1.setMonthCode("2026-04");
        summary1.setAttendanceDays(BigDecimal.ONE);
        summary1.setActualMinutes(480);
        summary1.setLeaveDays(BigDecimal.ZERO);
        summary1.setLeaveMinutes(0);
        summary1.setOvertimeMinutes(60);
        summary1.setLateCount(1);
        summary1.setEarlyLeaveCount(0);
        summary1.setMissingCardCount(0);
        summary1.setAbsenteeismDays(BigDecimal.ZERO);
        summary1.setAbnormalCount(1);
        HrAttendanceDaySummary summary2 = new HrAttendanceDaySummary();
        summary2.setEmployeeId(10L);
        summary2.setMonthCode("2026-04");
        summary2.setAttendanceDays(BigDecimal.ONE);
        summary2.setActualMinutes(450);
        summary2.setLeaveDays(new BigDecimal("0.5"));
        summary2.setLeaveMinutes(240);
        summary2.setOvertimeMinutes(0);
        summary2.setLateCount(0);
        summary2.setEarlyLeaveCount(1);
        summary2.setMissingCardCount(1);
        summary2.setAbsenteeismDays(BigDecimal.ZERO);
        summary2.setAbnormalCount(2);
        when(daySummaryMapper.selectList(any())).thenReturn(List.of(summary1, summary2));
        when(monthSummaryMapper.selectOne(any())).thenReturn(null);

        aggregationService.recalculateEmployeeMonth(10L, "2026-04", "tester");

        ArgumentCaptor<HrAttendanceMonthSummary> captor = ArgumentCaptor.forClass(HrAttendanceMonthSummary.class);
        verify(monthSummaryMapper).insert(captor.capture());
        HrAttendanceMonthSummary monthSummary = captor.getValue();
        Assertions.assertEquals(new BigDecimal("2"), monthSummary.getAttendanceDays());
        Assertions.assertEquals(930, monthSummary.getActualMinutes());
        Assertions.assertEquals(new BigDecimal("0.5"), monthSummary.getLeaveDays());
        Assertions.assertEquals(60, monthSummary.getOvertimeMinutes());
        Assertions.assertEquals(1, monthSummary.getLateCount());
        Assertions.assertEquals(1, monthSummary.getEarlyLeaveCount());
        Assertions.assertEquals(1, monthSummary.getMissingCardCount());
        Assertions.assertEquals(3, monthSummary.getAbnormalCount());
    }

    /**
     * 构造测试用出勤记录。
     *
     * @param recordId 记录ID
     * @param sourceType 数据来源
     * @param signInTime 签到时间
     * @param signOutTime 签退时间
     * @param signInInRange 签到范围标识
     * @param signOutInRange 签退范围标识
     * @param authorityFlag 是否权威
     * @return 出勤记录
     */
    private HrAttendanceRecord buildRecord(Long recordId, String sourceType, LocalDateTime signInTime,
            LocalDateTime signOutTime, String signInInRange, String signOutInRange, String authorityFlag) {
        HrAttendanceRecord record = new HrAttendanceRecord();
        record.setRecordId(recordId);
        record.setTenantId("000000");
        record.setEmployeeId(10L);
        record.setOrgId(1L);
        record.setDeptId(2L);
        record.setWorkDate(java.sql.Date.valueOf(signInTime.toLocalDate()));
        record.setSourceType(sourceType);
        record.setAuthorityFlag(authorityFlag);
        record.setSignInTime(java.sql.Timestamp.valueOf(signInTime));
        record.setSignOutTime(java.sql.Timestamp.valueOf(signOutTime));
        record.setSignInInRange(signInInRange);
        record.setSignOutInRange(signOutInRange);
        return record;
    }
}
