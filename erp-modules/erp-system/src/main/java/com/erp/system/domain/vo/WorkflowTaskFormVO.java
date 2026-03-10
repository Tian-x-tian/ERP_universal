package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程任务动态表单视图对象。
 */
@Data
public class WorkflowTaskFormVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程任务ID */
    private Long taskId;

    /** 流程实例ID */
    private Long instanceId;

    /** 流程定义ID */
    private Long definitionId;

    /** 流程定义版本号 */
    private Integer definitionVersion;

    /** 当前节点编码 */
    private String nodeKey;

    /** 当前节点名称 */
    private String nodeName;

    /** 表单版本号 */
    private Integer formVersion;

    /** 动态表单字段 */
    private List<WorkflowTaskFormFieldVO> fields = new ArrayList<>();
}
