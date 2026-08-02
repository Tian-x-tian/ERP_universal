package com.erp.system.controller;

import com.erp.platform.contract.model.PlatformAiBriefSaveRequest;
import com.erp.platform.contract.model.PlatformAiBriefView;
import com.erp.platform.contract.model.PlatformAiDataSet;
import com.erp.platform.contract.model.PlatformAiDataSetRequest;
import com.erp.platform.contract.model.PlatformAiMessageAppendRequest;
import com.erp.platform.contract.model.PlatformAiMessageView;
import com.erp.platform.contract.model.PlatformAiQuotaUsage;
import com.erp.platform.contract.model.PlatformAiSessionView;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IAiDatasetService;
import com.erp.system.service.ISysAiBriefService;
import com.erp.system.service.ISysAiSessionService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 平台域 AI 内部契约控制层（只读数据集 + 会话存档）。
 *
 * <p>会话相关接口全部以“调用方身份”为准做归属校验，erp-ai 通过内部认证头透传真实登录用户。</p>
 */
@RestController
@RequestMapping("/system/internal/ai")
public class SystemAiInternalController {
    private final IAiDatasetService aiDatasetService;
    private final ISysAiSessionService aiSessionService;
    private final ISysAiBriefService aiBriefService;
    private final SecurityUserResolver securityUserResolver;

    public SystemAiInternalController(IAiDatasetService aiDatasetService,
            ISysAiSessionService aiSessionService,
            ISysAiBriefService aiBriefService,
            SecurityUserResolver securityUserResolver) {
        this.aiDatasetService = aiDatasetService;
        this.aiSessionService = aiSessionService;
        this.aiBriefService = aiBriefService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 执行只读数据集查询。
     *
     * @param request 数据集请求
     * @return 数据集结果
     */
    @PostMapping("/dataset")
    public PlatformAiDataSet dataset(@RequestBody PlatformAiDataSetRequest request) {
        return aiDatasetService.query(request);
    }

    /**
     * 列出本模块支持的数据集编码。
     *
     * @return 数据集编码列表
     */
    @GetMapping("/dataset/keys")
    public List<String> datasetKeys() {
        return aiDatasetService.supportedKeys();
    }

    /**
     * 查询当日简报。
     *
     * @param userId    用户ID，为空时取当前登录用户
     * @param briefType 简报类型
     * @return 简报视图
     */
    @GetMapping("/brief")
    public PlatformAiBriefView brief(@RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "briefType", required = false) String briefType) {
        return aiBriefService.getToday(securityUserResolver.getCurrentTenantId(), resolveUserId(userId), briefType);
    }

    /**
     * 抢占当日简报生成权。
     *
     * @param userId       用户ID，为空时取当前登录用户
     * @param briefType    简报类型
     * @param staleMinutes 生成中状态的陈旧判定分钟数
     * @return true 表示抢占成功
     */
    @PostMapping("/brief/claim")
    public Boolean claimBrief(@RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "briefType", required = false) String briefType,
            @RequestParam(value = "staleMinutes", required = false) Integer staleMinutes) {
        return aiBriefService.claimToday(securityUserResolver.getCurrentTenantId(),
                resolveUserId(userId),
                briefType,
                staleMinutes == null ? 5 : staleMinutes);
    }

    /**
     * 回写简报生成结果。
     *
     * @param userId  用户ID，为空时取当前登录用户
     * @param request 回写请求
     * @return 回写后的简报视图
     */
    @PostMapping("/brief")
    public PlatformAiBriefView saveBrief(@RequestParam(value = "userId", required = false) Long userId,
            @RequestBody PlatformAiBriefSaveRequest request) {
        return aiBriefService.saveResult(securityUserResolver.getCurrentTenantId(), resolveUserId(userId), request);
    }

    /**
     * 查询当日 AI 用量，用于配额判定。
     *
     * @param userId 用户ID，为空时取当前登录用户
     * @return 用量统计
     */
    @GetMapping("/quota/usage")
    public PlatformAiQuotaUsage quotaUsage(@RequestParam(value = "userId", required = false) Long userId) {
        return aiDatasetService.queryQuotaUsage(resolveUserId(userId));
    }

    /**
     * 查询当前用户的会话列表。
     *
     * @param userId 用户ID，为空时取当前登录用户
     * @param limit  限制条数
     * @return 会话列表
     */
    @GetMapping("/sessions")
    public List<PlatformAiSessionView> sessions(@RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return aiSessionService.listSessions(securityUserResolver.getCurrentTenantId(),
                resolveUserId(userId),
                limit == null ? 0 : limit);
    }

    /**
     * 查询指定会话的消息列表。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID，为空时取当前登录用户
     * @param limit     限制条数
     * @return 消息列表
     */
    @GetMapping("/sessions/{sessionId}/messages")
    public List<PlatformAiMessageView> sessionMessages(@PathVariable("sessionId") Long sessionId,
            @RequestParam(value = "userId", required = false) Long userId,
            @RequestParam(value = "limit", required = false) Integer limit) {
        return aiSessionService.listMessages(securityUserResolver.getCurrentTenantId(),
                resolveUserId(userId),
                sessionId,
                limit == null ? 0 : limit);
    }

    /**
     * 追加一条会话消息。
     *
     * @param userId  用户ID，为空时取当前登录用户
     * @param request 追加请求
     * @return 追加后的消息视图
     */
    @PostMapping("/sessions/messages")
    public PlatformAiMessageView appendMessage(@RequestParam(value = "userId", required = false) Long userId,
            @RequestBody PlatformAiMessageAppendRequest request) {
        return aiSessionService.appendMessage(securityUserResolver.getCurrentTenantId(),
                resolveUserId(userId),
                request);
    }

    /**
     * 逻辑删除会话。
     *
     * @param sessionId 会话ID
     * @param userId    用户ID，为空时取当前登录用户
     * @return true 表示删除成功
     */
    @DeleteMapping("/sessions/{sessionId}")
    public Boolean removeSession(@PathVariable("sessionId") Long sessionId,
            @RequestParam(value = "userId", required = false) Long userId) {
        return aiSessionService.removeSession(securityUserResolver.getCurrentTenantId(),
                resolveUserId(userId),
                sessionId);
    }

    /**
     * 重命名会话。
     *
     * @param sessionId 会话ID
     * @param title     新标题
     * @param userId    用户ID，为空时取当前登录用户
     * @return true 表示重命名成功
     */
    @PutMapping("/sessions/{sessionId}/title")
    public Boolean renameSession(@PathVariable("sessionId") Long sessionId,
            @RequestParam("title") String title,
            @RequestParam(value = "userId", required = false) Long userId) {
        return aiSessionService.renameSession(securityUserResolver.getCurrentTenantId(),
                resolveUserId(userId),
                sessionId,
                title);
    }

    /**
     * 解析目标用户ID，显式入参优先，其次取当前登录用户。
     *
     * @param userId 显式传入的用户ID
     * @return 目标用户ID
     */
    private Long resolveUserId(Long userId) {
        return userId != null ? userId : securityUserResolver.getCurrentUserId();
    }
}
