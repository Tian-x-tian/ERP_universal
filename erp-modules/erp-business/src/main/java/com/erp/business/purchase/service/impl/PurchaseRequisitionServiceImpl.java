package com.erp.business.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.purchase.domain.PurchaseOrder;
import com.erp.business.purchase.domain.PurchaseOrderLine;
import com.erp.business.purchase.domain.PurchaseRequisition;
import com.erp.business.purchase.domain.PurchaseRequisitionLine;
import com.erp.business.purchase.mapper.PurchaseRequisitionLineMapper;
import com.erp.business.purchase.mapper.PurchaseRequisitionMapper;
import com.erp.business.purchase.service.IPurchaseOrderService;
import com.erp.business.purchase.service.IPurchaseRequisitionService;
import com.erp.business.purchase.service.PurchaseWorkflowGateway;
import com.erp.business.purchase.support.PurchaseBillStatusSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 采购申请服务实现。
 */
@Service
public class PurchaseRequisitionServiceImpl implements IPurchaseRequisitionService {

    /** 单据类型 */
    public static final String BILL_TYPE = "PURCHASE_REQUISITION";
    /** 审批流程标识 */
    private static final String PROCESS_KEY = "purchase_requisition_approval";
    private static final String BILL_NO_PREFIX = "PR";

    private final PurchaseRequisitionMapper requisitionMapper;
    private final PurchaseRequisitionLineMapper requisitionLineMapper;
    private final PurchaseWorkflowGateway workflowGateway;
    private final IPurchaseOrderService purchaseOrderService;
    private final SecurityUserResolver securityUserResolver;

    public PurchaseRequisitionServiceImpl(PurchaseRequisitionMapper requisitionMapper,
            PurchaseRequisitionLineMapper requisitionLineMapper,
            PurchaseWorkflowGateway workflowGateway,
            IPurchaseOrderService purchaseOrderService,
            SecurityUserResolver securityUserResolver) {
        this.requisitionMapper = requisitionMapper;
        this.requisitionLineMapper = requisitionLineMapper;
        this.workflowGateway = workflowGateway;
        this.purchaseOrderService = purchaseOrderService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 分页查询采购申请。
     *
     * @param status   状态
     * @param reqNo    申请单号
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    @Override
    public Page<PurchaseRequisition> selectPage(String status, String reqNo, long pageNum, long pageSize) {
        Page<PurchaseRequisition> page = new Page<>(Math.max(1, pageNum), Math.max(1, Math.min(200, pageSize)));
        return requisitionMapper.selectPage(page, new LambdaQueryWrapper<PurchaseRequisition>()
                .eq(StringUtils.hasText(status), PurchaseRequisition::getStatus, trim(status))
                .like(StringUtils.hasText(reqNo), PurchaseRequisition::getReqNo, trim(reqNo))
                .orderByDesc(PurchaseRequisition::getRequisitionId));
    }

    /**
     * 查询采购申请详情（含行）。
     *
     * @param requisitionId 申请ID
     * @return 申请详情
     */
    @Override
    public PurchaseRequisition getDetail(Long requisitionId) {
        PurchaseRequisition requisition = requireRequisition(requisitionId);
        requisition.setLines(loadLines(requisitionId));
        return requisition;
    }

    /**
     * 新增采购申请。
     *
     * @param requisition 申请
     * @return 申请ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(PurchaseRequisition requisition) {
        if (requisition == null || requisition.getLines() == null || requisition.getLines().isEmpty()) {
            throw new ServiceException("采购申请至少需要一行明细");
        }
        Date now = new Date();
        String operator = resolveOperator();
        requisition.setRequisitionId(null);
        requisition.setTenantId(currentTenantId());
        requisition.setReqNo(StringUtils.hasText(requisition.getReqNo())
                ? trim(requisition.getReqNo()) : BILL_NO_PREFIX + System.currentTimeMillis());
        requisition.setStatus(PurchaseBillStatusSupport.REQ_DRAFT);
        requisition.setProcessKey(PROCESS_KEY);
        requisition.setApplyDate(requisition.getApplyDate() == null ? now : requisition.getApplyDate());
        requisition.setVersionNo(1);
        requisition.setCreateBy(operator);
        requisition.setUpdateBy(operator);
        requisition.setCreateTime(now);
        requisition.setUpdateTime(now);
        requisition.setTotalAmount(sumEstimatedAmount(requisition.getLines()));
        if (requisitionMapper.insert(requisition) <= 0) {
            throw new ServiceException("采购申请保存失败");
        }
        saveLines(requisition);
        return requisition.getRequisitionId();
    }

    /**
     * 修改采购申请（仅草稿可改）。
     *
     * @param requisition 申请
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(PurchaseRequisition requisition) {
        PurchaseRequisition existed = requireRequisition(requisition == null ? null : requisition.getRequisitionId());
        if (!PurchaseBillStatusSupport.isEditable(existed.getStatus())) {
            throw new ServiceException("仅草稿状态的采购申请可以修改");
        }
        existed.setReqTitle(trim(requisition.getReqTitle()));
        existed.setDeptId(requisition.getDeptId());
        existed.setExpectDate(requisition.getExpectDate());
        existed.setRemark(trim(requisition.getRemark()));
        existed.setTotalAmount(sumEstimatedAmount(requisition.getLines()));
        existed.setUpdateBy(resolveOperator());
        existed.setUpdateTime(new Date());
        if (requisitionMapper.updateById(existed) <= 0) {
            return false;
        }
        requisitionLineMapper.delete(new LambdaQueryWrapper<PurchaseRequisitionLine>()
                .eq(PurchaseRequisitionLine::getRequisitionId, existed.getRequisitionId()));
        existed.setLines(requisition.getLines());
        saveLines(existed);
        return true;
    }

    /**
     * 删除采购申请（仅草稿可删）。
     *
     * @param requisitionId 申请ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean remove(Long requisitionId) {
        PurchaseRequisition existed = requireRequisition(requisitionId);
        if (!PurchaseBillStatusSupport.isEditable(existed.getStatus())) {
            throw new ServiceException("仅草稿状态的采购申请可以删除");
        }
        requisitionLineMapper.delete(new LambdaQueryWrapper<PurchaseRequisitionLine>()
                .eq(PurchaseRequisitionLine::getRequisitionId, requisitionId));
        return requisitionMapper.deleteById(requisitionId) > 0;
    }

    /**
     * 提交审批。
     *
     * @param requisitionId 申请ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long requisitionId) {
        PurchaseRequisition existed = requireRequisition(requisitionId);
        PurchaseBillStatusSupport.ensureRequisitionTransition(
                existed.getStatus(), PurchaseBillStatusSupport.REQ_SUBMITTED);
        if (!updateStatus(existed, PurchaseBillStatusSupport.REQ_SUBMITTED)) {
            throw new ServiceException("采购申请状态更新失败，请重试");
        }
        boolean accepted = workflowGateway.startWorkflow(
                existed.getProcessKey(), BILL_TYPE, existed.getRequisitionId(), existed.getReqNo());
        if (!accepted) {
            throw new ServiceException("采购申请审批流程发起失败");
        }
        return true;
    }

    /**
     * 将已审批通过的申请转为采购订单。
     *
     * @param requisitionId 申请ID
     * @param supplierId    供应商ID
     * @return 生成的采购订单ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long convertToOrder(Long requisitionId, Long supplierId) {
        PurchaseRequisition existed = requireRequisition(requisitionId);
        PurchaseBillStatusSupport.ensureRequisitionTransition(
                existed.getStatus(), PurchaseBillStatusSupport.REQ_CONVERTED);
        List<PurchaseRequisitionLine> lines = loadLines(requisitionId);
        if (lines.isEmpty()) {
            throw new ServiceException("采购申请没有明细，无法转订单");
        }

        PurchaseOrder order = new PurchaseOrder();
        order.setSupplierId(supplierId);
        order.setRequisitionId(existed.getRequisitionId());
        order.setRequisitionNo(existed.getReqNo());
        order.setOrderDate(new Date());
        order.setExpectDate(existed.getExpectDate());
        order.setRemark("由采购申请 " + existed.getReqNo() + " 转入");
        order.setLines(toOrderLines(lines));
        Long orderId = purchaseOrderService.create(order);

        if (!updateStatus(existed, PurchaseBillStatusSupport.REQ_CONVERTED)) {
            throw new ServiceException("采购申请状态更新失败，请重试");
        }
        return orderId;
    }

    /**
     * 申请行转订单行，单价沿用预估价，待人工确认。
     *
     * @param lines 申请行
     * @return 订单行
     */
    private List<PurchaseOrderLine> toOrderLines(List<PurchaseRequisitionLine> lines) {
        List<PurchaseOrderLine> orderLines = new ArrayList<>();
        for (PurchaseRequisitionLine source : lines) {
            PurchaseOrderLine target = new PurchaseOrderLine();
            target.setItemId(source.getItemId());
            target.setItemCode(source.getItemCode());
            target.setItemName(source.getItemName());
            target.setSpec(source.getSpec());
            target.setUom(source.getUom());
            target.setQty(source.getQty());
            target.setPrice(source.getEstPrice());
            target.setTaxRate(BigDecimal.ZERO);
            target.setRemark(source.getRemark());
            orderLines.add(target);
        }
        return orderLines;
    }

    /**
     * 以版本号条件更新申请状态。
     *
     * @param requisition  申请
     * @param targetStatus 目标状态
     * @return true 表示更新成功
     */
    private boolean updateStatus(PurchaseRequisition requisition, String targetStatus) {
        int currentVersion = requisition.getVersionNo() == null ? 1 : requisition.getVersionNo();
        int affected = requisitionMapper.update(null, new LambdaUpdateWrapper<PurchaseRequisition>()
                .eq(PurchaseRequisition::getRequisitionId, requisition.getRequisitionId())
                .eq(PurchaseRequisition::getVersionNo, currentVersion)
                .set(PurchaseRequisition::getStatus, targetStatus)
                .set(PurchaseRequisition::getVersionNo, currentVersion + 1)
                .set(PurchaseRequisition::getUpdateBy, resolveOperator())
                .set(PurchaseRequisition::getUpdateTime, new Date()));
        return affected > 0;
    }

    /**
     * 汇总预估金额。
     *
     * @param lines 申请行
     * @return 预估总金额
     */
    private BigDecimal sumEstimatedAmount(List<PurchaseRequisitionLine> lines) {
        BigDecimal total = BigDecimal.ZERO;
        if (lines == null) {
            return total;
        }
        for (PurchaseRequisitionLine line : lines) {
            BigDecimal qty = line.getQty() == null ? BigDecimal.ZERO : line.getQty();
            BigDecimal price = line.getEstPrice() == null ? BigDecimal.ZERO : line.getEstPrice();
            BigDecimal amount = qty.multiply(price);
            line.setEstAmount(amount);
            total = total.add(amount);
        }
        return total;
    }

    /**
     * 保存申请明细行。
     *
     * @param requisition 申请
     */
    private void saveLines(PurchaseRequisition requisition) {
        List<PurchaseRequisitionLine> lines = requisition.getLines() == null ? new ArrayList<>() : requisition.getLines();
        int lineNo = 1;
        for (PurchaseRequisitionLine line : lines) {
            line.setLineId(null);
            line.setRequisitionId(requisition.getRequisitionId());
            line.setTenantId(requisition.getTenantId());
            line.setLineNo(lineNo++);
            requisitionLineMapper.insert(line);
        }
    }

    /**
     * 加载申请明细行。
     *
     * @param requisitionId 申请ID
     * @return 明细行
     */
    private List<PurchaseRequisitionLine> loadLines(Long requisitionId) {
        return requisitionLineMapper.selectList(new LambdaQueryWrapper<PurchaseRequisitionLine>()
                .eq(PurchaseRequisitionLine::getRequisitionId, requisitionId)
                .orderByAsc(PurchaseRequisitionLine::getLineNo));
    }

    /**
     * 加载申请，不存在则抛业务异常。
     *
     * @param requisitionId 申请ID
     * @return 申请
     */
    private PurchaseRequisition requireRequisition(Long requisitionId) {
        if (requisitionId == null) {
            throw new ServiceException("采购申请ID不能为空");
        }
        PurchaseRequisition requisition = requisitionMapper.selectById(requisitionId);
        if (requisition == null) {
            throw new ServiceException("采购申请不存在");
        }
        return requisition;
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
