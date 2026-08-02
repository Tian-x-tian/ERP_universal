package com.erp.system.saas.impl;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.SysUserActivation;
import com.erp.system.domain.vo.SaasUserActivationRequest;
import com.erp.system.mapper.SysUserActivationMapper;
import com.erp.system.saas.SaasSecureTokenService;
import com.erp.system.saas.SaasUserActivationService;
import com.erp.system.service.ISysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.regex.Pattern;

@Service
public class SaasUserActivationServiceImpl implements SaasUserActivationService {
    private static final String INVALID_LINK = "激活链接无效或已过期";
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");

    private final SysUserActivationMapper activationMapper;
    private final ISysUserService userService;
    private final SaasSecureTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public SaasUserActivationServiceImpl(SysUserActivationMapper activationMapper,
            ISysUserService userService, SaasSecureTokenService tokenService,
            PasswordEncoder passwordEncoder) {
        this(activationMapper, userService, tokenService, passwordEncoder, Clock.systemUTC());
    }

    public SaasUserActivationServiceImpl(SysUserActivationMapper activationMapper,
            ISysUserService userService, SaasSecureTokenService tokenService,
            PasswordEncoder passwordEncoder, Clock clock) {
        this.activationMapper = Objects.requireNonNull(activationMapper);
        this.userService = Objects.requireNonNull(userService);
        this.tokenService = Objects.requireNonNull(tokenService);
        this.passwordEncoder = Objects.requireNonNull(passwordEncoder);
        this.clock = Objects.requireNonNull(clock);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void activate(String tenantId, SaasUserActivationRequest request) {
        String normalizedTenant = normalizeTenant(tenantId);
        String token = request == null ? null : normalize(request.getToken(), 256);
        String newPassword = request == null ? null : request.getNewPassword();
        if (token == null || newPassword == null || newPassword.length() < 8 || newPassword.length() > 128) {
            throw new ServiceException("新密码长度必须为8至128位");
        }
        String originalTenant = TenantContextHolder.getTenantId();
        TenantContextHolder.setTenantId(normalizedTenant);
        try {
            LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
            String tokenHash = tokenService.sha256(token);
            SysUserActivation activation = activationMapper.lockByTokenHash(normalizedTenant, tokenHash);
            if (activation == null || !"PENDING".equals(activation.getStatus())
                    || activation.getExpiresAt() == null || !activation.getExpiresAt().isAfter(now)
                    || activation.getVersionNo() == null) {
                throw new ServiceException(INVALID_LINK);
            }
            String encodedPassword = passwordEncoder.encode(newPassword);
            if (!userService.activateProvisionedUser(activation.getUserId(), encodedPassword)) {
                throw new ServiceException(INVALID_LINK);
            }
            int changed = activationMapper.markUsed(normalizedTenant, activation.getActivationId(),
                    tokenHash, activation.getVersionNo(), now);
            if (changed != 1) {
                throw new ServiceException(INVALID_LINK);
            }
        } finally {
            if (StringUtils.hasText(originalTenant)) {
                TenantContextHolder.setTenantId(originalTenant);
            } else {
                TenantContextHolder.clear();
            }
        }
    }

    private String normalizeTenant(String value) {
        String normalized = normalize(value, 20);
        if (normalized == null || !TENANT_ID.matcher(normalized).matches()) {
            throw new ServiceException(INVALID_LINK);
        }
        return normalized;
    }

    private String normalize(String value, int maximumLength) {
        if (!StringUtils.hasText(value)) return null;
        String normalized = value.trim();
        return normalized.length() <= maximumLength ? normalized : null;
    }
}
