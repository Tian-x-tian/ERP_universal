package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.domain.vo.InventoryAgeReportRow;
import com.erp.business.inventory.domain.vo.InventoryExpiryReportRow;
import com.erp.business.inventory.domain.vo.InventoryStocktakeDiffReportRow;

/**
 * 库存报表服务接口。
 */
public interface IInventoryReportService {

    /**
     * 查询库存汇总报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryStockBalance> selectSummaryPage(Long warehouseId, Long itemId, Long pageNum, Long pageSize);

    /**
     * 查询库存收发存报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryStockTxn> selectMovementPage(Long warehouseId, Long itemId, String actionType,
            Long pageNum, Long pageSize);

    /**
     * 查询库龄报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryAgeReportRow> selectAgePage(Long warehouseId, Long itemId, Long pageNum, Long pageSize);

    /**
     * 查询有效期报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryExpiryReportRow> selectExpiryPage(Long warehouseId, Long itemId, String status,
            Long pageNum, Long pageSize);

    /**
     * 查询盘点差异报表。
     *
     * @param warehouseId 仓库ID
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryStocktakeDiffReportRow> selectStocktakeDiffPage(Long warehouseId, String status,
            Long pageNum, Long pageSize);

    /**
     * 导出报表并创建异步任务。
     *
     * @param reportType 报表类型
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param status 状态
     * @return 导出任务ID
     */
    Long exportReport(String reportType, Long warehouseId, Long itemId, String actionType, String status);
}
