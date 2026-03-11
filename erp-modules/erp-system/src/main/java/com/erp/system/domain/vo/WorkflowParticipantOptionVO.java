package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程参与人选项对象。
 */
@Data
public class WorkflowParticipantOptionVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 选项值 */
    private Long value;

    /** 选项标签 */
    private String label;

    /** 上级节点值 */
    private Long parentId;
}
