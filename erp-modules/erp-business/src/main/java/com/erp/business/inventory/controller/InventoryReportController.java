package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.domain.vo.InventoryAgeReportRow;
import com.erp.business.inventory.domain.vo.InventoryExpiryReportRow;
import com.erp.business.inventory.domain.vo.InventoryStocktakeDiffReportRow;
import com.erp.business.inventory.service.IInventoryReportService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存报表控制器。
 */
@RestController
@RequestMapping("/business/inventory/report")
public class InventoryReportController {

    private final IInventoryReportService reportService;

    public InventoryReportController(IInventoryReportService reportService) {
        this.reportService = reportService;
    }

    /**
     * 查询库存汇总报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/summary/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:list')")
    public R<PageData<InventoryStockBalance>> summary(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockBalance> page = reportService.selectSummaryPage(warehouseId, itemId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

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
    @GetMapping("/movement/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:list')")
    public R<PageData<InventoryStockTxn>> movement(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockTxn> page = reportService.selectMovementPage(warehouseId, itemId, actionType, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询库龄报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/age/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:list')")
    public R<PageData<InventoryAgeReportRow>> age(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryAgeReportRow> page = reportService.selectAgePage(warehouseId, itemId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

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
    @GetMapping("/expiry/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:list')")
    public R<PageData<InventoryExpiryReportRow>> expiry(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryExpiryReportRow> page = reportService.selectExpiryPage(warehouseId, itemId, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询盘点差异报表。
     *
     * @param warehouseId 仓库ID
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/stocktake-diff/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:list')")
    public R<PageData<InventoryStocktakeDiffReportRow>> stocktakeDiff(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStocktakeDiffReportRow> page = reportService.selectStocktakeDiffPage(warehouseId, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 导出库存汇总报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @return 导出任务ID
     */
    @PostMapping("/summary/export")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:export')")
    public R<Long> exportSummary(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId) {
        return R.success(reportService.exportReport("summary", warehouseId, itemId, null, null));
    }

    /**
     * 导出库存收发存报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @return 导出任务ID
     */
    @PostMapping("/movement/export")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:export')")
    public R<Long> exportMovement(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "actionType", required = false) String actionType) {
        return R.success(reportService.exportReport("movement", warehouseId, itemId, actionType, null));
    }

    /**
     * 导出库龄报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @return 导出任务ID
     */
    @PostMapping("/age/export")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:export')")
    public R<Long> exportAge(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId) {
        return R.success(reportService.exportReport("age", warehouseId, itemId, null, null));
    }

    /**
     * 导出有效期报表。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param status 状态
     * @return 导出任务ID
     */
    @PostMapping("/expiry/export")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:export')")
    public R<Long> exportExpiry(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "status", required = false) String status) {
        return R.success(reportService.exportReport("expiry", warehouseId, itemId, null, status));
    }

    /**
     * 导出盘点差异报表。
     *
     * @param warehouseId 仓库ID
     * @param status 状态
     * @return 导出任务ID
     */
    @PostMapping("/stocktake-diff/export")
    @PreAuthorize("@ss.hasPermi('business:inventory:report:export')")
    public R<Long> exportStocktakeDiff(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "status", required = false) String status) {
        return R.success(reportService.exportReport("stocktake-diff", warehouseId, null, null, status));
    }
}
