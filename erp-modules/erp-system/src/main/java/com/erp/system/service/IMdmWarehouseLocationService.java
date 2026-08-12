package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmWarehouseLocation;

import java.util.List;

/**
 * MDM 仓库库位服务接口。
 */
public interface IMdmWarehouseLocationService extends IService<MdmWarehouseLocation> {

    List<MdmWarehouseLocation> selectLocationList(Long warehouseId, Long areaId, String locationCode, String locationName,
            String status);

    boolean createLocation(MdmWarehouseLocation location);

    boolean updateLocation(MdmWarehouseLocation location);

    boolean disableLocation(Long locationId, Integer versionNo);

    boolean removeLocation(Long locationId, Integer versionNo);
}
