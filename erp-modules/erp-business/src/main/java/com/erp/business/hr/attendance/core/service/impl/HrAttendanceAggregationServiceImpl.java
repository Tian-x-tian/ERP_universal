package com.erp.business.hr.attendance.core.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.attendance.core.domain.HrAttendanceDaySummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceException;
import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceMonthSummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceRecord;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceDaySummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceExceptionMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceLeaveOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceMonthSummaryMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceOvertimeOrderMapper;
import com.erp.business.hr.attendance.core.mapper.HrAttendanceRecordMapper;
import com.erp.business.hr.attendance.core.service.IHrAttendanceAggregationService;
import com.erp.business.hr.attendance.core.support.HrAttendanceSupport;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

/**
 * 出勤汇总聚合服务实现。
 */
@Service
public class HrAttendanceAggregationServiceImpl implements IHrAttendanceAggregationService {

    private final HrAttendanceRecordMapper recordMapper;
    private final HrAttendanceDaySummaryMapper daySummaryMapper;
    private final HrAttendanceMonthSummaryMapper monthSummaryMapper;
    private final HrAttendanceExceptionMapper exceptionMapper;
    private final HrAttendanceLeaveOrderMapper leaveOrderMapper;
    private final HrAttendanceOvertimeOrderMapper overtimeOrderMapper;

    public HrAttendanceAggregationServiceImpl(HrAttendanceRecordMapper recordMapper,
            HrAttendanceDaySummaryMapper daySummaryMapper,
            HrAttendanceMonthSummaryMapper monthSummaryMapper,
            HrAttendanceExceptionMapper exceptionMapper,
            HrAttendanceLeaveOrderMapper leaveOrderMapper,
            HrAttendanceOvertimeOrderMapper overtimeOrderMapper) {
        this.recordMapper = recordMapper;
        this.daySummaryMapper = daySummaryMapper;
        this.monthSummaryMapper = monthSummaryMapper;
        this.exceptionMapper = exceptionMapper;
        this.leaveOrderMapper = leaveOrderMapper;
        this.overtimeOrderMapper = overtimeOrderMapper;
    }

    /**
     * 重算指定员工的日汇总。
     *
     * @param employeeId 员工ID
     * @param workDate 工作日期
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateEmployeeDay(Long employeeId, LocalDate workDate, String operator) {
        if (employeeId == null || workDate == null) {
            return;
        }
        List<HrAttendanceRecord> recordList = recordMapper.selectList(new LambdaQueryWrapper<HrAttendanceRecord>()
                .eq(HrAttendanceRecord::getEmployeeId, employeeId)
                .eq(HrAttendanceRecord::getWorkDate, HrAttendanceSupport.toDate(workDate)));
        List<HrAttendanceLeaveOrder> leaveOrders = leaveOrderMapper.selectList(new LambdaQueryWrapper<HrAttendanceLeaveOrder>()
                .eq(HrAttendanceLeaveOrder::getEmployeeId, employeeId)
                .eq(HrAttendanceLeaveOrder::getStatus, HrAttendanceSupport.ORDER_STATUS_APPROVED));
        List<HrAttendanceOvertimeOrder> overtimeOrders = overtimeOrderMapper.selectList(new LambdaQueryWrapper<HrAttendanceOvertimeOrder>()
                .eq(HrAttendanceOvertimeOrder::getEmployeeId, employeeId)
                .eq(HrAttendanceOvertimeOrder::getStatus, HrAttendanceSupport.ORDER_STATUS_APPROVED));
        HrAttendanceRecord primaryRecord = choosePrimaryRecord(recordList);
        int leaveMinutes = sumLeaveMinutes(leaveOrders, workDate);
        int overtimeMinutes = sumOvertimeMinutes(overtimeOrders, workDate);
        int actualMinutes = primaryRecord == null ? 0
                : HrAttendanceSupport.calculateWorkMinutes(HrAttendanceSupport.toLocalDateTime(primaryRecord.getSignInTime()),
                        HrAttendanceSupport.toLocalDateTime(primaryRecord.getSignOutTime()));
        int lateCount = isLate(primaryRecord) ? 1 : 0;
        int earlyLeaveCount = isEarlyLeave(primaryRecord) ? 1 : 0;
        int missingCardCount = isMissingCard(primaryRecord) ? 1 : 0;
        BigDecimal attendanceDays = primaryRecord == null ? BigDecimal.ZERO : BigDecimal.ONE;
        BigDecimal absenteeismDays = (primaryRecord == null && leaveMinutes <= 0) ? BigDecimal.ONE : BigDecimal.ZERO;
        List<HrAttendanceException> exceptions = rebuildExceptions(primaryRecord, employeeId, workDate,
                lateCount, earlyLeaveCount, missingCardCount, absenteeismDays, operator);
        HrAttendanceDaySummary existing = daySummaryMapper.selectOne(new LambdaQueryWrapper<HrAttendanceDaySummary>()
                .eq(HrAttendanceDaySummary::getEmployeeId, employeeId)
                .eq(HrAttendanceDaySummary::getWorkDate, HrAttendanceSupport.toDate(workDate)));
        HrAttendanceDaySummary summary = existing == null ? new HrAttendanceDaySummary() : existing;
        summary.setEmployeeId(employeeId);
        summary.setTenantId(primaryRecord == null ? summary.getTenantId() : primaryRecord.getTenantId());
        summary.setOrgId(resolveOrgId(primaryRecord, leaveOrders, overtimeOrders));
        summary.setDeptId(resolveDeptId(primaryRecord, leaveOrders, overtimeOrders));
        summary.setWorkDate(HrAttendanceSupport.toDate(workDate));
        summary.setMonthCode(HrAttendanceSupport.monthCode(workDate));
        summary.setAuthorityRecordId(primaryRecord == null ? null : primaryRecord.getRecordId());
        summary.setPrimarySourceType(primaryRecord == null ? null : primaryRecord.getSourceType());
        summary.setSignInTime(primaryRecord == null ? null : primaryRecord.getSignInTime());
        summary.setSignOutTime(primaryRecord == null ? null : primaryRecord.getSignOutTime());
        summary.setActualMinutes(actualMinutes);
        summary.setAttendanceDays(attendanceDays);
        summary.setLeaveMinutes(leaveMinutes);
        summary.setLeaveDays(HrAttendanceSupport.minutesToDays(leaveMinutes));
        summary.setOvertimeMinutes(overtimeMinutes);
        summary.setLateCount(lateCount);
        summary.setEarlyLeaveCount(earlyLeaveCount);
        summary.setMissingCardCount(missingCardCount);
        summary.setAbsenteeismDays(absenteeismDays);
        summary.setAbnormalCount(exceptions.size());
        summary.setUpdateBy(resolveOperator(operator));
        if (existing == null) {
            summary.setCreateBy(resolveOperator(operator));
            daySummaryMapper.insert(summary);
        } else {
            daySummaryMapper.updateById(summary);
        }
    }

    /**
     * 重算指定员工的月汇总。
     *
     * @param employeeId 员工ID
     * @param monthCode 月份编码
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recalculateEmployeeMonth(Long employeeId, String monthCode, String operator) {
        if (employeeId == null) {
            return;
        }
        String normalizedMonthCode = HrAttendanceSupport.normalizeMonthCode(monthCode);
        if (normalizedMonthCode == null) {
            return;
        }
        List<HrAttendanceDaySummary> daySummaryList = daySummaryMapper.selectList(new LambdaQueryWrapper<HrAttendanceDaySummary>()
                .eq(HrAttendanceDaySummary::getEmployeeId, employeeId)
                .eq(HrAttendanceDaySummary::getMonthCode, normalizedMonthCode));
        HrAttendanceMonthSummary existing = monthSummaryMapper.selectOne(new LambdaQueryWrapper<HrAttendanceMonthSummary>()
                .eq(HrAttendanceMonthSummary::getEmployeeId, employeeId)
                .eq(HrAttendanceMonthSummary::getMonthCode, normalizedMonthCode));
        if (daySummaryList == null || daySummaryList.isEmpty()) {
            if (existing != null && existing.getSummaryId() != null) {
                monthSummaryMapper.deleteById(existing.getSummaryId());
            }
            return;
        }
        HrAttendanceMonthSummary monthSummary = existing == null ? new HrAttendanceMonthSummary() : existing;
        HrAttendanceDaySummary first = daySummaryList.get(0);
        monthSummary.setEmployeeId(employeeId);
        monthSummary.setTenantId(first.getTenantId());
        monthSummary.setOrgId(first.getOrgId());
        monthSummary.setDeptId(first.getDeptId());
        monthSummary.setMonthCode(normalizedMonthCode);
        monthSummary.setAttendanceDays(daySummaryList.stream()
                .map(HrAttendanceDaySummary::getAttendanceDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        monthSummary.setActualMinutes(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getActualMinutes())).sum());
        monthSummary.setLeaveMinutes(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getLeaveMinutes())).sum());
        monthSummary.setLeaveDays(daySummaryList.stream()
                .map(HrAttendanceDaySummary::getLeaveDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        monthSummary.setOvertimeMinutes(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getOvertimeMinutes())).sum());
        monthSummary.setLateCount(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getLateCount())).sum());
        monthSummary.setEarlyLeaveCount(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getEarlyLeaveCount())).sum());
        monthSummary.setMissingCardCount(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getMissingCardCount())).sum());
        monthSummary.setAbsenteeismDays(daySummaryList.stream()
                .map(HrAttendanceDaySummary::getAbsenteeismDays)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        monthSummary.setAbnormalCount(daySummaryList.stream().mapToInt(item -> nullSafeInt(item.getAbnormalCount())).sum());
        monthSummary.setUpdateBy(resolveOperator(operator));
        if (existing == null) {
            monthSummary.setCreateBy(resolveOperator(operator));
            monthSummaryMapper.insert(monthSummary);
        } else {
            monthSummaryMapper.updateById(monthSummary);
        }
    }

    /**
     * 选择当日优先记录。
     *
     * @param recordList 原子记录列表
     * @return 优先记录
     */
    private HrAttendanceRecord choosePrimaryRecord(List<HrAttendanceRecord> recordList) {
        if (recordList == null || recordList.isEmpty()) {
            return null;
        }
        return recordList.stream()
                .sorted(Comparator
                        .comparing((HrAttendanceRecord item) -> HrAttendanceSupport.FLAG_YES.equals(item.getAuthorityFlag()))
                        .thenComparing(item -> HrAttendanceSupport.SOURCE_INTEGRATION.equals(item.getSourceType()))
                        .thenComparing(HrAttendanceRecord::getUpdateTime,
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .reversed())
                .findFirst()
                .orElse(null);
    }

    /**
     * 汇总单日请假分钟数。
     *
     * @param orderList 请假单列表
     * @param workDate 工作日期
     * @return 请假分钟数
     */
    private int sumLeaveMinutes(List<HrAttendanceLeaveOrder> orderList, LocalDate workDate) {
        if (orderList == null || orderList.isEmpty()) {
            return 0;
        }
        return orderList.stream()
                .mapToInt(order -> HrAttendanceSupport.overlapMinutes(order.getStartTime(), order.getEndTime(), workDate))
                .sum();
    }

    /**
     * 汇总单日加班分钟数。
     *
     * @param orderList 加班单列表
     * @param workDate 工作日期
     * @return 加班分钟数
     */
    private int sumOvertimeMinutes(List<HrAttendanceOvertimeOrder> orderList, LocalDate workDate) {
        if (orderList == null || orderList.isEmpty()) {
            return 0;
        }
        return orderList.stream()
                .mapToInt(order -> HrAttendanceSupport.overlapMinutes(order.getStartTime(), order.getEndTime(), workDate))
                .sum();
    }

    /**
     * 判断是否迟到。
     *
     * @param record 主记录
     * @return true 表示迟到
     */
    private boolean isLate(HrAttendanceRecord record) {
        LocalDateTime signInTime = record == null ? null : HrAttendanceSupport.toLocalDateTime(record.getSignInTime());
        return signInTime != null && signInTime.toLocalTime().isAfter(HrAttendanceSupport.DEFAULT_SIGN_IN_LIMIT);
    }

    /**
     * 判断是否早退。
     *
     * @param record 主记录
     * @return true 表示早退
     */
    private boolean isEarlyLeave(HrAttendanceRecord record) {
        LocalDateTime signOutTime = record == null ? null : HrAttendanceSupport.toLocalDateTime(record.getSignOutTime());
        return signOutTime != null && signOutTime.toLocalTime().isBefore(HrAttendanceSupport.DEFAULT_SIGN_OUT_LIMIT);
    }

    /**
     * 判断是否缺卡。
     *
     * @param record 主记录
     * @return true 表示缺卡
     */
    private boolean isMissingCard(HrAttendanceRecord record) {
        if (record == null) {
            return false;
        }
        return (record.getSignInTime() == null && record.getSignOutTime() != null)
                || (record.getSignInTime() != null && record.getSignOutTime() == null);
    }

    /**
     * 重新构建异常记录。
     *
     * @param primaryRecord 主记录
     * @param employeeId 员工ID
     * @param workDate 工作日期
     * @param lateCount 迟到次数
     * @param earlyLeaveCount 早退次数
     * @param missingCardCount 缺卡次数
     * @param absenteeismDays 旷工天数
     * @param operator 操作人
     * @return 异常列表
     */
    private List<HrAttendanceException> rebuildExceptions(HrAttendanceRecord primaryRecord, Long employeeId,
            LocalDate workDate, int lateCount, int earlyLeaveCount, int missingCardCount,
            BigDecimal absenteeismDays, String operator) {
        exceptionMapper.delete(new LambdaQueryWrapper<HrAttendanceException>()
                .eq(HrAttendanceException::getEmployeeId, employeeId)
                .eq(HrAttendanceException::getWorkDate, HrAttendanceSupport.toDate(workDate)));
        List<HrAttendanceException> exceptionList = new ArrayList<>();
        if (lateCount > 0) {
            exceptionList.add(buildException(primaryRecord, employeeId, workDate, HrAttendanceSupport.EXCEPTION_LATE,
                    "签到时间晚于 09:00", operator));
        }
        if (earlyLeaveCount > 0) {
            exceptionList.add(buildException(primaryRecord, employeeId, workDate, HrAttendanceSupport.EXCEPTION_EARLY_LEAVE,
                    "签退时间早于 18:00", operator));
        }
        if (missingCardCount > 0) {
            exceptionList.add(buildException(primaryRecord, employeeId, workDate, HrAttendanceSupport.EXCEPTION_MISSING_CARD,
                    "签到或签退缺失", operator));
        }
        if (absenteeismDays.compareTo(BigDecimal.ZERO) > 0) {
            exceptionList.add(buildException(primaryRecord, employeeId, workDate, HrAttendanceSupport.EXCEPTION_ABSENTEEISM,
                    "当日未产生有效出勤记录", operator));
        }
        if (primaryRecord != null
                && (HrAttendanceSupport.FLAG_NO.equals(primaryRecord.getSignInInRange())
                || HrAttendanceSupport.FLAG_NO.equals(primaryRecord.getSignOutInRange()))) {
            exceptionList.add(buildException(primaryRecord, employeeId, workDate, HrAttendanceSupport.EXCEPTION_OUT_OF_RANGE,
                    "存在超出定位范围的签到或签退", operator));
        }
        for (HrAttendanceException item : exceptionList) {
            exceptionMapper.insert(item);
        }
        return exceptionList;
    }

    /**
     * 构造异常记录。
     *
     * @param primaryRecord 主记录
     * @param employeeId 员工ID
     * @param workDate 工作日期
     * @param exceptionType 异常类型
     * @param message 异常说明
     * @param operator 操作人
     * @return 异常记录
     */
    private HrAttendanceException buildException(HrAttendanceRecord primaryRecord, Long employeeId,
            LocalDate workDate, String exceptionType, String message, String operator) {
        HrAttendanceException exception = new HrAttendanceException();
        exception.setTenantId(primaryRecord == null ? null : primaryRecord.getTenantId());
        exception.setEmployeeId(employeeId);
        exception.setOrgId(primaryRecord == null ? null : primaryRecord.getOrgId());
        exception.setDeptId(primaryRecord == null ? null : primaryRecord.getDeptId());
        exception.setWorkDate(HrAttendanceSupport.toDate(workDate));
        exception.setRecordId(primaryRecord == null ? null : primaryRecord.getRecordId());
        exception.setExceptionType(exceptionType);
        exception.setExceptionMessage(message);
        exception.setSourceType(primaryRecord == null ? null : primaryRecord.getSourceType());
        exception.setCreateBy(resolveOperator(operator));
        exception.setUpdateBy(resolveOperator(operator));
        return exception;
    }

    /**
     * 解析组织ID。
     *
     * @param primaryRecord 主记录
     * @param leaveOrders 请假单列表
     * @param overtimeOrders 加班单列表
     * @return 组织ID
     */
    private Long resolveOrgId(HrAttendanceRecord primaryRecord, List<HrAttendanceLeaveOrder> leaveOrders,
            List<HrAttendanceOvertimeOrder> overtimeOrders) {
        if (primaryRecord != null) {
            return primaryRecord.getOrgId();
        }
        if (leaveOrders != null && !leaveOrders.isEmpty()) {
            return leaveOrders.get(0).getOrgId();
        }
        if (overtimeOrders != null && !overtimeOrders.isEmpty()) {
            return overtimeOrders.get(0).getOrgId();
        }
        return null;
    }

    /**
     * 解析部门ID。
     *
     * @param primaryRecord 主记录
     * @param leaveOrders 请假单列表
     * @param overtimeOrders 加班单列表
     * @return 部门ID
     */
    private Long resolveDeptId(HrAttendanceRecord primaryRecord, List<HrAttendanceLeaveOrder> leaveOrders,
            List<HrAttendanceOvertimeOrder> overtimeOrders) {
        if (primaryRecord != null) {
            return primaryRecord.getDeptId();
        }
        if (leaveOrders != null && !leaveOrders.isEmpty()) {
            return leaveOrders.get(0).getDeptId();
        }
        if (overtimeOrders != null && !overtimeOrders.isEmpty()) {
            return overtimeOrders.get(0).getDeptId();
        }
        return null;
    }

    /**
     * 将可空整数转为非空值。
     *
     * @param value 原始值
     * @return 非空整数
     */
    private int nullSafeInt(Integer value) {
        return value == null ? 0 : value;
    }

    /**
     * 解析操作人。
     *
     * @param operator 原始操作人
     * @return 规范化操作人
     */
    private String resolveOperator(String operator) {
        return HrAttendanceSupport.trimToNull(operator) == null ? "system" : operator.trim();
    }
}
