package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import com.erp.business.inventory.domain.InventoryBatchRecord;
import com.erp.business.inventory.domain.InventoryOutboundOrder;
import com.erp.business.inventory.domain.InventoryOutboundOrderLine;
import com.erp.business.inventory.domain.InventorySerialRecord;
import com.erp.business.inventory.domain.InventoryStockAdjustOrder;
import com.erp.business.inventory.domain.InventoryStockAdjustOrderLine;
import com.erp.business.inventory.domain.InventoryStockFreezeOrder;
import com.erp.business.inventory.domain.InventoryStockFreezeOrderLine;
import com.erp.business.inventory.domain.InventoryStockMoveOrder;
import com.erp.business.inventory.domain.InventoryStockMoveOrderLine;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.domain.InventoryStocktakeOrder;
import com.erp.business.inventory.domain.InventoryStocktakeOrderLine;
import com.erp.business.inventory.domain.InventoryTransferOrder;
import com.erp.business.inventory.domain.InventoryTransferOrderLine;
import com.erp.business.inventory.domain.MdmItem;
import com.erp.business.inventory.domain.MdmWarehouse;
import com.erp.business.inventory.mapper.InventoryBatchRecordMapper;
import com.erp.business.inventory.mapper.InventorySerialRecordMapper;
import com.erp.business.inventory.mapper.InventoryStockBalanceMapper;
import com.erp.business.inventory.mapper.InventoryStockTxnMapper;
import com.erp.business.inventory.mapper.MdmItemMapper;
import com.erp.business.inventory.mapper.MdmWarehouseMapper;
import com.erp.business.inventory.support.InventoryActionTypeSupport;
import com.erp.business.inventory.support.InventoryValueSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.RequestTraceContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * 库存变动执行支持组件。
 */
@Component
public class InventoryStockEngineSupport {

    private final InventoryStockBalanceMapper stockBalanceMapper;
    private final InventoryStockTxnMapper stockTxnMapper;
    private final MdmWarehouseMapper warehouseMapper;
    private final MdmItemMapper itemMapper;
    private final InventoryBatchRecordMapper batchRecordMapper;
    private final InventorySerialRecordMapper serialRecordMapper;
    private final SecurityUserResolver securityUserResolver;

    public InventoryStockEngineSupport(InventoryStockBalanceMapper stockBalanceMapper,
            InventoryStockTxnMapper stockTxnMapper,
            MdmWarehouseMapper warehouseMapper,
            MdmItemMapper itemMapper,
            InventoryBatchRecordMapper batchRecordMapper,
            InventorySerialRecordMapper serialRecordMapper,
            SecurityUserResolver securityUserResolver) {
        this.stockBalanceMapper = stockBalanceMapper;
        this.stockTxnMapper = stockTxnMapper;
        this.warehouseMapper = warehouseMapper;
        this.itemMapper = itemMapper;
        this.batchRecordMapper = batchRecordMapper;
        this.serialRecordMapper = serialRecordMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 执行入库库存变更。
     *
     * @param order 入库单头
     * @param lines 入库单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyInbound(InventoryInboundOrder order, List<InventoryInboundOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("入库执行数据不能为空");
        }
        for (InventoryInboundOrderLine line : lines) {
            applyInboundLine(order, line);
        }
    }

    /**
     * 执行出库库存变更。
     *
     * @param order 出库单头
     * @param lines 出库单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyOutbound(InventoryOutboundOrder order, List<InventoryOutboundOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("出库执行数据不能为空");
        }
        for (InventoryOutboundOrderLine line : lines) {
            applyOutboundLine(order, line);
        }
    }

    /**
     * 执行调拨库存变更。
     *
     * @param order 调拨单头
     * @param lines 调拨单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyTransfer(InventoryTransferOrder order, List<InventoryTransferOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("调拨执行数据不能为空");
        }
        for (InventoryTransferOrderLine line : lines) {
            validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    line.getProductionDate(), line.getExpiryDate());
            applyDecrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                    line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    validateChangeQty(line.getQty()), order.getIdempotencyNo(), line.getLineNo(),
                    InventoryActionTypeSupport.TRANSFER_OUT, order.getBillType(), order.getOrderId(),
                    order.getBillNo(), true, null, null);
            applyIncrease(order.getTenantId(), order.getOrgId(), order.getTargetWarehouseId(),
                    line.getTargetAreaId(), line.getTargetLocationId(), line.getItemId(), line.getBatchNo(),
                    line.getSerialNo(), validateChangeQty(line.getQty()), order.getIdempotencyNo(), line.getLineNo(),
                    InventoryActionTypeSupport.TRANSFER_IN, order.getBillType(), order.getOrderId(),
                    order.getBillNo(), line.getProductionDate(), line.getExpiryDate(), line.getAreaId(),
                    line.getLocationId(), line.getTargetAreaId(), line.getTargetLocationId());
        }
    }

    /**
     * 执行移库库存变更。
     *
     * @param order 移库单头
     * @param lines 移库单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyMove(InventoryStockMoveOrder order, List<InventoryStockMoveOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("移库执行数据不能为空");
        }
        for (InventoryStockMoveOrderLine line : lines) {
            validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    line.getProductionDate(), line.getExpiryDate());
            applyDecrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                    line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    validateChangeQty(line.getQty()), order.getIdempotencyNo(), line.getLineNo(),
                    InventoryActionTypeSupport.MOVE, order.getBillType(), order.getOrderId(), order.getBillNo(),
                    true, line.getTargetAreaId(), line.getTargetLocationId());
            applyIncrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getTargetAreaId(),
                    line.getTargetLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    validateChangeQty(line.getQty()), order.getIdempotencyNo(), line.getLineNo() * 1000,
                    InventoryActionTypeSupport.MOVE, order.getBillType(), order.getOrderId(), order.getBillNo(),
                    line.getProductionDate(), line.getExpiryDate(), line.getAreaId(), line.getLocationId(),
                    line.getTargetAreaId(), line.getTargetLocationId());
        }
    }

    /**
     * 执行冻结或解冻。
     *
     * @param order 冻结解冻单头
     * @param lines 冻结解冻单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyFreeze(InventoryStockFreezeOrder order, List<InventoryStockFreezeOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("冻结解冻执行数据不能为空");
        }
        boolean freeze = "FREEZE".equalsIgnoreCase(order.getOperationType());
        for (InventoryStockFreezeOrderLine line : lines) {
            validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    line.getProductionDate(), line.getExpiryDate());
            BigDecimal changeQty = validateChangeQty(line.getQty());
            InventoryStockBalance balance = requireBalance(order.getTenantId(), order.getOrgId(), order.getWarehouseId(),
                    line.getAreaId(), line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo());
            BigDecimal beforeOnHand = InventoryValueSupport.defaultQty(balance.getOnHandQty());
            BigDecimal beforeAvailable = InventoryValueSupport.defaultQty(balance.getAvailableQty());
            BigDecimal beforeFrozen = InventoryValueSupport.defaultQty(balance.getFrozenQty());
            if (freeze && beforeAvailable.compareTo(changeQty) < 0) {
                throw new ServiceException("可用库存不足，无法冻结", (int) ResultCode.CONFLICT.getCode());
            }
            if (!freeze && beforeFrozen.compareTo(changeQty) < 0) {
                throw new ServiceException("冻结库存不足，无法解冻", (int) ResultCode.CONFLICT.getCode());
            }
            BigDecimal afterAvailable = freeze ? beforeAvailable.subtract(changeQty) : beforeAvailable.add(changeQty);
            BigDecimal afterFrozen = freeze ? beforeFrozen.add(changeQty) : beforeFrozen.subtract(changeQty);
            updateBalance(balance, beforeOnHand, afterAvailable, afterFrozen, InventoryValueSupport.defaultQty(balance.getInTransitQty()));
            insertTxn(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(), line.getLocationId(),
                    line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    freeze ? InventoryActionTypeSupport.FREEZE : InventoryActionTypeSupport.UNFREEZE,
                    order.getBillType(), order.getOrderId(), order.getBillNo(), line.getLineNo(),
                    order.getIdempotencyNo(), beforeOnHand, beforeOnHand, beforeAvailable, afterAvailable,
                    freeze ? changeQty.negate() : changeQty, line.getAreaId(), line.getLocationId(),
                    line.getAreaId(), line.getLocationId());
        }
    }

    /**
     * 执行库存调整。
     *
     * @param order 调整单头
     * @param lines 调整单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyAdjust(InventoryStockAdjustOrder order, List<InventoryStockAdjustOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("库存调整执行数据不能为空");
        }
        for (InventoryStockAdjustOrderLine line : lines) {
            validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    line.getProductionDate(), line.getExpiryDate());
            boolean gain = !"LOSS".equalsIgnoreCase(line.getAdjustType());
            if (gain) {
                applyIncrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                        line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                        validateChangeQty(line.getQty()), order.getIdempotencyNo(), line.getLineNo(),
                        InventoryActionTypeSupport.ADJUST_GAIN, order.getBillType(), order.getOrderId(),
                        order.getBillNo(), line.getProductionDate(), line.getExpiryDate(),
                        line.getAreaId(), line.getLocationId(), line.getAreaId(), line.getLocationId());
                continue;
            }
            applyDecrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                    line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    validateChangeQty(line.getQty()), order.getIdempotencyNo(), line.getLineNo(),
                    InventoryActionTypeSupport.ADJUST_LOSS, order.getBillType(), order.getOrderId(),
                    order.getBillNo(), true, line.getAreaId(), line.getLocationId());
        }
    }

    /**
     * 执行盘点差异处理。
     *
     * @param order 盘点单头
     * @param lines 盘点单行
     */
    @Transactional(rollbackFor = Exception.class)
    public void applyStocktake(InventoryStocktakeOrder order, List<InventoryStocktakeOrderLine> lines) {
        if (order == null || order.getOrderId() == null || lines == null || lines.isEmpty()) {
            throw new IllegalArgumentException("盘点执行数据不能为空");
        }
        for (InventoryStocktakeOrderLine line : lines) {
            validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                    line.getProductionDate(), line.getExpiryDate());
            BigDecimal snapshotQty = InventoryValueSupport.defaultQty(line.getSnapshotQty());
            if (snapshotQty.compareTo(BigDecimal.ZERO) <= 0) {
                InventoryStockBalance balance = findBalance(order.getTenantId(), order.getOrgId(), order.getWarehouseId(),
                        line.getAreaId(), line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo());
                snapshotQty = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getOnHandQty());
            }
            BigDecimal countedQty = InventoryValueSupport.defaultQty(line.getCountedQty());
            BigDecimal diffQty = InventoryValueSupport.defaultQty(line.getDiffQty());
            if (diffQty.compareTo(BigDecimal.ZERO) == 0) {
                diffQty = countedQty.subtract(snapshotQty);
            }
            if (diffQty.compareTo(BigDecimal.ZERO) > 0) {
                applyIncrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                        line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(), diffQty,
                        order.getIdempotencyNo(), line.getLineNo(), InventoryActionTypeSupport.STOCKTAKE_GAIN,
                        order.getBillType(), order.getOrderId(), order.getBillNo(), line.getProductionDate(),
                        line.getExpiryDate(), line.getAreaId(), line.getLocationId(), line.getAreaId(), line.getLocationId());
                continue;
            }
            if (diffQty.compareTo(BigDecimal.ZERO) < 0) {
                applyDecrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                        line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                        diffQty.abs(), order.getIdempotencyNo(), line.getLineNo(),
                        InventoryActionTypeSupport.STOCKTAKE_LOSS, order.getBillType(), order.getOrderId(),
                        order.getBillNo(), true, line.getAreaId(), line.getLocationId());
            }
        }
    }

    /**
     * 执行单行入库。
     *
     * @param order 入库单头
     * @param line 入库单行
     */
    private void applyInboundLine(InventoryInboundOrder order, InventoryInboundOrderLine line) {
        BigDecimal changeQty = validateChangeQty(line == null ? null : line.getQty());
        if (isProcessed(order.getTenantId(), order.getIdempotencyNo(), line.getLineNo(), InventoryActionTypeSupport.INBOUND)) {
            return;
        }
        validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                line.getProductionDate(), line.getExpiryDate());
        applyIncrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(), changeQty,
                order.getIdempotencyNo(), line.getLineNo(), InventoryActionTypeSupport.INBOUND, order.getBillType(),
                order.getOrderId(), order.getBillNo(), line.getProductionDate(), line.getExpiryDate(),
                line.getAreaId(), line.getLocationId(), line.getAreaId(), line.getLocationId());
    }

    /**
     * 执行单行出库。
     *
     * @param order 出库单头
     * @param line 出库单行
     */
    private void applyOutboundLine(InventoryOutboundOrder order, InventoryOutboundOrderLine line) {
        BigDecimal changeQty = validateChangeQty(line == null ? null : line.getQty());
        if (isProcessed(order.getTenantId(), order.getIdempotencyNo(), line.getLineNo(), InventoryActionTypeSupport.OUTBOUND)) {
            return;
        }
        validateInventoryControls(line.getItemId(), line.getBatchNo(), line.getSerialNo(),
                line.getProductionDate(), line.getExpiryDate());
        applyDecrease(order.getTenantId(), order.getOrgId(), order.getWarehouseId(), line.getAreaId(),
                line.getLocationId(), line.getItemId(), line.getBatchNo(), line.getSerialNo(), changeQty,
                order.getIdempotencyNo(), line.getLineNo(), InventoryActionTypeSupport.OUTBOUND, order.getBillType(),
                order.getOrderId(), order.getBillNo(), true, line.getAreaId(), line.getLocationId());
    }

    /**
     * 按库存维度查询余额记录。
     *
     * @param tenantId 租户编号
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param areaId 库区ID
     * @param locationId 库位ID
     * @param itemId 物料ID
     * @param batchNo 批次号
     * @param serialNo 序列号
     * @return 余额对象
     */
    private InventoryStockBalance findBalance(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId,
            Long itemId, String batchNo, String serialNo) {
        LambdaQueryWrapper<InventoryStockBalance> queryWrapper = new LambdaQueryWrapper<InventoryStockBalance>()
                .eq(InventoryStockBalance::getTenantId, tenantId)
                .eq(InventoryStockBalance::getOrgId, orgId)
                .eq(InventoryStockBalance::getWarehouseId, warehouseId)
                .eq(InventoryStockBalance::getItemId, itemId);
        appendNullableDimension(queryWrapper, InventoryStockBalance::getAreaId, areaId);
        appendNullableDimension(queryWrapper, InventoryStockBalance::getLocationId, locationId);
        appendNullableDimension(queryWrapper, InventoryStockBalance::getBatchNo, InventoryValueSupport.trimToNull(batchNo));
        appendNullableDimension(queryWrapper, InventoryStockBalance::getSerialNo, InventoryValueSupport.trimToNull(serialNo));
        return stockBalanceMapper.selectOne(queryWrapper);
    }

    /**
     * 为可空库存维度追加查询条件。
     *
     * @param queryWrapper 查询条件
     * @param column 列引用
     * @param value 维度值
     * @param <T> 维度类型
     */
    private <T> void appendNullableDimension(LambdaQueryWrapper<InventoryStockBalance> queryWrapper,
            com.baomidou.mybatisplus.core.toolkit.support.SFunction<InventoryStockBalance, T> column, T value) {
        if (value == null) {
            queryWrapper.isNull(column);
            return;
        }
        queryWrapper.eq(column, value);
    }

    /**
     * 执行库存增加。
     */
    private void applyIncrease(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId,
            Long itemId, String batchNo, String serialNo, BigDecimal changeQty, String idempotencyNo, Integer lineNo,
            String actionType, String billType, Long billId, String billNo, Date productionDate, Date expiryDate,
            Long fromAreaId, Long fromLocationId, Long toAreaId, Long toLocationId) {
        if (isProcessed(tenantId, idempotencyNo, lineNo, actionType)) {
            return;
        }
        InventoryStockBalance balance = findBalance(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo);
        BigDecimal beforeOnHand = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getOnHandQty());
        BigDecimal beforeAvailable = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getAvailableQty());
        BigDecimal beforeFrozen = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getFrozenQty());
        BigDecimal beforeInTransit = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getInTransitQty());
        BigDecimal afterOnHand = beforeOnHand.add(changeQty);
        BigDecimal afterAvailable = beforeAvailable.add(changeQty);
        if (balance == null) {
            createBalance(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo,
                    afterOnHand, afterAvailable, beforeFrozen, beforeInTransit);
        } else {
            updateBalance(balance, afterOnHand, afterAvailable, beforeFrozen, beforeInTransit);
        }
        upsertBatchRecord(tenantId, orgId, warehouseId, itemId, batchNo, productionDate, expiryDate, changeQty);
        upsertSerialRecord(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo,
                productionDate, expiryDate, afterOnHand);
        insertTxn(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo, actionType, billType,
                billId, billNo, lineNo, idempotencyNo, beforeOnHand, afterOnHand, beforeAvailable, afterAvailable,
                changeQty, fromAreaId, fromLocationId, toAreaId, toLocationId);
    }

    /**
     * 执行库存减少。
     */
    private void applyDecrease(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId,
            Long itemId, String batchNo, String serialNo, BigDecimal changeQty, String idempotencyNo, Integer lineNo,
            String actionType, String billType, Long billId, String billNo, boolean consumeAvailable,
            Long toAreaId, Long toLocationId) {
        if (isProcessed(tenantId, idempotencyNo, lineNo, actionType)) {
            return;
        }
        InventoryStockBalance balance = findBalance(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo);
        BigDecimal beforeOnHand = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getOnHandQty());
        BigDecimal beforeAvailable = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getAvailableQty());
        BigDecimal beforeFrozen = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getFrozenQty());
        BigDecimal beforeInTransit = balance == null ? BigDecimal.ZERO : InventoryValueSupport.defaultQty(balance.getInTransitQty());
        if (!allowNegative(warehouseId) && consumeAvailable && beforeAvailable.compareTo(changeQty) < 0) {
            throw new ServiceException("可用库存不足，禁止负库存出库", (int) ResultCode.CONFLICT.getCode());
        }
        BigDecimal afterOnHand = beforeOnHand.subtract(changeQty);
        BigDecimal afterAvailable = consumeAvailable ? beforeAvailable.subtract(changeQty) : beforeAvailable;
        if (balance == null) {
            createBalance(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo,
                    afterOnHand, afterAvailable, beforeFrozen, beforeInTransit);
        } else {
            updateBalance(balance, afterOnHand, afterAvailable, beforeFrozen, beforeInTransit);
        }
        upsertBatchRecord(tenantId, orgId, warehouseId, itemId, batchNo, null, null, changeQty.negate());
        upsertSerialRecord(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo,
                null, null, afterOnHand);
        insertTxn(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo, actionType, billType,
                billId, billNo, lineNo, idempotencyNo, beforeOnHand, afterOnHand, beforeAvailable, afterAvailable,
                changeQty.negate(), areaId, locationId, toAreaId, toLocationId);
    }

    /**
     * 校验库存控制规则。
     */
    private void validateInventoryControls(Long itemId, String batchNo, String serialNo, Date productionDate, Date expiryDate) {
        if (itemId == null) {
            throw new IllegalArgumentException("库存明细物料不能为空");
        }
        MdmItem item = itemMapper.selectById(itemId);
        if (item == null) {
            throw new ServiceException("物料不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        String normalizedBatchNo = InventoryValueSupport.trimToNull(batchNo);
        String normalizedSerialNo = InventoryValueSupport.trimToNull(serialNo);
        if ("Y".equalsIgnoreCase(item.getBatchControl()) && !StringUtils.hasText(normalizedBatchNo)) {
            throw new IllegalArgumentException("批次控制物料必须维护批次号");
        }
        if ("Y".equalsIgnoreCase(item.getSerialControl()) && !StringUtils.hasText(normalizedSerialNo)) {
            throw new IllegalArgumentException("序列号控制物料必须维护序列号");
        }
        if (productionDate != null && expiryDate == null && item.getShelfLifeDays() != null && item.getShelfLifeDays() > 0) {
            return;
        }
        if (expiryDate != null && productionDate != null && expiryDate.before(productionDate)) {
            throw new IllegalArgumentException("失效日期不能早于生产日期");
        }
    }

    /**
     * 获取必需存在的余额。
     */
    private InventoryStockBalance requireBalance(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId,
            Long itemId, String batchNo, String serialNo) {
        InventoryStockBalance balance = findBalance(tenantId, orgId, warehouseId, areaId, locationId, itemId, batchNo, serialNo);
        if (balance == null) {
            throw new ServiceException("库存余额不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return balance;
    }

    /**
     * 创建新的库存余额记录。
     */
    private void createBalance(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId,
            Long itemId, String batchNo, String serialNo, BigDecimal onHandQty, BigDecimal availableQty,
            BigDecimal frozenQty, BigDecimal inTransitQty) {
        Date now = new Date();
        InventoryStockBalance balance = new InventoryStockBalance();
        balance.setTenantId(tenantId);
        balance.setOrgId(orgId);
        balance.setWarehouseId(warehouseId);
        balance.setAreaId(areaId);
        balance.setLocationId(locationId);
        balance.setItemId(itemId);
        balance.setBatchNo(InventoryValueSupport.trimToNull(batchNo));
        balance.setSerialNo(InventoryValueSupport.trimToNull(serialNo));
        balance.setOnHandQty(onHandQty);
        balance.setAvailableQty(availableQty);
        balance.setFrozenQty(frozenQty);
        balance.setInTransitQty(inTransitQty);
        balance.setVersionNo(1);
        balance.setLastTxnTime(now);
        balance.setCreateTime(now);
        balance.setUpdateTime(now);
        stockBalanceMapper.insert(balance);
    }

    /**
     * 使用乐观锁更新库存余额。
     */
    private void updateBalance(InventoryStockBalance balance, BigDecimal onHandQty, BigDecimal availableQty,
            BigDecimal frozenQty, BigDecimal inTransitQty) {
        InventoryStockBalance updateEntity = new InventoryStockBalance();
        updateEntity.setBalanceId(balance.getBalanceId());
        updateEntity.setOnHandQty(onHandQty);
        updateEntity.setAvailableQty(availableQty);
        updateEntity.setFrozenQty(frozenQty);
        updateEntity.setInTransitQty(inTransitQty);
        updateEntity.setVersionNo((balance.getVersionNo() == null ? 1 : balance.getVersionNo()) + 1);
        updateEntity.setLastTxnTime(new Date());
        updateEntity.setUpdateTime(new Date());
        int updated = stockBalanceMapper.update(updateEntity, new LambdaUpdateWrapper<InventoryStockBalance>()
                .eq(InventoryStockBalance::getBalanceId, balance.getBalanceId())
                .eq(InventoryStockBalance::getVersionNo, balance.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("库存余额已变化，请刷新后重试");
        }
    }

    /**
     * 更新批次数量。
     */
    private void upsertBatchRecord(String tenantId, Long orgId, Long warehouseId, Long itemId, String batchNo,
            Date productionDate, Date expiryDate, BigDecimal changeQty) {
        String normalizedBatchNo = InventoryValueSupport.trimToNull(batchNo);
        if (!StringUtils.hasText(normalizedBatchNo)) {
            return;
        }
        InventoryBatchRecord record = batchRecordMapper.selectOne(new LambdaQueryWrapper<InventoryBatchRecord>()
                .eq(InventoryBatchRecord::getTenantId, tenantId)
                .eq(InventoryBatchRecord::getOrgId, orgId)
                .eq(InventoryBatchRecord::getWarehouseId, warehouseId)
                .eq(InventoryBatchRecord::getItemId, itemId)
                .eq(InventoryBatchRecord::getBatchNo, normalizedBatchNo));
        Date now = new Date();
        if (record == null) {
            record = new InventoryBatchRecord();
            record.setTenantId(tenantId);
            record.setOrgId(orgId);
            record.setWarehouseId(warehouseId);
            record.setItemId(itemId);
            record.setBatchNo(normalizedBatchNo);
            record.setProductionDate(productionDate);
            record.setExpiryDate(expiryDate);
            record.setCurrentQty(changeQty);
            record.setStatus(changeQty.compareTo(BigDecimal.ZERO) > 0 ? "ACTIVE" : "EMPTY");
            record.setCreateTime(now);
            record.setUpdateTime(now);
            batchRecordMapper.insert(record);
            return;
        }
        InventoryBatchRecord updateEntity = new InventoryBatchRecord();
        updateEntity.setBatchId(record.getBatchId());
        updateEntity.setProductionDate(productionDate == null ? record.getProductionDate() : productionDate);
        updateEntity.setExpiryDate(expiryDate == null ? record.getExpiryDate() : expiryDate);
        BigDecimal currentQty = InventoryValueSupport.defaultQty(record.getCurrentQty()).add(changeQty);
        updateEntity.setCurrentQty(currentQty);
        updateEntity.setStatus(currentQty.compareTo(BigDecimal.ZERO) > 0 ? "ACTIVE" : "EMPTY");
        updateEntity.setUpdateTime(now);
        batchRecordMapper.updateById(updateEntity);
    }

    /**
     * 更新序列号状态。
     */
    private void upsertSerialRecord(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId,
            Long itemId, String batchNo, String serialNo, Date productionDate, Date expiryDate, BigDecimal currentQty) {
        String normalizedSerialNo = InventoryValueSupport.trimToNull(serialNo);
        if (!StringUtils.hasText(normalizedSerialNo)) {
            return;
        }
        InventorySerialRecord record = serialRecordMapper.selectOne(new LambdaQueryWrapper<InventorySerialRecord>()
                .eq(InventorySerialRecord::getTenantId, tenantId)
                .eq(InventorySerialRecord::getItemId, itemId)
                .eq(InventorySerialRecord::getSerialNo, normalizedSerialNo));
        Date now = new Date();
        if (record == null) {
            record = new InventorySerialRecord();
            record.setTenantId(tenantId);
            record.setOrgId(orgId);
            record.setWarehouseId(warehouseId);
            record.setAreaId(areaId);
            record.setLocationId(locationId);
            record.setItemId(itemId);
            record.setBatchNo(InventoryValueSupport.trimToNull(batchNo));
            record.setSerialNo(normalizedSerialNo);
            record.setProductionDate(productionDate);
            record.setExpiryDate(expiryDate);
            record.setStatus(currentQty.compareTo(BigDecimal.ZERO) > 0 ? "IN_STOCK" : "CONSUMED");
            record.setCreateTime(now);
            record.setUpdateTime(now);
            serialRecordMapper.insert(record);
            return;
        }
        InventorySerialRecord updateEntity = new InventorySerialRecord();
        updateEntity.setSerialId(record.getSerialId());
        updateEntity.setWarehouseId(warehouseId);
        updateEntity.setAreaId(areaId);
        updateEntity.setLocationId(locationId);
        updateEntity.setBatchNo(InventoryValueSupport.trimToNull(batchNo));
        updateEntity.setProductionDate(productionDate == null ? record.getProductionDate() : productionDate);
        updateEntity.setExpiryDate(expiryDate == null ? record.getExpiryDate() : expiryDate);
        updateEntity.setStatus(currentQty.compareTo(BigDecimal.ZERO) > 0 ? "IN_STOCK" : "CONSUMED");
        updateEntity.setUpdateTime(now);
        serialRecordMapper.updateById(updateEntity);
    }

    /**
     * 写入库存流水。
     */
    private void insertTxn(String tenantId, Long orgId, Long warehouseId, Long areaId, Long locationId, Long itemId,
            String batchNo, String serialNo, String actionType, String billType, Long billId, String billNo,
            Integer lineNo, String idempotencyNo, BigDecimal beforeOnHand, BigDecimal afterOnHand,
            BigDecimal beforeAvailable, BigDecimal afterAvailable, BigDecimal changeQty,
            Long fromAreaId, Long fromLocationId, Long toAreaId, Long toLocationId) {
        InventoryStockTxn txn = new InventoryStockTxn();
        txn.setTenantId(tenantId);
        txn.setOrgId(orgId);
        txn.setWarehouseId(warehouseId);
        txn.setFromAreaId(fromAreaId);
        txn.setFromLocationId(fromLocationId);
        txn.setAreaId(areaId);
        txn.setLocationId(locationId);
        txn.setToAreaId(toAreaId);
        txn.setToLocationId(toLocationId);
        txn.setItemId(itemId);
        txn.setBatchNo(InventoryValueSupport.trimToNull(batchNo));
        txn.setSerialNo(InventoryValueSupport.trimToNull(serialNo));
        txn.setActionType(actionType);
        txn.setBillType(billType);
        txn.setBillId(billId);
        txn.setBillNo(billNo);
        txn.setLineNo(lineNo);
        txn.setIdempotencyNo(idempotencyNo);
        txn.setTraceId(RequestTraceContextHolder.getTraceId());
        txn.setBeforeOnHandQty(beforeOnHand);
        txn.setAfterOnHandQty(afterOnHand);
        txn.setBeforeAvailableQty(beforeAvailable);
        txn.setAfterAvailableQty(afterAvailable);
        txn.setChangeQty(changeQty);
        txn.setOperator(resolveOperator());
        txn.setCreateTime(new Date());
        stockTxnMapper.insert(txn);
    }

    /**
     * 判断单行库存事务是否已处理。
     *
     * @param tenantId 租户编号
     * @param idempotencyNo 幂等号
     * @param lineNo 行号
     * @param actionType 动作类型
     * @return true 表示已处理
     */
    private boolean isProcessed(String tenantId, String idempotencyNo, Integer lineNo, String actionType) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(idempotencyNo) || lineNo == null) {
            return false;
        }
        Long count = stockTxnMapper.selectCount(new LambdaQueryWrapper<InventoryStockTxn>()
                .eq(InventoryStockTxn::getTenantId, tenantId)
                .eq(InventoryStockTxn::getIdempotencyNo, idempotencyNo)
                .eq(InventoryStockTxn::getLineNo, lineNo)
                .eq(InventoryStockTxn::getActionType, actionType));
        return count != null && count > 0;
    }

    /**
     * 判断仓库是否允许负库存。
     *
     * @param warehouseId 仓库ID
     * @return true 表示允许负库存
     */
    private boolean allowNegative(Long warehouseId) {
        if (warehouseId == null) {
            return false;
        }
        MdmWarehouse warehouse = warehouseMapper.selectById(warehouseId);
        return warehouse != null && "Y".equalsIgnoreCase(warehouse.getAllowNegativeStock());
    }

    /**
     * 校验变更数量。
     *
     * @param qty 原始数量
     * @return 标准数量
     */
    private BigDecimal validateChangeQty(BigDecimal qty) {
        BigDecimal value = InventoryValueSupport.defaultQty(qty);
        if (value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("库存变更数量必须大于0");
        }
        return value;
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }
}
