CREATE TABLE IF NOT EXISTS `sys_saas_entitlement_snapshot` (
  `tenant_id` varchar(20) NOT NULL COMMENT 'Tenant identifier',
  `snapshot_version` bigint(20) NOT NULL COMMENT 'Control-plane snapshot version',
  `snapshot_json` longtext NOT NULL COMMENT 'Signed entitlement snapshot JSON',
  `issued_at` datetime(3) NOT NULL COMMENT 'Snapshot issue time in UTC',
  `expires_at` datetime(3) NOT NULL COMMENT 'Snapshot lease expiry time in UTC',
  `signature_key_id` varchar(64) NOT NULL COMMENT 'Verification key identifier',
  `signature` varchar(128) NOT NULL COMMENT 'Base64url HMAC signature',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  `version_no` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  PRIMARY KEY (`tenant_id`),
  KEY `idx_sys_saas_snapshot_expiry` (`tenant_id`, `expires_at`),
  CONSTRAINT `ck_sys_saas_snapshot_version` CHECK (`snapshot_version` >= 1),
  CONSTRAINT `ck_sys_saas_snapshot_lease` CHECK (`expires_at` > `issued_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant-local signed SaaS entitlement snapshot';
