-- 20260801_01: AI 面板一期 —— 审计表补齐模型遥测字段
-- 背景：sys_ai_audit 此前只记录耗时，无法核算 token 成本，也看不出一次对话调用了哪些只读工具。
-- 说明：
--   1. 全部通过 information_schema 判定后再 ALTER，可重复执行；
--   2. 新列全部允许为空，存量行保持 NULL，不做回填（历史数据本就没有 token 信息）；
--   3. session_id 关联 sys_ai_session，便于从审计反查完整对话。

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `model` varchar(128) DEFAULT NULL COMMENT ''本次交互使用的模型编号'' AFTER `duration_ms`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'model'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `prompt_tokens` int(11) DEFAULT NULL COMMENT ''输入token数'' AFTER `model`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'prompt_tokens'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `completion_tokens` int(11) DEFAULT NULL COMMENT ''输出token数'' AFTER `prompt_tokens`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'completion_tokens'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `total_tokens` int(11) DEFAULT NULL COMMENT ''总token数'' AFTER `completion_tokens`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'total_tokens'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `tool_rounds` int(11) DEFAULT NULL COMMENT ''工具调用轮次'' AFTER `total_tokens`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'tool_rounds'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `tool_keys` varchar(500) DEFAULT NULL COMMENT ''本次调用的只读工具，逗号分隔'' AFTER `tool_rounds`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'tool_keys'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = (
    SELECT IF(COUNT(*) = 0,
              'ALTER TABLE `sys_ai_audit` ADD COLUMN `session_id` bigint(20) DEFAULT NULL COMMENT ''关联AI会话ID'' AFTER `tool_keys`',
              'SELECT 1')
    FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'sys_ai_audit'
      AND COLUMN_NAME = 'session_id'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
