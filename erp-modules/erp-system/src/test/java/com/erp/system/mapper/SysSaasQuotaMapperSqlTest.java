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
    }

    private void assertSqlContains(Class<?> mapperType, String methodName, String... fragments) {
        Method method = java.util.Arrays.stream(mapperType.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        String sql = java.util.Arrays.stream(method.getDeclaredAnnotations())
                .map(Object::toString).reduce("", (left, right) -> left + right).toLowerCase();
        for (String fragment : fragments) assertThat(sql).contains(fragment.toLowerCase());
    }
}
