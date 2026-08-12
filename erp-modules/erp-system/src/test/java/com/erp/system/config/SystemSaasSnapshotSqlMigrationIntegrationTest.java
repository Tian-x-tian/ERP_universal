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
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

class SystemSaasSnapshotSqlMigrationIntegrationTest {
    private static final String URL = "ERP_SYSTEM_TEST_JDBC_URL";
    private static final String USER = "ERP_SYSTEM_TEST_DB_USER";
    private static final String PASSWORD = "ERP_SYSTEM_TEST_DB_PASSWORD";

    @Test
    void shouldRunInitOnceAndUpgradeTwiceThroughScriptUtils() throws Exception {
        String url = System.getenv(URL);
        String user = System.getenv(USER);
        String password = System.getenv(PASSWORD);
        Assumptions.assumeTrue(text(url) && text(user) && password != null,
                "Isolated MySQL variables required");
        validateUrl(url);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        validateConnection(dataSource.getConnection());

        execute(dataSource, "sql/init_system.sql");
        validateSnapshotTable(dataSource);

        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE `sys_saas_entitlement_snapshot`");
        }
        execute(dataSource, "sql/upgrade/system/20260802_01_saas_entitlement_snapshot.sql");
        execute(dataSource, "sql/upgrade/system/20260802_01_saas_entitlement_snapshot.sql");
        validateSnapshotTable(dataSource);
    }

    private void validateSnapshotTable(DriverManagerDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet columns = statement.executeQuery("SELECT column_name, is_nullable FROM "
                    + "information_schema.columns WHERE table_schema = DATABASE() "
                    + "AND table_name = 'sys_saas_entitlement_snapshot' ORDER BY ordinal_position")) {
                java.util.List<String> names = new java.util.ArrayList<>();
                while (columns.next()) {
                    names.add(columns.getString(1));
                    assertThat(columns.getString(2)).isEqualTo("NO");
                }
                assertThat(names).containsExactly("tenant_id", "snapshot_version", "snapshot_json",
                        "issued_at", "expires_at", "signature_key_id", "signature", "create_by",
                        "create_time", "update_by", "update_time", "version_no");
            }
            try (ResultSet indexes = statement.executeQuery("SELECT index_name, GROUP_CONCAT(column_name "
                    + "ORDER BY seq_in_index) FROM information_schema.statistics WHERE table_schema = DATABASE() "
                    + "AND table_name = 'sys_saas_entitlement_snapshot' GROUP BY index_name")) {
                java.util.Map<String, String> actual = new java.util.HashMap<>();
                while (indexes.next()) actual.put(indexes.getString(1), indexes.getString(2));
                assertThat(actual).containsEntry("PRIMARY", "tenant_id")
                        .containsEntry("idx_sys_saas_snapshot_expiry", "tenant_id,expires_at");
            }
            try (ResultSet checks = statement.executeQuery("SELECT constraint_name, enforced FROM "
                    + "information_schema.check_constraints c JOIN information_schema.table_constraints t "
                    + "USING (constraint_schema, constraint_name) WHERE t.table_schema = DATABASE() "
                    + "AND t.table_name = 'sys_saas_entitlement_snapshot'")) {
                java.util.Map<String, String> actual = new java.util.HashMap<>();
                while (checks.next()) actual.put(checks.getString(1), checks.getString(2));
                assertThat(actual).containsEntry("ck_sys_saas_snapshot_version", "YES")
                        .containsEntry("ck_sys_saas_snapshot_lease", "YES");
            }
        }
    }

    private void execute(DriverManagerDataSource dataSource, String path) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new ClassPathResource(path), StandardCharsets.UTF_8));
        }
    }

    private void validateUrl(String url) throws Exception {
        if (!text(url) || !url.startsWith("jdbc:mysql://")) throw new IllegalStateException("MySQL URL required");
        URI uri = URI.create(url.substring(5));
        if (uri.getHost() == null || !InetAddress.getByName(uri.getHost()).isLoopbackAddress()) {
            throw new IllegalStateException("Loopback MySQL required");
        }
        if (!"/erp_system_test".equals(uri.getPath())) {
            throw new IllegalStateException("Database erp_system_test required");
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
