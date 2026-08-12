# SaaS Plan Catalog and Override Plan

> **For agentic workers:** REQUIRED SUB-SKILLS: `superpowers:subagent-driven-development` and `superpowers:test-driven-development`. Never connect to `192.168.0.22`.

**Goal:** Implement the transactional plan/feature/quota catalog and deterministic tenant overrides on top of the approved control persistence model.

**Architecture:** Published plan versions are immutable. A draft is edited, then published in one transaction: the previous ACTIVE version is retired with explicit version CAS and the draft becomes ACTIVE; the generated `active_slot` unique index is the final concurrency guard. Effective access is calculated at an injected UTC `Clock`. Override history uses half-open `[effective_from, effective_until)` windows, with application row locks preventing overlap. This slice has no controllers, subscriptions, RBAC intersection, snapshots, provisioning, usage counters, or legacy import.

**Tech Stack:** Java 17, Spring Boot 3.2, MyBatis-Plus, Spring transactions, JUnit 5, Mockito.

---

### Task 1: Freeze service contracts and validation

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/PlanDraftCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/FeatureDefinitionCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/PlanFeatureGrantCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/PlanQuotaCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/FeatureOverrideCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/QuotaOverrideCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/EffectiveTenantEntitlements.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/QuotaEntitlement.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/PublishPlanCommand.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/SaasPlanView.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/model/SaasFeatureView.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/SaasCatalogException.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasPlanCatalogContractTest.java`

- [ ] Commands are exact immutable records: `PlanDraftCommand(String planCode, Integer planVersion, String planName, Integer trialDays, Integer graceDays, String description)`; `FeatureDefinitionCommand(String featureKey, String featureName, FeatureStatus status, String description)`; `PlanFeatureGrantCommand(String featureKey, Boolean granted)`; `PlanQuotaCommand(String quotaKey, Long limitValue, QuotaPeriodType periodType)`; `FeatureOverrideCommand(String tenantId, String featureKey, FeatureOverrideState overrideState, LocalDateTime effectiveFrom, LocalDateTime effectiveUntil, String reason)`; `QuotaOverrideCommand(String tenantId, String quotaKey, Long limitValue, LocalDateTime effectiveFrom, LocalDateTime effectiveUntil, String reason)`; `PublishPlanCommand(Long planId, Long expectedPlanVersion, Long expectedActivePlanId, Long expectedActivePlanVersion)`. Nullable fields are only description/reason, effectiveUntil, limitValue, and the expected-active pair; that pair must be both NULL or both non-NULL.
- [ ] Normalize plan/feature/quota keys by trim only; require regex `[a-z][a-z0-9_.-]{1,127}` for feature keys and `[a-z][a-z0-9_.-]{1,63}` for plan/quota keys. Plan codes and feature keys are never case-folded silently. Tenant IDs are trimmed and require `[A-Za-z0-9_-]{1,20}`. Names are trimmed nonblank and at most 128; description/reason are trimmed and at most 512. `planVersion` is `1..Integer.MAX_VALUE`; trial/grace days are `0..3650`.
- [ ] Quota keys must be one of `SaasQuotaKeys.USER_COUNT`, `STORAGE_BYTES`, `AI_INPUT_TOKENS`, or `AI_OUTPUT_TOKENS`. `USER_COUNT` and `STORAGE_BYTES` require `QuotaPeriodType.CURRENT`; both AI keys require `MONTHLY`; wrong combinations are `INVALID_INPUT`. Unknown keys throw `UNKNOWN_QUOTA_KEY`; a known quota missing from a plan means limit `0`, while stored NULL means unlimited.
- [ ] Exact views are: `SaasPlanView(Long planId, String planCode, Integer planVersion, String planName, PlanStatus status, Integer trialDays, Integer graceDays, String description, Long versionNo)` and `SaasFeatureView(Long featureId, String featureKey, String featureName, FeatureStatus status, String description, Long versionNo)`; only description is nullable.
- [ ] Avoid null map values: `QuotaEntitlement(boolean unlimited, long limitValue)` requires `limitValue >= 0`, uses `unlimited=true, limitValue=0` as the canonical unlimited representation, and otherwise carries the finite limit. `EffectiveTenantEntitlements(String tenantId, Long subscriptionId, Long planId, SortedMap<String, Boolean> features, SortedMap<String, QuotaEntitlement> quotas)` permits nullable subscriptionId/planId only when both are absent. Its canonical constructor defensively copies with `Collections.unmodifiableSortedMap(new TreeMap<>(...))`. `isFeatureEnabled(String)` and `quotaLimit(String)` return Boolean/QuotaEntitlement and throw the corresponding UNKNOWN error for absent keys; callers treat either exception as deny. No mutable entity escapes the service.
- [ ] Freeze error codes: `NOT_FOUND`, `DUPLICATE`, `INVALID_INPUT`, `IMMUTABLE_PUBLISHED_PLAN`, `VERSION_CONFLICT`, `OVERLAPPING_OVERRIDE`, `UNKNOWN_FEATURE_KEY`, `UNKNOWN_QUOTA_KEY`.

### Task 2: Add exact mapper operations with bound parameters

**Files:**
- Modify: plan/feature/plan-feature/plan-quota/feature-override/quota-override mappers created by the persistence slice
- Modify: tenant and subscription mappers created by the persistence slice
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/mapper/SaasPlanCatalogMapperSqlTest.java`

- [ ] Add `FOR UPDATE` reads for the tenant anchor by tenant ID, the complete plan family by plan code ordered by plan ID, feature by key, and all override windows by tenant+key ordered by start. Add a separate non-locking current-subscription query for Task 5 read-only entitlement calculation. The tenant anchor is always locked before window rows, even when no window exists. Every logical value uses `#{}` binding, never `${}`.
- [ ] Add explicit CAS updates: edit/retire/publish/aggregate-bump use `WHERE id = #{id} AND version_no = #{expectedVersion}`, increment `version_no = version_no + 1`, and set bound `update_by/update_time`; zero affected rows maps to `VERSION_CONFLICT`.
- [ ] Add replacement methods for draft plan-feature/plan-quota rows and effective reads joining ACTIVE feature definitions. Only DRAFT plans are mutable; service checks state again under the same transaction lock.
- [ ] SQL inspection tests freeze tenant-anchor then window lock order, plan-family lock, `FOR UPDATE`, parameter binding, CAS/audit predicates, generated-column exclusion, and absence of `TenantContextHolder`.

### Task 3: Implement draft catalog and atomic publication

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/SaasPlanCatalogService.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/impl/SaasPlanCatalogServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/ControlUtcTime.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasPlanCatalogServiceTest.java`

- [ ] Service signatures are exact: `SaasPlanView createDraft(PlanDraftCommand, String operator)`; `SaasPlanView updateDraft(Long planId, Long expectedVersion, PlanDraftCommand, String operator)`; `SaasFeatureView defineFeature(FeatureDefinitionCommand, String operator)`; `SaasFeatureView updateFeature(Long featureId, Long expectedVersion, FeatureDefinitionCommand, String operator)`; `SaasPlanView replaceDraftFeatures(Long planId, Long expectedVersion, List<PlanFeatureGrantCommand>, String operator)`; `SaasPlanView replaceDraftQuotas(Long planId, Long expectedVersion, List<PlanQuotaCommand>, String operator)`; `SaasPlanView publish(PublishPlanCommand, String operator)`.
- [ ] Every public write method is `@Transactional(rollbackFor = Exception.class)`. Inject UTC `Clock`; convert audit instants with `LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC)`. Operator is trimmed, required, at most 64 characters, explicit service input sourced later from authenticated platform context; never read ambient tenant context.
- [ ] Implement all signatures above. Validate uniqueness inside each command list before writes. Empty feature/quota lists are valid and mean no grants/zero known quotas. Each replace locks the DRAFT, checks expected version, CAS-bumps the plan aggregate including audit fields, replaces children, and returns the incremented version atomically.
- [ ] `publish` locks the entire same-code plan family in deterministic ID order. It compares actual ACTIVE ID/version to the nullable expected-active pair before any update; mismatch is `VERSION_CONFLICT`. It then retires expected ACTIVE using CAS and promotes the expected DRAFT using CAS. Two different drafts cannot both publish from the same expected ACTIVE generation; the waiter fails after re-read. Re-publishing the same already ACTIVE id succeeds only when both its current version and the expected-active pair identify that same current row; otherwise it conflicts. Publishing RETIRED fails.
- [ ] Published/retired plan metadata, features, and quotas cannot be changed. Feature definition status/name may change only by its own feature version CAS; setting a feature INACTIVE makes it ineffective without rewriting historical plans.
- [ ] Tests cover duplicate version/key rejection, 0/14/3650 day boundaries, immutable published data, old-active retirement, publish idempotence, CAS conflict rollback, generated unique conflict mapping, and fixed UTC audit timestamps.

### Task 4: Implement non-overlapping override history

**Files:**
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/SaasTenantEntitlementService.java`
- Create: `erp-modules/erp-saas-control/src/main/java/com/erp/saas/control/service/impl/SaasTenantEntitlementServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasTenantOverrideServiceTest.java`

- [ ] Task 4 tenant service signatures are exactly the four writes: `Long addFeatureOverride(FeatureOverrideCommand, String operator)`; `Long addQuotaOverride(QuotaOverrideCommand, String operator)`; `void deleteFutureFeatureOverride(Long overrideId, Long expectedVersion, String operator)`; `void deleteFutureQuotaOverride(Long overrideId, Long expectedVersion, String operator)`. All four are `@Transactional(rollbackFor = Exception.class)`; Task 5 adds the read method only after its implementation is created.
- [ ] `addFeatureOverride` and `addQuotaOverride` capture one `now`, require `effectiveFrom >= now` (no historical backfill), require an existing tenant and feature/known quota key, lock the tenant anchor first and then all windows for that tenant+key, and reject overlap using `newStart < existingEnd && existingStart < newEnd`, treating NULL end as infinity. Adjacent windows are valid. Fixed tenant-anchor-first ordering serializes the empty-window case.
- [ ] Overrides are append-only in this slice; correcting a bad future window uses the explicit delete methods above, tenant anchor lock, row lock, and expected version; only windows whose `effective_from > now` may be deleted. Active/past history cannot be rewritten. Audit fields are written on insert; delete is physical because only never-effective future rows are eligible.
- [ ] Quota limit is signed `Long`, NULL unlimited, zero no allocation, negative invalid. Feature override is exactly GRANT or DENY. Reason is optional and trimmed, maximum 512 characters.
- [ ] Tests cover past-start rejection, start exactly at now, open-ended collision, adjacency, exact boundary, two empty-window concurrent inserts serialized by the same tenant anchor, fixed lock order, future deletion CAS, and rejection of active/past edits.

### Task 5: Calculate deterministic effective entitlements

**Files:**
- Extend: `SaasTenantEntitlementService.java`
- Extend: `SaasTenantEntitlementServiceImpl.java`
- Create: `erp-modules/erp-saas-control/src/test/java/com/erp/saas/control/service/SaasEffectiveEntitlementsTest.java`

- [ ] Extend the interface and implementation together with `EffectiveTenantEntitlements effectiveEntitlements(String tenantId)`.
- [ ] `effectiveEntitlements` is `@Transactional(readOnly = true)`. Resolve the tenant's current TRIAL/ACTIVE/GRACE subscription and its plan. Absence of a current subscription returns all features false and all four quotas zero. Subscription lifecycle semantics remain a later slice.
- [ ] Base feature is true only when a plan-feature row grants it and the feature definition is ACTIVE. A currently effective DENY/GRANT override wins over the base; inactive/unknown feature always remains false even if an override says GRANT.
- [ ] Base quota is plan value, missing known key is zero, NULL is unlimited. One currently effective tenant quota override wins. More than one effective row is treated as control-data corruption and fails closed with `DUPLICATE`.
- [ ] Window evaluation uses the same single injected `now`, with `start <= now && (end IS NULL || now < end)`. Return maps are deterministically sorted and include every known feature definition plus all four known quota keys.
- [ ] Tests cover plan grant/deny, inactive feature, tenant grant/deny, missing/zero/unlimited quota, exact `[start,end)` boundaries, no subscription, duplicate effective data, and clock advancement.

### Task 6: Verify and commit this slice

- [ ] Run focused mapper/contract/service tests, then `mvn -pl erp-modules/erp-saas-control -am test` serially.
- [ ] Run `git diff --check`, inspect status, and commit only code/tests for this slice as `feat: implement SaaS plan catalog`. No schema change or MySQL DDL is expected in this slice.
