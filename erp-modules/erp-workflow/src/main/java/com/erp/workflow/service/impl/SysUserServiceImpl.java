package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.workflow.domain.platform.SysUser;
import com.erp.workflow.mapper.SysUserMapper;
import com.erp.workflow.service.ISysUserService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 工作流内部用户只读服务实现。
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {
    private static final String STATUS_ENABLED = "0";
    private static final String DEL_FLAG_EXIST = "0";

    /**
     * 按账号查询活动用户。
     *
     * @param userName 用户账号
     * @return 用户对象
     */
    @Override
    public SysUser selectUserByUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            return null;
        }
        return getOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUserName, userName.trim())
                .eq(SysUser::getStatus, STATUS_ENABLED)
                .eq(SysUser::getDelFlag, DEL_FLAG_EXIST)
                .last("LIMIT 1"));
    }
}
