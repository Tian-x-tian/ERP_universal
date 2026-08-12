package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.erp.business.inventory.domain.InventoryStockAdjustOrder;
import com.erp.business.inventory.domain.InventoryStockAdjustOrderLine;
import com.erp.business.inventory.mapper.InventoryStockAdjustOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryStockAdjustOrderMapper;
import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import com.erp.business.inventory.service.IInventoryStockAdjustService;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 库存调整服务实现。
 */
@Service
public class InventoryStockAdjustServiceImpl extends AbstractInventoryOrderService<InventoryStockAdjustOrder, InventoryStockAdjustOrderLine>
        implements IInventoryStockAdjustService {
    private static final String DEFAULT_BILL_TYPE = "INVENTORY_ADJUST";

    private final InventoryStockEngineSupport stockEngineSupport;
    private final IInventoryIntegrationEventService integrationEventService;

    public InventoryStockAdjustServiceImpl(InventoryStockAdjustOrderMapper orderMapper,
            InventoryStockAdjustOrderLineMapper lineMapper,
            InventoryWorkflowGateway workflowGateway,
            SecurityUserResolver securityUserResolver,
            InventoryStockEngineSupport stockEngineSupport,
            IInventoryIntegrationEventService integrationEventService) {
        super(orderMapper, lineMapper, workflowGateway, securityUserResolver);
        this.stockEngineSupport = stockEngineSupport;
        this.integrationEventService = integrationEventService;
    }

    /**
     * 校验调整明细。
     *
     * @param line 调整明细
     */
    @Override
    protected void validateLine(InventoryStockAdjustOrderLine line) {
        super.validateLine(line);
        if (!StringUtils.hasText(line.getAdjustType())) {
            throw new IllegalArgumentException("调整类型不能为空");
        }
        String adjustType = line.getAdjustType().trim().toUpperCase();
        if (!"GAIN".equals(adjustType) && !"LOSS".equals(adjustType)) {
            throw new IllegalArgumentException("调整类型仅支持 GAIN 或 LOSS");
        }
    }

    /**
     * 保存前规范化调整类型。
     *
     * @param line 调整明细
     */
    @Override
    protected void prepareLineForSave(InventoryStockAdjustOrderLine line) {
        line.setAdjustType(line.getAdjustType().trim().toUpperCase());
    }

    /**
     * 执行库存调整逻辑。
     *
     * @param order 调整单头
     * @param lines 调整单行
     */
    @Override
    protected void applyExecution(InventoryStockAdjustOrder order, List<InventoryStockAdjustOrderLine> lines) {
        stockEngineSupport.applyAdjust(order, lines);
    }

    /**
     * 执行完成后生成财务凭证事件。
     *
     * @param order 调整单头
     * @param lines 调整单行
     */
    @Override
    protected void afterCompleted(InventoryStockAdjustOrder order, List<InventoryStockAdjustOrderLine> lines) {
        integrationEventService.recordFinanceVoucherEvent("INVENTORY_ADJUST", order.getOrderId(), order.getBillNo(),
                order.getBillType(), order.getOrderId(), order.getBillNo(),
                "{\"status\":\"COMPLETED\",\"lineCount\":" + (lines == null ? 0 : lines.size()) + "}");
    }

    @Override
    protected InventoryStockAdjustOrder newUpdateEntity() {
        return new InventoryStockAdjustOrder();
    }

    @Override
    protected String getBillNoPrefix() {
        return "ADJ";
    }

    @Override
    protected String getDefaultBillType() {
        return DEFAULT_BILL_TYPE;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, Long> getOrderIdColumn() {
        return InventoryStockAdjustOrder::getOrderId;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, String> getTenantColumn() {
        return InventoryStockAdjustOrder::getTenantId;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, String> getBillNoColumn() {
        return InventoryStockAdjustOrder::getBillNo;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, String> getStatusColumn() {
        return InventoryStockAdjustOrder::getStatus;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, Integer> getVersionNoColumn() {
        return InventoryStockAdjustOrder::getVersionNo;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, Date> getUpdateTimeColumn() {
        return InventoryStockAdjustOrder::getUpdateTime;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrder, Date> getCreateTimeColumn() {
        return InventoryStockAdjustOrder::getCreateTime;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrderLine, Long> getLineOrderIdColumn() {
        return InventoryStockAdjustOrderLine::getOrderId;
    }

    @Override
    protected SFunction<InventoryStockAdjustOrderLine, Integer> getLineNoColumn() {
        return InventoryStockAdjustOrderLine::getLineNo;
    }
}
