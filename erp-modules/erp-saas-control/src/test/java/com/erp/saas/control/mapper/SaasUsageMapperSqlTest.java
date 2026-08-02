package com.erp.saas.control.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SaasUsageMapperSqlTest {
    @Test
    void shouldBindIdempotencyLookupAndKeepNewestAbsoluteSnapshot() {
        String findSql = String.join(" ", method(SaasUsageEventMapper.class, "findByIdempotencyKey")
                .getAnnotation(Select.class).value());
        assertThat(findSql).containsIgnoringCase("idempotency_key = #{idempotencyKey}")
                .doesNotContain("${");

        String upsertSql = String.join(" ", method(SaasUsageSummaryMapper.class, "upsertLatest")
                .getAnnotation(Insert.class).value());
        assertThat(upsertSql).containsIgnoringCase("ON DUPLICATE KEY UPDATE")
                .containsIgnoringCase("last_occurred_at")
                .containsIgnoringCase("VALUES(used_amount)")
                .containsIgnoringCase("VALUES(last_occurred_at) >= last_occurred_at")
                .doesNotContain("${");
    }

    private static java.lang.reflect.Method method(Class<?> mapper, String name) {
        return Arrays.stream(mapper.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
    }
}
