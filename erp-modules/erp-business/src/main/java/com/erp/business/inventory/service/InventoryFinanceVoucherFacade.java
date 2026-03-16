package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryIntegrationEvent;

/**
 * 财务凭证集成门面接口。
 */
public interface InventoryFinanceVoucherFacade {

    /**
     * 推送财务凭证事件。
     *
     * @param event 集成事件
     * @return true 表示成功
     */
    boolean pushVoucher(InventoryIntegrationEvent event);
}
