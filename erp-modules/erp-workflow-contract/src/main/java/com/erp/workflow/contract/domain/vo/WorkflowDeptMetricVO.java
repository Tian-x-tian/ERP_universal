package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;

/**
 * 部门流程效率对比指标。
 */
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


    public Long getDeptId() {
        return deptId;
    }

    public void setDeptId(Long deptId) {
        this.deptId = deptId;
    }

    public String getDeptName() {
        return deptName;
    }

    public void setDeptName(String deptName) {
        this.deptName = deptName;
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

