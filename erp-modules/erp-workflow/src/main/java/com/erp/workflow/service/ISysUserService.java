package com.erp.workflow.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.workflow.domain.platform.SysUser;

/**
 * 工作流内部用户只读服务接口。
 */
public interface ISysUserService extends IService<SysUser> {

    /**
     * 按账号查询用户。
     *
     * @param userName 用户账号
     * @return 用户对象
     */
    SysUser selectUserByUserName(String userName);
}
