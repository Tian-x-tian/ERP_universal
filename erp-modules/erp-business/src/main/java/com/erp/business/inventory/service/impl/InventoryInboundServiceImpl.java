package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryInboundOrder;
import com.erp.business.inventory.domain.InventoryInboundOrderLine;
import com.erp.business.inventory.mapper.InventoryInboundOrderLineMapper;
import com.erp.business.inventory.mapper.InventoryInboundOrderMapper;
import com.erp.business.inventory.service.IInventoryInboundService;
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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 入库服务实现。
 */
@Service
public class InventoryInboundServiceImpl implements IInventoryInboundService {
    private static final String DEFAULT_BILL_TYPE = "PURCHASE_INBOUND";

    private final InventoryInboundOrderMapper orderMapper;
    private final InventoryInboundOrderLineMapper lineMapper;
    private final InventoryStockEngineSupport stockEngineSupport;
    private final InventoryWorkflowGateway workflowGateway;
    private final InventorySourceProgressCallback sourceProgressCallback;
    private final SecurityUserResolver securityUserResolver;

    public InventoryInboundServiceImpl(InventoryInboundOrderMapper orderMapper,
            InventoryInboundOrderLineMapper lineMapper,
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
     * 查询入库单分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryInboundOrder> selectPage(String billNo, String status, Long pageNum, Long pageSize) {
        Page<InventoryInboundOrder> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        String tenantId = currentTenantId();
        LambdaQueryWrapper<InventoryInboundOrder> queryWrapper = new LambdaQueryWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getTenantId, tenantId)
                .like(StringUtils.hasText(billNo), InventoryInboundOrder::getBillNo, trim(billNo))
                .eq(StringUtils.hasText(status), InventoryInboundOrder::getStatus,
                        InventoryBillStatusSupport.normalize(status, InventoryBillStatusSupport.DRAFT))
                .orderByDesc(InventoryInboundOrder::getUpdateTime)
                .orderByDesc(InventoryInboundOrder::getCreateTime);
        return orderMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询入库单详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @Override
    public InventoryInboundOrder getDetail(Long orderId) {
        InventoryInboundOrder order = loadOrder(orderId);
        order.setLines(loadLines(orderId));
        return order;
    }

    /**
     * 新增入库单。
     *
     * @param order 入库单
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(InventoryInboundOrder order) {
        validateOrderForSave(order);
        Date now = new Date();
        String tenantId = currentTenantId();
        String operator = resolveOperator();
        order.setTenantId(tenantId);
        order.setBillNo(StringUtils.hasText(order.getBillNo()) ? trim(order.getBillNo()) : buildBillNo("INB"));
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
     * 修改入库单。
     *
     * @param order 入库单
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(InventoryInboundOrder order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("入库单ID不能为空");
        }
        validateOrderForSave(order);
        InventoryInboundOrder existed = loadOrder(order.getOrderId());
        if (!InventoryBillStatusSupport.isEditable(existed.getStatus())) {
            throw new IllegalStateException("仅草稿入库单允许修改");
        }
        InventoryInboundOrder updateEntity = new InventoryInboundOrder();
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
        int updated = orderMapper.update(updateEntity, new LambdaUpdateWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getOrderId, existed.getOrderId())
                .eq(InventoryInboundOrder::getVersionNo, existed.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("入库单已变化，请刷新后重试");
        }
        lineMapper.delete(new LambdaQueryWrapper<InventoryInboundOrderLine>().eq(InventoryInboundOrderLine::getOrderId, existed.getOrderId()));
        saveLines(existed.getOrderId(), existed.getTenantId(), order.getLines());
        return true;
    }

    /**
     * 提交入库单。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long orderId) {
        InventoryInboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.isEditable(existed.getStatus())) {
            throw new IllegalStateException("仅草稿入库单允许提交");
        }
        String targetStatus = InventoryBillStatusSupport.APPROVED;
        if (StringUtils.hasText(existed.getProcessKey())) {
            boolean accepted = workflowGateway.startWorkflow(existed.getProcessKey(), existed.getBillType(),
                    existed.getOrderId(), existed.getBillNo());
            if (!accepted) {
                throw new IllegalStateException("入库单审批流程发起失败");
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
        InventoryInboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.PENDING_APPROVAL.equals(existed.getStatus())) {
            throw new IllegalStateException("仅待审批入库单允许回写审批通过");
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
        InventoryInboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.PENDING_APPROVAL.equals(existed.getStatus())) {
            throw new IllegalStateException("仅待审批入库单允许回写驳回");
        }
        return updateStatus(existed, InventoryBillStatusSupport.CANCELLED);
    }

    /**
     * 执行入库。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean execute(Long orderId) {
        InventoryInboundOrder existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.isApproved(existed.getStatus())) {
            throw new IllegalStateException("仅已审批入库单允许执行");
        }
        updateStatus(existed, InventoryBillStatusSupport.EXECUTING);
        InventoryInboundOrder latest = loadOrder(orderId);
        List<InventoryInboundOrderLine> lines = loadLines(orderId);
        stockEngineSupport.applyInbound(latest, lines);
        boolean success = updateStatus(loadOrder(orderId), InventoryBillStatusSupport.COMPLETED);
        if (success) {
            sourceProgressCallback.callback(latest.getSourceOrderType(), latest.getSourceOrderId(),
                    latest.getSourceOrderNo(), latest.getBillNo(), InventoryBillStatusSupport.COMPLETED);
        }
        return success;
    }

    /**
     * 取消入库单。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId) {
        InventoryInboundOrder existed = loadOrder(orderId);
        if (InventoryBillStatusSupport.COMPLETED.equals(existed.getStatus())) {
            throw new IllegalStateException("已完成入库单不允许取消");
        }
        return updateStatus(existed, InventoryBillStatusSupport.CANCELLED);
    }

    /**
     * 加载入库单并校验租户。
     *
     * @param orderId 单据ID
     * @return 单据对象
     */
    private InventoryInboundOrder loadOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("入库单ID不能为空");
        }
        InventoryInboundOrder order = orderMapper.selectOne(new LambdaQueryWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getOrderId, orderId)
                .eq(InventoryInboundOrder::getTenantId, currentTenantId()));
        if (order == null) {
            throw new ServiceException("入库单不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return order;
    }

    /**
     * 查询入库单行列表。
     *
     * @param orderId 单据ID
     * @return 单据行集合
     */
    private List<InventoryInboundOrderLine> loadLines(Long orderId) {
        return lineMapper.selectList(new LambdaQueryWrapper<InventoryInboundOrderLine>()
                .eq(InventoryInboundOrderLine::getOrderId, orderId)
                .orderByAsc(InventoryInboundOrderLine::getLineNo));
    }

    /**
     * 保存入库单行。
     *
     * @param orderId 单据ID
     * @param tenantId 租户编号
     * @param lines 单据行
     */
    private void saveLines(Long orderId, String tenantId, List<InventoryInboundOrderLine> lines) {
        List<InventoryInboundOrderLine> safeLines = lines == null ? new ArrayList<>() : lines;
        if (safeLines.isEmpty()) {
            throw new IllegalArgumentException("入库单至少需要一条明细");
        }
        int lineNo = 1;
        for (InventoryInboundOrderLine line : safeLines) {
            validateLine(line == null ? null : line.getItemId(), line == null ? null : line.getQty(), "入库");
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
    private boolean updateStatus(InventoryInboundOrder existed, String status) {
        InventoryInboundOrder updateEntity = new InventoryInboundOrder();
        updateEntity.setOrderId(existed.getOrderId());
        updateEntity.setStatus(status);
        updateEntity.setVersionNo((existed.getVersionNo() == null ? 1 : existed.getVersionNo()) + 1);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        int updated = orderMapper.update(updateEntity, new LambdaUpdateWrapper<InventoryInboundOrder>()
                .eq(InventoryInboundOrder::getOrderId, existed.getOrderId())
                .eq(InventoryInboundOrder::getVersionNo, existed.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("入库单状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 校验单据保存参数。
     *
     * @param order 单据对象
     */
    private void validateOrderForSave(InventoryInboundOrder order) {
        if (order == null) {
            throw new IllegalArgumentException("入库单不能为空");
        }
        if (order.getOrgId() == null || order.getWarehouseId() == null) {
            throw new IllegalArgumentException("组织ID和仓库ID不能为空");
        }
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new IllegalArgumentException("入库单至少需要一条明细");
        }
    }

    /**
     * 校验单据行参数。
     *
     * @param itemId 物料ID
     * @param qty 数量
     * @param scene 场景名称
     */
    private void validateLine(Long itemId, java.math.BigDecimal qty, String scene) {
        if (itemId == null) {
            throw new IllegalArgumentException(scene + "单明细物料不能为空");
        }
        if (qty == null || qty.compareTo(java.math.BigDecimal.ZERO) <= 0) {
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
     * 解析当前租户编号。
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
