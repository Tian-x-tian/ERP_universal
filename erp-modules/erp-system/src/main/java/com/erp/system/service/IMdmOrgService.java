package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.MdmOrg;

import java.util.List;

/**
 * MDM 组织主数据服务接口。
 */
public interface IMdmOrgService extends IService<MdmOrg> {

    /**
     * 查询组织列表。
     *
     * @param orgCode 组织编码
     * @param orgName 组织名称
     * @param status  状态
     * @return 组织列表
     */
    List<MdmOrg> selectOrgList(String orgCode, String orgName, String status);

    /**
     * 新增组织。
     *
     * @param org 组织对象
     * @return true 表示成功
     */
    boolean createOrg(MdmOrg org);

    /**
     * 修改组织。
     *
     * @param org 组织对象
     * @return true 表示成功
     */
    boolean updateOrg(MdmOrg org);

    /**
     * 停用组织。
     *
     * @param orgId 组织ID
     * @return true 表示成功
     */
    boolean disableOrg(Long orgId);

    /**
     * 删除组织（逻辑删除）。
     *
     * @param orgId 组织ID
     * @return true 表示成功
     */
    boolean removeOrg(Long orgId);
}
