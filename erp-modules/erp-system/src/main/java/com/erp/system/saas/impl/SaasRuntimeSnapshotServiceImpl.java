package com.erp.system.saas.impl;

import com.erp.common.client.internal.InternalSaasClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.saas.contract.model.SaasEntitlementSnapshotSignatureUtils;
import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.system.saas.SaasLocalSnapshotStore;
import com.erp.system.saas.SaasRuntimeEntitlements;
import com.erp.system.saas.SaasRuntimeSnapshotException;
import com.erp.system.saas.SaasRuntimeSnapshotService;
import com.erp.system.saas.SaasRuntimeSource;
import com.erp.system.saas.SaasSnapshotVerificationProperties;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
public class SaasRuntimeSnapshotServiceImpl implements SaasRuntimeSnapshotService {
    private static final EnumSet<TenantLifecycleState> LOGIN_ALLOWED = EnumSet.of(
            TenantLifecycleState.TRIAL, TenantLifecycleState.ACTIVE,
            TenantLifecycleState.GRACE, TenantLifecycleState.READ_ONLY);
    private static final EnumSet<TenantLifecycleState> WRITE_ALLOWED = EnumSet.of(
            TenantLifecycleState.TRIAL, TenantLifecycleState.ACTIVE, TenantLifecycleState.GRACE);

    private final InternalSaasClient client;
    private final SaasLocalSnapshotStore store;
    private final Clock clock;
    private final String keyId;
    private final byte[] secret;
    private final Duration clockSkew;

    public SaasRuntimeSnapshotServiceImpl(InternalSaasClient client, SaasLocalSnapshotStore store,
            SaasSnapshotVerificationProperties properties, Clock clock) {
        this.client = Objects.requireNonNull(client, "client must not be null");
        this.store = Objects.requireNonNull(store, "store must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
        this.keyId = keyId(properties == null ? null : properties.getKeyId());
        this.secret = secret(properties == null ? null : properties.getSecret());
        this.clockSkew = clockSkew(properties == null ? null : properties.getClockSkew());
    }

    @Override
    public SaasRuntimeEntitlements refresh(String tenantId) {
        requireContext(tenantId);
        Instant now = clock.instant();
        SaasEntitlementSnapshot snapshot = client.loadEntitlementSnapshot(tenantId);
        validate(snapshot, tenantId, now, true);
        store.save(snapshot, "saas-refresh", LocalDateTime.ofInstant(now, ZoneOffset.UTC));
        return decision(snapshot, now, SaasRuntimeSource.REMOTE_REFRESH);
    }

    @Override
    public SaasRuntimeEntitlements current(String tenantId) {
        requireContext(tenantId);
        SaasEntitlementSnapshot snapshot = store.load(tenantId);
        if (snapshot == null) {
            return new SaasRuntimeEntitlements(tenantId, null, 0L, SaasRuntimeSource.MISSING,
                    true, false, false, Map.of(), Map.of());
        }
        Instant now = clock.instant();
        validate(snapshot, tenantId, now, false);
        boolean stale = !now.isBefore(Instant.ofEpochMilli(snapshot.getExpiresAtEpochMs()));
        return decision(snapshot, now, stale ? SaasRuntimeSource.EXPIRED_CACHE : SaasRuntimeSource.LOCAL_CACHE);
    }

    private void validate(SaasEntitlementSnapshot snapshot, String tenantId, Instant now,
            boolean requireFresh) {
        if (snapshot == null || !tenantId.equals(snapshot.getTenantId())) {
            throw invalid(SaasRuntimeSnapshotException.ErrorCode.INVALID_SIGNATURE,
                    "Snapshot tenant identity is invalid");
        }
        if (!keyId.equals(snapshot.getSignatureKeyId())
                || !SaasEntitlementSnapshotSignatureUtils.verify(secret, snapshot)) {
            throw invalid(SaasRuntimeSnapshotException.ErrorCode.INVALID_SIGNATURE,
                    "Snapshot signature verification failed");
        }
        final Instant issuedAt;
        final Instant expiresAt;
        try {
            issuedAt = Instant.ofEpochMilli(snapshot.getIssuedAtEpochMs());
            expiresAt = Instant.ofEpochMilli(snapshot.getExpiresAtEpochMs());
        } catch (RuntimeException exception) {
            throw invalid(SaasRuntimeSnapshotException.ErrorCode.INVALID_LEASE,
                    "Snapshot lease timestamps are invalid");
        }
        if (snapshot.getVersion() <= 0 || !expiresAt.isAfter(issuedAt)
                || issuedAt.isAfter(now.plus(clockSkew))
                || (requireFresh && !expiresAt.isAfter(now))) {
            throw invalid(SaasRuntimeSnapshotException.ErrorCode.INVALID_LEASE,
                    "Snapshot lease is outside the accepted time window");
        }
    }

    private SaasRuntimeEntitlements decision(SaasEntitlementSnapshot snapshot, Instant now,
            SaasRuntimeSource source) {
        boolean stale = !now.isBefore(Instant.ofEpochMilli(snapshot.getExpiresAtEpochMs()));
        TenantLifecycleState lifecycle = snapshot.getLifecycleState();
        boolean loginAllowed = lifecycle != null && LOGIN_ALLOWED.contains(lifecycle);
        boolean writeAllowed = !stale && lifecycle != null && WRITE_ALLOWED.contains(lifecycle);
        TreeMap<String, Boolean> features = new TreeMap<>();
        snapshot.getFeatureGrants().forEach(grant -> features.put(grant.getFeatureKey(), grant.isGranted()));
        TreeMap<String, Long> quotas = new TreeMap<>();
        snapshot.getQuotaLimits().forEach(quota -> quotas.put(quota.getQuotaKey(), quota.getLimit()));
        return new SaasRuntimeEntitlements(snapshot.getTenantId(), lifecycle, snapshot.getVersion(),
                source, stale, loginAllowed, writeAllowed, features, quotas);
    }

    private static void requireContext(String tenantId) {
        String current = TenantContextHolder.getTenantId();
        if (tenantId == null || current == null || !tenantId.equals(current)) {
            throw invalid(SaasRuntimeSnapshotException.ErrorCode.TENANT_CONTEXT_MISMATCH,
                    "Requested tenant does not match the active tenant context");
        }
    }

    private static String keyId(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw new IllegalStateException("Snapshot verification key-id must contain 1 to 64 characters");
        }
        return value.trim();
    }

    private static byte[] secret(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Snapshot verification secret is required");
        }
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("Snapshot verification secret must contain at least 32 UTF-8 bytes");
        }
        return bytes;
    }

    private static Duration clockSkew(Duration value) {
        if (value == null || value.isNegative() || value.compareTo(Duration.ofMinutes(5)) > 0) {
            throw new IllegalStateException("Snapshot verification clock-skew must be between 0 and 5 minutes");
        }
        return value;
    }

    private static SaasRuntimeSnapshotException invalid(
            SaasRuntimeSnapshotException.ErrorCode code, String message) {
        return new SaasRuntimeSnapshotException(code, message);
    }
}
