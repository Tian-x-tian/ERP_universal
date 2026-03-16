package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockAdjustOrder;
import com.erp.business.inventory.service.IInventoryStockAdjustService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存调整控制器。
 */
@RestController
@RequestMapping("/business/inventory/adjust")
public class InventoryStockAdjustController {

    private final IInventoryStockAdjustService stockAdjustService;

    public InventoryStockAdjustController(IInventoryStockAdjustService stockAdjustService) {
        this.stockAdjustService = stockAdjustService;
    }

    /**
     * 查询库存调整单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:list')")
    public R<PageData<InventoryStockAdjustOrder>> list(@RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockAdjustOrder> page = stockAdjustService.selectPage(billNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询库存调整单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:query')")
    public R<InventoryStockAdjustOrder> detail(@PathVariable("orderId") Long orderId) {
        return R.success(stockAdjustService.getDetail(orderId));
    }

    /**
     * 新增库存调整单。
     *
     * @param order 库存调整单
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:add')")
    public R<Boolean> add(@RequestBody InventoryStockAdjustOrder order) {
        return stockAdjustService.create(order) ? R.success(true) : R.failed("新增库存调整单失败");
    }

    /**
     * 修改库存调整单。
     *
     * @param order 库存调整单
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:edit')")
    public R<Boolean> edit(@RequestBody InventoryStockAdjustOrder order) {
        return stockAdjustService.update(order) ? R.success(true) : R.failed("修改库存调整单失败");
    }

    /**
     * 提交库存调整单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:submit')")
    public R<Boolean> submit(@PathVariable("orderId") Long orderId) {
        return stockAdjustService.submit(orderId) ? R.success(true) : R.failed("提交库存调整单失败");
    }

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/approve/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:submit')")
    public R<Boolean> approve(@PathVariable("orderId") Long orderId) {
        return stockAdjustService.approve(orderId) ? R.success(true) : R.failed("回写审批通过失败");
    }

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/reject/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:submit')")
    public R<Boolean> reject(@PathVariable("orderId") Long orderId) {
        return stockAdjustService.reject(orderId) ? R.success(true) : R.failed("回写审批驳回失败");
    }

    /**
     * 执行库存调整单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/execute/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:execute')")
    public R<Boolean> execute(@PathVariable("orderId") Long orderId) {
        return stockAdjustService.execute(orderId) ? R.success(true) : R.failed("执行库存调整单失败");
    }

    /**
     * 取消库存调整单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:adjust:cancel')")
    public R<Boolean> cancel(@PathVariable("orderId") Long orderId) {
        return stockAdjustService.cancel(orderId) ? R.success(true) : R.failed("取消库存调整单失败");
    }
}
