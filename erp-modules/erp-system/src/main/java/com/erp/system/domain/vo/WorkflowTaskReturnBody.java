package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程任务退回请求对象。
 */
@Data
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
}
