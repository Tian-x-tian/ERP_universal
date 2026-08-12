package com.erp.business.hr.domain.vo;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 薪资推送参数。
 */
public class HrSalaryPushBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private String periodCode;
    private Date periodDate;
    private List<Long> employeeIds;
    private String triggerType;

    public String getPeriodCode() {
        return periodCode;
    }

    public void setPeriodCode(String periodCode) {
        this.periodCode = periodCode;
    }

    public Date getPeriodDate() {
        return periodDate;
    }

    public void setPeriodDate(Date periodDate) {
        this.periodDate = periodDate;
    }

    public List<Long> getEmployeeIds() {
        return employeeIds;
    }

    public void setEmployeeIds(List<Long> employeeIds) {
        this.employeeIds = employeeIds;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }
}
