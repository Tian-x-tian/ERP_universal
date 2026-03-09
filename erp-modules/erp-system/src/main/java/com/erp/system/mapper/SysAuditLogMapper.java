package com.erp.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.erp.system.domain.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 审计日志 Mapper 接口
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}
