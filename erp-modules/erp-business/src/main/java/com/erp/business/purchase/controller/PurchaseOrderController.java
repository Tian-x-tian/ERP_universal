package com.erp.business.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.purchase.domain.PurchaseOrder;
import com.erp.business.purchase.service.IPurchaseOrderService;
import com.erp.common.core.domain.R;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 采购订单控制层。
 */
@Tag(name = "采购订单")
@RestController
@RequestMapping("/business/purchase/order")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final IPurchaseOrderService purchaseOrderService;

    @Operation(summary = "分页查询采购订单")
    @PreAuthorize("@ss.hasPermi('business:pur:order:list')")
    @GetMapping("/list")
    public R<Page<PurchaseOrder>> list(@RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "orderNo", required = false) String orderNo,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return R.success(purchaseOrderService.selectPage(status, orderNo, pageNum, pageSize));
    }

    @Operation(summary = "查询采购订单详情")
    @PreAuthorize("@ss.hasPermi('business:pur:order:list')")
    @GetMapping("/{orderId}")
    public R<PurchaseOrder> detail(@PathVariable("orderId") Long orderId) {
        return R.success(purchaseOrderService.getDetail(orderId));
    }

    @Operation(summary = "新增采购订单")
    @PreAuthorize("@ss.hasPermi('business:pur:order:add')")
    @PostMapping
    public R<Long> add(@RequestBody PurchaseOrder order) {
        return R.success(purchaseOrderService.create(order));
    }

    @Operation(summary = "修改采购订单")
    @PreAuthorize("@ss.hasPermi('business:pur:order:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody PurchaseOrder order) {
        return R.success(purchaseOrderService.update(order));
    }

    @Operation(summary = "删除采购订单")
    @PreAuthorize("@ss.hasPermi('business:pur:order:remove')")
    @DeleteMapping("/{orderId}")
    public R<Boolean> remove(@PathVariable("orderId") Long orderId) {
        return R.success(purchaseOrderService.remove(orderId));
    }

    @Operation(summary = "提交采购订单（按金额阈值决定是否审批）")
    @PreAuthorize("@ss.hasPermi('business:pur:order:submit')")
    @PostMapping("/{orderId}/submit")
    public R<String> submit(@PathVariable("orderId") Long orderId) {
        return R.success(purchaseOrderService.submit(orderId));
    }

    @Operation(summary = "取消采购订单")
    @PreAuthorize("@ss.hasPermi('business:pur:order:cancel')")
    @PostMapping("/{orderId}/cancel")
    public R<Boolean> cancel(@PathVariable("orderId") Long orderId,
            @RequestParam(value = "reason", required = false) String reason) {
        return R.success(purchaseOrderService.cancel(orderId, reason));
    }
}
