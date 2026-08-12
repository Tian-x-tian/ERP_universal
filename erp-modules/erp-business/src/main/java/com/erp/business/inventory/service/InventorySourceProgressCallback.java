package com.erp.business.inventory.service;

/**
 * 库存来源单进度回写接口。
 */
public interface InventorySourceProgressCallback {

    /**
     * 回写来源单执行进度。
     *
     * @param sourceOrderType 来源单类型
     * @param sourceOrderId 来源单ID
     * @param sourceOrderNo 来源单编号
     * @param billNo 库存单号
     * @param status 库存单状态
     */
    void callback(String sourceOrderType, Long sourceOrderId, String sourceOrderNo, String billNo, String status);
}
