package com.erp.system.saas;

import com.erp.common.core.context.TenantContextHolder;
import com.erp.system.mapper.SysTenantMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "erp.saas.snapshot", name = "refresh-enabled",
        havingValue = "true", matchIfMissing = true)
public class SaasSnapshotRefreshScheduler {
    private static final Logger log = LoggerFactory.getLogger(SaasSnapshotRefreshScheduler.class);
    private static final String PLATFORM_TENANT_ID = "000000";
    private final SysTenantMapper tenantMapper;
    private final SaasRuntimeSnapshotService snapshotService;

    public SaasSnapshotRefreshScheduler(SysTenantMapper tenantMapper,
            SaasRuntimeSnapshotService snapshotService) {
        this.tenantMapper = tenantMapper;
        this.snapshotService = snapshotService;
    }

    @Scheduled(fixedDelayString = "${erp.saas.snapshot.refresh-interval-ms:300000}",
            initialDelayString = "${erp.saas.snapshot.initial-delay-ms:30000}")
    public void refreshAll() {
        TenantContextHolder.clear();
        for (String tenantId : tenantMapper.findActiveTenantIds()) {
            if (tenantId == null || tenantId.isBlank() || PLATFORM_TENANT_ID.equals(tenantId)) continue;
            try {
                TenantContextHolder.setTenantId(tenantId);
                snapshotService.refresh(tenantId);
            } catch (RuntimeException exception) {
                log.warn("SaaS entitlement snapshot refresh failed: tenantId={}, errorType={}",
                        tenantId, exception.getClass().getSimpleName());
            } finally {
                TenantContextHolder.clear();
            }
        }
    }
}
