package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import com.erp.business.inventory.domain.vo.InventoryInboundHomeSummaryVO;
import com.erp.business.inventory.mapper.InventoryInboundOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryInboundOrderMapper;
import com.erp.business.inventory.support.InventoryBillStatusSupport;
import com.erp.business.security.service.PermissionService;
import com.erp.business.security.service.SecurityUserResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 入库首页汇总服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class InventoryHomeSummaryServiceImplTest {

    @Mock
    private InventoryInboundOrderMapper orderMapper;

    @Mock
    private InventoryInboundOrderLineMapper lineMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    /**
     * 验证具备权限时会正确计算首页入库汇总。
     */
    @Test
    void shouldBuildInboundSummaryWhenPermissionGranted() {
        when(permissionService.hasPermi("business:inventory:inbound:list")).thenReturn(true);
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");
        when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(
                buildOrder(1L, InventoryBillStatusSupport.COMPLETED),
                buildOrder(2L, InventoryBillStatusSupport.PENDING_APPROVAL),
                buildOrder(3L, InventoryBillStatusSupport.COMPLETED),
                buildOrder(4L, InventoryBillStatusSupport.CANCELLED)));
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        when(lineMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Arrays.asList(
                buildLine(1L, "15.50"),
                buildLine(3L, "4.50"),
                buildLine(2L, "99.99")));

        InventoryHomeSummaryServiceImpl service = new InventoryHomeSummaryServiceImpl(
                orderMapper,
                lineMapper,
                permissionService,
                securityUserResolver);

        InventoryInboundHomeSummaryVO summary = service.buildInboundSummary();

        Assertions.assertEquals(new BigDecimal("20.00"), summary.getCurrentMonthInboundQty());
        Assertions.assertEquals(1L, summary.getPendingInboundOrderCount());
        Assertions.assertEquals(50.00D, summary.getCompletionRate30d());
    }

    /**
     * 验证无权限时返回空安全值。
     */
    @Test
    void shouldReturnSafeSummaryWhenNoPermission() {
        when(permissionService.hasPermi("business:inventory:inbound:list")).thenReturn(false);

        InventoryHomeSummaryServiceImpl service = new InventoryHomeSummaryServiceImpl(
                orderMapper,
                lineMapper,
                permissionService,
                securityUserResolver);

        InventoryInboundHomeSummaryVO summary = service.buildInboundSummary();

        Assertions.assertEquals(BigDecimal.ZERO.setScale(2), summary.getCurrentMonthInboundQty());
        Assertions.assertEquals(0L, summary.getPendingInboundOrderCount());
        Assertions.assertEquals(0D, summary.getCompletionRate30d());
    }

    /**
     * 验证近30天无数据时完成率安全降级为0。
     */
    @Test
    void shouldReturnZeroCompletionRateWhenNoRecentOrders() {
        when(permissionService.hasPermi("business:inventory:inbound:list")).thenReturn(true);
        when(securityUserResolver.getCurrentTenantId()).thenReturn("TENANT_A");
        when(orderMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
        when(orderMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);

        InventoryHomeSummaryServiceImpl service = new InventoryHomeSummaryServiceImpl(
                orderMapper,
                lineMapper,
                permissionService,
                securityUserResolver);

        InventoryInboundHomeSummaryVO summary = service.buildInboundSummary();

        Assertions.assertEquals(BigDecimal.ZERO.setScale(2), summary.getCurrentMonthInboundQty());
        Assertions.assertEquals(0L, summary.getPendingInboundOrderCount());
        Assertions.assertEquals(0D, summary.getCompletionRate30d());
    }

    /**
     * 构造入库单对象。
     *
     * @param orderId 单据ID
     * @param status 单据状态
     * @return 入库单对象
     */
    private InventoryInboundOrder buildOrder(Long orderId, String status) {
        InventoryInboundOrder order = new InventoryInboundOrder();
        order.setOrderId(orderId);
        order.setStatus(status);
        return order;
    }

    /**
     * 构造入库单行对象。
     *
     * @param orderId 单据ID
     * @param qty 数量
     * @return 入库单行对象
     */
    private InventoryInboundOrderLine buildLine(Long orderId, String qty) {
        InventoryInboundOrderLine line = new InventoryInboundOrderLine();
        line.setOrderId(orderId);
        line.setQty(new BigDecimal(qty));
        return line;
    }
}
