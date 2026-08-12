package com.erp.workflow.contract.domain.vo;

import java.io.Serializable;

/**
 * 工作流终态回调事件。
 */
public class WorkflowCallbackEvent implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程实例ID */
    private Long instanceId;

    /** 所属服务 */
    private String ownerService;

    /** 租户编号 */
    private String tenantId;

    /** 流程标识 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 业务类型 */
    private String businessType;

    /** 业务单号 */
    private String businessNo;

    /** 业务域类型 */
    private String domainType;

    /** 动作编码 */
    private String actionCode;

    /** 幂等键 */
    private String idempotencyKey;

    /** 流程状态 */
    private String status;

    /** 表单数据 */
    private String formData;

    /** 最近动作人账号 */
    private String operator;

    public Long getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(Long instanceId) {
        this.instanceId = instanceId;
    }

    public String getOwnerService() {
        return ownerService;
    }

    public void setOwnerService(String ownerService) {
        this.ownerService = ownerService;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
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

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getBusinessNo() {
        return businessNo;
    }

    public void setBusinessNo(String businessNo) {
        this.businessNo = businessNo;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFormData() {
        return formData;
    }

    public void setFormData(String formData) {
        this.formData = formData;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }
}
