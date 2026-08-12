package com.erp.business.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.purchase.domain.PurchaseOrder;

/**
 * 采购订单服务。
 */
public interface IPurchaseOrderService {

    /**
     * 分页查询采购订单。
     *
     * @param status   状态
     * @param orderNo  订单号
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    Page<PurchaseOrder> selectPage(String status, String orderNo, long pageNum, long pageSize);

    /**
     * 查询采购订单详情（含行）。
     *
     * @param orderId 订单ID
     * @return 订单详情
     */
    PurchaseOrder getDetail(Long orderId);

    /**
     * 新增采购订单。
     *
     * @param order 订单
     * @return 订单ID
     */
    Long create(PurchaseOrder order);

    /**
     * 修改采购订单（仅草稿可改）。
     *
     * @param order 订单
     * @return true 表示成功
     */
    boolean update(PurchaseOrder order);

    /**
     * 删除采购订单（仅草稿可删）。
     *
     * @param orderId 订单ID
     * @return true 表示成功
     */
    boolean remove(Long orderId);

    /**
     * 提交采购订单。
     *
     * <p>含税总金额达到 {@code purchase.order.approval.threshold} 时进入审批，
     * 否则直接生效。
     *
     * @param orderId 订单ID
     * @return 提交后的订单状态
     */
    String submit(Long orderId);

    /**
     * 取消采购订单（已有收货记录时拒绝）。
     *
     * @param orderId 订单ID
     * @param reason  取消原因
     * @return true 表示成功
     */
    boolean cancel(Long orderId, String reason);
}
