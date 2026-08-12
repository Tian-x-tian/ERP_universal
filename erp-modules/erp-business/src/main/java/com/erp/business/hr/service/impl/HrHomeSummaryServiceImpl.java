package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.erp.business.hr.domain.HrWarningRecord;
import com.erp.business.hr.domain.vo.HrWarningHomeSummaryVO;
import com.erp.business.hr.mapper.HrWarningRecordMapper;
import com.erp.business.hr.service.IHrHomeSummaryService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.PermissionService;
import com.erp.business.security.service.SecurityUserResolver;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * HR 首页汇总服务实现。
 */
@Service
public class HrHomeSummaryServiceImpl implements IHrHomeSummaryService {

    private final HrWarningRecordMapper warningRecordMapper;
    private final PermissionService permissionService;
    private final SecurityUserResolver securityUserResolver;

    public HrHomeSummaryServiceImpl(HrWarningRecordMapper warningRecordMapper,
            PermissionService permissionService,
            SecurityUserResolver securityUserResolver) {
        this.warningRecordMapper = warningRecordMapper;
        this.permissionService = permissionService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 构建预警首页汇总数据。
     *
     * @return 汇总数据
     */
    @Override
    public HrWarningHomeSummaryVO buildWarningSummary() {
        if (!permissionService.hasPermi("business:hr:warning:list")) {
            return emptySummary();
        }
        String tenantId = securityUserResolver.getCurrentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return emptySummary();
        }
        List<HrWarningRecord> warningRecordList = warningRecordMapper.selectList(new LambdaQueryWrapper<HrWarningRecord>()
                .eq(HrWarningRecord::getTenantId, tenantId.trim())
                .eq(HrWarningRecord::getStatus, HrEmployeeSupport.WARNING_STATUS_NEW));
        List<HrWarningRecord> newWarningList = (warningRecordList == null ? Collections.<HrWarningRecord>emptyList() : warningRecordList)
                .stream()
                .filter(record -> HrEmployeeSupport.WARNING_STATUS_NEW.equals(record.getStatus()))
                .collect(Collectors.toList());
        if (newWarningList.isEmpty()) {
            return emptySummary();
        }
        Set<Long> abnormalEmployeeSet = newWarningList.stream()
                .map(HrWarningRecord::getEmployeeId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Date now = new Date();
        Date urgentDeadline = new Date(now.getTime() + 3L * 24L * 60L * 60L * 1000L);
        long urgentWarningCount = newWarningList.stream()
                .filter(record -> record.getExpireDate() != null)
                .filter(record -> !record.getExpireDate().before(now) && !record.getExpireDate().after(urgentDeadline))
                .count();
        HrWarningHomeSummaryVO summaryVO = new HrWarningHomeSummaryVO();
        summaryVO.setAbnormalEmployeeCount(abnormalEmployeeSet.size());
        summaryVO.setUrgentWarningCount(urgentWarningCount);
        return summaryVO;
    }

    /**
     * 构建空安全汇总对象。
     *
     * @return 汇总对象
     */
    private HrWarningHomeSummaryVO emptySummary() {
        HrWarningHomeSummaryVO summaryVO = new HrWarningHomeSummaryVO();
        summaryVO.setAbnormalEmployeeCount(0L);
        summaryVO.setUrgentWarningCount(0L);
        return summaryVO;
    }
}
