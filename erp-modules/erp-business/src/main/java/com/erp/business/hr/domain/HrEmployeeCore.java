package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工核心主档镜像对象。
 */
@Data
@TableName("mdm_employee")
public class HrEmployeeCore implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "employee_id", type = IdType.AUTO)
    private Long employeeId;
    private String tenantId;
    private String empCode;
    private String empName;
    private String mobile;
    private String email;
    private Long orgId;
    private Long deptId;
    private String position;
    private Long userId;
    private Long costCenterId;
    private String status;
    private Date effectiveTime;
    private Integer versionNo;
    private String delFlag;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
