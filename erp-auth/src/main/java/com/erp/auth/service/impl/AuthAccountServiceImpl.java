package com.erp.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.erp.auth.domain.SysUser;
import com.erp.auth.mapper.SysUserMapper;
import com.erp.auth.service.AuthAccountService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 认证账号服务实现。
 */
@Service
public class AuthAccountServiceImpl implements AuthAccountService {
    private final SysUserMapper userMapper;

    public AuthAccountServiceImpl(SysUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public SysUser selectUserByUserNameAndTenant(String userName, String tenantId) {
        if (!StringUtils.hasText(userName) || !StringUtils.hasText(tenantId)) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName.trim())
                .eq(SysUser::getTenantId, tenantId.trim()));
    }

    @Override
    public SysUser selectUserByIdAndTenant(Long userId, String tenantId) {
        if (userId == null || !StringUtils.hasText(tenantId)) {
            return null;
        }
        return userMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .eq(SysUser::getTenantId, tenantId.trim()));
    }

    @Override
    public boolean updateLoginInfo(Long userId, String loginIp, Date loginDate) {
        if (userId == null) {
            return false;
        }
        return userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getLoginIp, loginIp)
                .set(SysUser::getLoginDate, loginDate)) > 0;
    }

    @Override
    public boolean incrementTokenVersion(Long userId) {
        if (userId == null) {
            return false;
        }
        SysUser user = userMapper.selectById(userId);
        if (user == null) {
            return false;
        }
        int currentVersion = user.getTokenVersion() == null ? 0 : user.getTokenVersion();
        return userMapper.update(null, new LambdaUpdateWrapper<SysUser>()
                .eq(SysUser::getUserId, userId)
                .set(SysUser::getTokenVersion, currentVersion + 1)) > 0;
    }
}
