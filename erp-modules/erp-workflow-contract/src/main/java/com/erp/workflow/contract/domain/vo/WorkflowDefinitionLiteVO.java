package com.erp.workflow.contract.domain.vo;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程定义轻量视图对象。
 */
public class WorkflowDefinitionLiteVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 流程定义ID。
     */
    private Long definitionId;

    /**
     * 流程标识。
     */
    private String processKey;

    /**
     * 流程名称。
     */
    private String processName;

    /**
     * 流程分类。
     */
    private String category;

    /**
     * 版本号。
     */
    private Integer version;

    /**
     * 状态。
     */
    private String status;

    /**
     * 发布时间。
     */
    private Date publishTime;

    /**
     * 创建时间。
     */
    private Date createTime;

    public Long getDefinitionId() {
        return definitionId;
    }

    public void setDefinitionId(Long definitionId) {
        this.definitionId = definitionId;
    }

    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getPublishTime() {
        return publishTime;
    }

    public void setPublishTime(Date publishTime) {
        this.publishTime = publishTime;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
