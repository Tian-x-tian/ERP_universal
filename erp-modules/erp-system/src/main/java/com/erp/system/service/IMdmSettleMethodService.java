package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmSettleMethod;

import java.util.List;

/**
 * MDM 结算方式字典服务接口。
 */
public interface IMdmSettleMethodService extends IService<MdmSettleMethod> {

    /**
     * 查询结算方式列表。
     *
     * @param settleCode 结算方式编码（可选）
     * @param settleName 结算方式名称（可选）
     * @param status     状态（可选）
     * @return 结算方式列表
     */
    List<MdmSettleMethod> selectList(String settleCode, String settleName, String status);

    /**
     * 新增结算方式。
     *
     * @param settleMethod 结算方式对象
     * @return true 表示新增成功
     */
    boolean create(MdmSettleMethod settleMethod);

    /**
     * 修改结算方式。
     *
     * @param settleMethod 结算方式对象
     * @return true 表示修改成功
     */
    boolean modify(MdmSettleMethod settleMethod);

    /**
     * 停用结算方式。
     *
     * @param settleMethodId 结算方式ID
     * @return true 表示停用成功
     */
    boolean disable(Long settleMethodId);

    /**
     * 删除结算方式（逻辑删除）。
     *
     * @param settleMethodId 结算方式ID
     * @return true 表示删除成功
     */
    boolean remove(Long settleMethodId);
}
