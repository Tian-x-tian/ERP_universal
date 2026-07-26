package com.erp.business.audit;

import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.common.logging.OperationLogPayload;
import com.erp.common.logging.OperationLogRecorder;
import com.erp.platform.contract.model.PlatformOperationLogCreateRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

/**
 * 业务模块日志落地实现。
 * sys_oper_log / sys_audit_log 归 erp-system 所有，这里通过内部接口回传，不直接写表。
 * 回传走独立线程池异步执行，避免日志写入拖慢业务请求。
 */
@Component
public class RemoteOperationLogRecorder implements OperationLogRecorder {
    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteOperationLogRecorder.class);

    private final InternalPlatformClient internalPlatformClient;
    private final TaskExecutor operationLogTaskExecutor;

    public RemoteOperationLogRecorder(InternalPlatformClient internalPlatformClient,
            @Qualifier("operationLogTaskExecutor") TaskExecutor operationLogTaskExecutor) {
        this.internalPlatformClient = internalPlatformClient;
        this.operationLogTaskExecutor = operationLogTaskExecutor;
    }

    /**
     * 异步回传日志，任何失败都只记录告警，不影响业务请求。
     *
     * @param payload 日志载荷
     */
    @Override
    public void record(OperationLogPayload payload) {
        if (payload == null) {
            return;
        }
        PlatformOperationLogCreateRequest request = toRequest(payload);
        try {
            operationLogTaskExecutor.execute(() -> send(request));
        } catch (RuntimeException ex) {
            LOGGER.warn("提交操作日志回传任务失败，请求路径 {}", payload.getRequestUri(), ex);
        }
    }

    /**
     * 执行实际回传。
     *
     * @param request 日志写入请求
     */
    private void send(PlatformOperationLogCreateRequest request) {
        try {
            internalPlatformClient.recordOperationLog(request);
        } catch (RuntimeException ex) {
            LOGGER.warn("回传操作日志失败，请求路径 {}", request.getRequestUri(), ex);
        }
    }

    /**
     * 转换为跨服务请求体。
     *
     * @param payload 日志载荷
     * @return 日志写入请求
     */
    private PlatformOperationLogCreateRequest toRequest(OperationLogPayload payload) {
        PlatformOperationLogCreateRequest request = new PlatformOperationLogCreateRequest();
        request.setLogType(payload.getLogType());
        request.setTenantId(payload.getTenantId());
        request.setOperator(payload.getOperator());
        request.setOperationType(payload.getOperationType());
        request.setRequestMethod(payload.getRequestMethod());
        request.setRequestUri(payload.getRequestUri());
        request.setRequestIp(payload.getRequestIp());
        request.setRequestParams(payload.getRequestParams());
        request.setResponseCode(payload.getResponseCode());
        request.setSuccessFlag(payload.getSuccessFlag());
        request.setErrorMsg(payload.getErrorMsg());
        request.setCostTime(payload.getCostTime());
        request.setOperationTime(payload.getOperationTime());
        return request;
    }
}
