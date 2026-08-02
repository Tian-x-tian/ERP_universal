package com.erp.system.domain;

import java.time.LocalDateTime;

public class SysSaasQuotaCounter {
    private String tenantId;
    private String metricKey;
    private LocalDateTime periodStart;
    private Long usedAmount;
    private Long reservedAmount;
    private Long versionNo;

    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public Long getUsedAmount() { return usedAmount; }
    public void setUsedAmount(Long usedAmount) { this.usedAmount = usedAmount; }
    public Long getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(Long reservedAmount) { this.reservedAmount = reservedAmount; }
    public Long getVersionNo() { return versionNo; }
    public void setVersionNo(Long versionNo) { this.versionNo = versionNo; }
}
