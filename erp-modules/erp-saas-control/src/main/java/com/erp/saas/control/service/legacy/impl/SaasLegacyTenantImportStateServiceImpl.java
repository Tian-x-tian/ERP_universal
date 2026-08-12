package com.erp.saas.control.service.legacy.impl;

import com.erp.platform.contract.model.PlatformTenantView;
import com.erp.saas.contract.model.DeploymentMode;
import com.erp.saas.contract.model.SubscriptionState;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.SaasCatalogException;
import com.erp.saas.control.service.SaasCatalogValidation;
import com.erp.saas.control.service.legacy.SaasLegacyTenantImportStateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Objects;

@Service
public class SaasLegacyTenantImportStateServiceImpl implements SaasLegacyTenantImportStateService {
    public static final String LEGACY_PLAN_CODE = "legacy-full-access";

    private final SaasTenantMapper tenantMapper;
    private final SaasPlanMapper planMapper;
    private final SaasSubscriptionMapper subscriptionMapper;
    private final SaasDeploymentMapper deploymentMapper;
    private final ControlUtcTime time;
    private final String sharedDeploymentRef;

    public SaasLegacyTenantImportStateServiceImpl(SaasTenantMapper tenantMapper, SaasPlanMapper planMapper,
            SaasSubscriptionMapper subscriptionMapper, SaasDeploymentMapper deploymentMapper,
            ControlUtcTime time,
            @Value("${erp.saas.shared-deployment-ref:http://erp-system}") String sharedDeploymentRef) {
        this.tenantMapper = Objects.requireNonNull(tenantMapper);
        this.planMapper = Objects.requireNonNull(planMapper);
        this.subscriptionMapper = Objects.requireNonNull(subscriptionMapper);
        this.deploymentMapper = Objects.requireNonNull(deploymentMapper);
        this.time = Objects.requireNonNull(time);
        this.sharedDeploymentRef = deploymentRef(sharedDeploymentRef);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean importTenant(PlatformTenantView source, String operator) {
        if (source == null) throw SaasCatalogValidation.invalid("Legacy tenant is required");
        String tenantId = SaasCatalogValidation.tenantId(source.getTenantId());
        String tenantName = SaasCatalogValidation.name(source.getTenantName(), "tenantName");
        if (!"0".equals(source.getStatus()) || !"0".equals(source.getDelFlag())) {
            throw SaasCatalogValidation.invalid("Only active legacy tenants can be imported");
        }
        String actor = time.operator(operator);
        if (tenantMapper.lockByTenantId(tenantId) != null) return false;
        String slug = "legacy-" + tenantId.toLowerCase(Locale.ROOT).replace('_', '-');
        if (tenantMapper.findBySlugForUpdate(slug) != null) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.DUPLICATE,
                    "Legacy tenant slug is already registered");
        }
        SaasPlanEntity plan = planMapper.findActiveByCode(LEGACY_PLAN_CODE);
        if (plan == null || plan.getStatus() != PlanStatus.ACTIVE) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.NOT_FOUND,
                    "Legacy Full Access plan is not installed");
        }
        LocalDateTime now = time.now();
        insert(tenantMapper.insert(tenant(tenantId, slug, tenantName, actor, now)), "legacy tenant");
        insert(subscriptionMapper.insert(subscription(tenantId, plan.getPlanId(), actor, now)),
                "legacy subscription");
        insert(deploymentMapper.insert(deployment(tenantId, actor, now)), "legacy deployment");
        return true;
    }

    private SaasTenantEntity tenant(String tenantId, String slug, String name,
            String operator, LocalDateTime now) {
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId(tenantId);
        tenant.setSlug(slug);
        tenant.setTenantName(name);
        tenant.setLifecycleState(TenantLifecycleState.ACTIVE);
        audit(tenant, operator, now);
        return tenant;
    }

    private SaasSubscriptionEntity subscription(String tenantId, Long planId,
            String operator, LocalDateTime now) {
        SaasSubscriptionEntity subscription = new SaasSubscriptionEntity();
        subscription.setTenantId(tenantId);
        subscription.setPlanId(planId);
        subscription.setState(SubscriptionState.ACTIVE);
        subscription.setStartAt(now);
        subscription.setNonExpiring(true);
        subscription.setCreateBy(operator);
        subscription.setCreateTime(now);
        subscription.setUpdateBy(operator);
        subscription.setUpdateTime(now);
        subscription.setVersionNo(0L);
        return subscription;
    }

    private SaasDeploymentEntity deployment(String tenantId, String operator, LocalDateTime now) {
        SaasDeploymentEntity deployment = new SaasDeploymentEntity();
        deployment.setTenantId(tenantId);
        deployment.setMode(DeploymentMode.SHARED);
        deployment.setStatus(DeploymentStatus.REGISTERED);
        deployment.setDeploymentRef(sharedDeploymentRef);
        deployment.setCreateBy(operator);
        deployment.setCreateTime(now);
        deployment.setUpdateBy(operator);
        deployment.setUpdateTime(now);
        deployment.setVersionNo(0L);
        return deployment;
    }

    private void audit(SaasTenantEntity tenant, String operator, LocalDateTime now) {
        tenant.setCreateBy(operator);
        tenant.setCreateTime(now);
        tenant.setUpdateBy(operator);
        tenant.setUpdateTime(now);
        tenant.setVersionNo(0L);
    }

    private String deploymentRef(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.trim());
            if (uri.getHost() == null || uri.getRawUserInfo() != null || uri.getRawQuery() != null
                    || uri.getRawFragment() != null || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
            return uri.toString();
        } catch (IllegalArgumentException error) {
            throw new IllegalArgumentException("erp.saas.shared-deployment-ref must be a safe HTTP base URL", error);
        }
    }

    private void insert(int affected, String target) {
        if (affected != 1) {
            throw new SaasCatalogException(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                    "Failed to create " + target);
        }
    }
}
