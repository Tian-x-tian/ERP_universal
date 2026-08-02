package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 每日简报视图。
 */
public class PlatformAiBriefView implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long briefId;
    private String tenantId;
    private Long userId;
    private Date briefDate;
    private String briefType;
    /** 状态（PENDING 生成中 / READY 可用 / FAILED 失败） */
    private String status;
    private String summary;
    /** 结构化区块 JSON */
    private String blocksJson;
    private String model;
    private Long generateMs;
    private Date updateTime;

    public Long getBriefId() {
        return briefId;
    }

    public void setBriefId(Long briefId) {
        this.briefId = briefId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Date getBriefDate() {
        return briefDate;
    }

    public void setBriefDate(Date briefDate) {
        this.briefDate = briefDate;
    }

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

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
