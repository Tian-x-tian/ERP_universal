package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务对象 sys_wf_task
 */
@Data
@TableName("sys_wf_task")
public class SysWorkflowTask implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程任务ID */
    @TableId(type = IdType.AUTO)
    private Long taskId;

    /** 租户编号 */
    private String tenantId;

    /** 流程实例ID */
    private Long instanceId;

    /** 流程定义ID */
    private Long definitionId;

    /** 节点编码 */
    private String nodeKey;

    /** 节点名称 */
    private String nodeName;

    /** 候选办理人ID列表（逗号分隔） */
    private String candidateUserIds;

    /** 办理人用户ID */
    private Long assigneeUserId;

    /** 办理人账号 */
    private String assigneeUserName;

    /** 办理人昵称 */
    private String assigneeNickName;

    /** 状态（0待处理 1处理中 2已同意 3已驳回 4已转交 5已取消） */
    private String status;

    /** 审批意见 */
    private String actionComment;

    /** 关联待办ID */
    private Long todoId;

    /** 截止时间 */
    private Date dueTime;

    /** 签收时间 */
    private Date claimTime;

    /** 办结时间 */
    private Date finishTime;

    /** 创建时间 */
    private Date createTime;
}

