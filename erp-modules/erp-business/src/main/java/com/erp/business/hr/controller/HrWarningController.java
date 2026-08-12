package com.erp.business.hr.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrWarningRecord;
import com.erp.business.hr.domain.vo.HrWarningHandleBody;
import com.erp.business.hr.domain.vo.HrWarningHomeSummaryVO;
import com.erp.business.hr.domain.vo.HrWarningQuery;
import com.erp.business.hr.service.IHrHomeSummaryService;
import com.erp.business.hr.service.IHrWarningService;
import com.erp.common.core.domain.PageData;
import com.erp.common.core.domain.R;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * HR 预警控制层。
 */
@RestController
@RequestMapping("/business/hr/warning")
public class HrWarningController {
    private final IHrWarningService warningService;
    private final IHrHomeSummaryService hrHomeSummaryService;

    public HrWarningController(IHrWarningService warningService,
            IHrHomeSummaryService hrHomeSummaryService) {
        this.warningService = warningService;
        this.hrHomeSummaryService = hrHomeSummaryService;
    }

    /**
     * 分页查询预警。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @GetMapping("/list")
    @PreAuthorize("@ss.hasPermi('business:hr:warning:list')")
    public R<PageData<HrWarningRecord>> list(HrWarningQuery query) {
        Page<HrWarningRecord> page = warningService.selectPage(query);
        return R.page(page.getRecords(), page.getCurrent(), page.getSize(), page.getTotal());
    }

    /**
     * 查询 HR 预警首页汇总。
     *
     * @return 汇总结果
     */
    @GetMapping("/home-summary")
    public R<HrWarningHomeSummaryVO> homeSummary() {
        return R.success(hrHomeSummaryService.buildWarningSummary());
    }

    /**
     * 手工触发预警扫描。
     *
     * @return 执行结果
     */
    @PostMapping("/scan")
    @PreAuthorize("@ss.hasPermi('business:hr:warning:scan')")
    public R<Boolean> scan() {
        warningService.scanWarnings();
        return R.success(true);
    }

    /**
     * 处理预警。
     *
     * @param warningId 预警ID
     * @param body 处理参数
     * @return 处理后的预警
     */
    @PostMapping("/handle/{warningId}")
    @PreAuthorize("@ss.hasPermi('business:hr:warning:handle')")
    public R<HrWarningRecord> handle(@PathVariable("warningId") Long warningId,
            @RequestBody(required = false) HrWarningHandleBody body) {
        return R.success(warningService.handleWarning(warningId, body));
    }
}
