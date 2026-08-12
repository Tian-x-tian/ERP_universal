package com.erp.system.domain;

import java.time.LocalDateTime;

public class SysSaasQuotaReservation {
    private Long reservationId;
    private String tenantId;
    private String metricKey;
    private String reservationKey;
    private LocalDateTime periodStart;
    private Long reservedAmount;
    private Long settledAmount;
    private String status;
    private String reserveEventKey;
    private String settleEventKey;
    private String releaseEventKey;
    private String createBy;
    private LocalDateTime createTime;
    private String updateBy;
    private LocalDateTime updateTime;
    private Long versionNo;

    public Long getReservationId() { return reservationId; }
    public void setReservationId(Long reservationId) { this.reservationId = reservationId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getMetricKey() { return metricKey; }
    public void setMetricKey(String metricKey) { this.metricKey = metricKey; }
    public String getReservationKey() { return reservationKey; }
    public void setReservationKey(String reservationKey) { this.reservationKey = reservationKey; }
    public LocalDateTime getPeriodStart() { return periodStart; }
    public void setPeriodStart(LocalDateTime periodStart) { this.periodStart = periodStart; }
    public Long getReservedAmount() { return reservedAmount; }
    public void setReservedAmount(Long reservedAmount) { this.reservedAmount = reservedAmount; }
    public Long getSettledAmount() { return settledAmount; }
    public void setSettledAmount(Long settledAmount) { this.settledAmount = settledAmount; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getReserveEventKey() { return reserveEventKey; }
    public void setReserveEventKey(String reserveEventKey) { this.reserveEventKey = reserveEventKey; }
    public String getSettleEventKey() { return settleEventKey; }
    public void setSettleEventKey(String settleEventKey) { this.settleEventKey = settleEventKey; }
    public String getReleaseEventKey() { return releaseEventKey; }
    public void setReleaseEventKey(String releaseEventKey) { this.releaseEventKey = releaseEventKey; }
    public String getCreateBy() { return createBy; }
    public void setCreateBy(String createBy) { this.createBy = createBy; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public String getUpdateBy() { return updateBy; }
    public void setUpdateBy(String updateBy) { this.updateBy = updateBy; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
    public Long getVersionNo() { return versionNo; }
    public void setVersionNo(Long versionNo) { this.versionNo = versionNo; }
}
