package com.erp.system.domain.vo;


import java.io.Serializable;

/**
 * 流程任务退回请求对象。
 */
public class WorkflowTaskReturnBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标节点编码 */
    private String targetNodeKey;

    /** 目标节点名称 */
    private String targetNodeName;

    /** 目标办理人用户ID */
    private Long targetUserId;

    /** 目标办理人账号 */
    private String targetUserName;

    /** 目标办理人昵称 */
    private String targetNickName;

    /** 退回说明 */
    private String actionComment;

    /** 退回后节点表单JSON */
    private String formData;


    public String getTargetNodeKey() {
        return targetNodeKey;
    }

    public void setTargetNodeKey(String targetNodeKey) {
        this.targetNodeKey = targetNodeKey;
    }

    public String getTargetNodeName() {
        return targetNodeName;
    }

    public void setTargetNodeName(String targetNodeName) {
        this.targetNodeName = targetNodeName;
    }

    public Long getTargetUserId() {
        return targetUserId;
    }

    public void setTargetUserId(Long targetUserId) {
        this.targetUserId = targetUserId;
    }

    public String getTargetUserName() {
        return targetUserName;
    }

    public void setTargetUserName(String targetUserName) {
        this.targetUserName = targetUserName;
    }

    public String getTargetNickName() {
        return targetNickName;
    }

    public void setTargetNickName(String targetNickName) {
        this.targetNickName = targetNickName;
    }

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
