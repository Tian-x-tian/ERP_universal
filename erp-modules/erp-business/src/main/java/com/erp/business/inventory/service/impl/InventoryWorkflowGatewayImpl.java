package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.service.InventoryWorkflowGateway;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * 库存流程网关默认实现。
 */
@Component
public class InventoryWorkflowGatewayImpl implements InventoryWorkflowGateway {

    /**
     * M1 默认仅校验流程标识非空并保留扩展点。
     *
     * @param processKey 流程标识
     * @param billType 单据类型
     * @param billId 单据ID
     * @param billNo 单据编号
     * @return true 表示受理成功
     */
    @Override
    public boolean startWorkflow(String processKey, String billType, Long billId, String billNo) {
        return StringUtils.hasText(processKey) && billId != null;
    }
}
