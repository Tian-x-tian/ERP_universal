package com.erp.system.saas;

import com.erp.saas.contract.model.SaasTenantActivationReissueRequest;
import com.erp.saas.contract.model.SaasTenantActivationReissueResult;

public interface SaasTenantActivationReissueService {
    SaasTenantActivationReissueResult reissue(SaasTenantActivationReissueRequest request);
}
