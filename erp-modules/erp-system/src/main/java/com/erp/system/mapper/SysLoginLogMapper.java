package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysLoginLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 登录日志Mapper接口
 */
@Mapper
public interface SysLoginLogMapper extends BaseMapper<SysLoginLog> {
}
