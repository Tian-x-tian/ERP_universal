package com.erp.business.saas.service;

import com.erp.business.hr.service.IHrObjectStorageService;
import com.erp.business.saas.domain.SaasStorageObject;
import com.erp.business.saas.mapper.SaasStorageObjectMapper;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantStoragePurgeResult;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.regex.Pattern;

@Service
public class SaasTenantStoragePurgeService {
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{8,128}");
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private static final String PLATFORM_TENANT = "000000";

    private final SaasStorageObjectMapper storageMapper;
    private final IHrObjectStorageService objectStorageService;

    public SaasTenantStoragePurgeService(SaasStorageObjectMapper storageMapper,
            IHrObjectStorageService objectStorageService) {
        this.storageMapper = storageMapper;
        this.objectStorageService = objectStorageService;
    }

    public SaasTenantStoragePurgeResult purge(SaasTenantPurgeRequest request) {
        String requestId = normalized(request == null ? null : request.getRequestId());
        String tenantId = normalized(request == null ? null : request.getTenantId());
        String confirmation = normalized(request == null ? null : request.getConfirmationTenantId());
        if (requestId == null || !REQUEST_ID.matcher(requestId).matches()) {
            throw new IllegalArgumentException("Purge request id is invalid");
        }
        if (tenantId == null || !TENANT_ID.matcher(tenantId).matches()
                || PLATFORM_TENANT.equals(tenantId)) {
            throw new IllegalArgumentException("Tenant id is invalid for storage purge");
        }
        if (!tenantId.equals(confirmation)) {
            throw new IllegalArgumentException("Typed tenant confirmation does not match");
        }

        List<SaasStorageObject> objects = storageMapper.findPurgeCandidates(tenantId);
        int deleted = 0;
        for (SaasStorageObject object : objects) {
            if (object == null || !tenantId.equals(object.getTenantId())
                    || !StringUtils.hasText(object.getObjectKey())) {
                throw new IllegalStateException("Storage ledger contains an invalid purge candidate");
            }
            objectStorageService.delete(object.getObjectKey().trim());
            deleted++;
        }
        return new SaasTenantStoragePurgeResult(requestId, tenantId, deleted, objects.isEmpty());
    }

    private static String normalized(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
