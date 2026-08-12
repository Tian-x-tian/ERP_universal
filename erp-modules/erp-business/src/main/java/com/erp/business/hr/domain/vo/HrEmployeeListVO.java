package com.erp.business.hr.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * HR 员工台账列表视图。
 */
@Data
public class HrEmployeeListVO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private Integer versionNo;
    private String empCode;
    private String empName;
    private String mobile;
    private String email;
    private Long orgId;
    private Long deptId;
    private String position;
    private String status;
    private String employmentType;
    private Date hireDate;
    private Date probationEndDate;
    private String certNoMasked;
}
