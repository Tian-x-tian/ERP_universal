package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.exception.ServiceException;
import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.system.domain.SysSaasProvisioningTask;
import com.erp.system.domain.SysUserActivation;
import com.erp.system.mapper.SysSaasProvisioningTaskMapper;
import com.erp.system.mapper.SysUserActivationMapper;
import com.erp.system.saas.impl.SaasTenantActivationReissueServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

class SaasTenantActivationReissueServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-02T12:00:00Z");
    private SysSaasProvisioningTaskMapper taskMapper;
    private SysUserActivationMapper activationMapper;
    private SaasSecureTokenService tokenService;
    private SaasTenantActivationReissueService service;

    @BeforeEach
    void setUp() {
        taskMapper = mock(SysSaasProvisioningTaskMapper.class);
        activationMapper = mock(SysUserActivationMapper.class);
        tokenService = mock(SaasSecureTokenService.class);
        service = new SaasTenantActivationReissueServiceImpl(taskMapper, activationMapper, tokenService,
                Clock.fixed(NOW, ZoneOffset.UTC), 24);
        TenantContextHolder.setTenantId("000000");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldAtomicallyReplacePendingTokenAndReturnRawValueOnce() {
        SysSaasProvisioningTask task = task();
        SysUserActivation activation = activation("PENDING");
        LocalDateTime expiresAt = LocalDateTime.ofInstant(NOW.plusSeconds(86_400), ZoneOffset.UTC);
        when(taskMapper.lock("tenant-a", "req-1")).thenReturn(task);
        when(activationMapper.lockByUser("tenant-a", 5L)).thenReturn(activation);
        when(tokenService.generate()).thenReturn(new SaasSecureTokenService.SecureToken(
                "new-raw-token", "new-token-hash"));
        when(activationMapper.reissue("tenant-a", 7L, 5L, 3L,
                "new-token-hash", expiresAt, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(1);
        when(taskMapper.updateActivationExpiry("tenant-a", "req-1", 5L, expiresAt,
                LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))).thenReturn(1);

        var result = service.reissue(new SaasTenantActivationReissueRequest("req-1", "tenant-a"));

        assertThat(result.getActivationToken()).isEqualTo("new-raw-token");
        assertThat(result.getActivationExpiresAtEpochMs()).isEqualTo(NOW.plusSeconds(86_400).toEpochMilli());
        assertThat(TenantContextHolder.getTenantId()).isEqualTo("000000");
        verify(activationMapper).reissue("tenant-a", 7L, 5L, 3L,
                "new-token-hash", expiresAt, LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));
    }

    @Test
    void shouldRefuseReissueAfterActivationWasUsed() {
        when(taskMapper.lock("tenant-a", "req-1")).thenReturn(task());
        when(activationMapper.lockByUser("tenant-a", 5L)).thenReturn(activation("USED"));

        assertThatThrownBy(() -> service.reissue(
                new SaasTenantActivationReissueRequest("req-1", "tenant-a")))
                .isInstanceOf(ServiceException.class)
                .hasMessageContaining("cannot be reissued");

        verify(tokenService, never()).generate();
    }

    private SysSaasProvisioningTask task() {
        SysSaasProvisioningTask task = new SysSaasProvisioningTask();
        task.setTenantId("tenant-a");
        task.setRequestId("req-1");
        task.setStatus("SUCCEEDED");
        task.setUserId(5L);
        return task;
    }

    private SysUserActivation activation(String status) {
        SysUserActivation activation = new SysUserActivation();
        activation.setActivationId(7L);
        activation.setTenantId("tenant-a");
        activation.setUserId(5L);
        activation.setStatus(status);
        activation.setVersionNo(3L);
        return activation;
    }
}
