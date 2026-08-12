package com.erp.business.hr.attendance.core.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门汇总查询参数。
 */
@Data
public class HrAttendanceDeptSummaryQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long deptId;
    private String date;
    private String month;
    private Boolean includeChildren;
}
