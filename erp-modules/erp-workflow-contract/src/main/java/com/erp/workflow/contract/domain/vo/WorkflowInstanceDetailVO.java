package com.erp.workflow.contract.domain.vo;

import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.SysWorkflowTaskAction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例详情视图对象。
 */
public class WorkflowInstanceDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程实例 */
    private SysWorkflowInstance instance;

    /** 任务列表 */
    private List<SysWorkflowTask> taskList = new ArrayList<>();

    /** 动作历史 */
    private List<SysWorkflowTaskAction> actionList = new ArrayList<>();



    public SysWorkflowInstance getInstance() {
        return instance;
    }

    public void setInstance(SysWorkflowInstance instance) {
        this.instance = instance;
    }

    public List<SysWorkflowTask> getTaskList() {
        return taskList;
    }

    public void setTaskList(List<SysWorkflowTask> taskList) {
        this.taskList = taskList;
    }

    public List<SysWorkflowTaskAction> getActionList() {
        return actionList;
    }

    public void setActionList(List<SysWorkflowTaskAction> actionList) {
        this.actionList = actionList;
    }
}


