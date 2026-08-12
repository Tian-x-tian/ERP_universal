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
