package com.erp.ai.controller;

import com.erp.common.core.domain.R;
import com.erp.ai.model.AiActionConfirmRequest;
import com.erp.ai.model.AiActionResultVO;
import com.erp.ai.model.AiPendingAction;
import com.erp.ai.model.AiChatRequest;
import com.erp.ai.model.AiMetaVO;
import com.erp.ai.service.AiChatService;
import com.erp.ai.service.AiStreamListener;
import com.erp.ai.service.impl.AiSaasFeatureGuard;
import jakarta.servlet.http.HttpServletResponse;
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

/**
 * ERP AI 对话控制层。
 */
@RestController
@RequestMapping("/system/ai")
public class AiChatController {
    private final AiChatService aiChatService;
    private final Executor aiStreamingExecutor;
    private final AiSaasFeatureGuard featureGuard;

    public AiChatController(AiChatService aiChatService,
                            @Qualifier("aiStreamingExecutor") Executor aiStreamingExecutor,
                            AiSaasFeatureGuard featureGuard) {
        this.aiChatService = aiChatService;
        this.aiStreamingExecutor = aiStreamingExecutor;
        this.featureGuard = featureGuard;
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
        SseEmitter emitter = new SseEmitter(0L);
        emitter.onTimeout(emitter::complete);
        aiStreamingExecutor.execute(() -> aiChatService.streamChat(request, new SseEmitterStreamListener(emitter)));
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

        /**
         * 构造 SSE 监听器适配器。
         *
         * @param emitter SSE 输出对象
         */
        private SseEmitterStreamListener(SseEmitter emitter) {
            this.emitter = emitter;
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
            emitter.complete();
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
            emitter.complete();
        }

        /**
         * 向前端发送统一格式的 SSE 事件。
         *
         * @param type    事件类型
         * @param payload 事件负载
         */
        private void sendEvent(String type, Map<String, Object> payload) {
            try {
                LinkedHashMap<String, Object> eventPayload = new LinkedHashMap<>();
                eventPayload.put("type", type);
                if (payload != null && !payload.isEmpty()) {
                    eventPayload.putAll(payload);
                }
                emitter.send(SseEmitter.event().data(eventPayload, MediaType.APPLICATION_JSON));
            } catch (IOException | IllegalStateException ex) {
                emitter.completeWithError(ex);
            }
        }
    }
}
