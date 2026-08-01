package com.erp.saas.control.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;

@Component
public class SaasControlSqlUpgradeRunner implements ApplicationRunner, Ordered {
    private static final Logger log = LoggerFactory.getLogger(SaasControlSqlUpgradeRunner.class);
    private static final String DEFAULT_SQL_LOCATION = "classpath:sql/upgrade/control/*.sql";
    private static final String HISTORY_TABLE_NAME = "saas_sql_upgrade_log";
    private static final String STATUS_RUNNING = "0";
    private static final String STATUS_SUCCESS = "1";
    private static final String STATUS_FAILED = "2";
    private static final int LOCK_TIMEOUT_SECONDS = 60;
    private static final String CREATE_HISTORY_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `saas_sql_upgrade_log` ("
            + "`log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',"
            + "`script_name` VARCHAR(255) NOT NULL COMMENT 'Upgrade script filename',"
            + "`checksum` CHAR(64) NOT NULL COMMENT 'SHA-256 checksum',"
            + "`status` CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Execution status: 0 running, 1 success, 2 failed',"
            + "`executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Execution time',"
            + "`remark` VARCHAR(500) DEFAULT NULL COMMENT 'Execution remark',"
            + "PRIMARY KEY (`log_id`),"
            + "UNIQUE KEY `uk_saas_sql_upgrade_script` (`script_name`)"
            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SaaS control SQL upgrade history'";

    private final DataSource dataSource;
    private final ResourcePatternResolver resourcePatternResolver;

    @Value("${erp.saas.sql.upgrade.enabled:true}")
    private boolean upgradeEnabled = true;

    @Value("${erp.saas.sql.upgrade.location:" + DEFAULT_SQL_LOCATION + "}")
    private String upgradeLocation = DEFAULT_SQL_LOCATION;

    @Autowired
    public SaasControlSqlUpgradeRunner(DataSource dataSource) {
        this(dataSource, new PathMatchingResourcePatternResolver());
    }

    SaasControlSqlUpgradeRunner(DataSource dataSource, ResourcePatternResolver resourcePatternResolver) {
        this.dataSource = dataSource;
        this.resourcePatternResolver = resourcePatternResolver;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!upgradeEnabled) {
            log.info("SaaS control SQL upgrades are disabled.");
            return;
        }
        if (!StringUtils.hasText(upgradeLocation)) {
            log.info("SaaS control SQL upgrade location is empty; skipping.");
            return;
        }
        List<Resource> scripts = loadScripts();
        if (scripts.isEmpty()) {
            log.info("No SaaS control SQL upgrade scripts found.");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean locked = false;
            String lockName = null;
            try {
                lockName = buildLockName(connection);
                acquireUpgradeLock(connection, lockName);
                locked = true;
                ensureHistoryTable(connection);
                int executed = 0;
                for (Resource script : scripts) {
                    String scriptName = safeFilename(script);
                    String checksum = calculateChecksum(script, scriptName);
                    HistoryRow history = findHistory(connection, scriptName);
                    if (history != null && shouldSkip(scriptName, checksum, history)) {
                        continue;
                    }
                    executeScript(connection, script, scriptName, checksum, history != null);
                    executed++;
                }
                log.info("SaaS control SQL upgrades completed: executed={}, scanned={}", executed, scripts.size());
            } finally {
                if (locked) {
                    releaseUpgradeLock(connection, lockName);
                }
            }
        }
    }

    @Override
    public int getOrder() {
        return 100;
    }

    private List<Resource> loadScripts() throws Exception {
        Resource[] resources = resourcePatternResolver.getResources(upgradeLocation.trim());
        List<Resource> scripts = new ArrayList<>();
        for (Resource resource : resources) {
            if (resource != null && resource.exists() && resource.isReadable()) {
                scripts.add(resource);
            }
        }
        scripts.sort(Comparator.comparing(this::safeFilename));
        return scripts;
    }

    private void ensureHistoryTable(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_HISTORY_TABLE_SQL);
        }
    }

    private HistoryRow findHistory(Connection connection, String scriptName) throws SQLException {
        String sql = "SELECT `checksum`, `status` FROM `" + HISTORY_TABLE_NAME + "` WHERE `script_name` = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scriptName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return new HistoryRow(resultSet.getString("checksum"), resultSet.getString("status"));
                }
            }
        }
        return null;
    }

    private boolean shouldSkip(String scriptName, String checksum, HistoryRow history) {
        if (!checksum.equals(history.checksum())) {
            throw new IllegalStateException("SaaS control SQL checksum mismatch: " + scriptName);
        }
        if (STATUS_SUCCESS.equals(history.status())) {
            return true;
        }
        if (STATUS_RUNNING.equals(history.status()) || STATUS_FAILED.equals(history.status())) {
            return false;
        }
        throw new IllegalStateException("SaaS control SQL history has unknown status: " + scriptName);
    }

    private void executeScript(Connection connection, Resource resource, String scriptName, String checksum,
            boolean existingHistory) throws SQLException {
        boolean originalAutoCommit = connection.getAutoCommit();
        try {
            connection.setAutoCommit(true);
            markRunning(connection, scriptName, checksum, existingHistory);
            connection.setAutoCommit(false);
            try {
                // MySQL DDL commits implicitly; scripts must remain idempotent. DML and the SUCCESS update commit together.
                ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
                updateHistory(connection, scriptName, checksum, STATUS_SUCCESS, "Automatic execution succeeded");
                connection.commit();
                log.info("SaaS control SQL upgrade executed: {}", scriptName);
            } catch (Exception ex) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackFailure) {
                    ex.addSuppressed(rollbackFailure);
                }
                connection.setAutoCommit(true);
                try {
                    updateHistory(connection, scriptName, checksum, STATUS_FAILED,
                            "Automatic execution failed; retry required");
                } catch (SQLException historyFailure) {
                    ex.addSuppressed(historyFailure);
                }
                throw new IllegalStateException("SaaS control SQL upgrade failed: " + scriptName, ex);
            }
        } finally {
            if (connection.getAutoCommit() != originalAutoCommit) {
                connection.setAutoCommit(originalAutoCommit);
            }
        }
    }

    private void markRunning(Connection connection, String scriptName, String checksum, boolean existingHistory)
            throws SQLException {
        if (existingHistory) {
            updateHistory(connection, scriptName, checksum, STATUS_RUNNING, "Automatic execution retry started");
            return;
        }
        String sql = "INSERT INTO `" + HISTORY_TABLE_NAME
                + "` (`script_name`, `checksum`, `status`, `executed_at`, `remark`) VALUES (?, ?, ?, NOW(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scriptName);
            statement.setString(2, checksum);
            statement.setString(3, STATUS_RUNNING);
            statement.setString(4, sanitizeRemark("Automatic execution started"));
            statement.executeUpdate();
        }
    }

    private void updateHistory(Connection connection, String scriptName, String checksum, String status, String remark)
            throws SQLException {
        String sql = "UPDATE `" + HISTORY_TABLE_NAME
                + "` SET `checksum` = ?, `status` = ?, `executed_at` = NOW(), `remark` = ? WHERE `script_name` = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, checksum);
            statement.setString(2, status);
            statement.setString(3, sanitizeRemark(remark));
            statement.setString(4, scriptName);
            statement.executeUpdate();
        }
    }

    private String buildLockName(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT DATABASE()");
                ResultSet resultSet = statement.executeQuery()) {
            if (!resultSet.next() || !StringUtils.hasText(resultSet.getString(1))) {
                throw new IllegalStateException("SaaS control SQL upgrade requires a selected database");
            }
            return sha256(resultSet.getString(1) + ":" + SaasControlSqlUpgradeRunner.class.getName());
        }
    }

    private void acquireUpgradeLock(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT GET_LOCK(?, ?)")) {
            statement.setString(1, lockName);
            statement.setInt(2, LOCK_TIMEOUT_SECONDS);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new IllegalStateException("Unable to acquire SaaS control SQL upgrade lock");
                }
            }
        }
    }

    private void releaseUpgradeLock(Connection connection, String lockName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT RELEASE_LOCK(?)")) {
            statement.setString(1, lockName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || resultSet.getInt(1) != 1) {
                    throw new IllegalStateException("Unable to release SaaS control SQL upgrade lock");
                }
            }
        }
    }

    private String calculateChecksum(Resource resource, String scriptName) {
        try (InputStream inputStream = resource.getInputStream()) {
            return sha256(inputStream.readAllBytes());
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate SaaS control SQL checksum: " + scriptName, ex);
        }
    }

    private String sha256(String value) {
        return sha256(value.getBytes(StandardCharsets.UTF_8));
    }

    private String sha256(byte[] value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate SHA-256", ex);
        }
    }

    private String sanitizeRemark(String remark) {
        if (!StringUtils.hasText(remark)) {
            return null;
        }
        String sanitized = remark.replace('\r', ' ').replace('\n', ' ').trim();
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }

    private String safeFilename(Resource resource) {
        String filename = resource == null ? null : resource.getFilename();
        return StringUtils.hasText(filename) ? filename : "unknown.sql";
    }

    private record HistoryRow(String checksum, String status) {
    }
}
