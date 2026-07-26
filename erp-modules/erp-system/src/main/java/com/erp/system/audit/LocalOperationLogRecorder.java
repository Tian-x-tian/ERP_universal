package com.erp.system.audit;

import com.erp.common.logging.OperationLogPayload;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.system.domain.SysAuditLog;
import com.erp.system.domain.SysOperLog;
import com.erp.system.service.ISysAuditLogService;
import com.erp.system.service.ISysOperLogService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 系统模块日志落地实现。
 * erp-system 拥有 sys_oper_log / sys_audit_log，直接本地写库。
 */
@Component
public class LocalOperationLogRecorder implements OperationLogRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger(LocalOperationLogRecorder.class);

    private final ISysOperLogService operLogService;
    private final ISysAuditLogService auditLogService;

    public LocalOperationLogRecorder(ISysOperLogService operLogService, ISysAuditLogService auditLogService) {
        this.operLogService = operLogService;
        this.auditLogService = auditLogService;
    }

    /**
     * 按日志类型写入对应的日志表。
     *
     * @param payload 日志载荷
     */
    @Override
    public void record(OperationLogPayload payload) {
        if (payload == null) {
            return;
        }
        try {
            if (OperationLogPayload.TYPE_AUDIT.equals(payload.getLogType())) {
                auditLogService.save(toAuditLog(payload));
                return;
            }
            operLogService.save(toOperLog(payload));
        } catch (RuntimeException ex) {
            LOGGER.warn("写入{}日志失败，请求路径 {}", payload.getLogType(), payload.getRequestUri(), ex);
        }
    }

    /**
     * 转换为操作日志实体。
     *
     * @param payload 日志载荷
     * @return 操作日志实体
     */
    private SysOperLog toOperLog(OperationLogPayload payload) {
        SysOperLog log = new SysOperLog();
        log.setTenantId(payload.getTenantId());
        log.setOperator(payload.getOperator());
        log.setRequestMethod(payload.getRequestMethod());
        log.setRequestUri(payload.getRequestUri());
        log.setRequestIp(payload.getRequestIp());
        log.setRequestParams(payload.getRequestParams());
        log.setResponseCode(payload.getResponseCode());
        log.setSuccessFlag(payload.getSuccessFlag());
        log.setErrorMsg(payload.getErrorMsg());
        log.setCostTime(payload.getCostTime());
        log.setOperationTime(payload.getOperationTime());
        return log;
    }

    /**
     * 转换为审计日志实体。
     *
     * @param payload 日志载荷
     * @return 审计日志实体
     */
    private SysAuditLog toAuditLog(OperationLogPayload payload) {
        SysAuditLog log = new SysAuditLog();
        log.setTenantId(payload.getTenantId());
        log.setOperator(payload.getOperator());
        log.setOperationType(payload.getOperationType());
        log.setRequestMethod(payload.getRequestMethod());
        log.setRequestUri(payload.getRequestUri());
        log.setRequestIp(payload.getRequestIp());
        log.setRequestParams(payload.getRequestParams());
        log.setResponseCode(payload.getResponseCode());
        log.setSuccessFlag(payload.getSuccessFlag());
        log.setErrorMsg(payload.getErrorMsg());
        log.setCostTime(payload.getCostTime());
        log.setOperationTime(payload.getOperationTime());
        return log;
    }
}
