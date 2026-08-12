package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.system.domain.MdmCustomer;

import java.util.List;

/**
 * MDM 客户主数据服务接口。
 */
public interface IMdmCustomerService extends IService<MdmCustomer> {

    /**
     * 查询客户列表。
     *
     * @param customerCode 客户编码（可选）
     * @param customerName 客户名称（可选）
     * @param status       状态（可选）
     * @return 客户列表
     */
    List<MdmCustomer> selectCustomerList(String customerCode, String customerName, String status);

    /**
     * 查询客户分页列表。
     *
     * @param page         分页参数
     * @param customerCode 客户编码
     * @param customerName 客户名称
     * @param status       状态
     * @return 客户分页结果
     */
    Page<MdmCustomer> selectCustomerPage(Page<MdmCustomer> page, String customerCode, String customerName, String status);

    /**
     * 新增客户。
     *
     * @param customer 客户对象
     * @return true 表示新增成功
     */
    boolean createCustomer(MdmCustomer customer);

    /**
     * 修改客户。
     *
     * @param customer 客户对象
     * @return true 表示修改成功
     */
    boolean updateCustomer(MdmCustomer customer);

    /**
     * 停用客户。
     *
     * @param customerId 客户ID
     * @return true 表示停用成功
     */
    boolean disableCustomer(Long customerId, Integer versionNo);

    /**
     * 删除客户（逻辑删除）。
     *
     * @param customerId 客户ID
     * @return true 表示删除成功
     */
    boolean removeCustomer(Long customerId, Integer versionNo);
}
