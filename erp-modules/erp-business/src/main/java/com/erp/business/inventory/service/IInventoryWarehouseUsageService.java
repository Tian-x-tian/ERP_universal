package com.erp.business.inventory.service;

import com.erp.platform.contract.model.WarehouseReferenceUsage;

/**
 * 仓库占用情况查询服务。
 */
public interface IInventoryWarehouseUsageService {

    /**
     * 查询仓库在库存模块中的引用占用情况。
     *
     * @param warehouseId 仓库ID
     * @return 占用情况
     */
    WarehouseReferenceUsage getWarehouseUsage(Long warehouseId);
}
