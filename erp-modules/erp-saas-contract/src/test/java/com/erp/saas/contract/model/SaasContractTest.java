package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class SaasContractTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final long augustUtc = ZonedDateTime.of(2026, 8, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            .toInstant().toEpochMilli();

    @Test
    void shouldExposeFrozenEnumValuesAndQuotaKeys() {
        Assertions.assertArrayEquals(new TenantLifecycleState[]{TenantLifecycleState.DRAFT,
                TenantLifecycleState.PROVISIONING, TenantLifecycleState.TRIAL, TenantLifecycleState.ACTIVE,
                TenantLifecycleState.GRACE, TenantLifecycleState.READ_ONLY, TenantLifecycleState.ARCHIVED,
                TenantLifecycleState.PURGE_PENDING, TenantLifecycleState.PURGED, TenantLifecycleState.SUSPENDED,
                TenantLifecycleState.PROVISION_FAILED}, TenantLifecycleState.values());
        Assertions.assertArrayEquals(new DeploymentMode[]{DeploymentMode.SHARED, DeploymentMode.DEDICATED},
                DeploymentMode.values());
        Assertions.assertArrayEquals(new SubscriptionState[]{SubscriptionState.TRIAL, SubscriptionState.ACTIVE,
                SubscriptionState.GRACE, SubscriptionState.EXPIRED, SubscriptionState.CANCELED},
                SubscriptionState.values());
        Assertions.assertArrayEquals(new SaasUsageOperation[]{SaasUsageOperation.RESERVE, SaasUsageOperation.SETTLE,
                SaasUsageOperation.RELEASE, SaasUsageOperation.REPORT}, SaasUsageOperation.values());
        Assertions.assertEquals(Set.of("user_count", "storage_bytes", "ai_input_tokens", "ai_output_tokens"),
                Set.of(SaasQuotaKeys.USER_COUNT, SaasQuotaKeys.STORAGE_BYTES, SaasQuotaKeys.AI_INPUT_TOKENS,
                        SaasQuotaKeys.AI_OUTPUT_TOKENS));
        Assertions.assertTrue(Modifier.isFinal(SaasQuotaKeys.class.getModifiers()));
        Assertions.assertEquals(1, SaasQuotaKeys.class.getDeclaredConstructors().length);
        Assertions.assertTrue(Modifier.isPrivate(SaasQuotaKeys.class.getDeclaredConstructors()[0].getModifiers()));
    }

    @Test
    void shouldBePojoOnlyUnknownFieldCompatibleMutableBeans() throws Exception {
        List<Class<?>> beanTypes = List.of(SaasFeatureGrant.class, SaasQuotaLimit.class, SaasQuotaUsage.class,
                SaasHostResolution.class, SaasUsageEvent.class, SaasProvisioningRequest.class,
                SaasProvisioningResult.class, SaasEntitlementSnapshot.class);

        for (Class<?> type : beanTypes) {
            JsonIgnoreProperties annotation = type.getAnnotation(JsonIgnoreProperties.class);
            Assertions.assertNotNull(annotation, type.getSimpleName());
            Assertions.assertTrue(annotation.ignoreUnknown(), type.getSimpleName());
            Assertions.assertNotNull(type.getDeclaredConstructor().newInstance());
            Assertions.assertTrue(Serializable.class.isAssignableFrom(type), type.getSimpleName());
            assertNoFrameworkAnnotations(type);
            Arrays.stream(type.getDeclaredFields()).forEach(this::assertNoFrameworkAnnotations);
            Arrays.stream(type.getDeclaredMethods()).forEach(this::assertNoFrameworkAnnotations);
            Arrays.stream(type.getDeclaredConstructors()).forEach(this::assertNoFrameworkAnnotations);
        }

        SaasHostResolution decoded = objectMapper.readValue("{\"host\":\"acme.example\",\"tenantId\":\"t1\","
                + "\"deploymentMode\":\"SHARED\",\"lifecycleState\":\"ACTIVE\",\"verified\":true,"
                + "\"futureField\":\"ignored\"}", SaasHostResolution.class);
        decoded.setTenantId("t2");
        Assertions.assertEquals("t2", decoded.getTenantId());
        Assertions.assertEquals("acme.example", decoded.getHost());
        Assertions.assertTrue(decoded.isVerified());
    }

    @Test
    void shouldExposeOnlyTheFrozenContractFields() {
        Map<Class<?>, Map<String, Class<?>>> expected = Map.of(
                SaasFeatureGrant.class, Map.of("featureKey", String.class, "granted", boolean.class),
                SaasQuotaLimit.class, Map.of("quotaKey", String.class, "limit", Long.class),
                SaasQuotaUsage.class, Map.of("quotaKey", String.class, "used", long.class, "reserved", long.class,
                        "periodStartEpochMs", Long.class),
                SaasHostResolution.class, Map.of("host", String.class, "tenantId", String.class,
                        "deploymentMode", DeploymentMode.class, "lifecycleState", TenantLifecycleState.class,
                        "verified", boolean.class),
                SaasUsageEvent.class, Map.of("idempotencyKey", String.class, "tenantId", String.class,
                        "metricKey", String.class, "operation", SaasUsageOperation.class,
                        "referenceKey", String.class, "amount", Long.class, "periodStartEpochMs", Long.class,
                        "occurredAtEpochMs", long.class),
                SaasProvisioningRequest.class, Map.of("requestId", String.class, "tenantId", String.class,
                        "deploymentMode", DeploymentMode.class, "planCode", String.class),
                SaasProvisioningResult.class, Map.of("requestId", String.class, "tenantId", String.class,
                        "success", boolean.class, "message", String.class, "activationRequired", boolean.class,
                        "completedAtEpochMs", long.class),
                SaasEntitlementSnapshot.class, Map.ofEntries(
                        Map.entry("tenantId", String.class),
                        Map.entry("lifecycleState", TenantLifecycleState.class),
                        Map.entry("deploymentMode", DeploymentMode.class),
                        Map.entry("subscriptionState", SubscriptionState.class),
                        Map.entry("planCode", String.class),
                        Map.entry("version", long.class),
                        Map.entry("issuedAtEpochMs", long.class),
                        Map.entry("expiresAtEpochMs", long.class),
                        Map.entry("featureGrants", List.class),
                        Map.entry("quotaLimits", List.class),
                        Map.entry("signatureKeyId", String.class),
                        Map.entry("signature", String.class)));

        expected.forEach((type, fields) -> {
            Map<String, Class<?>> actual = Arrays.stream(type.getDeclaredFields())
                    .filter(field -> !Modifier.isStatic(field.getModifiers()))
                    .collect(Collectors.toMap(Field::getName, Field::getType));
            Assertions.assertEquals(fields, actual, type.getSimpleName());
        });
    }

    @Test
    void shouldRoundTripAllUsageOperations() throws Exception {
        for (SaasUsageOperation operation : SaasUsageOperation.values()) {
            boolean report = operation == SaasUsageOperation.REPORT;
            Long amount = operation == SaasUsageOperation.RELEASE ? null : 5L;
            SaasUsageEvent event = new SaasUsageEvent("event-" + operation, "tenant-a", SaasQuotaKeys.USER_COUNT,
                    operation, report ? null : "ref-a", amount, null, 1000L);

            SaasUsageEvent decoded = objectMapper.readValue(objectMapper.writeValueAsBytes(event),
                    SaasUsageEvent.class);

            Assertions.assertEquals(operation, decoded.getOperation());
            Assertions.assertEquals(event.getIdempotencyKey(), decoded.getIdempotencyKey());
            Assertions.assertEquals(event.getReferenceKey(), decoded.getReferenceKey());
            Assertions.assertEquals(event.getAmount(), decoded.getAmount());
        }
    }

    @Test
    void shouldKeepSnapshotListsMutableOrderedAndNonNull() throws Exception {
        SaasEntitlementSnapshot snapshot = new SaasEntitlementSnapshot();
        snapshot.getFeatureGrants().add(new SaasFeatureGrant("feature.one", true));
        snapshot.getFeatureGrants().add(new SaasFeatureGrant("feature.two", false));
        snapshot.setFeatureGrants(null);
        snapshot.setQuotaLimits(null);

        String json = objectMapper.writeValueAsString(snapshot);

        Assertions.assertNotNull(snapshot.getFeatureGrants());
        Assertions.assertNotNull(snapshot.getQuotaLimits());
        Assertions.assertTrue(json.contains("\"featureGrants\":[]"));
        Assertions.assertTrue(json.contains("\"quotaLimits\":[]"));

        snapshot.setFeatureGrants(List.of(new SaasFeatureGrant("feature.one", true),
                new SaasFeatureGrant("feature.two", false)));
        SaasEntitlementSnapshot decoded = objectMapper.readValue(objectMapper.writeValueAsBytes(snapshot),
                SaasEntitlementSnapshot.class);
        Assertions.assertEquals(List.of("feature.one", "feature.two"), decoded.getFeatureGrants().stream()
                .map(SaasFeatureGrant::getFeatureKey).collect(Collectors.toList()));
        decoded.getFeatureGrants().add(new SaasFeatureGrant("feature.three", true));
    }

    @Test
    void shouldValidatePositiveUsageBoundaries() {
        Assertions.assertDoesNotThrow(() -> SaasUsageEventValidator.validate(event(SaasUsageOperation.RESERVE,
                SaasQuotaKeys.USER_COUNT, "ref", 1L, null)));
        Assertions.assertDoesNotThrow(() -> SaasUsageEventValidator.validate(event(SaasUsageOperation.SETTLE,
                SaasQuotaKeys.STORAGE_BYTES, "ref", 0L, null)));
        Assertions.assertDoesNotThrow(() -> SaasUsageEventValidator.validate(event(SaasUsageOperation.RELEASE,
                SaasQuotaKeys.STORAGE_BYTES, "ref", null, null)));
        Assertions.assertDoesNotThrow(() -> SaasUsageEventValidator.validate(event(SaasUsageOperation.REPORT,
                SaasQuotaKeys.AI_INPUT_TOKENS, null, 0L, augustUtc)));
    }

    @Test
    void shouldRejectInvalidUsageOperationBoundaries() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.RESERVE, SaasQuotaKeys.USER_COUNT, "ref", 0L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.SETTLE, SaasQuotaKeys.USER_COUNT, null, 1L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.RELEASE, SaasQuotaKeys.USER_COUNT, "ref", 0L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, SaasQuotaKeys.USER_COUNT, "ref", 1L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, SaasQuotaKeys.USER_COUNT, " ", 1L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, SaasQuotaKeys.USER_COUNT, null, -1L, null)));
    }

    @Test
    void shouldValidateMetricPeriodsAndRequiredFields() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, "unknown", null, 0L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, SaasQuotaKeys.USER_COUNT, null, 0L, augustUtc)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, SaasQuotaKeys.AI_OUTPUT_TOKENS, null, 0L, null)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(
                event(SaasUsageOperation.REPORT, SaasQuotaKeys.AI_OUTPUT_TOKENS, null, 0L,
                        augustUtc + 1)));
        SaasUsageEvent missing = event(SaasUsageOperation.REPORT, SaasQuotaKeys.USER_COUNT, null, 0L, null);
        missing.setIdempotencyKey(" ");
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(missing));
        missing.setIdempotencyKey("event-a");
        missing.setOccurredAtEpochMs(0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(missing));
    }

    @Test
    void shouldRejectUsageFieldsThatExceedPersistenceBoundaries() {
        SaasUsageEvent invalid = event(SaasUsageOperation.RESERVE, SaasQuotaKeys.USER_COUNT, "ref", 1L, null);
        invalid.setIdempotencyKey("e".repeat(129));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(invalid));
        invalid.setIdempotencyKey("event-a");
        invalid.setTenantId("t".repeat(21));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(invalid));
        invalid.setTenantId("tenant-a");
        invalid.setReferenceKey("r".repeat(129));
        Assertions.assertThrows(IllegalArgumentException.class, () -> SaasUsageEventValidator.validate(invalid));
    }

    private SaasUsageEvent event(SaasUsageOperation operation, String metric, String reference, Long amount,
            Long period) {
        return new SaasUsageEvent("event-a", "tenant-a", metric, operation, reference, amount, period,
                Instant.parse("2026-08-01T00:00:00Z").toEpochMilli());
    }

    private void assertNoFrameworkAnnotations(AnnotatedElement element) {
        for (Annotation declared : element.getAnnotations()) {
            String annotationName = declared.annotationType().getName();
            Assertions.assertFalse(annotationName.startsWith("org.springframework"), annotationName);
            Assertions.assertFalse(annotationName.startsWith("com.baomidou"), annotationName);
        }
    }
}
