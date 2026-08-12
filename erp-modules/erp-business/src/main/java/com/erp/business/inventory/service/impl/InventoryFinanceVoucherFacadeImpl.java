package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.domain.InventoryIntegrationEvent;
import com.erp.business.inventory.service.InventoryFinanceVoucherFacade;
import com.erp.common.core.exception.ServiceException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 财务凭证集成门面默认实现。
 *
 * <p><b>当前状态：财务模块尚未建设，本类是一个显式的扩展点占位。</b>
 * 默认 {@code erp.inventory.finance-voucher.enabled=false}，此时凭证事件会被归档为
 * SKIPPED，接入财务模块后可原样重放，不会丢单。
 *
 * <p>若开关置为 true 却没有提供真实实现，这里直接抛异常而不是静默返回成功——
 * 宁可失败得明显，也不要让集成事件表里留下一条从未发生过的"投递成功"。
 */
@Component
public class InventoryFinanceVoucherFacadeImpl implements InventoryFinanceVoucherFacade {

    private final boolean enabled;

    public InventoryFinanceVoucherFacadeImpl(
            @Value("${erp.inventory.finance-voucher.enabled:false}") boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * 财务凭证对接是否已接入。
     *
     * @return true 表示可以投递凭证
     */
    @Override
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * 推送财务凭证事件。
     *
     * @param event 集成事件
     * @return true 表示成功
     */
    @Override
    public boolean pushVoucher(InventoryIntegrationEvent event) {
        if (event == null) {
            return false;
        }
        throw new ServiceException("财务凭证对接已启用（erp.inventory.finance-voucher.enabled=true），"
                + "但尚未提供对接实现，请先接入财务模块或关闭该开关");
    }
}
