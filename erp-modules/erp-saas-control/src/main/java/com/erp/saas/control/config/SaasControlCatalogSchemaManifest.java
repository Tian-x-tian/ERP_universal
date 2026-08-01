package com.erp.saas.control.config;

import java.util.*;

public final class SaasControlCatalogSchemaManifest {
    private SaasControlCatalogSchemaManifest() { }

    public record Column(String name, String type, boolean nullable, String defaultValue,
            String generationExpression, String collation, boolean storedGenerated) { }
    public record IndexColumn(String name, Integer prefixLength, String order) { }
    public record Index(String name, boolean unique, List<IndexColumn> columns) { }
    public record Check(String expression, boolean enforced) { }
    public record Table(String name, String engine, String collation, Map<String, Column> columns,
            Map<String, Index> indexes, Map<String, Check> checks) { }

    public static Map<String, Table> tables() {
        LinkedHashMap<String, Table> tables = new LinkedHashMap<>();
        add(tables, "saas_tenant", "id:bigint:N:~:~:~;tenant_id:varchar(20):N:~:~:utf8mb4_unicode_ci;slug:varchar(64):N:~:~:utf8mb4_unicode_ci;tenant_name:varchar(128):N:~:~:utf8mb4_unicode_ci;lifecycle_state:varchar(32):N:~:~:utf8mb4_unicode_ci;suspended_from_state:varchar(32):Y:~:~:utf8mb4_unicode_ci;archived_at:datetime(3):Y:~:~:~;purge_eligible_at:datetime(3):Y:~:~:~;purged_at:datetime(3):Y:~:~:~;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:id;uk_saas_tenant_tenant_id:U:tenant_id;uk_saas_tenant_slug:U:slug;idx_saas_tenant_state_update:N:lifecycle_state,update_time", "");
        add(tables, "saas_domain", "domain_id:bigint:N:~:~:~;tenant_id:varchar(20):N:~:~:utf8mb4_unicode_ci;host:varchar(253):N:~:~:utf8mb4_unicode_ci;verification_state:varchar(32):N:~:~:utf8mb4_unicode_ci;verification_method:varchar(32):N:~:~:utf8mb4_unicode_ci;verified_at:datetime(3):Y:~:~:~;revoked_at:datetime(3):Y:~:~:~;owned_host:varchar(253):Y:~:CASE WHEN (verification_state <> 'REVOKED') THEN host ELSE NULL END:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:domain_id;uk_saas_domain_owned_host:U:owned_host;idx_saas_domain_host:N:host;idx_saas_domain_tenant_state:N:tenant_id,verification_state", "");
        add(tables, "saas_plan", "plan_id:bigint:N:~:~:~;plan_code:varchar(64):N:~:~:utf8mb4_unicode_ci;plan_version:int:N:~:~:~;plan_name:varchar(128):N:~:~:utf8mb4_unicode_ci;status:varchar(32):N:~:~:utf8mb4_unicode_ci;trial_days:int:N:14:~:~;grace_days:int:N:7:~:~;description:varchar(512):Y:~:~:utf8mb4_unicode_ci;active_slot:varchar(64):Y:~:CASE WHEN (status = 'ACTIVE') THEN plan_code ELSE NULL END:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:plan_id;uk_saas_plan_code_version:U:plan_code,plan_version;uk_saas_plan_active_slot:U:active_slot", "ck_saas_plan_version=plan_version BETWEEN 1 AND 2147483647;ck_saas_plan_trial_days=trial_days BETWEEN 0 AND 3650;ck_saas_plan_grace_days=grace_days BETWEEN 0 AND 3650");
        add(tables, "saas_feature", "feature_id:bigint:N:~:~:~;feature_key:varchar(128):N:~:~:utf8mb4_unicode_ci;feature_name:varchar(128):N:~:~:utf8mb4_unicode_ci;status:varchar(32):N:~:~:utf8mb4_unicode_ci;description:varchar(512):Y:~:~:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:feature_id;uk_saas_feature_key:U:feature_key", "");
        add(tables, "saas_plan_feature", "plan_feature_id:bigint:N:~:~:~;plan_id:bigint:N:~:~:~;feature_id:bigint:N:~:~:~;granted:tinyint(1):N:1:~:~;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~",
                "PRIMARY:U:plan_feature_id;uk_saas_plan_feature:U:plan_id,feature_id;idx_saas_plan_feature_feature:N:feature_id", "ck_saas_plan_feature_granted=granted IN (0, 1)");
        add(tables, "saas_plan_quota", "plan_quota_id:bigint:N:~:~:~;plan_id:bigint:N:~:~:~;quota_key:varchar(64):N:~:~:utf8mb4_unicode_ci;limit_value:bigint:Y:~:~:~;period_type:varchar(32):N:~:~:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~",
                "PRIMARY:U:plan_quota_id;uk_saas_plan_quota:U:plan_id,quota_key", "ck_saas_plan_quota_limit=(limit_value IS NULL) OR (limit_value >= 0)");
        add(tables, "saas_subscription", "subscription_id:bigint:N:~:~:~;tenant_id:varchar(20):N:~:~:utf8mb4_unicode_ci;plan_id:bigint:N:~:~:~;state:varchar(32):N:~:~:utf8mb4_unicode_ci;start_at:datetime(3):N:~:~:~;end_at:datetime(3):Y:~:~:~;grace_end_at:datetime(3):Y:~:~:~;non_expiring:tinyint(1):N:0:~:~;current_slot:varchar(20):Y:~:CASE WHEN (state IN ('TRIAL','ACTIVE','GRACE')) THEN tenant_id ELSE NULL END:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:subscription_id;uk_saas_subscription_current_slot:U:current_slot;idx_saas_subscription_tenant_state:N:tenant_id,state;idx_saas_subscription_lifecycle_time:N:state,end_at,grace_end_at", "ck_saas_subscription_non_expiring=non_expiring IN (0, 1);ck_saas_subscription_dates=((non_expiring = 1) AND (end_at IS NULL) AND (grace_end_at IS NULL)) OR ((non_expiring = 0) AND (end_at IS NOT NULL) AND (grace_end_at IS NOT NULL) AND (grace_end_at >= end_at))");
        add(tables, "saas_tenant_feature_override", "override_id:bigint:N:~:~:~;tenant_id:varchar(20):N:~:~:utf8mb4_unicode_ci;feature_id:bigint:N:~:~:~;override_state:varchar(32):N:~:~:utf8mb4_unicode_ci;effective_from:datetime(3):N:~:~:~;effective_until:datetime(3):Y:~:~:~;reason:varchar(512):Y:~:~:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:override_id;uk_saas_tenant_feature_window:U:tenant_id,feature_id,effective_from;idx_saas_tenant_feature_effective:N:tenant_id,effective_from,effective_until", "ck_saas_tenant_feature_window=(effective_until IS NULL) OR (effective_until > effective_from)");
        add(tables, "saas_tenant_quota_override", "override_id:bigint:N:~:~:~;tenant_id:varchar(20):N:~:~:utf8mb4_unicode_ci;quota_key:varchar(64):N:~:~:utf8mb4_unicode_ci;limit_value:bigint:Y:~:~:~;effective_from:datetime(3):N:~:~:~;effective_until:datetime(3):Y:~:~:~;reason:varchar(512):Y:~:~:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:override_id;uk_saas_tenant_quota_window:U:tenant_id,quota_key,effective_from;idx_saas_tenant_quota_effective:N:tenant_id,effective_from,effective_until", "ck_saas_tenant_quota_window=(effective_until IS NULL) OR (effective_until > effective_from);ck_saas_tenant_quota_limit=(limit_value IS NULL) OR (limit_value >= 0)");
        add(tables, "saas_deployment", "deployment_id:bigint:N:~:~:~;tenant_id:varchar(20):N:~:~:utf8mb4_unicode_ci;mode:varchar(32):N:~:~:utf8mb4_unicode_ci;status:varchar(32):N:~:~:utf8mb4_unicode_ci;deployment_ref:varchar(255):N:~:~:utf8mb4_unicode_ci;secret_ref:varchar(255):Y:~:~:utf8mb4_unicode_ci;create_by:varchar(64):N:~:~:utf8mb4_unicode_ci;create_time:datetime(3):N:~:~:~;update_by:varchar(64):N:~:~:utf8mb4_unicode_ci;update_time:datetime(3):N:~:~:~;version_no:bigint:N:0:~:~",
                "PRIMARY:U:deployment_id;uk_saas_deployment_tenant:U:tenant_id", "");
        return Collections.unmodifiableMap(tables);
    }

    private static void add(Map<String, Table> tables, String name, String columnsDsl, String indexesDsl, String checksDsl) {
        LinkedHashMap<String, Column> columns = new LinkedHashMap<>();
        for (String item : columnsDsl.split(";")) {
            String[] value = item.split(":", 6);
            String generationExpression = nullable(value[4]);
            columns.put(value[0], new Column(value[0], value[1], "Y".equals(value[2]), nullable(value[3]),
                    generationExpression, nullable(value[5]), generationExpression != null));
        }
        LinkedHashMap<String, Index> indexes = new LinkedHashMap<>();
        for (String item : indexesDsl.split(";")) {
            String[] value = item.split(":", 3);
            indexes.put(value[0], new Index(value[0], "U".equals(value[1]), Arrays.stream(value[2].split(","))
                    .map(column -> new IndexColumn(column, null, "A"))
                    .toList()));
        }
        LinkedHashMap<String, Check> checks = new LinkedHashMap<>();
        if (!checksDsl.isEmpty()) {
            for (String item : checksDsl.split(";")) {
                int separator = item.indexOf('=');
                checks.put(item.substring(0, separator), new Check(item.substring(separator + 1), true));
            }
        }
        tables.put(name, new Table(name, "InnoDB", "utf8mb4_unicode_ci",
                Collections.unmodifiableMap(columns), Collections.unmodifiableMap(indexes), Collections.unmodifiableMap(checks)));
    }

    private static String nullable(String value) { return "~".equals(value) ? null : value; }
}
