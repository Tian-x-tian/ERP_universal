package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 节点效率指标。
 */
@Data
public class WorkflowNodeMetricVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 节点编码 */
    private String nodeKey;

    /** 节点名称 */
    private String nodeName;

    /** 任务数量 */
    private long taskCount;

    /** 平均耗时（小时） */
    private double averageHours;

    /** 超时数量 */
    private long overtimeCount;

    /** 驳回数量 */
    private long rejectCount;
}
