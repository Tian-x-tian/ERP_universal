package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryWarningRecord;
import com.erp.business.inventory.service.IInventoryWarningService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 预警中心控制器。
 */
@RestController
@RequestMapping("/business/inventory/warning")
public class InventoryWarningController {

    private final IInventoryWarningService warningService;

    public InventoryWarningController(IInventoryWarningService warningService) {
        this.warningService = warningService;
    }

    /**
     * 查询预警分页。
     *
     * @param warningType 预警类型
     * @param status 状态
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:warning:list')")
    public R<PageData<InventoryWarningRecord>> list(@RequestParam(value = "warningType", required = false) String warningType,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryWarningRecord> page = warningService.selectPage(warningType, status, warehouseId, itemId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 标记预警已读。
     *
     * @param warningId 预警ID
     * @return 处理结果
     */
    @PostMapping("/read/{warningId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:warning:handle')")
    public R<Boolean> read(@PathVariable("warningId") Long warningId) {
        return warningService.markRead(warningId) ? R.success(true) : R.failed("标记预警已读失败");
    }

    /**
     * 关闭预警。
     *
     * @param warningId 预警ID
     * @return 处理结果
     */
    @PostMapping("/close/{warningId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:warning:handle')")
    public R<Boolean> close(@PathVariable("warningId") Long warningId) {
        return warningService.close(warningId) ? R.success(true) : R.failed("关闭预警失败");
    }

    /**
     * 手工触发预警扫描。
     *
     * @return 处理结果
     */
    @PostMapping("/scan")
    @PreAuthorize("@ss.hasPermi('business:inventory:warning:scan')")
    public R<Boolean> scan() {
        warningService.scanWarnings();
        return R.success(true);
    }
}
