package com.erp.saas.control.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SaasEntitlementSnapshotMapperSqlTest {
    @Test
    void shouldLockTenantSnapshotAndUseVersionCasForReplacement() {
        var lock = method(SaasEntitlementSnapshotMapper.class, "findForUpdate");
        assertThat(String.join(" ", lock.getAnnotation(Select.class).value()))
                .containsIgnoringCase("tenant_id = #{tenantId}")
                .containsIgnoringCase("FOR UPDATE")
                .doesNotContain("${");

        var update = method(SaasEntitlementSnapshotMapper.class, "updateVersioned");
        String sql = String.join(" ", update.getAnnotation(Update.class).value());
        assertThat(sql).containsIgnoringCase("snapshot_version = #{row.snapshotVersion}")
                .containsIgnoringCase("payload_hash = #{row.payloadHash}")
                .containsIgnoringCase("version_no = #{expectedVersion}")
                .containsIgnoringCase("version_no = version_no + 1")
                .doesNotContain("${");
    }

    @Test
    void shouldResolveDeploymentAndLatestSubscriptionWithBoundTenant() {
        String deploymentSql = String.join(" ", method(SaasDeploymentMapper.class, "findByTenantId")
                .getAnnotation(Select.class).value());
        assertThat(deploymentSql).containsIgnoringCase("tenant_id = #{tenantId}").doesNotContain("${");
        String subscriptionSql = String.join(" ", method(SaasSubscriptionMapper.class, "findLatestByTenantId")
                .getAnnotation(Select.class).value());
        assertThat(subscriptionSql).containsIgnoringCase("tenant_id = #{tenantId}")
                .containsIgnoringCase("ORDER BY subscription_id DESC LIMIT 1").doesNotContain("${");
    }

    private static java.lang.reflect.Method method(Class<?> mapper, String name) {
        return Arrays.stream(mapper.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
    }
}
