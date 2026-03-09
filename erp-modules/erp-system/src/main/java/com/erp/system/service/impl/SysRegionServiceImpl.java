package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysRegion;
import com.erp.system.mapper.SysRegionMapper;
import com.erp.system.service.ISysRegionService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 区域主数据服务实现
 */
@Service
public class SysRegionServiceImpl extends ServiceImpl<SysRegionMapper, SysRegion> implements ISysRegionService {

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
}
