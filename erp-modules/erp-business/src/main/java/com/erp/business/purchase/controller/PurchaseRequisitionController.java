package com.erp.business.purchase.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.purchase.domain.PurchaseRequisition;
import com.erp.business.purchase.service.IPurchaseRequisitionService;
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
 * 采购申请控制层。
 */
@Tag(name = "采购申请")
@RestController
@RequestMapping("/business/purchase/requisition")
@RequiredArgsConstructor
public class PurchaseRequisitionController {

    private final IPurchaseRequisitionService requisitionService;

    @Operation(summary = "分页查询采购申请")
    @PreAuthorize("@ss.hasPermi('business:pur:req:list')")
    @GetMapping("/list")
    public R<Page<PurchaseRequisition>> list(@RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "reqNo", required = false) String reqNo,
            @RequestParam(value = "pageNum", defaultValue = "1") long pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") long pageSize) {
        return R.success(requisitionService.selectPage(status, reqNo, pageNum, pageSize));
    }

    @Operation(summary = "查询采购申请详情")
    @PreAuthorize("@ss.hasPermi('business:pur:req:list')")
    @GetMapping("/{requisitionId}")
    public R<PurchaseRequisition> detail(@PathVariable("requisitionId") Long requisitionId) {
        return R.success(requisitionService.getDetail(requisitionId));
    }

    @Operation(summary = "新增采购申请")
    @PreAuthorize("@ss.hasPermi('business:pur:req:add')")
    @PostMapping
    public R<Long> add(@RequestBody PurchaseRequisition requisition) {
        return R.success(requisitionService.create(requisition));
    }

    @Operation(summary = "修改采购申请")
    @PreAuthorize("@ss.hasPermi('business:pur:req:edit')")
    @PutMapping
    public R<Boolean> edit(@RequestBody PurchaseRequisition requisition) {
        return R.success(requisitionService.update(requisition));
    }

    @Operation(summary = "删除采购申请")
    @PreAuthorize("@ss.hasPermi('business:pur:req:remove')")
    @DeleteMapping("/{requisitionId}")
    public R<Boolean> remove(@PathVariable("requisitionId") Long requisitionId) {
        return R.success(requisitionService.remove(requisitionId));
    }

    @Operation(summary = "提交采购申请审批")
    @PreAuthorize("@ss.hasPermi('business:pur:req:submit')")
    @PostMapping("/{requisitionId}/submit")
    public R<Boolean> submit(@PathVariable("requisitionId") Long requisitionId) {
        return R.success(requisitionService.submit(requisitionId));
    }

    @Operation(summary = "采购申请转采购订单")
    @PreAuthorize("@ss.hasPermi('business:pur:req:convert')")
    @PostMapping("/{requisitionId}/convert")
    public R<Long> convert(@PathVariable("requisitionId") Long requisitionId,
            @RequestParam("supplierId") Long supplierId) {
        return R.success(requisitionService.convertToOrder(requisitionId, supplierId));
    }
}
