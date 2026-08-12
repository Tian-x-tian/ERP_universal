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
 * 请假单据。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_attendance_leave_order")
public class HrAttendanceLeaveOrder extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private String orderNo;
    private String leaveType;
    private Date startTime;
    private Date endTime;
    private Integer leaveMinutes;
    private BigDecimal leaveDays;
    private String status;
    private String processKey;
    private String workflowInstanceNo;
    private String reason;
    private String remark;
}
