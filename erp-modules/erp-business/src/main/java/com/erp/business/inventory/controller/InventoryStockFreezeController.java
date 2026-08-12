package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockFreezeOrder;
import com.erp.business.inventory.service.IInventoryStockFreezeService;
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
 * 冻结解冻控制器。
 */
@RestController
@RequestMapping("/business/inventory/freeze")
public class InventoryStockFreezeController {

    private final IInventoryStockFreezeService stockFreezeService;

    public InventoryStockFreezeController(IInventoryStockFreezeService stockFreezeService) {
        this.stockFreezeService = stockFreezeService;
    }

    /**
     * 查询冻结解冻单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:list')")
    public R<PageData<InventoryStockFreezeOrder>> list(@RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockFreezeOrder> page = stockFreezeService.selectPage(billNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询冻结解冻单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:query')")
    public R<InventoryStockFreezeOrder> detail(@PathVariable("orderId") Long orderId) {
        return R.success(stockFreezeService.getDetail(orderId));
    }

    /**
     * 新增冻结解冻单。
     *
     * @param order 冻结解冻单
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:add')")
    public R<Boolean> add(@RequestBody InventoryStockFreezeOrder order) {
        return stockFreezeService.create(order) ? R.success(true) : R.failed("新增冻结解冻单失败");
    }

    /**
     * 修改冻结解冻单。
     *
     * @param order 冻结解冻单
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:edit')")
    public R<Boolean> edit(@RequestBody InventoryStockFreezeOrder order) {
        return stockFreezeService.update(order) ? R.success(true) : R.failed("修改冻结解冻单失败");
    }

    /**
     * 提交冻结解冻单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:submit')")
    public R<Boolean> submit(@PathVariable("orderId") Long orderId) {
        return stockFreezeService.submit(orderId) ? R.success(true) : R.failed("提交冻结解冻单失败");
    }

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/approve/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:submit')")
    public R<Boolean> approve(@PathVariable("orderId") Long orderId) {
        return stockFreezeService.approve(orderId) ? R.success(true) : R.failed("回写审批通过失败");
    }

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/reject/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:submit')")
    public R<Boolean> reject(@PathVariable("orderId") Long orderId) {
        return stockFreezeService.reject(orderId) ? R.success(true) : R.failed("回写审批驳回失败");
    }

    /**
     * 执行冻结解冻单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/execute/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:execute')")
    public R<Boolean> execute(@PathVariable("orderId") Long orderId) {
        return stockFreezeService.execute(orderId) ? R.success(true) : R.failed("执行冻结解冻单失败");
    }

    /**
     * 取消冻结解冻单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:freeze:cancel')")
    public R<Boolean> cancel(@PathVariable("orderId") Long orderId) {
        return stockFreezeService.cancel(orderId) ? R.success(true) : R.failed("取消冻结解冻单失败");
    }
}
