package com.erp.system.ai.service.impl;

import com.erp.system.ai.config.ErpAiProperties;
import com.erp.system.ai.model.AiActionConfirmRequest;
import com.erp.system.ai.model.AiActionDescriptor;
import com.erp.system.ai.model.AiActionHandleResult;
import com.erp.system.ai.model.AiActionResultVO;
import com.erp.system.ai.model.AiChatMessage;
import com.erp.system.ai.model.AiChatRequest;
import com.erp.system.ai.model.AiMetaVO;
import com.erp.system.ai.model.AiModelCompletion;
import com.erp.system.ai.model.AiPageContext;
import com.erp.system.ai.model.AiPendingAction;
import com.erp.system.ai.model.AiPromptContext;
import com.erp.system.ai.service.AiActionService;
import com.erp.system.ai.service.AiChatService;
import com.erp.system.ai.service.AiContextService;
import com.erp.system.ai.service.AiModelClient;
import com.erp.system.ai.service.AiStreamListener;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 对话服务实现。
 */
@Service
public class AiChatServiceImpl implements AiChatService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneId.systemDefault());
    private static final List<String> CAPABILITIES = Collections.unmodifiableList(Arrays.asList(
            "stream",
            "page_context",
            "todo_summary",
            "notice_summary",
            "permission_scoped_actions",
            "confirm_action"));

    private final AiContextService aiContextService;
    private final AiModelClient aiModelClient;
    private final AiActionService aiActionService;
    private final ErpAiProperties erpAiProperties;

    public AiChatServiceImpl(AiContextService aiContextService,
            AiModelClient aiModelClient,
            AiActionService aiActionService,
            ErpAiProperties erpAiProperties) {
        this.aiContextService = aiContextService;
        this.aiModelClient = aiModelClient;
        this.aiActionService = aiActionService;
        this.erpAiProperties = erpAiProperties;
    }

    /**
     * 查询 AI 元信息。
     *
     * @return AI 元信息
     */
    @Override
    public AiMetaVO getMeta() {
        AiMetaVO aiMeta = new AiMetaVO();
        aiMeta.setEnabled(erpAiProperties.isEnabled());
        aiMeta.setModel(erpAiProperties.getModel());
        aiMeta.setCapabilities(new ArrayList<>(CAPABILITIES));
        aiMeta.setActions(aiActionService.listAvailableActions());
        if (!erpAiProperties.isEnabled()) {
            aiMeta.setAvailable(false);
            aiMeta.setMessage("AI 功能未启用");
            return aiMeta;
        }
        boolean available = aiModelClient.isAvailable();
        aiMeta.setAvailable(available);
        aiMeta.setMessage(available ? "本地模型服务连接正常" : "当前无法连接本地模型服务");
        return aiMeta;
    }

    /**
     * 执行 AI 流式对话。
     *
     * @param request  对话请求
     * @param listener 流式监听器
     */
    @Override
    public void streamChat(AiChatRequest request, AiStreamListener listener) {
        if (listener == null) {
            return;
        }
        if (!erpAiProperties.isEnabled()) {
            listener.onError("AI 功能未启用");
            return;
        }
        listener.onReady(erpAiProperties.getModel());

        AiPromptContext promptContext;
        try {
            promptContext = aiContextService.buildPromptContext(request);
        } catch (Exception ex) {
            listener.onError(resolveFriendlyErrorMessage(ex, "当前无法装载 ERP 上下文，请稍后重试"));
            return;
        }

        List<AiChatMessage> conversationHistory = normalizeConversationHistory(request == null ? null : request.getMessages());
        if (conversationHistory.isEmpty()) {
            listener.onError("请输入对话内容后再试");
            return;
        }

        List<AiActionDescriptor> availableActions = aiActionService.listAvailableActions();
        List<AiChatMessage> modelMessages = new ArrayList<>();
        modelMessages.add(new AiChatMessage("system", buildSystemPrompt(promptContext, availableActions)));
        modelMessages.addAll(conversationHistory);

        try {
            AiModelCompletion completion = aiModelClient.completeChat(modelMessages, aiActionService.buildAvailableTools());
            if (completion != null && completion.getToolCalls() != null && !completion.getToolCalls().isEmpty()) {
                handleToolCall(promptContext, completion, listener);
                return;
            }
            String content = completion == null ? null : completion.getContent();
            if (!StringUtils.hasText(content)) {
                listener.onError("AI 未返回有效内容，请稍后重试");
                return;
            }
            emitText(content, listener);
            listener.onDone(content);
        } catch (Exception ex) {
            listener.onError(resolveFriendlyErrorMessage(ex, "AI 服务调用失败，请稍后重试"));
        }
    }

    /**
     * 确认执行高风险动作。
     *
     * @param request 确认请求
     * @return 执行结果
     */
    @Override
    public AiActionResultVO confirmAction(AiActionConfirmRequest request) {
        if (request == null || !StringUtils.hasText(request.getConfirmationToken())) {
            AiActionResultVO result = new AiActionResultVO();
            result.setSuccess(false);
            result.setMessage("确认票据不能为空");
            result.setAssistantMessage("确认票据不能为空，请重新发起操作。");
            return result;
        }
        return aiActionService.confirm(request.getConfirmationToken().trim());
    }

    /**
     * 处理模型返回的工具调用。
     *
     * @param promptContext 提示词上下文
     * @param completion    模型补全结果
     * @param listener      流式监听器
     */
    private void handleToolCall(AiPromptContext promptContext, AiModelCompletion completion, AiStreamListener listener) {
        AiActionHandleResult handleResult = aiActionService.handleToolCall(completion.getToolCalls().get(0), promptContext);
        String assistantMessage = resolveAssistantMessage(handleResult);
        if (!StringUtils.hasText(assistantMessage)) {
            listener.onError("AI 未返回有效内容，请稍后重试");
            return;
        }
        emitText(assistantMessage, listener);
        AiPendingAction pendingAction = handleResult.getPendingAction();
        if (pendingAction != null) {
            listener.onActionRequired(pendingAction);
        }
        listener.onDone(assistantMessage);
    }

    /**
     * 将文本分段推送到前端，保持流式体验。
     *
     * @param content  文本内容
     * @param listener 流式监听器
     */
    private void emitText(String content, AiStreamListener listener) {
        if (!StringUtils.hasText(content) || listener == null) {
            return;
        }
        int chunkSize = 28;
        for (int start = 0; start < content.length(); start += chunkSize) {
            listener.onDelta(content.substring(start, Math.min(content.length(), start + chunkSize)));
        }
    }

    /**
     * 解析动作返回的助手消息。
     *
     * @param handleResult 动作处理结果
     * @return 助手消息
     */
    private String resolveAssistantMessage(AiActionHandleResult handleResult) {
        if (handleResult == null) {
            return null;
        }
        return StringUtils.hasText(handleResult.getAssistantMessage()) ? handleResult.getAssistantMessage() : null;
    }

    /**
     * 规范化历史对话，过滤非法角色并截断超出上限的历史消息。
     *
     * @param rawMessages 原始消息列表
     * @return 规范化后的消息列表
     */
    private List<AiChatMessage> normalizeConversationHistory(List<AiChatMessage> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<AiChatMessage> normalizedMessages = new ArrayList<>();
        for (AiChatMessage rawMessage : rawMessages) {
            if (rawMessage == null || !StringUtils.hasText(rawMessage.getRole()) || !StringUtils.hasText(rawMessage.getContent())) {
                continue;
            }
            String normalizedRole = rawMessage.getRole().trim().toLowerCase(Locale.ROOT);
            if (!"user".equals(normalizedRole) && !"assistant".equals(normalizedRole)) {
                continue;
            }
            normalizedMessages.add(new AiChatMessage(normalizedRole, rawMessage.getContent().trim()));
        }
        if (normalizedMessages.isEmpty()) {
            return Collections.emptyList();
        }
        int maxMessages = Math.max(1, erpAiProperties.getMaxHistoryTurns()) * 2;
        if (normalizedMessages.size() <= maxMessages) {
            return normalizedMessages;
        }
        return new ArrayList<>(normalizedMessages.subList(normalizedMessages.size() - maxMessages, normalizedMessages.size()));
    }

    /**
     * 构造系统提示词。
     *
     * @param promptContext    提示词上下文
     * @param availableActions 可执行动作列表
     * @return 系统提示词
     */
    private String buildSystemPrompt(AiPromptContext promptContext, List<AiActionDescriptor> availableActions) {
        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("你是 ERP 系统内置 AI 助手。\n");
        promptBuilder.append("请严格遵守以下规则：\n");
        promptBuilder.append("1. 默认使用简体中文回复。\n");
        promptBuilder.append("2. 你可以做只读分析，也可以在权限范围内调用工具执行动作；除了工具列表中明确给出的动作，其他任何写操作都禁止执行。\n");
        promptBuilder.append("3. 未拿到工具执行成功结果之前，绝不能声称操作已完成。\n");
        promptBuilder.append("4. 命中多个候选目标时，必须先澄清，不允许猜测。\n");
        promptBuilder.append("5. 驳回动作如果用户没有提供驳回原因，必须先追问原因，不允许直接调用工具。\n");
        promptBuilder.append("6. 最多调用一个工具；如果只是回答问题，不要调用工具。\n");
        promptBuilder.append("7. 不要向用户暴露隐藏候选区里的内部 ID。\n");
        promptBuilder.append("8. 回答尽量直接、实用，可使用短列表，但不要输出 HTML 或 Markdown 表格。\n\n");
        appendActionContext(promptBuilder, availableActions);
        appendUserContext(promptBuilder, promptContext);
        appendPageContext(promptBuilder, promptContext == null ? null : promptContext.getPageContext());
        appendTodoContext(promptBuilder, promptContext);
        appendNoticeContext(promptBuilder, promptContext);
        appendHiddenCandidateContext(promptBuilder, promptContext, availableActions);
        return promptBuilder.toString();
    }

    /**
     * 追加当前可执行动作上下文。
     *
     * @param promptBuilder    提示词构建器
     * @param availableActions 可执行动作列表
     */
    private void appendActionContext(StringBuilder promptBuilder, List<AiActionDescriptor> availableActions) {
        promptBuilder.append("当前可执行动作：\n");
        if (availableActions == null || availableActions.isEmpty()) {
            promptBuilder.append("- 当前用户没有可执行动作，只能提供只读分析和建议\n\n");
            return;
        }
        for (AiActionDescriptor action : availableActions) {
            promptBuilder.append("- ")
                    .append(action.getLabel())
                    .append(" (")
                    .append(action.getKey())
                    .append("，risk=")
                    .append(action.getRiskLevel())
                    .append(")\n");
        }
        promptBuilder.append('\n');
    }

    /**
     * 追加当前登录用户上下文。
     *
     * @param promptBuilder 提示词构建器
     * @param promptContext 提示词上下文
     */
    private void appendUserContext(StringBuilder promptBuilder, AiPromptContext promptContext) {
        AiPromptContext.CurrentUserContext currentUser = promptContext == null ? null : promptContext.getCurrentUser();
        promptBuilder.append("当前登录用户：\n");
        if (currentUser == null) {
            promptBuilder.append("- 当前无法解析登录用户信息\n\n");
            return;
        }
        promptBuilder.append("- 用户ID：").append(nullToDash(currentUser.getUserId())).append('\n');
        promptBuilder.append("- 用户账号：").append(nullToDash(currentUser.getUserName())).append('\n');
        promptBuilder.append("- 用户昵称：").append(nullToDash(currentUser.getNickName())).append('\n');
        promptBuilder.append("- 租户编号：").append(nullToDash(currentUser.getTenantId())).append("\n\n");
    }

    /**
     * 追加当前页面上下文。
     *
     * @param promptBuilder 提示词构建器
     * @param pageContext   页面上下文
     */
    private void appendPageContext(StringBuilder promptBuilder, AiPageContext pageContext) {
        promptBuilder.append("当前页面：\n");
        promptBuilder.append("- 页面标题：").append(nullToDash(pageContext == null ? null : pageContext.getTitle())).append('\n');
        promptBuilder.append("- 页面路径：").append(nullToDash(pageContext == null ? null : pageContext.getPath())).append("\n\n");
    }

    /**
     * 追加待办摘要上下文。
     *
     * @param promptBuilder 提示词构建器
     * @param promptContext 提示词上下文
     */
    private void appendTodoContext(StringBuilder promptBuilder, AiPromptContext promptContext) {
        promptBuilder.append("待办摘要：\n");
        if (promptContext == null || !promptContext.isTodoContextAvailable()) {
            promptBuilder.append("- ").append(nullToDash(promptContext == null ? null : promptContext.getTodoContextMessage())).append("\n\n");
            return;
        }
        promptBuilder.append("- 开放待办数量：").append(promptContext.getTodoCount()).append('\n');
        List<AiPromptContext.TodoSummary> todoList = promptContext.getTodoList();
        if (todoList == null || todoList.isEmpty()) {
            promptBuilder.append("- 当前没有待处理待办\n\n");
            return;
        }
        for (int index = 0; index < todoList.size(); index++) {
            AiPromptContext.TodoSummary todoSummary = todoList.get(index);
            promptBuilder.append(index + 1)
                    .append(". 流程=")
                    .append(nullToDash(todoSummary.getProcessName()))
                    .append("，节点=")
                    .append(nullToDash(todoSummary.getNodeName()))
                    .append("，业务单号=")
                    .append(nullToDash(todoSummary.getBusinessNo()))
                    .append("，优先级=")
                    .append(resolveTodoPriorityLabel(todoSummary.getPriority()))
                    .append("，状态=")
                    .append(resolveTodoStatusLabel(todoSummary.getStatus()))
                    .append("，截止时间=")
                    .append(formatDate(todoSummary.getDueTime()))
                    .append('\n');
        }
        promptBuilder.append('\n');
    }

    /**
     * 追加消息摘要上下文。
     *
     * @param promptBuilder 提示词构建器
     * @param promptContext 提示词上下文
     */
    private void appendNoticeContext(StringBuilder promptBuilder, AiPromptContext promptContext) {
        promptBuilder.append("系统消息摘要：\n");
        if (promptContext == null || !promptContext.isNoticeContextAvailable()) {
            promptBuilder.append("- ").append(nullToDash(promptContext == null ? null : promptContext.getNoticeContextMessage())).append("\n\n");
            return;
        }
        promptBuilder.append("- 未读消息数量：").append(promptContext.getUnreadNoticeCount()).append('\n');
        List<AiPromptContext.NoticeSummary> noticeList = promptContext.getNoticeList();
        if (noticeList == null || noticeList.isEmpty()) {
            promptBuilder.append("- 当前没有可参考的系统消息\n\n");
            return;
        }
        for (int index = 0; index < noticeList.size(); index++) {
            AiPromptContext.NoticeSummary noticeSummary = noticeList.get(index);
            promptBuilder.append(index + 1)
                    .append(". 标题=")
                    .append(nullToDash(noticeSummary.getTitle()))
                    .append("，类型=")
                    .append(nullToDash(noticeSummary.getNoticeType()))
                    .append("，来源=")
                    .append(nullToDash(noticeSummary.getSource()))
                    .append("，业务单号=")
                    .append(nullToDash(noticeSummary.getBusinessNo()))
                    .append("，阅读状态=")
                    .append(resolveNoticeStatusLabel(noticeSummary.getStatus()))
                    .append("，送达状态=")
                    .append(resolveDeliveryStatusLabel(noticeSummary.getDeliveryStatus()))
                    .append("，创建时间=")
                    .append(formatDate(noticeSummary.getCreateTime()))
                    .append('\n');
        }
        promptBuilder.append('\n');
    }

    /**
     * 追加仅供工具调用使用的隐藏候选区。
     *
     * @param promptBuilder    提示词构建器
     * @param promptContext    提示词上下文
     * @param availableActions 可执行动作列表
     */
    private void appendHiddenCandidateContext(StringBuilder promptBuilder,
            AiPromptContext promptContext,
            List<AiActionDescriptor> availableActions) {
        if (promptContext == null || availableActions == null || availableActions.isEmpty()) {
            return;
        }
        Set<String> actionKeySet = availableActions.stream().map(AiActionDescriptor::getKey).collect(Collectors.toSet());
        promptBuilder.append("隐藏候选区（仅供工具调用，绝不能直接展示给用户）：\n");
        if (actionKeySet.contains("todo_claim") || actionKeySet.contains("todo_finish")
                || actionKeySet.contains("workflow_approve") || actionKeySet.contains("workflow_reject")) {
            List<AiPromptContext.TodoSummary> todoList = promptContext.getTodoList();
            if (todoList != null && !todoList.isEmpty()) {
                promptBuilder.append("- 待办候选：\n");
                for (int index = 0; index < todoList.size(); index++) {
                    AiPromptContext.TodoSummary item = todoList.get(index);
                    promptBuilder.append("  ")
                            .append(index + 1)
                            .append(") todoId=")
                            .append(nullToDash(item.getTodoId()))
                            .append(", taskId=")
                            .append(nullToDash(item.getTaskId()))
                            .append(", process=")
                            .append(nullToDash(item.getProcessName()))
                            .append(", businessNo=")
                            .append(nullToDash(item.getBusinessNo()))
                            .append(", node=")
                            .append(nullToDash(item.getNodeName()))
                            .append('\n');
                }
            }
        }
        if (actionKeySet.contains("notice_read") || actionKeySet.contains("notice_read_all")) {
            List<AiPromptContext.NoticeSummary> noticeList = promptContext.getNoticeList();
            if (noticeList != null && !noticeList.isEmpty()) {
                promptBuilder.append("- 消息候选：\n");
                for (int index = 0; index < noticeList.size(); index++) {
                    AiPromptContext.NoticeSummary item = noticeList.get(index);
                    promptBuilder.append("  ")
                            .append(index + 1)
                            .append(") noticeId=")
                            .append(nullToDash(item.getNoticeId()))
                            .append(", title=")
                            .append(nullToDash(item.getTitle()))
                            .append(", businessNo=")
                            .append(nullToDash(item.getBusinessNo()))
                            .append('\n');
                }
            }
        }
        if (actionKeySet.contains("workflow_definition_publish")) {
            List<AiPromptContext.WorkflowDefinitionCandidate> definitionCandidates = promptContext.getDefinitionCandidates();
            if (definitionCandidates != null && !definitionCandidates.isEmpty()) {
                promptBuilder.append("- 流程定义候选：\n");
                for (int index = 0; index < definitionCandidates.size(); index++) {
                    AiPromptContext.WorkflowDefinitionCandidate item = definitionCandidates.get(index);
                    promptBuilder.append("  ")
                            .append(index + 1)
                            .append(") definitionId=")
                            .append(nullToDash(item.getDefinitionId()))
                            .append(", processKey=")
                            .append(nullToDash(item.getProcessKey()))
                            .append(", processName=")
                            .append(nullToDash(item.getProcessName()))
                            .append(", version=")
                            .append(nullToDash(item.getVersion()))
                            .append(", status=")
                            .append(nullToDash(item.getStatus()))
                            .append('\n');
                }
            }
        }
        promptBuilder.append('\n');
    }

    private String resolveTodoStatusLabel(String status) {
        if ("0".equals(status)) {
            return "待处理";
        }
        if ("1".equals(status)) {
            return "处理中";
        }
        if ("2".equals(status)) {
            return "已完成";
        }
        return nullToDash(status);
    }

    private String resolveTodoPriorityLabel(String priority) {
        if ("H".equalsIgnoreCase(priority)) {
            return "高";
        }
        if ("M".equalsIgnoreCase(priority)) {
            return "中";
        }
        if ("L".equalsIgnoreCase(priority)) {
            return "低";
        }
        return nullToDash(priority);
    }

    private String resolveNoticeStatusLabel(String status) {
        if ("0".equals(status)) {
            return "未读";
        }
        if ("1".equals(status)) {
            return "已读";
        }
        return nullToDash(status);
    }

    private String resolveDeliveryStatusLabel(String deliveryStatus) {
        if ("0".equals(deliveryStatus)) {
            return "待发送";
        }
        if ("1".equals(deliveryStatus)) {
            return "发送中";
        }
        if ("2".equals(deliveryStatus)) {
            return "已送达";
        }
        if ("3".equals(deliveryStatus)) {
            return "发送失败";
        }
        return nullToDash(deliveryStatus);
    }

    private String formatDate(Date date) {
        return date == null ? "-" : DATE_TIME_FORMATTER.format(date.toInstant());
    }

    private String nullToDash(Object value) {
        if (value == null) {
            return "-";
        }
        String text = String.valueOf(value).trim();
        return StringUtils.hasText(text) ? text : "-";
    }

    private String resolveFriendlyErrorMessage(Exception ex, String defaultMessage) {
        if (ex == null || !StringUtils.hasText(ex.getMessage())) {
            return defaultMessage;
        }
        return ex.getMessage().trim();
    }
}
