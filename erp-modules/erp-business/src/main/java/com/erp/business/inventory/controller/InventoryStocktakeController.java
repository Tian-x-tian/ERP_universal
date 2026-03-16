package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStocktakeOrder;
import com.erp.business.inventory.service.IInventoryStocktakeService;
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
 * 盘点控制器。
 */
@RestController
@RequestMapping("/business/inventory/stocktake")
public class InventoryStocktakeController {

    private final IInventoryStocktakeService stocktakeService;

    public InventoryStocktakeController(IInventoryStocktakeService stocktakeService) {
        this.stocktakeService = stocktakeService;
    }

    /**
     * 查询盘点单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:list')")
    public R<PageData<InventoryStocktakeOrder>> list(@RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStocktakeOrder> page = stocktakeService.selectPage(billNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询盘点单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:query')")
    public R<InventoryStocktakeOrder> detail(@PathVariable("orderId") Long orderId) {
        return R.success(stocktakeService.getDetail(orderId));
    }

    /**
     * 新增盘点单。
     *
     * @param order 盘点单
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:add')")
    public R<Boolean> add(@RequestBody InventoryStocktakeOrder order) {
        return stocktakeService.create(order) ? R.success(true) : R.failed("新增盘点单失败");
    }

    /**
     * 修改盘点单。
     *
     * @param order 盘点单
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:edit')")
    public R<Boolean> edit(@RequestBody InventoryStocktakeOrder order) {
        return stocktakeService.update(order) ? R.success(true) : R.failed("修改盘点单失败");
    }

    /**
     * 提交盘点单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:submit')")
    public R<Boolean> submit(@PathVariable("orderId") Long orderId) {
        return stocktakeService.submit(orderId) ? R.success(true) : R.failed("提交盘点单失败");
    }

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/approve/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:submit')")
    public R<Boolean> approve(@PathVariable("orderId") Long orderId) {
        return stocktakeService.approve(orderId) ? R.success(true) : R.failed("回写审批通过失败");
    }

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/reject/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:submit')")
    public R<Boolean> reject(@PathVariable("orderId") Long orderId) {
        return stocktakeService.reject(orderId) ? R.success(true) : R.failed("回写审批驳回失败");
    }

    /**
     * 确认盘点差异。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/confirm/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:execute')")
    public R<Boolean> confirm(@PathVariable("orderId") Long orderId) {
        return stocktakeService.confirm(orderId) ? R.success(true) : R.failed("确认盘点差异失败");
    }

    /**
     * 兼容执行动作。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/execute/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:execute')")
    public R<Boolean> execute(@PathVariable("orderId") Long orderId) {
        return stocktakeService.execute(orderId) ? R.success(true) : R.failed("执行盘点单失败");
    }

    /**
     * 取消盘点单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:stocktake:cancel')")
    public R<Boolean> cancel(@PathVariable("orderId") Long orderId) {
        return stocktakeService.cancel(orderId) ? R.success(true) : R.failed("取消盘点单失败");
    }
}
