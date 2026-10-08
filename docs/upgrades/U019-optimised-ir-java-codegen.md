---
id: U019
subject: Optimised IR-routed Java codegen — the flag-gated route 3 (the rune-ir-java-optimised module + the OptimisedIRGenerationProvider seam behind -Drosetta.generator.ir.optimised=true; the FUNCTION seam flipped at PR #539, tranche 2 terminal-multi at PR #540, family 2 meta chains at PR #541, the member-injection seam + the § 11 stream re-price measured out at PR #542, the rule-face family 3 — the rule/report window flip + item-rooted ladders — at PR #544)
since_fork_version: "0.1.0"
since_pr_at_spec: 536
since_pr_actual: 538
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D43"]
related_audit_hooks: []
related_parity_constraints: []
doc_path: U019-optimised-ir-java-codegen.md
---

# U019 — Optimised IR-routed Java codegen: the flag-gated route 3

> **Era note.** Specced by the user-approved phase plan (PR #536); the
> skeleton — module + flag + seam + inertness locks — landed at PR-3 of the
> v3 phase (PR #538). THE FIRST EMISSION FAMILY landed at PR-4 (PR #539):
> the FUNCTION construction seam is FLIPPED to the navigation-chain family's
> `OptimisedFunctionGenerator` (tranche 1, differentially gated GREEN, zero
> API delta); every other seam still DECLINES (Path-1 fallback). PR-5
> (PR #540) widened the family to tranche 2 — TERMINAL_MULTI chains land
> (the `MapperC` boundary), while the STREAM sub-shape is a MEASURED DECLINE
> (census § 9d — kept reference-emitted on the paired JMH receipts). PR-6
> (PR #541) landed FAMILY 2 — META/reference-semantics chains
> (`FieldWithMetaX`/`ReferenceWithMetaX` wrapper hops) ride the SAME forms:
> interior derefs baked as `getValue` getter runs, meta tops witnessed with
> the wrapper class, the meta STREAM rows carrying the § 9d decline
> (census § 10). PR-7 (PR #542) landed the reference generator's
> MEMBER-INJECTION seam (the `buildHelperMethodsBlock` hooks + the
> `Default`-class template slot — byte-inert when unused, the OFF probe
> exact) and ran the § 9d STREAM re-price through it: the loop-form helpers
> were built, gate-proven equivalent (O5 zero — private members
> API-invisible) and MEASURED OUT (+494.7 MB/op, the same-day 6-fork A/B;
> census § 11 — the escaping-materialized-list mechanism), so the STREAM
> decline is permanent at this runtime's boundary contract and the seam
> stays as the reusable instrument. PR #544 (family 3, census § 12 —
> user-picked) flipped the RULE FACE into the window: rules + reports render
> through one bracketed entry with the synthetic-owner bridge, ITEM-rooted
> chains land at the same forms over a render-harvested root (`item.get()`),
> the gate widened to pair-execute every `*.reports.*` class (zero
> divergence, O5 zero on 3,743 drr classes), and the paired A/B priced
> −581.2 MB/op = −1.89% fully disjoint — the phase's largest single-family
> reduction. Further families arrive one per PR, each behind the
> functional-equivalence gate.

## Concept

A third generation route through the Java code generator — as measured 2026-09-11: AST →
the **optimised emitter** (`rune-ir-java-optimised`, hosted on the AST compiler and
consulting the **IR** (`rune-ir`, U012) to SELECT its converted families) → Java; "AST → IR →
the optimised emitter" is the v3.4 target (the optimised route), not this route's state. It stands beside Path-1 (the
legacy shipping route) and Path-2 (the byte-identical reference IR route, U013) — one IR, three
backends as the TARGET (the phase plan § 2). Opt-in via `-Drosetta.generator.ir.optimised=true` (read with
`Boolean.getBoolean`, so the `=true` spelling is the documented form) with
the module's jar on the classpath.

**As measured 2026-09-11 (PR #632, the honesty pass):** the route's compiler is
`OptimisedExpressionCompiler extends ExpressionCompiler` — the AST compiler; the IR
is consulted by `NavigationChainClassifier` (`ExpressionToIRAdapter.adapt` per
candidate site) to SELECT the converted families; the module depends on `rune-ir`
+ `rune-java-generator`, not `rune-ir-java`, and builds no declaration IR. So
"AST → IR → the optimised emitter" and "one IR, three backends" above describe the
v3.4 target (the optimised route's IR closure, after v3.3 closes the legacy route), not this route's state —
IR coverage — measured.

Unlike Path-2, the optimised route is **intentionally NOT fully
backward-compatible** (requirement 1 as revised by the user at P3.1): its
hard gate is FUNCTIONAL PARITY — identical results, validation outcomes with
byte-for-byte message text, thrown behavior, qualification, and
post-processed identity — while the API delta vs the reference output is
measured and REPORTED per family PR, not gated. The seam surface is the
reference route's own SPI inherited unchanged: `OptimisedIRGenerationProvider
extends IRGenerationProvider`, registered under its OWN `META-INF/services`
file so the reference lookup never sees it and both jars coexist without
tripping either route's exclusivity check.

## Why

The measured Mapper-route cost is the phase's motivation (the PR #537
baselines): four-hop navigation at 396.9 ns / 1,720 B vs 0.586 ns direct
(~677×), a full CDM validation sweep at 8.28 ms / 4.46 MB, DRR rule
evaluation at ~285 ms + 1.33 GB per report. The opportunity survey ranks
navigation chains the first emission family. Correctness-then-perf: the
skeleton lands BEFORE any family so the equivalence gate (the
`rune-equivalence` harness) exists first, and the ServiceLoader seam keeps
both standing routes untouched — THE STRICTNESS DIRECTIVE's zero-regression
bind stays mechanical (flag-off never consults the seam; the rings gate it).

## Structural changes

- NEW module `rune-ir-java-optimised` (`org.finos.rune:rune-ir-java-optimised`
  — outside the release six while the phase was in flight; it JOINS the release
  set at v3.0.0 as the seventh module): `OptimisedIRFlag` (the
  module-local flag mirror), `OptimisedIRGenerationProviderImpl` (since PR #539
  the FUNCTION construction seam returns `OptimisedFunctionGenerator` — the
  navigation-chain family, tranches 1–2 since PR #540 + the meta family 2
  since PR #541 + the rule-face family 3 since PR #544 (the same generator's
  `buildClassWithBaseInterface` override brackets the rule/report renders in
  the window and stamps the owner bridge); the other four
  construction seams return
  the PLAIN legacy generators and both dispatch seams decline), the emission
  stack (`NavigationChainClassifier` + `OptimisedExpressionCompiler` +
  `OptimisedFunctionGenerator` + `OptimisedFunctionExpressionRenderer`), the
  `META-INF/services/…OptimisedIRGenerationProvider` registration, and the
  seam-lock suite `OptimisedGenerationSeamTest` (9 tests) + the conversion
  conservation gate `OptimisedNavigationEmissionTest` + (since PR #542)
  `NavigationHelperForm` — the § 11 loop-form design of record,
  status-stamped MEASURED OUT, no live emitter path. Depends on
  `rune-ir` (READ-ONLY consumption) + `rune-java-generator` (the SPI types);
  zero dependency on `rune-ir-java` — no code-path sharing with the
  reference backend.
- `rune-java-generator` additions (both in the `spi` package, ADDITIVE):
  `OptimisedIRGenerationProvider` (the marker subinterface) and
  `IRGeneration`'s second flag surface — `OPTIMISED_PROPERTY`,
  `optimisedEnabled()`, a second cached ServiceLoader lookup keyed on the
  optimised flag, and the both-flags exclusivity error. ZERO call-site
  changes: every existing seam helper routes through the same
  `providerOrNull()`, whose both-flags-off path returns `null` exactly as
  before.
- `rune-java-generator` MEMBER-INJECTION seam (PR #542, ADDITIVE +
  byte-inert): `FunctionGenerator.onStandardModelBuildStart()` (default
  no-op — the per-file reset belt) + `buildHelperMethodsBlock(indentLevel)`
  (default `""`), wired at `buildStandardModel`'s single choke point
  (standard + rule-face + dispatch-variant model builds all pass through
  it); `FunctionTemplateModel` carries the post-construction
  `helperMethodsBlock` stamp; BOTH function templates
  (`java-function.stg` / `java-function-dispatch.stg`) render the block
  immediately before the `Default`-class close — an EMPTY block renders
  ZERO bytes, so the reference pipeline is byte-identical (the OFF-probe
  sha reproduced EXACT with the seam in place). The seam is the delivery
  surface for generated per-file `private static` members — built for the
  § 11 re-price and retained as the instrument for any future
  member-emitting family (private members are O5-invisible, mechanically
  proven on the § 11 gate runs).

## BC Story

- **Flag-off is byte-identical BY CONSTRUCTION on both standing routes**: with
  neither flag set, `providerOrNull()` returns `null` before any
  ServiceLoader touch — the identical legacy calls execute. The OFF/ON D11
  rings (SOT shas) re-derive green at every v3 PR (THE STRICTNESS
  DIRECTIVE), locking the claim mechanically.
- **Route exclusivity is loud**: `-Drosetta.generator.ir=true` plus
  `-Drosetta.generator.ir.optimised=true` throws `IllegalStateException` on
  every call — a run must state which backend it means. More than one
  optimised registration on the classpath throws the same way (the #466
  posture).
- **Coexistence**: the optimised registration lives ONLY under the optimised
  interface's service file — invisible to the reference route's lookup, so
  shipping both jars on one classpath is safe in either flag state.
- **Un-flipped seams ≡ Path-1**: every seam left un-flipped declines, so
  un-claimed kinds fall back to the legacy route, whose bytes the parity
  rings prove. Since PR #539 the FUNCTION seam is FLIPPED (the
  navigation-chain family; tranches 1–2 since PR #540, the meta family 2
  since PR #541, the rule-face family 3 since PR #544 — the RULE kind's
  flag-on output differs at converted sites too) — flag-on FUNCTION
  output differs from
  Path-1 at the converted sites under the functional-equivalence gate (zero
  divergence, zero API delta reported); every other kind still serves
  Path-1-identically by this same decline-by-default contract.
- **Families change bytes ONLY behind this flag**, each gated by the
  functional-equivalence harness (both legs blocking) with the API delta
  REPORTED (O5) per the revised requirement 1 — PR #539 is the first
  (`research/p3-navigation-family-census.md` carries the priced scope, the
  conservation laws and the JMH delta).

## How to use

Generate with `-Drosetta.generator.ir.optimised=true` and
`rune-ir-java-optimised` (+ its `rune-ir` dependency) on the generator
classpath. Since PR #539 the FUNCTION kind produces optimised bodies behind
the flag (the navigation-chain family's direct ladders — functionally
identical under the differential gate, zero API delta); every other kind
stays Path-1-served byte-identically (those seams still decline). Without
the flag (or without the jar) nothing changes — the flag-on-no-jar case
prints a single `System.err` notice and proceeds on Path-1.

## Migration

None. The route is opt-in; no consumer surface changes. When emission
families land, each family PR ships its API-delta report + migration notes
for any deliberate signature departure (expected ZERO in early body-only
families).

## Test coverage

`OptimisedGenerationSeamTest` (9 tests, rune-ir-java-optimised): property-name
agreement across modules (the Rule-3 lock) · registration resolves exactly
the impl under the optimised service file · the registration is INVISIBLE to
the reference lookup (the coexistence contract) · both-flags-off returns no
provider · optimised-flag-on resolves the impl through `IRGeneration` itself
· both-flags throws loudly and clearing re-closes · EXACT-CLASS pins on all
five construction seams — four decline pins returning the plain legacy
generators + the PR #539 FUNCTION flip pin asserting
`OptimisedFunctionGenerator` (the un-pin-deliberately contract: a family PR
flipping a seam must re-pin its line in the same PR) · dispatch seams
decline · the seam helper executes Path-1 end-to-end under the flag for
un-flipped kinds. The first family adds its own locks (PR #539; widened at
PR #540, PR #541 and PR #544): `OptimisedNavigationEmissionTest` (the conversion
conservation laws — shortfall=0 identity containment, bailed=0, the walk
widened to RULE bodies through the `fromRule` synthetic-owner bridge at
PR #544, the pinned
per-cell event counts now covering tranches 1–2 plus the meta family 2 plus
the family-3 item/rule-face pools (with the ITEM decline/suppression/bail
pins — incl. the rootTypeMissing unprovable-wrapper refusal — and the
REPORT-face zero tuple + the rule-face witnesses), the
per-family twin type-missing fallback pins, the § 9d STREAM decline pins
and their § 10a meta twins — annotated since PR #542 with the § 11
decomposition (walk + typed twins + missing twins per cell) — the ladder +
`MapperC`-boundary witnesses — the baked `getValue` interior-deref ladder
and BOTH wrapper-kind meta-top boundaries included — and the
decline-negative witness now covering BOTH measured-out stream forms'
unique tokens (`_nav0`/FQN-collect + `_navStream`); plus the PR #542
instruments: the shortfall FORENSICS print (which plan fact is missing,
per missed site), the `-Doptnav.reportOnly=true` diagnostic mode — the
laws still assert, the value pins report, so one run measures every cell
during a re-price — and, since v3.2 seat 8 (PR #629), the
`-Doptnav.dump-sites=<dir>` per-site instrument: every converted site of
every cell as a sorted multiset with its ranged ancestor, spelling, plan,
parent kind, enclosure and owner, so two runs at two contents diffed row
for row NAME the sites whose conversion verdict moved — the measure-first
receipt of any optimised-route pin movement) and the
rune-equivalence differential gate `OptimisedNavigationPairGateTest`
(reference-vs-optimised pair execution, zero divergence, O5 published) —
since PR #607 both gates read their cell list and every cell's transitive
dependency closure from `test-corpus/corpus-cells.tsv` and run over EVERY
function-bearing catalogue cell (the ten CDM + the ten DRR cells; the
function-free corpora proven so by a goldens-scanning belt), the emission
test's fourteen per-value pin maps folded into one per-cell row with a
loud missing-row law, and the cdm 6.20.x cells compiled WHOLE against
rune-fpml 1.5.3's own-toolchain goldens (the `cdm/ingest/` exclusion retired).
The member-injection seam's inertness lock is the OFF-probe sha itself
(the seam rode `target-542-gensuite1.log` with `e1815d1e` EXACT — an empty
block renders zero bytes at every function file).
The generation-level inertness lock is the standing ring pair: the
OFF-ring and ON-ring D11 SOT shas, re-derived at every v3 PR.

## Coverage

Since PR #539 the route claims the FUNCTION kind, and since PR #544 the
RULE face beside it (census § 12 — the same `OptimisedFunctionGenerator`
renders the drr rule/report classes through the bracketed
`buildClassWithBaseInterface` entry): all 2,901 FUNCTION files plus the
2,317 drr `<Name>Rule` files are re-emitted (the 23 `<Name>ReportFunction`
files render through the window and are proven conversion-free — the
REPORT-face zero tuple); tranche 1's 10,186 SINGLE-ladder conversions +
tranche 2's 544 TERMINAL_MULTI + family 2's 182 meta-chain + family 3's
2,509 item/rule-face conversions = **13,421 events since PR #544**, priced
by `research/p3-navigation-family-census.md` § 4 + § 9b + § 10b + § 12e;
the STREAM sub-shape is TWICE measured out — the § 9d stream pipeline and
the § 11 loop-form re-price (PR #542) — pinned 97/215/414 with its meta
rows pinned 41/38/14 (the drr growth = the rule-face STREAM events) plus
the family-3 item declines 2/4/152 + 3/2/173, permanent at this runtime's
boundary contract; the other kinds stay
declined per the phase plan § 3.1 treatment table
(POJO/ENUM/METAFIELD/XMETA/PACKAGE_INFO frozen; validators +
DEEP_PATH_UTIL frozen-initially; DATA_RULE re-priced as a later family by
the census § 3). A kind moves to re-emitted ONLY via a family PR carrying
its own census, differential-gate receipts (zero divergence on both legs),
JMH delta, and the seam-pin edit — never silently.

## Cross-references

- The phase plan (authority): `docs/plans/2026-08-10-p3-optimised-codegen-phase-plan.md` § 2 (architecture), § 5 (requirement-1 evolution), § 8 (PR sequence)
- Requirements SOT: `research/p3-technology-inventory.md` § 3 · the ranking: `research/p3-opportunity-survey.md`
- The harness dependency census: `research/p3-rosetta-common-census.md`
- U012 (the IR substrate) · U013 (the reference IR route + the #466 seam pattern this route mirrors)
- The equivalence harness module: `rune-equivalence/` (the gate the families run through)
