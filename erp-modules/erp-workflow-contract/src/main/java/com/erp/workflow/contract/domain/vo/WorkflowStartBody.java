package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;
import java.util.Date;

/**
 * 流程发起请求对象。
 */
public class WorkflowStartBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 指定流程标识 */
    private String requestedProcessKey;

    /** 所属服务 */
    private String ownerService;

    /** 业务单号 */
    private String businessNo;

    /** 业务类型 */
    private String businessType;

    /** 业务域类型 */
    private String domainType;

    /** 业务动作编码 */
    private String actionCode;

    /** 幂等键 */
    private String idempotencyKey;

    /** 操作人 */
    private String operator;

    /** 起始节点名称 */
    private String nodeName;

    /** 目标审批人用户ID */
    private Long assigneeUserId;

    /** 目标审批人账号 */
    private String assigneeUserName;

    /** 目标审批人昵称 */
    private String assigneeNickName;

    /** 优先级（H高 M中 L低） */
    private String priority;

    /** 截止时间 */
    private Date dueTime;

    /** 发起表单JSON */
    private String formData;

    /** 备注 */
    private String remark;



    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getRequestedProcessKey() {
        return requestedProcessKey;
    }

    public void setRequestedProcessKey(String requestedProcessKey) {
        this.requestedProcessKey = requestedProcessKey;
    }

    public String getOwnerService() {
        return ownerService;
    }

    public void setOwnerService(String ownerService) {
        this.ownerService = ownerService;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getDomainType() {
        return domainType;
    }

    public void setDomainType(String domainType) {
        this.domainType = domainType;
    }

    public String getActionCode() {
        return actionCode;
    }

    public void setActionCode(String actionCode) {
        this.actionCode = actionCode;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
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

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public Date getDueTime() {
        return dueTime;
    }

    public void setDueTime(Date dueTime) {
        this.dueTime = dueTime;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

