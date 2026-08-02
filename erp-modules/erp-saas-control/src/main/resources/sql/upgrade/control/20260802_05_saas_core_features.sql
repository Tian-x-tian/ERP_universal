INSERT INTO `saas_feature` (
    `feature_id`, `feature_key`, `feature_name`, `status`, `description`,
    `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000101, 'ai.assistant', 'AI Assistant', 'ACTIVE',
    'AI chat, assisted analysis, and controlled AI actions',
    'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_feature` WHERE `feature_key` = 'ai.assistant'
);
