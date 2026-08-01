package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasQuotaLimit implements Serializable {
    private static final long serialVersionUID = 1L;
    private String quotaKey;
    private Long limit;

    public SaasQuotaLimit() {
    }

    public SaasQuotaLimit(String quotaKey, Long limit) {
        this.quotaKey = quotaKey;
        this.limit = limit;
    }

    public String getQuotaKey() {
        return quotaKey;
    }

    public void setQuotaKey(String quotaKey) {
        this.quotaKey = quotaKey;
    }

    public Long getLimit() {
        return limit;
    }

    public void setLimit(Long limit) {
        this.limit = limit;
    }
}
