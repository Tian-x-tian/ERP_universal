package com.erp.saas.control.service.domain;

import com.erp.saas.contract.model.TenantLifecycleState;
import com.erp.saas.control.domain.DomainVerificationMethod;
import com.erp.saas.control.domain.DomainVerificationState;
import com.erp.saas.control.domain.entity.SaasDomainEntity;
import com.erp.saas.control.domain.entity.SaasTenantEntity;
import com.erp.saas.control.mapper.SaasDomainMapper;
import com.erp.saas.control.mapper.SaasTenantMapper;
import com.erp.saas.control.service.ControlUtcTime;
import com.erp.saas.control.service.domain.model.RegisterDomainCommand;
import com.erp.saas.control.service.domain.model.RevokeDomainCommand;
import com.erp.saas.control.service.domain.model.TransferDomainCommand;
import com.erp.saas.control.service.domain.model.VerifyDomainCommand;
import com.erp.saas.control.service.domain.impl.SaasDomainServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SaasDomainServiceTest {
    private static final Instant INSTANT = Instant.parse("2026-08-01T12:00:00Z");
    private static final LocalDateTime NOW = LocalDateTime.ofInstant(INSTANT, ZoneOffset.UTC);
    private SaasDomainMapper domainMapper;
    private SaasTenantMapper tenantMapper;
    private SaasDomainService service;

    @BeforeEach
    void setUp() {
        domainMapper = mock(SaasDomainMapper.class);
        tenantMapper = mock(SaasTenantMapper.class);
        service = new SaasDomainServiceImpl(domainMapper, tenantMapper, new SaasDomainHostNormalizer(),
                new ControlUtcTime(Clock.fixed(INSTANT, ZoneOffset.UTC)));
    }

    @Test
    void shouldRegisterNormalizedPendingDomainAndReturnExistingOwnershipIdempotently() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1", TenantLifecycleState.ACTIVE));
        when(domainMapper.findOwnedHostForUpdate("customer.example.com")).thenReturn(null);
        doAnswer(invocation -> {
            SaasDomainEntity row = invocation.getArgument(0);
            row.setDomainId(10L);
            return 1;
        }).when(domainMapper).insert(any());

        var created = service.register(new RegisterDomainCommand("tenant_1", "Customer.Example.com.:443",
                DomainVerificationMethod.PLATFORM_MANUAL, " admin "));
        assertThat(created.domainId()).isEqualTo(10L);
        assertThat(created.host()).isEqualTo("customer.example.com");
        assertThat(created.verificationState()).isEqualTo(DomainVerificationState.PENDING);
        assertThat(created.versionNo()).isZero();

        SaasDomainEntity existing = domain(10L, "tenant_1", "customer.example.com",
                DomainVerificationState.PENDING, 0L);
        when(domainMapper.findOwnedHostForUpdate("customer.example.com")).thenReturn(existing);
        assertThat(service.register(new RegisterDomainCommand("tenant_1", "customer.example.com",
                DomainVerificationMethod.PLATFORM_MANUAL, "admin")).domainId()).isEqualTo(10L);
        verify(domainMapper, org.mockito.Mockito.times(1)).insert(any());
    }

    @Test
    void shouldRejectCrossTenantOwnershipAndTerminalTenant() {
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1", TenantLifecycleState.ACTIVE));
        when(domainMapper.findOwnedHostForUpdate("customer.example.com")).thenReturn(domain(
                10L, "tenant_2", "customer.example.com", DomainVerificationState.VERIFIED, 1L));
        assertCode(SaasDomainException.ErrorCode.OWNERSHIP_CONFLICT,
                () -> service.register(new RegisterDomainCommand("tenant_1", "customer.example.com",
                        DomainVerificationMethod.PLATFORM_MANUAL, "admin")));

        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1", TenantLifecycleState.ARCHIVED));
        assertCode(SaasDomainException.ErrorCode.TENANT_NOT_ELIGIBLE,
                () -> service.register(new RegisterDomainCommand("tenant_1", "other.example.com",
                        DomainVerificationMethod.PLATFORM_MANUAL, "admin")));
    }

    @Test
    void shouldVerifyPendingDomainWithTenantFirstAndVersionCas() {
        SaasDomainEntity hint = domain(10L, "tenant_1", "customer.example.com",
                DomainVerificationState.PENDING, 2L);
        when(domainMapper.selectById(10L)).thenReturn(hint);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1", TenantLifecycleState.ACTIVE));
        when(domainMapper.findByIdForUpdate(10L)).thenReturn(hint);
        when(domainMapper.markVerified(10L, 2L, "admin", NOW)).thenReturn(1);

        var verified = service.verify(new VerifyDomainCommand(10L, 2L, "admin"));
        assertThat(verified.verificationState()).isEqualTo(DomainVerificationState.VERIFIED);
        assertThat(verified.verifiedAt()).isEqualTo(NOW);
        assertThat(verified.versionNo()).isEqualTo(3L);
        InOrder order = inOrder(domainMapper, tenantMapper);
        order.verify(domainMapper).selectById(10L);
        order.verify(tenantMapper).lockByTenantId("tenant_1");
        order.verify(domainMapper).findByIdForUpdate(10L);
        order.verify(domainMapper).markVerified(10L, 2L, "admin", NOW);
    }

    @Test
    void shouldRevokeOnceAndReturnRepeatedRevocationWithoutVersionIncrement() {
        SaasDomainEntity row = domain(10L, "tenant_1", "customer.example.com",
                DomainVerificationState.VERIFIED, 3L);
        when(domainMapper.selectById(10L)).thenReturn(row);
        when(tenantMapper.lockByTenantId("tenant_1")).thenReturn(tenant("tenant_1", TenantLifecycleState.ACTIVE));
        when(domainMapper.findByIdForUpdate(10L)).thenReturn(row);
        when(domainMapper.markRevoked(10L, 3L, "admin", NOW)).thenReturn(1);
        var revoked = service.revoke(new RevokeDomainCommand(10L, 3L, "admin"));
        assertThat(revoked.versionNo()).isEqualTo(4L);

        row.setVerificationState(DomainVerificationState.REVOKED);
        row.setVersionNo(4L);
        row.setRevokedAt(NOW);
        when(domainMapper.selectById(10L)).thenReturn(row);
        when(domainMapper.findByIdForUpdate(10L)).thenReturn(row);
        assertThat(service.revoke(new RevokeDomainCommand(10L, 3L, "admin")).versionNo()).isEqualTo(4L);
        verify(domainMapper, org.mockito.Mockito.times(1)).markRevoked(any(), any(), any(), any());
    }

    @Test
    void shouldTransferByRevokingSourceThenCreatingTargetPendingRow() {
        SaasDomainEntity source = domain(10L, "tenant_z", "customer.example.com",
                DomainVerificationState.VERIFIED, 5L);
        when(domainMapper.selectById(10L)).thenReturn(source);
        when(tenantMapper.lockByTenantId("tenant_a")).thenReturn(tenant("tenant_a", TenantLifecycleState.ACTIVE));
        when(tenantMapper.lockByTenantId("tenant_z")).thenReturn(tenant("tenant_z", TenantLifecycleState.ACTIVE));
        when(domainMapper.findByIdForUpdate(10L)).thenReturn(source);
        when(domainMapper.markRevoked(10L, 5L, "admin", NOW)).thenReturn(1);
        doAnswer(invocation -> {
            SaasDomainEntity target = invocation.getArgument(0);
            target.setDomainId(11L);
            return 1;
        }).when(domainMapper).insert(any());

        var target = service.transfer(new TransferDomainCommand(10L, 5L, "tenant_a", "admin"));

        assertThat(target.domainId()).isEqualTo(11L);
        assertThat(target.tenantId()).isEqualTo("tenant_a");
        assertThat(target.verificationState()).isEqualTo(DomainVerificationState.PENDING);
        InOrder order = inOrder(tenantMapper, domainMapper);
        order.verify(tenantMapper).lockByTenantId("tenant_a");
        order.verify(tenantMapper).lockByTenantId("tenant_z");
        order.verify(domainMapper).findByIdForUpdate(10L);
        order.verify(domainMapper).markRevoked(10L, 5L, "admin", NOW);
        order.verify(domainMapper).insert(any());
    }

    @Test
    void shouldResolveVerifiedEligibleOwnerAndHideEveryMiss() {
        ResolvedTenantDomainRow row = new ResolvedTenantDomainRow();
        row.setDomainId(10L);
        row.setTenantId("tenant_1");
        row.setHost("customer.example.com");
        row.setLifecycleState(TenantLifecycleState.READ_ONLY);
        when(domainMapper.resolveVerified("customer.example.com")).thenReturn(row);
        assertThat(service.resolve("Customer.Example.com:443")).get()
                .extracting("tenantId", "lifecycleState")
                .containsExactly("tenant_1", TenantLifecycleState.READ_ONLY);
        assertThat(service.resolve("https://bad.example.com")).isEmpty();
        when(domainMapper.resolveVerified("missing.example.com")).thenReturn(null);
        assertThat(service.resolve("missing.example.com")).isEmpty();
    }

    @Test
    void shouldMarkMutationsTransactionalAndResolutionReadOnly() throws Exception {
        for (String method : new String[]{"register", "verify", "revoke", "transfer"}) {
            Transactional annotation = java.util.Arrays.stream(SaasDomainServiceImpl.class.getDeclaredMethods())
                    .filter(candidate -> candidate.getName().equals(method)).findFirst().orElseThrow()
                    .getAnnotation(Transactional.class);
            assertThat(annotation).isNotNull();
            assertThat(annotation.rollbackFor()).contains(Exception.class);
        }
        Transactional resolve = SaasDomainServiceImpl.class.getDeclaredMethod("resolve", String.class)
                .getAnnotation(Transactional.class);
        assertThat(resolve.readOnly()).isTrue();
    }

    @Test
    void shouldRejectInvalidCommandInputsWithStableErrors() {
        assertCode(SaasDomainException.ErrorCode.INVALID_INPUT,
                () -> new VerifyDomainCommand(null, 0L, "admin"));
        assertCode(SaasDomainException.ErrorCode.INVALID_INPUT,
                () -> new TransferDomainCommand(1L, 0L, "bad.tenant", "admin"));
        assertCode(SaasDomainException.ErrorCode.INVALID_INPUT,
                () -> new RegisterDomainCommand("tenant_1", "example.com",
                        DomainVerificationMethod.PLATFORM_MANUAL, " "));
    }

    private static SaasTenantEntity tenant(String id, TenantLifecycleState state) {
        SaasTenantEntity tenant = new SaasTenantEntity();
        tenant.setTenantId(id);
        tenant.setLifecycleState(state);
        return tenant;
    }

    private static SaasDomainEntity domain(Long id, String tenantId, String host,
            DomainVerificationState state, Long version) {
        SaasDomainEntity domain = new SaasDomainEntity();
        domain.setDomainId(id);
        domain.setTenantId(tenantId);
        domain.setHost(host);
        domain.setVerificationState(state);
        domain.setVerificationMethod(DomainVerificationMethod.PLATFORM_MANUAL);
        domain.setVersionNo(version);
        return domain;
    }

    private static void assertCode(SaasDomainException.ErrorCode code, Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(SaasDomainException.class)
                .extracting(error -> ((SaasDomainException) error).getErrorCode()).isEqualTo(code);
    }
}
