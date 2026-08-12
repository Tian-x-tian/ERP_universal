package com.erp.ai.model;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 工具定义对象。
 */
public class AiToolDefinition implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 工具名称。
     */
    private String name;

    /**
     * 工具说明。
     */
    private String description;

    /**
     * JSON Schema 参数定义。
     */
    private Map<String, Object> parameters = new LinkedHashMap<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }
}
