package com.erp.business.hr.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HR 员工扩展档案提交参数。
 */
@Data
public class HrEmployeeArchiveBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
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
