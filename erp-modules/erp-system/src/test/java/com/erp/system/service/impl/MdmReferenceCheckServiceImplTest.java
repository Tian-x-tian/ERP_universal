package com.erp.system.service.impl;

import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.mapper.MdmCustomerMapper;
import com.erp.system.mapper.MdmCurrencyMapper;
import com.erp.system.mapper.MdmEmployeeMapper;
import com.erp.system.mapper.InventoryReferenceMapper;
import com.erp.system.mapper.MdmItemMapper;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.mapper.MdmSupplierMapper;
import com.erp.system.mapper.MdmWarehouseMapper;
import com.erp.system.service.ISysWorkflowEngineService;
import com.erp.system.support.MdmDomainTypeSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * MDM 引用检查服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class MdmReferenceCheckServiceImplTest {

    @Mock
    private MdmProjectMapper projectMapper;

    @Mock
    private MdmCustomerMapper customerMapper;

    @Mock
    private MdmEmployeeMapper employeeMapper;

    @Mock
    private MdmWarehouseMapper warehouseMapper;

    @Mock
    private MdmSupplierMapper supplierMapper;

    @Mock
    private MdmItemMapper itemMapper;

    @Mock
    private MdmCurrencyMapper currencyMapper;

    @Mock
    private ISysWorkflowEngineService workflowEngineService;

    @Mock
    private InventoryReferenceMapper inventoryReferenceMapper;

    private MdmReferenceCheckServiceImpl referenceCheckService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        referenceCheckService = new MdmReferenceCheckServiceImpl(
                projectMapper,
                customerMapper,
                employeeMapper,
                warehouseMapper,
                supplierMapper,
                itemMapper,
                currencyMapper,
                workflowEngineService,
                inventoryReferenceMapper);
    }

    /**
     * 验证引用检查会聚合主数据引用与流程占用明细。
     */
    @Test
    void shouldAggregateReferenceConflictDetails() {
        when(projectMapper.selectCount(any())).thenReturn(1L);
        when(workflowEngineService.hasRunningInstance(any(), any())).thenReturn(true);

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> referenceCheckService.check(MdmDomainTypeSupport.CUSTOMER, 100L));

        Assertions.assertEquals((int) ResultCode.CONFLICT.getCode(), exception.getCode());
        Assertions.assertTrue(exception.getMessage().contains("项目档案已引用当前客户"));
        Assertions.assertTrue(exception.getMessage().contains("运行中的审批流程"));
    }

    /**
     * 验证非法域类型会返回统一业务校验错误码。
     */
    @Test
    void shouldRejectUnsupportedDomainType() {
        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> referenceCheckService.check("UNKNOWN_DOMAIN", 100L));

        Assertions.assertEquals((int) ResultCode.VALIDATE_FAILED.getCode(), exception.getCode());
        Assertions.assertTrue(exception.getMessage().contains("暂不支持"));
    }
}
