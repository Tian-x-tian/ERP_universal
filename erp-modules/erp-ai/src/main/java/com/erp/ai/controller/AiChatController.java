package com.erp.ai.controller;

import com.erp.ai.config.ErpAiProperties;
import com.erp.common.core.domain.R;
import com.erp.ai.model.AiActionConfirmRequest;
import com.erp.ai.model.AiActionResultVO;
import com.erp.ai.model.AiPendingAction;
import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiMetaVO;
import com.erp.ai.model.AiStructuredBlock;
import com.erp.ai.service.AiChatService;
import com.erp.ai.service.AiStreamListener;
import com.erp.ai.service.impl.AiSaasFeatureGuard;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * ERP AI 对话控制层。
 */
@RestController
@RequestMapping("/system/ai")
public class AiChatController {
    private static final Logger log = LoggerFactory.getLogger(AiChatController.class);

    private final AiChatService aiChatService;
    private final Executor aiStreamingExecutor;
    private final AiSaasFeatureGuard featureGuard;
    private final ErpAiProperties erpAiProperties;

    public AiChatController(AiChatService aiChatService,
            @Qualifier("aiStreamingExecutor") Executor aiStreamingExecutor,
            AiSaasFeatureGuard featureGuard,
            ErpAiProperties erpAiProperties) {
        this.aiChatService = aiChatService;
        this.aiStreamingExecutor = aiStreamingExecutor;
        this.featureGuard = featureGuard;
        this.erpAiProperties = erpAiProperties;
    }

    /**
     * 查询 AI 元信息。
     *
     * @return AI 元信息
     */
    @GetMapping("/meta")
    public R<AiMetaVO> meta() {
        return R.success(aiChatService.getMeta());
    }

    /**
     * 执行 AI 流式对话。
     *
     * @param request  AI 对话请求
     * @param response HTTP 响应对象
     * @return SSE 输出对象
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody(required = false) AiChatRequest request, HttpServletResponse response) {
        featureGuard.requirePaidAccess();
        if (response != null) {
            response.setHeader("Cache-Control", "no-cache");
            response.setHeader("X-Accel-Buffering", "no");
        }
        // 保留有限超时作为兜底：上游模型卡死时，永不超时的连接会一直占用线程池。
        SseEmitter emitter = new SseEmitter(Math.max(0L, erpAiProperties.getSseTimeoutMs()));
        SseEmitterStreamListener listener = new SseEmitterStreamListener(emitter);
        emitter.onTimeout(() -> {
            listener.markClosed();
            emitter.complete();
        });
        emitter.onError(throwable -> listener.markClosed());
        emitter.onCompletion(listener::markClosed);

        try {
            aiStreamingExecutor.execute(() -> aiChatService.streamChat(request, listener));
        } catch (RejectedExecutionException ex) {
            log.warn("AI 流式任务被拒绝，当前并发已达上限");
            listener.onError("AI 助手当前繁忙，请稍后重试");
        }
        return emitter;
    }

    /**
     * 确认执行高风险动作。
     *
     * @param request 确认请求
     * @return 动作执行结果
     */
    @PostMapping("/action/confirm")
    public R<AiActionResultVO> confirmAction(@RequestBody(required = false) AiActionConfirmRequest request) {
        featureGuard.requirePaidAccess();
        return R.success(aiChatService.confirmAction(request));
    }

    /**
     * SSE 输出监听器适配器。
     */
    private static final class SseEmitterStreamListener implements AiStreamListener {
        private final SseEmitter emitter;
        /** 连接是否已关闭；超时或客户端断开后继续写入只会刷屏异常日志 */
        private final AtomicBoolean closed = new AtomicBoolean(false);

        /**
         * 构造 SSE 监听器适配器。
         *
         * @param emitter SSE 输出对象
         */
        private SseEmitterStreamListener(SseEmitter emitter) {
            this.emitter = emitter;
        }

        /**
         * 标记连接已关闭。
         */
        private void markClosed() {
            closed.set(true);
        }

        /**
         * 发送流式准备完成事件。
         *
         * @param model 当前模型编号
         */
        @Override
        public void onReady(String model) {
            sendEvent("ready", Map.of("model", model == null ? "" : model));
        }

        /**
         * 发送会话绑定事件。
         *
         * @param sessionId 会话ID
         */
        @Override
        public void onSession(Long sessionId) {
            if (sessionId == null) {
                return;
            }
            sendEvent("session", Map.of("sessionId", sessionId));
        }

        /**
         * 发送工具开始执行事件。
         *
         * @param toolName  工具名称
         * @param toolLabel 工具展示名
         */
        @Override
        public void onToolStart(String toolName, String toolLabel) {
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("toolName", toolName == null ? "" : toolName);
            payload.put("toolLabel", toolLabel == null ? "" : toolLabel);
            sendEvent("tool_start", payload);
        }

        /**
         * 发送结构化区块事件。
         *
         * @param block 结构化区块
         */
        @Override
        public void onBlock(AiStructuredBlock block) {
            if (block == null) {
                return;
            }
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("blockType", block.getBlockType());
            payload.put("toolName", block.getToolName());
            payload.put("dataSet", block.getDataSet());
            sendEvent("block", payload);
        }

        /**
         * 发送流式增量事件。
         *
         * @param delta 增量文本
         */
        @Override
        public void onDelta(String delta) {
            sendEvent("delta", Map.of("content", delta == null ? "" : delta));
        }

        /**
         * 发送流式结束事件。
         *
         * @param content 最终完整文本
         */
        @Override
        public void onDone(String content) {
            sendEvent("done", Map.of("content", content == null ? "" : content));
            complete();
        }

        /**
         * 发送待确认动作事件。
         *
         * @param pendingAction 待确认动作
         */
        @Override
        public void onActionRequired(AiPendingAction pendingAction) {
            if (pendingAction == null) {
                return;
            }
            LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
            payload.put("actionKey", pendingAction.getActionKey());
            payload.put("actionLabel", pendingAction.getActionLabel());
            payload.put("riskLevel", pendingAction.getRiskLevel());
            payload.put("targetLabel", pendingAction.getTargetLabel());
            payload.put("summary", pendingAction.getSummary());
            payload.put("confirmationToken", pendingAction.getConfirmationToken());
            sendEvent("action_required", payload);
        }

        /**
         * 发送流式错误事件。
         *
         * @param message 错误提示
         */
        @Override
        public void onError(String message) {
            sendEvent("error", Map.of("message", message == null ? "AI 服务异常" : message));
            complete();
        }

        /**
         * 完成 SSE 输出。
         */
        private void complete() {
            if (closed.compareAndSet(false, true)) {
                try {
                    emitter.complete();
                } catch (RuntimeException ignored) {
                    // 连接已被容器回收，忽略。
                }
            }
        }

        /**
         * 向前端发送统一格式的 SSE 事件。
         *
         * @param type    事件类型
         * @param payload 事件负载
         */
        private void sendEvent(String type, Map<String, Object> payload) {
            if (closed.get()) {
                return;
            }
            try {
                LinkedHashMap<String, Object> eventPayload = new LinkedHashMap<>();
                eventPayload.put("type", type);
                if (payload != null && !payload.isEmpty()) {
                    eventPayload.putAll(payload);
                }
                emitter.send(SseEmitter.event().data(eventPayload, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                // 客户端断开是常态，标记关闭后静默结束，避免后续写入持续抛异常。
                markClosed();
                try {
                    emitter.completeWithError(ex);
                } catch (RuntimeException ignored) {
                    // 忽略重复结束。
                }
            }
        }
    }
}
