package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrEmployeePosition;
import com.erp.business.hr.domain.vo.HrEmployeePositionBody;
import com.erp.business.hr.domain.vo.HrEmployeePositionQuery;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrEmployeePositionMapper;
import com.erp.business.hr.service.IHrEmployeePositionService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;

/**
 * 员工任职服务实现。
 */
@Service
public class HrEmployeePositionServiceImpl implements IHrEmployeePositionService {
    private final HrEmployeePositionMapper positionMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final SecurityUserResolver securityUserResolver;

    public HrEmployeePositionServiceImpl(HrEmployeePositionMapper positionMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            SecurityUserResolver securityUserResolver) {
        this.positionMapper = positionMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 分页查询员工任职。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrEmployeePosition> selectPage(HrEmployeePositionQuery query) {
        HrEmployeePositionQuery safeQuery = query == null ? new HrEmployeePositionQuery() : query;
        Page<HrEmployeePosition> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(safeQuery.getPageNum()),
                HrEmployeeSupport.normalizePageSize(safeQuery.getPageSize()));
        return positionMapper.selectPage(page, new LambdaQueryWrapper<HrEmployeePosition>()
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeePosition::getTenantId, currentTenantId())
                .eq(safeQuery.getEmployeeId() != null, HrEmployeePosition::getEmployeeId, safeQuery.getEmployeeId())
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrEmployeePosition::getStatus,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getStatus()))
                .orderByDesc(HrEmployeePosition::getPrimaryFlag)
                .orderByDesc(HrEmployeePosition::getStartDate)
                .orderByDesc(HrEmployeePosition::getCreateTime));
    }

    /**
     * 查询员工任职列表。
     *
     * @param employeeId 员工ID
     * @return 任职列表
     */
    @Override
    public List<HrEmployeePosition> listByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return List.of();
        }
        return positionMapper.selectList(new LambdaQueryWrapper<HrEmployeePosition>()
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeePosition::getTenantId, currentTenantId())
                .eq(HrEmployeePosition::getEmployeeId, employeeId)
                .orderByDesc(HrEmployeePosition::getPrimaryFlag)
                .orderByDesc(HrEmployeePosition::getStartDate)
                .orderByDesc(HrEmployeePosition::getCreateTime));
    }

    /**
     * 新增员工任职。
     *
     * @param body 保存参数
     * @return 任职记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeePosition createPosition(HrEmployeePositionBody body) {
        HrEmployeeCore employee = requireEmployee(body == null ? null : body.getEmployeeId());
        Date now = new Date();
        HrEmployeePosition position = new HrEmployeePosition();
        position.setTenantId(employee.getTenantId());
        position.setEmployeeId(employee.getEmployeeId());
        position.setCreateBy(resolveOperator());
        position.setCreateTime(now);
        position.setUpdateBy(resolveOperator());
        position.setUpdateTime(now);
        applyBody(position, body);
        normalizePrimaryPosition(position);
        positionMapper.insert(position);
        return positionMapper.selectById(position.getPositionId());
    }

    /**
     * 更新员工任职。
     *
     * @param positionId 任职ID
     * @param body 保存参数
     * @return 任职记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeePosition updatePosition(Long positionId, HrEmployeePositionBody body) {
        if (positionId == null) {
            throw new IllegalArgumentException("任职ID不能为空");
        }
        HrEmployeePosition existed = requirePosition(positionId);
        HrEmployeePosition updateEntity = new HrEmployeePosition();
        updateEntity.setPositionId(positionId);
        updateEntity.setTenantId(existed.getTenantId());
        updateEntity.setEmployeeId(existed.getEmployeeId());
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        applyBody(updateEntity, body);
        normalizePrimaryPosition(updateEntity);
        positionMapper.updateById(updateEntity);
        return positionMapper.selectById(positionId);
    }

    /**
     * 应用保存参数。
     *
     * @param position 任职对象
     * @param body 保存参数
     */
    private void applyBody(HrEmployeePosition position, HrEmployeePositionBody body) {
        if (body == null) {
            throw new IllegalArgumentException("任职参数不能为空");
        }
        position.setOrgId(body.getOrgId());
        position.setDeptId(body.getDeptId());
        position.setPostId(body.getPostId());
        position.setPostName(HrEmployeeSupport.trimToNull(body.getPostName()));
        position.setPositionType(HrEmployeeSupport.trimToNull(body.getPositionType()));
        position.setPrimaryFlag(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.trimToNull(body.getPrimaryFlag()), HrEmployeeSupport.PRIMARY_FLAG_YES));
        position.setStartDate(body.getStartDate());
        position.setEndDate(body.getEndDate());
        position.setStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body.getStatus()), HrEmployeeSupport.STATUS_ACTIVE));
        position.setRemark(HrEmployeeSupport.trimToNull(body.getRemark()));
    }

    /**
     * 维护主岗唯一性。
     *
     * @param position 任职对象
     */
    private void normalizePrimaryPosition(HrEmployeePosition position) {
        if (!HrEmployeeSupport.PRIMARY_FLAG_YES.equalsIgnoreCase(position.getPrimaryFlag())) {
            position.setPrimaryFlag(HrEmployeeSupport.PRIMARY_FLAG_NO);
            return;
        }
        positionMapper.update(new HrEmployeePosition(), new LambdaUpdateWrapper<HrEmployeePosition>()
                .eq(HrEmployeePosition::getTenantId, position.getTenantId())
                .eq(HrEmployeePosition::getEmployeeId, position.getEmployeeId())
                .set(HrEmployeePosition::getPrimaryFlag, HrEmployeeSupport.PRIMARY_FLAG_NO)
                .set(HrEmployeePosition::getUpdateBy, resolveOperator())
                .set(HrEmployeePosition::getUpdateTime, new Date()));
    }

    /**
     * 校验员工是否存在。
     *
     * @param employeeId 员工ID
     * @return 员工主档
     */
    private HrEmployeeCore requireEmployee(Long employeeId) {
        if (employeeId == null) {
            throw new IllegalArgumentException("员工ID不能为空");
        }
        HrEmployeeCore employee = employeeCoreMapper.selectOne(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getEmployeeId, employeeId)
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG));
        if (employee == null) {
            throw new ServiceException("员工不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return employee;
    }

    /**
     * 校验任职是否存在。
     *
     * @param positionId 任职ID
     * @return 任职记录
     */
    private HrEmployeePosition requirePosition(Long positionId) {
        HrEmployeePosition position = positionMapper.selectOne(new LambdaQueryWrapper<HrEmployeePosition>()
                .eq(HrEmployeePosition::getPositionId, positionId)
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeePosition::getTenantId, currentTenantId()));
        if (position == null) {
            throw new ServiceException("任职记录不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return position;
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        return securityUserResolver.getCurrentTenantId();
    }

    /**
     * 获取当前操作人。
     *
     * @return 操作人
     */
    private String resolveOperator() {
        String username = securityUserResolver.getCurrentUsername();
        return StringUtils.hasText(username) ? username.trim() : "system";
    }
}
