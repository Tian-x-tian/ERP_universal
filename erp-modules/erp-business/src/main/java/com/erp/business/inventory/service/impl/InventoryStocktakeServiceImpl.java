package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.erp.business.inventory.domain.InventoryStocktakeOrder;
import com.erp.business.inventory.domain.InventoryStocktakeOrderLine;
import com.erp.business.inventory.mapper.InventoryStocktakeOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryStocktakeOrderMapper;
import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import com.erp.business.inventory.service.IInventoryStocktakeService;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 盘点服务实现。
 */
@Service
public class InventoryStocktakeServiceImpl extends AbstractInventoryOrderService<InventoryStocktakeOrder, InventoryStocktakeOrderLine>
        implements IInventoryStocktakeService {
    private static final String DEFAULT_BILL_TYPE = "INVENTORY_STOCKTAKE";

    private final InventoryStockEngineSupport stockEngineSupport;
    private final IInventoryIntegrationEventService integrationEventService;

    public InventoryStocktakeServiceImpl(InventoryStocktakeOrderMapper orderMapper,
            InventoryStocktakeOrderLineMapper lineMapper,
            InventoryWorkflowGateway workflowGateway,
            SecurityUserResolver securityUserResolver,
            InventoryStockEngineSupport stockEngineSupport,
            IInventoryIntegrationEventService integrationEventService) {
        super(orderMapper, lineMapper, workflowGateway, securityUserResolver);
        this.stockEngineSupport = stockEngineSupport;
        this.integrationEventService = integrationEventService;
    }

    /**
     * 校验盘点明细。
     *
     * @param line 盘点明细
     */
    @Override
    protected void validateLine(InventoryStocktakeOrderLine line) {
        if (line == null || line.getItemId() == null) {
            throw new IllegalArgumentException("盘点明细物料不能为空");
        }
        if (line.getCountedQty() == null && line.getDiffQty() == null && line.getSnapshotQty() == null) {
            throw new IllegalArgumentException("盘点明细至少维护盘点数、差异数或快照数之一");
        }
    }

    /**
     * 创建前设置盘点阶段。
     *
     * @param order 盘点单
     * @param now 当前时间
     * @param operator 操作人
     */
    @Override
    protected void prepareCreate(InventoryStocktakeOrder order, Date now, String operator) {
        if (order.getStocktakeStage() == null || order.getStocktakeStage().trim().isEmpty()) {
            order.setStocktakeStage("COUNTING");
        } else {
            order.setStocktakeStage(order.getStocktakeStage().trim().toUpperCase());
        }
    }

    /**
     * 修改前同步盘点阶段。
     *
     * @param updateEntity 更新实体
     * @param input 输入参数
     * @param existed 原始单据
     */
    @Override
    protected void prepareUpdate(InventoryStocktakeOrder updateEntity, InventoryStocktakeOrder input,
            InventoryStocktakeOrder existed) {
        updateEntity.setStocktakeStage(input.getStocktakeStage() == null ? existed.getStocktakeStage()
                : input.getStocktakeStage().trim().toUpperCase());
    }

    /**
     * 状态更新时同步盘点阶段。
     *
     * @param updateEntity 更新实体
     * @param existed 原始单据
     * @param status 目标状态
     */
    @Override
    protected void prepareStatusUpdate(InventoryStocktakeOrder updateEntity, InventoryStocktakeOrder existed, String status) {
        if ("COMPLETED".equals(status)) {
            updateEntity.setStocktakeStage("CONFIRMED");
            return;
        }
        if ("APPROVED".equals(status)) {
            updateEntity.setStocktakeStage("READY");
        }
    }

    /**
     * 执行盘点差异处理。
     *
     * @param order 盘点单头
     * @param lines 盘点单行
     */
    @Override
    protected void applyExecution(InventoryStocktakeOrder order, List<InventoryStocktakeOrderLine> lines) {
        stockEngineSupport.applyStocktake(order, lines);
    }

    /**
     * 执行完成后生成财务凭证事件。
     *
     * @param order 盘点单头
     * @param lines 盘点单行
     */
    @Override
    protected void afterCompleted(InventoryStocktakeOrder order, List<InventoryStocktakeOrderLine> lines) {
        integrationEventService.recordFinanceVoucherEvent("INVENTORY_STOCKTAKE", order.getOrderId(), order.getBillNo(),
                order.getBillType(), order.getOrderId(), order.getBillNo(),
                "{\"status\":\"CONFIRMED\",\"lineCount\":" + (lines == null ? 0 : lines.size()) + "}");
    }

    /**
     * 确认盘点差异。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    public boolean confirm(Long orderId) {
        return execute(orderId);
    }

    @Override
    protected InventoryStocktakeOrder newUpdateEntity() {
        return new InventoryStocktakeOrder();
    }

    @Override
    protected String getBillNoPrefix() {
        return "STK";
    }

    @Override
    protected String getDefaultBillType() {
        return DEFAULT_BILL_TYPE;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, Long> getOrderIdColumn() {
        return InventoryStocktakeOrder::getOrderId;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, String> getTenantColumn() {
        return InventoryStocktakeOrder::getTenantId;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, String> getBillNoColumn() {
        return InventoryStocktakeOrder::getBillNo;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, String> getStatusColumn() {
        return InventoryStocktakeOrder::getStatus;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, Integer> getVersionNoColumn() {
        return InventoryStocktakeOrder::getVersionNo;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, Date> getUpdateTimeColumn() {
        return InventoryStocktakeOrder::getUpdateTime;
    }

    @Override
    protected SFunction<InventoryStocktakeOrder, Date> getCreateTimeColumn() {
        return InventoryStocktakeOrder::getCreateTime;
    }

    @Override
    protected SFunction<InventoryStocktakeOrderLine, Long> getLineOrderIdColumn() {
        return InventoryStocktakeOrderLine::getOrderId;
    }

    @Override
    protected SFunction<InventoryStocktakeOrderLine, Integer> getLineNoColumn() {
        return InventoryStocktakeOrderLine::getLineNo;
    }
}
