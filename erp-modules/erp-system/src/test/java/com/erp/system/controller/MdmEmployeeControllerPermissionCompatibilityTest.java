package com.erp.system.controller;

import com.erp.system.domain.MdmEmployee;
import com.erp.system.domain.vo.MdmEmployeeWorkflowSubmitBody;
import com.erp.system.domain.vo.MdmVersionActionBody;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

/**
 * 员工控制器权限兼容性测试。
 */
class MdmEmployeeControllerPermissionCompatibilityTest {

    /**
     * 验证 HR 权限可复用员工核心接口。
     *
     * @throws NoSuchMethodException 反射异常
     */
    @Test
    void shouldContainBusinessHrPermissionsInPreAuthorizeExpressions() throws NoSuchMethodException {
        assertAuthorizeContains("list", "business:hr:employee:list", String.class, String.class, String.class,
                Long.class, Long.class);
        assertAuthorizeContains("getInfo", "business:hr:employee:query", Long.class);
        assertAuthorizeContains("add", "business:hr:employee:add", MdmEmployee.class);
        assertAuthorizeContains("edit", "business:hr:employee:edit", MdmEmployee.class);
        assertAuthorizeContains("leave", "business:hr:employee:leave", Long.class, MdmVersionActionBody.class);
        assertAuthorizeContains("submit", "business:hr:employee:submit", Long.class, MdmEmployeeWorkflowSubmitBody.class);
        assertAuthorizeContains("submitChange", "business:hr:employee:submit", Long.class, MdmEmployeeWorkflowSubmitBody.class);
        assertAuthorizeContains("submitLeave", "business:hr:employee:leave", Long.class, MdmEmployeeWorkflowSubmitBody.class);
        assertAuthorizeContains("remove", "business:hr:employee:remove", Long.class, MdmVersionActionBody.class);
    }

    /**
     * 校验指定方法的权限表达式包含目标权限。
     *
     * @param methodName       方法名
     * @param expectedFragment 预期权限片段
     * @param parameterTypes   参数类型
     * @throws NoSuchMethodException 反射异常
     */
    private void assertAuthorizeContains(String methodName, String expectedFragment, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = MdmEmployeeController.class.getMethod(methodName, parameterTypes);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        Assertions.assertNotNull(preAuthorize, "方法缺少 PreAuthorize 注解: " + methodName);
        Assertions.assertTrue(preAuthorize.value().contains(expectedFragment),
                "方法权限表达式未包含 HR 权限: " + methodName);
    }
}
