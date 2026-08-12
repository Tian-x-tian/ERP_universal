package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.common.client.internal.InternalBusinessClient;
import com.erp.common.client.internal.InternalPlatformClient;
import com.erp.workflow.contract.domain.SysWorkflowInstance;
import com.erp.workflow.contract.domain.SysWorkflowTask;
import com.erp.workflow.contract.domain.SysWorkflowTaskAction;
import com.erp.workflow.contract.domain.vo.WorkflowSlaScanResultVO;
import com.erp.workflow.mapper.SysWorkflowInstanceMapper;
import com.erp.workflow.mapper.SysWorkflowTaskActionMapper;
import com.erp.workflow.mapper.SysWorkflowTaskMapper;
import com.erp.workflow.service.ISysNoticeService;
import com.erp.workflow.service.ISysTodoTaskService;
import com.erp.workflow.service.ISysWorkflowDefinitionService;
import com.erp.workflow.service.platform.IWorkflowPlatformReadService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 流程引擎核心行为单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysWorkflowEngineServiceImplTest {

    @Mock
    private SysWorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private SysWorkflowTaskMapper workflowTaskMapper;

    @Mock
    private SysWorkflowTaskActionMapper workflowTaskActionMapper;

    @Mock
    private ISysWorkflowDefinitionService workflowDefinitionService;

    @Mock
    private ISysTodoTaskService todoTaskService;

    @Mock
    private ISysNoticeService noticeService;

    @Mock
    private IWorkflowPlatformReadService platformReadService;

    @Mock
    private InternalPlatformClient internalPlatformClient;

    @Mock
    private InternalBusinessClient internalBusinessClient;

    private SysWorkflowEngineServiceImpl workflowEngineService;

    @BeforeEach
    void setUp() {
        workflowEngineService = new SysWorkflowEngineServiceImpl(
                workflowInstanceMapper,
                workflowTaskMapper,
                workflowTaskActionMapper,
                workflowDefinitionService,
                todoTaskService,
                noticeService,
                platformReadService,
                internalPlatformClient,
                internalBusinessClient);
    }

    /**
     * 验证重复签收已经处于处理中的本人任务时保持幂等。
     */
    @Test
    void shouldTreatAlreadyClaimedTaskAsIdempotent() {
        SysWorkflowTask task = buildTask(10L, 99L, 7L, "1");
        when(workflowTaskMapper.selectById(10L)).thenReturn(task);

        boolean claimed = workflowEngineService.claimTask(10L, 7L, "approver", "审批人");

        Assertions.assertTrue(claimed);
        verify(workflowTaskMapper, never()).updateById(any(SysWorkflowTask.class));
        verifyNoInteractions(workflowInstanceMapper, workflowTaskActionMapper, todoTaskService);
    }

    /**
     * 验证非任务受理人不能签收任务。
     */
    @Test
    void shouldRejectClaimFromDifferentAssignee() {
        SysWorkflowTask task = buildTask(10L, 99L, 8L, "0");
        when(workflowTaskMapper.selectById(10L)).thenReturn(task);

        boolean claimed = workflowEngineService.claimTask(10L, 7L, "approver", "审批人");

        Assertions.assertFalse(claimed);
        verify(workflowTaskMapper, never()).updateById(any(SysWorkflowTask.class));
        verifyNoInteractions(workflowInstanceMapper, workflowTaskActionMapper, todoTaskService);
    }

    /**
     * 验证 SLA 扫描遇到已删除的流程实例时安全跳过。
     */
    @Test
    void shouldSkipSlaTaskWhenInstanceMissing() {
        SysWorkflowTask task = buildTask(10L, 99L, 7L, "0");
        task.setDueTime(new Date(System.currentTimeMillis() - 60_000L));
        when(workflowTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(workflowInstanceMapper.selectById(99L)).thenReturn(null);

        WorkflowSlaScanResultVO result = workflowEngineService.scanTimeoutTasks();

        Assertions.assertEquals(1, result.getScannedCount());
        Assertions.assertEquals(1, result.getSkippedCount());
        Assertions.assertEquals(0, result.getWarningCount());
        Assertions.assertEquals(0, result.getOverdueCount());
        Assertions.assertNotNull(result.getScanTime());
        verifyNoInteractions(workflowTaskActionMapper, noticeService);
    }

    /**
     * 验证已记录过提醒动作的 SLA 任务不会重复通知。
     */
    @Test
    void shouldNotRepeatExistingSlaReminder() {
        SysWorkflowTask task = buildTask(10L, 99L, 7L, "0");
        task.setNodeKey("approve");
        task.setDueTime(new Date(System.currentTimeMillis() + 5 * 60_000L));

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(99L);
        instance.setModelContentSnapshot("{\"slaConfig\":{\"enabled\":true,\"reminderBeforeMinutes\":10,\"actions\":[\"REMIND\"]}}");

        when(workflowTaskMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(task));
        when(workflowInstanceMapper.selectById(99L)).thenReturn(instance);
        when(workflowTaskActionMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        WorkflowSlaScanResultVO result = workflowEngineService.scanTimeoutTasks();

        Assertions.assertEquals(1, result.getScannedCount());
        Assertions.assertEquals(0, result.getWarningCount());
        Assertions.assertEquals(0, result.getRemindedCount());
        Assertions.assertEquals(0, result.getSkippedCount());
        verify(workflowTaskActionMapper, never()).insert(any(SysWorkflowTaskAction.class));
        verifyNoInteractions(noticeService);
    }

    private SysWorkflowTask buildTask(Long taskId, Long instanceId, Long assigneeUserId, String status) {
        SysWorkflowTask task = new SysWorkflowTask();
        task.setTaskId(taskId);
        task.setInstanceId(instanceId);
        task.setAssigneeUserId(assigneeUserId);
        task.setStatus(status);
        return task;
    }
}
