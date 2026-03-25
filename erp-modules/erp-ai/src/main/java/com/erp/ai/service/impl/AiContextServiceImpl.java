package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiPageContext;
import com.erp.ai.model.AiPromptContext;
import com.erp.ai.service.AiContextService;
import com.erp.platform.contract.model.PlatformNoticeView;
import com.erp.platform.contract.model.PlatformUserView;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.contract.domain.vo.WorkflowDefinitionLiteVO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 上下文聚合服务实现。
 */
@Service
public class AiContextServiceImpl implements AiContextService {
    private static final String TODO_STATUS_PENDING = "0";
    private static final String TODO_STATUS_PROCESSING = "1";
    private static final String NOTICE_STATUS_UNREAD = "0";
    private static final String DEFINITION_STATUS_PUBLISHED = "1";
    private static final int MAX_DEFINITION_CANDIDATES = 20;

    private final SecurityUserResolver securityUserResolver;
    private final InternalSystemClient internalSystemClient;
    private final InternalWorkflowClient internalWorkflowClient;
    private final ErpAiProperties erpAiProperties;

    public AiContextServiceImpl(SecurityUserResolver securityUserResolver,
            InternalSystemClient internalSystemClient,
            InternalWorkflowClient internalWorkflowClient,
            ErpAiProperties erpAiProperties) {
        this.securityUserResolver = securityUserResolver;
        this.internalSystemClient = internalSystemClient;
        this.internalWorkflowClient = internalWorkflowClient;
        this.erpAiProperties = erpAiProperties;
    }

    /**
     * 聚合当前登录用户的 AI 提示词上下文。
     *
     * @param request 前端对话请求
     * @return 聚合后的提示词上下文
     */
    @Override
    public AiPromptContext buildPromptContext(AiChatRequest request) {
        PlatformUserView currentUser = resolveCurrentUser();
        if (currentUser == null || currentUser.getUserId() == null) {
            throw new IllegalStateException("当前登录用户不存在，请重新登录后重试");
        }

        AiPromptContext promptContext = new AiPromptContext();
        promptContext.setCurrentUser(buildCurrentUserContext(currentUser));
        promptContext.setPageContext(normalizePageContext(request == null ? null : request.getPageContext()));
        fillTodoContext(promptContext, currentUser.getUserId());
        fillNoticeContext(promptContext, currentUser.getUserId());
        fillDefinitionCandidates(promptContext);
        return promptContext;
    }

    /**
     * 解析当前登录用户。
     *
     * @return 当前登录用户
     */
    private PlatformUserView resolveCurrentUser() {
        Long currentUserId = securityUserResolver.getCurrentUserId();
        if (currentUserId != null) {
            PlatformUserView currentUser = internalSystemClient.getUserById(currentUserId);
            if (currentUser != null) {
                return currentUser;
            }
        }
        String currentUserName = securityUserResolver.getCurrentUsername();
        if (!StringUtils.hasText(currentUserName)) {
            return null;
        }
        return internalSystemClient.getActiveUserByUsername("", currentUserName.trim());
    }

    /**
     * 构造当前用户提示词上下文。
     *
     * @param currentUser 当前登录用户
     * @return 用户上下文
     */
    private AiPromptContext.CurrentUserContext buildCurrentUserContext(PlatformUserView currentUser) {
        AiPromptContext.CurrentUserContext userContext = new AiPromptContext.CurrentUserContext();
        userContext.setUserId(currentUser.getUserId());
        userContext.setUserName(trimToNull(currentUser.getUserName()));
        userContext.setNickName(trimToNull(currentUser.getNickName()));
        userContext.setTenantId(trimToNull(currentUser.getTenantId()));
        return userContext;
    }

    /**
     * 规范化页面上下文。
     *
     * @param pageContext 原始页面上下文
     * @return 规范化后的页面上下文
     */
    private AiPageContext normalizePageContext(AiPageContext pageContext) {
        AiPageContext normalizedPageContext = new AiPageContext();
        normalizedPageContext.setPath(trimToNull(pageContext == null ? null : pageContext.getPath()));
        normalizedPageContext.setTitle(trimToNull(pageContext == null ? null : pageContext.getTitle()));
        return normalizedPageContext;
    }

    /**
     * 装载待办上下文。
     *
     * @param promptContext 提示词上下文
     * @param userId        当前用户ID
     */
    private void fillTodoContext(AiPromptContext promptContext, Long userId) {
        promptContext.setTodoContextAvailable(false);
        promptContext.setTodoContextMessage("当前未读取到待办上下文");
        promptContext.setTodoCount(0);
        promptContext.setTodoList(Collections.emptyList());
        if (userId == null) {
            return;
        }
        try {
            Long todoCount = internalWorkflowClient.countPendingTodos(userId);

            int maxTodoItems = Math.max(1, erpAiProperties.getMaxTodoItems());
            List<AiPromptContext.TodoSummary> todoSummaryList = internalWorkflowClient.getPendingTodos(userId, maxTodoItems)
                    .stream()
                    .map(this::buildTodoSummary)
                    .collect(Collectors.toList());

            promptContext.setTodoContextAvailable(true);
            promptContext.setTodoContextMessage("待办上下文读取成功");
            promptContext.setTodoCount(todoCount == null ? 0 : todoCount.intValue());
            promptContext.setTodoList(todoSummaryList);
        } catch (Exception ex) {
            promptContext.setTodoContextAvailable(false);
            promptContext.setTodoContextMessage("当前暂无法读取待办上下文");
            promptContext.setTodoCount(0);
            promptContext.setTodoList(Collections.emptyList());
        }
    }

    /**
     * 构造待办摘要。
     *
     * @param todoTask 待办对象
     * @return 待办摘要
     */
    private AiPromptContext.TodoSummary buildTodoSummary(SysTodoTask todoTask) {
        AiPromptContext.TodoSummary todoSummary = new AiPromptContext.TodoSummary();
        todoSummary.setTodoId(todoTask.getTodoId());
        todoSummary.setTaskId(todoTask.getTaskId());
        todoSummary.setProcessName(trimToNull(todoTask.getProcessName()));
        todoSummary.setNodeName(trimToNull(todoTask.getNodeName()));
        todoSummary.setBusinessNo(trimToNull(todoTask.getBusinessNo()));
        todoSummary.setPriority(trimToNull(todoTask.getPriority()));
        todoSummary.setStatus(trimToNull(todoTask.getStatus()));
        todoSummary.setDueTime(todoTask.getDueTime());
        todoSummary.setCreateTime(todoTask.getCreateTime());
        return todoSummary;
    }

    /**
     * 装载消息上下文。
     *
     * @param promptContext 提示词上下文
     * @param userId        当前用户ID
     */
    private void fillNoticeContext(AiPromptContext promptContext, Long userId) {
        promptContext.setNoticeContextAvailable(false);
        promptContext.setNoticeContextMessage("当前未读取到消息上下文");
        promptContext.setUnreadNoticeCount(0);
        promptContext.setNoticeList(Collections.emptyList());
        if (userId == null) {
            return;
        }
        try {
            Long unreadNoticeCount = internalSystemClient.countUnreadNotices(userId);

            int maxNoticeItems = Math.max(1, erpAiProperties.getMaxNoticeItems());
            List<AiPromptContext.NoticeSummary> noticeSummaryList = internalSystemClient.getLatestNotices(userId, maxNoticeItems)
                    .stream()
                    .map(this::buildNoticeSummary)
                    .collect(Collectors.toList());

            promptContext.setNoticeContextAvailable(true);
            promptContext.setNoticeContextMessage("消息上下文读取成功");
            promptContext.setUnreadNoticeCount(unreadNoticeCount == null ? 0 : unreadNoticeCount.intValue());
            promptContext.setNoticeList(noticeSummaryList);
        } catch (Exception ex) {
            promptContext.setNoticeContextAvailable(false);
            promptContext.setNoticeContextMessage("当前暂无法读取系统消息上下文");
            promptContext.setUnreadNoticeCount(0);
            promptContext.setNoticeList(Collections.emptyList());
        }
    }

    /**
     * 构造消息摘要。
     *
     * @param notice 系统消息对象
     * @return 消息摘要
     */
    private AiPromptContext.NoticeSummary buildNoticeSummary(PlatformNoticeView notice) {
        AiPromptContext.NoticeSummary noticeSummary = new AiPromptContext.NoticeSummary();
        noticeSummary.setNoticeId(notice.getNoticeId());
        noticeSummary.setTitle(trimToNull(notice.getTitle()));
        noticeSummary.setNoticeType(trimToNull(notice.getNoticeType()));
        noticeSummary.setSource(trimToNull(notice.getSource()));
        noticeSummary.setBusinessNo(trimToNull(notice.getBusinessNo()));
        noticeSummary.setStatus(trimToNull(notice.getStatus()));
        noticeSummary.setDeliveryStatus(trimToNull(notice.getDeliveryStatus()));
        noticeSummary.setCreateTime(notice.getCreateTime());
        return noticeSummary;
    }

    /**
     * 装载未发布流程定义候选，供 AI 解析发布动作使用。
     *
     * @param promptContext 提示词上下文
     */
    private void fillDefinitionCandidates(AiPromptContext promptContext) {
        promptContext.setDefinitionCandidates(Collections.emptyList());
        try {
            List<WorkflowDefinitionLiteVO> definitionList = internalWorkflowClient.listDefinitionLite(null, null, null);
            if (definitionList == null || definitionList.isEmpty()) {
                return;
            }
            List<AiPromptContext.WorkflowDefinitionCandidate> candidateList = definitionList.stream()
                    .filter(definition -> definition != null && definition.getDefinitionId() != null)
                    .filter(definition -> !DEFINITION_STATUS_PUBLISHED.equals(trimToNull(definition.getStatus())))
                    .limit(MAX_DEFINITION_CANDIDATES)
                    .map(this::buildDefinitionCandidate)
                    .collect(Collectors.toList());
            promptContext.setDefinitionCandidates(candidateList);
        } catch (Exception ignored) {
            promptContext.setDefinitionCandidates(Collections.emptyList());
        }
    }

    /**
     * 构造流程定义候选对象。
     *
     * @param definition 流程定义轻量对象
     * @return 候选对象
     */
    private AiPromptContext.WorkflowDefinitionCandidate buildDefinitionCandidate(WorkflowDefinitionLiteVO definition) {
        AiPromptContext.WorkflowDefinitionCandidate candidate = new AiPromptContext.WorkflowDefinitionCandidate();
        candidate.setDefinitionId(definition.getDefinitionId());
        candidate.setProcessKey(trimToNull(definition.getProcessKey()));
        candidate.setProcessName(trimToNull(definition.getProcessName()));
        candidate.setCategory(trimToNull(definition.getCategory()));
        candidate.setVersion(definition.getVersion());
        candidate.setStatus(trimToNull(definition.getStatus()));
        return candidate;
    }

    /**
     * 去除空白并将空字符串转换为 null。
     *
     * @param value 原始值
     * @return 处理后的值
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
