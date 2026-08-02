package com.erp.saas.control.service.management.impl;

import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasDomainEntity;
import com.erp.saas.control.domain.entity.SaasFeatureEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasPlanFeatureEntity;
import com.erp.saas.control.domain.entity.SaasPlanQuotaEntity;
import com.erp.saas.control.domain.entity.SaasProvisioningTaskEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.domain.entity.SaasUsageSummaryEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasDomainMapper;
import com.erp.saas.control.mapper.SaasFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasPlanQuotaMapper;
import com.erp.saas.control.mapper.SaasProvisioningTaskMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.mapper.SaasUsageSummaryMapper;
import com.erp.saas.control.service.management.SaasManagementQueryService;
import com.erp.saas.control.service.SaasCatalogException;
import com.erp.saas.control.service.management.model.SaasDeploymentManagementView;
import com.erp.saas.control.service.management.model.SaasDomainManagementView;
import com.erp.saas.control.service.management.model.SaasFeatureManagementView;
import com.erp.saas.control.service.management.model.SaasPlanCatalogDetailView;
import com.erp.saas.control.service.management.model.SaasPlanFeatureManagementView;
import com.erp.saas.control.service.management.model.SaasPlanManagementView;
import com.erp.saas.control.service.management.model.SaasPlanQuotaManagementView;
import com.erp.saas.control.service.management.model.SaasTenantManagementView;
import com.erp.saas.control.service.management.model.SaasUsageManagementView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class SaasManagementQueryServiceImpl implements SaasManagementQueryService {
    private final SaasTenantMapper tenantMapper;
    private final SaasSubscriptionMapper subscriptionMapper;
    private final SaasPlanMapper planMapper;
    private final SaasFeatureMapper featureMapper;
    private final SaasPlanFeatureMapper planFeatureMapper;
    private final SaasPlanQuotaMapper planQuotaMapper;
    private final SaasDeploymentMapper deploymentMapper;
    private final SaasProvisioningTaskMapper taskMapper;
    private final SaasDomainMapper domainMapper;
    private final SaasUsageSummaryMapper usageSummaryMapper;

    public SaasManagementQueryServiceImpl(SaasTenantMapper tenantMapper,
            SaasSubscriptionMapper subscriptionMapper, SaasPlanMapper planMapper,
            SaasFeatureMapper featureMapper, SaasPlanFeatureMapper planFeatureMapper,
            SaasPlanQuotaMapper planQuotaMapper,
            SaasDeploymentMapper deploymentMapper, SaasProvisioningTaskMapper taskMapper,
            SaasDomainMapper domainMapper, SaasUsageSummaryMapper usageSummaryMapper) {
        this.tenantMapper = tenantMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.planMapper = planMapper;
        this.featureMapper = featureMapper;
        this.planFeatureMapper = planFeatureMapper;
        this.planQuotaMapper = planQuotaMapper;
        this.deploymentMapper = deploymentMapper;
        this.taskMapper = taskMapper;
        this.domainMapper = domainMapper;
        this.usageSummaryMapper = usageSummaryMapper;
    }

    @Override
    public List<SaasTenantManagementView> listTenants() {
        return tenantMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SaasTenantEntity::getTenantId))
                .map(this::tenantView)
                .toList();
    }

    @Override
    public List<SaasPlanManagementView> listPlans() {
        return planMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SaasPlanEntity::getPlanCode)
                        .thenComparing(SaasPlanEntity::getPlanVersion))
                .map(this::planView)
                .toList();
    }

    @Override
    public List<SaasFeatureManagementView> listFeatures() {
        return featureMapper.findAllOrdered().stream()
                .map(feature -> new SaasFeatureManagementView(feature.getFeatureId(),
                        feature.getFeatureKey(), feature.getFeatureName(), feature.getStatus(),
                        feature.getDescription(), feature.getVersionNo()))
                .toList();
    }

    @Override
    public SaasPlanCatalogDetailView getPlan(Long planId) {
        SaasPlanEntity plan = planMapper.selectById(planId);
        if (plan == null) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.NOT_FOUND, "Plan not found");
        }
        java.util.Map<Long, SaasFeatureEntity> featuresById = featureMapper.findAllOrdered().stream()
                .collect(java.util.stream.Collectors.toMap(SaasFeatureEntity::getFeatureId,
                        feature -> feature));
        List<SaasPlanFeatureManagementView> features = planFeatureMapper.findByPlanId(planId).stream()
                .map(grant -> planFeatureView(grant, featuresById))
                .toList();
        List<SaasPlanQuotaManagementView> quotas = planQuotaMapper.findByPlanId(planId).stream()
                .map(this::planQuotaView)
                .toList();
        return new SaasPlanCatalogDetailView(planView(plan), features, quotas);
    }

    @Override
    public List<SaasDomainManagementView> listDomains() {
        return domainMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SaasDomainEntity::getHost))
                .map(domain -> new SaasDomainManagementView(domain.getDomainId(), domain.getTenantId(),
                        domain.getHost(), domain.getVerificationState(), domain.getVerificationMethod(),
                        domain.getVerifiedAt(), domain.getRevokedAt(), domain.getVersionNo(),
                        domain.getUpdateTime()))
                .toList();
    }

    @Override
    public List<SaasDeploymentManagementView> listDeployments() {
        return deploymentMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SaasDeploymentEntity::getTenantId))
                .map(deployment -> new SaasDeploymentManagementView(deployment.getDeploymentId(),
                        deployment.getTenantId(), deployment.getMode(), deployment.getStatus(),
                        deployment.getDeploymentRef(), deployment.getSecretRef(), deployment.getVersionNo(),
                        deployment.getUpdateTime()))
                .toList();
    }

    @Override
    public List<SaasUsageManagementView> listUsage() {
        return usageSummaryMapper.selectList(null).stream()
                .sorted(Comparator.comparing(SaasUsageSummaryEntity::getTenantId)
                        .thenComparing(SaasUsageSummaryEntity::getMetricKey)
                        .thenComparing(SaasUsageSummaryEntity::getPeriodStart))
                .map(usage -> new SaasUsageManagementView(usage.getUsageSummaryId(), usage.getTenantId(),
                        usage.getMetricKey(), usage.getPeriodStart(), usage.getUsedAmount(),
                        usage.getLastOccurredAt(), usage.getVersionNo(), usage.getUpdateTime()))
                .toList();
    }

    private SaasTenantManagementView tenantView(SaasTenantEntity tenant) {
        SaasSubscriptionEntity subscription = subscriptionMapper.findLatestByTenantId(tenant.getTenantId());
        SaasPlanEntity plan = subscription == null ? null : planMapper.selectById(subscription.getPlanId());
        SaasDeploymentEntity deployment = deploymentMapper.findByTenantId(tenant.getTenantId());
        SaasProvisioningTaskEntity task = taskMapper.findByTenantId(tenant.getTenantId());
        return new SaasTenantManagementView(tenant.getTenantId(), tenant.getSlug(), tenant.getTenantName(),
                tenant.getLifecycleState(), plan == null ? null : plan.getPlanId(),
                plan == null ? null : plan.getPlanCode(), plan == null ? null : plan.getPlanName(),
                subscription == null ? null : subscription.getState(),
                subscription == null ? null : subscription.getEndAt(),
                subscription == null ? null : subscription.getGraceEndAt(),
                subscription == null ? null : subscription.getNonExpiring(),
                deployment == null ? null : deployment.getMode(),
                deployment == null ? null : deployment.getStatus(),
                task == null ? null : task.getStatus(), task == null ? null : task.getAttemptCount(),
                tenant.getVersionNo(), tenant.getUpdateTime());
    }

    private SaasPlanManagementView planView(SaasPlanEntity plan) {
        return new SaasPlanManagementView(plan.getPlanId(), plan.getPlanCode(),
                plan.getPlanVersion(), plan.getPlanName(), plan.getStatus(), plan.getTrialDays(),
                plan.getGraceDays(), plan.getDescription(), plan.getVersionNo(), plan.getUpdateTime());
    }

    private SaasPlanFeatureManagementView planFeatureView(SaasPlanFeatureEntity grant,
            java.util.Map<Long, SaasFeatureEntity> featuresById) {
        SaasFeatureEntity feature = featuresById.get(grant.getFeatureId());
        if (feature == null) {
            throw new IllegalStateException("Plan references an unknown feature id");
        }
        return new SaasPlanFeatureManagementView(feature.getFeatureKey(), grant.getGranted());
    }

    private SaasPlanQuotaManagementView planQuotaView(SaasPlanQuotaEntity quota) {
        return new SaasPlanQuotaManagementView(quota.getQuotaKey(), quota.getLimitValue(),
                quota.getPeriodType());
    }
}
