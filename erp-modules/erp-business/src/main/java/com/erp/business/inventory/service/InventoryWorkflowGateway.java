package com.erp.business.inventory.service;

/**
 * 库存流程网关接口。
 */
public interface InventoryWorkflowGateway {

    /**
     * 发起库存审批流程。
     *
     * @param processKey 流程标识
     * @param billType 单据类型
     * @param billId 单据ID
     * @param billNo 单据编号
     * @return true 表示发起成功
     */
    boolean startWorkflow(String processKey, String billType, Long billId, String billNo);
}
