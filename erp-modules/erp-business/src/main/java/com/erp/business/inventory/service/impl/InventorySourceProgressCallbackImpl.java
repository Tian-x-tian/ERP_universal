package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.service.InventorySourceProgressCallback;
import com.erp.business.inventory.service.IInventoryIntegrationEventService;
import org.springframework.stereotype.Component;

/**
 * 来源单进度回写默认实现。
 */
@Component
public class InventorySourceProgressCallbackImpl implements InventorySourceProgressCallback {

    private final IInventoryIntegrationEventService integrationEventService;

    public InventorySourceProgressCallbackImpl(IInventoryIntegrationEventService integrationEventService) {
        this.integrationEventService = integrationEventService;
    }

    /**
     * 默认实现将来源单回写请求转换为集成事件。
     *
     * @param sourceOrderType 来源单类型
     * @param sourceOrderId 来源单ID
     * @param sourceOrderNo 来源单编号
     * @param billNo 库存单号
     * @param status 库存状态
     */
    @Override
    public void callback(String sourceOrderType, Long sourceOrderId, String sourceOrderNo, String billNo,
            String status) {
        integrationEventService.recordSourceProgressEvent(sourceOrderType, sourceOrderId, sourceOrderNo,
                null, null, billNo, status);
    }
}
