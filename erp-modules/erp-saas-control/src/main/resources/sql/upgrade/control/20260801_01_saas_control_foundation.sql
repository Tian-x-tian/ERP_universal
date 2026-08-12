CREATE TABLE IF NOT EXISTS `saas_sql_upgrade_log` (
    `log_id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    `script_name` VARCHAR(255) NOT NULL COMMENT 'Upgrade script filename',
    `checksum` CHAR(64) NOT NULL COMMENT 'SHA-256 checksum',
    `status` CHAR(1) NOT NULL DEFAULT '0' COMMENT 'Execution status: 0 running, 1 success, 2 failed',
    `executed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Execution time',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT 'Execution remark',
    PRIMARY KEY (`log_id`),
    UNIQUE KEY `uk_saas_sql_upgrade_script` (`script_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='SaaS control SQL upgrade history';
