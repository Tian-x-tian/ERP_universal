package com.erp.workflow.controller;

import com.erp.common.core.domain.R;
import com.erp.workflow.contract.domain.vo.WorkflowHomeTodoSummaryVO;
import com.erp.workflow.service.IWorkflowHomeSummaryService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 工作流首页汇总控制层。
 */
@RestController
@ConditionalOnProperty(name = "erp.workflow.http-enabled", havingValue = "true")
@RequestMapping("/workflow/home")
public class WorkflowHomeController {

    private final IWorkflowHomeSummaryService workflowHomeSummaryService;

    public WorkflowHomeController(IWorkflowHomeSummaryService workflowHomeSummaryService) {
        this.workflowHomeSummaryService = workflowHomeSummaryService;
    }

    /**
     * 查询首页待办汇总。
     *
     * @return 待办汇总
     */
    @GetMapping("/todo-summary")
    public R<WorkflowHomeTodoSummaryVO> todoSummary() {
        return R.success(workflowHomeSummaryService.buildTodoSummary());
    }
}
