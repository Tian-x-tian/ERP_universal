package com.erp.saas.control.service.snapshot.impl;

import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasEntitlementSnapshotSignatureUtils;
import com.erp.saas.contract.model.SaasFeatureGrant;
import com.erp.saas.contract.model.SaasQuotaLimit;
import com.erp.saas.control.domain.DeploymentStatus;
import com.erp.saas.control.domain.entity.SaasDeploymentEntity;
import com.erp.saas.control.domain.entity.SaasEntitlementSnapshotEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasSubscriptionEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDeploymentMapper;
import com.erp.saas.control.mapper.SaasEntitlementSnapshotMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.SaasTenantEntitlementService;
import com.erp.saas.control.service.model.EffectiveTenantEntitlements;
import com.erp.saas.control.service.snapshot.SaasEntitlementSnapshotService;
import com.erp.saas.control.service.snapshot.SaasSnapshotException;
import com.erp.saas.control.service.snapshot.SaasSnapshotSigningProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class SaasEntitlementSnapshotServiceImpl implements SaasEntitlementSnapshotService {
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private final SaasTenantMapper tenantMapper;
    private final SaasDeploymentMapper deploymentMapper;
    private final SaasSubscriptionMapper subscriptionMapper;
    private final SaasPlanMapper planMapper;
    private final SaasEntitlementSnapshotMapper snapshotMapper;
    private final SaasTenantEntitlementService entitlementService;
    private final ObjectMapper objectMapper;
    private final ControlUtcTime time;
    private final String keyId;
    private final byte[] secret;
    private final Duration validity;
    private final Duration renewBefore;

    public SaasEntitlementSnapshotServiceImpl(SaasTenantMapper tenantMapper,
            SaasDeploymentMapper deploymentMapper, SaasSubscriptionMapper subscriptionMapper,
            SaasPlanMapper planMapper, SaasEntitlementSnapshotMapper snapshotMapper,
            SaasTenantEntitlementService entitlementService, ObjectMapper objectMapper,
            SaasSnapshotSigningProperties properties, ControlUtcTime time) {
        this.tenantMapper = Objects.requireNonNull(tenantMapper);
        this.deploymentMapper = Objects.requireNonNull(deploymentMapper);
        this.subscriptionMapper = Objects.requireNonNull(subscriptionMapper);
        this.planMapper = Objects.requireNonNull(planMapper);
        this.snapshotMapper = Objects.requireNonNull(snapshotMapper);
        this.entitlementService = Objects.requireNonNull(entitlementService);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.time = Objects.requireNonNull(time);
        this.keyId = requireKeyId(properties == null ? null : properties.getKeyId());
        this.secret = requireSecret(properties == null ? null : properties.getSecret());
        this.validity = requireValidity(properties == null ? null : properties.getValidity());
        this.renewBefore = requireRenewBefore(properties == null ? null : properties.getRenewBefore(), validity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasEntitlementSnapshot load(String tenantId, String operator) {
        String normalizedTenantId = tenantId(tenantId);
        String actor = operator(operator);
        LocalDateTime now = time.now();
        SaasTenantEntity tenant = tenantMapper.lockByTenantId(normalizedTenantId);
        if (tenant == null) {
            throw new SaasSnapshotException(SaasSnapshotException.ErrorCode.TENANT_NOT_FOUND,
                    "Tenant not found");
        }
        SaasDeploymentEntity deployment = deploymentMapper.findByTenantId(normalizedTenantId);
        if (deployment == null || deployment.getMode() == null || deployment.getStatus() == null
                || deployment.getStatus() == DeploymentStatus.DISABLED) {
            throw new SaasSnapshotException(SaasSnapshotException.ErrorCode.DEPLOYMENT_NOT_ELIGIBLE,
                    "Tenant has no eligible deployment");
        }
        EffectiveTenantEntitlements entitlements = entitlementService.effectiveEntitlements(normalizedTenantId);
        SaasSubscriptionEntity subscription = subscriptionMapper.findLatestByTenantId(normalizedTenantId);
        SaasPlanEntity plan = subscription == null ? null : planMapper.selectById(subscription.getPlanId());
        if (subscription != null && plan == null) {
            throw corrupted("Subscription plan is missing", null);
        }
        SaasEntitlementSnapshot logical = logicalSnapshot(tenant, deployment, subscription, plan, entitlements);
        String payloadHash = SaasEntitlementSnapshotSignatureUtils.contentDigest(logical);
        SaasEntitlementSnapshotEntity current = snapshotMapper.findForUpdate(normalizedTenantId);
        if (reusable(current, payloadHash, now)) {
            return verifiedStored(current);
        }
        long nextVersion = current == null ? 1L : nextVersion(current.getSnapshotVersion());
        SaasEntitlementSnapshot signed = sign(logical, nextVersion, now);
        SaasEntitlementSnapshotEntity replacement = entity(signed, payloadHash, actor, now, current);
        if (current == null) {
            insert(replacement);
        } else {
            if (snapshotMapper.updateVersioned(replacement, current.getVersionNo(), actor, now) != 1) {
                throw versionConflict();
            }
        }
        return signed;
    }

    private SaasEntitlementSnapshot logicalSnapshot(SaasTenantEntity tenant, SaasDeploymentEntity deployment,
            SaasSubscriptionEntity subscription, SaasPlanEntity plan,
            EffectiveTenantEntitlements entitlements) {
        SaasEntitlementSnapshot snapshot = new SaasEntitlementSnapshot();
        snapshot.setTenantId(tenant.getTenantId());
        snapshot.setLifecycleState(tenant.getLifecycleState());
        snapshot.setDeploymentMode(deployment.getMode());
        snapshot.setSubscriptionState(subscription == null ? null : subscription.getState());
        snapshot.setPlanCode(plan == null ? null : plan.getPlanCode());
        snapshot.setFeatureGrants(entitlements.features().entrySet().stream()
                .map(entry -> new SaasFeatureGrant(entry.getKey(), entry.getValue())).toList());
        snapshot.setQuotaLimits(entitlements.quotas().entrySet().stream()
                .map(entry -> new SaasQuotaLimit(entry.getKey(),
                        entry.getValue().unlimited() ? null : entry.getValue().limitValue())).toList());
        return snapshot;
    }

    private SaasEntitlementSnapshot sign(SaasEntitlementSnapshot snapshot, long version, LocalDateTime now) {
        LocalDateTime expiresAt;
        try {
            expiresAt = now.plus(validity);
        } catch (DateTimeException exception) {
            throw new IllegalStateException("Snapshot validity is outside the supported time range", exception);
        }
        snapshot.setVersion(version);
        snapshot.setIssuedAtEpochMs(now.toInstant(ZoneOffset.UTC).toEpochMilli());
        snapshot.setExpiresAtEpochMs(expiresAt.toInstant(ZoneOffset.UTC).toEpochMilli());
        snapshot.setSignatureKeyId(keyId);
        snapshot.setSignature(SaasEntitlementSnapshotSignatureUtils.sign(secret, snapshot));
        return snapshot;
    }

    private boolean reusable(SaasEntitlementSnapshotEntity current, String payloadHash, LocalDateTime now) {
        if (current == null || !payloadHash.equals(current.getPayloadHash())
                || !keyId.equals(current.getSignatureKeyId()) || current.getExpiresAt() == null) {
            return false;
        }
        return current.getExpiresAt().isAfter(now.plus(renewBefore));
    }

    private SaasEntitlementSnapshot verifiedStored(SaasEntitlementSnapshotEntity row) {
        final SaasEntitlementSnapshot snapshot;
        try {
            snapshot = objectMapper.readValue(row.getSnapshotJson(), SaasEntitlementSnapshot.class);
        } catch (JsonProcessingException exception) {
            throw corrupted("Stored entitlement snapshot is not valid JSON", exception);
        }
        LocalDateTime issuedAt = epoch(snapshot.getIssuedAtEpochMs());
        LocalDateTime expiresAt = epoch(snapshot.getExpiresAtEpochMs());
        if (!row.getTenantId().equals(snapshot.getTenantId())
                || !row.getSnapshotVersion().equals(snapshot.getVersion())
                || !row.getIssuedAt().equals(issuedAt) || !row.getExpiresAt().equals(expiresAt)
                || !row.getSignatureKeyId().equals(snapshot.getSignatureKeyId())
                || !row.getSignature().equals(snapshot.getSignature())
                || !row.getPayloadHash().equals(SaasEntitlementSnapshotSignatureUtils.contentDigest(snapshot))
                || !SaasEntitlementSnapshotSignatureUtils.verify(secret, snapshot)) {
            throw corrupted("Stored entitlement snapshot failed integrity verification", null);
        }
        return snapshot;
    }

    private SaasEntitlementSnapshotEntity entity(SaasEntitlementSnapshot snapshot, String payloadHash,
            String actor, LocalDateTime now, SaasEntitlementSnapshotEntity current) {
        SaasEntitlementSnapshotEntity row = new SaasEntitlementSnapshotEntity();
        row.setTenantId(snapshot.getTenantId());
        row.setSnapshotVersion(snapshot.getVersion());
        row.setPayloadHash(payloadHash);
        try {
            row.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize entitlement snapshot", exception);
        }
        row.setIssuedAt(epoch(snapshot.getIssuedAtEpochMs()));
        row.setExpiresAt(epoch(snapshot.getExpiresAtEpochMs()));
        row.setSignatureKeyId(snapshot.getSignatureKeyId());
        row.setSignature(snapshot.getSignature());
        row.setCreateBy(current == null ? actor : current.getCreateBy());
        row.setCreateTime(current == null ? now : current.getCreateTime());
        row.setUpdateBy(actor);
        row.setUpdateTime(now);
        row.setVersionNo(current == null ? 0L : current.getVersionNo() + 1);
        return row;
    }

    private void insert(SaasEntitlementSnapshotEntity row) {
        try {
            if (snapshotMapper.insert(row) != 1) throw versionConflict();
        } catch (DuplicateKeyException exception) {
            throw new SaasSnapshotException(SaasSnapshotException.ErrorCode.VERSION_CONFLICT,
                    "Entitlement snapshot was created concurrently", exception);
        }
    }

    private static long nextVersion(Long current) {
        if (current == null || current <= 0 || current == Long.MAX_VALUE) {
            throw corrupted("Stored snapshot version is invalid", null);
        }
        return current + 1;
    }

    private static LocalDateTime epoch(long epochMs) {
        try {
            return LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(epochMs), ZoneOffset.UTC);
        } catch (DateTimeException exception) {
            throw corrupted("Stored snapshot timestamp is invalid", exception);
        }
    }

    private static String tenantId(String value) {
        if (value == null || !TENANT_ID.matcher(value.trim()).matches()) {
            throw invalid("tenantId has an invalid format");
        }
        return value.trim();
    }

    private static String operator(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw invalid("operator must contain 1 to 64 characters");
        }
        return value.trim();
    }

    private static String requireKeyId(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw new IllegalStateException("Snapshot signing key-id must contain 1 to 64 characters");
        }
        return value.trim();
    }

    private static byte[] requireSecret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Snapshot signing secret is required");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("Snapshot signing secret must contain at least 32 UTF-8 bytes");
        }
        return bytes;
    }

    private static Duration requireValidity(Duration value) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalStateException("Snapshot validity must be positive");
        }
        return value;
    }

    private static Duration requireRenewBefore(Duration value, Duration validity) {
        if (value == null || value.isNegative() || value.compareTo(validity) >= 0) {
            throw new IllegalStateException("Snapshot renew-before must be non-negative and shorter than validity");
        }
        return value;
    }

    private static SaasSnapshotException invalid(String message) {
        return new SaasSnapshotException(SaasSnapshotException.ErrorCode.INVALID_INPUT, message);
    }

    private static SaasSnapshotException corrupted(String message, Throwable cause) {
        return cause == null
                ? new SaasSnapshotException(SaasSnapshotException.ErrorCode.SNAPSHOT_CORRUPTED, message)
                : new SaasSnapshotException(SaasSnapshotException.ErrorCode.SNAPSHOT_CORRUPTED, message, cause);
    }

    private static SaasSnapshotException versionConflict() {
        return new SaasSnapshotException(SaasSnapshotException.ErrorCode.VERSION_CONFLICT,
                "Entitlement snapshot changed concurrently");
    }
}
