---
id: U021
subject: Deferred MapperPath lineage in the fork-owned runtime — the cons-cell path (O(1) per hop, no per-hop ArrayList copy) + the lazy PathElement (memoized name/getterAndContext) + the memoized toAttributeName conversion cache; every corpus-reachable observation byte-identical
since_fork_version: "0.1.0"
since_pr_at_spec: 549
since_pr_actual: 550
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: []
related_parity_constraints: []
doc_path: U021-deferred-mapper-path-lineage.md
---

# U021 — Deferred MapperPath lineage: the cons-cell path + lazy element in the runtime's mapper package

> **Era note.** Censused + designed at PR #549 (the § 6.7 observation-seat
> census, `research/p3-navigation-family-census.md` § 16 — the user's pick A
> at the post-§ 15 menu); the runtime edit landed at PR #550 (v3 PR-14,
> PR-B = B2 — the user's pick at the PR-B menu) under the § 16f proof set.
> The SECOND runtime-lane change of the v3 optimised-codegen phase (the
> U020 posture verbatim): it edits `rune-runtime` (U018), which both
> generation routes share, so BOTH trees benefit — deliberately NOT an
> optimised-tree emission family and takes no scoreboard family row. The
> PR #550 paired same-session 6-fork A/B measured the DRR report run
> **1.29× faster** (2,617.369 → 2,031.881 ms/op = −585.488 ms/op = −22.4%)
> with allocation down −1,824.32 MB/op (10,096.20 → 8,271.87 = −18.1%),
> CI-disjoint on both channels (census § 16g).

## Concept

Every mapper hop used to pay path bookkeeping eagerly: `toBuilder()` copied
the ENTIRE parent element list into a fresh `ArrayList` (O(depth) per hop —
O(depth²) per chain), and each `PathElement` derived BOTH display fields at
construction (`name` via guava `CaseFormat.UPPER_CAMEL → LOWER_CAMEL`;
`getterAndContext` via `String.format` when list-indexed) — even though the
census proved NO corpus-reachable seat ever observes that lineage outside
failure text, which is itself lazy since U020 (§ 16: zero golden call sites
on every lineage channel across all five cells, both call syntaxes; the one
evaluation-time path reader — the deprecated legacy `onlyExists(List)` — is
corpus-dead, all 219 golden `onlyExists` occurrences being the modern
3-arg reflective overload). U021 defers all three seats: `MapperPath`
becomes a parent-linked immutable cons node (`parent` + `element` + `size`;
every `add*` O(1); the flat element view materializes on first read,
benign-race memoized), `PathElement` stores `getter` + `listIndex` and
memoizes `name`/`getterAndContext` on first read, and `toAttributeName`
serves the IDENTICAL guava conversion through a
`ConcurrentHashMap.computeIfAbsent` cache keyed by the getter literal.
Lineage nobody observes is never flattened, converted or formatted; lineage
anybody observes renders character-identical to the eager build, by
construction.

## Why

The #548 JFR re-composition (census § 15) made the MapperPath eager lineage
build the board's #1 addressable mechanism — ~32–36% charged self, the only
double-digit mechanism left after U020 retired the failure-text sink — with
`MapperPathBuilder`'s copy machinery the largest single class charge
(23.3/20.7%) and `PathElement.toAttributeName` dominating the element charge
(85/94% of it). The § 16 census then closed the risk profile: the reader set
is EMPTY at corpus scale (the all-channel golden zero), every runtime-main
path read left sits inside the U020 lazy failure suppliers, the parent-object
channel walks `parentItem` references (never PathElements), the RosettaPath
lane is token-disjoint, and the whole representation is package-encapsulated
— so a representation change is provably unobservable except through speed.

## Structural changes

All in `rune-runtime`, all inside ONE file —
`com.rosetta.model.lib.mapper.MapperPath` (package-private class); zero
emitter edits, zero generated-code changes, zero other-class edits:

1. **L3 — the cons-cell path**: `MapperPath` drops its `List<PathElement>`
   field for `parent` (nullable) + `element` + `size` (all final) + a
   benign-race memoized flat view built as
   `Collections.unmodifiableList(Arrays.asList(...))` (final-field chain —
   safely publishable under racy read). `builder()`/`toBuilder()` KEEP their
   signatures; `MapperPathBuilder` holds the base path and each `add*`
   constructs the child node in O(1). The ctor is package-private (a private
   ctor called from the nested builder would make javac at release 8
   synthesize a `MapperPath$1` access bridge — disclosed § 16g deviation
   from the § 16e sketch; the class itself is package-private, so the BC
   envelope is unchanged). Reads (`getNames`/`getGetters`/`getLastName`/
   `getFullPath`/`toString`/`equals`/`hashCode`/`compareTo`) keep their
   expressions verbatim over the materialized view — identical values and
   verdicts (List semantics over equal elements; the list-hash algorithm is
   shared by `AbstractList`).
2. **L2 — the lazy element**: `PathElement` keeps `getter` + `listIndex`
   final; `name` + `getterAndContext` become benign-race memoized reads
   (pure functions of the final fields — worst case a duplicate compute of
   an identical, safely-publishable String). `equals`/`hashCode`/`compareTo`
   route through the memoizing accessors; the three-term hash arithmetic
   (getter, listIndex, name) is kept exactly.
3. **L1 — the conversion cache**: `toAttributeName` becomes
   `ATTRIBUTE_NAMES.computeIfAbsent(getter, g -> CaseFormat.UPPER_CAMEL
   .to(CaseFormat.LOWER_CAMEL, g.substring(3)))` for get-prefixed getters —
   the IDENTICAL guava function, cached (no hand-rolled conversion); non-get
   labels pass through unconverted as before. The key population is the
   get-prefixed getter-literal vocabulary generated code passes — corpus-
   bounded, two small strings per entry — so the cache carries no eviction.
4. **The one behavior corner (dormant)**: the old builder held its mutable
   list BY REFERENCE, so reusing one builder for two `add*` calls would
   retro-mutate the first-built path; under the immutable design a reused
   builder yields INDEPENDENT sibling paths. Every runtime seat is
   single-shot (census § 16b — fresh builder per construction) and the
   builder is unnameable outside the package: zero possible callers. The
   new semantics is documented in-source and pinned by the lock battery.

## BC Story

**Zero public-API delta, by construction:** every touched type/member is
package-private or private-nested — `MapperPath` package-private,
`PathElement` private-nested, the builder nested inside the package-private
class; `Mapper`, `Mapper.Path`, `MapperS`/`MapperC`/`MapperBuilder`,
`AbstractMapperItem`, `MapperItem` signatures byte-stable. Receipts: the
FULL-jar `javap -public` sweep (227 classes) shows exactly 3 changed rows,
ALL members of the package-private family (the old `public
MapperPath(MapperPathBuilder)` ctor removed; the builder ctor re-typed
`List<PathElement>` → `MapperPath` — both unnameable outside the package);
the API-SCOPE diff (195 public-reachable classes — public own declaration
AND public enclosing chain; 1,859 surface lines each) is EMPTY
(`target-550-abi-publicdiff-apiscope.log`) — japicmp clean by construction;
`RuntimeSurfaceLockTest` (the U018 ABI floor) green. No lineage type is
Serializable — the persistence channel stays closed. Thread-safety:
immutability is kept and benign-race memoization added (String and
final-field-chain values only). The one semantic-delta class is exception
TIMING (the § 14d note verbatim): the conversion moves from construction to
first read; guava cannot throw for inputs the eager path accepted. A path
node pins its parent chain — which the items already pin via `parentItem`,
so no lifetime is extended.

## How to use

Nothing to do — the change is internal to the runtime and transparent to
generated code and external callers. `Mapper.getPaths()` /
`Mapper.Path` semantics are unchanged for every caller: the same rendered
names, getters, full paths, ordering and equality the eager build produced.

## Migration

None. No API change, no behavioural change at any corpus-reachable
observation seat, no generated-code change (the byte rings held exactly:
OFF `e1815d1e` / ON `bd0287c3` re-derived at PR #550).

## Test coverage

- **The § 14f/§ 16f before/after text-capture instrument** (`PairHarness`,
  `-Dequiv.textCapture=<dir>` — REUSED VERBATIM from U020): every O2
  validation line, both sides, embedding the failure text whose DRR lines
  render `getPaths()` with converted attribute names. BEFORE (pre-edit
  runtime) vs AFTER ×2 (edited runtime): all 8 capture files byte-identical
  across the five cells (cdm5 `70299af8` / cdm6 `7cbf4b07` / drr `9c61e8bd`
  / iso `721a6b80` / fpml `5eb3b3a1`); 478,846 capture lines across the 8
  files (334,812 across the five distinct cells), the failure subset 60,328
  / 41,246 (machine-recounted `|false|` rows, non-vacuous). Receipts
  `target-550-textcap-{before,after,after2}` +
  `target-550-equivcap-*.log` (22/0/0/0 each).
- **The standing lineage pin battery**: `MapperTest` (13; functions package)
  untouched-green — 13 reads on EACH of getPaths/getErrorPaths/getErrors/
  getParent/getParentMulti asserting rendered path text via
  `Mapper.Path::toString`.
- **The U021 contract locks**: `MapperPathDeferredLineageTest` (11;
  rune-runtime, same package) — per-field deferral-until-read (getFullPath
  must NOT force the name conversion), memoize-once identity (name /
  getterAndContext / the flat view), the eager-twin equivalence battery over
  root/null/hop/list-index/deep shapes (expectations recomputed in-test with
  the pre-deferral derivations, incl. the EXACT three-term hash replay),
  eager-semantics equality/ordering pins (converted-name ordering, the
  indexed-before-unindexed rule, the exhausted-left comparator corner), the
  cache-vs-guava witness battery (multi-capital `getID`/`getIDNumber`,
  digit-bearing, the bare-`get` edge, non-get pass-through, the
  shared-cache-instance pin), the benign-race convergence smoke, the
  through-`Mapper.Path` public-interface observation pin, and the builder
  single-shot independence corner. Runtime suite **110/0/0/0**.
- **The differential + smoke gates**: rune-equivalence 22/0/0/0 ×3 on this
  session's legs (the two capture legs + before), O2 byte-compare green over
  the full pair surface on the edited runtime.
- **The rings**: gensuite 3,720/0/0/53 with the 56-line OFF channel
  `e1815d1e` EXACT; the ON probe 55/55 with the 169-line channel `bd0287c3`
  EXACT — zero emitter drift, as a runtime-only change must show. Parser
  4,194/0/0/5 · rune-ir 519/0/0/4 · rune-ir-java 108/0/0/0 · plugin
  18/0/0/0 · optsuite 11/0/0/0 ×2 · benchmarks 7/0/0/0.
- **The leg-jar digestdiff**: 6,462 class entries per shade jar, class-set
  identical, content diff EXACTLY the MapperPath family (3 class files) and
  zero others (`target-550-legjar-digestdiff.log`; the runtime-jar twin:
  227 entries, same three).
- **The paired same-session 6-fork JMH A/B** (OLD vs NEW runtime, both legs
  the reference tree, workload census `survivors=23` echoed 6/6 forks both
  legs; `target-550-jmh-drr-{oldrt,newrt}6f1.log`): time 2,617.369 ±
  228.362 → 2,031.881 ± 127.596 ms/op (**−585.488 ms/op = −22.4%, 1.29×**),
  CI-disjoint (the effect = 2.56× the old-leg CI / 4.59× the new-leg CI;
  not iteration-disjoint — old min 1,878.068 vs new max 2,340.940);
  allocation 10,096.20 ± 15.69 → 8,271.87 ± 2.11 MB/op (**−1,824.32 MB/op =
  −18.1%**), massively disjoint. The § 16f(6) JFR arbitration (1 fork,
  5w+15m per leg) explains the below-floor landing: the lineage family's
  charged share 33.5% → 22.7% — the copy machinery collapsed (24.3 → 4.3%)
  and the eager derivations vanished (9.0 → 0.6%) while the residual O(1)
  cons-node construction charges under bare `MapperPath` (0.2 → 17.8%) —
  B2 is a replace-shape lever, so only the removed slice was recoverable
  (census § 16g).

## Coverage

All five corpus cells (cdm 5.38.0 · cdm 6.20.6 · drr 6.34.1 · iso20022
1.38.0 · rune-fpml 2.0.0), both generation routes (reference + optimised
trees ride the same runtime), every producer seat (the per-hop builders +
the roots — all runtime-internal, census § 16b), every observation channel
of the lineage (§ 16a closed enumeration — the paths-out channels, the
path→text channels, `getPath`, the dormant compareTo, the debug renders,
the unreachable path-injection overload, serialization closed).

## Cross-references

- The census + design SOT: `research/p3-navigation-family-census.md` § 16
  (§ 16a channels · § 16b producers · § 16c readers · § 16d window+pricing
  · § 16e design · § 16f proof plan · § 16g the landing).
- U018 (fork-owned runtime) — the module this lands in; the ABI posture it
  preserves.
- U020 (lazy comparison-failure text) — the sibling runtime-lane deferral
  whose lazy suppliers are now the ONLY corpus-reachable path readers, and
  whose § 14f capture instrument this PR reuses verbatim.
- U019 (optimised IR-routed codegen) — the phase this serves; U021 is the
  phase's second runtime-lane change, benefiting both routes.
- The phase plan: the development plan "2026-08-10-p3-optimised-codegen-phase-plan"
  § 7/§ 8.
- BC layer pairing: `docs/bc-verification.md` § Per-feature locks (U021
  bullet).
