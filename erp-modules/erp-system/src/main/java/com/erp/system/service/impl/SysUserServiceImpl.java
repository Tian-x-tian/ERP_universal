package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysUserMapper;
import com.erp.system.service.ISysUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.erp.system.domain.SysUserRole;
import com.erp.system.service.ISysUserRoleService;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现
 */
@Service
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements ISysUserService {

    private final PasswordEncoder passwordEncoder;
    private final ISysUserRoleService userRoleService;

    public SysUserServiceImpl(PasswordEncoder passwordEncoder, ISysUserRoleService userRoleService) {
        this.passwordEncoder = passwordEncoder;
        this.userRoleService = userRoleService;
    }

    @Override
    public SysUser selectUserByUserName(String userName) {
        return getOne(new LambdaQueryWrapper<SysUser>().eq(SysUser::getUserName, userName));
    }

    @Override
    @Transactional
    public boolean save(SysUser entity) {
        // 密码加密
        if (entity.getPassword() != null) {
            entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        }
        boolean success = super.save(entity);
        if (success && entity.getRoleIds() != null && !entity.getRoleIds().isEmpty()) {
            insertUserRole(entity);
        }
        return success;
    }

    @Override
    @Transactional
    public boolean updateById(SysUser entity) {
        // 先删除原有关联
        userRoleService.remove(new LambdaQueryWrapper<SysUserRole>().eq(SysUserRole::getUserId, entity.getUserId()));
        // 插入新关联
        if (entity.getRoleIds() != null && !entity.getRoleIds().isEmpty()) {
            insertUserRole(entity);
        }
        return super.updateById(entity);
    }

    /**
     * 新增用户角色信息
     */
    private void insertUserRole(SysUser user) {
        List<SysUserRole> list = user.getRoleIds().stream().map(roleId -> {
            SysUserRole ur = new SysUserRole();
            ur.setUserId(user.getUserId());
            ur.setRoleId(roleId);
            return ur;
        }).collect(Collectors.toList());
        userRoleService.saveBatch(list);
    }
}
