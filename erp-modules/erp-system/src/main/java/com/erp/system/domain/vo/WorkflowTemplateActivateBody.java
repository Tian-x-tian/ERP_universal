package com.erp.system.domain.vo;


import java.io.Serializable;

/**
 * 启用流程模板请求对象。
 */
public class WorkflowTemplateActivateBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标流程标识 */
    private String processKey;

    /** 目标流程名称 */
    private String processName;


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
}
