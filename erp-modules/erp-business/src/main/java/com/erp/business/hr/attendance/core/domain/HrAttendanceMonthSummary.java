package com.erp.business.hr.attendance.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 员工月出勤汇总。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_attendance_month_summary")
public class HrAttendanceMonthSummary extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "summary_id", type = IdType.AUTO)
    private Long summaryId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private String monthCode;
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
