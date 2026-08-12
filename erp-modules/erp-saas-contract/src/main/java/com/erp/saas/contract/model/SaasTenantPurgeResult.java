package com.erp.saas.contract.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SaasTenantPurgeResult implements Serializable {
    private static final long serialVersionUID = 1L;

    private String requestId;
    private String tenantId;
    private int tablesProcessed;
    private long rowsDeleted;
    private int objectsDeleted;
    private boolean replayed;

    public SaasTenantPurgeResult() {
    }

    public SaasTenantPurgeResult(String requestId, String tenantId,
            int tablesProcessed, long rowsDeleted, boolean replayed) {
        this(requestId, tenantId, tablesProcessed, rowsDeleted, 0, replayed);
    }

    public SaasTenantPurgeResult(String requestId, String tenantId,
            int tablesProcessed, long rowsDeleted, int objectsDeleted, boolean replayed) {
        this.requestId = requestId;
        this.tenantId = tenantId;
        this.tablesProcessed = tablesProcessed;
        this.rowsDeleted = rowsDeleted;
        this.objectsDeleted = objectsDeleted;
        this.replayed = replayed;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public void setTenantId(String tenantId) {
        this.tenantId = tenantId;
    }

    public int getTablesProcessed() {
        return tablesProcessed;
    }

    public void setTablesProcessed(int tablesProcessed) {
        this.tablesProcessed = tablesProcessed;
    }

    public long getRowsDeleted() {
        return rowsDeleted;
    }

    public void setRowsDeleted(long rowsDeleted) {
        this.rowsDeleted = rowsDeleted;
    }

    public int getObjectsDeleted() { return objectsDeleted; }
    public void setObjectsDeleted(int objectsDeleted) { this.objectsDeleted = objectsDeleted; }

    public boolean isReplayed() {
        return replayed;
    }

    public void setReplayed(boolean replayed) {
        this.replayed = replayed;
    }
}
