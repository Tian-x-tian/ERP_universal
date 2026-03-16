package com.erp.business.inventory.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockPolicy;
import com.erp.business.inventory.service.IInventoryStockPolicyService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
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
 * 库存策略控制器。
 */
@RestController
@RequestMapping("/business/inventory/policy")
public class InventoryStockPolicyController {

    private final IInventoryStockPolicyService stockPolicyService;

    public InventoryStockPolicyController(IInventoryStockPolicyService stockPolicyService) {
        this.stockPolicyService = stockPolicyService;
    }

    /**
     * 查询库存策略分页。
     *
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:inventory:policy:list')")
    public R<PageData<InventoryStockPolicy>> list(@RequestParam(value = "orgId", required = false) Long orgId,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "itemId", required = false) Long itemId,
            @RequestParam(value = "pageNum", required = false) Long pageNum,
            @RequestParam(value = "pageSize", required = false) Long pageSize) {
        Page<InventoryStockPolicy> page = stockPolicyService.selectPage(orgId, warehouseId, itemId, pageNum, pageSize);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询库存策略详情。
     *
     * @param policyId 策略ID
     * @return 策略详情
     */
    @GetMapping("/{policyId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:policy:query')")
    public R<InventoryStockPolicy> detail(@PathVariable("policyId") Long policyId) {
        return R.success(stockPolicyService.getDetail(policyId));
    }

    /**
     * 新增库存策略。
     *
     * @param policy 策略对象
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:policy:add')")
    public R<Boolean> add(@RequestBody InventoryStockPolicy policy) {
        return stockPolicyService.create(policy) ? R.success(true) : R.failed("新增库存策略失败");
    }

    /**
     * 修改库存策略。
     *
     * @param policy 策略对象
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('business:inventory:policy:edit')")
    public R<Boolean> edit(@RequestBody InventoryStockPolicy policy) {
        return stockPolicyService.update(policy) ? R.success(true) : R.failed("修改库存策略失败");
    }

    /**
     * 删除库存策略。
     *
     * @param policyId 策略ID
     * @return 处理结果
     */
    @DeleteMapping("/{policyId}")
    @PreAuthorize("@ss.hasPermi('business:inventory:policy:remove')")
    public R<Boolean> remove(@PathVariable("policyId") Long policyId) {
        return stockPolicyService.delete(policyId) ? R.success(true) : R.failed("删除库存策略失败");
    }
}
