package com.erp.ai.service;

import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.model.AiTokenUsage;
import com.erp.platform.contract.model.PlatformAiMessageView;
import com.erp.platform.contract.model.PlatformAiSessionView;

import java.util.List;

/**
 * AI 会话存档服务。
 *
 * <p>会话本身存放在 erp-system 的 {@code sys_ai_session} / {@code sys_ai_message} 里，
 * 本服务只负责通过内部客户端读写，并保证任何存档失败都不影响对话主流程。</p>
 */
public interface AiSessionService {

    /**
     * 记录一条用户提问，必要时新建会话。
     *
     * @param sessionId 现有会话ID，为空表示新建
     * @param content   提问内容
     * @param model     当前模型编号
     * @return 会话ID；存档关闭或失败时返回 null
     */
    Long recordUserMessage(Long sessionId, String content, String model);

    /**
     * 记录一条助手回复。
     *
     * @param sessionId 会话ID
     * @param content   回复内容
     * @param blocks    结构化区块
     * @param actionKey 关联动作编码
     * @param usage     token 用量
     */
    void recordAssistantMessage(Long sessionId,
            String content,
            List<AiStructuredBlock> blocks,
            String actionKey,
            AiTokenUsage usage);

    /**
     * 查询当前用户的会话列表。
     *
     * @param limit 限制条数
     * @return 会话列表
     */
    List<PlatformAiSessionView> listSessions(int limit);

    /**
     * 查询指定会话的消息列表。
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 消息列表
     */
    List<PlatformAiMessageView> listMessages(Long sessionId, int limit);

    /**
     * 删除会话。
     *
     * @param sessionId 会话ID
     * @return true 表示删除成功
     */
    boolean removeSession(Long sessionId);

    /**
     * 重命名会话。
     *
     * @param sessionId 会话ID
     * @param title     新标题
     * @return true 表示重命名成功
     */
    boolean renameSession(Long sessionId, String title);
}
