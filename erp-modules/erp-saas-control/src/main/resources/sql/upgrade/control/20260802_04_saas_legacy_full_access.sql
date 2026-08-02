INSERT INTO `saas_plan` (
    `plan_id`, `plan_code`, `plan_version`, `plan_name`, `status`, `trial_days`, `grace_days`,
    `description`, `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000001, 'legacy-full-access', 1, 'Legacy Full Access', 'ACTIVE', 14, 7,
    'Unlimited non-expiring access for tenants migrated from the existing ERP installation',
    'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_plan` WHERE `plan_code` = 'legacy-full-access'
);
