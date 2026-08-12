package com.erp.saas.control.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SaasPlanCatalogMapperSqlTest {

    @Test
    void shouldLockTenantPlanFeatureAndOverrideAggregatesWithBoundParameters() throws Exception {
        assertSelect(SaasTenantMapper.class, "lockByTenantId", "tenant_id = #{tenantId}", "FOR UPDATE");
        assertSelect(SaasPlanMapper.class, "findByIdForUpdate", "plan_id = #{planId}", "FOR UPDATE");
        assertSelect(SaasPlanMapper.class, "findFamilyForUpdate",
                "plan_code = #{planCode}", "ORDER BY plan_id", "FOR UPDATE");
        assertSelect(SaasFeatureMapper.class, "findByKeyForUpdate", "feature_key = #{featureKey}", "FOR UPDATE");
        assertSelect(SaasTenantFeatureOverrideMapper.class, "findWindowsForUpdate",
                "tenant_id = #{tenantId}", "feature_id = #{featureId}", "ORDER BY effective_from", "FOR UPDATE");
        assertSelect(SaasTenantQuotaOverrideMapper.class, "findWindowsForUpdate",
                "tenant_id = #{tenantId}", "quota_key = #{quotaKey}", "ORDER BY effective_from", "FOR UPDATE");
    }

    @Test
    void shouldUseVersionCasAndAuditForPlanAndFeatureUpdates() throws Exception {
        assertUpdate(SaasPlanMapper.class, "updateDraft",
                "version_no = version_no + 1", "update_by = #{operator}", "update_time = #{now}",
                "version_no = #{expectedVersion}", "status = 'DRAFT'");
        assertUpdate(SaasPlanMapper.class, "bumpDraft",
                "version_no = version_no + 1", "version_no = #{expectedVersion}", "status = 'DRAFT'");
        assertUpdate(SaasPlanMapper.class, "retire",
                "status = 'RETIRED'", "version_no = #{expectedVersion}", "status = 'ACTIVE'");
        assertUpdate(SaasPlanMapper.class, "activate",
                "status = 'ACTIVE'", "version_no = #{expectedVersion}", "status = 'DRAFT'");
        assertUpdate(SaasFeatureMapper.class, "updateVersioned",
                "version_no = version_no + 1", "update_by = #{operator}", "update_time = #{now}",
                "version_no = #{expectedVersion}");
    }

    @Test
    void shouldKeepEntitlementReadsNonLockingAndFutureDeletesVersioned() throws Exception {
        assertSelect(SaasSubscriptionMapper.class, "findCurrentByTenantId",
                "tenant_id = #{tenantId}", "state IN ('TRIAL','ACTIVE','GRACE')");
        assertThat(sql(selectMethod(SaasSubscriptionMapper.class, "findCurrentByTenantId")))
                .doesNotContainIgnoringCase("FOR UPDATE");
        assertDelete(SaasTenantFeatureOverrideMapper.class, "deleteFutureVersioned",
                "override_id = #{overrideId}", "version_no = #{expectedVersion}", "effective_from > #{now}");
        assertDelete(SaasTenantQuotaOverrideMapper.class, "deleteFutureVersioned",
                "override_id = #{overrideId}", "version_no = #{expectedVersion}", "effective_from > #{now}");
    }

    @Test
    void shouldNeverUseStringSubstitutionOrGeneratedColumns() {
        for (Class<?> mapper : new Class<?>[]{SaasTenantMapper.class, SaasPlanMapper.class,
                SaasFeatureMapper.class, SaasPlanFeatureMapper.class, SaasPlanQuotaMapper.class,
                SaasSubscriptionMapper.class, SaasTenantFeatureOverrideMapper.class,
                SaasTenantQuotaOverrideMapper.class}) {
            for (Method method : mapper.getDeclaredMethods()) {
                String statement = sql(method);
                assertThat(statement).doesNotContain("${");
                if (method.getAnnotation(Update.class) != null || method.getAnnotation(Delete.class) != null) {
                    assertThat(statement).doesNotContainIgnoringCase("active_slot", "current_slot", "owned_host");
                }
            }
        }
    }

    private static void assertSelect(Class<?> mapper, String methodName, String... fragments) throws Exception {
        String statement = sql(selectMethod(mapper, methodName));
        assertFragments(statement, fragments);
    }

    private static Method selectMethod(Class<?> mapper, String methodName) throws Exception {
        Method method = Arrays.stream(mapper.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow(NoSuchMethodException::new);
        assertThat(method.getAnnotation(Select.class)).isNotNull();
        return method;
    }

    private static void assertUpdate(Class<?> mapper, String methodName, String... fragments) throws Exception {
        Method method = Arrays.stream(mapper.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow(NoSuchMethodException::new);
        assertThat(method.getAnnotation(Update.class)).isNotNull();
        assertFragments(sql(method), fragments);
    }

    private static void assertDelete(Class<?> mapper, String methodName, String... fragments) throws Exception {
        Method method = Arrays.stream(mapper.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName))
                .findFirst().orElseThrow(NoSuchMethodException::new);
        assertThat(method.getAnnotation(Delete.class)).isNotNull();
        assertFragments(sql(method), fragments);
    }

    private static void assertFragments(String statement, String... fragments) {
        for (String fragment : fragments) {
            assertThat(statement).containsIgnoringCase(fragment);
        }
    }

    private static String sql(Method method) {
        if (method.getAnnotation(Select.class) != null) {
            return String.join(" ", method.getAnnotation(Select.class).value());
        }
        if (method.getAnnotation(Update.class) != null) {
            return String.join(" ", method.getAnnotation(Update.class).value());
        }
        if (method.getAnnotation(Delete.class) != null) {
            return String.join(" ", method.getAnnotation(Delete.class).value());
        }
        return "";
    }
}
