package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiActionHandleResult;
import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiModelCompletion;
import com.erp.ai.model.AiPendingAction;
import com.erp.ai.model.AiPromptContext;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.model.AiRoleProfileVO;
import com.erp.ai.model.AiRuntimeConfig;
import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.model.AiTokenUsage;
import com.erp.ai.model.AiToolCall;
import com.erp.ai.model.AiToolDefinition;
import com.erp.ai.service.AiActionService;
import com.erp.ai.service.AiContextService;
import com.erp.ai.service.AiModelClient;
import com.erp.ai.service.AiReadToolService;
import com.erp.ai.service.AiRoleGuideService;
import com.erp.ai.service.AiSessionService;
import com.erp.ai.service.AiStreamListener;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.erp.platform.contract.model.PlatformAiDataSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * AI 对话 Agent 循环单元测试。
 */
class AiChatServiceImplAgentLoopTest {

    private AiContextService contextService;
    private AiModelClient modelClient;
    private AiActionService actionService;
    private AiTenantConfigService tenantConfigService;
    private AiRoleGuideService roleGuideService;
    private AiReadToolService readToolService;
    private AiSessionService sessionService;
    private ErpAiProperties properties;
    private AiChatServiceImpl chatService;
    private RecordingListener listener;

    @BeforeEach
    void setUp() {
        contextService = Mockito.mock(AiContextService.class);
        modelClient = Mockito.mock(AiModelClient.class);
        actionService = Mockito.mock(AiActionService.class);
        tenantConfigService = Mockito.mock(AiTenantConfigService.class);
        roleGuideService = Mockito.mock(AiRoleGuideService.class);
        readToolService = Mockito.mock(AiReadToolService.class);
        sessionService = Mockito.mock(AiSessionService.class);
        properties = new ErpAiProperties();
        chatService = new AiChatServiceImpl(contextService, modelClient, actionService, tenantConfigService,
                roleGuideService, readToolService, sessionService, properties, new ObjectMapper());
        listener = new RecordingListener();

        AiRuntimeConfig runtimeConfig = new AiRuntimeConfig();
        runtimeConfig.setEnabled(true);
        runtimeConfig.setModel("test-model");
        runtimeConfig.setMaxHistoryTurns(12);
        Mockito.when(tenantConfigService.resolveRuntimeConfig()).thenReturn(runtimeConfig);
        Mockito.when(contextService.buildPromptContext(Mockito.any())).thenReturn(new AiPromptContext());
        Mockito.when(roleGuideService.buildCurrentRoleProfile()).thenReturn(new AiRoleProfileVO());
        Mockito.when(actionService.listAvailableActions()).thenReturn(Collections.emptyList());
        Mockito.when(actionService.buildAvailableTools()).thenReturn(Collections.emptyList());
        Mockito.when(readToolService.buildAvailableTools()).thenReturn(List.of(toolDefinition("query_todo_backlog")));
        // 必须显式指定参数类型：invocation.getArgument(0) 的泛型会被 String.valueOf 的 char[] 重载推断错
        Mockito.when(readToolService.isReadTool(Mockito.anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0, String.class).startsWith("query_"));
        Mockito.when(sessionService.recordUserMessage(Mockito.any(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(77L);
    }

    /**
     * 验证模型调用只读工具后，工具结果会回灌模型并最终收敛到文本回复。
     */
    @Test
    void shouldFeedToolResultBackAndFinishWithText() throws Exception {
        Mockito.when(readToolService.execute(Mockito.eq("query_todo_backlog"), Mockito.anyMap()))
                .thenReturn(AiReadToolResult.success("query_todo_backlog", "[数据集] 待办积压\n结论: 共 5 条",
                        new AiStructuredBlock("query_todo_backlog", sampleDataSet())));
        Mockito.when(modelClient.completeChat(Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(toolCallCompletion("call-1", "query_todo_backlog", "{}"))
                .thenReturn(textCompletion("你当前积压 5 条待办，建议先处理高优先级。"));

        chatService.streamChat(request("我的待办情况怎么样"), listener);

        Assertions.assertNull(listener.errorMessage);
        Assertions.assertEquals(1, listener.blocks.size());
        Assertions.assertEquals("query_todo_backlog", listener.blocks.get(0).getToolName());
        Assertions.assertEquals(List.of("query_todo_backlog"), listener.startedTools);
        Assertions.assertEquals("你当前积压 5 条待办，建议先处理高优先级。", listener.doneContent);
        Assertions.assertEquals(77L, listener.sessionId);

        // 第二轮请求必须携带 assistant(tool_calls) + tool 结果两条消息，否则模型无从得知取到了什么
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiChatMessage>> messagesCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(modelClient, Mockito.times(2))
                .completeChat(Mockito.anyString(), messagesCaptor.capture(), Mockito.anyList());
        List<AiChatMessage> secondRoundMessages = messagesCaptor.getAllValues().get(1);
        AiChatMessage toolMessage = secondRoundMessages.get(secondRoundMessages.size() - 1);
        Assertions.assertEquals("tool", toolMessage.getRole());
        Assertions.assertEquals("call-1", toolMessage.getToolCallId());
        Assertions.assertTrue(toolMessage.getContent().contains("共 5 条"));

        AiChatMessage assistantToolCallMessage = secondRoundMessages.get(secondRoundMessages.size() - 2);
        Assertions.assertEquals("assistant", assistantToolCallMessage.getRole());
        Assertions.assertEquals(1, assistantToolCallMessage.getToolCalls().size());
    }

    /**
     * 验证达到轮次上限后最后一轮不再带工具，强制模型直接作答。
     */
    @Test
    void shouldWithholdToolsOnFinalRound() throws Exception {
        properties.setMaxToolRounds(1);
        Mockito.when(readToolService.execute(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(AiReadToolResult.success("query_todo_backlog", "[数据集] 待办积压", null));
        Mockito.when(modelClient.completeChat(Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(toolCallCompletion("call-1", "query_todo_backlog", "{}"))
                .thenReturn(textCompletion("已完成分析。"));

        chatService.streamChat(request("看看待办"), listener);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiToolDefinition>> toolsCaptor = ArgumentCaptor.forClass(List.class);
        Mockito.verify(modelClient, Mockito.times(2))
                .completeChat(Mockito.anyString(), Mockito.anyList(), toolsCaptor.capture());
        Assertions.assertFalse(toolsCaptor.getAllValues().get(0).isEmpty());
        Assertions.assertTrue(toolsCaptor.getAllValues().get(1).isEmpty());
        Assertions.assertEquals("已完成分析。", listener.doneContent);
    }

    /**
     * 验证写动作会立即终止循环并产出确认卡片，不参与后续推理。
     */
    @Test
    void shouldTerminateLoopOnWriteAction() throws Exception {
        AiPendingAction pendingAction = new AiPendingAction();
        pendingAction.setActionKey("todo_finish");
        pendingAction.setActionLabel("办结待办");
        pendingAction.setRiskLevel("high");
        pendingAction.setTargetLabel("采购审批 / WF001");
        pendingAction.setConfirmationToken("token-1");

        AiActionHandleResult handleResult = new AiActionHandleResult();
        handleResult.setAssistantMessage("这是高风险操作，请先确认。");
        handleResult.setPendingAction(pendingAction);
        Mockito.when(actionService.handleToolCall(Mockito.any(), Mockito.any())).thenReturn(handleResult);
        Mockito.when(modelClient.completeChat(Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(toolCallCompletion("call-1", "todo_finish", "{\"todoId\":1}"));

        chatService.streamChat(request("帮我办结第一条待办"), listener);

        Assertions.assertNotNull(listener.pendingAction);
        Assertions.assertEquals("todo_finish", listener.pendingAction.getActionKey());
        Assertions.assertEquals("这是高风险操作，请先确认。", listener.doneContent);
        Mockito.verify(modelClient, Mockito.times(1))
                .completeChat(Mockito.anyString(), Mockito.anyList(), Mockito.anyList());
        Mockito.verify(readToolService, Mockito.never()).execute(Mockito.anyString(), Mockito.anyMap());
    }

    /**
     * 验证 token 用量与工具使用情况会跨轮累计并写入审计。
     */
    @Test
    void shouldAccumulateTelemetryAcrossRounds() throws Exception {
        Mockito.when(readToolService.execute(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(AiReadToolResult.success("query_todo_backlog", "[数据集] 待办积压", null));
        Mockito.when(modelClient.completeChat(Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(withUsage(toolCallCompletion("call-1", "query_todo_backlog", "{}"), 100, 20))
                .thenReturn(withUsage(textCompletion("分析完成。"), 150, 30));

        chatService.streamChat(request("看看待办"), listener);

        ArgumentCaptor<PlatformAiAuditCreateRequest> auditCaptor =
                ArgumentCaptor.forClass(PlatformAiAuditCreateRequest.class);
        Mockito.verify(tenantConfigService).recordAudit(auditCaptor.capture());
        PlatformAiAuditCreateRequest audit = auditCaptor.getValue();

        Assertions.assertEquals(250, audit.getPromptTokens());
        Assertions.assertEquals(50, audit.getCompletionTokens());
        Assertions.assertEquals(300, audit.getTotalTokens());
        Assertions.assertEquals("test-model", audit.getModel());
        Assertions.assertEquals("query_todo_backlog", audit.getToolKeys());
        Assertions.assertEquals(1, audit.getToolRounds());
        Assertions.assertEquals(77L, audit.getSessionId());
        Assertions.assertTrue(audit.getSuccess());
    }

    /**
     * 验证单轮内并行工具调用数受配置上限约束。
     */
    @Test
    void shouldCapParallelToolCallsPerRound() throws Exception {
        properties.setMaxToolCallsPerRound(2);
        Mockito.when(readToolService.execute(Mockito.anyString(), Mockito.anyMap()))
                .thenReturn(AiReadToolResult.success("query", "[数据集] X", null));

        AiModelCompletion multiCall = new AiModelCompletion();
        multiCall.setToolCalls(new ArrayList<>(List.of(
                toolCall("c1", "query_todo_backlog", "{}"),
                toolCall("c2", "query_todo_aging", "{}"),
                toolCall("c3", "query_notice_overview", "{}"))));
        Mockito.when(modelClient.completeChat(Mockito.anyString(), Mockito.anyList(), Mockito.anyList()))
                .thenReturn(multiCall)
                .thenReturn(textCompletion("好了。"));

        chatService.streamChat(request("给我一份概览"), listener);

        Assertions.assertEquals(2, listener.startedTools.size());
        Mockito.verify(readToolService, Mockito.times(2)).execute(Mockito.anyString(), Mockito.anyMap());
    }

    private AiChatRequest request(String content) {
        AiChatRequest request = new AiChatRequest();
        request.setMessages(new ArrayList<>(List.of(new AiChatMessage("user", content))));
        return request;
    }

    private AiToolDefinition toolDefinition(String name) {
        AiToolDefinition definition = new AiToolDefinition();
        definition.setName(name);
        definition.setDescription(name);
        return definition;
    }

    private AiToolCall toolCall(String id, String name, String argumentsJson) {
        AiToolCall toolCall = new AiToolCall();
        toolCall.setId(id);
        toolCall.setName(name);
        toolCall.setArgumentsJson(argumentsJson);
        return toolCall;
    }

    private AiModelCompletion toolCallCompletion(String id, String name, String argumentsJson) {
        AiModelCompletion completion = new AiModelCompletion();
        completion.setToolCalls(new ArrayList<>(List.of(toolCall(id, name, argumentsJson))));
        return completion;
    }

    private AiModelCompletion textCompletion(String content) {
        AiModelCompletion completion = new AiModelCompletion();
        completion.setContent(content);
        return completion;
    }

    private AiModelCompletion withUsage(AiModelCompletion completion, int promptTokens, int completionTokens) {
        AiTokenUsage usage = new AiTokenUsage();
        usage.setPromptTokens(promptTokens);
        usage.setCompletionTokens(completionTokens);
        usage.setTotalTokens(promptTokens + completionTokens);
        completion.setUsage(usage);
        return completion;
    }

    private PlatformAiDataSet sampleDataSet() {
        PlatformAiDataSet dataSet = new PlatformAiDataSet();
        dataSet.setKey("todo_backlog");
        dataSet.setTitle("待办积压");
        return dataSet;
    }

    /**
     * 记录 SSE 事件的测试监听器。
     */
    private static final class RecordingListener implements AiStreamListener {
        private final List<AiStructuredBlock> blocks = new ArrayList<>();
        private final List<String> startedTools = new ArrayList<>();
        private final StringBuilder deltas = new StringBuilder();
        private Long sessionId;
        private String doneContent;
        private String errorMessage;
        private AiPendingAction pendingAction;

        @Override
        public void onReady(String model) {
            // 无需记录
        }

        @Override
        public void onSession(Long sessionId) {
            this.sessionId = sessionId;
        }

        @Override
        public void onToolStart(String toolName, String toolLabel) {
            startedTools.add(toolName);
        }

        @Override
        public void onBlock(AiStructuredBlock block) {
            blocks.add(block);
        }

        @Override
        public void onDelta(String delta) {
            deltas.append(delta);
        }

        @Override
        public void onDone(String content) {
            this.doneContent = content;
        }

        @Override
        public void onActionRequired(AiPendingAction pendingAction) {
            this.pendingAction = pendingAction;
        }

        @Override
        public void onError(String message) {
            this.errorMessage = message;
        }
    }
}
