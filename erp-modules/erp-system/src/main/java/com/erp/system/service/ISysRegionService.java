package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysRegion;

import java.util.List;

/**
 * 区域主数据服务接口
 */
public interface ISysRegionService extends IService<SysRegion> {

    /**
     * 构建区域树结构。
     *
     * @param regionList 区域列表
     * @return 区域树
     */
    List<SysRegion> buildRegionTree(List<SysRegion> regionList);
}
