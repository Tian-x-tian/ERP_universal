package com.erp.system.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.system.ai.mapper.AiTodoTaskMapper;
import com.erp.system.ai.mapper.AiWorkflowTaskMapper;
import com.erp.system.ai.model.AiActionDescriptor;
import com.erp.system.ai.model.AiActionHandleResult;
import com.erp.system.ai.model.AiActionResultVO;
import com.erp.system.ai.model.AiConfirmationPayload;
import com.erp.system.ai.model.AiPendingAction;
import com.erp.system.ai.model.AiPromptContext;
import com.erp.system.ai.model.AiToolCall;
import com.erp.system.ai.model.AiToolDefinition;
import com.erp.system.ai.service.AiActionService;
import com.erp.system.ai.service.AiConfirmationTokenService;
import com.erp.system.domain.SysNotice;
import com.erp.system.security.service.PermissionService;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysNoticeService;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.vo.WorkflowDefinitionLiteVO;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * AI 动作编排服务实现。
 */
@Service
public class AiActionServiceImpl implements AiActionService {
    private static final String ACTION_TODO_CLAIM = "todo_claim";
    private static final String ACTION_TODO_FINISH = "todo_finish";
    private static final String ACTION_NOTICE_READ = "notice_read";
    private static final String ACTION_NOTICE_READ_ALL = "notice_read_all";
    private static final String ACTION_WORKFLOW_APPROVE = "workflow_approve";
    private static final String ACTION_WORKFLOW_REJECT = "workflow_reject";
    private static final String ACTION_WORKFLOW_DEFINITION_PUBLISH = "workflow_definition_publish";

    private static final String RISK_LOW = "low";
    private static final String RISK_HIGH = "high";

    private static final String TODO_STATUS_PENDING = "0";
    private static final String TODO_STATUS_FINISHED = "2";
    private static final String WORKFLOW_DEFINITION_STATUS_PUBLISHED = "1";
    private static final Set<String> TASK_TERMINAL_STATUS = new LinkedHashSet<>(Arrays.asList("2", "3", "4", "5"));
    private static final long CONFIRM_TOKEN_TTL_MILLIS = 5 * 60 * 1000L;

    private final PermissionService permissionService;
    private final SecurityUserResolver securityUserResolver;
    private final ISysNoticeService noticeService;
    private final AiTodoTaskMapper todoTaskMapper;
    private final AiWorkflowTaskMapper workflowTaskMapper;
    private final InternalWorkflowClient internalWorkflowClient;
    private final AiConfirmationTokenService aiConfirmationTokenService;
    private final ObjectMapper objectMapper;

    public AiActionServiceImpl(PermissionService permissionService,
            SecurityUserResolver securityUserResolver,
            ISysNoticeService noticeService,
            AiTodoTaskMapper todoTaskMapper,
            AiWorkflowTaskMapper workflowTaskMapper,
            InternalWorkflowClient internalWorkflowClient,
            AiConfirmationTokenService aiConfirmationTokenService,
            ObjectMapper objectMapper) {
        this.permissionService = permissionService;
        this.securityUserResolver = securityUserResolver;
        this.noticeService = noticeService;
        this.todoTaskMapper = todoTaskMapper;
        this.workflowTaskMapper = workflowTaskMapper;
        this.internalWorkflowClient = internalWorkflowClient;
        this.aiConfirmationTokenService = aiConfirmationTokenService;
        this.objectMapper = objectMapper;
    }

    /**
     * 查询当前用户可执行的动作列表。
     *
     * @return 动作列表
     */
    @Override
    public List<AiActionDescriptor> listAvailableActions() {
        List<AiActionDescriptor> actionList = new ArrayList<>();
        appendActionIfAllowed(actionList, ACTION_TODO_CLAIM, "签收待办", RISK_LOW, hasTodoClaimPermission());
        appendActionIfAllowed(actionList, ACTION_TODO_FINISH, "办结待办", RISK_HIGH, hasTodoFinishPermission());
        appendActionIfAllowed(actionList, ACTION_NOTICE_READ, "标记消息已读", RISK_LOW, hasNoticeReadPermission());
        appendActionIfAllowed(actionList, ACTION_NOTICE_READ_ALL, "全部消息已读", RISK_LOW, hasNoticeReadPermission());
        appendActionIfAllowed(actionList, ACTION_WORKFLOW_APPROVE, "审批通过", RISK_HIGH, hasWorkflowApprovePermission());
        appendActionIfAllowed(actionList, ACTION_WORKFLOW_REJECT, "审批驳回", RISK_HIGH, hasWorkflowRejectPermission());
        appendActionIfAllowed(actionList, ACTION_WORKFLOW_DEFINITION_PUBLISH, "发布流程定义", RISK_HIGH, hasWorkflowPublishPermission());
        return actionList;
    }

    /**
     * 构造当前用户可用的模型工具列表。
     *
     * @return 工具定义列表
     */
    @Override
    public List<AiToolDefinition> buildAvailableTools() {
        List<AiToolDefinition> toolList = new ArrayList<>();
        if (hasTodoClaimPermission()) {
            toolList.add(buildTool(ACTION_TODO_CLAIM,
                    "当用户明确要求签收某条待办时调用。优先传 todoId；若没有明确 ID，可传 reference。",
                    buildSchema(
                            buildIntegerProperty("todoId", "待办ID，优先使用"),
                            buildStringProperty("reference", "待办引用，如“第一条”“最新一条”“采购审批单 WF20260322001”"))));
        }
        if (hasTodoFinishPermission()) {
            toolList.add(buildTool(ACTION_TODO_FINISH,
                    "当用户明确要求办结某条待办时调用。该动作属于高风险，会进入确认卡片。",
                    buildSchema(
                            buildIntegerProperty("todoId", "待办ID，优先使用"),
                            buildStringProperty("reference", "待办引用，如“第一条”“最新一条”“采购审批单 WF20260322001”"))));
        }
        if (hasNoticeReadPermission()) {
            toolList.add(buildTool(ACTION_NOTICE_READ,
                    "当用户要求把某条系统消息标记为已读时调用。优先传 noticeId。",
                    buildSchema(
                            buildIntegerProperty("noticeId", "消息ID，优先使用"),
                            buildStringProperty("reference", "消息引用，如“第一条”“最新一条”“流程催办”"))));
            toolList.add(buildTool(ACTION_NOTICE_READ_ALL,
                    "当用户要求把当前账号全部未读消息标记为已读时调用。",
                    buildSchema()));
        }
        if (hasWorkflowApprovePermission()) {
            toolList.add(buildTool(ACTION_WORKFLOW_APPROVE,
                    "当用户明确要求审批通过某条流程任务时调用。优先传 taskId 或 todoId，可附 comment。",
                    buildSchema(
                            buildIntegerProperty("taskId", "流程任务ID，优先使用"),
                            buildIntegerProperty("todoId", "关联待办ID"),
                            buildStringProperty("reference", "任务引用，如“第一条”“最新一条”“采购审批单 WF20260322001”"),
                            buildStringProperty("comment", "审批意见，可为空"))));
        }
        if (hasWorkflowRejectPermission()) {
            toolList.add(buildTool(ACTION_WORKFLOW_REJECT,
                    "当用户明确要求驳回某条流程任务且已经提供驳回原因时调用。没有 reason 时不要调用。",
                    buildSchema(
                            buildIntegerProperty("taskId", "流程任务ID，优先使用"),
                            buildIntegerProperty("todoId", "关联待办ID"),
                            buildStringProperty("reference", "任务引用，如“第一条”“最新一条”“采购审批单 WF20260322001”"),
                            buildStringProperty("reason", "驳回原因，必填"))));
        }
        if (hasWorkflowPublishPermission()) {
            toolList.add(buildTool(ACTION_WORKFLOW_DEFINITION_PUBLISH,
                    "当用户明确要求发布流程定义时调用。优先传 definitionId；否则传 processKey 或 processName。",
                    buildSchema(
                            buildIntegerProperty("definitionId", "流程定义ID，优先使用"),
                            buildStringProperty("processKey", "流程标识"),
                            buildStringProperty("processName", "流程名称"),
                            buildStringProperty("reference", "定义引用，如“第一条”“采购申请流程”"))));
        }
        return toolList;
    }

    /**
     * 处理模型返回的工具调用。
     *
     * @param toolCall       工具调用
     * @param promptContext  提示词上下文
     * @return 动作处理结果
     */
    @Override
    public AiActionHandleResult handleToolCall(AiToolCall toolCall, AiPromptContext promptContext) {
        AiActionHandleResult handleResult = new AiActionHandleResult();
        if (toolCall == null || !StringUtils.hasText(toolCall.getName())) {
            handleResult.setAssistantMessage("当前未识别到可执行动作，请再说明要处理的目标。");
            return handleResult;
        }
        String actionKey = toolCall.getName().trim();
        if (!isActionAvailable(actionKey)) {
            handleResult.setAssistantMessage("当前账号没有执行该操作的权限。");
            return handleResult;
        }

        Map<String, Object> arguments = parseArguments(toolCall.getArgumentsJson());
        PreparedAction preparedAction = prepareAction(actionKey, arguments, promptContext);
        if (StringUtils.hasText(preparedAction.clarificationMessage)) {
            handleResult.setAssistantMessage(preparedAction.clarificationMessage);
            return handleResult;
        }
        if (isHighRiskAction(actionKey)) {
            AiPendingAction pendingAction = buildPendingAction(preparedAction);
            handleResult.setAssistantMessage("我已定位到【" + pendingAction.getTargetLabel() + "】。这是高风险操作，请先确认后再执行。");
            handleResult.setPendingAction(pendingAction);
            return handleResult;
        }

        AiActionResultVO actionResult = executeAction(preparedAction.actionKey, preparedAction.actionArgs, preparedAction.targetLabel);
        handleResult.setAssistantMessage(resolveAssistantMessage(actionResult));
        return handleResult;
    }

    /**
     * 确认执行高风险动作。
     *
     * @param confirmationToken 确认票据
     * @return 动作执行结果
     */
    @Override
    public AiActionResultVO confirm(String confirmationToken) {
        try {
            AiConfirmationPayload payload = aiConfirmationTokenService.parseToken(confirmationToken);
            Long currentUserId = securityUserResolver.getCurrentUserId();
            String currentTenantId = trimToNull(securityUserResolver.getCurrentTenantId());
            if (currentUserId == null || !Objects.equals(currentUserId, payload.getUserId())) {
                return buildFailureResult(payload.getActionKey(), payload.getTargetLabel(), "确认票据不属于当前用户", "当前无法确认该操作，请重新发起。");
            }
            if (!Objects.equals(trimToNull(payload.getTenantId()), currentTenantId)) {
                return buildFailureResult(payload.getActionKey(), payload.getTargetLabel(), "确认票据租户不匹配", "当前无法确认该操作，请重新发起。");
            }
            if (!isActionAvailable(payload.getActionKey()) || !isHighRiskAction(payload.getActionKey())) {
                return buildFailureResult(payload.getActionKey(), payload.getTargetLabel(), "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
            }
            AiActionResultVO result = executeAction(payload.getActionKey(), payload.getActionArgs(), payload.getTargetLabel());
            if (!StringUtils.hasText(result.getAssistantMessage())) {
                result.setAssistantMessage(resolveAssistantMessage(result));
            }
            return result;
        } catch (Exception ex) {
            return buildFailureResult(null, null, resolveFriendlyMessage(ex, "确认执行失败"), "确认执行失败，请重新发起。");
        }
    }

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

    private PreparedAction prepareAction(String actionKey, Map<String, Object> arguments, AiPromptContext promptContext) {
        return switch (actionKey) {
            case ACTION_NOTICE_READ_ALL -> prepareNoticeReadAll();
            case ACTION_NOTICE_READ -> prepareNoticeRead(arguments, promptContext);
            case ACTION_TODO_CLAIM -> prepareTodoClaim(arguments, promptContext);
            case ACTION_TODO_FINISH -> prepareTodoFinish(arguments, promptContext);
            case ACTION_WORKFLOW_APPROVE -> prepareWorkflowApprove(arguments, promptContext);
            case ACTION_WORKFLOW_REJECT -> prepareWorkflowReject(arguments, promptContext);
            case ACTION_WORKFLOW_DEFINITION_PUBLISH -> prepareDefinitionPublish(arguments, promptContext);
            default -> PreparedAction.clarify("当前未识别到可执行动作，请再说明要处理的目标。");
        };
    }

    private PreparedAction prepareNoticeReadAll() {
        PreparedAction preparedAction = PreparedAction.of(ACTION_NOTICE_READ_ALL, "全部消息已读", RISK_LOW);
        preparedAction.targetLabel = "当前账号全部未读消息";
        preparedAction.summary = "将当前账号全部未读消息标记为已读。";
        return preparedAction;
    }

    private PreparedAction prepareNoticeRead(Map<String, Object> arguments, AiPromptContext promptContext) {
        SysNotice notice = resolveNotice(arguments, promptContext);
        if (notice == null || notice.getNoticeId() == null) {
            return PreparedAction.clarify("我还没定位到要处理的消息，请说明是第几条或消息标题。");
        }
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null || !Objects.equals(currentUserId, notice.getReceiverUserId())) {
            return PreparedAction.clarify("当前只能处理属于你自己的系统消息。");
        }
        PreparedAction preparedAction = PreparedAction.of(ACTION_NOTICE_READ, "标记消息已读", RISK_LOW);
        preparedAction.targetLabel = buildNoticeLabel(notice);
        preparedAction.summary = "将消息标记为已读：" + preparedAction.targetLabel;
        preparedAction.actionArgs.put("noticeId", notice.getNoticeId());
        return preparedAction;
    }

    private PreparedAction prepareTodoClaim(Map<String, Object> arguments, AiPromptContext promptContext) {
        ResolvedTodoTarget resolvedTarget = resolveTodoTarget(arguments, promptContext, false);
        if (StringUtils.hasText(resolvedTarget.clarificationMessage)) {
            return PreparedAction.clarify(resolvedTarget.clarificationMessage);
        }
        if (resolvedTarget.todoTask == null || resolvedTarget.todoTask.getTodoId() == null) {
            return PreparedAction.clarify("我还没定位到要签收的待办，请说明是第几条或业务单号。");
        }
        if (!TODO_STATUS_PENDING.equals(trimToNull(resolvedTarget.todoTask.getStatus()))) {
            return PreparedAction.clarify("这条待办当前不是“待处理”状态，无需再次签收。");
        }
        PreparedAction preparedAction = PreparedAction.of(ACTION_TODO_CLAIM, "签收待办", RISK_LOW);
        preparedAction.targetLabel = resolvedTarget.targetLabel;
        preparedAction.summary = "签收待办：" + preparedAction.targetLabel;
        preparedAction.actionArgs.put("todoId", resolvedTarget.todoTask.getTodoId());
        return preparedAction;
    }

    private PreparedAction prepareTodoFinish(Map<String, Object> arguments, AiPromptContext promptContext) {
        ResolvedTodoTarget resolvedTarget = resolveTodoTarget(arguments, promptContext, false);
        if (StringUtils.hasText(resolvedTarget.clarificationMessage)) {
            return PreparedAction.clarify(resolvedTarget.clarificationMessage);
        }
        if (resolvedTarget.todoTask == null || resolvedTarget.todoTask.getTodoId() == null) {
            return PreparedAction.clarify("我还没定位到要办结的待办，请说明是第几条或业务单号。");
        }
        if (TODO_STATUS_FINISHED.equals(trimToNull(resolvedTarget.todoTask.getStatus()))) {
            return PreparedAction.clarify("这条待办已经办结，无需重复处理。");
        }
        PreparedAction preparedAction = PreparedAction.of(ACTION_TODO_FINISH, "办结待办", RISK_HIGH);
        preparedAction.targetLabel = resolvedTarget.targetLabel;
        preparedAction.summary = "办结待办后，可能会推进对应流程：" + preparedAction.targetLabel;
        preparedAction.actionArgs.put("todoId", resolvedTarget.todoTask.getTodoId());
        return preparedAction;
    }

    private PreparedAction prepareWorkflowApprove(Map<String, Object> arguments, AiPromptContext promptContext) {
        ResolvedTodoTarget resolvedTarget = resolveTodoTarget(arguments, promptContext, true);
        if (StringUtils.hasText(resolvedTarget.clarificationMessage)) {
            return PreparedAction.clarify(resolvedTarget.clarificationMessage);
        }
        if (resolvedTarget.workflowTask == null || resolvedTarget.workflowTask.getTaskId() == null) {
            return PreparedAction.clarify("当前没有可审批的流程任务，请先指定待办。");
        }
        PreparedAction preparedAction = PreparedAction.of(ACTION_WORKFLOW_APPROVE, "审批通过", RISK_HIGH);
        preparedAction.targetLabel = resolvedTarget.targetLabel;
        preparedAction.summary = "审批通过流程任务：" + preparedAction.targetLabel;
        preparedAction.actionArgs.put("taskId", resolvedTarget.workflowTask.getTaskId());
        String comment = trimToNull(stringValue(arguments.get("comment")));
        preparedAction.actionArgs.put("comment", StringUtils.hasText(comment) ? comment : "AI 助手审批通过");
        return preparedAction;
    }

    private PreparedAction prepareWorkflowReject(Map<String, Object> arguments, AiPromptContext promptContext) {
        String reason = trimToNull(stringValue(arguments.get("reason")));
        if (!StringUtils.hasText(reason)) {
            return PreparedAction.clarify("请先告诉我驳回原因，我再为你生成确认卡片。");
        }
        ResolvedTodoTarget resolvedTarget = resolveTodoTarget(arguments, promptContext, true);
        if (StringUtils.hasText(resolvedTarget.clarificationMessage)) {
            return PreparedAction.clarify(resolvedTarget.clarificationMessage);
        }
        if (resolvedTarget.workflowTask == null || resolvedTarget.workflowTask.getTaskId() == null) {
            return PreparedAction.clarify("当前没有可驳回的流程任务，请先指定待办。");
        }
        PreparedAction preparedAction = PreparedAction.of(ACTION_WORKFLOW_REJECT, "审批驳回", RISK_HIGH);
        preparedAction.targetLabel = resolvedTarget.targetLabel;
        preparedAction.summary = "驳回流程任务：" + preparedAction.targetLabel + "。驳回原因：" + reason;
        preparedAction.actionArgs.put("taskId", resolvedTarget.workflowTask.getTaskId());
        preparedAction.actionArgs.put("reason", reason);
        return preparedAction;
    }

    private PreparedAction prepareDefinitionPublish(Map<String, Object> arguments, AiPromptContext promptContext) {
        ResolutionResult<WorkflowDefinitionLiteVO> definitionResult = resolveDefinition(arguments, promptContext);
        if (StringUtils.hasText(definitionResult.message)) {
            return PreparedAction.clarify(definitionResult.message);
        }
        WorkflowDefinitionLiteVO definition = definitionResult.value;
        if (definition == null || definition.getDefinitionId() == null) {
            return PreparedAction.clarify("我还没定位到要发布的流程定义，请补充流程标识或流程名称。");
        }
        PreparedAction preparedAction = PreparedAction.of(ACTION_WORKFLOW_DEFINITION_PUBLISH, "发布流程定义", RISK_HIGH);
        preparedAction.targetLabel = buildDefinitionLabel(definition);
        preparedAction.summary = "发布流程定义：" + preparedAction.targetLabel;
        preparedAction.actionArgs.put("definitionId", definition.getDefinitionId());
        return preparedAction;
    }

    private AiActionResultVO executeAction(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        return switch (actionKey) {
            case ACTION_NOTICE_READ_ALL -> executeNoticeReadAll();
            case ACTION_NOTICE_READ -> executeNoticeRead(actionKey, actionArgs, targetLabel);
            case ACTION_TODO_CLAIM -> executeTodoClaim(actionKey, actionArgs, targetLabel);
            case ACTION_TODO_FINISH -> executeTodoFinish(actionKey, actionArgs, targetLabel);
            case ACTION_WORKFLOW_APPROVE -> executeWorkflowApprove(actionKey, actionArgs, targetLabel);
            case ACTION_WORKFLOW_REJECT -> executeWorkflowReject(actionKey, actionArgs, targetLabel);
            case ACTION_WORKFLOW_DEFINITION_PUBLISH -> executeDefinitionPublish(actionKey, actionArgs, targetLabel);
            default -> buildFailureResult(actionKey, targetLabel, "当前未识别到可执行动作", "当前未识别到可执行动作。");
        };
    }

    private AiActionResultVO executeNoticeReadAll() {
        if (!hasNoticeReadPermission()) {
            return buildFailureResult(ACTION_NOTICE_READ_ALL, "当前账号全部未读消息", "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return buildFailureResult(ACTION_NOTICE_READ_ALL, "当前账号全部未读消息", "当前登录状态已失效", "当前登录状态已失效，请重新登录后重试。");
        }
        int updatedCount = noticeService.markAllRead(currentUserId);
        if (updatedCount <= 0) {
            return buildSuccessResult(ACTION_NOTICE_READ_ALL, "当前账号全部未读消息", "当前没有未读消息需要处理", "当前没有未读消息需要处理。");
        }
        return buildSuccessResult(ACTION_NOTICE_READ_ALL, "当前账号全部未读消息",
                "已标记 " + updatedCount + " 条未读消息为已读",
                "已为你标记 " + updatedCount + " 条未读消息为已读。");
    }

    private AiActionResultVO executeNoticeRead(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        if (!hasNoticeReadPermission()) {
            return buildFailureResult(actionKey, targetLabel, "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long currentUserId = securityUserResolver.getCurrentUserId();
        Long noticeId = toLong(actionArgs == null ? null : actionArgs.get("noticeId"));
        if (currentUserId == null || noticeId == null) {
            return buildFailureResult(actionKey, targetLabel, "消息目标无效", "当前没有定位到可处理的消息。");
        }
        SysNotice notice = noticeService.getById(noticeId);
        if (notice == null || !Objects.equals(currentUserId, notice.getReceiverUserId())) {
            return buildFailureResult(actionKey, targetLabel, "消息不存在或无权限处理", "当前只能处理属于你自己的系统消息。");
        }
        boolean success = noticeService.markRead(noticeId, currentUserId);
        String resolvedTargetLabel = buildNoticeLabel(notice);
        if (!success) {
            return buildFailureResult(actionKey, resolvedTargetLabel, "消息标记已读失败", "未能标记该消息为已读，请刷新后重试。");
        }
        return buildSuccessResult(actionKey, resolvedTargetLabel, "消息已标记为已读", "已为你将该消息标记为已读。");
    }

    private AiActionResultVO executeTodoClaim(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        if (!hasTodoClaimPermission()) {
            return buildFailureResult(actionKey, targetLabel, "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long todoId = toLong(actionArgs == null ? null : actionArgs.get("todoId"));
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (todoId == null || currentUserId == null) {
            return buildFailureResult(actionKey, targetLabel, "待办目标无效", "当前没有定位到可签收的待办。");
        }
        SysTodoTask todoTask = todoTaskMapper.selectById(todoId);
        if (todoTask == null || !Objects.equals(todoTask.getAssigneeUserId(), currentUserId)) {
            return buildFailureResult(actionKey, targetLabel, "待办不存在或无权限处理", "当前只能处理属于你自己的待办。");
        }
        String resolvedTargetLabel = buildTodoLabel(todoTask);
        boolean success = internalWorkflowClient.claimTodo(todoId);
        if (!success) {
            return buildFailureResult(actionKey, resolvedTargetLabel, "待办签收失败", "未能签收该待办，可能状态已变化，请刷新后重试。");
        }
        return buildSuccessResult(actionKey, resolvedTargetLabel, "待办签收成功", "已为你签收该待办。");
    }

    private AiActionResultVO executeTodoFinish(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        if (!hasTodoFinishPermission()) {
            return buildFailureResult(actionKey, targetLabel, "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long todoId = toLong(actionArgs == null ? null : actionArgs.get("todoId"));
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (todoId == null || currentUserId == null) {
            return buildFailureResult(actionKey, targetLabel, "待办目标无效", "当前没有定位到可办结的待办。");
        }
        SysTodoTask todoTask = todoTaskMapper.selectById(todoId);
        if (todoTask == null || !Objects.equals(todoTask.getAssigneeUserId(), currentUserId)) {
            return buildFailureResult(actionKey, targetLabel, "待办不存在或无权限处理", "当前只能处理属于你自己的待办。");
        }
        String resolvedTargetLabel = buildTodoLabel(todoTask);
        boolean success = internalWorkflowClient.finishTodo(todoId);
        if (!success) {
            return buildFailureResult(actionKey, resolvedTargetLabel, "待办办结失败", "未能办结该待办，可能流程状态已变化，请刷新后重试。");
        }
        return buildSuccessResult(actionKey, resolvedTargetLabel, "待办办结成功", "已为你办结该待办。");
    }

    private AiActionResultVO executeWorkflowApprove(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        if (!hasWorkflowApprovePermission()) {
            return buildFailureResult(actionKey, targetLabel, "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long taskId = toLong(actionArgs == null ? null : actionArgs.get("taskId"));
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (taskId == null || currentUserId == null) {
            return buildFailureResult(actionKey, targetLabel, "流程任务目标无效", "当前没有定位到可审批的流程任务。");
        }
        SysWorkflowTask workflowTask = workflowTaskMapper.selectById(taskId);
        if (workflowTask == null) {
            return buildFailureResult(actionKey, targetLabel, "流程任务不存在", "当前没有定位到可审批的流程任务。");
        }
        if (workflowTask.getAssigneeUserId() != null && !Objects.equals(workflowTask.getAssigneeUserId(), currentUserId)) {
            return buildFailureResult(actionKey, targetLabel, "流程任务不属于当前用户", "当前只能处理属于你自己的流程任务。");
        }
        if (TASK_TERMINAL_STATUS.contains(trimToNull(workflowTask.getStatus()))) {
            return buildFailureResult(actionKey, targetLabel, "流程任务已处理", "该流程任务已经处理完成，请刷新后再试。");
        }
        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment(trimToNull(stringValue(actionArgs == null ? null : actionArgs.get("comment"))));
        boolean success = internalWorkflowClient.approveTask(taskId, actionBody);
        if (!success) {
            return buildFailureResult(actionKey, targetLabel, "流程审批通过失败", "未能审批通过该流程任务，请刷新后重试。");
        }
        return buildSuccessResult(actionKey, resolveTaskLabel(workflowTask, targetLabel), "流程审批通过成功", "已为你审批通过该流程任务。");
    }

    private AiActionResultVO executeWorkflowReject(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        if (!hasWorkflowRejectPermission()) {
            return buildFailureResult(actionKey, targetLabel, "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long taskId = toLong(actionArgs == null ? null : actionArgs.get("taskId"));
        String reason = trimToNull(stringValue(actionArgs == null ? null : actionArgs.get("reason")));
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (taskId == null || currentUserId == null) {
            return buildFailureResult(actionKey, targetLabel, "流程任务目标无效", "当前没有定位到可驳回的流程任务。");
        }
        if (!StringUtils.hasText(reason)) {
            return buildFailureResult(actionKey, targetLabel, "驳回原因不能为空", "请先提供驳回原因后再执行。");
        }
        SysWorkflowTask workflowTask = workflowTaskMapper.selectById(taskId);
        if (workflowTask == null) {
            return buildFailureResult(actionKey, targetLabel, "流程任务不存在", "当前没有定位到可驳回的流程任务。");
        }
        if (workflowTask.getAssigneeUserId() != null && !Objects.equals(workflowTask.getAssigneeUserId(), currentUserId)) {
            return buildFailureResult(actionKey, targetLabel, "流程任务不属于当前用户", "当前只能处理属于你自己的流程任务。");
        }
        if (TASK_TERMINAL_STATUS.contains(trimToNull(workflowTask.getStatus()))) {
            return buildFailureResult(actionKey, targetLabel, "流程任务已处理", "该流程任务已经处理完成，请刷新后再试。");
        }
        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment(reason);
        boolean success = internalWorkflowClient.rejectTask(taskId, actionBody);
        if (!success) {
            return buildFailureResult(actionKey, targetLabel, "流程审批驳回失败", "未能驳回该流程任务，请刷新后重试。");
        }
        return buildSuccessResult(actionKey, resolveTaskLabel(workflowTask, targetLabel), "流程审批驳回成功", "已为你驳回该流程任务。");
    }

    private AiActionResultVO executeDefinitionPublish(String actionKey, Map<String, Object> actionArgs, String targetLabel) {
        if (!hasWorkflowPublishPermission()) {
            return buildFailureResult(actionKey, targetLabel, "当前账号没有执行该操作的权限", "当前账号没有执行该操作的权限。");
        }
        Long definitionId = toLong(actionArgs == null ? null : actionArgs.get("definitionId"));
        if (definitionId == null) {
            return buildFailureResult(actionKey, targetLabel, "流程定义目标无效", "当前没有定位到可发布的流程定义。");
        }
        List<WorkflowDefinitionLiteVO> definitionList = internalWorkflowClient.listDefinitionLite(null, null, null);
        WorkflowDefinitionLiteVO definition = definitionList.stream()
                .filter(item -> item != null && Objects.equals(definitionId, item.getDefinitionId()))
                .findFirst()
                .orElse(null);
        if (definition == null) {
            return buildFailureResult(actionKey, targetLabel, "流程定义不存在", "当前没有定位到可发布的流程定义。");
        }
        if (WORKFLOW_DEFINITION_STATUS_PUBLISHED.equals(trimToNull(definition.getStatus()))) {
            return buildFailureResult(actionKey, buildDefinitionLabel(definition), "流程定义已发布", "该流程定义已经发布，无需重复操作。");
        }
        boolean success = internalWorkflowClient.publishDefinition(definitionId);
        if (!success) {
            return buildFailureResult(actionKey, buildDefinitionLabel(definition), "流程定义发布失败", "未能发布该流程定义，请刷新后重试。");
        }
        return buildSuccessResult(actionKey, buildDefinitionLabel(definition), "流程定义发布成功", "已为你发布该流程定义。");
    }

    private AiPendingAction buildPendingAction(PreparedAction preparedAction) {
        AiPendingAction pendingAction = new AiPendingAction();
        pendingAction.setActionKey(preparedAction.actionKey);
        pendingAction.setActionLabel(preparedAction.actionLabel);
        pendingAction.setRiskLevel(preparedAction.riskLevel);
        pendingAction.setTargetLabel(preparedAction.targetLabel);
        pendingAction.setSummary(preparedAction.summary);
        pendingAction.setActionArgs(new LinkedHashMap<>(preparedAction.actionArgs));

        AiConfirmationPayload payload = new AiConfirmationPayload();
        payload.setUserId(securityUserResolver.getCurrentUserId());
        payload.setTenantId(trimToNull(securityUserResolver.getCurrentTenantId()));
        payload.setActionKey(preparedAction.actionKey);
        payload.setTargetLabel(preparedAction.targetLabel);
        payload.setActionArgs(new LinkedHashMap<>(preparedAction.actionArgs));
        payload.setExpiresAt(System.currentTimeMillis() + CONFIRM_TOKEN_TTL_MILLIS);
        pendingAction.setConfirmationToken(aiConfirmationTokenService.createToken(payload));
        return pendingAction;
    }

    private SysNotice resolveNotice(Map<String, Object> arguments, AiPromptContext promptContext) {
        Long noticeId = toLong(arguments == null ? null : arguments.get("noticeId"));
        if (noticeId != null) {
            return noticeService.getById(noticeId);
        }
        String reference = trimToNull(stringValue(arguments == null ? null : arguments.get("reference")));
        AiPromptContext.NoticeSummary summary = pickNoticeSummary(reference, promptContext == null ? null : promptContext.getNoticeList());
        if (summary == null || summary.getNoticeId() == null) {
            return null;
        }
        return noticeService.getById(summary.getNoticeId());
    }

    private ResolvedTodoTarget resolveTodoTarget(Map<String, Object> arguments, AiPromptContext promptContext, boolean requireTask) {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId == null) {
            return ResolvedTodoTarget.clarify("当前登录状态已失效，请重新登录后重试。");
        }
        SysTodoTask todoTask = null;
        Long todoId = toLong(arguments == null ? null : arguments.get("todoId"));
        Long taskId = toLong(arguments == null ? null : arguments.get("taskId"));
        if (todoId != null) {
            todoTask = todoTaskMapper.selectById(todoId);
        } else if (taskId != null) {
            todoTask = todoTaskMapper.selectOne(new LambdaQueryWrapper<SysTodoTask>()
                    .eq(SysTodoTask::getTaskId, taskId)
                    .last("LIMIT 1"));
        } else {
            String reference = trimToNull(stringValue(arguments == null ? null : arguments.get("reference")));
            AiPromptContext.TodoSummary summary = pickTodoSummary(reference, promptContext == null ? null : promptContext.getTodoList());
            if (summary != null) {
                if (summary.getTodoId() != null) {
                    todoTask = todoTaskMapper.selectById(summary.getTodoId());
                } else if (summary.getTaskId() != null) {
                    todoTask = todoTaskMapper.selectOne(new LambdaQueryWrapper<SysTodoTask>()
                            .eq(SysTodoTask::getTaskId, summary.getTaskId())
                            .last("LIMIT 1"));
                }
            }
        }
        if (todoTask == null || todoTask.getTodoId() == null) {
            return ResolvedTodoTarget.clarify(requireTask ? "请说明要处理哪条流程任务或待办。" : "请说明要处理哪条待办。");
        }
        if (!Objects.equals(currentUserId, todoTask.getAssigneeUserId())) {
            return ResolvedTodoTarget.clarify("当前只能处理属于你自己的待办。");
        }
        ResolvedTodoTarget resolvedTarget = new ResolvedTodoTarget();
        resolvedTarget.todoTask = todoTask;
        resolvedTarget.targetLabel = buildTodoLabel(todoTask);
        if (!requireTask) {
            return resolvedTarget;
        }
        SysWorkflowTask workflowTask = resolveWorkflowTask(todoTask, taskId);
        if (workflowTask == null || workflowTask.getTaskId() == null) {
            return ResolvedTodoTarget.clarify("该待办当前没有可执行的流程任务，请刷新后再试。");
        }
        if (workflowTask.getAssigneeUserId() != null && !Objects.equals(currentUserId, workflowTask.getAssigneeUserId())) {
            return ResolvedTodoTarget.clarify("当前只能处理属于你自己的流程任务。");
        }
        if (TASK_TERMINAL_STATUS.contains(trimToNull(workflowTask.getStatus()))) {
            return ResolvedTodoTarget.clarify("该流程任务已经处理完成，请刷新后再试。");
        }
        resolvedTarget.workflowTask = workflowTask;
        return resolvedTarget;
    }

    private SysWorkflowTask resolveWorkflowTask(SysTodoTask todoTask, Long preferredTaskId) {
        if (preferredTaskId != null) {
            SysWorkflowTask workflowTask = workflowTaskMapper.selectById(preferredTaskId);
            if (workflowTask != null) {
                return workflowTask;
            }
        }
        if (todoTask == null) {
            return null;
        }
        if (todoTask.getTaskId() != null) {
            SysWorkflowTask workflowTask = workflowTaskMapper.selectById(todoTask.getTaskId());
            if (workflowTask != null) {
                return workflowTask;
            }
        }
        return workflowTaskMapper.selectOne(new LambdaQueryWrapper<SysWorkflowTask>()
                .eq(SysWorkflowTask::getTodoId, todoTask.getTodoId())
                .orderByDesc(SysWorkflowTask::getTaskId)
                .last("LIMIT 1"));
    }

    private ResolutionResult<WorkflowDefinitionLiteVO> resolveDefinition(Map<String, Object> arguments, AiPromptContext promptContext) {
        List<WorkflowDefinitionLiteVO> allDefinitions = internalWorkflowClient.listDefinitionLite(null, null, null).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (allDefinitions.isEmpty()) {
            return ResolutionResult.message("当前没有可发布的流程定义。");
        }

        Long definitionId = toLong(arguments == null ? null : arguments.get("definitionId"));
        if (definitionId != null) {
            WorkflowDefinitionLiteVO definition = allDefinitions.stream()
                    .filter(item -> Objects.equals(definitionId, item.getDefinitionId()))
                    .findFirst()
                    .orElse(null);
            if (definition == null) {
                return ResolutionResult.message("未找到对应的流程定义，请检查后重试。");
            }
            if (WORKFLOW_DEFINITION_STATUS_PUBLISHED.equals(trimToNull(definition.getStatus()))) {
                return ResolutionResult.message("该流程定义已经发布，无需重复操作。");
            }
            return ResolutionResult.value(definition);
        }

        List<WorkflowDefinitionLiteVO> unpublishedDefinitions = allDefinitions.stream()
                .filter(item -> !WORKFLOW_DEFINITION_STATUS_PUBLISHED.equals(trimToNull(item.getStatus())))
                .collect(Collectors.toList());
        if (unpublishedDefinitions.isEmpty()) {
            return ResolutionResult.message("当前没有可发布的流程定义。");
        }

        String processKey = trimToNull(stringValue(arguments == null ? null : arguments.get("processKey")));
        String processName = trimToNull(stringValue(arguments == null ? null : arguments.get("processName")));
        String reference = trimToNull(stringValue(arguments == null ? null : arguments.get("reference")));

        ResolutionResult<WorkflowDefinitionLiteVO> exactProcessKeyResult = matchDefinitionByExactProcessKey(processKey, unpublishedDefinitions);
        if (exactProcessKeyResult.isResolved() || StringUtils.hasText(exactProcessKeyResult.message)) {
            return exactProcessKeyResult;
        }

        ResolutionResult<WorkflowDefinitionLiteVO> exactProcessNameResult = matchDefinitionByExactProcessName(processName, unpublishedDefinitions);
        if (exactProcessNameResult.isResolved() || StringUtils.hasText(exactProcessNameResult.message)) {
            return exactProcessNameResult;
        }

        WorkflowDefinitionLiteVO indexedDefinition = pickDefinitionCandidate(reference, promptContext == null ? null : promptContext.getDefinitionCandidates());
        if (indexedDefinition != null) {
            return ResolutionResult.value(indexedDefinition);
        }

        String fuzzyTerm = StringUtils.hasText(reference) ? reference : StringUtils.hasText(processName) ? processName : processKey;
        if (!StringUtils.hasText(fuzzyTerm)) {
            return ResolutionResult.message("请补充流程标识、流程名称，或说明是第几条流程定义。");
        }
        List<WorkflowDefinitionLiteVO> fuzzyMatches = unpublishedDefinitions.stream()
                .filter(definition -> containsIgnoreCase(definition.getProcessKey(), fuzzyTerm)
                        || containsIgnoreCase(definition.getProcessName(), fuzzyTerm))
                .collect(Collectors.toList());
        if (fuzzyMatches.isEmpty()) {
            return ResolutionResult.message("未找到可发布的流程定义，请补充更具体的流程标识或名称。");
        }
        if (fuzzyMatches.size() > 1) {
            return ResolutionResult.message("找到多个可发布流程定义，请进一步说明："
                    + fuzzyMatches.stream().limit(3).map(this::buildDefinitionLabel).collect(Collectors.joining("、")));
        }
        return ResolutionResult.value(fuzzyMatches.get(0));
    }

    private ResolutionResult<WorkflowDefinitionLiteVO> matchDefinitionByExactProcessKey(String processKey, List<WorkflowDefinitionLiteVO> definitions) {
        if (!StringUtils.hasText(processKey)) {
            return ResolutionResult.empty();
        }
        List<WorkflowDefinitionLiteVO> matches = definitions.stream()
                .filter(definition -> equalsIgnoreCase(definition.getProcessKey(), processKey))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            return ResolutionResult.empty();
        }
        if (matches.size() > 1) {
            return ResolutionResult.message("同一流程标识下存在多个待发布版本，请进一步说明版本："
                    + matches.stream().limit(3).map(this::buildDefinitionLabel).collect(Collectors.joining("、")));
        }
        return ResolutionResult.value(matches.get(0));
    }

    private ResolutionResult<WorkflowDefinitionLiteVO> matchDefinitionByExactProcessName(String processName, List<WorkflowDefinitionLiteVO> definitions) {
        if (!StringUtils.hasText(processName)) {
            return ResolutionResult.empty();
        }
        List<WorkflowDefinitionLiteVO> matches = definitions.stream()
                .filter(definition -> equalsIgnoreCase(definition.getProcessName(), processName))
                .collect(Collectors.toList());
        if (matches.isEmpty()) {
            return ResolutionResult.empty();
        }
        if (matches.size() > 1) {
            return ResolutionResult.message("同名流程定义存在多个候选，请进一步说明流程标识："
                    + matches.stream().limit(3).map(this::buildDefinitionLabel).collect(Collectors.joining("、")));
        }
        return ResolutionResult.value(matches.get(0));
    }

    private AiPromptContext.NoticeSummary pickNoticeSummary(String reference, List<AiPromptContext.NoticeSummary> noticeList) {
        if (noticeList == null || noticeList.isEmpty()) {
            return null;
        }
        Integer index = resolveCandidateIndex(reference);
        if (index != null && index >= 0 && index < noticeList.size()) {
            return noticeList.get(index);
        }
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        List<AiPromptContext.NoticeSummary> matches = noticeList.stream()
                .filter(item -> containsIgnoreCase(item.getTitle(), reference)
                        || containsIgnoreCase(item.getBusinessNo(), reference)
                        || containsIgnoreCase(item.getSource(), reference))
                .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private AiPromptContext.TodoSummary pickTodoSummary(String reference, List<AiPromptContext.TodoSummary> todoList) {
        if (todoList == null || todoList.isEmpty()) {
            return null;
        }
        Integer index = resolveCandidateIndex(reference);
        if (index != null && index >= 0 && index < todoList.size()) {
            return todoList.get(index);
        }
        if (!StringUtils.hasText(reference)) {
            return null;
        }
        List<AiPromptContext.TodoSummary> matches = todoList.stream()
                .filter(item -> containsIgnoreCase(item.getProcessName(), reference)
                        || containsIgnoreCase(item.getBusinessNo(), reference)
                        || containsIgnoreCase(item.getNodeName(), reference))
                .collect(Collectors.toList());
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private WorkflowDefinitionLiteVO pickDefinitionCandidate(String reference, List<AiPromptContext.WorkflowDefinitionCandidate> definitionCandidates) {
        if (definitionCandidates == null || definitionCandidates.isEmpty()) {
            return null;
        }
        Integer index = resolveCandidateIndex(reference);
        if (index == null || index < 0 || index >= definitionCandidates.size()) {
            return null;
        }
        AiPromptContext.WorkflowDefinitionCandidate candidate = definitionCandidates.get(index);
        WorkflowDefinitionLiteVO definition = new WorkflowDefinitionLiteVO();
        definition.setDefinitionId(candidate.getDefinitionId());
        definition.setProcessKey(candidate.getProcessKey());
        definition.setProcessName(candidate.getProcessName());
        definition.setCategory(candidate.getCategory());
        definition.setVersion(candidate.getVersion());
        definition.setStatus(candidate.getStatus());
        return definition;
    }

    private Integer resolveCandidateIndex(String reference) {
        String normalizedReference = normalizeReference(reference);
        if (!StringUtils.hasText(normalizedReference)) {
            return null;
        }
        if (normalizedReference.contains("最新")) {
            return 0;
        }
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("第\\s*(\\d+)\\s*(条|个)?").matcher(normalizedReference);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1)) - 1;
        }
        Integer chineseIndex = resolveChineseOrder(normalizedReference);
        if (chineseIndex != null) {
            return chineseIndex;
        }
        if (normalizedReference.matches("\\d+")) {
            return Integer.parseInt(normalizedReference) - 1;
        }
        return null;
    }

    private Integer resolveChineseOrder(String reference) {
        Map<String, Integer> orderMap = new LinkedHashMap<>();
        orderMap.put("第一条", 0);
        orderMap.put("第一项", 0);
        orderMap.put("第一", 0);
        orderMap.put("第二条", 1);
        orderMap.put("第二项", 1);
        orderMap.put("第二", 1);
        orderMap.put("第三条", 2);
        orderMap.put("第三项", 2);
        orderMap.put("第三", 2);
        orderMap.put("第四条", 3);
        orderMap.put("第四项", 3);
        orderMap.put("第四", 3);
        orderMap.put("第五条", 4);
        orderMap.put("第五项", 4);
        orderMap.put("第五", 4);
        for (Map.Entry<String, Integer> entry : orderMap.entrySet()) {
            if (reference.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean isActionAvailable(String actionKey) {
        return listAvailableActions().stream().anyMatch(item -> Objects.equals(item.getKey(), actionKey));
    }

    private boolean isHighRiskAction(String actionKey) {
        return ACTION_TODO_FINISH.equals(actionKey)
                || ACTION_WORKFLOW_APPROVE.equals(actionKey)
                || ACTION_WORKFLOW_REJECT.equals(actionKey)
                || ACTION_WORKFLOW_DEFINITION_PUBLISH.equals(actionKey);
    }

    private String resolveAssistantMessage(AiActionResultVO result) {
        if (result == null) {
            return "操作执行失败，请稍后重试。";
        }
        if (StringUtils.hasText(result.getAssistantMessage())) {
            return result.getAssistantMessage();
        }
        return StringUtils.hasText(result.getMessage()) ? result.getMessage() : "操作执行完成。";
    }

    private AiActionResultVO buildSuccessResult(String actionKey, String targetLabel, String message, String assistantMessage) {
        AiActionResultVO result = new AiActionResultVO();
        result.setSuccess(true);
        result.setActionKey(actionKey);
        result.setTargetLabel(targetLabel);
        result.setMessage(message);
        result.setAssistantMessage(assistantMessage);
        return result;
    }

    private AiActionResultVO buildFailureResult(String actionKey, String targetLabel, String message, String assistantMessage) {
        AiActionResultVO result = new AiActionResultVO();
        result.setSuccess(false);
        result.setActionKey(actionKey);
        result.setTargetLabel(targetLabel);
        result.setMessage(message);
        result.setAssistantMessage(assistantMessage);
        return result;
    }

    private String buildTodoLabel(SysTodoTask todoTask) {
        if (todoTask == null) {
            return "待办";
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(todoTask.getProcessName())) {
            parts.add(todoTask.getProcessName().trim());
        }
        if (StringUtils.hasText(todoTask.getBusinessNo())) {
            parts.add(todoTask.getBusinessNo().trim());
        }
        if (StringUtils.hasText(todoTask.getNodeName())) {
            parts.add(todoTask.getNodeName().trim());
        }
        return parts.isEmpty() ? "待办#" + todoTask.getTodoId() : String.join(" / ", parts);
    }

    private String buildNoticeLabel(SysNotice notice) {
        if (notice == null) {
            return "系统消息";
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(notice.getTitle())) {
            parts.add(notice.getTitle().trim());
        }
        if (StringUtils.hasText(notice.getBusinessNo())) {
            parts.add(notice.getBusinessNo().trim());
        }
        return parts.isEmpty() ? "消息#" + notice.getNoticeId() : String.join(" / ", parts);
    }

    private String buildDefinitionLabel(WorkflowDefinitionLiteVO definition) {
        if (definition == null) {
            return "流程定义";
        }
        String processName = StringUtils.hasText(definition.getProcessName()) ? definition.getProcessName().trim() : "未命名流程";
        String processKey = StringUtils.hasText(definition.getProcessKey()) ? definition.getProcessKey().trim() : "-";
        String version = definition.getVersion() == null ? "-" : String.valueOf(definition.getVersion());
        return processName + " (" + processKey + ") v" + version;
    }

    private String resolveTaskLabel(SysWorkflowTask workflowTask, String fallback) {
        if (StringUtils.hasText(fallback)) {
            return fallback;
        }
        if (workflowTask == null) {
            return "流程任务";
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(workflowTask.getNodeName())) {
            parts.add(workflowTask.getNodeName().trim());
        }
        if (workflowTask.getTaskId() != null) {
            parts.add("任务#" + workflowTask.getTaskId());
        }
        return parts.isEmpty() ? "流程任务" : String.join(" / ", parts);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        String text = String.valueOf(value).trim();
        if (!StringUtils.hasText(text)) {
            return null;
        }
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String normalizeReference(String reference) {
        return StringUtils.hasText(reference)
                ? reference.trim().replace("第1条", "第一条").replace("第1个", "第一条")
                : null;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return StringUtils.hasText(left) && StringUtils.hasText(right) && left.trim().equalsIgnoreCase(right.trim());
    }

    private boolean containsIgnoreCase(String source, String term) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(term)) {
            return false;
        }
        return source.toLowerCase(Locale.ROOT).contains(term.trim().toLowerCase(Locale.ROOT));
    }

    private String resolveFriendlyMessage(Exception ex, String defaultMessage) {
        if (ex == null || !StringUtils.hasText(ex.getMessage())) {
            return defaultMessage;
        }
        return ex.getMessage().trim();
    }

    private void appendActionIfAllowed(List<AiActionDescriptor> actionList, String key, String label, String riskLevel, boolean allowed) {
        if (allowed) {
            actionList.add(new AiActionDescriptor(key, label, riskLevel));
        }
    }

    private AiToolDefinition buildTool(String name, String description, Map<String, Object> schema) {
        AiToolDefinition toolDefinition = new AiToolDefinition();
        toolDefinition.setName(name);
        toolDefinition.setDescription(description);
        toolDefinition.setParameters(schema);
        return toolDefinition;
    }

    @SafeVarargs
    private final Map<String, Object> buildSchema(Map.Entry<String, Object>... properties) {
        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        Map<String, Object> propertyMap = new LinkedHashMap<>();
        if (properties != null) {
            for (Map.Entry<String, Object> property : properties) {
                if (property != null && StringUtils.hasText(property.getKey())) {
                    propertyMap.put(property.getKey(), property.getValue());
                }
            }
        }
        schema.put("properties", propertyMap);
        schema.put("additionalProperties", Boolean.FALSE);
        return schema;
    }

    private Map.Entry<String, Object> buildStringProperty(String name, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "string");
        property.put("description", description);
        return Map.entry(name, property);
    }

    private Map.Entry<String, Object> buildIntegerProperty(String name, String description) {
        Map<String, Object> property = new LinkedHashMap<>();
        property.put("type", "integer");
        property.put("description", description);
        return Map.entry(name, property);
    }

    private boolean hasTodoClaimPermission() {
        return permissionService.hasAnyPermi("workflow:todo:claim", "workflow:todo:handle");
    }

    private boolean hasTodoFinishPermission() {
        return permissionService.hasAnyPermi("workflow:todo:finish", "workflow:todo:handle");
    }

    private boolean hasNoticeReadPermission() {
        return permissionService.hasAnyPermi("system:message:read");
    }

    private boolean hasWorkflowApprovePermission() {
        return permissionService.hasAnyPermi("workflow:todo:approve", "workflow:todo:handle");
    }

    private boolean hasWorkflowRejectPermission() {
        return permissionService.hasAnyPermi("workflow:todo:reject", "workflow:todo:handle");
    }

    private boolean hasWorkflowPublishPermission() {
        return permissionService.hasAnyPermi("workflow:definition:publish");
    }

    private static final class PreparedAction {
        private String actionKey;
        private String actionLabel;
        private String riskLevel;
        private String targetLabel;
        private String summary;
        private String clarificationMessage;
        private Map<String, Object> actionArgs = new LinkedHashMap<>();

        private static PreparedAction of(String actionKey, String actionLabel, String riskLevel) {
            PreparedAction preparedAction = new PreparedAction();
            preparedAction.actionKey = actionKey;
            preparedAction.actionLabel = actionLabel;
            preparedAction.riskLevel = riskLevel;
            return preparedAction;
        }

        private static PreparedAction clarify(String message) {
            PreparedAction preparedAction = new PreparedAction();
            preparedAction.clarificationMessage = message;
            return preparedAction;
        }
    }

    private static final class ResolvedTodoTarget {
        private SysTodoTask todoTask;
        private SysWorkflowTask workflowTask;
        private String targetLabel;
        private String clarificationMessage;

        private static ResolvedTodoTarget clarify(String message) {
            ResolvedTodoTarget target = new ResolvedTodoTarget();
            target.clarificationMessage = message;
            return target;
        }
    }

    private static final class ResolutionResult<T> {
        private T value;
        private String message;

        private static <T> ResolutionResult<T> value(T value) {
            ResolutionResult<T> result = new ResolutionResult<>();
            result.value = value;
            return result;
        }

        private static <T> ResolutionResult<T> message(String message) {
            ResolutionResult<T> result = new ResolutionResult<>();
            result.message = message;
            return result;
        }

        private static <T> ResolutionResult<T> empty() {
            return new ResolutionResult<>();
        }

        private boolean isResolved() {
            return value != null;
        }
    }
}
