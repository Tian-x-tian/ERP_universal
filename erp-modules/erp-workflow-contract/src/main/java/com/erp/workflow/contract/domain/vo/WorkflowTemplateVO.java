package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程模板对象。
 */
public class WorkflowTemplateVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 模板编码 */
    private String templateCode;

    /** 模板名称 */
    private String templateName;

    /** 所属行业 */
    private String industry;

    /** 流程分类 */
    private String category;

    /** 模板描述 */
    private String description;

    /** 建议流程标识 */
    private String suggestedProcessKey;

    /** 建议流程名称 */
    private String suggestedProcessName;

    /** 模板标签 */
    private List<String> tags = new ArrayList<>();

    /** 表单结构 JSON */
    private String formSchema;

    /** 流程模型 JSON */
    private String modelContent;


    public String getTemplateCode() {
        return templateCode;
    }

    public void setTemplateCode(String templateCode) {
        this.templateCode = templateCode;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getIndustry() {
        return industry;
    }

    public void setIndustry(String industry) {
        this.industry = industry;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestedProcessKey() {
        return suggestedProcessKey;
    }

    public void setSuggestedProcessKey(String suggestedProcessKey) {
        this.suggestedProcessKey = suggestedProcessKey;
    }

    public String getSuggestedProcessName() {
        return suggestedProcessName;
    }

    public void setSuggestedProcessName(String suggestedProcessName) {
        this.suggestedProcessName = suggestedProcessName;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getFormSchema() {
        return formSchema;
    }

    public void setFormSchema(String formSchema) {
        this.formSchema = formSchema;
    }

    public String getModelContent() {
        return modelContent;
    }

    public void setModelContent(String modelContent) {
        this.modelContent = modelContent;
    }
}

