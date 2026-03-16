package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.erp.business.inventory.domain.InventoryStockMoveOrder;
import com.erp.business.inventory.domain.InventoryStockMoveOrderLine;
import com.erp.business.inventory.mapper.InventoryStockMoveOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryStockMoveOrderMapper;
import com.erp.business.inventory.service.IInventoryStockMoveService;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 移库服务实现。
 */
@Service
public class InventoryStockMoveServiceImpl extends AbstractInventoryOrderService<InventoryStockMoveOrder, InventoryStockMoveOrderLine>
        implements IInventoryStockMoveService {
    private static final String DEFAULT_BILL_TYPE = "INVENTORY_MOVE";

    private final InventoryStockEngineSupport stockEngineSupport;

    public InventoryStockMoveServiceImpl(InventoryStockMoveOrderMapper orderMapper,
            InventoryStockMoveOrderLineMapper lineMapper,
            InventoryWorkflowGateway workflowGateway,
            SecurityUserResolver securityUserResolver,
            InventoryStockEngineSupport stockEngineSupport) {
        super(orderMapper, lineMapper, workflowGateway, securityUserResolver);
        this.stockEngineSupport = stockEngineSupport;
    }

    /**
     * 校验移库明细。
     *
     * @param line 移库明细
     */
    @Override
    protected void validateLine(InventoryStockMoveOrderLine line) {
        super.validateLine(line);
        if (line.getTargetAreaId() == null && line.getTargetLocationId() == null) {
            throw new IllegalArgumentException("移库明细目标库区或库位至少维护一项");
        }
    }

    /**
     * 执行移库库存逻辑。
     *
     * @param order 移库单头
     * @param lines 移库单行
     */
    @Override
    protected void applyExecution(InventoryStockMoveOrder order, List<InventoryStockMoveOrderLine> lines) {
        stockEngineSupport.applyMove(order, lines);
    }

    @Override
    protected InventoryStockMoveOrder newUpdateEntity() {
        return new InventoryStockMoveOrder();
    }

    @Override
    protected String getBillNoPrefix() {
        return "MOV";
    }

    @Override
    protected String getDefaultBillType() {
        return DEFAULT_BILL_TYPE;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, Long> getOrderIdColumn() {
        return InventoryStockMoveOrder::getOrderId;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, String> getTenantColumn() {
        return InventoryStockMoveOrder::getTenantId;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, String> getBillNoColumn() {
        return InventoryStockMoveOrder::getBillNo;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, String> getStatusColumn() {
        return InventoryStockMoveOrder::getStatus;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, Integer> getVersionNoColumn() {
        return InventoryStockMoveOrder::getVersionNo;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, Date> getUpdateTimeColumn() {
        return InventoryStockMoveOrder::getUpdateTime;
    }

    @Override
    protected SFunction<InventoryStockMoveOrder, Date> getCreateTimeColumn() {
        return InventoryStockMoveOrder::getCreateTime;
    }

    @Override
    protected SFunction<InventoryStockMoveOrderLine, Long> getLineOrderIdColumn() {
        return InventoryStockMoveOrderLine::getOrderId;
    }

    @Override
    protected SFunction<InventoryStockMoveOrderLine, Integer> getLineNoColumn() {
        return InventoryStockMoveOrderLine::getLineNo;
    }
}
