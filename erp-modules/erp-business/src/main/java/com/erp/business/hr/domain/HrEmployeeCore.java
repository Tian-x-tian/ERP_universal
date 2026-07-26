package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工核心主档镜像对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("mdm_employee")
public class HrEmployeeCore extends BaseAuditEntity implements Serializable {
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
}
