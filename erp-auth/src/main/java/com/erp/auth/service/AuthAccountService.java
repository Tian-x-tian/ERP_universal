package com.erp.auth.service;

import com.erp.auth.domain.SysUser;

import java.util.Date;

/**
 * 认证账号服务。
 */
public interface AuthAccountService {
    SysUser selectUserByUserNameAndTenant(String userName, String tenantId);

    SysUser selectUserByIdAndTenant(Long userId, String tenantId);

    boolean updateLoginInfo(Long userId, String loginIp, Date loginDate);

    boolean incrementTokenVersion(Long userId);
}
