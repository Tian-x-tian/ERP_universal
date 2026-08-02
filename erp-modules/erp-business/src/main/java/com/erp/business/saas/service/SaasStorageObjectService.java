package com.erp.business.saas.service;

import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.mapper.HrEmployeeDocumentMapper;
import com.erp.business.hr.support.HrEmployeeSupport;
import com.erp.business.saas.domain.SaasStorageObject;
import com.erp.business.saas.mapper.SaasStorageObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;

@Service
public class SaasStorageObjectService {
    private final SaasStorageObjectMapper storageMapper;
    private final HrEmployeeDocumentMapper documentMapper;

    public SaasStorageObjectService(SaasStorageObjectMapper storageMapper,
            HrEmployeeDocumentMapper documentMapper) {
        this.storageMapper = storageMapper;
        this.documentMapper = documentMapper;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void createUploading(String tenantId, String objectKey, long byteSize, String quotaReferenceKey) {
        requireText(tenantId, 20, "tenantId");
        requireText(objectKey, 512, "objectKey");
        requireText(quotaReferenceKey, 128, "quotaReferenceKey");
        if (byteSize <= 0) {
            throw new IllegalArgumentException("byteSize must be positive");
        }
        SaasStorageObject object = new SaasStorageObject();
        object.setTenantId(tenantId);
        object.setObjectKey(objectKey);
        object.setByteSize(byteSize);
        object.setStatus("UPLOADING");
        object.setQuotaReferenceKey(quotaReferenceKey);
        object.setCreateTime(new Date());
        object.setUpdateTime(new Date());
        if (storageMapper.insert(object) != 1) {
            throw new IllegalStateException("Storage ledger was not created");
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public HrEmployeeDocument completeUpload(HrEmployeeDocument document) {
        if (document == null || !StringUtils.hasText(document.getTenantId())
                || !StringUtils.hasText(document.getFileUrl())) {
            throw new IllegalArgumentException("Document storage metadata is incomplete");
        }
        if (documentMapper.insert(document) != 1
                || storageMapper.markActive(document.getTenantId(), document.getFileUrl()) != 1) {
            throw new IllegalStateException("Document upload metadata was not completed");
        }
        return documentMapper.selectById(document.getDocumentId());
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean completeDelete(HrEmployeeDocument document) {
        if (document == null || document.getDocumentId() == null) {
            return false;
        }
        HrEmployeeDocument update = new HrEmployeeDocument();
        update.setDocumentId(document.getDocumentId());
        update.setTenantId(document.getTenantId());
        update.setStatus(HrEmployeeSupport.DOCUMENT_STATUS_DELETED);
        if (documentMapper.updateById(update) != 1) {
            return false;
        }
        if (storageMapper.markTerminal(document.getTenantId(), document.getFileUrl(), "DELETED", null) != 1) {
            throw new IllegalStateException("Storage ledger was not deleted");
        }
        return true;
    }

    public SaasStorageObject find(String tenantId, String objectKey) {
        if (!StringUtils.hasText(tenantId) || !StringUtils.hasText(objectKey)) {
            return null;
        }
        return storageMapper.find(tenantId, objectKey);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markDeleted(String tenantId, String objectKey) {
        storageMapper.markTerminal(tenantId, objectKey, "DELETED", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void markOrphaned(String tenantId, String objectKey, String errorType) {
        String safeError = StringUtils.hasText(errorType) ? errorType.trim() : "unknown";
        if (safeError.length() > 128) {
            safeError = safeError.substring(0, 128);
        }
        storageMapper.markTerminal(tenantId, objectKey, "ORPHANED", safeError);
    }

    private static void requireText(String value, int maxLength, String field) {
        if (!StringUtils.hasText(value) || value.length() > maxLength) {
            throw new IllegalArgumentException("Invalid " + field);
        }
    }
}
