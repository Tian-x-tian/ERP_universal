package com.erp.ai.service.impl;

import com.erp.ai.model.AiActionConfirmRequest;
import com.erp.ai.model.AiActionDescriptor;
import com.erp.ai.model.AiActionHandleResult;
import com.erp.ai.model.AiActionResultVO;
import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiMetaVO;
import com.erp.ai.model.AiModelCompletion;
import com.erp.ai.model.AiPageContext;
import com.erp.ai.model.AiPendingAction;
import com.erp.ai.model.AiPolicySummaryVO;
import com.erp.ai.model.AiPromptContext;
import com.erp.ai.model.AiRoleProfileVO;
import com.erp.ai.model.AiRuntimeConfig;
import com.erp.ai.model.AiToolDefinition;
import com.erp.ai.service.AiActionService;
import com.erp.ai.service.AiChatService;
import com.erp.ai.service.AiContextService;
import com.erp.ai.service.AiModelClient;
import com.erp.ai.service.AiRoleGuideService;
import com.erp.ai.service.AiStreamListener;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
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
import java.util.Map;
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
    private final AiTenantConfigService aiTenantConfigService;
    private final AiRoleGuideService aiRoleGuideService;

    public AiChatServiceImpl(AiContextService aiContextService,
            AiModelClient aiModelClient,
            AiActionService aiActionService,
            AiTenantConfigService aiTenantConfigService,
            AiRoleGuideService aiRoleGuideService) {
        this.aiContextService = aiContextService;
        this.aiModelClient = aiModelClient;
        this.aiActionService = aiActionService;
        this.aiTenantConfigService = aiTenantConfigService;
        this.aiRoleGuideService = aiRoleGuideService;
    }

    /**
     * 查询 AI 元信息。
     *
     * @return AI 元信息
     */
    @Override
    public AiMetaVO getMeta() {
        AiRuntimeConfig runtimeConfig = aiTenantConfigService.resolveRuntimeConfig();
        List<AiActionDescriptor> actions = aiActionService.listAvailableActions();
        AiRoleProfileVO roleProfile = aiRoleGuideService.buildCurrentRoleProfile();
        Map<String, List<String>> pageQuestionTemplates = aiRoleGuideService.buildPageQuestionTemplates(roleProfile);
        AiPolicySummaryVO policySummary = aiRoleGuideService.buildPolicySummary(actions);

        AiMetaVO aiMeta = new AiMetaVO();
        aiMeta.setEnabled(runtimeConfig.isEnabled());
        aiMeta.setModel(runtimeConfig.getModel());
        aiMeta.setCapabilities(new ArrayList<>(CAPABILITIES));
        aiMeta.setActions(actions);
        aiMeta.setTenantConfigVersion(runtimeConfig.getTenantConfigVersion());
        aiMeta.setRoleProfile(roleProfile);
        aiMeta.setPolicySummary(policySummary);
        aiMeta.setPageQuestionTemplates(pageQuestionTemplates);
        if (!runtimeConfig.isEnabled()) {
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
        long startTime = System.currentTimeMillis();
        AiRuntimeConfig runtimeConfig = aiTenantConfigService.resolveRuntimeConfig();
        if (!runtimeConfig.isEnabled()) {
            listener.onError("AI 功能未启用");
            return;
        }
        listener.onReady(runtimeConfig.getModel());

        AiPromptContext promptContext;
        try {
            promptContext = aiContextService.buildPromptContext(request);
        } catch (Exception ex) {
            List<AiChatMessage> rawMessages = request == null ? null : request.getMessages();
            String latestUserMessage = getLatestUserMessage(rawMessages);
            recordAudit(resolveQuestionType(latestUserMessage), "L1", null, false, false,
                    latestUserMessage, resolveFriendlyErrorMessage(ex, "当前无法装载 ERP 上下文，请稍后重试"),
                    System.currentTimeMillis() - startTime);
            listener.onError(resolveFriendlyErrorMessage(ex, "当前无法装载 ERP 上下文，请稍后重试"));
            return;
        }

        AiRoleProfileVO roleProfile = aiRoleGuideService.buildCurrentRoleProfile();
        List<AiChatMessage> conversationHistory = normalizeConversationHistory(
                request == null ? null : request.getMessages(),
                runtimeConfig.getMaxHistoryTurns());
        if (conversationHistory.isEmpty()) {
            listener.onError("请输入对话内容后再试");
            return;
        }
        String latestUserMessage = getLatestUserMessage(conversationHistory);
        String contextFallbackReply = buildContextFallbackReply(promptContext, conversationHistory);
        if (StringUtils.hasText(contextFallbackReply)) {
            emitText(contextFallbackReply, listener);
            listener.onDone(contextFallbackReply);
            recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                    false, true, latestUserMessage, contextFallbackReply, System.currentTimeMillis() - startTime);
            return;
        }

        List<AiActionDescriptor> availableActions = aiActionService.listAvailableActions();
        List<AiChatMessage> modelMessages = new ArrayList<>();
        modelMessages.add(new AiChatMessage("system",
                buildSystemPrompt(promptContext, availableActions, roleProfile, runtimeConfig.getPromptTemplate())));
        modelMessages.addAll(conversationHistory);
        List<AiToolDefinition> availableTools = aiActionService.buildAvailableTools();

        try {
            AiModelCompletion completion = requestCompletionWithFallback(runtimeConfig.getModel(), modelMessages, availableTools);
            if (completion != null && completion.getToolCalls() != null && !completion.getToolCalls().isEmpty()) {
                handleToolCall(promptContext, completion, listener);
                String toolCallActionKey = completion.getToolCalls().get(0).getName();
                recordAudit(resolveQuestionType(latestUserMessage), "L3", toolCallActionKey, false, true,
                        latestUserMessage, resolveToolCallResponse(toolCallActionKey),
                        System.currentTimeMillis() - startTime);
                return;
            }
            String content = completion == null ? null : completion.getContent();
            if (StringUtils.hasText(content)) {
                emitText(content, listener);
                listener.onDone(content);
                recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                        false, true, latestUserMessage, content, System.currentTimeMillis() - startTime);
                return;
            }
            String streamedFallbackContent = requestStreamingFallback(runtimeConfig.getModel(), modelMessages, listener);
            if (StringUtils.hasText(streamedFallbackContent)) {
                listener.onDone(streamedFallbackContent);
                recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                        false, true, latestUserMessage, streamedFallbackContent,
                        System.currentTimeMillis() - startTime);
                return;
            }
            recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                    false, false, latestUserMessage, "AI 未返回有效内容，请稍后重试",
                    System.currentTimeMillis() - startTime);
            listener.onError("AI 未返回有效内容，请稍后重试");
        } catch (Exception ex) {
            String errorMessage = resolveFriendlyErrorMessage(ex, "AI 服务调用失败，请稍后重试");
            recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                    false, false, latestUserMessage, errorMessage, System.currentTimeMillis() - startTime);
            listener.onError(errorMessage);
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
            recordAudit("action_confirm", "L3", null, true, false,
                    "confirmationToken=empty", result.getAssistantMessage(), 0L);
            return result;
        }
        long startTime = System.currentTimeMillis();
        AiActionResultVO result = aiActionService.confirm(request.getConfirmationToken().trim());
        recordAudit("action_confirm", "L3", result == null ? null : result.getActionKey(), true,
                result != null && result.isSuccess(), "confirmationToken=provided",
                result == null ? null : resolveAssistantMessageFromResult(result),
                System.currentTimeMillis() - startTime);
        return result;
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
     * 优先按“带工具”模式请求模型；若上游不稳定或返回空结果，则自动降级为纯对话补全。
     *
     * @param model 模型编号
     * @param modelMessages 对话消息
     * @param availableTools 可用工具列表
     * @return 模型补全结果
     * @throws Exception 模型调用异常
     */
    private AiModelCompletion requestCompletionWithFallback(String model,
            List<AiChatMessage> modelMessages,
            List<AiToolDefinition> availableTools) throws Exception {
        Exception firstFailure = null;
        if (availableTools != null && !availableTools.isEmpty()) {
            try {
                AiModelCompletion completion = aiModelClient.completeChat(model, modelMessages, availableTools);
                if (hasUsableCompletion(completion)) {
                    return completion;
                }
            } catch (Exception ex) {
                firstFailure = ex;
            }
        }

        try {
            AiModelCompletion completion = aiModelClient.completeChat(model, modelMessages, Collections.emptyList());
            if (hasUsableCompletion(completion) || firstFailure == null) {
                return completion;
            }
        } catch (Exception ex) {
            if (firstFailure != null) {
                ex.addSuppressed(firstFailure);
            }
            throw ex;
        }

        if (firstFailure != null) {
            throw firstFailure;
        }
        return new AiModelCompletion();
    }

    /**
     * 当普通补全未拿到有效内容时，再降级走一次纯流式回复。
     *
     * @param model 模型编号
     * @param modelMessages 对话消息
     * @param listener 流式监听器
     * @return 流式完整回复
     * @throws Exception 模型调用异常
     */
    private String requestStreamingFallback(String model, List<AiChatMessage> modelMessages, AiStreamListener listener) throws Exception {
        return aiModelClient.streamChat(model, modelMessages, delta -> {
            if (listener != null && StringUtils.hasText(delta)) {
                listener.onDelta(delta);
            }
        });
    }

    /**
     * 判断模型补全结果是否包含可直接消费的文本或工具调用。
     *
     * @param completion 模型补全结果
     * @return true 表示结果可用
     */
    private boolean hasUsableCompletion(AiModelCompletion completion) {
        if (completion == null) {
            return false;
        }
        if (StringUtils.hasText(completion.getContent())) {
            return true;
        }
        return completion.getToolCalls() != null && !completion.getToolCalls().isEmpty();
    }

    /**
     * 针对常见 ERP 工作台问题生成确定性兜底回复，避免模型偶发空响应时影响核心使用体验。
     *
     * @param promptContext 提示词上下文
     * @param conversationHistory 对话历史
     * @return 兜底回复；若当前问题不适用则返回 null
     */
    private String buildContextFallbackReply(AiPromptContext promptContext, List<AiChatMessage> conversationHistory) {
        String latestUserMessage = getLatestUserMessage(conversationHistory);
        if (!StringUtils.hasText(latestUserMessage)) {
            return null;
        }
        if (matchesUnreadNoticeIntent(latestUserMessage)) {
            return buildUnreadNoticeReply(promptContext);
        }
        if (matchesTodayPriorityIntent(latestUserMessage)) {
            return buildTodayPriorityReply(promptContext);
        }
        if (matchesTodoSummaryIntent(latestUserMessage)) {
            return buildTodoSummaryReply(promptContext);
        }
        return null;
    }

    /**
     * 获取最近一条用户消息文本。
     *
     * @param conversationHistory 对话历史
     * @return 用户消息文本
     */
    private String getLatestUserMessage(List<AiChatMessage> conversationHistory) {
        if (conversationHistory == null || conversationHistory.isEmpty()) {
            return null;
        }
        for (int index = conversationHistory.size() - 1; index >= 0; index--) {
            AiChatMessage message = conversationHistory.get(index);
            if (message != null
                    && "user".equalsIgnoreCase(message.getRole())
                    && StringUtils.hasText(message.getContent())) {
                return message.getContent().trim();
            }
        }
        return null;
    }

    /**
     * 判断是否为未读消息查询意图。
     *
     * @param message 用户消息
     * @return true 表示命中
     */
    private boolean matchesUnreadNoticeIntent(String message) {
        String normalizedMessage = normalizeIntentText(message);
        return containsAny(normalizedMessage, "未读消息", "未读通知", "列出未读", "查看未读", "看看未读");
    }

    /**
     * 判断是否为今日优先级问题。
     *
     * @param message 用户消息
     * @return true 表示命中
     */
    private boolean matchesTodayPriorityIntent(String message) {
        String normalizedMessage = normalizeIntentText(message);
        return containsAny(normalizedMessage, "今天优先", "今日优先", "今天先做什么", "我今天优先做什么", "今天做什么");
    }

    /**
     * 判断是否为待办汇总问题。
     *
     * @param message 用户消息
     * @return true 表示命中
     */
    private boolean matchesTodoSummaryIntent(String message) {
        String normalizedMessage = normalizeIntentText(message);
        if (!containsAny(normalizedMessage, "待办")) {
            return false;
        }
        if (containsAny(normalizedMessage, "办结", "签收", "审批", "通过", "驳回", "处理", "发布")) {
            return false;
        }
        return containsAny(normalizedMessage, "总结", "汇总", "概览", "有哪些", "列出", "查看", "看看", "我的待办");
    }

    /**
     * 生成待办汇总回复。
     *
     * @param promptContext 提示词上下文
     * @return 汇总回复
     */
    private String buildTodoSummaryReply(AiPromptContext promptContext) {
        if (promptContext == null || !promptContext.isTodoContextAvailable()) {
            return "当前暂时无法读取你的待办数据，请稍后重试。";
        }
        StringBuilder replyBuilder = new StringBuilder();
        replyBuilder.append("你当前共有 ").append(promptContext.getTodoCount()).append(" 条待办。");
        List<AiPromptContext.TodoSummary> todoList = promptContext.getTodoList();
        if (todoList == null || todoList.isEmpty()) {
            replyBuilder.append("\n\n当前没有待处理待办。");
            return replyBuilder.toString();
        }
        replyBuilder.append("\n\n优先关注前 ").append(Math.min(3, todoList.size())).append(" 条：");
        for (int index = 0; index < todoList.size() && index < 3; index++) {
            AiPromptContext.TodoSummary todoSummary = todoList.get(index);
            replyBuilder.append("\n")
                    .append(index + 1)
                    .append(". ")
                    .append(nullToDash(todoSummary.getProcessName()))
                    .append(" / ")
                    .append(nullToDash(todoSummary.getNodeName()))
                    .append(" / 单号 ")
                    .append(nullToDash(todoSummary.getBusinessNo()))
                    .append(" / 优先级 ")
                    .append(resolveTodoPriorityLabel(todoSummary.getPriority()))
                    .append(" / 状态 ")
                    .append(resolveTodoStatusLabel(todoSummary.getStatus()));
            if (todoSummary.getDueTime() != null) {
                replyBuilder.append(" / 截止 ").append(formatDate(todoSummary.getDueTime()));
            }
        }
        return replyBuilder.toString();
    }

    /**
     * 生成未读消息汇总回复。
     *
     * @param promptContext 提示词上下文
     * @return 汇总回复
     */
    private String buildUnreadNoticeReply(AiPromptContext promptContext) {
        if (promptContext == null || !promptContext.isNoticeContextAvailable()) {
            return "当前暂时无法读取你的系统消息，请稍后重试。";
        }
        StringBuilder replyBuilder = new StringBuilder();
        replyBuilder.append("你当前共有 ").append(promptContext.getUnreadNoticeCount()).append(" 条未读消息。");
        List<AiPromptContext.NoticeSummary> noticeList = promptContext.getNoticeList();
        if (noticeList == null || noticeList.isEmpty()) {
            replyBuilder.append("\n\n当前没有可展示的消息明细。");
            return replyBuilder.toString();
        }
        int displayedCount = 0;
        for (AiPromptContext.NoticeSummary noticeSummary : noticeList) {
            if (noticeSummary == null || !"0".equals(noticeSummary.getStatus())) {
                continue;
            }
            if (displayedCount == 0) {
                replyBuilder.append("\n\n未读消息明细：");
            }
            displayedCount++;
            replyBuilder.append("\n")
                    .append(displayedCount)
                    .append(". ")
                    .append(nullToDash(noticeSummary.getTitle()))
                    .append(" / 类型 ")
                    .append(nullToDash(noticeSummary.getNoticeType()))
                    .append(" / 来源 ")
                    .append(nullToDash(noticeSummary.getSource()))
                    .append(" / 单号 ")
                    .append(nullToDash(noticeSummary.getBusinessNo()));
            if (noticeSummary.getCreateTime() != null) {
                replyBuilder.append(" / 时间 ").append(formatDate(noticeSummary.getCreateTime()));
            }
            if (displayedCount >= 5) {
                break;
            }
        }
        if (displayedCount == 0) {
            replyBuilder.append("\n\n当前没有可展示的未读消息明细。");
        }
        return replyBuilder.toString();
    }

    /**
     * 生成今日优先级建议回复。
     *
     * @param promptContext 提示词上下文
     * @return 建议回复
     */
    private String buildTodayPriorityReply(AiPromptContext promptContext) {
        if (promptContext == null || !promptContext.isTodoContextAvailable()) {
            return "当前暂时无法读取你的待办数据，建议先打开工作台确认今日待办和未读消息。";
        }
        StringBuilder replyBuilder = new StringBuilder();
        List<AiPromptContext.TodoSummary> todoList = promptContext.getTodoList();
        if (todoList == null || todoList.isEmpty()) {
            replyBuilder.append("你当前没有待办任务。");
        } else {
            replyBuilder.append("今天建议你优先处理这几件事：");
            for (int index = 0; index < todoList.size() && index < 3; index++) {
                AiPromptContext.TodoSummary todoSummary = todoList.get(index);
                replyBuilder.append("\n")
                        .append(index + 1)
                        .append(". ")
                        .append(nullToDash(todoSummary.getProcessName()))
                        .append(" / ")
                        .append(nullToDash(todoSummary.getNodeName()))
                        .append(" / 单号 ")
                        .append(nullToDash(todoSummary.getBusinessNo()))
                        .append(" / 优先级 ")
                        .append(resolveTodoPriorityLabel(todoSummary.getPriority()));
                if (todoSummary.getDueTime() != null) {
                    replyBuilder.append(" / 截止 ").append(formatDate(todoSummary.getDueTime()));
                }
            }
        }
        if (promptContext.isNoticeContextAvailable() && promptContext.getUnreadNoticeCount() > 0) {
            replyBuilder.append("\n\n另外你还有 ")
                    .append(promptContext.getUnreadNoticeCount())
                    .append(" 条未读消息，建议处理完前置待办后顺手检查是否有新的催办或审批提醒。");
        }
        return replyBuilder.toString();
    }

    /**
     * 规范化意图匹配文本。
     *
     * @param message 原始消息
     * @return 规范化文本
     */
    private String normalizeIntentText(String message) {
        if (!StringUtils.hasText(message)) {
            return "";
        }
        return message.replace(" ", "")
                .replace("\n", "")
                .replace("\r", "")
                .trim()
                .toLowerCase(Locale.ROOT);
    }

    /**
     * 判断文本中是否包含任一候选关键字。
     *
     * @param source 文本
     * @param candidates 候选关键字
     * @return true 表示命中
     */
    private boolean containsAny(String source, String... candidates) {
        if (!StringUtils.hasText(source) || candidates == null || candidates.length == 0) {
            return false;
        }
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate) && source.contains(candidate.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    /**
     * 规范化历史对话，过滤非法角色并截断超出上限的历史消息。
     *
     * @param rawMessages 原始消息列表
     * @param maxHistoryTurns 历史轮次上限
     * @return 规范化后的消息列表
     */
    private List<AiChatMessage> normalizeConversationHistory(List<AiChatMessage> rawMessages, int maxHistoryTurns) {
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
        int maxMessages = Math.max(1, maxHistoryTurns <= 0 ? 12 : maxHistoryTurns) * 2;
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
     * @param roleProfile 角色画像
     * @param promptTemplate 租户提示词模板
     * @return 系统提示词
     */
    private String buildSystemPrompt(AiPromptContext promptContext,
            List<AiActionDescriptor> availableActions,
            AiRoleProfileVO roleProfile,
            String promptTemplate) {
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
        appendRoleGuideContext(promptBuilder, roleProfile);
        appendActionContext(promptBuilder, availableActions);
        appendUserContext(promptBuilder, promptContext);
        appendPageContext(promptBuilder, promptContext == null ? null : promptContext.getPageContext());
        appendTodoContext(promptBuilder, promptContext);
        appendNoticeContext(promptBuilder, promptContext);
        appendHiddenCandidateContext(promptBuilder, promptContext, availableActions);
        appendCustomPromptTemplate(promptBuilder, promptTemplate);
        return promptBuilder.toString();
    }

    /**
     * 追加角色学习引导上下文。
     *
     * @param promptBuilder 提示词构建器
     * @param roleProfile 角色画像
     */
    private void appendRoleGuideContext(StringBuilder promptBuilder, AiRoleProfileVO roleProfile) {
        promptBuilder.append("角色引导：\n");
        if (roleProfile == null) {
            promptBuilder.append("- 当前未识别到角色画像，按通用业务用户方式回答。\n\n");
            return;
        }
        promptBuilder.append("- AI角色标签：").append(nullToDash(roleProfile.getAiRoleTag())).append('\n');
        promptBuilder.append("- 角色名称：").append(nullToDash(roleProfile.getRoleLabel())).append('\n');
        appendPromptList(promptBuilder, "学习卡片", roleProfile.getLearningCards());
        appendPromptList(promptBuilder, "首周高频任务", roleProfile.getFirstWeekTasks());
        appendPromptList(promptBuilder, "常见错误提醒", roleProfile.getCommonMistakes());
        promptBuilder.append('\n');
    }

    /**
     * 追加租户自定义提示词模板。
     *
     * @param promptBuilder 提示词构建器
     * @param promptTemplate 提示词模板
     */
    private void appendCustomPromptTemplate(StringBuilder promptBuilder, String promptTemplate) {
        if (!StringUtils.hasText(promptTemplate)) {
            return;
        }
        promptBuilder.append("租户自定义提示词（高优先级）：\n");
        promptBuilder.append(promptTemplate.trim()).append("\n\n");
    }

    /**
     * 追加列表型提示词段落。
     *
     * @param promptBuilder 提示词构建器
     * @param title 段落标题
     * @param items 文本列表
     */
    private void appendPromptList(StringBuilder promptBuilder, String title, List<String> items) {
        if (items == null || items.isEmpty()) {
            return;
        }
        promptBuilder.append("- ").append(title).append("：");
        for (int index = 0; index < items.size(); index++) {
            String item = items.get(index);
            if (!StringUtils.hasText(item)) {
                continue;
            }
            if (index > 0) {
                promptBuilder.append("；");
            }
            promptBuilder.append(item.trim());
        }
        promptBuilder.append('\n');
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

    /**
     * 构造工具调用阶段的审计响应摘要。
     *
     * @param actionKey 动作编码
     * @return 响应摘要
     */
    private String resolveToolCallResponse(String actionKey) {
        if (!StringUtils.hasText(actionKey)) {
            return "模型已触发受控动作";
        }
        return "模型已触发受控动作: " + actionKey.trim();
    }

    /**
     * 构造动作结果审计摘要。
     *
     * @param result 动作执行结果
     * @return 摘要
     */
    private String resolveAssistantMessageFromResult(AiActionResultVO result) {
        if (result == null) {
            return null;
        }
        if (StringUtils.hasText(result.getAssistantMessage())) {
            return result.getAssistantMessage().trim();
        }
        if (StringUtils.hasText(result.getMessage())) {
            return result.getMessage().trim();
        }
        return result.isSuccess() ? "动作执行成功" : "动作执行失败";
    }

    /**
     * 根据用户提问推断问题类型。
     *
     * @param latestUserMessage 最近一条用户提问
     * @return 问题类型
     */
    private String resolveQuestionType(String latestUserMessage) {
        String normalized = normalizeIntentText(latestUserMessage);
        if (!StringUtils.hasText(normalized)) {
            return "general";
        }
        if (containsAny(normalized, "待办", "审批", "办结", "签收")) {
            return "todo_workflow";
        }
        if (containsAny(normalized, "消息", "通知", "未读")) {
            return "notice";
        }
        if (containsAny(normalized, "配置", "策略", "模型", "审计")) {
            return "config_policy";
        }
        return "general";
    }

    /**
     * 推断本次交互分级。
     *
     * @param latestUserMessage 最近用户提问
     * @param actionKey 动作编码
     * @return 交互分级
     */
    private String resolveInteractionLevel(String latestUserMessage, String actionKey) {
        if (StringUtils.hasText(actionKey)) {
            return "L3";
        }
        String normalized = normalizeIntentText(latestUserMessage);
        if (containsAny(normalized, "怎么", "哪里", "路径", "菜单", "下一步", "跳转", "入口")) {
            return "L2";
        }
        return "L1";
    }

    /**
     * 写入审计记录（失败不影响主流程）。
     *
     * @param questionType 问题类型
     * @param interactionLevel 交互分级
     * @param actionKey 动作编码
     * @param actionConfirmed 是否确认执行
     * @param success 是否成功
     * @param requestExcerpt 请求摘要
     * @param responseExcerpt 响应摘要
     * @param durationMs 耗时毫秒
     */
    private void recordAudit(String questionType,
            String interactionLevel,
            String actionKey,
            boolean actionConfirmed,
            boolean success,
            String requestExcerpt,
            String responseExcerpt,
            long durationMs) {
        PlatformAiAuditCreateRequest request = new PlatformAiAuditCreateRequest();
        request.setQuestionType(limitAuditExcerpt(questionType, 64));
        request.setInteractionLevel(limitAuditExcerpt(interactionLevel, 16));
        request.setActionKey(limitAuditExcerpt(actionKey, 64));
        request.setActionConfirmed(actionConfirmed);
        request.setSuccess(success);
        request.setPromptInjectionDetected(Boolean.FALSE);
        request.setSensitiveHit(Boolean.FALSE);
        request.setRequestExcerpt(limitAuditExcerpt(requestExcerpt, 500));
        request.setResponseExcerpt(limitAuditExcerpt(responseExcerpt, 500));
        request.setDurationMs(Math.max(0L, durationMs));
        aiTenantConfigService.recordAudit(request);
    }

    /**
     * 限制审计摘要长度。
     *
     * @param text 原始文本
     * @param maxLength 最大长度
     * @return 限制后的文本
     */
    private String limitAuditExcerpt(String text, int maxLength) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
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
        String message = ex.getMessage().trim();
        String normalizedMessage = message.toLowerCase(Locale.ROOT);
        if (normalizedMessage.contains("connection prematurely closed")
                || normalizedMessage.contains("prematurely closed before response")
                || normalizedMessage.contains("connection reset")
                || normalizedMessage.contains("unexpected end of file")) {
            return "本地模型服务连接被中断，请稍后重试";
        }
        if (normalizedMessage.contains("timeout")) {
            return "本地模型服务响应超时，请稍后重试";
        }
        if (normalizedMessage.contains("internal server error") || normalizedMessage.contains("status code: 500")) {
            return "本地模型服务暂时异常，请稍后重试";
        }
        if (normalizedMessage.startsWith("{") || normalizedMessage.startsWith("<!doctype html")) {
            return defaultMessage;
        }
        return message;
    }
}
