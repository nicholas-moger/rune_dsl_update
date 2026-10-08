---
id: U005
subject: SymbolId opaque-handle cross-references
since_fork_version: "0.1.0"
since_pr_at_spec: 35
since_pr_actual: 35
since_sha: null
status: Available
migration_depth: Optional
removal_target: null
future_deprecation_risk: medium
related_d_entries: ["D8"]
related_audit_hooks: ["H7"]
related_parity_constraints: ["#14"]
doc_path: U005-symbolid-cross-references.md
---

# U005 — SymbolId opaque-handle cross-references

## Concept

Six cross-reference fields on AST nodes hold a
`SymbolId(namespace, localName, generation)` opaque handle instead of a
direct `RNode` pointer. The public accessor on each migrated node
preserves its existing `Optional<X>` signature; resolution happens lazily
through `RWorkspace.resolve(id, expectedClass)`. SymbolIds carry a
generation token that converts silent stale-pointer invalidation into a
loud `StaleSymbolIdException` at resolve time.

## Why

Audit hook H7 — decouple AST nodes from direct workspace references so
the workspace can be rebuilt without silently invalidating
cross-reference-bearing nodes. The generation token converts silent
invalidation into a loud `StaleSymbolIdException` at resolve time. Lays
the foundation for incremental compilation (P1.4.4: file-scope rebuild
without full workspace relink) and reduces retained-memory footprint
when consumers cache AST snapshots across builds.

## Structural changes

### Migrated cross-reference sites (6)

| # | Site | Field | Existing accessor (signature unchanged) | New SymbolId accessor |
|---|---|---|---|---|
| 1 | `RDataType` | `superTypeId` | `Optional<RDataType> superType()` | `Optional<SymbolId> superTypeId()` |
| 2 | `RDataType` | `choiceSuperTypeId` | `Optional<RChoice> choiceSuperType()` | `Optional<SymbolId> choiceSuperTypeId()` |
| 3 | `RFunction` | `superFunctionId` | `Optional<RFunction> superFunction()` | `Optional<SymbolId> superFunctionId()` |
| 4 | `REnumeration` | `superTypeId` | `Optional<REnumeration> superType()` | `Optional<SymbolId> superTypeId()` |
| 5 | `RTypeCall` | `referencedTypeId` | `Optional<RNode> referencedType()` | `Optional<SymbolId> referencedTypeId()` |
| 6 | `RQualifiableConfig` | `rootTypeId` | `Optional<RDataType> rootType()` | `Optional<SymbolId> rootTypeId()` |

### Foundation classes added

- **`SymbolId`** (`com.regnosys.rosetta.symbols.SymbolId`) — immutable
  value record `(String namespace, String localName, long generation)`.
  Instances are safe to serialize, cache, and compare with
  `equals`/`hashCode`. The generation token must match the issuing
  workspace's current generation for `resolve()` to succeed.
- **`StaleSymbolIdException`**
  (`com.regnosys.rosetta.symbols.StaleSymbolIdException`) — unchecked;
  thrown by `RWorkspace.resolve(id, expectedClass)` when
  `id.generation() != workspace.generation()`. Message identifies the
  SymbolId and both generation values.
- **`SymbolResolver`** (`com.regnosys.rosetta.symbols.SymbolResolver`)
  — interface extracted to allow `RNode`'s `WeakReference` back-pointer
  to be typed without a hard dependency on the concrete `RWorkspace`
  class. Implemented by `RWorkspace` in production; tests use
  `TestSymbolResolver`.

### `RWorkspace` additions

- `long generation()` — workspace's monotonic generation token, drawn
  from a process-wide `AtomicLong` sequence. Each call to
  `RWorkspace.build(...)` increments the counter, making every new
  workspace uniquely identifiable.
- `<T extends RNode> T resolve(SymbolId id, Class<T> expected)` — lazy
  resolution. Looks up `id.namespace()` directly via `namespaces.get(...)`,
  with a special-case scan for `BUILTIN_NAMESPACE`, then delegates to
  namespace lookup for `id.localName()`. Throws `StaleSymbolIdException`
  on generation mismatch; throws `IllegalArgumentException` on type
  mismatch (per the `SymbolResolver` contract).
- `SymbolId` emission is handled during linking in
  `GlobalResolutionPass`, which computes the opaque handle for each
  migrated cross-reference field at emit time. The
  `symbolIdOf(target, localName)` helper is private to
  `GlobalResolutionPass` (linear scan over `namespaces` to identify the
  namespace containing the resolved target) and not exposed on
  `RWorkspace`.

### `RNode` additions

- `void attachToWorkspace(SymbolResolver resolver)` — called by
  `RWorkspace.build()` in two stages: after pass 4, reachable nodes are
  attached to the interim `SymbolResolver` used during linking (so
  passes 5–7 and any lazy `superType()` / `referencedType()` resolution
  they trigger have a working back-pointer); before freeze, they are
  re-attached to the final `RWorkspace` instance. Tests may inject a
  `TestSymbolResolver` directly.
- `protected SymbolResolver workspace()` — accessor used by each lazy
  cross-ref accessor. Throws `IllegalStateException`
  ("cross-reference resolution unavailable") when the
  `WeakReference<SymbolResolver>` has been collected.

The back-pointer is `WeakReference<SymbolResolver>` — nodes do not
retain a strong reference to whichever resolver is currently attached.
During build that may be the interim linker resolver; after
re-attachment it is the final `RWorkspace`. This preserves lazy
resolution without pinning the resolver/workspace in memory.

### Removed setters (binary break, fork-internal-only)

The 6 direct `setResolvedX(RNode resolved)` setters are **removed**:

- `RDataType.setResolvedSuperType(RDataType)`
- `RDataType.setResolvedChoiceSuperType(RChoice)`
- `RFunction.setResolvedSuperFunction(RFunction)`
- `REnumeration.setResolvedSuperType(REnumeration)`
- `RTypeCall.setResolvedType(RNode)`
- `RQualifiableConfig.setResolvedRootType(RDataType)`

In production, only `GlobalResolutionPass` calls these setters.

## BC Story

- **Read-side:** existing `Optional<X>` accessor signatures are
  unchanged. No consumer reads break.
- **Write-side:** the 6 removed setters are binary-incompatible for any
  external consumer calling them directly. Verified fork-internal-only
  at spec time: all callers are inside `GlobalResolutionPass` and test
  fixtures (migrated in PR P1.4.1b Tasks 4.1–4.6).
- **Test sites:** ~24 test sites across `rune-parser` and
  `rune-java-generator` were updated to use
  `TestSymbolResolver.wireSuperType` or equivalent.
- **T9 cross-refs deferred to P1.4.x.** The 9 remaining direct-pointer
  cross-ref sites in `GlobalResolutionPass` (annotation refs, enum
  value refs, conversion target enum, rule refs, segment refs, external
  class/enum, map test func, external synonym/rule super-source)
  retain their existing `setResolvedX` setters and are not
  binary-affected by this PR.
- **Spec §12 risk #1 — linker freeze ordering.** Two complementary tests
  guard this surface: `WorkspaceAttachTest` covers the unattached-node
  case (cross-ref accessors called before `attachToWorkspace` throw
  `IllegalStateException`); `BindFreezeResolveLifecycleTest` covers the
  bind-freeze-resolve sequence as `RWorkspace.build(files)` drives it
  (lifecycle-stage ordering + freeze interactions).
- **Spec §12 risk #2 — SymbolId performance.** Addressed by the
  `symbolIdOf` linear-scan design combined with the D5 wall-clock budget
  gate. Escalation path documented under Coverage below.

## How to use

### Reading cross-references (signature-preserving)

```java
// Before AND after migration — exact same call shape
Optional<RDataType> parent = childType.superType();
```

The accessor now resolves lazily via
`workspace().resolve(superTypeId, RDataType.class)` instead of returning
a stored pointer. The returned value is identical as long as the
workspace is alive.

### Setting cross-references (production)

```java
// Before — REMOVED
data.setResolvedSuperType(parent);

// After
data.setSuperTypeId(SymbolId.of("com.example", "Parent", workspace.generation()));
```

### Test fixtures (preferred shape)

```java
// One-call wiring
TestSymbolResolver resolver = new TestSymbolResolver();
resolver.wireSuperType(child, "com.example", "Parent", parent, child::setSuperTypeId);
// child.superType() now resolves via TestSymbolResolver

// Step-by-step form (when a test needs finer control)
TestSymbolResolver r2 = new TestSymbolResolver();
r2.bind("com.example", "Parent", parent);
parent.attachToWorkspace(r2);
child.attachToWorkspace(r2);
child.setSuperTypeId(r2.idFor("com.example", "Parent"));
```

`Reference.reachabilityFence(resolver)` should be called at the end of
each test method that reads cross-refs to match the production pattern
and suppress GC-race false failures.

### Lifecycle during `RWorkspace.build()`

`RWorkspace.build(files)` runs 7 passes in sequence:

| Pass | Name | Cross-ref behaviour |
|---|---|---|
| 1 | SymbolRegistration | namespaces populated; no cross-refs yet |
| 2 | ImportResolution | import graphs resolved |
| 3 | DerivedState | structural children set |
| 4 | **GlobalResolution** | `GlobalResolutionPass` emits `SymbolId` for all 6 sites; nodes are then attached to the interim resolver |
| 5 | LexicalResolution | lexical scopes resolved |
| 6 | TypeInference | type inference |
| 7 | Validation | validators run before `freeze()` |

After pass 4, nodes are attached in an interim state to the in-build
`SymbolResolver`; after passes 5–7 complete, reachable nodes are
re-attached to the final `RWorkspace` instance, then `freeze()` runs.
The `Reference.reachabilityFence(interimResolver)` call between the
post-pass-7 phase and the re-attach ensures the interim resolver is not
collected by the GC between its `WeakReference` store and the re-attach
point.

## Migration

### Workspace lifetime

`SymbolId` objects are safe to cache within a single `RWorkspace`
lifetime. Across rebuilds (different `RWorkspace` instances), the
generation token differs and `resolve()` throws
`StaleSymbolIdException`. Consumers holding AST snapshots across
rebuilds must re-acquire SymbolIds from the new workspace.

### Edge cases handled

- **Qualified type calls (R7-1, commit `c2393ff`):** `tc.typeName()` in
  source can be a fully-qualified form like `"com.foo.Bar"` (in addition
  to unqualified `"Bar"`). Pre-fix, `symbolIdOf` was passed the source
  text directly — `namespace.lookup("com.foo.Bar")` returned empty (only
  `"Bar"` is registered locally), so `symbolIdOf` fell through to
  `BUILTIN_NAMESPACE` with `"com.foo.Bar"` as the (wrong) localName.
  Fixed by extracting the local segment via `lastIndexOf('.')` before
  passing to `symbolIdOf`. Diagnostic-path callers keep the full source
  form for their error messages.
- **Single-type aliased imports (R11-3, commit `0847e96`):** rune
  grammar (`importDecl: IMPORT qualifiedNameWithWildcard (AS validID)?`)
  supports both wildcard and single-type aliased imports — e.g.,
  `import com.foo.Bar as MyType` then `MyType` in a type call. The R7-1
  `lastIndexOf('.')` heuristic returns `MyType` (no dot to strip), but
  `MyType` isn't registered in any namespace (only `Bar` is). Fixed by
  `typeNodeName(RRootElement, String)` helper, which dispatches on the
  6 type-node subtypes and returns the resolved target's actual `name()`
  instead of deriving from source text. Falls back to source-text
  extraction only for non-type-like targets.
- **Namespace whitespace validation (R5-1):** `SymbolId` constructor
  rejects namespace strings that are non-empty and entirely whitespace
  (`!namespace.isEmpty() && namespace.isBlank()`). The empty string `""`
  (root namespace) and the builtin sentinel `"<builtin>"` remain valid.
  `IllegalArgumentException` with the offending string in the message.
- **GC-race fence sweep (R5-2):** 25 test methods across the suite
  called cross-ref accessors without a trailing
  `Reference.reachabilityFence`. Under aggressive GC (parallel/G1), the
  workspace could be collected between the `WeakReference` store and
  the accessor call, producing spurious `IllegalStateException` in CI.
  Fixed by adding `Reference.reachabilityFence(resolver)` at the end of
  each affected test method.

## Test coverage

All new tests live in `rune-parser`. Targeted suite: **71/71 parser
tests** + **60/60 generator tests** pass at landing.

| Test class | What it covers |
|---|---|
| `SymbolIdTest` | Constructor, `equals`, `hashCode`, null-namespace/localName rejection, namespace whitespace rejection |
| `StaleSymbolIdExceptionTest` | Message format; `id.generation()` vs `workspace.generation()` values in message |
| `TestSymbolResolverTest` | `bind`, `idFor`, `wireSuperType`, `resolve`, type-mismatch, generation-mismatch, unknown-id |
| `RWorkspaceResolveTest` | `resolve` happy path + `StaleSymbolIdException` on generation mismatch (type-mismatch IAE coverage is in `RWorkspaceResolveEdgeCasesTest`, see below) |
| `RDataTypeSuperTypeIdTest` | Site 1 — `setSuperTypeId` / `superTypeId()` / `superType()` lazy resolution |
| `RDataTypeChoiceSuperTypeIdTest` | Site 2 — `setChoiceSuperTypeId` / `choiceSuperTypeId()` / `choiceSuperType()` lazy resolution |
| `RFunctionSuperFunctionIdTest` | Site 3 — `setSuperFunctionId` / `superFunctionId()` / `superFunction()` lazy resolution |
| `REnumerationSuperTypeIdTest` | Site 4 — `setSuperTypeId` (Enumeration) / `superTypeId()` / `superType()` lazy resolution |
| `RTypeCallReferencedTypeIdTest` | Site 5 — `setReferencedTypeId` / `referencedTypeId()` / `referencedType()` lazy resolution |
| `RQualifiableConfigRootTypeIdTest` | Site 6 — `setRootTypeId` / `rootTypeId()` / `rootType()` lazy resolution |
| `BindFreezeResolveLifecycleTest` | Sentinel: bind-freeze-resolve sequence as `RWorkspace.build(files)` drives it; lifecycle-stage ordering + freeze interactions |
| `SymbolIdPropertyTest` | Jqwik property-based: `equals`/`hashCode` contract, round-trip identity, generation uniqueness |
| `RWorkspaceResolveEdgeCasesTest` | Type mismatch (IAE message contains both type names); unknown local name in known namespace returns null; namespace + local-name case sensitivity; null `expected` class throws NPE; SF8 `BUILTIN_NAMESPACE` round-trip via direct setter |
| `WorkspaceAttachTest` | `attachToWorkspace` happy path; "never attached" vs "GC'd" distinction in `IllegalStateException` message; re-attach replaces prior `WeakReference` |
| `WorkspaceGCResolveTest` | Best-effort GC test (skip-if-no-GC via `Assumptions.assumeTrue`) verifying `IllegalStateException` when the attached `SymbolResolver` is collected before lazy resolve |

## Coverage

Migration spans 6 of 15 cross-reference sites identified in the audit.
The remaining 9 (T9 deferred — annotation refs, enum value refs,
conversion target enum, rule refs, segment refs, external class/enum,
map test func, external synonym/rule super-source) retain their
direct-pointer setters and are not binary-affected by this PR; P1.4.x
follow-on completes the sweep.

### Performance escalation path

`symbolIdOf(target, localName)` is O(N\_namespaces) per cross-ref site,
doing a linear scan over the `namespaces` map (iteration order is
unspecified — `HashMap.entrySet()` order). For the 13-cell corpus
matrix (CDM × DRR × ISO-20022 × rune-fpml across versions), the D5
wall-clock budget gate is green at landing.

If pass-4 timing regresses more than 10% on the 13-cell corpus, the
documented escalation path is to build a `Map<RRootElement, String>`
reverse-index once during pass 1 and thread it into
`GlobalResolutionPass`. This converts the O(N) scan to O(1) amortised.
The design is documented here rather than implemented speculatively,
per the D5 gate mandate.

## Cross-references

- **Audit doc §3 hook H7** — the development audit "p1-parser-architectural-audit".
- **Plan** — the development plan "2026-04-26-p1.4.1b-symbolid-cross-refs".
- **Spec §5** — `docs/superpowers/specs/2026-04-26-p1.4.1-internal-hardening-design.md`
  (local).
- **`incremental-compilation.md` §5.3** (skill reference, local).
- **`symbol-table.md` §8.7** (skill reference, local).
- **U002** — Frozen AST after linker — sibling P1.4.1 surface; freeze
  interaction defines the lifecycle stages used by `attachToWorkspace`.
- **U003** — RNode metadata slot — sibling P1.4.1 surface.
- **`docs/features/INDEX.md`** — catalogue card for U005.
- **`docs/bc-verification.md`** § Per-feature locks — BC layer pairing.
- **PR-1.4.1b Copilot review evidence** —
  `docs/reviews/p1.4.1b-copilot-round-N-review.md` (rounds 1-14) +
  consolidated summary at `docs/reviews/p1.4.1b-final-summary.md`.
