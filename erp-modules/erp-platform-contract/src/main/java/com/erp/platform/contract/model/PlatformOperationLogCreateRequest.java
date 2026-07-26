package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 平台操作/审计日志写入请求。
 * 非 erp-system 的服务通过内部接口把日志回传给 erp-system 落库，
 * 避免跨服务直接写 sys_oper_log / sys_audit_log。
 */
public class PlatformOperationLogCreateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日志类型：OPERATION 写操作日志，AUDIT 查询审计日志。
     */
    private String logType;

    private String tenantId;
    private String operator;
    private String operationType;
    private String requestMethod;
    private String requestUri;
    private String requestIp;
    private String requestParams;
    private Integer responseCode;
    private String successFlag;
    private String errorMsg;
    private Long costTime;
    private Date operationTime;

    public String getLogType() {
        return logType;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getOperator() {
        return operator;
    }

    public void setOperator(String operator) {
        this.operator = operator;
    }

    public String getOperationType() {
        return operationType;
    }

    public void setOperationType(String operationType) {
        this.operationType = operationType;
    }

    public String getRequestMethod() {
        return requestMethod;
    }

    public void setRequestMethod(String requestMethod) {
        this.requestMethod = requestMethod;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getRequestIp() {
        return requestIp;
    }

    public void setRequestIp(String requestIp) {
        this.requestIp = requestIp;
    }

    public String getRequestParams() {
        return requestParams;
    }

    public void setRequestParams(String requestParams) {
        this.requestParams = requestParams;
    }

    public Integer getResponseCode() {
        return responseCode;
    }

    public void setResponseCode(Integer responseCode) {
        this.responseCode = responseCode;
    }

    public String getSuccessFlag() {
        return successFlag;
    }

    public void setSuccessFlag(String successFlag) {
        this.successFlag = successFlag;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        this.errorMsg = errorMsg;
    }

    public Long getCostTime() {
        return costTime;
    }

    public void setCostTime(Long costTime) {
        this.costTime = costTime;
    }

    public Date getOperationTime() {
        return operationTime;
    }

    public void setOperationTime(Date operationTime) {
        this.operationTime = operationTime;
    }
}
