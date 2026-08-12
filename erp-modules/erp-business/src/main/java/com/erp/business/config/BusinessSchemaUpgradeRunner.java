package com.erp.business.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Formatter;
import java.util.List;

/**
 * 业务模块启动期增量脚本执行器。
 * 仅执行未执行过的历史兼容脚本和日期版本脚本，避免每次启动重复执行。
 */
@Component
public class BusinessSchemaUpgradeRunner implements ApplicationRunner, Ordered {
    private static final Logger log = LoggerFactory.getLogger(BusinessSchemaUpgradeRunner.class);
    private static final String DEFAULT_LEGACY_SCRIPT_LOCATION = "classpath:sql/upgrade_business.sql";
    private static final String DEFAULT_VERSIONED_SCRIPT_LOCATION = "classpath*:sql/upgrade/business/*.sql";
    private static final String HISTORY_TABLE_NAME = "biz_sql_upgrade_log";

    private final DataSource dataSource;
    private final ResourcePatternResolver resourcePatternResolver;

    @Value("${erp.sql.upgrade.enabled:true}")
    private boolean upgradeEnabled;

    @Value("${erp.sql.upgrade.legacy-location:" + DEFAULT_LEGACY_SCRIPT_LOCATION + "}")
    private String legacyScriptLocation;

    @Value("${erp.sql.upgrade.location:" + DEFAULT_VERSIONED_SCRIPT_LOCATION + "}")
    private String versionedScriptLocation;

    public BusinessSchemaUpgradeRunner(DataSource dataSource) {
        this.dataSource = dataSource;
        this.resourcePatternResolver = new PathMatchingResourcePatternResolver();
    }

    /**
     * 应用启动后自动执行业务模块增量脚本，优先补齐历史库缺失对象。
     *
     * @param args 启动参数
     * @throws Exception 脚本执行异常
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        if (!upgradeEnabled) {
            log.info("业务增量 SQL 自动执行已关闭。");
            return;
        }
        List<Resource> scriptList = loadScriptList();
        if (scriptList.isEmpty()) {
            log.info("未发现可执行的业务增量 SQL 脚本。");
            return;
        }
        try (Connection connection = dataSource.getConnection()) {
            ensureUpgradeHistoryTable(connection);
            int executedCount = 0;
            for (Resource resource : scriptList) {
                String scriptName = safeFileName(resource);
                if (isScriptExecuted(connection, scriptName)) {
                    continue;
                }
                executeScript(connection, resource, scriptName);
                executedCount++;
            }
            log.info("业务增量 SQL 执行完成，执行数量：{}，扫描数量：{}", executedCount, scriptList.size());
        }
    }

    @Override
    public int getOrder() {
        return 100;
    }

    /**
     * 加载业务增量脚本列表，历史兼容脚本优先执行，版本化脚本按文件名排序。
     *
     * @return 脚本资源列表
     * @throws Exception 资源加载异常
     */
    private List<Resource> loadScriptList() throws Exception {
        List<Resource> scriptList = new ArrayList<>();
        if (StringUtils.hasText(legacyScriptLocation)) {
            Resource legacyScript = resourcePatternResolver.getResource(legacyScriptLocation.trim());
            if (legacyScript.exists() && legacyScript.isReadable()) {
                scriptList.add(legacyScript);
            }
        }
        if (StringUtils.hasText(versionedScriptLocation)) {
            Resource[] resources = resourcePatternResolver.getResources(versionedScriptLocation.trim());
            List<Resource> versionedScripts = new ArrayList<>();
            for (Resource resource : resources) {
                if (resource != null && resource.exists() && resource.isReadable()) {
                    versionedScripts.add(resource);
                }
            }
            versionedScripts.sort(Comparator.comparing(this::safeFileName));
            scriptList.addAll(versionedScripts);
        }
        return scriptList;
    }

    /**
     * 确保业务增量脚本执行记录表存在。
     *
     * @param connection 数据库连接
     * @throws SQLException SQL 执行异常
     */
    private void ensureUpgradeHistoryTable(Connection connection) throws SQLException {
        String createSql = "CREATE TABLE IF NOT EXISTS `" + HISTORY_TABLE_NAME + "` ("
                + "`log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',"
                + "`script_name` VARCHAR(255) NOT NULL COMMENT '脚本文件名',"
                + "`checksum` VARCHAR(64) DEFAULT NULL COMMENT '脚本摘要',"
                + "`status` CHAR(1) NOT NULL DEFAULT '1' COMMENT '状态（1成功）',"
                + "`executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '执行时间',"
                + "`remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',"
                + "PRIMARY KEY (`log_id`),"
                + "UNIQUE KEY `uk_biz_sql_upgrade_script` (`script_name`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='业务增量SQL执行记录表'";
        try (Statement statement = connection.createStatement()) {
            statement.execute(createSql);
        }
    }

    /**
     * 判断脚本是否已执行。
     *
     * @param connection 数据库连接
     * @param scriptName 脚本名称
     * @return true 表示已执行
     * @throws SQLException SQL 异常
     */
    private boolean isScriptExecuted(Connection connection, String scriptName) throws SQLException {
        String querySql = "SELECT COUNT(1) FROM `" + HISTORY_TABLE_NAME + "` WHERE `script_name` = ?";
        try (PreparedStatement statement = connection.prepareStatement(querySql)) {
            statement.setString(1, scriptName);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    /**
     * 执行单个 SQL 脚本并记录执行结果。
     *
     * @param connection 数据库连接
     * @param resource   SQL 资源
     * @param scriptName 脚本名称
     */
    private void executeScript(Connection connection, Resource resource, String scriptName) {
        LocalDateTime startTime = LocalDateTime.now();
        try {
            ScriptUtils.executeSqlScript(connection, new EncodedResource(resource, StandardCharsets.UTF_8));
            saveUpgradeLog(connection, scriptName, buildChecksum(resource), "自动执行成功");
            log.info("业务增量 SQL 执行成功：{}", scriptName);
        } catch (Exception ex) {
            throw new IllegalStateException("业务增量 SQL 执行失败：" + scriptName, ex);
        } finally {
            LocalDateTime endTime = LocalDateTime.now();
            log.debug("业务增量 SQL 执行耗时：{} -> {}，脚本：{}", startTime, endTime, scriptName);
        }
    }

    /**
     * 保存脚本执行记录。
     *
     * @param connection 数据库连接
     * @param scriptName 脚本名称
     * @param checksum   脚本摘要
     * @param remark     备注
     * @throws SQLException SQL 异常
     */
    private void saveUpgradeLog(Connection connection, String scriptName, String checksum, String remark) throws SQLException {
        String insertSql = "INSERT INTO `" + HISTORY_TABLE_NAME + "` (`script_name`, `checksum`, `status`, `executed_at`, `remark`) "
                + "VALUES (?, ?, '1', NOW(), ?)";
        try (PreparedStatement statement = connection.prepareStatement(insertSql)) {
            statement.setString(1, scriptName);
            statement.setString(2, checksum);
            statement.setString(3, remark);
            statement.executeUpdate();
        }
    }

    /**
     * 计算脚本摘要。
     *
     * @param resource SQL 资源
     * @return SHA-256 摘要
     */
    private String buildChecksum(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            byte[] content = inputStream.readAllBytes();
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(content);
            return toHex(hash);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 转换为十六进制字符串。
     *
     * @param data 字节数组
     * @return 十六进制字符串
     */
    private String toHex(byte[] data) {
        if (data == null || data.length == 0) {
            return null;
        }
        try (Formatter formatter = new Formatter()) {
            for (byte value : data) {
                formatter.format("%02x", value);
            }
            return formatter.toString();
        }
    }

    /**
     * 安全读取脚本文件名。
     *
     * @param resource 资源对象
     * @return 文件名
     */
    private String safeFileName(Resource resource) {
        String fileName = resource == null ? null : resource.getFilename();
        return StringUtils.hasText(fileName) ? fileName : "unknown.sql";
    }
}
