package com.erp.business.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
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

import static org.assertj.core.api.Assertions.assertThat;

class BusinessSaasStorageSqlMigrationIntegrationTest {
    private static final String URL = "ERP_BUSINESS_TEST_JDBC_URL";
    private static final String USER = "ERP_BUSINESS_TEST_DB_USER";
    private static final String PASSWORD = "ERP_BUSINESS_TEST_DB_PASSWORD";

    @Test
    void shouldRunInitOnceAndStorageUpgradeTwiceThroughScriptUtils() throws Exception {
        String url = System.getenv(URL);
        String user = System.getenv(USER);
        String password = System.getenv(PASSWORD);
        Assumptions.assumeTrue(text(url) && text(user) && password != null,
                "Isolated MySQL variables required");
        validateUrl(url);
        DriverManagerDataSource dataSource = new DriverManagerDataSource(url, user, password);
        validateConnection(dataSource.getConnection());

        execute(dataSource, new FileSystemResource(worktree().resolve(
                "erp-modules/erp-system/src/main/resources/sql/init_system.sql")));
        execute(dataSource, new ClassPathResource("sql/init_business.sql"));
        validateSchema(dataSource);
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE `biz_saas_storage_object`");
            statement.execute("ALTER TABLE `hr_employee_document` DROP COLUMN `file_size`");
        }
        Resource upgrade = new ClassPathResource("sql/upgrade/business/20260802_01_saas_storage_objects.sql");
        execute(dataSource, upgrade);
        execute(dataSource, upgrade);
        validateSchema(dataSource);
    }

    private void validateSchema(DriverManagerDataSource dataSource) throws Exception {
        try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {
            try (ResultSet columns = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = 'hr_employee_document' "
                    + "AND column_name = 'file_size' AND data_type = 'bigint'")) {
                columns.next();
                assertThat(columns.getInt(1)).isEqualTo(1);
            }
            try (ResultSet columns = statement.executeQuery("SELECT COUNT(*) FROM information_schema.columns "
                    + "WHERE table_schema = DATABASE() AND table_name = 'biz_saas_storage_object'")) {
                columns.next();
                assertThat(columns.getInt(1)).isEqualTo(11);
            }
            try (ResultSet indexes = statement.executeQuery("SELECT GROUP_CONCAT(column_name ORDER BY seq_in_index) "
                    + "FROM information_schema.statistics WHERE table_schema = DATABASE() "
                    + "AND table_name = 'biz_saas_storage_object' "
                    + "AND index_name = 'uk_biz_saas_storage_object' GROUP BY index_name")) {
                indexes.next();
                assertThat(indexes.getString(1)).isEqualTo("tenant_id,object_key");
            }
            try (ResultSet checks = statement.executeQuery("SELECT COUNT(*) FROM information_schema.table_constraints "
                    + "WHERE table_schema = DATABASE() AND table_name = 'biz_saas_storage_object' "
                    + "AND constraint_type = 'CHECK' AND enforced = 'YES'")) {
                checks.next();
                assertThat(checks.getInt(1)).isEqualTo(2);
            }
        }
    }

    private void execute(DriverManagerDataSource dataSource, Resource resource) throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(resource, StandardCharsets.UTF_8));
        }
    }

    private void validateUrl(String url) throws Exception {
        if (!url.startsWith("jdbc:mysql://")) {
            throw new IllegalStateException("Loopback MySQL erp_business_test URL required");
        }
        URI uri = URI.create(url.substring(5));
        if (uri.getHost() == null || !InetAddress.getByName(uri.getHost()).isLoopbackAddress()
                || !"/erp_business_test".equals(uri.getPath())) {
            throw new IllegalStateException("Loopback MySQL erp_business_test URL required");
        }
    }

    private void validateConnection(Connection connection) throws Exception {
        try (connection; Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("SELECT DATABASE(), VERSION(), @@datadir")) {
            result.next();
            assertThat(result.getString(1)).isEqualTo("erp_business_test");
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

    private boolean text(String value) {
        return value != null && !value.isBlank();
    }
}
