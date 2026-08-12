package com.erp.business.saas.service;

import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.mapper.HrEmployeeDocumentMapper;
import com.erp.business.saas.domain.SaasStorageObject;
import com.erp.business.saas.mapper.SaasStorageObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasStorageObjectServiceTest {
    private final SaasStorageObjectMapper storageMapper = mock(SaasStorageObjectMapper.class);
    private final HrEmployeeDocumentMapper documentMapper = mock(HrEmployeeDocumentMapper.class);
    private final SaasStorageObjectService service = new SaasStorageObjectService(storageMapper, documentMapper);

    @Test
    void shouldCreateAndActivateLedgerWithDocumentAtomically() {
        when(storageMapper.insert(any())).thenReturn(1);
        when(documentMapper.insert(any())).thenReturn(1);
        when(storageMapper.markActive("tenant-a", "object-a")).thenReturn(1);
        HrEmployeeDocument document = document();
        when(documentMapper.selectById(4L)).thenReturn(document);

        service.createUploading("tenant-a", "object-a", 100L, "quota-ref");
        HrEmployeeDocument result = service.completeUpload(document);

        verify(storageMapper).insert(any(SaasStorageObject.class));
        verify(documentMapper).insert(document);
        verify(storageMapper).markActive("tenant-a", "object-a");
        assertThat(result).isSameAs(document);
    }

    private static HrEmployeeDocument document() {
        HrEmployeeDocument document = new HrEmployeeDocument();
        document.setDocumentId(4L);
        document.setTenantId("tenant-a");
        document.setFileUrl("object-a");
        document.setFileSize(100L);
        document.setStatus("ACTIVE");
        return document;
    }
}
