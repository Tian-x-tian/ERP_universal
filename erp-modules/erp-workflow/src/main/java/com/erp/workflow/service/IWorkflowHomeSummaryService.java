package com.erp.workflow.service;

import com.erp.workflow.contract.domain.vo.WorkflowHomeTodoSummaryVO;

/**
 * 工作流首页汇总服务接口。
 */
public interface IWorkflowHomeSummaryService {

    /**
     * 构建当前用户首页待办汇总数据。
     *
     * @return 待办汇总
     */
    WorkflowHomeTodoSummaryVO buildTodoSummary();
}
