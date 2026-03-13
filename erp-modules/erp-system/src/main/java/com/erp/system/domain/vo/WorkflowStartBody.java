package com.erp.system.domain.vo;


import java.io.Serializable;
import java.util.Date;

/**
 * 流程发起请求对象。
 */
public class WorkflowStartBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 业务单号 */
    private String businessNo;

    /** 业务类型 */
    private String businessType;

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
