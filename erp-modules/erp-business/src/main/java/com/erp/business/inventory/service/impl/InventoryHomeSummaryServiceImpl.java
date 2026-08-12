package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import com.erp.business.inventory.domain.vo.InventoryInboundHomeSummaryVO;
import com.erp.business.inventory.mapper.InventoryInboundOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryInboundOrderMapper;
import com.erp.business.inventory.service.IInventoryHomeSummaryService;
import com.erp.business.inventory.support.InventoryBillStatusSupport;
import com.erp.business.security.service.PermissionService;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 库存首页汇总服务实现。
 */
@Service
public class InventoryHomeSummaryServiceImpl implements IInventoryHomeSummaryService {

    private final InventoryInboundOrderMapper orderMapper;
    private final InventoryInboundOrderLineMapper lineMapper;
    private final PermissionService permissionService;
    private final SecurityUserResolver securityUserResolver;

    public InventoryHomeSummaryServiceImpl(InventoryInboundOrderMapper orderMapper,
            InventoryInboundOrderLineMapper lineMapper,
            PermissionService permissionService,
            SecurityUserResolver securityUserResolver) {
        this.orderMapper = orderMapper;
        this.lineMapper = lineMapper;
        this.permissionService = permissionService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 构建入库首页汇总数据。
     *
     * @return 汇总数据
     */
    @Override
    public InventoryInboundHomeSummaryVO buildInboundSummary() {
        if (!permissionService.hasPermi("business:inventory:inbound:list")) {
            return emptySummary();
        }
        String tenantId = securityUserResolver.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return emptySummary();
        }
        Date now = new Date();
        Date monthStart = toDate(LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0));
        Date days30Ago = toDate(LocalDateTime.now().minusDays(30));
        List<InventoryInboundOrder> monthOrderList = orderMapper.selectList(new LambdaQueryWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getTenantId, tenantId.trim())
                .ge(InventoryInboundOrder::getCreateTime, monthStart)
                .le(InventoryInboundOrder::getCreateTime, now));
        BigDecimal currentMonthInboundQty = sumCompletedInboundQty(monthOrderList);
        Long pendingInboundOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getTenantId, tenantId.trim())
                .in(InventoryInboundOrder::getStatus,
                        InventoryBillStatusSupport.DRAFT,
                        InventoryBillStatusSupport.PENDING_APPROVAL,
                        InventoryBillStatusSupport.APPROVED,
                        InventoryBillStatusSupport.EXECUTING));
        List<InventoryInboundOrder> recentOrderList = orderMapper.selectList(new LambdaQueryWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getTenantId, tenantId.trim())
                .ge(InventoryInboundOrder::getCreateTime, days30Ago)
                .le(InventoryInboundOrder::getCreateTime, now));
        List<InventoryInboundOrder> safeRecentOrderList = recentOrderList == null ? Collections.emptyList() : recentOrderList;
        long completedCount30d = safeRecentOrderList.stream()
                .filter(order -> InventoryBillStatusSupport.COMPLETED.equals(order.getStatus()))
                .count();
        InventoryInboundHomeSummaryVO summaryVO = new InventoryInboundHomeSummaryVO();
        summaryVO.setCurrentMonthInboundQty(currentMonthInboundQty);
        summaryVO.setPendingInboundOrderCount(pendingInboundOrderCount == null ? 0L : pendingInboundOrderCount);
        summaryVO.setCompletionRate30d(calculateRate(completedCount30d, safeRecentOrderList.size()));
        return summaryVO;
    }

    /**
     * 汇总本月已完成入库单数量。
     *
     * @param monthOrderList 本月单据列表
     * @return 汇总数量
     */
    private BigDecimal sumCompletedInboundQty(List<InventoryInboundOrder> monthOrderList) {
        if (monthOrderList == null || monthOrderList.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        Set<Long> completedOrderIds = monthOrderList.stream()
                .filter(order -> InventoryBillStatusSupport.COMPLETED.equals(order.getStatus()))
                .map(InventoryInboundOrder::getOrderId)
                .filter(orderId -> orderId != null)
                .collect(Collectors.toSet());
        if (completedOrderIds.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        List<InventoryInboundOrderLine> lineList = lineMapper.selectList(new LambdaQueryWrapper<InventoryInboundOrderLine>()
                .in(InventoryInboundOrderLine::getOrderId, completedOrderIds));
        if (lineList == null || lineList.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return lineList.stream()
                .filter(line -> line != null
                        && line.getOrderId() != null
                        && completedOrderIds.contains(line.getOrderId()))
                .map(line -> line.getQty() == null ? BigDecimal.ZERO : line.getQty())
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 构建空安全汇总对象。
     *
     * @return 汇总对象
     */
    private InventoryInboundHomeSummaryVO emptySummary() {
        InventoryInboundHomeSummaryVO summaryVO = new InventoryInboundHomeSummaryVO();
        summaryVO.setCurrentMonthInboundQty(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        summaryVO.setPendingInboundOrderCount(0L);
        summaryVO.setCompletionRate30d(0D);
        return summaryVO;
    }

    /**
     * 计算百分比并保留两位小数。
     *
     * @param numerator 分子
     * @param denominator 分母
     * @return 百分比
     */
    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0L) {
            return 0D;
        }
        return BigDecimal.valueOf(numerator)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    /**
     * 将本地时间转换为 Date。
     *
     * @param localDateTime 本地时间
     * @return Date
     */
    private Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime.atZone(ZoneId.systemDefault()).toInstant());
    }
}
