package com.erp.saas.control.service;

import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.domain.entity.SaasFeatureEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.domain.entity.SaasTenantFeatureOverrideEntity;
import com.erp.saas.control.domain.entity.SaasTenantQuotaOverrideEntity;
import com.erp.saas.control.mapper.*;
import com.erp.saas.control.service.impl.SaasTenantEntitlementServiceImpl;
import com.erp.saas.control.service.model.FeatureOverrideCommand;
import com.erp.saas.control.service.model.QuotaOverrideCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantOverrideServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-08-01T10:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private SaasTenantMapper tenantMapper;
    private SaasFeatureMapper featureMapper;
    private SaasTenantFeatureOverrideMapper featureOverrideMapper;
    private SaasTenantQuotaOverrideMapper quotaOverrideMapper;
    private SaasTenantEntitlementService service;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SaasTenantMapper.class);
        featureMapper = mock(SaasFeatureMapper.class);
        featureOverrideMapper = mock(SaasTenantFeatureOverrideMapper.class);
        quotaOverrideMapper = mock(SaasTenantQuotaOverrideMapper.class);
        service = new SaasTenantEntitlementServiceImpl(tenantMapper, featureMapper,
                mock(SaasSubscriptionMapper.class), mock(SaasPlanMapper.class), mock(SaasPlanFeatureMapper.class),
                mock(SaasPlanQuotaMapper.class), featureOverrideMapper, quotaOverrideMapper,
                new ControlUtcTime(Clock.fixed(INSTANT, ZoneOffset.UTC)));
    }

    @Test
    void shouldLockTenantBeforeEmptyWindowAndInsertAtNow() {
        SaasTenantEntity tenant = tenant("tenant_1");
        SaasFeatureEntity feature = feature(9L, "reports.view", FeatureStatus.ACTIVE);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant);
        when(featureMapper.findByKeyForUpdate("reports.view")).thenReturn(feature);
        when(featureOverrideMapper.findWindowsForUpdate("tenant_1", 9L)).thenReturn(List.of());
        doAnswer(invocation -> {
            SaasTenantFeatureOverrideEntity row = invocation.getArgument(0);
            row.setOverrideId(100L);
            return 1;
        }).when(featureOverrideMapper).insert(any());

        Long id = service.addFeatureOverride(new FeatureOverrideCommand("tenant_1", "reports.view",
                FeatureOverrideState.GRANT, NOW, null, null), " admin ");

        assertThat(id).isEqualTo(100L);
        InOrder order = inOrder(tenantMapper, featureMapper, featureOverrideMapper);
        order.verify(tenantMapper).lockByTenantId("tenant_1");
        order.verify(featureMapper).findByKeyForUpdate("reports.view");
        order.verify(featureOverrideMapper).findWindowsForUpdate("tenant_1", 9L);
        order.verify(featureOverrideMapper).insert(any());
    }

    @Test
    void shouldRejectPastAndOverlappingWindowsButAllowAdjacency() {
        assertCode(SaasCatalogException.ErrorCode.INVALID_INPUT,
                () -> service.addFeatureOverride(new FeatureOverrideCommand("tenant_1", "reports.view",
                        FeatureOverrideState.GRANT, NOW.minusNanos(1), null, null), "admin"));

        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1"));
        when(featureMapper.findByKeyForUpdate("reports.view"))
                .thenReturn(feature(9L, "reports.view", FeatureStatus.ACTIVE));
        SaasTenantFeatureOverrideEntity existing = featureWindow(
                NOW.plusHours(1), NOW.plusHours(2), FeatureOverrideState.GRANT);
        when(featureOverrideMapper.findWindowsForUpdate("tenant_1", 9L)).thenReturn(List.of(existing));

        assertCode(SaasCatalogException.ErrorCode.OVERLAPPING_OVERRIDE,
                () -> service.addFeatureOverride(new FeatureOverrideCommand("tenant_1", "reports.view",
                        FeatureOverrideState.DENY, NOW.plusMinutes(30), NOW.plusHours(1).plusNanos(1), null), "admin"));

        service.addFeatureOverride(new FeatureOverrideCommand("tenant_1", "reports.view",
                FeatureOverrideState.DENY, NOW.plusHours(2), NOW.plusHours(3), null), "admin");
        verify(featureOverrideMapper).insert(any());
    }

    @Test
    void shouldAddUnlimitedQuotaOverrideAndDeleteOnlyFutureVersionedRows() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1"));
        when(quotaOverrideMapper.findWindowsForUpdate("tenant_1", SaasQuotaKeys.USER_COUNT))
                .thenReturn(List.of());
        doAnswer(invocation -> {
            SaasTenantQuotaOverrideEntity row = invocation.getArgument(0);
            row.setOverrideId(200L);
            return 1;
        }).when(quotaOverrideMapper).insert(any());
        assertThat(service.addQuotaOverride(new QuotaOverrideCommand("tenant_1", SaasQuotaKeys.USER_COUNT,
                null, NOW.plusMinutes(1), null, null), "admin")).isEqualTo(200L);

        SaasTenantQuotaOverrideEntity row = new SaasTenantQuotaOverrideEntity();
        row.setOverrideId(200L);
        row.setTenantId("tenant_1");
        row.setEffectiveFrom(NOW.plusMinutes(1));
        row.setVersionNo(3L);
        when(quotaOverrideMapper.selectById(200L)).thenReturn(row);
        when(quotaOverrideMapper.findByIdForUpdate(200L)).thenReturn(row);
        when(quotaOverrideMapper.deleteFutureVersioned(200L, 3L, NOW)).thenReturn(1);

        service.deleteFutureQuotaOverride(200L, 3L, "admin");
        InOrder order = inOrder(quotaOverrideMapper, tenantMapper);
        order.verify(quotaOverrideMapper).selectById(200L);
        order.verify(tenantMapper).lockByTenantId("tenant_1");
        order.verify(quotaOverrideMapper).findByIdForUpdate(200L);
        order.verify(quotaOverrideMapper).deleteFutureVersioned(200L, 3L, NOW);
    }

    @Test
    void shouldRejectDeletingActiveOrStaleOverride() {
        SaasTenantFeatureOverrideEntity row = featureWindow(NOW, null, FeatureOverrideState.GRANT);
        row.setOverrideId(10L);
        row.setTenantId("tenant_1");
        row.setVersionNo(2L);
        when(featureOverrideMapper.selectById(10L)).thenReturn(row);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1"));
        when(featureOverrideMapper.findByIdForUpdate(10L)).thenReturn(row);
        assertCode(SaasCatalogException.ErrorCode.INVALID_INPUT,
                () -> service.deleteFutureFeatureOverride(10L, 2L, "admin"));

        row.setEffectiveFrom(NOW.plusSeconds(1));
        assertCode(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                () -> service.deleteFutureFeatureOverride(10L, 1L, "admin"));
    }

    @Test
    void shouldMarkOverrideWritesTransactional() {
        for (var method : SaasTenantEntitlementServiceImpl.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())
                    || method.getName().equals("effectiveEntitlements")) {
                continue;
            }
            Transactional annotation = method.getAnnotation(Transactional.class);
            assertThat(annotation).as(method.getName()).isNotNull();
            assertThat(annotation.rollbackFor()).contains(Exception.class);
        }
    }

    private static SaasTenantEntity tenant(String tenantId) {
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId(tenantId);
        return tenant;
    }

    private static SaasFeatureEntity feature(Long id, String key, FeatureStatus status) {
        SaasFeatureEntity feature = new SaasFeatureEntity();
        feature.setFeatureId(id);
        feature.setFeatureKey(key);
        feature.setFeatureName(key);
        feature.setStatus(status);
        feature.setVersionNo(0L);
        return feature;
    }

    private static SaasTenantFeatureOverrideEntity featureWindow(LocalDateTime start, LocalDateTime end,
            FeatureOverrideState state) {
        SaasTenantFeatureOverrideEntity row = new SaasTenantFeatureOverrideEntity();
        row.setEffectiveFrom(start);
        row.setEffectiveUntil(end);
        row.setOverrideState(state);
        return row;
    }

    private static void assertCode(SaasCatalogException.ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(SaasCatalogException.class)
                .extracting(error -> ((SaasCatalogException) error).getErrorCode()).isEqualTo(code);
    }
}
