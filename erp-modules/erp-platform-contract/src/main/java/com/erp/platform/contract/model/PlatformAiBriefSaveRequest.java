package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * AI 每日简报回写请求。
 */
public class PlatformAiBriefSaveRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private String briefType;
    private String status;
    private String summary;
    private String blocksJson;
    private String model;
    private Long generateMs;

    public String getBriefType() {
        return briefType;
    }

    public void setBriefType(String briefType) {
        this.briefType = briefType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getBlocksJson() {
        return blocksJson;
    }

    public void setBlocksJson(String blocksJson) {
        this.blocksJson = blocksJson;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Long getGenerateMs() {
        return generateMs;
    }

    public void setGenerateMs(Long generateMs) {
        this.generateMs = generateMs;
    }
}
