package com.erp.business.hr.domain.vo;

import java.io.Serializable;

/**
 * 出勤回传参数。
 */
public class HrAttendanceCallbackBody implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long logId;
    private String externalBizNo;
    private String syncStatus;
    private String resultMessage;
    private String payloadJson;

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public String getExternalBizNo() {
        return externalBizNo;
    }

    public void setExternalBizNo(String externalBizNo) {
        this.externalBizNo = externalBizNo;
    }

    public String getSyncStatus() {
        return syncStatus;
    }

    public void setSyncStatus(String syncStatus) {
        this.syncStatus = syncStatus;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String resultMessage) {
        this.resultMessage = resultMessage;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }
}

