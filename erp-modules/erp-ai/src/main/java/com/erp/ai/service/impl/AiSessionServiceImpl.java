package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.model.AiTokenUsage;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiSessionService;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAiMessageAppendRequest;
import com.erp.platform.contract.model.PlatformAiMessageView;
import com.erp.platform.contract.model.PlatformAiSessionView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;

/**
 * AI 会话存档服务实现。
 */
@Service
public class AiSessionServiceImpl implements AiSessionService {
    private static final Logger log = LoggerFactory.getLogger(AiSessionServiceImpl.class);

    private final InternalSystemClient internalSystemClient;
    private final SecurityUserResolver securityUserResolver;
    private final ErpAiProperties erpAiProperties;
    private final ObjectMapper objectMapper;

    public AiSessionServiceImpl(InternalSystemClient internalSystemClient,
            SecurityUserResolver securityUserResolver,
            ErpAiProperties erpAiProperties,
            ObjectMapper objectMapper) {
        this.internalSystemClient = internalSystemClient;
        this.securityUserResolver = securityUserResolver;
        this.erpAiProperties = erpAiProperties;
        this.objectMapper = objectMapper;
    }

    /**
     * 记录一条用户提问，必要时新建会话。
     *
     * @param sessionId 现有会话ID
     * @param content   提问内容
     * @param model     当前模型编号
     * @return 会话ID
     */
    @Override
    public Long recordUserMessage(Long sessionId, String content, String model) {
        if (!erpAiProperties.isSessionPersistEnabled() || !StringUtils.hasText(content)) {
            return sessionId;
        }
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null) {
            return sessionId;
        }
        PlatformAiMessageAppendRequest request = new PlatformAiMessageAppendRequest();
        request.setSessionId(sessionId);
        request.setModel(model);
        request.setRole("user");
        request.setContent(content);
        try {
            PlatformAiMessageView view = internalSystemClient.appendAiSessionMessage(userId, request);
            return view == null ? sessionId : view.getSessionId();
        } catch (Exception ex) {
            // 存档失败不阻断对话，只是本轮不落库。
            log.warn("AI 会话存档失败(user message): {}", ex.getMessage());
            return sessionId;
        }
    }

    /**
     * 记录一条助手回复。
     *
     * @param sessionId 会话ID
     * @param content   回复内容
     * @param blocks    结构化区块
     * @param actionKey 关联动作编码
     * @param usage     token 用量
     */
    @Override
    public void recordAssistantMessage(Long sessionId,
            String content,
            List<AiStructuredBlock> blocks,
            String actionKey,
            AiTokenUsage usage) {
        if (!erpAiProperties.isSessionPersistEnabled() || sessionId == null) {
            return;
        }
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null || !StringUtils.hasText(content)) {
            return;
        }
        PlatformAiMessageAppendRequest request = new PlatformAiMessageAppendRequest();
        request.setSessionId(sessionId);
        request.setRole("assistant");
        request.setContent(content);
        request.setActionKey(actionKey);
        request.setBlocksJson(serializeBlocks(blocks));
        if (usage != null) {
            request.setPromptTokens(usage.getPromptTokens());
            request.setCompletionTokens(usage.getCompletionTokens());
        }
        try {
            internalSystemClient.appendAiSessionMessage(userId, request);
        } catch (Exception ex) {
            log.warn("AI 会话存档失败(assistant message): {}", ex.getMessage());
        }
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param limit 限制条数
     * @return 会话列表
     */
    @Override
    public List<PlatformAiSessionView> listSessions(int limit) {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null) {
            return Collections.emptyList();
        }
        try {
            return internalSystemClient.listAiSessions(userId, limit);
        } catch (Exception ex) {
            log.warn("查询 AI 会话列表失败: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 查询指定会话的消息列表。
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 消息列表
     */
    @Override
    public List<PlatformAiMessageView> listMessages(Long sessionId, int limit) {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null || sessionId == null) {
            return Collections.emptyList();
        }
        try {
            return internalSystemClient.listAiSessionMessages(sessionId, userId, limit);
        } catch (Exception ex) {
            log.warn("查询 AI 会话消息失败: {}", ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 删除会话。
     *
     * @param sessionId 会话ID
     * @return true 表示删除成功
     */
    @Override
    public boolean removeSession(Long sessionId) {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null || sessionId == null) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(internalSystemClient.removeAiSession(sessionId, userId));
        } catch (Exception ex) {
            log.warn("删除 AI 会话失败: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 重命名会话。
     *
     * @param sessionId 会话ID
     * @param title     新标题
     * @return true 表示重命名成功
     */
    @Override
    public boolean renameSession(Long sessionId, String title) {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null || sessionId == null || !StringUtils.hasText(title)) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(internalSystemClient.renameAiSession(sessionId, userId, title.trim()));
        } catch (Exception ex) {
            log.warn("重命名 AI 会话失败: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * 序列化结构化区块，供会话回放时重建可视化。
     *
     * @param blocks 结构化区块
     * @return JSON 字符串；无区块时返回 null
     */
    private String serializeBlocks(List<AiStructuredBlock> blocks) {
        if (blocks == null || blocks.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (Exception ex) {
            log.warn("AI 结构化区块序列化失败: {}", ex.getMessage());
            return null;
        }
    }
}
