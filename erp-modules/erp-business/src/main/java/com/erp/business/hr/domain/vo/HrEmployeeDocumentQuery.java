package com.erp.business.hr.domain.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 员工电子档案分页查询参数。
 */
public class HrEmployeeDocumentQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String documentType;
    private String status;
    private Date expireDateFrom;
    private Date expireDateTo;
    private Long pageNum;
    private Long pageSize;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getExpireDateFrom() {
        return expireDateFrom;
    }

    public void setExpireDateFrom(Date expireDateFrom) {
        this.expireDateFrom = expireDateFrom;
    }

    public Date getExpireDateTo() {
        return expireDateTo;
    }

    public void setExpireDateTo(Date expireDateTo) {
        this.expireDateTo = expireDateTo;
    }

    public Long getPageNum() {
        return pageNum;
    }

    public void setPageNum(Long pageNum) {
        this.pageNum = pageNum;
    }

    public Long getPageSize() {
        return pageSize;
    }

    public void setPageSize(Long pageSize) {
        this.pageSize = pageSize;
    }
}
