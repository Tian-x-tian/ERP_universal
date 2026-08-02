package com.erp.saas.control.service.lifecycle;

import com.erp.saas.control.mapper.SaasSubscriptionMapper;
import com.erp.saas.control.service.ControlUtcTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "erp.saas.lifecycle.reconcile-enabled", havingValue = "true", matchIfMissing = true)
public class SaasLifecycleReconciliationScheduler {
    private static final Logger log = LoggerFactory.getLogger(SaasLifecycleReconciliationScheduler.class);
    private static final String OPERATOR = "saas-lifecycle-scheduler";

    private final SaasSubscriptionMapper subscriptionMapper;
    private final SaasTenantLifecycleService lifecycleService;
    private final ControlUtcTime time;
    private final int batchSize;

    public SaasLifecycleReconciliationScheduler(SaasSubscriptionMapper subscriptionMapper,
            SaasTenantLifecycleService lifecycleService, ControlUtcTime time,
            @Value("${erp.saas.lifecycle.reconcile-batch-size:200}") int batchSize) {
        if (batchSize <= 0 || batchSize > 2000) {
            throw new IllegalStateException("erp.saas.lifecycle.reconcile-batch-size must be between 1 and 2000");
        }
        this.subscriptionMapper = subscriptionMapper;
        this.lifecycleService = lifecycleService;
        this.time = time;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${erp.saas.lifecycle.reconcile-interval-ms:60000}",
            initialDelayString = "${erp.saas.lifecycle.reconcile-initial-delay-ms:30000}")
    public void reconcileDueTenants() {
        List<String> tenantIds = subscriptionMapper.findDueTenantIds(time.now(), batchSize);
        if (tenantIds == null || tenantIds.isEmpty()) return;
        for (String tenantId : tenantIds) {
            try {
                lifecycleService.reconcile(tenantId, OPERATOR);
            } catch (RuntimeException error) {
                log.error("Failed to reconcile SaaS tenant lifecycle tenantId={}", tenantId, error);
            }
        }
    }
}
