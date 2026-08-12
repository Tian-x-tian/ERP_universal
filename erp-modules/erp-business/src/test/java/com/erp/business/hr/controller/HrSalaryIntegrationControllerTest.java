package com.erp.business.hr.controller;

import com.erp.business.hr.service.IHrSalaryIntegrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Collections;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 薪酬核算别名接口控制层单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrSalaryIntegrationControllerTest {

    @Mock
    private IHrSalaryIntegrationService salaryIntegrationService;

    private MockMvc mockMvc;

    /**
     * 初始化控制器测试环境。
     */
    @BeforeEach
    void setUp() {
        HrSalaryIntegrationController controller = new HrSalaryIntegrationController(salaryIntegrationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 验证薪酬核算别名路径可以复用原有映射查询接口。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldExposePayrollAliasMappingEndpoint() throws Exception {
        when(salaryIntegrationService.listMappings()).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/business/hr/payroll/mapping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(salaryIntegrationService).listMappings();
    }
}
