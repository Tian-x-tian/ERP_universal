package com.erp.system.support;

import com.erp.common.core.context.TenantContextHolder;
import org.springframework.util.StringUtils;

/**
 * 全局主数据写入保护工具。
 */
public final class TenantWriteGuard {

    private static final String PLATFORM_TENANT_ID = "000000";

    private TenantWriteGuard() {
    }

    /**
     * 判断当前租户是否允许写入全局主数据。
     *
     * @return true 表示允许写入
     */
    public static boolean canWriteGlobalData() {
        String currentTenantId = currentTenantId();
        return StringUtils.hasText(currentTenantId) && PLATFORM_TENANT_ID.equals(currentTenantId);
    }

    /**
     * 获取当前请求上下文中的租户编号。
     *
     * @return 当前租户编号
     */
    public static String currentTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }
}
