CREATE TABLE IF NOT EXISTS `sys_saas_quota_counter` (
  `tenant_id` varchar(20) NOT NULL COMMENT 'Tenant identifier',
  `metric_key` varchar(64) NOT NULL COMMENT 'Stable quota metric key',
  `period_start` datetime(3) NOT NULL COMMENT 'UTC period start; 1970-01-01 for non-periodic metrics',
  `used_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Settled usage',
  `reserved_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT 'In-flight reserved usage',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  `version_no` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  PRIMARY KEY (`tenant_id`, `metric_key`, `period_start`),
  CONSTRAINT `ck_sys_saas_quota_counter_used` CHECK (`used_amount` >= 0),
  CONSTRAINT `ck_sys_saas_quota_counter_reserved` CHECK (`reserved_amount` >= 0)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant-local real-time SaaS quota counter';

CREATE TABLE IF NOT EXISTS `sys_saas_quota_reservation` (
  `reservation_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Reservation primary key',
  `tenant_id` varchar(20) NOT NULL COMMENT 'Tenant identifier',
  `metric_key` varchar(64) NOT NULL COMMENT 'Stable quota metric key',
  `reservation_key` varchar(128) NOT NULL COMMENT 'Idempotent business reservation key',
  `period_start` datetime(3) NOT NULL COMMENT 'UTC period start; 1970-01-01 for non-periodic metrics',
  `reserved_amount` bigint(20) NOT NULL COMMENT 'Maximum reserved amount',
  `settled_amount` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Actual settled amount',
  `status` varchar(16) NOT NULL COMMENT 'RESERVED, SETTLED, or RELEASED',
  `reserve_event_key` varchar(128) NOT NULL COMMENT 'Reserve event idempotency key',
  `settle_event_key` varchar(128) DEFAULT NULL COMMENT 'Settle event idempotency key',
  `release_event_key` varchar(128) DEFAULT NULL COMMENT 'Release event idempotency key',
  `create_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) NOT NULL DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  `version_no` bigint(20) NOT NULL DEFAULT 0 COMMENT 'Optimistic lock version',
  PRIMARY KEY (`reservation_id`),
  UNIQUE KEY `uk_sys_saas_quota_reservation` (`tenant_id`, `metric_key`, `reservation_key`),
  KEY `idx_sys_saas_quota_reservation_period` (`tenant_id`, `metric_key`, `period_start`, `status`),
  CONSTRAINT `ck_sys_saas_quota_reservation_amount` CHECK (`reserved_amount` > 0 AND `settled_amount` >= 0 AND `settled_amount` <= `reserved_amount`),
  CONSTRAINT `ck_sys_saas_quota_reservation_status` CHECK (`status` IN ('RESERVED', 'SETTLED', 'RELEASED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant-local SaaS quota reservation ledger';

INSERT IGNORE INTO `sys_saas_quota_counter`
  (`tenant_id`, `metric_key`, `period_start`, `used_amount`, `reserved_amount`,
   `create_by`, `create_time`, `update_by`, `update_time`, `version_no`)
SELECT `tenant_id`, 'user_count', '1970-01-01 00:00:00.000', COUNT(*), 0,
       'saas-migration', NOW(3), 'saas-migration', NOW(3), 0
FROM `sys_user`
WHERE `status` = '0' AND `del_flag` = '0'
GROUP BY `tenant_id`;
