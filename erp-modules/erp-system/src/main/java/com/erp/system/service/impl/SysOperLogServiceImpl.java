package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysOperLog;
import com.erp.system.mapper.SysOperLogMapper;
import com.erp.system.service.ISysOperLogService;
import org.springframework.stereotype.Service;

/**
 * 操作日志服务实现
 */
@Service
public class SysOperLogServiceImpl extends ServiceImpl<SysOperLogMapper, SysOperLog> implements ISysOperLogService {
}
