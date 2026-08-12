package com.erp.saas.control.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SaasLifecycleMapperSqlTest {
    @Test
    void shouldLockOnlyCurrentSubscriptionForTenant() throws Exception {
        String sql = sql(SaasSubscriptionMapper.class, "findCurrentForUpdate");
        assertThat(sql).containsIgnoringCase("tenant_id = #{tenantId}");
        assertThat(sql).containsIgnoringCase("state IN ('TRIAL','ACTIVE','GRACE')");
        assertThat(sql).containsIgnoringCase("ORDER BY subscription_id DESC LIMIT 1 FOR UPDATE");
        assertThat(sql).doesNotContain("${");
        assertThat(method(SaasSubscriptionMapper.class, "findCurrentForUpdate").getAnnotation(Select.class))
                .isNotNull();
    }

    @Test
    void shouldUseStateAndVersionCasForSubscriptionTransitions() throws Exception {
        assertUpdateSql(SaasSubscriptionMapper.class, "transitionState",
                "state = #{nextState}", "state = #{expectedState}",
                "version_no = #{expectedVersion}", "version_no = version_no + 1");
        assertUpdateSql(SaasSubscriptionMapper.class, "renewCurrent",
                "state = 'ACTIVE'", "state IN ('TRIAL','ACTIVE','GRACE')",
                "version_no = #{expectedVersion}", "version_no = version_no + 1");
    }

    @Test
    void shouldUseLifecycleVersionCasAndEnforcePurgeClock() throws Exception {
        assertUpdateSql(SaasTenantMapper.class, "transitionLifecycle",
                "lifecycle_state = #{nextState}", "lifecycle_state = #{expectedState}",
                "version_no = #{expectedVersion}", "version_no = version_no + 1");
        assertUpdateSql(SaasTenantMapper.class, "archive",
                "lifecycle_state = 'ARCHIVED'", "archived_at = #{now}",
                "purge_eligible_at = #{purgeEligibleAt}", "version_no = #{expectedVersion}");
        assertUpdateSql(SaasTenantMapper.class, "markPurgePending",
                "lifecycle_state = 'PURGE_PENDING'", "lifecycle_state = 'ARCHIVED'",
                "purge_eligible_at <= #{now}", "version_no = #{expectedVersion}");
    }

    @Test
    void shouldUseRequestVersionStatusCasAndClearStaleProvisioningResults() throws Exception {
        String lockSql = sql(SaasProvisioningTaskMapper.class, "lockByRequestId");
        assertThat(lockSql).containsIgnoringCase("request_id = #{requestId}")
                .containsIgnoringCase("FOR UPDATE")
                .doesNotContain("${");
        assertUpdateSql(SaasProvisioningTaskMapper.class, "markProcessing",
                "status = 'PROVISIONING'", "attempt_count = attempt_count + 1",
                "lease_until = #{leaseUntil}", "tenant_record_id = NULL", "activation_expires_at = NULL",
                "request_id = #{requestId}", "version_no = #{expectedVersion}",
                "status IN ('PENDING', 'FAILED')");
        assertUpdateSql(SaasProvisioningTaskMapper.class, "markInitialized",
                "status = 'INITIALIZED'", "lease_until = NULL", "tenant_record_id = #{tenantRecordId}",
                "activation_expires_at = #{activationExpiresAt}", "version_no = #{expectedVersion}",
                "status = 'PROVISIONING'");
        assertUpdateSql(SaasProvisioningTaskMapper.class, "markSucceeded",
                "status = 'SUCCEEDED'", "version_no = #{expectedVersion}", "status = 'INITIALIZED'");
        assertUpdateSql(SaasProvisioningTaskMapper.class, "markFailed",
                "status = 'FAILED'", "lease_until = NULL", "last_error_type = #{errorType}",
                "version_no = #{expectedVersion}", "status IN ('PENDING', 'PROVISIONING', 'INITIALIZED')");
        assertUpdateSql(SaasProvisioningTaskMapper.class, "reclaimExpired",
                "status = 'FAILED'", "lease_until = NULL", "last_error_type = 'ProvisioningLeaseExpired'",
                "request_id = #{requestId}", "version_no = #{expectedVersion}",
                "status = 'PROVISIONING'", "lease_until <= #{now}");
    }

    private static void assertUpdateSql(Class<?> mapper, String method, String... fragments) throws Exception {
        String sql = sql(mapper, method);
        for (String fragment : fragments) {
            assertThat(sql).containsIgnoringCase(fragment);
        }
        assertThat(method(mapper, method).getAnnotation(Update.class)).isNotNull();
        assertThat(sql).doesNotContain("${");
    }

    private static java.lang.reflect.Method method(Class<?> mapper, String methodName) {
        return Arrays.stream(mapper.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
    }

    private static String sql(Class<?> mapper, String methodName) {
        var method = method(mapper, methodName);
        Select select = method.getAnnotation(Select.class);
        if (select != null) return String.join(" ", select.value());
        Update update = method.getAnnotation(Update.class);
        return update == null ? "" : String.join(" ", update.value());
    }
}
