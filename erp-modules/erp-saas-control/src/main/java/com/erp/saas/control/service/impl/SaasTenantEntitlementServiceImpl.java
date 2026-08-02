package com.erp.saas.control.service.impl;

import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.control.domain.FeatureOverrideState;
import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.domain.entity.*;
import com.erp.saas.control.mapper.*;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.SaasCatalogException;
import com.erp.saas.control.service.SaasCatalogValidation;
import com.erp.saas.control.service.SaasTenantEntitlementService;
import com.erp.saas.control.service.model.EffectiveTenantEntitlements;
import com.erp.saas.control.service.model.FeatureOverrideCommand;
import com.erp.saas.control.service.model.QuotaEntitlement;
import com.erp.saas.control.service.model.QuotaOverrideCommand;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SaasTenantEntitlementServiceImpl implements SaasTenantEntitlementService {
    private static final List<String> QUOTA_KEYS = List.of(SaasQuotaKeys.USER_COUNT,
            SaasQuotaKeys.STORAGE_BYTES, SaasQuotaKeys.AI_INPUT_TOKENS, SaasQuotaKeys.AI_OUTPUT_TOKENS);

    private final SaasTenantMapper tenantMapper;
    private final SaasFeatureMapper featureMapper;
    private final SaasSubscriptionMapper subscriptionMapper;
    private final SaasPlanMapper planMapper;
    private final SaasPlanFeatureMapper planFeatureMapper;
    private final SaasPlanQuotaMapper planQuotaMapper;
    private final SaasTenantFeatureOverrideMapper featureOverrideMapper;
    private final SaasTenantQuotaOverrideMapper quotaOverrideMapper;
    private final ControlUtcTime time;

    public SaasTenantEntitlementServiceImpl(SaasTenantMapper tenantMapper, SaasFeatureMapper featureMapper,
            SaasSubscriptionMapper subscriptionMapper, SaasPlanMapper planMapper,
            SaasPlanFeatureMapper planFeatureMapper, SaasPlanQuotaMapper planQuotaMapper,
            SaasTenantFeatureOverrideMapper featureOverrideMapper,
            SaasTenantQuotaOverrideMapper quotaOverrideMapper, ControlUtcTime time) {
        this.tenantMapper = tenantMapper;
        this.featureMapper = featureMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.planMapper = planMapper;
        this.planFeatureMapper = planFeatureMapper;
        this.planQuotaMapper = planQuotaMapper;
        this.featureOverrideMapper = featureOverrideMapper;
        this.quotaOverrideMapper = quotaOverrideMapper;
        this.time = time;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addFeatureOverride(FeatureOverrideCommand command, String operator) {
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        requireFutureOrNow(command.effectiveFrom(), now);
        lockTenant(command.tenantId());
        SaasFeatureEntity feature = featureMapper.findByKeyForUpdate(command.featureKey());
        if (feature == null) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.UNKNOWN_FEATURE_KEY,
                    "Unknown feature key: " + command.featureKey());
        }
        List<SaasTenantFeatureOverrideEntity> windows = featureOverrideMapper
                .findWindowsForUpdate(command.tenantId(), feature.getFeatureId());
        ensureNoOverlap(command.effectiveFrom(), command.effectiveUntil(), windows.stream()
                .map(row -> new Window(row.getEffectiveFrom(), row.getEffectiveUntil())).toList());
        SaasTenantFeatureOverrideEntity row = new SaasTenantFeatureOverrideEntity();
        row.setTenantId(command.tenantId());
        row.setFeatureId(feature.getFeatureId());
        row.setOverrideState(command.overrideState());
        row.setEffectiveFrom(command.effectiveFrom());
        row.setEffectiveUntil(command.effectiveUntil());
        row.setReason(command.reason());
        audit(row, actor, now);
        try {
            featureOverrideMapper.insert(row);
        } catch (DuplicateKeyException exception) {
            throw overlap(exception);
        }
        return row.getOverrideId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addQuotaOverride(QuotaOverrideCommand command, String operator) {
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        requireFutureOrNow(command.effectiveFrom(), now);
        lockTenant(command.tenantId());
        List<SaasTenantQuotaOverrideEntity> windows = quotaOverrideMapper
                .findWindowsForUpdate(command.tenantId(), command.quotaKey());
        ensureNoOverlap(command.effectiveFrom(), command.effectiveUntil(), windows.stream()
                .map(row -> new Window(row.getEffectiveFrom(), row.getEffectiveUntil())).toList());
        SaasTenantQuotaOverrideEntity row = new SaasTenantQuotaOverrideEntity();
        row.setTenantId(command.tenantId());
        row.setQuotaKey(command.quotaKey());
        row.setLimitValue(command.limitValue());
        row.setEffectiveFrom(command.effectiveFrom());
        row.setEffectiveUntil(command.effectiveUntil());
        row.setReason(command.reason());
        audit(row, actor, now);
        try {
            quotaOverrideMapper.insert(row);
        } catch (DuplicateKeyException exception) {
            throw overlap(exception);
        }
        return row.getOverrideId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFutureFeatureOverride(Long overrideId, Long expectedVersion, String operator) {
        requiredIdAndVersion(overrideId, expectedVersion);
        time.operator(operator);
        LocalDateTime now = time.now();
        SaasTenantFeatureOverrideEntity hint = featureOverrideMapper.selectById(overrideId);
        if (hint == null) {
            throw notFound("Feature override not found");
        }
        lockTenant(hint.getTenantId());
        SaasTenantFeatureOverrideEntity row = featureOverrideMapper.findByIdForUpdate(overrideId);
        requireFutureRow(row, expectedVersion, now, "Feature override not found");
        cas(featureOverrideMapper.deleteFutureVersioned(overrideId, expectedVersion, now));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteFutureQuotaOverride(Long overrideId, Long expectedVersion, String operator) {
        requiredIdAndVersion(overrideId, expectedVersion);
        time.operator(operator);
        LocalDateTime now = time.now();
        SaasTenantQuotaOverrideEntity hint = quotaOverrideMapper.selectById(overrideId);
        if (hint == null) {
            throw notFound("Quota override not found");
        }
        lockTenant(hint.getTenantId());
        SaasTenantQuotaOverrideEntity row = quotaOverrideMapper.findByIdForUpdate(overrideId);
        requireFutureRow(row, expectedVersion, now, "Quota override not found");
        cas(quotaOverrideMapper.deleteFutureVersioned(overrideId, expectedVersion, now));
    }

    @Override
    @Transactional(readOnly = true)
    public EffectiveTenantEntitlements effectiveEntitlements(String tenantId) {
        String normalizedTenantId = SaasCatalogValidation.tenantId(tenantId);
        LocalDateTime now = time.now();
        if (tenantMapper.findByTenantId(normalizedTenantId) == null) {
            throw notFound("Tenant not found");
        }
        List<SaasFeatureEntity> features = featureMapper.findAllOrdered();
        Map<Long, SaasFeatureEntity> featureById = new HashMap<>();
        TreeMap<String, Boolean> effectiveFeatures = new TreeMap<>();
        for (SaasFeatureEntity feature : features) {
            featureById.put(feature.getFeatureId(), feature);
            effectiveFeatures.put(feature.getFeatureKey(), false);
        }
        TreeMap<String, QuotaEntitlement> effectiveQuotas = zeroQuotas();
        SaasSubscriptionEntity subscription = subscriptionMapper.findCurrentByTenantId(normalizedTenantId);
        if (subscription == null) {
            return new EffectiveTenantEntitlements(normalizedTenantId, null, null,
                    effectiveFeatures, effectiveQuotas);
        }
        SaasPlanEntity plan = planMapper.selectById(subscription.getPlanId());
        if (plan == null) {
            throw notFound("Subscription plan not found");
        }
        for (SaasPlanFeatureEntity grant : planFeatureMapper.findByPlanId(plan.getPlanId())) {
            SaasFeatureEntity feature = featureById.get(grant.getFeatureId());
            if (feature != null && feature.getStatus() == FeatureStatus.ACTIVE && Boolean.TRUE.equals(grant.getGranted())) {
                effectiveFeatures.put(feature.getFeatureKey(), true);
            }
        }
        for (SaasPlanQuotaEntity quota : planQuotaMapper.findByPlanId(plan.getPlanId())) {
            if (!QUOTA_KEYS.contains(quota.getQuotaKey())) {
                throw new SaasCatalogException(SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY,
                        "Unknown stored quota key: " + quota.getQuotaKey());
            }
            effectiveQuotas.put(quota.getQuotaKey(), entitlement(quota.getLimitValue()));
        }
        applyFeatureOverrides(normalizedTenantId, now, featureById, effectiveFeatures);
        applyQuotaOverrides(normalizedTenantId, now, effectiveQuotas);
        return new EffectiveTenantEntitlements(normalizedTenantId, subscription.getSubscriptionId(),
                plan.getPlanId(), effectiveFeatures, effectiveQuotas);
    }

    private void applyFeatureOverrides(String tenantId, LocalDateTime now,
            Map<Long, SaasFeatureEntity> featureById, TreeMap<String, Boolean> result) {
        Map<Long, SaasTenantFeatureOverrideEntity> current = new HashMap<>();
        for (SaasTenantFeatureOverrideEntity row : featureOverrideMapper.findByTenantId(tenantId)) {
            if (!effective(row.getEffectiveFrom(), row.getEffectiveUntil(), now)) {
                continue;
            }
            if (current.putIfAbsent(row.getFeatureId(), row) != null) {
                throw duplicateEffective("feature override");
            }
        }
        current.forEach((featureId, row) -> {
            SaasFeatureEntity feature = featureById.get(featureId);
            if (feature != null && feature.getStatus() == FeatureStatus.ACTIVE) {
                result.put(feature.getFeatureKey(), row.getOverrideState() == FeatureOverrideState.GRANT);
            }
        });
    }

    private void applyQuotaOverrides(String tenantId, LocalDateTime now,
            TreeMap<String, QuotaEntitlement> result) {
        Map<String, SaasTenantQuotaOverrideEntity> current = new HashMap<>();
        for (SaasTenantQuotaOverrideEntity row : quotaOverrideMapper.findByTenantId(tenantId)) {
            if (!effective(row.getEffectiveFrom(), row.getEffectiveUntil(), now)) {
                continue;
            }
            if (!QUOTA_KEYS.contains(row.getQuotaKey())) {
                throw new SaasCatalogException(SaasCatalogException.ErrorCode.UNKNOWN_QUOTA_KEY,
                        "Unknown stored quota key: " + row.getQuotaKey());
            }
            if (current.putIfAbsent(row.getQuotaKey(), row) != null) {
                throw duplicateEffective("quota override");
            }
        }
        current.forEach((key, row) -> result.put(key, entitlement(row.getLimitValue())));
    }

    private void lockTenant(String tenantId) {
        if (tenantMapper.lockByTenantId(tenantId) == null) {
            throw notFound("Tenant not found");
        }
    }

    private static void ensureNoOverlap(LocalDateTime newStart, LocalDateTime newEnd, List<Window> windows) {
        for (Window window : windows) {
            boolean startsBeforeExistingEnd = window.end() == null || newStart.isBefore(window.end());
            boolean existingStartsBeforeEnd = newEnd == null || window.start().isBefore(newEnd);
            if (startsBeforeExistingEnd && existingStartsBeforeEnd) {
                throw overlap(null);
            }
        }
    }

    private static boolean effective(LocalDateTime start, LocalDateTime end, LocalDateTime now) {
        return !start.isAfter(now) && (end == null || now.isBefore(end));
    }

    private static void requireFutureOrNow(LocalDateTime start, LocalDateTime now) {
        if (start.isBefore(now)) {
            throw SaasCatalogValidation.invalid("effectiveFrom must not be in the past");
        }
    }

    private static void requireFutureRow(Object value, Long expectedVersion, LocalDateTime now, String message) {
        if (value == null) {
            throw notFound(message);
        }
        LocalDateTime start;
        Long actualVersion;
        if (value instanceof SaasTenantFeatureOverrideEntity row) {
            start = row.getEffectiveFrom(); actualVersion = row.getVersionNo();
        } else {
            SaasTenantQuotaOverrideEntity row = (SaasTenantQuotaOverrideEntity) value;
            start = row.getEffectiveFrom(); actualVersion = row.getVersionNo();
        }
        if (!Objects.equals(actualVersion, expectedVersion)) {
            throw versionConflict();
        }
        if (!start.isAfter(now)) {
            throw SaasCatalogValidation.invalid("Only future overrides can be deleted");
        }
    }

    private static void requiredIdAndVersion(Long id, Long version) {
        if (id == null || id <= 0 || version == null || version < 0) {
            throw SaasCatalogValidation.invalid("overrideId and expectedVersion are invalid");
        }
    }

    private static TreeMap<String, QuotaEntitlement> zeroQuotas() {
        TreeMap<String, QuotaEntitlement> quotas = new TreeMap<>();
        QUOTA_KEYS.forEach(key -> quotas.put(key, new QuotaEntitlement(false, 0)));
        return quotas;
    }

    private static QuotaEntitlement entitlement(Long limit) {
        return limit == null ? new QuotaEntitlement(true, 0) : new QuotaEntitlement(false, limit);
    }

    private static void audit(SaasTenantFeatureOverrideEntity row, String actor, LocalDateTime now) {
        row.setCreateBy(actor); row.setCreateTime(now); row.setUpdateBy(actor); row.setUpdateTime(now);
        row.setVersionNo(0L);
    }

    private static void audit(SaasTenantQuotaOverrideEntity row, String actor, LocalDateTime now) {
        row.setCreateBy(actor); row.setCreateTime(now); row.setUpdateBy(actor); row.setUpdateTime(now);
        row.setVersionNo(0L);
    }

    private static void cas(int affected) {
        if (affected != 1) {
            throw versionConflict();
        }
    }

    private static SaasCatalogException overlap(Throwable cause) {
        return cause == null
                ? new SaasCatalogException(SaasCatalogException.ErrorCode.OVERLAPPING_OVERRIDE,
                        "Override windows overlap")
                : new SaasCatalogException(SaasCatalogException.ErrorCode.OVERLAPPING_OVERRIDE,
                        "Override windows overlap", cause);
    }

    private static SaasCatalogException duplicateEffective(String label) {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.DUPLICATE,
                "More than one effective " + label);
    }

    private static SaasCatalogException notFound(String message) {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.NOT_FOUND, message);
    }

    private static SaasCatalogException versionConflict() {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                "The expected version no longer matches");
    }

    private record Window(LocalDateTime start, LocalDateTime end) { }
}
