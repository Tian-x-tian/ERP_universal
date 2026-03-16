package com.erp.business.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 权限查询 Mapper。
 */
@Mapper
public interface SecurityPermissionMapper {

    /**
     * 查询用户拥有的权限标识。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return 权限标识集合
     */
    @Select("""
            SELECT DISTINCT m.perms
            FROM sys_user u
            INNER JOIN sys_user_role ur ON ur.user_id = u.user_id
            INNER JOIN sys_role_menu rm ON rm.role_id = ur.role_id
            INNER JOIN sys_menu m ON m.menu_id = rm.menu_id
            WHERE u.tenant_id = #{tenantId}
              AND u.user_name = #{userName}
              AND u.status = '0'
              AND u.del_flag = '0'
              AND m.perms IS NOT NULL
              AND m.perms <> ''
            """)
    List<String> selectPermissionsByUserName(@Param("tenantId") String tenantId, @Param("userName") String userName);

    /**
     * 查询用户拥有的角色编码。
     *
     * @param tenantId 租户编号
     * @param userName 用户账号
     * @return 角色编码集合
     */
    @Select("""
            SELECT DISTINCT r.role_key
            FROM sys_role r
            INNER JOIN sys_user_role ur ON ur.role_id = r.role_id
            INNER JOIN sys_user u ON u.user_id = ur.user_id
            WHERE u.tenant_id = #{tenantId}
              AND u.user_name = #{userName}
              AND u.status = '0'
              AND u.del_flag = '0'
            """)
    List<String> selectRoleKeysByUserName(@Param("tenantId") String tenantId, @Param("userName") String userName);
}
