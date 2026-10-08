---
id: U016
subject: Rule / Report / LabelProvider Java codegen — scaffolding (Phase X) + emission + typing milestone (Phase X1 PR #79); byte-flip deferred to engine phase
since_fork_version: "0.1.0"
since_pr_at_spec: 72
since_pr_actual: 72
since_sha: 0c54806f81f55570426e9e8ee9584d2b009f70a4
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D38", "D39", "D40"]
related_audit_hooks: []
related_parity_constraints: []
doc_path: U016-rule-report-labelprovider-codegen.md
---

# U016 — Rule / Report / LabelProvider Java codegen (Phase X scaffolding + Phase X1 emission + typing milestone)

## Concept

This manifest captures the Phase X port of three generator surfaces from
upstream Xtend to native Java in the fork: `RuleGenerator`,
`ReportGenerator`, and `LabelProviderGenerator`. Plus the shared helper
`DeepFeatureCallUtil` (a 173-LOC port from upstream
`rune-dsl-9.75.3/.../utils/DeepFeatureCallUtil.xtend`) and supporting
`RFunction` static factories `fromRule(RRule)` + `fromReport(RReport)`
(fork analogues to upstream `RObjectFactory.buildRFunction(RosettaRule|RosettaReport)`).

**Scope at Phase X (this PR):** Generator classes are slotted into the
`JavaCodeGenerator` per-model dispatch list; their `createTypeRepresentation`
and class-decl emission paths run end-to-end and produce well-formed Java
class skeletons that compile + load against the fork's runtime. The
class-decl emission includes the `@ImplementedBy(...Default.class)`
annotation, the `ReportFunction<I, O>` interface clause (for Rule + Report)
or the `RuneLabelProvider` interface clause (for LabelProvider), the static
inner `*Default` class with the appropriate inheritance, and stub
`evaluate` / `doEvaluate` / `assignOutput` method signatures.

**NOT in scope at Phase X (initially deferred to Phase X1; see § Phase X1
emission + typing milestone below for the milestone-close + engine-phase
deferral of the byte-flip finish line):** The compiled-body
emission inside `assignOutput` (and the consequent `evaluate` /
`doEvaluate` body shape changes to match the upstream builder-validator
pattern). The fork's `RFunction.fromRule(RRule)` and `fromReport(RReport)`
factories produce synthetic `RFunction` instances with an empty `operations`
list, so the existing
`FunctionGenerator.compileOperations(func, indent, outputNeedsBuilder)`
machinery emits an empty `assignOutput(...)` body. Five specific gaps
were originally enumerated for Phase X1 (per-criterion status now
documented in § Phase X1 emission + typing milestone below):

1. **Operations population.** `fromRule` must wrap `rule.expression()` in a
   single `ROperation(SET, "output", expression)` per upstream
   `RObjectFactory.buildRFunction(RosettaRule)` line 121.
2. **Output-type backfill.** Synthetic output attribute carries
   `typeCall=null`; needs `ExpressionTypeComputer` invocation at codegen
   time so the `ReportFunction<I, O>` generic resolves to the correct
   output type (instead of the current `Object`).
3. **Null-guard for expression-tree traversal.** Empirical evidence from
   the T8 spike showed `FunctionExpressionRenderer` hitting a null `expr`
   case during visitor traversal on at least one rule shape; needs either
   defensive null handling or upstream-side fix to ensure full expression
   resolution before codegen.
4. **`outputNeedsBuilder` flag wiring.** The
   `templates/java-function.stg` ST template gates the builder-pattern emission
   (`OutputBuilder` + `.prune()` + `validate(...)` + `.build()` chain) on
   this flag. Synthetic Rule + Report `RFunction` instances need it set to
   `true` (the synthetic output attribute would otherwise yield `false`).
5. **`ModelObjectValidator` injection.** The class-level
   `@Inject protected ModelObjectValidator objectValidator;` field plus
   the corresponding `objectValidator.validate(<OutputType>.class, output);`
   call in `evaluate()` are template responsibilities gated on
   `outputNeedsBuilder=true`. Phase X1 lands these together with #4.

The full Phase X1 scope is captured in
the development audit "phase-x-T8X-body-emission-scope".

**Origin enum dispatch (T6.0.5 → locked at this PR).** `RFunction` carries
a new `Origin` discriminator (`FUNCTION` / `REPORT` / `RULE`), and
`JavaTypeTranslator.toFunctionJavaClass(RFunction, ModelSymbolId)` dispatches
on it to compose the generated class's `<namespace>.<functions|reports>/<Name>{|Rule}`
identity. This is the principled routing the three new generators consume.

**Why scaffolding-first.** PR #70's empirical harness classified 4,883
entries in the D11 `TRANSITIVE_CDM_GAP` cluster as `CODEGEN_MISSING`
(fork-emits-zero vs legacy-emits-file). The original Phase X premise was
that porting the three generators would achieve byte-match across all 4,784
target POJO entries (95 are deferred to Phase Y at P2.1.2c; 4 to Phase Z at
P2.1.2d). The T8 empirical validation revealed the gap is deeper: legacy's
emitted output includes the compiled-body shape, which requires the body
emission machinery (#1-#5 above). Phase X ships the architectural foundation
(class slots + factories + dispatcher routing + test scaffolding);
Phase X1 ships the body emission to complete the byte-match retirement.

## Why

The fork's M9 vision commits to native-Java codegen across all generator
surfaces. The three Xtend generators
(`RuleGenerator.xtend` / `ReportGenerator.xtend` / `LabelProviderGenerator.xtend`
in `rune-dsl-9.75.3/.../generator/java/{reports,function}/`) were not
ported in M1-M8 because:

- M7b focused on the FUNCTION-origin generator (`FunctionGenerator`),
  which itself paused at 38/1272 (the `M7B_PAUSED` waiver category at P0).
- The two report-tree generators (Rule + Report) and the label-tree
  generator (LabelProvider) were treated as "downstream of M7b" and
  inherited the pause.
- The P2.1.2b transitive-CDM-loader-extension plan, drafted 2026-05-16,
  was superseded by PR #70 + PR #71 evidence (the loader has the closure
  it needs; the missing files are CODEGEN_MISSING, not loader gaps —
  but see § Phase X1 emission + typing milestone for the further
  refinement: also CODEGEN_BODY_GAP, byte-flip deferred to engine phase).

Architectural placement decisions:

- `RuleGenerator` + `ReportGenerator` live in
  `com.regnosys.rosetta.generator.java.reports` (matches upstream layout).
- `LabelProviderGenerator` lives in
  `com.regnosys.rosetta.generator.java.function` (matches upstream's
  divergent placement under `function/`).
- `DeepFeatureCallUtil` lives in `com.regnosys.rosetta.utils` (matches
  upstream).
- `RFunction.fromRule` + `fromReport` are static factories on `RFunction`
  rather than methods on a new `RObjectFactory` service — the fork does
  not have upstream's `RObjectFactory` Guice service yet (D31 IR
  architectural commitment defers the service hierarchy decision).

## Structural changes

**New parser-side types:**

- `rune-parser/src/main/java/com/regnosys/rosetta/utils/DeepFeatureCallUtil.java`
  — port of upstream `DeepFeatureCallUtil.xtend` (173 LOC).
- `rune-parser/src/main/java/com/regnosys/rosetta/ast/functions/RFunction.java`
  — adds `Origin` enum (`FUNCTION` / `REPORT` / `RULE`) + `origin()` accessor
  + `setOrigin(Origin)`; adds `fromRule(RRule)` + `fromReport(RReport)`
  static factories with input + output synthetic attribute generation +
  `originRule` / `originReport` back-pointers. Body-emission wiring deferred
  to Phase X1 — see in-source SCOPE NOTE at the `fromRule` body.
- `rune-parser/src/main/java/com/regnosys/rosetta/ast/supporting/RTypeCall.java`
  — adds static `deepCopy(RTypeCall)` for the synthetic-attribute typeCall
  copy used by `fromReport` (Copilot R4 F9 + F10 refactor — produces
  fresh argument + argument-expression nodes per RNode single-parent
  contract).
- `rune-parser/src/main/java/com/regnosys/rosetta/types/RChoiceTypeRef.java`
  — adds the deep-copy entry point in the shared type-ref refactor (R4
  follow-on).

**New generator-side classes:**

- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/reports/RuleGenerator.java`
  — `JavaClassGenerator<RFunction, RGeneratedJavaClass<? extends RosettaFunction>>`
  subclass; `streamObjects(RModel)` filters `RRule` root elements + bridges
  via `RFunction.fromRule`; `createTypeRepresentation` delegates to
  `JavaTypeTranslator.toFunctionJavaClass`; `generate` constructs the
  `ReportFunction<I,O>` base interface and delegates to
  `FunctionGenerator.buildClassWithBaseInterface` with
  `renderAsReportFunction=true`.
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/reports/ReportGenerator.java`
  — symmetric pattern; uses `RFunction.fromReport` + `originReport()` for
  namespace recovery on the synthetic.
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/function/LabelProviderGenerator.java`
  — emits `<Name>LabelProvider` classes implementing `RuneLabelProvider`
  for each report; iterates the report's output-type attribute graph + each
  attribute's `ruleReferenceAnnotations()` to build the label graph.
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/function/LabelProviderGeneratorUtil.java`
  — graph-traversal helpers extracted to keep the generator class focused
  on the emission concerns.

**Generator-model + type-translator extensions:**

- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/types/JavaTypeTranslator.java`
  — adds principled `Origin`-dispatched `toFunctionJavaClass(RFunction, ModelSymbolId)`
  entry point (T6.0.5) routing `FUNCTION → toJavaFunctionClass`,
  `REPORT → toJavaReportClass`, `RULE → toJavaRuleClass` private helpers;
  adds `toMetaJavaType(RAttribute, RType)` 2-arg signature for
  meta-typed argument resolution (T2.5).
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/GeneratorModel.java`
  — adds `symbolId(RFunction)` synthetic-bridge that reads namespace +
  name off `originRule()` / `originReport()` since the synthetic
  `RFunction` has no `RModel` parent attachment (T5 synthetic-recovery
  invariant).
- `rune-java-generator/src/main/java/com/regnosys/rosetta/generator/java/function/FunctionGenerator.java`
  — `buildClassWithBaseInterface` extracted as a public entry point so
  Rule + Report generators can delegate to it with their own
  `JavaParameterizedType<ReportFunction>` base interface arg + the
  `renderAsReportFunction=true` flag (T4 extraction).

**Test scaffolding:**

- `rune-java-generator/src/test/java/com/regnosys/rosetta/generator/java/reports/RuleGeneratorTest.java`
  — 7 emission-shape tests covering streamObjects filter, createTypeRepresentation
  routing, base-interface emission, annotation emission, synthetic-bridge
  namespace recovery, and ImplementedBy emission.
- `rune-java-generator/src/test/java/com/regnosys/rosetta/generator/java/reports/ReportGeneratorTest.java`
  — 8 emission-shape tests covering report-name derivation, two-fail-fast
  paths (null fromType + null/empty withType), base-interface emission,
  symbolId recovery, and ImplementedBy emission.
- `rune-java-generator/src/test/java/com/regnosys/rosetta/generator/java/function/LabelProviderGeneratorTest.java`
  — 19 emission-shape tests covering label graph traversal, attribute
  iteration, ruleReferenceAnnotations harvesting, and emission shape.
- `rune-parser/src/test/java/com/regnosys/rosetta/utils/DeepFeatureCallUtilTest.java`
  — 8 unit tests on the ported algorithm.
- `rune-parser/src/test/java/com/regnosys/rosetta/ast/functions/RFunctionFactoryTest.java`
  — 17 tests on `fromRule` + `fromReport` (input attribute shape,
  synthetic output attribute, originRule/originReport back-pointers,
  Origin enum dispatch, fail-fast invariants — F21 in R8 fix bundle).

**Schema-lock snapshot:**

- `rune-parser/src/test/resources/symbols/schema-lock.txt` — 1-line
  addition for the new `RTypeCall.deepCopy(RTypeCall)` static method;
  schema-lock test regenerated via the documented
  `mvn -Dtest=SchemaLockTest#update_snapshot` path.

**Waiver file (no flip-out at Phase X):**

- `rune-java-generator/src/test/resources/d11-known-divergent.txt` — Phase X
  adds 8 Ingest_*LabelProvider waivers + 4 transitive-CDM emit-gap
  waivers (R7 F19 fix bundle catches the new emit paths with goldens
  expected from upstream). The 4,784 `CODEGEN_MISSING` POJO entries
  remain in the TRANSITIVE_CDM_GAP section with an annotated header
  banner explaining they're now reclassified as `CODEGEN_BODY_GAP`
  pending the engine-phase byte-flip (Phase X1 emission + typing
  milestone closed at PR #79; byte-flip is the engine-phase target).
  The waiver count is unchanged; the classification is
  refined.

## BC Story

**Layer 1 — Source-level.** No grammar change. Existing `rule X from T:`
and `report from T with type R:` declarations parse identically.
Trivially preserved by `CorpusParseTest` + `BackCompatCorpusOrthogonalityTest`.

**Layer 2 — Generated-Java byte-identity.** This is the layer this manifest
is built around. Phase X scaffolds the three generators + Phase X1 closes
the emission + typing milestone (PR #79 — 740 → 124 un-typable on 4 DRR
POJO cells = 88% reduction); byte-identity with legacy plugin output is
NOT yet achieved on the 4,784 target POJO entries (see § Phase X1
emission + typing milestone above for the milestone framing and
engine-phase deferral of the byte-flip finish line). The D11 waiver
still covers these entries; the test passes at the existing 89-identical
+ 2,454-mismatch + 13-missingOutput counts for drr/6.29.0/POJO (no
regression from Phase X / Phase X1).

**Layer 3 — Public API (japicmp).** The new public types are additive:

- `RuleGenerator` + `ReportGenerator` + `LabelProviderGenerator` —
  new generator classes; Layer 3.2 scope. japicmp Layer 3 reports them as
  ADDED entries; non-breaking under D5 budget.
- `RFunction.Origin` enum + `fromRule` + `fromReport` static factories +
  `origin()` + `originRule()` + `originReport()` accessors — new public
  surface on `RFunction`. ADDED only.
- `RTypeCall.deepCopy(RTypeCall)` — new static helper. ADDED.
- `JavaTypeTranslator.toFunctionJavaClass(RFunction, ModelSymbolId)` +
  `toMetaJavaType(RAttribute, RType)` — new methods. ADDED.
- `GeneratorModel.symbolId(RFunction)` — new method. ADDED.
- `FunctionGenerator.buildClassWithBaseInterface(...)` — promoted from
  private to package-visible (or public) for the delegation pattern.
  Signature change captured at T4 extraction commit.
- `DeepFeatureCallUtil` — new utility class. ADDED.

**Layer 4 — D11 byte-identity.** As Layer 2 — Phase X does not retire any
4,784 entries; Phase X1 closes the emission + typing milestone (the
files now emit + type correctly, but byte-identity is not yet reached);
the engine phase (M7b-3 typed pipeline burn-down) will retire them.

**Layer 5 — Consumer smoke.** Out of scope.

**Forward commitment.** The three new generator class signatures join the
permanent public surface contract per D5. The `Origin` enum + `fromRule` +
`fromReport` factories on `RFunction` are also locked. Phase X1 must
preserve all of these structurally; only the synthetic-RFunction body
content (`operations` list + output typeCall) and the template emission
side (`templates/java-function.stg` conditional regions) may evolve.

## How to use

No consumer-visible API change at Phase X. Downstream consumers that
construct `JavaCodeGenerator` directly will see the three new generators
participate in per-model dispatch automatically (they're wired into the
per-model generator list — see `JavaCodeGenerator` constructor changes
at T7).

For consumers that EXPECT compiled-body emission from `RuleGenerator` /
`ReportGenerator` outputs (the byte-match-with-legacy use case): wait for
Phase X1. Until Phase X1 lands, the generated `evaluate` / `doEvaluate` /
`assignOutput` method bodies are stubs that return `output` directly
(rather than compiling the rule body expression). The `ReportFunction<I, O>`
generic `O` is `Object` rather than the actual output type. These are the
five specific gaps enumerated under § Concept above.

## Migration

None. The change is backward-compatible — three new generator classes are
purely additive; their integration into `JavaCodeGenerator`'s per-model
dispatch list does not change existing FUNCTION-origin codegen behaviour.

## Test coverage

**New unit tests:** 59 tests across the five test classes named under
§ Structural changes (RuleGeneratorTest 7 + ReportGeneratorTest 8 +
LabelProviderGeneratorTest 19 + DeepFeatureCallUtilTest 8 +
RFunctionFactoryTest 17).

**Schema-lock coverage:** `SchemaLockTest.m2_ast_schema_matches_snapshot`
locks the public surface of `RFunction`, `RTypeCall`, and friends; the
1-line snapshot addition at this PR locks the new `RTypeCall.deepCopy`
entry.

**Mechanical lock coverage:** `UpgradeManifestStructureTest.ALLOWED_FILES`
bumped 15 → 16 with `U016` added to the allow-list; mechanically locks the
9-section template invariant on this manifest.

## Coverage

**Scope at Phase X (this PR — scaffolding only):**

| Generator | New class | Test class | Acceptance @ Phase X |
|---|---|---|---|
| RuleGenerator | `com.regnosys.rosetta.generator.java.reports.RuleGenerator` | `RuleGeneratorTest` (7 tests) | Class slot wired into `JavaCodeGenerator` per-model dispatch; emits class skeleton with `@ImplementedBy` + `ReportFunction<I, Object>` clause + stub `evaluate`/`doEvaluate`/`assignOutput` |
| ReportGenerator | `com.regnosys.rosetta.generator.java.reports.ReportGenerator` | `ReportGeneratorTest` (8 tests, includes 2 `generate()` fail-fast invariants on synthetic-bypass mutation — `no_inputs` + `no_output`) | Same scaffolding shape; the `fromReport` `null fromType` + `null/empty withType` fail-fast invariants (R8 F21 fix bundle) are covered by `RFunctionFactoryTest` (`fromReport_failsWhenFromTypeMissing` / `fromReport_failsWhenWithTypeMissing`), not this generator's test |
| LabelProviderGenerator | `com.regnosys.rosetta.generator.java.function.LabelProviderGenerator` | `LabelProviderGeneratorTest` (19 tests) | Class slot wired; label-graph traversal + emission |
| DeepFeatureCallUtil | `com.regnosys.rosetta.utils.DeepFeatureCallUtil` | `DeepFeatureCallUtilTest` (8 tests) | Algorithm port verbatim from upstream Xtend |
| `RFunction.fromRule`/`fromReport` | `RFunction.java` static factories | `RFunctionFactoryTest` (17 tests) | Origin enum + synthetic input/output attribute + originRule/originReport back-pointers |

**Waiver impact at Phase X: zero net change.**

| Cell | Waiver entries (pre/post Phase X) |
|---|---:|
| drr/6.29.0/POJO | 2,512 |
| drr/7.0.0-dev.113/POJO | 2,275 |
| **Total** | **4,787** |

(Counts derived from `grep -c "^drr/<cell>/POJO:" rune-java-generator/src/test/resources/d11-known-divergent.txt` at HEAD `9dc169e`. PR #70 + PR #71 evidence reported 4,784 entries on 2026-05-17 / 2026-05-18; today's count is +3 due to 3 additional entries added during Phase X T7 (8 Ingest_*LabelProvider waivers + 4 transitive-CDM emit-gap waivers — R7 F19 fix bundle — net +3 after rebaselining the prior count.)

The Phase X port adds the file-emission machinery (so files exist) but produces stub bodies; the byte-diff against goldens stays the same. All 4,787 entries remain waivered as `CODEGEN_BODY_GAP`. **Engine-phase acceptance:** 4,787 → 0 (byte-flip via M7b-3 typed pipeline burn-down — see the milestone-close § above for the refined framing).

**Test counts:**

- 59 new test methods (RuleGeneratorTest 7 + ReportGeneratorTest 8 + LabelProviderGeneratorTest 19 + DeepFeatureCallUtilTest 8 + RFunctionFactoryTest 17)
- 1 SchemaLockTest snapshot regen (for `RTypeCall.deepCopy`)
- 5 mechanical lock tests (FeaturesIndexCoverageTest + BCVerificationCoverageTest + UpgradeManifestStructureTest + P20SubsystemDeepDiveCoverageTest + P20MatrixCoverageTest) — all GREEN locally with U016 added

## Phase X1 emission + typing milestone (closed at PR #79, 2026-05-27)

PR #79 (merge SHA `4670996`) closes the **Phase X1 emission + typing
milestone** — distinct from the original 7-point byte-flip acceptance below.
The milestone scope: rules now **emit and type** far more correctly than
pre-PR `main`. Un-typable rules (RMissingType output → fail-fast) across
the 4 DRR POJO cells went 130 + 234 + 188 + 188 = 740 → 87 + 25 + 6 + 6 =
**124** (88% reduction). `RuleGenerator` output back-fills the synthetic
output `typeCall` via `ExpressionTypeComputer.compute(soleOp.expression(),
typeInferenceEngine).type()` + cardinality via `CardinalityComputer.compute`
+ explicit null-guard fail-fast with rule + op metadata, so the
`ReportFunction<I, O>` generic resolves to a concrete output type rather
than `Object` for the majority of DRR rule shapes.

PR #79 also lands a **waiver-aware D11 rule-family gate**:
`GenerationException` carries the target output path;
`JavaClassGenerator.generateClasses` resolves the path before body emission
and attaches on failure; the D11 POJO gate splits mature generators
(POJO/choice — hard-fail on any error) from rule-family generators
(Rule/Report/LabelProvider — tolerate generation failures for
*already-waivered* rules, throw on *new* ones, and print tolerated debt
count out loud). A dropped path → null → unwaivered → throws, so
regressions surface red. The 124 tolerated failures are the
**CODEGEN_BODY_GAP debt** tracked in the new standing completeness ledger
at the development audit "codegen-completeness-audit-2026-05-27".

The **byte-flip finish line** (acceptance criteria #1-#7 as originally
stated) is deferred to the **engine phase** — M7b-3 typed pipeline +
M7b-4 deep navigation, completing the M7b expression-compiler staged
migration.

### Per-criterion status at PR #79 close

1. `RFunction.fromRule(RRule)` populates `operations` with one
   `ROperation(SET, "output", rule.expression())` — **LANDED** at PR #76
   (D39 covers the type-inference scaffold that the populated `operations`
   feed into).
2. `RFunction.fromReport(RReport)` populates `operations` by iterating the
   report's RuleSource-equivalent path map (walk-the-output-type-attribute-graph
   + `ruleReferenceAnnotations()` harvesting) — **DEFERRED to engine phase**.
3. `RuleGenerator.generate(...)` backfills the synthetic output attribute's
   typeCall via `ExpressionTypeComputer.compute(rule.expression())` before
   delegating to FunctionGenerator — **LANDED** at PR #76; further
   shape-coverage extensions across the rule-body type-inference levers
   landed at PR #79. Same for `ReportGenerator` — **DEFERRED**.
4. `FunctionExpressionRenderer` handles null sub-children in the
   expression tree gracefully (defensive null-guard) — surfaced empirically
   at the T8 spike — **PARTIALLY LANDED** (defensive null-guards added at
   PR #76; full traversal coverage in engine phase).
5. `FunctionTemplateModel.outputNeedsBuilder` is set to `true` for
   synthetic Rule + Report `RFunction` instances so the
   `templates/java-function.stg` ST template emits the builder-pattern body shape
   (`OutputBuilder` + `.prune()` + `validate(...)` + `.build()`) — **DEFERRED**.
6. `templates/java-function.stg` ST template (or `FunctionGenerator` Java-side glue)
   emits the `@Inject protected ModelObjectValidator objectValidator;`
   class-level field + the `objectValidator.validate(<OutputType>.class, output);`
   call inside `evaluate()` when `outputNeedsBuilder=true` — **DEFERRED**.
7. D11 14-cell sweep: 4,784 (drr/6.29.0/POJO + drr/7.0.0-dev.113/POJO)
   entries flip from waivered to byte-identical — **DEFERRED to engine
   phase** (this is the engine-phase target metric).

## Cross-references

- **D38** (this manifest): `the development decision log` D38 entry (local).
  Phase X scaffolding-only scope + Phase X1 body-emission deferral
  decision.
- **Subagent scope memo:** the development audit "phase-x-T8X-body-emission-scope"
  — full body-emission gap inventory + decomposition into Phase X1 MVP
  steps (5-9 commits estimated).
- **Plan:** the development plan "2026-05-19-phase-x-port-3-generators" (committed).
- **Spec:** `docs/superpowers/specs/2026-05-19-phase-x-port-3-generators-design.md`
  (local).
- **T0 spike:** the development audit "phase-x-T0-spike" (committed) —
  initial scope investigation; surfaced the synthetic-RFunction
  namespace-recovery pattern.
- **T8 amendment:** the development plan "2026-05-20-resume-phase-x-T8-AMENDMENT"
  (local) — captures the T8.X1 discovery + user decision flow.
- **PR #70 harness evidence:** `docs/harness/2026-05-17-legacy-vs-fork-evidence.md`
  (committed) + PR #71 revalidation
  `docs/harness/2026-05-18-revalidation-14-cells.md` (committed) — original
  `CODEGEN_MISSING` classification that motivated Phase X.
- **Predecessor manifest:** `docs/upgrades/U015-choice-pojo-codegen.md` —
  same generator-side scaffolding pattern (`ChoiceObjectGenerator`
  precedent for `RuleGenerator`/`ReportGenerator`/`LabelProviderGenerator`).
- **BC verification:** `docs/bc-verification.md` § Per-feature locks
  (U016 entry).
