package com.erp.system.service.impl;

import com.erp.system.domain.SysTodoTask;
import com.erp.system.domain.SysUser;
import com.erp.system.domain.SysWorkflowDefinition;
import com.erp.system.domain.SysWorkflowInstance;
import com.erp.system.domain.SysWorkflowTask;
import com.erp.system.domain.SysWorkflowTaskAction;
import com.erp.system.domain.vo.WorkflowInstanceDetailVO;
import com.erp.system.domain.vo.WorkflowStartBody;
import com.erp.system.domain.vo.WorkflowTaskActionBody;
import com.erp.system.domain.vo.WorkflowTaskTransferBody;
import com.erp.system.mapper.SysWorkflowInstanceMapper;
import com.erp.system.mapper.SysWorkflowTaskActionMapper;
import com.erp.system.mapper.SysWorkflowTaskMapper;
import com.erp.system.service.ISysNoticeService;
import com.erp.system.service.ISysTodoTaskService;
import com.erp.system.service.ISysUserService;
import com.erp.system.service.ISysWorkflowDefinitionService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 流程引擎服务单元测试。
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
    private ISysUserService userService;

    private SysWorkflowEngineServiceImpl workflowEngineService;

    /**
     * 初始化被测服务。
     */
    @BeforeEach
    void setUp() {
        workflowEngineService = new SysWorkflowEngineServiceImpl(
                workflowInstanceMapper,
                workflowTaskMapper,
                workflowTaskActionMapper,
                workflowDefinitionService,
                todoTaskService,
                noticeService,
                userService);
    }

    /**
     * 验证流程实例列表查询。
     */
    @Test
    void shouldSelectInstanceList() {
        when(workflowInstanceMapper.selectList(any())).thenReturn(Collections.singletonList(new SysWorkflowInstance()));

        List<SysWorkflowInstance> instanceList = workflowEngineService.selectInstanceList("purchase", "0", "PO");

        Assertions.assertEquals(1, instanceList.size());
    }

    /**
     * 验证流程实例详情可组装任务与动作时间线。
     */
    @Test
    void shouldSelectInstanceDetail() {
        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(1L);
        when(workflowInstanceMapper.selectById(1L)).thenReturn(instance);
        when(workflowTaskMapper.selectList(any())).thenReturn(Collections.singletonList(new SysWorkflowTask()));
        when(workflowTaskActionMapper.selectList(any())).thenReturn(Collections.singletonList(new SysWorkflowTaskAction()));

        WorkflowInstanceDetailVO detail = workflowEngineService.selectInstanceDetail(1L);

        Assertions.assertNotNull(detail);
        Assertions.assertEquals(1, detail.getTaskList().size());
        Assertions.assertEquals(1, detail.getActionList().size());
    }

    /**
     * 验证流程发起成功时会写入实例、任务、待办与动作日志。
     */
    @Test
    void shouldStartProcessSuccessfully() {
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setDefinitionId(1L);
        definition.setProcessKey("purchase_apply");
        definition.setProcessName("采购审批流程");
        definition.setCategory("purchase");
        definition.setVersion(2);
        definition.setFormSchema("{\"fields\":[]}");
        definition.setModelContent("{\"nodes\":[]}");
        when(workflowDefinitionService.selectLatestPublishedByProcessKey("purchase_apply")).thenReturn(definition);
        when(workflowInstanceMapper.insert(any(SysWorkflowInstance.class))).thenAnswer(invocation -> {
            SysWorkflowInstance instance = invocation.getArgument(0);
            instance.setInstanceId(100L);
            return 1;
        });
        when(todoTaskService.save(any(SysTodoTask.class))).thenAnswer(invocation -> {
            SysTodoTask todoTask = invocation.getArgument(0);
            todoTask.setTodoId(200L);
            return true;
        });
        when(workflowTaskMapper.insert(any(SysWorkflowTask.class))).thenAnswer(invocation -> {
            SysWorkflowTask task = invocation.getArgument(0);
            task.setTaskId(300L);
            return 1;
        });
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(noticeService.createNotice(any())).thenReturn(true);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey("purchase_apply");
        startBody.setBusinessNo("PO-20260310-001");
        startBody.setBusinessType("采购申请");
        startBody.setNodeName("部门负责人审批");
        startBody.setFormData("{\"amount\":1000}");
        startBody.setPriority("H");
        startBody.setDueTime(new Date());

        boolean success = workflowEngineService.startProcess(startBody, 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
        ArgumentCaptor<SysWorkflowInstance> instanceCaptor = ArgumentCaptor.forClass(SysWorkflowInstance.class);
        verify(workflowInstanceMapper).insert(instanceCaptor.capture());
        Assertions.assertEquals(2, instanceCaptor.getValue().getDefinitionVersion());
        Assertions.assertEquals("{\"fields\":[]}", instanceCaptor.getValue().getFormSchemaSnapshot());
        Assertions.assertEquals("{\"nodes\":[]}", instanceCaptor.getValue().getModelContentSnapshot());
    }

    /**
     * 验证流程发起时若动作日志写入失败会返回失败，保证链路一致性。
     */
    @Test
    void shouldFailStartProcessWhenActionInsertFailed() {
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setDefinitionId(1L);
        definition.setProcessKey("expense_apply");
        definition.setProcessName("报销审批流程");
        definition.setCategory("expense");
        definition.setVersion(1);
        when(workflowDefinitionService.selectLatestPublishedByProcessKey("expense_apply")).thenReturn(definition);
        when(workflowInstanceMapper.insert(any(SysWorkflowInstance.class))).thenAnswer(invocation -> {
            SysWorkflowInstance instance = invocation.getArgument(0);
            instance.setInstanceId(101L);
            return 1;
        });
        when(todoTaskService.save(any(SysTodoTask.class))).thenAnswer(invocation -> {
            SysTodoTask todoTask = invocation.getArgument(0);
            todoTask.setTodoId(201L);
            return true;
        });
        when(workflowTaskMapper.insert(any(SysWorkflowTask.class))).thenAnswer(invocation -> {
            SysWorkflowTask task = invocation.getArgument(0);
            task.setTaskId(301L);
            return 1;
        });
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(0);

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey("expense_apply");
        startBody.setBusinessNo("EX-20260310-001");

        boolean success = workflowEngineService.startProcess(startBody, 1L, "admin", "系统管理员");

        Assertions.assertFalse(success);
    }

    /**
     * 验证当前用户任务列表查询。
     */
    @Test
    void shouldSelectMyTaskList() {
        when(workflowTaskMapper.selectList(any())).thenReturn(Collections.singletonList(new SysWorkflowTask()));

        List<SysWorkflowTask> taskList = workflowEngineService.selectMyTaskList(1L, "0");

        Assertions.assertEquals(1, taskList.size());
    }

    /**
     * 验证审批通过会同步更新任务、待办、实例与动作日志。
     */
    @Test
    void shouldApproveTaskSuccessfully() {
        SysWorkflowTask task = new SysWorkflowTask();
        task.setTaskId(1L);
        task.setInstanceId(10L);
        task.setTodoId(20L);
        task.setAssigneeUserId(1L);
        task.setStatus("0");
        task.setNodeKey("NODE_1");
        task.setNodeName("部门负责人审批");
        when(workflowTaskMapper.selectById(1L)).thenReturn(task);
        when(workflowTaskMapper.updateById(any(SysWorkflowTask.class))).thenReturn(1);
        when(todoTaskService.updateById(any(SysTodoTask.class))).thenReturn(true);
        when(workflowInstanceMapper.updateById(any(SysWorkflowInstance.class))).thenReturn(1);
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(noticeService.createNotice(any())).thenReturn(true);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(10L);
        instance.setDefinitionId(1L);
        instance.setTenantId("000000");
        instance.setProcessName("采购审批流程");
        instance.setBusinessNo("PO-20260310-001");
        instance.setInitiatorUserId(2L);
        when(workflowInstanceMapper.selectById(10L)).thenReturn(instance);

        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment("同意");

        boolean success = workflowEngineService.approveTask(1L, actionBody, 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
    }

    /**
     * 验证开始节点连向多个审批节点时，会在发起时一次性生成多个审批任务。
     */
    @Test
    void shouldStartProcessWithMultipleApprovalBranches() {
        SysWorkflowDefinition definition = new SysWorkflowDefinition();
        definition.setDefinitionId(2L);
        definition.setProcessKey("parallel_apply");
        definition.setProcessName("并行审批流程");
        definition.setCategory("custom");
        definition.setVersion(1);
        definition.setFormSchema("{\"fields\":[]}");
        definition.setModelContent("{\"startNodeKey\":\"START_1\",\"nodes\":["
                + "{\"nodeKey\":\"START_1\",\"nodeName\":\"开始\",\"nodeType\":\"start\"},"
                + "{\"nodeKey\":\"NODE_A\",\"nodeName\":\"部门审批\",\"nodeType\":\"approval\",\"candidateUserIds\":[11]},"
                + "{\"nodeKey\":\"NODE_B\",\"nodeName\":\"财务审批\",\"nodeType\":\"approval\",\"candidateUserIds\":[12]}"
                + "],\"edges\":["
                + "{\"from\":\"START_1\",\"to\":\"NODE_A\"},"
                + "{\"from\":\"START_1\",\"to\":\"NODE_B\"}"
                + "]}");
        when(workflowDefinitionService.selectLatestPublishedByProcessKey("parallel_apply")).thenReturn(definition);
        when(workflowInstanceMapper.insert(any(SysWorkflowInstance.class))).thenAnswer(invocation -> {
            SysWorkflowInstance instance = invocation.getArgument(0);
            instance.setInstanceId(102L);
            return 1;
        });
        when(todoTaskService.save(any(SysTodoTask.class))).thenAnswer(invocation -> {
            SysTodoTask todoTask = invocation.getArgument(0);
            todoTask.setTodoId(new Date().getTime());
            return true;
        });
        when(workflowTaskMapper.insert(any(SysWorkflowTask.class))).thenAnswer(invocation -> {
            SysWorkflowTask workflowTask = invocation.getArgument(0);
            workflowTask.setTaskId(new Date().getTime());
            return 1;
        });
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(noticeService.createNotice(any())).thenReturn(true);
        when(userService.getById(11L)).thenReturn(buildUser(11L, "deptLeader", "部门负责人"));
        when(userService.getById(12L)).thenReturn(buildUser(12L, "financeLeader", "财务负责人"));

        WorkflowStartBody startBody = new WorkflowStartBody();
        startBody.setProcessKey("parallel_apply");
        startBody.setBusinessNo("PA-20260310-001");

        boolean success = workflowEngineService.startProcess(startBody, 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
        verify(workflowTaskMapper, times(2)).insert(any(SysWorkflowTask.class));
    }

    /**
     * 验证 ALL 会签场景下，仍有同节点待办时不会提前流转。
     */
    @Test
    void shouldWaitAllCountersignTasksBeforeRouting() {
        SysWorkflowTask currentTask = new SysWorkflowTask();
        currentTask.setTaskId(101L);
        currentTask.setInstanceId(501L);
        currentTask.setTodoId(601L);
        currentTask.setDefinitionId(1L);
        currentTask.setAssigneeUserId(1L);
        currentTask.setStatus("0");
        currentTask.setNodeKey("NODE_CS");
        currentTask.setNodeName("会签审批");
        when(workflowTaskMapper.selectById(101L)).thenReturn(currentTask);
        when(workflowTaskMapper.updateById(any(SysWorkflowTask.class))).thenReturn(1);
        when(todoTaskService.updateById(any(SysTodoTask.class))).thenReturn(true);
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(workflowInstanceMapper.updateById(any(SysWorkflowInstance.class))).thenReturn(1);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(501L);
        instance.setDefinitionId(1L);
        instance.setTenantId("000000");
        instance.setProcessName("会签流程");
        instance.setBusinessNo("CS-001");
        instance.setInitiatorUserId(9L);
        instance.setModelContentSnapshot("{\"startNodeKey\":\"START_1\",\"nodes\":["
                + "{\"nodeKey\":\"START_1\",\"nodeType\":\"start\"},"
                + "{\"nodeKey\":\"NODE_CS\",\"nodeType\":\"approval\",\"approveStrategy\":\"ALL\"},"
                + "{\"nodeKey\":\"END_1\",\"nodeType\":\"end\"}"
                + "],\"edges\":[{\"from\":\"START_1\",\"to\":\"NODE_CS\"},{\"from\":\"NODE_CS\",\"to\":\"END_1\"}]}");
        when(workflowInstanceMapper.selectById(501L)).thenReturn(instance);

        SysWorkflowTask siblingTask = new SysWorkflowTask();
        siblingTask.setTaskId(102L);
        siblingTask.setNodeKey("NODE_CS");
        siblingTask.setNodeName("会签审批");
        siblingTask.setStatus("0");
        when(workflowTaskMapper.selectList(any())).thenReturn(Collections.singletonList(siblingTask), Collections.singletonList(siblingTask));

        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment("先审批");

        boolean success = workflowEngineService.approveTask(101L, actionBody, 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
        verify(workflowTaskMapper, never()).insert(any(SysWorkflowTask.class));
    }

    /**
     * 验证并行汇聚节点会等待其他前驱分支完成后再继续流转。
     */
    @Test
    void shouldWaitParallelJoinBeforeCreatingNextTask() {
        SysWorkflowTask currentTask = new SysWorkflowTask();
        currentTask.setTaskId(201L);
        currentTask.setInstanceId(701L);
        currentTask.setTodoId(801L);
        currentTask.setDefinitionId(1L);
        currentTask.setAssigneeUserId(1L);
        currentTask.setStatus("0");
        currentTask.setNodeKey("NODE_A");
        currentTask.setNodeName("部门审批");
        when(workflowTaskMapper.selectById(201L)).thenReturn(currentTask);
        when(workflowTaskMapper.updateById(any(SysWorkflowTask.class))).thenReturn(1);
        when(todoTaskService.updateById(any(SysTodoTask.class))).thenReturn(true);
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(workflowInstanceMapper.updateById(any(SysWorkflowInstance.class))).thenReturn(1);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(701L);
        instance.setDefinitionId(1L);
        instance.setTenantId("000000");
        instance.setProcessName("并行汇聚流程");
        instance.setBusinessNo("PJ-001");
        instance.setInitiatorUserId(9L);
        instance.setModelContentSnapshot("{\"startNodeKey\":\"START_1\",\"nodes\":["
                + "{\"nodeKey\":\"START_1\",\"nodeType\":\"start\"},"
                + "{\"nodeKey\":\"NODE_A\",\"nodeType\":\"approval\"},"
                + "{\"nodeKey\":\"NODE_B\",\"nodeType\":\"approval\"},"
                + "{\"nodeKey\":\"PARALLEL_1\",\"nodeType\":\"parallel\"},"
                + "{\"nodeKey\":\"NODE_C\",\"nodeType\":\"approval\",\"candidateUserIds\":[12]}"
                + "],\"edges\":["
                + "{\"from\":\"START_1\",\"to\":\"NODE_A\"},"
                + "{\"from\":\"START_1\",\"to\":\"NODE_B\"},"
                + "{\"from\":\"NODE_A\",\"to\":\"PARALLEL_1\"},"
                + "{\"from\":\"NODE_B\",\"to\":\"PARALLEL_1\"},"
                + "{\"from\":\"PARALLEL_1\",\"to\":\"NODE_C\"}"
                + "]}");
        when(workflowInstanceMapper.selectById(701L)).thenReturn(instance);

        SysWorkflowTask pendingBranchTask = new SysWorkflowTask();
        pendingBranchTask.setTaskId(202L);
        pendingBranchTask.setNodeKey("NODE_B");
        pendingBranchTask.setNodeName("财务审批");
        pendingBranchTask.setStatus("0");
        when(workflowTaskMapper.selectList(any())).thenReturn(Collections.emptyList(), Collections.singletonList(pendingBranchTask));
        when(workflowTaskMapper.selectCount(any())).thenReturn(0L, 1L, 1L);

        WorkflowTaskActionBody actionBody = new WorkflowTaskActionBody();
        actionBody.setActionComment("部门通过");

        boolean success = workflowEngineService.approveTask(201L, actionBody, 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
        verify(workflowTaskMapper, never()).insert(any(SysWorkflowTask.class));
    }

    /**
     * 验证驳回动作会同步更新实例与任务状态。
     */
    @Test
    void shouldRejectTaskSuccessfully() {
        SysWorkflowTask task = new SysWorkflowTask();
        task.setTaskId(2L);
        task.setInstanceId(11L);
        task.setTodoId(21L);
        task.setAssigneeUserId(1L);
        task.setStatus("0");
        task.setNodeName("财务复核");
        when(workflowTaskMapper.selectById(2L)).thenReturn(task);
        when(workflowTaskMapper.updateById(any(SysWorkflowTask.class))).thenReturn(1);
        when(todoTaskService.updateById(any(SysTodoTask.class))).thenReturn(true);
        when(workflowInstanceMapper.updateById(any(SysWorkflowInstance.class))).thenReturn(1);
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(noticeService.createNotice(any())).thenReturn(true);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(11L);
        instance.setDefinitionId(1L);
        instance.setTenantId("000000");
        instance.setProcessName("报销审批流程");
        instance.setBusinessNo("EX-20260310-001");
        instance.setInitiatorUserId(2L);
        when(workflowInstanceMapper.selectById(11L)).thenReturn(instance);

        boolean success = workflowEngineService.rejectTask(2L, new WorkflowTaskActionBody(), 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
    }

    /**
     * 验证转交任务会关闭原任务并创建新任务待办。
     */
    @Test
    void shouldTransferTaskSuccessfully() {
        SysWorkflowTask task = new SysWorkflowTask();
        task.setTaskId(3L);
        task.setInstanceId(12L);
        task.setDefinitionId(1L);
        task.setNodeKey("NODE_1");
        task.setNodeName("部门负责人审批");
        task.setTodoId(22L);
        task.setAssigneeUserId(1L);
        task.setStatus("0");
        task.setTenantId("000000");
        when(workflowTaskMapper.selectById(3L)).thenReturn(task);
        when(workflowTaskMapper.updateById(any(SysWorkflowTask.class))).thenReturn(1);
        when(todoTaskService.updateById(any(SysTodoTask.class))).thenReturn(true);
        when(todoTaskService.save(any(SysTodoTask.class))).thenAnswer(invocation -> {
            SysTodoTask todoTask = invocation.getArgument(0);
            todoTask.setTodoId(222L);
            return true;
        });
        when(workflowTaskMapper.insert(any(SysWorkflowTask.class))).thenReturn(1);
        when(workflowInstanceMapper.updateById(any(SysWorkflowInstance.class))).thenReturn(1);
        when(workflowTaskActionMapper.insert(any(SysWorkflowTaskAction.class))).thenReturn(1);
        when(noticeService.createNotice(any())).thenReturn(true);

        SysWorkflowInstance instance = new SysWorkflowInstance();
        instance.setInstanceId(12L);
        instance.setDefinitionId(1L);
        instance.setTenantId("000000");
        instance.setProcessName("采购审批流程");
        instance.setBusinessNo("PO-20260310-001");
        when(workflowInstanceMapper.selectById(12L)).thenReturn(instance);

        SysUser targetUser = new SysUser();
        targetUser.setUserId(9L);
        targetUser.setUserName("u9");
        targetUser.setNickName("用户9");
        when(userService.getById(9L)).thenReturn(targetUser);

        WorkflowTaskTransferBody transferBody = new WorkflowTaskTransferBody();
        transferBody.setTargetUserId(9L);
        transferBody.setActionComment("转交处理");

        boolean success = workflowEngineService.transferTask(3L, transferBody, 1L, "admin", "系统管理员");

        Assertions.assertTrue(success);
    }

    /**
     * 验证无效用户发起的审批会被拒绝。
     */
    @Test
    void shouldRejectApproveWhenUserNotAssignee() {
        SysWorkflowTask task = new SysWorkflowTask();
        task.setTaskId(5L);
        task.setInstanceId(15L);
        task.setTodoId(25L);
        task.setAssigneeUserId(2L);
        task.setStatus("0");
        when(workflowTaskMapper.selectById(5L)).thenReturn(task);

        boolean success = workflowEngineService.approveTask(5L, new WorkflowTaskActionBody(), 1L, "admin", "系统管理员");

        Assertions.assertFalse(success);
    }

    /**
     * 验证详情查询在实例不存在时返回空。
     */
    @Test
    void shouldReturnNullWhenInstanceNotExist() {
        when(workflowInstanceMapper.selectById(999L)).thenReturn(null);

        WorkflowInstanceDetailVO detail = workflowEngineService.selectInstanceDetail(999L);

        Assertions.assertNull(detail);
    }

    /**
     * 构造测试用户对象。
     *
     * @param userId 用户ID
     * @param userName 用户账号
     * @param nickName 用户昵称
     * @return 用户对象
     */
    private SysUser buildUser(Long userId, String userName, String nickName) {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setUserName(userName);
        user.setNickName(nickName);
        return user;
    }
}
