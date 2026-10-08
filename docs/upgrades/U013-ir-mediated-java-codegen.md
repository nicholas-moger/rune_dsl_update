---
id: U013
subject: IR-mediated Java codegen — the flag-gated Path-2 route (the rune-ir-java module + the ServiceLoader seam)
since_fork_version: "0.1.0"
since_pr_at_spec: 51
since_pr_actual: 466
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D21", "D30", "D31", "D43"]
related_audit_hooks: ["H2", "H11"]
related_parity_constraints: ["#1", "#5", "#6", "#16", "#17"]
doc_path: U013-ir-mediated-java-codegen.md
---

# U013 — IR-mediated Java codegen: the flag-gated Path-2 route

> **Era note.** This manifest was a STUB from P2.0.1 until the D43 train's
> PR-2 (PR #466), when the route gained src/main existence: the
> `rune-ir-java` module (the lab's IR-routed generator/compiler/renderer
> subclasses, ported at the lab's HEAD `8c14592`) + the ServiceLoader seam
> in the shipping generator. The originally-sketched in-generator flag
> dispatch was INVERTED at port time: the shipping `rune-java-generator`
> carries ZERO IR imports; the route activates only when the flag is set
> AND `rune-ir-java` is on the classpath.

## Concept

An optional second generation route ("Path-2") through the Java code
generator: AST → **IR** (the `rune-ir` substrate, U012) → the legacy
render oracles → Java. It operates ALONGSIDE the standard Path-1
(AST → renderers → Java). Path-1 stays the default and is byte-identical
by construction when the route is off; Path-2 is opt-in via
`-Drosetta.generator.ir=true`. The route is a forcing function: any IR
design gap surfaces as a Path-2 byte divergence against the SAME frozen
corpus manifest the standing D11 gate uses — zero new golden
infrastructure.

Two seam families make the route reachable without shipping IR code in
the generator:

- **The SPI** (`com.regnosys.rosetta.generator.java.spi`):
  `IRGenerationProvider` (five construction seams — pojo · choice · enum ·
  metafield · function — + two dispatch seams) and `IRGeneration` (the
  flag read + the cached `ServiceLoader` lookup + static helpers that
  execute the exact legacy calls when the route is off). `rune-ir-java`
  registers its `IRGenerationProviderImpl` via `META-INF/services`; the
  lab's `instanceof IREmittable*` dispatch moved INTO that impl.
- **The render seams** (10 generator files, all Path-1-inert): overridable
  handler/compiler/renderer factories (`ExpressionCompiler.create*Handler`,
  `FunctionGenerator.createExpressionCompiler` /
  `createFunctionExpressionRenderer`), overridable hoist-name seams
  returning the legacy constants (`thenArgBaseName`,
  `ifThenElseResultBaseName`, `bigIntegerBaseName`, `booleanHoistBaseName`,
  `renderSetAssignmentStatement`), visibility widenings for verbatim
  oracle reuse (8 in `ReferenceHandler`; `NavigationHandler`'s was already
  public), the `StatementHoistSession.THEN_ARG` constant, and the
  `switchHoistCount` counter (its lab reader was deliberately not ported;
  the fork's own ON-gate reader wired at the PR-3 drift wave — the D11
  FUNCTION flow asserts it `> 0` for the cdm/6.20.6 carrier cell when the
  flag is on, via a reflective accessor hop since `rune-ir-java` is not on
  the standing test classpath).

## Why

Dogfooding the IR against the most demanding consumer (byte-parity Java
generation) pre-validates the substrate before the Python phase consumes
it (D30/D31; W22 · W26 · W28 · W37 enablers). The strangler-pattern seam
keeps the drop-in guarantee intact: the shipping generator's behavior is
unchanged unless a consumer explicitly opts in with both the flag and the
extra jar on the classpath.

## Structural changes

- NEW module `rune-ir-java` (`org.finos.rune:rune-ir-java`), depending on
  `rune-ir` + `rune-java-generator` BY DESIGN (legacy-renderer reuse —
  the D43 module family): 24 main files (the lab's 21 `generator.java.ir`
  classes ported byte-identically at PR #466 — `IRExpressionCompiler`, the
  `IR*Handler`/`IR*Generator` subclasses, the `IREmittable*` interfaces,
  the focused renderer interfaces, `IRFlag` — + the fork-authored
  `IRGenerationProviderImpl`, and since PR #469 the fork-authored
  `ImplicitItemRenderer` + `ItemNavRenderer` seam interfaces) and 4 test
  classes (3 ported + the fork-authored `IRGenerationSeamTest`; the
  lab-divergence census reads test tree = 3
  [`IRAdapterCoherenceTest` + `Wave6AnfSkeletonTest`, fork-retargeted
  onto `test-corpus/` at PR-4, + `IRExpressionCompilerTest` since PR
  #470 — its resolved-divide witness lock pinned the PIN-ERA engine-first
  arm order and is RETENSED to today's legacy (the post-pin
  nested-divide-first arm, #369)], main tree = 3 ported files diverged +
  the 2 #469 fork
  additions (a tree-diff counts 3 fork-only main files — the #466
  `IRGenerationProviderImpl` is the third): `IRExpressionCompiler` (the
  guard, below),
  `IRJavaLeafEmitter` (the two #469 renderer seams + the resolver-frame
  `swapResolvers` re-entrancy fix — an oracle recursion's nested claim
  must RESTORE the outer emission's resolver frame, never null it) and
  `CallReceiverResolver` (javadoc retense only).
  `IRExpressionCompiler` carries the fork-authored POST-PIN COERCION
  GUARD (`subtreeTripsPostPinCoercion` + its arms, superseding the lab's
  top-level-only `aliasArgRequiresMetaDeref`; since PR #469 the scan
  returns the first-tripping `PostPinArm` — the PER-ARM DECLINE METER
  behind `postPinCoercionDeclineBreakdown()`, printed per cell by the
  ON-gate reader) — the subtree-wide decline scan that keeps every IR
  claim off the shapes whose LEGACY render gained post-pin refinements.
  The arm states after the #469–#489 share-growth wave (EVERY decline
  bucket reads explicit ZERO on BOTH metered seams — FUNCTION since
  PR #473, RULE since PR #474 — the endgame state, never silent; the
  #475 census meters sit BESIDE the guard's arm meter, sizing the
  route-wide worklist the guard never sees, the #476 blocker probe
  attributes the census's adapterGap bucket to the families the
  adapter actually chokes on, the #477 gate decode names each
  blocker's declining gate inside its own adapter arm, the #478
  teach landed the first navigation arm those gates named — the
  taught `inputFeatureNav:headInput` gate reads ZERO at population —
  the #479 teach claimed the filter/extract synthetic-item
  receiver slice the implicit-root witness sized, its
  `retypeSourceOk` gate likewise ZERO at population, the #480
  arms claimed the bare-attr + chain item-nav slices the #479
  signature pre-sized, their restated `claims` buckets likewise ZERO
  at population, the #481 widening resolved the grammar-elided
  PIPED sources through the enclosing then's argument, its
  `thenArg.provable.*` pipe facet likewise ZERO at population, the
  #482 widening descended into NESTED then-pipe arguments'
  bodies, its `thenPipe.provable.typeOk` facet likewise ZERO at
  population, the #483 widening descended into extract-bodied
  pipe sources' bodies — implicit-or-paramless binders only, the
  named/identity faces declining — its `extractPipe.provable.typeOk`
  facets (both seats) likewise ZERO at population, the #484
  widening proved the bare-FUNCTION-application bodies on the
  callee's OUTPUT attribute (the callable-output identity — the
  aliasShadow precedence declining first), its
  `symbol:RFunction.<facet>` admit tokens likewise ZERO at
  population on every composing channel, and the #485 widening
  proved the rule-top elided implicits as the RULE INPUT (the
  rule-input identity — legacy's `isElidedOperandTopLevel` →
  `MapperS.of(input)` route, the checker's from-type leg; the
  ruleInputNav precedence gate refined to the top-level slice), its
  `noBinder.fromData` admit facet likewise ZERO at population with
  the ruleInputLambda pipe channel emptied whole, the #486
  decode refined the `symbolUnresolved` reason token in place —
  99.71% of its 3,921 events read the `synthetic.` mechanism prefix,
  render-time synthesized disguised-nav receivers, NOT linker gaps;
  conservation exact at both instruments — and the #487 widening
  proved the conditional-bodied sources under the JOIN law (the
  element form of `if c then A else B` is the arms' COMMON form —
  the list-literal all-provable/all-identical laws lifted to the
  two-armed choice; an empty else admits on the then proof alone,
  `DefaultElseRule`'s synthetic else read structurally empty), its
  `conditional.sameType/.elseEmpty/.formGap` admit facets likewise
  ZERO at population with the join-meet question DISSOLVED at the
  decode — `typeDiffers` read zero: no mixed-form conditional
  exists in the corpus, and the #488 decode refined the flat `argNav`
  argument-position reason token in place — every occurrence a
  navigation the arms ALREADY LOWER; param-rooted 72.9% vs
  synthetic-item 22.7% (the RULE seam's rule-input elided navs) vs
  IRApply-rooted 3.4% vs item 0.8%, every meta/alias-root drift
  detector ZERO, conservation exact at both instruments — and the #489
  L-042 revival arm then CLAIMED the decoded population whole: every
  clean face fell to ZERO, the 124-event residue reading only the
  arm's decline faces, typeGap./itemChain./itemSrcUnprovable.): **the dispatch-variant context TAUGHT at PR #470** — the
  wave's biggest arm (524 declines = cdm5 262 + cdm6 262, the whole
  variant context wholesale) closed in two decoded faces: the alias-call
  arg list forwards the dispatch BASE's declared inputs
  (`aliasCallInputNames`' base swap — the same `dispatchBaseOf` oracle
  legacy `renderEnclosingInputs` consults, facet
  dispatchVariantParamResolution #369) and the nested-divide witness
  classifies NUMBER before the engine arm
  (`IRJavaLeafEmitter.nestedArithmeticWitness`, mirroring legacy
  `numericOperandKind`'s post-pin `earlyDiv` arm — the
  `<Integer,…>`-where-legacy-renders-`<BigDecimal,…>` face the #466 A/B
  decoded); the wholesale decline then retired outright (every other
  variant channel is context-generic: call receivers resolve through
  the variant's own dependency numbering, arg renders recurse the
  taught alias/enum/literal channels, arg accessors are callee-side,
  and base-input references never adapt — the fork AST leaves them
  unresolved inside variants, so those claims stay adapter-declined);
  **the dep-receiver collision numbering
  (`commodityLeg10`/`11`) TAUGHT** — the call-receiver resolver rebuilds
  legacy `disambiguateDependencyReceiver`'s full precedence, correlating
  each IR callee to its AST call site by SOURCE RANGE (the FQN-keyed
  collector map is unreachable from the neutral simple name alone);
  **implicit-item call args TAUGHT** — the `ImplicitItemRenderer`
  supplies the LIVE scope binding for every `USER_ITEM` render by
  reusing legacy `handle(RImplicitVariable)` verbatim on the
  range-correlated node; **multi-hop item navs + meta-terminal item
  receivers TAUGHT AT THE CLAIM ROOT** — the `ItemNavRenderer` serves a
  guarded nav only when it IS the whole claim (`super.visitFeatureCall`,
  literally the decline's own call), while a NESTED guarded nav still
  declines the whole claim (the composition boundary: a legacy-rendered
  chain inside IR-rendered parents — scope-group-numbered coercion
  params — is a composition legacy never produced; the `cpON4` drift
  receipt is the witness); **the meta evaluate-arg families TAUGHT AT
  THE CLAIM ROOT at PR #471** — the claim-root delegation seat
  (`claimRootDelegationArm` since the #473 widening, `metaArgClaimRootArm`
  pre-#473; ahead of the guard scan) renders a call that
  IS the whole claim, whose first-trip arg coercion is the meta-wrapper
  hoist+deref (`tryMetaDerefArg`'s family, #237/#285/#340/#346/#364) or
  the multi-param elementwise wrapper deref (facet
  multiArgElementwiseWrapperDeref #349, arm B2), via
  `super.visitSymbolReference` — the decline's own render, provably
  identical (the #469 claim-root law), keeping the hoist family's five
  legacy route facets single-sourced in legacy; the #471 decode
  (`target-471-witness1.log`, the per-decline witness channel:
  `-Drosetta.generator.ir.postpinWitness=true`, a property-gated line
  per guard decline routed through the guard seat itself so the witness
  set and the meter cannot disagree) censused 67 of the 80 meta declines
  at the claim root (metaWrapperSingleArg 51/56 · metaItemMultiArgB2
  16/24), and the seat runs BEFORE the guard so an un-fire re-declines
  and breaks the taught zero on the meter (self-signaling); **the
  composition-boundary cluster TAUGHT at PR #472 — the guard seat became
  a ROUTER** (`compositionRootDelegation`): a tripped claim whose ROOT
  class is one of the six PROVEN composition classes (`RFeatureCall` /
  `RExistenceExpr` / `REqualityExpr` / `RLogicalExpr` / `RListOpExpr` /
  `RCountExpr`) delegates the WHOLE claim via the caller's own fallback
  — the exact `super.visitX` call the decline path reaches (the #469
  claim-root law widened per-class; the recon proof pack: every
  six-class node enters `tryEmitFromIR` only from its own visit
  override, each caller's decline fallback is exactly the reproduced
  super call, both exits sit at the same guard position, and the six
  classes have zero subclasses repo-wide) — so a legacy-rendered subtree
  is never wrapped in IR-rendered parents and the `cpON4`
  mixed-composition drift class stays impossible either way (legacy
  renders parent + chain together); the NEW per-arm delegation meter
  (`postPinDelegationBreakdown()`, printed per cell next to the decline
  meter — the two lines partition the scan's trip set) keeps the
  delegated volume receipt-visible, and the witness channel prints
  `-delegated` lines carrying the scan's own trip detail; **the
  singletonList endgame TAUGHT at PR #473** — the claim-root seat's
  accepted-arm set widened by ONE membership (`SINGLETON_LIST_ARG`
  joins the #471 meta pair), so the frozen corpus's last FUNCTION
  guard decline — the drr root call `IsActionTypeMODI` calling
  `FilterOpenTradeStates`, argIndex 0 (`RSymbolReference`, the same
  proven claim-root shape as the meta pair; the byte-identity argument
  is arm-independent, the #469 claim-root law binds the render seat) —
  delegates at the seat and **the FUNCTION-seam decline meter reads
  ALL-ZEROS on every cell**; **the intLiteral RULE-seam teach at
  PR #474** — the seat's FOURTH accepted-arm membership
  (`INT_LITERAL_ARG`): the arm's whole frozen-corpus population — 10
  claim-root decline events across 6 drr rule names, ALL on the RULE
  seam (sized by the #473 witness decode; the FUNCTION population is
  zero, so that meter stays all-zeros with no pin movement) — delegates
  at the same seat (`callAtRoot=true`, `RSymbolReference` roots — the
  proven shape; the byte-identity argument is arm-independent), and
  **the RULE-seam decline meter reads ALL-ZEROS too** (its first-ever
  reader block wired in the same cut — see Test coverage). Roots
  outside the proven sets still decline — the honest-residue DEFAULT
  stays armed: every arm the evaluate-arg probe can return TODAY is
  accepted, but membership stays explicit, so a future NEW arm
  self-signals on the meter and breaks the `declined == 0` pins (the
  three FUNCTION carrier cells + every RULE-seam cell) rather than
  silently delegating; the zero-input alias-arg filter (#426) stays
  taught since #467; **the whole not-IR-driven population made
  measurable at PR #475 — the census meters** (the share-growth wave's
  decode widened from the guard seat to the entire route): the scalar
  decline counter gains two breakdowns bumped at the single
  `recordDecline` seat — by CLAIM-ROOT family and by decline SITE
  (`adapterGap` / `postPinGuard` / `ruleDelegation` / `aliasResolution`
  / `leafEmitter` — splitting the worklist by owner: an adapter gap is
  rune-ir teach work, a leaf-emitter gap is rune-ir-java's) — both
  conserving to the scalar by construction (the #469 per-arm-meter
  law), and the 21 `visit*` families with NO IR attempt (the §4.2
  denominator's documented blind spot) gain count-then-super meters
  (`untargetedVisitsByFamily`: the counter bump + the EXACT super call
  the un-overridden dispatch would have made — byte-inert by
  construction, no seat to un-fire), so the D11 ON ring prints per
  cell `IR declined by family` / `IR declined by site` /
  `IR untargeted visits` ranked count-DESCENDING — the lines ARE the
  emitter-teach worklist — plus a property-gated per-decline sample
  channel (`-Drosetta.generator.ir.declineWitness=true`, separate from
  the postpin channel whose documented captures are small pinned
  line-sets). The first reading (`target-475-cpON1.log`): the counted
  declines conserve head-for-head with the standing share lines (4,612
  / 10,344 / 15,033 FUNCTION + 13,714 RULE drr) and split
  adapterGap-heavy (3,601/7,277/10,171/13,316 vs leafEmitter
  1,011/3,067/4,862/398; `ruleDelegation` and `aliasResolution` read
  zero by absence on the ranked lines — conservation-proven, the
  open-set census format prints only non-zero tokens), the untargeted
  population reads 666 /
  2,678 / 6,822 FUNCTION + 11,516 RULE drr (grand total 21,682; its
  biggest families: `RImplicitVariable` 9,339 — already ADAPTER-READY,
  a Java-side teach — · `RConstructorExpr` 3,501 · `RExtractExpr`
  3,140 · `RFilterExpr` 2,014 · `RConditionalExpr` 1,133 —
  adapter-ready too — · `RConversionExpr` 956), and the whole
  not-IR-driven node population is 65,385 against 59,763 driven.
  Over-declining is byte-safe by construction (a declined claim
  renders via legacy in full); the flag-on D11 ring is the
  under-decline oracle. **The adapterGap bucket decoded at PR #476 —
  the blocker-attribution probe** (`-Drosetta.generator.ir.blockerProbe=true`,
  default OFF — a census-run channel): the family-of-root census keys
  an adapterGap decline by the CLAIM ROOT's class, which cannot see
  WHICH nested family the adapter choked on, so at every adapterGap
  decline the probe re-adapts the claim's expression subtree bottom-up
  (nearest-expression-descendant recursion through supporting nodes)
  and records the MINIMAL blockers — nodes whose own `adapt` returns
  empty while none of their expression descendants is blocked — into
  two per-claim rankings: participation (`blockerClaimsByFamily`, one
  count per distinct blocking family) and SOLE-blocker
  (`soleBlockerClaimsByFamily`, single-family claims = the claims that
  family's adapter teach fully unblocks on its own — the unlock
  ranking; measured against TODAY'S adapter — a landed teach can
  surface a successor blocker the probed set couldn't see, so the
  probe re-reads after each teach). Read-only by construction: the
  probe runs strictly AFTER
  `recordDecline` on the already-declined path, calls only the
  adapter's documented stateless `adapt` (never the visitor — the
  standing counters cannot move), and an anomaly meter counts any
  re-adapt that contradicts its decline (expected 0 — adapter
  statefulness would show here). Both reader blocks print the two
  ranked lines ONLY when the probe ran, so the standing receipts are
  byte-identical probe-off. The first reading
  (`target-476-cpONprobe1.log`): probed 3,601/7,277/10,171/13,316 =
  34,365 ≡ the adapterGap census EXACTLY, anomalies 0 on every cell,
  sole-blocker claims 33,250 (96.75% — multi-family co-blocking is
  rare, 1,115 claims), led by `REnumValueRef` 13,395 ·
  `RSymbolReference` 11,411 · `RFeatureCall` 5,426 — 87.97% of the
  mass behind ARM REFINEMENTS (the adapter has these arms; specific
  shapes decline), while the missing compound-family arms the visit
  counts alone suggested are marginal here (`RConstructorExpr` 8
  sole-blocker claims vs its 3,501 untargeted visits — different
  buckets, different teaches). **The arm gates decoded at PR #477 —
  the per-ARM refinement sub-decode:** the adapter itself gained a
  read-only `declineReason(expr, ws)` channel — a per-arm mirror of
  each `adapt` arm's own exit order that names the FIRST declining
  gate as a stable token, computed with the arm's own predicate
  helpers (`isGenuineEnumValueRef`, `isSimpleCallArg`,
  `isNavigableReceiver`, …); *empty ⟺ lowers* holds by construction
  (the method calls the real `adapt` first), a mirror that finds no
  failing gate returns the `unattributed` sentinel (the
  self-reporting mirror-currency meter, expected 0 always), and an
  un-armed family reads `noAdaptArm`. The probe buckets each minimal
  blocker as a `Family:reason` pair on the SAME probe run (no new
  flag): pair participation (`blockerClaimsByReason`), the
  SOLE-REASON unlock ranking (`soleReasonClaimsByReason` — claims ONE
  gate refinement unblocks whole) and the sole-family multi-reason
  residue (`soleFamilyMultiReasonClaims`), conserving at the single
  recording seat: per family, sole-blocker = Σ sole-reason +
  multi-reason — the #476 family counters are derived from the pairs
  and stay byte-identical. Three reason lines print per cell on both
  reader blocks (probe runs only). The first reading
  (`target-477-cpONprobe1.log`): unattributed **0** on every cell,
  the conservation identity ZERO violations over all four cell-seams,
  sole-reason total 33,028 + 222 multi-reason ≡ the #476 sole 33,250
  EXACTLY — and the gates INVERT the arm-refinement framing: the
  reference-family mass is DISGUISED/IMPLICIT NAVIGATION —
  `REnumValueRef:inputFeatureNav` 9,008 (the L-111 typing channel's
  disguised `<head> -> <feature>` navs) + `REnumValueRef:attributeChain`
  4,088 (3,326 of them drr RULE) + `RFeatureCall:receiverSyntheticItem`
  4,643 (navigation off the synthetic implicit item) — plus
  `RSymbolReference:symbolUnresolved` 3,921 (read then as "a
  RESOLUTION question, not an adapter arm" — the #486 decode
  overturned this in place: 99.71% are render-time SYNTHESIZED
  disguised-nav receivers, the shadow population of the alias/nav-arm
  families — see the #486 entry below),
  `RSymbolReference:attrOutsideFunction` 3,782
  (3,286 drr RULE — the rule-scoped attribute refs) and
  `RSymbolReference:bareFunctionReference` 2,120 (the L-109
  deliberately-declined point-free shape, nested positions); the real
  adapter teaches are navigation-channel lowerings, not render
  polish.
- The generator's SPI package + the 10 seam files (above). The ONE
  control-flow change is `JavaCodeGenerator.generate()` routing its
  per-generator and metafield calls through `IRGeneration` — with the
  flag off this executes the identical legacy calls.
- The D11 harness (`D11CorpusRegressionTest`) and the A/B dump vehicle
  (`SwapTreeDumpProbe`) construct/dispatch the five IR-variant kinds
  through the same helpers — flag-off runs are unchanged by construction.
- The `ir-on` surefire profile in `rune-java-generator/pom.xml`: adds the
  two installed jars to the test classpath (a test-scope dependency would
  cycle the reactor) + sets the flag. Inactive by default.

## BC Story

- **The #478 disguised-input-nav teach + the nav-gate shape witness**
  (`ExpressionToIRAdapter.adaptDisguisedInputNav` + the compiler's
  `classifyNavGateBlocker` seat): the witness rides the blocker probe —
  each `REnumValueRef` minimal blocker on the `inputFeatureNav`/
  `attributeChain` gates classifies by head/root class (legacy
  `ReferenceHandler.resolveNameInFunction`'s own order) × the adapter's
  verdict on the EXACT equivalent feature-call legacy synthesizes for
  the node (the widened `synthesizeFeatureCall`/
  `synthesizeImplicitInputChain` seams — the L-109e reuse precedent),
  with one pinned corpus site per bucket and a chain-receiver position
  facet, printed as three per-cell reader lines on both seams (probe
  runs only). The pre-teach reading sized the claimable slice —
  `inputFeatureNav:headInput:lowers` 11,259 blocker-node occurrences
  (the equivalent already adapter-lowerable) — and the ARM then claims
  exactly it: `adaptEnumValueRef`'s non-genuine path builds the same
  equivalent and delegates the gates to `adaptFeatureCall`, rebuilding
  the proven single-hop FieldAccess with the two types the caches hold
  (the head's attribute-channel type via the new
  `RWorkspace.getInferredAttributeType` delegate + the raw node's own
  #279-bound type — the expression-type cache is node-keyed and cannot
  hold a synthesized equivalent), the mirror twin composing the
  equivalent's own gate onto the channel token (`inputFeatureNav.<gate>`
  + the retype exits `headTypeMissing`/`navTypeMissing`). Byte-proving
  the unlocked claims surfaced and closed three latent emitter classes:
  the comparison/equality numeric-join guard now classifies EVERY
  numeric operand pair (a mixed resolved/resolved pair declines where
  legacy coerces — pre-#478 only literal-present pairs were reachable),
  the FieldAccess witness renders through legacy's first-claim-wins
  `ImportCollisionResolver` sentinel (a raw simple name let a
  same-simple-name construction type win a bare import), and the hop
  lambda var names through the DEFERRED registry
  (`registerDeferredLambdaParam` — resolved at finalization against
  later hoist locals, legacy's own channel). The post-teach probed
  re-read is the conservation proof: the taught gate reads ZERO on
  every cell with every other witness bucket byte-identical — probed
  30,882 ≡ the post-teach adapterGap exactly (−3,483 claims unlocked
  whole), anomalies 0, untargeted byte-unchanged; the walk series
  re-based (whole-claim absorption: an IR-driven claim stops
  re-entering the visitor at its sub-expressions — 125,148 → 108,013
  events, the headline 46.70% on the smaller denominator with real
  coverage GREW).

- **The #479 filter/extract synthetic-item receiver teach + the
  implicit-root shape witness**
  (`ExpressionToIRAdapter.retypedSyntheticFilterExtractItem` + the
  compiler's `classifyImplicitRootBlocker` seat + the
  `syntheticItemRenderer` oracle): the witness extends the #478 pattern
  to the implicit-ROOT cluster the gate decode named — each minimal
  blocker on `receiverSyntheticItem`/`attrOutsideFunction`/
  `attributeChain`(non-ruleInput) classifies by the LEGACY machinery
  that renders its root (the binding-arm order of
  `handle(RImplicitVariable)` restated read-only; the bare-attr
  synthesizer chain via the widened `synthesizeImplicitItemNavigation`/
  `synthesizeConditionInstanceNavigation` seams; the chain fall-through
  via the widened `synthesizeImplicitItemChain`), two per-cell reader
  lines on both seams (probe runs only), the #478 channel's buckets
  byte-unchanged. The pre-teach reading decoded the whole cluster to
  ONE mechanism — the synthetic implicit item reads MISSING through the
  expression-node-keyed type cache on EVERY bucket (the #478
  cache-boundary class) — and sized the claimable slice at
  `inLambdaImplicit.filterExtract:retypeSourceOk` 4,158 blocker nodes.
  The ARM claims the slice at the receiver gate: a SYNTHETIC_ITEM
  receiver whose nearest binder is an IMPLICIT-or-paramless
  filter/extract body (a NAMED extract carries the #357/#364/#367/#375
  scope-live walk-out re-roots; a switch between the item and the
  lambda carries the #221 case-narrowed subject machinery — both stay
  legacy's), whose source's element form is PROVABLY plain
  (`isProvablyNonMetaElementSource` — the allowlist over last-hop
  feature calls / non-meta bare attrs / bound chain leaves /
  all-provable list literals / filter recursion; anything unprovable
  declines, the L-029 split), RETYPES at the boundary with the binder
  source's element type (the engine's own Cat-8 law) and SINGLE
  cardinality; the hop nodes of a synthetic-based chain source their
  types from the resolved features' attribute channel (the #478
  `RWorkspace.getInferredAttributeType` delegate — the re-entrant
  legacy-synthesized equivalents are cache-invisible). The EMITTER
  renders the admitted item ONLY via the new synthetic-item oracle
  renderer — legacy `handle(RImplicitVariable)` verbatim on the
  range-correlated node (range-collision POISONING; no bare fallback —
  never guess a binding). The first ring was the decoder (the #478
  law): 15 mismatches → 0 in one wave, three classes each excluded at
  its own predicate (the named-extract walk-outs, the switch-case
  narrowing, the non-direct meta-sourced element forms — the arm
  tightened to the witness's context split, not patched). The
  post-teach probed re-read is the conservation proof:
  `retypeSourceOk` reads ZERO on every cell (arm ≡ witness at
  population — a nonzero re-read is drift, triage never absorb);
  probed 29,824 ≡ the post-teach adapterGap exactly;
  `receiverSyntheticItem` sole 4,643 → 3,577 (−1,066 claims unlocked
  whole); the `unprovableSource` residue 3,092 sizes a future
  allowlist widening; and the equivalents' bases being taught
  surfaced the #480 pre-sizing live (`bareAttr:itemNav:lowers` 336 +
  `chainFall:itemChain:lowers` 370 — the #476 upper-bound law's second
  consecutive observation). The walk series re-based a second time —
  and the absorption reached the UNTARGETED meter for the first time
  (a claimed chain absorbs its `RImplicitVariable` sub-visits: 9,339 →
  8,602): 108,013 → 107,272 events, driven +1,042, the headline
  47.99%.

- **The #480 bare-attr + chain item-nav arms off the taught base + the
  SOURCE decode** (`ExpressionToIRAdapter.adaptBareAttrItemNav`/
  `adaptImplicitItemChain` + the compiler's source-decode and
  arm-restatement channels + the widened synthetic-item oracle index):
  the pre-sized continuation — the #479 signature read the bare-attr and
  chain equivalents LOWERING at their taught bases (336 + 370 live), and
  the witness extension restated the PLANNED arms' admissions per
  blocker (the #479 law: the witness's verdict channel IS the arm's
  channel) — the pre-teach reading matched the pre-sizing EXACTLY
  (`bareAttrArm:claims` 336, `chainArm:claims` 370, zero admission
  residue) and the NEW source decode sub-classified the
  `unprovableSource` residue + the still-declining equivalents' implicit
  bases by binding-source AST shape: `elidedImplicit` (the
  grammar-elided PIPED implicit — the then-pipe class) dominates every
  channel (synItem 2,889/3,092 = 93.4%; 5,363 nodes over the four
  channels — the #481 widening sized), with every channel conserving to
  its standing buckets exactly. THE ARMS build legacy's EXACT
  `synthesizeImplicitItemNavigation`/`synthesizeImplicitItemChain`
  equivalents (fresh synthetic implicit + resolved features, parented
  at the raw node), mirror legacy's decline ladders in source order
  (the rule-input precedences, the shortcut/closure-param/scope-head
  guards incl. the #389 self-shadow exemption, the #479 filter/extract
  binder gates, the structural element derivation over the allowlist
  shapes + legacy's identity guard — prove-or-decline), delegate the
  gates to `adaptFeatureCall` (the #479 retype at the synthetic base +
  the attribute-channel hop typing) and rebuild at the cache boundary
  with the RAW node's cached type + source range on the whole spine —
  the correlation key the WIDENED oracle renderer resolves the lambda
  binding from (the raw claim nodes join the synthetic-item index; the
  serve builds the fresh implicit legacy itself builds on this exact
  render path; collision poisoning carries, NONE-range participation
  kept for the re-entrant fresh-node claims). The mirror twins compose
  the arm's first failing gate onto the channel tokens
  (`attrOutsideFunction.<gate>` / `attributeChain.<gate>` — the #478
  dot-suffix pattern; the witness hooks prefix-match). THE FIRST RING
  WAS CLEAN — the wave's first first-try-clean arm ring (55/55 ≡ the
  SOT, ZERO mismatches; the restated-admission discipline excluded the
  over-claim classes BEFORE the ring ran). The post-teach probed
  re-read is the conservation proof: both `claims` buckets and both
  `lowers` buckets read ZERO on every cell with every decline bucket
  byte-identical to the pre-teach reading; probed 29,499 ≡ the
  post-teach adapterGap exactly (−325 claims unlocked whole); the
  taught gates decomposed into their refinement residues
  (`attrOutsideFunction.noFilterExtractBinder` 2,253 + the
  `.sourceElementUnresolved` pair mass 2,316 = the elidedImplicit
  widening's live claim ceiling + `attributeChain.headSymbolNav` 1,166
  — the callable-collision face, legacy's by precedence — +
  `ruleInputChain` 674) and the sole ranking inverted an
  ELEVENTH time (`symbolUnresolved` 3,921 leads; sole 28,474 = 96.52%,
  sole-reason 28,274 + 200 ≡ exact). The walk series re-based a THIRD
  time — this absorption INSIDE the attempted meter (the vanished 962
  events are the L-113 relabel-driven re-entrant visits under
  now-claimed parents — the pre-teach double-count: the parent declined
  AND its nested bare attrs each relabel-drove): 107,272 → 106,310
  events, driven 50,851, the headline 47.83% with real IR-rendered
  coverage GROWN +325 whole claims and the untargeted meter
  byte-identical.

- **The #481 elidedImplicit widening + the elided-PIPE decode**
  (`ExpressionToIRAdapter.resolveElidedPipedSource` + the widened
  allowlist / structural-derivation arms + the compiler's pipe-decode
  channel): the decode-sized continuation — the #480 source decode read
  `elidedImplicit` dominating every unprovable channel (5,363 nodes),
  and the pipe-decode witness restated the PLANNED widening's admission
  BEFORE the arm existed (the #480 law): every `elidedImplicit` binding
  source decodes by the resolver's exit order — the non-then faces
  (`literalItem` / `switchBoundary` / `noBinder` / `namedBinder` /
  `nonThenBinder.<construct>`) size the structural residue, and the
  resolved then-ARGUMENT decodes by provability under the WIDENED
  allowlist restatement plus its cached-type facts. The pre-teach
  reading sized the claimable slice at `thenArg.provable.typeOk`
  12/106/89/91 = 298 nodes (synItem 180 · bareAttr 58 · chainBase 60 ·
  ruleInputLambda 0 — its elided mass is rule-input faces) with
  `typeMissing`/`typeMeta` ZERO, conserving to the standing flat
  `elidedImplicit` counts on all 13 cell-channel pairs EXACTLY
  (2,889/537/1,419/518 = 5,363 ≡ the #480 sizing), and mapped the
  residue: nested `thenPipe` arguments 2,109 · the rule-input faces
  1,356 (`noBinder` 1,038 + `filter.elidedImplicit` 318) · `extractPipe`
  753 · `call` 247. THE ARM: `resolveElidedPipedSource` walks a
  grammar-elided piped implicit to the enclosing THEN's ARGUMENT —
  legacy `handle(RImplicitVariable)`'s thenBody face serves the pipe
  input's value through BOTH render channels (the hoisted `thenArg`
  local and the runtime lambda binding), so the element form is the
  argument's own by the value identity; the walk mirrors the #479
  retype's conservatism exactly (a switch before the lambda, a
  missing/named binder, or a non-then implicit binder stops the
  resolution — prove-or-decline; chained hops under the depth bound).
  Consumed at THREE seats at once: the retype's source + type reads
  (the elided node is cache-invisible — both now see the pipe
  argument), the allowlist's `RImplicitVariable` arm (nested elided
  sources), and `sourceElementDataType`'s arm (the #480 arms' element
  derivation + the mirror twins, auto-served); the witness copies recut
  verbatim (the 2-arg widened allowlist + the resolver restatement +
  `retypeSourceFacet`'s widened flip). THE SECOND CONSECUTIVE
  FIRST-TRY-CLEAN ARM RING (55/55 ≡ the SOT, ZERO mismatches). The
  post-teach probed re-read is the conservation proof:
  `thenArg.provable.*` reads ZERO on every cell-channel with every
  residue bucket byte-identical to the pre-teach reading; probed 29,265
  ≡ the post-teach adapterGap exactly (−234); the elided channels
  shrank by the claimed slice to the digit (synItem −180 · bareAttr −58
  · chainBase −60 · ruleInputLambda 0); `receiverSyntheticItem` sole
  3,577 → 3,397 (−180 ≡ the synItem claims); the arm-restatement
  redistribution closed (`sourceElementUnresolved` −143/−90 =
  `equivalent.metaFeature`/receiver classes redistributed + 49/60
  claimed; the arm channel −109 ≡ the claimed occurrences); sole 28,241
  = sole-reason 28,043 + multi 198 ≡ exact. The walk series re-based a
  FOURTH time — the absorption reached the untargeted meter again
  (`RImplicitVariable` 8,602 → 8,464, the sole family moved; attempted
  −159): 106,310 → 106,013 events, driven 50,920 (+69 whole claims),
  the headline 48.03% — THE FIRST 48% CROSSING (the dial
  57.63/64.27/68.70 + RULE 45.75; leafEmitter 5,015 → 5,021, the +6 =
  the renderer's honest emit-declines).

- **The #482 nested-thenPipe widening + the pipe BODY decode**
  (`ExpressionToIRAdapter.isProvablyNonMetaElementSource` +
  `sourceElementDataType`'s `RThenExpr` arms + the compiler's
  `thenPipeBodyFacet` sub-decode): the residue-map continuation — the
  #481 pipe decode read nested `thenPipe` arguments as the dominant
  residue class (2,109 occurrences), and the body-decode witness
  restated the PLANNED RThenExpr arm's admission BEFORE the arm
  existed (the #480/#481 law), refining the flat
  `thenArg.unprovable.thenPipe` token IN PLACE (the full bucket:
  `pipe:<channel>:thenArg.unprovable.thenPipe.<facet>`; Σ over the
  `thenPipe.*` facets ≡ the flat count per cell-channel BY
  CONSTRUCTION — the same seat, dot-suffixed): the element form of
  `A then B` is B's BODY result (the pipe's value IS the body's value
  on the piped input — the #481 value identity one structural seat
  deeper), so each nested argument decodes by its body's provability
  under the PLANNED (then-widened) allowlist plus the THEN node's own
  cached-type facts (a parsed node — the arm needs no new type
  channel). The pre-teach reading proved the claim HONEST-SMALL:
  `thenPipe.provable.typeOk` = 97 occurrences (synItem 50 · bareAttr
  41 · chainBase 6), `typeMissing`/`typeMeta`/`noBody` ZERO, Σ
  thenPipe.* ≡ the #481 flat counts on every channel exactly (bareAttr
  289 · chainBase 765 · synItem 1,055 = 2,109), and the body-shape
  residue map named the TRUE successor: extract-bodied pipes 1,435
  (an extract re-shapes its elements — its element form is its own
  body's, a further body-descent widening, now sized) ·
  `filter.elidedImplicit` 272 · `listOp.ONLY_ELEMENT` 207 ·
  `listOp.FLATTEN` 78 · a 20-occurrence tail. THE ARM: the allowlist
  and the structural derivation gain the `RThenExpr` arm — a pure
  structural descent into `then.body()`'s expression
  (prove-or-decline; a bodyless then declines), the type read staying
  on the then node's own cached type; the termination measure extends
  the #481 post-order argument (an elided implicit only resolves to an
  ANCESTOR then's argument subtree, which wholly precedes it, and a
  body descent stays inside that subtree). The witness copies recut
  verbatim — the then arm folds into the live 2-arg widened form (the
  planned 3-arg tier dissolves exactly as #481's did) and the pipe
  facet's then branch sits ABOVE the live check, so the #481
  `thenArg.*` verdicts keep their exact meaning. THE THIRD CONSECUTIVE
  FIRST-TRY-CLEAN ARM RING (55/55 ≡ the SOT, ZERO mismatches). The
  post-teach probed re-read is the conservation proof:
  `thenPipe.provable.typeOk` reads ZERO on every cell-channel with
  every residue bucket byte-identical to the pre-teach reading; probed
  29,193 ≡ the post-teach adapterGap exactly (−72); the flat elided
  channels shrank by the claimed slice to the digit (synItem −50 ·
  bareAttr −41 · chainBase −6 · ruleInputLambda 0) with Σ pipe ≡ flat
  on every channel; `receiverSyntheticItem` sole 3,397 → 3,347 (−50 ≡
  the synItem claims); the taught chains' residues fell
  (`attrOutsideFunction.sourceElementUnresolved` 667 → 616 ·
  `attributeChain.sourceElementUnresolved` 1,456 → 1,446 — the
  −51/−10 falls exceed the 41/6 claimed occurrences by design: the
  derivation gate now succeeds on then-sources, so the excess
  re-lands on the ladder's LATER gates,
  `attrOutsideFunction.metaFeature` 101 → 131 +
  `attributeChain.metaFeature` 31 → 39 + `argNav` +3 compounding,
  with the family identities closing exactly — the honest precedence
  re-attribution, not drift); sole 28,171
  = sole-reason 27,973 + multi 198 ≡ exact (96.49% of probed; one
  tail-touch compounding lift, `RFilterExpr` 14 → 16 — the #476
  upper-bound law's standing participation class). The walk series
  re-based a FIFTH time — the absorption reached the untargeted meter
  again (`RImplicitVariable` 8,464 → 8,418, the sole family moved;
  attempted −70 ≡ driven +2 − declined 72): 106,013 → 105,897 events,
  driven 50,922 (+2 net whole claims), the headline 48.08% (the dial
  57.79/64.31/68.73 + RULE 45.80; leafEmitter byte-unchanged 5,021).

- **The #483 extract-bodied pipe widening + the extract-BODY decode**
  (`ExpressionToIRAdapter.isProvablyNonMetaElementSource` +
  `sourceElementDataType`'s `RExtractExpr` arms + the compiler's
  `extractPipeBodyFacet` sub-decode): the sized continuation — the
  #482 body decode named extract-bodied pipes the dominant residue
  (nested 1,435 + the flat `extractPipe` face 753 = 2,188), and the
  extract-body facet restated the PLANNED RExtractExpr arm's admission
  BEFORE the arm existed (the #480/#481/#482 law), refining BOTH flat
  `extractPipe` tokens IN PLACE at their own seats
  (`thenArg.unprovable.extractPipe.<facet>` at the flat seat ·
  `thenArg.unprovable.thenPipe.unprovable.extractPipe.<facet>` at the
  nested seat; Σ over the facets ≡ each flat count per cell-channel BY
  CONSTRUCTION): the element form of `A extract B` is B's OWN result
  per element (legacy `CollectionHandler.handle(RExtractExpr)`: EVERY
  receiver/body-cardinality map route — the single/item routes
  mapSingleToItem / mapItem / mapSingleToList / mapItemToList and the
  LoL routes mapListToItem / mapListToList, the Seat-1 OBS-3
  completion — produces result elements that ARE the body's values,
  the MULTI/LoL-body routes flattening to the body's own
  elements/lists, the switch-block lambda routes unreachable under
  the arm; the checker types the extract off its BODY —
  `ExpressionTypeComputer`'s RExtractExpr case), so each occurrence
  decodes by its extract body's provability under the planned
  allowlist + the SEAT's resolved source's cached-type facts (the
  extract node at the flat seat, the enclosing THEN at the nested seat
  — the claim seat's own read, no new type channel). The facet's exit
  order: `noBody` / `namedBinder` (the #357/#364/#367/#375 scope-live
  walk-out class — declined whole) / `literalItem` (the identity
  `extract item` — an argument-recursion face for a FUTURE widening,
  sized not claimed) / `unprovable.<shape>` (the residue map) /
  `provable.typeOk|typeMissing|typeMeta`. The pre-teach reading sized
  the direct claim at `provable.typeOk` = 351 of 2,188 (flat 81 +
  nested 270; synItem 181 · chainBase 88 · bareAttr 82),
  `typeMissing`/`typeMeta`/`noBody`/`literalItem` ZERO at population,
  and the residue map named the successor: extract bodies that are
  bare FUNCTION applications 1,080 (`symbol:RFunction` — the standing
  bareFunctionReference class, now sized on the pipe channel) ·
  `conditional` 435 · `call` 145 · `metaLeaf` 139 · `metaFeature` 18 ·
  a 19-occurrence tail (`listLiteral.symbol:RFunction` 8 ·
  `metaAttr` 8 · `symbol:RRule` 3) · `namedBinder` 1 — Σ residue +
  claims ≡ 2,188 EXACT. THE ARM: the allowlist and
  the structural derivation gain the `RExtractExpr` arm — the body
  descent gated on an IMPLICIT-or-paramless body lambda
  (prove-or-decline; a named extract declines whole, the identity
  face declines through the resolver arm's recursion); the
  termination measure extends verbatim (`children()` order [argument,
  body] — a body descent stays inside the extract's own subtree, the
  measure strictly decreases through every combination of the three
  arms). The witness copies recut verbatim — the extract arm folds
  into the live 2-arg widened form (the planned 3-arg tier dissolves
  exactly as #482's did) and both facet branches sit ABOVE the live
  check, so the standing verdicts keep their exact meaning. THE
  FOURTH CONSECUTIVE FIRST-TRY-CLEAN ARM RING (55/55 ≡ the SOT, ZERO
  mismatches). The post-teach probed re-read is the conservation
  proof: `extractPipe.provable.typeOk` reads ZERO on every
  cell-channel at BOTH seats; probed 28,814 ≡ the post-teach
  adapterGap exactly (declined −379 ≡ the gap delta; leafEmitter
  byte-unchanged 5,021); anomalies 0; **the claims exceeded the
  direct pre-sizing HONESTLY — a 135-occurrence RECURSIVE composition
  class claimed through the same recursion** (a nested then's body is
  a FILTER whose elided argument resolves, through the #481 resolver,
  to a now-provable extract: pre-teach the shape token named the
  filter's first failing inner, `thenPipe.unprovable.filter.elidedImplicit`;
  post-teach that inner proves, so the composition claims — the
  falls close per channel TO THE DIGIT: synItem −252 = 181 direct +
  71 recursive · chainBase −142 = 88 + 54 · bareAttr −92 = 82 + 10,
  residual 131 still-unprovable inners), with the flat
  `elidedImplicit` source channels falling −486 ≡ the total claims
  exactly and every OTHER residue bucket byte-identical;
  `receiverSyntheticItem` sole 3,347 → 3,095 (−252 ≡ the synItem
  claims); the taught chains' residues fell
  (`attributeChain.sourceElementUnresolved` 1,446 → 1,177 ·
  `attrOutsideFunction.sourceElementUnresolved` 616 → 427 — the
  −269/−189 falls exceed the 142/92 claimed occurrences by design:
  the excess re-lands on the ladder's LATER gates,
  `attrOutsideFunction.metaFeature` 131 → 233 +
  `attributeChain.metaFeature` 39 → 77 + the bare `metaFeature`
  605 → 665 + `attributeChain.receiverNotExpressible` 35 → 128 +
  `argNav` +14 — the #482 OBS-1 re-attribution class, the family
  identities closing exactly); sole 27,798 = sole-reason 27,597 +
  multi 201 ≡ exact (96.47% of probed; tail-touch compounding lifts
  REqualityExpr +2 · RListOpExpr +6 · ROnlyExistsExpr +2 — the #476
  upper-bound law's standing participation class). The walk series
  re-based a SIXTH time — the absorption reached the untargeted meter
  again (`RImplicitVariable` 8,418 → 8,241, the sole family moved;
  attempted −310 ≡ driven +69 − declined 379): 105,897 → 105,410
  events, driven 50,991 (+69 whole claims), the headline 48.37% (the
  dial 57.79/64.31/68.78 + RULE 46.60).

- **The #484 bare-function-application pipe widening + the
  callee-output decode**
  (`ExpressionToIRAdapter.isProvablyNonMetaElementSource` +
  `sourceElementDataType`'s `RSymbolReference`→`RFunction` legs + the
  compiler's `calleeOutputFacet` sub-decode): the decoded dominant
  residue — the #483 body decode named bare FUNCTION-application
  bodies the dominant class (`symbol:RFunction` 1,080 on the pipe
  channels: 787 nested + 293 flat) — claimed under a NEW law, the
  CALLABLE-OUTPUT identity: a bare (no-arg) function reference in
  element-source position is the L-109 point-free application, and
  its applied value IS the callee's OUTPUT (legacy
  `ReferenceHandler.renderImplicitFunctionInvocation` renders
  `<callee>.evaluate(<binding>.get())` — per its contract the
  invocation carries the function's output type; the checker types
  the bare reference off `fn.output()` —
  `ExpressionTypeComputer.inferTypeOfNode`'s RFunction leg), so the
  element form is the OUTPUT attribute's own. THE WITNESS (probe-only,
  commit 2): the flat `symbol:RFunction` shape token refined IN PLACE
  to `symbol:RFunction.<facet>` at its `unprovableSourceShapeToken`
  seat — EVERY composing channel (the extractPipe/thenArg pipe seats,
  the listLiteral/filter recursions, the #480 source channels)
  carries the facets at once, Σ facets ≡ each channel's flat count BY
  CONSTRUCTION — the facets in the planned arm's exit order:
  `aliasShadow` (legacy's `isAliasReference` precedence — a
  shortcut-colliding name renders as an ALIAS invocation, the #480
  MF-1 law; the coarse `collidesWithShortcut` form, declining a
  superset) / `outputMissing` / `outputMeta` (the decline faces) /
  `outputData[Multi]` / `outputNonData[Multi]` (the admit faces,
  sized per type-shape and cardinality); PLUS the PLANNED 3-arg
  `bareFnWidened` tier on the three pipe-facet seats, composing the
  carrier-type gate into the pre-read. THE PRE-TEACH READING: the
  WHOLE 1,125-occurrence population ADMITS (`outputData` 1,070 +
  `outputDataMulti` 55; aliasShadow / outputMissing / outputMeta /
  outputNonData* ALL ZERO — the first all-admit decode of the wave)
  and the claimable slice pre-reads `provable.typeOk` = 1,127 channel
  events (extractPipe 1,088 = nested 795 [787 direct + 8 listLiteral
  compositions] + flat 293 · bare thenArg 12 · thenPipe 27 — the
  RECURSIVE filter-composition class the tier PRE-composed,
  `thenPipe.unprovable.filter.elidedImplicit` −12 synItem / −15
  chainBase) with `typeMissing`/`typeMeta` ZERO everywhere; the 25
  source-channel occurrences (all `outputDataMulti`) stay on the
  live-form seats pre-teach. THE ARM (commit 3): the allowlist and
  the structural derivation gain the RFunction leg on the SHARED
  `RSymbolReference` case — the aliasShadow precedence declines
  first, then output present + non-meta (the allowlist) / the output
  attribute's declared `RDataType` (the derivation) —
  cardinality-agnostic like the bare-attribute leg (a MULTI output's
  applied values are the output's own elements, the #483 route-grid
  flatten faces) and RECURSION-FREE (an output attribute is a
  declaration read — the #482 termination measure untouched);
  consumed by every standing seat at once; the witness copies recut
  verbatim (the planned tier FOLDS into the shared case, dissolving
  exactly as the #482/#483 tiers did). THE FIFTH CONSECUTIVE
  FIRST-TRY-CLEAN ARM RING (55/55 ≡ the SOT, ZERO mismatches). The
  post-teach probed re-read is the conservation proof: every
  pipe-channel `symbol:RFunction` token reads ZERO with the falls ≡
  the pre-read claimable slice 1,127 TO THE DIGIT, ALL 25
  source-channel occurrences claimed too (the live fold serves the
  #480 seats), the flat `elidedImplicit` source channels −1,127 ≡
  the claims (synItem −515 −4 · chainBase −583 −4 · bareAttr −21);
  probed 27,945 ≡ the post-teach adapterGap exactly (declined −869;
  leafEmitter byte-unchanged 5,021); anomalies 0;
  `receiverSyntheticItem` sole 3,095 → 2,567 (−528) and the taught
  chains' residues fell (`attributeChain.sourceElementUnresolved`
  1,177 → 654 · `attrOutsideFunction.sourceElementUnresolved` 427 →
  389) with the excess re-landing on the ladder's LATER gates
  (the full sole-reason re-land map, the Seat-1 OBS-1 completion:
  `opOnlyElement` +104 — re-ranking RListOpExpr above REqualityExpr
  in the sole bars — · `argNav` +50 · `RFeatureCall:metaFeature` +46
  · `attrOutsideFunction.metaFeature` +14 · `allAnyModifier` +6 ·
  `receiver:IRListOp` +3, plus the multi-reason residue +8
  [RSymbolReference 149 → 157]; the falls −528/−523/−38, net sole
  −858, the family lifts RSymbolReference +34 · REqualityExpr +6 —
  the #482 OBS-1 re-attribution class, the family identities closing
  exactly); the DIRECT-seat `bareFunctionReference` gate stands at
  2,143 EXACTLY (the render-seat class — nested bare-F nodes under
  legacy-rendered spines — untouched by the source-seat arm, as
  decoded); sole 26,940 = sole-reason 26,731 + multi 209 ≡ exact
  (96.40% of probed). The walk series re-based a SEVENTH time — the
  absorption reached the untargeted meter again (`RImplicitVariable`
  8,241 → 7,956, the sole family moved; attempted −602 ≡ driven +267
  − declined 869): 105,410 → 104,523 events, driven 51,258 (+267
  whole claims), the headline 49.03% — THE FIRST 49% CROSSING (the
  dial 57.79/64.33/68.78 + RULE 48.87 — the RULE dial's own first
  48% crossing).

- **The #485 rule-input widening + the rule-top decode**
  (`ExpressionToIRAdapter.provableRuleInputElementType` — the
  allowlist's `RImplicitVariable` unresolved branch + the derivation
  leg + the #479 retype's type read — + the REFINED `ruleInputNav`
  precedence gate + the compiler's `ruleTopImplicitFacet` /
  `elidedImplicitStopFacet` sub-decodes): the #484 residue map's
  dominant remaining pipe class — the rule-input faces (`noBinder`
  1,038 + the flat `filter.elidedImplicit` 318, ALL drr RULE) —
  claimed under the RULE-INPUT identity: the elided implicit at RULE
  top level takes the RULE INPUT's value (legacy
  `ReferenceHandler.handle(RImplicitVariable)`'s
  `isElidedOperandTopLevel` route renders `MapperS.of(input)` — the
  `RFunction.fromRule`-synthesized input parameter, typed by the
  rule's `from` type; the checker types the same face off the
  rule's from-type — `computeImplicitItemType`'s top-level branch →
  `inferRuleFromType`, always `withNoMeta`), so the element form is
  the rule's declared from-type — a DECLARATION read. THE WITNESS
  (probe-only, commit 2): the flat `noBinder` token refined IN PLACE
  at its `elidedPipeFacetToken` seat AND the flat `elidedImplicit`
  token at its `unprovableSourceShapeToken` seat (every composing
  channel at once, Σ facets ≡ each flat count BY CONSTRUCTION) — the
  facets in the planned arm's exit order: `parentNotElidedOp` (the
  `isElidedOperandTopLevel` slot gate) / `lambdaAbove` /
  `switchAbove` (drift detectors) / `functionTop` / `conditionTop` /
  `noRoot` / `fromMissing` / `fromUnresolved` / `fromNonData` (the
  decline faces) / `fromData` (THE claim face); PLUS the PLANNED
  3-arg `ruleTopWidened` tier on the three pipe-facet verdict seats.
  THE PRE-TEACH READING: the WHOLE 1,038-occurrence direct
  population ADMITS (`noBinder.fromData` — every decline face ZERO,
  the second consecutive all-admit decode) and the tier pre-reads
  the claimable slice at 1,336 channel events (the direct 1,038 +
  294 flat filter compositions + 4 nested filter-of-filter, moved to
  `thenArg.provable.typeOk`/`thenPipe.provable.typeOk` with
  `typeMissing`/`typeMeta` ZERO); the flat filter class splits 294
  composable + 24 `nonThenBinder.RExtractExpr`; the nested 110 maps
  to deep `resolves.*` residues + the 4 composables. THE ARM
  (commit 3): `provableRuleInputElementType` — synthetic + the slot
  gate + a CLEAN walk to the `RRule` root (a
  lambda/switch/`RFunction`/`RCondition` crossing declines) + the
  `from` type resolving to an `RDataType` — consumed at the
  allowlist's unresolved-implicit branch, the derivation leg, and
  the retype's type read (`withNoMeta(new RDataTypeRef(fromType))` —
  the checker's own wrap; the branch unreachable pre-#485);
  RECURSION-FREE (the #484 pattern). PLUS the `ruleInputNav`
  precedence gate REFINED to the TOP-LEVEL slice
  (`wouldSynthesizeRuleInputNav && !hasEnclosingRuleLambda` on the
  arm + both twins — legacy's own `buildImplicitInputReceiver`
  split: the in-lambda receiver is a synthetic IMPLICIT, the item ≡
  input value identity, the exact equivalent shape the arm lowers;
  the `MapperS.of(input)`-rooted top-level render stays legacy's);
  the witness tier FOLDS into the live 2-arg form. THE SIXTH
  CONSECUTIVE FIRST-TRY-CLEAN ARM RING (55/55 ≡ the SOT, ZERO
  mismatches). The post-teach probed re-read is the conservation
  proof: every claim face reads ZERO with the falls ≡ the pre-read
  claimable slice 1,336 TO THE DIGIT (the `ruleInputLambda` pipe
  channel EMPTIED 518 → 0 whole; `synItem` −818 = 652 direct + 162
  flat-filter + 4 nested; ZERO recursive-disclosure excess — the
  tier pre-composed everything); probed 27,123 ≡ the post-teach
  adapterGap exactly (declined −822; leafEmitter byte-unchanged
  5,021); anomalies 0; `receiverSyntheticItem` sole 2,567 → 1,749
  (−818) and `ruleInputNav` 432 → 80 (the top-level residue) with
  `ruleInputChain` 674 EXACTLY unmoved (the enum-chain seat, out of
  the arm's scope as decoded); the excess re-lands on the ladder's
  LATER gates (the full sole-reason re-land map: `argNav` +338
  [1,959 → 2,297, re-ranked SECOND in the sole bars] ·
  `symbolNotAttribute` +64 · `bareFunctionReference` +12 ·
  `opOnlyElement` +8 · `attrOutsideFunction.metaFeature` +2, the
  multi-reason residue −76 [RSymbolReference 157 → 81]; the falls
  −818/−352, net sole −822 ≡ the adapterGap delta EXACTLY — the
  #482 OBS-1 re-attribution class, the family identities closing
  exactly); sole 26,118 = sole-reason 25,985 + multi 133 ≡ exact
  (96.29% of probed). The walk series re-based an EIGHTH time — the
  absorption reached the untargeted meter again (`RImplicitVariable`
  7,956 → 7,361, the sole family moved; attempted −206 ≡ driven +616
  − declined 822): 104,523 → 103,722 events, driven 51,874 (+616
  whole claims — the wave's biggest claim count since #480), the
  headline 50.01% — THE FIRST 50% CROSSING, THE HALFWAY MARK (the
  dial 57.79/64.33/68.78 + RULE 51.87 — the RULE dial's own first
  50% crossing).

- **The #486 symbolUnresolved decode (probe-only — the highest-information
  menu pick per the post-#485 estimate conversation; the adapter's
  `symbolUnresolvedFacet` + `isChildLinkedAtOwnSeat` seats):** the flat
  `RSymbolReference:symbolUnresolved` reason token — the top sole-reason
  gate at 3,921, carried since #477 as "a RESOLUTION question, possibly
  zero adapter work" and named by the estimate conversation as the one
  item able to re-scope the 100% estimate by ~4 points — refined IN
  PLACE at its single seat in `reasonForSymbolReference` to
  `symbolUnresolved.[synthetic.]<facet>`: the MECHANISM split FIRST (a
  reference whose parent does not list it among `children()` is an
  UP-only parented render-time synthesis — the shape legacy
  `ReferenceHandler.synthesizeFeatureCall` builds for a disguised
  `<head> -> <feature>` nav's receiver, resolved via
  `resolveNameInFunction` — inputs/output ONLY — so an
  alias/closure-param/other head stays EMPTY by construction and pass 5
  can never see the node; proven run-first by a parse-time carrier
  probe reading ZERO unresolved alias/param-named refs across the whole
  parsed cdm5 workspace, then per-occurrence at population by the
  linkage facet), THEN the head-name ladder in the resolution
  machinery's own precedence (`onlyExists` / `aliasName` — legacy
  `isAliasReference`'s name-match fallback class — / `closureParam` /
  `fnScopeName` / `itemAttr[Meta]` via the arms' own
  binder-source-element oracles / the `lambda.`/`lambdaSrcUnprovable.`
  position prefixes / `siblingEnumValue` — the #458 Arm B mirror,
  equality-sibling cached type only — / `global.<kind>` via
  exact-name-filtered `findByName` — the index's fuzzy fallback must
  not count — / `absent`). THE READING
  (`target-486-cpONprobe2.log`): **99.71% of the population is
  SYNTHETIC** — `synthetic.aliasName` 3,132 (79.8%: cdm5 704 + cdm6
  1,418 + drr-F 1,010 — alias-headed synthesized receivers, exactly
  legacy's name-match alias ladder) + `synthetic.closureParam` 510
  (drr RULE 278 + drr-F 188 + cdm 44 — closure-param heads) +
  `synthetic.absent` 232 (the headOther class, 116 + 116 the same
  functions in both cdm cells) + `synthetic.itemAttrMeta` 18 +
  `synthetic.lambdaSrcUnprovable.absent` 14 + `synthetic.lambda
  .global.type` 4 — the parse-time complement is ELEVEN events
  (`lambdaSrcUnprovable.global.type` 8 · `.global.typeAlias` 2 ·
  `.absent` 1). Conservation EXACT on BOTH instruments: participation
  Σ 844/1,583/1,206/292 and sole Σ 844/1,580/1,206/291 ≡ the #485
  flat counts TO THE DIGIT per cell-seam (ZERO facet-splitting
  excess — no claim carries two different-facet unresolved refs);
  probed 27,123 ≡ adapterGap, anomalies 0, unattributed 0; the #478
  nav-shape witness corroborates from its own seat
  (`headShortcut:declines:featureUnresolved` 864/1,002/816 — the same
  population, node-occurrence unit). THE RE-SCOPE: the class is NOT a
  linker/corpus gap and NOT a wall — it is the SHADOW population of
  the standing alias/nav-arm families (the synthesized receivers'
  claims are re-entrant renders inside legacy-rendered spines;
  claiming the OUTER disguised-nav classes absorbs these events, the
  #481–#485 absorption pattern), so the estimate's symbolUnresolved
  wildcard RESOLVES with no swing in either direction. Zero behavior
  change: the ON ring 55/55 with the identical= line-set ≡ the #437
  SOT on probe-off runs; the 13-pin facet lock joins the rune-ir
  suite (402/0/0/4) — the hand-parented pins mirror the
  synthesized-receiver shape (orphans by construction, the
  `synthetic.` faces) and the child-linked sibling pin locks the
  plain path.

- **The #487 conditional JOIN-law widening (the adapter's
  `isProvablyNonMetaElementSource` + `sourceElementDataType`
  RConditionalExpr branches; the witness's `conditionalJoinShapeFacet`
  + carrier-typed `conditionalJoinFacet`):** the residue map's dominant
  DECODED pipe face — the extract-bodied conditional class 435 + the
  direct conditional pipe face 68 + the thenPipe-direct 4 (507 pipe
  channel events; the sources instrument carries the 68 direct twins).
  Opened per the decode-first law: the flat `conditional` shape token
  refined IN PLACE to `conditional.<facet>` at its
  `unprovableSourceShapeToken` seat (every composing channel at once,
  Σ facets ≡ each flat count BY CONSTRUCTION) with the three pipe
  seats intercepting above their allowlist checks via the
  carrier-typed variant (the #483 terminal-triple pattern) and the
  PLANNED 3-arg `conditionalJoin` tier threading the recursions. THE
  JOIN LAW: the element form of `if c then A else B` is the arms'
  COMMON form — the runtime elements are the EXECUTED arm's (legacy
  `ControlFlowHandler.handle(RConditionalExpr)`: every route, the
  hoisted locals and the inline ternary alike, yields ONE arm's
  value), the checker types the node
  `withNoMeta(typeJoin.join(thenT, elseT))`
  (`ExpressionTypeComputer.computeConditional`; an absent/empty else
  types `NOTHING` — the join's bottom — so the empty-else face types
  as the THEN arm alone), and the `withNoMeta` wrap STRIPS arm meta
  from the cache channel, so the per-arm allowlist proofs are the
  ONLY meta protection (the L-029 law): the list-literal
  all-provable/all-identical laws lifted to the two-armed choice,
  with `hasGenuineElse`'s structural-emptiness discriminator (the
  `DefaultElseRule` synthetic empty-list else ≡ a user-written
  `else empty`). THE READING (`target-487-cpONprobe1.log`): the
  claimable slice 201 of 507 — `sameType.typeOk` 135 +
  `elseEmpty.typeOk` 52 + `formGap.typeOk` 14 (retype-channel
  claimable off the cached join, nav-channel not — the mixed-list
  asymmetry) — the dominant decline `thenUnprovable.call` 266 (the
  call-armed conditionals), and **the anticipated join-meet question
  DISSOLVED: `typeDiffers` read ZERO at population** (no mixed-form
  conditional exists in the corpus; the ancestor sub-split stays as
  drift detectors); `typeMissing`/`typeMeta` ZERO (the cached channel
  fully alive). THE ARM: the shape-law allowlist branch + the
  same-instance derivation leg landed in the adapter's shared
  functions (every consumer widened at once — the #483/#484/#485
  pattern); the witness tier folded into the live 2-arg form. THE
  CONSERVATION SIGNATURE (`target-487-cpONprobe2.log`): every claim
  face FELL to ZERO with every decline face unmoved to the digit
  (236 fallen channel events ≡ the pre-read EXACTLY); driven +25
  whole claims; adapterGap 27,123 → 26,952 (−171 ≡ net sole EXACT,
  `leafEmitter` 5,021 byte-unchanged); the re-land map:
  `receiverSyntheticItem` 1,749 → 1,640 (the retype claims) + BOTH
  `sourceElementUnresolved` gates cut (attrOutsideFunction −53 ·
  attributeChain −37 — conditional-formed sources now derive) vs the
  honest later-gate re-lands (`opOnlyElement` +12 ·
  `attrOutsideFunction.metaFeature` +8 · `argNav` +3 ·
  `bareFunctionReference` +3 · smaller +6, the multi residue 133 →
  129) and ONE `symbolUnresolved` event re-prefixed
  `lambdaSrcUnprovable.absent` → `lambda.absent` (its lambda SOURCE —
  a conditional — now provable: the #486 facet grammar's
  source-provability prefix moving exactly as designed); untargeted
  −86 ALL `RImplicitVariable` (7,361 → 7,275 — the ninth absorption
  re-base; `RConditionalExpr` untargeted UNCHANGED at 1,133, the
  claims being the INNER navs); the walk 51,899/103,490 = 50.14%.
  THE SEVENTH CONSECUTIVE FIRST-TRY-CLEAN ARM RING (55/55 ≡ the #437
  SOT); the JOIN-law lock joins the rune-ir suite (403/0/0/4 — the
  four parsed carriers: direct sameType retyping Trade through the
  cached join, extract-bodied sameType retyping Leg, elseEmpty on the
  then arm alone, the MIXED-form decline at the same-instance
  derivation) and the decode lock joins rune-ir-java (70/0/0/0 — the
  post-teach recut: fixtures 1–3 IR-driven with the breakdowns
  emptied; the mixed fixture's run-first disclosure pinned
  `bareAttr:itemNav:lowers` + `bareAttrArm:declines:
  sourceElementUnresolved`, the retype-vs-nav asymmetry).

- **The #488 argNav decode (probe-only — the user's pick A, the top
  actionable sole gate below the aliasName shadow; the adapter's
  `argToken` + `argNavFacet` seats):** the flat
  `RSymbolReference:argNav` reason token — the argument-position gate,
  2,300 sole / 2,438 participation, re-ranked SECOND among the
  actionable gates by the #485 honest re-lands — refined IN PLACE at
  its single seat to `argNav.[meta.|typeMissing.]<root>.<depth>
  [.multi]`. Every argNav occurrence is a navigation the nav arms
  ALREADY LOWER (the token is minted off the lowered `FieldAccess` —
  `adapt` returned non-empty), so the gate is purely
  `isSimpleCallArg`'s argument-position admission and the facet
  decodes what an L-042-class arm would compose: the ROOT names the
  render family (`param` — the `MapperS.of(name)` root the arms
  already emit; `item`/`synItem` — the filter/extract lambda binder;
  `root:IRApply` — a nav OFF a call result, the #484 callable-output
  law's composition seat), the DEPTH sizes the chain
  (`hop1`/`hop2`/`deep` ≥3 — the same chain machinery at any depth),
  `.multi` marks the accumulated-MapperC sub-population, and the
  `meta.`/`typeMissing.` prefixes + the `alias` root are drift/future
  detectors. THE READING (`target-488-cpONprobe1.log`): **param-rooted
  1,678 = 72.9% of sole** (`param.hop1` 803 the biggest single face +
  `.hop1.multi` 350 + `.hop2` 338 + `.deep` 102 + `.deep.multi` 64 +
  `.hop2.multi` 21 — the FUNCTION seam's mass, cdm6 `param.hop1` 489
  the biggest cell face) + synItem-rooted 523 = 22.7%
  (RULE-concentrated: 347 of the RULE 438 — the rule-input elided
  navs, the #485 arms' own render family) + `root:IRApply` 79 = 3.4%
  (drr-F's `hop2` 68 class — the #484 composition) + item 20 = 0.8%;
  EVERY detector ZERO (the alias-root emptiness independently
  confirming the alias RECEIVER declines at the nav arms before any
  arg lowers; the `typeMissing.` face reads the type engine's
  NON-NULL MISSING sentinel — `getInferredType` is
  `getOrDefault(expr, MISSING)`, a fixed-point cache read — made
  LIVE via `isMissing()` at the Seat-1 MF-1 recut, the corpus probe
  RE-RUN reading an IDENTICAL surface: every corpus hop carries a
  real fixed-point type, so the ZERO is live evidence, not
  vacuity). Conservation EXACT on BOTH instruments: Σ
  facets ≡ the flat counts TO THE DIGIT on all 8 channels (part
  280/1,036/638/484 = 2,438 · sole 280/955/627/438 = 2,300), zero
  stray flat tokens, and the FULL 101-line probe-surface diff vs the
  #487 log shows the 8 blocker/sole channels as the ONLY changed
  lines; probed ≡ adapterGap 26,952, anomalies 0, unattributed 0,
  census + walk UNCHANGED. THE RE-SCOPE: the class is nav-ARGUMENT
  admission, not nav lowering — an L-042-revival arm admitting
  `FieldAccess` args (the render already exists; the evaluate-slot
  unwrap is the alias-argument `.get()`/`.getMulti()` fall-through
  precedent) would compose the param slice first (72.9%), with the
  synItem slice riding the same lambda-binder renders the #479/#485
  arms produce and the IRApply slice composing the #484 law. Zero
  behavior change: the ON ring 55/55 with the identical= line-set ≡
  the #437 SOT on probe-off + OFF runs; the parsed-model decode lock
  joins the rune-ir suite (404/0/0/4 — 6 pins:
  `param.hop1/.hop2/.deep/.hop1.multi` + `item.hop1` +
  `synItem.hop1`, every predicted spelling correct FIRST RUN; the
  hand-built flat pin recut TWICE by the two-step run-first
  disclosure — the clean first read exposed the MISSING-sentinel
  mechanism, and the MF-1 live-detector recut pinned
  `argNav.typeMissing.param.hop1` on the exact shape that disclosed
  it), rune-ir-java 70/0/0/0 unchanged.

- **Flag off (the default): byte-identical by construction.** The helpers
  execute the exact legacy calls without touching `ServiceLoader`. Proven
  at PR #466 on the full OFF ring: gensuite 3,720/0/0/53 (the standing
  baseline, incl. the D11 full-corpus gate) · cp ×2 with all 55
  population lines byte-identical to the #437 SOT · parsersuite
  4,194/0/0/5 · plugin 18/0/0/0 · rune-ir 390/0/0/6 at that ring
  (390/0/0/4 since PR #468 — the census pair runs; 393/0/0/4 at
  PR #477 — the declineReason mirror locks; 394/0/0/4 since PR #478 —
  the disguised-input-nav arm lock; 395/0/0/4 since PR #479 — the
  synthetic-item retype-arm lock; 396/0/0/4 since PR #480 — the
  bare-attr + chain arm lock; 397/0/0/4 since PR #481 — the
  elided-pipe widening lock; 398/0/0/4 since PR #482 — the
  nested-thenPipe widening lock; 399/0/0/4 since PR #483 — the
  extract-bodied widening lock; 400/0/0/4 since PR #484 — the
  callable-output widening lock; 401/0/0/4 since PR #485 — the
  rule-input widening lock; 402/0/0/4 since PR #486 — the
  symbolUnresolved facet lock; 403/0/0/4 since PR #487 — the
  conditional JOIN widening lock; 404/0/0/4 since PR #488 — the
  argNav facet-decode lock; 404/0/0/4 HELD at PR #489 — the six decode pins RETENSED to the L-042-revival admission locks, the typeMissing sentinel pin standing as the decline-boundary pin; 405/0/0/4 since PR #491 commit 1 — the argNav root-type-belt pin, the #489 Seat-1 OBS-2 landing: an otherwise-ADMITTING root's OWN type joins the drift flags, pinned by the hand-constructed three-way contrast in `argNavRootTypeBeltDeclinesMetaAndMissingRoots`; 406/0/0/4 since the PR #491 alias-operand teach — the parsed six-function revival lock, the operandAlias facet grammar's admission/decline pins; 407/0/0/4 since the PR #492 alias-nav teach — the 5-leg parsed pin: admit single + admit MULTI via the faithful `getRuleBodyCardinality` channel + usesOutput + featureOffBody declines + the bound-receiver form; 408/0/0/4 since the PR #492 point-free teach — the IRPointFreeApply kind/JSON/printer seats joined `IRSamples` and the coverage locks). Re-proven at the #469 share-growth
  wave: cp flag-off 55/55 ≡ the #437 SOT with the route header `OFF` and
  ZERO share/meter lines (`target-469-cp1.log` — the wave's harness +
  compiler changes are flag-off-inert) + the gensuite baseline
  (`target-469-gensuite1.log`); and again at the #470 dispatchVariant
  teach (`target-470-cp1.log` 55/55 ≡ the SOT, header `OFF`, zero
  share/meter lines + `target-470-gensuite1.log` — the teach is
  rune-ir-java-local, Path-1 never loads it); and again at the #471
  claim-root meta-arg teach (`target-471-cp1.log` 55/55 ≡ the SOT,
  header `OFF`, zero share/meter lines + `target-471-gensuite1.log` —
  same argument, the teach and the witness channel are both
  rune-ir-java-local); and again at the #472 composition-boundary
  widening (`target-472-cp1.log` 55/55 ≡ the SOT, header `OFF`, zero
  share/meter lines + `target-472-gensuite1.log` — the router is
  rune-ir-java-local and the D11 reader additions gate on the resolved
  provider, so flag-off never reaches them).
- **Flag on without `rune-ir-java`:** Path-1 output after a one-time
  `System.err` notice (an explicit opt-in silently downgrading would
  misreport what ran).
- **Flag on with `rune-ir-java` — the full-population ON gate (the PR-3
  drift wave): 55/55 GREEN, 34,686/34,686 byte-identical**, the 55
  per-cell population lines BYTE-IDENTICAL to the #437 cp SOT
  (`target-467-cpON3.log` vs `target-437-cp2.log`). History: the ON
  ring's first honest reading (PR #466) was 34 divergent files (99.90%)
  across FUNCTION (cdm5 8 · cdm6 16 · drr 7) + drr rule-family POJO 3 —
  the A/B decode sampled three drift classes, and the drift wave's
  iteration surfaced the UN-sampled faces of the same pin-era family
  (dispatch-variant alias args + witnesses; dep-receiver collision
  numbering; implicit-item bound names; multi-hop lambda-var escapes;
  the multi-param B2/singletonList coercions) — ALL closed by the
  post-pin coercion guard (Structural changes above): the IR declines
  every refinement-bearing shape to legacy, byte-identical by
  construction. **The IR-driven-share dial (wired at PR-4, PR #468 —
  reported NEXT TO the byte count, never as the parity number; re-read
  after the #474 RULE-seam intLiteral teach):** on the 55/55-green ON
  ring (`target-474-cpON1.log`), the FUNCTION seam's §4.2 counters read
  cdm/5.38.0 **58.22%** (irDriven 6,429 / irDeclined 4,612 of 11,041
  attempted; guard 0, delegated 29) · cdm/6.20.6 **63.05%** (17,658 /
  10,344 of 28,002; guard 0, delegated 14) · drr/6.34.1 **61.76%**
  (24,282 / 15,033 of 39,315; guard **0**, delegated 25); the iso20022 +
  rune-fpml cells carry no FUNCTION population (0 attempted). The #472
  baseline read the same three displayed values with the one-decline
  drr residue (guard 0/0/1, drr 24,281/15,034, `target-472-cpON1.log`;
  the #471 baseline before it 57.96 / 63.00 / 61.69,
  guard 29/14/26, `target-471-cpON1.log`; the #470 baseline 57.83 /
  62.95 / 61.59, guard 43/29/64; the #469 baseline 56.03 / 62.21 /
  61.59, guard 302/288/64). The #473 movement is the ENDGAME teach:
  the last FUNCTION guard decline (the drr singletonList root call)
  delegates at the claim-root seat — a ONE-membership widening of the
  seat's accepted-arm set; the conservation is exact on the receipts:
  guard 0/0/**1** → **0/0/0** · drr irDriven **+1** (24,281 → 24,282)
  with irDeclined −1 · the ATTEMPTED denominators byte-UNCHANGED (the
  delegation signature — the delegated render is the decline's own
  legacy call path) · the router's delegation meter byte-UNCHANGED at
  **29/14/25** (multiHopItemNav 24/9/12 · metaItemReceiver 1/1/8 ·
  metaWrapperSingleArg 0/0/5 · metaItemMultiArgB2 4/4/0 — the seat
  delegation prints `-rootDelegated`, not on the router meter) · the
  witness head-for-head: 136 lines = **68** `-rootDelegated` + 68
  `-delegated` + **0** declines ≡ the #472 census's 67 + 68 + 1
  (`target-473-witness1.log`; the new line reads
  `arm=singletonListArg-rootDelegated in=IsActionTypeMODI`). The #474
  movement is RULE-seam-only and FUNCTION-INERT: every FUNCTION share,
  meter and dial figure above is byte-UNCHANGED vs the #473 receipts
  (the taught arm's FUNCTION population is zero), while the RULE seam
  prints its FIRST share reading — drr/6.34.1 **45.37%** (irDriven
  11,394 / irDeclined 13,714 of 25,108 attempted; guard **0**,
  delegated 13; itself a live floor case — 0.453799… floors to 45.37
  where rounding prints 45.38) with every other cell at 0 attempted (the seam's whole
  activity is drr — the #473 census's all-drr claim sharpened from
  trips to activity), and the drr conservation vs the pre-teach state
  is exact: the 10 taught events move decline → driven
  (`target-474-rulewitness1.log`: 23 witness lines = 13 `-delegated` +
  10 `-rootDelegated` + **0** declines ≡ the #473 census's 13 + 10). Two
  of the three FUNCTION cells print the floor convention live
  (unchanged):
  6,429/11,041 = 0.582284… floors to 58.22 (rounding would say 58.23)
  and 17,658/28,002 = 0.630597… floors to 63.05 (rounding 63.06); drr
  24,282/39,315 = 0.617626… reads 61.76 either way.
  Scope honesty: the share is attempt-success over the IR-TARGETED
  expression families at each metered seam (families with no IR
  override never reach the counters). The rule-family FunctionGenerator
  inside RuleGenerator/ReportGenerator — the RULE seam, served by the
  `pojo_comparison` pass's own `IRGeneration.functionGenerator` — was
  WITNESSED at #473 (the decode censused it at 23 lines, ALL drr: 13
  `-delegated` router delegations ALREADY firing seam-generically since
  #472 [12 `RFeatureCall` + 1 `REqualityExpr` roots, byte-green under
  the POJO ring] + 10 `intLiteralArg` decline events across 6 rule
  names, every one at the claim root — `callAtRoot=true`,
  `RSymbolReference` roots, the seat-servable shape; the same-named
  rules span regimes and not every regime's variant trips, so the event
  count is the census unit) and is READ since #474: the pojo pass
  carries its own reader block (the RULE share line + both per-arm
  meter lines + the pins — Test coverage below), the 10 decline events
  taught at the seat, so the seam's meter reads all-zeros with the
  router's 13 delegations as its firing witness; the guard still
  deliberately trades share for bytes — a lower share with a green byte
  ring is the designed strangler state, and no lab share figure is
  quoted forward.
- **A/B ring (the PR-3 re-diff):** flag-off vs flag-on live-tree dumps on
  the three previously-mismatch-bearing cells diff EMPTY — cdm5 0/3,950
  files · cdm6 0/5,902 · drr 0/6,675 (`target-467-ab-rediff.txt`); the
  remaining cells' A/B identity follows transitively from the two
  manifest gates (OFF ≡ manifest ∧ ON ≡ manifest ⇒ OFF ≡ ON).

## How to use

```
# 1. Install the route (once):
mvn -f rune-ir/pom.xml install
mvn -f rune-java-generator/pom.xml install
mvn -f rune-ir-java/pom.xml install

# 2. Run the D11 harness through the IR route (the ON ring):
mvn -f rune-java-generator/pom.xml test -Pir-on -Dtest=D11CorpusRegressionTest

# 2b. The per-decline witness channel (the share-growth wave's decode
#     vehicle, PR #471; default silent) — one line per guard decline
#     (arm + enclosing fn/rule + claim-root shape + trip-site detail),
#     one per claim-root delegation (PR #471; the seat's accepted-arm
#     set = the meta pair + singletonList since PR #473 + intLiteral
#     since PR #474, `-rootDelegated`) and one per composition-root delegation
#     (PR #472, `-delegated`, carrying the scan's own trip detail); the
#     FUNCTION-only selection scopes the capture to the meter line's
#     own population:
mvn -f rune-java-generator/pom.xml test -Pir-on \
    "-Dtest=D11CorpusRegressionTest#function_comparison" \
    -Drosetta.generator.ir.postpinWitness=true

# 2c. The RULE-seam capture (PR #473; its own reader block since
#     PR #474): the rule/report bodies render through the pojo pass's
#     own IR-routed FunctionGenerator, so the pojo selection scopes a
#     capture to that seam — the per-cell RULE share/meter lines print
#     alongside (post-#474 the witness reads 13 `-delegated` + 10
#     `-rootDelegated` + 0 declines, all drr):
mvn -f rune-java-generator/pom.xml test -Pir-on \
    "-Dtest=D11CorpusRegressionTest#pojo_comparison" \
    -Drosetta.generator.ir.postpinWitness=true

# 2d. The #475 census (no extra flags — the three ranked lines print
#     per cell on every ON run, both seams): `IR declined by family` /
#     `IR declined by site` (adapterGap = rune-ir teach work,
#     leafEmitter = rune-ir-java's; both conserve to the share line's
#     irDeclined) / `IR untargeted visits` (the 21 families with no IR
#     attempt — the §4.2 denominator's former blind spot). The
#     per-decline sample channel is its own property (SEPARATE from
#     2b/2c's postpin channel — a census dump is tens of thousands of
#     lines and must not flood the pinned postpin captures); one
#     `[irDecline witness] site=… family=… in=…` line per counted
#     decline:
mvn -f rune-java-generator/pom.xml test -Pir-on \
    "-Dtest=D11CorpusRegressionTest#function_comparison" \
    -Drosetta.generator.ir.declineWitness=true

# 2e. The #476 adapterGap blocker probe + the #477 per-ARM gate decode
#     + the #478 nav-gate shape witness + the #479 implicit-root shape
#     witness + the #480 source decode & arm restatement + the #481
#     elided-pipe decode (+ the #482 nested-pipe BODY sub-decode, the
#     #483 extract-BODY sub-decode, the #484 callee-output
#     sub-decode and the #485 rule-top stop-facet sub-decode riding
#     the same pipe/source lines)
#     (property-gated — a census-run cost, not a standing-ring
#     cost; the reader blocks print the FIFTEEN ranked lines ONLY when
#     the probe ran): re-adapts every adapterGap-declined claim's
#     subtree and prints per cell
#     `IR adapterGap blockers (probed=… anomalies=…)` (participation)
#     + `IR adapterGap sole-blocker claims` (the unlock ranking —
#     claims that one family's adapter teach fully unblocks)
#     + since #477 `IR adapterGap blocker reasons (unattributed=…)`
#     (Family:reason pair participation — the arm's own first-failing
#     gate) + `IR adapterGap sole-reason claims` (the per-gate unlock
#     ranking) + `IR adapterGap sole-family multi-reason claims` (the
#     residue closing soleFamily == Σ soleReason + multiReason)
#     + since #478 `IR nav-gate shapes` (head/root class × the
#     adapter's verdict on the legacy-synthesized equivalent) +
#     `IR nav-gate positions` (chain-receiver vs direct seats) +
#     `IR nav-gate witness` (one pinned corpus site per bucket)
#     + since #479 `IR implicit-root shapes` (the cluster gates by the
#     legacy binding/synthesizer machinery that renders each blocker's
#     implicit root — the #478 channel's buckets stay byte-unchanged) +
#     `IR implicit-root witness` (one pinned site per bucket)
#     + since #480 `IR implicit-root sources` (the unprovableSource
#     residue + the still-declining equivalents' implicit bases,
#     decoded by binding-source AST shape — the widening map) +
#     `IR implicit-root sources witness` (one pinned site per bucket) +
#     `IR implicit-root arm` (the bare-attr/chain arms' admissions
#     restated per blocker — post-teach `claims` reads ZERO, the
#     conservation signature)
#     + since #481 `IR implicit-root pipe` (every elidedImplicit
#     binding source decoded by the widening's admission — the
#     resolver's faces + the resolved then-argument's
#     provability/shape; Σ per channel ≡ the flat elidedImplicit
#     count, and the claimable `thenArg.provable.*` facet reads ZERO
#     post-teach; since #482 the nested `thenPipe` shape refines IN
#     PLACE to `thenPipe.<bodyFacet>` — the PLANNED RThenExpr arm's
#     admission per occurrence, Σ thenPipe.* ≡ the #481 flat thenPipe
#     count by construction, `thenPipe.provable.typeOk` ZERO
#     post-teach; since #483 BOTH flat `extractPipe` shapes refine the
#     same way to `extractPipe.<bodyFacet>` — the PLANNED RExtractExpr
#     arm's admission per occurrence at each seat, Σ facets ≡ each
#     flat count by construction, `extractPipe.provable.typeOk` ZERO
#     post-teach at both seats; since #484 the `symbol:RFunction`
#     SHAPE token refines IN PLACE to `symbol:RFunction.<facet>` at
#     its unprovableSourceShapeToken seat — the PLANNED
#     callable-output arm's admission per occurrence on EVERY
#     composing channel at once, Σ facets ≡ each channel's flat count
#     by construction, the admit faces ZERO post-teach; since #486
#     the `RSymbolReference:symbolUnresolved` REASON token refines IN
#     PLACE to `symbolUnresolved.[synthetic.]<facet>` at its
#     reasonForSymbolReference seat — the mechanism split (child-
#     linkage at the node's own seat: `synthetic.` = an UP-only
#     parented render-time synthesis) then the head-name ladder, Σ
#     facets ≡ the flat count per cell-seam on BOTH the participation
#     and sole channels by construction; since #487 the flat
#     `conditional` SHAPE token refines IN PLACE to
#     `conditional.<facet>` at its unprovableSourceShapeToken seat —
#     the JOIN-law arm's admission per occurrence on every composing
#     channel, the three pipe seats intercepting with the
#     carrier-typed variant, Σ facets ≡ each channel's flat count by
#     construction, the `sameType/elseEmpty/formGap` admit faces ZERO
#     post-teach; since #488 the flat `RSymbolReference:argNav` REASON
#     token refines IN PLACE to
#     `argNav.[meta.|typeMissing.]<root>.<depth>[.multi]` at its
#     `argToken` seat (since #489 the CLEAN spellings are CLAIMED — only
#     the residue faces typeGap./itemChain./itemSrcUnprovable. and the
#     drift detectors can mint; a clean-spelling re-read means witness
#     drift — triage, never absorb) — the argument-position facet: the ROOT names the
#     render family an admission would compose, the DEPTH sizes the
#     chain, `.multi` the accumulated cardinality, the prefixes drift
#     detectors; Σ facets ≡ the flat count per cell-seam on BOTH
#     blocker instruments by construction) +
#     `IR implicit-root pipe witness` (one
#     pinned site per bucket):
mvn -f rune-java-generator/pom.xml test -Pir-on \
    -Dtest=D11CorpusRegressionTest \
    -Drosetta.generator.ir.blockerProbe=true

# 3. Dump + diff a cell's live trees both ways (the A/B ring):
mvn -f rune-java-generator/pom.xml test -Dtest=SwapTreeDumpProbe \
    "-Dswap.out=<abs-empty-dir>/off" -Dswap.cell=cdm/6.20.6 -Dd11.corpus=cdm -Dd11.version=6.20.6
mvn -f rune-java-generator/pom.xml test -Pir-on -Dtest=SwapTreeDumpProbe \
    "-Dswap.out=<abs-empty-dir>/on"  -Dswap.cell=cdm/6.20.6 -Dd11.corpus=cdm -Dd11.version=6.20.6
```

Programmatic consumers construct the five IR-variant kinds through
`IRGeneration.enumGenerator(gm)` / `modelObjectGenerator(...)` /
`choiceObjectGenerator(...)` / `metaFieldGenerator(...)` /
`functionGenerator(...)` and dispatch per-model generation through
`IRGeneration.generateClasses(gen, model, version, output)` +
`IRGeneration.generateMeta(mfg, output)`; `JavaCodeGenerator.generate()`
already routes through the dispatch seams.

### Turning the route on in the Maven plugin

`rune-maven-plugin` routes since v3.3 seat 9 (PR #645 commit 14b):
`RunePluginRunner` constructs every one of its generators through the
`IRGeneration` construction seams and dispatches exactly as the D11
corpus gate does, site for site (the five derived per-model kinds and
the enum / pojo / choice passes through
`IRGeneration.generateClasses`, the metafield pass through
`IRGeneration.generateMeta`, DATA_RULE and the rule / report /
labelProvider generators directly, mirroring the host).

There is NO mojo parameter for the route &mdash; the seam reads the
system property and nothing else, and a second switch for one fact
would be a place for the two to disagree. A consumer opts in with BOTH:

```xml
<plugin>
  <groupId>org.finos.rune</groupId>
  <artifactId>rune-maven-plugin</artifactId>
  <dependencies>
    <!-- the provider jar on the PLUGIN's own classpath -->
    <dependency>
      <groupId>org.finos.rune</groupId>
      <artifactId>rune-ir-java</artifactId>
      <version>${rune.version}</version>
    </dependency>
  </dependencies>
</plugin>
```

```
mvn generate-sources -Drosetta.generator.ir=true
```

Every run prints the plugin's own three-way header &mdash;
`IR route: ON (provider <class>)` / `IR route: OFF (flag
-Drosetta.generator.ir=true set but NO provider on the classpath
&mdash; Path-1)` / `IR route: OFF` &mdash; and every generation pass
logs the generator class that actually ran and the dispatch it took
(`ENUM files=N generator=IREnumGenerator dispatch=seam`). The tokens
are ANSWERED by the calls that were made, not declared beside them, so
the log cannot disagree with the behaviour.

**Flag-off is byte-identical**, and the plugin carries its own byte
proof: `rune-maven-plugin/src/test/resources/route-fixture/` is a small
model exercising every pass, with goldens taken from the UNCHANGED
runner before the routing landed; `RunePluginRunnerRouteTest` drives
the real runner once per route (off, reference, optimised) and compares
every generated byte against them. Both provider jars are test-scope
dependencies of the plugin module, so the ON locks run on every build
rather than behind a profile that could skip silently.

## Migration

None. The flag defaults OFF; existing toolchain consumers continue on
Path-1 unchanged. Opting in requires BOTH the flag and the
`rune-ir-java` jar (+ its `rune-ir` dependency) on the generation
classpath. `rune-maven-plugin` wires the route since v3.3 seat 9
(PR #645 commit 14b) &mdash; the flag plus the provider jar on the
plugin's own `<dependencies>`; see
[Turning the route on in the Maven plugin](#turning-the-route-on-in-the-maven-plugin).

## Test coverage

- `IRGenerationSeamTest` (rune-ir-java) — the seam locks: flag-property
  agreement across modules (`IRFlag.PROPERTY` ≡ `IRGeneration.PROPERTY`,
  the Rule-3 lock), exactly-one ServiceLoader registration, the OFF
  contract (`providerOrNull() == null` flag-off), the construction seams
  returning IR variants, the dispatch decline contract for plain
  generators, and flag-on resolution + per-call flag re-read.
- `IRExpressionCompilerTest` (ported, 58 locks — the count went stale at
  53 through the #482 wave, caught + recut at #483; the #484 itemization
  below went missing in the #484 wave while its count landed — caught +
  appended at #485, the same Rule-2 stale-catch class; diverged since PR #470 —
  the resolved-divide witness lock retensed from the pin-era engine-first
  order to today's legacy nested-divide-first arm; the 3 census locks
  joined at PR #475: the untargeted count-then-super twin-render
  inertness proof [an `RListLiteral` renders byte-identically to a plain
  legacy compiler while its meter counts and its nested int elements
  drive], the family/site conservation-and-ranking lock
  [`RIntLiteral=2 RSymbolReference=1` / `leafEmitter=2
  aliasResolution=1` off two beyond-long declines + one parentless
  alias], and the fresh-compiler `none` form; the 3 blocker-probe locks
  joined at PR #476: the nested-family attribution proof [`p = (a
  contains b)` declines at adapterGap and attributes to the nested
  `RContainsExpr`, not the equality root — sole and participation both],
  the multi-family split [`(a contains b) = (c disjoint d)` counts both
  families on participation and NEITHER on sole], and the
  off-by-default + read-only twin [probe-on renders byte-identical to
  probe-off with every standing meter unmoved — since #477 the reason
  maps read `none` off too]; the #477 same-family multi-reason lock
  joined at PR #477 [`(FooEnum -> Bar) = (BazEnum -> Cash)` with a
  value-name-fallback left and a choice-option-disguised right — one
  family, two gates: sole-FAMILY counts it, sole-REASON does not, the
  multi-reason residue closes the conservation, and the #476 rankings
  extended with the `Family:reason` pair assertions on all three
  sibling locks; +1 at PR #478 — the nav-gate shape-witness lock:
  the head-class × equivalent-verdict taxonomy over both navigation
  gates, the pinned samples, the chain-receiver position facet, the
  #476 re-entrant event unit and the probe-off `none` contract; +2 at
  PR #479 — the implicit-root shape-witness lock (the three cluster
  channels' classification, the #478-channel additive contract, the
  pinned samples, the probe-off `none` form) and the synthetic-item
  arm's end-to-end lock (a parsed filter/extract fixture: the
  oracle-rendered receiver + the emitter's hop compose ≡ the legacy
  bytes with the DRIVEN meter as the teach witness, and the
  outside-slice residue staying legacy with driven 0); +2 at PR #480 —
  the source-decode & arm-restatement lock (a META `[metadata scheme]`
  itemNav fixture declining at the equivalent's meta gate on BOTH the
  standing and the restatement channels — =2 by the channel's
  node-occurrence unit, the exists claim + the legacy fallback's
  re-entrant root visit — plus the synItem unprovable-source
  sub-decode with its pinned sample and the probe-off `none` contract)
  and the bare-attr + chain arms' end-to-end DRIVEN lock (both parsed
  claims IR-DRIVEN via the widened oracle renderer with bytes ≡ legacy
  — the chain compared under a REAL finalized `JavaStatementScope`, the
  deferred-registry channel — and the outside-lambda exists claim
  DECLINING, the inner bare attr staying on the standing L-113
  relabel); +1 at PR #481 — the elided-pipe decode lock (the parsed
  `legs then filter rate exists` carrier: pre-teach it read the
  claimable `thenArg.provable.typeOk` facet on both the bareAttr and
  synItem channels — the commit-2 receipts carry that reading —
  post-teach the claim is IR-DRIVEN byte-identically and NO blocker
  reaches the witness, the conservation in miniature; plus the
  hand-built `nonThenBinder.RExtractExpr` still-declining face and the
  probe-off `none` contract); +1 at PR #482 — the nested-thenPipe
  body-decode lock (the parsed `legs then filter rate exists then
  filter qty exists` carrier: pre-teach it read the claimable
  `thenPipe.provable.typeOk` facet on both channels — the commit-2
  receipts carry that reading — post-teach the claim is IR-DRIVEN
  byte-identically with NO blocker reaching the witness; plus the
  parsed `legs then extract item then filter qty exists` residue
  carrier pinning the extract-bodied face on both channels — since
  #483 the pin reads the refined
  `thenPipe.unprovable.extractPipe.literalItem` token, the identity
  face sized for a future argument-recursion widening — and the
  probe-off `none` contract); +1 at PR #483 — the extract-body
  decode lock (the parsed Trade/Leg carriers: the FLAT
  `trades extract leg then filter qty exists` and the NESTED
  `trades then extract leg then filter qty exists` claims — pre-teach
  both read the claimable `extractPipe.provable.typeOk` facet at
  their seats on both channels, the commit-2 receipts carry that
  reading — post-teach both are IR-DRIVEN byte-identically with NO
  blocker reaching the witness; plus the parsed
  `trades extract t [ t -> leg ]` named-binder residue carrier
  pinning `thenArg.unprovable.extractPipe.namedBinder` on both
  channels — declined whole, the safe direction — and the probe-off
  `none` contract); +1 at PR #484 — the bare-function pipe-body
  decode lock (the parsed Trade/Leg/ToLeg carriers: the FLAT
  `trades extract ToLeg then filter qty exists` and the NESTED
  `trades then extract ToLeg then filter qty exists` claims IR-DRIVEN
  byte-identically post-teach with NO blocker reaching the witness —
  the commit-2 receipts carry the pre-teach
  `extractPipe.provable.typeOk` / `symbol:RFunction.outputData`
  readings — plus the META-output residue carrier (`ToRef` with
  `[metadata reference]`) pinning the refined
  `symbol:RFunction.outputMeta` decline face on both channels, the
  MULTI-output carrier (`ToLegs (0..*)`) IR-DRIVEN under the
  cardinality-agnostic leg, and the probe-off `none` contract);
  +1 at PR #485 — the rule-top decode lock (the parsed reporting-rule
  carriers: the FLAT `reporting rule RuleTopFilter from Trade: filter
  leg exists` and the COMPOSED `filter leg exists then extract leg`
  claims IR-DRIVEN byte-identically post-teach with NO blocker
  reaching the witness — the commit-2 receipts carry the pre-teach
  `noBinder.fromData` [BOTH decode channels] and tier
  `thenArg.provable.typeOk` readings — plus the hand-built
  function-top fixture pinning the `noBinder.functionTop` decline
  face — legacy's `isElidedOperandTopLevel` RFunction stop, stays
  post-teach — and the probe-off `none` contract);
  +1 at PR #487 — the conditional JOIN decode lock (the four parsed
  carriers, recut to the post-teach state at the teach: the DIRECT
  sameType carrier `(if useMain then trades else fallback) then
  filter leg exists` and the EXTRACT-BODIED sameType carrier
  `trades extract (if useAlt then altLeg else leg) then filter qty
  exists` and the elseEmpty carrier `… extract (if useAlt then
  altLeg) …` all IR-DRIVEN byte-identically with the pipe AND
  sources breakdowns emptied — the commit-2 receipts carry the
  pre-teach `conditional.sameType.typeOk` /
  `…extractPipe.unprovable.conditional.sameType.typeOk` /
  `….elseEmpty.typeOk` readings on both channels — while the
  MIXED-form carrier (`ext ExtLeg` vs `base Leg`) pins the run-first
  disclosure: irDriven=2 through the retype/oracle route off the
  cached join with the render byte-identical, the shape channel
  `bareAttr:itemNav:lowers` and the arm channel
  `bareAttrArm:declines:sourceElementUnresolved` — the standing
  mixed-list retype-vs-nav asymmetry, zero corpus mass — plus the
  probe-off `none` contract)]) —
  the IR expression compiler's unit behavior. The
  adapter-side mirror locks live in rune-ir's
  `ExpressionToIRAdapterTest` (+3 at PR #477, the module suite 390 →
  393: the enum-channel tokens [genuine → empty, attributeChain,
  inputFeatureNav, noResolutionChannel], the symbol/call/feature-call
  gates [attrOutsideFunction, symbolUnresolved (since #486 the
  refined `symbolUnresolved.absent` spelling), bareFunctionReference,
  calleeMetaOutput, argNav, metaFeature, featureUnresolved,
  receiverAlias], and the noAdaptArm + empty-⟺-lowers equivalence
  sweep with the `unattributed` residue asserted absent; +1 at PR
  #478 — the disguised-input-nav arm's defer gates with the composed
  `inputFeatureNav.<gate>` mirror tokens, the module suite 393 → 394;
  the arm's LOWERS side is population-locked by the probed re-read's
  conservation signature + the byte ring, the builtins workspace
  carrying no data types for a unit lowers-fixture; +1 at PR #479 —
  the synthetic-item retype arm's parsed-fixture lock, the module
  suite 394 → 395: a real `legs extract rate` snippet types the
  binder source through the fixed-point walk, the hand-built
  synthesized-equivalent nav ADAPTS with the receiver rebuilt on the
  source's element type + SINGLE cardinality + the hop typed off the
  attribute channel, the mirror reads empty ⟺ lowers, and the
  outside-binder residue keeps the `receiverSyntheticItem` token;
  +1 at PR #480 — the bare-attr + chain arms' parsed-fixture lock, the
  module suite 395 → 396: the bare `rate` and the bound `inner -> x`
  ADAPT whole with the retyped synthetic base, the raw node's range
  stamped on the whole spine (the emitter's correlation key), empty ⟺
  lowers on both twins, and the outside-lambda residue naming the
  composed `attrOutsideFunction.noFilterExtractBinder` token; the two
  standing mirror locks recut to the composed channel tokens; +1 at
  PR #481 — the elided-pipe widening's parsed-fixture lock, the module
  suite 396 → 397: the bare `rate` inside `legs then filter rate
  exists` ADAPTS whole — the structural derivation resolves the
  element type THROUGH the grammar-elided piped argument and the
  equivalent's base retypes off the resolved argument's cached type —
  while the hand-built extract-bound elided face stays declined with
  the standing `receiverSyntheticItem` token; +1 at PR #482 — the
  nested-thenPipe widening's parsed-fixture lock, the module suite
  397 → 398: the bare `qty` inside `legs then filter rate exists then
  filter qty exists` ADAPTS whole — the derivation descends into the
  NESTED then's body (the element form of `A then B` is B's body
  result) and the base retypes off the then node's own cached type —
  while the parsed extract-bodied nested pipe stays declined with the
  composed `attrOutsideFunction.sourceElementUnresolved` token (since
  #483 that carrier's decline is the IDENTITY face — the body-descent
  arm claims provable-bodied extracts, and the literal-`item` body
  declines through the resolver arm); +1 at PR #483 — the
  extract-bodied widening's parsed-fixture lock, the module suite
  398 → 399: the bare `qty` ADAPTS whole on BOTH the flat carrier
  (`trades extract leg then filter qty exists` — the derivation
  descends into the extract's body and the base retypes off the
  EXTRACT node's own cached type) and the nested carrier
  (`trades then extract leg then filter qty exists` — the #482 then
  descent composing with the extract descent, the type read on the
  inner THEN node), empty ⟺ lowers on the twins, while the parsed
  NAMED-binder carrier (`trades extract t [ t -> leg ]`) stays
  declined with the composed
  `attrOutsideFunction.sourceElementUnresolved` token — the
  scope-live walk-out class, prove-or-decline; +1 at PR #484 — the
  callable-output widening's parsed-fixture lock, the module suite
  399 → 400: the bare `qty` ADAPTS whole on the flat carrier
  (`trades extract ToLeg then filter qty exists` — the derivation
  descends into the extract body's bare FUNCTION reference and reads
  the callee's OUTPUT attribute's declared type, the base retyping
  off the extract node's own cached type = the callee's output), the
  nested carrier (`trades then extract ToLeg …` — the #482/#483
  descents composing with the callable-output leg) and the
  MULTI-output carrier (`ToLegs` returning `Leg (0..*)` — the
  cardinality-agnostic leg), empty ⟺ lowers on the twins, while the
  META-output carrier (`ToRef` with `[metadata reference]` on its
  output) stays declined with the composed
  `attrOutsideFunction.receiverSyntheticItem` token — the non-meta
  gate fails the allowlist so the synthetic base never retypes,
  prove-or-decline; +1 at PR #485 — the rule-input widening's
  parsed-fixture lock, the module suite 400 → 401: the bare `leg`
  ADAPTS whole on BOTH the flat reporting-rule carrier
  (`reporting rule RuleTopFilter from Trade: filter leg exists` —
  the base retypes with the RULE's declared FROM-type, the
  rule-input identity's declaration read, no cached-type channel)
  and the composed carrier (`filter leg exists then extract leg` —
  the filter arm's element-preserving recursion composing with the
  rule-input arm, the type read on the FILTER's own cached type),
  empty ⟺ lowers on the twins, while the hand-built FUNCTION-top
  fixture stays declined with the `receiverSyntheticItem` token —
  legacy's `isElidedOperandTopLevel` RFunction stop,
  prove-or-decline; +1 at PR #486 — the symbolUnresolved decode's
  13-pin facet lock, the module suite 401 → 402: a parsed
  Colour/Leg/Trade/Helper/MyFunc model gives the workspace a REAL
  name index and a REAL extract lambda, and the pins build each
  facet's construction — the hand-parented probe refs are orphans BY
  CONSTRUCTION (setParent only — the exact synthesized-receiver
  shape), pinning `synthetic.onlyExists` / `synthetic.aliasName` /
  `synthetic.closureParam` / `synthetic.fnScopeName` /
  `synthetic.itemAttr` / `synthetic.lambda.global.type` /
  `synthetic.lambda.absent` / `synthetic.global.function` /
  `synthetic.global.enum`, while the parentless refs pin the plain
  `absent` / `nameMissing` / `dottedName` faces and the child-linked
  equality-sibling ref — the fresh equality POINTING at the parsed
  cache-typed `t -> colour` disguised nav, no parsed-node mutation —
  pins the plain `siblingEnumValue` path; the standing #477-era
  unattached-ref mirror pin recut to `symbolUnresolved.absent`;
  +1 at PR #487 — the conditional JOIN widening's parsed-fixture
  lock, the module suite 402 → 403: the bare `leg` ADAPTS whole on
  the DIRECT carrier (`(if useMain then trades else fallback) then
  filter leg exists` — the derivation reads the arms' COMMON form
  and the base retypes off the conditional's cached JOIN type,
  `withNoMeta(join(Trade, Trade)) = Trade`), the bare `qty` ADAPTS
  whole on the EXTRACT-BODIED carrier (`trades extract (if useAlt
  then altLeg else leg) then filter qty exists` — the #483 body
  descent composing with the JOIN leg, the base retyping Leg off the
  extract's cached type) and the elseEmpty carrier (`… (if useAlt
  then altLeg) …` — the then form alone, the join's bottom rule),
  empty ⟺ lowers on all three, while the MIXED-form carrier
  (`ext ExtLeg` vs `base Leg`) stays declined at the same-instance
  derivation — the list-literal mixed law, prove-or-decline).
- `IRAdapterCoherenceTest` + `Wave6AnfSkeletonTest` — retargeted off the
  lab's `phase1-bundle` geometry onto this repo's `test-corpus/` at PR-4
  (PR #468) and now RUN (the module suite 75/0/0/0 since PR #493 — the
  `drivenMetricSplitSeparatesLoweredFromDelegatedMass` lock joined: the
  split's two faces (a pure-emitter render keeps the delegated total at
  zero; the L-113 relabel counts driven AND delegated together with its
  seat sub-counter) + the Σ conservation over the eight delegated-seat
  sub-counters;
  74/0/0/0 since the PR #492
  Copilot round — the `pointFreeAmbiguousRangeCollisionDeclines`
  ambiguity-boundary lock joined: two DISTINCT sentinel-ranged
  point-free sites POISON their shared correlation key and the claim
  declines to legacy byte-identically, while the single-sentinel face
  stays admitted (the corpus's parser-materialized population — the
  #479 range-collision-poisoning pattern at both site indexes);
  73/0/0/0 at the PR #492
  point-free teach — the `pointFreeCallArgDrivesThroughTheOracleRenderer`
  oracle-renderer lock joined, pinned through the `renderScoped` harness
  (the plain harness NPEs on the oracle's `ctx.scope()` read);
  72/0/0/0 since PR #489 —
  the nav-arg chain-render + expectation-neutrality locks joined;
  69/0/0/0 at #485/#486, 68/0/0/0 at #484, 67/0/0/0 at #483, 66/0/0/0 at #482, 65/0/0/0 at #481, 64/0/0/0 at #480, 62/0/0/0 at #479, 60/0/0/0 at #478, 59/0/0/0 at #477, 58/0/0/0 at #476, 55/0/0/0 at #475, 52/0/0/0 through #474; all 5
  former bundle-geometry skips cleared): Coherence proves the AST↔IR structural
  relation over cdm/5.38.0 (555 structs + 208 enums coherent), and
  Wave6's four corpus legs pin the ANF hoist skeletons against the lab's
  Q3 dump — with §1.1's decl-type byte cross-check now LIVE (the pinned
  L-032-MISSING trip fired on today's engine: the fork's #447
  typed-alias-nav wave resolves the alias-receiver then-branch types, so
  the dump's real `Date`/`BusinessCenters`/`BusinessDayConventionEnum`
  decl bytes are asserted, the slice-2 §6 retense precedent; the
  §5.1/§5.2 `thenArg` types stay masked — render-derived, still a "C"
  cross-check).
- The OFF ring = the standing suites (gensuite incl. D11 · cp · parser ·
  plugin); the ON ring = the same D11 harness under `-Pir-on` (its
  per-cell population lines ARE the honest divergence record — fully
  green since the PR-3 drift wave); the A/B ring = `SwapTreeDumpProbe`
  under both flags.
- The ON-gate reader set (PR-3 + PR-4), active only when the route
  actually RESOLVES (`providerOrNull()`, the seam's own contract —
  flag-on-no-provider stays Path-1-silent): the D11 FUNCTION flow asserts
  `switchHoistCount > 0` off the IR-routed renderer for the cdm/6.20.6
  carrier cell (the lab's L-056 lock, re-homed at PR-3) AND the guard
  seat's anti-L-042 firing lock — wired at PR-4 as
  `postPinCoercionDeclinedCount > 0` on the drift-bearing cdm/6.20.6
  carrier, then RETENSED at PR #472 when the composition-boundary teach
  drove the cdm decline counts to zero (the #470 OBS-3 tripwire firing
  BY DESIGN): the firing witness moves to the DELEGATION channel —
  `postPinDelegatedCount() > 0` on all three FUNCTION-bearing carrier
  cells (cdm5 · cdm6 · drr; a silent scan un-fire would zero it and
  readmit the #467 drift class) plus the fully-taught pin
  `postPinCoercionDeclinedCount() == 0` on ALL THREE carrier cells
  since PR #473 (cdm cells pinned at #472; drr joined when the
  singletonList endgame teach delegated its last residue — a new
  decline anywhere is a claim-root-seat un-fire re-declining or a NEW
  decline family, triaged, never absorbed). The same block prints the
  per-cell §4.2 IR-share line (carrying `postPinDelegated=` since #472)
  plus — since the #469 wave — the per-arm decline-meter line
  (`D11 <cell> FUNCTION postPin by arm: …`, one `label=count` token per
  `PostPinArm` with first-trip attribution summing exactly to the guard
  total; a taught arm's explicit zero is the wave's per-arm success
  signal) and — since #472 — the per-arm delegation-meter line
  (`… postPin delegated by arm: …`, same token format; the two lines
  partition the scan's trip set), and every D11 run opens with the
  self-certifying route header — `D11 IR route: ON (provider <class>)` /
  `OFF` / the distinct flag-on-no-provider form — so a receipt log names
  the route it actually ran (the #467 Seat-1 OBS-1); the reader-failure
  message names the likely causes (seam-bypass or stale m2 jars — the
  OBS-4 hardening). Since PR #474 the `pojo_comparison` pass carries
  the RULE-seam edition of the same block on its OWN funcGen (the
  generator instance RuleGenerator + ReportGenerator render through —
  a different instance from the FUNCTION pass's, the #473 Seat-1
  counter-instance isolation): the per-cell `RULE IR share` line plus
  both per-arm meter lines, the fully-taught pin
  `postPinCoercionDeclinedCount() == 0` on EVERY cell (the seam's
  whole trip population is drr, so the pin is universally tense — a
  new decline is a seat un-fire or a NEW family, triaged never
  absorbed) and the router's un-fire witness
  `postPinDelegatedCount() > 0` on drr only (the census's 13
  delegations are the seam's whole delegation population; the other
  cells read 0 attempted and cannot carry a `> 0` pin). Since PR #475
  BOTH blocks additionally print the three census lines — `IR declined
  by family` / `IR declined by site` / `IR untargeted visits` (ranked
  count-descending; `none` for a zero-activity cell) — off the same
  reflective reader; no new pins ride them (a census is a reading, not
  a lock — the conservation is by construction at the `recordDecline`
  seat and unit-locked in `IRExpressionCompilerTest`). Since PR #476
  both blocks additionally print the blocker-probe lines — `IR
  adapterGap blockers (probed=… anomalies=…)` (participation) + `IR
  adapterGap sole-blocker claims` (the unlock ranking) — joined at
  PR #477 by the three gate-decode lines — `IR adapterGap blocker
  reasons (unattributed=…)` (`Family:reason` pair participation) +
  `IR adapterGap sole-reason claims` (the per-gate unlock ranking) +
  `IR adapterGap sole-family multi-reason claims` (the conservation
  residue) — ALL printed ONLY when the probe ran
  (`blockerProbedClaimCount() > 0`), so the standing flag-on receipts
  are byte-identical with the probe property off; no pins ride these
  either (the same census-is-a-reading law).
- **The declaration route (PR #641, decision D55 (local)):**
  `IRDeclarationFactsReconcileTest` (rune-ir-java) — a two-namespace fixture
  carrying every declaration fact: each witnessed on the IR, all reconciled
  through the three wired generators (`IRModelObjectGenerator`,
  `IRChoiceObjectGenerator`, `IREnumGenerator`), the reconcile's OWN POPULATION
  (a declaration is counted before its adapter runs) and a THROWING declaration
  fed through each generator (counted, a mismatch, a named generation error);
  26 mutation lanes drop a fact family each in the adapter and read the class
  RED on that fact's own name. `D11IrFallbackRegisterTest`
  (rune-java-generator, corpus-free) — the fallback register's grammar
  refusals and the arbiter's every verdict direction (NEW FALLBACK / HEALED /
  BROKEN PRINT / declared == measured). On the corpus both ride the ON-route
  `D11CorpusRegressionTest`: `IR declaration reconcile` (declarations ==
  expected, mismatches 0) and `IR file writers` (the old generator's files ==
  the register's rows), 78 lines each, with a wholeness law per channel.

- **The data-type emitter (PR #645, v3.3 seat 9):** `IRTypeUnitTest` (17),
  `IRDataTypeEmitterTest` (48), `IRValidatorEmittersTest` (4),
  `IRModelMetaEmitterTest` (4) and `IRDeepPathUtilEmitterTest` (4) in rune-ir-java
  &mdash; each member emitter byte-equal to the old generator on the seat fixtures
  and the hold-out groups, every refusal by name, the all-or-nothing law of the
  unit (a throwing or empty member refuses the whole type; the deep-path util's
  no-file the one lawful empty answer), the wiring's switch and the population
  assertion; `RunePluginRunnerRouteTest` (5) in rune-maven-plugin drives the REAL
  runner once per route over a 49-line fixture with 47 goldens taken from the
  unchanged runner at the parent head (flag-off, the reference route ON and the
  optimised route ON each 47 / 47 byte-equal). On the corpus the ON-route
  `D11CorpusRegressionTest` gates `UNIT READY`, `TYPE UNIT VERDICTS`, the six
  `UNIT SHADOW[<member>]` lines (their oracle the LEGACY generator's own render of
  the same cell since commit 18 &mdash; round 1 cq MF-1), the `POJO MEMBER GATE`, the five derived kinds'
  `IR file writers` lines with the `WRITERS CROSS-CHECK` and `DERIVED SPLIT`, and
  the fallback register's equality per cell and sub-kind (249 rows since commit 15 &mdash; 12 rows since PR #646,
  the vendored DATA_TYPE rows 0).
- **The choice unit (PR #646, v3.3 seat 10 &mdash; THE TYPE UNIT GENERALISED to the
  CHOICE kind, no second unit, no per-file fallback):** `IRDataTypeEmitterTest`
  (48 &rarr; 54: the whole choice POJO against `ChoiceObjectGenerator`'s own render
  through `PojoSectionOracle.ofChoice`, every section arm by arm &mdash; the
  `RuneChoiceType` import as a claim, the empty doc-reference list, the flat
  hierarchy, the bare `@RuneChoiceType` line after `@RuneDataType` &mdash; a CHOICE node
  reporting a base type refused by name, and the SIXTH `type == null` difference
  PINNED: the choice javadoc's `@version` is the DECLARING model's, never the
  caller's) and `IRTypeUnitTest` (17 &rarr; 26: the kind-scoped second switch
  `IRTypeUnitWiring.CHOICE_AVAILABLE` &mdash; a CHOICE node refused whole by name while it
  is off with the STRUCT verdict unmoved, `inspect` ungated, the route-on law, the
  choice pass's delegate refusal &mdash; and the per-pass memo's three arms) in
  rune-ir-java (366 tests over 25 classes). On the corpus the ON-route
  `D11CorpusRegressionTest` gates six `... CHOICES` lines per cell (the POJO and the
  five UNIT members, each against a SECOND REAL PRODUCER &mdash; the legacy
  `ChoiceObjectGenerator` and the legacy derived generators rendered beside the
  pass, `LEGACY ORACLE[POJO-CHOICE]`), `UNIT READY`'s kind switch, `TYPE UNIT
  VERDICTS`' `attempted == dataTypes + choices` by name, the `CHOICE IR file
  writers` line with the `WRITERS CROSS-CHECK` over DATA_TYPE + CHOICE, and the
  fallback register's equality per cell and sub-kind (12 rows since PR #646 commit 5,
  the CHOICE rows 0 on every cell).
## Coverage

**Measured 2026-09-11 on the 25-cell corpus (the #632 honesty pass), the rule-body half re-measured 2026-09-16 at PR #638 (the five-cell figures below are history):** 99.9% of function-body expression claims and 99.6% of rule-body claims are lowered through the IR (DRR 7.x carries adapter gaps: 99.65% / 98.97% since PR #640, 99.63% / 98.87% before its heal), 18.6% / 20.5% of the lowered claims are rendered by the legacy handlers as oracle roots, declarations are modelled in the IR, the enum files emitted from it by a new IR emitter since PR #642, every data type's six files by the type unit's IR emitters since PR #645 and every choice's six files by the same unit since PR #646 (v3.3 seat 10), data-rule conditions compile through the IR compiler under the IR-share gate since PR #639 (v3.3 seat 3) and were first healed at PR #640 (v3.3 seat 4: 98.42&ndash;100.0% of claims handled per vendored cell, 43.44&ndash;100.0% before the heal; 63.6% of the handled claims written by the old code), and the validators' and metafields' INPUT FACTS are in the IR and reconciled since PR #644 (the property gate), the data types' validators, `*Meta` and deep-path utils written from them since PR #645 and the choices' derived files by the same unit since PR #646 (the metafield wrappers and package-info not yet); under the strict file meter ratified 2026-09-17 (PR #641, decision D55: a kind counts only when 100% of its claims are handled by the IR, none is written by the old code, and a new IR emitter writes the file) the ENUM files count since PR #642 (v3.3 seat 6: a NEW IR emitter writes every one of them from `IREnumNode` alone, byte-identical, the register's ENUM rows at zero), and the DATA-TYPE files and the data types' DERIVED files since PR #645 (v3.3 seat 9: the type unit writes a data type's six files TOGETHER from the IR alone or refuses the type WHOLE, and the register's vendored DATA_TYPE rows stand at zero), and the CHOICE files and the choices' DERIVED files since PR #646 (v3.3 seat 10: THE SAME UNIT generalised to the choice kind writes a choice's six files TOGETHER from the IR alone or refuses the choice WHOLE, and the register's CHOICE rows stand at zero on every cell) &mdash; the meter reads 53.8% of files (93,703 of 174,141; it read 53.5% of files &mdash; 93,223 of 174,141 &mdash; from PR #645 until PR #646, 2.6% of files &mdash; 4,542 of 174,141 &mdash; from PR #642 until PR #645, 0% of files from PR #641 until PR #642, and the 32.4% of files read at PR #640 under the withdrawn 98% threshold is WITHDRAWN) &mdash; the table and the rule are IR coverage — measured; the closure is v3.3 (the legacy route) and v3.4 (the optimised route). **The declaration route since PR #641 (v3.3 seat 5, decision D55 (local)):** the three IR-routed declaration generators reconcile EVERY fact of the enriched declaration IR per declaration through one `IRDeclarationReconciler` — it re-reads the source itself, never the adapter, and holds a type reference against `GeneratorModel.resolveTypeCall`, the oracle the emitted bytes already agree with — and the ON-route D11 prints `IR declaration reconcile: declarations / expected / factsAsserted / mismatches` per cell and kind before its gate, holding `mismatches == 0` AND `declarations == expected` (its own count of the cell's declarations — a declaration is counted before its adapter runs and a throw is a mismatch, never a silent drop) (22,207 declarations / 2,438,198 facts / 0 mismatches over the 25 vendored cells at PR #642; 23,240 declarations / 2,692,395 facts / 0 mismatches there since PR #643, the type gate — the three standing kinds' 22,207 declarations UNMOVED with their facts grown to 2,673,389, plus the fourth line's 1,033 `typeAlias` declarations / 19,006 facts — the adversarial cell's 2,441 / 101,985 / 0 beside them); the files the OLD generator still writes are rows of the shrink-only fallback register `d11-ir-fallbacks.txt` (0 vendored rows since PR #646: the CHOICE rows 80 &rarr; 0 at PR #646 after the ENUM rows 4,542 &rarr; 0 at PR #642 and the DATA_TYPE rows 17,585 &rarr; 0 at PR #645; 12 rows in all &mdash; 249 rows from PR #645 until PR #646, 19,553 rows before that &mdash; all twelve on the adversarial cell), asserted EQUAL per cell and sub-kind (`IR file writers`), the seam an emitter PR overrides being `IREmittableGenerator.filesWrittenByIrEmitter()`, first overridden at PR #642 by the enum generator. Flag-off both channels print nothing; not one byte moved. *Locked by* `IRDeclarationFactsReconcileTest` (rune-ir-java: every fact witnessed and reconciled through the three wired generators; a THROWING declaration fed through each one is counted and is a mismatch) and the corpus-free `D11IrFallbackRegisterTest` (rune-java-generator: the register's grammar refusals and every verdict direction). **The ENUM emitter (PR #642, v3.3 seat 6):** `IREnumEmitter` + `IREnumIndex` in `rune-ir-java` write every enum file from `IREnumNode` alone, *locked by* `IREnumEmitterTest` (9 tests since PR #643 — 7 at PR #642: byte-identical to the old generator on every arm; a parent outside the generated set; an absent or unresolved parent refused; a name outside its namespace refused; the caret stripped; a mismatched declaration refused with no file written; and since PR #643 the parent refusal's two arms — a parent whose IR disagrees with its source refuses every child across two passes, reconciled once, and a throwing parent is a mismatch carrying its cause) and by `D11IrFallbackRegisterTest`'s ENUM clause, now `assertEquals(0, ...)`; on the corpus by the ON-route `D11CorpusRegressionTest`'s `IR file writers` line, `newEmitter=N oldGenerator=0 declaredFallbacks=0` on every ENUM line of all 26 cells. **The TYPE GATE (PR #643, v3.3 seat 7 &mdash; gate A of the strict path to the data-type emitter, zero bytes):** the reconciler asserts, per TYPE REFERENCE, three new families &mdash; `.resolution` (WHICH leg answered: the declaration, the builtin registry, or unresolved; the old generator's three UNLINKED fallbacks — the alias table, the workspace simple-name scan and the alias search — read `workspace-fallback` and are RED by name; its post-resolution bypass answers a name the linker DID resolve, so it reads `declaration` on both halves and is caught on `.kind` / `.legacyDeclaration` and `.javaType` instead), `.javaType` (`IRJavaTypeNames.of` in `rune-ir-java` derives the canonical Java reference type from the IR facts ALONE &mdash; no AST, no `GeneratorModel`, no `JavaTypeTranslator` &mdash; and it is held EQUAL to `JavaTypeTranslator.toJavaReferenceType(resolveTypeCall(…))`, a refusal on either side reading `REFUSED:<reason>` — the reason token since PR #644, so two refusals for different reasons never read EQUAL) and, on an alias reference, `.effectiveBase.*` against the reconciler's OWN independent walk with a number belt over the parser's own evaluation &mdash; plus `reconcileTypeAlias` for the DECLARATION and a FOURTH per-cell line, `TYPE_ALIAS IR declaration reconcile`, on its own population (1,033 declarations / 19,006 facts / 0 mismatches over the 25 vendored cells, 151 / 2,796 / 0 on the adversarial one; 81,494 vendored type references and 4,433 on the adversarial cell carry the Java-type and resolution facts). *Locked by* `IRTypeGateTest` (rune-ir-java, 19 tests since PR #644 commit 2 — 15 at PR #643 round 1, 14 at its commit 3: every rung of the digits ladder, every builtin and record, every refusal by name and, since PR #644, by its REASON (`REFUSED:<reason>` — the two halves share one vocabulary, the old generator's classed from its own throw sites), the escape refusal, a parameter's own type arguments carried and red twice when dropped, the paired `pattern` refusal, the alias table, the simple-name scan and the alias search RED by the leg's own name, a lying effective base RED on the arguments AND on the Java type, the alias declaration's reconcile) beside `IRDeclarationFactsReconcileTest`; the rune-ir-java suite read 244 tests, 0 failures at PR #644 - 366 over 25 classes since PR #646 commit 5 (365 at commit 4, 354 over 25 at PR #646 commit 3, 351 over 25 at PR #645 commit 18), the ladder in the PROPERTY GATE segment below - (PR #644 commit 5, the pre-review's fixes with their witnesses - the per-property javadoc rendered from the IR alone, the qualifiable root over the workspace-wide `IRModelIndex`, the `*Meta` condition-ref seam, the two-unnamed-conditions fixture, lane P06's doc reference; 229 at PR #644 commit 4, the property gate's index, property model and derived-fact reconcilers with their gate witnesses; 159 at PR #644 commit 2, 155 at PR #643 round 1's L4 witness, 154 at its commit 3). NOT in this PR: an emitter, a byte, a register row &mdash; the file meter is UNMOVED at 2.6% of files (it reads 53.5% of files since PR #645 and 53.8% of files since PR #646). **The PROPERTY GATE (PR #644, v3.3 seat 8 &mdash; gate B of the strict path, zero bytes):** `IRTypeIndex` (the `IREnumIndex` shape over data types, choices and aliases: one node per declaration, an ABSENT or AMBIGUOUS name REFUSED and never first-wins, a parent adapted and reconciled ONCE through a second reconciler whose refusal is memoised for every descendant) and `IRPropertyModel` (the `RJavaPojoInterface` law reproduced PURELY over `IRTypeNode` + the index: the two `LinkedHashMap`s, the parent seed, Case 0's absence, the re-put position, the four compat-name arms, `isPojoSubtype`'s walk, the meta wrap, both parent-first unions, the synthetic `meta` last, the 0..1 inherited choice option, two named refusals) are reconciled property-for-property against the old generator's own POJO, and `IRDerivedFactsReconciler` / `IRModelReconciler` / `IRWrapperReconciler` / `IRJavaLangCollision` hold the per-type DERIVED files' input facts against the four validator generators, `ModelMetaGenerator`, `MetaFieldGenerator`, `JavaPackageInfoGenerator` and `DeepPathScan` (the validator seams in `rune-java-generator` are VISIBILITY ONLY). FIVE new ON-route D11 lines per cell, four asserting ITS OWN population and the parent line a FLOOR (parents &ge; the host's own count of the elements' transitive supertypes, printed as `supertypes=`; the index's reach beyond the floor &mdash; the item types of specialized properties &mdash; is READ): `PROPERTY IR declaration reconcile` (17,665 declarations / 2,435,739 facts / 0 mismatches over the 25 vendored cells, 1,888 / 105,573 / 0 on the adversarial one), `PROPERTY IR parent reconcile` (1,776 parents / 318,645 facts / 0), `DERIVED IR declaration reconcile` (17,665 / 1,667,296 / 0), `MODEL IR declaration reconcile` (3,952 models / 202,502 / 0) and `WRAPPER IR spec reconcile` (1,079 specs = the host's own `collectSpecs().size()` / 6,549 / 0), plus the DERIVED TWO-READS third read (the literal tokens the validators WROTE against the IR walk, the refused elements subtracted by their own target paths, `-> AGREE` on all 26 cells). *Locked by* `IRTypeIndexTest` (10), `IRPropertyModelTest` (26), `IRPropertyGateTest` (12), `IRDerivedFactsReconcileTest` (9), `IRDerivedGateTest` (14), `IRModelReconcileTest` (8) and `IRWrapperReconcileTest` (6) beside `IRTypeGateTest` and `IRDeclarationFactsReconcileTest`; the rune-ir-java suite reads 366 tests, 0 failures over 25 classes since PR #646 commit 5 (365 at commit 4: the choice arm's six and the kind switch's five; 354 over 25 at PR #646 commit 3 - the memo's three arms; 351 over 25 at PR #645 commit 18 - the sharing lint's new AST / workspace witness; 350 at commit 15) (the type unit's own non-data-type split held against `ModelObjectGenerator.streamObjects`, both failure arms witnessed; 349 over 25 at commit 14, whose deep-path util member byte-equal to `DeepPathUtilGenerator` on the seat-9 injection fixture's 11 eligible data types - the two injected sibling utils, the deeper arm, the meta unwrap, a multi feature, the #306 law, the keyword escape and the empty class - and its NO-FILE answer held both ways on the 39 data types of the nine hold-out groups that carry a deep-path golden, NOT ONE of which is eligible; beside the `*Meta` member, the three validator members, the derived facts relocated, the member contract, the five derived seams, the sharing law's lint; 341 over 24 at commit 13, 334 over 23 at commit 12, 318 over 20 at commit 10, 305 over 19 at commit 9, 295 at commit 8, 287 at commit 7, 275 at commit 6, 270 at commit 5, 266 at commit 4, 244 over 17 at PR #644). NOT in this PR: an emitter, a byte, a register row, a fact on any standing reconcile line &mdash; the file meter is UNMOVED at 2.6% of files (4,542 of 174,141), the fallback register at 19,553 rows (ENUM 0 / CHOICE 237 / DATA_TYPE 19,316) and the decline register at 363 rows &mdash; the meter reads 53.5% of files (93,223 of 174,141) and the register 249 rows (ENUM 0 / CHOICE 237 / DATA_TYPE 12) since PR #645, and 53.8% of files (93,703 of 174,141) with the register at 12 rows (ENUM 0 / CHOICE 0 / DATA_TYPE 12) since PR #646. **THE DATA-TYPE EMITTER (PR #645, v3.3 seat 9 &mdash; the ONE all-or-nothing emitter PR of the strict path, NO per-file fallback: the maintainer's decision of 2026-09-18 and its amendment, the `type` file AND its per-type derived files as ONE unit):** THE TYPE UNIT (`IRTypeUnit` in `rune-ir-java`; ONE declaration of its six members and their construction, `IRTypeUnitWiring`, read by all six callers; the five derived passes routed through `IRUnitPass`) writes a data type's SIX files TOGETHER from `IRTypeNode`, `IRPropertyModel`, `IRDerivedFacts` and the workspace-wide indexes ALONE, or REFUSES the type BY NAME and leaves all six to the old generator: the POJO (`IRDataTypeEmitter` &mdash; every section of the file a pure function of the type's reconciled facts, the compat arms included), the three validators (`IRTypeFormatValidatorEmitter`, `IRCardinalityValidatorEmitter`, `IROnlyExistsValidatorEmitter`), the `*Meta` (`IRModelMetaEmitter`) and the deep-path util (`IRDeepPathUtilEmitter`, whose NO FILE for a type the eligibility fact refuses is the ONE lawful empty answer of the six; every other empty answer, and any throw, refuses the WHOLE unit &mdash; a partial map is never returned). The verdict is memoised per node, so every kind's ask gets the same answer within a pass, and the six passes' ATTEMPTED and REFUSED name sets are held EQUAL by the D11 host (`TYPE UNIT VERDICTS`; the DEEP_PATH pass a NARROWER population by construction &mdash; the old generator streams the eligible types alone &mdash; so its set is held a SUBSET of the others', a contract erratum of commit 15; the other erratum: run A read SIX healed lines per cell, the derived kinds' owner-row projections healing with the DATA_TYPE line). The switch `IRTypeUnitWiring.AVAILABLE` (commit 14's second declaration, ON since commit 15) is the whole-route kill switch: off, every data type goes back to the old generator byte-identically, but the register is shrink-only, so the way back is a revert of the commit, register and all. The POJO pass's non-data-type half is EMPTY BY CONSTRUCTION (`ModelObjectGenerator.streamObjects` yields the data types alone) and is replaced by a POPULATION ASSERTION holding the unit's walk equal, element for element and in order, to the inherited generator's own (a hole, a double writer or an order disagreement throws by name). The Maven plugin is routed through every seam site for site as the D11 host is (commit 14b: `RunePluginRunner`'s twelve routable construction sites, the route header a pure function, one observability line per kind). THE RUNS OF RECORD at commit 15 (`e17daf61d`: the chain s9c15, the offload box run s9c15 and the overlay, pinned by the head-run receipt): `UNIT READY` 26 / 0 with `members 6 of 6 ; AVAILABLE=true`; `TYPE UNIT VERDICTS` attempted == the cell's data types on every line, 19,316 over the 26 cells, one line naming refused types (the adversarial cell); the writer lines &mdash; DATA_TYPE `newEmitter` 19,304 over the 26 cells with `oldGenerator` 0 on every vendored line and 12 on the adversarial one, ONLY_EXISTS / CARDINALITY / TYPE_FORMAT / XMETA 19,304 each, DEEP_PATH 773 (the `WRITERS CROSS-CHECK`: the four validator / meta kinds read the DATA_TYPE line cell for cell, `oldGenerator == declaredFallbacks` on every DATA_TYPE line); the six `UNIT SHADOW` gates &mdash; their oracle the LEGACY generator's own render of the same cell, built directly from the legacy class beside the unit's pass on the same run (commit 18; from commit 15 to commit 17 the pass's own output at a written key was the unit's own text, so the compare was the unit against itself and could not fail &mdash; round 1 cq MF-1; a `differing == 0` assertion by name now stands beside the sum law, the two proved independent in both directions by the lanes) &mdash; (TYPE_FORMAT identical 19,304 + refusedExpected 12; CARDINALITY, ONLY_EXISTS and META 19,316; DEEP_PATH 773 + 18,543 notEligible; the POJO 19,300 + 16 noGolden &mdash; the `POJO MEMBER GATE` 0 / 26); `DERIVED SPLIT` the six fact families EXACT (1,753,950 / 116,802 / 116,802 / 19,553 / 19,553 / 19,553); the register 19,553 &rarr; 249 rows FROM THE PRINT (run A's dump: ENUM 0 / CHOICE 237 EXACT / DATA_TYPE 12 &mdash; and 249 &rarr; 12 rows at PR #646 commit 5, the CHOICE rows 237 &rarr; 0 &mdash; every one on the adversarial cell, the eleven `C27Holder` and the one `C99Holder` `BOILERPLATE_NAME_COLLISION` types the unit refuses whole; the vendored DATA_TYPE rows 0), all 78 (cell, sub-kind) keys judged; BOTH RINGS `9062fc14` = 34,685 / 0 / 1 on both routes, MATRIX-FULL `5aa6bbb0`, matrix25 `d47fa88f`, identical25 174,129, ROUTE ROW DIFF NONE &mdash; with the unit ON, not one generated byte moved: the 88,681 files the unit writes on the 25 vendored cells (17,585 &times; 5 + 756) are the old generator's bytes; the file meter COMPUTED by the chart regenerator from the register and the print-typed constants reads 53.5% of files (93,223 of 174,141 &mdash; the enums 4,542, the data types 17,585 and the data types' derived files 71,096) &mdash; and 53.8% of files (93,703 of 174,141) since PR #646, the 80 choices and their 400 derived files joining. *Locked by* `IRTypeUnitTest` (rune-ir-java, 17: the six output keys, a six-stub unit available and writing six files, every partially-ready unit from none to five refusing and naming what is missing, a throwing member and a no-text member refusing the whole unit, the wiring's six of six with the switch ON, the unit loop held against the inherited generator's own population on both failure arms, the real deep-path and meta members' present-text law, the one lawful no-file answer, the memo's once-per-member law, a refused node staying refused, two units disagreeing and the inspection path), `IRDataTypeEmitterTest` (48: every section of the POJO byte-equal to the old generator on every decidable type, the setter matrix, the collision pair, the hold-out fixtures, a choice refused by name), `IRValidatorEmittersTest` (4: the seat fixtures and all twelve hold-out groups byte-equal, every refusal at a site the old generator also refuses at, no validator member ever answering no file), `IRModelMetaEmitterTest` (4: the seat fixtures and every hold-out group with a meta golden byte-equal, the meta member never answering no file, the `java.lang` collision law), `IRDeepPathUtilEmitterTest` (4: byte-equal on every validated data type and every hold-out group with a deep-path golden, the injection fixture's two dependencies, the deeper arm and the meta unwrap, the only empty answer for a type the old generator writes no file for) and `RunePluginRunnerRouteTest` (rune-maven-plugin, 5: the route header's three arms by exact text, flag-off running the legacy classes and writing the 47 goldens byte for byte, the reference route ON selecting the IR-routed classes and moving not one byte, the optimised flag keeping the provider's declines, both flags loud through the runner); on the corpus by the ON-route `D11CorpusRegressionTest`'s `UNIT READY`, `TYPE UNIT VERDICTS`, the six `UNIT SHADOW[<member>]` gates, the `POJO MEMBER GATE`, the five derived kinds' `IR file writers` lines with the `WRITERS CROSS-CHECK` and `DERIVED SPLIT`. NOT in this PR: the choices (the register's 237 CHOICE rows and their derived files &mdash; the next unit, PR #646 below), the metafield wrappers and package-info (units of their own, after it). **THE CHOICE UNIT (PR #646, v3.3 seat 10 &mdash; THE TYPE UNIT GENERALISED to the CHOICE kind, the strictest option: ONE `IRTypeUnit`, ONE `IRUnitPass`, ONE wiring, the same six members and six output keys, no second unit class and no per-file fallback):** the POJO member (`IRDataTypeEmitter`) admits a CHOICE `IRTypeNode` through a `node.kind()` arm at each of the legacy `ModelObjectGenerator`'s `type == null` sites (the `RuneChoiceType` import as a claim, no `choiceSuperType`, the javadoc from the definition with an EMPTY doc-reference list, the bare `@RuneChoiceType` line after `@RuneDataType`; the meta-class symbol needs no arm; a SIXTH difference the fixture found &mdash; the choice javadoc's `@version` is the DECLARING model's &mdash; is pinned and inert on every route), and the five derived members render a choice UNCHANGED from the choice-aware `IRDerivedFacts` / `IRPropertyModel` of PR #644; a SECOND, kind-scoped switch `IRTypeUnitWiring.CHOICE_AVAILABLE` beside `AVAILABLE` (`IRTypeUnit.available(kind)`) split the landing into TWO commits with their own runs of record, the #645 commit 14 &rarr; 15 shape: commit 4 the arm and the CHOICE SHADOW (six `... CHOICES` lines per cell, each gated against a SECOND REAL PRODUCER &mdash; the legacy `ChoiceObjectGenerator` rendered beside the pass as `LEGACY ORACLE[POJO-CHOICE]`, and the legacy derived generators &mdash; with a SURPLUS arm naming a shadow key no population claims: 237 of 237 choices IDENTICAL to the legacy render on the five derived members and 235 identical + 2 no-golden on the POJO member over the 26 cells, 0 differing, 0 refused, NOT ONE chaos choice refused; the routing law UNCHANGED, `TYPE UNIT VERDICTS` proving BY NAME that no pass's attempted set carried a choice), commit 5 THE ROUTE ON (the routing law ONE declaration for seven callers &mdash; `IRUnitPass.routeWith` admits `RChoice`, `IRChoiceObjectGenerator` routing ITS population through the same loop with the population assertion over its own `streamObjects`; `attempted == dataTypes + choices` per full pass; the register's 237 CHOICE rows to ZERO FROM RUN A's DUMP by `make-fallbacks-s10.py` &mdash; the vendored rows asserted zero before a byte was written, the eight CDM cells DISAPPEARED from the register, 12 rows since: ENUM 0 / CHOICE 0 / DATA_TYPE 12, all on the adversarial cell; the chart regenerator's choice legs gated on the CHOICE rows reading zero) &mdash; the meter reads 53.8% of files since (93,703 of 174,141: the 4,542 enum files, the 17,585 data-type files with their 71,096 derived files, and the 80 choice files with their 400 derived files, every one written by a NEW IR emitter from the IR alone); both rings `9062fc14` on both routes, matrix25 `d47fa88f`, identical25 174,129, ROUTE ROW DIFF NONE at every commit's runs of record &mdash; not one generated byte moved. NOT in this PR: the metafield wrappers and package-info (their own units), the label providers.

The route reaches all five IR-variant kinds on every corpus cell the D11
harness drives (55 kind-cell parameters over the uniform 9.83.0 5-cell
matrix). Since the PR-3 drift wave the flag-on gate is FULLY GREEN:
55/55 kind-cells, 34,686/34,686 byte-identical, population lines ≡ the
#437 cp SOT (`target-467-cpON3.log`; re-proven at PR-4 with the reader
set + share instrumentation live — `target-468-cpON1.log` — and again
through the #469 share-growth wave: `target-469-cpON{1,2,3,5}.log`, each
green loop's populations byte-identical to the SOT, with the ONE
intentionally-failing loop `target-469-cpON4.log` kept as the drift
receipt that set the nested-delegation composition boundary — and
through the #470 dispatchVariant teach: `target-470-cpON1.log` (the
narrowed call-bearing intermediate, dispatchVariant 34/34/0) +
`target-470-cpON2.log` (the retirement, dispatchVariant 0/0/0), both
55/55 with populations byte-identical to the SOT, zero drift loops — and
through the #471 claim-root meta-arg teach: `target-471-witness1.log`
(the decode census, 136 witness lines ≡ the 43/29/64 guard totals) +
`target-471-witness2.log` (the FUNCTION-only post-teach reading, 67
root-delegated lines = 51 + 16) + `target-471-cpON1.log` (the full ON
ring, 55/55, populations ≡ the SOT first-try, zero drift loops) — and
through the #472 composition-boundary widening: `target-472-witness1.log`
(the pre-teach census re-proof on the #471 state, 136 = 67 + 69) +
`target-472-witness2.log` (the post-teach FUNCTION-only reading, 136 =
67 `-rootDelegated` + 68 `-delegated` + 1 decline; the retensed locks
green) + `target-472-cpON1.log` (the full ON ring, 55/55, populations ≡
the SOT FIRST-TRY, zero drift loops) — and through the #473
singletonList endgame teach: `target-473-witness1.log` (the
FUNCTION-only post-teach reading, 136 = **68** `-rootDelegated` + 68
`-delegated` + **0** declines — the FUNCTION decline meter ALL-ZEROS
with the three-cell `declined == 0` pins live-green) +
`target-473-rulewitness1.log` / `target-473-rulewitness2.log` (the
RULE-seam decode pair — 23 witness lines all drr, 13 router
delegations + 10 intLiteral claim-root declines; the pre/post sets
diff EMPTY, the teach RULE-seam-inert) + `target-473-cpON1.log` (the
full ON ring, 55/55, populations ≡ the SOT FIRST-TRY, zero drift
loops) — and through the #474 RULE-seam intLiteral teach:
`target-474-rulewitness1.log` (the post-teach RULE-seam capture — 23
witness lines = 13 `-delegated` + 10 `-rootDelegated` + **0**
declines, all drr, the #473 census head-for-head; the POJO ring 5/5
byte-green under it and the NEW reader-block pins live-green) +
`target-474-cpON1.log` (the full ON ring, 55/55, populations ≡ the
SOT FIRST-TRY — the wave's FOURTH consecutive; the FUNCTION dial and
meters byte-unchanged, the RULE seam's first share reading drr
45.37%) — and through the #475 census meters:
`target-475-irjavainstall1.log` (the module suite 55/0/0/0 — the 3
census unit locks joined) + `target-475-cpON1.log` (the full ON ring,
55/55, populations ≡ the SOT FIRST-TRY — the wave's FIFTH consecutive
— with every share/meter line byte-identical to the #474 receipts
[sorted-diff empty, the instrumentation behavior-inert] and the first
census reading printed on all ten seat-cells) + `target-475-cp1.log`
(the OFF ring, 55/55 ≡ the SOT, route header `OFF`, zero share/meter/
census lines — the flag-off inertness re-proof) — and through the #476
adapterGap blocker probe: `target-476-irjavainstall1.log` (the module
suite 58/0/0/0 — the 3 probe locks joined first-try on the recut
fixtures) + `target-476-cpON1.log` (the probe-off ON ring, 55/55,
populations ≡ the SOT with every share/meter/census line
byte-identical to the #475 receipts and ZERO probe lines) +
`target-476-cpONprobe1.log` (the PROBED ON ring — 55/55, populations ≡
the SOT and the standing instrumentation byte-unchanged under the
probe, the read-only proof at full population; the first blocker
reading: probed 3,601/7,277/10,171/13,316 = 34,365 ≡ the adapterGap
census exactly, anomalies 0 on every cell, sole-blocker claims 33,250
led by `REnumValueRef` 13,395 · `RSymbolReference` 11,411 ·
`RFeatureCall` 5,426) + `target-476-cp1.log` (the OFF ring, 55/55 ≡
the SOT, route header `OFF`, zero IR lines) — and through the #477
per-ARM gate decode: `target-477-irinstall1.log` (the rune-ir module
suite 393/0/0/4 — the 3 adapter-mirror locks joined first-try) +
`target-477-irjavainstall1.log` (the module suite 59/0/0/0 — the
same-family multi-reason conservation lock joined first-try) +
`target-477-cpON1.log` (the probe-off ON ring, 55/55, populations ≡
the SOT with every IR line byte-identical to the #476 receipts and
ZERO probe lines) + `target-477-cpONprobe1.log` (the PROBED ON ring —
55/55, populations ≡ the SOT, the #476 probe lines byte-unchanged;
the first gate reading: unattributed 0 on every cell, the
conservation identity zero violations over all four cell-seams,
sole-reason 33,028 + 222 multi-reason ≡ the sole 33,250 exactly, led
by `REnumValueRef:inputFeatureNav` 9,008 ·
`RFeatureCall:receiverSyntheticItem` 4,643 ·
`REnumValueRef:attributeChain` 4,088 ·
`RSymbolReference:symbolUnresolved` 3,921) + `target-477-cp1.log`
(the OFF ring, 55/55 ≡ the SOT, route header `OFF`, zero IR lines) —
and through the #478 disguised-input-nav teach:
`target-478-irinstall2.log` (the rune-ir module suite 394/0/0/4 — the
arm lock joined) + `target-478-irjavainstall6.log` (the module suite
60/0/0/0 — the nav-gate witness lock joined) +
`target-478-parserinstall1.log` (4,194/0/0/5 — the RWorkspace
attribute-type delegate rode the parser suite) +
`target-478-cpONprobe1.log` (the PRE-teach witness reading:
`inputFeatureNav:headInput:lowers` 2,157/3,878/5,224 = 11,259
blocker-node occurrences = the claimable mass, with the alias-head
slice 2,682 and the whole attributeChain channel measured
inexpressible) + `target-478-cpON6.log` (the post-teach ON ring —
55/55, populations ≡ the SOT; the teach signature: adapterGap
3,066/6,732/7,768/13,316, leafEmitter 377/1,812/2,416/398, the dial
56.88/63.27/66.02 + RULE 45.37 on the absorption-shrunken event
denominator) + `target-478-cpONprobe2.log` (the POST-teach probed
re-read — the conservation signature: the taught gate ZERO on every
cell, every other witness bucket byte-identical, probed 30,882 ≡ the
new adapterGap exactly, anomalies 0, untargeted byte-unchanged) +
`target-478-cp1.log` (the OFF ring, 55/55 ≡ the SOT, route header
`OFF`, zero IR lines) + `target-478-gensuite1.log` (3,720/0/0/53) —
and through the #479 synthetic-item receiver teach:
`target-479-irinstall3.log` (the rune-ir module suite 395/0/0/4 — the
retype-arm lock joined) + `target-479-irjavainstall4.log` (the module
suite 62/0/0/0 — the implicit-root witness + end-to-end arm locks
joined) + `target-479-cpONprobe2.log` (the PRE-teach implicit-root
reading: the whole cluster decoded to the untyped synthetic item —
every synthetic bucket `typeMissing` — with the claimable slice
`inLambdaImplicit.filterExtract:retypeSourceOk` 96/361/1,144/2,557 =
4,158 blocker nodes conserving to the 4,643 sole EXACTLY) +
`target-479-cpON3.log` (the post-teach ON ring — 55/55, populations ≡
the SOT; the teach signature: adapterGap 3,008/6,535/7,035/13,246 =
29,824, the dial 57.61/64.06/68.46 + RULE 45.65, driven +1,042 on the
absorption-re-based attempted 7,984/23,262/29,973/25,108) +
`target-479-cpONprobe3.log` (the POST-teach probed re-read — the
conservation signature: `retypeSourceOk` ZERO on every cell, probed
29,824 ≡ the new adapterGap exactly, anomalies 0, sole
`receiverSyntheticItem` 4,643 → 3,577, the `unprovableSource` residue
3,092, and the #480 pre-sizing live: `bareAttr:itemNav:lowers` 336 +
`chainFall:itemChain:lowers` 370) + `target-479-cp1.log` (the OFF
ring, 55/55 ≡ the SOT, route header `OFF`, zero IR lines) +
`target-479-gensuite1.log` (3,720/0/0/53) — and through the #480
bare-attr + chain arms: `target-480-irjavainstall1.log` (63/0/0/0 —
the source-decode & arm-restatement lock joined) +
`target-480-cpONprobe1.log` (the PRE-teach reading:
`bareAttrArm:claims` 57/86/138/55 = 336 ≡ the #479 pre-sizing EXACTLY,
`chainArm:claims` 39/50/229/52 = 370 ≡ EXACTLY — zero admission
residue; the source decode: `elidedImplicit` 2,889/3,092 = 93.4% of
the synItem residue, 5,363 nodes over the four channels, every
channel conserving to its standing buckets) +
`target-480-irinstall1.log` (the rune-ir suite 396/0/0/4 — the arm
lock joined) + `target-480-irjavainstall5.log` (64/0/0/0 — the e2e
driven lock joined) + `target-480-cpON2.log` (the post-teach ON ring
— 55/55 ≡ the SOT with ZERO mismatches, the wave's first
first-try-clean arm ring) + `target-480-cpONprobe2.log` (the
POST-teach probed re-read — the conservation signature) +
`target-480-cpON7.log`/`target-480-cpONprobe4.log` (the FINAL ring
after the Seat-1 MF wave — 55/55 ≡ the SOT; the teach signature:
adapterGap 2,978/6,492/6,833/13,196 = 29,499, the dial
57.57/64.00/68.50 + RULE 45.60 on the absorption-re-based attempted
7,905/23,093/29,375/24,992 — the third re-base, driven 50,851 with
+325 whole claims and the vanished 962 L-113 relabel events stated;
both `claims` and both `lowers` buckets ZERO on every cell, probed
29,499 ≡ the new adapterGap exactly, anomalies 0, sole 28,474 =
96.52% with sole-reason 28,274 + 200 ≡ exact and the ranking inverted
an eleventh time) + `target-480-cp1.log` (the
OFF ring, 55/55 ≡ the SOT, zero IR lines) +
`target-480-gensuite1.log` (3,720/0/0/53); and through the #481
elided-pipe widening: `target-481-irjavainstall1..4.log` (the
pipe-decode lock joined — 65/0/0/0 from install2, the separator recut;
install4 = the widened-restatement recut) + `target-481-cpON1.log`
(pre-arm probe-off — every IR line byte-identical to the #480 cpON7
with ZERO pipe lines) + `target-481-cpONprobe1/2.log` (the PRE-teach
reading — cpONprobe2 the widened-restatement re-read, byte-identical
to cpONprobe1 at population: `thenArg.provable.typeOk` 12/106/89/91 =
298 nodes, `typeMissing`/`typeMeta` ZERO, Σ pipe ≡ the flat
`elidedImplicit` counts on all 13 cell-channel pairs, the residue map
sized) + `target-481-irinstall1.log` (the rune-ir suite 397/0/0/4 —
the widening lock joined) + `target-481-irjavainstall5.log` (65/0/0/0
— the lock recut to the post-teach driven state) +
`target-481-cpON2.log` (the post-teach ON ring — 55/55 ≡ the SOT with
ZERO mismatches, the second consecutive first-try-clean arm ring; the
teach signature: adapterGap 2,969/6,414/6,756/13,126 = 29,265, the
dial 57.63/64.27/68.70 + RULE 45.75 on the absorption-re-based
attempted 7,896/23,048/29,323/24,939 — the fourth re-base, driven
50,920 with +69 whole claims, the headline 48.03%) +
`target-481-cpONprobe3.log` (the POST-teach conservation signature:
`thenArg.provable.*` ZERO everywhere with every residue bucket
byte-identical, probed 29,265 ≡ the new adapterGap, anomalies 0, sole
28,241 = 96.50% with sole-reason 28,043 + 198 ≡ exact, the elided
channels −298 to the digit, `RImplicitVariable` untargeted 8,602 →
8,464 the sole family moved) + `target-481-cp1.log` (the OFF ring,
55/55 ≡ the SOT, zero IR lines) + `target-481-gensuite1.log`
(3,720/0/0/53); and through the #482 nested-thenPipe widening:
`target-482-irjavainstall1.log` (the body-decode lock joined —
66/0/0/0, every pin green FIRST RUN) + `target-482-cpON1.log`
(pre-arm probe-off — every IR line byte-identical to the #481 cpON2
SOT) + `target-482-cpONprobe1.log` (the PRE-teach reading:
`thenPipe.provable.typeOk` = 97 occurrences [synItem 50 · bareAttr 41
· chainBase 6], `typeMissing`/`typeMeta`/`noBody` ZERO, Σ thenPipe.*
≡ the #481 flat counts on every channel [289/765/1,055 = 2,109], the
body-shape residue map sized [extract-bodied 1,435 dominant]) +
`target-482-irinstall1.log` (the rune-ir suite 398/0/0/4 — the
widening lock joined) + `target-482-irjavainstall2.log` (66/0/0/0 —
the lock recut to the post-teach driven state) +
`target-482-cpON2.log` (the post-teach ON ring — 55/55 ≡ the SOT with
ZERO mismatches, the THIRD consecutive first-try-clean arm ring; the
teach signature: adapterGap 2,945/6,395/6,743/13,110 = 29,193, the
dial 57.79/64.31/68.73 + RULE 45.80 on the absorption-re-based
attempted 7,868/23,024/29,311/24,933 — the fifth re-base, driven
50,922 with +2 net whole claims, the headline 48.08%) +
`target-482-cpONprobe2.log` (the POST-teach conservation signature:
`thenPipe.provable.typeOk` ZERO everywhere with every residue bucket
byte-identical, probed 29,193 ≡ the new adapterGap, anomalies 0, sole
28,171 = 96.49% with sole-reason 27,973 + 198 ≡ exact, the flat
elided channels −97 to the digit with Σ pipe ≡ flat,
`RImplicitVariable` untargeted 8,464 → 8,418 the sole family moved) +
`target-482-cp1.log` (the OFF ring, 55/55 ≡ the SOT, zero IR lines) +
`target-482-gensuite1.log` (3,720/0/0/53); and through the #483
extract-bodied pipe widening: `target-483-irjavainstall1.log` (the
extract-body decode lock joined — 67/0/0/0, every pin green FIRST
RUN) + `target-483-cpON1.log` (pre-arm probe-off — every IR line
byte-identical to the #482 cpON2 SOT) + `target-483-cpONprobe1.log`
(the PRE-teach reading: `extractPipe.provable.typeOk` = 351 of 2,188
[flat 81 + nested 270; synItem 181 · chainBase 88 · bareAttr 82],
`typeMissing`/`typeMeta`/`noBody`/`literalItem` ZERO, Σ facets ≡
each flat count on every cell-channel [753 + 1,435], the residue map
sized [`symbol:RFunction` 1,080 dominant · conditional 435 · call
145 · metaLeaf 139]) + `target-483-irinstall1.log` (the rune-ir
suite 399/0/0/4 — the widening lock joined) +
`target-483-irjavainstall2.log` (67/0/0/0 — the lock recut to the
post-teach driven state) + `target-483-cpON2.log` (the post-teach ON
ring — 55/55 ≡ the SOT with ZERO mismatches, the FOURTH consecutive
first-try-clean arm ring; the teach signature: adapterGap
2,945/6,394/6,722/12,753 = 28,814, the dial 57.79/64.31/68.78 +
RULE 46.60 on the absorption-re-based attempted
7,868/23,024/29,293/24,641 — the sixth re-base, driven 50,991 with
+69 whole claims, the headline 48.37%) +
`target-483-cpONprobe2.log` (the POST-teach conservation signature:
`extractPipe.provable.typeOk` ZERO everywhere at both seats, probed
28,814 ≡ the new adapterGap, anomalies 0, sole 27,798 = 96.47% with
sole-reason 27,597 + 201 ≡ exact, the flat elided channels −486
closing per channel to the digit [the 351 direct claims + the
135-occurrence recursive composition class], `RImplicitVariable`
untargeted 8,418 → 8,241 the sole family moved) +
`target-483-cp1.log` (the OFF ring, 55/55 ≡ the SOT, zero IR lines) +
`target-483-gensuite1.log` (3,720/0/0/53); and through the #484
bare-function-application pipe widening:
`target-484-irjavainstall1.log` (the callee-output facet lock joined
— 68/0/0/0, every pin green FIRST RUN) + `target-484-cpON1.log`
(pre-arm probe-off — every IR line byte-identical to the #483 cpON2
SOT) + `target-484-cpONprobe1.log` (the FACET decode: the WHOLE
1,125-occurrence population ADMITS — `outputData` 1,070 +
`outputDataMulti` 55, the decline faces ALL ZERO, Σ facets ≡ each
flat count per cell-channel) + `target-484-irjavainstall2.log`
(68/0/0/0 — the planned-tier recut, green FIRST RUN) +
`target-484-cpON2.log` (probe-off ≡ the SOT after the tier) +
`target-484-cpONprobe2.log` (the CLAIMABLE-SLICE pre-read:
`provable.typeOk` 1,127 = extractPipe 1,088 [nested 795 = 787 direct
+ 8 listLiteral compositions · flat 293] + bare thenArg 12 +
thenPipe 27 [the recursive filter class pre-composed],
`typeMissing`/`typeMeta` ZERO, the non-pipe families byte-identical)
+ `target-484-irinstall1.log` (the rune-ir suite 400/0/0/4 — the
widening lock joined, the predicted meta-face token green FIRST RUN)
+ `target-484-irjavainstall3.log` (68/0/0/0 — the lock recut to the
post-teach driven state) + `target-484-cpON3.log` (the post-teach ON
ring — 55/55 ≡ the SOT with ZERO mismatches, the FIFTH consecutive
first-try-clean arm ring; the teach signature: adapterGap
2,945/6,390/6,722/11,888 = 27,945, the dial 57.79/64.33/68.78 +
RULE 48.87 on the absorption-re-based attempted
7,868/23,024/29,293/24,039 — the seventh re-base, driven 51,258 with
+267 whole claims, the headline 49.03% — the first 49% crossing) +
`target-484-cpONprobe3.log` (the POST-teach conservation signature:
every pipe-channel `symbol:RFunction` token ZERO with the falls ≡
the pre-read 1,127 to the digit + all 25 source-channel occurrences
claimed, probed 27,945 ≡ the new adapterGap, anomalies 0, sole
26,940 = 96.40% with sole-reason 26,731 + 209 ≡ exact, the flat
elided channels −1,127 ≡ the claims, `bareFunctionReference` 2,143
EXACTLY unmoved [the direct render-seat class, as decoded],
`RImplicitVariable` untargeted 8,241 → 7,956 the sole family moved) +
`target-484-cp1.log` (the OFF ring, 55/55 with the identical= line-set
≡ the #437 SOT, zero IR lines) +
`target-484-gensuite1.log` (3,720/0/0/53); and through the #485
rule-input widening: `target-485-irjavainstall1.log` (the rule-top
facet lock joined — 69/0/0/0, the flat carrier's dual-channel pin
recut run-first-then-pin) + `target-485-cpON1.log` (pre-arm
probe-off — every IR line byte-identical to the #484 cpON3 SOT) +
`target-485-cpONprobe1.log` (the FACET decode: the WHOLE
1,038-occurrence direct population ADMITS — `noBinder.fromData` 652
synItem + 386 ruleInputLambda, every decline face ZERO, the second
consecutive all-admit decode; the flat filter class 318 = 294
composable + 24 nonThen; Σ ≡ every flat count [1,038 / 428 / 3,355]
with the non-refined tokens byte-identical) +
`target-485-irjavainstall2.log` (69/0/0/0 — the planned-tier recut,
green FIRST RUN) + `target-485-cpON2.log` (probe-off ≡ the SOT after
the tier) + `target-485-cpONprobe2.log` (the CLAIMABLE-SLICE
pre-read: 1,336 channel events = the direct 1,038 at the facet +
`thenArg.provable.typeOk` +294 + `thenPipe.provable.typeOk` +4,
`typeMissing`/`typeMeta` ZERO, the non-pipe families byte-identical)
+ `target-485-irinstall1.log` (the rune-ir suite 401/0/0/4 — the
widening lock joined) + `target-485-irjavainstall3.log` (69/0/0/0 —
the lock recut to the post-teach driven state, green FIRST RUN) +
`target-485-cpON3.log` (the post-teach ON ring — 55/55 ≡ the SOT
with ZERO mismatches, the SIXTH consecutive first-try-clean arm
ring; the teach signature: adapterGap 2,945/6,390/6,722/11,066 =
27,123, the dial 57.79/64.33/68.78 + RULE 51.87 on the
absorption-re-based attempted 7,868/23,024/29,293/23,833 — the
eighth re-base, driven 51,874 with +616 whole claims, the headline
50.01% — the first 50% crossing) + `target-485-cpONprobe3.log` (the
POST-teach conservation signature: every claim face ZERO with the
falls ≡ the pre-read 1,336 to the digit — the `ruleInputLambda`
channel emptied 518 → 0 whole, `synItem` −818 — probed 27,123 ≡ the
new adapterGap, anomalies 0, sole 26,118 = 96.29% with sole-reason
25,985 + 133 ≡ exact, `ruleInputNav` 432 → 80 with `ruleInputChain`
674 EXACTLY unmoved [the enum-chain seat, out of scope as decoded],
`RImplicitVariable` untargeted 7,956 → 7,361 the sole family moved)
+ `target-485-cp1.log` (the OFF ring, 55/55 with the identical=
line-set ≡ the #437 SOT, zero IR lines) +
`target-485-gensuite1.log` (3,720/0/0/53); and through the #487
conditional JOIN widening (the #486 decode's receipts are cited in
its own paragraph — a probe-only wave, no teach ring):
`target-487-lockrun1.log` (the decode lock green FIRST RUN — all
four carriers reading the predicted facet spellings) +
`target-487-irjavainstall1.log` (the decode lock joined — 70/0/0/0)
+ `target-487-cpON1.log` (pre-arm probe-off — the identical=
line-set ≡ the #437 SOT) + `target-487-cpONprobe1.log` (THE
READING: the claimable slice 201 of 507 — sameType.typeOk 135 +
elseEmpty.typeOk 52 + formGap.typeOk 14, typeDiffers ZERO,
typeMissing/typeMeta ZERO; Σ facets ≡ the flat 68/198/237/4 per
composition class + the 68 sources twins; the full probe-line diff
vs #486 showing 74 changed keys ALL conditional) +
`target-487-irinstall1.log` (the rune-ir suite 403/0/0/4 — the
JOIN-law lock joined, all four faces green FIRST RUN) +
`target-487-lockrun2..8.log` (the run-first disclosure ladder for
the mixed fixture's post-teach channels) +
`target-487-irjavainstall2.log` (70/0/0/0 — the lock recut to the
post-teach driven state) + `target-487-cpON2.log` (the post-teach ON
ring — 55/55 ≡ the SOT with ZERO mismatches, the SEVENTH consecutive
first-try-clean arm ring; the teach signature: adapterGap
2,945/6,387/6,694/10,926 = 26,952, the dial 57.79/64.34/68.85 +
RULE 52.22 on the absorption-re-based attempted
7,868/23,021/29,271/23,712 — the ninth re-base, driven 51,899 with
+25 whole claims, the headline 50.14%) +
`target-487-cpONprobe2.log` (the POST-teach conservation signature:
every claim face FELL to ZERO with every decline face unmoved to
the digit — 236 fallen channel events ≡ the pre-read EXACTLY,
probed 26,952 ≡ the new adapterGap, anomalies 0, sole 25,947 =
96.27% with sole-reason 25,818 + 129 ≡ exact,
`receiverSyntheticItem` 1,749 → 1,640 + both
`sourceElementUnresolved` gates cut −53/−37, `RImplicitVariable`
untargeted 7,361 → 7,275 the sole family moved,
`RConditionalExpr` untargeted UNCHANGED 1,133) +
`target-487-cp1.log` (the OFF ring, 55/55 with the identical=
line-set ≡ the #437 SOT, zero IR lines) +
`target-487-gensuite1.log` (3,720/0/0/53)). The #488 argNav decode
(probe-only, like #486 — no teach ring): `target-488-lockrun1..3.log`
(the two-step run-first disclosure ladder — the clean first read
exposed the MISSING-sentinel mechanism, the MF-1 live-detector
recut re-ran green with `argNav.typeMissing.param.hop1` pinned;
`lockrun4` the R1 grammar-comment compile-check) +
`target-488-irinstall1..3.log` (the rune-ir suite 404/0/0/4 — the
6-pin decode lock joined FIRST RUN; the post-MF recut and the R2
`rootMissing` belt each re-proven) +
`target-488-cpON1..3.log`
(probe-off — the identical= line-set ≡ the #437 SOT byte-for-byte,
every cut) + `target-488-cpONprobe1..3.log` (THE READING + the
conservation: Σ facets ≡ 2,300 sole / 2,438 participation TO THE
DIGIT on all 8 channels, the full 101-line probe-surface diff vs
the #487 log argNav-only [the 101 lines = the D11-IR-prefixed probe
lines], probed ≡ adapterGap 26,952, census + walk UNCHANGED; probe2
≡ probe1 line-for-line — the live `typeMissing.` detector reads the
same corpus, the ZERO now live evidence; probe3 ≡ probe2 — the R2
`rootMissing` belt population-inert by construction) +
`target-488-cp1..2.log` (the OFF ring ≡ SOT, both cuts) +
`target-488-irjavasuite1.log` (rune-ir-java 70/0/0/0 — the neighbor
suite unchanged) + `target-488-gensuite1.log` (3,720/0/0/53).
The
historical 34-file first reading and its decode remain in the PR #466
receipts (`target-466-ab-{cdm5,cdm6,drr}.txt` +
`target-466-ab-samples.txt`, repo-root receipt logs). The
rule/report/labelProvider generators and the
validator/registry/deep-path/package-info kinds have no IR variants (they
run Path-1 under both flags; the rule-family divergence entered via the
IR-routed FunctionGenerator those generators wrap — closed with the
same wave).

## Cross-references

- **U012 IR model + adapters (the rune-ir module)**
  (`docs/upgrades/U012-ir-scaffolding.md`) — the substrate this route
  consumes
- **D21 / D30 / D31** (`rebaseline.md`, local) — IR scaffolding + Java
  codegen as first IR consumer + the 7-consumer roster
- **D43** — the post-parity IR phase + module family (rune-ir ·
  rune-ir-java · future rune-ir-java-optimised / rune-ir-python)
- **The lab provenance** — ported by COPY from the frozen IR-lab tree at
  its HEAD `8c14592` (2026-07-01; vendored parser pin `3c60acec` =
  PR #279); the lab's in-generator flag dispatch inverted to the
  ServiceLoader seam at port time (PR #466)
- **W22 cross-DSL interop** · **W26 Codegen transparency** · **W28
  Incremental indexing** · **W37 Multi-target IR codegen**
  (`docs/p0/wishlist.md`)
