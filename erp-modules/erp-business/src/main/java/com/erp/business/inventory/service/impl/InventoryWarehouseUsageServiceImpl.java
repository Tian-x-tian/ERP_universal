package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.mapper.InventoryWarehouseUsageMapper;
import com.erp.business.inventory.service.IInventoryWarehouseUsageService;
import com.erp.platform.contract.model.WarehouseReferenceUsage;
import org.springframework.stereotype.Service;

/**
 * 仓库占用情况查询服务实现。
 */
@Service
public class InventoryWarehouseUsageServiceImpl implements IInventoryWarehouseUsageService {

    private final InventoryWarehouseUsageMapper warehouseUsageMapper;

    public InventoryWarehouseUsageServiceImpl(InventoryWarehouseUsageMapper warehouseUsageMapper) {
        this.warehouseUsageMapper = warehouseUsageMapper;
    }

    /**
     * 查询仓库在库存模块中的引用占用情况。
     *
     * @param warehouseId 仓库ID
     * @return 占用情况
     */
    @Override
    public WarehouseReferenceUsage getWarehouseUsage(Long warehouseId) {
        WarehouseReferenceUsage usage = new WarehouseReferenceUsage();
        usage.setWarehouseId(warehouseId);
        if (warehouseId == null) {
            usage.setAvailableStockCount(0L);
            usage.setOpenInboundOrderCount(0L);
            usage.setOpenOutboundOrderCount(0L);
            return usage;
        }
        usage.setAvailableStockCount(zeroIfNull(warehouseUsageMapper.countAvailableStock(warehouseId)));
        usage.setOpenInboundOrderCount(zeroIfNull(warehouseUsageMapper.countOpenInboundOrders(warehouseId)));
        usage.setOpenOutboundOrderCount(zeroIfNull(warehouseUsageMapper.countOpenOutboundOrders(warehouseId)));
        return usage;
    }

    /**
     * 空值归零。
     *
     * @param value 原始计数
     * @return 非空计数
     */
    private Long zeroIfNull(Long value) {
        return value == null ? 0L : value;
    }
}
