package com.erp.system.service.impl;

import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.support.MdmStatusSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户工作流终态回调单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MdmCustomerWorkflowCallbackServiceImplTest {

    @Mock
    private MdmCustomerMapper customerMapper;

    @Mock
    private IMdmAuditTrailService auditTrailService;

    private MdmCustomerWorkflowCallbackServiceImpl callbackService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        callbackService = new MdmCustomerWorkflowCallbackServiceImpl(customerMapper, auditTrailService);
    }

    /**
     * 验证审批通过时会完成状态流转，并保持原租户隔离信息不被表单覆盖。
     */
    @Test
    void shouldApplyApprovedChangeAndKeepTenantIsolation() {
        MdmCustomer before = new MdmCustomer();
        before.setCustomerId(1L);
        before.setTenantId("TENANT_A");
        before.setCustomerCode("CUS001");
        before.setCustomerName("旧客户");
        before.setVersionNo(3);
        before.setStatus(MdmStatusSupport.SUBMITTED);
        before.setDelFlag("0");

        MdmCustomer after = new MdmCustomer();
        after.setCustomerId(1L);
        after.setTenantId("TENANT_A");
        after.setCustomerCode("CUS001");
        after.setCustomerName("新客户");
        after.setVersionNo(4);
        after.setStatus(MdmStatusSupport.ACTIVE);
        after.setDelFlag("0");

        when(customerMapper.selectOne(any())).thenReturn(before, after);
        when(customerMapper.update(any(MdmCustomer.class), any())).thenReturn(1);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setFormData("{\"__mdmCustomerMeta\":{\"action\":\"UPDATE\",\"customerId\":1,\"baseVersionNo\":3,"
                + "\"afterCustomer\":{\"tenantId\":\"TENANT_B\",\"customerCode\":\"CUS999\",\"customerName\":\"新客户\",\"status\":\"ACTIVE\"}}}");
        instance.setLastActionUserName("approver");

        callbackService.onWorkflowCompleted(instance);

        ArgumentCaptor<MdmCustomer> updateCaptor = ArgumentCaptor.forClass(MdmCustomer.class);
        verify(customerMapper).update(updateCaptor.capture(), any());
        MdmCustomer updateEntity = updateCaptor.getValue();
        Assertions.assertEquals("TENANT_A", updateEntity.getTenantId());
        Assertions.assertEquals("CUS001", updateEntity.getCustomerCode());
        Assertions.assertEquals(MdmStatusSupport.ACTIVE, updateEntity.getStatus());
        Assertions.assertEquals(4, updateEntity.getVersionNo());
        Assertions.assertEquals("approver", updateEntity.getUpdateBy());
        verify(auditTrailService).record(any(), any(), any(), any(), any(), any(), any());
    }
}
