package com.erp.ai.model;

import java.io.Serializable;

/**
 * AI 工具调用对象。
 */
public class AiToolCall implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 工具调用标识。
     */
    private String id;

    /**
     * 工具名称。
     */
    private String name;

    /**
     * 工具参数原始 JSON。
     */
    private String argumentsJson;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getArgumentsJson() {
        return argumentsJson;
    }

    public void setArgumentsJson(String argumentsJson) {
        this.argumentsJson = argumentsJson;
    }
}
