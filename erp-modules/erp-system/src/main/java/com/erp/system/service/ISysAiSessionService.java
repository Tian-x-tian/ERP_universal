package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.platform.contract.model.PlatformAiMessageAppendRequest;
import com.erp.platform.contract.model.PlatformAiMessageView;
import com.erp.platform.contract.model.PlatformAiSessionView;
import com.erp.system.domain.SysAiSession;

import java.util.List;

/**
 * AI 会话服务。
 */
public interface ISysAiSessionService extends IService<SysAiSession> {

    /**
     * 查询指定用户的会话列表。
     *
     * @param tenantId 租户编号
     * @param userId   用户ID
     * @param limit    限制条数
     * @return 会话列表
     */
    List<PlatformAiSessionView> listSessions(String tenantId, Long userId, int limit);

    /**
     * 查询指定会话的消息列表。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 消息列表
     */
    List<PlatformAiMessageView> listMessages(String tenantId, Long userId, Long sessionId, int limit);

    /**
     * 追加一条会话消息，必要时新建会话。
     *
     * @param tenantId 租户编号
     * @param userId   用户ID
     * @param request  追加请求
     * @return 追加后的消息视图（含所属会话ID）
     */
    PlatformAiMessageView appendMessage(String tenantId, Long userId, PlatformAiMessageAppendRequest request);

    /**
     * 逻辑删除会话。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @return true 表示删除成功
     */
    boolean removeSession(String tenantId, Long userId, Long sessionId);

    /**
     * 重命名会话。
     *
     * @param tenantId  租户编号
     * @param userId    用户ID
     * @param sessionId 会话ID
     * @param title     新标题
     * @return true 表示重命名成功
     */
    boolean renameSession(String tenantId, Long userId, Long sessionId, String title);
}
