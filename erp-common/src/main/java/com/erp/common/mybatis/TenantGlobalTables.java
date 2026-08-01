package com.erp.common.mybatis;

import java.util.Set;

/**
 * Explicit global-table allowlist shared by tenant-aware ERP modules.
 */
public final class TenantGlobalTables {
    public static final Set<String> TABLES = Set.of(
            "sys_tenant",
            "sys_menu",
            "sys_dict_type",
            "sys_dict_data",
            "sys_config",
            "sys_sql_upgrade_log",
            "biz_sql_upgrade_log");

    private TenantGlobalTables() {
    }
}
