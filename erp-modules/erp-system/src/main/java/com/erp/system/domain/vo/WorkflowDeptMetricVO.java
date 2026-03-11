package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 部门流程效率对比指标。
 */
@Data
public class WorkflowDeptMetricVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 部门ID */
    private Long deptId;

    /** 部门名称 */
    private String deptName;

    /** 发起实例数 */
    private long instanceCount;

    /** 平均耗时（小时） */
    private double averageHours;

    /** 驳回率 */
    private double rejectRate;

    /** 超时率 */
    private double overtimeRate;
}
