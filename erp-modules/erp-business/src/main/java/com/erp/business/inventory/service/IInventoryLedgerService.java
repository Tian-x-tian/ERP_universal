package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;

/**
 * 库存台账服务接口。
 */
public interface IInventoryLedgerService {

    /**
     * 查询库存余额分页。
     *
     * @param tenantId 租户编号
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryStockBalance> selectBalancePage(String tenantId, Long orgId, Long warehouseId, Long itemId,
            Long pageNum, Long pageSize);

    /**
     * 查询库存流水分页。
     *
     * @param tenantId 租户编号
     * @param billNo 单据编号
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryStockTxn> selectTxnPage(String tenantId, String billNo, Long itemId, String actionType,
            Long pageNum, Long pageSize);
}
