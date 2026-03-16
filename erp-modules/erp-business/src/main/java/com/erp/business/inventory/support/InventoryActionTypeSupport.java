package com.erp.business.inventory.support;

/**
 * 库存事务动作常量。
 */
public final class InventoryActionTypeSupport {
    public static final String INBOUND = "INBOUND";
    public static final String OUTBOUND = "OUTBOUND";
    public static final String TRANSFER_OUT = "TRANSFER_OUT";
    public static final String TRANSFER_IN = "TRANSFER_IN";
    public static final String MOVE = "MOVE";
    public static final String FREEZE = "FREEZE";
    public static final String UNFREEZE = "UNFREEZE";
    public static final String ADJUST_GAIN = "ADJUST_GAIN";
    public static final String ADJUST_LOSS = "ADJUST_LOSS";
    public static final String STOCKTAKE_GAIN = "STOCKTAKE_GAIN";
    public static final String STOCKTAKE_LOSS = "STOCKTAKE_LOSS";
    public static final String REPORT_EXPORT = "REPORT_EXPORT";
    public static final String INTEGRATION_CALLBACK = "INTEGRATION_CALLBACK";

    private InventoryActionTypeSupport() {
    }
}
