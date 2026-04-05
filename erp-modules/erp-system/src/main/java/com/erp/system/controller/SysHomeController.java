package com.erp.system.controller;

import com.erp.common.core.domain.R;
import com.erp.system.domain.vo.SystemHomeHealthSummaryVO;
import com.erp.system.service.ISysHomeSummaryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 系统首页汇总控制层。
 */
@RestController
@RequestMapping("/system/home")
public class SysHomeController {

    private final ISysHomeSummaryService sysHomeSummaryService;

    public SysHomeController(ISysHomeSummaryService sysHomeSummaryService) {
        this.sysHomeSummaryService = sysHomeSummaryService;
    }

    /**
     * 查询系统健康汇总。
     *
     * @return 健康汇总
     */
    @GetMapping("/health-summary")
    public R<SystemHomeHealthSummaryVO> healthSummary() {
        return R.success(sysHomeSummaryService.buildHealthSummary());
    }
}
