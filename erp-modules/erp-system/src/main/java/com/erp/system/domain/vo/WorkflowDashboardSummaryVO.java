package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * 流程效率看板汇总指标。
 */
@Data
public class WorkflowDashboardSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程实例总数 */
    private long totalInstances;

    /** 进行中实例数 */
    private long runningInstances;

    /** 已完成实例数 */
    private long completedInstances;

    /** 平均审批耗时（小时） */
    private double averageApprovalHours;

    /** 驳回率 */
    private double rejectRate;

    /** 超时率 */
    private double overtimeRate;

    /** 当前超期待处理数 */
    private long overduePendingCount;

    /** 瓶颈节点名称 */
    private String bottleneckNodeName;
}
