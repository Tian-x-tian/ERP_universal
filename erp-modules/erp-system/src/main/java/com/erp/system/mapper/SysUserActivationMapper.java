package com.erp.system.mapper;

import com.erp.system.domain.SysUserActivation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysUserActivationMapper {
    @Insert("INSERT INTO sys_user_activation (tenant_id, user_id, token_hash, expires_at, activated_at, "
            + "status, create_by, create_time, update_by, update_time, version_no) VALUES (#{tenantId}, "
            + "#{userId}, #{tokenHash}, #{expiresAt}, #{activatedAt}, #{status}, #{createBy}, "
            + "#{createTime}, #{updateBy}, #{updateTime}, #{versionNo})")
    int insert(SysUserActivation activation);

    @Select("SELECT activation_id, tenant_id, user_id, token_hash, expires_at, activated_at, status, "
            + "create_by, create_time, update_by, update_time, version_no FROM sys_user_activation "
            + "WHERE tenant_id = #{tenantId} AND token_hash = #{tokenHash} FOR UPDATE")
    SysUserActivation lockByTokenHash(@Param("tenantId") String tenantId,
            @Param("tokenHash") String tokenHash);

    @Select("SELECT activation_id, tenant_id, user_id, token_hash, expires_at, activated_at, status, "
            + "create_by, create_time, update_by, update_time, version_no FROM sys_user_activation "
            + "WHERE tenant_id = #{tenantId} AND user_id = #{userId} FOR UPDATE")
    SysUserActivation lockByUser(@Param("tenantId") String tenantId, @Param("userId") Long userId);

    @Update("UPDATE sys_user_activation SET token_hash = #{tokenHash}, expires_at = #{expiresAt}, "
            + "activated_at = NULL, status = 'PENDING', update_by = 'saas-provisioning', "
            + "update_time = #{now}, version_no = version_no + 1 WHERE tenant_id = #{tenantId} "
            + "AND activation_id = #{activationId} AND user_id = #{userId} AND status = 'PENDING' "
            + "AND version_no = #{versionNo}")
    int reissue(@Param("tenantId") String tenantId, @Param("activationId") Long activationId,
            @Param("userId") Long userId, @Param("versionNo") Long versionNo,
            @Param("tokenHash") String tokenHash, @Param("expiresAt") LocalDateTime expiresAt,
            @Param("now") LocalDateTime now);

    @Update("UPDATE sys_user_activation SET status = 'USED', activated_at = #{now}, "
            + "update_by = 'saas-activation', update_time = #{now}, version_no = version_no + 1 "
            + "WHERE tenant_id = #{tenantId} AND activation_id = #{activationId} "
            + "AND token_hash = #{tokenHash} AND status = 'PENDING' AND version_no = #{versionNo} "
            + "AND expires_at > #{now}")
    int markUsed(@Param("tenantId") String tenantId, @Param("activationId") Long activationId,
            @Param("tokenHash") String tokenHash, @Param("versionNo") Long versionNo,
            @Param("now") LocalDateTime now);
}
