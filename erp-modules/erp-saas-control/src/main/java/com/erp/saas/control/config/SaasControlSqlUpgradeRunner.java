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
    private static final String CREATE_HISTORY_TABLE_SQL = "CREATE TABLE IF NOT EXISTS `saas_sql_upgrade_log` ("
            + "`log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',"
            + "`script_name` VARCHAR(255) NOT NULL COMMENT 'Upgrade script filename',"
            + "`checksum` CHAR(64) NOT NULL COMMENT 'SHA-256 checksum',"
            + "`status` CHAR(1) NOT NULL DEFAULT '1' COMMENT 'Execution status: 1 success',"
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
            ensureHistoryTable(connection);
            int executed = 0;
            for (Resource script : scripts) {
                String scriptName = safeFilename(script);
                String checksum = calculateChecksum(script, scriptName);
                HistoryRow history = findHistory(connection, scriptName);
                if (history != null) {
                    assertMatchingHistory(scriptName, checksum, history);
                    continue;
                }
                executeScript(connection, script, scriptName, checksum);
                executed++;
            }
            log.info("SaaS control SQL upgrades completed: executed={}, scanned={}", executed, scripts.size());
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

    private void assertMatchingHistory(String scriptName, String checksum, HistoryRow history) {
        if (!"1".equals(history.status())) {
            throw new IllegalStateException("SaaS control SQL history is not successful: " + scriptName);
        }
        if (!checksum.equals(history.checksum())) {
            throw new IllegalStateException("SaaS control SQL checksum mismatch: " + scriptName);
        }
    }

    private void executeScript(Connection connection, Resource resource, String scriptName, String checksum) {
        try {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
            saveHistory(connection, scriptName, checksum, "Automatic execution succeeded");
            log.info("SaaS control SQL upgrade executed: {}", scriptName);
        } catch (Exception ex) {
            throw new IllegalStateException("SaaS control SQL upgrade failed: " + scriptName, ex);
        }
    }

    private void saveHistory(Connection connection, String scriptName, String checksum, String remark)
            throws SQLException {
        String sql = "INSERT INTO `" + HISTORY_TABLE_NAME
                + "` (`script_name`, `checksum`, `status`, `executed_at`, `remark`) VALUES (?, ?, '1', NOW(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, scriptName);
            statement.setString(2, checksum);
            statement.setString(3, sanitizeRemark(remark));
            statement.executeUpdate();
        }
    }

    private String calculateChecksum(Resource resource, String scriptName) {
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(inputStream.readAllBytes());
            return HexFormat.of().formatHex(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to calculate SaaS control SQL checksum: " + scriptName, ex);
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
