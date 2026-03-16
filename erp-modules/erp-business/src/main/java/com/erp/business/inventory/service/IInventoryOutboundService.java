package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryOutboundOrder;

/**
 * 出库服务接口。
 */
public interface IInventoryOutboundService {

    Page<InventoryOutboundOrder> selectPage(String billNo, String status, Long pageNum, Long pageSize);

    InventoryOutboundOrder getDetail(Long orderId);

    boolean create(InventoryOutboundOrder order);

    boolean update(InventoryOutboundOrder order);

    boolean submit(Long orderId);

    boolean approve(Long orderId);

    boolean reject(Long orderId);

    boolean execute(Long orderId);

    boolean cancel(Long orderId);
}
