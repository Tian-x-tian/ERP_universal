package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.common.core.context.TenantContextHolder;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.platform.contract.model.PlatformImexJobCreateRequest;
import com.erp.platform.contract.model.PlatformImexJobUpdateRequest;
import com.erp.system.domain.SysImexJob;
import com.erp.system.mapper.SysImexJobMapper;
import com.erp.system.service.ISysImexJobService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Date;

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
     * 通过内部契约创建导入导出任务。
     *
     * @param request 创建参数
     * @return 新建任务
     */
    @Override
    public SysImexJob createInternalJob(PlatformImexJobCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("导入导出任务参数不能为空");
        }
        Date now = new Date();
        SysImexJob job = new SysImexJob();
        job.setTenantId(trimToNull(request.getTenantId()));
        job.setJobNo(trimToNull(request.getJobNo()));
        job.setJobType(trimToNull(request.getJobType()));
        job.setModuleCode(trimToNull(request.getModuleCode()));
        job.setFileName(trimToNull(request.getFileName()));
        job.setFilePath(trimToNull(request.getFilePath()));
        job.setStatus(trimToNull(request.getStatus()));
        job.setProgress(request.getProgress());
        job.setTriggerType(trimToNull(request.getTriggerType()));
        job.setMessage(trimToNull(request.getMessage()));
        job.setCreateBy(trimToNull(request.getCreateBy()));
        job.setCreateTime(request.getCreateTime() == null ? now : request.getCreateTime());
        job.setUpdateBy(trimToNull(request.getUpdateBy()));
        job.setUpdateTime(request.getUpdateTime() == null ? now : request.getUpdateTime());
        imexJobMapper.insert(job);
        return imexJobMapper.selectById(job.getJobId());
    }

    /**
     * 通过内部契约更新导入导出任务。
     *
     * @param jobId   任务ID
     * @param request 更新参数
     * @return 更新后的任务
     */
    @Override
    public SysImexJob updateInternalJob(Long jobId, PlatformImexJobUpdateRequest request) {
        if (jobId == null || request == null) {
            throw new IllegalArgumentException("导入导出任务更新参数不能为空");
        }
        SysImexJob existed = imexJobMapper.selectById(jobId);
        if (existed == null) {
            throw new ServiceException("导入导出任务不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        SysImexJob updateEntity = new SysImexJob();
        updateEntity.setJobId(jobId);
        updateEntity.setFilePath(trimToNull(request.getFilePath()));
        updateEntity.setStatus(trimToNull(request.getStatus()));
        updateEntity.setProgress(request.getProgress());
        updateEntity.setMessage(trimToNull(request.getMessage()));
        updateEntity.setUpdateBy(trimToNull(request.getUpdateBy()));
        updateEntity.setUpdateTime(request.getUpdateTime() == null ? new Date() : request.getUpdateTime());
        imexJobMapper.updateById(updateEntity);
        return imexJobMapper.selectById(jobId);
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

    /**
     * 规范化文本值。
     *
     * @param value 原始值
     * @return 规范化结果
     */
    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}

