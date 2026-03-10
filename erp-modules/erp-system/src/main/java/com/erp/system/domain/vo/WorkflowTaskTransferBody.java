package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程任务转交请求对象。
 */
@Data
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
}

