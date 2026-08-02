package com.erp.business.hr.service.impl;

import com.erp.business.hr.domain.HrEmployeeCore;
import com.erp.business.hr.domain.HrEmployeeDocument;
import com.erp.business.hr.domain.vo.HrEmployeeDocumentBody;
import com.erp.business.hr.mapper.HrEmployeeCoreMapper;
import com.erp.business.hr.mapper.HrEmployeeDocumentMapper;
import com.erp.business.hr.service.IHrObjectStorageService;
import com.erp.business.saas.domain.SaasStorageObject;
import com.erp.business.saas.service.SaasStorageObjectService;
import com.erp.business.security.service.SecurityUserResolver;
import com.erp.common.client.internal.InternalSystemClient;
import com.erp.saas.contract.model.SaasQuotaKeys;
import com.erp.saas.contract.model.SaasUsageEvent;
import com.erp.saas.contract.model.SaasUsageOperation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HrEmployeeDocumentSaasQuotaTest {
    @Mock private HrEmployeeDocumentMapper documentMapper;
    @Mock private HrEmployeeCoreMapper employeeCoreMapper;
    @Mock private IHrObjectStorageService objectStorageService;
    @Mock private SecurityUserResolver securityUserResolver;
    @Mock private InternalSystemClient internalSystemClient;
    @Mock private SaasStorageObjectService storageObjectService;
    @Mock private MultipartFile file;

    private HrEmployeeDocumentServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HrEmployeeDocumentServiceImpl(documentMapper, employeeCoreMapper,
                objectStorageService, securityUserResolver, internalSystemClient, storageObjectService);
    }

    @Test
    void shouldReserveUploadSettleAndActivateStorageObject() {
        when(employeeCoreMapper.selectOne(any())).thenReturn(employee());
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(128L);
        when(file.getOriginalFilename()).thenReturn("contract.pdf");
        when(objectStorageService.upload(anyString(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(storageObjectService.completeUpload(any())).thenAnswer(invocation -> invocation.getArgument(0));

        HrEmployeeDocument result = service.uploadDocument(body(), file);

        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        ArgumentCaptor<String> objectKeyCaptor = ArgumentCaptor.forClass(String.class);
        InOrder order = inOrder(internalSystemClient, storageObjectService, objectStorageService);
        order.verify(internalSystemClient).applyQuotaEvent(eventCaptor.capture());
        order.verify(storageObjectService).createUploading(anyString(), objectKeyCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(128L), anyString());
        order.verify(objectStorageService).upload(objectKeyCaptor.getValue(), file);
        order.verify(internalSystemClient).applyQuotaEvent(eventCaptor.capture());
        order.verify(storageObjectService).completeUpload(any(HrEmployeeDocument.class));
        assertThat(eventCaptor.getAllValues()).extracting(SaasUsageEvent::getOperation)
                .containsExactly(SaasUsageOperation.RESERVE, SaasUsageOperation.SETTLE);
        assertThat(eventCaptor.getAllValues().get(0).getReferenceKey())
                .isEqualTo(eventCaptor.getAllValues().get(1).getReferenceKey());
        assertThat(eventCaptor.getAllValues().get(0).getMetricKey()).isEqualTo(SaasQuotaKeys.STORAGE_BYTES);
        assertThat(result.getFileSize()).isEqualTo(128L);
        assertThat(result.getFileUrl()).isEqualTo(objectKeyCaptor.getValue());
    }

    @Test
    void shouldDeleteAmbiguousUploadAndReleaseReservation() {
        when(employeeCoreMapper.selectOne(any())).thenReturn(employee());
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(64L);
        when(file.getOriginalFilename()).thenReturn("file.bin");
        when(objectStorageService.upload(anyString(), any())).thenThrow(new IllegalStateException("upload failed"));

        assertThatThrownBy(() -> service.uploadDocument(body(), file))
                .isInstanceOf(IllegalStateException.class).hasMessage("upload failed");

        verify(objectStorageService).delete(anyString());
        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        verify(internalSystemClient, org.mockito.Mockito.times(2)).applyQuotaEvent(eventCaptor.capture());
        assertThat(eventCaptor.getAllValues()).extracting(SaasUsageEvent::getOperation)
                .containsExactly(SaasUsageOperation.RESERVE, SaasUsageOperation.RELEASE);
        verify(storageObjectService).markDeleted(anyString(), anyString());
        verify(storageObjectService, never()).completeUpload(any());
    }

    @Test
    void shouldKeepQuotaAndMarkOrphanWhenCompensationDeleteFails() {
        when(employeeCoreMapper.selectOne(any())).thenReturn(employee());
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(64L);
        when(file.getOriginalFilename()).thenReturn("file.bin");
        when(objectStorageService.upload(anyString(), any())).thenThrow(new IllegalStateException("upload failed"));
        org.mockito.Mockito.doThrow(new IllegalStateException("delete failed"))
                .when(objectStorageService).delete(anyString());

        assertThatThrownBy(() -> service.uploadDocument(body(), file)).isInstanceOf(IllegalStateException.class);

        verify(storageObjectService).markOrphaned(anyString(), anyString(), anyString());
        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        verify(internalSystemClient).applyQuotaEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getOperation()).isEqualTo(SaasUsageOperation.RESERVE);
    }

    @Test
    void shouldReleaseSettledStorageOnlyAfterObjectDeletion() {
        HrEmployeeDocument document = new HrEmployeeDocument();
        document.setDocumentId(3L);
        document.setTenantId("tenant-a");
        document.setFileUrl("hr/tenant-a/object.pdf");
        document.setStatus("ACTIVE");
        when(documentMapper.selectOne(any())).thenReturn(document);
        SaasStorageObject storageObject = new SaasStorageObject();
        storageObject.setTenantId("tenant-a");
        storageObject.setObjectKey(document.getFileUrl());
        storageObject.setQuotaReferenceKey("storage-ref");
        when(storageObjectService.find("tenant-a", document.getFileUrl())).thenReturn(storageObject);
        when(storageObjectService.completeDelete(document)).thenReturn(true);

        assertThat(service.deleteDocument(3L)).isTrue();

        InOrder order = inOrder(objectStorageService, internalSystemClient, storageObjectService);
        order.verify(objectStorageService).delete(document.getFileUrl());
        ArgumentCaptor<SaasUsageEvent> eventCaptor = ArgumentCaptor.forClass(SaasUsageEvent.class);
        order.verify(internalSystemClient).applyQuotaEvent(eventCaptor.capture());
        order.verify(storageObjectService).completeDelete(document);
        assertThat(eventCaptor.getValue().getOperation()).isEqualTo(SaasUsageOperation.RELEASE);
        assertThat(eventCaptor.getValue().getReferenceKey()).isEqualTo("storage-ref");
    }

    private static HrEmployeeCore employee() {
        HrEmployeeCore employee = new HrEmployeeCore();
        employee.setEmployeeId(9L);
        employee.setTenantId("tenant-a");
        return employee;
    }

    private static HrEmployeeDocumentBody body() {
        HrEmployeeDocumentBody body = new HrEmployeeDocumentBody();
        body.setEmployeeId(9L);
        body.setDocumentType("CONTRACT");
        return body;
    }
}
