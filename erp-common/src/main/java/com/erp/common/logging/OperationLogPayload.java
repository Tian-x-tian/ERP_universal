package com.erp.common.logging;

import java.io.Serializable;
import java.util.Date;

/**
 * 操作/审计日志载荷。
 * 由 {@link OperationLogInterceptorSupport} 与 {@link AuditLogAspectSupport} 采集，
 * 交给各服务自行实现的 {@link OperationLogRecorder} 落库或回传。
 */
public class OperationLogPayload implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 日志类型：写操作日志。
     */
    public static final String TYPE_OPERATION = "OPERATION";

    /**
     * 日志类型：查询审计日志。
     */
    public static final String TYPE_AUDIT = "AUDIT";

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
