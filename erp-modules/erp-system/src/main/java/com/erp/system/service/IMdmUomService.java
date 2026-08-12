package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmUom;

import java.util.List;

/**
 * MDM 计量单位字典服务接口。
 */
public interface IMdmUomService extends IService<MdmUom> {

    /**
     * 查询计量单位列表。
     *
     * @param uomCode 单位编码（可选）
     * @param uomName 单位名称（可选）
     * @param status  状态（可选）
     * @return 计量单位列表
     */
    List<MdmUom> selectList(String uomCode, String uomName, String status);

    /**
     * 新增计量单位。
     *
     * @param uom 计量单位对象
     * @return true 表示新增成功
     */
    boolean create(MdmUom uom);

    /**
     * 修改计量单位。
     *
     * @param uom 计量单位对象
     * @return true 表示修改成功
     */
    boolean modify(MdmUom uom);

    /**
     * 停用计量单位。
     *
     * @param uomId 计量单位ID
     * @return true 表示停用成功
     */
    boolean disable(Long uomId);

    /**
     * 删除计量单位（逻辑删除）。
     *
     * @param uomId 计量单位ID
     * @return true 表示删除成功
     */
    boolean remove(Long uomId);
}
