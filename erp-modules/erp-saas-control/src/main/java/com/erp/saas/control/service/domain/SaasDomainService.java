package com.erp.saas.control.service.domain;

import com.erp.saas.control.service.domain.model.*;

import java.util.Optional;

public interface SaasDomainService {
    SaasDomainView register(RegisterDomainCommand command);
    SaasDomainView verify(VerifyDomainCommand command);
    SaasDomainView revoke(RevokeDomainCommand command);
    SaasDomainView transfer(TransferDomainCommand command);
    Optional<ResolvedTenantDomain> resolve(String host);
}
