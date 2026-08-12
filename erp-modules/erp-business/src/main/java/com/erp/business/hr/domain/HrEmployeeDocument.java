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
 * 员工电子档案底座对象。
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("hr_employee_document")
public class HrEmployeeDocument extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long documentId;
    private String tenantId;
    private Long employeeId;
    private String documentType;
    private String documentName;
    private String fileUrl;
    private Long fileSize;
    private Date expireDate;
    private String status;
    private String remark;
}
