package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasUsageEvent implements Serializable {
    private static final long serialVersionUID = 1L;
    private String idempotencyKey;
    private String tenantId;
    private String metricKey;
    private SaasUsageOperation operation;
    private String referenceKey;
    private Long amount;
    private Long periodStartEpochMs;
    private long occurredAtEpochMs;

    public SaasUsageEvent() {
    }

    public SaasUsageEvent(String idempotencyKey, String tenantId, String metricKey, SaasUsageOperation operation,
            String referenceKey, Long amount, Long periodStartEpochMs, long occurredAtEpochMs) {
        this.idempotencyKey = idempotencyKey;
        this.tenantId = tenantId;
        this.metricKey = metricKey;
        this.operation = operation;
        this.referenceKey = referenceKey;
        this.amount = amount;
        this.periodStartEpochMs = periodStartEpochMs;
        this.occurredAtEpochMs = occurredAtEpochMs;
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public SaasUsageOperation getOperation() { return operation; }
    public void setOperation(SaasUsageOperation operation) { this.operation = operation; }
    public String getReferenceKey() { return referenceKey; }
    public void setReferenceKey(String referenceKey) { this.referenceKey = referenceKey; }
    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public Long getPeriodStartEpochMs() { return periodStartEpochMs; }
    public void setPeriodStartEpochMs(Long periodStartEpochMs) { this.periodStartEpochMs = periodStartEpochMs; }
    public long getOccurredAtEpochMs() { return occurredAtEpochMs; }
    public void setOccurredAtEpochMs(long occurredAtEpochMs) { this.occurredAtEpochMs = occurredAtEpochMs; }
}
