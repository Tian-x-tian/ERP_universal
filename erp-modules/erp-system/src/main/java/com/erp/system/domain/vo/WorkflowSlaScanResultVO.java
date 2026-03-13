package com.erp.system.domain.vo;


import java.io.Serializable;
import java.util.Date;

/**
 * 流程 SLA 扫描结果。
 */
public class WorkflowSlaScanResultVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 扫描任务数 */
    private int scannedCount;

    /** 即将超时任务数 */
    private int warningCount;

    /** 已超时任务数 */
    private int overdueCount;

    /** 已发送提醒数 */
    private int remindedCount;

    /** 已执行升级数 */
    private int escalatedCount;

    /** 已执行自动转办数 */
    private int transferredCount;

    /** 跳过处理数 */
    private int skippedCount;

    /** 扫描时间 */
    private Date scanTime;


    public int getScannedCount() {
        return scannedCount;
    }

    public void setScannedCount(int scannedCount) {
        this.scannedCount = scannedCount;
    }

    public int getWarningCount() {
        return warningCount;
    }

    public void setWarningCount(int warningCount) {
        this.warningCount = warningCount;
    }

    public int getOverdueCount() {
        return overdueCount;
    }

    public void setOverdueCount(int overdueCount) {
        this.overdueCount = overdueCount;
    }

    public int getRemindedCount() {
        return remindedCount;
    }

    public void setRemindedCount(int remindedCount) {
        this.remindedCount = remindedCount;
    }

    public int getEscalatedCount() {
        return escalatedCount;
    }

    public void setEscalatedCount(int escalatedCount) {
        this.escalatedCount = escalatedCount;
    }

    public int getTransferredCount() {
        return transferredCount;
    }

    public void setTransferredCount(int transferredCount) {
        this.transferredCount = transferredCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public Date getScanTime() {
        return scanTime;
    }

    public void setScanTime(Date scanTime) {
        this.scanTime = scanTime;
    }
}
