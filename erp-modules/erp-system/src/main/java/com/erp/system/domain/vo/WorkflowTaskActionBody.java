package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程任务审批动作请求对象。
 */
@Data
public class WorkflowTaskActionBody implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 审批意见 */
    private String actionComment;
}

