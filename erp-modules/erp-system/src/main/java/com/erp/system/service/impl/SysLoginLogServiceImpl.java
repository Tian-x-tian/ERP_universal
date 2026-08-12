package com.erp.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.SysLoginLog;
import com.erp.system.mapper.SysLoginLogMapper;
import com.erp.system.service.ISysLoginLogService;
import org.springframework.stereotype.Service;

/**
 * 登录日志服务实现
 */
@Service
public class SysLoginLogServiceImpl extends ServiceImpl<SysLoginLogMapper, SysLoginLog> implements ISysLoginLogService {
}
