package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventorySerialRecord;
import com.erp.business.inventory.mapper.InventorySerialRecordMapper;
import com.erp.business.inventory.service.IInventorySerialService;
import com.erp.common.core.context.TenantContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 序列号查询服务实现。
 */
@Service
public class InventorySerialServiceImpl implements IInventorySerialService {

    private final InventorySerialRecordMapper serialRecordMapper;

    public InventorySerialServiceImpl(InventorySerialRecordMapper serialRecordMapper) {
        this.serialRecordMapper = serialRecordMapper;
    }

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
    @Override
    public Page<InventorySerialRecord> selectPage(Long warehouseId, Long itemId, String serialNo, String status,
            Long pageNum, Long pageSize) {
        Page<InventorySerialRecord> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventorySerialRecord> queryWrapper = new LambdaQueryWrapper<InventorySerialRecord>()
                .eq(InventorySerialRecord::getTenantId, currentTenantId())
                .eq(warehouseId != null, InventorySerialRecord::getWarehouseId, warehouseId)
                .eq(itemId != null, InventorySerialRecord::getItemId, itemId)
                .like(StringUtils.hasText(serialNo), InventorySerialRecord::getSerialNo, serialNo == null ? null : serialNo.trim())
                .eq(StringUtils.hasText(status), InventorySerialRecord::getStatus, status == null ? null : status.trim().toUpperCase())
                .orderByDesc(InventorySerialRecord::getUpdateTime);
        return serialRecordMapper.selectPage(page, queryWrapper);
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
