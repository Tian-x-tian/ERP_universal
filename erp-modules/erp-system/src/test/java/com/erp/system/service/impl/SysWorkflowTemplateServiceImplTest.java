package com.erp.system.service.impl;

import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.vo.WorkflowTemplateActivateBody;
import com.erp.system.domain.vo.WorkflowTemplateVO;
import com.erp.system.service.ISysWorkflowDefinitionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 流程模板服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysWorkflowTemplateServiceImplTest {

    @Mock
    private ISysWorkflowDefinitionService workflowDefinitionService;

    private SysWorkflowTemplateServiceImpl workflowTemplateService;

    /**
     * 初始化测试对象。
     */
    @BeforeEach
    void setUp() {
        workflowTemplateService = new SysWorkflowTemplateServiceImpl(workflowDefinitionService);
    }

    /**
     * 验证模板市场可按行业过滤。
     */
    @Test
    void shouldFilterTemplateByIndustry() {
        List<WorkflowTemplateVO> templateList = workflowTemplateService.selectTemplateList("制造");

        Assertions.assertFalse(templateList.isEmpty());
        Assertions.assertEquals("制造", templateList.get(0).getIndustry());
    }

    /**
     * 验证启用模板时会生成唯一流程标识并返回草稿。
     */
    @Test
    void shouldActivateTemplateWithUniqueProcessKey() {
        when(workflowDefinitionService.count(any())).thenReturn(1L, 0L);
        when(workflowDefinitionService.createDefinition(any(SysWorkflowDefinition.class), any())).thenReturn(true);
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setDefinitionId(88L);
        definition.setProcessKey("service_expense_1");
        when(workflowDefinitionService.getOne(any())).thenReturn(definition);

        WorkflowTemplateActivateBody activateBody = new WorkflowTemplateActivateBody();
        activateBody.setProcessKey("service_expense");
        activateBody.setProcessName("服务业费用报销");

        SysWorkflowDefinition result = workflowTemplateService.activateTemplate("TPL_SERVICE_EXPENSE", activateBody, "admin");

        Assertions.assertNotNull(result);
        Assertions.assertEquals(88L, result.getDefinitionId());
        Assertions.assertEquals("service_expense_1", result.getProcessKey());
    }
}
