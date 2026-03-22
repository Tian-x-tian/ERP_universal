package com.erp.business.inventory.service.impl;

import com.erp.business.inventory.service.InventoryWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.erp.workflow.contract.domain.vo.WorkflowTaskActionBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 库存流程网关默认实现。
 */
@Component
public class InventoryWorkflowGatewayImpl implements InventoryWorkflowGateway {
    private static final String OWNER_SERVICE = "business";
    private static final String ACTION_CODE_SUBMIT = "SUBMIT";

    private final InternalWorkflowClient internalWorkflowClient;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public InventoryWorkflowGatewayImpl(InternalWorkflowClient internalWorkflowClient,
            SecurityUserResolver securityUserResolver) {
        this.internalWorkflowClient = internalWorkflowClient;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发起库存审批流程。
     *
     * @param processKey 流程标识
     * @param billType 单据类型
     * @param billId 单据ID
     * @param billNo 单据编号
     * @return true 表示受理成功
     */
    @Override
    public boolean startWorkflow(String processKey, String billType, Long billId, String billNo) {
        if (!StringUtils.hasText(processKey) || !StringUtils.hasText(billType) || billId == null || !StringUtils.hasText(billNo)) {
            return false;
        }
        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey.trim());
        startBody.setRequestedProcessKey(processKey.trim());
        startBody.setOwnerService(OWNER_SERVICE);
        startBody.setBusinessType(billType.trim());
        startBody.setBusinessNo(billNo.trim());
        startBody.setDomainType(billType.trim());
        startBody.setActionCode(ACTION_CODE_SUBMIT);
        startBody.setIdempotencyKey(billNo.trim());
        startBody.setOperator(resolveOperator());
        startBody.setRemark("库存单据提交审批");
        startBody.setFormData(writeFormData(billType, billId, billNo));
        return internalWorkflowClient.startProcess(startBody);
    }

    /**
     * 中止库存审批流程。
     *
     * @param billType 单据类型
     * @param billNo 单据编号
     * @param reason 中止原因
     * @return true 表示中止成功
     */
    @Override
    public boolean abortWorkflow(String billType, String billNo, String reason) {
        if (!StringUtils.hasText(billType) || !StringUtils.hasText(billNo)) {
            return false;
        }
        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment(StringUtils.hasText(reason) ? reason.trim() : "库存单据状态更新失败，自动中止流程");
        return internalWorkflowClient.abortProcess(billType.trim(), billNo.trim(), actionBody);
    }

    /**
     * 构建库存工作流表单数据。
     *
     * @param billType 单据类型
     * @param billId 单据ID
     * @param billNo 单据编号
     * @return JSON
     */
    private String writeFormData(String billType, Long billId, String billNo) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("billType", billType == null ? null : billType.trim());
        formData.put("orderId", billId);
        formData.put("billNo", billNo == null ? null : billNo.trim());
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("序列化库存工作流表单失败", ex);
        }
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String operator = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(operator) ? operator.trim() : "system";
    }
}
