package com.erp.saas.control.service.model;

import com.erp.saas.control.service.SaasCatalogValidation;

public record QuotaEntitlement(boolean unlimited, long limitValue) {

    public QuotaEntitlement {
        if (unlimited) {
            limitValue = 0L;
        } else if (limitValue < 0) {
            throw SaasCatalogValidation.invalid("limitValue must not be negative");
        }
    }
}
