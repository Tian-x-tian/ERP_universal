package com.erp.workflow.contract.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程任务对象 sys_wf_task
 */
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



    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
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

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getCandidateUserIds() {
        return candidateUserIds;
    }

    public void setCandidateUserIds(String candidateUserIds) {
        this.candidateUserIds = candidateUserIds;
    }

    public Long getAssigneeUserId() {
        return assigneeUserId;
    }

    public void setAssigneeUserId(Long assigneeUserId) {
        this.assigneeUserId = assigneeUserId;
    }

    public String getAssigneeUserName() {
        return assigneeUserName;
    }

    public void setAssigneeUserName(String assigneeUserName) {
        this.assigneeUserName = assigneeUserName;
    }

    public String getAssigneeNickName() {
        return assigneeNickName;
    }

    public void setAssigneeNickName(String assigneeNickName) {
        this.assigneeNickName = assigneeNickName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getActionComment() {
        return actionComment;
    }

    public void setActionComment(String actionComment) {
        this.actionComment = actionComment;
    }

    public Long getTodoId() {
        return todoId;
    }

    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }

    public Date getDueTime() {
        return dueTime;
    }

    public void setDueTime(Date dueTime) {
        this.dueTime = dueTime;
    }

    public Date getClaimTime() {
        return claimTime;
    }

    public void setClaimTime(Date claimTime) {
        this.claimTime = claimTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}

