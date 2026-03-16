package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmSupplier;

import java.util.List;

/**
 * MDM 供应商主数据服务接口。
 */
public interface IMdmSupplierService extends IService<MdmSupplier> {

    /**
     * 查询供应商列表。
     *
     * @param supplierCode 供应商编码
     * @param supplierName 供应商名称
     * @param status       状态
     * @return 供应商列表
     */
    List<MdmSupplier> selectSupplierList(String supplierCode, String supplierName, String status);

    /**
     * 新增供应商。
     *
     * @param supplier 供应商对象
     * @return true 表示成功
     */
    boolean createSupplier(MdmSupplier supplier);

    /**
     * 修改供应商。
     *
     * @param supplier 供应商对象
     * @return true 表示成功
     */
    boolean updateSupplier(MdmSupplier supplier);

    /**
     * 停用供应商。
     *
     * @param supplierId 供应商ID
     * @return true 表示成功
     */
    boolean disableSupplier(Long supplierId, Integer versionNo);

    /**
     * 删除供应商（逻辑删除）。
     *
     * @param supplierId 供应商ID
     * @return true 表示成功
     */
    boolean removeSupplier(Long supplierId, Integer versionNo);
}
