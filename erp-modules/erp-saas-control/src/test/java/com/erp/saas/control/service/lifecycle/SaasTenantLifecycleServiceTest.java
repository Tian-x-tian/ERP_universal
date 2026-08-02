package com.erp.saas.control.service.lifecycle;

import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.lifecycle.impl.SaasTenantLifecycleServiceImpl;
import com.erp.saas.control.service.lifecycle.model.ActivateSubscriptionCommand;
import com.erp.saas.control.service.lifecycle.model.StartTrialCommand;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantLifecycleServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-08-02T00:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private SaasTenantMapper tenantMapper;
    private SaasSubscriptionMapper subscriptionMapper;
    private SaasPlanMapper planMapper;
    private SaasTenantLifecycleService service;

    @BeforeEach
    void setUp() {
        tenantMapper = mock(SaasTenantMapper.class);
        subscriptionMapper = mock(SaasSubscriptionMapper.class);
        planMapper = mock(SaasPlanMapper.class);
        service = serviceAt(INSTANT);
    }

    @Test
    void shouldStartPlanDefinedTrialAndGraceWindowAtOneUtcInstant() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant(TenantLifecycleState.PROVISIONING, 2L));
        when(planMapper.findByIdForUpdate(10L)).thenReturn(plan(10L, 14, 7));
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(null);
        doAnswer(invocation -> {
            SaasSubscriptionEntity row = invocation.getArgument(0);
            row.setSubscriptionId(100L);
            return 1;
        }).when(subscriptionMapper).insert(any());
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.PROVISIONING,
                TenantLifecycleState.TRIAL, null, 2L, "admin", NOW)).thenReturn(1);

        var view = service.startTrial(new StartTrialCommand("tenant_1", 10L, 2L, " admin "));

        assertThat(view.lifecycleState()).isEqualTo(TenantLifecycleState.TRIAL);
        assertThat(view.subscriptionState()).isEqualTo(SubscriptionState.TRIAL);
        assertThat(view.endAt()).isEqualTo(NOW.plusDays(14));
        assertThat(view.graceEndAt()).isEqualTo(NOW.plusDays(21));
        ArgumentCaptor<SaasSubscriptionEntity> captor = ArgumentCaptor.forClass(SaasSubscriptionEntity.class);
        verify(subscriptionMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreateTime()).isEqualTo(NOW);
        assertThat(captor.getValue().getNonExpiring()).isFalse();
    }

    @Test
    void shouldRetryProvisioningAndPreserveStateAcrossRecoverableSuspension() {
        SaasTenantEntity draft = tenant(TenantLifecycleState.DRAFT, 0L);
        SaasTenantEntity provisioning = tenant(TenantLifecycleState.PROVISIONING, 1L);
        SaasTenantEntity failed = tenant(TenantLifecycleState.PROVISION_FAILED, 2L);
        SaasTenantEntity active = tenant(TenantLifecycleState.ACTIVE, 4L);
        when(tenantMapper.lockByTenantId("tenant_1"))
                .thenReturn(draft, provisioning, failed, active);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.DRAFT,
                TenantLifecycleState.PROVISIONING, null, 0L, "admin", NOW)).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.PROVISIONING,
                TenantLifecycleState.PROVISION_FAILED, null, 1L, "admin", NOW)).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.PROVISION_FAILED,
                TenantLifecycleState.PROVISIONING, null, 2L, "admin", NOW)).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.ACTIVE,
                TenantLifecycleState.SUSPENDED, TenantLifecycleState.ACTIVE, 4L, "admin", NOW)).thenReturn(1);
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(null);

        assertThat(service.beginProvisioning(new TenantVersionCommand("tenant_1", 0L, "admin"))
                .lifecycleState()).isEqualTo(TenantLifecycleState.PROVISIONING);
        assertThat(service.markProvisionFailed(new TenantVersionCommand("tenant_1", 1L, "admin"))
                .lifecycleState()).isEqualTo(TenantLifecycleState.PROVISION_FAILED);
        assertThat(service.beginProvisioning(new TenantVersionCommand("tenant_1", 2L, "admin"))
                .lifecycleState()).isEqualTo(TenantLifecycleState.PROVISIONING);
        var suspended = service.suspend(new TenantVersionCommand("tenant_1", 4L, "admin"));
        assertThat(suspended.lifecycleState()).isEqualTo(TenantLifecycleState.SUSPENDED);
        assertThat(suspended.suspendedFromState()).isEqualTo(TenantLifecycleState.ACTIVE);
    }

    @Test
    void shouldSkipEmptyTrialAndGraceDirectlyToReadOnly() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant(TenantLifecycleState.PROVISIONING, 1L));
        when(planMapper.findByIdForUpdate(10L)).thenReturn(plan(10L, 0, 0));
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(null);
        when(subscriptionMapper.insert(any())).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.PROVISIONING,
                TenantLifecycleState.READ_ONLY, null, 1L, "admin", NOW)).thenReturn(1);

        var view = service.startTrial(new StartTrialCommand("tenant_1", 10L, 1L, "admin"));

        assertThat(view.lifecycleState()).isEqualTo(TenantLifecycleState.READ_ONLY);
        assertThat(view.subscriptionState()).isEqualTo(SubscriptionState.EXPIRED);
        assertThat(view.endAt()).isEqualTo(NOW);
        assertThat(view.graceEndAt()).isEqualTo(NOW);
    }

    @Test
    void shouldEnterGraceAtExactEndAndReadOnlyAtExactGraceEnd() {
        SaasTenantEntity active = tenant(TenantLifecycleState.ACTIVE, 3L);
        SaasSubscriptionEntity subscription = subscription(SubscriptionState.ACTIVE, 5L,
                NOW, NOW.plusDays(7), false);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(active);
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(subscription);
        when(subscriptionMapper.transitionState(100L, SubscriptionState.ACTIVE,
                SubscriptionState.GRACE, 5L, "system", NOW)).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.ACTIVE,
                TenantLifecycleState.GRACE, null, 3L, "system", NOW)).thenReturn(1);

        var grace = service.reconcile("tenant_1", "system");
        assertThat(grace.lifecycleState()).isEqualTo(TenantLifecycleState.GRACE);
        assertThat(grace.subscriptionState()).isEqualTo(SubscriptionState.GRACE);

        LocalDateTime graceEnd = NOW.plusDays(7);
        SaasTenantEntity graceTenant = tenant(TenantLifecycleState.GRACE, 4L);
        SaasSubscriptionEntity graceSubscription = subscription(SubscriptionState.GRACE, 6L,
                NOW.minusDays(7), graceEnd, false);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(graceTenant);
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(graceSubscription);
        when(subscriptionMapper.transitionState(100L, SubscriptionState.GRACE,
                SubscriptionState.EXPIRED, 6L, "system", graceEnd)).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.GRACE,
                TenantLifecycleState.READ_ONLY, null, 4L, "system", graceEnd)).thenReturn(1);

        var readOnly = serviceAt(graceEnd.toInstant(ZoneOffset.UTC)).reconcile("tenant_1", "system");
        assertThat(readOnly.lifecycleState()).isEqualTo(TenantLifecycleState.READ_ONLY);
        assertThat(readOnly.subscriptionState()).isEqualTo(SubscriptionState.EXPIRED);
    }

    @Test
    void shouldAdvanceSuspendedUnderlyingStateAndResumeThere() {
        SaasTenantEntity suspended = tenant(TenantLifecycleState.SUSPENDED, 6L);
        suspended.setSuspendedFromState(TenantLifecycleState.ACTIVE);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(suspended);
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(subscription(
                SubscriptionState.ACTIVE, 2L, NOW, NOW.plusDays(7), false));
        when(subscriptionMapper.transitionState(100L, SubscriptionState.ACTIVE,
                SubscriptionState.GRACE, 2L, "system", NOW)).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.SUSPENDED,
                TenantLifecycleState.SUSPENDED, TenantLifecycleState.GRACE, 6L, "system", NOW)).thenReturn(1);

        var reconciled = service.reconcile("tenant_1", "system");
        assertThat(reconciled.lifecycleState()).isEqualTo(TenantLifecycleState.SUSPENDED);
        assertThat(reconciled.suspendedFromState()).isEqualTo(TenantLifecycleState.GRACE);

        SaasTenantEntity resumable = tenant(TenantLifecycleState.SUSPENDED, 7L);
        resumable.setSuspendedFromState(TenantLifecycleState.GRACE);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(resumable);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.SUSPENDED,
                TenantLifecycleState.GRACE, null, 7L, "admin", NOW)).thenReturn(1);
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(null);
        var resumed = service.resume(new TenantVersionCommand("tenant_1", 7L, "admin"));
        assertThat(resumed.lifecycleState()).isEqualTo(TenantLifecycleState.GRACE);
    }

    @Test
    void shouldLeaveNonExpiringSubscriptionUntouched() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant(TenantLifecycleState.ACTIVE, 3L));
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(subscription(
                SubscriptionState.ACTIVE, 5L, null, null, true));

        var view = service.reconcile("tenant_1", "system");

        assertThat(view.nonExpiring()).isTrue();
        verify(subscriptionMapper, never()).transitionState(any(), any(), any(), any(), any(), any());
        verify(tenantMapper, never()).transitionLifecycle(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void shouldActivateNonExpiringLegacySubscription() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant(TenantLifecycleState.PROVISIONING, 1L));
        when(planMapper.findByIdForUpdate(10L)).thenReturn(plan(10L, 14, 7));
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(null);
        when(subscriptionMapper.insert(any())).thenReturn(1);
        when(tenantMapper.transitionLifecycle("tenant_1", TenantLifecycleState.PROVISIONING,
                TenantLifecycleState.ACTIVE, null, 1L, "admin", NOW)).thenReturn(1);

        var view = service.activate(new ActivateSubscriptionCommand(
                "tenant_1", 10L, 1L, null, true, "admin"));

        assertThat(view.lifecycleState()).isEqualTo(TenantLifecycleState.ACTIVE);
        assertThat(view.nonExpiring()).isTrue();
        assertThat(view.endAt()).isNull();
        assertThat(view.graceEndAt()).isNull();
    }

    @Test
    void shouldCancelCurrentSubscriptionWhenArchivingAndWaitNinetyDaysForPurge() {
        SaasTenantEntity row = tenant(TenantLifecycleState.READ_ONLY, 8L);
        SaasSubscriptionEntity current = subscription(SubscriptionState.GRACE, 4L,
                NOW.minusDays(1), NOW.plusDays(1), false);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(row);
        when(subscriptionMapper.findCurrentForUpdate("tenant_1")).thenReturn(current);
        when(subscriptionMapper.transitionState(100L, SubscriptionState.GRACE,
                SubscriptionState.CANCELED, 4L, "admin", NOW)).thenReturn(1);
        when(tenantMapper.archive("tenant_1", TenantLifecycleState.READ_ONLY, 8L,
                NOW.plusDays(90), "admin", NOW)).thenReturn(1);

        var archived = service.archive(new TenantVersionCommand("tenant_1", 8L, "admin"));
        assertThat(archived.lifecycleState()).isEqualTo(TenantLifecycleState.ARCHIVED);
        assertThat(archived.subscriptionState()).isEqualTo(SubscriptionState.CANCELED);
        assertThat(archived.purgeEligibleAt()).isEqualTo(NOW.plusDays(90));

        SaasTenantEntity tooEarly = tenant(TenantLifecycleState.ARCHIVED, 9L);
        tooEarly.setPurgeEligibleAt(NOW.plusDays(90));
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tooEarly);
        assertCode(SaasLifecycleException.ErrorCode.INVALID_TRANSITION,
                () -> serviceAt(INSTANT.plusSeconds(89L * 24 * 60 * 60))
                        .markPurgePending(new TenantVersionCommand("tenant_1", 9L, "admin")));

        when(tenantMapper.markPurgePending("tenant_1", 9L, "admin", NOW.plusDays(90))).thenReturn(1);
        var pending = serviceAt(INSTANT.plusSeconds(90L * 24 * 60 * 60))
                .markPurgePending(new TenantVersionCommand("tenant_1", 9L, "admin"));
        assertThat(pending.lifecycleState()).isEqualTo(TenantLifecycleState.PURGE_PENDING);
    }

    @Test
    void shouldRejectVersionConflictsAndInvalidTransitions() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant(TenantLifecycleState.DRAFT, 4L));
        assertCode(SaasLifecycleException.ErrorCode.VERSION_CONFLICT,
                () -> service.beginProvisioning(new TenantVersionCommand("tenant_1", 3L, "admin")));

        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant(TenantLifecycleState.ACTIVE, 4L));
        assertCode(SaasLifecycleException.ErrorCode.INVALID_TRANSITION,
                () -> service.beginProvisioning(new TenantVersionCommand("tenant_1", 4L, "admin")));
    }

    @Test
    void shouldMarkEveryMutationTransactional() {
        for (var method : SaasTenantLifecycleServiceImpl.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) continue;
            Transactional annotation = method.getAnnotation(Transactional.class);
            assertThat(annotation).as(method.getName()).isNotNull();
            assertThat(annotation.rollbackFor()).contains(Exception.class);
        }
    }

    private SaasTenantLifecycleService serviceAt(Instant instant) {
        return new SaasTenantLifecycleServiceImpl(tenantMapper, subscriptionMapper, planMapper,
                new ControlUtcTime(Clock.fixed(instant, ZoneOffset.UTC)));
    }

    private static SaasTenantEntity tenant(TenantLifecycleState state, long version) {
        SaasTenantEntity row = new SaasTenantEntity();
        row.setTenantId("tenant_1");
        row.setLifecycleState(state);
        row.setVersionNo(version);
        return row;
    }

    private static SaasPlanEntity plan(long id, int trialDays, int graceDays) {
        SaasPlanEntity row = new SaasPlanEntity();
        row.setPlanId(id);
        row.setStatus(PlanStatus.ACTIVE);
        row.setTrialDays(trialDays);
        row.setGraceDays(graceDays);
        return row;
    }

    private static SaasSubscriptionEntity subscription(SubscriptionState state, long version,
            LocalDateTime endAt, LocalDateTime graceEndAt, boolean nonExpiring) {
        SaasSubscriptionEntity row = new SaasSubscriptionEntity();
        row.setSubscriptionId(100L);
        row.setTenantId("tenant_1");
        row.setPlanId(10L);
        row.setState(state);
        row.setStartAt(NOW.minusDays(14));
        row.setEndAt(endAt);
        row.setGraceEndAt(graceEndAt);
        row.setNonExpiring(nonExpiring);
        row.setVersionNo(version);
        return row;
    }

    private static void assertCode(SaasLifecycleException.ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(SaasLifecycleException.class)
                .extracting(error -> ((SaasLifecycleException) error).getErrorCode()).isEqualTo(code);
    }
}
