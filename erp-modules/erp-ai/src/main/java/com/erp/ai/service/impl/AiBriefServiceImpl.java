package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiChatMessage;
import com.erp.ai.model.AiModelCompletion;
import com.erp.ai.model.AiReadToolResult;
import com.erp.ai.model.AiRuntimeConfig;
import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiBriefService;
import com.erp.ai.service.AiModelClient;
import com.erp.ai.service.AiReadToolService;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAiBriefSaveRequest;
import com.erp.platform.contract.model.PlatformAiBriefView;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * AI 每日简报服务实现。
 *
 * <p>生成必须跑在「真实用户身份」下：只读工具要按该用户的 RBAC 判权，userId 也由服务端从
 * 当前登录态注入。因此这里只做「用户首次打开时异步生成 + 全天缓存」，没有做跨用户的定时预热
 * —— 定时线程上没有任何用户身份，内部调用会降级成合成服务账号，取到的数据是错的。
 * 若要做预热，需要先决定是否允许 erp-ai 为任意用户签发身份头，那是一个独立的安全决策。</p>
 */
@Service
public class AiBriefServiceImpl implements AiBriefService {
    private static final Logger log = LoggerFactory.getLogger(AiBriefServiceImpl.class);

    private static final String BRIEF_TYPE_DAILY = "daily";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_READY = "READY";
    private static final String STATUS_FAILED = "FAILED";
    private static final int CLAIM_STALE_MINUTES = 5;

    /** 简报固定取这几份只读数据，顺序即面板上的展示顺序 */
    private static final List<String> BRIEF_TOOLS = List.of(
            "query_todo_backlog", "query_todo_aging", "query_notice_overview");

    private final InternalSystemClient internalSystemClient;
    private final AiReadToolService aiReadToolService;
    private final AiModelClient aiModelClient;
    private final AiTenantConfigService aiTenantConfigService;
    private final SecurityUserResolver securityUserResolver;
    private final ErpAiProperties erpAiProperties;
    private final ObjectMapper objectMapper;
    private final Executor aiStreamingExecutor;

    public AiBriefServiceImpl(InternalSystemClient internalSystemClient,
            AiReadToolService aiReadToolService,
            AiModelClient aiModelClient,
            AiTenantConfigService aiTenantConfigService,
            SecurityUserResolver securityUserResolver,
            ErpAiProperties erpAiProperties,
            ObjectMapper objectMapper,
            @Qualifier("aiStreamingExecutor") Executor aiStreamingExecutor) {
        this.internalSystemClient = internalSystemClient;
        this.aiReadToolService = aiReadToolService;
        this.aiModelClient = aiModelClient;
        this.aiTenantConfigService = aiTenantConfigService;
        this.securityUserResolver = securityUserResolver;
        this.erpAiProperties = erpAiProperties;
        this.objectMapper = objectMapper;
        this.aiStreamingExecutor = aiStreamingExecutor;
    }

    /**
     * 获取当日简报；缺失或过期时触发一次后台生成。
     *
     * @return 简报视图
     */
    @Override
    public PlatformAiBriefView getOrTrigger() {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        PlatformAiBriefView brief = loadBrief(userId);
        if (brief != null && STATUS_READY.equals(brief.getStatus()) && !isExpired(brief)) {
            return brief;
        }
        if (brief != null && STATUS_PENDING.equals(brief.getStatus()) && !isClaimStale(brief)) {
            // 已有实例在生成，直接返回生成中，避免重复打模型。
            return brief;
        }
        triggerAsyncGeneration(userId, false);
        return loadBrief(userId);
    }

    /**
     * 强制重新生成当日简报。
     *
     * @return 简报视图
     */
    @Override
    public PlatformAiBriefView refresh() {
        Long userId = securityUserResolver.getCurrentUserId();
        if (userId == null) {
            return null;
        }
        triggerAsyncGeneration(userId, true);
        return loadBrief(userId);
    }

    /**
     * 抢占生成权后异步生成，抢不到说明别的实例正在做。
     *
     * @param userId 目标用户ID
     * @param force  是否强制重算
     */
    private void triggerAsyncGeneration(Long userId, boolean force) {
        if (!force) {
            PlatformAiBriefView existing = loadBrief(userId);
            if (existing != null && STATUS_READY.equals(existing.getStatus()) && !isExpired(existing)) {
                return;
            }
        }
        if (!Boolean.TRUE.equals(claim(userId))) {
            return;
        }
        try {
            aiStreamingExecutor.execute(() -> doGenerate(userId));
        } catch (RejectedExecutionException ex) {
            log.warn("AI 简报生成任务被拒绝，线程池已满：userId={}", userId);
            saveFailure(userId, "当前生成任务较多，请稍后刷新重试。");
        }
    }

    /**
     * 抢占当日简报生成权。
     *
     * @param userId 目标用户ID
     * @return true 表示抢占成功
     */
    private Boolean claim(Long userId) {
        try {
            return internalSystemClient.claimAiBrief(userId, BRIEF_TYPE_DAILY, CLAIM_STALE_MINUTES);
        } catch (Exception ex) {
            log.warn("抢占 AI 简报生成权失败：{}", ex.getMessage());
            return Boolean.FALSE;
        }
    }

    /**
     * 实际生成简报：先取数，再让模型写一段解读。
     *
     * @param userId 目标用户ID
     */
    private void doGenerate(Long userId) {
        long startTime = System.currentTimeMillis();
        try {
            AiRuntimeConfig runtimeConfig = aiTenantConfigService.resolveRuntimeConfig();
            List<AiStructuredBlock> blocks = new ArrayList<>();
            StringBuilder factBuilder = new StringBuilder();

            for (String toolName : BRIEF_TOOLS) {
                Map<String, Object> params = new LinkedHashMap<>();
                params.put("scope", "mine");
                AiReadToolResult result = aiReadToolService.execute(toolName, params);
                if (!result.isSuccess()) {
                    continue;
                }
                if (result.getBlock() != null) {
                    blocks.add(result.getBlock());
                }
                factBuilder.append(result.getModelText()).append("\n\n");
            }

            if (blocks.isEmpty()) {
                // 一份数据都取不到时不写 READY，否则面板会长期展示一份空简报。
                saveFailure(userId, "当前没有可用于生成简报的数据，请确认权限后重试。");
                return;
            }

            String summary = requestSummary(runtimeConfig.getModel(), factBuilder.toString());
            PlatformAiBriefSaveRequest request = new PlatformAiBriefSaveRequest();
            request.setBriefType(BRIEF_TYPE_DAILY);
            request.setStatus(STATUS_READY);
            request.setSummary(summary);
            request.setBlocksJson(serializeBlocks(blocks));
            request.setModel(runtimeConfig.getModel());
            request.setGenerateMs(System.currentTimeMillis() - startTime);
            internalSystemClient.saveAiBrief(userId, request);
        } catch (Exception ex) {
            log.warn("生成 AI 简报失败：userId={}, reason={}", userId, ex.getMessage());
            saveFailure(userId, "简报生成失败，请稍后刷新重试。");
        }
    }

    /**
     * 让模型基于取数结果写一段可读的解读。
     *
     * <p>模型不可用时退回一段确定性提示，简报本身的指标与表格仍然可用。</p>
     *
     * @param model 模型编号
     * @param facts 取数事实
     * @return 解读文本
     */
    private String requestSummary(String model, String facts) {
        List<AiChatMessage> messages = new ArrayList<>();
        messages.add(new AiChatMessage("system",
                "你是 ERP 系统的工作简报助手。根据给出的数据事实写一段今日工作简报。要求：\n"
                        + "1. 使用简体中文，控制在 150 字以内。\n"
                        + "2. 先点出最需要优先处理的一到两件事，再补充整体状况。\n"
                        + "3. 只能使用给出的数据，不要编造数字。\n"
                        + "4. 不要输出表格或 Markdown 标题，直接写成一段话或极短的列表。"));
        messages.add(new AiChatMessage("user", "数据事实：\n" + facts));
        try {
            AiModelCompletion completion = aiModelClient.completeChat(model, messages, Collections.emptyList());
            String content = completion == null ? null : completion.getContent();
            if (StringUtils.hasText(content)) {
                return content.trim();
            }
        } catch (Exception ex) {
            log.warn("AI 简报解读生成失败，退回确定性文案：{}", ex.getMessage());
        }
        return "以下是你今天的工作数据概览，模型解读暂不可用，可直接查看下方指标与明细。";
    }

    /**
     * 回写失败状态。
     *
     * @param userId  目标用户ID
     * @param message 失败提示
     */
    private void saveFailure(Long userId, String message) {
        try {
            PlatformAiBriefSaveRequest request = new PlatformAiBriefSaveRequest();
            request.setBriefType(BRIEF_TYPE_DAILY);
            request.setStatus(STATUS_FAILED);
            request.setSummary(message);
            internalSystemClient.saveAiBrief(userId, request);
        } catch (Exception ex) {
            log.warn("回写 AI 简报失败状态失败：{}", ex.getMessage());
        }
    }

    /**
     * 读取当日简报。
     *
     * @param userId 目标用户ID
     * @return 简报视图
     */
    private PlatformAiBriefView loadBrief(Long userId) {
        try {
            return internalSystemClient.getAiBrief(userId, BRIEF_TYPE_DAILY);
        } catch (Exception ex) {
            log.warn("查询 AI 简报失败：{}", ex.getMessage());
            return null;
        }
    }

    /**
     * 判断简报是否已过缓存有效期。
     *
     * @param brief 简报视图
     * @return true 表示已过期
     */
    private boolean isExpired(PlatformAiBriefView brief) {
        Date updateTime = brief.getUpdateTime();
        if (updateTime == null) {
            return true;
        }
        long ttlMillis = Math.max(1, erpAiProperties.getBriefTtlMinutes()) * 60_000L;
        return System.currentTimeMillis() - updateTime.getTime() > ttlMillis;
    }

    /**
     * 判断生成中状态是否已陈旧（上一个生成方大概率已经挂了）。
     *
     * @param brief 简报视图
     * @return true 表示可以重新抢占
     */
    private boolean isClaimStale(PlatformAiBriefView brief) {
        Date updateTime = brief.getUpdateTime();
        if (updateTime == null) {
            return true;
        }
        return System.currentTimeMillis() - updateTime.getTime() > CLAIM_STALE_MINUTES * 60_000L;
    }

    /**
     * 序列化结构化区块。
     *
     * @param blocks 区块列表
     * @return JSON 字符串
     */
    private String serializeBlocks(List<AiStructuredBlock> blocks) {
        try {
            return objectMapper.writeValueAsString(blocks);
        } catch (Exception ex) {
            log.warn("AI 简报区块序列化失败：{}", ex.getMessage());
            return null;
        }
    }
}
