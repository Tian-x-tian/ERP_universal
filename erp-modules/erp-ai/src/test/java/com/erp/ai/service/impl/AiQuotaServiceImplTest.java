package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.model.AiQuotaDecision;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.platform.contract.model.PlatformAiQuotaUsage;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * AI 配额服务单元测试。
 */
class AiQuotaServiceImplTest {

    private InternalSystemClient systemClient;
    private AiTenantConfigService tenantConfigService;
    private ErpAiProperties properties;
    private AiQuotaServiceImpl quotaService;
    private PlatformAiConfigView config;

    @BeforeEach
    void setUp() {
        systemClient = Mockito.mock(InternalSystemClient.class);
        tenantConfigService = Mockito.mock(AiTenantConfigService.class);
        SecurityUserResolver securityUserResolver = Mockito.mock(SecurityUserResolver.class);
        properties = new ErpAiProperties();
        quotaService = new AiQuotaServiceImpl(systemClient, tenantConfigService, securityUserResolver, properties);

        config = new PlatformAiConfigView();
        Mockito.when(tenantConfigService.getConfig()).thenReturn(config);
        Mockito.when(securityUserResolver.getCurrentUserId()).thenReturn(1001L);
    }

    /**
     * 验证未配置任何限额时直接放行，且不去查用量。
     */
    @Test
    void shouldAllowWhenNoLimitConfigured() {
        AiQuotaDecision decision = quotaService.check();

        Assertions.assertTrue(decision.isAllowed());
        Mockito.verifyNoInteractions(systemClient);
    }

    /**
     * 验证租户请求数超限时拒绝。
     */
    @Test
    void shouldDenyWhenTenantRequestLimitReached() {
        config.setTenantDailyRequestLimit(100);
        Mockito.when(systemClient.getAiQuotaUsage(Mockito.any())).thenReturn(usage(100, 0L, 1));

        AiQuotaDecision decision = quotaService.check();

        Assertions.assertFalse(decision.isAllowed());
        Assertions.assertTrue(decision.getMessage().contains("100"));
    }

    /**
     * 验证租户 token 超限时拒绝。
     */
    @Test
    void shouldDenyWhenTenantTokenLimitReached() {
        config.setTenantDailyTokenLimit(50_000);
        Mockito.when(systemClient.getAiQuotaUsage(Mockito.any())).thenReturn(usage(10, 50_000L, 1));

        AiQuotaDecision decision = quotaService.check();

        Assertions.assertFalse(decision.isAllowed());
        Assertions.assertTrue(decision.getMessage().contains("token"));
    }

    /**
     * 验证单用户限额独立生效。
     */
    @Test
    void shouldDenyWhenUserRequestLimitReached() {
        config.setUserDailyRequestLimit(20);
        Mockito.when(systemClient.getAiQuotaUsage(Mockito.any())).thenReturn(usage(500, 999L, 20));

        AiQuotaDecision decision = quotaService.check();

        Assertions.assertFalse(decision.isAllowed());
        Assertions.assertTrue(decision.getMessage().contains("你今日"));
    }

    /**
     * 验证租户显式配置 0 表示不限制，不再回落实例配置。
     */
    @Test
    void shouldTreatExplicitZeroAsUnlimited() {
        properties.setTenantDailyRequestLimit(10);
        config.setTenantDailyRequestLimit(0);
        Mockito.when(systemClient.getAiQuotaUsage(Mockito.any())).thenReturn(usage(9999, 0L, 0));

        Assertions.assertTrue(quotaService.check().isAllowed());
    }

    /**
     * 验证租户未配置时回落到实例配置。
     */
    @Test
    void shouldFallBackToInstanceLimit() {
        properties.setTenantDailyRequestLimit(10);
        Mockito.when(systemClient.getAiQuotaUsage(Mockito.any())).thenReturn(usage(10, 0L, 0));

        Assertions.assertFalse(quotaService.check().isAllowed());
    }

    /**
     * 验证用量查询失败时放行。
     *
     * <p>配额是成本护栏，不应该因为统计接口抖动就把 AI 功能整体拦死。</p>
     */
    @Test
    void shouldAllowWhenUsageQueryFails() {
        config.setTenantDailyRequestLimit(1);
        Mockito.when(systemClient.getAiQuotaUsage(Mockito.any()))
                .thenThrow(new IllegalStateException("connection refused"));

        Assertions.assertTrue(quotaService.check().isAllowed());
    }

    private PlatformAiQuotaUsage usage(int tenantRequests, long tenantTokens, int userRequests) {
        PlatformAiQuotaUsage usage = new PlatformAiQuotaUsage();
        usage.setTenantRequestCount(tenantRequests);
        usage.setTenantTokenCount(tenantTokens);
        usage.setUserRequestCount(userRequests);
        return usage;
    }
}
