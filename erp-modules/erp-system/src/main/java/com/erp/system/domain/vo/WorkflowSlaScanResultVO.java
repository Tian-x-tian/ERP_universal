package com.erp.system.domain.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 流程 SLA 扫描结果。
 */
@Data
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
}
