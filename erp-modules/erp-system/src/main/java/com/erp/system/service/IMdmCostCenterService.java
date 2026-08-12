package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmCostCenter;

import java.util.List;

/**
 * MDM 成本中心主数据服务接口。
 */
public interface IMdmCostCenterService extends IService<MdmCostCenter> {

    /**
     * 查询成本中心列表。
     *
     * @param ccCode 成本中心编码
     * @param ccName 成本中心名称
     * @param status 状态
     * @return 成本中心列表
     */
    List<MdmCostCenter> selectCostCenterList(String ccCode, String ccName, String status);

    /**
     * 新增成本中心。
     *
     * @param costCenter 成本中心对象
     * @return true 表示成功
     */
    boolean createCostCenter(MdmCostCenter costCenter);

    /**
     * 修改成本中心。
     *
     * @param costCenter 成本中心对象
     * @return true 表示成功
     */
    boolean updateCostCenter(MdmCostCenter costCenter);

    /**
     * 停用成本中心。
     *
     * @param ccId 成本中心ID
     * @return true 表示成功
     */
    boolean disableCostCenter(Long ccId);

    /**
     * 删除成本中心（逻辑删除）。
     *
     * @param ccId 成本中心ID
     * @return true 表示成功
     */
    boolean removeCostCenter(Long ccId);
}
