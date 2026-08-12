package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.SysUserActivation;
import com.erp.system.domain.vo.SaasUserActivationRequest;
import com.erp.system.mapper.SysUserActivationMapper;
import com.erp.system.saas.impl.SaasUserActivationServiceImpl;
import com.erp.system.service.ISysUserService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasUserActivationServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
    private SysUserActivationMapper activationMapper;
    private ISysUserService userService;
    private SaasSecureTokenService tokenService;
    private PasswordEncoder passwordEncoder;
    private SaasUserActivationService service;

    @Test
    void shouldCreateServiceThroughSpringConstructorInjection() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.registerBean(SysUserActivationMapper.class, () -> mock(SysUserActivationMapper.class));
            context.registerBean(ISysUserService.class, () -> mock(ISysUserService.class));
            context.registerBean(SaasSecureTokenService.class, () -> mock(SaasSecureTokenService.class));
            context.registerBean(PasswordEncoder.class, () -> mock(PasswordEncoder.class));
            context.register(SaasUserActivationServiceImpl.class);

            context.refresh();

            assertThat(context.getBean(SaasUserActivationServiceImpl.class)).isNotNull();
        }
    }

    @BeforeEach
    void setUp() {
        activationMapper = mock(SysUserActivationMapper.class);
        userService = mock(ISysUserService.class);
        tokenService = mock(SaasSecureTokenService.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new SaasUserActivationServiceImpl(activationMapper, userService, tokenService,
                passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
        TenantContextHolder.setTenantId("000000");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldConsumeTokenEnableUserAndRestoreTenantContext() {
        SysUserActivation activation = pending(NOW.plusSeconds(3600));
        when(tokenService.sha256("raw-token")).thenReturn("token-hash");
        when(activationMapper.lockByTokenHash("tenant-a", "token-hash")).thenReturn(activation);
        when(passwordEncoder.encode("StrongPass8")).thenReturn("encoded-password");
        when(userService.activateProvisionedUser(5L, "encoded-password")).thenReturn(true);
        when(activationMapper.markUsed("tenant-a", 7L, "token-hash", 0L,
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(1);

        service.activate("tenant-a", request("raw-token", "StrongPass8"));

        verify(userService).activateProvisionedUser(5L, "encoded-password");
        verify(activationMapper).markUsed("tenant-a", 7L, "token-hash", 0L,
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("000000");
    }

    @Test
    void shouldRejectExpiredOrAlreadyUsedTokenWithoutChangingUser() {
        when(tokenService.sha256("raw-token")).thenReturn("token-hash");
        when(activationMapper.lockByTokenHash("tenant-a", "token-hash"))
                .thenReturn(pending(NOW.minusSeconds(1)));

        assertThatThrownBy(() -> service.activate("tenant-a", request("raw-token", "StrongPass8")))
                .isInstanceOf(ServiceException.class)
                .hasMessage("激活链接无效或已过期");

        verify(userService, never()).activateProvisionedUser(5L, "encoded-password");
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("000000");
    }

    private SysUserActivation pending(Instant expiresAt) {
        SysUserActivation activation = new SysUserActivation();
        activation.setActivationId(7L);
        activation.setTenantId("tenant-a");
        activation.setUserId(5L);
        activation.setTokenHash("token-hash");
        activation.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneOffset.UTC));
        activation.setStatus("PENDING");
        activation.setVersionNo(0L);
        return activation;
    }

    private SaasUserActivationRequest request(String token, String password) {
        SaasUserActivationRequest request = new SaasUserActivationRequest();
        request.setToken(token);
        request.setNewPassword(password);
        return request;
    }
}
