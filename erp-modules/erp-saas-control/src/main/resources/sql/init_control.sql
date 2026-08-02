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
CREATE TABLE IF NOT EXISTS `saas_tenant` (
    `id` BIGINT NOT NULL, `tenant_id` VARCHAR(20) NOT NULL, `slug` VARCHAR(64) NOT NULL,
    `tenant_name` VARCHAR(128) NOT NULL, `lifecycle_state` VARCHAR(32) NOT NULL,
    `suspended_from_state` VARCHAR(32) DEFAULT NULL, `archived_at` DATETIME(3) DEFAULT NULL,
    `purge_eligible_at` DATETIME(3) DEFAULT NULL, `purged_at` DATETIME(3) DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`id`),
    UNIQUE KEY `uk_saas_tenant_tenant_id` (`tenant_id`), UNIQUE KEY `uk_saas_tenant_slug` (`slug`),
    KEY `idx_saas_tenant_state_update` (`lifecycle_state`, `update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_domain` (
    `domain_id` BIGINT NOT NULL, `tenant_id` VARCHAR(20) NOT NULL, `host` VARCHAR(253) NOT NULL,
    `verification_state` VARCHAR(32) NOT NULL, `verification_method` VARCHAR(32) NOT NULL,
    `verified_at` DATETIME(3) DEFAULT NULL, `revoked_at` DATETIME(3) DEFAULT NULL,
    `owned_host` VARCHAR(253) AS (CASE WHEN `verification_state` <> 'REVOKED' THEN `host` ELSE NULL END) STORED,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`domain_id`),
    UNIQUE KEY `uk_saas_domain_owned_host` (`owned_host`), KEY `idx_saas_domain_host` (`host`),
    KEY `idx_saas_domain_tenant_state` (`tenant_id`, `verification_state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_plan` (
    `plan_id` BIGINT NOT NULL, `plan_code` VARCHAR(64) NOT NULL, `plan_version` INT NOT NULL,
    `plan_name` VARCHAR(128) NOT NULL, `status` VARCHAR(32) NOT NULL,
    `trial_days` INT NOT NULL DEFAULT 14, `grace_days` INT NOT NULL DEFAULT 7,
    `description` VARCHAR(512) DEFAULT NULL,
    `active_slot` VARCHAR(64) AS (CASE WHEN `status` = 'ACTIVE' THEN `plan_code` ELSE NULL END) STORED,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`plan_id`),
    UNIQUE KEY `uk_saas_plan_code_version` (`plan_code`, `plan_version`),
    UNIQUE KEY `uk_saas_plan_active_slot` (`active_slot`),
    CONSTRAINT `ck_saas_plan_version` CHECK (`plan_version` BETWEEN 1 AND 2147483647),
    CONSTRAINT `ck_saas_plan_trial_days` CHECK (`trial_days` BETWEEN 0 AND 3650),
    CONSTRAINT `ck_saas_plan_grace_days` CHECK (`grace_days` BETWEEN 0 AND 3650)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_feature` (
    `feature_id` BIGINT NOT NULL, `feature_key` VARCHAR(128) NOT NULL, `feature_name` VARCHAR(128) NOT NULL,
    `status` VARCHAR(32) NOT NULL, `description` VARCHAR(512) DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`feature_id`),
    UNIQUE KEY `uk_saas_feature_key` (`feature_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_plan_feature` (
    `plan_feature_id` BIGINT NOT NULL, `plan_id` BIGINT NOT NULL, `feature_id` BIGINT NOT NULL,
    `granted` TINYINT(1) NOT NULL DEFAULT 1, `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME(3) NOT NULL, `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    PRIMARY KEY (`plan_feature_id`), UNIQUE KEY `uk_saas_plan_feature` (`plan_id`, `feature_id`),
    KEY `idx_saas_plan_feature_feature` (`feature_id`),
    CONSTRAINT `ck_saas_plan_feature_granted` CHECK (`granted` IN (0, 1))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_plan_quota` (
    `plan_quota_id` BIGINT NOT NULL, `plan_id` BIGINT NOT NULL, `quota_key` VARCHAR(64) NOT NULL,
    `limit_value` BIGINT DEFAULT NULL, `period_type` VARCHAR(32) NOT NULL,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    PRIMARY KEY (`plan_quota_id`), UNIQUE KEY `uk_saas_plan_quota` (`plan_id`, `quota_key`),
    CONSTRAINT `ck_saas_plan_quota_limit` CHECK (`limit_value` IS NULL OR `limit_value` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_subscription` (
    `subscription_id` BIGINT NOT NULL, `tenant_id` VARCHAR(20) NOT NULL, `plan_id` BIGINT NOT NULL,
    `state` VARCHAR(32) NOT NULL, `start_at` DATETIME(3) NOT NULL, `end_at` DATETIME(3) DEFAULT NULL,
    `grace_end_at` DATETIME(3) DEFAULT NULL, `non_expiring` TINYINT(1) NOT NULL DEFAULT 0,
    `current_slot` VARCHAR(20) AS (CASE WHEN `state` IN ('TRIAL','ACTIVE','GRACE') THEN `tenant_id` ELSE NULL END) STORED,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`subscription_id`),
    UNIQUE KEY `uk_saas_subscription_current_slot` (`current_slot`),
    KEY `idx_saas_subscription_tenant_state` (`tenant_id`, `state`),
    KEY `idx_saas_subscription_lifecycle_time` (`state`, `end_at`, `grace_end_at`),
    CONSTRAINT `ck_saas_subscription_non_expiring` CHECK (`non_expiring` IN (0, 1)),
    CONSTRAINT `ck_saas_subscription_dates` CHECK ((`non_expiring` = 1 AND `end_at` IS NULL AND `grace_end_at` IS NULL) OR
        (`non_expiring` = 0 AND `end_at` IS NOT NULL AND `grace_end_at` IS NOT NULL AND `grace_end_at` >= `end_at`))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_tenant_feature_override` (
    `override_id` BIGINT NOT NULL, `tenant_id` VARCHAR(20) NOT NULL, `feature_id` BIGINT NOT NULL,
    `override_state` VARCHAR(32) NOT NULL, `effective_from` DATETIME(3) NOT NULL,
    `effective_until` DATETIME(3) DEFAULT NULL, `reason` VARCHAR(512) DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`override_id`),
    UNIQUE KEY `uk_saas_tenant_feature_window` (`tenant_id`, `feature_id`, `effective_from`),
    KEY `idx_saas_tenant_feature_effective` (`tenant_id`, `effective_from`, `effective_until`),
    CONSTRAINT `ck_saas_tenant_feature_window` CHECK (`effective_until` IS NULL OR `effective_until` > `effective_from`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_tenant_quota_override` (
    `override_id` BIGINT NOT NULL, `tenant_id` VARCHAR(20) NOT NULL, `quota_key` VARCHAR(64) NOT NULL,
    `limit_value` BIGINT DEFAULT NULL, `effective_from` DATETIME(3) NOT NULL,
    `effective_until` DATETIME(3) DEFAULT NULL, `reason` VARCHAR(512) DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`override_id`),
    UNIQUE KEY `uk_saas_tenant_quota_window` (`tenant_id`, `quota_key`, `effective_from`),
    KEY `idx_saas_tenant_quota_effective` (`tenant_id`, `effective_from`, `effective_until`),
    CONSTRAINT `ck_saas_tenant_quota_window` CHECK (`effective_until` IS NULL OR `effective_until` > `effective_from`),
    CONSTRAINT `ck_saas_tenant_quota_limit` CHECK (`limit_value` IS NULL OR `limit_value` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_deployment` (
    `deployment_id` BIGINT NOT NULL, `tenant_id` VARCHAR(20) NOT NULL, `mode` VARCHAR(32) NOT NULL,
    `status` VARCHAR(32) NOT NULL, `deployment_ref` VARCHAR(255) NOT NULL, `secret_ref` VARCHAR(255) DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL, `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL, `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0, PRIMARY KEY (`deployment_id`),
    UNIQUE KEY `uk_saas_deployment_tenant` (`tenant_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_provisioning_task` (
    `task_id` BIGINT NOT NULL,
    `request_id` VARCHAR(128) NOT NULL,
    `request_hash` CHAR(64) NOT NULL,
    `tenant_id` VARCHAR(20) NOT NULL,
    `plan_id` BIGINT NOT NULL,
    `status` VARCHAR(16) NOT NULL,
    `attempt_count` INT NOT NULL DEFAULT 0,
    `lease_until` DATETIME(3) DEFAULT NULL,
    `tenant_record_id` BIGINT DEFAULT NULL,
    `company_id` BIGINT DEFAULT NULL,
    `dept_id` BIGINT DEFAULT NULL,
    `role_id` BIGINT DEFAULT NULL,
    `user_id` BIGINT DEFAULT NULL,
    `activation_expires_at` DATETIME(3) DEFAULT NULL,
    `last_error_type` VARCHAR(128) DEFAULT NULL,
    `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL,
    `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`task_id`),
    UNIQUE KEY `uk_saas_provisioning_request` (`request_id`),
    UNIQUE KEY `uk_saas_provisioning_tenant` (`tenant_id`),
    KEY `idx_saas_provisioning_status_lease_update` (`status`, `lease_until`, `update_time`),
    CONSTRAINT `ck_saas_provisioning_status` CHECK (`status` IN ('PENDING', 'PROVISIONING', 'INITIALIZED', 'SUCCEEDED', 'FAILED')),
    CONSTRAINT `ck_saas_provisioning_attempt_count` CHECK (`attempt_count` >= 0),
    CONSTRAINT `ck_saas_provisioning_lease` CHECK (
        (`status` = 'PROVISIONING' AND `lease_until` IS NOT NULL) OR
        (`status` <> 'PROVISIONING' AND `lease_until` IS NULL)
    ),
    CONSTRAINT `ck_saas_provisioning_result` CHECK (
        (`status` IN ('PENDING', 'PROVISIONING') AND
            `tenant_record_id` IS NULL AND `company_id` IS NULL AND `dept_id` IS NULL AND
            `role_id` IS NULL AND `user_id` IS NULL AND `activation_expires_at` IS NULL) OR
        (`status` IN ('INITIALIZED', 'SUCCEEDED') AND
            `tenant_record_id` IS NOT NULL AND `company_id` IS NOT NULL AND `dept_id` IS NOT NULL AND
            `role_id` IS NOT NULL AND `user_id` IS NOT NULL AND `activation_expires_at` IS NOT NULL) OR
        (`status` = 'FAILED' AND (
            (`tenant_record_id` IS NULL AND `company_id` IS NULL AND `dept_id` IS NULL AND
                `role_id` IS NULL AND `user_id` IS NULL AND `activation_expires_at` IS NULL) OR
            (`tenant_record_id` IS NOT NULL AND `company_id` IS NOT NULL AND `dept_id` IS NOT NULL AND
                `role_id` IS NOT NULL AND `user_id` IS NOT NULL AND `activation_expires_at` IS NOT NULL)
        ))
    )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `saas_entitlement_snapshot` (
    `tenant_id` VARCHAR(20) NOT NULL,
    `snapshot_version` BIGINT NOT NULL,
    `payload_hash` CHAR(64) NOT NULL,
    `snapshot_json` LONGTEXT NOT NULL,
    `issued_at` DATETIME(3) NOT NULL,
    `expires_at` DATETIME(3) NOT NULL,
    `signature_key_id` VARCHAR(64) NOT NULL,
    `signature` VARCHAR(128) NOT NULL,
    `create_by` VARCHAR(64) NOT NULL,
    `create_time` DATETIME(3) NOT NULL,
    `update_by` VARCHAR(64) NOT NULL,
    `update_time` DATETIME(3) NOT NULL,
    `version_no` BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (`tenant_id`),
    KEY `idx_saas_entitlement_snapshot_expiry` (`expires_at`),
    CONSTRAINT `ck_saas_entitlement_snapshot_version` CHECK (`snapshot_version` >= 1),
    CONSTRAINT `ck_saas_entitlement_snapshot_lease` CHECK (`expires_at` > `issued_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

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

INSERT INTO `saas_plan` (
    `plan_id`, `plan_code`, `plan_version`, `plan_name`, `status`, `trial_days`, `grace_days`,
    `description`, `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000001, 'legacy-full-access', 1, 'Legacy Full Access', 'ACTIVE', 14, 7,
    'Unlimited non-expiring access for tenants migrated from the existing ERP installation',
    'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_plan` WHERE `plan_code` = 'legacy-full-access'
);

INSERT INTO `saas_feature` (
    `feature_id`, `feature_key`, `feature_name`, `status`, `description`,
    `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000101, 'ai.assistant', 'AI Assistant', 'ACTIVE',
    'AI chat, assisted analysis, and controlled AI actions',
    'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_feature` WHERE `feature_key` = 'ai.assistant'
);
