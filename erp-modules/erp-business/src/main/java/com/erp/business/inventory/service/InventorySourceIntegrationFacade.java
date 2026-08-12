package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryIntegrationEvent;

/**
 * 来源单集成门面接口。
 */
public interface InventorySourceIntegrationFacade {

    /**
     * 推送来源单回写事件。
     *
     * @param event 集成事件
     * @return true 表示成功
     */
    boolean pushProgress(InventoryIntegrationEvent event);
}
