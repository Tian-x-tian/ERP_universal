package com.erp.business.inventory.service.impl;

import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存流程网关默认实现单元测试。
 */
class InventoryWorkflowGatewayImplTest {

    /**
     * 验证网关会调用工作流内部客户端发起流程。
     */
    @Test
    void shouldStartWorkflowViaInternalClient() {
        InternalWorkflowClient internalWorkflowClient = mock(InternalWorkflowClient.class);
        SecurityUserResolver securityUserResolver = mock(SecurityUserResolver.class);
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        when(internalWorkflowClient.startProcess(any(WorkflowStartBody.class))).thenReturn(true);
        InventoryWorkflowGatewayImpl workflowGateway = new InventoryWorkflowGatewayImpl(internalWorkflowClient, securityUserResolver);

        boolean accepted = workflowGateway.startWorkflow("inventory_inbound", "PURCHASE_INBOUND", 10L, "INB-001");

        Assertions.assertTrue(accepted);
        ArgumentCaptor<WorkflowStartBody> captor = ArgumentCaptor.forClass(WorkflowStartBody.class);
        verify(internalWorkflowClient).startProcess(captor.capture());
        WorkflowStartBody startBody = captor.getValue();
        Assertions.assertEquals("inventory_inbound", startBody.getProcessKey());
        Assertions.assertEquals("business", startBody.getOwnerService());
        Assertions.assertEquals("PURCHASE_INBOUND", startBody.getBusinessType());
        Assertions.assertTrue(startBody.getFormData().contains("\"orderId\":10"));
    }

    /**
     * 验证缺少关键参数时拒绝发起流程。
     */
    @Test
    void shouldRejectWorkflowWhenRequiredFieldsMissing() {
        InternalWorkflowClient internalWorkflowClient = mock(InternalWorkflowClient.class);
        SecurityUserResolver securityUserResolver = mock(SecurityUserResolver.class);
        InventoryWorkflowGatewayImpl workflowGateway = new InventoryWorkflowGatewayImpl(internalWorkflowClient, securityUserResolver);

        boolean accepted = workflowGateway.startWorkflow("", "PURCHASE_INBOUND", 10L, "INB-001");

        Assertions.assertFalse(accepted);
    }
}
