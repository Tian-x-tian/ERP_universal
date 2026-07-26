package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工扩展档案对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_employee_archive")
public class HrEmployeeArchive extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId("employee_id")
    private Long employeeId;
    private String tenantId;
    private String certType;
    private String certNo;
    private String gender;
    private Date birthDate;
    private String employmentType;
    private Date hireDate;
    private Date probationEndDate;
    private String highestEducation;
    private String emergencyContact;
    private String emergencyPhone;
    private String homeAddress;
    private String remark;
}
