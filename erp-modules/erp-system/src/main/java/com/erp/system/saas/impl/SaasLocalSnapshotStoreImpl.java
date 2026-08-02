package com.erp.system.saas.impl;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasEntitlementSnapshot;
import com.erp.system.domain.SysSaasEntitlementSnapshot;
import com.erp.system.mapper.SysSaasEntitlementSnapshotMapper;
import com.erp.system.saas.SaasLocalSnapshotStore;
import com.erp.system.saas.SaasRuntimeSnapshotException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Objects;

@Service
public class SaasLocalSnapshotStoreImpl implements SaasLocalSnapshotStore {
    private final SysSaasEntitlementSnapshotMapper mapper;
    private final ObjectMapper objectMapper;

    public SaasLocalSnapshotStoreImpl(SysSaasEntitlementSnapshotMapper mapper, ObjectMapper objectMapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }

    @Override
    @Transactional(readOnly = true)
    public SaasEntitlementSnapshot load(String tenantId) {
        requireContext(tenantId);
        SysSaasEntitlementSnapshot row = mapper.findByTenantId(tenantId);
        if (row == null) return null;
        try {
            SaasEntitlementSnapshot snapshot = objectMapper.readValue(
                    row.getSnapshotJson(), SaasEntitlementSnapshot.class);
            if (!tenantId.equals(snapshot.getTenantId())
                    || !Objects.equals(row.getSnapshotVersion(), snapshot.getVersion())
                    || !Objects.equals(row.getIssuedAt(), epoch(snapshot.getIssuedAtEpochMs()))
                    || !Objects.equals(row.getExpiresAt(), epoch(snapshot.getExpiresAtEpochMs()))
                    || !Objects.equals(row.getSignatureKeyId(), snapshot.getSignatureKeyId())
                    || !Objects.equals(row.getSignature(), snapshot.getSignature())) {
                throw corrupted("Stored snapshot metadata does not match its payload", null);
            }
            return snapshot;
        } catch (JsonProcessingException exception) {
            throw corrupted("Stored snapshot is not valid JSON", exception);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void save(SaasEntitlementSnapshot snapshot, String operator, LocalDateTime now) {
        if (snapshot == null || now == null) throw invalid("snapshot and now are required");
        requireContext(snapshot.getTenantId());
        String actor = operator(operator);
        SysSaasEntitlementSnapshot current = mapper.findForUpdate(snapshot.getTenantId());
        if (current != null) {
            if (current.getSnapshotVersion() == null || current.getVersionNo() == null) {
                throw corrupted("Stored snapshot version metadata is missing", null);
            }
            if (snapshot.getVersion() < current.getSnapshotVersion()) {
                throw new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.VERSION_ROLLBACK,
                        "Control-plane snapshot version moved backwards");
            }
            if (snapshot.getVersion() == current.getSnapshotVersion()) {
                if (!Objects.equals(snapshot.getSignature(), current.getSignature())
                        || !Objects.equals(snapshot.getSignatureKeyId(), current.getSignatureKeyId())) {
                    throw new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.VERSION_COLLISION,
                            "Equal snapshot versions contain different signed content");
                }
            }
        }
        SysSaasEntitlementSnapshot replacement = entity(snapshot, actor, now, current);
        if (current != null && snapshot.getVersion() == current.getSnapshotVersion()
                && sameSignedPayload(current, replacement)) {
            return;
        }
        if (current == null) {
            try {
                if (mapper.insert(replacement) != 1) throw conflict();
            } catch (DuplicateKeyException exception) {
                throw new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.VERSION_CONFLICT,
                        "Local snapshot was created concurrently", exception);
            }
        } else if (mapper.updateVersioned(replacement, current.getVersionNo(), actor, now) != 1) {
            throw conflict();
        }
    }

    private static boolean sameSignedPayload(SysSaasEntitlementSnapshot current,
            SysSaasEntitlementSnapshot replacement) {
        return Objects.equals(current.getSnapshotVersion(), replacement.getSnapshotVersion())
                && Objects.equals(current.getSnapshotJson(), replacement.getSnapshotJson())
                && Objects.equals(current.getIssuedAt(), replacement.getIssuedAt())
                && Objects.equals(current.getExpiresAt(), replacement.getExpiresAt())
                && Objects.equals(current.getSignatureKeyId(), replacement.getSignatureKeyId())
                && Objects.equals(current.getSignature(), replacement.getSignature());
    }

    private SysSaasEntitlementSnapshot entity(SaasEntitlementSnapshot snapshot, String actor,
            LocalDateTime now, SysSaasEntitlementSnapshot current) {
        SysSaasEntitlementSnapshot row = new SysSaasEntitlementSnapshot();
        row.setTenantId(snapshot.getTenantId());
        row.setSnapshotVersion(snapshot.getVersion());
        try {
            row.setSnapshotJson(objectMapper.writeValueAsString(snapshot));
        } catch (JsonProcessingException exception) {
            throw corrupted("Unable to serialize signed snapshot", exception);
        }
        row.setIssuedAt(epoch(snapshot.getIssuedAtEpochMs()));
        row.setExpiresAt(epoch(snapshot.getExpiresAtEpochMs()));
        row.setSignatureKeyId(snapshot.getSignatureKeyId());
        row.setSignature(snapshot.getSignature());
        Date auditTime = Date.from(now.toInstant(ZoneOffset.UTC));
        row.setCreateBy(current == null ? actor : current.getCreateBy());
        row.setCreateTime(current == null ? auditTime : current.getCreateTime());
        row.setUpdateBy(actor);
        row.setUpdateTime(auditTime);
        row.setVersionNo(current == null ? 0L : current.getVersionNo() + 1);
        return row;
    }

    private static void requireContext(String tenantId) {
        String current = TenantContextHolder.getTenantId();
        if (tenantId == null || current == null || !tenantId.equals(current)) {
            throw new SaasRuntimeSnapshotException(
                    SaasRuntimeSnapshotException.ErrorCode.TENANT_CONTEXT_MISMATCH,
                    "Snapshot tenant does not match the active tenant context");
        }
    }

    private static String operator(String value) {
        if (value == null || value.trim().isEmpty() || value.trim().length() > 64) {
            throw invalid("operator must contain 1 to 64 characters");
        }
        return value.trim();
    }

    private static LocalDateTime epoch(long epochMs) {
        try {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMs), ZoneOffset.UTC);
        } catch (RuntimeException exception) {
            throw corrupted("Snapshot timestamp is invalid", exception);
        }
    }

    private static SaasRuntimeSnapshotException invalid(String message) {
        return new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.INVALID_INPUT, message);
    }

    private static SaasRuntimeSnapshotException corrupted(String message, Throwable cause) {
        return cause == null
                ? new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.SNAPSHOT_CORRUPTED, message)
                : new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.SNAPSHOT_CORRUPTED,
                        message, cause);
    }

    private static SaasRuntimeSnapshotException conflict() {
        return new SaasRuntimeSnapshotException(SaasRuntimeSnapshotException.ErrorCode.VERSION_CONFLICT,
                "Local snapshot changed concurrently");
    }
}
