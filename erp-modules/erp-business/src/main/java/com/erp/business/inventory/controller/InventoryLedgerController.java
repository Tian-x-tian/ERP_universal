package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.service.IInventoryLedgerService;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存台账控制器。
 */
@RestController
@RequestMapping("/business/inventory/ledger")
public class InventoryLedgerController {

    private final IInventoryLedgerService ledgerService;

    public InventoryLedgerController(IInventoryLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * 查询库存余额台账。
     *
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:ledger:list')")
    public R<PageData<InventoryStockBalance>> list(@RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockBalance> page = ledgerService.selectBalancePage(TenantContextHolder.getTenantId(), orgId,
                warehouseId, itemId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询库存流水台账。
     *
     * @param billNo 单据编号
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/txn/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:ledger:list')")
    public R<PageData<InventoryStockTxn>> txnList(@RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "actionType", required = false) String actionType,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockTxn> page = ledgerService.selectTxnPage(TenantContextHolder.getTenantId(), billNo, itemId,
                actionType, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }
}
