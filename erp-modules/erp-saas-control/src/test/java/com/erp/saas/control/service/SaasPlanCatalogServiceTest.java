package com.erp.saas.control.service;

import com.erp.saas.control.domain.FeatureStatus;
import com.erp.saas.control.domain.PlanStatus;
import com.erp.saas.control.domain.entity.SaasFeatureEntity;
import com.erp.saas.control.domain.entity.SaasPlanEntity;
import com.erp.saas.control.domain.entity.SaasPlanFeatureEntity;
import com.erp.saas.control.mapper.SaasFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanFeatureMapper;
import com.erp.saas.control.mapper.SaasPlanMapper;
import com.erp.saas.control.mapper.SaasPlanQuotaMapper;
import com.erp.saas.control.service.impl.SaasPlanCatalogServiceImpl;
import com.erp.saas.control.service.model.FeatureDefinitionCommand;
import com.erp.saas.control.service.model.PlanDraftCommand;
import com.erp.saas.control.service.model.PlanFeatureGrantCommand;
import com.erp.saas.control.service.model.PublishPlanCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasPlanCatalogServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-01T02:03:04Z");
    private SaasPlanMapper planMapper;
    private SaasFeatureMapper featureMapper;
    private SaasPlanFeatureMapper planFeatureMapper;
    private SaasPlanQuotaMapper planQuotaMapper;
    private SaasPlanCatalogService service;

    @BeforeEach
    void setUp() {
        planMapper = mock(SaasPlanMapper.class);
        featureMapper = mock(SaasFeatureMapper.class);
        planFeatureMapper = mock(SaasPlanFeatureMapper.class);
        planQuotaMapper = mock(SaasPlanQuotaMapper.class);
        service = new SaasPlanCatalogServiceImpl(planMapper, featureMapper, planFeatureMapper,
                planQuotaMapper, new ControlUtcTime(Clock.fixed(NOW, ZoneOffset.UTC)));
    }

    @Test
    void shouldCreateAuditedDraftAndRejectDuplicateVersion() {
        when(planMapper.findFamilyForUpdate("starter")).thenReturn(List.of());
        doAnswer(invocation -> {
            SaasPlanEntity entity = invocation.getArgument(0);
            entity.setPlanId(101L);
            return 1;
        }).when(planMapper).insert(any(SaasPlanEntity.class));

        var view = service.createDraft(new PlanDraftCommand("starter", 1, "Starter", 14, 7, null), " admin ");

        assertThat(view.planId()).isEqualTo(101L);
        assertThat(view.status()).isEqualTo(PlanStatus.DRAFT);
        assertThat(view.versionNo()).isZero();
        ArgumentCaptor<SaasPlanEntity> captor = ArgumentCaptor.forClass(SaasPlanEntity.class);
        verify(planMapper).insert(captor.capture());
        assertThat(captor.getValue().getCreateBy()).isEqualTo("admin");
        assertThat(captor.getValue().getCreateTime()).isEqualTo(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC));

        when(planMapper.findFamilyForUpdate("starter")).thenReturn(List.of(plan(1L, "starter", 1, PlanStatus.DRAFT, 0L)));
        assertCode(SaasCatalogException.ErrorCode.DUPLICATE,
                () -> service.createDraft(new PlanDraftCommand("starter", 1, "Other", 0, 0, null), "admin"));
    }

    @Test
    void shouldUpdateOnlyDraftWithVersionCas() {
        SaasPlanEntity draft = plan(1L, "starter", 1, PlanStatus.DRAFT, 3L);
        when(planMapper.selectById(1L)).thenReturn(draft);
        when(planMapper.findFamilyForUpdate("starter")).thenReturn(List.of(draft));
        when(planMapper.updateDraft(any(), eq(3L), eq("admin"), any())).thenReturn(1);

        var view = service.updateDraft(1L, 3L,
                new PlanDraftCommand("starter", 1, "Starter Plus", 30, 7, null), "admin");
        assertThat(view.versionNo()).isEqualTo(4L);
        assertThat(view.planName()).isEqualTo("Starter Plus");

        draft.setStatus(PlanStatus.ACTIVE);
        assertCode(SaasCatalogException.ErrorCode.IMMUTABLE_PUBLISHED_PLAN,
                () -> service.updateDraft(1L, 3L,
                        new PlanDraftCommand("starter", 1, "No", 0, 0, null), "admin"));
    }

    @Test
    void shouldReplaceDraftFeaturesAndBumpAggregateVersion() {
        SaasPlanEntity draft = plan(1L, "starter", 1, PlanStatus.DRAFT, 4L);
        SaasFeatureEntity feature = feature(9L, "reports.view", FeatureStatus.ACTIVE, 2L);
        when(planMapper.findByIdForUpdate(1L)).thenReturn(draft);
        when(featureMapper.findByKeyForUpdate("reports.view")).thenReturn(feature);
        when(planMapper.bumpDraft(eq(1L), eq(4L), eq("admin"), any())).thenReturn(1);

        var view = service.replaceDraftFeatures(1L, 4L,
                List.of(new PlanFeatureGrantCommand("reports.view", true)), "admin");

        assertThat(view.versionNo()).isEqualTo(5L);
        verify(planFeatureMapper).deleteByPlanId(1L);
        ArgumentCaptor<SaasPlanFeatureEntity> captor = ArgumentCaptor.forClass(SaasPlanFeatureEntity.class);
        verify(planFeatureMapper).insert(captor.capture());
        assertThat(captor.getValue().getFeatureId()).isEqualTo(9L);

        assertCode(SaasCatalogException.ErrorCode.DUPLICATE,
                () -> service.replaceDraftFeatures(1L, 4L, List.of(
                        new PlanFeatureGrantCommand("reports.view", true),
                        new PlanFeatureGrantCommand("reports.view", false)), "admin"));
    }

    @Test
    void shouldRetireExpectedActiveAndPublishDraftAtomically() {
        SaasPlanEntity active = plan(10L, "starter", 1, PlanStatus.ACTIVE, 5L);
        SaasPlanEntity draft = plan(11L, "starter", 2, PlanStatus.DRAFT, 2L);
        when(planMapper.selectById(11L)).thenReturn(draft);
        when(planMapper.findFamilyForUpdate("starter")).thenReturn(List.of(active, draft));
        when(planMapper.retire(eq(10L), eq(5L), eq("admin"), any())).thenReturn(1);
        when(planMapper.activate(eq(11L), eq(2L), eq("admin"), any())).thenReturn(1);

        var view = service.publish(new PublishPlanCommand(11L, 2L, 10L, 5L), "admin");

        assertThat(view.status()).isEqualTo(PlanStatus.ACTIVE);
        assertThat(view.versionNo()).isEqualTo(3L);
        verify(planMapper).retire(eq(10L), eq(5L), eq("admin"), any());
        verify(planMapper).activate(eq(11L), eq(2L), eq("admin"), any());
    }

    @Test
    void shouldRequireExactPublicationGenerationAndAllowStrictIdempotence() {
        SaasPlanEntity active = plan(11L, "starter", 2, PlanStatus.ACTIVE, 3L);
        when(planMapper.selectById(11L)).thenReturn(active);
        when(planMapper.findFamilyForUpdate("starter")).thenReturn(List.of(active));

        var view = service.publish(new PublishPlanCommand(11L, 3L, 11L, 3L), "admin");
        assertThat(view.status()).isEqualTo(PlanStatus.ACTIVE);
        verify(planMapper, never()).activate(any(), any(), any(), any());

        assertCode(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                () -> service.publish(new PublishPlanCommand(11L, 3L, null, null), "admin"));
    }

    @Test
    void shouldDefineAndVersionFeatureWithUtcAudit() {
        when(featureMapper.findByKeyForUpdate("reports.view")).thenReturn(null);
        doAnswer(invocation -> {
            SaasFeatureEntity entity = invocation.getArgument(0);
            entity.setFeatureId(7L);
            return 1;
        }).when(featureMapper).insert(any(SaasFeatureEntity.class));
        var created = service.defineFeature(new FeatureDefinitionCommand(
                "reports.view", "Reports", FeatureStatus.ACTIVE, null), "admin");
        assertThat(created.featureId()).isEqualTo(7L);

        SaasFeatureEntity entity = feature(7L, "reports.view", FeatureStatus.ACTIVE, 6L);
        when(featureMapper.selectById(7L)).thenReturn(entity);
        when(featureMapper.findByKeyForUpdate("reports.view")).thenReturn(entity);
        when(featureMapper.updateVersioned(any(), eq(6L), eq("admin"), any())).thenReturn(0);
        assertCode(SaasCatalogException.ErrorCode.VERSION_CONFLICT,
                () -> service.updateFeature(7L, 6L, new FeatureDefinitionCommand(
                        "reports.view", "Reports 2", FeatureStatus.ACTIVE, null), "admin"));
    }

    @Test
    void shouldMarkEveryWriteMethodTransactionalWithRollback() {
        for (var method : SaasPlanCatalogServiceImpl.class.getDeclaredMethods()) {
            if (!java.lang.reflect.Modifier.isPublic(method.getModifiers())) {
                continue;
            }
            Transactional annotation = method.getAnnotation(Transactional.class);
            assertThat(annotation).as(method.getName()).isNotNull();
            assertThat(annotation.rollbackFor()).contains(Exception.class);
        }
    }

    private static SaasPlanEntity plan(Long id, String code, int version, PlanStatus status, long versionNo) {
        SaasPlanEntity entity = new SaasPlanEntity();
        entity.setPlanId(id);
        entity.setPlanCode(code);
        entity.setPlanVersion(version);
        entity.setPlanName("Plan " + version);
        entity.setStatus(status);
        entity.setTrialDays(14);
        entity.setGraceDays(7);
        entity.setVersionNo(versionNo);
        return entity;
    }

    private static SaasFeatureEntity feature(Long id, String key, FeatureStatus status, long versionNo) {
        SaasFeatureEntity entity = new SaasFeatureEntity();
        entity.setFeatureId(id);
        entity.setFeatureKey(key);
        entity.setFeatureName(key);
        entity.setStatus(status);
        entity.setVersionNo(versionNo);
        return entity;
    }

    private static void assertCode(SaasCatalogException.ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(SaasCatalogException.class)
                .extracting(error -> ((SaasCatalogException) error).getErrorCode())
                .isEqualTo(code);
    }
}
