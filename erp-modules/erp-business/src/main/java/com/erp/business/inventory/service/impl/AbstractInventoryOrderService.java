package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.AbstractInventoryOrder;
import com.erp.business.inventory.domain.AbstractInventoryOrderLine;
import com.erp.business.inventory.service.IInventoryOrderService;
import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.inventory.support.InventoryBillStatusSupport;
import com.erp.business.inventory.support.InventoryValueSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 库存单据通用服务抽象基类。
 *
 * @param <T> 单据头类型
 * @param <L> 单据行类型
 */
public abstract class AbstractInventoryOrderService<T extends AbstractInventoryOrder<L>, L extends AbstractInventoryOrderLine>
        implements IInventoryOrderService<T, L> {

    private final BaseMapper<T> orderMapper;
    private final BaseMapper<L> lineMapper;
    private final InventoryWorkflowGateway workflowGateway;
    private final SecurityUserResolver securityUserResolver;

    protected AbstractInventoryOrderService(BaseMapper<T> orderMapper,
            BaseMapper<L> lineMapper,
            InventoryWorkflowGateway workflowGateway,
            SecurityUserResolver securityUserResolver) {
        this.orderMapper = orderMapper;
        this.lineMapper = lineMapper;
        this.workflowGateway = workflowGateway;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询单据分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<T> selectPage(String billNo, String status, Long pageNum, Long pageSize) {
        Page<T> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<T> queryWrapper = new LambdaQueryWrapper<T>()
                .eq(getTenantColumn(), currentTenantId())
                .like(StringUtils.hasText(billNo), getBillNoColumn(), trim(billNo))
                .eq(StringUtils.hasText(status), getStatusColumn(),
                        InventoryBillStatusSupport.normalize(status, InventoryBillStatusSupport.DRAFT))
                .orderByDesc(getUpdateTimeColumn())
                .orderByDesc(getCreateTimeColumn());
        appendPageQuery(queryWrapper, billNo, status);
        return orderMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询单据详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    @Override
    public T getDetail(Long orderId) {
        T order = loadOrder(orderId);
        order.setLines(loadLines(orderId));
        return order;
    }

    /**
     * 新增单据。
     *
     * @param order 单据对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean create(T order) {
        validateOrderForSave(order);
        Date now = new Date();
        String tenantId = currentTenantId();
        String operator = resolveOperator();
        order.setTenantId(tenantId);
        order.setBillNo(StringUtils.hasText(order.getBillNo()) ? trim(order.getBillNo()) : buildBillNo(getBillNoPrefix()));
        order.setBillType(StringUtils.hasText(order.getBillType()) ? trim(order.getBillType()) : getDefaultBillType());
        order.setStatus(InventoryBillStatusSupport.DRAFT);
        order.setIdempotencyNo(StringUtils.hasText(order.getIdempotencyNo()) ? trim(order.getIdempotencyNo()) : order.getBillNo());
        order.setVersionNo(1);
        order.setRemark(InventoryValueSupport.trimToNull(order.getRemark()));
        order.setCreateBy(operator);
        order.setUpdateBy(operator);
        order.setCreateTime(now);
        order.setUpdateTime(now);
        prepareCreate(order, now, operator);
        if (orderMapper.insert(order) <= 0) {
            return false;
        }
        saveLines(order.getOrderId(), tenantId, order.getLines());
        return true;
    }

    /**
     * 修改单据。
     *
     * @param order 单据对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean update(T order) {
        if (order == null || order.getOrderId() == null) {
            throw new IllegalArgumentException("单据ID不能为空");
        }
        validateOrderForSave(order);
        T existed = loadOrder(order.getOrderId());
        if (!InventoryBillStatusSupport.isEditable(existed.getStatus())) {
            throw new IllegalStateException("仅草稿单据允许修改");
        }
        T updateEntity = newUpdateEntity();
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
        updateEntity.setVersionNo(nextVersion(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        prepareUpdate(updateEntity, order, existed);
        int updated = orderMapper.update(updateEntity, new LambdaUpdateWrapper<T>()
                .eq(getOrderIdColumn(), existed.getOrderId())
                .eq(getVersionNoColumn(), existed.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("单据已变化，请刷新后重试");
        }
        lineMapper.delete(new LambdaQueryWrapper<L>().eq(getLineOrderIdColumn(), existed.getOrderId()));
        saveLines(existed.getOrderId(), existed.getTenantId(), order.getLines());
        return true;
    }

    /**
     * 提交单据。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean submit(Long orderId) {
        T existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.isEditable(existed.getStatus())) {
            throw new IllegalStateException("仅草稿单据允许提交");
        }
        String targetStatus = InventoryBillStatusSupport.APPROVED;
        if (StringUtils.hasText(existed.getProcessKey())) {
            boolean accepted = workflowGateway.startWorkflow(existed.getProcessKey(), existed.getBillType(),
                    existed.getOrderId(), existed.getBillNo());
            if (!accepted) {
                throw new IllegalStateException("审批流程发起失败");
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
        T existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.PENDING_APPROVAL.equals(existed.getStatus())) {
            throw new IllegalStateException("仅待审批单据允许回写审批通过");
        }
        return updateStatus(existed, InventoryBillStatusSupport.APPROVED);
    }

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean reject(Long orderId) {
        T existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.PENDING_APPROVAL.equals(existed.getStatus())) {
            throw new IllegalStateException("仅待审批单据允许回写驳回");
        }
        return updateStatus(existed, InventoryBillStatusSupport.CANCELLED);
    }

    /**
     * 执行单据。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean execute(Long orderId) {
        T existed = loadOrder(orderId);
        if (!InventoryBillStatusSupport.isApproved(existed.getStatus())) {
            throw new IllegalStateException("仅已审批单据允许执行");
        }
        updateStatus(existed, InventoryBillStatusSupport.EXECUTING);
        T latest = loadOrder(orderId);
        List<L> lines = loadLines(orderId);
        applyExecution(latest, lines);
        boolean success = updateStatus(loadOrder(orderId), InventoryBillStatusSupport.COMPLETED);
        if (success) {
            afterCompleted(latest, lines);
        }
        return success;
    }

    /**
     * 取消单据。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean cancel(Long orderId) {
        T existed = loadOrder(orderId);
        if (InventoryBillStatusSupport.COMPLETED.equals(existed.getStatus())) {
            throw new IllegalStateException("已完成单据不允许取消");
        }
        return updateStatus(existed, InventoryBillStatusSupport.CANCELLED);
    }

    /**
     * 加载单据并校验租户。
     *
     * @param orderId 单据ID
     * @return 单据对象
     */
    protected T loadOrder(Long orderId) {
        if (orderId == null) {
            throw new IllegalArgumentException("单据ID不能为空");
        }
        T order = orderMapper.selectOne(new LambdaQueryWrapper<T>()
                .eq(getOrderIdColumn(), orderId)
                .eq(getTenantColumn(), currentTenantId()));
        if (order == null) {
            throw new ServiceException("单据不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return order;
    }

    /**
     * 查询单据行列表。
     *
     * @param orderId 单据ID
     * @return 单据行集合
     */
    protected List<L> loadLines(Long orderId) {
        return lineMapper.selectList(new LambdaQueryWrapper<L>()
                .eq(getLineOrderIdColumn(), orderId)
                .orderByAsc(getLineNoColumn()));
    }

    /**
     * 保存单据行。
     *
     * @param orderId 单据ID
     * @param tenantId 租户编号
     * @param lines 单据行
     */
    protected void saveLines(Long orderId, String tenantId, List<L> lines) {
        List<L> safeLines = lines == null ? new ArrayList<>() : lines;
        if (safeLines.isEmpty()) {
            throw new IllegalArgumentException("单据至少需要一条明细");
        }
        int lineNo = 1;
        for (L line : safeLines) {
            validateLine(line);
            line.setTenantId(tenantId);
            line.setOrderId(orderId);
            line.setLineNo(line.getLineNo() == null || line.getLineNo() < 1 ? lineNo : line.getLineNo());
            line.setBatchNo(InventoryValueSupport.trimToNull(line.getBatchNo()));
            line.setSerialNo(InventoryValueSupport.trimToNull(line.getSerialNo()));
            line.setRemark(InventoryValueSupport.trimToNull(line.getRemark()));
            prepareLineForSave(line);
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
    protected boolean updateStatus(T existed, String status) {
        T updateEntity = newUpdateEntity();
        updateEntity.setOrderId(existed.getOrderId());
        updateEntity.setStatus(status);
        updateEntity.setVersionNo(nextVersion(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        prepareStatusUpdate(updateEntity, existed, status);
        int updated = orderMapper.update(updateEntity, new LambdaUpdateWrapper<T>()
                .eq(getOrderIdColumn(), existed.getOrderId())
                .eq(getVersionNoColumn(), existed.getVersionNo()));
        if (updated <= 0) {
            throw new IllegalStateException("单据状态已变化，请刷新后重试");
        }
        return true;
    }

    /**
     * 校验单据保存参数。
     *
     * @param order 单据对象
     */
    protected void validateOrderForSave(T order) {
        if (order == null) {
            throw new IllegalArgumentException("单据不能为空");
        }
        if (order.getOrgId() == null || order.getWarehouseId() == null) {
            throw new IllegalArgumentException("组织ID和仓库ID不能为空");
        }
        if (order.getLines() == null || order.getLines().isEmpty()) {
            throw new IllegalArgumentException("单据至少需要一条明细");
        }
    }

    /**
     * 校验单据行。
     *
     * @param line 单据行
     */
    protected void validateLine(L line) {
        if (line == null || line.getItemId() == null) {
            throw new IllegalArgumentException("单据明细物料不能为空");
        }
        BigDecimal qty = line.getQty();
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("单据明细数量必须大于0");
        }
    }

    /**
     * 追加分页查询条件。
     *
     * @param queryWrapper 查询条件
     * @param billNo 单据编号
     * @param status 单据状态
     */
    protected void appendPageQuery(LambdaQueryWrapper<T> queryWrapper, String billNo, String status) {
        // 默认无需追加条件，子类可按业务扩展。
    }

    /**
     * 创建前的附加准备。
     *
     * @param order 单据对象
     * @param now 当前时间
     * @param operator 操作人
     */
    protected void prepareCreate(T order, Date now, String operator) {
        // 默认无需额外处理，子类按需覆盖。
    }

    /**
     * 修改前的附加准备。
     *
     * @param updateEntity 更新实体
     * @param input 输入参数
     * @param existed 原始单据
     */
    protected void prepareUpdate(T updateEntity, T input, T existed) {
        // 默认无需额外处理，子类按需覆盖。
    }

    /**
     * 单据行保存前的附加处理。
     *
     * @param line 单据行
     */
    protected void prepareLineForSave(L line) {
        // 默认无需额外处理，子类按需覆盖。
    }

    /**
     * 状态更新前的附加处理。
     *
     * @param updateEntity 更新实体
     * @param existed 原始单据
     * @param status 目标状态
     */
    protected void prepareStatusUpdate(T updateEntity, T existed, String status) {
        // 默认无需额外处理，子类按需覆盖。
    }

    /**
     * 执行完成后的附加处理。
     *
     * @param order 单据头
     * @param lines 单据行
     */
    protected void afterCompleted(T order, List<L> lines) {
        // 默认无需额外处理，子类按需覆盖。
    }

    /**
     * 创建新的更新实体。
     *
     * @return 空更新实体
     */
    protected abstract T newUpdateEntity();

    /**
     * 获取单号前缀。
     *
     * @return 单号前缀
     */
    protected abstract String getBillNoPrefix();

    /**
     * 获取默认单据类型。
     *
     * @return 默认单据类型
     */
    protected abstract String getDefaultBillType();

    /**
     * 执行库存业务逻辑。
     *
     * @param order 单据头
     * @param lines 单据行
     */
    protected abstract void applyExecution(T order, List<L> lines);

    /**
     * 获取单据ID列。
     *
     * @return 单据ID列引用
     */
    protected abstract SFunction<T, Long> getOrderIdColumn();

    /**
     * 获取租户列。
     *
     * @return 租户列引用
     */
    protected abstract SFunction<T, String> getTenantColumn();

    /**
     * 获取单号列。
     *
     * @return 单号列引用
     */
    protected abstract SFunction<T, String> getBillNoColumn();

    /**
     * 获取状态列。
     *
     * @return 状态列引用
     */
    protected abstract SFunction<T, String> getStatusColumn();

    /**
     * 获取版本列。
     *
     * @return 版本列引用
     */
    protected abstract SFunction<T, Integer> getVersionNoColumn();

    /**
     * 获取更新时间列。
     *
     * @return 更新时间列引用
     */
    protected abstract SFunction<T, Date> getUpdateTimeColumn();

    /**
     * 获取创建时间列。
     *
     * @return 创建时间列引用
     */
    protected abstract SFunction<T, Date> getCreateTimeColumn();

    /**
     * 获取单据行的单据ID列。
     *
     * @return 单据ID列引用
     */
    protected abstract SFunction<L, Long> getLineOrderIdColumn();

    /**
     * 获取单据行号列。
     *
     * @return 行号列引用
     */
    protected abstract SFunction<L, Integer> getLineNoColumn();

    /**
     * 生成业务单号。
     *
     * @param prefix 单号前缀
     * @return 单号
     */
    protected String buildBillNo(String prefix) {
        return prefix + System.currentTimeMillis();
    }

    /**
     * 解析当前租户编号。
     *
     * @return 当前租户编号
     */
    protected String currentTenantId() {
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
    protected String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }

    /**
     * 规范化字符串。
     *
     * @param value 原始字符串
     * @return 标准字符串
     */
    protected String trim(String value) {
        return value == null ? null : value.trim();
    }

    /**
     * 生成下一版本号。
     *
     * @param versionNo 当前版本号
     * @return 下一版本号
     */
    protected int nextVersion(Integer versionNo) {
        return (versionNo == null ? 1 : versionNo) + 1;
    }

    /**
     * 规范化页码。
     *
     * @param pageNum 原始页码
     * @return 标准页码
     */
    protected long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化页长。
     *
     * @param pageSize 原始页长
     * @return 标准页长
     */
    protected long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }
}
