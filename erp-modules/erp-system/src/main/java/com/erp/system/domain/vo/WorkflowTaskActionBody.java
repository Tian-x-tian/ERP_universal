package com.erp.system.domain.vo;


import java.io.Serializable;

/**
 * 流程任务审批动作请求对象。
 */
public class WorkflowTaskActionBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 审批意见 */
    private String actionComment;

    /** 动作提交表单JSON（用于节点必填校验与表单留痕） */
    private String formData;


    public String getActionComment() {
        return actionComment;
    }

    public void setActionComment(String actionComment) {
        this.actionComment = actionComment;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }
}
