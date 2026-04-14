package com.erp.business.hr.attendance.core.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 出勤原子记录。
 */
@Data
@TableName("hr_attendance_record")
public class HrAttendanceRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private Date workDate;
    private String sourceType;
    private String authorityFlag;
    private String externalBizNo;
    private Date signInTime;
    private Date signOutTime;
    private BigDecimal signInLatitude;
    private BigDecimal signInLongitude;
    private BigDecimal signOutLatitude;
    private BigDecimal signOutLongitude;
    private String signInAddress;
    private String signOutAddress;
    private String signInInRange;
    private String signOutInRange;
    private String deviceSource;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
