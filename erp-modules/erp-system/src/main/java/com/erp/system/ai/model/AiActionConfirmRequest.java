package com.erp.system.ai.model;

import java.io.Serializable;

/**
 * AI 动作确认请求对象。
 */
public class AiActionConfirmRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 待确认票据。
     */
    private String confirmationToken;

    public String getConfirmationToken() {
        return confirmationToken;
    }

    public void setConfirmationToken(String confirmationToken) {
        this.confirmationToken = confirmationToken;
    }
}
