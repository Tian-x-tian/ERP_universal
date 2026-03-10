package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务动作记录对象 sys_wf_task_action
 */
@Data
@TableName("sys_wf_task_action")
public class SysWorkflowTaskAction implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 动作记录ID */
    @TableId(type = IdType.AUTO)
    private Long actionId;

    /** 租户编号 */
    private String tenantId;

    /** 流程实例ID */
    private Long instanceId;

    /** 流程任务ID */
    private Long taskId;

    /** 流程定义ID */
    private Long definitionId;

    /** 节点名称 */
    private String nodeName;

    /** 动作类型（START/CLAIM/APPROVE/REJECT/TRANSFER/CANCEL） */
    private String actionType;

    /** 动作执行人用户ID */
    private Long actionUserId;

    /** 动作执行人账号 */
    private String actionUserName;

    /** 动作执行人昵称 */
    private String actionNickName;

    /** 来源办理人用户ID */
    private Long fromAssigneeUserId;

    /** 目标办理人用户ID */
    private Long toAssigneeUserId;

    /** 动作意见 */
    private String actionComment;

    /** 动作时间 */
    private Date actionTime;
}

