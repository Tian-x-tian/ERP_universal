package com.erp.ai.service.impl;

/**
 * 单次模型调用的输入、输出 Token 双配额预留凭据。
 */
public record AiQuotaReservation(String tenantId, long periodStartEpochMs,
        String inputReference, String outputReference,
        long reservedInputTokens, long reservedOutputTokens) {
}
