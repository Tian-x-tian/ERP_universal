package com.erp.system.domain.vo;


import java.io.Serializable;

/**
 * 流程效率看板汇总指标。
 */
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


    public long getTotalInstances() {
        return totalInstances;
    }

    public void setTotalInstances(long totalInstances) {
        this.totalInstances = totalInstances;
    }

    public long getRunningInstances() {
        return runningInstances;
    }

    public void setRunningInstances(long runningInstances) {
        this.runningInstances = runningInstances;
    }

    public long getCompletedInstances() {
        return completedInstances;
    }

    public void setCompletedInstances(long completedInstances) {
        this.completedInstances = completedInstances;
    }

    public double getAverageApprovalHours() {
        return averageApprovalHours;
    }

    public void setAverageApprovalHours(double averageApprovalHours) {
        this.averageApprovalHours = averageApprovalHours;
    }

    public double getRejectRate() {
        return rejectRate;
    }

    public void setRejectRate(double rejectRate) {
        this.rejectRate = rejectRate;
    }

    public double getOvertimeRate() {
        return overtimeRate;
    }

    public void setOvertimeRate(double overtimeRate) {
        this.overtimeRate = overtimeRate;
    }

    public long getOverduePendingCount() {
        return overduePendingCount;
    }

    public void setOverduePendingCount(long overduePendingCount) {
        this.overduePendingCount = overduePendingCount;
    }

    public String getBottleneckNodeName() {
        return bottleneckNodeName;
    }

    public void setBottleneckNodeName(String bottleneckNodeName) {
        this.bottleneckNodeName = bottleneckNodeName;
    }
}
