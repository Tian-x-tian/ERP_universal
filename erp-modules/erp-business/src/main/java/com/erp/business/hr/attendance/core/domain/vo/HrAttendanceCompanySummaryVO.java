package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 公司出勤汇总视图对象。
 */
@Data
public class HrAttendanceCompanySummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String dateLabel;
    private Long deptCount;
    private Long employeeCount;
    private BigDecimal attendanceDays;
    private Integer actualMinutes;
    private Integer leaveMinutes;
    private BigDecimal leaveDays;
    private Integer overtimeMinutes;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer missingCardCount;
    private BigDecimal absenteeismDays;
    private Integer abnormalCount;
}
