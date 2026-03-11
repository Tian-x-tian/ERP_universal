package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程维度效率指标。
 */
@Data
public class WorkflowProcessMetricVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程标识 */
    private String processKey;

    /** 流程名称 */
    private String processName;

    /** 实例数量 */
    private long instanceCount;

    /** 平均耗时（小时） */
    private double averageHours;

    /** 驳回率 */
    private double rejectRate;

    /** 超时率 */
    private double overtimeRate;
}
