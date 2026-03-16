package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import com.erp.business.inventory.service.InventoryFinanceVoucherFacade;
import org.springframework.stereotype.Component;

/**
 * 财务凭证集成门面默认实现。
 */
@Component
public class InventoryFinanceVoucherFacadeImpl implements InventoryFinanceVoucherFacade {

    /**
     * 推送财务凭证事件。
     *
     * @param event 集成事件
     * @return true 表示成功
     */
    @Override
    public boolean pushVoucher(InventoryIntegrationEvent event) {
        return event != null;
    }
}
