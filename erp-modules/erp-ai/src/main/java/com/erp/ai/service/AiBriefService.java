package com.erp.ai.service;

import com.erp.platform.contract.model.PlatformAiBriefView;

/**
 * AI 每日简报服务。
 *
 * <p>面板的核心诉求是「打开就有内容」。实时生成一份简报要跑若干次取数加一次模型调用，
 * 秒级起步，因此简报按 天 + 用户 缓存，打开时只读缓存；缺失或过期时后台异步生成，
 * 前端拿到 PENDING 后轮询即可。</p>
 */
public interface AiBriefService {

    /**
     * 获取当日简报；缺失或过期时触发一次后台生成。
     *
     * @return 简报视图
     */
    PlatformAiBriefView getOrTrigger();

    /**
     * 强制重新生成当日简报。
     *
     * @return 简报视图（通常为 PENDING）
     */
    PlatformAiBriefView refresh();
}
