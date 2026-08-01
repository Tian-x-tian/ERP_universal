package com.erp.saas.control.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.LinkedHashMap;
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
}
