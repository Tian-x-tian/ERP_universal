package com.erp.workflow.contract.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程实例对象 sys_wf_instance
 */
@TableName("sys_wf_instance")
public class SysWorkflowInstance implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程实例ID */
    @TableId(type = IdType.AUTO)
    private Long instanceId;

    /** 租户编号 */
    private String tenantId;

    /** 流程定义ID */
    private Long definitionId;

    /** 发起时流程定义版本号 */
    private Integer definitionVersion;

    /** 流程标识 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 流程分类 */
    private String category;

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

    /** 发起表单数据JSON */
    private String formData;

    /** 发起时表单结构快照JSON */
    private String formSchemaSnapshot;

    /** 发起时流程模型快照JSON */
    private String modelContentSnapshot;

    /** 当前节点名称 */
    private String currentNode;

    /** 发起人用户ID */
    private Long initiatorUserId;

    /** 发起人账号 */
    private String initiatorUserName;

    /** 发起人昵称 */
    private String initiatorNickName;

    /** 状态（0进行中 1已完成 2已驳回 3已撤销） */
    private String status;

    /** 发起时间 */
    private Date startTime;

    /** 结束时间 */
    private Date finishTime;

    /** 最近动作类型 */
    private String lastAction;

    /** 最近动作人ID */
    private Long lastActionUserId;

    /** 最近动作人账号 */
    private String lastActionUserName;

    /** 最近动作时间 */
    private Date lastActionTime;

    /** 备注 */
    private String remark;


    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public Integer getDefinitionVersion() {
        return definitionVersion;
    }

    public void setDefinitionVersion(Integer definitionVersion) {
        this.definitionVersion = definitionVersion;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public String getFormSchemaSnapshot() {
        return formSchemaSnapshot;
    }

    public void setFormSchemaSnapshot(String formSchemaSnapshot) {
        this.formSchemaSnapshot = formSchemaSnapshot;
    }

    public String getModelContentSnapshot() {
        return modelContentSnapshot;
    }

    public void setModelContentSnapshot(String modelContentSnapshot) {
        this.modelContentSnapshot = modelContentSnapshot;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public Long getInitiatorUserId() {
        return initiatorUserId;
    }

    public void setInitiatorUserId(Long initiatorUserId) {
        this.initiatorUserId = initiatorUserId;
    }

    public String getInitiatorUserName() {
        return initiatorUserName;
    }

    public void setInitiatorUserName(String initiatorUserName) {
        this.initiatorUserName = initiatorUserName;
    }

    public String getInitiatorNickName() {
        return initiatorNickName;
    }

    public void setInitiatorNickName(String initiatorNickName) {
        this.initiatorNickName = initiatorNickName;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getFinishTime() {
        return finishTime;
    }

    public void setFinishTime(Date finishTime) {
        this.finishTime = finishTime;
    }

    public String getLastAction() {
        return lastAction;
    }

    public void setLastAction(String lastAction) {
        this.lastAction = lastAction;
    }

    public Long getLastActionUserId() {
        return lastActionUserId;
    }

    public void setLastActionUserId(Long lastActionUserId) {
        this.lastActionUserId = lastActionUserId;
    }

    public String getLastActionUserName() {
        return lastActionUserName;
    }

    public void setLastActionUserName(String lastActionUserName) {
        this.lastActionUserName = lastActionUserName;
    }

    public Date getLastActionTime() {
        return lastActionTime;
    }

    public void setLastActionTime(Date lastActionTime) {
        this.lastActionTime = lastActionTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}

