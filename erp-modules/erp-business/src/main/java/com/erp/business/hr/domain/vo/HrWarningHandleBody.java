package com.erp.business.hr.domain.vo;

import java.io.Serializable;

/**
 * HR 预警处理参数。
 */
public class HrWarningHandleBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private String targetStatus;
    private String remark;

    public String getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(String targetStatus) {
        this.targetStatus = targetStatus;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
