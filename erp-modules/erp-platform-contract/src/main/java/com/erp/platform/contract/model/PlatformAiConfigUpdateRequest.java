package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * 平台 AI 租户配置更新请求。
 */
public class PlatformAiConfigUpdateRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    private Boolean enabled;
    private String model;
    private Integer maxHistoryTurns;
    private Integer maxTodoItems;
    private Integer maxNoticeItems;
    private String promptTemplate;

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
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
}
