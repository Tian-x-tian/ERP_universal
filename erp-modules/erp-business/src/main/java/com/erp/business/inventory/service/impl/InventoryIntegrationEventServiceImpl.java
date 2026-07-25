package com.erp.business.inventory.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import com.erp.business.inventory.mapper.InventoryIntegrationEventMapper;
import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import com.erp.business.inventory.service.InventoryFinanceVoucherFacade;
import com.erp.business.inventory.service.InventorySourceIntegrationFacade;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 库存集成事件服务实现。
 */
@Service
public class InventoryIntegrationEventServiceImpl implements IInventoryIntegrationEventService {

    /** 下游模块尚未接入：事件已归档，接入后可原样重放 */
    private static final String SKIPPED_STATUS = "SKIPPED";

    private final InventoryIntegrationEventMapper integrationEventMapper;
    private final InventorySourceIntegrationFacade sourceIntegrationFacade;
    private final InventoryFinanceVoucherFacade financeVoucherFacade;
    private final SecurityUserResolver securityUserResolver;

    public InventoryIntegrationEventServiceImpl(InventoryIntegrationEventMapper integrationEventMapper,
            InventorySourceIntegrationFacade sourceIntegrationFacade,
            InventoryFinanceVoucherFacade financeVoucherFacade,
            SecurityUserResolver securityUserResolver) {
        this.integrationEventMapper = integrationEventMapper;
        this.sourceIntegrationFacade = sourceIntegrationFacade;
        this.financeVoucherFacade = financeVoucherFacade;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询集成事件分页。
     *
     * @param eventType 事件类型
     * @param eventStatus 事件状态
     * @param billNo 单据编号
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<InventoryIntegrationEvent> selectPage(String eventType, String eventStatus, String billNo,
            Long pageNum, Long pageSize) {
        Page<InventoryIntegrationEvent> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<InventoryIntegrationEvent> queryWrapper = new LambdaQueryWrapper<InventoryIntegrationEvent>()
                .eq(InventoryIntegrationEvent::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(eventType), InventoryIntegrationEvent::getEventType,
                        eventType == null ? null : eventType.trim().toUpperCase())
                .eq(StringUtils.hasText(eventStatus), InventoryIntegrationEvent::getEventStatus,
                        eventStatus == null ? null : eventStatus.trim().toUpperCase())
                .like(StringUtils.hasText(billNo), InventoryIntegrationEvent::getBillNo, billNo == null ? null : billNo.trim())
                .orderByDesc(InventoryIntegrationEvent::getUpdateTime)
                .orderByDesc(InventoryIntegrationEvent::getCreateTime);
        return integrationEventMapper.selectPage(page, queryWrapper);
    }

    /**
     * 记录来源单回写事件。
     *
     * @param sourceOrderType 来源单类型
     * @param sourceOrderId 来源单ID
     * @param sourceOrderNo 来源单号
     * @param billType 库存单类型
     * @param billId 库存单ID
     * @param billNo 库存单号
     * @param status 状态
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordSourceProgressEvent(String sourceOrderType, Long sourceOrderId, String sourceOrderNo,
            String billType, Long billId, String billNo, String status) {
        if (!StringUtils.hasText(sourceOrderType) && sourceOrderId == null && !StringUtils.hasText(sourceOrderNo)) {
            return;
        }
        saveEvent("SOURCE_PROGRESS", sourceOrderType, sourceOrderId, sourceOrderNo, billType, billId, billNo,
                "{\"status\":\"" + safeText(status) + "\"}");
    }

    /**
     * 记录财务凭证事件。
     *
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param sourceNo 来源单号
     * @param billType 库存单类型
     * @param billId 库存单ID
     * @param billNo 库存单号
     * @param payloadJson 事件载荷
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recordFinanceVoucherEvent(String sourceType, Long sourceId, String sourceNo, String billType, Long billId,
            String billNo, String payloadJson) {
        saveEvent("FINANCE_VOUCHER", sourceType, sourceId, sourceNo, billType, billId, billNo, payloadJson);
    }

    /**
     * 重放集成事件。
     *
     * @param eventId 事件ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean replay(Long eventId) {
        InventoryIntegrationEvent event = loadEvent(eventId);
        boolean isFinanceEvent = "FINANCE_VOUCHER".equals(event.getEventType());

        // 财务模块尚未接入：事件归档为 SKIPPED 等待后续重放，不谎报成功、也不制造失败告警
        if (isFinanceEvent && !financeVoucherFacade.isEnabled()) {
            return updateEventStatus(event, SKIPPED_STATUS, "财务模块尚未接入，事件已归档待重放");
        }

        boolean success;
        String errorMessage = null;
        try {
            if (isFinanceEvent) {
                success = financeVoucherFacade.pushVoucher(event);
            } else {
                success = sourceIntegrationFacade.pushProgress(event);
            }
        } catch (Exception ex) {
            success = false;
            errorMessage = ex.getMessage();
        }
        InventoryIntegrationEvent updateEntity = new InventoryIntegrationEvent();
        updateEntity.setEventId(event.getEventId());
        updateEntity.setEventStatus(success ? "SUCCESS" : "FAILED");
        updateEntity.setRetryCount((event.getRetryCount() == null ? 0 : event.getRetryCount()) + 1);
        updateEntity.setLastError(success ? null : safeText(errorMessage));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return integrationEventMapper.updateById(updateEntity) > 0;
    }

    /**
     * 更新事件状态（不增加重试次数，用于归档类状态流转）。
     *
     * @param event   事件
     * @param status  目标状态
     * @param message 状态说明
     * @return true 表示更新成功
     */
    private boolean updateEventStatus(InventoryIntegrationEvent event, String status, String message) {
        InventoryIntegrationEvent updateEntity = new InventoryIntegrationEvent();
        updateEntity.setEventId(event.getEventId());
        updateEntity.setEventStatus(status);
        updateEntity.setLastError(safeText(message));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return integrationEventMapper.updateById(updateEntity) > 0;
    }

    /**
     * 扫描并重试待处理事件。
     */
    @Override
    public void retryPendingEvents() {
        List<InventoryIntegrationEvent> events = integrationEventMapper.selectList(new LambdaQueryWrapper<InventoryIntegrationEvent>()
                .eq(InventoryIntegrationEvent::getTenantId, currentTenantId())
                .in(InventoryIntegrationEvent::getEventStatus, "PENDING", "FAILED")
                .lt(InventoryIntegrationEvent::getRetryCount, 3)
                .orderByAsc(InventoryIntegrationEvent::getCreateTime)
                .last("limit 20"));
        for (InventoryIntegrationEvent event : events) {
            replay(event.getEventId());
        }
    }

    /**
     * 保存集成事件。
     *
     * @param eventType 事件类型
     * @param sourceType 来源类型
     * @param sourceId 来源ID
     * @param sourceNo 来源单号
     * @param billType 单据类型
     * @param billId 单据ID
     * @param billNo 单据编号
     * @param payloadJson 事件载荷
     */
    private void saveEvent(String eventType, String sourceType, Long sourceId, String sourceNo, String billType,
            Long billId, String billNo, String payloadJson) {
        InventoryIntegrationEvent event = new InventoryIntegrationEvent();
        Date now = new Date();
        event.setTenantId(currentTenantId());
        event.setEventType(eventType);
        event.setEventStatus("PENDING");
        event.setSourceType(safeText(sourceType));
        event.setSourceId(sourceId);
        event.setSourceNo(safeText(sourceNo));
        event.setBillType(safeText(billType));
        event.setBillId(billId);
        event.setBillNo(safeText(billNo));
        event.setPayloadJson(safeText(payloadJson));
        event.setRetryCount(0);
        event.setCreateBy(resolveOperator());
        event.setUpdateBy(resolveOperator());
        event.setCreateTime(now);
        event.setUpdateTime(now);
        integrationEventMapper.insert(event);
    }

    /**
     * 加载集成事件。
     *
     * @param eventId 事件ID
     * @return 事件对象
     */
    private InventoryIntegrationEvent loadEvent(Long eventId) {
        InventoryIntegrationEvent event = integrationEventMapper.selectOne(new LambdaQueryWrapper<InventoryIntegrationEvent>()
                .eq(InventoryIntegrationEvent::getEventId, eventId)
                .eq(InventoryIntegrationEvent::getTenantId, currentTenantId()));
        if (event == null) {
            throw new ServiceException("集成事件不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return event;
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
     * 安全规范化字符串。
     *
     * @param value 原始字符串
     * @return 标准字符串
     */
    private String safeText(String value) {
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
