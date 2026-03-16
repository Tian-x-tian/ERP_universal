package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryBatchRecord;
import com.erp.business.inventory.domain.InventoryStockBalance;
import com.erp.business.inventory.domain.InventoryStockPolicy;
import com.erp.business.inventory.domain.InventoryWarningRecord;
import com.erp.business.inventory.domain.MdmItem;
import com.erp.business.inventory.mapper.InventoryBatchRecordMapper;
import com.erp.business.inventory.mapper.InventoryStockBalanceMapper;
import com.erp.business.inventory.mapper.InventoryStockPolicyMapper;
import com.erp.business.inventory.mapper.InventoryWarningRecordMapper;
import com.erp.business.inventory.mapper.MdmItemMapper;
import com.erp.business.inventory.service.IInventoryWarningService;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 库存预警服务实现。
 */
@Service
public class InventoryWarningServiceImpl implements IInventoryWarningService {

    private final InventoryWarningRecordMapper warningRecordMapper;
    private final InventoryStockPolicyMapper stockPolicyMapper;
    private final InventoryStockBalanceMapper stockBalanceMapper;
    private final InventoryBatchRecordMapper batchRecordMapper;
    private final MdmItemMapper itemMapper;
    private final SecurityUserResolver securityUserResolver;

    public InventoryWarningServiceImpl(InventoryWarningRecordMapper warningRecordMapper,
            InventoryStockPolicyMapper stockPolicyMapper,
            InventoryStockBalanceMapper stockBalanceMapper,
            InventoryBatchRecordMapper batchRecordMapper,
            MdmItemMapper itemMapper,
            SecurityUserResolver securityUserResolver) {
        this.warningRecordMapper = warningRecordMapper;
        this.stockPolicyMapper = stockPolicyMapper;
        this.stockBalanceMapper = stockBalanceMapper;
        this.batchRecordMapper = batchRecordMapper;
        this.itemMapper = itemMapper;
        this.securityUserResolver = securityUserResolver;
    }

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
    @Override
    public Page<InventoryWarningRecord> selectPage(String warningType, String status, Long warehouseId, Long itemId,
            Long pageNum, Long pageSize) {
        Page<InventoryWarningRecord> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryWarningRecord> queryWrapper = new LambdaQueryWrapper<InventoryWarningRecord>()
                .eq(InventoryWarningRecord::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(warningType), InventoryWarningRecord::getWarningType,
                        warningType == null ? null : warningType.trim().toUpperCase())
                .eq(StringUtils.hasText(status), InventoryWarningRecord::getStatus,
                        status == null ? null : status.trim().toUpperCase())
                .eq(warehouseId != null, InventoryWarningRecord::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryWarningRecord::getItemId, itemId)
                .orderByDesc(InventoryWarningRecord::getUpdateTime)
                .orderByDesc(InventoryWarningRecord::getCreateTime);
        return warningRecordMapper.selectPage(page, queryWrapper);
    }

    /**
     * 标记预警已读。
     *
     * @param warningId 预警ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean markRead(Long warningId) {
        return updateStatus(loadWarning(warningId), "READ");
    }

    /**
     * 关闭预警。
     *
     * @param warningId 预警ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean close(Long warningId) {
        return updateStatus(loadWarning(warningId), "CLOSED");
    }

    /**
     * 触发预警扫描。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanWarnings() {
        List<InventoryStockBalance> balances = stockBalanceMapper.selectList(new LambdaQueryWrapper<InventoryStockBalance>()
                .eq(InventoryStockBalance::getTenantId, currentTenantId()));
        for (InventoryStockBalance balance : balances) {
            InventoryStockPolicy policy = findPolicy(balance.getOrgId(), balance.getWarehouseId(), balance.getItemId());
            if (policy != null) {
                BigDecimal availableQty = safeQty(balance.getAvailableQty());
                BigDecimal onHandQty = safeQty(balance.getOnHandQty());
                if (policy.getSafetyQty() != null && availableQty.compareTo(policy.getSafetyQty()) < 0) {
                    upsertWarning("LOW_STOCK", "LOW_STOCK:" + balance.getBalanceId(), balance,
                            "库存低于安全库存：" + availableQty + " < " + policy.getSafetyQty());
                }
                if (policy.getMaxQty() != null && onHandQty.compareTo(policy.getMaxQty()) > 0) {
                    upsertWarning("HIGH_STOCK", "HIGH_STOCK:" + balance.getBalanceId(), balance,
                            "库存高于上限库存：" + onHandQty + " > " + policy.getMaxQty());
                }
                if (policy.getStagnantDays() != null && policy.getStagnantDays() > 0 && balance.getLastTxnTime() != null) {
                    long stagnantDays = ChronoUnit.DAYS.between(toLocalDate(balance.getLastTxnTime()), LocalDate.now());
                    if (stagnantDays >= policy.getStagnantDays()) {
                        upsertWarning("STAGNANT", "STAGNANT:" + balance.getBalanceId(), balance,
                                "库存连续 " + stagnantDays + " 天未发生变动");
                    }
                }
            }
            if (safeQty(balance.getOnHandQty()).compareTo(BigDecimal.ZERO) < 0) {
                upsertWarning("NEGATIVE_STOCK", "NEGATIVE_STOCK:" + balance.getBalanceId(), balance,
                        "库存已出现负库存：" + safeQty(balance.getOnHandQty()));
            }
        }

        List<InventoryBatchRecord> batches = batchRecordMapper.selectList(new LambdaQueryWrapper<InventoryBatchRecord>()
                .eq(InventoryBatchRecord::getTenantId, currentTenantId())
                .gt(InventoryBatchRecord::getCurrentQty, BigDecimal.ZERO));
        for (InventoryBatchRecord batch : batches) {
            if (batch.getExpiryDate() == null) {
                continue;
            }
            long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(batch.getExpiryDate()));
            InventoryStockBalance balance = stockBalanceMapper.selectOne(new LambdaQueryWrapper<InventoryStockBalance>()
                    .eq(InventoryStockBalance::getTenantId, batch.getTenantId())
                    .eq(InventoryStockBalance::getWarehouseId, batch.getWarehouseId())
                    .eq(InventoryStockBalance::getItemId, batch.getItemId())
                    .eq(InventoryStockBalance::getBatchNo, batch.getBatchNo())
                    .last("limit 1"));
            InventoryStockPolicy policy = findPolicy(balance == null ? batch.getOrgId() : balance.getOrgId(),
                    batch.getWarehouseId(), batch.getItemId());
            MdmItem item = itemMapper.selectById(batch.getItemId());
            int warnDays = policy != null && policy.getExpiryWarnDays() != null
                    ? policy.getExpiryWarnDays()
                    : (item != null && item.getDefaultExpiryWarnDays() != null ? item.getDefaultExpiryWarnDays() : 0);
            if (remainingDays < 0) {
                upsertWarning("EXPIRED", "EXPIRED:" + batch.getBatchId(), batch, "批次已过期");
            } else if (warnDays > 0 && remainingDays <= warnDays) {
                upsertWarning("EXPIRING", "EXPIRING:" + batch.getBatchId(), batch,
                        "批次临期，剩余 " + remainingDays + " 天");
            }
        }
    }

    /**
     * 按租户加载预警记录。
     *
     * @param warningId 预警ID
     * @return 预警记录
     */
    private InventoryWarningRecord loadWarning(Long warningId) {
        InventoryWarningRecord warning = warningRecordMapper.selectOne(new LambdaQueryWrapper<InventoryWarningRecord>()
                .eq(InventoryWarningRecord::getWarningId, warningId)
                .eq(InventoryWarningRecord::getTenantId, currentTenantId()));
        if (warning == null) {
            throw new ServiceException("预警记录不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return warning;
    }

    /**
     * 更新预警状态。
     *
     * @param warning 预警记录
     * @param status 目标状态
     * @return true 表示成功
     */
    private boolean updateStatus(InventoryWarningRecord warning, String status) {
        InventoryWarningRecord updateEntity = new InventoryWarningRecord();
        updateEntity.setWarningId(warning.getWarningId());
        updateEntity.setStatus(status);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return warningRecordMapper.updateById(updateEntity) > 0;
    }

    /**
     * 查找最匹配的库存策略。
     *
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @return 库存策略
     */
    private InventoryStockPolicy findPolicy(Long orgId, Long warehouseId, Long itemId) {
        return stockPolicyMapper.selectOne(new LambdaQueryWrapper<InventoryStockPolicy>()
                .eq(InventoryStockPolicy::getTenantId, currentTenantId())
                .eq(orgId != null, InventoryStockPolicy::getOrgId, orgId)
                .eq(warehouseId != null, InventoryStockPolicy::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryStockPolicy::getItemId, itemId)
                .last("limit 1"));
    }

    /**
     * 根据余额生成或刷新预警。
     *
     * @param warningType 预警类型
     * @param warningKey 预警幂等键
     * @param balance 余额对象
     * @param message 预警消息
     */
    private void upsertWarning(String warningType, String warningKey, InventoryStockBalance balance, String message) {
        InventoryWarningRecord existing = warningRecordMapper.selectOne(new LambdaQueryWrapper<InventoryWarningRecord>()
                .eq(InventoryWarningRecord::getTenantId, currentTenantId())
                .eq(InventoryWarningRecord::getWarningKey, warningKey)
                .last("limit 1"));
        saveOrUpdateWarning(existing, warningType, warningKey, balance.getOrgId(), balance.getWarehouseId(),
                balance.getItemId(), balance.getBatchNo(), balance.getSerialNo(), message);
    }

    /**
     * 根据批次生成或刷新预警。
     *
     * @param warningType 预警类型
     * @param warningKey 预警幂等键
     * @param batch 批次对象
     * @param message 预警消息
     */
    private void upsertWarning(String warningType, String warningKey, InventoryBatchRecord batch, String message) {
        InventoryWarningRecord existing = warningRecordMapper.selectOne(new LambdaQueryWrapper<InventoryWarningRecord>()
                .eq(InventoryWarningRecord::getTenantId, currentTenantId())
                .eq(InventoryWarningRecord::getWarningKey, warningKey)
                .last("limit 1"));
        saveOrUpdateWarning(existing, warningType, warningKey, batch.getOrgId(), batch.getWarehouseId(),
                batch.getItemId(), batch.getBatchNo(), null, message);
    }

    /**
     * 保存或更新预警记录。
     *
     * @param existing 现有预警
     * @param warningType 预警类型
     * @param warningKey 预警幂等键
     * @param orgId 组织ID
     * @param warehouseId 仓库ID
     * @param itemId 物料ID
     * @param batchNo 批次号
     * @param serialNo 序列号
     * @param message 预警消息
     */
    private void saveOrUpdateWarning(InventoryWarningRecord existing, String warningType, String warningKey,
            Long orgId, Long warehouseId, Long itemId, String batchNo, String serialNo, String message) {
        Date now = new Date();
        if (existing == null) {
            InventoryWarningRecord warning = new InventoryWarningRecord();
            warning.setTenantId(currentTenantId());
            warning.setWarningType(warningType);
            warning.setStatus("NEW");
            warning.setOrgId(orgId);
            warning.setWarehouseId(warehouseId);
            warning.setItemId(itemId);
            warning.setBatchNo(batchNo);
            warning.setSerialNo(serialNo);
            warning.setWarningKey(warningKey);
            warning.setWarningMessage(message);
            warning.setCreateBy(resolveOperator());
            warning.setUpdateBy(resolveOperator());
            warning.setCreateTime(now);
            warning.setUpdateTime(now);
            warningRecordMapper.insert(warning);
            return;
        }
        InventoryWarningRecord updateEntity = new InventoryWarningRecord();
        updateEntity.setWarningId(existing.getWarningId());
        updateEntity.setWarningType(warningType);
        updateEntity.setStatus("NEW");
        updateEntity.setWarningMessage(message);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(now);
        warningRecordMapper.updateById(updateEntity);
    }

    /**
     * 安全处理数量。
     *
     * @param value 原始数量
     * @return 标准数量
     */
    private BigDecimal safeQty(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /**
     * 日期转本地日期。
     *
     * @param date 日期对象
     * @return 本地日期
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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
     * 获取当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
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
