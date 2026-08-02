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
    void shouldRunInitOnceAndQuotaUpgradeTwiceThroughScriptUtils() throws Exception {
        String url = System.getenv(URL);
        String user = System.getenv(USER);
        String password = System.getenv(PASSWORD);
        Assumptions.assumeTrue(text(url) && text(user) && password != null,
                "Isolated MySQL variables required");
        validateUrl(url);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        validateConnection(dataSource.getConnection());

        execute(dataSource, "sql/init_system.sql");
        validateTables(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE `sys_saas_quota_reservation`");
            statement.execute("DROP TABLE `sys_saas_quota_counter`");
        }
        execute(dataSource, "sql/upgrade/system/20260802_02_saas_quota_counters.sql");
        execute(dataSource, "sql/upgrade/system/20260802_02_saas_quota_counters.sql");
        validateTables(dataSource);
    }

    private void validateTables(DriverManagerDataSource dataSource) throws Exception {
        assertColumns(dataSource, "sys_saas_quota_counter", 10);
        assertColumns(dataSource, "sys_saas_quota_reservation", 16);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet indexes = statement.executeQuery("SELECT table_name, index_name, "
                    + "GROUP_CONCAT(column_name ORDER BY seq_in_index) columns_value "
                    + "FROM information_schema.statistics WHERE table_schema = DATABASE() "
                    + "AND table_name IN ('sys_saas_quota_counter','sys_saas_quota_reservation') "
                    + "GROUP BY table_name, index_name")) {
                Map<String, String> actual = new HashMap<>();
                while (indexes.next()) {
                    actual.put(indexes.getString(1) + "." + indexes.getString(2), indexes.getString(3));
                }
                assertThat(actual).containsEntry("sys_saas_quota_counter.PRIMARY",
                                "tenant_id,metric_key,period_start")
                        .containsEntry("sys_saas_quota_reservation.uk_sys_saas_quota_reservation",
                                "tenant_id,metric_key,reservation_key");
            }
            try (ResultSet checks = statement.executeQuery("SELECT COUNT(*) FROM information_schema.table_constraints "
                    + "WHERE table_schema = DATABASE() AND table_name IN "
                    + "('sys_saas_quota_counter','sys_saas_quota_reservation') "
                    + "AND constraint_type = 'CHECK' AND enforced = 'YES'")) {
                checks.next();
                assertThat(checks.getInt(1)).isEqualTo(4);
            }
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
