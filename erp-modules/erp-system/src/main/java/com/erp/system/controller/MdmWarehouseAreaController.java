package com.erp.system.controller;

import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import com.erp.system.domain.MdmWarehouseArea;
import com.erp.system.domain.vo.MdmVersionActionBody;
import com.erp.system.service.IMdmWarehouseAreaService;
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
 * MDM 仓库库区控制器。
 */
@RestController
@RequestMapping("/system/mdm/warehouse-area")
public class MdmWarehouseAreaController {

    private final IMdmWarehouseAreaService warehouseAreaService;

    public MdmWarehouseAreaController(IMdmWarehouseAreaService warehouseAreaService) {
        this.warehouseAreaService = warehouseAreaService;
    }

    /**
     * 查询库区列表。
     *
     * @param warehouseId 仓库ID
     * @param areaCode 库区编码
     * @param areaName 库区名称
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-area:list')")
    public R<PageData<MdmWarehouseArea>> list(@RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "areaCode", required = false) String areaCode,
            @RequestParam(value = "areaName", required = false) String areaName,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "pageNum", required = false, defaultValue = "1") Long pageNum,
            @RequestParam(value = "pageSize", required = false, defaultValue = "20") Long pageSize) {
        return MdmResponseSupport.page(warehouseAreaService.selectAreaList(warehouseId, areaCode, areaName, status),
                pageNum, pageSize);
    }

    /**
     * 查询库区详情。
     *
     * @param areaId 库区ID
     * @return 库区详情
     */
    @GetMapping("/{areaId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-area:query')")
    public R<MdmWarehouseArea> detail(@PathVariable("areaId") Long areaId) {
        return R.success(warehouseAreaService.getById(areaId));
    }

    /**
     * 新增库区。
     *
     * @param area 库区对象
     * @return 处理结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-area:add')")
    public R<Boolean> add(@RequestBody MdmWarehouseArea area) {
        return warehouseAreaService.createArea(area) ? R.success(true) : R.failed("新增库区失败");
    }

    /**
     * 修改库区。
     *
     * @param area 库区对象
     * @return 处理结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-area:edit')")
    public R<Boolean> edit(@RequestBody MdmWarehouseArea area) {
        return warehouseAreaService.updateArea(area) ? R.success(true) : R.failed("修改库区失败");
    }

    /**
     * 停用库区。
     *
     * @param areaId 库区ID
     * @param actionBody 版本动作体
     * @return 处理结果
     */
    @PostMapping("/disable/{areaId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-area:disable')")
    public R<Boolean> disable(@PathVariable("areaId") Long areaId,
            @RequestBody(required = false) MdmVersionActionBody actionBody) {
        return warehouseAreaService.disableArea(areaId, actionBody == null ? null : actionBody.getVersionNo())
                ? R.success(true)
                : R.failed("停用库区失败");
    }

    /**
     * 删除库区。
     *
     * @param areaId 库区ID
     * @param actionBody 版本动作体
     * @return 处理结果
     */
    @DeleteMapping("/{areaId}")
    @PreAuthorize("@ss.hasPermi('system:mdm:warehouse-area:remove')")
    public R<Boolean> remove(@PathVariable("areaId") Long areaId,
            @RequestBody(required = false) MdmVersionActionBody actionBody) {
        return warehouseAreaService.removeArea(areaId, actionBody == null ? null : actionBody.getVersionNo())
                ? R.success(true)
                : R.failed("删除库区失败");
    }
}
