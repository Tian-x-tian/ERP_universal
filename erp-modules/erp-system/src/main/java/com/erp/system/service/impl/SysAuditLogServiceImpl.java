package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysAuditLog;
import com.erp.system.mapper.SysAuditLogMapper;
import com.erp.system.service.ISysAuditLogService;
import org.springframework.stereotype.Service;

/**
 * 审计日志服务实现
 */
@Service
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog> implements ISysAuditLogService {
}
