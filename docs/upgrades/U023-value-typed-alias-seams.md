---
id: U023
subject: Value-typed alias seams on the optimised route — the § 6.3 alias re-typing program's emitter tranches; T1 re-types the 231 whole-body-one-chain members' protected-abstract seams from MapperS/MapperC to bare T / List<? extends T>, emits ladder-value bodies, bridges every Mapper-consuming seat via the structural MapperS.of/MapperC.<X>of wrap, and lands the alias-rooted § 9a ladder at operation-seat chain roots; T2 extends the flip to the 587 chain-headed + APPLY-headed members — the direct call composition at every APPLY body, the disclosed boundary fallback at every chain-headed body, two element/cardinality strip guards, and T2 chain roots kept on the bridge; T3 extends it to the 720 CONDITIONAL-headed + tail-kind members — the decorated return/switch ladders with per-rung value decoration and § 2 value empties, the decorated then-hoist, DIRECT ctors/collapses, the per-kind boundary fallback, and three further consumer guards (the ctor bridge-cardinality read, the ADD-seat refs merge, the evaluate-arg single→list lift); T4 completes the flip population with the 220 careful-class members — the dispatch case units atomic by construction on the dispatch-base thread, the 29 D5 rows on render-authority (the seam string alone is the S/C bit), the 79 oracle-live D4 rows with their disguised chains converting at operation seats — zero new emission code, zero new guards
since_fork_version: "0.1.0"
since_pr_at_spec: 556
since_pr_actual: 558
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: []
related_audit_hooks: []
related_parity_constraints: []
doc_path: U023-value-typed-alias-seams.md
---

# U023 — Value-typed alias seams: the § 6.3 re-typing program's emitter tranches (T1: whole-body-one-chain · T2: chain/APPLY-headed · T3: conditional + tail kinds · T4: the careful classes)

> **Era note.** The census landed at PR #554 (`research/p3-navigation-family-census.md`
> § 19 — the population 1,820 declarations / 1,819 Mapper-typed protected-abstract
> seams / 651 generated class files), the program plan at PR #556
> (the development plan "2026-08-12-p3-63-alias-retyping-program-plan" — the user's pick A
> at the post-§ 20 menu), the T0 instruments at PR #557 (the per-member disposition
> map + the golden-invocation oracle + the O5 protected channel), T1 — this
> manifest's first emitter tranche — at PR #558, T2 — the second, extending this
> manifest's scope per the ladder convention — at PR #559, T3 — the third,
> extending it again — at v3 PR-24, and T4 — the fourth and LAST emitter tranche,
> the careful classes — at v3 PR-25 (landed tranches T1∪T2∪T3∪T4 = 1,758 of 1,820
> = the FULL flip population; the 62 D1/D1T/D3 exclusions are the entire residue).
> **An API-QUALITY feature: no PR in the program makes a
> workload perf claim** (the § 19e/§ 20d verdict rides every tranche; JMH ships as
> measured-and-reported capture only). T5 — the program close — landed at
> v3 PR-26: the aggregate O5 report, the 62-row residual census (per member,
> with reasons) and the consolidated migration notes published at
> the development audit "2026-08-13-p3-63-alias-retyping-o5-report", with the T4
> generated-body emission witnesses folded into the emission gate (the #561
> Rule-6 N1 follow-up). **THE PROGRAM IS COMPLETE.**

## Concept

Every rune alias (`RShortcut`) renders today as a Mapper-typed protected-abstract
seam — `protected abstract MapperS<? extends T> aliasName(<inputs>)` (`MapperC`
for multi) — with an `@Override` implementation whose body is the compiled Mapper
expression, re-invoked per consumer reference (call-by-name). U023 re-types
eligible members ON THE OPTIMISED ROUTE ONLY to the value seam the census § 19b
named: `protected abstract T aliasName(<inputs>)` / `List<? extends T>`, with the
body emitted as a plain value expression and every in-file consumer seat re-emitted
per the ratified consumption-form table (plan § 5). The T1 tranche covers the
whole-body-one-chain pool — the 231 members (52 cdm5 / 136 cdm6 / 43 drr) whose
body is a pure PARAM-rooted navigation chain — machine-derived from the T0
disposition map and reproduced per-row by the emitter's own flip policy
(`AliasValueSeamPolicy`), with the map test asserting policy ≡ map on every alias
row of every cell. The T2 tranche (PR #559) extends the same predicate to the
chain-headed + APPLY-headed pool — the 587 members (173/269/145) whose adapted
body tops at APPLY, FIELD_ACCESS or META_ACCESS without being a whole chain —
the belt then asserting policy ≡ the map's T1∪T2 partition. The T3 tranche
(v3 PR-24) makes the body-form leg TOTAL over adapted bodies — the 720
CONDITIONAL-headed + tail-kind members (182/318/220), every remaining top kind
admitting (the adaptEmpty refusal stays the corpus-zero twin belt). The T4
tranche (v3 PR-25) lifts the three careful-class refusals — the dispatch case
units (112, atomic BY CONSTRUCTION: one `AliasModel` renders the abstract seam
AND the `Default` override; the variant's facts thread the dispatch BASE), the
29 D5 rows (RENDER-AUTHORITY: the seam string alone is the S/C bit) and the 79
oracle-live D4 rows (every enumerated seat on the standing forms; the
whole-chain subset's disguised chains convert at operation seats) — the belt
asserting policy ≡ the map's T1∪T2∪T3∪T4 partition; the landed total is
1,758 of 1,820 = the FULL flip population.

## Why

The census § 19 re-priced the alias family as an API-QUALITY road: the runtime
lane (U020/U021/U022) had already collapsed the per-seat headroom ~100×, law-32
arithmetic closes both workload channels, and § 20d confirmed it compositionally —
what remains is CODE SHAPE: value-typed seams a subclass implementer reads and
overrides directly, direct-ladder bodies without `NamedFunctionImpl`/Mapper hops,
and the 873 alias-rooted chains freed to convert (deliverable 3 — a chain could
not ladder while its root was a Mapper-typed seam). The program publishes the
protected-surface delta per tranche through the T0 O5 channel
(REQUIREMENT-1: measured and reported, never a gate term).

## Structural changes

The byte-inert shared seams (rune-java-generator — the reference route's behaviour
is unreachable-different by construction; the rings prove the bytes):

1. **`ExpressionCompiler.aliasValueSeamOrNull`** (+ the `AliasValueSeam` record) —
   the invocation-seat query; the base implementation is a constant null.
2. **`FunctionGenerator.aliasValueFormOrNull`** (+ the `AliasValueForm` record) —
   the `compileAliases` flip seat; base constant null. The per-shortcut loop reads
   the hook once and carries a `returnType` local (the flipped form replaces the
   return type and the facet-chain body wholesale; the name-disambiguation,
   signature-ref union and `AliasModel` construction run unchanged for both
   shapes).
3. **`ReferenceHandler`'s alias arm** — when the compiler's seam query answers,
   the invocation wraps in the STRUCTURAL `MapperS.of(...)` /
   `MapperC.<X>of(...)` bridge (`wrappedInMapperSOf` / `wrappedInMapperCOfSingle`)
   instead of rendering the bare Mapper-returning call. The structural wrap's
   `unwrapToBuilder` channel restores the bare value at the evaluate-arg /
   assignment / addAll strip seats — the § 5 CALL_ARG and BODY_ROOT value forms,
   wrap+unwrap round-trip deleted — and every other consumer composes Mapper
   machinery over the wrap text exactly as it composed over the Mapper-returning
   call (the disclosed bridge; operand runtime class MapperS/MapperC preserved per
   the wrap-class clause). All invocation routes reach this single arm — genuine
   symbol references AND the disguised `REnumValueRef` heads (their synthesized
   receivers re-enter the same arm), so the seam and every seat agree by
   construction.

The optimised-route family code (rune-ir-java-optimised):

4. **`AliasValueSeamPolicy`** (new) — the per-alias flip predicate reproducing the
   T0 map's `assignTranche` T1 outcome from generation-visible channels:
   whole-body-one-chain (the shared `NavigationChainClassifier`) · ¬D3
   (usesOutput) · ¬dispatch · ¬D5 (the render-channel seam's S/C bit — the recorded form since PR #612 — must
   agree with the adapted body's IR cardinality) · ¬D1/¬D1T (the only-exists
   ancestor rule + the alias→alias fixpoint, both mirrored from the map's walks) ·
   ¬D4-live (zero references on BOTH counting channels AND a disguised
   `REnumValueRef` head present → the member stays Mapper-typed for T4) · the
   multi boundary witness present and same-walk-consistent. Carries per member:
   the value return-type string (since PR #612 built from the seam FACTS the render
   channel emitted — sentinel elements ride verbatim), the multi element text +
   witness class, the ladder plan, and the seat-invocation spelling (derived from
   the same public authorities the invocation seat consults:
   `FunctionDependencyCollector.hasFunctionDependencyNamed` numbering,
   `HandlerHelper.staticOperatorMembersUsed` escape, declaration-order raw input
   names with the synthesized placeholder filtered).
5. **`OptimisedExpressionCompiler`** — overrides the seam query (window-gated, so
   the dependency collector's never-windowed instance keeps reading reference
   shapes); admits ALIAS_CALL-rooted chains into `convertOrDelegate` through the
   classifier's opt-in overload and swaps SINGLE/TERMINAL_MULTI shapes for the
   alias-rooted § 9a ladder (`MapperS.of((aliasName(inputs) == null ? null :
   aliasName(inputs).getX())).filterSingle(sameNav -> true)` — the ladder roots at
   the invocation and repeats it per null-guard level, the ratified § 5 CHAIN_ROOT
   form; call-by-name is preserved per REFERENCE, and a T1 body is a pure getter
   ladder); STREAM shapes stay on the bridge composition (counted). All alias-root
   counters are DISJOINT from the landed family tuples — the § 9b/§ 10b/§ 12
   receipts stay byte-comparable.
6. **`NavigationChainClassifier`** — the 4-arg `classify` overload with the
   `AliasRootAdmission` channel (+ `RootKind.ALIAS_CALL`); the 3-arg entry the
   census and map instruments call delegates with a null admission and behaves
   byte-identically (NAVCENSUS and the map TSVs proven unchanged).
7. **`OptimisedFunctionGenerator.aliasValueFormOrNull`** — the flip emission:
   SINGLE bodies return the parenthesized null-guarded ladder; TERMINAL_MULTI
   bodies return the guard structure with `Collections.<X>emptyList()` in every
   else seat (the § 2 empty contract — a multi seam returns the empty list for
   absent, matching `getMulti()`'s observable); STREAM bodies keep the reference
   Mapper chain and value-unwrap once at the boundary
   (`return (<chain>).getMulti();` — the § 4 DISCLOSED fallback, counted per
   cell: 7/12/1 members).

The T2 extension (PR #559 — the same seams, three additions):

8. **The policy's T2 legs** — where the classifier returns no ladder plan, the
   adapted body's TOP kind admits the member exactly when it is APPLY,
   FIELD_ACCESS or META_ACCESS (the map's `assignTranche` T2 leg mirrored over
   the same adapted channel; every other kind is a later tranche's member); the
   multi boundary witness then derives from the adapted TOP node (`IRExpr.type()`
   + the classifier's `metaKindOf` discriminant, package-shared — never
   reimplemented) with the same-walk name agreement belted. `FlipFacts.plan` is
   null for a T2 member and `aliasRootLadderEligible` refuses — **T2 chain-root
   seats ride the BRIDGE, never the ladder** (the § 9a ladder re-invokes its
   root k+1 times per guard: the T1 getter-repeat class, not the
   dependency-call class; the alias-root pins holding at the T1 values are the
   mechanised proof).
9. **The value-body emission channel** (`FunctionExpressionRenderer`, additive
   and reference-unreachable): `renderAliasValueOrNull` compiles the body once
   and answers structurally — a zero-arg no-real-input call or a bare
   structural Mapper-of wrap is DIRECT (the § 4 direct call composition; the
   wrap's own `unwrapToBuilder` invariant is the discriminator — the channel is
   set only on un-appended wraps), anything else is the Mapper expression the
   emitter boundary-unwraps (`return (…).get()/.getMulti();` — the disclosed
   fallback); the decl-led lifted/sink renderers gain boundary-DECORATED
   overloads (`AliasValueBoundary`; null = the standing byte-identical
   behaviour) so hoisted bodies keep their decls verbatim and re-shape only the
   trailing return. T2 landed forms: DIRECT 414 = every APPLY-headed member
   (122/201/91; 9 hoist-carried), BOUNDARY 173 = every chain-headed member
   (51/68/54; 6 hoist-carried).
10. **Two strip guards** where the T1 strips needed element/cardinality
    awareness — both ALIAS-WALK-gated, two projections of the same
    `FunctionAliasHelper` walk, so wrap-present ∧ walk-resolves is satisfiable
    by the bridge exclusively (reference bytes inert BY CODE, ring-proven):
    (a) the evaluate-arg WRAPPER guard (`ReferenceHandler`), reading the
    TYPED #347-F5 channel (`NavigationHandler.tryAliasReceiverMapperType` —
    the coercion arm's OWN read, so guard ≡ arm by construction) — a multi
    META-topped member's arg at a meta-free param keeps the bridge and
    composes the standing #347-F5 wrapper→value `"Type coercion"` map (a
    bare pass hands `List<Wrapper>` where `List<? extends Value>` is
    expected — compile-loud); (b) the ADD-seat single-lift guard
    (`FunctionExpressionRenderer`), reading the render-channel alias seam (the typed facts since PR #612; the seam-string
    walk (`NavigationHandler.aliasSeamOrNull` since PR #612, which returns the typed `AliasSeam`; born here as `aliasSeamMapperReturnTypeOrNull`, the seam-STRING channel, deleted at PR #612 — the
    § 3 S/C authority, which unlike the typed channel answers for
    model-item aliases too, the #352 boundary) — a SINGLE-seamed member at
    a whole-output ADD keeps the bridge + the `.getMulti()` single→list
    lift (null → the EMPTY list, never a singleton-of-null — the
    Create_Exercise runtime-loud catch).

The T3 extension (v3 PR-24 — the same seams, four additions):

11. **The policy's body-form leg TOTAL + the witness reconstruction** — the
    `plan == null` arm admits EVERY adapted top kind (the map's `assignTranche`
    fall-through leg; an un-adapted body still refuses via the ¬D5 twin,
    corpus-zero); the multi witness gains the TYPE-level meta channel
    (`RMetaAnnotatedType.metaAttributes()` through the same shared `metaKindOf`)
    and the IR-BLIND reconstruction — where inference types a lambda element at
    the bare VALUE while the seam names the wrapper, the witness wraps the IR
    value class through the same `RJavaWithMetaValue` factory and admits ONLY
    on an EXACT seam-element name match (the MapLegalEntity class; all 102 T3
    multi rows resolve).
12. **The decorated ladder channels** (`FunctionExpressionRenderer`, additive):
    `AliasValueLadderForm` threads through `renderAliasReturnLadderOrNull` /
    `renderAliasChoiceSwitchLadderOrNull` (+ the private ladder recursion) —
    each rung's return value re-shapes (a `MapperS.of` lift INVERTS by
    construction equality; a structural wrap unwraps; every other rung takes
    the paren boundary) and the empty terminals take the § 2 value empties
    (`null` / `Collections.<X>emptyList()`); the conditions and hoists render
    byte-identically. The then-hoist and coerce paths gain decorated overloads
    (the F4 collapse arms become value forms; the ctor arm returns the bare
    ctor DIRECT, the ComparisonResult arm `(…).get()` — `ComparisonResult`
    implements `Mapper<Boolean>`); the sink's item-top arm renders the bare
    collapse verbatim under a boundary; the item-typed-top plain arm lands
    DIRECT.
13. **Three further consumer guards** (each a differential-gate compile catch):
    (a) the CTOR bridge-CARDINALITY guard (`ConstructionHandler`) — a
    flipped-alias ctor value's cardinality authority is the SEAM's S/C bit via
    the NEW NON-COUNTING `ExpressionCompiler.aliasValueSeamIsMultiOrNull`
    channel (the workspace inference is blind on some conditional-bodied alias
    references; the corrected MULTI arm splices `new ArrayList(<bare List>)` —
    Create_QuantityChange.newTradeLots); (b) the ADD-seat metaWrapCoerce REFS
    MERGE (`FunctionExpressionRenderer`) — the #364 arm now merges the bridged
    value's refs so the `MapperS` import survives in a file whose only use is
    the bridge (drr EnrichReportableEventWithUpiForSwaption); (c) the
    EVALUATE-ARG single→list lift (`ReferenceHandler`) — a SINGLE-seamed
    bridge at a MULTI callee param keeps the bridge + `.getMulti()` (the
    ADD-seat guard's twin on the same alias-seam channel (typed since PR #612); drr
    IsActionTypeMODI.afterTradeStateOpen).
14. **T3 chain roots stay on the BRIDGE** — `aliasRootLadderEligible` still
    requires a T1 ladder plan; the alias-root pins holding UNCHANGED
    (38/118/127 + declines 2/5/5) mechanise the k+1 decision at T3 grain.

The T4 extension (v3 PR-25 — the same seams, refusal-leg lifts ONLY; zero new
emission code, zero new guards):

15. **The three careful-class admissions** (`AliasValueSeamPolicy`): the
    dispatch refusal lifts with the DISPATCH-BASE THREAD — a variant's
    invocation args and escape belt walk the BASE's declared inputs via
    `HandlerHelper.dispatchBaseOf` (the `renderEnclosingInputs` /
    `buildStandardModel` signatureSource law) while the dependency/operator
    collision laws stay on the VARIANT; the D5 refusal lifts to
    RENDER-AUTHORITY — `isMulti` reads the seam's recorded form alone (the seam string until PR #612; the § 3 shipped
    truth; the disagreeing IR bit recorded, never consulted); the D4-live
    refusal lifts on the oracle receipt (law 48) — the now-dead counting
    channels prune from the policy (the map keeps them as the receipt
    authority). The dispatch unit's atomicity holds BY CONSTRUCTION (one
    `AliasModel` → both the abstract seam and the `Default` override; javac
    enforces the pairing) and the O5p channel attributes per CASE UNIT through
    the nest-prefixed signatures.
16. **The T4 whole-chain rows ladder; the value-body rows bridge** — a
    T1-shaped careful body (the cdm6 `cdm/ingest/**` D4-live class dominantly)
    is `aliasRootLadderEligible` under the SAME T1 law, and its DISGUISED
    consumer chains (the walk's disguised-head resolution) convert at
    operation seats: the alias-root pins re-derive 38/118/127 → 38/313/127
    (+195 cdm6; declines 4/8/5) — the getter-repeat economics, mechanised in
    the emission gate's grown pins.

## BC Story

**The delta is PROTECTED-surface, optimised-route-only, and REPORTED — never
gated** (REQUIREMENT-1 revised; plan § 7): the public channel of the equivalence
gate's `ApiDeltaReport` stays ZERO on every cell, and the T0
`compareDeclaredProtected` channel publishes EXACTLY the tranche's flipped
members' signature pairs (the abstract seam + its `...Default` override per
member; a dispatch member's pair carries its CASE-UNIT nest prefix),
reconciling to the landed-tranche receipt (231 members at T1; the
T1∪T2 UNION of 818 at T2; the T1∪T2∪T3 TRIPLE-UNION of 1,538 at T3; the
T1∪T2∪T3∪T4 QUADRUPLE-UNION of 1,758 at T4) — the
membership-reconciliation belt. The reference route is BYTE-UNTOUCHED: the shared seams default to null/no-op
(the PR-7 member-injection precedent) and the rings re-derive EXACT (OFF
`e1815d1e` / ON `bd0287c3`). Entry points (`evaluate` public / `doEvaluate`
protected) and the model surface are untouched; the CALL_ARG callee side is
untouched (call sites pass values today — the strip deletes the round-trip
without touching any callee signature). Call-by-name semantics are preserved
IDENTICALLY (no caching, no memoization); empty-value semantics ride the § 2
contract (absent = `null` for single seams, empty `List` for multi seams). The
8 T3 D2 rows (condition-context members — four member pairs across both cdm
cells) armed the § 14f capture MANDATORY, and it held byte-identical ×2; the
9th and LAST D2 row (cdm6 `GetNetInitialMarginFromExposure.tradeInitialMargin`
— a T4/D5 row) armed it MANDATORY AGAIN at T4 and it held ×2 again: the
flipped condition-context members left every runtime validation text untouched
at every tranche. After T4 only D1 (only-exists participants, 61 aliases + 0
transitive) and D3 (the usesOutput seam) stay Mapper-typed verbatim per the
plan § 6 dispositions — the 62-row residue of the 1,820.

## How to use

Nothing to do for generated-code consumers — the optimised tree's function
classes keep their public API byte-for-byte. A subclass implementer overriding a
re-typed alias seam now implements the VALUE form (`T` / `List<? extends T>`)
instead of hand-building a Mapper: return the value (null = absent for single;
the empty list = absent for multi). The migration note per tranche PR lists the
flipped members with their seam-kind pairs (the O5 protected channel's published
rows).

## Migration

None for the reference route (byte-identical). For optimised-route subclassers:
the per-tranche O5p publication is the authoritative member list; re-implement
overridden seams in the value form per the § 3 table (the parameter list is
unchanged). The consolidated program-grain notes — all four tranches, the
complete 62-row residue tables and the implementer rules — live in the T5
report: the development audit "2026-08-13-p3-63-alias-retyping-o5-report".

## Test coverage

- **The policy ≡ map belt** (`AliasDispositionMapTest`): the emitter's flip
  policy must reproduce the T0 map's LANDED-tranche partition (T1 at PR #558;
  T1∪T2 at PR #559; T1∪T2∪T3 at v3 PR-24; T1∪T2∪T3∪T4 since v3 PR-25 — the
  flip predicate now TOTAL over the ¬EXCLUDED population, with the per-row
  cross-checks re-keyed to the wholeChain⟺plan mirror and the render-channel
  S/C bit — the D5 rows' render-authority landing proof per row) EXACTLY, per
  alias row, on every cell — asserted over all
  1,820 rows; the map TSVs stay byte-identical to the T0 receipts
  (`326a3b78`/`a1dc5cc3`/`c4f7ebe2`/`cf0963d2`), proving the instrument
  undrifted while gaining the belt.
- **The emission gate** (`OptimisedNavigationEmissionTest`): the T1∪T2∪T3∪T4
  pins — seams flipped per cell **480/850/428** with the full per-kind
  body-form breakdown (the T1/T2/T3 keys carried forward at their grown
  values, the 16 T4 CONDITIONAL rows (6/6/4) + the 2 T4 SWITCH_OP rows (0/2/0,
  both cdm6) riding the T3 ladder/switch-ladder machinery — LADDER
  81/113/78 · LADDER:switch 0/13/0 — and the T4 marginal adding the
  VALUE:LIBRARY_APPLY:DIRECT/:BOUNDARY:hoist 3+3/3+3/0 ·
  VALUE:RECORD_FEATURE_NAV:BOUNDARY 12/12/0 ·
  VALUE:DEEP_FEATURE_NAV:BOUNDARY 0/1/0 keys plus the T4 whole-chain rows on
  the T1 ladder shapes — ZERO `BOUNDARY:block` anywhere, any tranche) · the
  leaf bridge wraps (C=260 S=844 / C=384 S=1694 / C=266 S=1811) · the
  alias-rooted ladder conversions **38/313/127 — held at the T1 values
  through T2 AND T3, GROWN +195 cdm6 at T4** (the T4 whole-chain careful
  rows' disguised chains convert under the T1 law; the bridge-not-ladder
  decision for value-body members stays mechanised) with the ALIAS-ROOT
  COMPLETENESS law (shortfall 0) · the STREAM declines 4/8/5 · the zero-belts
  (bails, interior suppressions, witness refusals, escape refusals all 0).
  The landed family pins (conversions 782/3,499/9,140 + every
  decline/twin/suppression tuple) held UNCHANGED through the alias
  program — the § 9b/§ 10b/§ 12 receipts byte-comparable — until the belt
  (PR #607) moved the drr 6.34.1 row by its then-body root law: conversions
  9,140 → **9,125** (9,122 since #629), itemStreamDeclines 152 → 167, itemMetaStreamDeclines
  173 → 182, itemHarvestBails 300 → 291 (fifteen list-typed `then` seats
  re-shaped STREAM and kept on the reference form; the cdm rows unmoved),
  the pins re-transcribed from the measured run. The cdm5
  witnesses: the T1 set (`ConvertToAdjustableOrRelativeDate.relativeDate`
  seam + ladder + bridge + alias-rooted ladder; the D1-excluded
  `Qualify_Shaping.instruction` Mapper seam verbatim) plus the T2 set —
  `IsWeekend.dayOfWeek1` (DIRECT, the collision-numbered name kept),
  `IsHoliday.holidays` (multi DIRECT on the bare-`MapperC<Date>` →
  `List<Date>` canonical form), `Create_Exercise.underlier` (BOUNDARY; since
  T3 bridge-composed over the flipped `optionPayout`) + its `execution`
  ADD-seat lift, `FxMarkToMarket.quotedQuantity` (the hoist decls verbatim;
  since T3 the hoist consumes the flipped `quotedCurrency` BARE) + its
  wrapper-coerced `quantities` evaluate-arg — plus the T3 set —
  `Create_Exercise.optionPayout` (the D2 conditional member: the decorated
  ladder with the Mapper-composed condition, a paren-boundary rung and the
  INVERTED `MapperS.of` lift at the only-element terminal) and
  `CashPriceQuantityNoOfUnitsTriangulation.notional` (the PIPE member: the
  thenArg decls verbatim + the boundary trailing return) — plus the T4 set,
  folded in at T5 = v3 PR-26 per the #561 Rule-6 N1 follow-up:
  `YearFraction$YearFractionACT_360` (the dispatch case unit's abstract seam
  + `Default` override re-typed TOGETHER — atomicity by construction — with
  the arithmetic consumer's bridge reading the DISPATCH BASE's declared
  input names, the Mapper-typed seam proven GONE, and the ACT_ACT_ICMA
  golden's SECOND `daysInPeriod` invocation surviving — the § 2 call-by-name
  witness), `DetermineObservationPeriod.allBusinessDays` (the D5 row's seam
  re-typed to the bare `List<BusinessCenterEnum>` on RENDER-AUTHORITY — the
  IR bit said SINGLE — with the bare-List CALL_ARG strip at its
  evaluate-arg seat) and `Qualify_InterestRate_Option_DebtOption.optionPayout`
  (the D4-live row: the wildcard multi seam, the T1 TERMINAL_MULTI
  empty-coalescing ladder body — the first cdm5 TERMINAL_MULTI landing —
  and the oracle-enumerated DISGUISED invocation riding the structural
  `MapperC` bridge).
- **The census instrument byte-stable**: NAVCENSUS 605/605 ≡ the #554 SOT (the
  classifier extension is census-inert by the null-admission default).
- **The differential gate** (rune-equivalence): O1–O4/O6/O7 zero-divergence on
  all function-bearing cells ×2; O5 public 0; O5p ≡ the LANDED tranches'
  signature pairs over the PAIRED population — at T4 the QUADRUPLE-UNION
  receipt ×2: 127/155/174 delta classes, 960/1,130/856 signature pairs =
  EXACTLY 2 per member, zero excess, the dispatch pairs attributed per CASE
  UNIT through the nest-prefixed signatures (the cdm6 `cdm/ingest/**`
  exclusion grows to 285 members
  — the fpml-dependency compile exclusion, `CorpusClasses` — proven at
  emission grain by the seam pins + witnesses); **the § 14f capture pair
  byte-identical vs the T0 baseline lineage ×2 — MANDATORY-armed at T3 (8 of
  the 9 D2 condition-context rows) AND at T4 (the 9th row, a T4/D5 member)
  and GREEN both times: the flipped condition-context members left every
  runtime validation text untouched.**
- **The golden-invocation oracle** (the map re-run): every landed member's
  enumerated invocation seats covered by the landed forms (the leaf bridge
  serves every seat by construction — all routes reach the single alias arm;
  at T4 the D4-live members' oracle rows ARE the liveness receipts, law 48).

## Coverage

The three alias-bearing cells (cdm 5.38.0 · cdm 6.20.6 · drr 6.34.1; iso20022 +
rune-fpml are alias-free), the OPTIMISED route only (the reference route is the
byte-locked parity baseline), the landed tranches T1 231 + T2 587 + T3 720 +
T4 220 = 1,758 members = the FULL flip population (the T0 map's tranche column
is the membership authority; EXCLUDED_D1 61 + EXCLUDED_D3 1 — the 62-row
residue — stay Mapper-typed by disposition; T5 = v3 PR-26 closed the program:
the aggregate O5 report + the per-member residual census + the consolidated
migration notes at the development audit "2026-08-13-p3-63-alias-retyping-o5-report").

## Cross-references

- The program plan (the ladder SOT):
  the development plan "2026-08-12-p3-63-alias-retyping-program-plan" §§ 3–9.
- The T5 program-close report (the O5 aggregate + the residual census + the
  migration notes): the development audit "2026-08-13-p3-63-alias-retyping-o5-report".
- The census SOT: `research/p3-navigation-family-census.md` § 19 (a–e), § 20d.
- The T0 instruments: `AliasDispositionMapTest` (the disposition map + the
  golden-invocation oracle) + `ApiDeltaReport.compareDeclaredProtected` (the O5
  protected channel) — PR #557.
- U019 (optimised IR-routed codegen) — the route this family lands on.
- U020/U021/U022 — the runtime lane whose landings re-priced this family to
  API-quality (census § 19d/§ 19e).
- The phase plan: the development plan "2026-08-10-p3-optimised-codegen-phase-plan"
  § 7/§ 8.
- BC layer pairing: `docs/bc-verification.md` § Per-feature locks (U023 bullet).
