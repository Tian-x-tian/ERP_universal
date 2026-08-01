package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiActionConfirmRequest;
import com.erp.ai.model.AiActionDescriptor;
import com.erp.ai.model.AiActionHandleResult;
import com.erp.ai.model.AiActionResultVO;
import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiConversationTelemetry;
import com.erp.ai.model.AiMetaVO;
import com.erp.ai.model.AiModelCompletion;
import com.erp.ai.model.AiPageContext;
import com.erp.ai.model.AiPendingAction;
import com.erp.ai.model.AiPolicySummaryVO;
import com.erp.ai.model.AiPromptContext;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.model.AiRoleProfileVO;
import com.erp.ai.model.AiRuntimeConfig;
import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.model.AiTokenUsage;
import com.erp.ai.model.AiToolCall;
import com.erp.ai.model.AiToolDefinition;
import com.erp.ai.service.AiActionService;
import com.erp.ai.service.AiChatService;
import com.erp.ai.service.AiContextService;
import com.erp.ai.service.AiModelClient;
import com.erp.ai.service.AiReadToolService;
import com.erp.ai.service.AiRoleGuideService;
import com.erp.ai.service.AiSessionService;
import com.erp.ai.service.AiStreamListener;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.platform.contract.model.PlatformAiAuditCreateRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
            "confirm_action",
            "read_tools",
            "agent_loop",
            "structured_blocks",
            "session_history",
            "token_usage"));

    private final AiContextService aiContextService;
    private final AiModelClient aiModelClient;
    private final AiActionService aiActionService;
    private final AiTenantConfigService aiTenantConfigService;
    private final AiRoleGuideService aiRoleGuideService;
    private final AiReadToolService aiReadToolService;
    private final AiSessionService aiSessionService;
    private final ErpAiProperties erpAiProperties;
    private final ObjectMapper objectMapper;

    public AiChatServiceImpl(AiContextService aiContextService,
            AiModelClient aiModelClient,
            AiActionService aiActionService,
            AiTenantConfigService aiTenantConfigService,
            AiRoleGuideService aiRoleGuideService,
            AiReadToolService aiReadToolService,
            AiSessionService aiSessionService,
            ErpAiProperties erpAiProperties,
            ObjectMapper objectMapper) {
        this.aiContextService = aiContextService;
        this.aiModelClient = aiModelClient;
        this.aiActionService = aiActionService;
        this.aiTenantConfigService = aiTenantConfigService;
        this.aiRoleGuideService = aiRoleGuideService;
        this.aiReadToolService = aiReadToolService;
        this.aiSessionService = aiSessionService;
        this.erpAiProperties = erpAiProperties;
        this.objectMapper = objectMapper;
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

        AiConversationTelemetry telemetry = new AiConversationTelemetry(runtimeConfig.getModel());

        AiPromptContext promptContext;
        try {
            promptContext = aiContextService.buildPromptContext(request);
        } catch (Exception ex) {
            List<AiChatMessage> rawMessages = request == null ? null : request.getMessages();
            String latestUserMessage = getLatestUserMessage(rawMessages);
            String errorMessage = resolveFriendlyErrorMessage(ex, "当前无法装载 ERP 上下文，请稍后重试");
            recordAudit(resolveQuestionType(latestUserMessage), "L1", null, false, false,
                    latestUserMessage, errorMessage, System.currentTimeMillis() - startTime, telemetry);
            listener.onError(errorMessage);
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

        Long sessionId = aiSessionService.recordUserMessage(
                request == null ? null : request.getSessionId(), latestUserMessage, runtimeConfig.getModel());
        telemetry.setSessionId(sessionId);
        listener.onSession(sessionId);

        List<AiActionDescriptor> availableActions = aiActionService.listAvailableActions();
        List<AiChatMessage> modelMessages = new ArrayList<>();
        modelMessages.add(new AiChatMessage("system",
                buildSystemPrompt(promptContext, availableActions, roleProfile, runtimeConfig.getPromptTemplate())));
        modelMessages.addAll(conversationHistory);

        List<AiToolDefinition> availableTools = new ArrayList<>(aiActionService.buildAvailableTools());
        availableTools.addAll(aiReadToolService.buildAvailableTools());

        try {
            runAgentLoop(runtimeConfig, promptContext, modelMessages, availableTools, listener, telemetry,
                    conversationHistory, latestUserMessage, startTime);
        } catch (Exception ex) {
            String errorMessage = resolveFriendlyErrorMessage(ex, "AI 服务调用失败，请稍后重试");
            recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                    false, false, latestUserMessage, errorMessage, System.currentTimeMillis() - startTime, telemetry);
            listener.onError(errorMessage);
        }
    }

    /**
     * 执行 Agent 循环：模型可以连续多轮调用只读工具取数，直到给出最终文本答复。
     *
     * <p>循环有三道闸：轮次上限、总耗时上限，以及最后一轮强制不带工具，
     * 保证任何情况下都会收敛到一段可读的回复，而不是无限取数。
     * 写动作一旦出现即终止循环——写操作要么直接执行，要么进入确认卡片，不参与后续推理。</p>
     *
     * @param runtimeConfig       运行时配置
     * @param promptContext       提示词上下文
     * @param modelMessages       模型消息列表（会被就地追加）
     * @param availableTools      可用工具列表
     * @param listener            流式监听器
     * @param telemetry           本轮遥测数据
     * @param conversationHistory 规范化后的对话历史
     * @param latestUserMessage   最近一条用户提问
     * @param startTime           开始时间戳
     * @throws Exception 模型调用异常
     */
    private void runAgentLoop(AiRuntimeConfig runtimeConfig,
            AiPromptContext promptContext,
            List<AiChatMessage> modelMessages,
            List<AiToolDefinition> availableTools,
            AiStreamListener listener,
            AiConversationTelemetry telemetry,
            List<AiChatMessage> conversationHistory,
            String latestUserMessage,
            long startTime) throws Exception {
        int maxRounds = Math.max(1, erpAiProperties.getMaxToolRounds());
        long maxConversationMs = Math.max(10000L, erpAiProperties.getMaxConversationMs());

        for (int round = 0; round <= maxRounds; round++) {
            boolean finalRound = round == maxRounds || System.currentTimeMillis() - startTime > maxConversationMs;
            // 最后一轮撤掉工具，逼模型直接作答，避免在取数循环里打转。
            List<AiToolDefinition> toolsForRound = finalRound ? Collections.emptyList() : availableTools;

            AiModelCompletion completion = requestCompletionWithFallback(
                    runtimeConfig.getModel(), modelMessages, toolsForRound);
            telemetry.addUsage(completion == null ? null : completion.getUsage());

            List<AiToolCall> toolCalls = completion == null ? null : completion.getToolCalls();
            if (toolCalls == null || toolCalls.isEmpty()) {
                finishWithText(completion, promptContext, conversationHistory, listener, telemetry,
                        latestUserMessage, startTime, runtimeConfig, modelMessages);
                return;
            }

            AiToolCall writeCall = findWriteCall(toolCalls);
            if (writeCall != null) {
                handleWriteCall(promptContext, writeCall, listener, telemetry, latestUserMessage, startTime);
                return;
            }

            telemetry.markToolRound();
            List<AiToolCall> executedCalls = executeReadCalls(toolCalls, listener, telemetry, modelMessages);
            if (executedCalls.isEmpty()) {
                // 一个工具都没能执行（例如全部越权），直接进入最终作答轮，避免空转。
                round = maxRounds - 1;
            }
        }
    }

    /**
     * 执行本轮的只读工具调用，并把结果同时推给前端与模型。
     *
     * @param toolCalls     模型返回的工具调用列表
     * @param listener      流式监听器
     * @param telemetry     遥测数据
     * @param modelMessages 模型消息列表（会被就地追加）
     * @return 实际执行的工具调用列表
     */
    private List<AiToolCall> executeReadCalls(List<AiToolCall> toolCalls,
            AiStreamListener listener,
            AiConversationTelemetry telemetry,
            List<AiChatMessage> modelMessages) {
        int maxCalls = Math.max(1, erpAiProperties.getMaxToolCallsPerRound());
        List<AiToolCall> executedCalls = new ArrayList<>();
        List<AiReadToolResult> results = new ArrayList<>();

        for (AiToolCall toolCall : toolCalls) {
            if (executedCalls.size() >= maxCalls) {
                break;
            }
            if (toolCall == null || !aiReadToolService.isReadTool(toolCall.getName())) {
                continue;
            }
            listener.onToolStart(toolCall.getName(), resolveToolLabel(toolCall.getName()));
            AiReadToolResult result = aiReadToolService.execute(toolCall.getName(), parseArguments(toolCall.getArgumentsJson()));
            telemetry.markToolUsed(toolCall.getName());
            if (result.getBlock() != null) {
                listener.onBlock(result.getBlock());
                telemetry.addBlock(result.getBlock());
            }
            executedCalls.add(toolCall);
            results.add(result);
        }

        if (executedCalls.isEmpty()) {
            return executedCalls;
        }
        // 先追加带 tool_calls 的 assistant 消息，再逐条追加 tool 结果，顺序必须与协议一致。
        modelMessages.add(AiChatMessage.assistantToolCalls(null, executedCalls));
        for (int index = 0; index < executedCalls.size(); index++) {
            AiToolCall toolCall = executedCalls.get(index);
            AiReadToolResult result = results.get(index);
            modelMessages.add(AiChatMessage.toolResult(
                    StringUtils.hasText(toolCall.getId()) ? toolCall.getId() : toolCall.getName(),
                    toolCall.getName(),
                    result.getModelText()));
        }
        return executedCalls;
    }

    /**
     * 从工具调用列表中找出第一个写动作。
     *
     * @param toolCalls 工具调用列表
     * @return 写动作调用；不存在时返回 null
     */
    private AiToolCall findWriteCall(List<AiToolCall> toolCalls) {
        for (AiToolCall toolCall : toolCalls) {
            if (toolCall != null && StringUtils.hasText(toolCall.getName())
                    && !aiReadToolService.isReadTool(toolCall.getName())) {
                return toolCall;
            }
        }
        return null;
    }

    /**
     * 处理写动作调用：低风险直接执行，高风险产出确认卡片。
     *
     * @param promptContext     提示词上下文
     * @param writeCall         写动作调用
     * @param listener          流式监听器
     * @param telemetry         遥测数据
     * @param latestUserMessage 最近一条用户提问
     * @param startTime         开始时间戳
     */
    private void handleWriteCall(AiPromptContext promptContext,
            AiToolCall writeCall,
            AiStreamListener listener,
            AiConversationTelemetry telemetry,
            String latestUserMessage,
            long startTime) {
        AiActionHandleResult handleResult = aiActionService.handleToolCall(writeCall, promptContext);
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

        String actionKey = writeCall.getName();
        aiSessionService.recordAssistantMessage(telemetry.getSessionId(), assistantMessage,
                telemetry.getBlocks(), actionKey, telemetry.getUsage());
        recordAudit(resolveQuestionType(latestUserMessage), "L3", actionKey, false, true,
                latestUserMessage, resolveToolCallResponse(actionKey),
                System.currentTimeMillis() - startTime, telemetry);
    }

    /**
     * 以最终文本收尾：模型有内容就用模型的，否则退到确定性兜底回复，再不行才报错。
     *
     * @param completion          模型补全结果
     * @param promptContext       提示词上下文
     * @param conversationHistory 对话历史
     * @param listener            流式监听器
     * @param telemetry           遥测数据
     * @param latestUserMessage   最近一条用户提问
     * @param startTime           开始时间戳
     * @param runtimeConfig       运行时配置
     * @param modelMessages       模型消息列表
     * @throws Exception 模型调用异常
     */
    private void finishWithText(AiModelCompletion completion,
            AiPromptContext promptContext,
            List<AiChatMessage> conversationHistory,
            AiStreamListener listener,
            AiConversationTelemetry telemetry,
            String latestUserMessage,
            long startTime,
            AiRuntimeConfig runtimeConfig,
            List<AiChatMessage> modelMessages) throws Exception {
        String content = completion == null ? null : completion.getContent();
        if (StringUtils.hasText(content)) {
            emitText(content, listener);
            listener.onDone(content);
            finishAudit(content, true, latestUserMessage, startTime, telemetry);
            return;
        }

        String streamedFallbackContent = requestStreamingFallback(runtimeConfig.getModel(), modelMessages, listener);
        if (StringUtils.hasText(streamedFallbackContent)) {
            listener.onDone(streamedFallbackContent);
            finishAudit(streamedFallbackContent, true, latestUserMessage, startTime, telemetry);
            return;
        }

        // 模型彻底没给出内容时，用本地上下文拼一段确定性回复，至少保住核心问题的可用性。
        String contextFallbackReply = buildContextFallbackReply(promptContext, conversationHistory);
        if (StringUtils.hasText(contextFallbackReply)) {
            emitText(contextFallbackReply, listener);
            listener.onDone(contextFallbackReply);
            finishAudit(contextFallbackReply, true, latestUserMessage, startTime, telemetry);
            return;
        }

        finishAudit("AI 未返回有效内容，请稍后重试", false, latestUserMessage, startTime, telemetry);
        listener.onError("AI 未返回有效内容，请稍后重试");
    }

    /**
     * 统一收尾：写会话存档并落审计。
     *
     * @param content           最终回复
     * @param success           是否成功
     * @param latestUserMessage 最近一条用户提问
     * @param startTime         开始时间戳
     * @param telemetry         遥测数据
     */
    private void finishAudit(String content,
            boolean success,
            String latestUserMessage,
            long startTime,
            AiConversationTelemetry telemetry) {
        if (success) {
            aiSessionService.recordAssistantMessage(telemetry.getSessionId(), content,
                    telemetry.getBlocks(), null, telemetry.getUsage());
        }
        recordAudit(resolveQuestionType(latestUserMessage), resolveInteractionLevel(latestUserMessage, null), null,
                false, success, latestUserMessage, content, System.currentTimeMillis() - startTime, telemetry);
    }

    /**
     * 解析只读工具的展示名，用于前端过程反馈。
     *
     * @param toolName 工具名称
     * @return 展示名
     */
    private String resolveToolLabel(String toolName) {
        if (!StringUtils.hasText(toolName)) {
            return "数据查询";
        }
        return switch (toolName) {
            case "query_todo_backlog" -> "待办积压分布";
            case "query_todo_aging" -> "待办滞留明细";
            case "query_approval_duration" -> "审批节点耗时";
            case "query_process_instance_stats" -> "流程实例分布";
            case "query_user_workload" -> "人员负载排行";
            case "query_approval_trend" -> "审批动作趋势";
            case "query_notice_overview" -> "消息分布";
            case "query_operation_trend" -> "系统操作趋势";
            case "query_ai_usage_trend" -> "AI 使用量";
            case "query_stock_overview" -> "库存概览";
            case "query_stock_warning" -> "库存预警";
            case "query_hr_headcount" -> "在岗人数";
            case "query_hr_warning" -> "HR 预警";
            default -> "数据查询";
        };
    }

    /**
     * 解析工具参数 JSON。
     *
     * @param argumentsJson 参数 JSON
     * @return 参数映射
     */
    private Map<String, Object> parseArguments(String argumentsJson) {
        if (!StringUtils.hasText(argumentsJson)) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(argumentsJson, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            return Collections.emptyMap();
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
        AiConversationTelemetry telemetry = new AiConversationTelemetry(
                aiTenantConfigService.resolveRuntimeConfig().getModel());
        if (request == null || !StringUtils.hasText(request.getConfirmationToken())) {
            AiActionResultVO result = new AiActionResultVO();
            result.setSuccess(false);
            result.setMessage("确认票据不能为空");
            result.setAssistantMessage("确认票据不能为空，请重新发起操作。");
            recordAudit("action_confirm", "L3", null, true, false,
                    "confirmationToken=empty", result.getAssistantMessage(), 0L, telemetry);
            return result;
        }
        long startTime = System.currentTimeMillis();
        AiActionResultVO result = aiActionService.confirm(request.getConfirmationToken().trim());
        recordAudit("action_confirm", "L3", result == null ? null : result.getActionKey(), true,
                result != null && result.isSuccess(), "confirmationToken=provided",
                result == null ? null : resolveAssistantMessageFromResult(result),
                System.currentTimeMillis() - startTime, telemetry);
        return result;
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
        promptBuilder.append("2. 你可以做只读分析，也可以在权限范围内调用工具；除了工具列表中明确给出的动作，其他任何写操作都禁止执行。\n");
        promptBuilder.append("3. 工具分两类：query_ 开头的是只读查询工具，可以连续调用多轮来获取数据；其余是写动作工具。\n");
        promptBuilder.append("4. 需要具体业务数据（积压量、耗时、库存、人数、趋势等）时，必须调用只读查询工具取数，不要凭空编造数字。\n");
        promptBuilder.append("5. 只读工具的结果会以指标卡和表格的形式直接展示给用户，你的文字回复要给出解读和建议，不要逐行复述表格内容。\n");
        promptBuilder.append("6. 未拿到工具执行成功结果之前，绝不能声称操作已完成。\n");
        promptBuilder.append("7. 命中多个候选目标时，必须先澄清，不允许猜测。\n");
        promptBuilder.append("8. 驳回动作如果用户没有提供驳回原因，必须先追问原因，不允许直接调用工具。\n");
        promptBuilder.append("9. 一次最多发起一个写动作；写动作不能与只读查询混在同一轮里。\n");
        promptBuilder.append("10. 不要向用户暴露隐藏候选区里的内部 ID。\n");
        promptBuilder.append("11. 回答尽量直接、实用，可使用短列表，但不要输出 HTML 或 Markdown 表格。\n\n");
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
            long durationMs,
            AiConversationTelemetry telemetry) {
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
        if (telemetry != null) {
            AiTokenUsage usage = telemetry.getUsage();
            request.setModel(limitAuditExcerpt(telemetry.getModel(), 128));
            request.setSessionId(telemetry.getSessionId());
            request.setToolRounds(telemetry.getToolRounds());
            request.setToolKeys(limitAuditExcerpt(telemetry.joinToolKeys(), 500));
            if (usage != null && usage.hasValue()) {
                request.setPromptTokens(usage.getPromptTokens());
                request.setCompletionTokens(usage.getCompletionTokens());
                request.setTotalTokens(usage.getTotalTokens());
            }
        }
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
