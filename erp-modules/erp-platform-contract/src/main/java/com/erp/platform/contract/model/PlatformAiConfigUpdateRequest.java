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
    /** 租户每日请求上限，0 或空表示不限制 */
    private Integer tenantDailyRequestLimit;
    /** 租户每日 token 上限，0 或空表示不限制 */
    private Integer tenantDailyTokenLimit;
    /** 单用户每日请求上限，0 或空表示不限制 */
    private Integer userDailyRequestLimit;

    public Integer getTenantDailyRequestLimit() {
        return tenantDailyRequestLimit;
    }

    public void setTenantDailyRequestLimit(Integer tenantDailyRequestLimit) {
        this.tenantDailyRequestLimit = tenantDailyRequestLimit;
    }

    public Integer getTenantDailyTokenLimit() {
        return tenantDailyTokenLimit;
    }

    public void setTenantDailyTokenLimit(Integer tenantDailyTokenLimit) {
        this.tenantDailyTokenLimit = tenantDailyTokenLimit;
    }

    public Integer getUserDailyRequestLimit() {
        return userDailyRequestLimit;
    }

    public void setUserDailyRequestLimit(Integer userDailyRequestLimit) {
        this.userDailyRequestLimit = userDailyRequestLimit;
    }

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
