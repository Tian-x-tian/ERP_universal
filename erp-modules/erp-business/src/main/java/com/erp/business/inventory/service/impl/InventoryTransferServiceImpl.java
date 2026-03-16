package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.erp.business.inventory.domain.InventoryTransferOrder;
import com.erp.business.inventory.domain.InventoryTransferOrderLine;
import com.erp.business.inventory.mapper.InventoryTransferOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryTransferOrderMapper;
import com.erp.business.inventory.service.IInventoryTransferService;
import com.erp.business.inventory.service.InventorySourceProgressCallback;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * 调拨服务实现。
 */
@Service
public class InventoryTransferServiceImpl extends AbstractInventoryOrderService<InventoryTransferOrder, InventoryTransferOrderLine>
        implements IInventoryTransferService {
    private static final String DEFAULT_BILL_TYPE = "INVENTORY_TRANSFER";

    private final InventoryStockEngineSupport stockEngineSupport;
    private final InventorySourceProgressCallback sourceProgressCallback;

    public InventoryTransferServiceImpl(InventoryTransferOrderMapper orderMapper,
            InventoryTransferOrderLineMapper lineMapper,
            InventoryWorkflowGateway workflowGateway,
            SecurityUserResolver securityUserResolver,
            InventoryStockEngineSupport stockEngineSupport,
            InventorySourceProgressCallback sourceProgressCallback) {
        super(orderMapper, lineMapper, workflowGateway, securityUserResolver);
        this.stockEngineSupport = stockEngineSupport;
        this.sourceProgressCallback = sourceProgressCallback;
    }

    /**
     * 校验调拨单保存参数。
     *
     * @param order 调拨单
     */
    @Override
    protected void validateOrderForSave(InventoryTransferOrder order) {
        super.validateOrderForSave(order);
        if (order.getTargetWarehouseId() == null) {
            throw new IllegalArgumentException("目标仓库ID不能为空");
        }
    }

    /**
     * 校验调拨明细。
     *
     * @param line 调拨明细
     */
    @Override
    protected void validateLine(InventoryTransferOrderLine line) {
        super.validateLine(line);
        if (line.getTargetAreaId() == null && line.getTargetLocationId() == null) {
            throw new IllegalArgumentException("调拨明细目标库区或库位至少维护一项");
        }
    }

    /**
     * 修改前同步扩展字段。
     *
     * @param updateEntity 更新实体
     * @param input 输入参数
     * @param existed 原始单据
     */
    @Override
    protected void prepareUpdate(InventoryTransferOrder updateEntity, InventoryTransferOrder input,
            InventoryTransferOrder existed) {
        updateEntity.setTargetWarehouseId(input.getTargetWarehouseId());
    }

    /**
     * 执行调拨库存逻辑。
     *
     * @param order 调拨单头
     * @param lines 调拨单行
     */
    @Override
    protected void applyExecution(InventoryTransferOrder order, List<InventoryTransferOrderLine> lines) {
        stockEngineSupport.applyTransfer(order, lines);
    }

    /**
     * 执行完成后回写来源单进度。
     *
     * @param order 调拨单头
     * @param lines 调拨单行
     */
    @Override
    protected void afterCompleted(InventoryTransferOrder order, List<InventoryTransferOrderLine> lines) {
        sourceProgressCallback.callback(order.getSourceOrderType(), order.getSourceOrderId(),
                order.getSourceOrderNo(), order.getBillNo(), "COMPLETED");
    }

    @Override
    protected InventoryTransferOrder newUpdateEntity() {
        return new InventoryTransferOrder();
    }

    @Override
    protected String getBillNoPrefix() {
        return "TRF";
    }

    @Override
    protected String getDefaultBillType() {
        return DEFAULT_BILL_TYPE;
    }

    @Override
    protected SFunction<InventoryTransferOrder, Long> getOrderIdColumn() {
        return InventoryTransferOrder::getOrderId;
    }

    @Override
    protected SFunction<InventoryTransferOrder, String> getTenantColumn() {
        return InventoryTransferOrder::getTenantId;
    }

    @Override
    protected SFunction<InventoryTransferOrder, String> getBillNoColumn() {
        return InventoryTransferOrder::getBillNo;
    }

    @Override
    protected SFunction<InventoryTransferOrder, String> getStatusColumn() {
        return InventoryTransferOrder::getStatus;
    }

    @Override
    protected SFunction<InventoryTransferOrder, Integer> getVersionNoColumn() {
        return InventoryTransferOrder::getVersionNo;
    }

    @Override
    protected SFunction<InventoryTransferOrder, Date> getUpdateTimeColumn() {
        return InventoryTransferOrder::getUpdateTime;
    }

    @Override
    protected SFunction<InventoryTransferOrder, Date> getCreateTimeColumn() {
        return InventoryTransferOrder::getCreateTime;
    }

    @Override
    protected SFunction<InventoryTransferOrderLine, Long> getLineOrderIdColumn() {
        return InventoryTransferOrderLine::getOrderId;
    }

    @Override
    protected SFunction<InventoryTransferOrderLine, Integer> getLineNoColumn() {
        return InventoryTransferOrderLine::getLineNo;
    }
}
