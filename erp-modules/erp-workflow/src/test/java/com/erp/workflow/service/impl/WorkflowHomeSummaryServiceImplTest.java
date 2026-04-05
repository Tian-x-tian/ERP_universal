package com.erp.workflow.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.workflow.contract.domain.SysTodoTask;
import com.erp.workflow.contract.domain.vo.WorkflowHomeTodoSummaryVO;
import com.erp.workflow.mapper.SysTodoTaskMapper;
import com.erp.workflow.security.service.PermissionService;
import com.erp.workflow.security.service.SecurityUserResolver;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 首页待办汇总服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class WorkflowHomeSummaryServiceImplTest {

    @Mock
    private SysTodoTaskMapper todoTaskMapper;

    @Mock
    private PermissionService permissionService;

    @Mock
    private SecurityUserResolver securityUserResolver;

    /**
     * 验证具备权限时会正确聚合待办数据。
     */
    @Test
    void shouldBuildTodoSummaryWhenPermissionGranted() {
        when(permissionService.hasPermi("workflow:todo:list")).thenReturn(true);
        when(securityUserResolver.getCurrentUserId()).thenReturn(7L);
        when(todoTaskMapper.selectList(any(LambdaQueryWrapper.class)))
                .thenReturn(Arrays.asList(
                        buildTodo("0", hoursOffset(-2)),
                        buildTodo("1", hoursOffset(8)),
                        buildTodo("2", null)));

        WorkflowHomeSummaryServiceImpl service = new WorkflowHomeSummaryServiceImpl(
                todoTaskMapper,
                permissionService,
                securityUserResolver);

        WorkflowHomeTodoSummaryVO summary = service.buildTodoSummary();

        Assertions.assertEquals(1L, summary.getPendingCount());
        Assertions.assertEquals(1L, summary.getProcessingCount());
        Assertions.assertEquals(1L, summary.getCompletedCount());
        Assertions.assertEquals(1L, summary.getOverdueCount());
        Assertions.assertEquals(1L, summary.getCollaborationDone());
        Assertions.assertEquals(3L, summary.getCollaborationTotal());
        Assertions.assertEquals(33.33D, summary.getCollaborationRate());
    }

    /**
     * 验证无权限时返回空安全值。
     */
    @Test
    void shouldReturnSafeSummaryWhenNoPermission() {
        when(permissionService.hasPermi("workflow:todo:list")).thenReturn(false);

        WorkflowHomeSummaryServiceImpl service = new WorkflowHomeSummaryServiceImpl(
                todoTaskMapper,
                permissionService,
                securityUserResolver);

        WorkflowHomeTodoSummaryVO summary = service.buildTodoSummary();

        Assertions.assertEquals(0L, summary.getPendingCount());
        Assertions.assertEquals(0L, summary.getProcessingCount());
        Assertions.assertEquals(0L, summary.getCompletedCount());
        Assertions.assertEquals(0L, summary.getOverdueCount());
        Assertions.assertEquals(0L, summary.getCollaborationDone());
        Assertions.assertEquals(0L, summary.getCollaborationTotal());
        Assertions.assertEquals(0D, summary.getCollaborationRate());
    }

    /**
     * 验证当前用户缺失时返回空安全值。
     */
    @Test
    void shouldReturnSafeSummaryWhenCurrentUserMissing() {
        when(permissionService.hasPermi("workflow:todo:list")).thenReturn(true);
        when(securityUserResolver.getCurrentUserId()).thenReturn(null);

        WorkflowHomeSummaryServiceImpl service = new WorkflowHomeSummaryServiceImpl(
                todoTaskMapper,
                permissionService,
                securityUserResolver);

        WorkflowHomeTodoSummaryVO summary = service.buildTodoSummary();

        Assertions.assertEquals(0L, summary.getPendingCount());
        Assertions.assertEquals(0L, summary.getProcessingCount());
        Assertions.assertEquals(0L, summary.getCompletedCount());
        Assertions.assertEquals(0L, summary.getOverdueCount());
        Assertions.assertEquals(0L, summary.getCollaborationDone());
        Assertions.assertEquals(0L, summary.getCollaborationTotal());
        Assertions.assertEquals(0D, summary.getCollaborationRate());
    }

    /**
     * 构造待办对象。
     *
     * @param status 任务状态
     * @param dueTime 截止时间
     * @return 待办对象
     */
    private SysTodoTask buildTodo(String status, Date dueTime) {
        SysTodoTask todoTask = new SysTodoTask();
        todoTask.setStatus(status);
        todoTask.setDueTime(dueTime);
        return todoTask;
    }

    /**
     * 基于当前时间构造偏移后的时间。
     *
     * @param hours 小时偏移
     * @return 时间
     */
    private Date hoursOffset(int hours) {
        return new Date(System.currentTimeMillis() + hours * 60L * 60L * 1000L);
    }
}
