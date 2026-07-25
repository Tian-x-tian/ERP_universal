-- 20260725_01: 单据明细行表补齐审计字段（create_by/create_time/update_by/update_time）
-- 背景：库存与采购的明细行表此前无任何留痕字段，行级修改无法追溯操作人与时间。
-- 说明：
--   1. 全部通过 information_schema 判定后再 ALTER，可重复执行；
--   2. 新列带 CURRENT_TIMESTAMP 默认值，作为应用层 MetaObjectHandler 之外的兜底；
--   3. 存量行按所属主单回填 create_by/create_time，仅在 create_by 为空时执行，重复执行安全。

-- ---------- inv_inbound_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_inbound_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_inbound_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_inbound_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_inbound_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_inbound_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_inbound_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_inbound_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_inbound_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_inbound_order_line` line
INNER JOIN `inv_inbound_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- inv_outbound_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_outbound_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_outbound_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_outbound_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_outbound_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_outbound_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_outbound_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_outbound_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_outbound_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_outbound_order_line` line
INNER JOIN `inv_outbound_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- inv_transfer_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_transfer_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_transfer_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_transfer_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_transfer_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_transfer_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_transfer_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_transfer_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_transfer_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_transfer_order_line` line
INNER JOIN `inv_transfer_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- inv_stock_move_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_move_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_move_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_move_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_move_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_move_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_move_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_move_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_move_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_stock_move_order_line` line
INNER JOIN `inv_stock_move_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- inv_stock_freeze_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_freeze_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_freeze_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_freeze_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_freeze_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_freeze_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_freeze_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_freeze_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_freeze_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_stock_freeze_order_line` line
INNER JOIN `inv_stock_freeze_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- inv_stock_adjust_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_adjust_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_adjust_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_adjust_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_adjust_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_adjust_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_adjust_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stock_adjust_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stock_adjust_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_stock_adjust_order_line` line
INNER JOIN `inv_stock_adjust_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- inv_stocktake_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stocktake_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stocktake_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stocktake_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stocktake_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stocktake_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stocktake_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `inv_stocktake_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'inv_stocktake_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `inv_stocktake_order_line` line
INNER JOIN `inv_stocktake_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- pur_requisition_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_requisition_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_requisition_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_requisition_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_requisition_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_requisition_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_requisition_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_requisition_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_requisition_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `pur_requisition_line` line
INNER JOIN `pur_requisition` head ON head.`requisition_id` = line.`requisition_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;


-- ---------- pur_order_line ----------
SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_order_line` ADD COLUMN `create_by` varchar(64) DEFAULT NULL COMMENT ''创建人'' AFTER `remark`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_order_line'
      AND COLUMN_NAME = 'create_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_order_line` ADD COLUMN `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT ''创建时间'' AFTER `create_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_order_line'
      AND COLUMN_NAME = 'create_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_order_line` ADD COLUMN `update_by` varchar(64) DEFAULT NULL COMMENT ''更新人'' AFTER `create_time`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_order_line'
      AND COLUMN_NAME = 'update_by'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `pur_order_line` ADD COLUMN `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT ''更新时间'' AFTER `update_by`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'pur_order_line'
      AND COLUMN_NAME = 'update_time'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 存量行从主表回填留痕，避免历史数据被记成迁移当天创建
UPDATE `pur_order_line` line
INNER JOIN `pur_order` head ON head.`order_id` = line.`order_id` AND head.`tenant_id` = line.`tenant_id`
SET line.`create_by` = head.`create_by`,
    line.`create_time` = IFNULL(head.`create_time`, line.`create_time`),
    line.`update_by` = head.`update_by`,
    line.`update_time` = IFNULL(head.`update_time`, line.`update_time`)
WHERE line.`create_by` IS NULL;

