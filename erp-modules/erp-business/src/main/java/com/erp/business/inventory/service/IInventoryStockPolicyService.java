package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockPolicy;

/**
 * 库存策略服务接口。
 */
public interface IInventoryStockPolicyService {

    /**
     * 查询库存策略分页。
     *
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<InventoryStockPolicy> selectPage(Long orgId, Long warehouseId, Long itemId, Long pageNum, Long pageSize);

    /**
     * 查询库存策略详情。
     *
     * @param policyId 策略ID
     * @return 策略详情
     */
    InventoryStockPolicy getDetail(Long policyId);

    /**
     * 新增库存策略。
     *
     * @param policy 策略对象
     * @return true 表示成功
     */
    boolean create(InventoryStockPolicy policy);

    /**
     * 修改库存策略。
     *
     * @param policy 策略对象
     * @return true 表示成功
     */
    boolean update(InventoryStockPolicy policy);

    /**
     * 删除库存策略。
     *
     * @param policyId 策略ID
     * @return true 表示成功
     */
    boolean delete(Long policyId);
}
