package com.erp.business.hr.controller;

import com.erp.business.hr.domain.vo.HrWarningHomeSummaryVO;
import com.erp.business.hr.service.IHrHomeSummaryService;
import com.erp.business.hr.service.IHrWarningService;
import com.erp.common.core.domain.R;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.when;

/**
 * HR 预警控制器首页汇总接口单元测试。
 */
@ExtendWith(MockitoExtension.class)
class HrWarningControllerHomeSummaryTest {

    @Mock
    private IHrWarningService warningService;

    @Mock
    private IHrHomeSummaryService hrHomeSummaryService;

    /**
     * 验证首页汇总接口返回预期结构。
     */
    @Test
    void shouldReturnHrWarningHomeSummary() {
        HrWarningHomeSummaryVO summaryVO = new HrWarningHomeSummaryVO();
        summaryVO.setAbnormalEmployeeCount(9L);
        when(hrHomeSummaryService.buildWarningSummary()).thenReturn(summaryVO);
        HrWarningController controller = new HrWarningController(warningService, hrHomeSummaryService);

        R<HrWarningHomeSummaryVO> response = controller.homeSummary();

        Assertions.assertNotNull(response);
        Assertions.assertNotNull(response.getData());
        Assertions.assertEquals(9L, response.getData().getAbnormalEmployeeCount());
    }
}
