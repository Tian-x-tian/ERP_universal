package com.erp.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.erp.system.domain.MdmProject;
import com.erp.system.mapper.MdmProjectMapper;
import com.erp.system.security.service.SecurityUserResolver;
import com.erp.system.service.IMdmAuditTrailService;
import com.erp.system.service.IMdmProjectService;
import com.erp.system.support.MdmChangeTypeSupport;
import com.erp.system.support.MdmDomainTypeSupport;
import com.erp.system.support.MdmStatusSupport;
import com.erp.system.support.MdmValueSupport;
import com.erp.system.support.TenantWriteGuard;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * MDM 项目主数据服务实现。
 */
@Service
public class MdmProjectServiceImpl extends ServiceImpl<MdmProjectMapper, MdmProject> implements IMdmProjectService {
    private static final String DEL_FLAG_EXIST = "0";
    private static final String DEL_FLAG_DELETED = "2";
    private static final String DEFAULT_OPERATOR = "system";

    private final IMdmAuditTrailService auditTrailService;
    private final SecurityUserResolver securityUserResolver;

    public MdmProjectServiceImpl(IMdmAuditTrailService auditTrailService, SecurityUserResolver securityUserResolver) {
        this.auditTrailService = auditTrailService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 查询项目列表。
     *
     * @param projectCode 项目编码
     * @param projectName 项目名称
     * @param status      状态
     * @return 项目列表
     */
    @Override
    public List<MdmProject> selectProjectList(String projectCode, String projectName, String status) {
        LambdaQueryWrapper<MdmProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmProject::getDelFlag, DEL_FLAG_EXIST);
        if (StringUtils.hasText(projectCode)) {
            queryWrapper.like(MdmProject::getProjectCode, projectCode.trim());
        }
        if (StringUtils.hasText(projectName)) {
            queryWrapper.like(MdmProject::getProjectName, projectName.trim());
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq(MdmProject::getStatus, MdmStatusSupport.normalizeStatus(status));
        }
        queryWrapper.orderByDesc(MdmProject::getUpdateTime).orderByDesc(MdmProject::getCreateTime);
        return list(queryWrapper);
    }

    /**
     * 新增项目。
     *
     * @param project 项目对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createProject(MdmProject project) {
        if (project == null || !StringUtils.hasText(project.getProjectCode())
                || !StringUtils.hasText(project.getProjectName()) || !isDateRangeValid(project.getStartDate(), project.getEndDate())) {
            return false;
        }
        String tenantId = TenantWriteGuard.currentTenantId();
        if (!StringUtils.hasText(tenantId)) {
            return false;
        }
        String projectCode = project.getProjectCode().trim();
        if (existsProjectCode(projectCode, null)) {
            return false;
        }
        Date now = new Date();
        String operator = resolveOperator();
        project.setTenantId(tenantId);
        project.setProjectCode(projectCode);
        project.setProjectName(project.getProjectName().trim());
        project.setStatus(MdmStatusSupport.normalizeStatus(project.getStatus()));
        project.setVersionNo(1);
        project.setDelFlag(DEL_FLAG_EXIST);
        project.setCreateBy(operator);
        project.setUpdateBy(operator);
        project.setCreateTime(now);
        project.setUpdateTime(now);
        boolean saved = save(project);
        if (saved) {
            auditTrailService.record(MdmDomainTypeSupport.PROJECT,
                    project.getProjectId(),
                    MdmChangeTypeSupport.CREATE,
                    project.getVersionNo(),
                    project.getStatus(),
                    null,
                    project);
        }
        return saved;
    }

    /**
     * 修改项目。
     *
     * @param project 项目对象
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateProject(MdmProject project) {
        if (project == null || project.getProjectId() == null) {
            return false;
        }
        MdmProject existed = getOne(new LambdaQueryWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, project.getProjectId())
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (!isDateRangeValid(project.getStartDate() == null ? existed.getStartDate() : project.getStartDate(),
                project.getEndDate() == null ? existed.getEndDate() : project.getEndDate())) {
            return false;
        }
        MdmProject before = new MdmProject();
        BeanUtils.copyProperties(existed, before);

        if (StringUtils.hasText(project.getProjectCode())) {
            String projectCode = project.getProjectCode().trim();
            if (existsProjectCode(projectCode, project.getProjectId())) {
                return false;
            }
            project.setProjectCode(projectCode);
        }
        project.setProjectName(MdmValueSupport.trimToNull(project.getProjectName()));
        project.setStatus(MdmStatusSupport.normalizeStatusForUpdate(project.getStatus(), existed.getStatus()));
        project.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        project.setUpdateBy(resolveOperator());
        project.setUpdateTime(new Date());
        boolean updated = updateById(project);
        if (updated) {
            MdmProject after = getById(project.getProjectId());
            auditTrailService.record(MdmDomainTypeSupport.PROJECT,
                    project.getProjectId(),
                    MdmChangeTypeSupport.UPDATE,
                    after == null ? project.getVersionNo() : after.getVersionNo(),
                    after == null ? project.getStatus() : after.getStatus(),
                    before,
                    after);
        }
        return updated;
    }

    /**
     * 停用项目。
     *
     * @param projectId 项目ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean disableProject(Long projectId) {
        if (projectId == null) {
            return false;
        }
        MdmProject existed = getOne(new LambdaQueryWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null) {
            return false;
        }
        if (MdmStatusSupport.DISABLED.equals(existed.getStatus())) {
            return true;
        }
        MdmProject updateEntity = new MdmProject();
        updateEntity.setProjectId(projectId);
        updateEntity.setStatus(MdmStatusSupport.DISABLED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            MdmProject after = getById(projectId);
            auditTrailService.record(MdmDomainTypeSupport.PROJECT,
                    projectId,
                    MdmChangeTypeSupport.STATUS,
                    after == null ? updateEntity.getVersionNo() : after.getVersionNo(),
                    after == null ? updateEntity.getStatus() : after.getStatus(),
                    existed,
                    after);
        }
        return updated;
    }

    /**
     * 删除项目（逻辑删除）。
     *
     * @param projectId 项目ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeProject(Long projectId) {
        if (projectId == null) {
            return false;
        }
        MdmProject existed = getOne(new LambdaQueryWrapper<MdmProject>()
                .eq(MdmProject::getProjectId, projectId)
                .eq(MdmProject::getDelFlag, DEL_FLAG_EXIST));
        if (existed == null || !MdmStatusSupport.isDraft(existed.getStatus())) {
            return false;
        }
        MdmProject updateEntity = new MdmProject();
        updateEntity.setProjectId(projectId);
        updateEntity.setDelFlag(DEL_FLAG_DELETED);
        updateEntity.setVersionNo(MdmValueSupport.resolveNextVersionNo(existed.getVersionNo()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        boolean updated = updateById(updateEntity);
        if (updated) {
            auditTrailService.record(MdmDomainTypeSupport.PROJECT,
                    projectId,
                    MdmChangeTypeSupport.DELETE,
                    updateEntity.getVersionNo(),
                    existed.getStatus(),
                    existed,
                    null);
        }
        return updated;
    }

    /**
     * 判断项目编码是否重复。
     *
     * @param projectCode 项目编码
     * @param excludeId 排除主键
     * @return true 表示重复
     */
    private boolean existsProjectCode(String projectCode, Long excludeId) {
        LambdaQueryWrapper<MdmProject> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(MdmProject::getProjectCode, projectCode);
        queryWrapper.eq(MdmProject::getDelFlag, DEL_FLAG_EXIST);
        if (excludeId != null) {
            queryWrapper.ne(MdmProject::getProjectId, excludeId);
        }
        return count(queryWrapper) > 0;
    }

    /**
     * 校验项目日期区间。
     *
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @return true 表示合法
     */
    private boolean isDateRangeValid(Date startDate, Date endDate) {
        if (startDate == null || endDate == null) {
            return true;
        }
        return !startDate.after(endDate);
    }

    /**
     * 解析操作人账号。
     *
     * @return 操作人账号
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : DEFAULT_OPERATOR;
    }
}
