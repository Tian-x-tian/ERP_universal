package com.erp.system.domain.vo;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程效率看板返回对象。
 */
public class WorkflowDashboardVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 汇总指标 */
    private WorkflowDashboardSummaryVO summary = new WorkflowDashboardSummaryVO();

    /** 节点指标列表 */
    private List<WorkflowNodeMetricVO> nodeMetrics = new ArrayList<>();

    /** 部门对比列表 */
    private List<WorkflowDeptMetricVO> deptMetrics = new ArrayList<>();

    /** 流程对比列表 */
    private List<WorkflowProcessMetricVO> processMetrics = new ArrayList<>();


    public WorkflowDashboardSummaryVO getSummary() {
        return summary;
    }

    public void setSummary(WorkflowDashboardSummaryVO summary) {
        this.summary = summary;
    }

    public List<WorkflowNodeMetricVO> getNodeMetrics() {
        return nodeMetrics;
    }

    public void setNodeMetrics(List<WorkflowNodeMetricVO> nodeMetrics) {
        this.nodeMetrics = nodeMetrics;
    }

    public List<WorkflowDeptMetricVO> getDeptMetrics() {
        return deptMetrics;
    }

    public void setDeptMetrics(List<WorkflowDeptMetricVO> deptMetrics) {
        this.deptMetrics = deptMetrics;
    }

    public List<WorkflowProcessMetricVO> getProcessMetrics() {
        return processMetrics;
    }

    public void setProcessMetrics(List<WorkflowProcessMetricVO> processMetrics) {
        this.processMetrics = processMetrics;
    }
}
