package com.erp.business.inventory.service;

import com.erp.business.inventory.domain.vo.InventoryInboundHomeSummaryVO;

/**
 * 库存首页汇总服务接口。
 */
public interface IInventoryHomeSummaryService {

    /**
     * 构建入库首页汇总数据。
     *
     * @return 汇总数据
     */
    InventoryInboundHomeSummaryVO buildInboundSummary();
}
