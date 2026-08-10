package com.erp.ai.service.impl;

import com.erp.ai.config.ErpAiProperties;
import com.erp.ai.context.AiInvocationScope;
import com.erp.ai.model.AiQuotaDecision;
import com.erp.ai.security.service.SecurityUserResolver;
import com.erp.ai.service.AiQuotaService;
import com.erp.ai.service.AiTenantConfigService;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.platform.contract.model.PlatformAiConfigView;
import com.erp.platform.contract.model.PlatformAiQuotaUsage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * AI 配额服务实现。
 *
 * <p>限额优先取租户配置，未配置时回落到实例级配置；两者都为 0 表示不限制。
 * 用量口径与 {@code sys_ai_audit} 同源，不额外维护计数器，多实例部署下不会漂移。</p>
 */
@Service
public class AiQuotaServiceImpl implements AiQuotaService {
    private static final Logger log = LoggerFactory.getLogger(AiQuotaServiceImpl.class);
    private static final String CACHE_KEY_USAGE = "ai:quota-usage";

    private final InternalSystemClient internalSystemClient;
    private final AiTenantConfigService aiTenantConfigService;
    private final SecurityUserResolver securityUserResolver;
    private final ErpAiProperties erpAiProperties;

    public AiQuotaServiceImpl(InternalSystemClient internalSystemClient,
            AiTenantConfigService aiTenantConfigService,
            SecurityUserResolver securityUserResolver,
            ErpAiProperties erpAiProperties) {
        this.internalSystemClient = internalSystemClient;
        this.aiTenantConfigService = aiTenantConfigService;
        this.securityUserResolver = securityUserResolver;
        this.erpAiProperties = erpAiProperties;
    }

    /**
     * 判定当前调用是否在配额内。
     *
     * @return 判定结果
     */
    @Override
    public AiQuotaDecision check() {
        PlatformAiConfigView config = aiTenantConfigService.getConfig();
        int tenantRequestLimit = resolveLimit(config.getTenantDailyRequestLimit(),
                erpAiProperties.getTenantDailyRequestLimit());
        int tenantTokenLimit = resolveLimit(config.getTenantDailyTokenLimit(),
                erpAiProperties.getTenantDailyTokenLimit());
        int userRequestLimit = resolveLimit(config.getUserDailyRequestLimit(),
                erpAiProperties.getUserDailyRequestLimit());

        if (tenantRequestLimit <= 0 && tenantTokenLimit <= 0 && userRequestLimit <= 0) {
            return AiQuotaDecision.allow();
        }

        PlatformAiQuotaUsage usage = loadUsage();
        if (usage == null) {
            // 查不到用量时放行：配额是成本护栏，不该因为统计接口抖动就把功能整体拦死。
            return AiQuotaDecision.allow();
        }

        if (tenantRequestLimit > 0 && usage.getTenantRequestCount() >= tenantRequestLimit) {
            return AiQuotaDecision.deny("本租户今日 AI 调用次数已达上限（" + tenantRequestLimit + " 次），请明天再试或联系管理员调整配额。");
        }
        if (tenantTokenLimit > 0 && usage.getTenantTokenCount() >= tenantTokenLimit) {
            return AiQuotaDecision.deny("本租户今日 AI token 消耗已达上限（" + tenantTokenLimit + "），请明天再试或联系管理员调整配额。");
        }
        if (userRequestLimit > 0 && usage.getUserRequestCount() >= userRequestLimit) {
            return AiQuotaDecision.deny("你今日的 AI 调用次数已达上限（" + userRequestLimit + " 次），请明天再试。");
        }
        return AiQuotaDecision.allow();
    }

    /**
     * 读取当日用量，按调用作用域缓存。
     *
     * @return 用量统计；查询失败时返回 null
     */
    private PlatformAiQuotaUsage loadUsage() {
        return AiInvocationScope.compute(CACHE_KEY_USAGE, () -> {
            try {
                return internalSystemClient.getAiQuotaUsage(securityUserResolver.getCurrentUserId());
            } catch (Exception ex) {
                log.warn("查询 AI 当日用量失败，本次不做配额拦截：{}", ex.getMessage());
                return null;
            }
        });
    }

    /**
     * 解析生效限额：租户配置优先，未配置时回落实例配置。
     *
     * @param tenantLimit   租户配置
     * @param instanceLimit 实例配置
     * @return 生效限额
     */
    private int resolveLimit(Integer tenantLimit, int instanceLimit) {
        if (tenantLimit != null && tenantLimit > 0) {
            return tenantLimit;
        }
        if (tenantLimit != null && tenantLimit == 0) {
            // 租户显式配置为 0 表示「本租户不限制」，此时不再回落实例配置。
            return 0;
        }
        return Math.max(0, instanceLimit);
    }
}
