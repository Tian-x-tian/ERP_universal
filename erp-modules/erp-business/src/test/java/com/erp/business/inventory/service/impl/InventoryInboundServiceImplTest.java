package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import com.erp.business.inventory.mapper.InventoryInboundOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryInboundOrderMapper;
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
 * 入库服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryInboundServiceImplTest {

    @Mock
    private InventoryInboundOrderMapper orderMapper;

    @Mock
    private InventoryInboundOrderLineMapper lineMapper;

    @Mock
    private InventoryStockEngineSupport stockEngineSupport;

    @Mock
    private InventoryWorkflowGateway workflowGateway;

    @Mock
    private InventorySourceProgressCallback sourceProgressCallback;

    @Mock
    private SecurityUserResolver securityUserResolver;

    private InventoryInboundServiceImpl inboundService;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        TenantContextHolder.setTenantId("TENANT_A");
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        inboundService = new InventoryInboundServiceImpl(orderMapper, lineMapper, stockEngineSupport, workflowGateway,
                sourceProgressCallback, securityUserResolver);
        initTableInfoIfAbsent(InventoryInboundOrder.class);
        initTableInfoIfAbsent(InventoryInboundOrderLine.class);
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
     * 验证新增入库单时会补齐草稿默认值并保存明细。
     */
    @Test
    void shouldCreateInboundOrderWithDraftStatus() {
        InventoryInboundOrder order = buildDraftOrder();
        when(orderMapper.insert(any(InventoryInboundOrder.class))).thenAnswer(invocation -> {
            InventoryInboundOrder entity = invocation.getArgument(0);
            entity.setOrderId(100L);
            return 1;
        });
        when(lineMapper.insert(any(InventoryInboundOrderLine.class))).thenReturn(1);

        boolean success = inboundService.create(order);

        Assertions.assertTrue(success);
        Assertions.assertEquals(InventoryBillStatusSupport.DRAFT, order.getStatus());
        Assertions.assertEquals("TENANT_A", order.getTenantId());
        verify(lineMapper).insert(any(InventoryInboundOrderLine.class));
    }

    /**
     * 验证提交入库单时，配置流程后进入待审批状态。
     */
    @Test
    void shouldSubmitInboundOrderToPendingApprovalWhenWorkflowConfigured() {
        InventoryInboundOrder existed = buildExistingOrder(10L, InventoryBillStatusSupport.DRAFT, "inventory_inbound", 1);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existed);
        when(workflowGateway.startWorkflow("inventory_inbound", existed.getBillType(), existed.getOrderId(),
                existed.getBillNo())).thenReturn(true);
        when(orderMapper.update(any(InventoryInboundOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);

        boolean success = inboundService.submit(10L);

        Assertions.assertTrue(success);
        ArgumentCaptor<InventoryInboundOrder> captor = ArgumentCaptor.forClass(InventoryInboundOrder.class);
        verify(orderMapper).update(captor.capture(), any(LambdaUpdateWrapper.class));
        Assertions.assertEquals(InventoryBillStatusSupport.PENDING_APPROVAL, captor.getValue().getStatus());
    }

    /**
     * 验证执行入库单时会推动库存引擎并回写来源进度。
     */
    @Test
    void shouldExecuteInboundOrderAndCallbackSourceProgress() {
        InventoryInboundOrder approved = buildExistingOrder(20L, InventoryBillStatusSupport.APPROVED, "", 1);
        InventoryInboundOrder executing = buildExistingOrder(20L, InventoryBillStatusSupport.EXECUTING, "", 2);
        InventoryInboundOrder latest = buildExistingOrder(20L, InventoryBillStatusSupport.EXECUTING, "", 3);
        InventoryInboundOrderLine line = new InventoryInboundOrderLine();
        line.setLineNo(1);
        line.setItemId(88L);
        line.setQty(BigDecimal.TEN);
        when(orderMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(approved, latest, executing);
        when(orderMapper.update(any(InventoryInboundOrder.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(lineMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.singletonList(line));

        boolean success = inboundService.execute(20L);

        Assertions.assertTrue(success);
        verify(stockEngineSupport).applyInbound(latest, Collections.singletonList(line));
        verify(sourceProgressCallback).callback(latest.getSourceOrderType(), latest.getSourceOrderId(),
                latest.getSourceOrderNo(), latest.getBillNo(), InventoryBillStatusSupport.COMPLETED);
        verify(orderMapper, times(2)).update(any(InventoryInboundOrder.class), any(LambdaUpdateWrapper.class));
    }

    /**
     * 构造草稿单据。
     *
     * @return 入库单
     */
    private InventoryInboundOrder buildDraftOrder() {
        InventoryInboundOrder order = new InventoryInboundOrder();
        order.setOrgId(1L);
        order.setWarehouseId(2L);
        order.setBillNo("INB-001");
        InventoryInboundOrderLine line = new InventoryInboundOrderLine();
        line.setItemId(10L);
        line.setQty(BigDecimal.ONE);
        order.setLines(Collections.singletonList(line));
        return order;
    }

    /**
     * 构造已存在单据。
     *
     * @param orderId 单据ID
     * @param status 状态
     * @param processKey 流程Key
     * @param versionNo 版本号
     * @return 入库单
     */
    private InventoryInboundOrder buildExistingOrder(Long orderId, String status, String processKey, int versionNo) {
        InventoryInboundOrder order = new InventoryInboundOrder();
        order.setOrderId(orderId);
        order.setTenantId("TENANT_A");
        order.setBillNo("INB-001");
        order.setBillType("PURCHASE_INBOUND");
        order.setStatus(status);
        order.setOrgId(1L);
        order.setWarehouseId(2L);
        order.setProcessKey(processKey);
        order.setIdempotencyNo("INB-001");
        order.setVersionNo(versionNo);
        return order;
    }
}
