CREATE TABLE IF NOT EXISTS `saas_usage_event` (
    `usage_event_id` BIGINT NOT NULL,
    `idempotency_key` VARCHAR(128) NOT NULL,
    `tenant_id` VARCHAR(20) NOT NULL,
    `metric_key` VARCHAR(64) NOT NULL,
    `operation` VARCHAR(16) NOT NULL,
    `amount` BIGINT NOT NULL,
    `period_start` DATETIME(3) NOT NULL,
    `occurred_at` DATETIME(3) NOT NULL,
    `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL,
    `update_time` DATETIME(3) NOT NULL,
    PRIMARY KEY (`usage_event_id`),
    UNIQUE KEY `uk_saas_usage_event_idempotency` (`idempotency_key`),
    KEY `idx_saas_usage_event_tenant_metric_period` (`tenant_id`, `metric_key`, `period_start`),
    KEY `idx_saas_usage_event_occurred` (`occurred_at`),
    CONSTRAINT `ck_saas_usage_event_operation` CHECK (`operation` = 'REPORT'),
    CONSTRAINT `ck_saas_usage_event_amount` CHECK (`amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_usage_summary` (
    `usage_summary_id` BIGINT NOT NULL,
    `tenant_id` VARCHAR(20) NOT NULL,
    `metric_key` VARCHAR(64) NOT NULL,
    `period_start` DATETIME(3) NOT NULL,
    `used_amount` BIGINT NOT NULL,
    `last_event_key` VARCHAR(128) NOT NULL,
    `last_occurred_at` DATETIME(3) NOT NULL,
    `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL,
    `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`usage_summary_id`),
    UNIQUE KEY `uk_saas_usage_summary_period` (`tenant_id`, `metric_key`, `period_start`),
    KEY `idx_saas_usage_summary_metric_period` (`metric_key`, `period_start`),
    CONSTRAINT `ck_saas_usage_summary_amount` CHECK (`used_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
