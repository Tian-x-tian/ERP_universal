package com.erp.business.purchase.service.impl;

import com.erp.business.purchase.service.PurchaseWorkflowGateway;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 采购流程网关默认实现。
 *
 * <p>与库存单据一致，通过 erp-workflow-client 发起流程，
 * 以单据编号作为幂等键，重复提交不会产生重复流程实例。
 */
@Component
public class PurchaseWorkflowGatewayImpl implements PurchaseWorkflowGateway {
    private static final String OWNER_SERVICE = "business";
    private static final String ACTION_CODE_SUBMIT = "SUBMIT";

    private final InternalWorkflowClient internalWorkflowClient;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public PurchaseWorkflowGatewayImpl(InternalWorkflowClient internalWorkflowClient,
            SecurityUserResolver securityUserResolver) {
        this.internalWorkflowClient = internalWorkflowClient;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发起采购审批流程。
     *
     * @param processKey 流程标识
     * @param billType   单据类型
     * @param billId     单据ID
     * @param billNo     单据编号
     * @return true 表示受理成功
     */
    @Override
    public boolean startWorkflow(String processKey, String billType, Long billId, String billNo) {
        if (!StringUtils.hasText(processKey) || !StringUtils.hasText(billType)
                || billId == null || !StringUtils.hasText(billNo)) {
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
        startBody.setRemark("采购单据提交审批");
        startBody.setFormData(writeFormData(billType, billId, billNo));
        return internalWorkflowClient.startProcess(startBody);
    }

    /**
     * 构建工作流表单数据。
     *
     * @param billType 单据类型
     * @param billId   单据ID
     * @param billNo   单据编号
     * @return JSON 文本
     */
    private String writeFormData(String billType, Long billId, String billNo) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("billType", billType == null ? null : billType.trim());
        formData.put("orderId", billId);
        formData.put("billNo", billNo == null ? null : billNo.trim());
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (Exception ignored) {
            return "{}";
        }
    }

    /**
     * 解析当前操作人。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String userName = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(userName) ? userName.trim() : "system";
    }
}
