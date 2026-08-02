package com.erp.saas.control.config;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.erp.saas.control.domain.entity.SaasUsageSummaryEntity;
import com.erp.saas.control.mapper.SaasUsageSummaryMapper;
import org.apache.ibatis.session.SqlSession;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SaasControlCatalogSqlMigrationIntegrationTest {
    private static final String URL = "ERP_SAAS_TEST_JDBC_URL";
    private static final String USER = "ERP_SAAS_TEST_DB_USER";
    private static final String PASSWORD = "ERP_SAAS_TEST_DB_PASSWORD";
    private static final List<String> TABLES = List.of("saas_usage_summary", "saas_usage_event",
            "saas_entitlement_snapshot", "saas_deployment", "saas_tenant_quota_override",
            "saas_tenant_feature_override", "saas_subscription", "saas_plan_quota", "saas_plan_feature",
            "saas_feature", "saas_plan", "saas_domain", "saas_tenant");

    @Test void shouldValidateInitUpgradeIdempotenceAndRejectIncompatibleSchemas() throws Exception {
        String url = System.getenv(URL); String user = System.getenv(USER); String password = System.getenv(PASSWORD);
        Assumptions.assumeTrue(text(url) && text(user) && password != null, "Isolated MySQL variables required");
        validateUrl(url);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        try {
            reset(url, user, password); executeTwice(dataSource, "sql/init_control.sql"); validate(dataSource);
            verifyUsageSummaryKeepsNewestSnapshot(dataSource);
            reset(url, user, password); executeCatalogUpgradesTwice(dataSource); validate(dataSource);
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_tenant` ALTER COLUMN `version_no` SET DEFAULT 1", "saas_tenant");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_plan` DROP CHECK `ck_saas_plan_version`", "saas_plan");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_domain` DROP INDEX `uk_saas_domain_owned_host`, DROP COLUMN `owned_host`, "
                            + "ADD COLUMN `owned_host` VARCHAR(253) AS (`host`) STORED, "
                            + "ADD UNIQUE KEY `uk_saas_domain_owned_host` (`owned_host`)", "saas_domain");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_domain` DROP INDEX `uk_saas_domain_owned_host`, DROP COLUMN `owned_host`, "
                            + "ADD COLUMN `owned_host` VARCHAR(253) AS "
                            + "(CASE WHEN (`verification_state` <> 'REVOKED_X') THEN `host` ELSE NULL END) STORED, "
                            + "ADD UNIQUE KEY `uk_saas_domain_owned_host` (`owned_host`)", "saas_domain");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_tenant` DROP INDEX `uk_saas_tenant_slug`, "
                            + "ADD UNIQUE KEY `uk_saas_tenant_slug` (`slug`(16))", "saas_tenant");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_tenant` DROP INDEX `uk_saas_tenant_slug`, "
                            + "ADD UNIQUE KEY `uk_saas_tenant_slug` (`slug` DESC)", "saas_tenant");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_plan` ALTER CHECK `ck_saas_plan_version` NOT ENFORCED", "saas_plan");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_feature` ENGINE=MyISAM", "saas_feature");
            assertRejected(dataSource, url, user, password,
                    "ALTER TABLE `saas_deployment` CONVERT TO CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci",
                    "saas_deployment");
        } finally {
            reset(url, user, password);
        }
    }

    private void assertRejected(DriverManagerDataSource dataSource, String url, String user, String password,
            String mutation, String table) throws Exception {
        reset(url, user, password);
        executeCatalogUpgrades(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute(mutation);
        }
        executeCatalogUpgrades(dataSource);
        assertThatThrownBy(() -> validate(dataSource)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining(table);
    }

    private void validate(DriverManagerDataSource dataSource) { new SaasControlCatalogSchemaValidator(dataSource).validate(); }
    private void verifyUsageSummaryKeepsNewestSnapshot(DriverManagerDataSource dataSource) throws Exception {
        MybatisConfiguration configuration = new MybatisConfiguration();
        configuration.addMapper(SaasUsageSummaryMapper.class);
        MybatisSqlSessionFactoryBean factoryBean = new MybatisSqlSessionFactoryBean();
        factoryBean.setDataSource(dataSource);
        factoryBean.setConfiguration(configuration);
        try (SqlSession session = factoryBean.getObject().openSession(false)) {
            SaasUsageSummaryMapper mapper = session.getMapper(SaasUsageSummaryMapper.class);
            mapper.upsertLatest(summary(101L, 20L, "newer", LocalDateTime.of(2026, 8, 2, 4, 0)));
            mapper.upsertLatest(summary(102L, 10L, "older", LocalDateTime.of(2026, 8, 2, 3, 0)));
            session.commit();
        }
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery(
                        "SELECT used_amount, last_event_key FROM saas_usage_summary WHERE usage_summary_id = 101")) {
            assertThat(result.next()).isTrue();
            assertThat(result.getLong("used_amount")).isEqualTo(20L);
            assertThat(result.getString("last_event_key")).isEqualTo("newer");
        }
    }
    private SaasUsageSummaryEntity summary(long id, long amount, String eventKey, LocalDateTime occurredAt) {
        SaasUsageSummaryEntity row = new SaasUsageSummaryEntity();
        row.setUsageSummaryId(id); row.setTenantId("tenant-a"); row.setMetricKey("ai_input_tokens");
        row.setPeriodStart(LocalDateTime.of(2026, 8, 1, 0, 0)); row.setUsedAmount(amount);
        row.setLastEventKey(eventKey); row.setLastOccurredAt(occurredAt); row.setCreateBy("test");
        row.setCreateTime(occurredAt); row.setUpdateBy("test"); row.setUpdateTime(occurredAt); row.setVersionNo(0L);
        return row;
    }
    private void executeTwice(DriverManagerDataSource dataSource, String path) throws Exception {
        execute(dataSource, path); execute(dataSource, path);
    }
    private void executeCatalogUpgradesTwice(DriverManagerDataSource dataSource) throws Exception {
        executeCatalogUpgrades(dataSource); executeCatalogUpgrades(dataSource);
    }
    private void executeCatalogUpgrades(DriverManagerDataSource dataSource) throws Exception {
        execute(dataSource, "sql/upgrade/control/20260801_02_saas_control_catalog.sql");
        execute(dataSource, "sql/upgrade/control/20260802_01_saas_entitlement_snapshot.sql");
        execute(dataSource, "sql/upgrade/control/20260802_02_saas_usage_aggregation.sql");
    }
    private void execute(DriverManagerDataSource dataSource, String path) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            EncodedResource script = new EncodedResource(new ClassPathResource(path), StandardCharsets.UTF_8);
            ScriptUtils.executeSqlScript(connection, script);
        }
    }

    private void reset(String url, String user, String password) throws Exception {
        validateUrl(url);
        try (Connection connection = DriverManager.getConnection(url, user, password);
                Statement statement = connection.createStatement()) {
            validateConnection(connection);
            for (String table : TABLES) { statement.execute("DROP TABLE IF EXISTS `" + table + "`"); }
        }
    }

    private void validateUrl(String url) throws Exception {
        if (!text(url) || !url.startsWith("jdbc:mysql://")) { throw new IllegalStateException("MySQL URL required"); }
        URI uri = URI.create(url.substring(5));
        if (uri.getHost() == null || !InetAddress.getByName(uri.getHost()).isLoopbackAddress()) {
            throw new IllegalStateException("Loopback MySQL required");
        }
        if (!"/erp_saas_control_test".equals(uri.getPath())) {
            throw new IllegalStateException("Database erp_saas_control_test required");
        }
    }

    private void validateConnection(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DATABASE(), VERSION(), @@datadir")) {
            result.next();
            if (!"erp_saas_control_test".equals(result.getString(1)) || !result.getString(2).startsWith("8.0.17")) {
                throw new IllegalStateException("Unsafe MySQL target");
            }
            Path expected = worktree().resolve(".tmp/saas-mysql-8017").toRealPath();
            if (!Path.of(result.getString(3)).toRealPath().startsWith(expected)) {
                throw new IllegalStateException("Unsafe MySQL data directory");
            }
        }
    }

    private Path worktree() throws Exception {
        Path current = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        while (current != null && !Files.exists(current.resolve(".git"))) { current = current.getParent(); }
        if (current == null) { throw new IllegalStateException("Worktree not found"); }
        return current.toRealPath();
    }
    private boolean text(String value) { return value != null && !value.isBlank(); }
}
