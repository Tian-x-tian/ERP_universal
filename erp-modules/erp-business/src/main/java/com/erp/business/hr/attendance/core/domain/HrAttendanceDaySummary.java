package com.erp.business.hr.attendance.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 员工日出勤汇总。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_attendance_day_summary")
public class HrAttendanceDaySummary extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "summary_id", type = IdType.AUTO)
    private Long summaryId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private Date workDate;
    private String monthCode;
    private Long authorityRecordId;
    private String primarySourceType;
    private Date signInTime;
    private Date signOutTime;
    private Integer actualMinutes;
    private BigDecimal attendanceDays;
    private Integer leaveMinutes;
    private BigDecimal leaveDays;
    private Integer overtimeMinutes;
    private Integer lateCount;
    private Integer earlyLeaveCount;
    private Integer missingCardCount;
    private BigDecimal absenteeismDays;
    private Integer abnormalCount;
    private String remark;
}
