---
id: U015
subject: Top-level `choice` POJO codegen — parallel ChoiceObjectGenerator + resolver hardening
since_fork_version: "0.1.0"
since_pr_at_spec: 68
since_pr_actual: 68
since_sha: 76ea246bf205e8c3e73bb351deadb2730823c20a
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D35", "D36", "D37"]
related_audit_hooks: []
related_parity_constraints: []
doc_path: U015-choice-pojo-codegen.md
---

# U015 — Top-level `choice` POJO codegen + resolver hardening (Cluster F final retirement)

## Concept

This manifest captures the four invariants locked at P2.1.3c that retire the
sole remaining `CODEGEN_DRIFT` cluster (Cluster F) in the D11 byte-identity
parity gate. The headline change is generator-side: top-level `choice X:`
declarations now emit a POJO interface byte-equivalent to upstream Xtend
codegen, via a new `ChoiceObjectGenerator` running alongside the existing
`ModelObjectGenerator`. The three secondary invariants harden
`GeneratorModel.resolveTypeCall` against three distinct sub-causes that all
manifested as `Object` (or `List<Object>`) emission where the golden output
was a concrete type or `List<? extends Type>`.

P2.1.3 (D35 — `TypeAliasSolver.evaluateAliasBody`) and P2.1.3b (D36 — α Locus 1
Case B bypass + β2 classification override) retired 152 of 184 Cluster F
entries via two independent fixes; the remaining 32 entries plus the
BasketConstituent residual that surfaced mid-PR-#68 are retired here. The
P2.1.3c re-audit (the development audit "cluster-f-final-re-audit") further
revealed that 4 entries previously classified as "5th sub-cause" had already
been retired upstream by D35/D36/T3 changes and only required waiver-removal.

The final cumulative retirement is **191 of 191 = 100%**: Cluster F is fully
retired and the `CODEGEN_DRIFT` taxonomy bucket is empty.

## Why

The 4-layer parity gate's strict-equality Layer 4 (D11 byte-identity) is the
canonical measure of "Java generator parity" in this fork. Until P2.1.3c,
Cluster F was the sole `CODEGEN_DRIFT` cluster — distinguishing real
fork-vs-upstream generator drift from the eight `VERSION_DELTA` /
`PERMANENT` / `TRANSITIVE_CDM_GAP` / `M7B_PAUSED` clusters that classify as
inherent (not codegen-fixable) drift. Closing it shrinks the "code we have
to fix" portion of the waiver to zero across the 13-cell × 4-element-kind
matrix.

Five sub-causes mapped to the four invariants:

- **β1 — top-level `choice X:` POJO codegen.** `ModelObjectGenerator` was
  hard-coded to `RDataType`, so `choice` declarations silently produced no
  POJO interface (23 missing-output waiver entries across three CDM cells).
  The parallel `ChoiceObjectGenerator` closes this.
- **Meta-annotation propagation on choice options (β1 cont).** Per-option
  `[metadata location/address/scheme/id/reference]` annotations were lost
  during the initial `ChoiceObjectGenerator` wiring; surfaced post-PR-open
  during ground-truth byte-match validation. Propagation invariant locked
  at commit `d5f00a8`.
- **4th sub-cause — `List<UserDataType>` attributes.** Workspace-loaded
  user-defined types failed both the standard `astNodeToRType` lookup AND
  the `TYPE_ALIAS_TO_BUILTIN` map fallback, falling through to
  `RMissingType.INSTANCE` → `Object`. Workspace-search fallback in
  `GeneratorModel.resolveTypeCall` closes this.
- **Latent sub-cause — `List<TypeAlias>` attributes (drr only).** When the
  list element resolved to an `RTypeAlias` whose body called
  `evaluateAliasBody` and got back `RMissingType` (the R11 F49 invariant —
  pattern/minLength/maxLength constraint surface unsupported), the
  `resolveTypeCall` Case A path emitted `RMissingType` rather than the
  underlying built-in. Recovery via `BUILTINS.lookup(...)` at the codegen
  call site closes this.
- **BasketConstituent — `extendsChoice` builder param naming.** The
  builder `_index` vs `index` discriminator was a boolean
  `choiceSuperType().isPresent()`; the correct upstream rule is
  scope-collision-based (escape-prefix with `_` when `index` is already a
  property in the inherited-property scope). Single residual; refactored
  at commit `912efed`.

## Structural changes

**New generator class:**

- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/object/ChoiceObjectGenerator.java`
  — extends `JavaClassGenerator<RChoice, RJavaPojoInterface>`. Shares
  `ModelObjectGenerator` as a delegate (Objects.requireNonNull-guarded)
  for the common POJO-shape template invocations. Wired at every
  `JavaCodeGenerator` constructor caller (commit `dc25c49`).

**Generator-model resolver extensions:**

- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/GeneratorModel.java`
  `resolveTypeCall`:
  - Case A `RTypeAlias` branch — new `BUILTINS.lookup(...)` fallback when
    `evaluateAliasBody` returns `RMissingType` (commit `5d99eac`).
  - New workspace-search fallback at Locus 1 + Locus 2 covering
    `RDataType` / `REnumeration` / `RChoice` matches in
    `RModel.files()` (commit `a74ebf8`).

**Type-system extensions:**

- `rune-parser/src/main/java/com/regnosys/rosetta/types/RJavaPojoInterface.java`
  — `initializeChoiceProperties()` extension propagating per-option meta
  annotations (commit `d5f00a8`).
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/object/MetaFieldGenerator.java`
  — `collectFromChoiceOption()` integration (commit `d5f00a8`).
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/object/ModelObjectGenerator.java`
  L869 — `_index` discriminator refactored from
  `type.choiceSuperType().isPresent()` to a precise
  scope-collision predicate (commit `912efed`).

**Waiver shrinkage:**

- `rune-java-generator/src/test/resources/d11-known-divergent.txt` — Cluster
  F `CODEGEN_DRIFT` section emptied; section header retained as a sentinel
  marker per the D34 5-category taxonomy convention (commits `dc25c49` +
  `d5f00a8` + `a74ebf8` + `5d99eac` + `912efed`).

## BC Story

**Layer 1 — Source-level.** No grammar change; existing `choice X:`
declarations parse identically. Trivially preserved by
`CorpusParseTest` + `BackCompatCorpusOrthogonalityTest`.

**Layer 2 — Generated-Java byte-identity.** D11 strict-equality with
`KNOWN_DIVERGENT` is the gate this manifest is built around. Post-fix, the
13-cell × 4-element-kind D11 matrix reports zero unwaived Cluster F drift
across the 14-cell run (56 tests / 0 failures / 4 skipped per `cellGoldensExist`
gate). See the development audit "cluster-f-fix-evidence" § 8 for per-cell
pre/post counts.

**Layer 3 — Public API (japicmp).** The new public types are additive:

- `ChoiceObjectGenerator` — new generator class in
  `com.regnosys.rosetta.generator.java.object` (in Layer 3.2 scope — output-side
  jar). japicmp Layer 3 reports it as ADDED entries; ADDED is non-breaking
  under the D5 budget; REPORT-ONLY at P2.1.1-P2.1.5 (flips to ENFORCING at
  P2.1.6).
- `RJavaPojoInterface.initializeChoiceProperties()` — internal helper;
  Layer 3.2 scope. ADDED method only.
- `GeneratorModel.resolveTypeCall` behaviour changes are internal to the
  method body; signature unchanged.
- `MetaFieldGenerator.collectFromChoiceOption(...)` — new method; ADDED.

**Layer 4 — D11 byte-identity.** This is the layer whose drift this PR
retires. Post-P2.1.3c, the `CODEGEN_DRIFT` waiver section contains zero
entries; Cluster F is fully retired.

**Layer 5 — Consumer smoke.** Out of scope; the smoke layer locks parser
entry-point shape only, not generated-Java output.

**Forward commitment.** The `ChoiceObjectGenerator` class signature joins
the permanent public surface contract per D5. Future POJO-shape evolution
must propagate through both `ModelObjectGenerator` and
`ChoiceObjectGenerator` symmetrically (or the delegate refactor pattern
extended to a third generator) — locked structurally by both classes
sharing the `ModelObjectGenerator delegate` field.

## How to use

No consumer-visible API changes. Generated-Java output is byte-equivalent
to upstream Xtend codegen for the affected attribute / declaration shapes:

| Before P2.1.3c | After P2.1.3c |
|---|---|
| `choice Asset:` → no POJO emitted | `choice Asset:` → POJO `interface Asset extends ...` byte-match upstream |
| `attr List<UserType>` → `List<Object> getAttr()` | `List<? extends UserType> getAttr()` byte-match upstream |
| `attr List<StringAlias>` (with pattern constraint) → `List<Object>` | `List<? extends String> ...` byte-match upstream |
| `BasketConstituent extends Product` builder `int index` param | builder `int _index` param byte-match upstream |

Downstream consumers (CDM / DRR / rune-fpml corpora) see the fix
automatically the next time they consume a fork-built `rune-java-generator`
JAR.

## Migration

None. The change is backward-compatible — the new generator class is purely
additive, the resolver extensions only fire on cases that previously
emitted `Object` / `RMissingType` (i.e. the new behaviour is strictly more
correct, not different).

## Test coverage

**New unit tests:**

- `rune-java-generator/src/test/java/com/regnosys/rosetta/generator/java/object/ChoiceObjectGeneratorTest.java`
  — 5 byte-match tests covering choice POJO emission across cdm/5.35.0,
  cdm/6.18.0, cdm/7.0.0-dev.101 (commit `dc25c49`; refactored at `5f43c05`
  per R1 fix bundle).

**Extended unit tests:**

- `rune-java-generator/src/test/java/com/regnosys/rosetta/generator/java/GeneratorModelTest.java`
  — 2 new tests covering Invariants 2 + 3:
  - `resolveTypeCall_list_of_data_type_attribute_yields_RDataTypeRef_not_RMissingType`
    (commit `a74ebf8`).
  - `resolveTypeCall_list_of_type_alias_attribute_yields_RAliasType_element`
    (commit `5d99eac`). Preserves the R11 F49 invariant (`evaluateAliasBody`
    returning `RMissingType` for unsupported string constraints) by
    recovering only at the codegen call site.
- `rune-java-generator/src/test/java/com/regnosys/rosetta/generator/java/object/ModelObjectGeneratorTest.java`
  — 2 new tests covering Invariant 4 (`hasIndexPropertyInScope` predicate
  + builder `_index` emission for BasketConstituent shape; commit `912efed`).

**External gates:**

- D11 corpus regression — `D11CorpusRegressionTest` 14-cell × 4-kind matrix
  reports 0 unwaived Cluster F drift post-P2.1.3c.
- japicmp Layer 3 — REPORT-ONLY at P2.1.1-P2.1.5; new
  `ChoiceObjectGenerator` + extensions surface as ADDED.

**Cumulative test green count at P2.1.3c HEAD `912efed`:**

- 26 `TypeAliasSolverTest` GREEN.
- 25 `GeneratorModelTest` GREEN (added 2 at P2.1.3c on top of P2.1.3b's 22 + 1).
- 13 `ModelObjectGeneratorTest` GREEN (added 2 at P2.1.3c).
- 5 `ChoiceObjectGeneratorTest` GREEN.
- 2 `Guard` tests GREEN (defensive guards on `ChoiceObjectGenerator` /
  `MetaFieldGenerator`).
- 5 mechanical lock tests GREEN
  (`FeaturesIndexCoverageTest`, `BCVerificationCoverageTest`,
  `UpgradeManifestStructureTest`, `P20SubsystemDeepDiveCoverageTest`,
  `P20MatrixCoverageTest`).
- 56 D11 cells / 0 failures / 4 skipped (5:06 wall-clock).

## Coverage

**5 sub-causes addressed by 4 invariants:**

| Sub-cause | Invariant | Pre-fix waiver entries | Post-fix |
|---|---|---:|---:|
| β1 top-level choice POJO | Invariant 1 + 1.5 | 23 | 0 |
| 4th list-of-data-type-to-Object | Invariant 2 | 5 (3 ground-truth + 2 pattern) | 0 |
| 5th attribute-name-shadows-type-name | (auto-retired; UNNEEDED waivers) | 4 | 0 |
| Latent list-of-type-alias-to-Object | Invariant 3 | 6 (drr only) | 0 |
| BasketConstituent legacy-choice-by-convention | Invariant 4 | 1 (cdm/5.35.0) | 0 |
| **P2.1.3c total** | | **39** | **0** |

**Cumulative Cluster F retirement (P2.1.3 + P2.1.3b + P2.1.3c):**

| Phase | Pre | Post | Retired | Cumulative |
|---|---:|---:|---:|---|
| P2.1.3 (D35) | 184 | 64 | 120 | 120 / 184 = 65% |
| P2.1.3b (D36) | 64 | 32 | 32 | 152 / 184 = 83% |
| P2.1.3c (D37) | 39 | 0 | 39 | 191 / 191 = 100% (revised denominator per re-audit) |

The denominator was revised from 184 to 191 at the re-audit: the
BasketConstituent entry was not in the original Cluster F count (surfaced
mid-PR-#68 from T3 cont's RuneMetaType investigation), and the 4 UNNEEDED
waivers were originally pattern-extrapolated from a single ground-truth
seed without source-side ground-truth probing. The corrected 191 denominator
counts all entries flipped OUT (NOT lines-of-source-code-changed); see
the development audit "cluster-f-final-re-audit" § 4.5 + § 5 for the
reconciliation rationale.

## Cross-references

- **D35** (locked envelope): `the development decision log` D35 entry (local).
  Alias-body argument evaluation invariant — `TypeAliasSolver.evaluateAliasBody`.
- **D36** (locked envelope): `the development decision log` D36 entry (local).
  Cluster F α Locus 1 Case B bypass + β2 classification override.
- **D37** (this manifest): `the development decision log` D37 entry (local).
  4-invariant Cluster F final retirement.
- **Plan:** the development plan "2026-05-15-p2.1.3c-cluster-f-final" (committed).
- **Spec:** `docs/superpowers/specs/2026-05-15-p2.1.3c-cluster-f-final-design.md`
  (local).
- **T0 spike:** the development audit "cluster-f-final-T0-spike" (committed) —
  4 parallel-Agent investigations + Locus ranking.
- **Re-audit:** the development audit "cluster-f-final-re-audit" (committed) —
  ground-truth probe of all 11 remaining waiver entries at HEAD `5ef0739`.
- **Evidence § 8:** the development audit "cluster-f-fix-evidence" (committed) —
  per-cell pre/post counts + zero-residual claim.
- **Predecessor manifest:** `docs/upgrades/U014-first-class-document-structure.md`
  — STUB precedent (P2.0.2). Note: U015 is a FULL manifest (no STUB notice).
- **BC verification:** `docs/bc-verification.md` § Per-feature locks (U015 entry)
  + Layer 4 narrative refresh (Cluster F 191/191 = 100% retired).
- **Feature catalogue:** `docs/features/INDEX.md` — IR & Code Generation topic
  + new "Code Generation" subsection for U015.
