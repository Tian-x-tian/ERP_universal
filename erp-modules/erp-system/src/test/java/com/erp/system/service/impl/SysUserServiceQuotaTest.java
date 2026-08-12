package com.erp.system.service.impl;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.saas.SaasLocalQuotaService;
import com.erp.system.service.ISysUserPostService;
import com.erp.system.service.ISysUserRoleService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SysUserServiceQuotaTest {
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private ISysUserRoleService userRoleService;
    @Mock
    private ISysUserPostService userPostService;
    @Mock
    private SaasLocalQuotaService quotaService;
    @Mock
    private SysUserMapper userMapper;

    private SysUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SysUserServiceImpl(passwordEncoder, userRoleService, userPostService, quotaService);
        ReflectionTestUtils.setField(service, "baseMapper", userMapper);
        TenantContextHolder.setTenantId("TENANT_A");
    }

    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    void shouldReserveAndSettleWhenCreatingActiveUser() {
        SysUser user = user("0", "0");
        when(userMapper.insert(user)).thenReturn(1);

        assertTrue(service.save(user));

        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        InOrder order = inOrder(quotaService, userMapper);
        order.verify(quotaService).apply(eventCaptor.capture());
        order.verify(userMapper).insert(user);
        order.verify(quotaService).apply(eventCaptor.capture());
        List<SaasUsageEvent> events = eventCaptor.getAllValues();
        org.junit.jupiter.api.Assertions.assertEquals(SaasUsageOperation.RESERVE, events.get(0).getOperation());
        org.junit.jupiter.api.Assertions.assertEquals(SaasUsageOperation.SETTLE, events.get(1).getOperation());
        org.junit.jupiter.api.Assertions.assertEquals(events.get(0).getReferenceKey(), events.get(1).getReferenceKey());
        org.junit.jupiter.api.Assertions.assertEquals(SaasQuotaKeys.USER_COUNT, events.get(0).getMetricKey());
    }

    @Test
    void shouldReserveOnEnableAndDecreaseOnDisable() {
        SysUser inactive = user("1", "0");
        inactive.setUserId(9L);
        when(userMapper.selectById(9L)).thenReturn(inactive);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        SysUser enable = new SysUser();
        enable.setUserId(9L);
        enable.setStatus("0");

        assertTrue(service.updateById(enable));

        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        verify(quotaService, org.mockito.Mockito.times(2)).apply(eventCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(SaasUsageOperation.RESERVE,
                eventCaptor.getAllValues().get(0).getOperation());
        org.junit.jupiter.api.Assertions.assertEquals(SaasUsageOperation.SETTLE,
                eventCaptor.getAllValues().get(1).getOperation());

        org.mockito.Mockito.reset(quotaService, userMapper, userRoleService, userPostService);
        SysUser active = user("0", "0");
        active.setUserId(9L);
        when(userMapper.selectById(9L)).thenReturn(active);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);
        SysUser disable = new SysUser();
        disable.setUserId(9L);
        disable.setStatus("1");

        assertTrue(service.updateById(disable));

        verify(quotaService).decreaseUsed(SaasQuotaKeys.USER_COUNT, 1L, "user-service");
        verify(quotaService, never()).apply(any());
    }

    @Test
    void shouldDecreaseOnlyAfterActiveUserWasDeleted() {
        SysUser active = user("0", "0");
        active.setUserId(7L);
        when(userMapper.selectById(7L)).thenReturn(active);
        when(userMapper.deleteById(7L)).thenReturn(1);

        assertTrue(service.removeUserById(7L));

        InOrder order = inOrder(userMapper, quotaService);
        order.verify(userMapper).deleteById(7L);
        order.verify(quotaService).decreaseUsed(SaasQuotaKeys.USER_COUNT, 1L, "user-service");
    }

    @Test
    void shouldActivateProvisionedUserWithoutRemovingRoleLinks() {
        SysUser inactive = user("1", "0");
        inactive.setUserId(11L);
        when(userMapper.selectById(11L)).thenReturn(inactive);
        when(userMapper.updateById(any(SysUser.class))).thenReturn(1);

        assertTrue(service.activateProvisionedUser(11L, "encoded-password"));

        ArgumentCaptor<SysUser> updateCaptor = ArgumentCaptor.forClass(SysUser.class);
        verify(userMapper).updateById(updateCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("0", updateCaptor.getValue().getStatus());
        org.junit.jupiter.api.Assertions.assertEquals("encoded-password", updateCaptor.getValue().getPassword());
        verify(userRoleService, never()).remove(any());
        verify(userPostService, never()).remove(any());
        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        verify(quotaService, org.mockito.Mockito.times(2)).apply(eventCaptor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(SaasUsageOperation.RESERVE,
                eventCaptor.getAllValues().get(0).getOperation());
        org.junit.jupiter.api.Assertions.assertEquals(SaasUsageOperation.SETTLE,
                eventCaptor.getAllValues().get(1).getOperation());
    }

    private static SysUser user(String status, String delFlag) {
        SysUser user = new SysUser();
        user.setTenantId("TENANT_A");
        user.setUserName("tester");
        user.setStatus(status);
        user.setDelFlag(delFlag);
        return user;
    }
}
