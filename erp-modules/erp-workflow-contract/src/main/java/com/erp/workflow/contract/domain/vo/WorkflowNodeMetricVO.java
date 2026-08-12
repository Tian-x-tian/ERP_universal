package com.erp.workflow.contract.domain.vo;


import java.io.Serializable;

/**
 * 节点效率指标。
 */
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


    public String getNodeKey() {
        return nodeKey;
    }

    public void setNodeKey(String nodeKey) {
        this.nodeKey = nodeKey;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public long getTaskCount() {
        return taskCount;
    }

    public void setTaskCount(long taskCount) {
        this.taskCount = taskCount;
    }

    public double getAverageHours() {
        return averageHours;
    }

    public void setAverageHours(double averageHours) {
        this.averageHours = averageHours;
    }

    public long getOvertimeCount() {
        return overtimeCount;
    }

    public void setOvertimeCount(long overtimeCount) {
        this.overtimeCount = overtimeCount;
    }

    public long getRejectCount() {
        return rejectCount;
    }

    public void setRejectCount(long rejectCount) {
        this.rejectCount = rejectCount;
    }
}

