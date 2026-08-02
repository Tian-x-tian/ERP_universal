package com.erp.saas.control.config;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SaasControlCatalogSchemaManifestTest {
    @Test void shouldFreezeExactCatalogStructure() {
        var tables = SaasControlCatalogSchemaManifest.tables();
        assertThat(tables.keySet()).containsExactly(
                "saas_tenant", "saas_domain", "saas_plan", "saas_feature", "saas_plan_feature",
                "saas_plan_quota", "saas_subscription", "saas_tenant_feature_override",
                "saas_tenant_quota_override", "saas_deployment", "saas_provisioning_task",
                "saas_entitlement_snapshot",
                "saas_usage_event", "saas_usage_summary");
        assertThat(tables.values()).allSatisfy(table -> {
            assertThat(table.engine()).isEqualTo("InnoDB");
            assertThat(table.collation()).isEqualTo("utf8mb4_unicode_ci");
            assertThat(table.columns()).containsKeys("create_by", "create_time", "update_by", "update_time");
        });
        assertThat(tables.get("saas_domain").columns().get("owned_host").generationExpression())
                .isEqualTo("CASE WHEN (verification_state <> 'REVOKED') THEN host ELSE NULL END");
        assertThat(tables.get("saas_plan").checks()).containsKey("ck_saas_plan_version");
        assertThat(tables.get("saas_subscription").indexes()).containsKey("uk_saas_subscription_current_slot");
        assertThat(tables.get("saas_deployment").columns()).doesNotContainKeys(
                "jdbc_url", "password", "token", "certificate", "connection_string");
        assertThat(tables.get("saas_provisioning_task").columns()).doesNotContainKeys(
                "activation_token", "password", "secret", "jdbc_url", "connection_string");
        assertThat(tables.get("saas_provisioning_task").indexes())
                .containsKeys("uk_saas_provisioning_request", "uk_saas_provisioning_tenant",
                        "idx_saas_provisioning_status_lease_update");
        assertThat(tables.get("saas_provisioning_task").checks())
                .containsKeys("ck_saas_provisioning_status", "ck_saas_provisioning_attempt_count",
                        "ck_saas_provisioning_lease", "ck_saas_provisioning_result");
        assertThat(tables.get("saas_entitlement_snapshot").indexes())
                .containsKeys("PRIMARY", "idx_saas_entitlement_snapshot_expiry");
        assertThat(tables.get("saas_usage_event").indexes())
                .containsKeys("uk_saas_usage_event_idempotency", "idx_saas_usage_event_occurred");
        assertThat(tables.get("saas_usage_summary").indexes())
                .containsKey("uk_saas_usage_summary_period");
    }
}
