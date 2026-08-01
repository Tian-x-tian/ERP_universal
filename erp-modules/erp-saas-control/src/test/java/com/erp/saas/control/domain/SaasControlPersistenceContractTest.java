package com.erp.saas.control.domain;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.erp.saas.control.domain.entity.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SaasControlPersistenceContractTest {
    private static final Map<Class<?>, String> TABLES = Map.ofEntries(
            Map.entry(SaasTenantEntity.class, "saas_tenant"), Map.entry(SaasDomainEntity.class, "saas_domain"),
            Map.entry(SaasPlanEntity.class, "saas_plan"), Map.entry(SaasFeatureEntity.class, "saas_feature"),
            Map.entry(SaasPlanFeatureEntity.class, "saas_plan_feature"), Map.entry(SaasPlanQuotaEntity.class, "saas_plan_quota"),
            Map.entry(SaasSubscriptionEntity.class, "saas_subscription"),
            Map.entry(SaasTenantFeatureOverrideEntity.class, "saas_tenant_feature_override"),
            Map.entry(SaasTenantQuotaOverrideEntity.class, "saas_tenant_quota_override"),
            Map.entry(SaasDeploymentEntity.class, "saas_deployment"));

    @Test void shouldFreezeTablesKeysAndFieldTypes() throws Exception {
        for (var entry : TABLES.entrySet()) {
            assertThat(entry.getKey().getAnnotation(TableName.class).value()).isEqualTo(entry.getValue());
            List<Field> ids = List.of(entry.getKey().getDeclaredFields()).stream()
                    .filter(field -> field.isAnnotationPresent(TableId.class)).toList();
            assertThat(ids).hasSize(1);
            assertThat(ids.get(0).getType()).isEqualTo(Long.class);
            assertThat(ids.get(0).getAnnotation(TableId.class).type()).isEqualTo(IdType.ASSIGN_ID);
        }
        assertType(SaasTenantEntity.class, "lifecycleState", com.erp.saas.contract.model.TenantLifecycleState.class);
        assertType(SaasDomainEntity.class, "verificationState", DomainVerificationState.class);
        assertType(SaasDomainEntity.class, "verificationMethod", DomainVerificationMethod.class);
        assertType(SaasPlanEntity.class, "planVersion", Integer.class);
        assertType(SaasPlanFeatureEntity.class, "granted", Boolean.class);
        assertType(SaasPlanQuotaEntity.class, "limitValue", Long.class);
        assertType(SaasSubscriptionEntity.class, "state", com.erp.saas.contract.model.SubscriptionState.class);
        assertType(SaasSubscriptionEntity.class, "startAt", LocalDateTime.class);
        assertType(SaasTenantFeatureOverrideEntity.class, "overrideState", FeatureOverrideState.class);
        assertType(SaasDeploymentEntity.class, "mode", com.erp.saas.contract.model.DeploymentMode.class);
        assertGenerated(SaasDomainEntity.class, "ownedHost");
        assertGenerated(SaasPlanEntity.class, "activeSlot");
        assertGenerated(SaasSubscriptionEntity.class, "currentSlot");
    }

    @Test void shouldFreezeLocalEnums() {
        assertThat(PlanStatus.values()).extracting(Enum::name).containsExactly("DRAFT", "ACTIVE", "RETIRED");
        assertThat(FeatureStatus.values()).extracting(Enum::name).containsExactly("ACTIVE", "INACTIVE");
        assertThat(DomainVerificationState.values()).extracting(Enum::name).containsExactly("PENDING", "VERIFIED", "REVOKED");
        assertThat(DomainVerificationMethod.values()).extracting(Enum::name).containsExactly("PLATFORM_MANUAL");
        assertThat(QuotaPeriodType.values()).extracting(Enum::name).containsExactly("CURRENT", "MONTHLY");
        assertThat(FeatureOverrideState.values()).extracting(Enum::name).containsExactly("GRANT", "DENY");
        assertThat(DeploymentStatus.values()).extracting(Enum::name).containsExactly("REGISTERED", "HEALTHY", "UNHEALTHY", "DISABLED");
    }

    private void assertType(Class<?> type, String field, Class<?> expected) throws Exception {
        assertThat(type.getDeclaredField(field).getType()).isEqualTo(expected);
    }

    private void assertGenerated(Class<?> type, String fieldName) throws Exception {
        TableField field = type.getDeclaredField(fieldName).getAnnotation(TableField.class);
        assertThat(field.insertStrategy()).isEqualTo(FieldStrategy.NEVER);
        assertThat(field.updateStrategy()).isEqualTo(FieldStrategy.NEVER);
    }
}
