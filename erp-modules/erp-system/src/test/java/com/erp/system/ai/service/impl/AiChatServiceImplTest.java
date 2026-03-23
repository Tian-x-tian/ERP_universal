package com.erp.system.ai.service.impl;

import com.erp.system.ai.config.ErpAiProperties;
import com.erp.system.ai.model.AiActionHandleResult;
import com.erp.system.ai.model.AiChatMessage;
import com.erp.system.ai.model.AiChatRequest;
import com.erp.system.ai.model.AiModelCompletion;
import com.erp.system.ai.model.AiPageContext;
import com.erp.system.ai.model.AiPendingAction;
import com.erp.system.ai.model.AiPromptContext;
import com.erp.system.ai.model.AiToolCall;
import com.erp.system.ai.model.AiToolDefinition;
import com.erp.system.ai.service.AiActionService;
import com.erp.system.ai.service.AiContextService;
import com.erp.system.ai.service.AiModelClient;
import com.erp.system.ai.service.AiStreamListener;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * AI 对话服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AiChatServiceImplTest {

    @Mock
    private AiContextService aiContextService;

    @Mock
    private AiModelClient aiModelClient;

    @Mock
    private AiActionService aiActionService;

    @Mock
    private AiStreamListener aiStreamListener;

    /**
     * 验证对话服务会过滤非法角色、截断历史并按流式文本回调。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldStreamChatWithNormalizedConversation() throws Exception {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setModel("gpt-5.1");
        properties.setMaxHistoryTurns(2);
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContext());
        when(aiActionService.listAvailableActions()).thenReturn(Collections.emptyList());
        when(aiActionService.buildAvailableTools()).thenReturn(Collections.emptyList());

        AiModelCompletion completion = new AiModelCompletion();
        completion.setContent("分析结果");
        when(aiModelClient.completeChat(anyList(), anyList())).thenReturn(completion);

        aiChatService.streamChat(buildRequest(), aiStreamListener);

        verify(aiStreamListener).onReady("gpt-5.1");
        verify(aiStreamListener).onDelta("分析结果");
        verify(aiStreamListener).onDone("分析结果");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<AiChatMessage>> captor = ArgumentCaptor.forClass(List.class);
        verify(aiModelClient).completeChat(captor.capture(), anyList());
        List<AiChatMessage> actualMessages = captor.getValue();
        Assertions.assertEquals("system", actualMessages.get(0).getRole());
        Assertions.assertEquals(5, actualMessages.size());
        Assertions.assertEquals("assistant", actualMessages.get(1).getRole());
        Assertions.assertEquals("最后一条用户消息", actualMessages.get(4).getContent());
    }

    /**
     * 验证高风险动作会输出确认卡片事件。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldEmitActionRequiredWhenToolCallNeedsConfirmation() throws Exception {
        ErpAiProperties properties = new ErpAiProperties();
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContext());
        when(aiActionService.listAvailableActions()).thenReturn(Collections.emptyList());
        when(aiActionService.buildAvailableTools()).thenReturn(Collections.emptyList());

        AiToolCall toolCall = new AiToolCall();
        toolCall.setName("todo_finish");
        toolCall.setArgumentsJson("{\"todoId\":1}");
        AiModelCompletion completion = new AiModelCompletion();
        completion.setToolCalls(Collections.singletonList(toolCall));
        when(aiModelClient.completeChat(anyList(), anyList())).thenReturn(completion);

        AiPendingAction pendingAction = new AiPendingAction();
        pendingAction.setActionKey("todo_finish");
        pendingAction.setActionLabel("办结待办");
        pendingAction.setRiskLevel("high");
        pendingAction.setTargetLabel("采购审批 / WF20260322001 / 部门审批");
        pendingAction.setSummary("办结待办");
        pendingAction.setConfirmationToken("token");
        AiActionHandleResult handleResult = new AiActionHandleResult();
        handleResult.setAssistantMessage("我已定位到该待办，请先确认。");
        handleResult.setPendingAction(pendingAction);
        when(aiActionService.handleToolCall(any(), any())).thenReturn(handleResult);

        aiChatService.streamChat(buildMinimalRequest(), aiStreamListener);

        verify(aiStreamListener).onReady("gpt-5.1");
        verify(aiStreamListener).onActionRequired(pendingAction);
        verify(aiStreamListener).onDone("我已定位到该待办，请先确认。");
    }

    /**
     * 验证未启用 AI 时不会继续调用模型服务。
     */
    @Test
    void shouldReturnErrorWhenAiDisabled() throws Exception {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setEnabled(false);
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);

        aiChatService.streamChat(buildRequest(), aiStreamListener);

        verify(aiStreamListener).onError("AI 功能未启用");
        verifyNoInteractions(aiModelClient);
    }

    /**
     * 验证“列出未读消息”会直接使用 ERP 上下文生成回复。
     */
    @Test
    void shouldReplyUnreadNoticeSummaryWithoutCallingModel() {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setModel("gpt-5.1");
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContextWithTodoAndNotice());

        AiChatRequest request = buildMinimalRequest();
        request.setMessages(Collections.singletonList(new AiChatMessage("user", "列出未读消息")));

        aiChatService.streamChat(request, aiStreamListener);

        verify(aiStreamListener).onDone(org.mockito.ArgumentMatchers.contains("未读消息"));
        verifyNoInteractions(aiModelClient);
    }

    /**
     * 验证“我今天优先做什么”会直接使用 ERP 上下文生成建议。
     */
    @Test
    void shouldReplyTodayPriorityWithoutCallingModel() {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setModel("gpt-5.1");
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContextWithTodoAndNotice());

        AiChatRequest request = buildMinimalRequest();
        request.setMessages(Collections.singletonList(new AiChatMessage("user", "我今天优先做什么")));

        aiChatService.streamChat(request, aiStreamListener);

        verify(aiStreamListener).onDone(org.mockito.ArgumentMatchers.contains("今天建议你优先处理"));
        verifyNoInteractions(aiModelClient);
    }

    /**
     * 验证当带工具补全失败时，会自动降级为纯对话补全。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldFallbackToPlainCompletionWhenToolCompletionFails() throws Exception {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setModel("gpt-5.1");
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContext());
        when(aiActionService.listAvailableActions()).thenReturn(Collections.emptyList());
        AiToolDefinition toolDefinition = buildTool("notice_read_all");
        when(aiActionService.buildAvailableTools()).thenReturn(Collections.singletonList(toolDefinition));

        AiModelCompletion fallbackCompletion = new AiModelCompletion();
        fallbackCompletion.setContent("这是降级后的回复");
        when(aiModelClient.completeChat(anyList(), anyList()))
                .thenThrow(new java.io.IOException("Connection prematurely closed BEFORE response"))
                .thenReturn(fallbackCompletion);

        aiChatService.streamChat(buildMinimalRequest(), aiStreamListener);

        verify(aiStreamListener).onDelta("这是降级后的回复");
        verify(aiStreamListener).onDone("这是降级后的回复");
        verify(aiModelClient).completeChat(anyList(), eq(Collections.singletonList(toolDefinition)));
        verify(aiModelClient).completeChat(anyList(), eq(Collections.emptyList()));
        verify(aiStreamListener, never()).onError(any());
    }

    /**
     * 验证当普通补全没有有效文本时，会继续降级为纯流式回复。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldFallbackToStreamWhenCompletionHasNoContent() throws Exception {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setModel("gpt-5.1");
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContext());
        when(aiActionService.listAvailableActions()).thenReturn(Collections.emptyList());
        when(aiActionService.buildAvailableTools()).thenReturn(Collections.emptyList());
        when(aiModelClient.completeChat(anyList(), eq(Collections.emptyList()))).thenReturn(new AiModelCompletion());
        when(aiModelClient.streamChat(anyList(), any())).thenAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            java.util.function.Consumer<String> deltaConsumer = invocation.getArgument(1);
            deltaConsumer.accept("流式降级回复");
            return "流式降级回复";
        });

        aiChatService.streamChat(buildMinimalRequest(), aiStreamListener);

        verify(aiStreamListener).onDelta("流式降级回复");
        verify(aiStreamListener).onDone("流式降级回复");
        verify(aiStreamListener, never()).onError(any());
    }

    /**
     * 验证传输层英文异常会转换为面向用户的中文提示。
     *
     * @throws Exception 异常
     */
    @Test
    void shouldMapTransportErrorToFriendlyChineseMessage() throws Exception {
        ErpAiProperties properties = new ErpAiProperties();
        properties.setModel("gpt-5.1");
        AiChatServiceImpl aiChatService = new AiChatServiceImpl(aiContextService, aiModelClient, aiActionService, properties);
        when(aiContextService.buildPromptContext(any())).thenReturn(buildPromptContext());
        when(aiActionService.listAvailableActions()).thenReturn(Collections.emptyList());
        when(aiActionService.buildAvailableTools()).thenReturn(Collections.emptyList());
        when(aiModelClient.completeChat(anyList(), eq(Collections.emptyList())))
                .thenThrow(new java.io.IOException("Connection prematurely closed BEFORE response"));

        aiChatService.streamChat(buildMinimalRequest(), aiStreamListener);

        verify(aiStreamListener).onError("本地模型服务连接被中断，请稍后重试");
    }

    private AiChatRequest buildRequest() {
        AiChatRequest request = buildMinimalRequest();
        request.setMessages(Arrays.asList(
                new AiChatMessage("system", "外部系统提示"),
                new AiChatMessage("user", "第一条用户消息"),
                new AiChatMessage("assistant", "第一条助手回复"),
                new AiChatMessage("user", "第二条用户消息"),
                new AiChatMessage("assistant", "第二条助手回复"),
                new AiChatMessage("user", "最后一条用户消息")));
        return request;
    }

    private AiChatRequest buildMinimalRequest() {
        AiPageContext pageContext = new AiPageContext();
        pageContext.setPath("/home");
        pageContext.setTitle("首页");
        AiChatRequest request = new AiChatRequest();
        request.setPageContext(pageContext);
        request.setMessages(Collections.singletonList(new AiChatMessage("user", "你好")));
        return request;
    }

    private AiPromptContext buildPromptContext() {
        AiPromptContext promptContext = new AiPromptContext();
        AiPromptContext.CurrentUserContext currentUser = new AiPromptContext.CurrentUserContext();
        currentUser.setUserId(1L);
        currentUser.setUserName("admin");
        currentUser.setNickName("系统管理员");
        currentUser.setTenantId("000000");
        promptContext.setCurrentUser(currentUser);
        return promptContext;
    }

    private AiPromptContext buildPromptContextWithTodoAndNotice() {
        AiPromptContext promptContext = buildPromptContext();
        promptContext.setTodoContextAvailable(true);
        promptContext.setTodoCount(2);
        AiPromptContext.TodoSummary todoSummary = new AiPromptContext.TodoSummary();
        todoSummary.setProcessName("采购申请");
        todoSummary.setNodeName("部门审批");
        todoSummary.setBusinessNo("PO20260322001");
        todoSummary.setPriority("HIGH");
        todoSummary.setStatus("0");
        promptContext.setTodoList(Collections.singletonList(todoSummary));

        promptContext.setNoticeContextAvailable(true);
        promptContext.setUnreadNoticeCount(1);
        AiPromptContext.NoticeSummary noticeSummary = new AiPromptContext.NoticeSummary();
        noticeSummary.setTitle("流程催办");
        noticeSummary.setNoticeType("workflow");
        noticeSummary.setSource("system");
        noticeSummary.setBusinessNo("PO20260322001");
        noticeSummary.setStatus("0");
        promptContext.setNoticeList(Collections.singletonList(noticeSummary));
        return promptContext;
    }

    private AiToolDefinition buildTool(String name) {
        AiToolDefinition toolDefinition = new AiToolDefinition();
        toolDefinition.setName(name);
        return toolDefinition;
    }
}
