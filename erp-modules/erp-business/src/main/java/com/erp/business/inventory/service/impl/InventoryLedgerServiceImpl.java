package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockTxn;
import com.erp.business.inventory.mapper.InventoryStockBalanceMapper;
import com.erp.business.inventory.mapper.InventoryStockTxnMapper;
import com.erp.business.inventory.service.IInventoryLedgerService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 库存台账服务实现。
 */
@Service
public class InventoryLedgerServiceImpl implements IInventoryLedgerService {

    private final InventoryStockBalanceMapper stockBalanceMapper;
    private final InventoryStockTxnMapper stockTxnMapper;

    public InventoryLedgerServiceImpl(InventoryStockBalanceMapper stockBalanceMapper,
            InventoryStockTxnMapper stockTxnMapper) {
        this.stockBalanceMapper = stockBalanceMapper;
        this.stockTxnMapper = stockTxnMapper;
    }

    /**
     * 查询库存余额分页。
     *
     * @param tenantId 租户编号
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryStockBalance> selectBalancePage(String tenantId, Long orgId, Long warehouseId, Long itemId,
            Long pageNum, Long pageSize) {
        Page<InventoryStockBalance> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryStockBalance> queryWrapper = new LambdaQueryWrapper<InventoryStockBalance>()
                .eq(StringUtils.hasText(tenantId), InventoryStockBalance::getTenantId, tenantId)
                .eq(orgId != null, InventoryStockBalance::getOrgId, orgId)
                .eq(warehouseId != null, InventoryStockBalance::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryStockBalance::getItemId, itemId)
                .orderByDesc(InventoryStockBalance::getUpdateTime)
                .orderByDesc(InventoryStockBalance::getCreateTime);
        return stockBalanceMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询库存流水分页。
     *
     * @param tenantId 租户编号
     * @param billNo 单据编号
     * @param itemId 物料ID
     * @param actionType 动作类型
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryStockTxn> selectTxnPage(String tenantId, String billNo, Long itemId, String actionType,
            Long pageNum, Long pageSize) {
        Page<InventoryStockTxn> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryStockTxn> queryWrapper = new LambdaQueryWrapper<InventoryStockTxn>()
                .eq(StringUtils.hasText(tenantId), InventoryStockTxn::getTenantId, tenantId)
                .like(StringUtils.hasText(billNo), InventoryStockTxn::getBillNo, billNo == null ? null : billNo.trim())
                .eq(itemId != null, InventoryStockTxn::getItemId, itemId)
                .eq(StringUtils.hasText(actionType), InventoryStockTxn::getActionType,
                        actionType == null ? null : actionType.trim().toUpperCase())
                .orderByDesc(InventoryStockTxn::getCreateTime);
        return stockTxnMapper.selectPage(page, queryWrapper);
    }

    /**
     * 规范化页码。
     *
     * @param pageNum 原始页码
     * @return 标准页码
     */
    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化页长。
     *
     * @param pageSize 原始页长
     * @return 标准页长
     */
    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }
}
