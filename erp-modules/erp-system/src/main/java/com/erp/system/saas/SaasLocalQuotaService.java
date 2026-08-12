package com.erp.system.saas;

import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasUsageEvent;

import java.util.List;

public interface SaasLocalQuotaService {
    SaasQuotaUsage apply(SaasUsageEvent event);

    List<SaasQuotaUsage> applyBatch(List<SaasUsageEvent> events);

    SaasQuotaUsage decreaseUsed(String metricKey, long amount, String operator);
}
