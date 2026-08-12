package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 部门出勤汇总视图对象。
 */
@Data
public class HrAttendanceDeptSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long deptId;
    private String deptName;
    private String dateLabel;
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
