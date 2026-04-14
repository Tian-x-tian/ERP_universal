package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 请假单保存参数。
 */
@Data
public class HrAttendanceLeaveBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private String leaveType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String reason;
    private String remark;
}
