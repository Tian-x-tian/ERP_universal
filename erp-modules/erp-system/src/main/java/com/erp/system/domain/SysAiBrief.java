package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

/**
 * AI 每日简报对象 sys_ai_brief。
 */
@TableName("sys_ai_brief")
public class SysAiBrief implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long briefId;
    private String tenantId;
    private Long userId;
    private Date briefDate;
    private String briefType;
    /** 状态（PENDING/READY/FAILED） */
    private String status;
    private String summary;
    private String blocksJson;
    private String model;
    private Long generateMs;
    private Date createTime;
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

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
