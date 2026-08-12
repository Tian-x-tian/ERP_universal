package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.InventoryInboundOrder;

/**
 * 入库服务接口。
 */
public interface IInventoryInboundService {

    Page<InventoryInboundOrder> selectPage(String billNo, String status, Long pageNum, Long pageSize);

    InventoryInboundOrder getDetail(Long orderId);

    boolean create(InventoryInboundOrder order);

    boolean update(InventoryInboundOrder order);

    boolean submit(Long orderId);

    boolean approve(Long orderId);

    boolean reject(Long orderId);

    boolean execute(Long orderId);

    boolean cancel(Long orderId);
}
