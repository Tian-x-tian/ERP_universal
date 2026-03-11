package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程模板对象。
 */
@Data
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
}
