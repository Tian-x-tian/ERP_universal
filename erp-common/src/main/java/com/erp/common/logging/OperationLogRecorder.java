package com.erp.common.logging;

/**
 * 操作/审计日志落地口。
 * erp-system 直接写本地日志表；其余服务通过 erp-platform-client 回传给 erp-system，
 * 以免跨服务直接写别人的表。
 */
public interface OperationLogRecorder {

    /**
     * 记录一条操作或审计日志。
     * 实现方必须自行吞掉异常，日志失败不允许影响业务请求。
     *
     * @param payload 日志载荷
     */
    void record(OperationLogPayload payload);
}
