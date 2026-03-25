package com.erp.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 策略摘要。
 */
public class AiPolicySummaryVO {
    private List<String> interactionLevels = new ArrayList<>();
    private String strategySummary;
    private List<AiActionDescriptor> allowedActions = new ArrayList<>();

    public List<String> getInteractionLevels() {
        return interactionLevels;
    }

    public void setInteractionLevels(List<String> interactionLevels) {
        this.interactionLevels = interactionLevels;
    }

    public String getStrategySummary() {
        return strategySummary;
    }

    public void setStrategySummary(String strategySummary) {
        this.strategySummary = strategySummary;
    }

    public List<AiActionDescriptor> getAllowedActions() {
        return allowedActions;
    }

    public void setAllowedActions(List<AiActionDescriptor> allowedActions) {
        this.allowedActions = allowedActions;
    }
}
