package com.erp.system.config;

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
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SystemSaasQuotaSqlMigrationIntegrationTest {
    private static final String URL = "ERP_SYSTEM_TEST_JDBC_URL";
    private static final String USER = "ERP_SYSTEM_TEST_DB_USER";
    private static final String PASSWORD = "ERP_SYSTEM_TEST_DB_PASSWORD";

    @Test
    void shouldRunInitAndSaasQuotaUpgradesTwiceThroughScriptUtils() throws Exception {
        String url = System.getenv(URL);
        String user = System.getenv(USER);
        String password = System.getenv(PASSWORD);
        Assumptions.assumeTrue(text(url) && text(user) && password != null,
                "Isolated MySQL variables required");
        validateUrl(url);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        validateConnection(dataSource.getConnection());

        execute(dataSource, "sql/init_system.sql");
        execute(dataSource, "sql/init_system.sql");
        executeTwice(dataSource, "sql/upgrade/system/20260801_01_ai_panel_audit_telemetry.sql");
        executeTwice(dataSource, "sql/upgrade/system/20260801_02_ai_session_archive.sql");
        executeTwice(dataSource, "sql/upgrade/system/20260802_01_ai_panel_brief_and_quota.sql");
        validateTables(dataSource);
        validateProvisioningTables(dataSource);
        validateMenuFeatures(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE `sys_user_activation`");
            statement.execute("DROP TABLE `sys_saas_provisioning_task`");
            statement.execute("DROP TABLE `sys_saas_usage_outbox`");
            statement.execute("DROP TABLE `sys_saas_quota_reservation`");
            statement.execute("DROP TABLE `sys_saas_quota_counter`");
            statement.execute("ALTER TABLE `sys_menu` DROP INDEX `idx_sys_menu_feature_key`, "
                    + "DROP COLUMN `feature_key`");
        }
        execute(dataSource, "sql/upgrade/system/20260802_02_saas_quota_counters.sql");
        execute(dataSource, "sql/upgrade/system/20260802_02_saas_quota_counters.sql");
        seedCounter(dataSource);
        execute(dataSource, "sql/upgrade/system/20260802_03_saas_usage_outbox.sql");
        execute(dataSource, "sql/upgrade/system/20260802_03_saas_usage_outbox.sql");
        execute(dataSource, "sql/upgrade/system/20260802_04_saas_tenant_provisioning.sql");
        execute(dataSource, "sql/upgrade/system/20260802_04_saas_tenant_provisioning.sql");
        execute(dataSource, "sql/upgrade/system/20260802_05_saas_menu_features.sql");
        execute(dataSource, "sql/upgrade/system/20260802_05_saas_menu_features.sql");
        validateTables(dataSource);
        validateProvisioningTables(dataSource);
        validateMenuFeatures(dataSource);
        assertBootstrapSnapshot(dataSource);
    }

    private void validateMenuFeatures(DriverManagerDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet column = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = 'sys_menu' "
                    + "AND column_name = 'feature_key' AND column_type = 'varchar(128)'")) {
                column.next();
                assertThat(column.getInt(1)).isEqualTo(1);
            }
            try (ResultSet index = statement.executeQuery("SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) "
                    + "FROM information_schema.statistics WHERE table_schema = DATABASE() "
                    + "AND table_name = 'sys_menu' AND index_name = 'idx_sys_menu_feature_key'")) {
                index.next();
                assertThat(index.getString(1)).isEqualTo("feature_key");
            }
            try (ResultSet menus = statement.executeQuery("SELECT COUNT(*), "
                    + "SUM(feature_key = 'platform.saas.manage') FROM sys_menu "
                    + "WHERE path = '/platform/saas' OR path LIKE '/platform/saas/%'")) {
                menus.next();
                assertThat(menus.getInt(1)).isEqualTo(6);
                assertThat(menus.getInt(2)).isEqualTo(6);
            }
            try (ResultSet grants = statement.executeQuery("SELECT COUNT(DISTINCT menu_item.menu_id) "
                    + "FROM sys_role role_item JOIN sys_role_menu role_menu "
                    + "ON role_menu.tenant_id = role_item.tenant_id AND role_menu.role_id = role_item.role_id "
                    + "JOIN sys_menu menu_item ON menu_item.menu_id = role_menu.menu_id "
                    + "WHERE role_item.tenant_id = '000000' AND role_item.role_key = 'admin' "
                    + "AND (menu_item.path = '/platform/saas' OR menu_item.path LIKE '/platform/saas/%')")) {
                grants.next();
                assertThat(grants.getInt(1)).isEqualTo(6);
            }
        }
    }

    private void validateProvisioningTables(DriverManagerDataSource dataSource) throws Exception {
        assertColumns(dataSource, "sys_saas_provisioning_task", 15);
        assertColumns(dataSource, "sys_user_activation", 12);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet indexes = statement.executeQuery("SELECT table_name, index_name, "
                    + "GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_value "
                    + "FROM information_schema.statistics WHERE table_schema = DATABASE() "
                    + "AND table_name IN ('sys_saas_provisioning_task','sys_user_activation') "
                    + "GROUP BY table_name, index_name")) {
                Map<String, String> actual = new HashMap<>();
                while (indexes.next()) {
                    actual.put(indexes.getString(1) + "." + indexes.getString(2), indexes.getString(3));
                }
                assertThat(actual).containsEntry("sys_saas_provisioning_task.uk_sys_saas_provisioning_request",
                                "tenant_id,request_id")
                        .containsEntry("sys_saas_provisioning_task.idx_sys_saas_provisioning_status",
                                "tenant_id,status,update_time,task_id")
                        .containsEntry("sys_user_activation.uk_sys_user_activation_user", "tenant_id,user_id")
                        .containsEntry("sys_user_activation.uk_sys_user_activation_token", "tenant_id,token_hash")
                        .containsEntry("sys_user_activation.idx_sys_user_activation_pending",
                                "tenant_id,status,expires_at");
            }
            try (ResultSet checks = statement.executeQuery("SELECT COUNT(*) FROM information_schema.table_constraints "
                    + "WHERE table_schema = DATABASE() AND table_name IN "
                    + "('sys_saas_provisioning_task','sys_user_activation') "
                    + "AND constraint_type = 'CHECK' AND enforced = 'YES'")) {
                checks.next();
                assertThat(checks.getInt(1)).isEqualTo(4);
            }
            try (ResultSet sensitiveColumns = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = 'sys_user_activation' "
                    + "AND column_name IN ('token','password','initial_password')")) {
                sensitiveColumns.next();
                assertThat(sensitiveColumns.getInt(1)).isZero();
            }
        }
    }

    private void validateTables(DriverManagerDataSource dataSource) throws Exception {
        assertColumns(dataSource, "sys_saas_quota_counter", 10);
        assertColumns(dataSource, "sys_saas_quota_reservation", 16);
        assertColumns(dataSource, "sys_saas_usage_outbox", 16);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet indexes = statement.executeQuery("SELECT table_name, index_name, "
                    + "GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_value "
                    + "FROM information_schema.statistics WHERE table_schema = DATABASE() "
                    + "AND table_name IN ('sys_saas_quota_counter','sys_saas_quota_reservation',"
                    + "'sys_saas_usage_outbox') "
                    + "GROUP BY table_name, index_name")) {
                Map<String, String> actual = new HashMap<>();
                while (indexes.next()) {
                    actual.put(indexes.getString(1) + "." + indexes.getString(2), indexes.getString(3));
                }
                assertThat(actual).containsEntry("sys_saas_quota_counter.PRIMARY",
                                "tenant_id,metric_key,period_start")
                        .containsEntry("sys_saas_quota_reservation.uk_sys_saas_quota_reservation",
                                "tenant_id,metric_key,reservation_key")
                        .containsEntry("sys_saas_usage_outbox.uk_sys_saas_usage_outbox_event",
                                "tenant_id,event_key")
                        .containsEntry("sys_saas_usage_outbox.idx_sys_saas_usage_outbox_pending",
                                "tenant_id,status,next_attempt_at,outbox_id");
            }
            try (ResultSet checks = statement.executeQuery("SELECT COUNT(*) FROM information_schema.table_constraints "
                    + "WHERE table_schema = DATABASE() AND table_name IN "
                    + "('sys_saas_quota_counter','sys_saas_quota_reservation','sys_saas_usage_outbox') "
                    + "AND constraint_type = 'CHECK' AND enforced = 'YES'")) {
                checks.next();
                assertThat(checks.getInt(1)).isEqualTo(8);
            }
        }
    }

    private void seedCounter(DriverManagerDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO sys_saas_quota_counter "
                    + "(tenant_id, metric_key, period_start, used_amount, reserved_amount, create_by, "
                    + "create_time, update_by, update_time, version_no) VALUES "
                    + "('tenant-outbox-test', 'storage_bytes', '1970-01-01 00:00:00.000', 42, 0, "
                    + "'test', '2026-08-02 00:00:00.000', 'test', '2026-08-02 00:00:01.000', 0)");
        }
    }

    private void assertBootstrapSnapshot(DriverManagerDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet row = statement.executeQuery("SELECT amount, period_start, occurred_at, status, "
                        + "COUNT(*) OVER () FROM sys_saas_usage_outbox WHERE tenant_id = 'tenant-outbox-test' "
                        + "AND metric_key = 'storage_bytes'")) {
            assertThat(row.next()).isTrue();
            assertThat(row.getLong(1)).isEqualTo(42L);
            assertThat(row.getObject(2)).isNull();
            assertThat(row.getString(3)).startsWith("2026-08-02 00:00:01");
            assertThat(row.getString(4)).isEqualTo("PENDING");
            assertThat(row.getInt(5)).isEqualTo(1);
        }
    }

    private void assertColumns(DriverManagerDataSource dataSource, String table, int expected) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement();
                ResultSet columns = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
                        + "WHERE table_schema = DATABASE() AND table_name = '" + table + "'")) {
            columns.next();
            assertThat(columns.getInt(1)).isEqualTo(expected);
        }
    }

    private void execute(DriverManagerDataSource dataSource, String path) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new ClassPathResource(path), StandardCharsets.UTF_8));
        }
    }

    private void executeTwice(DriverManagerDataSource dataSource, String path) throws Exception {
        execute(dataSource, path);
        execute(dataSource, path);
    }

    private void validateUrl(String url) throws Exception {
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException("Loopback MySQL erp_system_test URL required");
        }
        URI uri = URI.create(url.substring(5));
        if (uri.getHost() == null
                || !InetAddress.getByName(uri.getHost()).isLoopbackAddress()
                || !"/erp_system_test".equals(uri.getPath())) {
            throw new IllegalStateException("Loopback MySQL erp_system_test URL required");
        }
    }

    private void validateConnection(Connection connection) throws Exception {
        try (connection; Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DATABASE(), VERSION(), @@datadir")) {
            result.next();
            assertThat(result.getString(1)).isEqualTo("erp_system_test");
            assertThat(result.getString(2)).startsWith("8.0.17");
            Path expected = worktree().resolve(".tmp/saas-mysql-8017").toRealPath();
            assertThat(Path.of(result.getString(3)).toRealPath()).startsWith(expected);
        }
    }

    private Path worktree() throws Exception {
        Path current = Path.of(getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
        while (current != null && !Files.exists(current.resolve(".git"))) current = current.getParent();
        if (current == null) throw new IllegalStateException("Worktree not found");
        return current.toRealPath();
    }

    private boolean text(String value) { return value != null && !value.isBlank(); }
}
