package com.erp.business.hr.attendance.core.domain.vo;

import com.erp.business.hr.attendance.core.domain.HrAttendanceMonthSummary;
import lombok.Data;

import java.io.Serializable;

/**
 * 出勤工作台视图对象。
 */
@Data
public class HrAttendanceDashboardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private HrAttendancePersonalDayVO today;
    private HrAttendanceMonthSummary personalMonth;
    private HrAttendanceCompanySummaryVO companySummary;
    private Long pendingExceptionCount;
}
