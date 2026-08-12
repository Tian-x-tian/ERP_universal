package com.erp.ai.model;

import java.io.Serializable;

/**
 * AI 配额判定结果。
 */
public class AiQuotaDecision implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 是否放行 */
    private boolean allowed;
    /** 拒绝原因，放行时为空 */
    private String message;

    /**
     * 构造放行结果。
     *
     * @return 判定结果
     */
    public static AiQuotaDecision allow() {
        AiQuotaDecision decision = new AiQuotaDecision();
        decision.allowed = true;
        return decision;
    }

    /**
     * 构造拒绝结果。
     *
     * @param message 拒绝原因
     * @return 判定结果
     */
    public static AiQuotaDecision deny(String message) {
        AiQuotaDecision decision = new AiQuotaDecision();
        decision.allowed = false;
        decision.message = message;
        return decision;
    }

    public boolean isAllowed() {
        return allowed;
    }

    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
