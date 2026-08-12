package com.erp.saas.control.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaasControlCatalogSchemaValidatorTest {
    @Test void shouldAcceptExactManifestAndRejectStructuralMismatch() {
        var expected = SaasControlCatalogSchemaManifest.tables();
        new SaasControlCatalogSchemaValidator(() -> expected).validate();

        var actual = new LinkedHashMap<>(expected);
        var tenant = expected.get("saas_tenant");
        var columns = new LinkedHashMap<>(tenant.columns());
        columns.remove("slug");
        actual.put("saas_tenant", new SaasControlCatalogSchemaManifest.Table(
                tenant.name(), tenant.engine(), tenant.collation(), columns, tenant.indexes(), tenant.checks()));
        assertThatThrownBy(() -> new SaasControlCatalogSchemaValidator(() -> actual).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("saas_tenant")
                .satisfies(error -> assertThat(error.getMessage()).doesNotContain("slug"));
    }

    @Test void shouldRunAfterUpgradeAndDefaultToEnabled() {
        SaasControlCatalogSchemaValidationRunner runner = new SaasControlCatalogSchemaValidationRunner(
                new SaasControlCatalogSchemaValidator(SaasControlCatalogSchemaManifest::tables));
        assertThat(runner.getOrder()).isEqualTo(200);
        ConditionalOnProperty condition = SaasControlCatalogSchemaValidationRunner.class
                .getAnnotation(ConditionalOnProperty.class);
        assertThat(condition.name()).containsExactly("enabled");
        assertThat(condition.prefix()).isEqualTo("erp.saas.schema-validation");
        assertThat(condition.matchIfMissing()).isTrue();
    }

    @Test void shouldAcceptMySql8017IntegerDisplayWidthsAndBlankGenerationExpressions() {
        var actual = new LinkedHashMap<String, SaasControlCatalogSchemaManifest.Table>();
        SaasControlCatalogSchemaManifest.tables().forEach((tableName, table) -> {
            var columns = new LinkedHashMap<String, SaasControlCatalogSchemaManifest.Column>();
            table.columns().forEach((columnName, column) -> {
                String type = Map.of("bigint", "bigint(20)", "int", "int(11)")
                        .getOrDefault(column.type(), column.type());
                String generation = column.generationExpression() == null ? "" : column.generationExpression()
                        .replace("'REVOKED'", "_utf8mb4\\'REVOKED\\'")
                        .replace("'ACTIVE'", "_utf8mb4\\'ACTIVE\\'")
                        .replace("'TRIAL'", "_utf8mb4\\'TRIAL\\'")
                        .replace("'GRACE'", "_utf8mb4\\'GRACE\\'");
                columns.put(columnName, new SaasControlCatalogSchemaManifest.Column(column.name(), type,
                        column.nullable(), column.defaultValue(), generation, column.collation(),
                        column.storedGenerated()));
            });
            actual.put(tableName, new SaasControlCatalogSchemaManifest.Table(table.name(), table.engine(),
                    table.collation(), columns, table.indexes(), table.checks()));
        });

        new SaasControlCatalogSchemaValidator(() -> actual).validate();
    }

    @Test void shouldNotCollapseDifferentStringLiteralsDuringExpressionNormalization() {
        assertColumnExpressionRejected("CASE WHEN (verification_state <> 'REVOKED_X') THEN host ELSE NULL END");
        assertColumnExpressionRejected("CASE WHEN (verification_state <> 'RE VOKED') THEN host ELSE NULL END");
    }

    @Test void shouldRejectPrefixAndDescendingIndexes() {
        var expected = SaasControlCatalogSchemaManifest.tables();
        var tenant = expected.get("saas_tenant");
        var indexes = new LinkedHashMap<>(tenant.indexes());
        indexes.put("uk_saas_tenant_slug", new SaasControlCatalogSchemaManifest.Index(
                "uk_saas_tenant_slug", true,
                List.of(new SaasControlCatalogSchemaManifest.IndexColumn("slug", 16, "A"))));
        assertTableRejected("saas_tenant", new SaasControlCatalogSchemaManifest.Table(
                tenant.name(), tenant.engine(), tenant.collation(), tenant.columns(), indexes, tenant.checks()));

        indexes.put("uk_saas_tenant_slug", new SaasControlCatalogSchemaManifest.Index(
                "uk_saas_tenant_slug", true,
                List.of(new SaasControlCatalogSchemaManifest.IndexColumn("slug", null, "D"))));
        assertTableRejected("saas_tenant", new SaasControlCatalogSchemaManifest.Table(
                tenant.name(), tenant.engine(), tenant.collation(), tenant.columns(), indexes, tenant.checks()));
    }

    @Test void shouldRejectCheckConstraintThatIsNotEnforced() {
        var expected = SaasControlCatalogSchemaManifest.tables();
        var plan = expected.get("saas_plan");
        var checks = new LinkedHashMap<>(plan.checks());
        var check = checks.get("ck_saas_plan_version");
        checks.put("ck_saas_plan_version", new SaasControlCatalogSchemaManifest.Check(check.expression(), false));
        assertTableRejected("saas_plan", new SaasControlCatalogSchemaManifest.Table(
                plan.name(), plan.engine(), plan.collation(), plan.columns(), plan.indexes(), checks));
    }

    private void assertColumnExpressionRejected(String generationExpression) {
        var expected = SaasControlCatalogSchemaManifest.tables();
        var domain = expected.get("saas_domain");
        var columns = new LinkedHashMap<>(domain.columns());
        var ownedHost = columns.get("owned_host");
        columns.put("owned_host", new SaasControlCatalogSchemaManifest.Column(ownedHost.name(), ownedHost.type(),
                ownedHost.nullable(), ownedHost.defaultValue(), generationExpression, ownedHost.collation(),
                ownedHost.storedGenerated()));
        assertTableRejected("saas_domain", new SaasControlCatalogSchemaManifest.Table(
                domain.name(), domain.engine(), domain.collation(), columns, domain.indexes(), domain.checks()));
    }

    private void assertTableRejected(String tableName, SaasControlCatalogSchemaManifest.Table table) {
        var actual = new LinkedHashMap<>(SaasControlCatalogSchemaManifest.tables());
        actual.put(tableName, table);
        assertThatThrownBy(() -> new SaasControlCatalogSchemaValidator(() -> actual).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(tableName);
    }
}
