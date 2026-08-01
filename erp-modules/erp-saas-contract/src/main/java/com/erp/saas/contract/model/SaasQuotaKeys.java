package com.erp.saas.contract.model;

import java.util.Set;

public final class SaasQuotaKeys {
    public static final String USER_COUNT = "user_count";
    public static final String STORAGE_BYTES = "storage_bytes";
    public static final String AI_INPUT_TOKENS = "ai_input_tokens";
    public static final String AI_OUTPUT_TOKENS = "ai_output_tokens";

    private static final Set<String> KNOWN_KEYS = Set.of(
            USER_COUNT, STORAGE_BYTES, AI_INPUT_TOKENS, AI_OUTPUT_TOKENS);
    private static final Set<String> MONTHLY_KEYS = Set.of(AI_INPUT_TOKENS, AI_OUTPUT_TOKENS);

    private SaasQuotaKeys() {
    }

    public static boolean isKnown(String quotaKey) {
        return KNOWN_KEYS.contains(quotaKey);
    }

    public static boolean isMonthly(String quotaKey) {
        return MONTHLY_KEYS.contains(quotaKey);
    }
}
