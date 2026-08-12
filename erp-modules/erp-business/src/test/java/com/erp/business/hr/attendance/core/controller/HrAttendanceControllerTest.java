package com.erp.business.hr.attendance.core.controller;

import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceCompanySummaryVO;
import com.erp.business.hr.attendance.core.domain.vo.HrAttendanceDashboardVO;
import com.erp.business.hr.attendance.core.service.IHrAttendanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 出勤控制层单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrAttendanceControllerTest {

    @Mock
    private IHrAttendanceService attendanceService;

    private MockMvc mockMvc;

    /**
     * 初始化控制器测试环境。
     */
    @BeforeEach
    void setUp() {
        HrAttendanceController controller = new HrAttendanceController(attendanceService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    /**
     * 验证工作台接口可以通过统一出勤入口返回结果。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldExposeDashboardEndpoint() throws Exception {
        when(attendanceService.getDashboard()).thenReturn(new HrAttendanceDashboardVO());

        mockMvc.perform(get("/business/hr/attendance/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceService).getDashboard();
    }

    /**
     * 验证公司汇总接口可以通过统一出勤入口返回结果。
     *
     * @throws Exception MockMvc 执行异常
     */
    @Test
    void shouldExposeCompanySummaryEndpoint() throws Exception {
        when(attendanceService.getCompanySummary("2026-04")).thenReturn(new HrAttendanceCompanySummaryVO());

        mockMvc.perform(get("/business/hr/attendance/company/summary").param("month", "2026-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));

        verify(attendanceService).getCompanySummary("2026-04");
    }
}
