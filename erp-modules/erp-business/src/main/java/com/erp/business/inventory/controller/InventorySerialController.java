package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventorySerialRecord;
import com.erp.business.inventory.service.IInventorySerialService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 序列号查询控制器。
 */
@RestController
@RequestMapping("/business/inventory/serial")
public class InventorySerialController {

    private final IInventorySerialService serialService;

    public InventorySerialController(IInventorySerialService serialService) {
        this.serialService = serialService;
    }

    /**
     * 查询序列号分页。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param serialNo 序列号
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:serial:list')")
    public R<PageData<InventorySerialRecord>> list(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "serialNo", required = false) String serialNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventorySerialRecord> page = serialService.selectPage(warehouseId, itemId, serialNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }
}
