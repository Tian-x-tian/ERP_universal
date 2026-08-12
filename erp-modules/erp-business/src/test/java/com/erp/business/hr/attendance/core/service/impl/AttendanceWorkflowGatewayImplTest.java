package com.erp.business.hr.attendance.core.service.impl;

import com.erp.business.hr.attendance.core.domain.HrAttendanceLeaveOrder;
import com.erp.business.hr.attendance.core.domain.HrAttendanceOvertimeOrder;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.workflow.contract.domain.vo.WorkflowStartBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 出勤流程网关单元测试。
 */
class AttendanceWorkflowGatewayImplTest {

    /**
     * 验证请假单提交流程时会调用内部工作流客户端。
     */
    @Test
    void shouldStartLeaveWorkflowViaInternalClient() {
        InternalWorkflowClient workflowClient = mock(InternalWorkflowClient.class);
        SecurityUserResolver securityUserResolver = mock(SecurityUserResolver.class);
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        when(workflowClient.startProcess(any(WorkflowStartBody.class))).thenReturn(true);
        AttendanceWorkflowGatewayImpl workflowGateway = new AttendanceWorkflowGatewayImpl(workflowClient, securityUserResolver);

        HrAttendanceLeaveOrder order = new HrAttendanceLeaveOrder();
        order.setOrderId(10L);
        order.setOrderNo("AL20260410001");
        boolean accepted = workflowGateway.startLeaveWorkflow(order);

        Assertions.assertTrue(accepted);
        ArgumentCaptor<WorkflowStartBody> captor = ArgumentCaptor.forClass(WorkflowStartBody.class);
        verify(workflowClient).startProcess(captor.capture());
        Assertions.assertEquals("HR_ATTENDANCE_LEAVE", captor.getValue().getBusinessType());
        Assertions.assertTrue(captor.getValue().getFormData().contains("\"orderId\":10"));
    }

    /**
     * 验证加班单提交流程时会调用内部工作流客户端。
     */
    @Test
    void shouldStartOvertimeWorkflowViaInternalClient() {
        InternalWorkflowClient workflowClient = mock(InternalWorkflowClient.class);
        SecurityUserResolver securityUserResolver = mock(SecurityUserResolver.class);
        when(securityUserResolver.getCurrentUsername()).thenReturn("tester");
        when(workflowClient.startProcess(any(WorkflowStartBody.class))).thenReturn(true);
        AttendanceWorkflowGatewayImpl workflowGateway = new AttendanceWorkflowGatewayImpl(workflowClient, securityUserResolver);

        HrAttendanceOvertimeOrder order = new HrAttendanceOvertimeOrder();
        order.setOrderId(20L);
        order.setOrderNo("AO20260410001");
        boolean accepted = workflowGateway.startOvertimeWorkflow(order);

        Assertions.assertTrue(accepted);
        ArgumentCaptor<WorkflowStartBody> captor = ArgumentCaptor.forClass(WorkflowStartBody.class);
        verify(workflowClient).startProcess(captor.capture());
        Assertions.assertEquals("HR_ATTENDANCE_OVERTIME", captor.getValue().getBusinessType());
        Assertions.assertTrue(captor.getValue().getFormData().contains("\"orderId\":20"));
    }
}
