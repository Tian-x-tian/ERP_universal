package com.erp.system.ai.model;

import java.io.Serializable;

/**
 * AI 可执行动作描述对象。
 */
public class AiActionDescriptor implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 动作编码。
     */
    private String key;

    /**
     * 动作展示名称。
     */
    private String label;

    /**
     * 风险等级。
     */
    private String riskLevel;

    public AiActionDescriptor() {
    }

    /**
     * 构造动作描述对象。
     *
     * @param key       动作编码
     * @param label     动作展示名称
     * @param riskLevel 风险等级
     */
    public AiActionDescriptor(String key, String label, String riskLevel) {
        this.key = key;
        this.label = label;
        this.riskLevel = riskLevel;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }
}
