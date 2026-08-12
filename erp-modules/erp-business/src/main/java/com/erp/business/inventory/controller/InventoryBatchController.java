package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryBatchRecord;
import com.erp.business.inventory.service.IInventoryBatchService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 批次查询控制器。
 */
@RestController
@RequestMapping("/business/inventory/batch")
public class InventoryBatchController {

    private final IInventoryBatchService batchService;

    public InventoryBatchController(IInventoryBatchService batchService) {
        this.batchService = batchService;
    }

    /**
     * 查询批次分页。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param batchNo 批次号
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:batch:list')")
    public R<PageData<InventoryBatchRecord>> list(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "batchNo", required = false) String batchNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryBatchRecord> page = batchService.selectPage(warehouseId, itemId, batchNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }
}
