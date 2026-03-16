package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工异动事件底座对象。
 */
@Data
@TableName("hr_employee_change")
public class HrEmployeeChange implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long changeId;
    private String tenantId;
    private Long employeeId;
    private String changeType;
    private Date effectiveDate;
    private String beforeSnapshot;
    private String afterSnapshot;
    private String status;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
