INSERT INTO `saas_tenant` (
    `id`, `tenant_id`, `slug`, `tenant_name`, `lifecycle_state`,
    `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000000, '000000', 'platform', 'Platform Admin', 'ACTIVE',
    'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_tenant` WHERE `tenant_id` = '000000'
);

INSERT INTO `saas_subscription` (
    `subscription_id`, `tenant_id`, `plan_id`, `state`, `start_at`,
    `non_expiring`, `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000000, '000000', 1960000000000000001, 'ACTIVE', UTC_TIMESTAMP(3),
    1, 'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_subscription` WHERE `tenant_id` = '000000'
);

INSERT INTO `saas_deployment` (
    `deployment_id`, `tenant_id`, `mode`, `status`, `deployment_ref`,
    `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000000, '000000', 'SHARED', 'HEALTHY', 'http://erp-system',
    'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_deployment` WHERE `tenant_id` = '000000'
);

INSERT INTO `saas_domain` (
    `domain_id`, `tenant_id`, `host`, `verification_state`, `verification_method`,
    `verified_at`, `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000001, '000000', 'localhost', 'VERIFIED', 'PLATFORM_MANUAL',
    UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_domain` WHERE `host` = 'localhost' AND `verification_state` <> 'REVOKED'
);

INSERT INTO `saas_domain` (
    `domain_id`, `tenant_id`, `host`, `verification_state`, `verification_method`,
    `verified_at`, `create_by`, `create_time`, `update_by`, `update_time`, `version_no`
)
SELECT
    1960000000000000002, '000000', '127.0.0.1', 'VERIFIED', 'PLATFORM_MANUAL',
    UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 'saas-bootstrap', UTC_TIMESTAMP(3), 0
WHERE NOT EXISTS (
    SELECT 1 FROM `saas_domain` WHERE `host` = '127.0.0.1' AND `verification_state` <> 'REVOKED'
);
