package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 请假单查询参数。
 */
@Data
public class HrAttendanceLeaveQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String status;
    private LocalDate beginDate;
    private LocalDate endDate;
    private Long pageNum;
    private Long pageSize;
}
