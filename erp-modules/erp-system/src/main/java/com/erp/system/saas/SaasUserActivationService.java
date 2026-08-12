package com.erp.system.saas;

import com.erp.system.domain.vo.SaasUserActivationRequest;

public interface SaasUserActivationService {
    void activate(String tenantId, SaasUserActivationRequest request);
}
