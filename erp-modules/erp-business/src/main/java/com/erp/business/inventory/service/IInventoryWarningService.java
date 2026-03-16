package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryWarningRecord;

/**
 * 库存预警服务接口。
 */
public interface IInventoryWarningService {

    /**
     * 查询预警分页。
     *
     * @param warningType 预警类型
     * @param status 状态
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryWarningRecord> selectPage(String warningType, String status, Long warehouseId, Long itemId,
            Long pageNum, Long pageSize);

    /**
     * 标记预警已读。
     *
     * @param warningId 预警ID
     * @return true 表示成功
     */
    boolean markRead(Long warningId);

    /**
     * 关闭预警。
     *
     * @param warningId 预警ID
     * @return true 表示成功
     */
    boolean close(Long warningId);

    /**
     * 触发预警扫描。
     */
    void scanWarnings();
}
