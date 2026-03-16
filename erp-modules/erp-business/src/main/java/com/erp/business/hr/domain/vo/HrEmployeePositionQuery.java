package com.erp.business.hr.domain.vo;

import java.io.Serializable;

/**
 * 员工任职查询参数。
 */
public class HrEmployeePositionQuery implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long employeeId;
    private String status;
    private Long pageNum;
    private Long pageSize;

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
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
