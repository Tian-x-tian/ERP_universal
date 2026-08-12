package com.erp.system.saas.impl;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.system.saas.SaasTenantDatabasePurgeExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SaasTenantDatabasePurgeExecutorImpl implements SaasTenantDatabasePurgeExecutor {
    public static final String TENANT_TABLES_SQL = "SELECT DISTINCT c.TABLE_NAME "
            + "FROM information_schema.COLUMNS c "
            + "JOIN information_schema.TABLES t ON t.TABLE_SCHEMA = c.TABLE_SCHEMA "
            + "AND t.TABLE_NAME = c.TABLE_NAME "
            + "WHERE c.TABLE_SCHEMA = DATABASE() AND c.COLUMN_NAME = 'tenant_id' "
            + "AND t.TABLE_TYPE = 'BASE TABLE' ORDER BY c.TABLE_NAME";

    private static final Pattern TABLE_NAME = Pattern.compile("[A-Za-z0-9_]{1,64}");
    private final JdbcTemplate jdbcTemplate;

    public SaasTenantDatabasePurgeExecutorImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantPurgeResult purgeDatabase(SaasTenantPurgeRequest request) {
        SaasTenantPurgeServiceImpl.ValidatedPurge validated = SaasTenantPurgeServiceImpl.validate(request);
        List<String> tables = jdbcTemplate.queryForList(TENANT_TABLES_SQL, String.class).stream()
                .map(SaasTenantDatabasePurgeExecutorImpl::tableName)
                .sorted(Comparator.comparing((String table) -> "sys_tenant".equals(table)))
                .toList();
        String originalTenantId = TenantContextHolder.getTenantId();
        long deleted = 0;
        try {
            TenantContextHolder.setTenantId(validated.tenantId());
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
            for (String table : tables) {
                deleted += jdbcTemplate.update(
                        "DELETE FROM `" + table + "` WHERE tenant_id = ?", validated.tenantId());
            }
        } finally {
            try {
                jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
            } finally {
                restoreTenant(originalTenantId);
            }
        }
        return new SaasTenantPurgeResult(validated.requestId(), validated.tenantId(),
                tables.size(), deleted, deleted == 0);
    }

    private static String tableName(String value) {
        if (!StringUtils.hasText(value) || !TABLE_NAME.matcher(value).matches()) {
            throw new IllegalStateException("Unsafe tenant table name discovered");
        }
        return value;
    }

    private static void restoreTenant(String tenantId) {
        TenantContextHolder.clear();
        if (StringUtils.hasText(tenantId)) {
            TenantContextHolder.setTenantId(tenantId.trim());
        }
    }
}
