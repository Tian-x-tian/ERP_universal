# SaaS Domain Resolution Implementation Plan

> Scope: control-plane domain registration, manual verification, revocation/transfer, and internal host resolution. This slice does not perform DNS/HTTP ownership challenges and does not change gateway routing yet.

## Contract and invariants

- A normalized host is the only persistence and lookup key. Normalize by trimming, converting Unicode labels with `IDN.toASCII`, lower-casing, removing one terminal dot, and removing a syntactically valid port.
- Reject schemes, user info, paths, queries, fragments, control characters, wildcards, empty labels, invalid IDN labels, labels longer than 63 bytes, and hosts longer than 253 bytes. Do not guess malformed `host:port` input.
- Domain states are `PENDING -> VERIFIED -> REVOKED`. Verification is a manual platform-admin assertion in this slice.
- A non-revoked host has exactly one owner through the existing `owned_host` generated column and unique index. Transfer explicitly revokes the prior row before creating a new pending row; it is never an in-place tenant reassignment.
- Every mutation validates a non-blank operator, uses an injected UTC `Clock`, locks the relevant tenant/domain aggregate, and updates through `version_no` compare-and-set.
- Resolution returns only a `VERIFIED` domain whose tenant is not `ARCHIVED`, `PURGE_PENDING`, or `PURGED`. `SUSPENDED` and `READ_ONLY` tenants still resolve so downstream policy can return the correct login/read-only response.
- Unknown, pending, revoked, inactive-owner, and invalid hosts share the same public not-found result. Internal administration may return typed conflict/not-found errors but must not expose another tenant's details.

## Files

### 1. Add red contract tests

Create:

- `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasDomainServiceContractTest.java`
- `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasDomainHostNormalizerTest.java`

Cover exact normalization, all rejection categories, idempotent registration for the same tenant/host, cross-tenant ownership conflict, legal and illegal state transitions, stale-version rejection, transfer ordering, and operator/time auditing. Use an injected fixed UTC clock.

### 2. Define immutable commands and views

Create under `com.erp.saas.control.service.domain`:

- `RegisterDomainCommand(tenantId, host, verificationMethod, operator)`
- `VerifyDomainCommand(domainId, expectedVersion, operator)`
- `RevokeDomainCommand(domainId, expectedVersion, operator)`
- `TransferDomainCommand(domainId, expectedVersion, targetTenantId, operator)`
- `SaasDomainView`
- `ResolvedTenantDomain`
- `SaasDomainException` with stable error codes

Do not expose persistence entities from service methods.

### 3. Implement host normalization

Create `SaasDomainHostNormalizer` as a stateless component. Parse bracketed IPv6 deliberately or reject all IP literals consistently; the first release should accept DNS names only. Preserve no Unicode host in persistence. Add collision tests proving equivalent Unicode/case/trailing-dot/port forms produce one normalized host.

### 4. Add explicit locking and CAS mapper methods

Extend:

- `SaasDomainMapper`
- `SaasTenantMapper`

Add XML or annotated SQL for:

- select tenant by `tenant_id FOR UPDATE`
- select non-revoked domain by `owned_host FOR UPDATE`
- select domain by id `FOR UPDATE`
- update verification/revocation fields with `WHERE domain_id = ? AND version_no = ?`
- resolve verified domain joined to tenant lifecycle by normalized host, returning only the allowed owner states

Every tenant-bearing query includes the tenant predicate where applicable. Do not rely on the business tenant interceptor in the control database.

### 5. Implement transactional service

Create `SaasDomainService` and `SaasDomainServiceImpl`.

- `register`: lock tenant anchor, reject terminal tenant states, normalize host, lock current ownership, return existing same-tenant pending/verified row idempotently, otherwise insert pending.
- `verify`: lock row and tenant, require pending, set verified timestamp, clear revoked timestamp, CAS update.
- `revoke`: require pending/verified, set revoked timestamp, CAS update; repeated revoke returns current view without another version increment.
- `transfer`: lock source row and both tenants in stable tenant-id order, revoke source by CAS, then insert a new pending row for the target tenant in the same transaction.
- `resolve`: normalize host and perform one non-locking read; map every public miss to `Optional.empty()`.

Translate duplicate-key races into stable ownership conflicts and optimistic update misses into stale-version errors.

### 6. Focused verification

Run serially from the repository root:

```powershell
mvn -pl erp-modules/erp-saas-control -am "-Dtest=SaasDomainHostNormalizerTest,SaasDomainServiceContractTest" "-Dsurefire.failIfNoSpecifiedTests=false" test
mvn -pl erp-modules/erp-saas-control -am test
git diff --check
```

Add a MySQL-backed integration test only if mapper SQL cannot be proven by the existing isolated MySQL harness. It must retain the loopback, exact database, exact MySQL 8.0.17, and datadir safety guards before any reset.

