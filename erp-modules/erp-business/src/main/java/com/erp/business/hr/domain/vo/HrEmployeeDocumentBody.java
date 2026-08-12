package com.erp.business.hr.domain.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工电子档案元数据保存参数。
 */
public class HrEmployeeDocumentBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String documentType;
    private String documentName;
    private Date expireDate;
    private String status;
    private String remark;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getDocumentType() {
        return documentType;
    }

    public void setDocumentType(String documentType) {
        this.documentType = documentType;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public Date getExpireDate() {
        return expireDate;
    }

    public void setExpireDate(Date expireDate) {
        this.expireDate = expireDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
