package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmWarehouse;

import java.util.List;

/**
 * MDM 仓库主数据服务接口。
 */
public interface IMdmWarehouseService extends IService<MdmWarehouse> {

    /**
     * 查询仓库列表。
     *
     * @param whCode 仓库编码
     * @param whName 仓库名称
     * @param status 状态
     * @return 仓库列表
     */
    List<MdmWarehouse> selectWarehouseList(String whCode, String whName, String status);

    /**
     * 新增仓库。
     *
     * @param warehouse 仓库对象
     * @return true 表示成功
     */
    boolean createWarehouse(MdmWarehouse warehouse);

    /**
     * 修改仓库。
     *
     * @param warehouse 仓库对象
     * @return true 表示成功
     */
    boolean updateWarehouse(MdmWarehouse warehouse);

    /**
     * 停用仓库。
     *
     * @param warehouseId 仓库ID
     * @return true 表示成功
     */
    boolean disableWarehouse(Long warehouseId);

    /**
     * 删除仓库（逻辑删除）。
     *
     * @param warehouseId 仓库ID
     * @return true 表示成功
     */
    boolean removeWarehouse(Long warehouseId);
}
