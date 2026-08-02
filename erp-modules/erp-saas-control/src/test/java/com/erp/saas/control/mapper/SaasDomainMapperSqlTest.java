package com.erp.saas.control.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SaasDomainMapperSqlTest {
    @Test
    void shouldLockOwnershipAndDomainRowsWithBoundHosts() throws Exception {
        assertSql("findOwnedHostForUpdate", true,
                "owned_host = #{host}", "verification_state <> 'REVOKED'", "FOR UPDATE");
        assertSql("findByIdForUpdate", true, "domain_id = #{domainId}", "FOR UPDATE");
    }

    @Test
    void shouldUseVersionCasForVerificationAndRevocation() throws Exception {
        assertSql("markVerified", false, "verification_state = 'VERIFIED'",
                "verified_at = #{now}", "version_no = #{expectedVersion}", "version_no = version_no + 1");
        assertSql("markRevoked", false, "verification_state = 'REVOKED'",
                "revoked_at = #{now}", "version_no = #{expectedVersion}", "version_no = version_no + 1");
    }

    @Test
    void shouldResolveOnlyVerifiedDomainsWithEligibleOwnerStates() throws Exception {
        String sql = sql("resolveVerified");
        assertThat(sql).containsIgnoringCase("d.host = #{host}");
        assertThat(sql).containsIgnoringCase("d.verification_state = 'VERIFIED'");
        assertThat(sql).containsIgnoringCase("t.lifecycle_state NOT IN ('ARCHIVED','PURGE_PENDING','PURGED')");
        assertThat(sql).doesNotContain("${", "FOR UPDATE");
    }

    private static void assertSql(String methodName, boolean selectExpected, String... fragments) throws Exception {
        String sql = sql(methodName);
        for (String fragment : fragments) {
            assertThat(sql).containsIgnoringCase(fragment);
        }
        var method = Arrays.stream(SaasDomainMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        assertThat(selectExpected ? method.getAnnotation(Select.class) : method.getAnnotation(Update.class))
                .isNotNull();
    }

    private static String sql(String methodName) throws Exception {
        var method = Arrays.stream(SaasDomainMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        Select select = method.getAnnotation(Select.class);
        if (select != null) return String.join(" ", select.value());
        Update update = method.getAnnotation(Update.class);
        return update == null ? "" : String.join(" ", update.value());
    }
}
