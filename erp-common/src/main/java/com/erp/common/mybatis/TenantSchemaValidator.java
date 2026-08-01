package com.erp.common.mybatis;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates that every application table is either tenant-scoped or explicitly global.
 */
public class TenantSchemaValidator {
    private static final String TABLE_NAME_KEY = "table_name";
    private static final String HAS_TENANT_ID_KEY = "has_tenant_id";
    private static final String VALIDATION_SQL = "SELECT t.TABLE_NAME AS table_name, "
            + "MAX(CASE WHEN c.COLUMN_NAME = 'tenant_id' THEN 1 ELSE 0 END) AS has_tenant_id "
            + "FROM information_schema.TABLES t "
            + "LEFT JOIN information_schema.COLUMNS c ON c.TABLE_SCHEMA = t.TABLE_SCHEMA "
            + "AND c.TABLE_NAME = t.TABLE_NAME AND c.COLUMN_NAME = 'tenant_id' "
            + "WHERE t.TABLE_SCHEMA = DATABASE() AND t.TABLE_TYPE = 'BASE TABLE' "
            + "GROUP BY t.TABLE_NAME";

    private final JdbcTemplate jdbcTemplate;
    private final Set<String> globalTables;

    /**
     * Creates a tenant schema validator.
     *
     * @param jdbcTemplate JDBC template used for read-only metadata access
     * @param globalTables explicitly global platform tables
     */
    public TenantSchemaValidator(JdbcTemplate jdbcTemplate, Set<String> globalTables) {
        this.jdbcTemplate = jdbcTemplate;
        this.globalTables = globalTables == null ? Set.of() : globalTables.stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeTableName)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    /**
     * Validates the current database schema.
     */
    public void validate() {
        List<String> violations = new ArrayList<>();
        for (Map<String, Object> metadataRow : jdbcTemplate.queryForList(VALIDATION_SQL)) {
            String tableName = normalizeTableName(String.valueOf(value(metadataRow, TABLE_NAME_KEY)));
            if (!globalTables.contains(tableName) && !hasTenantId(metadataRow)) {
                violations.add(tableName);
            }
        }
        violations.sort(Comparator.naturalOrder());
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Tenant schema validation failed. Missing tenant_id column: "
                    + String.join(", ", violations) + ".");
        }
    }

    private Object value(Map<String, Object> metadataRow, String expectedKey) {
        if (metadataRow == null) {
            return null;
        }
        for (Map.Entry<String, Object> entry : metadataRow.entrySet()) {
            if (expectedKey.equalsIgnoreCase(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean hasTenantId(Map<String, Object> metadataRow) {
        Object value = value(metadataRow, HAS_TENANT_ID_KEY);
        if (value instanceof Number number) {
            return number.intValue() > 0;
        }
        return value instanceof Boolean booleanValue && booleanValue;
    }

    private String normalizeTableName(String tableName) {
        if (!StringUtils.hasText(tableName)) {
            return "";
        }
        String normalized = tableName.replace("`", "").trim().toLowerCase(Locale.ROOT);
        int separatorIndex = normalized.lastIndexOf('.');
        return separatorIndex >= 0 && separatorIndex < normalized.length() - 1
                ? normalized.substring(separatorIndex + 1)
                : normalized;
    }
}
