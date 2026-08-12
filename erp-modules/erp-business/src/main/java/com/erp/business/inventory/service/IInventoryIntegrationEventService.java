package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryIntegrationEvent;

/**
 * 库存集成事件服务接口。
 */
public interface IInventoryIntegrationEventService {

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
    Page<InventoryIntegrationEvent> selectPage(String eventType, String eventStatus, String billNo,
            Long pageNum, Long pageSize);

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
    void recordSourceProgressEvent(String sourceOrderType, Long sourceOrderId, String sourceOrderNo,
            String billType, Long billId, String billNo, String status);

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
    void recordFinanceVoucherEvent(String sourceType, Long sourceId, String sourceNo, String billType, Long billId,
            String billNo, String payloadJson);

    /**
     * 重放集成事件。
     *
     * @param eventId 事件ID
     * @return true 表示成功
     */
    boolean replay(Long eventId);

    /**
     * 扫描并重试待处理事件。
     */
    void retryPendingEvents();
}
