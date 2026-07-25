package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.InventoryIntegrationEvent;

/**
 * 财务凭证集成门面接口。
 *
 * <p>库存单据过账后会产出财务凭证事件（本地消息表 + 重试补偿），由本门面投递给财务模块。
 * 财务模块尚未建设时 {@link #isEnabled()} 返回 false，事件归档为 SKIPPED 等待接入后重放，
 * 而不是被记为"投递成功"。
 */
public interface InventoryFinanceVoucherFacade {

    /**
     * 财务凭证对接是否已接入。
     *
     * @return true 表示可以投递凭证
     */
    boolean isEnabled();

    /**
     * 推送财务凭证事件。
     *
     * @param event 集成事件
     * @return true 表示成功
     */
    boolean pushVoucher(InventoryIntegrationEvent event);
}
