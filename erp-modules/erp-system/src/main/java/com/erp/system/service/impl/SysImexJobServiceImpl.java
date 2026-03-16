package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.system.domain.SysImexJob;
import com.erp.system.mapper.SysImexJobMapper;
import com.erp.system.service.ISysImexJobService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 导入导出任务服务实现。
 */
@Service
public class SysImexJobServiceImpl implements ISysImexJobService {

    private final SysImexJobMapper imexJobMapper;

    public SysImexJobServiceImpl(SysImexJobMapper imexJobMapper) {
        this.imexJobMapper = imexJobMapper;
    }

    /**
     * 查询导入导出任务分页。
     *
     * @param jobType 任务类型
     * @param status 状态
     * @param moduleCode 模块编码
     * @param pageNum 页码
     * @param pageSize 页长
     * @return 分页结果
     */
    @Override
    public Page<SysImexJob> selectPage(String jobType, String status, String moduleCode, Long pageNum, Long pageSize) {
        Page<SysImexJob> page = new Page<>(normalizePageNum(pageNum), normalizePageSize(pageSize));
        LambdaQueryWrapper<SysImexJob> queryWrapper = new LambdaQueryWrapper<SysImexJob>()
                .eq(StringUtils.hasText(currentTenantId()), SysImexJob::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(jobType), SysImexJob::getJobType, jobType == null ? null : jobType.trim().toUpperCase())
                .eq(StringUtils.hasText(status), SysImexJob::getStatus, status == null ? null : status.trim().toUpperCase())
                .like(StringUtils.hasText(moduleCode), SysImexJob::getModuleCode, moduleCode == null ? null : moduleCode.trim().toUpperCase())
                .orderByDesc(SysImexJob::getUpdateTime)
                .orderByDesc(SysImexJob::getCreateTime);
        return imexJobMapper.selectPage(page, queryWrapper);
    }

    /**
     * 查询任务详情。
     *
     * @param jobId 任务ID
     * @return 任务详情
     */
    @Override
    public SysImexJob getDetail(Long jobId) {
        SysImexJob job = imexJobMapper.selectOne(new LambdaQueryWrapper<SysImexJob>()
                .eq(SysImexJob::getJobId, jobId)
                .eq(StringUtils.hasText(currentTenantId()), SysImexJob::getTenantId, currentTenantId()));
        if (job == null) {
            throw new ServiceException("导入导出任务不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return job;
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        String tenantId = TenantContextHolder.getTenantId();
        return StringUtils.hasText(tenantId) ? tenantId.trim() : null;
    }

    /**
     * 规范化页码。
     *
     * @param pageNum 原始页码
     * @return 标准页码
     */
    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum < 1 ? 1L : pageNum;
    }

    /**
     * 规范化页长。
     *
     * @param pageSize 原始页长
     * @return 标准页长
     */
    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 20L;
        }
        return Math.min(pageSize, 200L);
    }
}
