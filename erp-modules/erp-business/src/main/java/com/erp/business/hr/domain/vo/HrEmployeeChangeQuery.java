package com.erp.business.hr.domain.vo;

import java.io.Serializable;

/**
 * 员工异动分页查询参数。
 */
public class HrEmployeeChangeQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String changeType;
    private String status;
    private Long pageNum;
    private Long pageSize;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
