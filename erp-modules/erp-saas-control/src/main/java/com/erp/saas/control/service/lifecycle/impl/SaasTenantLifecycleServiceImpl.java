package com.erp.saas.control.service.lifecycle.impl;

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
import com.erp.saas.control.service.lifecycle.SaasLifecycleException;
import com.erp.saas.control.service.lifecycle.SaasLifecycleValidation;
import com.erp.saas.control.service.lifecycle.SaasTenantLifecycleService;
import com.erp.saas.control.service.lifecycle.model.ActivateSubscriptionCommand;
import com.erp.saas.control.service.lifecycle.model.SaasTenantLifecycleView;
import com.erp.saas.control.service.lifecycle.model.StartTrialCommand;
import com.erp.saas.control.service.lifecycle.model.TenantVersionCommand;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

@Service
public class SaasTenantLifecycleServiceImpl implements SaasTenantLifecycleService {
    private static final long ARCHIVE_RETENTION_DAYS = 90;
    private static final Set<TenantLifecycleState> SUSPENDABLE = EnumSet.of(
            TenantLifecycleState.TRIAL, TenantLifecycleState.ACTIVE,
            TenantLifecycleState.GRACE, TenantLifecycleState.READ_ONLY);
    private static final Set<TenantLifecycleState> ARCHIVABLE = EnumSet.of(
            TenantLifecycleState.DRAFT, TenantLifecycleState.PROVISION_FAILED,
            TenantLifecycleState.TRIAL, TenantLifecycleState.ACTIVE,
            TenantLifecycleState.GRACE, TenantLifecycleState.READ_ONLY,
            TenantLifecycleState.SUSPENDED);
    private static final Set<TenantLifecycleState> ACTIVATABLE = EnumSet.of(
            TenantLifecycleState.PROVISIONING, TenantLifecycleState.TRIAL,
            TenantLifecycleState.ACTIVE, TenantLifecycleState.GRACE,
            TenantLifecycleState.READ_ONLY, TenantLifecycleState.SUSPENDED);

    private final SaasTenantMapper tenantMapper;
    private final SaasSubscriptionMapper subscriptionMapper;
    private final SaasPlanMapper planMapper;
    private final ControlUtcTime time;

    public SaasTenantLifecycleServiceImpl(SaasTenantMapper tenantMapper,
            SaasSubscriptionMapper subscriptionMapper, SaasPlanMapper planMapper, ControlUtcTime time) {
        this.tenantMapper = tenantMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.planMapper = planMapper;
        this.time = time;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView beginProvisioning(TenantVersionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        requireVersion(tenant, command.expectedTenantVersion());
        if (tenant.getLifecycleState() != TenantLifecycleState.DRAFT
                && tenant.getLifecycleState() != TenantLifecycleState.PROVISION_FAILED) {
            throw invalidTransition("Only draft or failed tenants can start provisioning");
        }
        LocalDateTime now = time.now();
        transitionTenant(tenant, TenantLifecycleState.PROVISIONING, null, command.operator(), now);
        return view(tenant, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView markProvisionFailed(TenantVersionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        requireVersion(tenant, command.expectedTenantVersion());
        requireState(tenant, TenantLifecycleState.PROVISIONING,
                "Only provisioning tenants can be marked failed");
        transitionTenant(tenant, TenantLifecycleState.PROVISION_FAILED, null,
                command.operator(), time.now());
        return view(tenant, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView startTrial(StartTrialCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        requireVersion(tenant, command.expectedTenantVersion());
        requireState(tenant, TenantLifecycleState.PROVISIONING,
                "Only provisioning tenants can start a trial");
        SaasPlanEntity plan = activePlan(command.planId());
        if (subscriptionMapper.findCurrentForUpdate(command.tenantId()) != null) {
            throw subscriptionConflict("Tenant already has a current subscription", null);
        }
        LocalDateTime now = time.now();
        LocalDateTime endAt = now.plusDays(plan.getTrialDays());
        LocalDateTime graceEndAt = endAt.plusDays(plan.getGraceDays());
        SubscriptionState subscriptionState;
        TenantLifecycleState tenantState;
        if (plan.getTrialDays() > 0) {
            subscriptionState = SubscriptionState.TRIAL;
            tenantState = TenantLifecycleState.TRIAL;
        } else if (plan.getGraceDays() > 0) {
            subscriptionState = SubscriptionState.GRACE;
            tenantState = TenantLifecycleState.GRACE;
        } else {
            subscriptionState = SubscriptionState.EXPIRED;
            tenantState = TenantLifecycleState.READ_ONLY;
        }
        SaasSubscriptionEntity subscription = newSubscription(command.tenantId(), plan.getPlanId(),
                subscriptionState, now, endAt, graceEndAt, false, command.operator());
        insertSubscription(subscription);
        transitionTenant(tenant, tenantState, null, command.operator(), now);
        return view(tenant, subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView activate(ActivateSubscriptionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        requireVersion(tenant, command.expectedTenantVersion());
        if (!ACTIVATABLE.contains(tenant.getLifecycleState())) {
            throw invalidTransition("Tenant cannot activate a subscription from its current state");
        }
        SaasPlanEntity plan = activePlan(command.planId());
        LocalDateTime now = time.now();
        if (!command.nonExpiring() && !command.endAt().isAfter(now)) {
            throw SaasLifecycleValidation.invalid("endAt must be later than the current UTC time");
        }
        LocalDateTime graceEndAt = command.nonExpiring()
                ? null : command.endAt().plusDays(plan.getGraceDays());
        SaasSubscriptionEntity subscription = subscriptionMapper.findCurrentForUpdate(command.tenantId());
        if (subscription == null) {
            subscription = newSubscription(command.tenantId(), plan.getPlanId(), SubscriptionState.ACTIVE,
                    now, command.endAt(), graceEndAt, command.nonExpiring(), command.operator());
            insertSubscription(subscription);
        } else {
            int affected = subscriptionMapper.renewCurrent(subscription.getSubscriptionId(), plan.getPlanId(),
                    command.endAt(), graceEndAt, command.nonExpiring(), subscription.getVersionNo(),
                    command.operator(), now);
            casSubscription(affected);
            subscription.setPlanId(plan.getPlanId());
            subscription.setState(SubscriptionState.ACTIVE);
            subscription.setStartAt(now);
            subscription.setEndAt(command.endAt());
            subscription.setGraceEndAt(graceEndAt);
            subscription.setNonExpiring(command.nonExpiring());
            subscription.setVersionNo(subscription.getVersionNo() + 1);
        }
        TenantLifecycleState next = tenant.getLifecycleState() == TenantLifecycleState.SUSPENDED
                ? TenantLifecycleState.SUSPENDED : TenantLifecycleState.ACTIVE;
        TenantLifecycleState suspendedFrom = next == TenantLifecycleState.SUSPENDED
                ? TenantLifecycleState.ACTIVE : null;
        transitionTenant(tenant, next, suspendedFrom, command.operator(), now);
        return view(tenant, subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView reconcile(String tenantId, String operator) {
        tenantId = SaasLifecycleValidation.tenantId(tenantId);
        operator = SaasLifecycleValidation.operator(operator);
        SaasTenantEntity tenant = locked(tenantId);
        SaasSubscriptionEntity subscription = subscriptionMapper.findCurrentForUpdate(tenantId);
        if (subscription == null) {
            ensureNoMissingCurrentSubscription(tenant);
            return view(tenant, null);
        }
        validateSubscriptionAlignment(tenant, subscription);
        if (Boolean.TRUE.equals(subscription.getNonExpiring())) {
            if (subscription.getState() != SubscriptionState.ACTIVE
                    || subscription.getEndAt() != null || subscription.getGraceEndAt() != null) {
                throw subscriptionConflict("Non-expiring subscription is inconsistent", null);
            }
            return view(tenant, subscription);
        }
        if (subscription.getEndAt() == null || subscription.getGraceEndAt() == null
                || subscription.getGraceEndAt().isBefore(subscription.getEndAt())) {
            throw subscriptionConflict("Finite subscription dates are inconsistent", null);
        }
        LocalDateTime now = time.now();
        SubscriptionState next = nextState(subscription, now);
        if (next == subscription.getState()) {
            return view(tenant, subscription);
        }
        casSubscription(subscriptionMapper.transitionState(subscription.getSubscriptionId(),
                subscription.getState(), next, subscription.getVersionNo(), operator, now));
        subscription.setState(next);
        subscription.setVersionNo(subscription.getVersionNo() + 1);
        TenantLifecycleState nextLifecycle = next == SubscriptionState.GRACE
                ? TenantLifecycleState.GRACE : TenantLifecycleState.READ_ONLY;
        if (tenant.getLifecycleState() == TenantLifecycleState.SUSPENDED) {
            transitionTenant(tenant, TenantLifecycleState.SUSPENDED, nextLifecycle, operator, now);
        } else {
            transitionTenant(tenant, nextLifecycle, null, operator, now);
        }
        return view(tenant, subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView suspend(TenantVersionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        requireVersion(tenant, command.expectedTenantVersion());
        if (!SUSPENDABLE.contains(tenant.getLifecycleState())) {
            throw invalidTransition("Tenant cannot be suspended from its current state");
        }
        TenantLifecycleState previous = tenant.getLifecycleState();
        transitionTenant(tenant, TenantLifecycleState.SUSPENDED, previous, command.operator(), time.now());
        return view(tenant, subscriptionMapper.findCurrentForUpdate(command.tenantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView resume(TenantVersionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        requireVersion(tenant, command.expectedTenantVersion());
        requireState(tenant, TenantLifecycleState.SUSPENDED, "Only suspended tenants can be resumed");
        TenantLifecycleState restored = tenant.getSuspendedFromState();
        if (!SUSPENDABLE.contains(restored)) {
            throw invalidTransition("Suspended tenant has no resumable prior state");
        }
        transitionTenant(tenant, restored, null, command.operator(), time.now());
        return view(tenant, subscriptionMapper.findCurrentForUpdate(command.tenantId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView archive(TenantVersionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        if (tenant.getLifecycleState() == TenantLifecycleState.ARCHIVED) {
            return view(tenant, null);
        }
        requireVersion(tenant, command.expectedTenantVersion());
        if (!ARCHIVABLE.contains(tenant.getLifecycleState())) {
            throw invalidTransition("Tenant cannot be archived from its current state");
        }
        LocalDateTime now = time.now();
        SaasSubscriptionEntity subscription = subscriptionMapper.findCurrentForUpdate(command.tenantId());
        if (subscription != null) {
            casSubscription(subscriptionMapper.transitionState(subscription.getSubscriptionId(),
                    subscription.getState(), SubscriptionState.CANCELED, subscription.getVersionNo(),
                    command.operator(), now));
            subscription.setState(SubscriptionState.CANCELED);
            subscription.setVersionNo(subscription.getVersionNo() + 1);
        }
        LocalDateTime purgeEligibleAt = now.plusDays(ARCHIVE_RETENTION_DAYS);
        int affected = tenantMapper.archive(tenant.getTenantId(), tenant.getLifecycleState(),
                tenant.getVersionNo(), purgeEligibleAt, command.operator(), now);
        casTenant(affected);
        tenant.setLifecycleState(TenantLifecycleState.ARCHIVED);
        tenant.setSuspendedFromState(null);
        tenant.setArchivedAt(now);
        tenant.setPurgeEligibleAt(purgeEligibleAt);
        tenant.setVersionNo(tenant.getVersionNo() + 1);
        return view(tenant, subscription);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasTenantLifecycleView markPurgePending(TenantVersionCommand command) {
        SaasTenantEntity tenant = locked(command.tenantId());
        if (tenant.getLifecycleState() == TenantLifecycleState.PURGE_PENDING) {
            return view(tenant, null);
        }
        requireVersion(tenant, command.expectedTenantVersion());
        requireState(tenant, TenantLifecycleState.ARCHIVED,
                "Only archived tenants can be marked purge pending");
        LocalDateTime now = time.now();
        if (tenant.getPurgeEligibleAt() == null || now.isBefore(tenant.getPurgeEligibleAt())) {
            throw invalidTransition("Tenant retention period has not elapsed");
        }
        casTenant(tenantMapper.markPurgePending(tenant.getTenantId(), tenant.getVersionNo(),
                command.operator(), now));
        tenant.setLifecycleState(TenantLifecycleState.PURGE_PENDING);
        tenant.setVersionNo(tenant.getVersionNo() + 1);
        return view(tenant, null);
    }

    private SaasPlanEntity activePlan(Long planId) {
        SaasPlanEntity plan = planMapper.findByIdForUpdate(planId);
        if (plan == null) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.NOT_FOUND, "Plan not found");
        }
        if (plan.getStatus() != PlanStatus.ACTIVE) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.PLAN_NOT_ACTIVE,
                    "Only an active plan can be assigned");
        }
        if (plan.getTrialDays() == null || plan.getTrialDays() < 0
                || plan.getGraceDays() == null || plan.getGraceDays() < 0) {
            throw subscriptionConflict("Plan lifecycle settings are inconsistent", null);
        }
        return plan;
    }

    private void insertSubscription(SaasSubscriptionEntity subscription) {
        try {
            if (subscriptionMapper.insert(subscription) != 1) {
                throw subscriptionConflict("Subscription was not created", null);
            }
        } catch (DuplicateKeyException exception) {
            throw subscriptionConflict("Tenant already has a current subscription", exception);
        }
    }

    private static SaasSubscriptionEntity newSubscription(String tenantId, Long planId,
            SubscriptionState state, LocalDateTime now, LocalDateTime endAt,
            LocalDateTime graceEndAt, boolean nonExpiring, String operator) {
        SaasSubscriptionEntity row = new SaasSubscriptionEntity();
        row.setTenantId(tenantId);
        row.setPlanId(planId);
        row.setState(state);
        row.setStartAt(now);
        row.setEndAt(endAt);
        row.setGraceEndAt(graceEndAt);
        row.setNonExpiring(nonExpiring);
        row.setCreateBy(operator);
        row.setCreateTime(now);
        row.setUpdateBy(operator);
        row.setUpdateTime(now);
        row.setVersionNo(0L);
        return row;
    }

    private void transitionTenant(SaasTenantEntity tenant, TenantLifecycleState next,
            TenantLifecycleState suspendedFrom, String operator, LocalDateTime now) {
        casTenant(tenantMapper.transitionLifecycle(tenant.getTenantId(), tenant.getLifecycleState(),
                next, suspendedFrom, tenant.getVersionNo(), operator, now));
        tenant.setLifecycleState(next);
        tenant.setSuspendedFromState(suspendedFrom);
        tenant.setVersionNo(tenant.getVersionNo() + 1);
    }

    private SaasTenantEntity locked(String tenantId) {
        SaasTenantEntity tenant = tenantMapper.lockByTenantId(tenantId);
        if (tenant == null) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.NOT_FOUND, "Tenant not found");
        }
        return tenant;
    }

    private static void validateSubscriptionAlignment(SaasTenantEntity tenant,
            SaasSubscriptionEntity subscription) {
        TenantLifecycleState actual = tenant.getLifecycleState() == TenantLifecycleState.SUSPENDED
                ? tenant.getSuspendedFromState() : tenant.getLifecycleState();
        TenantLifecycleState expected = switch (subscription.getState()) {
            case TRIAL -> TenantLifecycleState.TRIAL;
            case ACTIVE -> TenantLifecycleState.ACTIVE;
            case GRACE -> TenantLifecycleState.GRACE;
            default -> null;
        };
        if (expected == null || actual != expected) {
            throw subscriptionConflict("Tenant and current subscription states are inconsistent", null);
        }
    }

    private static void ensureNoMissingCurrentSubscription(SaasTenantEntity tenant) {
        TenantLifecycleState effective = tenant.getLifecycleState() == TenantLifecycleState.SUSPENDED
                ? tenant.getSuspendedFromState() : tenant.getLifecycleState();
        if (effective == TenantLifecycleState.TRIAL || effective == TenantLifecycleState.ACTIVE
                || effective == TenantLifecycleState.GRACE) {
            throw subscriptionConflict("Tenant lifecycle requires a current subscription", null);
        }
    }

    private static SubscriptionState nextState(SaasSubscriptionEntity subscription, LocalDateTime now) {
        if (subscription.getState() == SubscriptionState.GRACE) {
            return now.isBefore(subscription.getGraceEndAt())
                    ? SubscriptionState.GRACE : SubscriptionState.EXPIRED;
        }
        if ((subscription.getState() == SubscriptionState.TRIAL
                || subscription.getState() == SubscriptionState.ACTIVE)
                && !now.isBefore(subscription.getEndAt())) {
            return now.isBefore(subscription.getGraceEndAt())
                    ? SubscriptionState.GRACE : SubscriptionState.EXPIRED;
        }
        return subscription.getState();
    }

    private static void requireVersion(SaasTenantEntity tenant, Long expectedVersion) {
        if (!Objects.equals(tenant.getVersionNo(), expectedVersion)) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.VERSION_CONFLICT,
                    "The expected tenant version no longer matches");
        }
    }

    private static void requireState(SaasTenantEntity tenant, TenantLifecycleState expected, String message) {
        if (tenant.getLifecycleState() != expected) {
            throw invalidTransition(message);
        }
    }

    private static void casTenant(int affected) {
        if (affected != 1) {
            throw new SaasLifecycleException(SaasLifecycleException.ErrorCode.VERSION_CONFLICT,
                    "Tenant state changed concurrently");
        }
    }

    private static void casSubscription(int affected) {
        if (affected != 1) {
            throw subscriptionConflict("Subscription state changed concurrently", null);
        }
    }

    private static SaasLifecycleException invalidTransition(String message) {
        return new SaasLifecycleException(SaasLifecycleException.ErrorCode.INVALID_TRANSITION, message);
    }

    private static SaasLifecycleException subscriptionConflict(String message, Throwable cause) {
        return cause == null
                ? new SaasLifecycleException(SaasLifecycleException.ErrorCode.CURRENT_SUBSCRIPTION_CONFLICT, message)
                : new SaasLifecycleException(SaasLifecycleException.ErrorCode.CURRENT_SUBSCRIPTION_CONFLICT,
                        message, cause);
    }

    private static SaasTenantLifecycleView view(SaasTenantEntity tenant, SaasSubscriptionEntity subscription) {
        return new SaasTenantLifecycleView(tenant.getTenantId(), tenant.getLifecycleState(),
                tenant.getSuspendedFromState(), tenant.getVersionNo(),
                subscription == null ? null : subscription.getSubscriptionId(),
                subscription == null ? null : subscription.getState(),
                subscription == null ? null : subscription.getPlanId(),
                subscription == null ? null : subscription.getStartAt(),
                subscription == null ? null : subscription.getEndAt(),
                subscription == null ? null : subscription.getGraceEndAt(),
                subscription != null && Boolean.TRUE.equals(subscription.getNonExpiring()),
                subscription == null ? null : subscription.getVersionNo(),
                tenant.getArchivedAt(), tenant.getPurgeEligibleAt());
    }
}
