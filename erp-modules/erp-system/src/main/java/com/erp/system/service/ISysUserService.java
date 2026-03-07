package com.erp.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.erp.system.domain.SysUser;

/**
 * 用户服务接口
 */
public interface ISysUserService extends IService<SysUser> {
    /**
     * 根据用户名查询用户
     */
    SysUser selectUserByUserName(String userName);
}
