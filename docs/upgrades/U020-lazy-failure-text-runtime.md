---
id: U020
subject: Lazy comparison-failure text in the fork-owned runtime — the dual-field ComparisonResult carrier (memoized String + Supplier) with package-private lazy factories; 68 formatter seats defer message construction until the text is observed, byte-identical at every live reading seat
since_fork_version: "0.1.0"
since_pr_at_spec: 546
since_pr_actual: 547
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: []
related_parity_constraints: []
doc_path: U020-lazy-failure-text-runtime.md
---

# U020 — Lazy comparison-failure text: the deferred error channel in `ComparisonResult`

> **Era note.** Censused + designed at PR #546 (the § 6.6 observation-seat
> census, `research/p3-navigation-family-census.md` § 14 — the user's pick C
> at the three-way menu); the runtime edit landed at PR #547 under the § 14f
> proof set. The FIRST runtime-lane change of the v3 optimised-codegen
> phase: it edits `rune-runtime` (U018), which both generation routes share,
> so BOTH trees benefit — it is deliberately NOT an optimised-tree emission
> family and takes no scoreboard family row. The PR #547 paired
> same-session 6-fork A/B measured the DRR report run **3.47× faster**
> (8,527.481 → 2,454.674 ms/op = −71.2%) with allocation down two-thirds
> (30,790.11 → 10,457.24 MB/op = −66.0%) — ~3× the census § 13d
> prediction (census § 14g).

## Concept

Every failing (or empty-operand) comparison in the runtime's expression
layer used to build its failure message eagerly at result-creation time —
`String.format` over `getPaths()`, list renders, guava-backed CaseFormat
hops (`ErrorHelper.formatEqualsComparisonResultError`) — even though the
census proved the report-run path reads NO failure text anywhere (the
load-bearing zero, § 14c: zero `ComparisonResult.getError()`/`getErrors()`
readers under any `functions/`, `reports/`, `qualify/` or `meta/` generated
package across all five corpus cells). U020 defers that construction: the
carrier holds EITHER an eagerly-materialized `String error` OR a
`Supplier<String> errorSupplier`, and `getError()` materializes-and-memoizes
on first read (the benign-race `String.hashCode` pattern — deterministic
side-effect-free suppliers, atomic reference write, supplier field final for
safe publication). Text nobody observes is never built; text anybody
observes is character-identical to the eager text, by construction (§ 14d —
every live reading seat forces same-thread, immediately adjacent to
creation, with no interleaved mutation of the subject object).

## Why

The #545 JFR time-composition census charged ~16–18% of the DRR report
run's execution samples to `ErrorHelper` alone (plus its induced path/string
build; `byte[]` = 50.9% of reference-tree allocation pressure, qualitative),
making lazy failure text the ONLY double-digit time lever on the § 13d
re-ranking (~20–25% predicted). The § 14 census then proved the risk
profile: the observing-seat surface equals the validator populations exactly
(13,325 reader files = cardinality 4,736 + type-format 4,736 + data-rule
3,853), every reader forces immediately, the deprecated/secondary channels
are corpus-dead, and `toString`/`equals`/`hashCode`/serialization channels
are closed — so deferral is provably unobservable except through speed.

## Structural changes

All in `rune-runtime`, all inside `com.rosetta.model.lib.expression`; zero
emitter edits, zero generated-code changes:

1. **The carrier** (`ComparisonResult`): `private final String error` →
   `private String error` (the memoized materialization) + NEW
   `private final Supplier<String> errorSupplier`; invariant — at most ONE
   of {error, errorSupplier} non-null at construction; success/empty keep
   both null, so materialized-null ⟺ no-channel exactly as before.
   `getError()` materializes and memoizes. A private `errorText()` accessor
   is the single internal read seam.
2. **Three package-private lazy factories**: `failureLazy(Supplier<String>)`
   plus the emptyOperand twins `successEmptyOperandLazy` /
   `failureEmptyOperandLazy` (deprecated-annotated to mirror their eager
   public twins). The public factory surface is untouched.
3. **68 formatter-seat flips** — each message expression moved VERBATIM
   into a lambda: `ExpressionEqualityUtil` 16 + `ExpressionEqualityUtilNullSafe`
   14, `ExpressionCompareUtil` 3 + `ExpressionCompareUtilNullSafe` 3,
   `ExpressionOperators` 16 + `ExpressionOperatorsNullSafe` 16. The 8
   fixed-literal seats among the census's 76 producer sites stay eager per
   the § 14b plain-string class ("No fields set.", "All required fields not
   set.", `""`, "Results are not equal" — ×2 files), as do `checkCardinality`
   (its text is read unconditionally by every cardinality validator) and all
   plain-string seats (`MapperUtils`, `QualifyFunctionFactory`, the
   generated data-rule catch-block).
4. **The five combinators compose lazily** (`andNullSafe`, `orNullSafe`,
   deprecated `and`/`or`/`combineIgnoreEmptyOperand`): child texts
   materialize FIRST inside the composed supplier via `errorText()`, then
   the EXISTING building code runs verbatim — the passing-`and` stored-`""`
   quirk, the `orNullSafe` `"null"` rendering for null-channel children, and
   the deprecated trio's concat semantics all reproduced exactly. The
   boolean/emptyOperand logic stays eager — only the error channel defers.
   Every internal raw `.error` field read was swept to the accessor.

## BC Story

**Zero public-API delta, by construction:** every formatter-backed producer
seat lives in the carrier's own package, so the lazy channel is
package-private throughout — no public method added, none changed, none
removed. Receipt: `javap -p` on the installed jar shows the new members
(`errorSupplier`, `failureLazy`, the emptyOperand lazy twins, `errorText`)
all non-public; the public declaration surface of `ComparisonResult` is
line-identical to the pre-edit class (the U018 ABI posture — the released
9.83.0 surface — is undisturbed). External callers of the public factories
(`failure`, `successEmptyOperand`, `failureEmptyOperand`, `of`,
`ofNullSafe`) get eager semantics unchanged; external subclass/reflection
observation of the private field layout is outside the BC envelope (the
carrier documents the dual-field invariant in-source). The one semantic
delta class is exception TIMING: no formatter arm can throw for inputs the
eager path accepts, but a theoretical throw would move from creation-site to
read-site (§ 14d note) — recorded, and empirically covered by the § 14f
proof set. Thread-safety: the memoization is the benign-race pattern —
worst case a duplicate format of identical content; the supplier field is
final for safe publication.

## How to use

Nothing to do — the change is internal to the runtime and transparent to
generated code and external callers. Runtime-internal producers wanting the
lazy channel call `ComparisonResult.failureLazy(() -> ...)` (package-private;
suppliers must be deterministic, side-effect-free and non-null-returning).
`getError()` semantics are unchanged for every caller: null means no error
channel; a non-null return is the same bytes the eager path produced.

## Migration

None. No API change, no behavioural change at any observation seat, no
generated-code change (the byte rings held exactly: OFF `e1815d1e` / ON
`bd0287c3`).

## Test coverage

- **The § 14f before/after text-capture instrument** (`PairHarness`,
  `-Dequiv.textCapture=<dir>`, PR #547): every O2 validation line — both
  sides, embedding `getFailureReason` — sorted + SHA-256 per run. BEFORE
  (pre-edit runtime) vs AFTER (edited runtime): all 8 capture files
  byte-identical across the five cells (cdm5 `70299af8` / cdm6 `7cbf4b07` /
  drr `9c61e8bd` / iso `721a6b80` / fpml `5eb3b3a1`; 60,328 failure lines
  across the 8 files — 41,246 across the five distinct cells —
  non-vacuous, incl. `formatEqualsComparisonResultError` output composed
  through `andNullSafe`).
- **The 10 standing text pins**: `ExpressionOperatorsNullSafeTest` (8
  `getError()` reads) + `ExpressionOperatorsNullSafeParameterizedTest` (2) —
  exact message bytes at the unit level.
- **The U020 contract locks**: `ComparisonResultLazyChannelTest` (7;
  rune-runtime) — deferral-until-read, materialize-once memoization,
  eager-twin text equivalence for all three lazy factories, unforced
  combinator composition, the passing-`and` stored-`""` quirk, the
  passing-`or` null channel (with the child supplier provably never
  invoked — the laziness win itself), and `getErrors()` routing. Runtime
  suite 99/0/0/0 at PR #547.
- **The differential + smoke gates**: rune-equivalence 22/0/0/0 ×2
  (cross-JVM), O2 zero over the full pair surface on the edited runtime.
- **The rings**: gensuite 3,720/0/0/53 with the 56-line OFF channel
  `e1815d1e` EXACT; the ON probe 55/55 with the 169-line channel `bd0287c3`
  EXACT — zero emitter drift, as a runtime-only change must show.
- **The paired same-session 6-fork JMH A/B** (OLD vs NEW runtime, both legs
  the reference tree, identical workload census 6/6 forks each;
  `target-547-jmh-drr-{oldrt,newrt}6f1.log`): time 8,527.481 ± 411.588 →
  2,454.674 ± 258.851 ms/op (**−71.2%**); allocation 30,790.11 ± 121.15 →
  10,457.24 ± 2.13 MB/op (**−66.0%**); fully disjoint at the iteration
  level (OLD min 7,651 vs NEW max 3,344 ms).

## Coverage

All five corpus cells (cdm 5.38.0 · cdm 6.20.6 · drr 6.34.1 · iso20022
1.38.0 · rune-fpml 2.0.0), both generation routes (reference + optimised
trees ride the same runtime), every formatter-backed producer seat (68/68
flipped; the 8 fixed-literal seats deliberately eager), every observation
channel of the carrier (§ 14a closed enumeration — `getError()`,
`getErrors()`, the five combinators, the validator/qualify/checkString/
checkNumber readers).

## Cross-references

- The census + design SOT: `research/p3-navigation-family-census.md` § 14
  (§ 14a channels · § 14b producers · § 14c readers · § 14d deferral window
  · § 14e design · § 14f proof plan).
- U018 (fork-owned runtime) — the module this lands in; the ABI posture it
  preserves.
- U019 (optimised IR-routed codegen) — the phase this serves; U020 is the
  phase's runtime lane, benefiting both routes.
- The phase plan: the development plan "2026-08-10-p3-optimised-codegen-phase-plan"
  § 7/§ 8.
- BC layer pairing: `docs/bc-verification.md` § Per-feature locks (U020
  bullet).
