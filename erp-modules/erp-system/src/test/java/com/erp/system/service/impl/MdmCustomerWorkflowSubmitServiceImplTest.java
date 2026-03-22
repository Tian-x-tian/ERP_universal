package com.erp.system.service.impl;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.MdmCustomer;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmCustomerService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.MdmStatusSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 客户审批提交流程服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MdmCustomerWorkflowSubmitServiceImplTest {

    @Mock
    private IMdmCustomerService customerService;

    @Mock
    private ISysWorkflowEngineService workflowEngineService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    @Mock
    private ISysUserService userService;

    @Mock
    private MdmCustomerMapper customerMapper;

    private MdmCustomerWorkflowSubmitServiceImpl workflowSubmitService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        workflowSubmitService = new MdmCustomerWorkflowSubmitServiceImpl(
                customerService,
                workflowEngineService,
                securityUserResolver,
                userService,
                customerMapper);
    }

    /**
     * 验证版本冲突时禁止发起审批流程。
     */
    @Test
    void shouldRejectSubmitWhenVersionConflict() {
        MdmCustomer customer = buildCustomer(1L, "TENANT_A", "DRAFT-C01", 2, MdmStatusSupport.DRAFT);
        when(customerService.getOne(any())).thenReturn(customer);

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> workflowSubmitService.submitDraftActivation(1L, 1, "mdm_customer", "提交审批"));

        Assertions.assertEquals((int) ResultCode.CONFLICT.getCode(), exception.getCode());
        verify(workflowEngineService, never()).startProcess(any(), any(), any(), any());
    }

    /**
     * 验证草稿客户提交审批后会进入审批中状态。
     */
    @Test
    void shouldSubmitDraftActivationSuccessfully() {
        MdmCustomer customer = buildCustomer(1L, "TENANT_A", "DRAFT-C01", 2, MdmStatusSupport.DRAFT);
        SysUser user = new SysUser();
        user.setUserId(99L);
        user.setUserName("tester");
        user.setNickName("测试员");
        when(customerService.getOne(any())).thenReturn(customer);
        when(workflowEngineService.hasRunningInstance(any(), any())).thenReturn(false);
        when(securityUserResolver.getCurrentUserId()).thenReturn(99L);
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        when(userService.selectUserByUserName("tester")).thenReturn(user);
        when(workflowEngineService.startProcess(any(), any(), any(), any())).thenReturn(true);
        when(customerService.update(any(), any())).thenReturn(true);

        boolean success = workflowSubmitService.submitDraftActivation(1L, 2, "mdm_customer", "提交审批");

        Assertions.assertTrue(success);
        verify(workflowEngineService).startProcess(any(), any(), any(), any());
        verify(customerService).update(any(), any());
    }

    /**
     * 构造客户测试数据。
     *
     * @param customerId 客户ID
     * @param tenantId 租户ID
     * @param customerCode 客户编码
     * @param versionNo 版本号
     * @param status 状态
     * @return 客户对象
     */
    private MdmCustomer buildCustomer(Long customerId, String tenantId, String customerCode, Integer versionNo, String status) {
        MdmCustomer customer = new MdmCustomer();
        customer.setCustomerId(customerId);
        customer.setTenantId(tenantId);
        customer.setCustomerCode(customerCode);
        customer.setCustomerName("测试客户");
        customer.setVersionNo(versionNo);
        customer.setStatus(status);
        customer.setDelFlag("0");
        return customer;
    }
}
