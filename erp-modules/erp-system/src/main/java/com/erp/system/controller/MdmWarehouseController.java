package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmWarehouse;
import com.erp.system.service.IMdmWarehouseService;
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

import java.util.List;

/**
 * MDM 仓库主数据控制层。
 */
@RestController
@RequestMapping("/system/mdm/warehouse")
public class MdmWarehouseController {
    private final IMdmWarehouseService warehouseService;

    public MdmWarehouseController(IMdmWarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    /**
     * 查询仓库列表。
     *
     * @param whCode 仓库编码
     * @param whName 仓库名称
     * @param status 状态
     * @return 仓库列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse:list')")
    public R<List<MdmWarehouse>> list(@RequestParam(value = "whCode", required = false) String whCode,
            @RequestParam(value = "whName", required = false) String whName,
            @RequestParam(value = "status", required = false) String status) {
        return R.success(warehouseService.selectWarehouseList(whCode, whName, status));
    }

    /**
     * 查询仓库详情。
     *
     * @param warehouseId 仓库ID
     * @return 仓库详情
     */
    @GetMapping("/{warehouseId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse:query')")
    public R<MdmWarehouse> getInfo(@PathVariable("warehouseId") Long warehouseId) {
        MdmWarehouse warehouse = warehouseService.getById(warehouseId);
        if (warehouse == null || "2".equals(warehouse.getDelFlag())) {
            return R.failed("仓库不存在");
        }
        return R.success(warehouse);
    }

    /**
     * 新增仓库。
     *
     * @param warehouse 仓库对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse:add')")
    public R<Boolean> add(@RequestBody MdmWarehouse warehouse) {
        boolean success = warehouseService.createWarehouse(warehouse);
        return success ? R.success(true) : R.failed("新增仓库失败，请检查编码唯一性与关键字段");
    }

    /**
     * 修改仓库。
     *
     * @param warehouse 仓库对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse:edit')")
    public R<Boolean> edit(@RequestBody MdmWarehouse warehouse) {
        if (warehouse == null || warehouse.getWarehouseId() == null) {
            return R.failed("仓库ID不能为空");
        }
        boolean success = warehouseService.updateWarehouse(warehouse);
        return success ? R.success(true) : R.failed("修改仓库失败，请检查参数");
    }

    /**
     * 停用仓库。
     *
     * @param warehouseId 仓库ID
     * @return 停用结果
     */
    @PostMapping("/disable/{warehouseId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse:disable')")
    public R<Boolean> disable(@PathVariable("warehouseId") Long warehouseId) {
        boolean success = warehouseService.disableWarehouse(warehouseId);
        return success ? R.success(true) : R.failed("停用仓库失败");
    }

    /**
     * 删除仓库（逻辑删除）。
     *
     * @param warehouseId 仓库ID
     * @return 删除结果
     */
    @DeleteMapping("/{warehouseId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse:remove')")
    public R<Boolean> remove(@PathVariable("warehouseId") Long warehouseId) {
        boolean success = warehouseService.removeWarehouse(warehouseId);
        return success ? R.success(true) : R.failed("删除仓库失败，仅草稿状态允许删除");
    }
}
