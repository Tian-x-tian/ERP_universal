package com.erp.business.hr.attendance.core.service.impl;

import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.hr.attendance.core.service.AttendanceWorkflowGateway;
import com.erp.business.hr.attendance.core.support.HrAttendanceSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 出勤流程网关实现。
 */
@Component
public class AttendanceWorkflowGatewayImpl implements AttendanceWorkflowGateway {
    private static final String ACTION_CODE_SUBMIT = "SUBMIT";

    private final InternalWorkflowClient internalWorkflowClient;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public AttendanceWorkflowGatewayImpl(InternalWorkflowClient internalWorkflowClient,
            SecurityUserResolver securityUserResolver) {
        this.internalWorkflowClient = internalWorkflowClient;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 发起请假审批流程。
     *
     * @param order 请假单
     * @return true 表示发起成功
     */
    @Override
    public boolean startLeaveWorkflow(HrAttendanceLeaveOrder order) {
        if (order == null || order.getOrderId() == null || !StringUtils.hasText(order.getOrderNo())) {
            return false;
        }
        WorkflowStartBody body = buildStartBody(HrAttendanceSupport.WORKFLOW_PROCESS_KEY_LEAVE,
                HrAttendanceSupport.BUSINESS_TYPE_LEAVE, order.getOrderId(), order.getOrderNo(), "请假单提交审批");
        return internalWorkflowClient.startProcess(body);
    }

    /**
     * 发起加班审批流程。
     *
     * @param order 加班单
     * @return true 表示发起成功
     */
    @Override
    public boolean startOvertimeWorkflow(HrAttendanceOvertimeOrder order) {
        if (order == null || order.getOrderId() == null || !StringUtils.hasText(order.getOrderNo())) {
            return false;
        }
        WorkflowStartBody body = buildStartBody(HrAttendanceSupport.WORKFLOW_PROCESS_KEY_OVERTIME,
                HrAttendanceSupport.BUSINESS_TYPE_OVERTIME, order.getOrderId(), order.getOrderNo(), "加班单提交审批");
        return internalWorkflowClient.startProcess(body);
    }

    /**
     * 构建工作流启动参数。
     *
     * @param processKey 流程标识
     * @param businessType 业务类型
     * @param orderId 单据ID
     * @param orderNo 单据编号
     * @param remark 流程备注
     * @return 工作流启动参数
     */
    private WorkflowStartBody buildStartBody(String processKey, String businessType, Long orderId,
            String orderNo, String remark) {
        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey(processKey);
        startBody.setRequestedProcessKey(processKey);
        startBody.setOwnerService(HrAttendanceSupport.OWNER_SERVICE);
        startBody.setBusinessType(businessType);
        startBody.setBusinessNo(orderNo);
        startBody.setDomainType(businessType);
        startBody.setActionCode(ACTION_CODE_SUBMIT);
        startBody.setIdempotencyKey(orderNo);
        startBody.setOperator(resolveOperator());
        startBody.setRemark(remark);
        startBody.setFormData(writeFormData(businessType, orderId, orderNo));
        return startBody;
    }

    /**
     * 序列化工作流表单数据。
     *
     * @param businessType 业务类型
     * @param orderId 单据ID
     * @param orderNo 单据编号
     * @return JSON 字符串
     */
    private String writeFormData(String businessType, Long orderId, String orderNo) {
        Map<String, Object> formData = new LinkedHashMap<>();
        formData.put("businessType", businessType);
        formData.put("orderId", orderId);
        formData.put("orderNo", orderNo);
        try {
            return objectMapper.writeValueAsString(formData);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("出勤工作流表单序列化失败", ex);
        }
    }

    /**
     * 解析当前操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }
}
