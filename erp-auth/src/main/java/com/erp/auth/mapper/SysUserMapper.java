package com.erp.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.auth.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户 Mapper。
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
