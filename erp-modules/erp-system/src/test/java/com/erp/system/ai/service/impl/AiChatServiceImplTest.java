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
}
