package com.erp.business.saas.mapper;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class SaasStorageObjectMapperSqlTest {
    @Test
    void shouldBindTenantAndObjectKeyInEveryLedgerMutation() {
        assertSqlContains("markActive", "tenant_id", "object_key", "uploading");
        assertSqlContains("markTerminal", "tenant_id", "object_key", "deleted");
    }

    private void assertSqlContains(String methodName, String... fragments) {
        Method method = java.util.Arrays.stream(SaasStorageObjectMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(methodName)).findFirst().orElseThrow();
        String sql = java.util.Arrays.stream(method.getDeclaredAnnotations())
                .map(Object::toString).reduce("", (left, right) -> left + right).toLowerCase();
        for (String fragment : fragments) {
            assertThat(sql).contains(fragment.toLowerCase());
        }
    }
}
