package com.erp.ai.controller;

import com.erp.ai.service.AiSessionService;
import com.erp.common.core.domain.R;
import com.erp.platform.contract.model.PlatformAiMessageView;
import com.erp.platform.contract.model.PlatformAiSessionView;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 会话存档控制层。
 *
 * <p>会话归属由服务端按当前登录用户判定，前端不需要也不能指定用户。</p>
 */
@RestController
@RequestMapping("/system/ai/sessions")
public class AiSessionController {
    private final AiSessionService aiSessionService;

    public AiSessionController(AiSessionService aiSessionService) {
        this.aiSessionService = aiSessionService;
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param limit 限制条数
     * @return 会话列表
     */
    @GetMapping
    public R<List<PlatformAiSessionView>> sessions(@RequestParam(value = "limit", required = false) Integer limit) {
        return R.success(aiSessionService.listSessions(limit == null ? 0 : limit));
    }

    /**
     * 查询指定会话的消息列表。
     *
     * @param sessionId 会话ID
     * @param limit     限制条数
     * @return 消息列表
     */
    @GetMapping("/{sessionId}/messages")
    public R<List<PlatformAiMessageView>> messages(@PathVariable("sessionId") Long sessionId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return R.success(aiSessionService.listMessages(sessionId, limit == null ? 0 : limit));
    }

    /**
     * 删除会话。
     *
     * @param sessionId 会话ID
     * @return 删除结果
     */
    @DeleteMapping("/{sessionId}")
    public R<Boolean> remove(@PathVariable("sessionId") Long sessionId) {
        return R.success(aiSessionService.removeSession(sessionId));
    }

    /**
     * 重命名会话。
     *
     * @param sessionId 会话ID
     * @param title     新标题
     * @return 重命名结果
     */
    @PutMapping("/{sessionId}/title")
    public R<Boolean> rename(@PathVariable("sessionId") Long sessionId, @RequestParam("title") String title) {
        return R.success(aiSessionService.renameSession(sessionId, title));
    }
}
