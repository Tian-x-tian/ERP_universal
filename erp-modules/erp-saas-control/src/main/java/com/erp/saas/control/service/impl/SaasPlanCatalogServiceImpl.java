package com.erp.saas.control.service.impl;

import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.entity.SaasFeatureEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasPlanFeatureEntity;
import com.erp.saas.control.domain.entity.SaasPlanQuotaEntity;
import com.erp.saas.control.mapper.SaasFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasPlanQuotaMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.SaasCatalogException;
import com.erp.saas.control.service.SaasCatalogValidation;
import com.erp.saas.control.service.SaasPlanCatalogService;
import com.erp.saas.control.service.model.FeatureDefinitionCommand;
import com.erp.saas.control.service.model.PlanDraftCommand;
import com.erp.saas.control.service.model.PlanFeatureGrantCommand;
import com.erp.saas.control.service.model.PlanQuotaCommand;
import com.erp.saas.control.service.model.PublishPlanCommand;
import com.erp.saas.control.service.model.SaasFeatureView;
import com.erp.saas.control.service.model.SaasPlanView;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class SaasPlanCatalogServiceImpl implements SaasPlanCatalogService {
    private final SaasPlanMapper planMapper;
    private final SaasFeatureMapper featureMapper;
    private final SaasPlanFeatureMapper planFeatureMapper;
    private final SaasPlanQuotaMapper planQuotaMapper;
    private final ControlUtcTime time;

    public SaasPlanCatalogServiceImpl(SaasPlanMapper planMapper, SaasFeatureMapper featureMapper,
            SaasPlanFeatureMapper planFeatureMapper, SaasPlanQuotaMapper planQuotaMapper, ControlUtcTime time) {
        this.planMapper = planMapper;
        this.featureMapper = featureMapper;
        this.planFeatureMapper = planFeatureMapper;
        this.planQuotaMapper = planQuotaMapper;
        this.time = time;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasPlanView createDraft(PlanDraftCommand command, String operator) {
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        List<SaasPlanEntity> family = planMapper.findFamilyForUpdate(command.planCode());
        ensureUniqueVersion(family, null, command.planCode(), command.planVersion());
        SaasPlanEntity entity = draft(command);
        auditCreate(entity, actor, now);
        try {
            planMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw duplicate("Plan code/version already exists", exception);
        }
        return view(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasPlanView updateDraft(Long planId, Long expectedVersion, PlanDraftCommand command, String operator) {
        requiredPositive(planId, "planId");
        requiredVersion(expectedVersion);
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        SaasPlanEntity hint = requirePlan(planMapper.selectById(planId));
        List<SaasPlanEntity> family = lockFamilies(hint.getPlanCode(), command.planCode());
        SaasPlanEntity current = family.stream().filter(plan -> planId.equals(plan.getPlanId()))
                .findFirst().orElseThrow(() -> notFound("Plan not found"));
        requireDraft(current);
        requireVersion(current.getVersionNo(), expectedVersion);
        ensureUniqueVersion(family, planId, command.planCode(), command.planVersion());
        apply(current, command);
        try {
            cas(planMapper.updateDraft(current, expectedVersion, actor, now));
        } catch (DuplicateKeyException exception) {
            throw duplicate("Plan code/version already exists", exception);
        }
        current.setVersionNo(expectedVersion + 1);
        return view(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasFeatureView defineFeature(FeatureDefinitionCommand command, String operator) {
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        if (featureMapper.findByKeyForUpdate(command.featureKey()) != null) {
            throw duplicate("Feature key already exists", null);
        }
        SaasFeatureEntity entity = new SaasFeatureEntity();
        apply(entity, command);
        entity.setVersionNo(0L);
        auditCreate(entity, actor, now);
        try {
            featureMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw duplicate("Feature key already exists", exception);
        }
        return view(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasFeatureView updateFeature(Long featureId, Long expectedVersion,
            FeatureDefinitionCommand command, String operator) {
        requiredPositive(featureId, "featureId");
        requiredVersion(expectedVersion);
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        SaasFeatureEntity hint = featureMapper.selectById(featureId);
        if (hint == null) {
            throw notFound("Feature not found");
        }
        Set<String> keys = new java.util.TreeSet<>(List.of(hint.getFeatureKey(), command.featureKey()));
        SaasFeatureEntity current = null;
        SaasFeatureEntity sameKey = null;
        for (String key : keys) {
            SaasFeatureEntity locked = featureMapper.findByKeyForUpdate(key);
            if (locked != null && featureId.equals(locked.getFeatureId())) {
                current = locked;
            }
            if (locked != null && command.featureKey().equals(locked.getFeatureKey())) {
                sameKey = locked;
            }
        }
        if (current == null) {
            throw notFound("Feature not found");
        }
        requireVersion(current.getVersionNo(), expectedVersion);
        if (sameKey != null && !featureId.equals(sameKey.getFeatureId())) {
            throw duplicate("Feature key already exists", null);
        }
        apply(current, command);
        try {
            cas(featureMapper.updateVersioned(current, expectedVersion, actor, now));
        } catch (DuplicateKeyException exception) {
            throw duplicate("Feature key already exists", exception);
        }
        current.setVersionNo(expectedVersion + 1);
        return view(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasPlanView replaceDraftFeatures(Long planId, Long expectedVersion,
            List<PlanFeatureGrantCommand> grants, String operator) {
        requiredPositive(planId, "planId");
        requiredVersion(expectedVersion);
        grants = SaasCatalogValidation.required(grants, "grants");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        ensureUnique(grants.stream().map(PlanFeatureGrantCommand::featureKey).toList(), "feature key");
        SaasPlanEntity plan = lockedDraft(planId, expectedVersion);
        List<SaasFeatureEntity> features = grants.stream().map(grant -> {
            SaasFeatureEntity feature = featureMapper.findByKeyForUpdate(grant.featureKey());
            if (feature == null) {
                throw new SaasCatalogException(SaasCatalogException.ErrorCode.UNKNOWN_FEATURE_KEY,
                        "Unknown feature key: " + grant.featureKey());
            }
            return feature;
        }).toList();
        cas(planMapper.bumpDraft(planId, expectedVersion, actor, now));
        planFeatureMapper.deleteByPlanId(planId);
        for (int index = 0; index < grants.size(); index++) {
            SaasPlanFeatureEntity row = new SaasPlanFeatureEntity();
            row.setPlanId(planId);
            row.setFeatureId(features.get(index).getFeatureId());
            row.setGranted(grants.get(index).granted());
            row.setCreateBy(actor);
            row.setCreateTime(now);
            row.setUpdateBy(actor);
            row.setUpdateTime(now);
            planFeatureMapper.insert(row);
        }
        plan.setVersionNo(expectedVersion + 1);
        return view(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasPlanView replaceDraftQuotas(Long planId, Long expectedVersion,
            List<PlanQuotaCommand> quotas, String operator) {
        requiredPositive(planId, "planId");
        requiredVersion(expectedVersion);
        quotas = SaasCatalogValidation.required(quotas, "quotas");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        ensureUnique(quotas.stream().map(PlanQuotaCommand::quotaKey).toList(), "quota key");
        SaasPlanEntity plan = lockedDraft(planId, expectedVersion);
        cas(planMapper.bumpDraft(planId, expectedVersion, actor, now));
        planQuotaMapper.deleteByPlanId(planId);
        for (PlanQuotaCommand quota : quotas) {
            SaasPlanQuotaEntity row = new SaasPlanQuotaEntity();
            row.setPlanId(planId);
            row.setQuotaKey(quota.quotaKey());
            row.setLimitValue(quota.limitValue());
            row.setPeriodType(quota.periodType());
            row.setCreateBy(actor);
            row.setCreateTime(now);
            row.setUpdateBy(actor);
            row.setUpdateTime(now);
            planQuotaMapper.insert(row);
        }
        plan.setVersionNo(expectedVersion + 1);
        return view(plan);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasPlanView publish(PublishPlanCommand command, String operator) {
        command = SaasCatalogValidation.required(command, "command");
        String actor = time.operator(operator);
        LocalDateTime now = time.now();
        Long targetPlanId = command.planId();
        SaasPlanEntity initial = requirePlan(planMapper.selectById(targetPlanId));
        List<SaasPlanEntity> family = planMapper.findFamilyForUpdate(initial.getPlanCode());
        SaasPlanEntity target = family.stream().filter(plan -> targetPlanId.equals(plan.getPlanId()))
                .findFirst().orElseThrow(() -> notFound("Plan not found"));
        requireVersion(target.getVersionNo(), command.expectedPlanVersion());
        SaasPlanEntity active = family.stream().filter(plan -> plan.getStatus() == PlanStatus.ACTIVE)
                .findFirst().orElse(null);
        requireActiveGeneration(active, command);
        if (target.getStatus() == PlanStatus.ACTIVE) {
            return view(target);
        }
        if (target.getStatus() != PlanStatus.DRAFT) {
            throw immutablePlan();
        }
        if (active != null) {
            cas(planMapper.retire(active.getPlanId(), active.getVersionNo(), actor, now));
        }
        try {
            cas(planMapper.activate(target.getPlanId(), target.getVersionNo(), actor, now));
        } catch (DuplicateKeyException exception) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                    "The active plan generation changed", exception);
        }
        target.setStatus(PlanStatus.ACTIVE);
        target.setVersionNo(target.getVersionNo() + 1);
        return view(target);
    }

    private SaasPlanEntity lockedDraft(Long planId, Long expectedVersion) {
        SaasPlanEntity plan = requirePlan(planMapper.findByIdForUpdate(planId));
        requireDraft(plan);
        requireVersion(plan.getVersionNo(), expectedVersion);
        return plan;
    }

    private List<SaasPlanEntity> lockFamilies(String firstCode, String secondCode) {
        Set<String> codes = new java.util.TreeSet<>(List.of(firstCode, secondCode));
        List<SaasPlanEntity> result = new java.util.ArrayList<>();
        for (String code : codes) {
            result.addAll(planMapper.findFamilyForUpdate(code));
        }
        return result;
    }

    private static void requireActiveGeneration(SaasPlanEntity active, PublishPlanCommand command) {
        if (active == null) {
            if (command.expectedActivePlanId() != null) {
                throw versionConflict();
            }
            return;
        }
        if (!active.getPlanId().equals(command.expectedActivePlanId())
                || !active.getVersionNo().equals(command.expectedActivePlanVersion())) {
            throw versionConflict();
        }
    }

    private static void ensureUniqueVersion(List<SaasPlanEntity> family, Long excludedId,
            String planCode, Integer version) {
        if (family.stream().anyMatch(plan -> plan.getPlanCode().equals(planCode)
                && plan.getPlanVersion().equals(version)
                && !plan.getPlanId().equals(excludedId))) {
            throw duplicate("Plan code/version already exists", null);
        }
    }

    private static void ensureUnique(List<String> keys, String label) {
        Set<String> unique = new HashSet<>();
        for (String key : keys) {
            if (!unique.add(key)) {
                throw duplicate("Duplicate " + label + ": " + key, null);
            }
        }
    }

    private static SaasPlanEntity draft(PlanDraftCommand command) {
        SaasPlanEntity entity = new SaasPlanEntity();
        apply(entity, command);
        entity.setStatus(PlanStatus.DRAFT);
        entity.setVersionNo(0L);
        return entity;
    }

    private static void apply(SaasPlanEntity entity, PlanDraftCommand command) {
        entity.setPlanCode(command.planCode());
        entity.setPlanVersion(command.planVersion());
        entity.setPlanName(command.planName());
        entity.setTrialDays(command.trialDays());
        entity.setGraceDays(command.graceDays());
        entity.setDescription(command.description());
    }

    private static void apply(SaasFeatureEntity entity, FeatureDefinitionCommand command) {
        entity.setFeatureKey(command.featureKey());
        entity.setFeatureName(command.featureName());
        entity.setStatus(command.status());
        entity.setDescription(command.description());
    }

    private static void auditCreate(SaasPlanEntity entity, String actor, LocalDateTime now) {
        entity.setCreateBy(actor);
        entity.setCreateTime(now);
        entity.setUpdateBy(actor);
        entity.setUpdateTime(now);
    }

    private static void auditCreate(SaasFeatureEntity entity, String actor, LocalDateTime now) {
        entity.setCreateBy(actor);
        entity.setCreateTime(now);
        entity.setUpdateBy(actor);
        entity.setUpdateTime(now);
    }

    private static SaasPlanView view(SaasPlanEntity entity) {
        return new SaasPlanView(entity.getPlanId(), entity.getPlanCode(), entity.getPlanVersion(),
                entity.getPlanName(), entity.getStatus(), entity.getTrialDays(), entity.getGraceDays(),
                entity.getDescription(), entity.getVersionNo());
    }

    private static SaasFeatureView view(SaasFeatureEntity entity) {
        return new SaasFeatureView(entity.getFeatureId(), entity.getFeatureKey(), entity.getFeatureName(),
                entity.getStatus(), entity.getDescription(), entity.getVersionNo());
    }

    private static SaasPlanEntity requirePlan(SaasPlanEntity entity) {
        if (entity == null) {
            throw notFound("Plan not found");
        }
        return entity;
    }

    private static void requireDraft(SaasPlanEntity entity) {
        if (entity.getStatus() != PlanStatus.DRAFT) {
            throw immutablePlan();
        }
    }

    private static void requireVersion(Long actual, Long expected) {
        if (!Objects.equals(actual, expected)) {
            throw versionConflict();
        }
    }

    private static void requiredPositive(Long value, String field) {
        if (value == null || value <= 0) {
            throw SaasCatalogValidation.invalid(field + " must be positive");
        }
    }

    private static void requiredVersion(Long value) {
        if (value == null || value < 0) {
            throw SaasCatalogValidation.invalid("expectedVersion must not be negative");
        }
    }

    private static void cas(int affected) {
        if (affected != 1) {
            throw versionConflict();
        }
    }

    private static SaasCatalogException notFound(String message) {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.NOT_FOUND, message);
    }

    private static SaasCatalogException duplicate(String message, Throwable cause) {
        return cause == null
                ? new SaasCatalogException(SaasCatalogException.ErrorCode.DUPLICATE, message)
                : new SaasCatalogException(SaasCatalogException.ErrorCode.DUPLICATE, message, cause);
    }

    private static SaasCatalogException immutablePlan() {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.IMMUTABLE_PUBLISHED_PLAN,
                "Published and retired plans are immutable");
    }

    private static SaasCatalogException versionConflict() {
        return new SaasCatalogException(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                "The expected version no longer matches");
    }
}
