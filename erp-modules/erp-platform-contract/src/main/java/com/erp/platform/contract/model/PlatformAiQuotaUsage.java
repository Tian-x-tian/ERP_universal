package com.erp.platform.contract.model;

import java.io.Serializable;

/**
 * AI 当日用量视图。
 *
 * <p>统计口径直接来自 {@code sys_ai_audit}，与审计同源，不再单独维护计数器，
 * 避免多实例部署下计数漂移。</p>
 */
public class PlatformAiQuotaUsage implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 当前租户当日请求数 */
    private int tenantRequestCount;
    /** 当前租户当日 token 消耗 */
    private long tenantTokenCount;
    /** 当前用户当日请求数 */
    private int userRequestCount;

    public int getTenantRequestCount() {
        return tenantRequestCount;
    }

    public void setTenantRequestCount(int tenantRequestCount) {
        this.tenantRequestCount = tenantRequestCount;
    }

    public long getTenantTokenCount() {
        return tenantTokenCount;
    }

    public void setTenantTokenCount(long tenantTokenCount) {
        this.tenantTokenCount = tenantTokenCount;
    }

    public int getUserRequestCount() {
        return userRequestCount;
    }

    public void setUserRequestCount(int userRequestCount) {
        this.userRequestCount = userRequestCount;
    }
}
