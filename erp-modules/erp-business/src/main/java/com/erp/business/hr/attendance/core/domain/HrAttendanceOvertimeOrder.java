package com.erp.business.hr.attendance.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 加班单据。
 */
@Data
@TableName("hr_attendance_overtime_order")
public class HrAttendanceOvertimeOrder implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "order_id", type = IdType.AUTO)
    private Long orderId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private String orderNo;
    private String overtimeType;
    private Date startTime;
    private Date endTime;
    private Integer overtimeMinutes;
    private String status;
    private String processKey;
    private String workflowInstanceNo;
    private String reason;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
