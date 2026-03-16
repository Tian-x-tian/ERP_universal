package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import com.erp.business.inventory.domain.InventoryOutboundOrder;
import com.erp.business.inventory.domain.InventoryOutboundOrderLine;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.domain.MdmItem;
import com.erp.business.inventory.domain.MdmWarehouse;
import com.erp.business.inventory.mapper.InventoryBatchRecordMapper;
import com.erp.business.inventory.mapper.InventorySerialRecordMapper;
import com.erp.business.inventory.mapper.InventoryStockBalanceMapper;
import com.erp.business.inventory.mapper.InventoryStockTxnMapper;
import com.erp.business.inventory.mapper.MdmItemMapper;
import com.erp.business.inventory.mapper.MdmWarehouseMapper;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.apache.ibatis.builder.MapperBuilderAssistant;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 库存事务引擎支持组件单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryStockEngineSupportTest {

    @Mock
    private InventoryStockBalanceMapper stockBalanceMapper;

    @Mock
    private InventoryStockTxnMapper stockTxnMapper;

    @Mock
    private MdmWarehouseMapper warehouseMapper;

    @Mock
    private MdmItemMapper itemMapper;

    @Mock
    private InventoryBatchRecordMapper batchRecordMapper;

    @Mock
    private InventorySerialRecordMapper serialRecordMapper;

    @Mock
    private SecurityUserResolver securityUserResolver;

    private InventoryStockEngineSupport stockEngineSupport;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        stockEngineSupport = new InventoryStockEngineSupport(stockBalanceMapper, stockTxnMapper, warehouseMapper,
                itemMapper, batchRecordMapper, serialRecordMapper, securityUserResolver);
        initTableInfoIfAbsent(InventoryStockBalance.class);
        initTableInfoIfAbsent(InventoryStockTxn.class);
        MdmItem item = new MdmItem();
        item.setBatchControl("N");
        item.setSerialControl("N");
        lenient().when(itemMapper.selectById(any())).thenReturn(item);
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
     * 验证幂等命中时不会重复扣账。
     */
    @Test
    void shouldSkipInboundWhenIdempotentTxnExists() {
        InventoryInboundOrder order = buildInboundOrder();
        InventoryInboundOrderLine line = buildInboundLine(BigDecimal.TEN);
        when(stockTxnMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        stockEngineSupport.applyInbound(order, Collections.singletonList(line));

        verify(stockBalanceMapper, never()).insert(any(InventoryStockBalance.class));
        verify(stockBalanceMapper, never()).update(any(InventoryStockBalance.class), any(LambdaUpdateWrapper.class));
        verify(stockTxnMapper, never()).insert(any(InventoryStockTxn.class));
    }

    /**
     * 验证禁止负库存时库存不足会抛出冲突异常。
     */
    @Test
    void shouldRejectOutboundWhenAvailableStockInsufficient() {
        InventoryOutboundOrder order = buildOutboundOrder();
        InventoryOutboundOrderLine line = buildOutboundLine(BigDecimal.valueOf(5));
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setBalanceId(1L);
        balance.setOnHandQty(BigDecimal.ONE);
        balance.setAvailableQty(BigDecimal.ONE);
        balance.setVersionNo(1);
        MdmWarehouse warehouse = new MdmWarehouse();
        warehouse.setAllowNegativeStock("N");
        when(stockTxnMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(stockBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balance);
        when(warehouseMapper.selectById(order.getWarehouseId())).thenReturn(warehouse);

        ServiceException exception = Assertions.assertThrows(ServiceException.class,
                () -> stockEngineSupport.applyOutbound(order, Collections.singletonList(line)));

        Assertions.assertEquals((int) ResultCode.CONFLICT.getCode(), exception.getCode());
        Assertions.assertTrue(exception.getMessage().contains("可用库存不足"));
    }

    /**
     * 验证允许负库存时会写入负数余额与流水。
     */
    @Test
    void shouldAllowOutboundWhenWarehouseSupportsNegativeStock() {
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        InventoryOutboundOrder order = buildOutboundOrder();
        InventoryOutboundOrderLine line = buildOutboundLine(BigDecimal.valueOf(3));
        MdmWarehouse warehouse = new MdmWarehouse();
        warehouse.setAllowNegativeStock("Y");
        when(stockTxnMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(stockBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(warehouseMapper.selectById(order.getWarehouseId())).thenReturn(warehouse);
        when(stockBalanceMapper.insert(any(InventoryStockBalance.class))).thenReturn(1);
        when(stockTxnMapper.insert(any(InventoryStockTxn.class))).thenReturn(1);

        Assertions.assertDoesNotThrow(() -> stockEngineSupport.applyOutbound(order, Collections.singletonList(line)));
        ArgumentCaptor<InventoryStockBalance> balanceCaptor = ArgumentCaptor.forClass(InventoryStockBalance.class);
        verify(stockBalanceMapper).insert(balanceCaptor.capture());
        Assertions.assertEquals(BigDecimal.valueOf(-3), balanceCaptor.getValue().getOnHandQty());
        Assertions.assertEquals(BigDecimal.valueOf(-3), balanceCaptor.getValue().getAvailableQty());
        verify(stockTxnMapper).insert(any(InventoryStockTxn.class));
    }

    /**
     * 验证空批次与空序列号会按空维度命中余额并完成正常出库。
     */
    @Test
    void shouldMatchNullBatchAndSerialBalanceWhenOutbound() {
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        InventoryOutboundOrder order = buildOutboundOrder();
        InventoryOutboundOrderLine line = buildOutboundLine(BigDecimal.valueOf(3));
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setBalanceId(1L);
        balance.setTenantId(order.getTenantId());
        balance.setOrgId(order.getOrgId());
        balance.setWarehouseId(order.getWarehouseId());
        balance.setItemId(line.getItemId());
        balance.setOnHandQty(BigDecimal.TEN);
        balance.setAvailableQty(BigDecimal.TEN);
        balance.setVersionNo(1);
        MdmWarehouse warehouse = new MdmWarehouse();
        warehouse.setAllowNegativeStock("N");
        when(stockTxnMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(stockBalanceMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(balance);
        when(stockBalanceMapper.update(any(InventoryStockBalance.class), any(LambdaUpdateWrapper.class))).thenReturn(1);
        when(stockTxnMapper.insert(any(InventoryStockTxn.class))).thenReturn(1);
        when(warehouseMapper.selectById(order.getWarehouseId())).thenReturn(warehouse);

        Assertions.assertDoesNotThrow(() -> stockEngineSupport.applyOutbound(order, Collections.singletonList(line)));

        ArgumentCaptor<LambdaQueryWrapper<InventoryStockBalance>> queryCaptor = ArgumentCaptor.forClass(LambdaQueryWrapper.class);
        ArgumentCaptor<InventoryStockBalance> updateCaptor = ArgumentCaptor.forClass(InventoryStockBalance.class);
        verify(stockBalanceMapper).selectOne(queryCaptor.capture());
        verify(stockBalanceMapper).update(updateCaptor.capture(), any(LambdaUpdateWrapper.class));
        String sqlSegment = queryCaptor.getValue().getSqlSegment().toUpperCase();
        Assertions.assertTrue(sqlSegment.contains("BATCH_NO IS NULL"));
        Assertions.assertTrue(sqlSegment.contains("SERIAL_NO IS NULL"));
        Assertions.assertEquals(BigDecimal.valueOf(7), updateCaptor.getValue().getOnHandQty());
        Assertions.assertEquals(BigDecimal.valueOf(7), updateCaptor.getValue().getAvailableQty());
        verify(stockTxnMapper).insert(any(InventoryStockTxn.class));
    }

    /**
     * 构造入库单。
     *
     * @return 入库单
     */
    private InventoryInboundOrder buildInboundOrder() {
        InventoryInboundOrder order = new InventoryInboundOrder();
        order.setOrderId(1L);
        order.setTenantId("TENANT_A");
        order.setOrgId(10L);
        order.setWarehouseId(20L);
        order.setBillNo("INB-001");
        order.setBillType("PURCHASE_INBOUND");
        order.setIdempotencyNo("IDEMP-001");
        return order;
    }

    /**
     * 构造入库单行。
     *
     * @param qty 数量
     * @return 入库单行
     */
    private InventoryInboundOrderLine buildInboundLine(BigDecimal qty) {
        InventoryInboundOrderLine line = new InventoryInboundOrderLine();
        line.setLineNo(1);
        line.setItemId(100L);
        line.setQty(qty);
        return line;
    }

    /**
     * 构造出库单。
     *
     * @return 出库单
     */
    private InventoryOutboundOrder buildOutboundOrder() {
        InventoryOutboundOrder order = new InventoryOutboundOrder();
        order.setOrderId(2L);
        order.setTenantId("TENANT_A");
        order.setOrgId(10L);
        order.setWarehouseId(20L);
        order.setBillNo("OUT-001");
        order.setBillType("SALES_OUTBOUND");
        order.setIdempotencyNo("IDEMP-OUT-001");
        return order;
    }

    /**
     * 构造出库单行。
     *
     * @param qty 数量
     * @return 出库单行
     */
    private InventoryOutboundOrderLine buildOutboundLine(BigDecimal qty) {
        InventoryOutboundOrderLine line = new InventoryOutboundOrderLine();
        line.setLineNo(1);
        line.setItemId(100L);
        line.setQty(qty);
        return line;
    }
}
