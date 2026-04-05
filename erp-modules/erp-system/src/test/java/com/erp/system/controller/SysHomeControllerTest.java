package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.vo.SystemHomeHealthSummaryVO;
import com.erp.system.service.ISysHomeSummaryService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 * 系统首页控制器单元测试。
 */
@ExtendWith(MockitoExtension.class)
class SysHomeControllerTest {

    @Mock
    private ISysHomeSummaryService sysHomeSummaryService;

    /**
     * 验证系统健康汇总接口返回结构完整。
     */
    @Test
    void shouldReturnHealthSummary() {
        SystemHomeHealthSummaryVO summaryVO = new SystemHomeHealthSummaryVO();
        summaryVO.setSuccessRate24h(88.88D);
        when(sysHomeSummaryService.buildHealthSummary()).thenReturn(summaryVO);
        SysHomeController controller = new SysHomeController(sysHomeSummaryService);

        R<SystemHomeHealthSummaryVO> response = controller.healthSummary();

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getData());
        Assertions.assertEquals(88.88D, response.getData().getSuccessRate24h());
    }
}
