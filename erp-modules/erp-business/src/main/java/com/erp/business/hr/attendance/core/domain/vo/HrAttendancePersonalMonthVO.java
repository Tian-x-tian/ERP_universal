package com.erp.business.hr.attendance.core.domain.vo;

import com.erp.business.hr.attendance.core.domain.HrAttendanceDaySummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceMonthSummary;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 个人月出勤视图对象。
 */
@Data
public class HrAttendancePersonalMonthVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private HrAttendanceMonthSummary summary;
    private List<HrAttendanceDaySummary> dayList;
}
