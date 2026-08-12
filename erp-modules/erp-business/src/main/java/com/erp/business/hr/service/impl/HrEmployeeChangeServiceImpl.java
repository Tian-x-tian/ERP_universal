package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.erp.business.hr.domain.HrEmployeeChange;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.vo.HrEmployeeArchiveBody;
import com.erp.business.hr.domain.vo.HrEmployeeChangeQuery;
import com.erp.business.hr.domain.vo.HrEmployeeChangeSubmitBody;
import com.erp.business.hr.mapper.HrEmployeeChangeMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.service.IHrEmployeeArchiveService;
import com.erp.business.hr.service.IHrEmployeeChangeService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 员工异动服务实现。
 */
@Service
public class HrEmployeeChangeServiceImpl implements IHrEmployeeChangeService {
    private static final String SNAPSHOT_KEY_CORE = "core";
    private static final String SNAPSHOT_KEY_ARCHIVE = "archive";

    private final HrEmployeeChangeMapper changeMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final IHrEmployeeArchiveService archiveService;
    private final SecurityUserResolver securityUserResolver;
    private final ObjectMapper objectMapper;

    public HrEmployeeChangeServiceImpl(HrEmployeeChangeMapper changeMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            IHrEmployeeArchiveService archiveService,
            SecurityUserResolver securityUserResolver) {
        this.changeMapper = changeMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.archiveService = archiveService;
        this.securityUserResolver = securityUserResolver;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 分页查询异动记录。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrEmployeeChange> selectPage(HrEmployeeChangeQuery query) {
        HrEmployeeChangeQuery safeQuery = query == null ? new HrEmployeeChangeQuery() : query;
        Page<HrEmployeeChange> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(safeQuery.getPageNum()),
                HrEmployeeSupport.normalizePageSize(safeQuery.getPageSize()));
        return changeMapper.selectPage(page, new LambdaQueryWrapper<HrEmployeeChange>()
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeChange::getTenantId, currentTenantId())
                .eq(safeQuery.getEmployeeId() != null, HrEmployeeChange::getEmployeeId, safeQuery.getEmployeeId())
                .eq(StringUtils.hasText(safeQuery.getChangeType()), HrEmployeeChange::getChangeType,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getChangeType()))
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrEmployeeChange::getStatus,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getStatus()))
                .orderByDesc(HrEmployeeChange::getCreateTime));
    }

    /**
     * 按员工查询异动记录。
     *
     * @param employeeId 员工ID
     * @return 异动记录列表
     */
    @Override
    public List<HrEmployeeChange> listByEmployeeId(Long employeeId) {
        if (employeeId == null) {
            return List.of();
        }
        return changeMapper.selectList(new LambdaQueryWrapper<HrEmployeeChange>()
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeChange::getTenantId, currentTenantId())
                .eq(HrEmployeeChange::getEmployeeId, employeeId)
                .orderByDesc(HrEmployeeChange::getCreateTime));
    }

    /**
     * 创建待审批异动记录。
     *
     * @param employeeId 员工ID
     * @param body 提交参数
     * @return 异动记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeChange createChange(Long employeeId, HrEmployeeChangeSubmitBody body) {
        HrEmployeeCore employee = requireEmployee(employeeId);
        if (!HrEmployeeSupport.STATUS_ACTIVE.equalsIgnoreCase(employee.getStatus())) {
            throw new IllegalStateException("仅在职员工允许提交 HR 异动审批");
        }
        HrEmployeeChange change = new HrEmployeeChange();
        Date now = new Date();
        change.setTenantId(employee.getTenantId());
        change.setEmployeeId(employeeId);
        change.setChangeType(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body == null ? null : body.getChangeType()),
                HrEmployeeSupport.CHANGE_TYPE_ARCHIVE));
        change.setEffectiveDate(body == null ? null : body.getEffectiveDate());
        Map<String, Object> beforeSnapshot = new LinkedHashMap<>();
        beforeSnapshot.put(SNAPSHOT_KEY_CORE, employee);
        beforeSnapshot.put(SNAPSHOT_KEY_ARCHIVE, archiveService.getArchiveByEmployeeId(employeeId));
        change.setBeforeSnapshot(writeSnapshot(beforeSnapshot));
        Map<String, Object> afterSnapshot = new LinkedHashMap<>();
        afterSnapshot.put(SNAPSHOT_KEY_CORE, body == null ? null : body.getEmployee());
        afterSnapshot.put(SNAPSHOT_KEY_ARCHIVE, body == null ? null : body.getArchive());
        change.setAfterSnapshot(writeSnapshot(afterSnapshot));
        change.setStatus(HrEmployeeSupport.CHANGE_STATUS_SUBMITTED);
        change.setRemark(HrEmployeeSupport.trimToNull(body == null ? null : body.getRemark()));
        changeMapper.insert(change);
        return changeMapper.selectById(change.getChangeId());
    }

    /**
     * 审批通过后生效异动。
     *
     * @param changeId 异动ID
     * @param archiveBody 审批后的扩展档案
     * @param approvedBy 审批人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approveChange(Long changeId, HrEmployeeArchiveBody archiveBody, String approvedBy) {
        HrEmployeeChange change = getById(changeId);
        HrEmployeeArchiveBody targetArchive = archiveBody == null ? readAfterArchive(change) : archiveBody;
        if (targetArchive != null) {
            archiveService.applyApprovedArchive(change.getEmployeeId(), targetArchive, approvedBy);
        }
        updateChangeStatus(change, HrEmployeeSupport.CHANGE_STATUS_APPROVED, approvedBy);
    }

    /**
     * 审批驳回后回写状态。
     *
     * @param changeId 异动ID
     * @param rejectedBy 审批人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectChange(Long changeId, String rejectedBy) {
        HrEmployeeChange change = getById(changeId);
        updateChangeStatus(change, HrEmployeeSupport.CHANGE_STATUS_REJECTED, rejectedBy);
    }

    /**
     * 根据异动ID查询记录。
     *
     * @param changeId 异动ID
     * @return 异动记录
     */
    @Override
    public HrEmployeeChange getById(Long changeId) {
        HrEmployeeChange change = changeMapper.selectOne(new LambdaQueryWrapper<HrEmployeeChange>()
                .eq(HrEmployeeChange::getChangeId, changeId)
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeChange::getTenantId, currentTenantId()));
        if (change == null) {
            throw new ServiceException("异动记录不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return change;
    }

    /**
     * 解析审批后的核心员工快照。
     *
     * @param change 变更记录
     * @return 核心员工快照
     */
    @SuppressWarnings("unchecked")
    @Override
    public Map<String, Object> readAfterEmployee(HrEmployeeChange change) {
        Map<String, Object> snapshot = readSnapshot(change == null ? null : change.getAfterSnapshot());
        Object employee = snapshot.get(SNAPSHOT_KEY_CORE);
        return employee instanceof Map ? (Map<String, Object>) employee : Map.of();
    }

    /**
     * 解析审批后的扩展档案快照。
     *
     * @param change 变更记录
     * @return 扩展档案快照
     */
    @Override
    public HrEmployeeArchiveBody readAfterArchive(HrEmployeeChange change) {
        Map<String, Object> snapshot = readSnapshot(change == null ? null : change.getAfterSnapshot());
        Object archive = snapshot.get(SNAPSHOT_KEY_ARCHIVE);
        if (archive == null) {
            return null;
        }
        return objectMapper.convertValue(archive, HrEmployeeArchiveBody.class);
    }

    /**
     * 更新异动状态。
     *
     * @param change 异动记录
     * @param status 目标状态
     * @param operator 操作人
     */
    private void updateChangeStatus(HrEmployeeChange change, String status, String operator) {
        HrEmployeeChange updateEntity = new HrEmployeeChange();
        updateEntity.setChangeId(change.getChangeId());
        updateEntity.setStatus(status);
        updateEntity.setUpdateBy(StringUtils.hasText(operator) ? operator.trim() : "system");
        changeMapper.updateById(updateEntity);
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
     * 序列化快照。
     *
     * @param snapshot 快照数据
     * @return JSON 字符串
     */
    private String writeSnapshot(Object snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("异动快照序列化失败", ex);
        }
    }

    /**
     * 解析快照。
     *
     * @param snapshotJson 快照JSON
     * @return 快照Map
     */
    private Map<String, Object> readSnapshot(String snapshotJson) {
        if (!StringUtils.hasText(snapshotJson)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(snapshotJson, new TypeReference<Map<String, Object>>() { });
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("异动快照解析失败", ex);
        }
    }

    /**
     * 获取当前租户编号。
     *
     * @return 租户编号
     */
    private String currentTenantId() {
        return securityUserResolver.getCurrentTenantId();
    }

}
