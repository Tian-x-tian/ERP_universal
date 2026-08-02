package com.erp.system.saas;

import com.erp.saas.contract.model.SaasQuotaUsage;
import com.erp.saas.contract.model.SaasUsageEvent;

public interface SaasLocalQuotaService {
    SaasQuotaUsage apply(SaasUsageEvent event);

    SaasQuotaUsage decreaseUsed(String metricKey, long amount, String operator);
}
