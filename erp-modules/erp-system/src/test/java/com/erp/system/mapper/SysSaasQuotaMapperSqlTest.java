package com.erp.system.mapper;

import org.junit.jupiter.api.Test;
import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SysSaasQuotaMapperSqlTest {
    @Test
    void shouldBindTenantMetricPeriodAndReferenceInEveryMutation() {
        assertSqlContains(SysSaasQuotaCounterMapper.class, "addReservation",
                "tenant_id", "metric_key", "period_start", "used_amount", "reserved_amount");
        assertSqlContains(SysSaasQuotaCounterMapper.class, "settleReservation",
                "tenant_id", "metric_key", "period_start", "reserved_amount", "used_amount");
        assertSqlContains(SysSaasQuotaReservationMapper.class, "markSettled",
                "tenant_id", "metric_key", "reservation_key", "status", "reserved");
        assertSqlContains(SysSaasUsageOutboxMapper.class, "findPending",
                "tenant_id", "status", "next_attempt_at", "limit #{limit}");
        assertSqlContains(SysSaasUsageOutboxMapper.class, "markSent",
                "tenant_id", "outbox_id", "status", "pending");
        assertSqlContains(SysSaasUsageOutboxMapper.class, "markRetry",
                "tenant_id", "outbox_id", "status", "pending");
        assertSqlContains(SysSaasProvisioningTaskMapper.class, "insertProcessing",
                "tenant_id", "request_id", "request_hash", "#{status}");
        assertSqlContains(SysSaasProvisioningTaskMapper.class, "lock",
                "tenant_id", "request_id", "for update");
        assertSqlContains(SysSaasProvisioningTaskMapper.class, "markSucceeded",
                "tenant_id", "request_id", "status", "processing");
        assertSqlContains(SysSaasProvisioningTaskMapper.class, "updateActivationExpiry",
                "tenant_id", "request_id", "user_id", "activation_expires_at", "succeeded");
        assertSqlContains(SysUserActivationMapper.class, "insert",
                "tenant_id", "user_id", "token_hash", "expires_at");
        assertSqlContains(SysUserActivationMapper.class, "lockByTokenHash",
                "tenant_id", "token_hash", "for update");
        assertSqlContains(SysUserActivationMapper.class, "lockByUser",
                "tenant_id", "user_id", "for update");
        assertSqlContains(SysUserActivationMapper.class, "reissue",
                "tenant_id", "activation_id", "user_id", "token_hash", "expires_at",
                "status", "pending", "version_no");
        assertSqlContains(SysUserActivationMapper.class, "markUsed",
                "tenant_id", "activation_id", "token_hash", "status", "pending", "version_no", "expires_at");
        assertSqlContains(SysTenantMapper.class, "findByTenantIdForUpdate",
                "tenant_id", "for update");
    }

    private void assertSqlContains(Class<?> mapperType, String methodName, String... fragments) {
        Method method = java.util.Arrays.stream(mapperType.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        String sql = java.util.Arrays.stream(method.getDeclaredAnnotations())
                .map(Object::toString).reduce("", (left, right) -> left + right).toLowerCase();
        assertThat(sql).doesNotContain("${");
        for (String fragment : fragments) assertThat(sql).contains(fragment.toLowerCase());
    }
}
