CREATE TABLE IF NOT EXISTS `sys_saas_usage_outbox` (
  `outbox_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Outbox primary key',
  `tenant_id` varchar(20) NOT NULL COMMENT 'Tenant identifier',
  `event_key` varchar(128) NOT NULL COMMENT 'Idempotent central usage event key',
  `metric_key` varchar(64) NOT NULL COMMENT 'Stable quota metric key',
  `amount` bigint(20) NOT NULL COMMENT 'Absolute settled usage snapshot',
  `period_start` datetime(3) DEFAULT NULL COMMENT 'UTC monthly period start; null for non-periodic metrics',
  `occurred_at` datetime(3) NOT NULL COMMENT 'Local snapshot time in UTC',
  `status` varchar(16) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING or SENT',
  `attempt_count` int(11) NOT NULL DEFAULT 0 COMMENT 'Delivery attempt count',
  `next_attempt_at` datetime(3) NOT NULL COMMENT 'Next eligible delivery time in UTC',
  `sent_at` datetime(3) DEFAULT NULL COMMENT 'Successful delivery time in UTC',
  `last_error_type` varchar(128) DEFAULT NULL COMMENT 'Last delivery exception type without message',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  PRIMARY KEY (`outbox_id`),
  UNIQUE KEY `uk_sys_saas_usage_outbox_event` (`tenant_id`, `event_key`),
  KEY `idx_sys_saas_usage_outbox_pending` (`tenant_id`, `status`, `next_attempt_at`, `outbox_id`),
  CONSTRAINT `ck_sys_saas_usage_outbox_amount` CHECK (`amount` >= 0),
  CONSTRAINT `ck_sys_saas_usage_outbox_attempt` CHECK (`attempt_count` >= 0),
  CONSTRAINT `ck_sys_saas_usage_outbox_status` CHECK (`status` IN ('PENDING', 'SENT')),
  CONSTRAINT `ck_sys_saas_usage_outbox_sent` CHECK ((`status` = 'PENDING' AND `sent_at` IS NULL) OR (`status` = 'SENT' AND `sent_at` IS NOT NULL))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant-local SaaS usage delivery outbox';

INSERT IGNORE INTO `sys_saas_usage_outbox`
  (`tenant_id`, `event_key`, `metric_key`, `amount`, `period_start`, `occurred_at`,
   `status`, `attempt_count`, `next_attempt_at`, `sent_at`, `last_error_type`,
   `create_by`, `create_time`, `update_by`, `update_time`)
SELECT `tenant_id`,
       CONCAT('bootstrap:', SHA2(CONCAT_WS('|', `tenant_id`, `metric_key`,
              DATE_FORMAT(`period_start`, '%Y%m%d%H%i%s%f')), 256)),
       `metric_key`, `used_amount`,
       IF(`period_start` = '1970-01-01 00:00:00.000', NULL, `period_start`),
       `update_time`, 'PENDING', 0, NOW(3), NULL, NULL,
       'saas-migration', NOW(3), 'saas-migration', NOW(3)
FROM `sys_saas_quota_counter`;
