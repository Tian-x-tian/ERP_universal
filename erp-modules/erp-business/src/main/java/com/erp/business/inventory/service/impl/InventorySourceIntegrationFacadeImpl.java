package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import com.erp.business.inventory.service.InventorySourceIntegrationFacade;
import org.springframework.stereotype.Component;

/**
 * 来源单集成门面默认实现。
 */
@Component
public class InventorySourceIntegrationFacadeImpl implements InventorySourceIntegrationFacade {

    /**
     * 推送来源单回写事件。
     *
     * @param event 集成事件
     * @return true 表示成功
     */
    @Override
    public boolean pushProgress(InventoryIntegrationEvent event) {
        return event != null;
    }
}
