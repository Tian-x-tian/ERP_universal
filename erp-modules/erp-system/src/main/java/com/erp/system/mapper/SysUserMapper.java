package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysUser;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户信息 Mapper 接口
 */
@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
