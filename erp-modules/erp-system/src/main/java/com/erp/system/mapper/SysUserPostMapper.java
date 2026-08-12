package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysUserPost;
import org.apache.ibatis.annotations.Mapper;

/**
 * 用户岗位关联 Mapper 接口
 */
@Mapper
public interface SysUserPostMapper extends BaseMapper<SysUserPost> {
}
