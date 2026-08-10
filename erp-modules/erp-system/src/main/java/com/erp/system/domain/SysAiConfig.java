package com.erp.system.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
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
    /*
     * 配额是三态字段：NULL=按实例配置、0=本租户不限制、N=上限 N。
     *
     * 因此必须用 ALWAYS 更新策略——默认的 NOT_NULL 会把 null 从 SET 子句里省掉，
     * 一旦写过具体数值就再也回不到 NULL，「按实例配置」这一档将永远不可达。
     * 与同表其它字段的「不传即保持」不同，这三个字段是「不传即回到 NULL」，
     * 这是三态语义的必然结果；当前唯一的写入方（AI 配置中心）每次都会提交全部三项。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer tenantDailyRequestLimit;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Integer tenantDailyTokenLimit;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
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
