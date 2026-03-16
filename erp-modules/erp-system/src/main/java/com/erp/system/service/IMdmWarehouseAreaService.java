package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmWarehouseArea;

import java.util.List;

/**
 * MDM 仓库库区服务接口。
 */
public interface IMdmWarehouseAreaService extends IService<MdmWarehouseArea> {

    List<MdmWarehouseArea> selectAreaList(Long warehouseId, String areaCode, String areaName, String status);

    boolean createArea(MdmWarehouseArea area);

    boolean updateArea(MdmWarehouseArea area);

    boolean disableArea(Long areaId, Integer versionNo);

    boolean removeArea(Long areaId, Integer versionNo);
}
