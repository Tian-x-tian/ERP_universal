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
