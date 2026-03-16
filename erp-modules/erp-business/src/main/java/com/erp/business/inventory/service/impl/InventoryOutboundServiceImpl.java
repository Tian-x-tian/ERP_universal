package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryOutboundOrder;
import com.erp.business.inventory.domain.InventoryOutboundOrderLine;
import com.erp.business.inventory.mapper.InventoryOutboundOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryOutboundOrderMapper;
import com.erp.business.inventory.service.IInventoryOutboundService;
import com.erp.business.inventory.service.InventorySourceProgressCallback;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.inventory.support.InventoryBillStatusSupport;
import com.erp.business.inventory.support.InventoryValueSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 出库服务实现。
 */
@Service
public class InventoryOutboundServiceImpl implements IInventoryOutboundService {
    private static final String DEFAULT_BILL_TYPE = "SALES_OUTBOUND";

    private final InventoryOutboundOrderMapper orderMapper;
    private final InventoryOutboundOrderLineMapper lineMapper;
    private final InventoryStockEngineSupport stockEngineSupport;
    private final InventoryWorkflowGateway workflowGateway;
    private final InventorySourceProgressCallback sourceProgressCallback;
    private final SecurityUserResolver securityUserResolver;

    public InventoryOutboundServiceImpl(InventoryOutboundOrderMapper orderMapper,
            InventoryOutboundOrderLineMapper lineMapper,
            InventoryStockEngineSupport stockEngineSupport,
            InventoryWorkflowGateway workflowGateway,
            InventorySourceProgressCallback sourceProgressCallback,
            SecurityUserResolver securityUserResolver) {
        this.orderMapper = orderMapper;
        this.lineMapper = lineMapper;
        this.stockEngineSupport = stockEngineSupport;
        this.workflowGateway = workflowGateway;
        this.sourceProgressCallback = sourceProgressCallback;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询出库单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryOutboundOrder> selectPage(String billNo, String status, Long pageNum, Long pageSize) {
        Page<InventoryOutboundOrder> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryOutboundOrder> queryWrapper = new LambdaQueryWrapper<InventoryOutboundOrder>()
                .eq(InventoryOutboundOrder::getTenantId, currentTenantId())
                .like(StringUtils.hasText(billNo), InventoryOutboundOrder::getBillNo, trim(billNo))
                .eq(StringUtils.hasText(status), InventoryOutboundOrder::getStatus,
                        InventoryBillStatusSupport.normalize(status, InventoryBillStatusSupport.DRAFT))
                .orderByDesc(InventoryOutboundOrder::getUpdateTime)
                .orderByDesc(InventoryOutboundOrder::getCreateTime);
        return orderMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询出库单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @Override
    public InventoryOutboundOrder getDetail(Long orderId) {
        InventoryOutboundOrder order = loadOrder(orderId);
        order.setLines(loadLines(orderId));
        return order;
    }

    /**
     * 新增出库单。
     *
     * @param order 出库单
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(InventoryOutboundOrder order) {
        validateOrderForSave(order);
        String tenantId = currentTenantId();
        String operator = resolveOperator();
        Date now = new Date();
        order.setTenantId(tenantId);
        order.setBillNo(StringUtils.hasText(order.getBillNo()) ? trim(order.getBillNo()) : buildBillNo("OUT"));
        order.setBillType(StringUtils.hasText(order.getBillType()) ? trim(order.getBillType()) : DEFAULT_BILL_TYPE);
        order.setStatus(InventoryBillStatusSupport.DRAFT);
        order.setIdempotencyNo(StringUtils.hasText(order.getIdempotencyNo()) ? trim(order.getIdempotencyNo()) : order.getBillNo());
        order.setVersionNo(1);
        order.setRemark(InventoryValueSupport.trimToNull(order.getRemark()));
        order.setCreateBy(operator);
        order.setUpdateBy(operator);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        if (orderMapper.insert(order) <= 0) {
            return false;
        }
        saveLines(order.getOrderId(), tenantId, order.getLines());
        return true;
    }

    /**
     * 修改出库单。
     *
     * @param order 出库单
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(InventoryOutboundOrder order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("出库单ID不能为空");
        }
        validateOrderForSave(order);
        InventoryOutboundOrder existed = loadOrder(order.getOrderId());
        if (!InventoryBillStatusSupport.isEditable(existed.getStatus())) {
            throw new IllegalStateException("仅草稿出库单允许修改");
        }
        InventoryOutboundOrder updateEntity = new InventoryOutboundOrder();
        updateEntity.setOrderId(existed.getOrderId());
        updateEntity.setBillType(StringUtils.hasText(order.getBillType()) ? trim(order.getBillType()) : existed.getBillType());
        updateEntity.setOrgId(order.getOrgId());
        updateEntity.setWarehouseId(order.getWarehouseId());
        updateEntity.setSourceOrderType(InventoryValueSupport.trimToNull(order.getSourceOrderType()));
        updateEntity.setSourceOrderId(order.getSourceOrderId());
        updateEntity.setSourceOrderNo(InventoryValueSupport.trimToNull(order.getSourceOrderNo()));
        updateEntity.setProcessKey(InventoryValueSupport.trimToNull(order.getProcessKey()));
        updateEntity.setIdempotencyNo(StringUtils.hasText(order.getIdempotencyNo()) ? trim(order.getIdempotencyNo()) : existed.getIdempotencyNo());
        updateEntity.setRemark(InventoryValueSupport.trimToNull(order.getRemark()));
        updateEntity.setVersionNo((existed.getVersionNo() == null ? 1 : existed.getVersionNo()) + 1);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        int updated = orderMapper.update(updateEntity, new LambdaUpdateWrapper<InventoryOutboundOrder>()
                .eq(InventoryOutboundOrder::getOrderId, existed.getOrderId())
                .eq(InventoryOutboundOrder::getVersionNo, existed.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("出库单已变化，请刷新后重试");
        }
        lineMapper.delete(new LambdaQueryWrapper<InventoryOutboundOrderLine>().eq(InventoryOutboundOrderLine::getOrderId, existed.getOrderId()));
        saveLines(existed.getOrderId(), existed.getTenantId(), order.getLines());
        return true;
    }

    /**
     * 提交出库单。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long orderId) {
        InventoryOutboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.isEditable(existed.getStatus())) {
            throw new IllegalStateException("仅草稿出库单允许提交");
        }
        String targetStatus = InventoryBillStatusSupport.APPROVED;
        if (StringUtils.hasText(existed.getProcessKey())) {
            boolean accepted = workflowGateway.startWorkflow(existed.getProcessKey(), existed.getBillType(),
                    existed.getOrderId(), existed.getBillNo());
            if (!accepted) {
                throw new IllegalStateException("出库单审批流程发起失败");
            }
            targetStatus = InventoryBillStatusSupport.PENDING_APPROVAL;
        }
        return updateStatus(existed, targetStatus);
    }

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean approve(Long orderId) {
        InventoryOutboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.PENDING_APPROVAL.equals(existed.getStatus())) {
            throw new IllegalStateException("仅待审批出库单允许回写审批通过");
        }
        return updateStatus(existed, InventoryBillStatusSupport.APPROVED);
    }

    /**
     * 回写审批拒绝。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long orderId) {
        InventoryOutboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.PENDING_APPROVAL.equals(existed.getStatus())) {
            throw new IllegalStateException("仅待审批出库单允许回写驳回");
        }
        return updateStatus(existed, InventoryBillStatusSupport.CANCELLED);
    }

    /**
     * 执行出库。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean execute(Long orderId) {
        InventoryOutboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.isApproved(existed.getStatus())) {
            throw new IllegalStateException("仅已审批出库单允许执行");
        }
        updateStatus(existed, InventoryBillStatusSupport.EXECUTING);
        InventoryOutboundOrder latest = loadOrder(orderId);
        List<InventoryOutboundOrderLine> lines = loadLines(orderId);
        stockEngineSupport.applyOutbound(latest, lines);
        boolean success = updateStatus(loadOrder(orderId), InventoryBillStatusSupport.COMPLETED);
        if (success) {
            sourceProgressCallback.callback(latest.getSourceOrderType(), latest.getSourceOrderId(),
                    latest.getSourceOrderNo(), latest.getBillNo(), InventoryBillStatusSupport.COMPLETED);
        }
        return success;
    }

    /**
     * 取消出库单。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId) {
        InventoryOutboundOrder existed = loadOrder(orderId);
        if (InventoryBillStatusSupport.COMPLETED.equals(existed.getStatus())) {
            throw new IllegalStateException("已完成出库单不允许取消");
        }
        return updateStatus(existed, InventoryBillStatusSupport.CANCELLED);
    }

    /**
     * 按ID加载出库单。
     *
     * @param orderId 单据ID
     * @return 单据对象
     */
    private InventoryOutboundOrder loadOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("出库单ID不能为空");
        }
        InventoryOutboundOrder order = orderMapper.selectOne(new LambdaQueryWrapper<InventoryOutboundOrder>()
                .eq(InventoryOutboundOrder::getOrderId, orderId)
                .eq(InventoryOutboundOrder::getTenantId, currentTenantId()));
        if (order == null) {
            throw new ServiceException("出库单不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return order;
    }

    /**
     * 查询出库单行列表。
     *
     * @param orderId 单据ID
     * @return 单据行集合
     */
    private List<InventoryOutboundOrderLine> loadLines(Long orderId) {
        return lineMapper.selectList(new LambdaQueryWrapper<InventoryOutboundOrderLine>()
                .eq(InventoryOutboundOrderLine::getOrderId, orderId)
                .orderByAsc(InventoryOutboundOrderLine::getLineNo));
    }

    /**
     * 保存出库单行。
     *
     * @param orderId 单据ID
     * @param tenantId 租户编号
     * @param lines 单据行
     */
    private void saveLines(Long orderId, String tenantId, List<InventoryOutboundOrderLine> lines) {
        List<InventoryOutboundOrderLine> safeLines = lines == null ? new ArrayList<>() : lines;
        if (safeLines.isEmpty()) {
            throw new IllegalArgumentException("出库单至少需要一条明细");
        }
        int lineNo = 1;
        for (InventoryOutboundOrderLine line : safeLines) {
            validateLine(line == null ? null : line.getItemId(), line == null ? null : line.getQty(), "出库");
            line.setTenantId(tenantId);
            line.setOrderId(orderId);
            line.setLineNo(line.getLineNo() == null || line.getLineNo() < 1 ? lineNo : line.getLineNo());
            line.setBatchNo(InventoryValueSupport.trimToNull(line.getBatchNo()));
            line.setSerialNo(InventoryValueSupport.trimToNull(line.getSerialNo()));
            line.setRemark(InventoryValueSupport.trimToNull(line.getRemark()));
            lineMapper.insert(line);
            lineNo++;
        }
    }

    /**
     * 更新单据状态。
     *
     * @param existed 原单据
     * @param status 目标状态
     * @return true 表示成功
     */
    private boolean updateStatus(InventoryOutboundOrder existed, String status) {
        InventoryOutboundOrder updateEntity = new InventoryOutboundOrder();
        updateEntity.setOrderId(existed.getOrderId());
        updateEntity.setStatus(status);
        updateEntity.setVersionNo((existed.getVersionNo() == null ? 1 : existed.getVersionNo()) + 1);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        int updated = orderMapper.update(updateEntity, new LambdaUpdateWrapper<InventoryOutboundOrder>()
                .eq(InventoryOutboundOrder::getOrderId, existed.getOrderId())
                .eq(InventoryOutboundOrder::getVersionNo, existed.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("出库单状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 校验单据保存参数。
     *
     * @param order 单据对象
     */
    private void validateOrderForSave(InventoryOutboundOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("出库单不能为空");
        }
        if (order.getOrgId() == null || order.getWarehouseId() == null) {
            throw new IllegalArgumentException("组织ID和仓库ID不能为空");
        }
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new IllegalArgumentException("出库单至少需要一条明细");
        }
    }

    /**
     * 校验单据行参数。
     *
     * @param itemId 物料ID
     * @param qty 数量
     * @param scene 场景名称
     */
    private void validateLine(Long itemId, BigDecimal qty, String scene) {
        if (itemId == null) {
            throw new IllegalArgumentException(scene + "单明细物料不能为空");
        }
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(scene + "单明细数量必须大于0");
        }
    }

    /**
     * 生成业务单号。
     *
     * @param prefix 单号前缀
     * @return 单号
     */
    private String buildBillNo(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    /**
     * 获取当前租户。
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
     * 规范化字符串。
     *
     * @param value 原始字符串
     * @return 标准字符串
     */
    private String trim(String value) {
        return value == null ? null : value.trim();
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
