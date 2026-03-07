package com.erp.common.core.context;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * 租户上下文持有人
 */
public class TenantContextHolder {
    private static final ThreadLocal<String> TENANT_CONTEXT = new TransmittableThreadLocal<>();

    public static void setTenantId(String tenantId) {
        TENANT_CONTEXT.set(tenantId);
    }

    public static String getTenantId() {
        return TENANT_CONTEXT.get();
    }

    public static void clear() {
        TENANT_CONTEXT.remove();
    }
}
