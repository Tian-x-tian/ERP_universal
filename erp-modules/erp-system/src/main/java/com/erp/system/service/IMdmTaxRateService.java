package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmTaxRate;

import java.util.List;

/**
 * MDM 税率字典服务接口。
 */
public interface IMdmTaxRateService extends IService<MdmTaxRate> {

    /**
     * 查询税率列表。
     *
     * @param taxCode 税率编码（可选）
     * @param taxName 税率名称（可选）
     * @param status  状态（可选）
     * @return 税率列表
     */
    List<MdmTaxRate> selectList(String taxCode, String taxName, String status);

    /**
     * 新增税率。
     *
     * @param taxRate 税率对象
     * @return true 表示新增成功
     */
    boolean create(MdmTaxRate taxRate);

    /**
     * 修改税率。
     *
     * @param taxRate 税率对象
     * @return true 表示修改成功
     */
    boolean modify(MdmTaxRate taxRate);

    /**
     * 停用税率。
     *
     * @param taxRateId 税率ID
     * @return true 表示停用成功
     */
    boolean disable(Long taxRateId);

    /**
     * 删除税率（逻辑删除）。
     *
     * @param taxRateId 税率ID
     * @return true 表示删除成功
     */
    boolean remove(Long taxRateId);
}
