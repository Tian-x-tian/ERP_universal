package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryStockPolicy;
import com.erp.business.inventory.mapper.InventoryStockPolicyMapper;
import com.erp.business.inventory.service.IInventoryStockPolicyService;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 库存策略服务实现。
 */
@Service
public class InventoryStockPolicyServiceImpl implements IInventoryStockPolicyService {

    private final InventoryStockPolicyMapper stockPolicyMapper;

    public InventoryStockPolicyServiceImpl(InventoryStockPolicyMapper stockPolicyMapper) {
        this.stockPolicyMapper = stockPolicyMapper;
    }

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
    @Override
    public Page<InventoryStockPolicy> selectPage(Long orgId, Long warehouseId, Long itemId, Long pageNum, Long pageSize) {
        Page<InventoryStockPolicy> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryStockPolicy> queryWrapper = new LambdaQueryWrapper<InventoryStockPolicy>()
                .eq(InventoryStockPolicy::getTenantId, currentTenantId())
                .eq(orgId != null, InventoryStockPolicy::getOrgId, orgId)
                .eq(warehouseId != null, InventoryStockPolicy::getWarehouseId, warehouseId)
                .eq(itemId != null, InventoryStockPolicy::getItemId, itemId)
                .orderByDesc(InventoryStockPolicy::getUpdateTime)
                .orderByDesc(InventoryStockPolicy::getCreateTime);
        return stockPolicyMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询库存策略详情。
     *
     * @param policyId 策略ID
     * @return 策略详情
     */
    @Override
    public InventoryStockPolicy getDetail(Long policyId) {
        InventoryStockPolicy policy = loadPolicy(policyId);
        if (policy == null) {
            throw new ServiceException("库存策略不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return policy;
    }

    /**
     * 新增库存策略。
     *
     * @param policy 策略对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(InventoryStockPolicy policy) {
        validatePolicy(policy);
        Date now = new Date();
        policy.setTenantId(currentTenantId());
        policy.setAllowNegative(normalizeFlag(policy.getAllowNegative(), "N"));
        policy.setAllowExpiredOutbound(normalizeFlag(policy.getAllowExpiredOutbound(), "N"));
        return stockPolicyMapper.insert(policy) > 0;
    }

    /**
     * 修改库存策略。
     *
     * @param policy 策略对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(InventoryStockPolicy policy) {
        if (policy == null || policy.getPolicyId() == null) {
            throw new IllegalArgumentException("库存策略ID不能为空");
        }
        validatePolicy(policy);
        InventoryStockPolicy existed = getDetail(policy.getPolicyId());
        policy.setTenantId(existed.getTenantId());
        policy.setAllowNegative(normalizeFlag(policy.getAllowNegative(), "N"));
        policy.setAllowExpiredOutbound(normalizeFlag(policy.getAllowExpiredOutbound(), "N"));
        return stockPolicyMapper.updateById(policy) > 0;
    }

    /**
     * 删除库存策略。
     *
     * @param policyId 策略ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean delete(Long policyId) {
        getDetail(policyId);
        return stockPolicyMapper.deleteById(policyId) > 0;
    }

    /**
     * 校验库存策略。
     *
     * @param policy 策略对象
     */
    private void validatePolicy(InventoryStockPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("库存策略不能为空");
        }
        if (policy.getOrgId() == null || policy.getWarehouseId() == null || policy.getItemId() == null) {
            throw new IllegalArgumentException("组织、仓库和物料不能为空");
        }
    }

    /**
     * 按租户加载库存策略。
     *
     * @param policyId 策略ID
     * @return 策略对象
     */
    private InventoryStockPolicy loadPolicy(Long policyId) {
        return stockPolicyMapper.selectOne(new LambdaQueryWrapper<InventoryStockPolicy>()
                .eq(InventoryStockPolicy::getPolicyId, policyId)
                .eq(InventoryStockPolicy::getTenantId, currentTenantId()));
    }

    /**
     * 标准化 Y/N 标记。
     *
     * @param value 原始值
     * @param defaultValue 默认值
     * @return 标准值
     */
    private String normalizeFlag(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        return "Y".equalsIgnoreCase(value.trim()) ? "Y" : "N";
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
