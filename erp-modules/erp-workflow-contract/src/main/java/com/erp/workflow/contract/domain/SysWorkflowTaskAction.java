package com.erp.workflow.contract.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务动作记录对象 sys_wf_task_action
 */
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



    public Long getActionId() {
        return actionId;
    }

    public void setActionId(Long actionId) {
        this.actionId = actionId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getActionType() {
        return actionType;
    }

    public void setActionType(String actionType) {
        this.actionType = actionType;
    }

    public Long getActionUserId() {
        return actionUserId;
    }

    public void setActionUserId(Long actionUserId) {
        this.actionUserId = actionUserId;
    }

    public String getActionUserName() {
        return actionUserName;
    }

    public void setActionUserName(String actionUserName) {
        this.actionUserName = actionUserName;
    }

    public String getActionNickName() {
        return actionNickName;
    }

    public void setActionNickName(String actionNickName) {
        this.actionNickName = actionNickName;
    }

    public Long getFromAssigneeUserId() {
        return fromAssigneeUserId;
    }

    public void setFromAssigneeUserId(Long fromAssigneeUserId) {
        this.fromAssigneeUserId = fromAssigneeUserId;
    }

    public Long getToAssigneeUserId() {
        return toAssigneeUserId;
    }

    public void setToAssigneeUserId(Long toAssigneeUserId) {
        this.toAssigneeUserId = toAssigneeUserId;
    }

    public String getActionComment() {
        return actionComment;
    }

    public void setActionComment(String actionComment) {
        this.actionComment = actionComment;
    }

    public Date getActionTime() {
        return actionTime;
    }

    public void setActionTime(Date actionTime) {
        this.actionTime = actionTime;
    }
}

