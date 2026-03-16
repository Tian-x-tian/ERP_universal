package com.erp.business.inventory.service.impl;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * 库存流程网关默认实现单元测试。
 */
class InventoryWorkflowGatewayImplTest {

    /**
     * 验证流程标识和单据ID齐全时返回受理成功。
     */
    @Test
    void shouldAcceptWorkflowWhenProcessKeyAndBillIdPresent() {
        InventoryWorkflowGatewayImpl workflowGateway = new InventoryWorkflowGatewayImpl();

        boolean accepted = workflowGateway.startWorkflow("inventory_inbound", "PURCHASE_INBOUND", 10L, "INB-001");

        Assertions.assertTrue(accepted);
    }

    /**
     * 验证缺少流程标识时拒绝受理。
     */
    @Test
    void shouldRejectWorkflowWhenProcessKeyMissing() {
        InventoryWorkflowGatewayImpl workflowGateway = new InventoryWorkflowGatewayImpl();

        boolean accepted = workflowGateway.startWorkflow("", "PURCHASE_INBOUND", 10L, "INB-001");

        Assertions.assertFalse(accepted);
    }
}
