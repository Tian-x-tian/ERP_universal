package com.erp.saas.control.service.domain.impl;

import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DomainVerificationState;
import com.erp.saas.control.domain.entity.SaasDomainEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDomainMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.domain.*;
import com.erp.saas.control.service.domain.model.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class SaasDomainServiceImpl implements SaasDomainService {
    private static final Set<TenantLifecycleState> TERMINAL_STATES = EnumSet.of(
            TenantLifecycleState.ARCHIVED, TenantLifecycleState.PURGE_PENDING, TenantLifecycleState.PURGED);

    private final SaasDomainMapper domainMapper;
    private final SaasTenantMapper tenantMapper;
    private final SaasDomainHostNormalizer normalizer;
    private final ControlUtcTime time;

    public SaasDomainServiceImpl(SaasDomainMapper domainMapper, SaasTenantMapper tenantMapper,
            SaasDomainHostNormalizer normalizer, ControlUtcTime time) {
        this.domainMapper = domainMapper;
        this.tenantMapper = tenantMapper;
        this.normalizer = normalizer;
        this.time = time;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasDomainView register(RegisterDomainCommand command) {
        command = SaasDomainValidation.required(command, "command");
        String host = normalizer.normalize(command.host());
        LocalDateTime now = time.now();
        lockEligibleTenant(command.tenantId());
        SaasDomainEntity owned = domainMapper.findOwnedHostForUpdate(host);
        if (owned != null) {
            if (command.tenantId().equals(owned.getTenantId())) {
                return view(owned);
            }
            throw ownershipConflict(null);
        }
        SaasDomainEntity entity = new SaasDomainEntity();
        entity.setTenantId(command.tenantId());
        entity.setHost(host);
        entity.setVerificationState(DomainVerificationState.PENDING);
        entity.setVerificationMethod(command.verificationMethod());
        entity.setVersionNo(0L);
        audit(entity, command.operator(), now);
        try {
            domainMapper.insert(entity);
        } catch (DuplicateKeyException exception) {
            throw ownershipConflict(exception);
        }
        return view(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasDomainView verify(VerifyDomainCommand command) {
        command = SaasDomainValidation.required(command, "command");
        LocalDateTime now = time.now();
        SaasDomainEntity hint = requireDomain(domainMapper.selectById(command.domainId()));
        lockEligibleTenant(hint.getTenantId());
        SaasDomainEntity current = requireDomain(domainMapper.findByIdForUpdate(command.domainId()));
        requireVersion(current, command.expectedVersion());
        if (current.getVerificationState() != DomainVerificationState.PENDING) {
            throw invalidState("Only pending domains can be verified");
        }
        cas(domainMapper.markVerified(current.getDomainId(), command.expectedVersion(),
                command.operator(), now));
        current.setVerificationState(DomainVerificationState.VERIFIED);
        current.setVerifiedAt(now);
        current.setRevokedAt(null);
        current.setVersionNo(current.getVersionNo() + 1);
        return view(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasDomainView revoke(RevokeDomainCommand command) {
        command = SaasDomainValidation.required(command, "command");
        LocalDateTime now = time.now();
        SaasDomainEntity hint = requireDomain(domainMapper.selectById(command.domainId()));
        lockTenant(hint.getTenantId());
        SaasDomainEntity current = requireDomain(domainMapper.findByIdForUpdate(command.domainId()));
        if (current.getVerificationState() == DomainVerificationState.REVOKED) {
            return view(current);
        }
        requireVersion(current, command.expectedVersion());
        cas(domainMapper.markRevoked(current.getDomainId(), command.expectedVersion(), command.operator(), now));
        current.setVerificationState(DomainVerificationState.REVOKED);
        current.setRevokedAt(now);
        current.setVersionNo(current.getVersionNo() + 1);
        return view(current);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SaasDomainView transfer(TransferDomainCommand command) {
        command = SaasDomainValidation.required(command, "command");
        LocalDateTime now = time.now();
        SaasDomainEntity hint = requireDomain(domainMapper.selectById(command.domainId()));
        if (hint.getTenantId().equals(command.targetTenantId())) {
            throw invalidState("Source and target tenants must differ");
        }
        for (String tenantId : new TreeSet<>(List.of(hint.getTenantId(), command.targetTenantId()))) {
            SaasTenantEntity tenant = lockTenant(tenantId);
            if (tenantId.equals(command.targetTenantId())) {
                requireEligible(tenant);
            }
        }
        SaasDomainEntity source = requireDomain(domainMapper.findByIdForUpdate(command.domainId()));
        requireVersion(source, command.expectedVersion());
        if (source.getVerificationState() == DomainVerificationState.REVOKED) {
            throw invalidState("Revoked domains cannot be transferred");
        }
        cas(domainMapper.markRevoked(source.getDomainId(), command.expectedVersion(), command.operator(), now));
        SaasDomainEntity target = new SaasDomainEntity();
        target.setTenantId(command.targetTenantId());
        target.setHost(source.getHost());
        target.setVerificationState(DomainVerificationState.PENDING);
        target.setVerificationMethod(source.getVerificationMethod());
        target.setVersionNo(0L);
        audit(target, command.operator(), now);
        try {
            domainMapper.insert(target);
        } catch (DuplicateKeyException exception) {
            throw ownershipConflict(exception);
        }
        return view(target);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ResolvedTenantDomain> resolve(String rawHost) {
        final String host;
        try {
            host = normalizer.normalize(rawHost);
        } catch (SaasDomainException exception) {
            if (exception.getErrorCode() == SaasDomainException.ErrorCode.INVALID_HOST) {
                return Optional.empty();
            }
            throw exception;
        }
        ResolvedTenantDomainRow row = domainMapper.resolveVerified(host);
        if (row == null) {
            return Optional.empty();
        }
        return Optional.of(new ResolvedTenantDomain(row.getDomainId(), row.getTenantId(),
                row.getHost(), row.getLifecycleState()));
    }

    private SaasTenantEntity lockEligibleTenant(String tenantId) {
        SaasTenantEntity tenant = lockTenant(tenantId);
        requireEligible(tenant);
        return tenant;
    }

    private SaasTenantEntity lockTenant(String tenantId) {
        SaasTenantEntity tenant = tenantMapper.lockByTenantId(tenantId);
        if (tenant == null) {
            throw new SaasDomainException(SaasDomainException.ErrorCode.TENANT_NOT_FOUND,
                    "Tenant not found");
        }
        return tenant;
    }

    private static void requireEligible(SaasTenantEntity tenant) {
        if (tenant.getLifecycleState() == null || TERMINAL_STATES.contains(tenant.getLifecycleState())) {
            throw new SaasDomainException(SaasDomainException.ErrorCode.TENANT_NOT_ELIGIBLE,
                    "Tenant cannot own domains in its current state");
        }
    }

    private static SaasDomainEntity requireDomain(SaasDomainEntity domain) {
        if (domain == null) {
            throw new SaasDomainException(SaasDomainException.ErrorCode.NOT_FOUND, "Domain not found");
        }
        return domain;
    }

    private static void requireVersion(SaasDomainEntity domain, Long expectedVersion) {
        if (!Objects.equals(domain.getVersionNo(), expectedVersion)) {
            throw versionConflict();
        }
    }

    private static void cas(int affected) {
        if (affected != 1) {
            throw versionConflict();
        }
    }

    private static void audit(SaasDomainEntity domain, String operator, LocalDateTime now) {
        domain.setCreateBy(operator);
        domain.setCreateTime(now);
        domain.setUpdateBy(operator);
        domain.setUpdateTime(now);
    }

    private static SaasDomainView view(SaasDomainEntity domain) {
        return new SaasDomainView(domain.getDomainId(), domain.getTenantId(), domain.getHost(),
                domain.getVerificationState(), domain.getVerificationMethod(), domain.getVerifiedAt(),
                domain.getRevokedAt(), domain.getVersionNo());
    }

    private static SaasDomainException ownershipConflict(Throwable cause) {
        return cause == null
                ? new SaasDomainException(SaasDomainException.ErrorCode.OWNERSHIP_CONFLICT,
                        "Domain host is already owned")
                : new SaasDomainException(SaasDomainException.ErrorCode.OWNERSHIP_CONFLICT,
                        "Domain host is already owned", cause);
    }

    private static SaasDomainException invalidState(String message) {
        return new SaasDomainException(SaasDomainException.ErrorCode.INVALID_STATE, message);
    }

    private static SaasDomainException versionConflict() {
        return new SaasDomainException(SaasDomainException.ErrorCode.VERSION_CONFLICT,
                "The expected version no longer matches");
    }
}
