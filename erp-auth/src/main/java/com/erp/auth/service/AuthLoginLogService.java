package com.erp.auth.service;

/**
 * 登录日志服务。
 */
public interface AuthLoginLogService {
    boolean record(String tenantId, String userName, String status, String msg, String ipaddr);
}
