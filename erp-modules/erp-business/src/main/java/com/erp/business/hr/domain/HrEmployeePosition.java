package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工任职关系底座对象。
 */
@Data
@TableName("hr_employee_position")
public class HrEmployeePosition implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long positionId;
    private String tenantId;
    private Long employeeId;
    private Long orgId;
    private Long deptId;
    private Long postId;
    private String postName;
    private String positionType;
    private String primaryFlag;
    private Date startDate;
    private Date endDate;
    private String status;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
