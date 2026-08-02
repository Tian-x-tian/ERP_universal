package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.common.mybatis.BaseAuditEntity;

import java.io.Serializable;

/**
 * AI 租户配置对象 sys_ai_config。
 */
@TableName("sys_ai_config")
public class SysAiConfig extends BaseAuditEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long configId;
    private String tenantId;
    private String enabled;
    private String model;
    private Integer maxHistoryTurns;
    private Integer maxTodoItems;
    private Integer maxNoticeItems;
    private String promptTemplate;
    private String actionPolicyJson;
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

    public Long getConfigId() {
        return configId;
    }

    public void setConfigId(Long configId) {
        this.configId = configId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public String getEnabled() {
        return enabled;
    }

    public void setEnabled(String enabled) {
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

    public String getActionPolicyJson() {
        return actionPolicyJson;
    }

    public void setActionPolicyJson(String actionPolicyJson) {
        this.actionPolicyJson = actionPolicyJson;
    }

}
