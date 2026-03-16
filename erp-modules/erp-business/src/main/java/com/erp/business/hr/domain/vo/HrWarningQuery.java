package com.erp.business.hr.domain.vo;

import java.io.Serializable;

/**
 * HR 预警分页查询参数。
 */
public class HrWarningQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private String warningType;
    private String status;
    private Long employeeId;
    private Long pageNum;
    private Long pageSize;

    public String getWarningType() {
        return warningType;
    }

    public void setWarningType(String warningType) {
        this.warningType = warningType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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
