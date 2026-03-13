package com.erp.system.domain.vo;


import java.io.Serializable;

/**
 * 流程参与人选项对象。
 */
public class WorkflowParticipantOptionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 选项值 */
    private Long value;

    /** 选项标签 */
    private String label;

    /** 上级节点值 */
    private Long parentId;


    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }
}
