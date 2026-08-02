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
