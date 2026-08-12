package com.erp.system.saas.impl;

import com.erp.common.client.internal.InternalBusinessClient;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import com.erp.saas.contract.model.SaasTenantPurgeResult;
import com.erp.saas.contract.model.SaasTenantStoragePurgeResult;
import com.erp.system.saas.SaasTenantDatabasePurgeExecutor;
import com.erp.system.saas.SaasTenantPurgeService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.regex.Pattern;

@Service
public class SaasTenantPurgeServiceImpl implements SaasTenantPurgeService {
    private static final Pattern REQUEST_ID = Pattern.compile("[A-Za-z0-9_-]{8,128}");
    private static final Pattern TENANT_ID = Pattern.compile("[A-Za-z0-9_-]{1,20}");
    private static final String PLATFORM_TENANT = "000000";

    private final InternalBusinessClient businessClient;
    private final SaasTenantDatabasePurgeExecutor databasePurgeExecutor;

    public SaasTenantPurgeServiceImpl(InternalBusinessClient businessClient,
            SaasTenantDatabasePurgeExecutor databasePurgeExecutor) {
        this.businessClient = businessClient;
        this.databasePurgeExecutor = databasePurgeExecutor;
    }

    @Override
    public SaasTenantPurgeResult purge(SaasTenantPurgeRequest request) {
        validate(request);
        SaasTenantStoragePurgeResult storage = businessClient.purgeSaasTenantStorage(request);
        if (storage == null) {
            throw new IllegalStateException("Business storage purge returned no result");
        }
        SaasTenantPurgeResult database = databasePurgeExecutor.purgeDatabase(request);
        return new SaasTenantPurgeResult(database.getRequestId(), database.getTenantId(),
                database.getTablesProcessed(), database.getRowsDeleted(), storage.getObjectsDeleted(),
                database.isReplayed() && storage.isReplayed());
    }

    static ValidatedPurge validate(SaasTenantPurgeRequest request) {
        String requestId = normalize(request == null ? null : request.getRequestId());
        String tenantId = normalize(request == null ? null : request.getTenantId());
        String confirmation = normalize(request == null ? null : request.getConfirmationTenantId());
        if (requestId == null || !REQUEST_ID.matcher(requestId).matches()) {
            throw new IllegalArgumentException("Purge request id is invalid");
        }
        if (tenantId == null || !TENANT_ID.matcher(tenantId).matches()) {
            throw new IllegalArgumentException("Tenant id is invalid");
        }
        if (!tenantId.equals(confirmation)) {
            throw new IllegalArgumentException("Typed tenant confirmation does not match");
        }
        if (PLATFORM_TENANT.equals(tenantId)) {
            throw new IllegalArgumentException("The platform tenant cannot be purged");
        }
        return new ValidatedPurge(requestId, tenantId);
    }

    private static String normalize(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    record ValidatedPurge(String requestId, String tenantId) {
    }
}
