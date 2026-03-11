package com.erp.system.service.impl;

import com.erp.system.domain.SysDept;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.vo.WorkflowDashboardQueryVO;
import com.erp.system.domain.vo.WorkflowDashboardVO;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.mapper.SysWorkflowTaskMapper;
import com.erp.system.service.ISysDeptService;
import com.erp.system.service.ISysUserService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * 流程分析服务单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysWorkflowAnalyticsServiceImplTest {

    @Mock
    private SysWorkflowInstanceMapper workflowInstanceMapper;

    @Mock
    private SysWorkflowTaskMapper workflowTaskMapper;

    @Mock
    private ISysUserService userService;

    @Mock
    private ISysDeptService deptService;

    private SysWorkflowAnalyticsServiceImpl workflowAnalyticsService;

    /**
     * 初始化测试对象。
     */
    @BeforeEach
    void setUp() {
        workflowAnalyticsService = new SysWorkflowAnalyticsServiceImpl(
                workflowInstanceMapper,
                workflowTaskMapper,
                userService,
                deptService);
    }

    /**
     * 验证看板会聚合实例、节点与部门指标。
     */
    @Test
    void shouldBuildWorkflowDashboard() {
        SysWorkflowInstance runningInstance = new SysWorkflowInstance();
        runningInstance.setInstanceId(1L);
        runningInstance.setProcessKey("expense_apply");
        runningInstance.setProcessName("报销审批");
        runningInstance.setInitiatorUserId(10L);
        runningInstance.setStatus("0");
        runningInstance.setStartTime(hoursAgo(6));

        SysWorkflowInstance rejectedInstance = new SysWorkflowInstance();
        rejectedInstance.setInstanceId(2L);
        rejectedInstance.setProcessKey("expense_apply");
        rejectedInstance.setProcessName("报销审批");
        rejectedInstance.setInitiatorUserId(10L);
        rejectedInstance.setStatus("2");
        rejectedInstance.setStartTime(hoursAgo(12));
        rejectedInstance.setFinishTime(hoursAgo(1));

        SysWorkflowTask overtimeTask = new SysWorkflowTask();
        overtimeTask.setTaskId(100L);
        overtimeTask.setInstanceId(1L);
        overtimeTask.setNodeKey("NODE_MANAGER");
        overtimeTask.setNodeName("部门经理审批");
        overtimeTask.setStatus("0");
        overtimeTask.setCreateTime(hoursAgo(5));
        overtimeTask.setDueTime(hoursAgo(1));

        SysWorkflowTask finishedTask = new SysWorkflowTask();
        finishedTask.setTaskId(101L);
        finishedTask.setInstanceId(2L);
        finishedTask.setNodeKey("NODE_FINANCE");
        finishedTask.setNodeName("财务审批");
        finishedTask.setStatus("2");
        finishedTask.setCreateTime(hoursAgo(10));
        finishedTask.setFinishTime(hoursAgo(2));
        finishedTask.setDueTime(hoursAgo(3));

        SysUser user = new SysUser();
        user.setUserId(10L);
        user.setDeptId(20L);

        SysDept dept = new SysDept();
        dept.setDeptId(20L);
        dept.setDeptName("咨询事业部");

        when(workflowInstanceMapper.selectList(any())).thenReturn(Arrays.asList(runningInstance, rejectedInstance));
        when(workflowTaskMapper.selectList(any())).thenReturn(Arrays.asList(overtimeTask, finishedTask));
        when(userService.listByIds(any())).thenReturn(Collections.singletonList(user));
        when(deptService.listByIds(any())).thenReturn(Collections.singletonList(dept));

        WorkflowDashboardVO dashboard = workflowAnalyticsService.buildDashboard(null);

        Assertions.assertNotNull(dashboard);
        Assertions.assertEquals(2, dashboard.getSummary().getTotalInstances());
        Assertions.assertEquals(1, dashboard.getSummary().getRunningInstances());
        Assertions.assertEquals(50.0D, dashboard.getSummary().getRejectRate());
        Assertions.assertEquals("部门经理审批", dashboard.getSummary().getBottleneckNodeName());
        Assertions.assertFalse(dashboard.getNodeMetrics().isEmpty());
        Assertions.assertFalse(dashboard.getDeptMetrics().isEmpty());
        Assertions.assertFalse(dashboard.getProcessMetrics().isEmpty());
        Assertions.assertEquals("咨询事业部", dashboard.getDeptMetrics().get(0).getDeptName());
    }

    /**
     * 验证看板支持按部门过滤实例与任务。
     */
    @Test
    void shouldBuildWorkflowDashboardByDeptFilter() {
        SysWorkflowInstance deptAInstance = new SysWorkflowInstance();
        deptAInstance.setInstanceId(11L);
        deptAInstance.setProcessKey("purchase_apply");
        deptAInstance.setProcessName("采购审批");
        deptAInstance.setInitiatorUserId(101L);
        deptAInstance.setStatus("1");
        deptAInstance.setStartTime(hoursAgo(8));
        deptAInstance.setFinishTime(hoursAgo(2));

        SysWorkflowInstance deptBInstance = new SysWorkflowInstance();
        deptBInstance.setInstanceId(12L);
        deptBInstance.setProcessKey("purchase_apply");
        deptBInstance.setProcessName("采购审批");
        deptBInstance.setInitiatorUserId(102L);
        deptBInstance.setStatus("0");
        deptBInstance.setStartTime(hoursAgo(4));

        SysWorkflowTask deptATask = new SysWorkflowTask();
        deptATask.setTaskId(201L);
        deptATask.setInstanceId(11L);
        deptATask.setNodeKey("NODE_BUYER");
        deptATask.setNodeName("采购经理审批");
        deptATask.setStatus("2");
        deptATask.setCreateTime(hoursAgo(7));
        deptATask.setFinishTime(hoursAgo(3));
        deptATask.setDueTime(hoursAgo(1));

        SysWorkflowTask deptBTask = new SysWorkflowTask();
        deptBTask.setTaskId(202L);
        deptBTask.setInstanceId(12L);
        deptBTask.setNodeKey("NODE_FINANCE");
        deptBTask.setNodeName("财务审批");
        deptBTask.setStatus("0");
        deptBTask.setCreateTime(hoursAgo(3));
        deptBTask.setDueTime(hoursAgo(1));

        SysUser deptAUser = new SysUser();
        deptAUser.setUserId(101L);
        deptAUser.setDeptId(20L);
        SysUser deptBUser = new SysUser();
        deptBUser.setUserId(102L);
        deptBUser.setDeptId(21L);

        SysDept deptA = new SysDept();
        deptA.setDeptId(20L);
        deptA.setDeptName("采购中心");
        SysDept deptB = new SysDept();
        deptB.setDeptId(21L);
        deptB.setDeptName("财务中心");

        when(workflowInstanceMapper.selectList(any())).thenReturn(Arrays.asList(deptAInstance, deptBInstance));
        when(workflowTaskMapper.selectList(any())).thenReturn(Arrays.asList(deptATask));
        when(userService.listByIds(any())).thenReturn(Arrays.asList(deptAUser, deptBUser));
        when(deptService.listByIds(any())).thenReturn(Arrays.asList(deptA, deptB));

        WorkflowDashboardQueryVO queryVO = new WorkflowDashboardQueryVO();
        queryVO.setDeptId(20L);
        WorkflowDashboardVO dashboard = workflowAnalyticsService.buildDashboard(queryVO);

        Assertions.assertNotNull(dashboard);
        Assertions.assertEquals(1, dashboard.getSummary().getTotalInstances());
        Assertions.assertEquals("采购中心", dashboard.getDeptMetrics().get(0).getDeptName());
        Assertions.assertEquals(1, dashboard.getProcessMetrics().get(0).getInstanceCount());
    }

    /**
     * 构造过去指定小时的时间点。
     *
     * @param hours 小时数
     * @return 时间对象
     */
    private Date hoursAgo(int hours) {
        return new Date(System.currentTimeMillis() - hours * 3600000L);
    }
}
