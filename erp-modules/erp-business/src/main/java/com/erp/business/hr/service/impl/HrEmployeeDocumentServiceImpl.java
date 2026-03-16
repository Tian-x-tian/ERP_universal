package com.erp.business.hr.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentBody;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentQuery;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrEmployeeDocumentMapper;
import com.erp.business.hr.service.IHrEmployeeDocumentService;
import com.erp.business.hr.service.IHrObjectStorageService;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.core.domain.ResultCode;
import com.erp.common.core.exception.ServiceException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.Locale;
import java.util.UUID;

/**
 * 员工电子档案服务实现。
 */
@Service
public class HrEmployeeDocumentServiceImpl implements IHrEmployeeDocumentService {
    private final HrEmployeeDocumentMapper documentMapper;
    private final HrEmployeeCoreMapper employeeCoreMapper;
    private final IHrObjectStorageService objectStorageService;
    private final SecurityUserResolver securityUserResolver;

    public HrEmployeeDocumentServiceImpl(HrEmployeeDocumentMapper documentMapper,
            HrEmployeeCoreMapper employeeCoreMapper,
            IHrObjectStorageService objectStorageService,
            SecurityUserResolver securityUserResolver) {
        this.documentMapper = documentMapper;
        this.employeeCoreMapper = employeeCoreMapper;
        this.objectStorageService = objectStorageService;
        this.securityUserResolver = securityUserResolver;
    }

    /**
     * 分页查询电子档案。
     *
     * @param query 查询参数
     * @return 分页结果
     */
    @Override
    public Page<HrEmployeeDocument> selectPage(HrEmployeeDocumentQuery query) {
        HrEmployeeDocumentQuery safeQuery = query == null ? new HrEmployeeDocumentQuery() : query;
        Page<HrEmployeeDocument> page = new Page<>(
                HrEmployeeSupport.normalizePageNum(safeQuery.getPageNum()),
                HrEmployeeSupport.normalizePageSize(safeQuery.getPageSize()));
        return documentMapper.selectPage(page, new LambdaQueryWrapper<HrEmployeeDocument>()
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeDocument::getTenantId, currentTenantId())
                .eq(safeQuery.getEmployeeId() != null, HrEmployeeDocument::getEmployeeId, safeQuery.getEmployeeId())
                .eq(StringUtils.hasText(safeQuery.getDocumentType()), HrEmployeeDocument::getDocumentType,
                        HrEmployeeSupport.trimToNull(safeQuery.getDocumentType()))
                .eq(StringUtils.hasText(safeQuery.getStatus()), HrEmployeeDocument::getStatus,
                        HrEmployeeSupport.normalizeStatus(safeQuery.getStatus()))
                .ge(safeQuery.getExpireDateFrom() != null, HrEmployeeDocument::getExpireDate, safeQuery.getExpireDateFrom())
                .le(safeQuery.getExpireDateTo() != null, HrEmployeeDocument::getExpireDate, safeQuery.getExpireDateTo())
                .ne(HrEmployeeDocument::getStatus, HrEmployeeSupport.DOCUMENT_STATUS_DELETED)
                .orderByDesc(HrEmployeeDocument::getExpireDate)
                .orderByDesc(HrEmployeeDocument::getUpdateTime));
    }

    /**
     * 查询电子档案详情。
     *
     * @param documentId 档案ID
     * @return 档案详情
     */
    @Override
    public HrEmployeeDocument getById(Long documentId) {
        HrEmployeeDocument document = documentMapper.selectOne(new LambdaQueryWrapper<HrEmployeeDocument>()
                .eq(HrEmployeeDocument::getDocumentId, documentId)
                .eq(StringUtils.hasText(currentTenantId()), HrEmployeeDocument::getTenantId, currentTenantId())
                .ne(HrEmployeeDocument::getStatus, HrEmployeeSupport.DOCUMENT_STATUS_DELETED));
        if (document == null) {
            throw new ServiceException("电子档案不存在", (int) ResultCode.NOT_FOUND.getCode());
        }
        return document;
    }

    /**
     * 上传电子档案并保存元数据。
     *
     * @param body 元数据
     * @param file 上传文件
     * @return 档案详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeDocument uploadDocument(HrEmployeeDocumentBody body, MultipartFile file) {
        HrEmployeeCore employee = requireEmployee(body == null ? null : body.getEmployeeId());
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }
        String objectKey = buildObjectKey(employee.getTenantId(), employee.getEmployeeId(), file.getOriginalFilename());
        String storedKey = objectStorageService.upload(objectKey, file);
        Date now = new Date();
        HrEmployeeDocument document = new HrEmployeeDocument();
        document.setTenantId(employee.getTenantId());
        document.setEmployeeId(employee.getEmployeeId());
        document.setDocumentType(HrEmployeeSupport.trimToNull(body == null ? null : body.getDocumentType()));
        document.setDocumentName(StringUtils.hasText(body == null ? null : body.getDocumentName())
                ? body.getDocumentName().trim()
                : file.getOriginalFilename());
        document.setFileUrl(storedKey);
        document.setExpireDate(body == null ? null : body.getExpireDate());
        document.setStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body == null ? null : body.getStatus()),
                HrEmployeeSupport.DOCUMENT_STATUS_ACTIVE));
        document.setRemark(HrEmployeeSupport.trimToNull(body == null ? null : body.getRemark()));
        document.setCreateBy(resolveOperator());
        document.setCreateTime(now);
        document.setUpdateBy(resolveOperator());
        document.setUpdateTime(now);
        documentMapper.insert(document);
        return documentMapper.selectById(document.getDocumentId());
    }

    /**
     * 更新电子档案元数据。
     *
     * @param documentId 档案ID
     * @param body 元数据
     * @return 档案详情
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeDocument updateDocument(Long documentId, HrEmployeeDocumentBody body) {
        HrEmployeeDocument existed = getById(documentId);
        HrEmployeeDocument updateEntity = new HrEmployeeDocument();
        updateEntity.setDocumentId(documentId);
        updateEntity.setTenantId(existed.getTenantId());
        updateEntity.setEmployeeId(existed.getEmployeeId());
        updateEntity.setDocumentType(HrEmployeeSupport.trimToNull(body == null ? null : body.getDocumentType()));
        updateEntity.setDocumentName(HrEmployeeSupport.trimToNull(body == null ? null : body.getDocumentName()));
        updateEntity.setExpireDate(body == null ? null : body.getExpireDate());
        updateEntity.setStatus(HrEmployeeSupport.defaultIfBlank(
                HrEmployeeSupport.normalizeStatus(body == null ? null : body.getStatus()), existed.getStatus()));
        updateEntity.setRemark(HrEmployeeSupport.trimToNull(body == null ? null : body.getRemark()));
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        documentMapper.updateById(updateEntity);
        return documentMapper.selectById(documentId);
    }

    /**
     * 删除电子档案。
     *
     * @param documentId 档案ID
     * @return true 表示成功
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteDocument(Long documentId) {
        HrEmployeeDocument existed = getById(documentId);
        HrEmployeeDocument updateEntity = new HrEmployeeDocument();
        updateEntity.setDocumentId(documentId);
        updateEntity.setTenantId(existed.getTenantId());
        updateEntity.setStatus(HrEmployeeSupport.DOCUMENT_STATUS_DELETED);
        updateEntity.setUpdateBy(resolveOperator());
        updateEntity.setUpdateTime(new Date());
        return documentMapper.updateById(updateEntity) > 0;
    }

    /**
     * 生成预览链接。
     *
     * @param documentId 档案ID
     * @return 预览链接
     */
    @Override
    public String buildPreviewUrl(Long documentId) {
        HrEmployeeDocument document = getById(documentId);
        return objectStorageService.generatePreviewUrl(document.getFileUrl());
    }

    /**
     * 生成下载链接。
     *
     * @param documentId 档案ID
     * @return 下载链接
     */
    @Override
    public String buildDownloadUrl(Long documentId) {
        HrEmployeeDocument document = getById(documentId);
        return objectStorageService.generateDownloadUrl(document.getFileUrl());
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
     * 生成对象键。
     *
     * @param tenantId 租户编号
     * @param employeeId 员工ID
     * @param originalFilename 原始文件名
     * @return 对象键
     */
    private String buildObjectKey(String tenantId, Long employeeId, String originalFilename) {
        String fileName = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "document.bin";
        String extension = "";
        int separatorIndex = fileName.lastIndexOf('.');
        if (separatorIndex >= 0) {
            extension = fileName.substring(separatorIndex).toLowerCase(Locale.ROOT);
        }
        return "hr/" + tenantId + "/employee/" + employeeId + "/document/" + UUID.randomUUID().toString().replace("-", "") + extension;
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
