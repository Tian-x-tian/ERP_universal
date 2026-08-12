package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockMoveOrder;
import com.erp.business.inventory.service.IInventoryStockMoveService;
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
 * 移库控制器。
 */
@RestController
@RequestMapping("/business/inventory/move")
public class InventoryStockMoveController {

    private final IInventoryStockMoveService stockMoveService;

    public InventoryStockMoveController(IInventoryStockMoveService stockMoveService) {
        this.stockMoveService = stockMoveService;
    }

    /**
     * 查询移库单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:list')")
    public R<PageData<InventoryStockMoveOrder>> list(@RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockMoveOrder> page = stockMoveService.selectPage(billNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询移库单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:query')")
    public R<InventoryStockMoveOrder> detail(@PathVariable("orderId") Long orderId) {
        return R.success(stockMoveService.getDetail(orderId));
    }

    /**
     * 新增移库单。
     *
     * @param order 移库单
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:move:add')")
    public R<Boolean> add(@RequestBody InventoryStockMoveOrder order) {
        return stockMoveService.create(order) ? R.success(true) : R.failed("新增移库单失败");
    }

    /**
     * 修改移库单。
     *
     * @param order 移库单
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:move:edit')")
    public R<Boolean> edit(@RequestBody InventoryStockMoveOrder order) {
        return stockMoveService.update(order) ? R.success(true) : R.failed("修改移库单失败");
    }

    /**
     * 提交移库单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:submit')")
    public R<Boolean> submit(@PathVariable("orderId") Long orderId) {
        return stockMoveService.submit(orderId) ? R.success(true) : R.failed("提交移库单失败");
    }

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/approve/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:submit')")
    public R<Boolean> approve(@PathVariable("orderId") Long orderId) {
        return stockMoveService.approve(orderId) ? R.success(true) : R.failed("回写审批通过失败");
    }

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/reject/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:submit')")
    public R<Boolean> reject(@PathVariable("orderId") Long orderId) {
        return stockMoveService.reject(orderId) ? R.success(true) : R.failed("回写审批驳回失败");
    }

    /**
     * 执行移库单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/execute/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:execute')")
    public R<Boolean> execute(@PathVariable("orderId") Long orderId) {
        return stockMoveService.execute(orderId) ? R.success(true) : R.failed("执行移库单失败");
    }

    /**
     * 取消移库单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:move:cancel')")
    public R<Boolean> cancel(@PathVariable("orderId") Long orderId) {
        return stockMoveService.cancel(orderId) ? R.success(true) : R.failed("取消移库单失败");
    }
}
