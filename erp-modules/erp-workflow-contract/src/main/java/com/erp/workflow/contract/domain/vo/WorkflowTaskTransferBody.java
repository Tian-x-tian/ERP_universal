package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;

/**
 * 流程任务转交请求对象。
 */
public class WorkflowTaskTransferBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 目标办理人用户ID */
    private Long targetUserId;

    /** 目标办理人账号 */
    private String targetUserName;

    /** 目标办理人昵称 */
    private String targetNickName;

    /** 转交说明 */
    private String actionComment;



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
}

