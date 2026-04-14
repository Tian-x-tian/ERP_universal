package com.erp.business.hr.attendance.core.domain.vo;

import com.erp.business.hr.attendance.core.domain.HrAttendanceDaySummary;
import com.erp.business.hr.attendance.core.domain.HrAttendanceException;
import com.erp.business.hr.attendance.core.domain.HrAttendanceRecord;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 个人日出勤视图对象。
 */
@Data
public class HrAttendancePersonalDayVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private HrAttendanceDaySummary summary;
    private List<HrAttendanceRecord> records;
    private List<HrAttendanceException> exceptions;
    private boolean signedIn;
    private boolean signedOut;
}
