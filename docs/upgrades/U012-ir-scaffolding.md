---
id: U012
subject: IR (Intermediate Representation) — model, adapters, serialization contract + emitter SPI (the rune-ir module)
since_fork_version: "0.1.0"
since_pr_at_spec: 39
since_pr_actual: null
since_sha: null
status: Available
migration_depth: None
removal_target: null
future_deprecation_risk: low
related_d_entries: ["D21", "D22", "D43"]
related_audit_hooks: ["H2", "H11"]
related_parity_constraints: ["#1", "#5", "#6", "#16", "#17"]
doc_path: U012-ir-scaffolding.md
---

# U012 — IR (Intermediate Representation): the model, adapters, serialization contract and emitter SPI

> **Era note.** This manifest was born at P1.4.3 (PR #39) as the
> *type-surface-only* scaffolding — 9 interfaces + supporting types inside
> `rune-parser`, no concrete classes. At PR #464 (the D43 IR train's PR-1)
> the surface graduated: the IR-lab foundation was ported in at the lab's
> HEAD `8c14592` (2026-07-01; the lab's vendored parser pin `3c60acec` =
> PR #279) and the whole `com.regnosys.rosetta.ir.*` tree — the original
> scaffolding included — was **relocated to the new `rune-ir` module**
> (`org.finos.rune:rune-ir`). This document describes the current state;
> the scaffolding-era details it replaced live in git history and the
> PR #39 / PR #464 CHANGELOG entries.

## Concept

An Intermediate Representation (IR) is a second view of the parsed
`.rosetta` source that exposes the SAME content at a SEMANTIC level rather
than a SYNTACTIC one. The AST is grammar-shaped; the IR sits above it and
projects grammar variants onto single concepts (a type with fields, a
choice, an enum, an expression tree). Mature compilers ship both views
side-by-side (Roslyn `SyntaxTree` + `SemanticModel`; TypeScript likewise) —
syntax-shaped tools need the AST, semantic-shaped tools (codegen, semantic
diff, dependency graphs, cross-DSL interop) need the IR.
**rune-dsl-plus follows that pattern.**

As of PR #464 the IR is no longer a type surface: it is a working model
with concrete adapters from the AST (declarations and expressions), an
off-JVM serialization contract (`irFormatVersion` 1), an MLIR-style
printer, and a target-neutral emitter SPI whose first emitter targets
Python declarations. It lives in its own module — `rune-ir` — which
depends on `rune-parser` ONLY. The Java-generation route over this IR —
`rune-ir-java`, flag-gated `-Drosetta.generator.ir=true` (U013's subject) —
is live since PR #466 through a ServiceLoader seam; the shipping
generator itself still carries no IR code.

## Why

Five forward-state items unlock from a stable IR (unchanged since the
scaffolding era): **W22** cross-DSL interop · **W26** codegen
transparency + source maps · **W31** LLM-consumable metadata (every IR
node carries a typed `Metadata` bag) · **W1** dependency graph & field
traceability · **W16** semantic diff between `.rosetta` versions. D43 adds
the strategic one: **W37 multi-target codegen** — one IR feeding Java,
optimised-Java and Python emitters, with the off-JVM JSON contract letting
non-JVM tooling consume the same semantics.

The foundation was built in a parallel external effort (the IR lab)
against a mid-parity engine snapshot and byte-gated there
(`ast→ir→java ≡ ast→java` at the pin). Porting it in-repo ends the
vendored-slice model: every figure the lab proved is treated as
pin-relative until re-proven against the completed engine — no lab number
is quoted forward as a current claim.

## Structural changes

The NEW module `rune-ir/` (`org.finos.rune:rune-ir:0.0.1-SNAPSHOT`,
depends on `rune-parser` only) carries `com.regnosys.rosetta.ir.**` —
125 main files across 10 packages (counted off the tree at PR #644; 118 at PR #643; the header read 75 from the scaffolding era until then):

- **`.core`** (29 — 23 at PR #643) — DSL-agnostic primitives: `IRNode`, `IRType`/`IRField`, `IREnum`/`IREnumValue`,
  `IRFunction`/`IRParameter`, `IRRule`, `Metadata`/`MetadataKey<T>`,
  `IRKind` (22 values since PR #644, the property gate - `MODEL` the model-level container kind; 21 before; closed), `Cardinality`, `SourceRange` (the 13 of the
  scaffolding era) + since PR #641 (decision D55 (local)) the eight value records of the DECLARATION FACTS
  (ten value records since PR #643, the TYPE GATE): `IRBounds`, `IRAnnotationUse`,
  `IRTypeArgument`, `IRDocReference`, `IRAnnotationPath`, `IRLabel`,
  `IRRuleReference`, `IREnumSynonym`, and the type gate's own two —
  `IREffectiveBase` (a collapsed alias chain's leaf: its kind, name, namespace and
  the effective LITERAL arguments; the record refuses a name value and a non-leaf
  kind) and `IRTypeParameter` (an alias's declared parameter — and, since PR #644, the
  arguments its own type call wrote, `typeArguments`, optional in the codec and schema) — and since PR #644, the
  PROPERTY GATE, six more: `IRAliasLink` (one rung of a walked alias chain: its name, namespace, parameter names
  and its condition names WITH their kinds — the whole-validator refusal law is a pure function of the chain),
  `IRModel` (the first MODEL-level contract: namespace, definition, version, the qualifiable configs, the
  qualification functions, the function signatures and the with-meta uses) and its four members
  `IRQualifiableConfig`, `IRQualificationFunction`, `IRFunctionSignature` and `IRWithMetaUse` (which carries
  EXACTLY one named refusal, `nothing` or `missing`, where the argument has no inferred leaf type — the
  `ReferenceWithMetaVoid` line stated, never silently skipped). The four declaration
  interfaces gained 24 DEFAULT methods (`IRType` 7, `IRField` 8, `IREnum` 5,
  `IREnumValue` 4) — 27 DEFAULT methods since PR #643 (`IRType` 10: `effectiveBase()`,
  `typeParameters()`, `baseTypeArguments()`) and 29 since PR #644 (`IRType` 12:
  `conditionKinds()`, index-parallel to `conditionNames()` with one of
  `OneOf` / `Choice` / `DataRule` per condition, and `aliasChain()`, the alias
  hierarchy walked OUTERMOST-FIRST on a TYPE_ALIAS reference beside the
  `effectiveBase()` that collapses the same chain) — additive for every
  implementation, pinned row by row on `IRContractSurfaceTest` (70 rows).
- **`.rune`** (4) — rune-specific extensions: `IRReport`,
  `IRRegulatoryUnit`, `IRAnnotationDecl`, `IRSourceDecl`.
- **`.adapter`** (8) — the DECLARATION adapter, now concrete:
  `AstToIRAdapter` (data → STRUCT, choice → CHOICE, enum → ENUM, and since PR #643
  typeAlias → TYPE_ALIAS — the alias's parameters, its body as a reference, the
  body's arguments as written and its condition names; every reference resolving to
  an alias also carries the chain collapsed at the use site, its EFFECTIVE BASE) +
  `IRNodeImpl`/`IRTypeNode`/`IRFieldNode`/`IREnumNode`/`IREnumValueNode` +
  `IRMetadata`. Since PR #641 the node records carry the declaration facts as
  PROPER NAMED COMPONENTS (not the metadata bag) with their OLD-ARITY
  constructors kept, and the adapter fills every one: a type reference's TRUE
  kind with the namespace and own name of the declaration it resolves to, the
  documentation, `override`, the base type / enum parent as resolved references,
  the annotations, the doc references whole (a corpus resolved through the
  host's `CorpusResolver` seam — one producer with the legacy javadoc renderer),
  the condition names, the EXACT bounds, the type arguments, an enum value's
  synonyms, a field's labels and rule references. A name the workspace cannot
  resolve falls back to the shared `BuiltinTypeRegistry`, the old generator's own
  first fallback — and since PR #643 that is the ONLY workspace fallback this route
  keeps: the old generator's three UNLINKED fallbacks — its alias table, its
  workspace simple-name scan and its alias search — read `workspace-fallback` and
  are REFUSED by name; its post-resolution bypass answers a name the linker DID
  resolve and is caught on `.kind` / `.legacyDeclaration` and `.javaType` instead
  (no cell of the 26 reaches any of the four). NOT carried: attribute-, class- and
  enum-level synonyms.
- **`.expr`** (52) + **`.expr.anf`** (11) + **`.expr.adapter`** (2) — the
  expression IR (literals, references, field access, apply, conditionals,
  list construct/ops, existence, binary ops, let/variables, `NodeId`,
  `Optionality`; since PR #492 `IRPointFreeApply` — the 14th
  `IRExprKind`, the L-109 point-free function application, deliberately
  NOT an `IRApply` so the four IRApply-keyed gates stay byte-frozen)
  with A-Normal-Form support (hoists, binds, blocks,
  `Normalize`) and `ExpressionToIRAdapter` from the typed AST.
- **`.json`** (8) — the dependency-free JSON serializer/reader
  (`IRJsonSerializer`/`IRJsonDeserializer`, `JsonWriter`/`JsonReader`) +
  the generated Draft 2020-12 schema (`IRJsonSchema`,
  `irFormatVersion: 1`). Since PR #641 every declaration fact is an OPTIONAL
  member written only when non-default, appended after the v1 members in a
  fixed documented order: the format version STAYS 1, a node without the facts
  serializes to the bytes it did, and a v1 document reads back with every fact
  at its empty default (both pinned). The reader admits exactly what the schema
  admits: a type reference's kind is one of the schema's SEVEN referenceable
  kinds (one list, `IRJsonSchema.TYPE_REFERENCE_KINDS`), a flag is written only
  when set and an explicit `false` is refused, and a type argument carries
  EXACTLY ONE of `nameValue` / `literalValue` (the record's law, the schema's
  `oneOf`). Since PR #643 three more OPTIONAL members ride the same rules —
  `<prefix>EffectiveBase` on a resolved TYPE_ALIAS reference (an object
  {kind, name, namespace?, arguments?}, the effective arguments literal-only) and
  `baseTypeArguments` / `typeParameters` on a TYPE_ALIAS declaration node — each
  written only when non-default with its dependent law stated in the writer, the
  reader and the schema; `irFormatVersion` STAYS 1 and a v1 document reads
  unchanged (`v1.schema.json` regenerated).
- **`.print`** (3) — the MLIR-style `IRPrinter` + `RTypeFormatter`.
- **`.emit`** (4) + **`.emit.python`** (3) — the target-neutral emitter
  SPI (`IRExprEmitter`, `AbstractIRExprEmitter`) and the first emitter:
  Python declarations (`IRPythonDeclarationEmitter`, `IRPythonEmitter`,
  `PythonIdentifiers` keyword/snake-case sanitization). The Python
  emitter rides inside rune-ir until the Python phase splits it out
  (`rune-ir-python`, per the D43 module family).

**The relocation:** `rune-parser` no longer contains any
`com.regnosys.rosetta.ir` source (18 main + 7 contract-test files moved;
17 of the 18 were byte-identical to the lab's copies). The parser keeps
`ast.IRKindForClass` (a test-scope FQN-string registry with no IR import;
its parser-side consumers were `AuditDocGenerator` +
`RRootElementCorpusAuditTest`; in this snapshot `RRootElementRegistryConsistencyTest`,
see SOURCE-SNAPSHOT.md) — rune-ir carries its own test-scope copy
for the relocated mapping test. Existing AST (`com.regnosys.rosetta.ast.*`)
is untouched.

## BC Story

**The relocation is BC-free by timing.** The scaffolding-era forward
commitment ("V1 interface signatures become a permanent contract once a
consumer exists") never fired: at relocation time ZERO consumers of
`com.regnosys.rosetta.ir.*` existed outside the package itself
(grep-proven; the only external hit was a javadoc `@link` in the parser's
`CorpusWalker`). PR #466 (the train's PR-2) wired the FIRST consumer
(`rune-ir-java`), so PR #464 was the last window in which the residence
could move without breaking anyone — and the commitment has now FIRED:
the rune-ir surface is the permanent contract the scaffolding era
promised.

- **Layer 1 (source-level):** no grammar change; trivially preserved.
- **Layer 2 / Layer 4 (generation):** rune-ir is NOT on the shipping
  generation path (the U013 route is opt-in, default OFF). The parser-side
  deletion was proven generation-inert by the full ringfence chain at
  PR #464: D11 cp 55/55 with the population summary byte-identical to the
  #437 SOT, gensuite 3,720/0/0/53 unchanged, plugin suite 18/0/0/0
  unchanged (`target-464-*.log` receipts).
- **Layer 3 (japicmp vs upstream):** upstream rune-dsl never had
  `com.regnosys.rosetta.ir.*` — the fork-added surface moving OUT of the
  `rune-parser` jar returns that artifact to upstream-shaped scope for
  this package; the surface reappears in the NEW `rune-ir` artifact
  (ADDED-in-new-artifact; nothing any upstream-compat consumer can hold a
  reference to).
- **Sealedness:** the no-sealed-interfaces commitment stands, still locked
  by `IRSealednessTest` (now in rune-ir).
- **Serialization:** the v1 JSON wire form is golden-locked
  (`irFormatVersion: 1`; schema + payload goldens byte-compared, LF-pinned
  via per-directory `.gitattributes`).
- **Parser suite accounting:** the 7 relocated contract-test classes
  carried 43 tests; the parsersuite baseline moved 4,237 → 4,194 (skips 5
  unchanged) with the delta exactly the relocation set — no test was lost
  (their evolved lab versions run in rune-ir's suite).

## How to use

```xml
<dependency>
    <groupId>org.finos.rune</groupId>
    <artifactId>rune-ir</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

```java
// declarations: AST → IR
RModel model = AstBuilder.buildFromFile(path);
List<IRNode> nodes = new AstToIRAdapter().adaptModel(model);

// print (MLIR-style) and JSON (irFormatVersion 1) round-trip
String printed = new IRPrinter().printAll(nodes);
String json    = new IRJsonSerializer().toJson(nodes);
List<IRNode> back = new IRJsonDeserializer().fromJson(json);

// expressions: typed AST → expression IR (+ ANF normalization)
// see ExpressionToIRAdapter + expr.anf.Normalize

// first emitter: Python declarations (per-node)
IRPythonDeclarationEmitter emitter = new IRPythonDeclarationEmitter();
List<String> py = nodes.stream().map(emitter::emit).toList();
```

**Java generation through the IR:** live since PR #466 — the
`rune-ir-java` module + the ServiceLoader seam (U013's subject), behind
`-Drosetta.generator.ir=true` (default OFF; the shipping generator still
carries no IR code — the flag read + provider lookup live in its `spi`
package).

## Migration

None. No consumer existed before the relocation; package names are
unchanged, so any future code written against the scaffolding-era parser
jar location compiles against rune-ir by adding the dependency above.

## Test coverage

The module's own suite: **41 test classes, 813 tests, 0 failures** at PR #644
(READ from `c5-ir-install.log`; 408 tests over 34 classes at PR #492, where the
accretion ledger below stops — that ledger is a record of how the suite grew,
not the standing total)
(390 through PR #476; +3 at PR #477 — the `declineReason` mirror locks
in `ExpressionToIRAdapterTest`: the adapter's read-only per-arm
decline-reason channel [first-failing-gate tokens, *empty ⟺ lowers* by
construction, the `unattributed` sentinel] consumed by U013's blocker
probe for the per-ARM gate decode — the enum-channel tokens, the
symbol/call/feature-call gate tokens, and the noAdaptArm +
empty-⟺-lowers equivalence sweep; +1 at PR #478 — the
disguised-input-nav ARM (`adaptDisguisedInputNav`: the L-111
`inputFeatureNav` disguise lowers by adapting the EXACT equivalent
feature-call legacy synthesizes for it, retyped through the
attribute channel at the expression-cache boundary, with the mirror
composing the equivalent's own gate onto the channel token
`inputFeatureNav.<gate>`) locked on its defer gates + composed
tokens, the lowers side population-locked by U013's probed re-read
conservation signature; +1 at PR #479 — the filter/extract
synthetic-item receiver ARM (`retypedSyntheticFilterExtractItem`:
a SYNTHETIC_ITEM navigation base whose IMPLICIT filter/extract
binder's source element form is PROVABLY plain retypes at the
boundary with the source's Cat-8 element type — the synthetic node
is invisible to the node-keyed type cache — and the hop nodes of a
synthetic-based chain type off the attribute channel) locked on a
PARSED fixture (the fixed-point walk types the binder source; the
outside-binder residue keeps the `receiverSyntheticItem` token), the
lowers side population-locked by the same conservation-signature
pattern (`retypeSourceOk` ZERO at population); +1 at PR #480 — the
bare-attr + chain item-nav ARMS (`adaptBareAttrItemNav` /
`adaptImplicitItemChain`: a Cat-9 bare in-lambda ITEM-feature read
and a Cat-10 bound-chain disguise lower by adapting legacy's EXACT
`synthesizeImplicitItemNavigation`/`-Chain` equivalents through the
#479-taught synthetic base — legacy's decline ladders mirrored in
source order with the rule-input precedences, the
shortcut/closure-param/scope-head guards, the filter/extract binder
gates and the structural element derivation + identity guard,
prove-or-decline — rebuilt at the cache boundary with the RAW node's
cached type + source range on the whole spine; the mirror twins
compose `attrOutsideFunction.<gate>`/`attributeChain.<gate>`) locked
on PARSED fixtures (both claims adapt whole with retyped bases and
stamped ranges; empty ⟺ lowers on both twins; the outside-lambda
residue names the composed token), the lowers side population-locked
by U013's probed re-read conservation signature (both restated
`claims` buckets ZERO at population); +1 at PR #481 — the
elidedImplicit WIDENING (`resolveElidedPipedSource`: a grammar-elided
PIPED implicit binding source resolves to the enclosing then's
ARGUMENT — legacy `handle(RImplicitVariable)`'s thenBody face, the
value identity — so the #479 retype's source/type reads, the
element-form allowlist and the #480 arms' structural derivation all
see THROUGH the pipe; switch/named/non-then/top-level faces stay
declined, prove-or-decline) locked on a PARSED `legs then filter rate
exists` fixture (the bare read adapts whole with the base retyped off
the RESOLVED pipe argument; the hand-built extract-bound elided face
keeps the `receiverSyntheticItem` token), the lowers side
population-locked by U013's pipe-facet conservation signature
(`thenArg.provable.*` ZERO at population); +1 at PR #482 — the
nested-thenPipe WIDENING (the `RThenExpr` arms in
`isProvablyNonMetaElementSource` + `sourceElementDataType`: a NESTED
pipe source proves and derives through its BODY result — the element
form of `A then B` is B's body's own, the #481 value identity one
structural seat deeper; a bodyless then declines, and the type read
stays on the then node's own cached type) locked on a PARSED `legs
then filter rate exists then filter qty exists` fixture (the bare
read adapts whole with the base retyped off the nested pipe's cached
type; the parsed extract-bodied nested pipe stays declined with the
composed `attrOutsideFunction.sourceElementUnresolved` token — since
#483 that carrier's decline is the IDENTITY face, the literal-`item`
body declining through the resolver arm), the
lowers side population-locked by U013's pipe-facet conservation
signature (`thenPipe.provable.typeOk` ZERO at population); +1 at PR
#483 — the extract-bodied WIDENING (the `RExtractExpr` arms in
`isProvablyNonMetaElementSource` + `sourceElementDataType`: an
extract-bodied pipe source proves and derives through its BODY
result — the element form of `A extract B` is B's OWN result per
element, the body-descent law one construct wider;
implicit-or-paramless body lambdas only — a NAMED extract declines
whole, the scope-live walk-out class — and the type read stays on
the SEAT's resolved source) locked on PARSED Trade/Leg fixtures (the
bare `qty` adapts whole on both the FLAT `trades extract leg then
filter qty exists` carrier — the base retyped off the EXTRACT node's
cached type — and the NESTED `trades then extract leg then filter
qty exists` carrier — the then descent composing with the extract
descent, the type read on the inner then; the parsed named-binder
carrier stays declined with the composed
`attrOutsideFunction.sourceElementUnresolved` token), the lowers
side population-locked by U013's pipe-facet conservation signature
(`extractPipe.provable.typeOk` ZERO at population, both seats); +1
at PR #484 — the callable-output WIDENING (the
`RSymbolReference`→`RFunction` legs in
`isProvablyNonMetaElementSource` + `sourceElementDataType`: a bare
no-arg function reference in element-source position proves on the
callee's OUTPUT attribute and derives its declared data type — the
L-109 point-free application's value IS the output, legacy
`renderImplicitFunctionInvocation`'s own render + the checker's
`fn.output()` type leg; the aliasShadow precedence declines FIRST
per legacy's `isAliasReference` [the coarse `collidesWithShortcut`
form — the safe superset], cardinality-agnostic like the
bare-attribute leg and recursion-free) locked on PARSED
Trade/Leg/ToLeg fixtures (the bare `qty` adapts whole on the FLAT
`trades extract ToLeg then filter qty exists` carrier, the NESTED
`trades then extract ToLeg …` carrier and the MULTI-output `ToLegs
(0..*)` carrier — the base retyped off the seat's cached type = the
callee's output; the META-output `ToRef [metadata reference]`
carrier stays declined with the composed
`attrOutsideFunction.receiverSyntheticItem` token), the lowers side
population-locked by U013's callee-output conservation signature
(the `symbol:RFunction.*` admit tokens ZERO at population on every
composing channel); +1 at PR #485 — the RULE-INPUT widening
(`provableRuleInputElementType` consumed at the allowlist's
unresolved-implicit branch, the derivation leg and the #479
retype's type read: a TRUE-noBinder rule-top elided implicit proves
as the RULE INPUT and derives the rule's declared from-type — the
rule-input identity, legacy `isElidedOperandTopLevel` →
`MapperS.of(input)` + the checker's `inferRuleFromType` leg,
`withNoMeta` by construction; synthetic + the slot gate + a CLEAN
walk to the `RRule` root, a lambda/switch/function/condition
crossing declines; recursion-free — PLUS the `ruleInputNav`
precedence gate refined to the TOP-LEVEL slice per legacy's own
`buildImplicitInputReceiver` split: the in-lambda receiver is a
synthetic implicit, the item ≡ input value identity) locked on
PARSED reporting-rule fixtures (the bare `leg` adapts whole on the
FLAT `reporting rule RuleTopFilter from Trade: filter leg exists`
carrier — the base retyped with the RULE's declared FROM-type, the
declaration read — and the COMPOSED `filter leg exists then extract
leg` carrier — the filter arm's element-preserving recursion
composing with the rule-input arm; the hand-built FUNCTION-top
fixture stays declined with the `receiverSyntheticItem` token), the
lowers side population-locked by U013's rule-top conservation
signature (the `noBinder.fromData` admit facet ZERO at population
with the `ruleInputLambda` pipe channel emptied whole); +1 at PR
#486 — the symbolUnresolved DECODE's facet lock
(`symbolUnresolvedFacet` + `isChildLinkedAtOwnSeat`: the flat reason
token refined IN PLACE to `symbolUnresolved.[synthetic.]<facet>` —
the mechanism split first [a ref not child-linked at its own seat is
an UP-only parented render-time synthesis, the
`synthesizeFeatureCall` disguised-nav receiver shape], then the
head-name ladder in the resolution machinery's own precedence)
locked on a PARSED Colour/Leg/Trade/Helper/MyFunc model with a REAL
name index (13 pins: the hand-parented orphan probes pin the
`synthetic.` faces — onlyExists / aliasName / closureParam /
fnScopeName / itemAttr / lambda.global.type / lambda.absent /
global.function / global.enum — the parentless probes pin the plain
absent/nameMissing/dottedName faces, and the child-linked
equality-sibling probe pins the plain `siblingEnumValue` path; the
standing #477-era unattached-ref pin recut to
`symbolUnresolved.absent`), the population side proven by U013's
conservation re-read (participation and sole Σ ≡ the #485 flat
counts TO THE DIGIT per cell-seam); +1 at PR #487 — the conditional
JOIN-law widening (the `RConditionalExpr` branches in
`isProvablyNonMetaElementSource` — the shape law: the THEN arm
provable, an EMPTY else admitting on the then proof alone
(`hasGenuineElse`'s structural-emptiness discriminator), a genuine
else proving too — and `sourceElementDataType` — the same-instance
JOIN, the list-literal all-provable/all-identical laws lifted to
the two-armed choice; the checker's own
`withNoMeta(typeJoin.join(thenT, elseT))` wrap strips arm meta from
the cache, so the per-arm proofs are the ONLY meta protection;
`join(thenT, NOTHING) = thenT` types the empty-else face) locked on
PARSED fixtures (the bare `leg`/`qty` adapt whole on the DIRECT,
EXTRACT-BODIED and elseEmpty conditional carriers with the bases
retyped off the cached join; the MIXED-form carrier — `ExtLeg` vs
`Leg` — stays declined at the same-instance derivation,
prove-or-decline), the lowers side population-locked by U013's
conditional conservation signature (the
`conditional.sameType/.elseEmpty/.formGap` admit facets ZERO at
population, 236 fallen channel events ≡ the pre-read EXACTLY,
`typeDiffers` ZERO at the decode — no mixed-form conditional exists
in the corpus); +1 at PR #488 — the argNav facet DECODE
(`argNavFacet` at the `argToken` seat: the flat argument-position
reason token refined IN PLACE to
`argNav.[meta.|typeMissing.]<root>.<depth>[.multi]` — every argNav
occurrence a navigation the nav arms ALREADY LOWER, so the facet
decodes the `isSimpleCallArg` argument-position admission an
L-042-class arm would compose: the ROOT the render family, the
DEPTH the chain, `.multi` the accumulated cardinality, the prefixes
+ the `alias` root drift/future detectors) locked on a PARSED
Sub/Leg/Trade model with per-case callees (6 pins:
`param.hop1/.hop2/.deep/.hop1.multi` off the input chains +
`item.hop1` off the explicit lambda item + `synItem.hop1` off the
elided bare attr — every predicted spelling correct FIRST RUN; the
standing hand-built flat pin recut TWICE by the two-step run-first
disclosure: the clean first read exposed the type engine's
NON-NULL MISSING sentinel [`getInferredType` =
`getOrDefault(expr, MISSING)` — nothing computes on demand], and
the Seat-1 MF-1 recut made the `typeMissing.` detector LIVE via
`isMissing()` with the pin locking that face on the exact shape
that disclosed it), the population
side proven by U013's conservation re-read (Σ facets ≡ 2,300 sole /
2,438 participation TO THE DIGIT on all 8 channels, the full
probe-surface diff argNav-only); +1 at PR #491 — the argNav
ROOT-TYPE BELT pin (the #489 Seat-1 OBS-2 landing: the facet walk
read hop RESULT types only, so a meta/missing-typed ROOT under an
otherwise-ADMITTING root kind was excluded only INDIRECTLY by
`adaptSymbolReference`'s meta-input decline; the belt joins the
root's OWN type to the #488 drift flags, gated on admission so no
decline face re-spells) locked by the hand-CONSTRUCTED three-way
contrast (`argNavRootTypeBeltDeclinesMetaAndMissingRoots`: the same
clean-hop chain over a meta root → `meta.param.hop1`, a MISSING
root → `typeMissing.param.hop1`, a clean root → the belt does not
fire and the face falls through to the type-agreement gate's
`typeGap.` — the classifier package-private for the pin, the belt's
distinct state structurally unreachable through `adapt`); +1 at PR
#491 — the alias-operand REVIVAL lock (the L-035 arithmetic-operand
law extended to the EQUALITY + EXISTENCE gates: the flat
`operandAlias` token refined IN PLACE to `operandAlias.<clean|
bodyMeta|bodyMissing|rawUnresolved>[.enumSibling]` at its single
seat — all three consuming gate mirrors compose it — and the
admission is the SAME classifier, `isAdmittedAliasOperand`: body
typed + meta-free per the engine read of the shortcut's DEFINING
expression + non-enum sibling at equality) locked on a PARSED
six-function fixture (`aliasOperandRevivalAdmitsProvenBodiesAt
EqualityAndExistence`: the equality-clean claim lowers WHOLE with
the ALIAS reference lowered in place · the existence claim lowers ·
the enum-value sibling declines on the decoded
`operandAlias.clean.enumSibling` face · comparison keeps declining
every alias face — the #372 `comparisonIntWiden` body-walk lever ·
logical keeps the FLAT face — the `ofNullSafe` coercion family ·
an unresolvable body declines `operandAlias.bodyMissing`), the
population side proven by the #491 probed re-read (existence 402 +
equality 228 clean sole CLAIMED — the faces read ZERO post-arm —
with the FunctionAliasHelper-retype subset — the #326 META
byte-divergent class union the #334 byte-inert basic scalars —
delegating WHOLE at the guard's `aliasEqualityOperandMeta` arm,
driven not declined); +1 at PR #492 — the alias-nav teach's 5-leg
parsed pin (the L-032 disguised alias-head arm `adaptAliasHeadNav`:
a single-hop FieldAccess over the BODY-retyped `IRReference{ALIAS}`
— admit single + admit MULTI via the FAITHFUL
`getRuleBodyCardinality` channel [the global `compute()` reads
every disguised chain as conservative SINGLE, an under-read MULTI
would pass the scalar gates] + the usesOutput and featureOffBody
declines + the bound-receiver form at `adaptFeatureCall`'s #479
retype slot); +1 at PR #492 — the point-free kind's seats
(`IRPointFreeApply`, the 14th `IRExprKind` — deliberately NOT an
`IRApply`, the four IRApply-keyed gates stay byte-frozen — the
kind/node/emitter-hook/JSON/printer seats joined `IRSamples` and
the kind-coverage locks; the adapter arm `adaptPointFree` with the
callee-output channels and the aliasCollision decline face);
4 skips = the 4 opt-in `@EnabledIfSystemProperty` golden-regen writers;
the 2 corpus censuses — `CorpusPythonDeclineCensusTest` +
`CorpusSnakeCollisionCensusTest` — were retargeted off the lab's
`phase1-bundle` geometry onto this repo's `test-corpus/` at the IR-train
PR-4 (PR #468) and now RUN: the snake census walks the cells'
`rosetta-source/src/main/rosetta` roots (the EXACT 576-file lab universe
— the lab-calibrated non-vacuity figures reproduce byte-for-byte:
24,635 attrs / 5,365 multi-attr types / 18,779 snaked / 2,957 fns; all
collision invariants 0), and the decline census loads the D11 gate's own
5-cell closures (builtins-union + dep maps + version stamps mirroring
the gate)). Notable locks:

- **Contract** (8 classes, relocated + lab-evolved):
  `IRContractSurfaceTest` (70-row signature pin since PR #644 — the MODEL node's rows; 61 at PR #643 for the three `IRType` type-gate defaults; 58 at PR #642, with the completeness and package legs; 53 at PR #641, 29 before), `IRKindCoverageTest`,
  `IRPackageStructureTest` (`.rune` → `.core` import direction),
  `IRSealednessTest`, `IRAdapterShellTest` (now pins the FILLED adapter
  package shape), `MetadataContractTest`, `IRJsonDependencyTest` (the
  production JSON layer stays dependency-free),
  `RRootElementToIRKindMappingTest` (every `RRootElement` encountered on
  the live 589+2-file corpus maps to an `IRKind`).
- **Adapters:** `ExpressionToIRAdapterTest` (52) + the declaration golden
  suites below exercise `AstToIRAdapter` end-to-end.
- **Serialization:** JSON round-trip, deserializer, coverage and
  schema-validation suites — the generated v1 schema is oracled with the
  networknt Draft 2020-12 validator (accepts the goldens, rejects
  malformed docs) and byte-locked by `IRJsonSchemaGoldenTest`.
- **Print:** printer expression/declaration suites + byte-locked goldens.
- **Python:** `IRPythonEmitterTest` (102), `IRPythonDeclarationEmitterTest`
  (46), `PythonIdentifiersTest`, byte-locked declaration goldens.
- **Golden discipline:** every golden dir carries an `eol=lf`
  `.gitattributes` pin; comparisons are CR-sensitive raw string equality
  (`Files.readString` + `String.equals`); regen is opt-in via
  `-Dir.{json,json.schema,print,python.decl}.regen=true`.

- **The declaration facts (PR #641):** `IRJsonDeclarationFactsRoundTripTest`
  (a round trip per new property + the backward-compat pin: an old-arity node
  byte-identical, a v1 document read at the empty defaults),
  `IRPrinterDeclarationFactsTest`, the enriched `IRSamples`, and the 24 default
  methods — 29 default methods since PR #644, 27 default methods since PR #643 — as rows of `IRContractSurfaceTest`
  (29 → 53; 58 since PR #642, 61 since PR #643 — the three `IRType` type-gate
  defaults — and 70 since PR #644, the MODEL node's rows — which
  pinned `Metadata` / `MetadataKey` and added the COMPLETENESS leg — every pinned
  interface's public methods reflected and held EQUAL to its rows — and the PACKAGE
  leg — every public interface of `ir.core` / `ir.rune` pinned). The facts' RECONCILE
  against the source lives with the route (U013): `IRDeclarationFactsReconcileTest`
  in `rune-ir-java` and its 26 mutation lanes.

- **The type gate (PR #643, v3.3 seat 7):** `AstToIRAdapterTypeAliasTest` (13 tests:
  the `typeAlias` declaration adapted whole — namespace, definition, parameters, the
  body reference, the body's arguments as written, the condition names — and the alias
  chain COLLAPSED at a use site with the use-site bindings applied, an alias of an alias
  recursing, a body no declaration answers seeded from the builtin registry by name),
  the codec and printer additions (`IRJsonDeclarationFactsRoundTripTest`,
  `IRJsonSchemaFaithfulnessTest`, `IRJsonSerializerDeclarationTest`,
  `IRPrinterDeclarationFactsTest`, `IRSamples` carrying a real TYPE_ALIAS node) and the
  three new `IRType` rows of `IRContractSurfaceTest`. The rune-ir suite reads 813 tests,
  0 failures at PR #644 commit 3, THE CONTRACT (`c3-ir-install2.log`; 731 at PR #644 commit 2,
  `c2-ir-install2.log`; 729 at PR #643's head). The RECONCILE of the new facts
  against the source lives with the route (U013): `IRTypeGateTest` in `rune-ir-java`.

- **The property gate (PR #644, v3.3 seat 8):** `AstToIRAdapterModelTest` (11 tests:
  a model adapted whole — its namespace definition, its version, the qualifiable
  configs and their roots as references, every `[qualification]` function with its
  FIRST input's type, every function's DECLARED signature with the parser's own
  synthesized-input placeholder excluded by the parser's own predicate, and every
  with-meta use with its named refusal), `AstToIRAdapterConditionKindsTest` (7: a
  condition's KIND beside its name on every type, named conditions too, and a choice
  declaring none by the language's own shape) and `IRJsonModelNodeRoundTripTest` (16:
  the MODEL node through the codec, the schema and the printer, every new member
  OPTIONAL and a malformed one REFUSED at its own path). The alias chain rides
  `AstToIRAdapterTypeAliasTest` (17 tests, 13 at PR #643). `irFormatVersion` stays **1**:
  a v1 document is read unchanged and a node without the new facts serialises
  byte-identically. The rune-ir suite reads 813 tests, 0 failures over 41 classes at
  PR #644 (`c5-ir-install.log`; 731 at commit 2, 729 at PR #643's head). The
  RECONCILE of the new facts against the source lives with the route (U013):
  `IRTypeIndexTest`, `IRPropertyModelTest`, `IRPropertyGateTest`,
  `IRDerivedFactsReconcileTest`, `IRDerivedGateTest`, `IRModelReconcileTest` and
  `IRWrapperReconcileTest` in `rune-ir-java`.

The doc-triplet locks (`UpgradeManifestStructureTest`,
`FeaturesIndexCoverageTest`, `BCVerificationCoverageTest`, rune-parser)
continue to cover this manifest.

## Coverage

What the IR covers today (ported state, pre-re-baseline):

- **Declarations:** data → STRUCT, choice → CHOICE, enum → ENUM — and since PR #643
  typeAlias → TYPE_ALIAS, with every alias reference carrying its collapsed effective
  base — with
  metadata, cardinality and base-type links; metafields are deliberate
  pass-through (no IR is built for them).
- **Expressions:** the common families — literals, symbol/enum/implicit
  references, field access chains, function application, conditionals,
  list construct/ops, existence, comparison/equality/logical/arithmetic,
  to-string, let-binds — plus ANF normalization (hoists for conditionals,
  boolean conditions, then-chains, BigInteger widening).
- **Off-JVM contract:** print + JSON forms for the declaration surface,
  `irFormatVersion` 1, schema-validated.
- **Python:** the declaration noun trio (STRUCT → `@dataclass`,
  ENUM → enum, CHOICE → `Union` alias) + the first verb slice
  (conditional → ternary), keyword/snake-case sanitization unified.

**Honest boundaries:** `ir → java` emission lives in U013's `rune-ir-java`
route (declarations and expressions byte-gated BY DELEGATING to the
legacy renderer where the IR declines); the lab's pin-era share and
byte-gate figures are NOT quoted as current. The re-baseline the train
chartered is DONE: the declaration transparency gate + the full-population
byte gate closed at PR-3 (#467, the ON ring 55/55), and the
decline-census re-run landed at PR-4 (#468) — today's engine reading:
5,510 seam-roots, 1,118 adapt (20.29% vs ~18% at the lab's recorded
census), 732 lower to Python, adapter declines 4,392 (top families
still arm-less: RConstructorExpr 956 · RConditionalExpr 789 ·
RSymbolReference 443), emitter declines 386 (reference:ALIAS 365 = 94.5%
— the top buildable Python verb slice), cross-inheritance collisions 0.
The movement vs the lab's recorded reading (ALIAS 317 → 365; conditional
declines 896 → 789; adapt-reach ~18% → 20.29%) reflects BOTH the ported
adapter's own post-census arms AND the fork's #447/#451 typing waves
feeding typed ingredients — the clean fork-side witness is U013's Wave6
§1.1 trip (the L-032-MISSING alias-receiver types now resolve, the #447
typed-alias-nav mechanism exactly). The 22-value `IRKind` universe (21-value until PR #644, which
added `MODEL`) and
the 18-subclass `RRootElement` mapping remain corpus-complete (mapping
test green on today's corpus).

## Cross-references

- **D21** (locked envelope) + **D22** — the development decision log.
- **D43** — the post-parity IR phase + module family (rune-ir ·
  rune-ir-java · future rune-ir-java-optimised / rune-ir-python).
- **U013** (`docs/upgrades/U013-ir-mediated-java-codegen.md`) — the
  Java-generation route over this IR; REAL since PR #466.
- **Provenance:** the frozen IR lab (a local checkout of the performance toolkit, not included)
  (read-only archive), lab HEAD `8c14592` (2026-07-01), vendored parser
  pin `3c60acec` = PR #279. Port + relocation: the PR #464 CHANGELOG
  entry (`the development changelog`).
- **Constraints:** `docs/p0/parity-constraints.md` — W22 (#1, #6),
  W26 (#5), W31 (#16, #17), W1, W16.
- **Wishlist:** `docs/p0/wishlist.md` — W22, W26, W31, W1, W16, W37.
