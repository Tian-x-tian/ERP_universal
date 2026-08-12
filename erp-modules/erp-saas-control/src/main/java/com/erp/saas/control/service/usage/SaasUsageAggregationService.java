package com.erp.saas.control.service.usage;

import com.erp.saas.contract.model.SaasUsageEvent;

public interface SaasUsageAggregationService {
    void report(SaasUsageEvent event, String operator);
}
