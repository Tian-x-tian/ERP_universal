package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;

/**
 * 流程维度效率指标。
 */
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


    public String getProcessKey() {
        return processKey;
    }

    public void setProcessKey(String processKey) {
        this.processKey = processKey;
    }

    public String getProcessName() {
        return processName;
    }

    public void setProcessName(String processName) {
        this.processName = processName;
    }

    public long getInstanceCount() {
        return instanceCount;
    }

    public void setInstanceCount(long instanceCount) {
        this.instanceCount = instanceCount;
    }

    public double getAverageHours() {
        return averageHours;
    }

    public void setAverageHours(double averageHours) {
        this.averageHours = averageHours;
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
}

