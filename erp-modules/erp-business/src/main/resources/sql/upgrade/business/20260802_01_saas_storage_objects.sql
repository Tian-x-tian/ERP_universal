SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `hr_employee_document` ADD COLUMN `file_size` bigint(20) NOT NULL DEFAULT 0 COMMENT ''文件字节数'' AFTER `file_url`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'hr_employee_document'
      AND COLUMN_NAME = 'file_size'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `biz_saas_storage_object` (
  `storage_object_id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT 'Storage object ID',
  `tenant_id` varchar(20) NOT NULL COMMENT 'Tenant identifier',
  `object_key` varchar(512) NOT NULL COMMENT 'Object storage key',
  `byte_size` bigint(20) NOT NULL COMMENT 'Object size in bytes',
  `status` varchar(16) NOT NULL COMMENT 'UPLOADING, ACTIVE, ORPHANED, or DELETED',
  `quota_reference_key` varchar(128) DEFAULT NULL COMMENT 'Local quota reservation reference',
  `last_error` varchar(128) DEFAULT NULL COMMENT 'Last compensation error type',
  `create_by` varchar(64) DEFAULT '' COMMENT 'Created by',
  `create_time` datetime(3) NOT NULL COMMENT 'Created at',
  `update_by` varchar(64) DEFAULT '' COMMENT 'Updated by',
  `update_time` datetime(3) NOT NULL COMMENT 'Updated at',
  PRIMARY KEY (`storage_object_id`),
  UNIQUE KEY `uk_biz_saas_storage_object` (`tenant_id`, `object_key`),
  KEY `idx_biz_saas_storage_status` (`tenant_id`, `status`, `update_time`),
  CONSTRAINT `ck_biz_saas_storage_size` CHECK (`byte_size` >= 0),
  CONSTRAINT `ck_biz_saas_storage_status` CHECK (`status` IN ('UPLOADING', 'ACTIVE', 'ORPHANED', 'DELETED'))
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Tenant object storage usage ledger';

INSERT IGNORE INTO `biz_saas_storage_object`
  (`tenant_id`, `object_key`, `byte_size`, `status`, `quota_reference_key`, `last_error`,
   `create_by`, `create_time`, `update_by`, `update_time`)
SELECT `tenant_id`, `file_url`, COALESCE(`file_size`, 0), 'ACTIVE', NULL, NULL,
       COALESCE(`create_by`, 'saas-migration'), COALESCE(`create_time`, NOW(3)),
       COALESCE(`update_by`, 'saas-migration'), COALESCE(`update_time`, NOW(3))
FROM `hr_employee_document`
WHERE `file_url` IS NOT NULL AND TRIM(`file_url`) <> '';
