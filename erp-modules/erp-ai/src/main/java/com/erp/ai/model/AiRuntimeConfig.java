package com.erp.ai.model;

import com.erp.platform.contract.model.PlatformAiActionPolicyItem;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 运行时配置。
 */
public class AiRuntimeConfig {
    private boolean enabled;
    private String model;
    private int maxHistoryTurns;
    private int maxTodoItems;
    private int maxNoticeItems;
    private String promptTemplate;
    private String tenantConfigVersion;
    private List<PlatformAiActionPolicyItem> actionPolicyList = new ArrayList<>();

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

    public int getMaxHistoryTurns() {
        return maxHistoryTurns;
    }

    public void setMaxHistoryTurns(int maxHistoryTurns) {
        this.maxHistoryTurns = maxHistoryTurns;
    }

    public int getMaxTodoItems() {
        return maxTodoItems;
    }

    public void setMaxTodoItems(int maxTodoItems) {
        this.maxTodoItems = maxTodoItems;
    }

    public int getMaxNoticeItems() {
        return maxNoticeItems;
    }

    public void setMaxNoticeItems(int maxNoticeItems) {
        this.maxNoticeItems = maxNoticeItems;
    }

    public String getPromptTemplate() {
        return promptTemplate;
    }

    public void setPromptTemplate(String promptTemplate) {
        this.promptTemplate = promptTemplate;
    }

    public String getTenantConfigVersion() {
        return tenantConfigVersion;
    }

    public void setTenantConfigVersion(String tenantConfigVersion) {
        this.tenantConfigVersion = tenantConfigVersion;
    }

    public List<PlatformAiActionPolicyItem> getActionPolicyList() {
        return actionPolicyList;
    }

    public void setActionPolicyList(List<PlatformAiActionPolicyItem> actionPolicyList) {
        this.actionPolicyList = actionPolicyList;
    }
}
