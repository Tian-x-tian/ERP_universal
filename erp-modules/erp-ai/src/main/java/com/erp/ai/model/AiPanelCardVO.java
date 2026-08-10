package com.erp.ai.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 面板可用卡片描述。
 *
 * <p>面板上的每张卡片背后就是一个只读工具；能看到哪些卡片完全由该用户的 RBAC 决定。</p>
 */
public class AiPanelCardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 对应的只读工具名 */
    private String toolName;
    /** 卡片展示名 */
    private String label;
    /** 卡片用途说明 */
    private String description;
    /** 该卡片支持的参数名 */
    private List<String> parameters = new ArrayList<>();

    public AiPanelCardVO() {
    }

    public AiPanelCardVO(String toolName, String label, String description, List<String> parameters) {
        this.toolName = toolName;
        this.label = label;
        this.description = description;
        this.parameters = parameters == null ? new ArrayList<>() : new ArrayList<>(parameters);
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getParameters() {
        return parameters;
    }

    public void setParameters(List<String> parameters) {
        this.parameters = parameters == null ? new ArrayList<>() : parameters;
    }
}
