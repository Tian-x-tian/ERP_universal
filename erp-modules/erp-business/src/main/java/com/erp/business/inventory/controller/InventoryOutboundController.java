package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryOutboundOrder;
import com.erp.business.inventory.service.IInventoryOutboundService;
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
 * 出库控制器。
 */
@RestController
@RequestMapping("/business/inventory/outbound")
public class InventoryOutboundController {

    private final IInventoryOutboundService outboundService;

    public InventoryOutboundController(IInventoryOutboundService outboundService) {
        this.outboundService = outboundService;
    }

    /**
     * 查询出库单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:list')")
    public R<PageData<InventoryOutboundOrder>> list(@RequestParam(value = "billNo", required = false) String billNo,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryOutboundOrder> page = outboundService.selectPage(billNo, status, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询出库单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @GetMapping("/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:query')")
    public R<InventoryOutboundOrder> detail(@PathVariable("orderId") Long orderId) {
        return R.success(outboundService.getDetail(orderId));
    }

    /**
     * 新增出库单。
     *
     * @param order 出库单
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:add')")
    public R<Boolean> add(@RequestBody InventoryOutboundOrder order) {
        return outboundService.create(order) ? R.success(true) : R.failed("新增出库单失败");
    }

    /**
     * 修改出库单。
     *
     * @param order 出库单
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:edit')")
    public R<Boolean> edit(@RequestBody InventoryOutboundOrder order) {
        return outboundService.update(order) ? R.success(true) : R.failed("修改出库单失败");
    }

    /**
     * 提交出库单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/submit/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:submit')")
    public R<Boolean> submit(@PathVariable("orderId") Long orderId) {
        return outboundService.submit(orderId) ? R.success(true) : R.failed("提交出库单失败");
    }

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/approve/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:submit')")
    public R<Boolean> approve(@PathVariable("orderId") Long orderId) {
        return outboundService.approve(orderId) ? R.success(true) : R.failed("回写审批通过失败");
    }

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/reject/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:submit')")
    public R<Boolean> reject(@PathVariable("orderId") Long orderId) {
        return outboundService.reject(orderId) ? R.success(true) : R.failed("回写审批驳回失败");
    }

    /**
     * 执行出库单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/execute/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:execute')")
    public R<Boolean> execute(@PathVariable("orderId") Long orderId) {
        return outboundService.execute(orderId) ? R.success(true) : R.failed("执行出库单失败");
    }

    /**
     * 取消出库单。
     *
     * @param orderId 单据ID
     * @return 处理结果
     */
    @PostMapping("/cancel/{orderId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:outbound:cancel')")
    public R<Boolean> cancel(@PathVariable("orderId") Long orderId) {
        return outboundService.cancel(orderId) ? R.success(true) : R.failed("取消出库单失败");
    }
}
