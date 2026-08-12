package com.erp.workflow.controller;

import com.erp.common.core.domain.R;
import com.erp.workflow.contract.domain.vo.WorkflowHomeTodoSummaryVO;
import com.erp.workflow.service.IWorkflowHomeSummaryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 * 工作流首页控制器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowHomeControllerTest {

    @Mock
    private IWorkflowHomeSummaryService workflowHomeSummaryService;

    /**
     * 验证控制器返回结构完整。
     */
    @Test
    void shouldReturnTodoSummary() {
        WorkflowHomeTodoSummaryVO summaryVO = new WorkflowHomeTodoSummaryVO();
        summaryVO.setPendingCount(1L);
        when(workflowHomeSummaryService.buildTodoSummary()).thenReturn(summaryVO);
        WorkflowHomeController controller = new WorkflowHomeController(workflowHomeSummaryService);

        R<WorkflowHomeTodoSummaryVO> response = controller.todoSummary();

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getData());
        Assertions.assertEquals(1L, response.getData().getPendingCount());
    }
}
