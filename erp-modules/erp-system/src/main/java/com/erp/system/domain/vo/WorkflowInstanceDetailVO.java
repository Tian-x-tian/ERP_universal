package com.erp.system.domain.vo;

import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.SysWorkflowTaskAction;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 流程实例详情视图对象。
 */
@Data
public class WorkflowInstanceDetailVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 流程实例 */
    private SysWorkflowInstance instance;

    /** 任务列表 */
    private List<SysWorkflowTask> taskList = new ArrayList<>();

    /** 动作历史 */
    private List<SysWorkflowTaskAction> actionList = new ArrayList<>();
}

