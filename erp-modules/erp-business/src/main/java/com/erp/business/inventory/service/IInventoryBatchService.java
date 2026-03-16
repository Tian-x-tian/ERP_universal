package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryBatchRecord;

/**
 * 批次查询服务接口。
 */
public interface IInventoryBatchService {

    /**
     * 查询批次分页。
     *
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param batchNo 批次号
     * @param status 状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryBatchRecord> selectPage(Long warehouseId, Long itemId, String batchNo, String status,
            Long pageNum, Long pageSize);
}
