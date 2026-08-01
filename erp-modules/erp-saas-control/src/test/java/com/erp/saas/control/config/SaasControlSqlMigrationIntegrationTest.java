package com.erp.saas.control.config;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SaasControlSqlMigrationIntegrationTest {
    private static final String JDBC_URL_ENV = "ERP_SAAS_TEST_JDBC_URL";
    private static final String DB_USER_ENV = "ERP_SAAS_TEST_DB_USER";
    private static final String DB_PASSWORD_ENV = "ERP_SAAS_TEST_DB_PASSWORD";
    private static final String UPGRADE_LOCATION = "classpath:sql/upgrade/control/*.sql";
    private static final Resource INIT_SCRIPT = new ClassPathResource("sql/init_control.sql");
    private static final Resource UPGRADE_SCRIPT =
            new ClassPathResource("sql/upgrade/control/20260801_01_saas_control_foundation.sql");

    @Test
    void shouldKeepRunnerInitAndUpgradeMetadataIdenticalAndIdempotent() throws Exception {
        String jdbcUrl = System.getenv(JDBC_URL_ENV);
        String username = System.getenv(DB_USER_ENV);
        String password = System.getenv(DB_PASSWORD_ENV);
        Assumptions.assumeTrue(hasText(jdbcUrl) && hasText(username) && password != null,
                "Isolated MySQL environment variables are required");

        DriverManagerDataSource dataSource = new DriverManagerDataSource(jdbcUrl, username, password);

        resetTable(jdbcUrl, username, password);
        SaasControlSqlUpgradeRunner runner = new SaasControlSqlUpgradeRunner(dataSource);
        ReflectionTestUtils.setField(runner, "upgradeEnabled", true);
        ReflectionTestUtils.setField(runner, "upgradeLocation", UPGRADE_LOCATION);
        runner.run(new DefaultApplicationArguments(new String[0]));
        runner.run(new DefaultApplicationArguments(new String[0]));

        SchemaSignature runnerSignature;
        try (Connection connection = dataSource.getConnection()) {
            assertThat(historyCount(connection)).isEqualTo(1);
            assertThat(historyChecksum(connection)).isEqualTo(checksum(UPGRADE_SCRIPT));
            assertScriptNameUnique(connection);
            runnerSignature = signature(connection);
        }

        resetTable(jdbcUrl, username, password);
        SchemaSignature initSignature;
        try (Connection connection = dataSource.getConnection()) {
            executeTwice(connection, INIT_SCRIPT);
            assertScriptNameUnique(connection);
            initSignature = signature(connection);
        }

        resetTable(jdbcUrl, username, password);
        SchemaSignature upgradeSignature;
        try (Connection connection = dataSource.getConnection()) {
            executeTwice(connection, UPGRADE_SCRIPT);
            assertScriptNameUnique(connection);
            upgradeSignature = signature(connection);
        }

        assertThat(initSignature).isEqualTo(runnerSignature);
        assertThat(upgradeSignature).isEqualTo(runnerSignature);
    }

    private void executeTwice(Connection connection, Resource resource) {
        EncodedResource encoded = new EncodedResource(resource, StandardCharsets.UTF_8);
        ScriptUtils.executeSqlScript(connection, encoded);
        ScriptUtils.executeSqlScript(connection, encoded);
    }

    private void resetTable(String jdbcUrl, String username, String password) throws Exception {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
                Statement statement = connection.createStatement()) {
            statement.execute("DROP TABLE IF EXISTS `saas_sql_upgrade_log`");
        }
    }

    private int historyCount(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT COUNT(*) FROM `saas_sql_upgrade_log`")) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String historyChecksum(Connection connection) throws Exception {
        try (Statement statement = connection.createStatement();
                ResultSet resultSet = statement.executeQuery(
                        "SELECT `checksum` FROM `saas_sql_upgrade_log` WHERE `script_name` = "
                                + "'20260801_01_saas_control_foundation.sql'")) {
            assertThat(resultSet.next()).isTrue();
            return resultSet.getString(1);
        }
    }

    private void assertScriptNameUnique(Connection connection) throws Exception {
        String sql = "SELECT COUNT(*) FROM information_schema.STATISTICS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'saas_sql_upgrade_log' "
                + "AND COLUMN_NAME = 'script_name' AND NON_UNIQUE = 0";
        try (Statement statement = connection.createStatement(); ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            assertThat(resultSet.getInt(1)).isEqualTo(1);
        }
    }

    private SchemaSignature signature(Connection connection) throws Exception {
        List<String> columns = new ArrayList<>();
        String columnSql = "SELECT COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_DEFAULT, EXTRA "
                + "FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = 'saas_sql_upgrade_log' ORDER BY ORDINAL_POSITION";
        try (PreparedStatement statement = connection.prepareStatement(columnSql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                columns.add(resultSet.getString(1) + "|" + resultSet.getString(2) + "|"
                        + resultSet.getString(3) + "|" + resultSet.getString(4) + "|" + resultSet.getString(5));
            }
        }

        List<String> indexes = new ArrayList<>();
        String indexSql = "SELECT INDEX_NAME, NON_UNIQUE, SEQ_IN_INDEX, COLUMN_NAME "
                + "FROM information_schema.STATISTICS WHERE TABLE_SCHEMA = DATABASE() "
                + "AND TABLE_NAME = 'saas_sql_upgrade_log' ORDER BY INDEX_NAME, SEQ_IN_INDEX";
        try (PreparedStatement statement = connection.prepareStatement(indexSql);
                ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                indexes.add(resultSet.getString(1) + "|" + resultSet.getInt(2) + "|"
                        + resultSet.getInt(3) + "|" + resultSet.getString(4));
            }
        }
        return new SchemaSignature(List.copyOf(columns), List.copyOf(indexes));
    }

    private String checksum(Resource resource) throws Exception {
        try (var input = resource.getInputStream()) {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(input.readAllBytes()));
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record SchemaSignature(List<String> columns, List<String> indexes) {
    }
}
