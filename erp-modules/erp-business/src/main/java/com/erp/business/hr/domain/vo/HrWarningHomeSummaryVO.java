package com.erp.business.hr.domain.vo;

import java.io.Serializable;

/**
 * HR 预警首页汇总对象。
 */
public class HrWarningHomeSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 异常员工数 */
    private long abnormalEmployeeCount;

    /** 紧急预警数 */
    private long urgentWarningCount;

    public long getAbnormalEmployeeCount() {
        return abnormalEmployeeCount;
    }

    public void setAbnormalEmployeeCount(long abnormalEmployeeCount) {
        this.abnormalEmployeeCount = abnormalEmployeeCount;
    }

    public long getUrgentWarningCount() {
        return urgentWarningCount;
    }

    public void setUrgentWarningCount(long urgentWarningCount) {
        this.urgentWarningCount = urgentWarningCount;
    }
}
