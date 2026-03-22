package com.erp.system.ai.service.impl;

import com.erp.common.client.internal.InternalWorkflowClient;
import com.erp.system.ai.config.ErpAiProperties;
import com.erp.system.ai.mapper.AiTodoTaskMapper;
import com.erp.system.ai.model.AiChatRequest;
import com.erp.system.ai.model.AiPageContext;
import com.erp.system.ai.model.AiPromptContext;
import com.erp.system.domain.SysNotice;
import com.erp.system.domain.SysUser;
import com.erp.system.mapper.SysNoticeMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.ISysUserService;
import com.erp.workflow.contract.domain.SysTodoTask;
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
 * AI 上下文聚合服务测试。
 */
@ExtendWith(MockitoExtension.class)
class AiContextServiceImplTest {

    @Mock
    private SecurityUserResolver securityUserResolver;

    @Mock
    private ISysUserService userService;

    @Mock
    private SysNoticeMapper noticeMapper;

    @Mock
    private AiTodoTaskMapper todoTaskMapper;

    @Mock
    private InternalWorkflowClient internalWorkflowClient;

    private AiContextServiceImpl aiContextService;

    /**
     * 初始化被测对象。
     */
    @BeforeEach
    void setUp() {
        ErpAiProperties erpAiProperties = new ErpAiProperties();
        erpAiProperties.setMaxTodoItems(2);
        erpAiProperties.setMaxNoticeItems(2);
        aiContextService = new AiContextServiceImpl(
                securityUserResolver,
                userService,
                noticeMapper,
                todoTaskMapper,
                internalWorkflowClient,
                erpAiProperties);
    }

    /**
     * 验证上下文聚合会带出用户、页面、待办和消息摘要。
     */
    @Test
    void shouldBuildPromptContextWithTodoAndNoticeSummary() {
        when(securityUserResolver.getCurrentUserId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(buildUser());
        when(todoTaskMapper.selectCount(any())).thenReturn(3L);
        when(todoTaskMapper.selectList(any())).thenReturn(Arrays.asList(buildTodo("采购审批"), buildTodo("入库审批")));
        when(noticeMapper.selectCount(any())).thenReturn(4L);
        when(noticeMapper.selectList(any())).thenReturn(Arrays.asList(buildNotice("系统提醒"), buildNotice("流程催办")));
        when(internalWorkflowClient.listDefinitionLite(any(), any(), any())).thenReturn(Collections.emptyList());

        AiPromptContext promptContext = aiContextService.buildPromptContext(buildRequest());

        Assertions.assertNotNull(promptContext.getCurrentUser());
        Assertions.assertEquals("admin", promptContext.getCurrentUser().getUserName());
        Assertions.assertEquals("/workbench/process-todo", promptContext.getPageContext().getPath());
        Assertions.assertTrue(promptContext.isTodoContextAvailable());
        Assertions.assertEquals(3, promptContext.getTodoCount());
        Assertions.assertEquals(2, promptContext.getTodoList().size());
        Assertions.assertTrue(promptContext.isNoticeContextAvailable());
        Assertions.assertEquals(4, promptContext.getUnreadNoticeCount());
        Assertions.assertEquals(2, promptContext.getNoticeList().size());
    }

    /**
     * 验证待办查询异常时会自动降级，不中断整体上下文聚合。
     */
    @Test
    void shouldDegradeTodoContextWhenTodoQueryFails() {
        when(securityUserResolver.getCurrentUserId()).thenReturn(1L);
        when(userService.getById(1L)).thenReturn(buildUser());
        when(todoTaskMapper.selectCount(any())).thenThrow(new RuntimeException("todo table missing"));
        when(noticeMapper.selectCount(any())).thenReturn(1L);
        when(noticeMapper.selectList(any())).thenReturn(Arrays.asList(buildNotice("系统提醒")));
        when(internalWorkflowClient.listDefinitionLite(any(), any(), any())).thenReturn(Collections.emptyList());

        AiPromptContext promptContext = aiContextService.buildPromptContext(buildRequest());

        Assertions.assertFalse(promptContext.isTodoContextAvailable());
        Assertions.assertTrue(promptContext.getTodoContextMessage().contains("待办"));
        Assertions.assertTrue(promptContext.isNoticeContextAvailable());
    }

    /**
     * 构造 AI 请求对象。
     *
     * @return AI 请求对象
     */
    private AiChatRequest buildRequest() {
        AiPageContext pageContext = new AiPageContext();
        pageContext.setPath("/workbench/process-todo");
        pageContext.setTitle("待办事项");
        AiChatRequest request = new AiChatRequest();
        request.setPageContext(pageContext);
        return request;
    }

    /**
     * 构造用户对象。
     *
     * @return 用户对象
     */
    private SysUser buildUser() {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        user.setNickName("系统管理员");
        user.setTenantId("000000");
        return user;
    }

    /**
     * 构造待办对象。
     *
     * @param processName 流程名称
     * @return 待办对象
     */
    private SysTodoTask buildTodo(String processName) {
        SysTodoTask todoTask = new SysTodoTask();
        todoTask.setProcessName(processName);
        todoTask.setNodeName("部门审批");
        todoTask.setBusinessNo("WF20260322001");
        todoTask.setPriority("H");
        todoTask.setStatus("0");
        todoTask.setDueTime(new Date());
        todoTask.setCreateTime(new Date());
        return todoTask;
    }

    /**
     * 构造消息对象。
     *
     * @param title 标题
     * @return 消息对象
     */
    private SysNotice buildNotice(String title) {
        SysNotice notice = new SysNotice();
        notice.setTitle(title);
        notice.setNoticeType("流程消息");
        notice.setSource("workflow");
        notice.setBusinessNo("WF20260322001");
        notice.setStatus("0");
        notice.setDeliveryStatus("2");
        notice.setCreateTime(new Date());
        return notice;
    }
}
