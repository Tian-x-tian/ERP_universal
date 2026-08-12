package com.erp.business.hr.attendance.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 出勤异常记录。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_attendance_exception")
public class HrAttendanceException extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "exception_id", type = IdType.AUTO)
    private Long exceptionId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private Date workDate;
    private Long recordId;
    private String exceptionType;
    private String exceptionMessage;
    private String sourceType;
}
