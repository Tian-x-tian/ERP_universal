package com.erp.ai.service;

import com.erp.ai.model.AiConfirmationPayload;

/**
 * AI 动作确认票据服务。
 */
public interface AiConfirmationTokenService {

    /**
     * 生成确认票据。
     *
     * @param payload 票据载荷
     * @return 签名票据
     */
    String createToken(AiConfirmationPayload payload);

    /**
     * 解析并校验确认票据。
     *
     * @param token 票据文本
     * @return 解析后的载荷
     */
    AiConfirmationPayload parseToken(String token);
}
