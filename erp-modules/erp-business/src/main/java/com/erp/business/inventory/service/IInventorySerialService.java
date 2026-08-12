package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventorySerialRecord;

/**
 * 序列号查询服务接口。
 */
public interface IInventorySerialService {

    /**
     * 查询序列号分页。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param serialNo 序列号
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventorySerialRecord> selectPage(Long warehouseId, Long itemId, String serialNo, String status,
            Long pageNum, Long pageSize);
}
