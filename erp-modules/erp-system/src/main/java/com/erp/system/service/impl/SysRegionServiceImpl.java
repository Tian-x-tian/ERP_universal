package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysRegion;
import com.erp.system.mapper.SysRegionMapper;
import com.erp.system.service.ISysRegionService;
import com.erp.system.support.StatusFieldSupport;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 区域主数据服务实现
 */
@Service
public class SysRegionServiceImpl extends ServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {

    /**
     * 新增区域时规范状态字段和基础字段。
     *
     * @param entity 区域实体
     * @return 新增结果
     */
    @Override
    public boolean save(SysRegion entity) {
        normalizeRegion(entity, true, null);
        return super.save(entity);
    }

    /**
     * 修改区域时规范状态字段和更新时间。
     *
     * @param entity 区域实体
     * @return 修改结果
     */
    @Override
    public boolean updateById(SysRegion entity) {
        String currentStatus = null;
        if (entity != null && entity.getRegionId() != null) {
            SysRegion existedRegion = getById(entity.getRegionId());
            currentStatus = existedRegion == null ? null : existedRegion.getStatus();
        }
        normalizeRegion(entity, false, currentStatus);
        return super.updateById(entity);
    }

    /**
     * 构建区域树结构。
     *
     * @param regionList 区域列表
     * @return 区域树
     */
    @Override
    public List<SysRegion> buildRegionTree(List<SysRegion> regionList) {
        return buildChildren(regionList, 0L);
    }

    /**
     * 递归组装子节点。
     *
     * @param regionList 区域列表
     * @param parentId   父节点ID
     * @return 子节点列表
     */
    private List<SysRegion> buildChildren(List<SysRegion> regionList, Long parentId) {
        List<SysRegion> children = new ArrayList<>();
        for (SysRegion region : regionList) {
            if (parentId.equals(region.getParentId())) {
                region.setChildren(buildChildren(regionList, region.getRegionId()));
                children.add(region);
            }
        }
        return children;
    }

    /**
     * 规范区域核心字段，避免状态为空导致前端无法判断启停。
     *
     * @param region        区域对象
     * @param isCreate      是否为新增操作
     * @param currentStatus 当前已落库状态值（更新场景使用）
     */
    private void normalizeRegion(SysRegion region, boolean isCreate, String currentStatus) {
        if (region == null) {
            return;
        }
        if (isCreate) {
            region.setStatus(StatusFieldSupport.normalizeBinaryStatus(region.getStatus()));
        } else {
            region.setStatus(StatusFieldSupport.normalizeBinaryStatusForUpdate(region.getStatus(), currentStatus));
        }
        if (StringUtils.hasText(region.getRegionCode())) {
            region.setRegionCode(region.getRegionCode().trim());
        }
        if (StringUtils.hasText(region.getRegionName())) {
            region.setRegionName(region.getRegionName().trim());
        }
        if (isCreate) {
            region.setCreateTime(new Date());
        } else {
            region.setUpdateTime(new Date());
        }
    }
}
