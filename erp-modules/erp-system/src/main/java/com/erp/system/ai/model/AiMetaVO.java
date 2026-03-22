package com.erp.system.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 能力元信息。
 */
public class AiMetaVO {
    /**
     * AI 功能是否启用。
     */
    private boolean enabled;

    /**
     * 当前模型服务是否可用。
     */
    private boolean available;

    /**
     * 当前模型编号。
     */
    private String model;

    /**
     * 能力标识列表。
     */
    private List<String> capabilities = new ArrayList<>();

    /**
     * 当前状态提示信息。
     */
    private String message;

    /**
     * 当前用户可执行的动作列表。
     */
    private List<AiActionDescriptor> actions = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public void setCapabilities(List<String> capabilities) {
        this.capabilities = capabilities;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<AiActionDescriptor> getActions() {
        return actions;
    }

    public void setActions(List<AiActionDescriptor> actions) {
        this.actions = actions;
    }
}
