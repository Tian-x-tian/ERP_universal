package com.erp.ai.model;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 角色引导画像。
 */
public class AiRoleProfileVO {
    private String aiRoleTag;
    private String roleLabel;
    private List<String> learningCards = new ArrayList<>();
    private List<String> firstWeekTasks = new ArrayList<>();
    private List<String> commonMistakes = new ArrayList<>();
    private List<String> suggestedQuestions = new ArrayList<>();

    public String getAiRoleTag() {
        return aiRoleTag;
    }

    public void setAiRoleTag(String aiRoleTag) {
        this.aiRoleTag = aiRoleTag;
    }

    public String getRoleLabel() {
        return roleLabel;
    }

    public void setRoleLabel(String roleLabel) {
        this.roleLabel = roleLabel;
    }

    public List<String> getLearningCards() {
        return learningCards;
    }

    public void setLearningCards(List<String> learningCards) {
        this.learningCards = learningCards;
    }

    public List<String> getFirstWeekTasks() {
        return firstWeekTasks;
    }

    public void setFirstWeekTasks(List<String> firstWeekTasks) {
        this.firstWeekTasks = firstWeekTasks;
    }

    public List<String> getCommonMistakes() {
        return commonMistakes;
    }

    public void setCommonMistakes(List<String> commonMistakes) {
        this.commonMistakes = commonMistakes;
    }

    public List<String> getSuggestedQuestions() {
        return suggestedQuestions;
    }

    public void setSuggestedQuestions(List<String> suggestedQuestions) {
        this.suggestedQuestions = suggestedQuestions;
    }
}
