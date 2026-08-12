package com.erp.workflow.contract.domain.vo;

import java.io.Serializable;

/**
 * 首页待办汇总对象。
 */
public class WorkflowHomeTodoSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 待处理数量 */
    private long pendingCount;

    /** 处理中数量 */
    private long processingCount;

    /** 超时数量 */
    private long overdueCount;

    /** 已完成数量 */
    private long completedCount;

    /** 协同已完成数量 */
    private long collaborationDone;

    /** 协同总任务数量 */
    private long collaborationTotal;

    /** 协同完成率 */
    private double collaborationRate;

    public long getPendingCount() {
        return pendingCount;
    }

    public void setPendingCount(long pendingCount) {
        this.pendingCount = pendingCount;
    }

    public long getProcessingCount() {
        return processingCount;
    }

    public void setProcessingCount(long processingCount) {
        this.processingCount = processingCount;
    }

    public long getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(long overdueCount) {
        this.overdueCount = overdueCount;
    }

    public long getCompletedCount() {
        return completedCount;
    }

    public void setCompletedCount(long completedCount) {
        this.completedCount = completedCount;
    }

    public long getCollaborationDone() {
        return collaborationDone;
    }

    public void setCollaborationDone(long collaborationDone) {
        this.collaborationDone = collaborationDone;
    }

    public long getCollaborationTotal() {
        return collaborationTotal;
    }

    public void setCollaborationTotal(long collaborationTotal) {
        this.collaborationTotal = collaborationTotal;
    }

    public double getCollaborationRate() {
        return collaborationRate;
    }

    public void setCollaborationRate(double collaborationRate) {
        this.collaborationRate = collaborationRate;
    }
}
