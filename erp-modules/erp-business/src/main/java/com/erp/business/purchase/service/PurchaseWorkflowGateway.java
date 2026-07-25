package com.erp.business.purchase.service;

/**
 * 采购流程网关。
 */
public interface PurchaseWorkflowGateway {

    /**
     * 发起采购审批流程。
     *
     * @param processKey 流程标识
     * @param billType   单据类型
     * @param billId     单据ID
     * @param billNo     单据编号
     * @return true 表示受理成功
     */
    boolean startWorkflow(String processKey, String billType, Long billId, String billNo);
}
