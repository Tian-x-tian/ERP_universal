package com.erp.business.hr.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工电子档案底座对象。
 */
@Data
@TableName("hr_employee_document")
public class HrEmployeeDocument implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long documentId;
    private String tenantId;
    private Long employeeId;
    private String documentType;
    private String documentName;
    private String fileUrl;
    private Date expireDate;
    private String status;
    private String remark;
    private String createBy;
    private Date createTime;
    private String updateBy;
    private Date updateTime;
}
