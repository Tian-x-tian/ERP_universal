package com.erp.business.inventory.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.inventory.domain.AbstractInventoryOrder;
import com.erp.business.inventory.domain.AbstractInventoryOrderLine;

/**
 * 库存单据通用服务接口。
 *
 * @param <T> 单据头类型
 * @param <L> 单据行类型
 */
public interface IInventoryOrderService<T extends AbstractInventoryOrder<L>, L extends AbstractInventoryOrderLine> {

    /**
     * 查询单据分页。
     *
     * @param billNo 单据编号
     * @param status 单据状态
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    Page<T> selectPage(String billNo, String status, Long pageNum, Long pageSize);

    /**
     * 查询单据详情。
     *
     * @param orderId 单据ID
     * @return 单据详情
     */
    T getDetail(Long orderId);

    /**
     * 新增单据。
     *
     * @param order 单据对象
     * @return true 表示成功
     */
    boolean create(T order);

    /**
     * 修改单据。
     *
     * @param order 单据对象
     * @return true 表示成功
     */
    boolean update(T order);

    /**
     * 提交单据。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    boolean submit(Long orderId);

    /**
     * 回写审批通过。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    boolean approve(Long orderId);

    /**
     * 回写审批驳回。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    boolean reject(Long orderId);

    /**
     * 执行单据。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    boolean execute(Long orderId);

    /**
     * 取消单据。
     *
     * @param orderId 单据ID
     * @return true 表示成功
     */
    boolean cancel(Long orderId);
}
