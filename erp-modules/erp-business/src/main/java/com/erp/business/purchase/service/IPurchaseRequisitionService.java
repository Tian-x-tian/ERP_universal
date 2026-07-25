package com.erp.business.purchase.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.purchase.domain.PurchaseRequisition;

/**
 * 采购申请服务。
 */
public interface IPurchaseRequisitionService {

    /**
     * 分页查询采购申请。
     *
     * @param status   状态
     * @param reqNo    申请单号
     * @param pageNum  页码
     * @param pageSize 每页条数
     * @return 分页结果
     */
    Page<PurchaseRequisition> selectPage(String status, String reqNo, long pageNum, long pageSize);

    /**
     * 查询采购申请详情（含行）。
     *
     * @param requisitionId 申请ID
     * @return 申请详情
     */
    PurchaseRequisition getDetail(Long requisitionId);

    /**
     * 新增采购申请。
     *
     * @param requisition 申请
     * @return 申请ID
     */
    Long create(PurchaseRequisition requisition);

    /**
     * 修改采购申请（仅草稿可改）。
     *
     * @param requisition 申请
     * @return true 表示成功
     */
    boolean update(PurchaseRequisition requisition);

    /**
     * 删除采购申请（仅草稿可删）。
     *
     * @param requisitionId 申请ID
     * @return true 表示成功
     */
    boolean remove(Long requisitionId);

    /**
     * 提交审批。
     *
     * @param requisitionId 申请ID
     * @return true 表示成功
     */
    boolean submit(Long requisitionId);

    /**
     * 将已审批通过的申请转为采购订单。
     *
     * @param requisitionId 申请ID
     * @param supplierId    供应商ID
     * @return 生成的采购订单ID
     */
    Long convertToOrder(Long requisitionId, Long supplierId);
}
