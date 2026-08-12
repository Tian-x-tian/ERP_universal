package com.erp.system.mapper;

import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class SysSaasEntitlementSnapshotMapperSqlTest {
    @Test
    void shouldLockTenantSnapshotAndUseVersionCas() {
        var lock = method("findForUpdate");
        assertThat(String.join(" ", lock.getAnnotation(Select.class).value()))
                .containsIgnoringCase("tenant_id = #{tenantId}")
                .containsIgnoringCase("FOR UPDATE").doesNotContain("${");
        var update = method("updateVersioned");
        assertThat(String.join(" ", update.getAnnotation(Update.class).value()))
                .containsIgnoringCase("snapshot_version = #{row.snapshotVersion}")
                .containsIgnoringCase("version_no = #{expectedVersion}")
                .containsIgnoringCase("version_no = version_no + 1").doesNotContain("${");
    }

    private static java.lang.reflect.Method method(String name) {
        return Arrays.stream(SysSaasEntitlementSnapshotMapper.class.getDeclaredMethods())
                .filter(candidate -> candidate.getName().equals(name)).findFirst().orElseThrow();
    }
}
