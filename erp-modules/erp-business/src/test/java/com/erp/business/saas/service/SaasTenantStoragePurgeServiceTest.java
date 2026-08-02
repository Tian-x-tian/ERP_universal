package com.erp.business.saas.service;

import com.erp.business.hr.service.IHrObjectStorageService;
import com.erp.business.saas.domain.SaasStorageObject;
import com.erp.business.saas.mapper.SaasStorageObjectMapper;
import com.erp.saas.contract.model.SaasTenantPurgeRequest;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasTenantStoragePurgeServiceTest {
    @Test
    void shouldDeleteEveryRegisteredObjectBeforeDatabasePurge() {
        SaasStorageObjectMapper mapper = mock(SaasStorageObjectMapper.class);
        IHrObjectStorageService storage = mock(IHrObjectStorageService.class);
        SaasTenantStoragePurgeService service = new SaasTenantStoragePurgeService(mapper, storage);
        when(mapper.findPurgeCandidates("tenant-a")).thenReturn(List.of(
                object("tenant-a", "tenant-a/hr/one.pdf"),
                object("tenant-a", "tenant-a/hr/two.pdf")));

        var result = service.purge(new SaasTenantPurgeRequest(
                "purge-001", "tenant-a", "tenant-a"));

        assertThat(result.getObjectsDeleted()).isEqualTo(2);
        verify(storage).delete("tenant-a/hr/one.pdf");
        verify(storage).delete("tenant-a/hr/two.pdf");
    }

    private SaasStorageObject object(String tenantId, String key) {
        SaasStorageObject object = new SaasStorageObject();
        object.setTenantId(tenantId);
        object.setObjectKey(key);
        return object;
    }
}
