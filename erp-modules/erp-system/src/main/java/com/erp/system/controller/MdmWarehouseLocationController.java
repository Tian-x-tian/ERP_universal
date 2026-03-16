package com.erp.system.controller;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmWarehouseLocation;
import com.erp.system.domain.vo.MdmVersionActionBody;
import com.erp.system.service.IMdmWarehouseLocationService;
import com.erp.system.support.MdmResponseSupport;
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
 * MDM 仓库库位控制器。
 */
@RestController
@RequestMapping("/system/mdm/warehouse-location")
public class MdmWarehouseLocationController {

    private final IMdmWarehouseLocationService warehouseLocationService;

    public MdmWarehouseLocationController(IMdmWarehouseLocationService warehouseLocationService) {
        this.warehouseLocationService = warehouseLocationService;
    }

    /**
     * 查询库位列表。
     *
     * @param warehouseId 仓库ID
     * @param areaId 库区ID
     * @param locationCode 库位编码
     * @param locationName 库位名称
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-location:list')")
    public R<PageData<MdmWarehouseLocation>> list(
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "areaId", required = false) Long areaId,
            @RequestParam(value = "locationCode", required = false) String locationCode,
            @RequestParam(value = "locationName", required = false) String locationName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return MdmResponseSupport.page(
                warehouseLocationService.selectLocationList(warehouseId, areaId, locationCode, locationName, status),
                pageNum, pageSize);
    }

    /**
     * 查询库位详情。
     *
     * @param locationId 库位ID
     * @return 库位详情
     */
    @GetMapping("/{locationId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-location:query')")
    public R<MdmWarehouseLocation> detail(@PathVariable("locationId") Long locationId) {
        return R.success(warehouseLocationService.getById(locationId));
    }

    /**
     * 新增库位。
     *
     * @param location 库位对象
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-location:add')")
    public R<Boolean> add(@RequestBody MdmWarehouseLocation location) {
        return warehouseLocationService.createLocation(location) ? R.success(true) : R.failed("新增库位失败");
    }

    /**
     * 修改库位。
     *
     * @param location 库位对象
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-location:edit')")
    public R<Boolean> edit(@RequestBody MdmWarehouseLocation location) {
        return warehouseLocationService.updateLocation(location) ? R.success(true) : R.failed("修改库位失败");
    }

    /**
     * 停用库位。
     *
     * @param locationId 库位ID
     * @param actionBody 版本动作体
     * @return 处理结果
     */
    @PostMapping("/disable/{locationId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-location:disable')")
    public R<Boolean> disable(@PathVariable("locationId") Long locationId,
            @RequestBody(required = false) MdmVersionActionBody actionBody) {
        return warehouseLocationService.disableLocation(locationId, actionBody == null ? null : actionBody.getVersionNo())
                ? R.success(true)
                : R.failed("停用库位失败");
    }

    /**
     * 删除库位。
     *
     * @param locationId 库位ID
     * @param actionBody 版本动作体
     * @return 处理结果
     */
    @DeleteMapping("/{locationId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-location:remove')")
    public R<Boolean> remove(@PathVariable("locationId") Long locationId,
            @RequestBody(required = false) MdmVersionActionBody actionBody) {
        return warehouseLocationService.removeLocation(locationId, actionBody == null ? null : actionBody.getVersionNo())
                ? R.success(true)
                : R.failed("删除库位失败");
    }
}
