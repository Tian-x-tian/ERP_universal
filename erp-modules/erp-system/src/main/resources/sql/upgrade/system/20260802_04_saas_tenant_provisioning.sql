CREATE TABLE IF NOT EXISTS `sys_saas_provisioning_task` (
  `task_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Provisioning task primary key',
  `tenant_id` varchar(20) NOT NULL COMMENT 'Target tenant identifier',
  `request_id` varchar(128) NOT NULL COMMENT 'Idempotent provisioning request identifier',
  `request_hash` char(64) NOT NULL COMMENT 'SHA-256 hash of normalized request payload',
  `status` varchar(16) NOT NULL COMMENT 'PROCESSING or SUCCEEDED',
  `tenant_record_id` bigint(20) DEFAULT NULL COMMENT 'Created tenant record identifier',
  `company_id` bigint(20) DEFAULT NULL COMMENT 'Created root company identifier',
  `dept_id` bigint(20) DEFAULT NULL COMMENT 'Created root department identifier',
  `role_id` bigint(20) DEFAULT NULL COMMENT 'Created tenant administrator role identifier',
  `user_id` bigint(20) DEFAULT NULL COMMENT 'Created tenant administrator user identifier',
  `activation_expires_at` datetime(3) DEFAULT NULL COMMENT 'One-time activation expiry in UTC',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  PRIMARY KEY (`task_id`),
  UNIQUE KEY `uk_sys_saas_provisioning_request` (`tenant_id`, `request_id`),
  KEY `idx_sys_saas_provisioning_status` (`tenant_id`, `status`, `update_time`, `task_id`),
  CONSTRAINT `ck_sys_saas_provisioning_status` CHECK (`status` IN ('PROCESSING', 'SUCCEEDED')),
  CONSTRAINT `ck_sys_saas_provisioning_result` CHECK (
    (`status` = 'PROCESSING' AND `tenant_record_id` IS NULL AND `company_id` IS NULL
      AND `dept_id` IS NULL AND `role_id` IS NULL AND `user_id` IS NULL
      AND `activation_expires_at` IS NULL)
    OR
    (`status` = 'SUCCEEDED' AND `tenant_record_id` IS NOT NULL AND `company_id` IS NOT NULL
      AND `dept_id` IS NOT NULL AND `role_id` IS NOT NULL AND `user_id` IS NOT NULL
      AND `activation_expires_at` IS NOT NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Idempotent tenant provisioning task';

CREATE TABLE IF NOT EXISTS `sys_user_activation` (
  `activation_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Activation primary key',
  `tenant_id` varchar(20) NOT NULL COMMENT 'Tenant identifier',
  `user_id` bigint(20) NOT NULL COMMENT 'Disabled user awaiting activation',
  `token_hash` char(64) NOT NULL COMMENT 'SHA-256 hash of one-time activation token',
  `expires_at` datetime(3) NOT NULL COMMENT 'Activation expiry in UTC',
  `activated_at` datetime(3) DEFAULT NULL COMMENT 'Successful activation time in UTC',
  `status` varchar(16) NOT NULL COMMENT 'PENDING, USED, or REVOKED',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  `version_no` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  PRIMARY KEY (`activation_id`),
  UNIQUE KEY `uk_sys_user_activation_user` (`tenant_id`, `user_id`),
  UNIQUE KEY `uk_sys_user_activation_token` (`tenant_id`, `token_hash`),
  KEY `idx_sys_user_activation_pending` (`tenant_id`, `status`, `expires_at`),
  CONSTRAINT `ck_sys_user_activation_status` CHECK (`status` IN ('PENDING', 'USED', 'REVOKED')),
  CONSTRAINT `ck_sys_user_activation_used` CHECK (
    (`status` = 'USED' AND `activated_at` IS NOT NULL)
    OR (`status` IN ('PENDING', 'REVOKED') AND `activated_at` IS NULL)
  )
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='One-time tenant administrator activation';
