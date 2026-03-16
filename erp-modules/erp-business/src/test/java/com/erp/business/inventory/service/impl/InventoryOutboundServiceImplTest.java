package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.business.inventory.domain.InventoryOutboundOrder;
import com.erp.business.inventory.domain.InventoryOutboundOrderLine;
import com.erp.business.inventory.mapper.InventoryOutboundOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryOutboundOrderMapper;
import com.erp.business.inventory.service.InventorySourceProgressCallback;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.inventory.support.InventoryBillStatusSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.TenantContextHolder;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 出库服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryOutboundServiceImplTest {

    @Mock
    private InventoryOutboundOrderMapper orderMapper;

    @Mock
    private InventoryOutboundOrderLineMapper lineMapper;

    @Mock
    private InventoryStockEngineSupport stockEngineSupport;

    @Mock
    private InventoryWorkflowGateway workflowGateway;

    @Mock
    private InventorySourceProgressCallback sourceProgressCallback;

    @Mock
    private SecurityUserResolver securityUserResolver;

    private InventoryOutboundServiceImpl outboundService;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("TENANT_A");
        outboundService = new InventoryOutboundServiceImpl(orderMapper, lineMapper, stockEngineSupport, workflowGateway,
                sourceProgressCallback, securityUserResolver);
        initTableInfoIfAbsent(InventoryOutboundOrder.class);
        initTableInfoIfAbsent(InventoryOutboundOrderLine.class);
    }

    /**
     * 清理租户上下文。
     */
    @AfterEach
    void tearDown() {
        TenantContextHolder.clear();
    }

    /**
     * 初始化实体元数据缓存。
     *
     * @param entityClass 实体类型
     */
    private void initTableInfoIfAbsent(Class<?> entityClass) {
        if (TableInfoHelper.getTableInfo(entityClass) != null) {
            return;
        }
        MybatisConfiguration configuration = new MybatisConfiguration();
        MapperBuilderAssistant builderAssistant = new MapperBuilderAssistant(configuration, "");
        TableInfoHelper.initTableInfo(builderAssistant, entityClass);
    }

    /**
     * 验证提交出库单时，无流程则直接变更为已审批。
     */
    @Test
    void shouldSubmitOutboundOrderToApprovedWhenWorkflowMissing() {
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        InventoryOutboundOrder existed = buildExistingOrder(10L, InventoryBillStatusSupport.DRAFT, "", 1);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existed);
        when(orderMapper.update(any(InventoryOutboundOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean success = outboundService.submit(10L);

        Assertions.assertTrue(success);
        ArgumentCaptor<InventoryOutboundOrder> captor = ArgumentCaptor.forClass(InventoryOutboundOrder.class);
        verify(orderMapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
        Assertions.assertEquals(InventoryBillStatusSupport.APPROVED, captor.getValue().getStatus());
    }

    /**
     * 验证执行出库单时会推动库存引擎并回写来源进度。
     */
    @Test
    void shouldExecuteOutboundOrderAndCallbackSourceProgress() {
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        InventoryOutboundOrder approved = buildExistingOrder(20L, InventoryBillStatusSupport.APPROVED, "", 1);
        InventoryOutboundOrder latest = buildExistingOrder(20L, InventoryBillStatusSupport.EXECUTING, "", 2);
        InventoryOutboundOrder executing = buildExistingOrder(20L, InventoryBillStatusSupport.EXECUTING, "", 3);
        InventoryOutboundOrderLine line = new InventoryOutboundOrderLine();
        line.setLineNo(1);
        line.setItemId(99L);
        line.setQty(BigDecimal.valueOf(5));
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approved, latest, executing);
        when(orderMapper.update(any(InventoryOutboundOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(lineMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(line));

        boolean success = outboundService.execute(20L);

        Assertions.assertTrue(success);
        verify(stockEngineSupport).applyOutbound(latest, Collections.singletonList(line));
        verify(sourceProgressCallback).callback(latest.getSourceOrderType(), latest.getSourceOrderId(),
                latest.getSourceOrderNo(), latest.getBillNo(), InventoryBillStatusSupport.COMPLETED);
        verify(orderMapper, times(2)).update(any(InventoryOutboundOrder.class), any(LambdaUpdateWrapper.class));
    }

    /**
     * 验证取消已完成出库单会抛出异常。
     */
    @Test
    void shouldRejectCancelWhenOutboundOrderCompleted() {
        InventoryOutboundOrder existed = buildExistingOrder(30L, InventoryBillStatusSupport.COMPLETED, "", 1);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existed);

        IllegalStateException exception = Assertions.assertThrows(IllegalStateException.class,
                () -> outboundService.cancel(30L));

        Assertions.assertTrue(exception.getMessage().contains("已完成出库单不允许取消"));
    }

    /**
     * 构造已存在单据。
     *
     * @param orderId 单据ID
     * @param status 状态
     * @param processKey 流程Key
     * @param versionNo 版本号
     * @return 出库单
     */
    private InventoryOutboundOrder buildExistingOrder(Long orderId, String status, String processKey, int versionNo) {
        InventoryOutboundOrder order = new InventoryOutboundOrder();
        order.setOrderId(orderId);
        order.setTenantId("TENANT_A");
        order.setBillNo("OUT-001");
        order.setBillType("SALES_OUTBOUND");
        order.setStatus(status);
        order.setOrgId(1L);
        order.setWarehouseId(2L);
        order.setProcessKey(processKey);
        order.setIdempotencyNo("OUT-001");
        order.setVersionNo(versionNo);
        return order;
    }
}
