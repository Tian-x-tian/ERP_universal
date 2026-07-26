package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeArchive;
import com.erp.business.hr.domain.HrEmployeeContract;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.domain.HrWarningRecord;
import com.erp.business.hr.domain.vo.HrWarningHandleBody;
import com.erp.business.hr.domain.vo.HrWarningQuery;
import com.erp.business.hr.mapper.HrEmployeeArchiveMapper;
import com.erp.business.hr.mapper.HrEmployeeContractMapper;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrEmployeeDocumentMapper;
import com.erp.business.hr.mapper.HrWarningRecordMapper;
import com.erp.business.hr.service.IHrWarningService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import com.erp.platform.contract.model.PlatformNoticeCreateRequest;
import com.erp.platform.contract.model.PlatformUserView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * HR 预警服务实现。
 */
@Service
public class HrWarningServiceImpl implements IHrWarningService {
    private final HrWarningRecordMapper warningRecordMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final HrEmployeeArchiveMapper archiveMapper;
    private final HrEmployeeContractMapper contractMapper;
    private final HrEmployeeDocumentMapper documentMapper;
    private final InternalSystemClient internalSystemClient;
    private final SecurityUserResolver securityUserResolver;

    public HrWarningServiceImpl(HrWarningRecordMapper warningRecordMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            HrEmployeeArchiveMapper archiveMapper,
            HrEmployeeContractMapper contractMapper,
            HrEmployeeDocumentMapper documentMapper,
            InternalSystemClient internalSystemClient,
            SecurityUserResolver securityUserResolver) {
        this.warningRecordMapper = warningRecordMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.archiveMapper = archiveMapper;
        this.contractMapper = contractMapper;
        this.documentMapper = documentMapper;
        this.internalSystemClient = internalSystemClient;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 分页查询预警。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrWarningRecord> selectPage(HrWarningQuery query) {
        HrWarningQuery safeQuery = query == null ? new HrWarningQuery() : query;
        Page<HrWarningRecord> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(safeQuery.getPageNum()),
                HrEmployeeSupport.normalizePageSize(safeQuery.getPageSize()));
        return warningRecordMapper.selectPage(page, new LambdaQueryWrapper<HrWarningRecord>()
                .eq(StringUtils.hasText(currentTenantId()), HrWarningRecord::getTenantId, currentTenantId())
                .eq(StringUtils.hasText(safeQuery.getWarningType()), HrWarningRecord::getWarningType,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getWarningType()))
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrWarningRecord::getStatus,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getStatus()))
                .eq(safeQuery.getEmployeeId() != null, HrWarningRecord::getEmployeeId, safeQuery.getEmployeeId())
                .orderByDesc(HrWarningRecord::getExpireDate)
                .orderByDesc(HrWarningRecord::getUpdateTime));
    }

    /**
     * 触发预警扫描。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void scanWarnings() {
        List<HrEmployeeCore> employees = employeeCoreMapper.selectList(new LambdaQueryWrapper<HrEmployeeCore>()
                .eq(HrEmployeeCore::getDelFlag, HrEmployeeSupport.EXIST_DEL_FLAG)
                .eq(HrEmployeeCore::getStatus, HrEmployeeSupport.STATUS_ACTIVE)
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeCore::getTenantId, currentTenantId()));
        for (HrEmployeeCore employee : employees) {
            scanProbationWarning(employee);
            scanContractWarning(employee);
            scanDocumentWarning(employee);
        }
    }

    /**
     * 处理预警。
     *
     * @param warningId 预警ID
     * @param body 处理参数
     * @return 处理后的预警
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrWarningRecord handleWarning(Long warningId, HrWarningHandleBody body) {
        HrWarningRecord warning = warningRecordMapper.selectOne(new LambdaQueryWrapper<HrWarningRecord>()
                .eq(HrWarningRecord::getWarningId, warningId)
                .eq(StringUtils.hasText(currentTenantId()), HrWarningRecord::getTenantId, currentTenantId()));
        if (warning == null) {
            throw new ServiceException("预警记录不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        HrWarningRecord updateEntity = new HrWarningRecord();
        updateEntity.setWarningId(warningId);
        updateEntity.setStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body == null ? null : body.getTargetStatus()),
                HrEmployeeSupport.WARNING_STATUS_HANDLED));
        updateEntity.setHandledBy(resolveOperator());
        updateEntity.setHandledTime(new Date());
        updateEntity.setRemark(HrEmployeeSupport.trimToNull(body == null ? null : body.getRemark()));
        warningRecordMapper.updateById(updateEntity);
        return warningRecordMapper.selectById(warningId);
    }

    /**
     * 扫描试用期预警。
     *
     * @param employee 员工主档
     */
    private void scanProbationWarning(HrEmployeeCore employee) {
        HrEmployeeArchive archive = archiveMapper.selectById(employee.getEmployeeId());
        if (archive == null || archive.getProbationEndDate() == null) {
            return;
        }
        long remainDays = ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(archive.getProbationEndDate()));
        if (remainDays >= 0 && remainDays <= 7) {
            upsertWarning(employee, "PROBATION_EXPIRING", "PROBATION:" + employee.getEmployeeId(),
                    archive.getProbationEndDate(), "试用期即将到期", "员工试用期将在 " + remainDays + " 天后到期");
        }
    }

    /**
     * 扫描合同预警。
     *
     * @param employee 员工主档
     */
    private void scanContractWarning(HrEmployeeCore employee) {
        HrEmployeeContract contract = contractMapper.selectOne(new LambdaQueryWrapper<HrEmployeeContract>()
                .eq(HrEmployeeContract::getTenantId, employee.getTenantId())
                .eq(HrEmployeeContract::getEmployeeId, employee.getEmployeeId())
                .ne(HrEmployeeContract::getStatus, "DELETED")
                .orderByDesc(HrEmployeeContract::getEndDate)
                .last("limit 1"));
        if (contract == null || contract.getEndDate() == null) {
            return;
        }
        long remainDays = ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(contract.getEndDate()));
        if (remainDays >= 0 && remainDays <= 30) {
            upsertWarning(employee, "CONTRACT_EXPIRING", "CONTRACT:" + contract.getContractId(),
                    contract.getEndDate(), "合同即将到期", "员工合同将在 " + remainDays + " 天后到期");
        }
    }

    /**
     * 扫描电子档案预警。
     *
     * @param employee 员工主档
     */
    private void scanDocumentWarning(HrEmployeeCore employee) {
        List<HrEmployeeDocument> documents = documentMapper.selectList(new LambdaQueryWrapper<HrEmployeeDocument>()
                .eq(HrEmployeeDocument::getTenantId, employee.getTenantId())
                .eq(HrEmployeeDocument::getEmployeeId, employee.getEmployeeId())
                .eq(HrEmployeeDocument::getStatus, HrEmployeeSupport.DOCUMENT_STATUS_ACTIVE)
                .isNotNull(HrEmployeeDocument::getExpireDate));
        for (HrEmployeeDocument document : documents) {
            long remainDays = ChronoUnit.DAYS.between(LocalDate.now(), toLocalDate(document.getExpireDate()));
            if (remainDays >= 0 && remainDays <= 30) {
                upsertWarning(employee, "DOCUMENT_EXPIRING", "DOCUMENT:" + document.getDocumentId(),
                        document.getExpireDate(), "电子档案即将到期", "档案“" + document.getDocumentName() + "”将在 " + remainDays + " 天后到期");
            }
        }
    }

    /**
     * 保存或更新预警。
     *
     * @param employee 员工主档
     * @param warningType 预警类型
     * @param warningKey 幂等键
     * @param expireDate 到期时间
     * @param title 标题
     * @param content 内容
     */
    private void upsertWarning(HrEmployeeCore employee, String warningType, String warningKey, Date expireDate, String title, String content) {
        HrWarningRecord existing = warningRecordMapper.selectOne(new LambdaQueryWrapper<HrWarningRecord>()
                .eq(HrWarningRecord::getTenantId, employee.getTenantId())
                .eq(HrWarningRecord::getWarningKey, warningKey)
                .last("limit 1"));
        Date now = new Date();
        if (existing == null) {
            HrWarningRecord warning = new HrWarningRecord();
            warning.setTenantId(employee.getTenantId());
            warning.setEmployeeId(employee.getEmployeeId());
            warning.setWarningType(warningType);
            warning.setWarningKey(warningKey);
            warning.setWarningTitle(title);
            warning.setWarningContent(content);
            warning.setExpireDate(expireDate);
            warning.setStatus(HrEmployeeSupport.WARNING_STATUS_NEW);
            warningRecordMapper.insert(warning);
            createNotice(warning);
            return;
        }
        HrWarningRecord updateEntity = new HrWarningRecord();
        updateEntity.setWarningId(existing.getWarningId());
        updateEntity.setWarningTitle(title);
        updateEntity.setWarningContent(content);
        updateEntity.setExpireDate(expireDate);
        updateEntity.setStatus(HrEmployeeSupport.WARNING_STATUS_NEW);
        warningRecordMapper.updateById(updateEntity);
    }

    /**
     * 生成站内通知。
     *
     * @param warning 预警记录
     */
    private void createNotice(HrWarningRecord warning) {
        PlatformUserView receiver = resolveReceiver();
        if (receiver == null || receiver.getUserId() == null) {
            return;
        }
        PlatformNoticeCreateRequest notice = new PlatformNoticeCreateRequest();
        notice.setTenantId(warning.getTenantId());
        notice.setTitle(warning.getWarningTitle());
        notice.setNoticeType("HR_WARNING");
        notice.setSource("HR");
        notice.setBusinessNo(warning.getWarningKey());
        notice.setContent(warning.getWarningContent());
        notice.setReceiverUserId(receiver.getUserId());
        notice.setDeliveryChannel("IN_APP");
        notice.setDeliveryStatus("2");
        notice.setDeliveryTime(new Date());
        notice.setStatus("0");
        notice.setCreateTime(new Date());
        internalSystemClient.createNotice(notice);
    }

    /**
     * 解析通知接收人。
     *
     * @return 接收人
     */
    private PlatformUserView resolveReceiver() {
        String username = securityUserResolver.getCurrentUsername();
        if (StringUtils.hasText(username)) {
            PlatformUserView currentUser = internalSystemClient.getActiveUserByUsername(currentTenantId(), username.trim());
            if (currentUser != null) {
                return currentUser;
            }
        }
        return internalSystemClient.getFirstActiveUser(currentTenantId());
    }

    /**
     * 日期转本地日期。
     *
     * @param date 日期对象
     * @return 本地日期
     */
    private LocalDate toLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
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

