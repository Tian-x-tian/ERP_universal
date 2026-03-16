package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryBatchRecord;
import com.erp.business.inventory.mapper.InventoryBatchRecordMapper;
import com.erp.business.inventory.service.IInventoryBatchService;
import com.erp.common.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 批次查询服务实现。
 */
@Service
public class InventoryBatchServiceImpl implements IInventoryBatchService {

    private final InventoryBatchRecordMapper batchRecordMapper;

    public InventoryBatchServiceImpl(InventoryBatchRecordMapper batchRecordMapper) {
        this.batchRecordMapper = batchRecordMapper;
    }

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
    @Override
    public Page<InventoryBatchRecord> selectPage(Long warehouseId, Long itemId, String batchNo, String status,
            Long pageNum, Long pageSize) {
        Page<InventoryBatchRecord> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryBatchRecord> queryWrapper = new LambdaQueryWrapper<InventoryBatchRecord>()
                .eq(InventoryBatchRecord::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventoryBatchRecord::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryBatchRecord::getItemId, itemId)
                .like(StringUtils.hasText(batchNo), InventoryBatchRecord::getBatchNo, batchNo == null ? null : batchNo.trim())
                .eq(StringUtils.hasText(status), InventoryBatchRecord::getStatus, status == null ? null : status.trim().toUpperCase())
                .orderByAsc(InventoryBatchRecord::getExpiryDate)
                .orderByDesc(InventoryBatchRecord::getUpdateTime);
        return batchRecordMapper.selectPage(page, queryWrapper);
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new IllegalStateException("当前租户上下文缺失");
        }
        return tenantId.trim();
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
