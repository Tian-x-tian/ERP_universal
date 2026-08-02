package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.platform.contract.model.PlatformAiMessageAppendRequest;
import com.erp.platform.contract.model.PlatformAiMessageView;
import com.erp.platform.contract.model.PlatformAiSessionView;
import com.erp.system.domain.SysAiMessage;
import com.erp.system.domain.SysAiSession;
import com.erp.system.mapper.SysAiMessageMapper;
import com.erp.system.mapper.SysAiSessionMapper;
import com.erp.system.service.ISysAiSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * AI 会话服务实现。
 */
@Service
public class SysAiSessionServiceImpl extends ServiceImpl<SysAiSessionMapper, SysAiSession> implements ISysAiSessionService {
    private static final String DEL_FLAG_NORMAL = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final int DEFAULT_SESSION_LIMIT = 30;
    private static final int MAX_SESSION_LIMIT = 100;
    private static final int DEFAULT_MESSAGE_LIMIT = 100;
    private static final int MAX_MESSAGE_LIMIT = 500;
    private static final int MAX_TITLE_LENGTH = 60;
    private static final int MAX_CONTENT_LENGTH = 8000;

    private final SysAiMessageMapper messageMapper;

    public SysAiSessionServiceImpl(SysAiMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    /**
     * 查询指定用户的会话列表。
     *
     * @param tenantId 租户编号
     * @param userId   用户ID
     * @param limit    限制条数
     * @return 会话列表
     */
    @Override
    public List<PlatformAiSessionView> listSessions(String tenantId, Long userId, int limit) {
        if (!StringUtils.hasText(tenantId) || userId == null) {
            return Collections.emptyList();
        }
        int safeLimit = normalize(limit, DEFAULT_SESSION_LIMIT, MAX_SESSION_LIMIT);
        List<SysAiSession> sessions = list(new LambdaQueryWrapper<SysAiSession>()
                .eq(SysAiSession::getTenantId, tenantId.trim())
                .eq(SysAiSession::getUserId, userId)
                .eq(SysAiSession::getDelFlag, DEL_FLAG_NORMAL)
                .orderByDesc(SysAiSession::getLastMessageTime)
                .orderByDesc(SysAiSession::getSessionId)
                .last("LIMIT " + safeLimit));
        List<PlatformAiSessionView> views = new ArrayList<>();
        for (SysAiSession session : sessions) {
            views.add(toSessionView(session));
        }
        return views;
    }

    /**
     * 查询指定会话的消息列表。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 消息列表
     */
    @Override
    public List<PlatformAiMessageView> listMessages(String tenantId, Long userId, Long sessionId, int limit) {
        if (loadOwnedSession(tenantId, userId, sessionId) == null) {
            return Collections.emptyList();
        }
        int safeLimit = normalize(limit, DEFAULT_MESSAGE_LIMIT, MAX_MESSAGE_LIMIT);
        List<SysAiMessage> messages = messageMapper.selectList(new LambdaQueryWrapper<SysAiMessage>()
                .eq(SysAiMessage::getSessionId, sessionId)
                .orderByDesc(SysAiMessage::getMessageId)
                .last("LIMIT " + safeLimit));
        // 查询按倒序取最近 N 条，返回前恢复成正序，保证会话可直接回放。
        Collections.reverse(messages);
        List<PlatformAiMessageView> views = new ArrayList<>();
        for (SysAiMessage message : messages) {
            views.add(toMessageView(message));
        }
        return views;
    }

    /**
     * 追加一条会话消息，必要时新建会话。
     *
     * @param tenantId 租户编号
     * @param userId   用户ID
     * @param request  追加请求
     * @return 追加后的消息视图
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PlatformAiMessageView appendMessage(String tenantId, Long userId, PlatformAiMessageAppendRequest request) {
        if (!StringUtils.hasText(tenantId) || userId == null || request == null) {
            return null;
        }
        String role = trimToNull(request.getRole());
        if (!"user".equals(role) && !"assistant".equals(role)) {
            return null;
        }

        SysAiSession session = loadOwnedSession(tenantId, userId, request.getSessionId());
        Date now = new Date();
        if (session == null) {
            session = new SysAiSession();
            session.setTenantId(tenantId.trim());
            session.setUserId(userId);
            session.setTitle(resolveNewSessionTitle(request));
            session.setModel(limitLength(trimToNull(request.getModel()), 128));
            session.setMessageCount(0);
            session.setDelFlag(DEL_FLAG_NORMAL);
            session.setLastMessageTime(now);
            session.setCreateTime(now);
            session.setUpdateTime(now);
            save(session);
        }

        SysAiMessage message = new SysAiMessage();
        message.setSessionId(session.getSessionId());
        message.setTenantId(tenantId.trim());
        message.setUserId(userId);
        message.setRole(role);
        message.setContent(limitLength(request.getContent(), MAX_CONTENT_LENGTH));
        message.setBlocksJson(request.getBlocksJson());
        message.setActionKey(limitLength(trimToNull(request.getActionKey()), 64));
        message.setPromptTokens(request.getPromptTokens());
        message.setCompletionTokens(request.getCompletionTokens());
        message.setCreateTime(now);
        messageMapper.insert(message);

        session.setMessageCount((session.getMessageCount() == null ? 0 : session.getMessageCount()) + 1);
        session.setLastMessageTime(now);
        session.setUpdateTime(now);
        if (StringUtils.hasText(request.getModel())) {
            session.setModel(limitLength(request.getModel().trim(), 128));
        }
        updateById(session);

        return toMessageView(message);
    }

    /**
     * 逻辑删除会话。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return true 表示删除成功
     */
    @Override
    public boolean removeSession(String tenantId, Long userId, Long sessionId) {
        SysAiSession session = loadOwnedSession(tenantId, userId, sessionId);
        if (session == null) {
            return false;
        }
        session.setDelFlag(DEL_FLAG_DELETED);
        session.setUpdateTime(new Date());
        return updateById(session);
    }

    /**
     * 重命名会话。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param title     新标题
     * @return true 表示重命名成功
     */
    @Override
    public boolean renameSession(String tenantId, Long userId, Long sessionId, String title) {
        SysAiSession session = loadOwnedSession(tenantId, userId, sessionId);
        if (session == null || !StringUtils.hasText(title)) {
            return false;
        }
        session.setTitle(limitLength(title.trim(), MAX_TITLE_LENGTH));
        session.setUpdateTime(new Date());
        return updateById(session);
    }

    /**
     * 加载归属于当前用户的会话。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return 会话对象，不存在或不归属当前用户时返回 null
     */
    private SysAiSession loadOwnedSession(String tenantId, Long userId, Long sessionId) {
        if (!StringUtils.hasText(tenantId) || userId == null || sessionId == null) {
            return null;
        }
        SysAiSession session = getById(sessionId);
        if (session == null
                || !Objects.equals(userId, session.getUserId())
                || !tenantId.trim().equals(session.getTenantId())
                || DEL_FLAG_DELETED.equals(session.getDelFlag())) {
            return null;
        }
        return session;
    }

    /**
     * 由首条消息内容推导会话标题。
     *
     * @param request 追加请求
     * @return 会话标题
     */
    private String resolveNewSessionTitle(PlatformAiMessageAppendRequest request) {
        String title = trimToNull(request.getTitle());
        if (StringUtils.hasText(title)) {
            return limitLength(title, MAX_TITLE_LENGTH);
        }
        String content = trimToNull(request.getContent());
        if (StringUtils.hasText(content)) {
            return limitLength(content.replaceAll("\\s+", " "), MAX_TITLE_LENGTH);
        }
        return "新的对话";
    }

    /**
     * 转换会话视图。
     *
     * @param session 会话实体
     * @return 会话视图
     */
    private PlatformAiSessionView toSessionView(SysAiSession session) {
        PlatformAiSessionView view = new PlatformAiSessionView();
        view.setSessionId(session.getSessionId());
        view.setTenantId(session.getTenantId());
        view.setUserId(session.getUserId());
        view.setTitle(session.getTitle());
        view.setModel(session.getModel());
        view.setMessageCount(session.getMessageCount());
        view.setLastMessageTime(session.getLastMessageTime());
        view.setCreateTime(session.getCreateTime());
        return view;
    }

    /**
     * 转换消息视图。
     *
     * @param message 消息实体
     * @return 消息视图
     */
    private PlatformAiMessageView toMessageView(SysAiMessage message) {
        PlatformAiMessageView view = new PlatformAiMessageView();
        view.setMessageId(message.getMessageId());
        view.setSessionId(message.getSessionId());
        view.setRole(message.getRole());
        view.setContent(message.getContent());
        view.setBlocksJson(message.getBlocksJson());
        view.setActionKey(message.getActionKey());
        view.setPromptTokens(message.getPromptTokens());
        view.setCompletionTokens(message.getCompletionTokens());
        view.setCreateTime(message.getCreateTime());
        return view;
    }

    /**
     * 规范化查询条数。
     *
     * @param limit        原始条数
     * @param defaultLimit 默认条数
     * @param maxLimit     最大条数
     * @return 规范化条数
     */
    private int normalize(int limit, int defaultLimit, int maxLimit) {
        if (limit <= 0) {
            return defaultLimit;
        }
        return Math.min(limit, maxLimit);
    }

    /**
     * 限制文本长度。
     *
     * @param value     原始文本
     * @param maxLength 最大长度
     * @return 限制后的文本
     */
    private String limitLength(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * 去除空白并将空字符串转为 null。
     *
     * @param value 原始文本
     * @return 规范化文本
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
