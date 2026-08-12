package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.SysRegion;
import com.erp.system.service.ISysRegionService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 区域主数据控制层
 */
@RestController
@RequestMapping("/system/region")
public class SysRegionController {

    private final ISysRegionService regionService;

    public SysRegionController(ISysRegionService regionService) {
        this.regionService = regionService;
    }

    /**
     * 查询区域列表。
     *
     * @return 区域列表
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('system:region:list')")
    public R<List<SysRegion>> list() {
        return R.success(normalizeRegionList(regionService.list()));
    }

    /**
     * 查询区域树。
     *
     * @return 区域树
     */
    @GetMapping("/tree")
    @PreAuthorize("@ss.hasPermi('system:region:list')")
    public R<List<SysRegion>> tree() {
        List<SysRegion> regionList = normalizeRegionList(regionService.list());
        return R.success(regionService.buildRegionTree(regionList));
    }

    /**
     * 查询区域详情。
     *
     * @param regionId 区域ID
     * @return 区域详情
     */
    @GetMapping("/{regionId}")
    @PreAuthorize("@ss.hasPermi('system:region:query')")
    public R<SysRegion> getInfo(@PathVariable("regionId") Long regionId) {
        return R.success(normalizeRegion(regionService.getById(regionId)));
    }

    /**
     * 新增区域。
     *
     * @param region 区域对象
     * @return 新增结果
     */
    @PostMapping
    @PreAuthorize("@ss.hasPermi('system:region:add')")
    public R<Boolean> add(@RequestBody SysRegion region) {
        if (region == null) {
            return R.failed("区域参数不能为空");
        }
        region.setStatus(StatusFieldSupport.normalizeBinaryStatus(region.getStatus()));
        return R.success(regionService.save(region));
    }

    /**
     * 修改区域。
     *
     * @param region 区域对象
     * @return 修改结果
     */
    @PutMapping
    @PreAuthorize("@ss.hasPermi('system:region:edit')")
    public R<Boolean> edit(@RequestBody SysRegion region) {
        if (region == null || region.getRegionId() == null) {
            return R.failed("区域ID不能为空");
        }
        return R.success(regionService.updateById(region));
    }

    /**
     * 删除区域。
     *
     * @param regionId 区域ID
     * @return 删除结果
     */
    @DeleteMapping("/{regionId}")
    @PreAuthorize("@ss.hasPermi('system:region:remove')")
    public R<Boolean> remove(@PathVariable("regionId") Long regionId) {
        return R.success(regionService.removeById(regionId));
    }

    /**
     * 规范区域列表中的状态字段，避免前端出现空白状态。
     *
     * @param regionList 区域列表
     * @return 状态字段已规范化的区域列表
     */
    private List<SysRegion> normalizeRegionList(List<SysRegion> regionList) {
        if (regionList == null || regionList.isEmpty()) {
            return regionList;
        }
        for (SysRegion region : regionList) {
            normalizeRegion(region);
        }
        return regionList;
    }

    /**
     * 规范区域状态字段。
     *
     * @param region 区域对象
     * @return 规范化后的区域对象
     */
    private SysRegion normalizeRegion(SysRegion region) {
        if (region != null) {
            region.setStatus(StatusFieldSupport.normalizeBinaryStatus(region.getStatus()));
        }
        return region;
    }
}
