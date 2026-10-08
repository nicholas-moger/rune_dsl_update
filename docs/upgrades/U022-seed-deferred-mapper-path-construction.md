---
id: U022
subject: Seed-deferred MapperPath construction in the fork-owned runtime — the item stores a package-private path seed (Class ref / name literal + int listIndex) in place of the built node; getPath() builds memoized on first observation; the error-share/upcast corners seed-shared; every corpus-reachable observation byte-identical
since_fork_version: "0.1.0"
since_pr_at_spec: 552
since_pr_actual: 553
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: []
related_parity_constraints: []
doc_path: U022-seed-deferred-mapper-path-construction.md
---

# U022 — Seed-deferred MapperPath construction: path nodes built only on first observation

> **Era note.** Censused + designed at PR #552 (the § 16g residual
> observation-seat census, `research/p3-navigation-family-census.md` § 18 —
> the user's pick A at the post-§ 17 menu); the runtime edit landed at
> PR #553 (v3 PR-17 — the user's pick A at the post-§ 18 menu) under the
> § 18e proof set. The THIRD runtime-lane change of the v3 optimised-codegen
> phase (the U020/U021 posture verbatim): it edits `rune-runtime` (U018),
> which both generation routes share, so BOTH trees benefit — deliberately
> NOT an optimised-tree emission family and takes no scoreboard family row.
> The PR #553 paired same-session 6-fork A/B measured the DRR report run
> **1.56× faster** (1,970.522 → 1,260.064 ms/op = −710.458 ms/op =
> −36.05%) with allocation down −951.95 MB/op (8,224.56 → 7,272.61 =
> −11.57%), CI-disjoint on both channels and the TIME channel
> ITERATION-DISJOINT — B2's landing was CI-disjoint only (census § 18f).

## Concept

U021 made each path node O(1) to build — but every mapper item still BUILT
its node eagerly at construction: per report-evaluation op the runtime
constructed ~18.0M path cons nodes (ref tree) / ~17.9M (opt), and the
PR #552 counting-probe census measured the observed closure at **ZERO on
both trees** — not one flat view materialized, not one path handed out,
all sixteen observation seats silent (§ 18b; positive-control-verified,
arithmetically exact). U022 moves node construction itself behind the
observation seam: the item stores a package-private SEED in place of the
built `MapperPath` — root seats keep the `Class` ref (or the Null marker),
hop seats keep the `NamedFunction` name literal + the int list index
riding the public `parentItem` channel, sharing seats keep a ref to the
item whose node they share — and `getPath()` (unchanged signature) builds
the chain on first call, memoized benign-race. Paths nobody observes are
never built at all; paths anybody observes render character-identical to
the eager build, on the identical shared-node object graph.

## Why

After the U021/B2 landing, the § 17 re-composition sized the residual: the
bare-`MapperPath` per-hop cons construction still charged 15.8/15.1% of
samples (the family 19.1/18.2%) — the largest addressable mechanism left
on the board shy of decline-locked territory. Law 40 (the
replace-shape-vs-delete-shape floor lesson) gated any pricing on the
observed fraction FIRST — and the § 18 census returned the strongest
possible answer: the fraction is zero, making deferral-until-observed
DELETE-shape for the lane workload's entire construction population. The
five mechanistic gates behind the zero (NoOp output validation ·
boolean-only condition validation · U020-gated failure text · the
passing-path constant-fold · the workload's probe filter) are censused at
§ 18a/§ 18b; the § 18e proof set keeps every text-rendering channel
byte-identical so the deferral is safe at ANY observed fraction.

## Structural changes

All in `rune-runtime`, all inside the mapper package (six classes + one
new package-private nested class); zero emitter edits, zero
generated-code changes:

1. **The seed core** (`AbstractMapperItem`): the final `MapperPath path`
   field becomes the seed pair — `Object pathSeed` + `int pathListIndex`
   (−1 = no list index) — plus a non-final memoized `path`. `getPath()`
   builds on first call from the seed, discriminated by runtime type:
   `Class` → root node (the `getSimpleName` call now runs at observation,
   not construction); the NULL marker → the `"Null"` root; `String` →
   the parent item's node extended by one hop (the parent rides the
   public `parentItem` channel, which hop seats always carry);
   `AbstractMapperItem` → the source item's own node (share);
   `ChildHopSeed` → the parent's node extended by one hop while the
   public parent channel stays EMPTY (the empty-list error corner);
   `MapperPath` → handed-in pre-built node (the legacy constructor shape,
   kept for the package-private 3-arg `MapperS.of` and in-package tests).
   `compareTo` routes through `getPath()` (the forcing seat).
2. **The producer seats re-seeded**: `MapperS.identity/ofNull/of(t)`,
   `MapperC.of(List)`, `MapperListOfLists.of` (roots — no builder, no
   node, no element, no box at construction); `MapperItem.getMapperItem/
   getCheckedMapperItem/getMapperItems` (hops — the name literal + index
   replace the eager `getPath().toBuilder().add*` build).
3. **The sharing seats re-seeded**: the three error branches share the
   SOURCE item (`getErrorMapperItem(parentItem)`) and `upcast()` shares
   `this` — on observation every sharer materializes the SAME node
   instance, exactly the eager object graph; `upcast()` no longer forces
   the path build. The empty-list corner (`getMapperItems`' else branch)
   carries the one disclosed deviation from the census sketch: its seed
   is a two-field `ChildHopSeed` holder (parent-item ref + name literal)
   because the public parent channel is empty by contract — ONE small
   allocation on that error corner, replacing the builder + node +
   element it used to allocate eagerly.
4. **Semantics preserved**: chain identity survives (children build via
   the parent item's memoized node); value equality/ordering/hash are
   pure functions of content, unchanged; `getParentItem()` behavior is
   untouched (the share-ref is a package-private field, not the public
   parent channel); the memo is benign-race (a pure function of finals —
   a lost race builds an identical chain, and `==` over path-typed
   operands has zero sites in runtime main, § 18a).

## BC Story

**Zero public-API delta, by construction:** every touched constructor is
package-private; the one new type (`AbstractMapperItem.ChildHopSeed`) is
a package-private nested class. Receipts: the API-SCOPE javap diff (195
public-reachable classes, 1,859 surface lines per jar — the same figures
as the U021-era receipt) is EMPTY (`target-553-abi-apiscopediff.log`);
the FULL-jar `javap -public` sweep shows exactly ONE new row — the
package-private nested seed class (227 → 228 entries); the runtime-jar
and shaded leg-jar digestdiffs bound the content delta to EXACTLY the
six touched mapper classes + that nested class, zero others
(`target-553-digestdiff.log`; re-derived on the shipped post-hardening
jars with identical bounds — `target-553-digestdiff2.log` +
`target-553-abi2-apiscopediff.log`, EMPTY again);
`RuntimeSurfaceLockTest` (the U018 ABI floor) green. No seed or path type is Serializable — the persistence
channel stays closed. Thread-safety: immutability kept for all seed
fields (finals); the memo is the established benign-race idiom (U021's
`pathElements`, U020's `getError`). The semantic-delta classes, both
disclosed and pinned: (i) exception/cost TIMING — `Class.getSimpleName`
and the O(depth) node build move from construction to first observation
(paid zero times in the lane workload; byte-identical text when paid, per
the capture gate and the U022 qualification pin); (ii) LIFETIME — an
error item's seed pins its source ITEM (previously only the source's
path node); items are evaluation-scoped and already pin parents via
`parentItem`, so no new lifetime class arises.

## How to use

Nothing to do — the change is internal to the runtime and transparent to
generated code and external callers. `Mapper.getPaths()` / `Mapper.Path`
semantics are unchanged for every caller: the same rendered names,
getters, full paths, ordering and equality the eager build produced, on
the same shared-node object graph.

## Migration

None. No API change, no behavioural change at any corpus-reachable
observation seat, no generated-code change (the byte rings held exactly:
OFF `e1815d1e` / ON `bd0287c3` re-derived at PR #553).

## Test coverage

- **The § 14f/§ 16f before/after text-capture instrument** (`PairHarness`,
  `-Dequiv.textCapture=<dir>` — REUSED VERBATIM): BEFORE (pre-edit
  runtime) vs AFTER ×2 (seed runtime): all 8 capture files byte-identical
  across the five cells, and the five distinct-cell shas equal the
  #550-era baseline EXACTLY (cdm5 `70299af8` / cdm6 `7cbf4b07` / drr
  `9c61e8bd` / iso `721a6b80` / fpml `5eb3b3a1`); 478,846 capture lines
  per leg (334,812 across the five distinct cells), the failure subset
  60,328 / 41,246 (machine-recounted, non-vacuous —
  `target-553-capcount.txt`); equivalence 22/0/0/0 ×4 (the fourth the
  post-hardening after3 witness — 8/8 byte-identical again on the
  shipped jar).
- **The standing pin batteries untouched-green**: `MapperTest` (13) +
  `MapperPathDeferredLineageTest` (the 11 U021 locks) +
  `RuntimeSurfaceLockTest`.
- **The U022 contract locks**: `MapperPathSeedDeferralTest` (10;
  rune-runtime, same package) — the never-observed item builds NO node
  (every producer seat, via the package seam), observed rendering ≡ an
  eager `MapperPathBuilder` twin per channel (getPaths/getErrorPaths/
  getErrors/toString on both MapperS and MapperC + every `Mapper.Path`
  accessor), the TWO sharing shapes (the error item's node IS the source
  item's node instance; the empty-list child hop extends the parent's
  chain instance-shared while the public parent channel stays empty),
  the upcast share (same node instance, no forced build), memoize-once
  identity, the compareTo forcing pin, the benign-race convergence
  smoke, the QUALIFICATION-channel pin (`QualifyResult`'s
  unconditional `getError()` over a path-bearing
  `successEmptyOperandLazy` carrier — the § 18b workload-honest
  corollary's seat — renders the eager text byte-identically), and the
  pre-built legacy-channel pin (a handed-in node served unchanged
  through the 3-arg `MapperS.of`, closing the last `buildPath` branch).
  Runtime suite **120/0/0/0**.
- **The rings**: gensuite 3,720/0/0/53 with the 56-line OFF channel
  `e1815d1e` EXACT; the ON probe 55/55 with the 169-line channel
  `bd0287c3` EXACT — zero emitter drift. Parser 4,194/0/0/5 · rune-ir
  519/0/0/4 · rune-ir-java 108/0/0/0 · plugin 18/0/0/0 · optsuite
  11/0/0/0 ×2 · benchmarks 7/0/0/0 ×2.
- **The paired same-session 6-fork JMH A/B** (OLD = B2/U021 runtime vs
  NEW = seed runtime, both legs the reference tree, `survivors=23`
  echoed 6/6 forks both legs;
  `target-553-jmh-drr-{oldrt,newrt}6f1.log`): time 1,970.522 ± 90.017 →
  1,260.064 ± 21.444 ms/op (**−710.458 ms/op = −36.05%, 1.5638×**),
  CI-disjoint (7.89×/33.13× the leg CIs) AND ITERATION-DISJOINT
  (old-min 1,577.939 > new-max 1,358.119; B2's landing was CI-disjoint
  only);
  allocation 8,224.56 ± 31.21 → 7,272.61 ± 6.36 MB/op (**−951.95 MB/op
  = −11.57%**), CI-disjoint. The landing arbitrates law 40 in the
  overshoot direction: 2.28×/2.39× the § 17b charged residual
  (15.8/15.1%) — a delete-shape lever's charged share is a floor, and
  sampling grain under-prices constructor-dominated work (census § 18f).

## Coverage

All five corpus cells (cdm 5.38.0 · cdm 6.20.6 · drr 6.34.1 · iso20022
1.38.0 · rune-fpml 2.0.0), both generation routes (reference + optimised
trees ride the same runtime), every producer seat (8 roots + 4 hops),
every sharing seat (the 4+1 inventory: the three parent-shares, the
empty-list child hop, `upcast`), every observation seat (the sixteen-seat
inventory of § 18a — the seventeen methods funneling through
`pathElements()` — plus the qualification channel's unconditional
reader).

## Cross-references

- The census + design SOT: `research/p3-navigation-family-census.md` § 18
  (§ 18a the seat inventory · § 18b the observed fraction · § 18c the
  seed design · § 18d the law-40 pricing · § 18e the proof plan ·
  § 18f the landing).
- U018 (fork-owned runtime) — the module this lands in; the ABI posture
  it preserves.
- U021 (deferred MapperPath lineage) — the sibling deferral one layer
  UP (the node's flat view + element derivations); U022 defers the node
  chain itself, completing the construction-side deferral stack.
- U020 (lazy comparison-failure text) — the observation-side gate whose
  lazy suppliers keep the corpus-reachable reader set empty.
- U019 (optimised IR-routed codegen) — the phase this serves; U022 is
  the phase's third runtime-lane change, benefiting both routes.
- The phase plan: the development plan "2026-08-10-p3-optimised-codegen-phase-plan"
  § 7/§ 8.
- BC layer pairing: `docs/bc-verification.md` § Per-feature locks (U022
  bullet).
