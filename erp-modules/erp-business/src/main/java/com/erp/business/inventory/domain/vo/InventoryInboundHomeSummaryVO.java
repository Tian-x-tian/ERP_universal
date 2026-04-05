package com.erp.business.inventory.domain.vo;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 入库首页汇总对象。
 */
public class InventoryInboundHomeSummaryVO implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 本月入库数量 */
    private BigDecimal currentMonthInboundQty;

    /** 待入库单据数 */
    private long pendingInboundOrderCount;

    /** 近30天完成率 */
    private double completionRate30d;

    public BigDecimal getCurrentMonthInboundQty() {
        return currentMonthInboundQty;
    }

    public void setCurrentMonthInboundQty(BigDecimal currentMonthInboundQty) {
        this.currentMonthInboundQty = currentMonthInboundQty;
    }

    public long getPendingInboundOrderCount() {
        return pendingInboundOrderCount;
    }

    public void setPendingInboundOrderCount(long pendingInboundOrderCount) {
        this.pendingInboundOrderCount = pendingInboundOrderCount;
    }

    public double getCompletionRate30d() {
        return completionRate30d;
    }

    public void setCompletionRate30d(double completionRate30d) {
        this.completionRate30d = completionRate30d;
    }
}
