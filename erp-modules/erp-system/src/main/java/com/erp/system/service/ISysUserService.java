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

    /**
     * 更新当前用户个人资料（不处理角色、岗位关联）。
     *
     * @param profile 包含 userId 与可更新字段的用户对象
     * @return 更新是否成功
     */
    boolean updateProfileByUserId(SysUser profile);

    /**
     * 更新当前用户密码（不处理角色、岗位关联）。
     *
     * @param userId          用户ID
     * @param encodedPassword 已加密的新密码
     * @return 更新是否成功
     */
    boolean updatePasswordByUserId(Long userId, String encodedPassword);

    /**
     * 递增用户 Token 版本号，使既有令牌失效。
     *
     * @param userId 用户ID
     * @return 更新是否成功
     */
    boolean incrementTokenVersion(Long userId);
}
