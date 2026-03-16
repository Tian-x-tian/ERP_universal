package com.erp.business.hr.domain.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

/**
 * 员工异动提交参数。
 */
public class HrEmployeeChangeSubmitBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private String changeType;
    private Date effectiveDate;
    private String remark;
    private Map<String, Object> employee;
    private HrEmployeeArchiveBody archive;

    public String getChangeType() {
        return changeType;
    }

    public void setChangeType(String changeType) {
        this.changeType = changeType;
    }

    public Date getEffectiveDate() {
        return effectiveDate;
    }

    public void setEffectiveDate(Date effectiveDate) {
        this.effectiveDate = effectiveDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public Map<String, Object> getEmployee() {
        return employee;
    }

    public void setEmployee(Map<String, Object> employee) {
        this.employee = employee;
    }

    public HrEmployeeArchiveBody getArchive() {
        return archive;
    }

    public void setArchive(HrEmployeeArchiveBody archive) {
        this.archive = archive;
    }
}
