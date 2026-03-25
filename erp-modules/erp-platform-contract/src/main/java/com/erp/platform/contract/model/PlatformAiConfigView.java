package com.erp.platform.contract.model;

import java.io.Serializable;
import java.util.Date;

/**
 * 平台 AI 租户配置视图。
 */
public class PlatformAiConfigView implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tenantId;
    private boolean enabled;
    private String model;
    private Integer maxHistoryTurns;
    private Integer maxTodoItems;
    private Integer maxNoticeItems;
    private String promptTemplate;
    private String configVersion;
    private String updateBy;
    private Date updateTime;

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getMaxHistoryTurns() {
        return maxHistoryTurns;
    }

    public void setMaxHistoryTurns(Integer maxHistoryTurns) {
        this.maxHistoryTurns = maxHistoryTurns;
    }

    public Integer getMaxTodoItems() {
        return maxTodoItems;
    }

    public void setMaxTodoItems(Integer maxTodoItems) {
        this.maxTodoItems = maxTodoItems;
    }

    public Integer getMaxNoticeItems() {
        return maxNoticeItems;
    }

    public void setMaxNoticeItems(Integer maxNoticeItems) {
        this.maxNoticeItems = maxNoticeItems;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getConfigVersion() {
        return configVersion;
    }

    public void setConfigVersion(String configVersion) {
        this.configVersion = configVersion;
    }

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }
}
