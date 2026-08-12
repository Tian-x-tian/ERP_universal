package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasQuotaUsage implements Serializable {
    private static final long serialVersionUID = 1L;
    private String quotaKey;
    private long used;
    private long reserved;
    private Long periodStartEpochMs;

    public SaasQuotaUsage() {
    }

    public SaasQuotaUsage(String quotaKey, long used, long reserved, Long periodStartEpochMs) {
        this.quotaKey = quotaKey;
        this.used = used;
        this.reserved = reserved;
        this.periodStartEpochMs = periodStartEpochMs;
    }

    public String getQuotaKey() {
        return quotaKey;
    }

    public void setQuotaKey(String quotaKey) {
        this.quotaKey = quotaKey;
    }

    public long getUsed() {
        return used;
    }

    public void setUsed(long used) {
        this.used = used;
    }

    public long getReserved() {
        return reserved;
    }

    public void setReserved(long reserved) {
        this.reserved = reserved;
    }

    public Long getPeriodStartEpochMs() {
        return periodStartEpochMs;
    }

    public void setPeriodStartEpochMs(Long periodStartEpochMs) {
        this.periodStartEpochMs = periodStartEpochMs;
    }
}
