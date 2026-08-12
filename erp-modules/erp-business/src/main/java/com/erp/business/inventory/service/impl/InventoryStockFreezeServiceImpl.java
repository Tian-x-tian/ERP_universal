package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.erp.business.inventory.domain.InventoryStockFreezeOrder;
import com.erp.business.inventory.domain.InventoryStockFreezeOrderLine;
import com.erp.business.inventory.mapper.InventoryStockFreezeOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryStockFreezeOrderMapper;
import com.erp.business.inventory.service.IInventoryStockFreezeService;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 冻结解冻服务实现。
 */
@Service
public class InventoryStockFreezeServiceImpl extends AbstractInventoryOrderService<InventoryStockFreezeOrder, InventoryStockFreezeOrderLine>
        implements IInventoryStockFreezeService {
    private static final String DEFAULT_BILL_TYPE = "INVENTORY_FREEZE";

    private final InventoryStockEngineSupport stockEngineSupport;

    public InventoryStockFreezeServiceImpl(InventoryStockFreezeOrderMapper orderMapper,
            InventoryStockFreezeOrderLineMapper lineMapper,
            InventoryWorkflowGateway workflowGateway,
            SecurityUserResolver securityUserResolver,
            InventoryStockEngineSupport stockEngineSupport) {
        super(orderMapper, lineMapper, workflowGateway, securityUserResolver);
        this.stockEngineSupport = stockEngineSupport;
    }

    /**
     * 校验冻结解冻单。
     *
     * @param order 冻结解冻单
     */
    @Override
    protected void validateOrderForSave(InventoryStockFreezeOrder order) {
        super.validateOrderForSave(order);
        if (!StringUtils.hasText(order.getOperationType())) {
            throw new IllegalArgumentException("操作类型不能为空");
        }
        String operationType = order.getOperationType().trim().toUpperCase();
        if (!"FREEZE".equals(operationType) && !"UNFREEZE".equals(operationType)) {
            throw new IllegalArgumentException("操作类型仅支持 FREEZE 或 UNFREEZE");
        }
    }

    /**
     * 创建前规范化操作类型。
     *
     * @param order 单据对象
     * @param now 当前时间
     * @param operator 操作人
     */
    @Override
    protected void prepareCreate(InventoryStockFreezeOrder order, Date now, String operator) {
        order.setOperationType(order.getOperationType().trim().toUpperCase());
    }

    /**
     * 修改前同步扩展字段。
     *
     * @param updateEntity 更新实体
     * @param input 输入参数
     * @param existed 原始单据
     */
    @Override
    protected void prepareUpdate(InventoryStockFreezeOrder updateEntity, InventoryStockFreezeOrder input,
            InventoryStockFreezeOrder existed) {
        updateEntity.setOperationType(input.getOperationType() == null ? null : input.getOperationType().trim().toUpperCase());
    }

    /**
     * 执行冻结解冻库存逻辑。
     *
     * @param order 冻结解冻单头
     * @param lines 冻结解冻单行
     */
    @Override
    protected void applyExecution(InventoryStockFreezeOrder order, List<InventoryStockFreezeOrderLine> lines) {
        stockEngineSupport.applyFreeze(order, lines);
    }

    @Override
    protected InventoryStockFreezeOrder newUpdateEntity() {
        return new InventoryStockFreezeOrder();
    }

    @Override
    protected String getBillNoPrefix() {
        return "FRZ";
    }

    @Override
    protected String getDefaultBillType() {
        return DEFAULT_BILL_TYPE;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, Long> getOrderIdColumn() {
        return InventoryStockFreezeOrder::getOrderId;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, String> getTenantColumn() {
        return InventoryStockFreezeOrder::getTenantId;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, String> getBillNoColumn() {
        return InventoryStockFreezeOrder::getBillNo;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, String> getStatusColumn() {
        return InventoryStockFreezeOrder::getStatus;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, Integer> getVersionNoColumn() {
        return InventoryStockFreezeOrder::getVersionNo;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, Date> getUpdateTimeColumn() {
        return InventoryStockFreezeOrder::getUpdateTime;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrder, Date> getCreateTimeColumn() {
        return InventoryStockFreezeOrder::getCreateTime;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrderLine, Long> getLineOrderIdColumn() {
        return InventoryStockFreezeOrderLine::getOrderId;
    }

    @Override
    protected SFunction<InventoryStockFreezeOrderLine, Integer> getLineNoColumn() {
        return InventoryStockFreezeOrderLine::getLineNo;
    }
}
