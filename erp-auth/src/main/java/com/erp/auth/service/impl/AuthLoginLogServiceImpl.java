package com.erp.auth.service.impl;

import com.erp.auth.domain.SysLoginLog;
import com.erp.auth.mapper.SysLoginLogMapper;
import com.erp.auth.service.AuthLoginLogService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

/**
 * 登录日志服务实现。
 */
@Service
public class AuthLoginLogServiceImpl implements AuthLoginLogService {
    private static final String DEFAULT_LOG_TENANT_ID = "UNKNOWN";

    private final SysLoginLogMapper loginLogMapper;

    public AuthLoginLogServiceImpl(SysLoginLogMapper loginLogMapper) {
        this.loginLogMapper = loginLogMapper;
    }

    @Override
    public boolean record(String tenantId, String userName, String status, String msg, String ipaddr) {
        SysLoginLog log = new SysLoginLog();
        log.setTenantId(normalizeTenantForLog(tenantId));
        log.setUserName(StringUtils.hasText(userName) ? userName.trim() : "anonymous");
        log.setStatus(StringUtils.hasText(status) ? status : "1");
        log.setMsg(StringUtils.hasText(msg) ? msg : "-");
        log.setIpaddr(StringUtils.hasText(ipaddr) ? ipaddr : "unknown");
        log.setLoginTime(new Date());
        return loginLogMapper.insert(log) > 0;
    }

    private String normalizeTenantForLog(String tenantId) {
        if (!StringUtils.hasText(tenantId)) {
            return DEFAULT_LOG_TENANT_ID;
        }
        return tenantId.trim();
    }
}
