package com.regnosys.rosetta.ir.expr;

/**
 * Closed discriminator for {@link IRExpr} node kinds — the expression-IR analogue of
 * the declaration IR's {@code com.regnosys.rosetta.ir.core.IRKind}, kept as a
 * <strong>separate</strong> closed enum precisely so the wire-pinned {@code IRKind}
 * (frozen for the M9 Python target, guarded by {@code IRKindCoverageTest}) never
 * gains an expression member. See {@link IRExpr} and the package documentation for
 * the rationale (Phase-2 design gate Q1, ratified 2026-06-19).
 *
 * <p>Exhaustive {@code switch} over this enum is the intended emitter dispatch, and the set grows
 * <em>additively</em> per emission wave (adding a constant is a backward-compatible change). How an emitter
 * handles a not-yet-wired kind depends on the emitter's role:
 * <ul>
 *   <li>the <strong>deprecation-bound Java byte-parity bridge</strong> keeps a {@code default} branch that
 *       falls back to legacy delegation rather than fail, so an unwired kind stays on the proven legacy path
 *       (the strangler safety net) while later waves land;</li>
 *   <li>a <strong>neutral SPI emitter</strong> (the strategic targets — Java-V2/Python/Rust/Morphir, via
 *       {@code AbstractIRExprEmitter}) uses an exhaustive <em>no-{@code default}</em> {@code switch}, so a new
 *       kind is a <em>compile</em> error that forces a maintainer to wire it — there is no legacy to delegate
 *       to (see {@code com.regnosys.rosetta.ir.emit.AbstractIRExprEmitter}).</li>
 * </ul>
 *
 * <p><strong>Wave 0</strong> introduces the leaf primitives below. The binary,
 * navigation, collection, control-flow, existence, conversion and construction kinds
 * from {@code notes/expr-ir-design.md} §3 arrive in their respective waves.
 */
public enum IRExprKind {

    /**
     * A scalar literal — integer, decimal, string or boolean. The flavour is the
     * {@link IRLiteral#literalKind()} payload.
     */
    LITERAL,

    /** A bracketed list literal {@code [a, b, ...]} over element expressions. */
    LIST_CONSTRUCT,

    /** The {@code empty} absent-value leaf ({@link IREmptyLiteral}). */
    EMPTY_LITERAL,

    /**
     * A bound variable reference — the implicit {@code item}, a function parameter,
     * or an alias/shortcut binder. The binding flavour is {@link IRVariable#variableKind()}.
     */
    VARIABLE,

    /**
     * A by-name reference to a declared symbol with no argument list of its own — an
     * enum value, a nullary function, or {@code super}. The referent flavour is
     * {@link IRReference#referenceKind()}.
     */
    REFERENCE,

    /** Application of a callee expression to zero or more argument expressions. */
    APPLY,

    /**
     * A binary operator over two operand subtrees ({@link BinaryOp}) — the first
     * structural kind, whose {@code children()} are load-bearing. The operator flavour
     * is {@link BinaryOp#op()} (the boolean comparison/equality/logical operators so far;
     * the arithmetic operators arrive in a later wave).
     */
    BINARY_OP,

    /**
     * A unary existence / absence check over one operand subtree ({@link Existence}) — the
     * first unary structural kind, whose single {@code child()} is load-bearing. The flavour
     * is {@link Existence#op()} + the optional {@link Existence#modifier()}.
     */
    EXISTENCE,

    /**
     * A single-hop feature navigation ({@link FieldAccess}) — {@code receiver -> feature},
     * whose single {@code child()} (the receiver subtree) is load-bearing and onto which the
     * emitter appends one {@code .<Witness>map("getX", v -> v.getX())} getter step.
     */
    FIELD_ACCESS,

    /**
     * A single-hop navigation to a {@code [metadata …]}-annotated feature ({@link IRMetaAccess},
     * #499) — {@code receiver -> feature} whose Java render is the {@code FieldWithMetaX} wrapper
     * family, served at the claim root by the meta-aware legacy renderer (the L-029 split); a
     * DISTINCT kind so no meta-blind native consumer admits it. The qualifier flavour is
     * {@link IRMetaAccess#metaQualifiers()}.
     */
    META_ACCESS,

    /**
     * A flat postfix list/collection operation ({@link IRListOp}) over one receiver subtree —
     * {@code distinct}/{@code flatten}/{@code first}/{@code last}/{@code reverse}/{@code count} —
     * whose single {@code child()} is load-bearing. The flavour is {@link IRListOp#op()}.
     */
    LIST_OP,

    /**
     * A conditional {@code if cond then X (else Y)?} ({@link IRConditional}) — the first control-flow
     * kind, whose {@code children()} (condition, then-branch, optional else-branch) are load-bearing.
     * At the Java target a conditional consumed in statement (SET) position hoists into an
     * {@code ifThenElseResult} temporary (the {@code ir.expr.anf} ANF tier); an operand-position
     * conditional becomes a {@code JoinPoint}. The node itself is a neutral fact — it records the
     * branch structure, not any hoisting strategy or target form.
     */
    CONDITIONAL,

    /**
     * A let-binding {@code let <binder> = <value> in <in>} ({@link Let}) — the desugared form of a
     * {@code then}-pipe (design §5 {@code then → Let}), whose {@code children()} ({@code value},
     * {@code in}) are load-bearing. It is a first-class neutral binding (Python {@code let}/walrus,
     * Rust {@code let}, Morphir {@code Let}), not a target form: it records "bind {@code value} to
     * {@code binder}, then evaluate {@code in} (which references {@code binder})". At the Java target a
     * chain of {@code Let}s consumed in statement (SET) position hoists into the {@code thenArg}
     * temporaries (the {@code ir.expr.anf} ANF tier; the {@code thenArg} render lexeme is an emitter
     * decision, not on this node). The node itself records the binding structure, no hoisting strategy.
     */
    LET,

    /**
     * A {@code to-string} conversion ({@link IRToString}) over one receiver subtree — {@code <child> to-string} —
     * whose single {@code child()} is load-bearing. A neutral conversion-to-text node (Python {@code str(x)}, Rust
     * {@code x.to_string()}, Morphir). At the Java target it renders {@code <child>.map("to-string", Object::toString)}
     * (or {@code <SourceEnum>::toDisplayString} for an enum source) by reusing the legacy {@code ConversionHandler}
     * oracle verbatim — the {@code "to-string"} lexeme, the source-enum detection and the meta-unwrap are emitter
     * decisions, NOT carried on this neutral node (the L-029/L-050 split).
     */
    TO_STRING,

    /**
     * A POINT-FREE function application ({@link IRPointFreeApply}) — a bare no-argument reference to a
     * function used as a value ({@code partyRoles filter Foo}, a call argument {@code Callee(BareF)}, a
     * navigation head {@code BareF -> field}) whose IMPLICIT argument legacy derives from the position's
     * enclosing context (the rule input, the lambda item, the condition instance). Deliberately NOT an
     * {@link IRApply} subtype (the #492 L-109 resolution): the four {@code IRApply}-keyed admission gates
     * stay byte-frozen by construction, and each position admits the new kind EXPLICITLY. The node carries
     * the callee's simple name as a neutral fact; the implicit-argument derivation, the
     * {@code MapperS.of} nav-receiver wrap and the {@code evaluate(...)} form are Java-emission decisions
     * served by the compiler's range-correlated {@code PointFreeRenderer} reusing legacy
     * {@code ReferenceHandler.renderImplicitFunctionInvocation} verbatim (the same public oracle the
     * top-level point-free seat already reuses — byte-identical by the strongest argument).
     */
    POINT_FREE_APPLY,

    /**
     * A type-construction expression ({@link IRConstruct}) — {@code Type { field: value, ... }} — the
     * #494 teach of the biggest untargeted family. This wave the node is deliberately SHALLOW (the
     * constructed type's name, the attribute names and the {@code ...} spread flag are the neutral
     * facts; the pair-value subtrees are NOT carried as IR children) and lowers at the claim ROOT
     * only, so every child-position admission allow-list stays byte-frozen (the #492
     * explicit-position law taken to its root-only extreme). The Java render — the typed builder
     * block with its coercion/hoist facet family — is served by the compiler's range-correlated
     * {@code ConstructRenderer} reusing legacy {@code ConstructionHandler.handle} verbatim (the
     * literal {@code super.visitConstructor} body — byte-identical by the strongest argument); the
     * DEEP enrichment (typed field-value IR children for the neutral SPI targets) is a named later
     * wave.
     */
    CONSTRUCT,

    /**
     * A lambda-bodied collection operation ({@link IRLambdaOp}) — {@code receiver extract body} /
     * {@code receiver filter body} — the #496 monster wave's leg-1 teach of the two biggest
     * untargeted families. One kind for both operators (the {@link IRListOp} flavour precedent);
     * deliberately DEEP: {@code children()} (receiver, body) are load-bearing and D1
     * all-or-nothing, with the single declared closure-parameter name (or {@code null} for the
     * implicit {@code item} binding) carried as the neutral binder fact. The Java render — legacy
     * {@code CollectionHandler}'s lambda forms — is served by the compiler's range-correlated
     * {@code LambdaOpRenderer} reusing the literal {@code super.visitExtract}/{@code super.visitFilter}
     * fallbacks (byte-identical by the strongest argument); a future SPI target renders the
     * lambda natively from the children + the binder fact.
     */
    LAMBDA_OP,

    /**
     * A typed conversion ({@link IRConversion}) — {@code <child> to-enum <Target>} /
     * {@code to-number} / {@code to-int} / the temporal kinds — the #500 arm-B teach of the
     * {@code RConversionExpr} family (the census read 954 of 956 {@code to-enum}). Deliberately
     * DEEP: the single {@code child()} is load-bearing and all-or-nothing, with the neutral
     * operator token ({@code conversionKind}) and the {@code to-enum} target name carried as
     * facts. The {@code to-string} form is the separate {@link #TO_STRING} kind. The Java render
     * — legacy {@code ConversionHandler}'s parse/valueOf facet family — is served by the
     * compiler's range-correlated oracle-root renderer reusing the literal
     * {@code super.visitConversion} fallback (byte-identical by the strongest argument); a
     * future SPI target renders from the child + the kind/target facts.
     */
    CONVERSION,

    /**
     * A {@code then}-pipe chain root ({@link IRPipe}) — {@code <arg> then <body>} — the #500
     * arm-B teach of the {@code RThenExpr} family (100% implicit-bare binders at the census).
     * Deliberately SHALLOW this wave (the {@link #CONSTRUCT} pattern): the spine length is the
     * neutral fact; the argument/body subtrees are NOT carried as IR children, so every
     * child-position admission allow-list stays byte-frozen and the node lowers at the claim
     * ROOT only. The deep {@code Let}-desugared form stays the separate Python-side entry point
     * ({@code adaptThenChainToLet}, byte-inert on the Java path by its own contract). The Java
     * render — the {@code thenArg} hoist family — is served by the compiler's range-correlated
     * oracle-root renderer reusing the literal {@code super.visitThen} fallback (byte-identical
     * by the strongest argument).
     */
    PIPE,

    /**
     * An {@code only exists} check root ({@link IROnlyExists}) — {@code <path>[, <path>…] only
     * exists} — the #500 arm-B teach of the {@code ROnlyExistsExpr} family. Deliberately SHALLOW
     * this wave (the {@link #CONSTRUCT} pattern): the path count is the neutral fact; the
     * heterogeneous path elements (root / implicit-item flag / feature chain / receiver
     * expression) are NOT carried as IR children — the deep wave models them with an element
     * node. The Java render — the 3-arg {@code onlyExists(<receiver>, Arrays.asList(<all>),
     * Arrays.asList(<selected>))} wrap (the deprecated list form is rendered by no generator)
     * and the per-path chains — is served by the compiler's range-correlated oracle-root renderer reusing the
     * literal {@code super.visitOnlyExists} fallback (byte-identical by the strongest argument).
     */
    ONLY_EXISTS,

    /**
     * A disguised SYMBOL-receiver navigation ({@link IRSymbolNav}) — {@code <Rule|Function> ->
     * <feature>} parsed as a bound-chain {@code REnumValueRef} whose {@code resolvedSymbol} is a
     * workspace rule/function — the #501 arm-C teach of the {@code attributeChain.headSymbolNav}
     * face. Deliberately SHALLOW (the {@link #CONSTRUCT} pattern): the receiver symbol kind is
     * the neutral fact; the receiver invocation and feature hop are NOT carried as IR children
     * (the bare-delegation receiver and legacy's branch-selection ladder stay legacy's own). The
     * DISTINCT kind is the safety (the #499 law): consumers admit it only by name, and every
     * containing Java claim root renders through the compiler's oracle-root serve (the literal
     * {@code super.visitX} line — byte-identical by identity); the kind itself has no
     * leaf-emitter arm, so a native compose can never reach it.
     */
    SYMBOL_NAV,

    /**
     * A CLOSURE-PARAM reference ({@link IRClosureParam}) — a bare name naming an enclosing
     * inline function's declared lambda parameter, in either flavor the #502 census read: the
     * linker-BOUND named-param reference (the {@code symbolNotAttribute.RInlineFunction} class of
     * the #502 census — the binding is the parameter's own {@code RClosureParameter} node since
     * v3.2 seat 8, read through {@code RInlineFunction.declaringLambdaOf} in either shape —
     * {@code extract x [ … x … ]}) and the legacy re-entrant SYNTHESIS head resolved by name
     * ({@code symbolUnresolved.synthetic.closureParam} — render-time receivers whose head
     * resolver binds inputs/output only). Deliberately SHALLOW (the {@link #SYMBOL_NAV}
     * pattern): the param name is the neutral fact; the binder and its scope-live Java variable
     * naming are legacy's own (the L-029 split — lambda-witness naming is a Java-emission
     * decision). The DISTINCT kind is the safety (the #499 law): consumers admit it only by
     * name, every containing Java claim root renders through the compiler's oracle-root serve
     * (the literal {@code super.visitX} line), and the kind has no leaf-emitter arm.
     */
    CLOSURE_PARAM,

    /**
     * A FUNCTION call whose callee OUTPUT is {@code [metadata …]}-annotated
     * ({@link IRMetaOutputApply}) — the #502 {@code calleeMetaOutput} teach (406 sole at the
     * #501 SOT; 382 atRoot / 33 interior node-unit at the census, 100% FUNCTION callees).
     * Deliberately SHALLOW (the {@link #SYMBOL_NAV} pattern): the callee name is the neutral
     * fact; the argument renders, the meta-typed output threading and any downstream coercion
     * are legacy's own — NOT carried as IR children (an args-carrying deep form would invite
     * native arg consumption the meta output cannot survive). The DISTINCT kind is the safety
     * (the #499 law): NO consumer gate admits it BY DESIGN — an interior occurrence declines
     * its containing claim at the kind-dispatch end (or the containing root serves whole
     * through its standing oracle slot) — while a claim root renders through the compiler's
     * oracle-root serve ({@code super.visitSymbolReference} — the literal legacy call line),
     * and the kind has no leaf-emitter arm.
     */
    META_OUTPUT_APPLY,

    /**
     * An equality/comparison with an explicit {@code all}/{@code any} cardinality modifier
     * ({@link IRAllAnyCompare}) — {@code xs any = y} — the #503 {@code allAnyModifier} teach
     * (1,166 sole at the #502 SOT across both binary mirrors, ~96% {@code ANY.EQ} with both
     * operands lowerable). Deliberately SHALLOW (the {@link #SYMBOL_NAV} pattern): the operator
     * and modifier are the neutral facts; the operands are NOT carried — legacy renders the
     * whole modified comparison through its own operator family with the modifier as the
     * {@code CardinalityOperator} argument. The DISTINCT kind is the safety (the #499 law):
     * only {@code producesComparisonResult} admits it (the census's logical consumers), every
     * containing claim root renders through the compiler's oracle-root serve (the standing
     * equality/comparison dispatch legs — the literal {@code super.visitX} lines), and the
     * kind has no leaf-emitter arm.
     */
    ALL_ANY_COMPARE,

    /**
     * A resolved-feature navigation off an UN-RETYPEABLE synthetic implicit item
     * ({@link IRSynItemNav}) — the #504 {@code receiverSyntheticItem} residue teach (749 sole +
     * the bare-attr arm's 271 {@code attrOutsideFunction.receiverSyntheticItem} equivalents at
     * the #503 SOT): the receiver lowers to a {@code SYNTHETIC_ITEM} variable but BOTH the #479
     * filter/extract retype and the #492 alias retype decline (meta/unprovable/deep binder
     * sources, then/switch/named-walk-out binders — the #502 census's walked residue).
     * Deliberately SHALLOW (the {@link #SYMBOL_NAV} pattern): the resolved feature's name is
     * the neutral fact; the item's Java binding (the lambda variable, the then-arg, the
     * switch-case cast var) and every hop coercion are legacy's own (the L-029 split) — NOT
     * carried as IR children. The DISTINCT kind is the safety (the #499 law): the consumers
     * that admit it are NAMED — the nav-receiver gate (the chained hops) plus the equality/
     * existence operand gates and the call-arg gate (the post-arm probe's exposed frontier) —
     * every containing claim root renders through the compiler's oracle-root serve (the
     * {@code navChain}/{@code bareSymbolRef} dispatch legs — the literal {@code super.visitX}
     * lines), and the kind has no leaf-emitter arm.
     */
    SYN_ITEM_NAV,

    /**
     * A choice-OPTION selection spelled as a qualified name ({@link IRChoiceOptionNav}) —
     * {@code head -> OptionType} where the leaf segment resolved through the pass-6
     * choice-option channel ({@code REnumValueRef.resolvedChoiceOption} — the #451 clearing
     * arm's bind) — the #505 {@code choiceOption} teach (239 sole at the #504 SOT,
     * cdm6-dominant). Deliberately SHALLOW (the {@link #SYMBOL_NAV} pattern): the two name
     * segments and the OPTION's resolved binding are the neutral facts; the head's own
     * resolution (a scoped attribute, an implicit-item member, a global choice type) and the
     * option-selection render (the choice projection, the case narrowing) are legacy's own
     * render decisions (the L-029 split) — NOT carried as IR children. The DISTINCT kind is
     * the safety (the #499 law): the consumers that admit it are NAMED — the nav-receiver
     * gate (chained hops compose {@code FieldAccess} over it), the equality/existence operand
     * gates and the call-arg gate — every containing claim root renders through the
     * compiler's oracle-root serve (incl. the #505 {@code enumChain} dispatch leg — the
     * literal {@code super.visitEnumValueRef} line), and the kind has no leaf-emitter arm.
     */
    CHOICE_OPTION_NAV,

    /**
     * A bare reference to a DISPATCH-BASE input from inside a per-enum-value dispatch VARIANT
     * body ({@link IRDispatchInputRef}) — the #505 {@code symbolUnresolved.synthetic.absent}
     * teach (232 sole at the #504 SOT, the cdm-twin 116/116; the #504 qualNameGate census
     * decoded the class and the #505 dispatchGate census proved it 100% variant +
     * base-input-hit): the variant's own signature declares only the dispatch parameter (its
     * {@code inputs()} is the synthesized placeholder), so a body reference to a BASE
     * declaration input ({@code startDate}/{@code interestRatePayout} in the
     * YearFraction-family bodies) resolves through the dispatch scope-join legacy applies
     * ({@code HandlerHelper.dispatchBaseOf} — the PR #369 facet) and the fork's linker leaves
     * EMPTY. Deliberately SHALLOW (the {@link #SYMBOL_NAV} pattern): the input's name and its
     * attribute-channel type are the neutral facts; the render — the raw name in the
     * variant's evaluate signature (the base's declared inputs, the PR #369 signature law),
     * the {@code [calculation]}-body coercions — is legacy's own (the L-029 split). The
     * DISTINCT kind is the safety (the #499 law): every containing claim root renders
     * through the compiler's oracle-root serve (the {@code navChain}/{@code bareSymbolRef}
     * dispatch legs — the literal {@code super.visitX} lines, the oracle-first design the
     * charter mandates for the {@code [calculation]}-body byte risk), and the kind has no
     * leaf-emitter arm.
     */
    DISPATCH_INPUT_REF,

    /**
     * A deep-path feature navigation ({@link IRDeepFeatureNav}) — {@code receiver ->> feature},
     * the {@code DeepFeatureCallExpr} grammar operator — the #507 {@code RDeepFeatureCall:
     * noAdaptArm} teach (191 sole + 71 untargeted root visits at the #506 SOT, all cdm6
     * FUNCTION; the #507 deepGate census read the family 100% {@code featHit} + 100%
     * {@code recvLowers}). The RECEIVER subtree is a real IR child (it lowers by the arm's own
     * gate — the census's uniform fact); the deep-path RESOLUTION and render (legacy's
     * generated {@code DeepPathUtil.choose<Feature>} routing, the multi-candidate walk) are
     * legacy's own render decisions (the L-029 split) — only the resolved feature's NAME is
     * carried (the neutral fact). The DISTINCT kind is the safety (the #499 law): the
     * consumers that admit it are NAMED — the nav-receiver gate (chained hops compose
     * {@code FieldAccess} over it), the equality/existence operand gates and the call-arg
     * gate — and every containing claim root renders through the compiler's oracle-root serve
     * (incl. the #507 {@code deepChain} dispatch leg — the literal
     * {@code super.visitDeepFeatureCall} line), so the deep-path machinery is byte-identical
     * BY IDENTITY while the kind itself has NO leaf-emitter arm.
     */
    DEEP_FEATURE_NAV,

    /**
     * A RECORD-feature read spelled as a single-arrow navigation ({@link IRRecordFeatureNav})
     * — {@code head -> feature} where the head is an in-scope function value of a RECORD type
     * ({@code date}/{@code dateTime}/{@code zonedDateTime} — {@code basictypes.rosetta}) and
     * the leaf is one of the record's OWN features ({@code RRecordType.features()}, the #444
     * membership rule) — the #507 {@code REnumValueRef:noResolutionChannel} teach (175 sole
     * at the #506 SOT; the #507 noChanGate census read the dominant faces
     * {@code dispatchInput.rec:date.leafRecHit} [the YearFraction-family dispatch bodies] and
     * {@code input.rec:date/zonedDateTime.leafRecHit} [plain inputs, drr]). Record features
     * are NOT {@code RAttribute}s, so the fork's resolver deliberately binds NO channel (the
     * #444 clear-without-binding law) and the node's inferred type is MISSING BY CONSTRUCTION
     * — the mint carries the sentinel; no typing gate applies (unlike {@code
     * CHOICE_OPTION_NAV}), because the kind is served only BY IDENTITY. Deliberately SHALLOW
     * (the {@link #SYMBOL_NAV} pattern): the two name segments and the record type's name are
     * the neutral facts; the head's own resolution (a plain input/output, the dispatch-base
     * scope-join) and the record-feature render (legacy's record-feature machinery) are
     * legacy's own (the L-029 split). The DISTINCT kind is the safety (the #499 law): the
     * consumers that admit it are NAMED — the nav-receiver, equality/existence operand and
     * call-arg gates — and every containing claim root renders through the compiler's
     * oracle-root serve (incl. the #505 {@code enumChain} dispatch leg — the literal
     * {@code super.visitEnumValueRef} line), and the kind has no leaf-emitter arm.
     */
    RECORD_FEATURE_NAV,

    /**
     * A resolved-feature navigation off a META-SOURCED bound USER item ({@link IRMetaItemNav})
     * — {@code item -> feature} where the implicit item's registering binder ranges a
     * meta-annotated source (a {@code FieldWithMetaX}-wrapped element list: the filter/extract
     * direct meta-feature-call sources and the widened then/min/max/sort legs' unprovable
     * element forms — the L-029 boundary {@link
     * com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter}'s {@code
     * itemBindingMetaSourced} split names) — the #508 {@code RFeatureCall:itemMetaSourced}
     * teach (67 sole at the #507 SOT; the #508 metaSrcGate census read the accessed feature
     * 100% PLAIN [{@code featPlain}] over {@code metaFc}/{@code alias}/pipe sources, all
     * typed). Legacy renders the per-element binding with its own null-safe {@code "Type
     * coercion"} deref legs (the FieldWithMetaX unwrap the neutral IR deliberately does not
     * model — the L-029 split), so the item's Java binding and every hop coercion stay
     * legacy-side. Deliberately SHALLOW (the {@link #SYN_ITEM_NAV} pattern): only the resolved
     * feature's NAME is carried (the neutral fact; the feature gate passed — the RECEIVER's
     * meta-source proof is what declined the native compose). The DISTINCT kind is the safety
     * (the #499 law): the consumers that admit it are NAMED, and every containing claim root
     * renders through the compiler's oracle-root serve (the literal {@code super.visitX}
     * line), byte-identical BY IDENTITY, while the kind itself has NO leaf-emitter arm.
     */
    META_ITEM_NAV,

    /**
     * A RECORD-feature read over a LOWERED compound receiver ({@link IRRecordReceiverNav}) —
     * {@code <receiver> -> feature} where the receiver is any non-symbol expression whose
     * ENGINE type is a RECORD type and the leaf is one of the record's OWN features
     * ({@code RRecordType.features()}, the #444 membership rule) — the #512 arm-B1 teach of
     * the {@code RFeatureCall:featureUnresolved.nonSymbolReceiver} record class (the
     * #509-banked nsrGate census: {@code recvLowers:FieldAccess/IRSymbolNav +
     * rec:zonedDateTime/dateTime + mRecHit} — the {@code … -> value -> date}-family chains).
     * The #507 {@link #RECORD_FEATURE_NAV} twin keeps the SYMBOL-head seat (childless,
     * head-keyed); this kind is the receiver-carrying sibling ({@link IRDeepFeatureNav}
     * pattern — the RECEIVER subtree is a real IR child, the census's uniform
     * {@code recvLowers} fact; the record-feature render is legacy's own, the L-029 split).
     * The DISTINCT kind is the safety (the #499 law): the consumers that admit it are NAMED
     * — the call-arg gate — and every containing claim root renders through the compiler's
     * oracle-root serve (the shared {@code containsOracleLeaf} walk), byte-identical BY
     * IDENTITY, while the kind itself has NO leaf-emitter arm.
     */
    RECORD_RECEIVER_NAV,

    /**
     * A METADATA-QUALIFIER read off a bound USER item ({@link IRQualifierItemNav}) —
     * {@code item -> scheme} where the implicit item's registering filter/extract binder
     * ranges the elements of a {@code [metadata …]}-annotated feature and the nav's feature
     * name IS one of that feature's metadata qualifiers (never a member — the linker binds
     * no channel) — the #512 arm-B2 teach of the {@code RFeatureCall:featureUnresolved.
     * nonSymbolReceiver} qualifier class (the #509-banked nsrGate census: {@code
     * RImplicitVariable.recvLowers:IRVariable + mMiss + qMiss} over {@code string (0..*)
     * [metadata scheme]}-family sources; the #512 DEEP PIPE WALK —
     * {@code terminalBindingSourceFeature} — resolves the elided-pipe binder sources the
     * census's shallow read bottomed on, prove-or-decline). Deliberately SHALLOW (the
     * {@link #META_ITEM_NAV} pattern): the qualifier's name is the neutral fact; the item's
     * Java binding and the FieldWithMetaX qualifier deref are legacy's own (the L-029
     * split). The DISTINCT kind is the safety (the #499 law): the consumers that admit it
     * are NAMED — the equality operand and call-arg gates — and every containing claim root
     * renders through the compiler's oracle-root serve, byte-identical BY IDENTITY, while
     * the kind itself has NO leaf-emitter arm.
     */
    QUALIFIER_ITEM_NAV,

    /**
     * A {@code switch} expression root ({@link IRSwitchOp}) — {@code <arg> switch <guard>
     * then <result>, …} — the #513 noAdaptArm-sweep teach of the {@code RSwitchExpr} family
     * (33 sole + 2 untargeted at the #512 SOT). Deliberately SHALLOW (the #500
     * {@link IRPipe} family-arm pattern): the case count and default-case presence are the
     * neutral facts; the argument/guard/result subtrees are NOT carried — legacy's whole
     * switch ladder renders inside the compiler's {@code switchOp} oracle leg (the literal
     * {@code super.visitSwitch} line), whose re-walk visits every interior node at its own
     * seat (the #504 re-entrant law). The DISTINCT kind is the safety (the #499 law):
     * consumers admit it only by name, and the kind has no leaf-emitter arm.
     */
    SWITCH_OP,

    /**
     * A {@code default} fallback ({@link IRDefaultOp}) — {@code <left> default <right>} —
     * the #513 noAdaptArm-sweep teach of the {@code RDefaultExpr} family (24 sole + 84
     * untargeted at the #512 SOT). Deliberately SHALLOW (the #500 {@link IRPipe} family-arm
     * pattern): no extra facts; the operand subtrees are NOT carried — legacy's null-safe
     * default composition renders inside the compiler's {@code defaultOp} oracle leg (the
     * literal {@code super.visitDefault} line). The DISTINCT kind is the safety (the #499
     * law): consumers admit it only by name, and the kind has no leaf-emitter arm.
     */
    DEFAULT_OP,

    /**
     * A list-membership test ({@link IRMembershipOp}) — {@code contains}/{@code disjoint}
     * — the #513 noAdaptArm-sweep teach of the {@code RContainsExpr} + {@code RDisjointExpr}
     * families (23 + 2 sole, 33 + 2 untargeted at the #512 SOT). One kind for both operators
     * (the {@link IRListOp} flavour precedent) — deliberately NOT new {@link BinaryOp}
     * flavours, so the #511 BinaryOp-KIND-WIDE operand admissions stay unexamined-widening-
     * free (the #499 distinct-kind law). SHALLOW: the operator is the neutral fact; renders
     * via the {@code contains}/{@code disjoint} oracle legs (the literal
     * {@code super.visitContains}/{@code super.visitDisjoint} lines); no leaf-emitter arm.
     */
    MEMBERSHIP_OP,

    /**
     * An element-collecting postfix op ({@link IRCollectOp}) — {@code max}/{@code min}/
     * {@code sort} with an optional key body — the #513 noAdaptArm-sweep teach of the
     * {@code RMaxExpr} + {@code RMinExpr} + {@code RSortExpr} families (13 + 3 + 1 sole,
     * 86 + 37 + 10 untargeted at the #512 SOT). One kind for the three operators (the
     * {@link IRListOp} flavour precedent) — deliberately NOT new {@link IRListOp} flavours,
     * so the #511 IRListOp-KIND-WIDE receiver/operand admissions stay unexamined-widening-
     * free (the #499 distinct-kind law). SHALLOW: the operator + body presence are the
     * neutral facts; renders via the {@code maxOp}/{@code minOp}/{@code sortOp} oracle legs
     * (the literal {@code super.visitMax}/{@code super.visitMin}/{@code super.visitSort}
     * lines); no leaf-emitter arm.
     */
    COLLECT_OP,

    /**
     * A bare reference to the enclosing function's declared OUTPUT ({@link IROutputRef}) —
     * the #514 {@code notAnInputParam.out} teach (the face read 100% output-by-identity at
     * the decode probe, {@code declaring=RFunction} ≡ {@code fn.output()}; legacy
     * variable-paths the output holder — the {@code implicitAttrRoot} census's
     * {@code noLambda.varPath} rows). The SAME class is the dominant head family behind the
     * {@code receiverNotExpressible.child.recv:notAnInputParam.out} disguised navs, so the
     * receiver admission heals both faces with one mint. Deliberately SHALLOW (the
     * {@link #DISPATCH_INPUT_REF} pattern): the output's name and its attribute-channel type
     * are the neutral facts; the output-local naming and every hop coercion are legacy's own
     * (the L-029 split). The DISTINCT kind is the safety (the #499 law): the consumers that
     * admit it are NAMED — the nav-receiver, equality/comparison/arithmetic operand and
     * call-arg gates — and every containing claim root renders through the compiler's
     * oracle-root serve (the shared {@code containsOracleLeaf} walk — the literal
     * {@code super.visitX} lines), and the kind has no leaf-emitter arm.
     */
    OUTPUT_REF,

    /**
     * A bare reference to a META-ANNOTATED function input ({@link IRMetaParamRef}) — the
     * #514 {@code metaParam} teach (the #513 parent-facet decode: RDefaultExpr-operand +
     * ROperation statement + RKeyValuePair ctor seats, plus the
     * {@code receiverNotExpressible.child.recv:metaParam.*} disguised-nav heads — the
     * {@code price -> value} chains over {@code [metadata …]}-annotated params). Legacy
     * variable-paths the param and threads the {@code FieldWithMetaX} wrapper machinery
     * inside its own render lines. Deliberately SHALLOW (the {@link #DISPATCH_INPUT_REF}
     * pattern): the param's name and its attribute-channel type (the engine's own read —
     * PLAIN for qualifier-only annotations, the annotation itself the arm's gate) are the
     * neutral facts; every wrapper coercion is legacy's own (the L-029 split). The DISTINCT
     * kind is the containment (the #499 law): the unwrap-needing call-argument seats are NOT
     * admitted (the {@code pPlain}/{@code argSeatMiss} decode slices keep declining at the
     * callArgs gate), the admitted seats route every containing claim root through the
     * compiler's oracle-root serve, and the kind has no leaf-emitter arm.
     */
    META_PARAM_REF,

    /**
     * A TOP-LEVEL bare rule-input navigation ({@link IRRuleInputNav}) — {@code attr} at
     * rule-body top level, legacy's synthesized {@code MapperS.of(input) -> attr} render —
     * the #514 {@code attrOutsideFunction.ruleInputNav} teach (equality/existence/call-arg/
     * list-receiver seats, ALL typed; the probe2 pivot from the native
     * {@code FieldAccess{IRVariable{PARAM,"input"}}} form, whose rule-cell nav render is
     * unproven — the claims leaked to the emitter decline site — to the oracle leaf, the
     * #513-p3 spine precedent one hop shorter; the META-FEATURED slice mints the SAME kind,
     * legacy's FieldWithMetaX machinery riding inside the serve). Deliberately SHALLOW (the
     * {@link #SYN_ITEM_NAV} pattern): only the feature's NAME is carried; the {@code input}
     * receiver synthesis and every hop coercion are legacy's own (the L-029 split). The
     * DISTINCT kind is the safety (the #499 law): the consumers that admit it are NAMED,
     * every containing claim root renders through the compiler's oracle-root serve, and the
     * kind has no leaf-emitter arm.
     */
    RULE_INPUT_NAV,

    /**
     * A with-metadata annotation expression ({@link IRWithMetaOp}) — {@code <arg> with-meta
     * {…}} — the #515 untargeted-close teach (the meter's dominant residue, 92 root visits;
     * the meta-channel question the #513 sweep parked, answered the sweep's own way: the
     * render IS the serve). SHALLOW (the {@link #DEFAULT_OP} pattern): the entry count is
     * the neutral fact; the FieldWithMetaX builder threading is legacy's own (the L-029
     * split); renders via the {@code withMetaOp} oracle leg (the literal
     * {@code super.visitWithMeta} line); no leaf-emitter arm.
     */
    WITH_META_OP,

    /**
     * A list-join expression ({@link IRJoinOp}) — {@code <list> join [<sep>]} — the #515
     * untargeted-close teach (2 root visits; the #513 fixture-contrast anchor family, its
     * five recut locks moving on to reduce/cardinality-check with this teach). SHALLOW (the
     * {@link #COLLECT_OP} pattern): the separator's presence is the neutral fact (the #513
     * Copilot catch pinned it to {@code setSeparator}, never {@code rawRight}); renders via
     * the {@code joinOp} oracle leg (the literal {@code super.visitJoin} line); no
     * leaf-emitter arm.
     */
    JOIN_OP,

    /**
     * An input-feature navigation over an OUTPUT-consuming alias head
     * ({@link IROutputAliasNav}) — {@code <alias> -> <feature>} where the alias body
     * references the enclosing function's output — the #518 usesOutput teach (the L-111
     * belt's blocked residue + the probed twins at the same nav gate; the face every
     * Mapper-shaped arm must never admit: legacy's {@code FunctionAliasHelper
     * .inferShortcutMapperJavaType} hard-declines the Mapper signature for an
     * output-referencing body, and the W-class output-builder nav renders a BUILDER walk,
     * never a Mapper). Deliberately SHALLOW (the {@link #RULE_INPUT_NAV} pattern): only the
     * alias head name and the feature name are carried; the builder-walk render is legacy's
     * own whole line (the L-029 split). The DISTINCT kind is the safety (the #499 law): the
     * kind never enters the {@code FieldAccess}-over-{@code ALIAS} native quiet-claim shape,
     * has no leaf-emitter arm, and every claim root that carries it renders BY IDENTITY —
     * the L-111 seat's #507 identity-serve leg at the relabel seat, the standing
     * enumChain/navChain oracle serves at the probed roots.
     */
    OUTPUT_ALIAS_NAV,

    /**
     * A LIBRARY-function application ({@link IRLibraryApply}) — {@code Min(a, b)} /
     * {@code Max(a, b)} / {@code IsLeapYear(x)}: an args-present symbol reference whose
     * symbol is an {@code RLibraryFunction} builtin — the #520 teach of the
     * {@code calleeNotFunction} face (the #519-SOT calleeGate census: 100%
     * {@code RLibraryFunction}, 100% atRoot). Childless SHALLOW (the
     * {@link #META_OUTPUT_APPLY} pattern verbatim): the args are NOT carried — a deep form
     * would invite native argument consumption while the render is legacy's own
     * external-function threading; only the callee name is the neutral fact. At-root
     * claims serve through the oracle callArgs dispatch (the literal
     * {@code super.visitSymbolReference} line), interiors route their containing roots
     * whole-legacy through {@code containsOracleLeaf}; no leaf-emitter arm.
     */
    LIBRARY_APPLY,

    /**
     * A METADATA-QUALIFIER read over a LOWERED bare-item member head
     * ({@link IRQualifierReceiverNav}) — {@code partyReference -> reference} /
     * {@code identifier -> scheme} where the head is a disguised bare-item member nav (an
     * EMPTY-symbol reference the #524 admission lowers — the headUnGate census's uniform
     * {@code headLowers:IRMetaAccess} fact), the head's resolved attribute is
     * {@code [metadata …]}-annotated and the nav's feature name IS one of its metadata
     * qualifiers (never a member — the {@code featureUnresolved.headUnresolved} class) — the
     * #525 teach of the #524 wave's own exposed frontier. The #512
     * {@link #QUALIFIER_ITEM_NAV} twin keeps the ITEM seat (childless); this kind is the
     * receiver-carrying sibling (the {@link #RECORD_FEATURE_NAV} →
     * {@link #RECORD_RECEIVER_NAV} relationship at the qualifier seat — the RECEIVER subtree
     * is a real IR child, the qualifier deref legacy's own, the L-029 split). The DISTINCT
     * kind is the safety (the #499 law): the consumers that admit it are NAMED — the
     * equality-operand and existence-operand gates — and every containing claim root renders
     * through the compiler's oracle-root serve (the shared {@code containsOracleLeaf} walk;
     * at-root claims serve through the standing {@code navChain} dispatch leg),
     * byte-identical BY IDENTITY, while the kind itself has NO leaf-emitter arm.
     */
    QUALIFIER_RECEIVER_NAV,

    /**
     * A bare ATTRIBUTE reference whose receiver legacy synthesizes
     * ({@link IRImplicitAttrNav}) — {@code attr} naming a member of the enclosing lambda's
     * element type (or of a rule/report scope) that legacy expands to an implicit input/item
     * navigation — the #528 teach of the L-113 relabel belt: the residue the adapter's own
     * bare-attr item-nav arm leaves when it cannot BUILD the equivalent navigation (the #497
     * {@code implicitAttrRoot} census's {@code shortcutCollision} / {@code
     * noFilterExtractBinder} / {@code sourceElementUnresolved} walk families). Minting the
     * lowering is the ACCOUNTING move (the #516 law): the belt's claims sat in the compiler's
     * ir-EMPTY branch and counted DELEGATED, so only a real lowering — not a counter flip —
     * converts them to IR-LOWERED, with the render unchanged. Deliberately SHALLOW (the
     * {@link #RULE_INPUT_NAV} pattern one seat over): only the attribute's NAME is carried;
     * the receiver synthesis and every hop coercion are legacy's own (the L-029 split). The
     * consumers that admit it are NAMED (the #499 law): the #529 equality-operand admission
     * (the mint's own disclosed frontier — the {@code filter intentToClear = True}-family
     * bare-attr-vs-literal predicates) and, since PR #640 (v3.3 seat 4, the data-rule heal), the
     * navigation RECEIVER ({@code attr -> x}: the grammar's own form of it is the DISGUISED {@code REnumValueRef},
     * the {@code RFeatureCall} form is synthesized by the adapter and by legacy, never parsed; a meta leaf over it included), the
     * COMPARISON operand and its sibling, the EQUALITY sibling, the EXISTENCE operand and the CALL
     * argument - every one an oracle-leaf admission: the containing root renders WHOLE through legacy.
     * Since PR #640 the mint also takes the META-annotated attribute (typed by the reference's own
     * engine type). At-root claims serve through the standing
     * {@code bareSymbolRef} dispatch leg (the literal {@code super.visitSymbolReference}
     * line, the belt's own render), interiors route their containing roots whole-legacy
     * through {@code containsOracleLeaf}; no leaf-emitter arm.
     */
    IMPLICIT_ATTR_NAV,

    /**
     * A choice-OPTION selection over a LOWERED receiver ({@link IRChoiceReceiverNav}) —
     * {@code <receiver> -> OptionType} where the receiver's PROVEN form is a CHOICE (the
     * head attribute's declared choice at the symbol-head channel, or the lowered receiver's
     * own proven-choice type at the chain-hop channel) and the feature segment selects one
     * of the choice's own options BY TYPE NAME (the #503 arm-B1 projection at the parsed
     * seat) — the #529 {@code featureUnresolved.headAttr.declMiss} teach (35 cdm6-f sole at
     * the #528 SOT; the headAttrGate census read the pool 100% {@code ht:choice:<name>} —
     * the {@code observable -> Asset}-family navigations the #492 facet's
     * {@code declaredDataTypeOf} ladder can never derive). The #505
     * {@link #CHOICE_OPTION_NAV} twin keeps the qualified-name seat (childless); this kind
     * is the receiver-carrying sibling (the {@link #RECORD_FEATURE_NAV} →
     * {@link #RECORD_RECEIVER_NAV} relationship at the choice seat — the #525 twin
     * convention). The DISTINCT kind is the safety (the #499 law): the consumers that admit
     * it are NAMED — the #529 chain-hop leg (a deeper option hop composes over it) and the
     * equality/existence operand gates — and every containing claim root renders through
     * the compiler's oracle-root serve (the shared {@code containsOracleLeaf} walk;
     * at-root claims serve through the standing {@code navChain} dispatch leg),
     * byte-identical BY IDENTITY, while the kind itself has NO leaf-emitter arm.
     */
    CHOICE_RECEIVER_NAV,

    /**
     * The data-rule CONDITION'S implicit instance ({@link IRConditionInstance}) - the reference legacy synthesizes
     * to the condition method's own parameter ({@code adjustableDates} inside {@code type AdjustableDates}) while it
     * renders a condition body and re-enters the compiler with (v3.3 seat 4, PR #640: the blocker probe on the
     * DATA_RULE seam ranked the class FIRST, 9,393 sole claims on DRR 7.3). Deliberately SHALLOW (the
     * {@link #IMPLICIT_ATTR_NAV} pattern): only the owning type's NAME is carried, typed as that data type; the
     * parameter naming and every navigation off it are legacy's own. An oracle leaf - every containing root renders
     * WHOLE through the legacy handler by identity; the consumers that admit it are NAMED (the #499 law): the
     * navigation-receiver set ({@code <instance> -> attr}, legacy's synthesized shape) and the standing oracle-root
     * serve at root; no leaf-emitter arm.
     */
    CONDITION_INSTANCE
}
