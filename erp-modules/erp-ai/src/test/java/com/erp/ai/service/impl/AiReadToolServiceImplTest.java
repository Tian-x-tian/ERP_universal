package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.model.AiToolDefinition;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.client.internal.InternalBusinessClient;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.platform.contract.model.PlatformAiDataSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

/**
 * AI 只读工具服务单元测试。
 */
class AiReadToolServiceImplTest {

    private InternalWorkflowClient workflowClient;
    private InternalSystemClient systemClient;
    private InternalBusinessClient businessClient;
    private AiTenantConfigService tenantConfigService;
    private SecurityUserResolver securityUserResolver;
    private ErpAiProperties properties;
    private AiReadToolServiceImpl readToolService;

    @BeforeEach
    void setUp() {
        workflowClient = Mockito.mock(InternalWorkflowClient.class);
        systemClient = Mockito.mock(InternalSystemClient.class);
        businessClient = Mockito.mock(InternalBusinessClient.class);
        tenantConfigService = Mockito.mock(AiTenantConfigService.class);
        securityUserResolver = Mockito.mock(SecurityUserResolver.class);
        properties = new ErpAiProperties();
        readToolService = new AiReadToolServiceImpl(workflowClient, systemClient, businessClient,
                tenantConfigService, securityUserResolver, properties);
    }

    /**
     * 验证只有拥有对应权限的工具才会暴露给模型。
     */
    @Test
    void shouldOnlyExposeToolsWithPermission() {
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(false);
        Mockito.when(tenantConfigService.hasPermission("workflow:todo:list")).thenReturn(true);

        List<AiToolDefinition> tools = readToolService.buildAvailableTools();

        List<String> toolNames = tools.stream().map(AiToolDefinition::getName).toList();
        Assertions.assertTrue(toolNames.contains("query_todo_backlog"));
        Assertions.assertTrue(toolNames.contains("query_todo_aging"));
        Assertions.assertFalse(toolNames.contains("query_stock_overview"));
        Assertions.assertFalse(toolNames.contains("query_hr_headcount"));
    }

    /**
     * 验证关闭只读工具开关后不再暴露任何工具。
     */
    @Test
    void shouldExposeNothingWhenReadToolsDisabled() {
        properties.setReadToolsEnabled(false);
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(true);

        Assertions.assertTrue(readToolService.buildAvailableTools().isEmpty());
    }

    /**
     * 验证执行阶段会重新判权，避免模型拿着过期工具清单越权取数。
     */
    @Test
    void shouldRejectExecutionWithoutPermission() {
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(false);

        AiReadToolResult result = readToolService.execute("query_todo_backlog", Map.of());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertTrue(result.getModelText().contains("没有查看该数据的权限"));
        Mockito.verifyNoInteractions(workflowClient);
    }

    /**
     * 验证 userId 由服务端注入，模型伪造的 userId 会被丢弃。
     *
     * <p>这是只读工具最关键的一条安全边界：模型可以决定「查什么」，但不能决定「以谁的身份查」。</p>
     */
    @Test
    void shouldIgnoreModelSuppliedUserId() {
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(true);
        Mockito.when(securityUserResolver.getCurrentUserId()).thenReturn(1001L);
        Mockito.when(workflowClient.queryAiDataset(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(sampleDataSet());

        readToolService.execute("query_todo_backlog", Map.of("userId", 999L, "scope", "mine"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(workflowClient).queryAiDataset(Mockito.eq("todo_backlog"), paramsCaptor.capture());
        Assertions.assertEquals(1001L, paramsCaptor.getValue().get("userId"));
        Assertions.assertEquals("mine", paramsCaptor.getValue().get("scope"));
    }

    /**
     * 验证未声明的参数会被剔除，不会原样透传给业务服务。
     */
    @Test
    void shouldDropUndeclaredParameters() {
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(true);
        Mockito.when(businessClient.queryAiDataset(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(sampleDataSet());

        readToolService.execute("query_stock_overview", Map.of("limit", 5, "warehouseId", 88));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
        Mockito.verify(businessClient).queryAiDataset(Mockito.eq("stock_overview"), paramsCaptor.capture());
        Assertions.assertEquals(5, paramsCaptor.getValue().get("limit"));
        Assertions.assertFalse(paramsCaptor.getValue().containsKey("warehouseId"));
    }

    /**
     * 验证数据集会同时产出模型可读文本与前端可渲染区块。
     */
    @Test
    void shouldRenderDataSetForModelAndPanel() {
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(true);
        Mockito.when(securityUserResolver.getCurrentUserId()).thenReturn(1001L);
        Mockito.when(workflowClient.queryAiDataset(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(sampleDataSet());

        AiReadToolResult result = readToolService.execute("query_todo_backlog", Map.of());

        Assertions.assertTrue(result.isSuccess());
        Assertions.assertNotNull(result.getBlock());
        Assertions.assertEquals("query_todo_backlog", result.getBlock().getToolName());

        String modelText = result.getModelText();
        Assertions.assertTrue(modelText.contains("待办积压分布"));
        Assertions.assertTrue(modelText.contains("结论: 共 5 条未办结待办"));
        Assertions.assertTrue(modelText.contains("待办总数=5条"));
        Assertions.assertTrue(modelText.contains("优先级|待办总数"));
        Assertions.assertTrue(modelText.contains("高|3"));
    }

    /**
     * 验证超过行数上限时会截断并给出提示。
     */
    @Test
    void shouldTruncateRowsBeyondLimit() {
        properties.setMaxDatasetRows(1);
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(true);
        Mockito.when(securityUserResolver.getCurrentUserId()).thenReturn(1001L);
        Mockito.when(workflowClient.queryAiDataset(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(sampleDataSet());

        AiReadToolResult result = readToolService.execute("query_todo_backlog", Map.of());

        Assertions.assertEquals(1, result.getBlock().getDataSet().getRows().size());
        Assertions.assertTrue(result.getBlock().getDataSet().isTruncated());
        Assertions.assertTrue(result.getModelText().contains("已按上限截断"));
    }

    /**
     * 验证下游异常被收敛成失败结果，不会把堆栈抛给模型。
     */
    @Test
    void shouldConvertDownstreamFailureIntoToolFailure() {
        Mockito.when(tenantConfigService.hasPermission(Mockito.anyString())).thenReturn(true);
        Mockito.when(securityUserResolver.getCurrentUserId()).thenReturn(1001L);
        Mockito.when(workflowClient.queryAiDataset(Mockito.anyString(), Mockito.anyMap()))
                .thenThrow(new IllegalStateException("connection refused"));

        AiReadToolResult result = readToolService.execute("query_todo_backlog", Map.of());

        Assertions.assertFalse(result.isSuccess());
        Assertions.assertNull(result.getBlock());
        Assertions.assertTrue(result.getModelText().contains("数据服务暂时不可用"));
    }

    /**
     * 验证只读工具名判定。
     */
    @Test
    void shouldRecognizeReadToolNames() {
        Assertions.assertTrue(readToolService.isReadTool("query_todo_backlog"));
        Assertions.assertFalse(readToolService.isReadTool("todo_finish"));
        Assertions.assertFalse(readToolService.isReadTool(null));
    }

    /**
     * 构造用于断言的样例数据集。
     *
     * @return 数据集
     */
    private PlatformAiDataSet sampleDataSet() {
        PlatformAiDataSet dataSet = new PlatformAiDataSet();
        dataSet.setKey("todo_backlog");
        dataSet.setTitle("我的待办积压分布");
        dataSet.setSummary("共 5 条未办结待办，其中高优先级 3 条、已超期 1 条。");
        dataSet.addColumn("priorityLabel", "优先级", "text")
                .addColumn("totalCount", "待办总数", "number");
        dataSet.addRow("高", 3L);
        dataSet.addRow("中", 2L);
        dataSet.addMetric("total", "待办总数", "5", "条", "normal");
        return dataSet;
    }
}
