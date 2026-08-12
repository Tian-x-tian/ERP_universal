package com.erp.workflow.contract.domain.vo;

import java.io.Serializable;

/**
 * 业务动作可选流程对象。
 */
public class WorkflowProcessOptionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 是否默认 */
    private Boolean isDefault;

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean aDefault) {
        isDefault = aDefault;
    }
}


