package com.erp.business.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.purchase.domain.PurchaseOrder;
import com.erp.business.purchase.domain.PurchaseOrderLine;
import com.erp.business.purchase.mapper.PurchaseOrderLineMapper;
import com.erp.business.purchase.mapper.PurchaseOrderMapper;
import com.erp.business.purchase.service.IPurchaseOrderService;
import com.erp.business.purchase.service.PurchaseWorkflowGateway;
import com.erp.business.purchase.support.PurchaseBillStatusSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.exception.ServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 采购订单服务实现。
 *
 * <p>状态流转一律经 {@link PurchaseBillStatusSupport} 校验；状态变更使用
 * 版本号条件更新，避免并发下的丢失更新。
 */
@Service
public class PurchaseOrderServiceImpl implements IPurchaseOrderService {
    private static final Logger log = LoggerFactory.getLogger(PurchaseOrderServiceImpl.class);

    /** 单据类型 */
    public static final String BILL_TYPE = "PURCHASE_ORDER";
    /** 审批阈值参数键 */
    private static final String THRESHOLD_CONFIG_KEY = "purchase.order.approval.threshold";
    /** 阈值取不到时的兜底值 */
    private static final BigDecimal DEFAULT_THRESHOLD = new BigDecimal("10000");
    /** 审批流程标识 */
    private static final String PROCESS_KEY = "purchase_order_approval";
    private static final String BILL_NO_PREFIX = "PO";
    private static final String LINE_STATUS_OPEN = "OPEN";

    private final PurchaseOrderMapper orderMapper;
    private final PurchaseOrderLineMapper orderLineMapper;
    private final PurchaseWorkflowGateway workflowGateway;
    private final InternalPlatformClient platformClient;
    private final SecurityUserResolver securityUserResolver;

    public PurchaseOrderServiceImpl(PurchaseOrderMapper orderMapper,
            PurchaseOrderLineMapper orderLineMapper,
            PurchaseWorkflowGateway workflowGateway,
            InternalPlatformClient platformClient,
            SecurityUserResolver securityUserResolver) {
        this.orderMapper = orderMapper;
        this.orderLineMapper = orderLineMapper;
        this.workflowGateway = workflowGateway;
        this.platformClient = platformClient;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 分页查询采购订单。
     *
     * @param status   状态
     * @param orderNo  订单号
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @Override
    public Page<PurchaseOrder> selectPage(String status, String orderNo, long pageNum, long pageSize) {
        Page<PurchaseOrder> page = new Page<>(Math.max(1, pageNum), Math.max(1, Math.min(200, pageSize)));
        return orderMapper.selectPage(page, new LambdaQueryWrapper<PurchaseOrder>()
                .eq(StringUtils.hasText(status), PurchaseOrder::getStatus, trim(status))
                .like(StringUtils.hasText(orderNo), PurchaseOrder::getOrderNo, trim(orderNo))
                .orderByDesc(PurchaseOrder::getOrderId));
    }

    /**
     * 查询采购订单详情（含行）。
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    @Override
    public PurchaseOrder getDetail(Long orderId) {
        PurchaseOrder order = requireOrder(orderId);
        order.setLines(orderLineMapper.selectList(new LambdaQueryWrapper<PurchaseOrderLine>()
                .eq(PurchaseOrderLine::getOrderId, orderId)
                .orderByAsc(PurchaseOrderLine::getLineNo)));
        return order;
    }

    /**
     * 新增采购订单。
     *
     * @param order 订单
     * @return 订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseOrder order) {
        if (order == null || order.getLines() == null || order.getLines().isEmpty()) {
            throw new ServiceException("采购订单至少需要一行明细");
        }
        Date now = new Date();
        String operator = resolveOperator();
        order.setOrderId(null);
        order.setTenantId(currentTenantId());
        order.setOrderNo(StringUtils.hasText(order.getOrderNo()) ? trim(order.getOrderNo()) : buildBillNo());
        order.setStatus(PurchaseBillStatusSupport.ORDER_DRAFT);
        order.setProcessKey(PROCESS_KEY);
        order.setIdempotencyNo(StringUtils.hasText(order.getIdempotencyNo())
                ? trim(order.getIdempotencyNo()) : order.getOrderNo());
        order.setCurrencyCode(StringUtils.hasText(order.getCurrencyCode()) ? trim(order.getCurrencyCode()) : "CNY");
        order.setVersionNo(1);
        applyTotals(order);
        if (orderMapper.insert(order) <= 0) {
            throw new ServiceException("采购订单保存失败");
        }
        saveLines(order);
        return order.getOrderId();
    }

    /**
     * 修改采购订单（仅草稿可改）。
     *
     * @param order 订单
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(PurchaseOrder order) {
        PurchaseOrder existed = requireOrder(order == null ? null : order.getOrderId());
        if (!PurchaseBillStatusSupport.ORDER_DRAFT.equals(existed.getStatus())) {
            throw new ServiceException("仅草稿状态的采购订单可以修改");
        }
        existed.setSupplierId(order.getSupplierId());
        existed.setSupplierCode(trim(order.getSupplierCode()));
        existed.setSupplierName(trim(order.getSupplierName()));
        existed.setOrderDate(order.getOrderDate());
        existed.setExpectDate(order.getExpectDate());
        existed.setRemark(trim(order.getRemark()));
        existed.setLines(order.getLines());
        applyTotals(existed);
        if (orderMapper.updateById(existed) <= 0) {
            return false;
        }
        orderLineMapper.delete(new LambdaQueryWrapper<PurchaseOrderLine>()
                .eq(PurchaseOrderLine::getOrderId, existed.getOrderId()));
        saveLines(existed);
        return true;
    }

    /**
     * 删除采购订单（仅草稿可删）。
     *
     * @param orderId 订单ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long orderId) {
        PurchaseOrder existed = requireOrder(orderId);
        if (!PurchaseBillStatusSupport.ORDER_DRAFT.equals(existed.getStatus())) {
            throw new ServiceException("仅草稿状态的采购订单可以删除");
        }
        orderLineMapper.delete(new LambdaQueryWrapper<PurchaseOrderLine>().eq(PurchaseOrderLine::getOrderId, orderId));
        return orderMapper.deleteById(orderId) > 0;
    }

    /**
     * 提交采购订单：金额达到阈值走审批，否则直接生效。
     *
     * @param orderId 订单ID
     * @return 提交后的订单状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String submit(Long orderId) {
        PurchaseOrder existed = requireOrder(orderId);
        BigDecimal threshold = resolveApprovalThreshold();
        BigDecimal amount = existed.getTotalAmount() == null ? BigDecimal.ZERO : existed.getTotalAmount();
        boolean needApproval = amount.compareTo(threshold) >= 0;
        String targetStatus = needApproval
                ? PurchaseBillStatusSupport.ORDER_PENDING_APPROVAL
                : PurchaseBillStatusSupport.ORDER_APPROVED;

        PurchaseBillStatusSupport.ensureOrderTransition(existed.getStatus(), targetStatus);
        if (!updateStatus(existed, targetStatus, existed.getRemark())) {
            throw new ServiceException("采购订单状态更新失败，请重试");
        }
        if (needApproval) {
            boolean accepted = workflowGateway.startWorkflow(
                    existed.getProcessKey(), BILL_TYPE, existed.getOrderId(), existed.getOrderNo());
            if (!accepted) {
                throw new ServiceException("采购订单审批流程发起失败");
            }
        }
        return targetStatus;
    }

    /**
     * 取消采购订单（已有收货记录时拒绝）。
     *
     * @param orderId 订单ID
     * @param reason  取消原因
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId, String reason) {
        PurchaseOrder existed = requireOrder(orderId);
        if (PurchaseBillStatusSupport.orderHasReceiving(existed.getStatus())) {
            throw new ServiceException("采购订单已产生收货记录，不允许取消");
        }
        PurchaseBillStatusSupport.ensureOrderTransition(existed.getStatus(), PurchaseBillStatusSupport.ORDER_CANCELLED);
        String remark = StringUtils.hasText(reason) ? trim(reason) : existed.getRemark();
        return updateStatus(existed, PurchaseBillStatusSupport.ORDER_CANCELLED, remark);
    }

    /**
     * 以版本号条件更新订单状态，防止并发下的丢失更新。
     *
     * @param order        订单
     * @param targetStatus 目标状态
     * @param remark       备注
     * @return true 表示更新成功
     */
    private boolean updateStatus(PurchaseOrder order, String targetStatus, String remark) {
        int currentVersion = order.getVersionNo() == null ? 1 : order.getVersionNo();
        int affected = orderMapper.update(null, new LambdaUpdateWrapper<PurchaseOrder>()
                .eq(PurchaseOrder::getOrderId, order.getOrderId())
                .eq(PurchaseOrder::getVersionNo, currentVersion)
                .set(PurchaseOrder::getStatus, targetStatus)
                .set(PurchaseOrder::getVersionNo, currentVersion + 1)
                .set(PurchaseOrder::getRemark, remark)
                .set(PurchaseOrder::getUpdateBy, resolveOperator())
                .set(PurchaseOrder::getUpdateTime, new Date()));
        return affected > 0;
    }

    /**
     * 读取审批阈值参数，取不到时使用兜底值。
     *
     * @return 阈值金额
     */
    private BigDecimal resolveApprovalThreshold() {
        try {
            String value = platformClient.getConfigValue(THRESHOLD_CONFIG_KEY);
            if (StringUtils.hasText(value)) {
                return new BigDecimal(value.trim());
            }
        } catch (Exception ex) {
            log.warn("读取采购审批阈值失败，回退默认值 {}", DEFAULT_THRESHOLD, ex);
        }
        return DEFAULT_THRESHOLD;
    }

    /**
     * 依据明细汇总订单数量与金额。
     *
     * @param order 订单
     */
    private void applyTotals(PurchaseOrder order) {
        BigDecimal totalQty = BigDecimal.ZERO;
        BigDecimal netAmount = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        for (PurchaseOrderLine line : safeLines(order)) {
            BigDecimal qty = line.getQty() == null ? BigDecimal.ZERO : line.getQty();
            BigDecimal price = line.getPrice() == null ? BigDecimal.ZERO : line.getPrice();
            BigDecimal rate = line.getTaxRate() == null ? BigDecimal.ZERO : line.getTaxRate();
            BigDecimal amount = qty.multiply(price);
            line.setAmount(amount);
            totalQty = totalQty.add(qty);
            netAmount = netAmount.add(amount);
            taxAmount = taxAmount.add(amount.multiply(rate));
        }
        order.setTotalQty(totalQty);
        order.setTaxAmount(taxAmount);
        order.setTotalAmount(netAmount.add(taxAmount));
    }

    /**
     * 保存订单明细行。
     *
     * @param order 订单
     */
    private void saveLines(PurchaseOrder order) {
        int lineNo = 1;
        for (PurchaseOrderLine line : safeLines(order)) {
            line.setLineId(null);
            line.setOrderId(order.getOrderId());
            line.setTenantId(order.getTenantId());
            line.setLineNo(lineNo++);
            line.setReceivedQty(BigDecimal.ZERO);
            line.setBilledQty(BigDecimal.ZERO);
            line.setLineStatus(LINE_STATUS_OPEN);
            line.setVersionNo(1);
            orderLineMapper.insert(line);
        }
    }

    /**
     * 空安全的明细列表。
     *
     * @param order 订单
     * @return 明细列表
     */
    private List<PurchaseOrderLine> safeLines(PurchaseOrder order) {
        return order == null || order.getLines() == null ? new ArrayList<>() : order.getLines();
    }

    /**
     * 加载订单，不存在则抛业务异常。
     *
     * @param orderId 订单ID
     * @return 订单
     */
    private PurchaseOrder requireOrder(Long orderId) {
        if (orderId == null) {
            throw new ServiceException("采购订单ID不能为空");
        }
        PurchaseOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new ServiceException("采购订单不存在");
        }
        return order;
    }

    private String buildBillNo() {
        return BILL_NO_PREFIX + System.currentTimeMillis();
    }

    private String currentTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new ServiceException("未获取到当前租户");
        }
        return tenantId.trim();
    }

    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }

    private String trim(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
