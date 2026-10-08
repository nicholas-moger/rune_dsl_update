package com.regnosys.rosetta.generator.java.ir;

import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.SourceRange;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.expressions.binary.RArithmeticExpr;
import com.regnosys.rosetta.ast.expressions.binary.RComparisonExpr;
import com.regnosys.rosetta.ast.expressions.binary.RContainsExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDefaultExpr;
import com.regnosys.rosetta.ast.expressions.binary.RDisjointExpr;
import com.regnosys.rosetta.ast.expressions.binary.REqualityExpr;
import com.regnosys.rosetta.ast.expressions.binary.RJoinExpr;
import com.regnosys.rosetta.ast.expressions.binary.RLogicalExpr;
import com.regnosys.rosetta.ast.expressions.binary.RThenExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCardinalityCheckExpr;
import com.regnosys.rosetta.ast.expressions.unary.RConversionExpr;
import com.regnosys.rosetta.ast.expressions.unary.RCountExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExistenceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RExtractExpr;
import com.regnosys.rosetta.ast.expressions.unary.RFilterExpr;
import com.regnosys.rosetta.ast.expressions.unary.RListOpExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMaxExpr;
import com.regnosys.rosetta.ast.expressions.unary.RMinExpr;
import com.regnosys.rosetta.ast.expressions.unary.ROnlyExistsExpr;
import com.regnosys.rosetta.ast.expressions.unary.RReduceExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSortExpr;
import com.regnosys.rosetta.ast.expressions.unary.RSwitchExpr;
import com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
import com.regnosys.rosetta.ast.expressions.unary.RWithMetaExpr;
import com.regnosys.rosetta.ast.expressions.literals.RBooleanLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RIntLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RListLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RNumberLiteral;
import com.regnosys.rosetta.ast.expressions.literals.RStringLiteral;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSuperCall;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RKeyValuePair;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.functions.RCondition;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RRule;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.regulatory.RBody;
import com.regnosys.rosetta.ast.regulatory.RCorpus;
import com.regnosys.rosetta.ast.types.RChoice;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.supporting.RRecordFeature;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.ast.types.RRecordType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.ExpressionCompiler;
import com.regnosys.rosetta.generator.java.expression.ExpressionContext;
import com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ControlFlowHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper;
import com.regnosys.rosetta.generator.java.expression.handlers.LiteralHandler;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.function.FunctionDependencyCollector;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.statement.builder.JavaExpression;
import com.regnosys.rosetta.generator.java.statement.builder.JavaStatementBuilder;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.ir.expr.IRApply;
import com.regnosys.rosetta.ir.expr.FieldAccess;
import com.regnosys.rosetta.ir.expr.IRClosureParam;
import com.regnosys.rosetta.ir.expr.IRConditional;
import com.regnosys.rosetta.ir.expr.IRConstruct;
import com.regnosys.rosetta.ir.expr.IRConversion;
import com.regnosys.rosetta.ir.expr.IRExpr;
import com.regnosys.rosetta.ir.expr.IRLambdaOp;
import com.regnosys.rosetta.ir.expr.IRMetaAccess;
import com.regnosys.rosetta.ir.expr.IRMetaItemNav;
import com.regnosys.rosetta.ir.expr.IRMetaOutputApply;
import com.regnosys.rosetta.ir.expr.IREmptyLiteral;
import com.regnosys.rosetta.ir.expr.IROnlyExists;
import com.regnosys.rosetta.ir.expr.IRPipe;
import com.regnosys.rosetta.ir.expr.IRPointFreeApply;
import com.regnosys.rosetta.ast.supporting.REnumValue;
import com.regnosys.rosetta.ast.supporting.RTypeCall;
import com.regnosys.rosetta.ast.types.REnumeration;
import com.regnosys.rosetta.types.REnumTypeRef;
import com.regnosys.rosetta.ir.expr.IRAllAnyCompare;
import com.regnosys.rosetta.ir.expr.IRSymbolNav;
import com.regnosys.rosetta.ir.expr.IRChoiceOptionNav;
import com.regnosys.rosetta.ir.expr.IRDeepFeatureNav;
import com.regnosys.rosetta.ir.expr.IRDispatchInputRef;
import com.regnosys.rosetta.ir.expr.IRMetaParamRef;
import com.regnosys.rosetta.ir.expr.IROutputRef;
import com.regnosys.rosetta.ir.expr.IRRuleInputNav;
import com.regnosys.rosetta.ir.expr.IRRecordFeatureNav;
import com.regnosys.rosetta.ir.expr.IRRecordReceiverNav;
import com.regnosys.rosetta.ir.expr.IRQualifierItemNav;
import com.regnosys.rosetta.ir.expr.IRQualifierReceiverNav;
import com.regnosys.rosetta.ir.expr.IRSynItemNav;
import com.regnosys.rosetta.ir.expr.IRToString;
import com.regnosys.rosetta.ir.expr.IRReference;
import com.regnosys.rosetta.ir.expr.IRVariable;
import com.regnosys.rosetta.ir.expr.adapter.ExpressionToIRAdapter;
import com.regnosys.rosetta.symbols.RWorkspace;
import com.regnosys.rosetta.types.ExpressionCardinality;
import com.regnosys.rosetta.types.RChoiceTypeRef;
import com.regnosys.rosetta.types.RDataTypeRef;
import com.regnosys.rosetta.types.RMetaAnnotatedType;
import com.regnosys.rosetta.types.RMissingType;
import com.regnosys.rosetta.types.RType;
import com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Path-2 (IR-routed) variant of {@link ExpressionCompiler}: the strangler seam where
 * expression emission migrates from the legacy AST-driven handlers to the neutral
 * expression IR, one behaviour at a time.
 *
 * <p><strong>Wave 0 — literal leaves are IR-driven.</strong> The {@code visit*Literal}
 * methods lower the node through {@link ExpressionToIRAdapter} → {@link IRJavaLeafEmitter}
 * (reading only IR facts) and emit the Java from the IR: string, boolean, number and
 * {@code empty} fully, and integer literals up to {@code long} range (the beyond-long form
 * hoists, so it stays on legacy). The references tier is underway — the bare <em>scalar
 * function-parameter</em> case of {@code RSymbolReference} (scalar → {@code MapperS.of(name)},
 * multi → {@code MapperC.<Item>of(name)}),
 * {@code super} (→ {@code super.doEvaluate()}) and the bare <em>enum-value</em> reference
 * (→ {@code EnumName.CONSTANT}) are IR-driven — and the <em>structural</em> tier has landed for
 * the boolean binary operators: the ordered <em>comparison</em> and <em>equality</em> of two
 * scalar parameters (→ {@code {method}(MapperS.of(l), MapperS.of(r), CardinalityOperator.{All
 * |Any})}, the {@code Any} default being {@code <>}'s) and the <em>logical</em> {@code and}/{@code or}
 * composing those boolean leaves (→ {@code left.andNullSafe(right)} / {@code orNullSafe}) — plus the
 * first unary structural family, a scalar-parameter <em>existence</em> check (→ {@code exists}/
 * {@code notExists}/…{@code (MapperS.of(name))}), itself a {@code ComparisonResult} that composes
 * through logical, and the first <em>navigation</em> hop — a single non-meta feature off a scalar
 * parameter (→ {@code receiver.<Witness>map("getX", v -> v.getX())}) — where the IR tree's operands
 * recurse through the emitter. Aliases, calls, navigation chains, multi/meta features,
 * literal/enum/nav comparison operands and all other compound families still compile through the
 * inherited legacy handlers.
 * The migration is byte-safe by construction: the
 * IR emitter reproduces the legacy {@code Mapper} form factory-call-for-factory-call, and
 * any node the IR cannot (yet) express falls back to {@code super} (the strangler net).
 * The {@code Path2ByteIdentityTest} FUNCTION rows are the oracle that must stay green.
 *
 * <p>Used only by {@link IRFunctionGenerator} (i.e. behind the lab's IR routing);
 * Path-1 is otherwise untouched. Lab-authored Phase-2 (not present upstream).
 */
public class IRExpressionCompiler extends ExpressionCompiler {

    private final ExpressionToIRAdapter adapter = new ExpressionToIRAdapter();
    private final IRJavaLeafEmitter leafEmitter = new IRJavaLeafEmitter();

    /**
     * The IR-routed {@link IRCollectionHandler} this compiler created — kept so the FUNCTION byte gate can read its
     * slice-4 firing count. <strong>Declared with NO initializer on purpose:</strong> {@link #createCollectionHandler()}
     * is invoked from the BASE {@code ExpressionCompiler} constructor (during {@code super()}), BEFORE this subclass's
     * field initializers run (JLS §12.5), so the assignment made there must NOT be clobbered — a {@code = null} / {@code
     * = new …} initializer would run after {@code super()} and overwrite it. (The {@code adapter}/{@code leafEmitter}
     * initializers above are unaffected — the factory does not touch them.)
     */
    private IRCollectionHandler irCollectionHandler;

    /**
     * The IR-routed {@link IRLiteralHandler} this compiler created — kept so the FUNCTION byte gate can read its
     * slice-5 {@code bigInteger} firing count. <strong>Declared with NO initializer on purpose</strong> for the
     * SAME JLS §12.5 reason as {@link #irCollectionHandler}: {@link #createLiteralHandler()} is invoked from the
     * BASE {@code ExpressionCompiler} constructor (during {@code super()}, at the {@code literalHandler} assignment),
     * BEFORE this subclass's field initializers run, so a {@code = null} / {@code = new …} initializer would run
     * after {@code super()} and clobber the assignment made there.
     */
    private IRLiteralHandler irLiteralHandler;

    /**
     * The IR-routed {@link IRControlFlowHandler} this compiler created — kept so the FUNCTION byte gate can read its
     * slice-6 {@code ifThenElseResult} firing count (the two expression-compiler-reached ite seats). <strong>Declared
     * with NO initializer on purpose</strong> for the SAME JLS §12.5 reason as {@link #irCollectionHandler} /
     * {@link #irLiteralHandler}: {@link #createControlFlowHandler()} is invoked from the BASE
     * {@code ExpressionCompiler} constructor (during {@code super()}, at the {@code controlFlowHandler} assignment),
     * BEFORE this subclass's field initializers run, so a {@code = null} / {@code = new …} initializer would run
     * after {@code super()} and clobber the assignment made there.
     */
    private IRControlFlowHandler irControlFlowHandler;

    /**
     * Count of expressions this compiler emitted from the IR rather than via legacy
     * fallback. Lets a test confirm the IR path actually fired (guards against a
     * vacuous "green only because everything fell back" result).
     */
    private int irDrivenCount = 0;

    /**
     * Count of expressions in an IR-<em>targeted</em> family (one of the overridden {@code visit*}
     * methods) that fell back to the legacy handler — because the adapter could not lower the node or
     * the leaf emitter declined its kind. Together with {@link #irDrivenCount} this gives the §4.2
     * progress metric: {@code irDriven / (irDriven + irDeclined)} = the IR-driven share of expression
     * nodes in the families the IR currently attempts. (The 20 families with no {@code tryEmitFromIR}
     * override never reach this seam and are not counted — so the ratio measures attempt success, not
     * total coverage; since #475 they ARE separately visible on {@link #untargetedVisitBreakdown()},
     * the count-then-super census meters.)
     */
    private int irDeclinedCount = 0;

    /**
     * The #493 driven-metric split (the #492 relabel-banking discovery's mandate): of
     * {@link #irDrivenCount}, how many increments moved on a render that is WHOLESALE legacy's own
     * fallback/oracle render for a claim root the adapter could NOT lower — since the #516
     * delegated-seat serve the split's line is the ADAPTER-LOWERING fact: a claim with no IR lowering
     * whose relabel gate claimed the raw shape is DELEGATED; a claim whose IR lowering EXISTS counts
     * LOWERED whatever the render route (native emission or a literal-fallback identity serve with its
     * own receipt — the #507 accounting law). The eight delegated
     * seats (each with its own firing sub-counter; this total increments at the SAME eight seats, so
     * {@code irDrivenDelegatedCount == Σ} of the eight is a two-channel conservation the D11 ring
     * HARD-asserts): the L-111 disguised-input-nav relabel ({@link #inputFeatureNavDrivenCount}), the
     * L-109 point-free residue belt ({@link #pointFreeFnDrivenCount}), the L-109e implicit-input-nav
     * relabel ({@link #implicitInputNavDrivenCount}), the L-109d meta-nav relabel
     * ({@link #metaNavDrivenCount}), the L-112 input-feature-receiver relabel
     * ({@link #inputFeatureNavReceiverDrivenCount}), the L-113 implicit-attr relabel
     * ({@link #implicitAttrNavDrivenCount}) — the four live belts + two structurally-empty belts, all
     * inside the adapt gate's ir-EMPTY branch — plus two FROZEN-ZERO seats the #516 serve CONVERTED
     * (their claims sit after the adapt gate, ir present by construction): the #471/#473/#474
     * claim-root seat ({@link #claimRootDelegatedCount} — new at #493; that seat had no sub-counter)
     * and the #472 composition router ({@link #postPinDelegatedCount}); both now count LOWERED on
     * their own flip receipts ({@link #claimRootServeLoweredCount}/{@link #postPinServeLoweredCount})
     * and no code path increments their delegated sub-counters. The complement —
     * {@code irDrivenCount − irDrivenDelegatedCount} — is the IR-LOWERED share: the adapter lowered
     * the claim and the leaf emitter emitted it, or a receipted identity serve rendered it. An emitter emission whose per-emission renderer
     * reuses a legacy oracle MID-emission (the rule-delegation/point-free/item-arm renderers — the
     * L-029/D43-seam architecture keeps gm-aware Java sub-decisions legacy-side BY DESIGN) counts as
     * LOWERED: the split's line is claim-root render provenance, not sub-render provenance (stated on
     * the D11 split line's javadoc too, so the census reader inherits the definition). Counting-only:
     * no render behavior gates on this field anywhere.
     */
    private int irDrivenDelegatedCount = 0;

    /**
     * The #475 share-growth census: {@link #irDeclinedCount}'s per-family breakdown, keyed by the CLAIM
     * ROOT's node class simple name. Bumped exactly where the scalar bumps ({@link #recordDecline} — the
     * single helper increments the scalar, this map and {@link #irDeclinedBySite} together, so the three
     * conserve by construction: family-sum == site-sum == scalar). The worklist ranking for the
     * emitter-teach phase: the biggest family is the next teach candidate, sized on the live corpus
     * rather than guessed (the #470/#473 decode-first law applied to the whole declined population).
     */
    private final Map<String, Integer> irDeclinedByFamily = new HashMap<>();

    /**
     * The #475 census's per-SITE axis: which of the five decline seats inside {@link #tryEmitFromIR}
     * declined the claim — {@code adapterGap} (the adapter returned empty and no relabel gate claimed
     * the node), {@code postPinGuard} (the #467 guard's honest residue — explicit ZERO on the frozen
     * corpus since #473/#474), {@code ruleDelegation} (a bare rule reference legacy would not dispatch),
     * {@code aliasResolution} (an alias whose enclosing function did not resolve), {@code leafEmitter}
     * (the adapter lowered the node but {@link IRJavaLeafEmitter} has no arm for some IR kind in the
     * tree). Separates "no adapt arm" work (rune-ir) from "no emitter arm" work (rune-ir-java) — the
     * two teach surfaces have different owners, so the census must not blur them.
     */
    private final Map<String, Integer> irDeclinedBySite = new HashMap<>();

    /**
     * The #475 census's UNTARGETED axis: per-family visit counts for the 20 {@code visit*} families with
     * no IR attempt at all (no {@code tryEmitFromIR} override — the population the §4.2 denominator has
     * never seen). Bumped by the count-then-super overrides below ({@code visitConstructor} … — count,
     * then the EXACT super call the un-overridden dispatch would have made, so the render is
     * byte-identical BY CONSTRUCTION: there is no IR attempt, no speculative state, only the counter
     * moves). A visit is counted at every depth the legacy walk reaches: nested nodes under an
     * IR-EMITTED claim are never visited (the leaf emitter renders the whole subtree), while nodes
     * under a DELEGATED or oracle-reuse render ARE walked by the legacy visitor and count — so this
     * measures exactly the node population the legacy walk renders per family, the same unit the
     * driven/declined counters have used since #468.
     */
    private final Map<String, Integer> untargetedVisitsByFamily = new HashMap<>();

    /**
     * The #491 implicitVisit facet probe: the {@code RImplicitVariable} slice of the untargeted
     * census ({@link #untargetedVisitsByFamily}) refined IN PLACE at its single bump seat
     * ({@link #visitImplicitVariable}) — the #486/#488 decode pattern lifted to an UNTARGETED
     * family (the flat count has no decode channel BY DESIGN, so the planned one-seat
     * {@code tryEmitFromIR} arm could not size its honest residue: the leaf emitter's
     * {@code type().isMissing()} gates decline fresh render-time-minted implicits and untypeable
     * elided operands, and the driven/declined split was UNKNOWN). The spelling
     * {@code implicitVisit.<user|synthetic>.<typed|typeMissing>.<context>} — kind from the raw
     * node's own {@code isSynthetic()} (the SAME read the adapter's unconditional lowering keys
     * its {@code VariableKind} from, so the facet PREDICTS the emitter arm), typing from the
     * fixed-point cache ({@code getInferredType} — the MISSING sentinel for any node the fixed
     * point never saw, exactly the emitter gate's read), context from the #485-convention parent
     * walk ({@code minted} first — the #486 single-hop child-linkage mechanism: an UP-only
     * parented node is a render-time synthesis, every {@code ReferenceHandler} mint seat
     * {@code setParent}s without child-linking; then {@code lambda} / {@code switchTop} /
     * {@code condTop} / {@code ruleTop} / {@code fnTop} walking up, {@code detached} for a
     * parentless node, {@code other} on walk-out). Bumped at the SAME seat as the flat counter
     * so Σ facets ≡ the family's flat count per cell BY CONSTRUCTION (the #489 one-walk law);
     * gated on {@link #blockerProbeEnabled} — the classification walks run only where the census
     * computes, zero standing-render cost, and the standing receipts stay byte-identical
     * probe-off.
     */
    private final Map<String, Integer> implicitVisitFacets = new TreeMap<>();

    /**
     * The #494 ctorVisit facet probe: the {@code RConstructorExpr} visit population refined IN
     * PLACE at its single seat ({@link #visitConstructor} — pre-arm the untargeted census's bump
     * seat; post-teach the seat CLAIMS and the probe reads driven+declined ≡ Σ facets) — the #491
     * decode pattern repeated on the NEXT-biggest untargeted family (3,501 at the #493 read; the
     * probe was pre-decode-gated on the #492 alias teach, which landed). The spelling
     * {@code ctorVisit.<target>.<typing>.<values>[.spread]}:
     * <ul>
     *   <li><b>target</b> mirrors legacy {@code ConstructionHandler.tryTypedBuilderBlock}'s own
     *       entry gates (the golden-form render the future arm must reproduce): {@code data}
     *       ({@code resolveTypeCall} → {@code RDataTypeRef}), {@code choice} ({@code RChoiceTypeRef}),
     *       {@code otherType} (any other resolution — the typed block declines to the one-line
     *       legacy placeholder there), {@code noTypeCall};</li>
     *   <li><b>typing</b> reads the fixed-point cache exactly as the leaf emitter's type gates
     *       would ({@code getInferredType} — MISSING sentinel ⇒ {@code typeMissing}, the #491
     *       convention);</li>
     *   <li><b>values</b> sizes the DEEP-node design (an {@code IRConstruct} carrying every pair
     *       value as a lowered IR child) against the shallow oracle-closed alternative:
     *       {@code valuesLowerable} (every pair value {@code adapter.adapt}-lowerable TODAY),
     *       {@code valueBlocked.<Family>} (the FIRST source-order pair value the adapter declines,
     *       keyed by its node family — the recursion class {@code valueBlocked.RConstructorExpr}
     *       reads nested constructors), {@code noPairs} (a pair-free constructor — vacuously
     *       lowerable, kept distinct to see the population);</li>
     *   <li>the {@code .spread} suffix marks the {@code ...} operator (legacy's render never reads
     *       {@code isSpread()} — render-inert there, but a neutral IR node must record it, so the
     *       face sizes the proof obligation).</li>
     * </ul>
     * Bumped at the visit seat itself so Σ facets ≡ the family's WHOLE visit population per cell
     * BY CONSTRUCTION (the #489 one-walk law; pre-arm that equalled the untargeted flat count,
     * post-teach it reads driven+declined); gated on {@link #blockerProbeEnabled} — the
     * classification (including the per-value re-adapt walks, the #476 blocker-probe cost class)
     * runs only where the census computes, zero standing-render cost, and the standing receipts
     * stay byte-identical probe-off.
     */
    private final Map<String, Integer> ctorVisitFacets = new TreeMap<>();

    /**
     * The #495 condVisit facet probe: the {@code RConditionalExpr} visit population refined IN
     * PLACE at its single seat ({@link #visitConditional} — pre-arm the untargeted census's bump
     * seat; post-teach the seat CLAIMS and the probe reads driven+declined ≡ Σ facets) — the
     * #491/#494 decode pattern repeated on the census-marked adapter-READY family (1,133 at the
     * #494 read). Unlike the #494 probe this sizes a SEAM-CLAIM
     * yield, not an adapter design: {@code adaptConditional} is already wired into the shared
     * {@code adapt} dispatch (step-12), but its D1 all-or-nothing recursion (condition + then +
     * genuine else must ALL lower) caps the claimable slice, and the probe measures that cap plus
     * the #494-class post-pin-guard interaction BEFORE any arm lands. The spelling
     * {@code condVisit.<shape>.<typing>.<lower>[.guardTrip]}:
     * <ul>
     *   <li><b>shape</b> is {@code else}/{@code noElse} via {@link #hasGenuineElseShape} (the
     *       adapter's {@code hasGenuineElse} restated: {@code DefaultElseRule} stamps a synthetic
     *       empty else on every else-omitting conditional, so raw presence proves nothing) — the
     *       else-bearing vs else-less render-class split (a genuine-else conditional can be
     *       PRESENT-typed; an else-less one is OPTIONAL by the adapter's D5 absorbing rule);</li>
     *   <li><b>typing</b> reads the fixed-point cache exactly as the leaf emitter's type gates
     *       would ({@code getInferredType} — MISSING sentinel ⇒ {@code typeMissing}, the #491
     *       convention);</li>
     *   <li><b>lower</b> is the ROOT {@code adapter.adapt} verdict — {@code adapterLowers} when
     *       the whole conditional lowers TODAY, else {@code blocked.<slot>.<Family>} naming the
     *       first source-order declining slot ({@code cond}/{@code then}/{@code else}, the
     *       adapter's own recursion order) by that slot expression's node family (the ctorVisit
     *       values-leg convention), else {@code blocked.ctorSlot.<slot>} — every slot lowers at
     *       the probe's ROOT position yet the conditional declines, because the slot IS a
     *       constructor and the #494 ctor arm lowers ROOT-ONLY (the first probe cut read this
     *       class at 320 of 1,133 — the {@code arg:IRConstruct} admission-lever class at the
     *       conditional's slots; the #495 ctor-slot admission then claimed it WHOLE, so the face
     *       reads ZERO post-arm and stands as the admission's regression belt);</li>
     *   <li>the {@code .guardTrip} suffix marks an {@code adapterLowers} root whose claimed
     *       subtree trips {@link #subtreeTripsPostPinCoercion} — sizing the #494-class
     *       guard-exemption question on THIS family with receipts before the arm (the #494 ring
     *       caught that interaction on run one; this probe reads it on run zero).</li>
     * </ul>
     * Bumped at the visit seat itself so Σ facets ≡ the family's WHOLE visit population per cell
     * BY CONSTRUCTION (the #489 one-walk law; pre-arm that equalled the untargeted flat count,
     * post-teach it reads driven+declined); gated on {@link #blockerProbeEnabled} — the
     * classification (including the root re-adapt + guard-scan walks, the #476 blocker-probe cost
     * class) runs only where the census computes, zero standing-render cost, and the standing
     * receipts stay byte-identical probe-off.
     */
    private final Map<String, Integer> condVisitFacets = new TreeMap<>();

    /**
     * The #496 lambdaVisit facet probe (the monster wave's leg 1): the {@link RExtractExpr} and
     * {@link RFilterExpr} visit populations refined IN PLACE at their two seats
     * ({@link #visitExtract} / {@link #visitFilter} — the untargeted census's bump seats for the
     * two biggest untargeted families, 3,140 + 2,014 at the #495 census) — the #491/#494/#495
     * decode pattern at the lambda super-cluster. At the cut-1 read neither family had an adapt
     * arm, so like the #494 ctorVisit probe the probe sized an ARM DESIGN (the D1 recursion
     * pieces — receiver, then lambda body — each re-adapted at the probe's ROOT position, with
     * the L-080 item-binder machinery serving body-interior {@code item} references through the
     * raw AST parent chain exactly as the real recursion would); since the #496 arm the seats
     * CLAIM and the lower leg reads {@code adaptLambdaOp}'s own verdict first. The spelling
     * {@code lambdaVisit.<family>.<binder>.<typing>.<lower>[.guardTrip]}:
     * <ul>
     *   <li><b>family</b> is {@code extract}/{@code filter} — the seat itself;</li>
     *   <li><b>binder</b> is {@code implicitBare} (the grammar's implicitInlineFunction — no
     *       brackets at all; read via {@code isImplicit()} FIRST, because the
     *       {@code ImplicitVariableRule} derived-state pass injects a SYNTHETIC {@code item}
     *       parameter into every bare lambda, so paramNames alone would mis-read the class as
     *       named — the probe cut-1's own catch) / {@code named} (genuinely declared closure
     *       parameters) / {@code implicitBracket} (bracketed, zero declared parameters): the
     *       adapter's L-080 machinery binds only implicit-or-paramless lambdas
     *       ({@code filterExtractSourceOfArmBinder}'s own gate), so a named binder is a
     *       DIFFERENT teach and the leg must read separately;</li>
     *   <li><b>typing</b> reads the fixed-point cache exactly as the leaf emitter's type gates
     *       would ({@code getInferredType} — MISSING sentinel ⇒ {@code typeMissing}, the #491
     *       convention);</li>
     *   <li><b>lower</b> is the ROOT {@code adapter.adapt} verdict FIRST (the drift-proof read —
     *       since the #496 arm {@code adaptLambdaOp}'s own D1 recursion decides, the claim-root
     *       ctor-body admission included, so the cut-1 {@code blocked.ctorSlot.body} class reads
     *       {@code lowers} post-teach and the ctorSlot faces stand as the admission's regression
     *       belts), else the piece walk in recursion order — {@code blocked.recv.<Family>} /
     *       {@code blocked.body.<Family>} name the first declining piece by its node family
     *       ({@code nullSlot} the parse-robustness belts), {@code blocked.ctorSlot.<piece>} /
     *       {@code blocked.condCtorSlot.<piece>} the POSITION-DIVERGENT classes (the piece
     *       lowers at the probe's root but declines at the arm's child position: a direct
     *       {@link RConstructorExpr} receiver — the #494 root-only arm; a direct
     *       {@link RConditionalExpr} piece with a direct constructor slot — the #495 claim-ROOT
     *       admission one level down; deeper nestings decline at the probe root too, so the
     *       divergence is provably one level), the {@code multiParamBinder} belt, and the
     *       contract-impossible {@code blocked.rootOnly} residue;</li>
     *   <li>the {@code .guardTrip} suffix on {@code lowers} marks a fully-lowerable node whose
     *       subtree trips {@link #subtreeTripsPostPinCoercion} — sizing the #494-class
     *       guard-exemption question on THIS cluster with receipts before any arm (the #495
     *       run-zero convention).</li>
     * </ul>
     * Bumped at the visit seats themselves so Σ facets ≡ each family's WHOLE visit population per
     * cell BY CONSTRUCTION (the #489 one-walk law); gated on {@link #blockerProbeEnabled} — the
     * classification (two root re-adapts + the guard scan per visit, the #476 blocker-probe cost
     * class) runs only where the census computes, zero standing-render cost, and the standing
     * receipts stay byte-identical probe-off.
     */
    private final Map<String, Integer> lambdaVisitFacets = new TreeMap<>();

    /**
     * The #496 binder-gate cross-read (the monster wave's leg-1 second channel): the adapterGap
     * blocker mass the SAME lambda machinery gates, attributed by the exact widening lever that
     * would move each event — the read that DECIDED whether leg 1's teach also unblocks the gate
     * mass (the verdict: largely NOT — {@code attrChain.noLambda} 2,150 has no enclosing lambda
     * at all, every {@code onlyElement} receiver is a nav, and the named-binder faces are their
     * own future levers; the teach itself confirmed it — the big gate faces stood unmoved).
     * Hooked at the minimal-blocker walk where the
     * #478/#479 shape witnesses already hook (node-occurrence unit — one bump per minimal blocker
     * carrying a matched gate token), keyed {@code binderGate.<gate>.<face>}:
     * <ul>
     *   <li><b>gate</b> = {@code bareAttr} (the flat {@code noFilterExtractBinder} face — the
     *       bare-attr item-nav arm's binder gate, 2,254 at the #495 census) / {@code attrChain}
     *       (the composed {@code attributeChain.noFilterExtractBinder} face, ~66) /
     *       {@code itemBound} (the {@code itemNotFilterExtractBound} item-receiver face, ~121) /
     *       {@code syntheticItem} (the {@code receiverSyntheticItem} implicit-root face — the
     *       #479-witnessed class whose binder slice this read sizes) / {@code onlyElement} (the
     *       {@code opOnlyElement} operator face — a knock-on, not a binder gate; HISTORICAL since
     *       the #498 teach: the reason token no longer mints, so the face reads zero on a
     *       post-#498 probe — kept as the drift belt);</li>
     *   <li><b>face</b> for the four binder gates = the nearest enclosing inline function's
     *       parent family + binder spelling ({@code <ParentFamily>.<named|implicitBare|implicitBracket>}
     *       — mirroring the arm walk {@code nearestInlineFunctionBeforeSwitch}: {@code switchCut}
     *       when a switch intervenes, {@code noLambda} when no lambda encloses the node,
     *       {@code orphanLambda} the parse belt) — a {@code RThenExpr.implicitBare} face is mass a
     *       THEN-binder widening would move, a {@code RExtractExpr.named} face mass a NAMED-binder
     *       widening would move, and so on;</li>
     *   <li><b>face</b> for {@code onlyElement} = {@code arg.<Family>} — the operator's
     *       RECEIVER family (an {@code arg.RFilterExpr} face was the {@code filter [...]
     *       only-element} slice whose receiver leg-1's teach lowered, leaving the operator itself
     *       the one standing gate — until the #498 teach claimed the operator too).</li>
     * </ul>
     * Gated on {@link #blockerProbeEnabled} via its single call site (the probe's own walk);
     * diagnostic-only, the #471 OBS-4 census-run cost envelope.
     */
    private final Map<String, Integer> binderGateCrossFacets = new TreeMap<>();

    /**
     * The #496 L-111 root census (the monster wave's leg 2): the {@code inputFeatureNav} seat's
     * shape read — the 4,690 disguised {@code <input> -> <feature>} {@link REnumValueRef} roots
     * (the #495 census), refined IN PLACE at the seat ({@link #visitEnumValueRef}'s disguised
     * branch). The cut-1 read sized the conversion (adapterLowers 4,411 of 4,690); since the
     * leg-2 arm the {@code adapterLowers.inputHead} slice CLAIMS through the QUIET path (the
     * lowered receipt {@link #inputFeatureNavLoweredCount} ≡ that facet per cell BY THE SAME
     * VERDICT — both read {@code adapter.adapt}) and the aliasHead / guardTrip / blocked faces
     * are the delegated residue on the byte-proven relabel
     * ({@link #inputFeatureNavDrivenCount}). The spelling
     * {@code inputNavRoot.<meta>.<typing>.<lower>[.guardTrip]} (the lower leg's lowering faces
     * carry a recvKind split — {@code adapterLowers.<inputHead|aliasHead|otherShape>} — the
     * conversion's own shape gate restated, so the claimed slice and the alias-head residue read
     * apart):
     * <ul>
     *   <li><b>meta</b> is {@code meta}/{@code plain} — the resolved input feature's meta-ness
     *       (the adapter's meta gate is a named decline; the leg cross-cuts it for the census's
     *       meta-ness read);</li>
     *   <li><b>typing</b> reads the fixed-point cache exactly as the leaf emitter's type gates
     *       would (MISSING sentinel ⇒ {@code typeMissing});</li>
     *   <li><b>lower</b> is the ROOT {@code adapter.adapt} verdict (the drift-proof read — the
     *       #478 arm's own gates decide, never a probe restatement): {@code adapterLowers} when
     *       the root lowers TODAY, else {@code blocked.<token>} carrying the adapter's own
     *       {@code declineReason} spelling (the composed {@code inputFeatureNav.<gate>} faces);</li>
     *   <li>the {@code .guardTrip} suffix marks an {@code adapterLowers} root whose subtree trips
     *       {@link #subtreeTripsPostPinCoercion} — these decline at the conversion's claim gate
     *       (the arm's render is the emitter's OWN FieldAccess emission, NOT the literal legacy
     *       fallback, so the #494/#495 exemption identity does NOT extend to it and the guard
     *       stays live; the suffix sizes that honest residue on run zero).</li>
     * </ul>
     * The population is single-hop BY THE SEAT'S OWN PRE-CONDITION (resolvedInputFeature present,
     * resolvedAttributeChain absent — the depth leg is constant 1; multi-hop siblings live at the
     * L-112 seat, a different receipt). Σ facets ≡ the disguised branch's WHOLE population per
     * cell BY CONSTRUCTION (bumped before the claim/relabel split — since the conversion that is
     * lowered {@link #inputFeatureNavLoweredCount} + relabeled {@link #inputFeatureNavDrivenCount},
     * 3,717 + 973 at the #496 read); gated on {@link #blockerProbeEnabled} — one root adapt + the
     * mirror walk + the guard scan per event, probe runs only, the standing receipts
     * byte-identical probe-off.
     */
    private final Map<String, Integer> inputNavRootFacets = new TreeMap<>();

    /**
     * The #497 metaNav census (the conversion cluster's decode, seat 1 of 3): the L-109d
     * meta-navigation DELEGATED seat's shape read — 2,962 meta-annotated feature navigations at
     * the pre-#499-arm decode, refined IN PLACE at the belt; since the #499 meta arm the belt
     * keeps only the 886-event recvBlocked residue (a lowering receiver lowers the whole node to
     * {@link com.regnosys.rosetta.ir.expr.IRMetaAccess} and renders through the root-site
     * {@link MetaNavRenderer}, counted {@link #metaNavLoweredCount}). Pre-arm the adapter
     * declined every meta feature (the with-meta {@code FieldWithMetaX} retype + coercion is the
     * L-029 gm-aware class — kept legacy-side on BOTH routes; the #497 conversion question this
     * census sized BEFORE the design).
     * The spelling {@code metaNavRoot.<metaKind>.<typing>.<recv>}:
     * <ul>
     *   <li><b>metaKind</b> = the resolved feature's {@code metadata} annotation qualifiers,
     *       sorted and {@code +}-joined ({@code reference}/{@code scheme}/{@code id}/… ;
     *       {@code bare} = a qualifier-less metadata annotation) — the render-class discriminant
     *       (each kind has its own legacy witness/getter family);</li>
     *   <li><b>typing</b> reads the fixed-point cache exactly as the emitter's type gates
     *       would;</li>
     *   <li><b>recv</b> = {@code recvNull} (an elided/implicit receiver) /
     *       {@code recvLowers.<leaf|chain>} (the receiver adapts today — leaf = a non-nav
     *       receiver, chain = the receiver is itself a navigation) /
     *       {@code recvBlocked.<Family>.<reasonHead>[.cascade]} — the receiver leg a native
     *       arm's D1 recursion would need; since #499 refined IN PLACE (the #497 cut-2
     *       ladder-leg precedent) with the receiver's OWN first failing gate (the adapter's
     *       declineReason HEAD segment) and the {@code .cascade} marker for a receiver that is
     *       itself a meta chain lowering bottom-up under the planned recursive meta admission
     *       ({@link #metaChainWouldLower}) — the residue-vs-recursive-slice split the #499
     *       conversion sizing needs.</li>
     * </ul>
     * Σ facets ≡ the seat's relabel count per cell BY CONSTRUCTION (bumped in the same belt);
     * gated on {@link #blockerProbeEnabled} — probe runs only, the standing receipts
     * byte-identical probe-off.
     */
    private final Map<String, Integer> metaNavRootFacets = new TreeMap<>();

    /**
     * #514: the leafEmitter-GAP census (probe-only, the #491 facet-first law applied to the
     * EMITTER decline site): the site's count line carried no faces, so a claim that reaches
     * {@link #emitViaLeafEmitter} and declines records its RAW family × the lowered ROOT
     * kind here — the decomposition the probe loop reads when a teach moves declines
     * DOWNSTREAM (adapter-claimed, emitter-declined — the #478 cache-boundary class made
     * visible at this seat).
     */
    private final Map<String, Integer> leafEmitterGapFacets = new TreeMap<>();

    /**
     * The #531 W-facet belt census (the LAST decline's decode — the #381 aliasOutputBuilderNav
     * belt, cdm5-f {@code aliasResolution=6}): the {@link #tryEmitAlias} seat's call population
     * read BY EXIT, each token minted AT the exit it names (the census BY-CALL law — the verdict
     * IS the arm's own predicate result, never a re-derived compare). The spellings:
     * {@code wAlias.enclosingNull.declined} (no enclosing function — the standing legacy
     * empty-arg-list channel) · {@code wAlias.escape.declined} (the #420 static-import escape
     * belt) · {@code wAlias.w.<fate>.out:<outputName>.collide:<0|1>.inputs:<n>} (the
     * {@link #aliasTakesOutputBuilderNavRender} verdict, carrying the THREE render facts the
     * legacy W-arm composes from — the output name, the dep-collision bit, the input count; the
     * census names the render shape so the teach-vs-permanent pricing reads the exact compose)
     * · {@code wAlias.bare.lowered.inputs:<n>} (the standing bare-form render). Σ tokens ≡ the
     * seat's whole call population per cell BY CONSTRUCTION (the seat-complete census law). The
     * operand-channel mirror ({@link #buildAliasResolver}'s W-decline) mints
     * {@code wAliasOperand.w.declined} — its absence from a census read IS the proof the
     * operand-channel W-mass is zero (the refuse-to-emit contract). Collection gates on
     * {@link #blockerProbeEnabled} (the standing receipts stay byte-identical probe-off);
     * the D11 reader prints on its own non-empty guard OUTSIDE the {@code blockerProbed > 0}
     * gate — the closed-board census channel: at adapterGap-probed ZERO the standing censuses
     * suppress whole, and a DECLINE-seat census must not ride that gate.
     */
    private final Map<String, Integer> wAliasGateFacets = new TreeMap<>();

    /**
     * The #497 implicitAttrNav census (the conversion cluster's decode, seat 2 of 3): the L-113
     * bare-attribute DELEGATED seat's shape read — 2,773 bare non-meta attribute references at
     * the PRE-ARM decode ({@link #implicitAttrNavDrivenCount}; drr RULE dominant); the #497
     * orphan-input arm then claims the 2,210-event dominant face UPSTREAM of this belt, so the
     * shipped belt population — and this census's Σ — reads the 563-event residue (the in-lambda
     * item-nav faces + the noLambda tail). Refined IN PLACE at the belt. The spelling
     * {@code implicitAttrRoot.<reason>.<binderFace>.<ladder>}: <b>reason</b> is the adapter's own
     * {@code declineReason} token on the event (the drift-proof read — the L-113 belt fires only
     * on adapter-declined refs, so the mirror names the exact gate a native arm must open;
     * {@code mirrorLowersOnReread} is the contract-impossible drift belt), <b>binderFace</b>
     * is the nearest-binder context ({@link #nearestBinderFace} — {@code noLambda} = the
     * no-lambda class, an in-lambda face names the binder family the binding walk would
     * consult), and <b>ladder</b> (the cut-2 refinement) is the LEGACY render branch the
     * conversion must byte-match ({@code itemNav}/{@code condInstance}/{@code varPath} — see
     * {@link #implicitAttrRootFacet}). Σ facets ≡ the seat's relabel count per cell BY
     * CONSTRUCTION; gated on {@link #blockerProbeEnabled}.
     */
    private final Map<String, Integer> implicitAttrRootFacets = new TreeMap<>();

    /**
     * The #497 implicitAttrNav census's first-sample witness (the {@code navGateFirstSamples}
     * pattern): the FIRST sampled site per {@link #implicitAttrRootFacets} bucket —
     * {@code in=<enclosing> attr=<name> declaring=<declaring>} — the decode's qualitative face
     * (which real corpus shapes the dominant buckets hold; the varPath class's compiling-render
     * question is answered by WHERE the events live — the cut-4 read of exactly this channel
     * decoded the orphan-input class). {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> implicitAttrFirstSamples = new TreeMap<>();

    /**
     * The #497 L-112 census (the conversion cluster's decode, seat 3 of 3): the input-feature
     * RECEIVER navigation DELEGATED seat's shape read — the 913 multi-hop
     * {@code (<input> -> <f>) -> <g>} feature calls whose receiver chain bottoms in a disguised
     * ref ({@link #inputFeatureNavReceiverDrivenCount}), refined IN PLACE at the belt. Since the
     * #478 arm the disguised BASE lowers at nested positions, so the 913 residue declines at
     * DEEPER gates — the spelling {@code inputNavReceiverRoot.<typing>.<reason>} reads the
     * adapter's own {@code declineReason} on the WHOLE call (the composed
     * {@code receiverBase.}/{@code featureUnresolved.} descents name the exact remaining gate;
     * {@code mirrorLowersOnReread} the drift belt). Σ facets ≡ the seat's relabel count per cell
     * BY CONSTRUCTION; gated on {@link #blockerProbeEnabled}.
     */
    private final Map<String, Integer> inputNavReceiverRootFacets = new TreeMap<>();

    /**
     * The #498 leg-A census (the composed A+B cluster's decode, gate 1 of 2): the deferred
     * {@code only-element} operator gate ({@code RListOpExpr:opOnlyElement} — the top sole gate,
     * 2,331 claim-unit at the #497 SOT) read at each minimal-blocker NODE (node-occurrence unit,
     * the #478 convention — Σ over this channel counts blocker nodes, not claims; the claim-unit
     * projection reads off the sole-reason receipt). The spelling
     * {@code onlyElemGate.<pos>.<typing>.<recvVerdict>}: <b>pos</b> = {@code atRoot} (the blocker
     * IS the claim root — the teach lowers it at its own visit seat) /
     * {@code interior.<RootFamily>} (the blocker sits under a different claim root, keyed by that
     * root's family because the post-teach outcome splits by its RENDER MODE — a whole-oracle root
     * covers its interior, a natively-composed root declines at the null-renderer frame-slot gate;
     * see {@link #positionToken}); <b>typing</b> = {@code typed}/{@code typeMissing} (the emitter
     * type-gate projection on the collapse node itself); <b>recvVerdict</b> =
     * {@code recvLowers.<Family>} (the receiver re-adapts at the probe — expected UNIVERSAL here,
     * since a minimal blocker's children lower by the walk's own construction; the read PROVES it
     * rather than assumes it) / {@code recvBlocked.<Family>} (the contradiction face — triaged,
     * never absorbed) / {@code recvNull}. Gated by its seat ({@link #collectMinimalBlockers} runs
     * probe-only); probe-off receipts byte-identical. POST-ARM STATE: the same-PR #498 teach then
     * claimed the operator ({@code adaptListOp} maps ONLY_ELEMENT, the mirror no longer mints
     * {@code opOnlyElement}), so this channel documents the PRE-ARM decode and reads {@code none}
     * on a post-arm probe — kept as the drift belt (a re-minted token would read here first).
     */
    private final Map<String, Integer> onlyElemGateFacets = new TreeMap<>();

    /**
     * The #498 leg-A first-sample witness ({@link #onlyElemGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket — the qualitative face (the #497 retro: a
     * first-sample witness is the cheapest decisive instrument, added EARLY). {@link TreeMap} for
     * deterministic receipt order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> onlyElemGateFirstSamples = new TreeMap<>();

    /**
     * The #498 leg-B census (the composed A+B cluster's decode, gate 2 of 2): the
     * {@code RSymbolReference:calleeNotFunction} gate (the #2 sole gate, 2,045 claim-unit at the
     * #497 SOT — that conversion wave's own re-land: an args-present reference whose resolved
     * symbol is NOT an {@link RFunction}) read at each minimal-blocker NODE (node-occurrence
     * unit, the #478 convention). The spelling
     * {@code calleeGate.<pos>.<symbolKind>.<typing>.<enclosing>.<argN>}: <b>pos</b> =
     * {@code atRoot}/{@code interior.<RootFamily>} (the {@link #onlyElemGateFacets} position leg
     * — the same teach-outcome split, see {@link #positionToken}); <b>symbolKind</b> = the
     * resolved symbol's class simple name ({@code unresolved} is the drift belt — the
     * {@code calleeUnresolved} gate precedes this one in {@code reasonForApply}, so a null symbol
     * cannot mint the token); <b>typing</b> = {@code typed}/{@code typeMissing} on the call node;
     * <b>enclosing</b> = {@code inFunction}/{@code inRule}/{@code orphan}; <b>argN</b> = the
     * literal arg count. The read names the arm (what ARE the callees?) — decode-first, the
     * #494–#497 cadence. Probe-only (same seat gating as {@link #onlyElemGateFacets}). POST-ARM
     * STATE: the same-PR #498 rule leg then claimed the RRule-callee class whole (the census read
     * it 100% RRule), so the token — and this channel — now covers only the residual
     * neither-function-nor-rule symbol class (zero live at the census); the channel documents the
     * PRE-ARM decode and stands as the drift belt.
     */
    private final Map<String, Integer> calleeGateFacets = new TreeMap<>();

    /**
     * The #498 leg-B first-sample witness ({@link #calleeGateFacets} buckets): one
     * {@code in=<enclosing> callee=<name>} sample per bucket. {@link TreeMap} for deterministic
     * receipt order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> calleeGateFirstSamples = new TreeMap<>();

    /**
     * The #499 leg-A census (the composed A+B admission cluster's decode, gate 1 of 2): the
     * three {@code <pos>:IRListOp} consumer faces the #498 ONLY_ELEMENT teach exposed
     * ({@code receiver:IRListOp} — the top new sole gate, 1,014 claim-unit at the #498 SOT;
     * {@code operand:IRListOp}; {@code arg:IRListOp}) read at each minimal-blocker NODE
     * (node-occurrence unit, the #478 convention). The spelling
     * {@code listOpGate.<seat>.<pos>.<opKinds>.<rt>.<typing>.<family>}: <b>seat</b> =
     * {@code recv}/{@code operand}/{@code arg} (from the reason spelling); <b>pos</b> =
     * {@code atRoot}/{@code interior.<RootFamily>} (the {@link #positionToken} teach-outcome
     * split); <b>opKinds</b> = the lowered {@link com.regnosys.rosetta.ir.expr.IRListOp} child
     * kind(s), each prefixed by its child index and suffixed (cut-2) by the collapse's OWN
     * child-shape token ({@code c0:onlyElement/param}; {@code +}-joined when several children
     * lower to list ops — the consumer-side admission must serve each; the child-shape leg names
     * the L-051 param/nav byte-equivalence slice apart from the facet-conditional shapes —
     * {@link #listOpChildShapeToken});
     * <b>rt</b> = {@code rt}/{@code rtMissing} — the FIRST lowered list-op child's own
     * result-type presence (the {@code receiverTypeName}/lambda-naming projection for the
     * planned nav admission); <b>typing</b> = {@code typed}/{@code typeMissing} on the consumer
     * node; <b>family</b> = the consumer node's class simple name (the drift belt —
     * {@code recv} expects {@code RFeatureCall} only). Probe-only (the
     * {@link #collectMinimalBlockers} seat gating); probe-off receipts byte-identical.
     */
    private final Map<String, Integer> listOpGateFacets = new TreeMap<>();

    /**
     * The #499 leg-A first-sample witness ({@link #listOpGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket (the #497 retro's cheapest decisive
     * instrument, added EARLY). {@link TreeMap} for deterministic receipt order; bounded by the
     * bucket key space. Probe-only.
     */
    private final Map<String, String> listOpGateFirstSamples = new TreeMap<>();

    /**
     * The #499 leg-B census (the composed A+B admission cluster's decode, gate 2 of 2): the
     * {@code metaFeature} adapter gate (1,194 claim-unit at the #498 SOT — the INTERIOR
     * meta-navigation population; a claim-ROOT meta nav is claimed by the L-109d belt and never
     * probed, so {@code atRoot} is the drift belt here) read at each minimal-blocker NODE
     * (node-occurrence unit). The spelling
     * {@code metaFeatureGate.<pos>.<kind>.<card>.<typing>.<recvVerdict>}: <b>pos</b> =
     * {@code atRoot}/{@code interior.<RootFamily>} (the {@link #positionToken} teach-outcome
     * split — an oracle-rendered root covers a LOWERED interior and FLIPS, a natively-composed
     * root re-declines at its own admission gates); <b>kind</b> = the
     * {@link #metaQualifierToken} qualifier(s); <b>card</b> = {@code single}/{@code multi} (the
     * feature's declared cardinality — the {@code MapperS}/{@code MapperC} wrapper split);
     * <b>typing</b> = {@code typed}/{@code typeMissing} on the node; <b>recvVerdict</b> =
     * {@code recvLowers.<chain|leaf>} (the receiver lowers TODAY — the immediately-convertible
     * slice) / {@code recvCascade} (the receiver is itself a meta chain that would lower under
     * the planned recursive meta admission — {@link #metaChainWouldLower}) /
     * {@code recvBlocked.<Class>} / {@code recvNull}. Probe-only (the
     * {@link #collectMinimalBlockers} seat gating); probe-off receipts byte-identical.
     */
    private final Map<String, Integer> metaFeatureGateFacets = new TreeMap<>();

    /**
     * The #499 leg-B first-sample witness ({@link #metaFeatureGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> metaFeatureGateFirstSamples = new TreeMap<>();

    /**
     * The #500 arm-A census (the composed A+B cluster's decode, arm A): the FOUR
     * {@code <seat>:IRMetaAccess} consumer faces the #499 metaNav conversion exposed
     * ({@code receiver:IRMetaAccess} — the top new sole face, 596 claim-unit at the #499 SOT;
     * {@code operand:IRMetaAccess}; {@code arg:IRMetaAccess};
     * {@code attributeChain.receiver:IRMetaAccess}) read at each minimal-blocker NODE
     * (node-occurrence unit, the #478 convention). The spelling
     * {@code metaAccessGate.<seat>.<pos>.<metaKids>.<low#>.<typing>.<family>}: <b>seat</b> =
     * {@code recv}/{@code operand}/{@code arg}/{@code chainRecv} (from the reason spelling);
     * <b>pos</b> = {@code atRoot}/{@code interior.<RootFamily>} (the {@link #positionToken}
     * teach-outcome split); <b>metaKids</b> = the lowered
     * {@link com.regnosys.rosetta.ir.expr.IRMetaAccess} child(ren), each prefixed by its child
     * index and carrying the qualifier name(s) plus the meta hop's OWN receiver shape
     * ({@code c0:scheme/fieldAccess}; {@code +}-joined when several children lower to meta hops
     * — the consumer-side admission must serve each; the receiver-shape leg sizes the recursive
     * meta-chain slice apart from the flat hops — {@link #metaAccessChildToken}); <b>low#</b> =
     * {@code low<k>} — the count of nearest-descendant subtrees that LOWER today (the #499
     * absorption-projection law's composition leg: each lowering child under a taught consumer
     * CONSOLIDATES its currently-separate driven root, so the driven/denominator projection
     * reads off this distribution, never off the sole count alone); <b>typing</b> =
     * {@code typed}/{@code typeMissing} on the consumer node; <b>family</b> = the consumer
     * node's class simple name. Probe-only (the {@link #collectMinimalBlockers} seat gating);
     * probe-off receipts byte-identical.
     */
    private final Map<String, Integer> metaAccessGateFacets = new TreeMap<>();

    /**
     * The #500 arm-A first-sample witness ({@link #metaAccessGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> metaAccessGateFirstSamples = new TreeMap<>();

    /**
     * The #500 arm-B censuses (the untargeted four-family sweep's decode): one probe-only facet
     * channel per family riding its visit seat (the #489 one-walk law — Σ facets ≡ the family's
     * whole untargeted visit population by construction, the census's own conservation gate
     * against {@link #untargetedVisitsByFamily}). Grammars:
     * <ul>
     *   <li>{@code thenVisit.<spine>.<binder>.<typing>.<argVerdict>.<bodyVerdict>} —
     *       <b>spine</b> = {@code n1}/{@code n2}/{@code n3plus} ({@link RThenExpr} nodes on the
     *       left-associative {@code argument()} spine including this one — the {@code thenArg}
     *       hoist count the legacy render opens); <b>binder</b> = the {@link #lambdaVisitFacet}
     *       binder split ({@code implicitBare}/{@code named}/{@code implicitBracket}/
     *       {@code noBody}); <b>argVerdict</b>/<b>bodyVerdict</b> = {@code argLowers}/
     *       {@code bodyLowers} or {@code arg:<head>}/{@code body:<head>} — the adapter's verdict
     *       on the piped source and the body EXPRESSION probed separately (a
     *       {@code body:closureParam} head names the binder-scope slice a THEN arm must serve,
     *       the #496 lambda decode's exact analogue).</li>
     *   <li>{@code conversionVisit.<kind>.<argFam>.<argVerdict>.<typing>} — <b>kind</b> = the
     *       {@link com.regnosys.rosetta.ast.enums.ConversionKind} constant lower-camel with the
     *       {@code to} prefix ({@code toEnum} splits on target-enum presence —
     *       {@code toEnumUnresolved} the unresolved face); the {@code to-string} form is the
     *       separate {@code RToStringExpr} family (already targeted) and never reaches this
     *       seat.</li>
     *   <li>{@code listLitVisit.<n>.<verdict>.<typing>.<parent>} — <b>n</b> = {@code n0}/…/
     *       {@code n3plus} element-count bucket; <b>verdict</b> = {@code lowers} (the adapter's
     *       {@code IRListConstruct} arm serves the whole literal today — the adapter-ready
     *       slice) / {@code declined.<head>}; <b>parent</b> = the consumer context's class
     *       simple name (which legacy seat renders the root literal).</li>
     *   <li>{@code onlyExistsVisit.<n>.<roots>.<typing>} — <b>n</b> = the path-element count
     *       bucket; <b>roots</b> = the per-element root forms ({@code item}/{@code recvExpr}/
     *       {@code named}), {@code +}-joined, the first three indexed then {@code +more}.</li>
     * </ul>
     * All four probe-only at their seats; probe-off receipts byte-identical.
     */
    private final Map<String, Integer> thenVisitFacets = new TreeMap<>();

    /** The #500 arm-B thenVisit first-sample witness — one {@code in=<enclosing>} per bucket. Probe-only. */
    private final Map<String, String> thenVisitFirstSamples = new TreeMap<>();

    /** The #500 arm-B conversion channel — grammar at {@link #thenVisitFacets}. Probe-only. */
    private final Map<String, Integer> conversionVisitFacets = new TreeMap<>();

    /** The #500 arm-B conversionVisit first-sample witness. Probe-only. */
    private final Map<String, String> conversionVisitFirstSamples = new TreeMap<>();

    /** The #500 arm-B list-literal channel — grammar at {@link #thenVisitFacets}. Probe-only. */
    private final Map<String, Integer> listLitVisitFacets = new TreeMap<>();

    /** The #500 arm-B listLitVisit first-sample witness. Probe-only. */
    private final Map<String, String> listLitVisitFirstSamples = new TreeMap<>();

    /** The #500 arm-B only-exists channel — grammar at {@link #thenVisitFacets}. Probe-only. */
    private final Map<String, Integer> onlyExistsVisitFacets = new TreeMap<>();

    /** The #500 arm-B onlyExistsVisit first-sample witness. Probe-only. */
    private final Map<String, String> onlyExistsVisitFirstSamples = new TreeMap<>();

    /**
     * The #501 arm-A census (the composed A+C cluster's decode, arm A): the FOUR shallow-kind
     * consumer faces the #500 family sweep exposed ({@code operand:IROnlyExists} — the top new
     * sole face, 246 claim-unit at the #500 SOT, 100% RLogicalExpr; {@code operand:IRPipe} 12;
     * {@code arg:IRConversion} 28; {@code arg:IRPipe} 6) read at each minimal-blocker NODE
     * (node-occurrence unit, the #478 convention). The spelling
     * {@code shallowGate.<seat>.<kind>[.<callee>].<pos>.<kids>.<low#>.<typing>.<family>}:
     * <b>seat</b> = {@code operand}/{@code arg} (from the reason spelling); <b>kind</b> =
     * {@code onlyExists}/{@code pipe}/{@code conversion} (the shallow child's IR class);
     * <b>callee</b> = {@code calleeRule}/{@code calleeFunction}/{@code calleeOther} on the arg
     * seat only (a RULE-apply root is already whole-served by the #498 RuleApplyRenderer — the
     * teach outcome differs by callee kind); <b>pos</b> = {@code atRoot}/{@code
     * interior.<RootFamily>} (the {@link #positionToken} teach-outcome split — the families
     * needing an oracle-root dispatch leg); <b>kids</b> = the child index(es) whose re-adapt
     * lowers to the reason's shallow kind ({@code c0}, {@code +}-joined; {@code noShallowChild}
     * the drift face); <b>low#</b> = the count of nearest-descendant subtrees that LOWER today
     * (the #499 absorption-projection composition leg); <b>typing</b> = {@code typed}/{@code
     * typeMissing} on the consumer node; <b>family</b> = the consumer node's class simple name.
     * Probe-only (the {@link #collectMinimalBlockers} seat gating); probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> shallowGateFacets = new TreeMap<>();

    /**
     * The #501 arm-A first-sample witness ({@link #shallowGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> shallowGateFirstSamples = new TreeMap<>();

    /**
     * The #501 arm-C census (the composed A+C cluster's decode, arm C — the REnumValueRef
     * chain-drain cluster): the FIVE disguised-chain faces
     * ({@code attributeChain.headSymbolNav} 1,312 sole at the #500 SOT ·
     * {@code attributeChain.ruleInputChain} 1,047 · {@code attributeChain.sourceElementUnresolved}
     * 736 · {@code attributeChain.equivalentMetaAccess} 197 · the
     * {@code attrOutsideFunction.equivalentMetaAccess} bare-nav sibling 337 — the SAME re-range
     * mechanism at the #499 seat) read at each minimal-blocker NODE (node-occurrence unit). The
     * spelling {@code chainDrainGate.<face>.<pos>.<ctx>.<detail>.<typing>}: <b>face</b> =
     * {@code headSym}/{@code ruleChain}/{@code srcElem}/{@code chainMeta}/{@code bareMeta};
     * <b>pos</b> = the {@link #positionToken} teach-outcome split; <b>ctx</b> =
     * {@code inFunction}/{@code inRule}/{@code orphan} ({@link #enclosingContextToken} — which
     * render machinery hosts the site); <b>detail</b> per face:
     * <ul>
     *   <li>{@code headSym} — {@code <SymClass>.<metaLeaf|plainLeaf|leafUnbound>.<verdict>}: the
     *       resolved symbol's class (legacy's RRule/RFunction receiver-nav branches — the #480
     *       MF-1 precedence), the chain leaf's meta split (the RFunction branch's own gate axis),
     *       and the adapter's verdict on the EXACT legacy-synthesized receiver-nav equivalent
     *       ({@code synthesizeRuleReceiverNavigation}/{@code synthesizeFunctionReceiverNavigation}
     *       — {@code lowers:<IRKind>} sizes the DEEP route; {@code declines:<gate>} names the
     *       blocking admission);</li>
     *   <li>{@code ruleChain} — {@code <topLevel|inLambda>.<verdict>}: the
     *       {@code buildImplicitInputReceiver} receiver split (the #485 law: top-level roots at
     *       the ORPHAN synthetic {@code input} — the #497 arm's own class — while in-lambda
     *       roots at the synthetic item) and the adapter's verdict on legacy's OWN
     *       {@code synthesizeImplicitInputChain} equivalent (the input-rooted 3-hop chain);</li>
     *   <li>{@code srcElem} — the filter/extract binder SOURCE's shape
     *       ({@code <SrcClass>[.<sub>]} — for a feature-call source the declared-type token of
     *       its resolved last hop; for a symbol source the symbol class + declared-type token;
     *       for a pipe/extract source the body's class one level down), naming which
     *       {@code sourceElementDataType} leg a widening must teach;</li>
     *   <li>{@code chainMeta} — {@code <metaLeaf|metaHead|metaBoth>.<verdict>}: which hop
     *       carries the meta annotation (the #499/#500 shape-gate exits) and the adapter's
     *       verdict on legacy's own {@code synthesizeImplicitItemChain} equivalent
     *       ({@code lowers:IRMetaAccess}/{@code lowers:FieldAccess} — the re-range teach's
     *       lowered-kind split, deciding the render seat);</li>
     *   <li>{@code bareMeta} — {@code <qualifier>.<verdict>}: the bare attr's meta qualifier
     *       ({@link #metaQualifierToken}) and the verdict on the single-hop synthetic-item
     *       equivalent (the {@code implicitItemNavEquivalent} mirror built locally).</li>
     * </ul>
     * <b>typing</b> = {@code typed}/{@code typeMissing} on the node. Probe-only (the
     * {@link #collectMinimalBlockers} seat gating); probe-off receipts byte-identical.
     */
    private final Map<String, Integer> chainDrainGateFacets = new TreeMap<>();

    /**
     * The #501 arm-C first-sample witness ({@link #chainDrainGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> chainDrainGateFirstSamples = new TreeMap<>();

    /**
     * The #502 arm-A census (the composed A+B cluster's decode, arm A — the RSymbolReference
     * drain): the SIX top sole faces of the family at the #501 SOT
     * ({@code symbolNotAttribute} 680 · {@code symbolUnresolved.synthetic.aliasName.bodyTyped}
     * 532 · {@code symbolUnresolved.synthetic.closureParam} 510 · {@code argItem} 458 ·
     * {@code attrOutsideFunction.sourceElementUnresolved} 425 · {@code calleeMetaOutput} 406)
     * read at each minimal-blocker NODE (node-occurrence unit, the #478 convention). The
     * spelling {@code symbolDrainGate.<face>.<detail>.<pos>.<ctx>.<typing>}: <b>face/detail</b>
     * per reason —
     * <ul>
     *   <li>{@code symNotAttr.<SymClass>} — the resolved symbol's class simple name (the flat
     *       token carries NO decode today: WHAT non-attribute symbol classes reach the exit
     *       names the teach);</li>
     *   <li>{@code aliasHead.<verdict>.<bodyType>} — the legacy re-entrant synthesis head whose
     *       name matches an enclosing shortcut (legacy {@code isAliasReference}'s NAME-MATCH
     *       fallback renders the alias invocation — the adapter alias arm's own documented
     *       deferred edge): the adapter's verdict on the LINKER-BOUND equivalent (a fresh
     *       reference with the shortcut resolved — exactly the form the alias arm claims) and
     *       the shortcut BODY's cached-type state ({@code bodyTyped}/{@code bodyMissing}/
     *       {@code bodyMeta} — the type-read lever for the MISSING-typed synthetic seat);</li>
     *   <li>{@code closureParam.<binderOp>.<src>} — the registering binder (the nearest
     *       enclosing inline fn whose declared param names carry the head name; its PARENT op
     *       class names the render family) and the binder SOURCE's element-form verdicts
     *       ({@code aw<0|1>tw<0|1>} — the allowlist × typing-walk cross-read: the param's value
     *       is the source's element by the same Cat-8 law an adapter-boundary type would
     *       apply);</li>
     *   <li>{@code argItem.<itemKind>.<binderCtx>.<calleeKind>} — the lowered item argument's
     *       kind ({@code user}/{@code syn}), the raw item's binding context
     *       ({@link #syntheticItemContext} for a synthetic implicit; {@code userBound}/
     *       {@code userUnbound} for an explicit {@code item}), and the callee kind
     *       ({@code fn}/{@code rule}/{@code other});</li>
     *   <li>{@code calleeMetaOut.<qualifier>.<args>} — the callee OUTPUT's meta qualifier
     *       ({@link #metaQualifierToken} — which annotation family the render must thread) and
     *       the argument decode ({@code argsLow<k>of<n>} — how many args adapt today; the
     *       all-or-nothing admission's composition leg);</li>
     *   <li>{@code srcElemBare.<shape>.<aw>} — the filter/extract binder SOURCE's DEEP shape
     *       ({@link #deepSourceShapeToken} — recursing through then/extract BODIES to name the
     *       true proof bottom, the flat {@code thenPipe}/{@code extractPipe} stop tokens'
     *       refinement) and the allowlist cross-verdict ({@code aw0}/{@code aw1} — an
     *       allowlist-provable source whose typing walk nulls names the walk-unification
     *       lever directly).</li>
     * </ul>
     * <b>pos</b> = the {@link #positionToken} teach-outcome split; <b>ctx</b> =
     * {@link #enclosingContextToken}; <b>typing</b> = {@code typed}/{@code typeMissing} on the
     * node. Probe-only (the {@link #collectMinimalBlockers} seat gating); probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> symbolDrainGateFacets = new TreeMap<>();

    /**
     * The #502 arm-A first-sample witness ({@link #symbolDrainGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> symbolDrainGateFirstSamples = new TreeMap<>();

    /**
     * The #502 arm-B census (the composed A+B cluster's decode, arm B — the
     * {@code receiverSyntheticItem} drain, 1,219 sole at the #501 SOT = the #479 retype's
     * still-declining residue): read at each minimal-blocker NODE (node-occurrence unit). The
     * #479 witness already names context × verdict ({@code synItemNav:*} — unprovableSource 734
     * dominant · thenBody 265 · metaSourced 81 · switchCase 71 · named 43 · otherBinder 25 at
     * the #501 SOT); THIS census adds the two design channels the arm needs: the DEEP source
     * shape (through then/extract bodies to the true proof bottom) and the THREE-WALK
     * cross-verdict. The spelling {@code synItemGate.<context>.<detail>.<pos>}: <b>context</b> =
     * {@link #syntheticItemContext}; <b>detail</b> per context —
     * <ul>
     *   <li>{@code inLambdaImplicit} (the filter/extract binder class) — the pipe-resolved
     *       binder source's {@link #deepSourceShapeToken} + the verdict triple
     *       {@code aw<0|1>tw<0|1>ct<Ok|Meta|Miss>} ({@code aw} = the #479 allowlist
     *       {@code isProvablyNonMetaElementSource}; {@code tw} = the #480 typing walk
     *       {@code sourceElementDataType}; {@code ct} = the resolved source's cached engine
     *       type) — the walk-unification map: {@code aw0tw1} rows name allowlist legs the
     *       typing walk already proves, {@code aw1tw0} rows the converse;</li>
     *   <li>{@code thenBody} — the then ARGUMENT's shape + verdicts + the argument's cached
     *       CARDINALITY ({@code card1}/{@code cardN}/{@code cardMiss} — a then-body item takes
     *       the WHOLE argument value, so the planned binder leg's retype stamp is the
     *       argument's own cardinality, not the Cat-8 SINGLE);</li>
     *   <li>{@code switchCase}/{@code namedExtract*}/{@code inLambdaNamed}/{@code otherBinder}/
     *       {@code otherTopLevel} — context-only (the honest-decline classes; no design
     *       lever rides them).</li>
     * </ul>
     * <b>pos</b> = the {@link #positionToken} teach-outcome split. Probe-only (the
     * {@link #collectMinimalBlockers} seat gating); probe-off receipts byte-identical.
     */
    private final Map<String, Integer> synItemGateFacets = new TreeMap<>();

    /**
     * The #502 arm-B first-sample witness ({@link #synItemGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> synItemGateFirstSamples = new TreeMap<>();

    /**
     * The #503 arm-A census (the composed A+B cluster's decode, arm A — the REqualityExpr
     * cluster): the three cluster faces at the #502 SOT ({@code allAnyModifier} 1,166 sole
     * across BOTH the equality and comparison families · {@code operand:IRToString} 440 ·
     * {@code operandAlias.clean.enumSibling} 338), read at each minimal-blocker NODE
     * (node-occurrence unit, the #478 convention). The spelling
     * {@code equalityGate.<face>.<detail>.<pos>.<ctx>.<typing>}: <b>face/detail</b> per reason —
     * <ul>
     *   <li>{@code allAny.<fam>.<mod>.<op>.low<k>} — the family ({@code eq}/{@code cmp} — both
     *       mirrors mint the SAME reason token), the {@code ALL}/{@code ANY} modifier, the
     *       operator, and the count of operands that LOWER today (the absorption-composition
     *       leg — legacy renders the modifier as the {@code CardinalityOperator} argument of
     *       the SAME operator family, so the teach outcome splits by operand lowerability);</li>
     *   <li>{@code toStr.<side>.<sibling>.low<k>} — which operand lowers to the to-string
     *       ({@code left}/{@code right}), the SIBLING's lowered class (the pair the equality
     *       root would oracle-serve), and the lowering count;</li>
     *   <li>{@code enumSib.<bodyType>.<enumResolved>} — the alias-operand/enum-sibling pair:
     *       whether the alias SHORTCUT's body cached type names an enumeration (the
     *       qualifier-through-body lever the #491 admission deferred on) and whether the
     *       sibling enum value's enumeration resolves.</li>
     * </ul>
     * <b>pos</b> = the {@link #positionToken} teach-outcome split; <b>ctx</b> =
     * {@link #enclosingContextToken}; <b>typing</b> = {@code typed}/{@code typeMissing} on the
     * node. Probe-only (the {@link #collectMinimalBlockers} seat gating); probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> equalityGateFacets = new TreeMap<>();

    /**
     * The #503 arm-A first-sample witness ({@link #equalityGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> equalityGateFirstSamples = new TreeMap<>();

    /**
     * The #503 arm-B census (the composed A+B cluster's decode, arm B — the RFeatureCall
     * drain): the two feature-resolution faces at the #502 SOT
     * ({@code featureUnresolved.nonSymbolReceiver} 834 · {@code featureUnresolved.headUnresolved}
     * 255 — the #502-exposed parent-gate class: nav features over now-lowered closure-param
     * heads the linker never typed), read at each minimal-blocker NODE (node-occurrence unit).
     * The spelling {@code featureDrainGate.<face>.<detail>.<pos>.<ctx>.<typing>}: <b>face/detail</b>
     * per reason —
     * <ul>
     *   <li>{@code nonSymRecv.<RecvClass>.<adaptVerdict>.<typeVerdict>.<member>} — the compound
     *       receiver's class, its ADAPT verdict ({@code lowers:<kind>}/{@code declines}), the
     *       receiver's own cached-type verdict ({@code ctData:<Type>}/{@code ctOther}/
     *       {@code ctMiss} — the engine may have typed what the linker could not), and the
     *       BY-NAME member lookup on that data type ({@code memberHit}/{@code memberMiss} —
     *       the resolve-at-the-adapter lever);</li>
     *   <li>{@code headUnres.<headKind>.<elemVerdict>.<member>} — the unresolved head's decode
     *       ({@code closureParamName} when an enclosing binder declares the name /
     *       {@code aliasName} / {@code otherName}), the head's PROVEN element type through the
     *       #502 walks ({@code twData:<Type>}/{@code twNull} — for a closure-param head the
     *       registering binder's source derivation), and the member lookup on it.</li>
     * </ul>
     * <b>pos</b>/<b>ctx</b>/<b>typing</b> as above. Probe-only; probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> featureDrainGateFacets = new TreeMap<>();

    /**
     * The #503 arm-B first-sample witness ({@link #featureDrainGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> featureDrainGateFirstSamples = new TreeMap<>();

    /**
     * The #504 arm-A census (the composed A+B+C cluster's decode, arm A — the boolean-operand
     * cluster): the three cluster faces at the #503 SOT ({@code operandBareBooleanCall} 829
     * sole [RLogicalExpr — the {@code ComparisonResult.ofNullSafe} coercion family] ·
     * {@code operand:IRPointFreeApply} 347 eq + 332 logical [+ 142 existence, the same
     * point-free operand class at its third seat]), read at each minimal-blocker NODE
     * (node-occurrence unit, the #478 convention). The spelling
     * {@code booleanOpGate.<face>.<fam>.<side>.<callee>.<out>.<card>.<sib>.<pos>.<ctx>.<typing>}:
     * <b>face</b> = {@code bareCall} (the IRApply operand legacy coerces
     * {@code ComparisonResult.ofNullSafe(<call>)}) / {@code pf} (the IRPointFreeApply operand —
     * the #492 point-free kind at the operand frontier); <b>fam</b> = {@code log}/{@code eq}/
     * {@code exist} (the three operand seats); <b>side</b> = which operand carries the named
     * lowering (the reason mirrors check left before right); <b>callee</b> = {@code fn}/
     * {@code rule}/{@code otherCallee} for the bareCall raw callee ({@code pfCallee} on the pf
     * face — the kind is its own callee witness); <b>out</b> = the lowered operand's engine
     * type ({@code out:<name>}/{@code outMiss} — boolean-ness is the coercion family's own
     * fact); <b>card</b> = the lowered operand's cardinality ({@code card1}/{@code cardN} —
     * legacy's coercion wraps MapperS and MapperC alike); <b>sib</b> = the SIBLING operand's
     * lowered class ({@code sib:<Class>}/{@code sibDeclines}/{@code sibNone} — a sibling that
     * also names a coercion face is the both-coerced signature; the existence seat has none).
     * <b>pos</b> = the {@link #positionToken} teach-outcome split; <b>ctx</b> =
     * {@link #enclosingContextToken}; <b>typing</b> = {@code typed}/{@code typeMissing} on the
     * node. Probe-only (the {@link #collectMinimalBlockers} seat gating); probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> booleanOpGateFacets = new TreeMap<>();

    /**
     * The #504 arm-A first-sample witness ({@link #booleanOpGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> booleanOpGateFirstSamples = new TreeMap<>();

    /**
     * The #504 arm-C census (the composed A+B+C cluster's decode, arm C — the RSymbolReference
     * qualified-name residue): the three residue faces at the #503 SOT
     * ({@code symbolNotAttribute} 301 sole [the #502 decode's parked non-attribute bind
     * classes — RBody/RDataType/RCorpus/RAnnotation/RSegmentDef] · {@code argEmpty} 239 ·
     * {@code symbolUnresolved.synthetic.absent} 232 [the cdm-twin 116/116 signature]), read at
     * each minimal-blocker NODE (node-occurrence unit). The spelling
     * {@code qualNameGate.<face>.<pos>.<ctx>.<typing>}: <b>face</b> per reason —
     * <ul>
     *   <li>{@code symNot.<SymClass>.<seat>} — the resolved symbol's class plus the SEAT decode
     *       ({@link #qualNameSeatToken}): which legacy recovery arm would render the bare name —
     *       {@code argOf.<callee>.<param>} (the {@code tryBareEnumArg} mirror: the callee's
     *       positional param's declared enum scanned for the name — {@code enumHit:<Enum>} is
     *       the re-qualification lever), {@code eqSib.<verdict>} (the sibling-typed enum scan),
     *       {@code inLambda.<verdict>} (the {@code synthesizeImplicitItemBareNav} mirror: the
     *       binder source's element type scanned for a same-named attribute), or
     *       {@code topLevel.<Parent>} (the residue);</li>
     *   <li>{@code argEmpty.<callee>.<param>.low<k>of<n>} — the first EMPTY-lowering argument's
     *       positional param (declared type + cardinality — the render-lever fact) and the
     *       args-lowering count (the all-or-nothing composition leg);</li>
     *   <li>{@code synAbsent.name:<name>.<Parent>.<Grandparent>} — the unresolved synthetic
     *       head's NAME (the #486 ladder proved no root element carries it — the name class
     *       names the legacy machinery that renders it) and the enclosing two parent classes
     *       (the synthesis-seat shape).</li>
     * </ul>
     * <b>pos</b> = the {@link #positionToken} teach-outcome split; <b>ctx</b> =
     * {@link #enclosingContextToken}; <b>typing</b> = {@code typed}/{@code typeMissing} on the
     * node. Probe-only (the {@link #collectMinimalBlockers} seat gating); probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> qualNameGateFacets = new TreeMap<>();

    /**
     * The #504 arm-C first-sample witness ({@link #qualNameGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> qualNameGateFirstSamples = new TreeMap<>();

    /**
     * The #505 arm-A census (the composed A+B+C cluster's decode, arm A — the REnumValueRef
     * chain-bucket drain): the four undecoded chain-bucket faces at the #504 SOT
     * ({@code attributeChain.closureParamHead} 441 sole · {@code choiceOption} 239 ·
     * {@code attributeChain.noFilterExtractBinder} 202 · {@code attributeChain
     * .equivalentSynItemSpine} 67 — {@code sourceElementUnresolved}/{@code equivalentMetaAccess}
     * ride the standing #501 chainDrainGate census), read at each minimal-blocker NODE
     * (node-occurrence unit). The spelling {@code enumSeatGate.<face>.<pos>.<ctx>.<typing>}:
     * <b>face</b> per reason —
     * <ul>
     *   <li>{@code chainCp.<elem>.<leaf>} — the closure-param head's element derivation (the
     *       #503 arm-B2 walks restated at the CHAIN shape: the registering binder's SOURCE
     *       derives {@code twData:<Type>}/{@code twNull}; {@code noBinder} = the registration
     *       walk missed — drift) and the VALUE-segment lookup on it ({@code leafHit}/
     *       {@code leafMetaHit}/{@code leafMiss}/{@code noLookup} — {@code leafHit} is the
     *       teach-shaped slice: {@code IRClosureParam} head + by-name leaf);</li>
     *   <li>{@code chainChoice.<head>.<opt>.<member>} — the head segment's model-type
     *       resolution ({@code head:<Class>} via {@code GeneratorModel.resolveTypeByName} —
     *       the #503 arm-B1 projection lever's head-side analogue; {@code headNone} = no
     *       global type carries the name), the resolved option's node class, and the
     *       option-on-projection membership when the head is a choice
     *       ({@code optOnHead}/{@code optOffHead}/{@code noProjection});</li>
     *   <li>{@code chainNoBinder.<absence>.<head>} — WHY the binder walk returned null
     *       ({@code switchCrossed}/{@code noInlineFn}/{@code paramBinder} — the
     *       {@code filterExtractSourceOfArmBinder} exits in order) and the head segment's
     *       model-type resolution (the disguised static-selection shapes read
     *       {@code head:<Class>} here);</li>
     *   <li>{@code chainSpine.<form>} — the legacy-synthesized equivalent's lowered form
     *       ({@code faSynNav} = FieldAccess over the #504-minted {@link IRSynItemNav} — the
     *       arm's excluded spine, the teach-shaped slice; {@code faFaSyn} = the plain
     *       accepted spine [drift — the face implies exclusion]; {@code other:<Class>}/
     *       {@code eqDeclines}/{@code eqNull} the residue).</li>
     * </ul>
     * <b>pos</b> = the {@link #positionToken} teach-outcome split; <b>ctx</b> =
     * {@link #enclosingContextToken}; <b>typing</b> = {@code typed}/{@code typeMissing} on the
     * node. Probe-only (the {@link #collectMinimalBlockers} seat gating); probe-off receipts
     * byte-identical.
     */
    private final Map<String, Integer> enumSeatGateFacets = new TreeMap<>();

    /**
     * The #505 arm-A first-sample witness ({@link #enumSeatGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. {@link TreeMap} for deterministic receipt
     * order; bounded by the bucket key space. Probe-only.
     */
    private final Map<String, String> enumSeatGateFirstSamples = new TreeMap<>();

    /**
     * The #505 arm-A SEAT census (the visitEnumValueRef seat-conversion sizing — the L-109c
     * chain-bucket exclusion routes disguised ROOT visits straight to legacy UNCOUNTED, so
     * this channel reads the excluded population BEFORE any conversion): fires at the
     * disguised residue return (the branch below the L-111 conversion — every visit that
     * joins neither the driven stream nor the delegated relabel). The spelling
     * {@code enumSeatRoot.<channel>.<typing>.<verdict>}: <b>channel</b> = the resolution
     * channel in {@code ExpressionTypeComputer.computeEnumValueRef}'s own precedence
     * ({@code chain}/{@code choiceOpt}/{@code typeRestrict}/{@code headSym}/{@code noChannel}/
     * {@code valueNameFb}; {@code inputLeak} = the L-111 branch's class reached this seat —
     * drift; {@code genuineLeak} = the pre-gate screens genuine refs — contract-impossible);
     * <b>typing</b> = {@code typed}/{@code typeMissing}; <b>verdict</b> = the adapter's take
     * ({@code lowers.<shape>} — the conversion's claimable mass, shape-split like the
     * {@link #inputNavRootFacet} recvKind leg — or {@code blocked.<reasonHead2>}, the first
     * two decline-reason segments bounding the key space). Probe-only, teach-independent
     * (the #489 one-walk law: Σ facets ≡ the excluded-seat population by construction).
     */
    private final Map<String, Integer> enumSeatRootFacets = new TreeMap<>();

    /**
     * The #505 arm-A seat-census first-sample witness ({@link #enumSeatRootFacets} buckets):
     * one {@code in=<enclosing>} site sample per bucket. Probe-only.
     */
    private final Map<String, String> enumSeatRootFirstSamples = new TreeMap<>();

    /**
     * The #505 arm-B census (the composed A+B+C cluster's decode, arm B — the RSymbolReference
     * alias-rooted argument residue): the {@code argNav.alias.*} faces at the #504 SOT
     * ({@code hop1} 211 sole · {@code hop2} 108 — the #492-named follow-up: the alias-nav
     * teach lowers alias-rooted chains, and the ARG-side admission was declined until its own
     * decode — THIS census; {@code arg:IRListOp}/{@code srcElemBare} ride the standing
     * #499/#502 censuses), read at each minimal-blocker NODE (node-occurrence unit — the
     * blocker is the CALL whose arg lowered to the alias-rooted chain). The spelling
     * {@code argResidueGate.aliasArg.<tail>.<callee>.<param>.<body>.<pos>.<ctx>.<typing>}:
     * <b>tail</b> = the reason's own depth token ({@code hop1}/{@code hop2}/{@code deep}
     * [{@code .multi}]); <b>callee</b> = {@code fn}/{@code rule}/{@code otherCallee};
     * <b>param</b> = the FIRST alias-rooted argument's positional declared param
     * ({@code param:<Type>.<p1|pN>} — the render-lever fact: legacy's #360
     * evaluate-arg-neutral compile + the callee-param-driven unwrap) or {@code noParam}/
     * {@code argUnlocated}; <b>body</b> = the alias SHORTCUT body's cached type class
     * ({@code body:<Class>} — the #503 enumSib decode pattern; {@code bodyUnreadable}/
     * {@code bodyMiss}/{@code bodyNullType}). Probe-only.
     */
    private final Map<String, Integer> argResidueGateFacets = new TreeMap<>();

    /**
     * The #505 arm-B first-sample witness ({@link #argResidueGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. Probe-only.
     */
    private final Map<String, String> argResidueGateFirstSamples = new TreeMap<>();

    /**
     * The #505 arm-C census (the composed A+B+C cluster's decode, arm C — the DISPATCH-class
     * design census): the {@code symbolUnresolved.synthetic.absent} face (232 sole at the
     * #504 SOT — the #504 qualNameGate census DECODED the class to per-enum-value overload
     * bodies naming BASE-signature inputs; THIS census reads the teach-design facts), at each
     * minimal-blocker NODE (node-occurrence unit). The spelling
     * {@code dispatchGate.synAbsent.<variant>.<base>.<anno>.<pos>.<ctx>.<typing>}:
     * <b>variant</b> = the enclosing function's dispatch shape ({@code variant} =
     * {@code dispatch()} present — the scope-join class; {@code plainFn}/{@code noFn});
     * <b>base</b> = the base-declaration join ({@code baseInput:<Type>.<p1|pN>} — the name
     * resolves on {@code HandlerHelper.dispatchBaseOf}'s inputs, the scope-join lever with
     * its render-type fact; {@code baseNoInput}/{@code noBase}); <b>anno</b> = the enclosing
     * function's {@code [calculation]} annotation ({@code calc}/{@code plainAnno}/
     * {@code noAnno} — the native-render byte-risk axis the teach designs around,
     * oracle-first). Probe-only.
     */
    private final Map<String, Integer> dispatchGateFacets = new TreeMap<>();

    /**
     * The #505 arm-C first-sample witness ({@link #dispatchGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. Probe-only.
     */
    private final Map<String, String> dispatchGateFirstSamples = new TreeMap<>();

    /**
     * The #506 wave census (the walk-widening cluster's decode): the binder/element-derivation
     * faces of the ONE shared walk family — the five sole gates
     * {@code attributeChain.noFilterExtractBinder} (323) / {@code attrOutsideFunction
     * .sourceElementUnresolved} (221) / {@code attributeChain.closureParamHead.twNull} (204) /
     * {@code itemNotFilterExtractBound} (194) / {@code attributeChain.sourceElementUnresolved}
     * (128) + the adjacent {@code attrOutsideFunction.noFilterExtractBinder} (121), at each
     * minimal-blocker NODE (node-occurrence unit). Every face bottoms in the SAME three walks
     * ({@code filterExtractSourceOfArmBinder} / {@code sourceElementDataType} /
     * {@code closureParamElementType}), so the census reads the walk's stopping fact per face —
     * the #505 enumSeatGate {@code chainNoBinder} decode refined (its {@code bodyNotFilterExtract}
     * 320-row dominant class left the binder's PARENT OP undecoded) and the standing
     * srcElemBare/srcElem shape tokens refined with the OP-NAMING deep walk
     * ({@link #deepSourceOpShapeToken} — the flat {@code listOp} stop was 136 rows without the
     * op). The spelling {@code walkBindGate.<face>.<detail>.<pos>.<ctx>.<typing>}:
     * <ul>
     *   <li>{@code chainBnfe.<absence>} / {@code bareBnfe.<absence>} — the two
     *       noFilterExtractBinder seats ({@link #binderAbsenceFacet}): {@code switchCrossed} /
     *       {@code noInlineFn} / {@code named.<Op>} / {@code other.<Op>} name the walk's stop;
     *       the {@code thenBody.<aw×tw>.<resolution>} class carries the widening's viability
     *       cross-read — would the then-ARGUMENT's element derive
     *       ({@code sourceElementDataType}), and does the head(+leaf) resolve on it (the arm's
     *       own downstream ladder restated)?</li>
     *   <li>{@code cpTw.<binderOp>.<read>} — the closureParamHead.twNull face:
     *       the registering binder's parent op + the two-walk cross-verdict on its source
     *       ({@code aw0tw1.<leafV>} = the allowlist is the only gap and the value segment's
     *       own resolution viability; {@code aw<a>tw0.<deepOpShape>} = the walk itself stops,
     *       the op-named bottom).</li>
     *   <li>{@code itemBind.<binder>.<leafV>} — the USER-item receiver face: the item's ACTUAL
     *       binder ({@code thenBody.msrc<0|1>} with the meta-sourced analogue on the then
     *       argument / {@code named.<Op>} / {@code implicit.<Op>} / {@code noLambda} /
     *       {@code switchCrossed}) + the nav leaf's meta state (the receiver already lowered
     *       typed + non-meta by the mint's own gate order).</li>
     *   <li>{@code chainSrcDeep.<deepOpShape>.<verdicts>} / {@code bareSrcDeep.<deepOpShape>
     *       .<verdicts>} — the two sourceElementUnresolved seats: the op-named TRUE proof
     *       bottom of the binder source (fires ADDITIONALLY to the standing #501/#502
     *       chainDrainGate.srcElem / symbolDrainGate.srcElemBare rows, which keep their
     *       coarse tokens byte-unchanged — the movement-inertness anchors).</li>
     * </ul>
     * Probe-only; read-only against the tree (derivation walks + resolved-model lookups, no
     * adapts of live tree nodes beyond the standing stateless-adapter contract).
     */
    private final Map<String, Integer> walkBindGateFacets = new TreeMap<>();

    /**
     * The #506 census first-sample witness ({@link #walkBindGateFacets} buckets): one
     * {@code in=<enclosing>} site sample per bucket. Probe-only.
     */
    private final Map<String, String> walkBindGateFirstSamples = new TreeMap<>();

    /**
     * The #507 arm-A census ({@link #classifyDeepGateBlocker}): the {@code RDeepFeatureCall:
     * noAdaptArm} sole face (191 — the board lead; the family has NO adapt arm at all, so the
     * generic fallback token carries the whole class) read at each minimal-blocker NODE
     * (node-occurrence unit). The spelling {@code deepGate.<feat>.<recv>.<pos>.<ctx>.<typing>}:
     * {@code feat} = the linker's deep-feature resolution FACT ({@code featHit} /
     * {@code featMetaHit} — resolved and meta-annotated — / {@code featMiss});
     * {@code recv} = the receiver subtree's own adapter verdict ({@code recvLowers.<shape>} with
     * the {@link #enumSeatLoweredShapeToken} shape convention / {@code recvBlocked.<reasonHead>} /
     * {@code recvNull}) — the arm-sizing read: a featHit+recvLowers face is the provable adapt
     * arm's direct serve population. Probe-only; read-only against the tree.
     */
    private final Map<String, Integer> deepGateFacets = new TreeMap<>();

    /** The #507 arm-A first-sample witness ({@link #deepGateFacets} buckets). Probe-only. */
    private final Map<String, String> deepGateFirstSamples = new TreeMap<>();

    /**
     * The #507 arm-A ROOT-seat census ({@link #visitDeepFeatureCall}): the untargeted
     * RDeepFeatureCall visits (71 — the family's whole root population, invisible to the blocker
     * probe because the visit never attempts IR) read at the visit seat with the SAME
     * {@code <feat>.<recv>} grammar as {@link #deepGateFacets} plus the node's PARENT family
     * (the statement seat a root conversion would serve). Spelling
     * {@code deepVisit.<feat>.<recv>.<parent>.<typing>}. Probe-only.
     */
    private final Map<String, Integer> deepVisitFacets = new TreeMap<>();

    /** The #507 arm-A root-seat first-sample witness ({@link #deepVisitFacets}). Probe-only. */
    private final Map<String, String> deepVisitFirstSamples = new TreeMap<>();

    /**
     * The #507 arm-B census ({@link #classifyNoChanGateBlocker}): the {@code REnumValueRef:
     * noResolutionChannel} sole face (175 — no resolution channel bound at all: the #444
     * record-feature disguised navs the resolver deliberately clears WITHOUT binding, the #433
     * direct disguised meta reads, and the genuinely unresolved) read at each minimal-blocker
     * NODE (node-occurrence unit). The spelling
     * {@code noChanGate.<head>.<leaf>.<scope>.<pos>.<ctx>.<typing>}:
     * {@code head} = the head name's own scope resolution FACT with its declared-type kind
     * ({@code input.<tk>} / {@code output.<tk>} / {@code alias} / {@code dispatchInput.<tk>} /
     * {@code headMiss}; {@code <tk>} per {@link #attrTypeKindToken} — {@code rec:date} names the
     * record class); {@code leaf} = the value name's resolution on that head type
     * ({@code leafRecHit} — a record feature per the type's own feature list — /
     * {@code leafAttrHit} / {@code leafAttrMetaHit} / {@code leafMiss}); {@code scope} = the
     * enclosing function's dispatch face ({@code dispatch} / {@code plain}). Probe-only;
     * read-only against the tree (resolved-model lookups only).
     */
    private final Map<String, Integer> noChanGateFacets = new TreeMap<>();

    /** The #507 arm-B first-sample witness ({@link #noChanGateFacets} buckets). Probe-only. */
    private final Map<String, String> noChanGateFirstSamples = new TreeMap<>();

    /**
     * The #507 arm-B seat census ({@link #l111ResidueFacet}): the L-111 relabel seat's
     * non-emitter-served slice (pre-arm 422 = guardTrip 143 + blocked 279 at the #506 SOT —
     * the single-hop disguised-input-nav events the #496/#497 quiet conversion could not
     * lower; the #505 seat conversion's disclosed 1-hop faVar exclusion), read at the seat
     * itself (probe-gated; a clean adapterLowers event that the leaf emitter serves takes NO
     * row). The spelling {@code l111Residue.<face>}:
     * {@code served.<arm>.<shape>} — the LOWERABLE residue the #507 identity-serve leg now
     * counts lowered (the post-pin guard's own tripping arm, {@link PostPinArm#label()}, +
     * the lowered shape) — since #508 joined by {@code served.identityShape.<shape>}, the
     * guard-CLEAN lowerings outside the quiet claim's FieldAccess shapes (the arm-A1/A2
     * IRMetaAccess acceptances, which take the same identity serve by the seat's shape gate;
     * together {@code Σ served rows ≡ inputNavIdentityLowered} per cell);
     * {@code blocked.<reason>.<metaQual>} — the adapter-declined residue that keeps the
     * relabel (the adapter's own full decline token + the resolved input feature's
     * metadata-qualifier token, {@link #metaQualifierToken} — the pre-fix meta.unattributed
     * 119's sub-decode, named {@code inputFeatureNav.metaFeature} since the #507 mirror fix;
     * {@code Σ blocked rows ≡ the seat's delegated count} per cell — the cross-check against
     * the driven split's L-111 brace); {@code noWorkspace} — the contract-impossible belt
     * (the D11 run always carries a generator model; the standard census robustness token,
     * never a live face — a hit means drift). Probe-only.
     */
    private final Map<String, Integer> l111ResidueFacets = new TreeMap<>();

    /** The #507 arm-B seat-census first-sample witness ({@link #l111ResidueFacets}). Probe-only. */
    private final Map<String, String> l111ResidueFirstSamples = new TreeMap<>();

    /**
     * The #508 arm-A census ({@link #classifyMetaHopGateBlocker}): the composed meta-facet +
     * alias-nav cluster's five sole faces read at each minimal-blocker NODE (node-occurrence
     * unit) — the L-111 meta single-hop ({@code REnumValueRef:inputFeatureNav.metaFeature}, 96),
     * the alias-head meta-feature navs at BOTH seats ({@code REnumValueRef:inputFeatureNav
     * .featureUnresolved.aliasHead.bodyTyped.featureOnBodyMeta}, 99 · {@code RFeatureCall:
     * featureUnresolved.aliasHead.bodyTyped.featureOnBodyMeta}, 67), the alias-head PLAIN nav at
     * the parsed seat ({@code RFeatureCall:featureUnresolved.aliasHead.bodyTyped.featureOnBody},
     * 143 — the #492 REnumValueRef arm's missing RFeatureCall twin), and the meta-bearing
     * implicit-item chains ({@code REnumValueRef:attributeChain.equivalentMetaAccess}, 137).
     * The spelling {@code metaHopGate.<face>.<detail>.<pos>.<ctx>.<typing>}:
     * <ul>
     *   <li>{@code l111Meta.<recv>.<q:quals>} — the L-111 equivalent re-adapted (the arm's OWN
     *       {@code disguisedInputNavEquivalent} construction restated read-only): the lowered
     *       {@link IRMetaAccess}'s receiver class + the meta-qualifier list — the identity-serve
     *       widening's shape read ({@code adaptEmpty}/{@code notMeta:<K>} the drift belts);</li>
     *   <li>{@code aliasMeta.<seat>.<bind>.<bodyCard>.<q:quals>} — the alias-head meta-feature
     *       navs: seat {@code enr}/{@code fc}, the head's bind class ({@code bound} linker-bound /
     *       {@code collision} name-collision), the shortcut BODY's engine cardinality (the #492
     *       {@code getRuleBodyCardinality} channel the receiver retype consumes) and the meta
     *       feature's qualifier list — the meta-twin leg's own gates read;</li>
     *   <li>{@code aliasPlain.<bind>.<bodyCard>.<hopCard>.<hopTyped>} — the parsed-seat PLAIN
     *       alias nav (the B-pair face): the #492 arm's own admission axes at the RFeatureCall
     *       seat (feature resolves non-meta on the typed body; hop type presence = the arm's
     *       last gate);</li>
     *   <li>{@code chainMeta.<spine>.<hopMeta>.<q:quals>} — the meta-bearing implicit-item chain
     *       equivalent re-adapted (the arm's OWN {@code implicitItemChainEquivalent} construction
     *       restated): the FULL lowered spine {@code top:<K>.inner:<K>[.base:<K>]} that the
     *       2-link gate excluded ({@code adaptEmpty} when the equivalent declines), the
     *       head/leaf meta split ({@code metaLeaf}/{@code metaHead}/{@code metaBoth}) and the
     *       meta hop's qualifier list — the acceptance-widening's exact shape read.</li>
     * </ul>
     * Probe-only; read-only against the tree and adapter-only (stateless by its documented
     * contract) — the standing counters cannot move.
     */
    private final Map<String, Integer> metaHopGateFacets = new TreeMap<>();

    /** The #508 arm-A first-sample witness ({@link #metaHopGateFacets} buckets). Probe-only. */
    private final Map<String, String> metaHopGateFirstSamples = new TreeMap<>();

    /**
     * The #508 arm-B census ({@link #classifyMetaSrcGateBlocker}): the L-029 meta-sourced lambda
     * pair read at each minimal-blocker NODE (node-occurrence unit) — the closure-param-head
     * chains whose element walk declined ({@code REnumValueRef:attributeChain.closureParamHead
     * .twNull}, 118 — the #506-kept aw0tw1 meta-sourced-filter slice) and the meta-sourced
     * USER-item navs ({@code RFeatureCall:itemMetaSourced}, 67). The spelling
     * {@code metaSrcGate.<face>.<detail>.<pos>.<ctx>.<typing>}:
     * <ul>
     *   <li>{@code cpSrc.<srcShape>.<elem>.<leaf>} — the twNull residue: the registering
     *       binder-source's shape ({@code metaFc} a direct meta-annotated feature-call source /
     *       {@code plainFc} / {@code argsCall} / {@code alias} / {@code other:<K>} /
     *       {@code noBinder}), the census's OWN structural element read
     *       ({@code elemHit:<Type>} via {@link ExpressionToIRAdapter#sourceElementDataType} —
     *       the walk the arm's L-029 allowlist declines to run — / {@code elemMiss}), and the
     *       VALUE segment's resolution on it ({@code leafPlain}/{@code leafMeta}/{@code leafMiss}
     *       /{@code leafNoElem}) — the oracle-leaf mint's provability read;</li>
     *   <li>{@code itemSrc.<binder>.<srcShape>.<featClass>} — the itemMetaSourced residue: the
     *       binding lambda's op ({@code filter}/{@code extract}/{@code then}/{@code min}/
     *       {@code max}/{@code sort}), the binding source's shape (same spellings) and the
     *       accessed feature's OWN class ({@code featPlain}/{@code featMeta} — the hop the
     *       claim navigates) — the distinct-kind mint's shape read.</li>
     * </ul>
     * Probe-only; read-only against the tree (resolved-model lookups + the adapter's static
     * source walks) — the standing counters cannot move.
     */
    private final Map<String, Integer> metaSrcGateFacets = new TreeMap<>();

    /** The #508 arm-B first-sample witness ({@link #metaSrcGateFacets} buckets). Probe-only. */
    private final Map<String, String> metaSrcGateFirstSamples = new TreeMap<>();

    /**
     * The #508 arm-C census ({@link #classifySymNotGateBlocker}): the {@code RSymbolReference:
     * symbolNotAttribute} sole face (166) one key deeper than the #504 {@code qualNameGate}
     * seat decode — the STANDING #504 arms' own gates re-run in the REAL parent context, naming
     * WHY the taught recovery does not fire on the residue. The spelling
     * {@code symNotGate.<SymClass>.<seat>.<verdict>.<pos>.<ctx>.<typing>}:
     * {@code seat} = {@code argOf.<fnKind>}/{@code eqSib}/{@code other:<ParentClass>};
     * {@code verdict} at the ARG seat = the {@code requalifiedEnumArg} gate ladder re-run
     * ({@code argFires.<Enum>} — the arm WOULD mint in context, so the loss is the CONTAINING
     * claim's own routing (the parent apply's verdict appended: {@code .applyLowers} /
     * {@code .applyDeclines}) — / {@code classExcluded} / {@code noParam} / {@code paramNotEnum}
     * / {@code valueMiss:<Enum>}); at the EQ seat = the {@code requalifiedEnumComparand} ladder
     * ({@code eqFires.<Enum>.<eqLowers|eqDeclines>} / {@code classExcluded} / {@code sibUntyped}
     * / {@code sibNotEnum} / {@code valueMiss:<Enum>}). Probe-only; read-only against the tree
     * and adapter-only — the standing counters cannot move.
     */
    private final Map<String, Integer> symNotGateFacets = new TreeMap<>();

    /** The #508 arm-C first-sample witness ({@link #symNotGateFacets} buckets). Probe-only. */
    private final Map<String, String> symNotGateFirstSamples = new TreeMap<>();

    /**
     * The #509 arm-A census ({@link #classifyAliasIdGateBlocker}): the parsed-seat alias-nav
     * decline pair one key deeper — the guard-held plain rows ({@code RFeatureCall:
     * featureUnresolved.aliasHead.bodyTyped.featureOnBody}, 143 sole at the #508 SOT: the
     * #326-class slice the strong body allowlist declines to render natively — the
     * identity-serve leg's own sizing read) and the off-body residue ({@code …featureOffBody},
     * 56, cdm6-only at the #508 SOT). The spelling
     * {@code aliasIdGate.<face>.<pos>.<ctx>.<typing>}:
     * <ul>
     *   <li>{@code onBody.<guard>.b:<BodyClass>.<bodyCt>.<hop>} — the strong-guard verdict
     *       re-derived ({@code guardHeld} the live class / {@code guardPass} a DRIFT face —
     *       the #508 arm claims that slice), the shortcut BODY's node class + engine-type kind
     *       per {@link #inferredTypeKindToken} (the serve-shape read) and the resolved member's
     *       cardinality ({@code hopS}/{@code hopM});</li>
     *   <li>{@code offBody.<bodyCt>.<member>} — the body's engine-type kind and the feature's
     *       resolution on it per {@link #memberOnTypeToken} ({@code mRecHit} = the
     *       record-feature membership, the #507 29th-kind class at the alias seat);</li>
     *   <li>{@code <face>.contractMiss} — the head/shortcut lookup failed at census time
     *       (contract-impossible per the reason token; ranked, not silently dropped);</li>
     *   <li>{@code served.identityShape.<shape>} — the #509 arm-A1 SERVE rows (bumped at the
     *       belt leg, not the attribution walk): Σ served rows ≡
     *       {@link #aliasNavIdentityLoweredCount} per cell by construction (the OBS-4
     *       cross-check law — a serve path names its census face at birth).</li>
     * </ul>
     * Probe-only; read-only against the tree and adapter-only — the standing counters cannot
     * move (the served rows ride the serve leg's own claim, not a counter change).
     */
    private final Map<String, Integer> aliasIdGateFacets = new TreeMap<>();

    /** The #509 arm-A first-sample witness ({@link #aliasIdGateFacets} buckets). Probe-only. */
    private final Map<String, String> aliasIdGateFirstSamples = new TreeMap<>();

    /**
     * The #509 arm-B census ({@link #classifyHeadUnGateBlocker}): the {@code RFeatureCall:
     * featureUnresolved.headUnresolved} sole face (112 at the #508 SOT) decoded by the head's
     * own resolution channels re-run in the REAL context. The spelling
     * {@code headUnGate.<ladder>.<adaptV>.<ht>.<member>.<pos>.<ctx>.<typing>}: {@code ladder} =
     * the name ladder ({@code fnInput.<tk>}/{@code fnOutput.<tk>}/{@code dispatchInput.<tk>}
     * per {@link #attrTypeKindToken}, each with the ladder-CHANNEL member verdict appended
     * per {@link #memberOnDeclaredTypeToken} / {@code cpBind.<sym>.<proof>.<srcShape>.<elemV>
     * .<member>} — the #503 arm-B2 gate ladder re-run fact-by-fact (the head-symbol state,
     * the STRONG {@code isProvablyNonMetaElementSource} source proof, the binding source's
     * shape per {@link #bindingSourceShapeToken}, the census's own element read and the
     * feature's resolution on it) / {@code noMatch}); {@code adaptV} = the adapter's verdict
     * on the HEAD node itself ({@code headLowers:<IRKind>}/{@code headBlocked}); {@code ht} =
     * the head's engine-type kind per {@link #inferredTypeKindToken}; {@code member} = the
     * feature's resolution on it per {@link #memberOnTypeToken}. Probe-only; read-only
     * against the tree and adapter-only — the standing counters cannot move.
     */
    private final Map<String, Integer> headUnGateFacets = new TreeMap<>();

    /** The #509 arm-B first-sample witness ({@link #headUnGateFacets} buckets). Probe-only. */
    private final Map<String, String> headUnGateFirstSamples = new TreeMap<>();

    /**
     * The #509 arm-B census ({@link #classifyNsrGateBlocker}): the {@code RFeatureCall:
     * featureUnresolved.nonSymbolReceiver} sole face (111 at the #508 SOT) decoded by the
     * compound receiver's own channels. The spelling
     * {@code nsrGate.<RecvClass>.<recvAdapt>.<ct>.<member>.<pos>.<ctx>.<typing>}:
     * {@code recvAdapt} = the adapter's verdict on the receiver subtree
     * ({@code recvLowers:<IRKind>}/{@code recvBlocked}); {@code ct} = the receiver's
     * engine-type kind per {@link #inferredTypeKindToken}; {@code member} = the feature's
     * by-name resolution on it per {@link #memberOnTypeToken} (a {@code choice:} + hit spells
     * {@code mChoiceHit:*} — a DRIFT face, the #503 arm-B1 claims that slice); an
     * {@code RImplicitVariable} receiver appends {@code .<binderOp>.<srcShape>.<qual>} — the
     * binding lambda's op, the binding source's shape per {@link #bindingSourceShapeToken}
     * and the META-QUALIFIER verdict ({@code qHit:<name>} = the nav's feature name is a
     * metadata qualifier the source's terminal resolved feature carries — the {@code item ->
     * scheme} meta-leaf class / {@code qMiss}). Probe-only; read-only against the tree and
     * adapter-only — the standing counters cannot move.
     */
    private final Map<String, Integer> nsrGateFacets = new TreeMap<>();

    /** The #509 arm-B first-sample witness ({@link #nsrGateFacets} buckets). Probe-only. */
    private final Map<String, String> nsrGateFirstSamples = new TreeMap<>();

    /**
     * The #520 arm-A3 census buckets ({@code headOtherGate.<SymClass>.<member>.<pos>.<ctx>.
     * <typing>=count}) — the {@code featureUnresolved.headOther} face's own decode (20 sole
     * at the #519 SOT, 100% drr-FUNCTION; the face had NO standing walker decode — the #498
     * calleeGate / #509 nsrGate pattern extended to the third feature-resolution face).
     * {@code SymClass} = the head SYMBOL's class (the facet's own definition — a
     * non-attribute, non-shortcut symbol); {@code member} = the feature's by-name
     * resolution against the head's ENGINE type per {@link #memberOnTypeToken} (the
     * teach-routing fact: a typed head with a member hit is a by-name recovery candidate,
     * a {@code ctMiss} head needs the declared-channel walk probe2 would name). Probe-only;
     * read-only against the tree and adapter-only — the standing counters cannot move.
     */
    private final Map<String, Integer> headOtherGateFacets = new TreeMap<>();

    /** The #520 arm-A3 first-sample witness ({@link #headOtherGateFacets} buckets). Probe-only. */
    private final Map<String, String> headOtherGateFirstSamples = new TreeMap<>();

    /**
     * The #524 pre-arm census buckets ({@code iaGate.<face>.n:<name>.<legacyLeg>.<member>.
     * <adaptV>.<pos>.<ctx>.<typing>=count}) — the six in-lambda {@code symbolUnresolved}
     * faces the B-cluster menu names (the synthetic item-attr pair + the lambdaSrc compose;
     * 55 sole at the #523 SOT: iaMeta 18 · iaPlain 14 · lsuType 13 · synLgt 4 · lsuAlias 3 ·
     * lAbsent 3). {@code face} = the reason's short key; {@code n:<name>} = the unresolved
     * bare name (the by-name teach key); {@code legacyLeg} = which leg of legacy's OWN
     * bare-name ladder fires when re-run BY CALL ({@code ReferenceHandler
     * .synthesizeImplicitItemBareNav} — the #523 C2 by-call pattern at the BARE-symbol
     * seat): {@code lgFires} the resolved-feature synthesis / {@code lgMsf} the #348
     * metaPathShortForm synthesis (feature deliberately UNRESOLVED — scheme/reference over
     * a meta-wrapper item) / {@code lgNull} the ladder declines (the variable path);
     * {@code member} = the fired member's meta shape ({@code mMeta:<quals>} /
     * {@code mPlain} / {@code mNone}); {@code adaptV} = the adapter's verdict on the EXACT
     * synthesized equivalent, shape-named when it lowers ({@code av:<IRKind>} /
     * {@code declines:<token>} — the would-fire pre-sizing, the #479 law). Probe-only;
     * read-only against the tree and adapter-only — the standing counters cannot move.
     */
    private final Map<String, Integer> itemAttrGateFacets = new TreeMap<>();

    /** The #524 pre-arm census first-sample witness ({@link #itemAttrGateFacets} buckets). Probe-only. */
    private final Map<String, String> itemAttrGateFirstSamples = new TreeMap<>();

    /**
     * The #526 pre-arm census buckets ({@code neGate.<face>[.fails:<n>of<N>].<shape>.<adaptV>.
     * <pos>.<path>.<ctx>.<typing>=count}) — the child-subtree not-expressible faces the
     * B-cluster menu names (38 sole at the #525 SOT: RListLiteral elementNotExpressible 17 ·
     * RConditionalExpr thenNotExpressible 8 + elseNotExpressible 6 · RExtractExpr
     * bodyNotExpressible 7), plus the same-predicate sibling seats as sweep-boundary negatives
     * (condition / extract-filter receiver / count / to-string / conversion arg — every one
     * routes the SAME {@code slotNotExpressibleAtChild} verdict, expected zero). {@code face} =
     * the named slot's seat ({@code elem}/{@code cond}/{@code then}/{@code else}/
     * {@code body.extract}/{@code recv.count}/…; a list face appends {@code fails:<n>of<N>} —
     * the failing-element count over the element count); {@code shape} = the named slot's
     * position-divergence class read BY CALL against the adapter's own predicates (the #524
     * census BY-CALL law): {@code ctor.t:<Type>} (a direct constructor — the #494 root-only
     * admission's class) / {@code condCtorSlot.ic:<seat>.t:<Type>} (a conditional whose direct
     * slot is one — the #495 admission's class, the inner seat in the predicate's own
     * condition-then-else order) / {@code lamCtorBody.<fam>.t:<Type>} (an extract/filter whose
     * direct body is one — the #496 admission's class) / {@code rootDecline:<Family>} (the slot
     * declines at its own probe root — contract-impossible at a SOLE-reason blocker, the
     * multi-blocker belt) / {@code anomaly:<Family>} (no class claims it — contradicts the
     * reason token); {@code adaptV} = the adapter's ROOT re-adapt verdict on the named slot
     * ({@code av:<IRKind>} — the admission the slot lowers through / {@code avBlocked});
     * {@code path} = the raw parent-chain from the claim root down to the blocker
     * ({@link #neGatePathToken} — the GLUE-REACHABILITY read: a chain of admitted-slot steps is
     * exactly what a root-reachable recursive admission covers, an {@code x:<Family>} step names
     * the coverage breaker). Probe-only; read-only against the tree and adapter-only — the
     * standing counters cannot move.
     */
    private final Map<String, Integer> neGateFacets = new TreeMap<>();

    /** The #526 pre-arm census first-sample witness ({@link #neGateFacets} buckets). Probe-only. */
    private final Map<String, String> neGateFirstSamples = new TreeMap<>();

    /**
     * The #527 pre-arm census buckets ({@code argGapGate.<tail>.<callee>.<verdicts>.<scan>.
     * <pos>.<ctx>.<typing>=count}) — the ARG_NAV dict's residue faces (52 sole at the #526 SOT
     * over 13 spellings: the {@code argNav.typeGap.<root>.<depth>.g:<navT>_<paramT>} agreement
     * declines + the {@code argNav.typeMissing.param.deep[.multi]} hop-flag declines — the
     * #489/#514 designed residue, the C-menu class). {@code tail} = the face minus the
     * {@code argNav.} prefix (the gap class rides verbatim); {@code callee} = {@code fn}/
     * {@code rule}/{@code otherCallee}; {@code verdicts} = the per-NAV-arg legacy coercion
     * verdict sequence read BY CALL against legacy's own arm predicates (the #524 census
     * BY-CALL law — {@code ReferenceHandler.tryMetaDerefArg}'s numeric-arm preconditions over
     * the SAME oracles: {@code getGeneratorModel().getType(param)} →
     * {@code toJavaReferenceType} vs the arg's inferred-read Java class): {@code javaEq} (the
     * raw-pass equality precondition) / {@code coerceIntBD} / {@code coerceBigIntBD} (the #277/
     * #128 hoist arms — the render the recursion-free arg arm cannot express) / {@code rawNoArm}
     * (NO arm can fire — legacy passes the arg RAW, the meta arm being impossible here by the
     * argNav chains' meta-free-by-declaration construction) / {@code multiParam}/
     * {@code paramUnread}/{@code actualUnread} (legacy's own early raw-pass gates; the actual
     * side is the INFERRED-read channel only — the compiled-type channel needs a render, so a
     * stamped-walk-typed chain the fixed point never saw reads {@code actualUnread} here while
     * the FACE still spells its stamped pair, the disclosed channel split); {@code scan} = the
     * standing post-pin scan's verdict on the claim root ({@code scan:<armLabel>}/
     * {@code scanNone} — the serve-route fact: a tripping claim serves at the standing #523
     * claim-root leg once admitted). Probe-only; read-only against the tree and adapter-only —
     * the standing counters cannot move.
     */
    private final Map<String, Integer> argGapGateFacets = new TreeMap<>();

    /** The #527 pre-arm census first-sample witness ({@link #argGapGateFacets} buckets). Probe-only. */
    private final Map<String, String> argGapGateFirstSamples = new TreeMap<>();

    /**
     * The #528 pre-arm census buckets ({@code opResGate.<face>.f:<fam>.<detail>.<scan>.<pos>.
     * <ctx>.<typing>=count}) — the OPERAND/ARG residue cluster the F-menu names (14 sole at the
     * #527 SOT over five spellings: {@code operandBareBooleanNav} 4 · {@code operand:
     * IRPointFreeApply} 4 · {@code calleeMetaParam} 3 · {@code operandItem} 2 · {@code argItem}
     * 1). Each face is decoded against the ADAPTER's own gate predicates BY CALL (the #524
     * census BY-CALL law — {@link ExpressionToIRAdapter#producesComparisonResult} /
     * {@link ExpressionToIRAdapter#isComparisonOperand} /
     * {@link ExpressionToIRAdapter#isFilterExtractBoundItemOperand} /
     * {@link ExpressionToIRAdapter#isTypedUserItemArg} /
     * {@link ExpressionToIRAdapter#isMetaAnnotated}, each widened public at #528), so the
     * census reports the gate's OWN verdict rather than a re-derived approximation.
     *
     * <p>{@code face} = {@code logNav} / {@code pfOperand} / {@code itemOperand} /
     * {@code cMetaP} / {@code argItem}; {@code fam} = the consuming seat
     * ({@code log}/{@code cmp}/{@code eq}/{@code exist}/{@code arith}/{@code apply}) — the
     * operand faces enumerate EVERY seat that can spell them, so the seats carrying zero
     * carriers stand as sweep-boundary negatives (the {@code operandItem} spelling is minted by
     * three separate operand tokenizers, {@code operand:IRPointFreeApply} by two). The pf face
     * has a standing #504 channel ({@link #booleanOpGateFacets}) whose family dispatch covers
     * log/eq/exist ONLY — the surviving carriers sit at the COMPARISON seat, which is exactly
     * why that census reads {@code none} ×4 at the #527 SOT while the face persists; this
     * channel is seat-complete.
     *
     * <p>{@code detail} per face: {@code logNav} = {@code s:<L|R>} (the failing side in the
     * mirror's own left-then-right order, read off {@code producesComparisonResult}) +
     * {@code c:<card1|cardN>} + {@code bt:<TypeName|tMiss|tMeta|tNull>} (the nav's own result
     * type — what legacy's {@code ComparisonResult.ofNullSafe} coerces; the variants are
     * {@code opResTypeClass}'s own emitted values — the Copilot #528 R2 suppressed catch) +
     * {@code base:<Class>} (the receiver-walk base kind) + {@code sib:<Class>};
     * {@code pfOperand} = {@code s:} +
     * {@code out:<TypeName|tMiss|tMeta|tNull>} + {@code c:} + {@code sib:}; {@code itemOperand} =
     * {@code s:} + {@code k:<user|syn>} + {@code t:<TypeName|tMiss|tMeta|tNull>} + {@code c:} +
     * {@code b:<bindingClass>} ({@link ExpressionToIRAdapter#itemBindingCensusToken} — the
     * adapter's own binder predicates, {@code .metaSrc} when the L-029 belt reads the source
     * meta-suspect) + {@code argGate:<yes|no>} (whether the binder-UNBOUNDED ARG-seat gate
     * would admit this exact operand — the direct price of the #502-noted operand-seat
     * widening) + {@code sib:}; {@code cMetaP} = {@code <fn|rule|otherCallee>} +
     * {@code mp:<idx>q<quals>} (every meta-annotated input with its qualifier set — the class
     * legacy's own meta-arg machinery keys on) + {@code a:<LoweredClass|un>} (the per-argument
     * adapter verdicts — whether the call's arguments would clear their seats at all);
     * {@code argItem} = {@code i:<idx>} + {@code k:} + {@code t:} + {@code c:} + {@code b:} +
     * {@code p:<pSingle|pMulti|pNone>} (the receiving parameter's cardinality). {@code scan} =
     * the standing post-pin scan's verdict on the claim root ({@code scan:<armLabel>}/
     * {@code scanNone} — the serve-route fact). Probe-only; read-only against the tree and
     * adapter-only — the standing counters cannot move.
     */
    private final Map<String, Integer> opResGateFacets = new TreeMap<>();

    /** The #528 pre-arm census first-sample witness ({@link #opResGateFacets} buckets). Probe-only. */
    private final Map<String, String> opResGateFirstSamples = new TreeMap<>();

    /**
     * The #529 ingest-wall pricing census, channel 1 ({@link #classifyItemHeadGateBlocker}):
     * the {@code REnumValueRef:noResolutionChannel.headMiss.other} pool (38 at the #528 SOT —
     * 30 cdm6-f + 8 drr-f, the board's largest single face) re-read against the IMPLICIT-ITEM
     * channel the #507 record arm's function-scope ladder never searches. Tokens
     * {@code itemHeadGate.<ladder>.<pos>.<ctx>.<typing>} where {@code <ladder>} is
     * {@link ExpressionToIRAdapter#itemHeadCensusToken} — the #522/#524 bare-item ladder's own
     * gates BY CALL from the node's tree position with the HEAD name, then the LEAF ladder on
     * the resolved head attribute in legacy's member-first order ({@code recHit:<record>} /
     * {@code attrHit.<plain|meta>} / {@code qual:<name>} / {@code miss}). The token prices the
     * three fates per face: an implicit-item head teach (the resolvable classes), an
     * identity-serve mint, or the honest wall. Probe-only; read-only.
     */
    private final Map<String, Integer> itemHeadGateFacets = new TreeMap<>();

    /** The #529 channel-1 first-sample witness ({@link #itemHeadGateFacets} buckets). Probe-only. */
    private final Map<String, String> itemHeadGateFirstSamples = new TreeMap<>();

    /**
     * The #529 ingest-wall pricing census, channel 2 ({@link #classifyHeadAttrGateBlocker}):
     * the {@code RFeatureCall:featureUnresolved.headAttr.declMiss} pool (35 cdm6-f at the
     * #528 SOT) — a RESOLVED head attribute whose declared type is neither a record nor a
     * derivable data type. Tokens {@code headAttrGate.<head>.<pos>.<ctx>.<typing>} where
     * {@code <head>} is {@link ExpressionToIRAdapter#headAttrCensusToken} — the head's
     * remaining type class BY the facet's own reads ({@code unres:<writtenName>} names the
     * ingest wall's missing types; {@code basic:<name>}/{@code enumT:<name>}/
     * {@code choice:<name>}/{@code noCall} the rest) paired with the #512 qualifier verdict
     * on the feature name ({@code qual:hit:<name>}/{@code qual:offList}/{@code qual:noMeta}).
     * Probe-only; read-only.
     */
    private final Map<String, Integer> headAttrGateFacets = new TreeMap<>();

    /** The #529 channel-2 first-sample witness ({@link #headAttrGateFacets} buckets). Probe-only. */
    private final Map<String, String> headAttrGateFirstSamples = new TreeMap<>();

    /**
     * The #529 A-tail census, channel 3 ({@link #classifyEqImplGateBlocker}): the
     * {@code REqualityExpr:operand:IRImplicitAttrNav} frontier (2 cdm6-f — the #528 mint's
     * own disclosed frontier at the equality seat). Tokens {@code eqImplGate.s:<L|R>.sib:
     * <Class>.c:<card1|cardN>.<pos>.<ctx>.<typing>} — the failing side (the operand whose
     * re-adapt lowers the #528 kind), the sibling's lowered class and the operand
     * cardinality, all BY CALL through the adapter (the #528 opResGate operand-detail
     * pattern). The kind is already an oracle leaf kind-wide ({@code containsOracleLeaf}),
     * so the token prices the standard consumer-seat admission exactly. Probe-only;
     * read-only.
     */
    private final Map<String, Integer> eqImplGateFacets = new TreeMap<>();

    /** The #529 channel-3 first-sample witness ({@link #eqImplGateFacets} buckets). Probe-only. */
    private final Map<String, String> eqImplGateFirstSamples = new TreeMap<>();

    /**
     * The #529 A-tail census, channel 4 ({@link #classifyItemArgSrcGateBlocker}): the
     * {@code RSymbolReference:argItem} residue (1 cdm6-f at the #528 SOT — the un-admitted
     * UNTYPED item argument, {@code b:unbound.t:tMiss} on the standing opResGate row).
     * Tokens {@code itemArgSrcGate.i:<idx>.<src>.<pos>.<ctx>.<typing>} where {@code <src>}
     * is {@link ExpressionToIRAdapter#itemArgSrcCensusToken} on the item argument — the
     * nearest switch-case guard class and the switch SUBJECT's declaration channel BY the
     * adapter's own walks ({@code g:…} + {@code op:input.<metaFree|meta>}/{@code op:…}), the
     * provable facts a missing item type cannot carry. Probe-only; read-only.
     */
    private final Map<String, Integer> itemArgSrcGateFacets = new TreeMap<>();

    /** The #529 channel-4 first-sample witness ({@link #itemArgSrcGateFacets} buckets). Probe-only. */
    private final Map<String, String> itemArgSrcGateFirstSamples = new TreeMap<>();

    /**
     * The #530 endgame-residue census, channel 1 ({@link #classifyPipeCondGateBlocker}): the
     * {@code RSymbolReference:attrOutsideFunction.sourceElementUnresolved} family (4 drr-r at
     * the #529 SOT — the elided-pipe deep bottoms, {@code deep:RThenExpr.b:RExtractExpr
     * .b:RConditionalExpr}). Tokens {@code pipeCondGate.<read>.<pos>.<ctx>} where
     * {@code <read>} is {@link ExpressionToIRAdapter#pipeDeepCensusToken} — the arm's own
     * source derivation descended to the conditional with per-branch derivation verdicts,
     * the #487 join read and the identity-guard pre-read, all BY CALL. Probe-only;
     * read-only.
     */
    private final Map<String, Integer> pipeCondGateFacets = new TreeMap<>();

    /** The #530 channel-1 first-sample witness ({@link #pipeCondGateFacets} buckets). Probe-only. */
    private final Map<String, String> pipeCondGateFirstSamples = new TreeMap<>();

    /**
     * The #530 endgame-residue census, channel 2 ({@link #classifyNsrCtGateBlocker}): the
     * {@code RFeatureCall:featureUnresolved.nonSymbolReceiver} pool (19 at the #529 SOT)
     * re-read for the MISSING TYPE NAMES the standing nsrGate rows abstract as
     * {@code ctMiss}/{@code mMiss}. Tokens {@code nsrCtGate.<detail>.feat:<name>} where
     * {@code <detail>} names the receiver chain's FIRST typing break root-first
     * ({@code break:<feat>:<declVerdict>} — {@code refMiss:<writtenType>} a declared type
     * the model itself cannot resolve vs {@code ref:<writtenType>:<Class>} a
     * declared-resolvable type the engine's inference still misses — the fate fork), the
     * item-receiver terminal read ({@code item.term:<feat>:<writtenType>}), or the flat
     * receiver class. Model reads only (the nsrGate precedent). Probe-only; read-only.
     */
    private final Map<String, Integer> nsrCtGateFacets = new TreeMap<>();

    /** The #530 channel-2 first-sample witness ({@link #nsrCtGateFacets} buckets). Probe-only. */
    private final Map<String, String> nsrCtGateFirstSamples = new TreeMap<>();

    /**
     * The #530 endgame-residue census, channel 3 ({@link #classifyKvpGateBlocker}): the
     * {@code RSymbolReference:symbolNotAttribute} survivor family (8 at the #529 SOT — the
     * #522 arm's residue at the KVP/conditional seats). Tokens {@code kvpGate.<read>.<pos>
     * .<ctx>} where {@code <read>} is {@link ExpressionToIRAdapter#kvpCensusToken} — the
     * bound symbol's own name, the seat detail (the KVP key / the conditional branch) and
     * the arm's decline ladder re-run leg-by-leg BY CALL. Probe-only; read-only.
     */
    private final Map<String, Integer> kvpGateFacets = new TreeMap<>();

    /** The #530 channel-3 first-sample witness ({@link #kvpGateFacets} buckets). Probe-only. */
    private final Map<String, String> kvpGateFirstSamples = new TreeMap<>();

    /**
     * The #530 endgame-residue census, channel 4 ({@link #classifyMetaArgGateBlocker}): the
     * {@code RSymbolReference:arg:IRMetaParamRef} containment (4 at the #529 SOT — the #514
     * designed decline at the callArgs gate). Tokens {@code metaArgGate.callee:<name>
     * .arg<i>.meta:<quals>.val:<valueType>.param:<type>:<meta|plain>:<one|many>
     * .dir:<deref|other>.<pos>.<ctx>} — the caller-side meta param's declared qualifiers
     * and value type against the callee param's declared type/meta/cardinality, with the
     * legacy {@code tryMetaDerefArg} precondition restated as the {@code dir} verdict (a
     * meta-wrapped single-card arg at a plain param of the SAME value type = the hoist +
     * {@code getValue()} deref shape — the render proof's fate fork). Model reads + the
     * adapter's own meta reads ({@link ExpressionToIRAdapter#metaQualifierNames}); the
     * arg scan re-runs {@code adapter.adapt} on the raw args (the nsrGate receiver-adapt
     * precedent — the ONE #530 census that runs a real adapt rather than a pure walk;
     * read-only proven operationally by the probe1 stripped-sha identity, the Seat-1
     * #530 OBS-4 note). Probe-only; read-only.
     */
    private final Map<String, Integer> metaArgGateFacets = new TreeMap<>();

    /** The #530 channel-4 first-sample witness ({@link #metaArgGateFacets} buckets). Probe-only. */
    private final Map<String, String> metaArgGateFirstSamples = new TreeMap<>();

    /**
     * Count of witness/output simple-name collision FQN-inline renders that occurred inside an
     * IR-<em>driven</em> emission (the speculative renders of a subtree that ultimately declined are
     * discarded — see {@link #tryEmitFromIR}). This decisively answers whether the IR's own
     * {@code collisionFqn} path (L-029) is exercised by the corpus: 0 means every cross-namespace
     * collision render in the goldens came from the legacy fallback, not the IR.
     */
    private int collisionRenderCount = 0;

    /**
     * Count of top-level / legacy-fallback-child GENUINE enum-value references this compiler drove from the IR
     * (slice L-069 — {@link #visitEnumValueRef}). The DEDICATED firing oracle: this family is §4.2-counted, but the
     * global {@link #irDrivenCount} {@code > 0} gate is already satisfied by prior slices, so it would stay green
     * even if this seam regressed to ZERO firing. A per-cell assertion on THIS counter is what pins the live drive
     * (anti-L-042) — the enum-value-ref population is large + cell-specific (the recon: cdm5 250, cdm6 305, drr 1806
     * FUNCTION; drr 743 RULE), so a silent zero must fail the gate.
     */
    private int enumValueRefDrivenCount = 0;

    /**
     * #505: count of DISGUISED REnumValueRef roots the converted seat drove from the IR (the
     * L-109c exclusion RETIRED — {@link #visitEnumValueRef}'s residue branch now consults
     * {@link #tryEmitFromIR} first, so the chain/choice-option/symbol-nav/dispatch-input
     * disguises claim at their own roots: natively for the plain spines, through the #505
     * {@code enumChain} oracle leg for the oracle-leaf-bearing shapes). The dedicated firing
     * oracle, for the same reason as {@link #enumValueRefDrivenCount}: the converted seat's
     * live drive must pin per-cell (the #505 enumSeatRoot census read the claimable mass
     * ~1,383 across the cells — faFa 438 · symNav 545 · IRMetaAccess 344 · fa:IRMetaAccess
     * 56). A DECLINING disguised root falls to the same legacy line as pre-conversion (the
     * decline recorded + probed inside tryEmitFromIR — the honest full-population joining;
     * the #504 re-entrant law states BOTH directions).
     */
    private int enumChainRootDrivenCount = 0;

    /**
     * Count of {@code to-string} conversions this compiler drove from the IR (slice L-070 — {@link #visitToString}).
     * The dedicated firing oracle, for the same reason as {@link #enumValueRefDrivenCount}: this family is
     * §4.2-counted, but the global {@link #irDrivenCount} {@code > 0} gate is already satisfied by prior slices, so a
     * per-cell assertion on THIS counter pins the live drive (anti-L-042; the recon: drr FUNCTION 687, drr RULE 19).
     */
    private int toStringDrivenCount = 0;

    /**
     * Count of top-level item-rooted navigation chains this compiler drove from the IR (the extract/filter
     * percolation slice — {@link #visitFeatureCall}). The dedicated firing oracle, for the same reason as
     * {@link #enumValueRefDrivenCount}: navigation is already a §4.2-targeted family, so the global
     * {@link #irDrivenCount} {@code > 0} gate is already satisfied and would stay green even if THIS seam
     * regressed to zero. A per-cell {@code > 0} assertion is the DESIGNED anti-L-042 lock — not yet wired
     * (diagnostic-only today, zero test consumers; Seat-1 #472 OBS-3). A WITNESS counter — it counts
     * the top-level item-rooted chains (an item-nav folded as a comparison operand is counted by the parent's
     * §4.2 drive, not here); the FULL firing is the {@link #irDrivenCount}/{@link #irDeclinedCount} shift.
     * Since #472 a composition-root DELEGATED claim ({@link #compositionRootDelegation}) that is item-rooted
     * also lands here — the count witnesses IR-SERVED drives (native + delegated), the #471 OBS-1 re-scope class.
     */
    private int itemNavDrivenCount = 0;

    /**
     * Count of top-level checks this compiler drove from the IR whose operand is the bare filter/extract-bound
     * {@code item} (the bare-item-OPERAND slice — {@link #visitExistence}/{@link #visitComparison}/
     * {@link #visitEquality}). The dedicated firing oracle, for the same reason as {@link #itemNavDrivenCount}:
     * existence/comparison/equality are already §4.2-targeted, so the global {@link #irDrivenCount} {@code > 0}
     * gate cannot pin THIS seam — a per-cell {@code > 0} assertion is the DESIGNED anti-L-042 lock, not yet
     * wired (diagnostic-only today, zero test consumers). Counted ONLY inside a present emission,
     * so a then/switch-bound bare item (which declines) never increments it (the probe: FUNCTION 37, RULE 88).
     * Since #472 a composition-root DELEGATED claim ({@link #compositionRootDelegation}) with a bare-item
     * operand also lands here — IR-SERVED drives (native + delegated), the #471 OBS-1 re-scope class.
     */
    private int itemOperandDrivenCount = 0;

    /**
     * Count of top-level navigations this compiler drove from the IR whose chain BASE is a function CALL
     * ({@code someFunc(args) -> feature}) — the call-as-nav-base slice's firing oracle. Navigation is already
     * §4.2-targeted, so the global {@code irDrivenCount > 0} gate cannot pin THIS seam; a per-cell {@code > 0}
     * assertion is the DESIGNED anti-L-042 lock, not yet wired (diagnostic-only today, zero test
     * consumers). Counted only at a realized present emission. Since #472 a call-rooted
     * composition-root DELEGATED claim ({@link #compositionRootDelegation}) also lands here — IR-SERVED
     * drives (native + delegated), the #471 OBS-1 re-scope class.
     */
    private int callBaseNavDrivenCount = 0;

    /**
     * Count of existence checks this compiler drove from the IR whose operand is a function CALL
     * ({@code someFunc(args) exists}) — the L-105 call-as-existence-operand slice's firing oracle. Existence is
     * already §4.2-targeted, so the global {@code irDrivenCount > 0} gate cannot pin THIS seam; a per-cell
     * {@code > 0} assertion is the DESIGNED anti-L-042 lock, not yet wired (diagnostic-only today, zero
     * test consumers). Counted only at a realized present emission. Since #472 a
     * DELEGATED existence root ({@link #compositionRootDelegation}) with a call operand also lands here —
     * IR-SERVED drives (native + delegated), the #471 OBS-1 re-scope class.
     */
    private int callExistenceOperandDrivenCount = 0;

    /**
     * Count of comparison / equality checks this compiler drove from the IR whose operand is a function CALL
     * ({@code someFunc(args) = baz} / {@code someFunc(args) >= 1}) — the L-106 call-as-cmp/eq-operand slice's firing
     * oracle. The slice drives MORE comparison/equality nodes (real coverage) but DIPS the §4.2 proxy (the
     * binary-absorption trap: a driven binary node absorbs its two driven operand sub-counts); shipped on the
     * coverage-over-proxy steer. This count is the coverage witness (anti-L-042: the designed {@code > 0}
     * per-cell assert — not yet wired; diagnostic-only today, zero test consumers).
     * Since #472 a DELEGATED equality root ({@link #compositionRootDelegation}) with a call operand also lands
     * here — IR-SERVED drives (native + delegated), the #471 OBS-1 re-scope class.
     */
    private int callCmpEqOperandDrivenCount = 0;

    /**
     * Count of function CALLS this compiler drove from the IR carrying a NESTED function-call argument
     * ({@code f(g(x))}) — the L-107 call-as-nested-arg slice's firing oracle. The CALL family is already
     * §4.2-targeted, so the global {@code irDrivenCount > 0} gate cannot pin THIS seam; a per-cell {@code > 0}
     * coverage assert does (anti-L-042). Like the operand slices it drives MORE call nodes (coverage) while the
     * §4.2 proxy dips (the nested arg's own drive is absorbed) — shipped on the coverage-over-proxy steer.
     */
    private int callNestedArgDrivenCount = 0;

    /**
     * Count of function CALLS this compiler drove from the IR carrying a filter/extract-bound implicit {@code item}
     * argument ({@code someFunc(item)} inside an extract/filter lambda) — the L-108 call-as-item-arg slice's firing
     * oracle. The CALL family is already §4.2-targeted; a per-cell {@code > 0} coverage assert pins THIS seam
     * (anti-L-042). Concentrated on the RULE seam (rule bodies thread the implicit input through extract/filter).
     */
    private int callItemArgDrivenCount = 0;

    /**
     * Count of POINT-FREE function applications the RESIDUE-BELT seat drove (L-109 — the function analogue of the
     * L-049 rule-delegation renderer): a bare no-arg function reference whose adapter lowering came back empty,
     * rendered at the decline point by reusing legacy {@code ReferenceHandler.renderImplicitFunctionInvocation}
     * verbatim. Before #492 this seat carried the WHOLE top-level point-free family (the adapter deliberately
     * declined the shape — the L-109 deferral); since the #492 teach the adapter lowers the family to
     * {@link com.regnosys.rosetta.ir.expr.IRPointFreeApply} and the EMITTER path renders it through the
     * range-correlated {@link PointFreeRenderer} reusing the SAME oracle (byte-identical either way), so this
     * counter reads the belt's residue — expected ~ZERO (the one adapter decline face, shortcut-collision, is
     * deliberately skipped by the {@code isAliasReference} guard onto the legacy alias render).
     */
    private int pointFreeFnDrivenCount = 0;

    /**
     * Count of META-FEATURE navigations the L-109d RELABEL BELT claimed — a {@code receiver -> metaFeature}
     * whose feature carries a {@code metadata} annotation and whose RECEIVER does not lower (since the #499
     * meta arm, the belt's RESIDUE class: a lowering receiver now lowers the whole node to
     * {@link IRMetaAccess} and renders through the root-site {@link MetaNavRenderer} — the SAME
     * {@code super.visitFeatureCall} line, counted {@link #metaNavLoweredCount}; pre-#499 this seat carried
     * the WHOLE 2,962-event family). The gm-aware {@code MapperS<FieldWithMetaX>} retype stays an emitter
     * decision (the L-029 split) on BOTH routes — the belt routes the node to the meta-aware legacy renderer
     * ({@code super.visitFeatureCall}, the exact decline fallback — bytes provably unchanged). The navigation
     * family is already §4.2-targeted; a per-cell {@code > 0} assert pins THIS seam (anti-L-042).
     */
    private int metaNavDrivenCount = 0;

    /**
     * Count of claims this compiler DECLINED to legacy because the claimed subtree trips a POST-PIN coercion
     * refinement the IR route does not reproduce (the #467 drift-wave guard — {@link #subtreeTripsPostPinCoercion}).
     * Write-only at PR-3 by design (the {@code switchHoistCount} staging precedent); since PR-4 the
     * {@link #postPinCoercionDeclinedCount()} accessor is read by the D11 flag-on ring's ON-gate reader as the
     * guard's anti-L-042 firing lock ({@code > 0} on the drift-bearing cdm/6.20.6 FUNCTION carrier — a silent
     * un-fire now fails the gate directly, alongside the byte drift it would readmit). Since #472 the guard
     * seat is a ROUTER ({@link #compositionRootDelegation}): proven composition roots render via the
     * caller's own fallback instead of
     * declining, so THIS counter reads the honest residue only (roots outside the proven set — ZERO on the
     * frozen corpus's FUNCTION seam since the #473 singletonList endgame teach and on its RULE seam since
     * the #474 intLiteral teach) and the carrier-cell locks RETENSE to the router's firing counter
     * with {@code == 0} here on ALL THREE FUNCTION cells (the #470 OBS-3 designed retense; the drr pin
     * joined at #473 when its singletonList residue taught; the retensed counter moved
     * {@code postPinDelegatedCount() > 0} → {@code postPinServeLoweredCount() > 0} at the #516 serve
     * conversion) plus, since #474, {@code == 0} on EVERY cell of
     * the pojo-pass RULE-seam reader block (that seam's whole trip population is drr; its firing witness is
     * the drr router pin).
     */
    private int postPinCoercionDeclinedCount = 0;

    /**
     * Pre-#516 count of claims the post-pin guard seat DELEGATED WHOLE to the caller's own legacy fallback
     * render because the tripped claim's ROOT class is one of the seven proven composition classes (the #472 six + the #506
     * composition-boundary widening — {@link #compositionRootDelegation}). The render IS the
     * decline's own code path (the #469 claim-root law widened per-class: each proven caller's fallback is
     * exactly {@code super.visitX(expr, ctx)}, both exits sit at the same guard position, and the proven classes
     * have zero subclasses), so bytes are identical by construction and only the accounting moves (driven,
     * not declined). THE FROZEN ZERO since the #516 serve conversion: the router's claims count LOWERED on
     * {@link #postPinServeLoweredCount} (the adapter's IR lowering exists at the seat — the #507 accounting
     * law) and no code path increments this counter; the braces-dict token stays printed as the
     * honest-residue meter, and the D11 carrier locks that read {@code postPinDelegatedCount() > 0} (the
     * #470 OBS-3 tripwire's #472 retense) RETENSED AGAIN to {@code postPinServeLoweredCount() > 0} with
     * {@code postPinCoercionDeclinedCount() == 0} unchanged — the scan still fires on every
     * composition-boundary claim (FUNCTION carriers + the pojo-pass RULE-seam drr cell, the router's 13
     * RULE-seam claims of the #473 witness census).
     */
    private int postPinDelegatedCount = 0;

    /**
     * Firing sub-counter for the #471/#473/#474 claim-root delegation seat (a call that IS the whole
     * claim whose first-trip evaluate-arg arm is ACCEPTED, rendered via
     * {@code super.visitSymbolReference} — the decline's own render): the one delegated seat that had
     * NO dedicated counter before the #493 split (its only witness channel was the
     * {@code POSTPIN_WITNESS} stdout prints, off by default — the per-arm array at that seat was never
     * added because the {@code postPinDelegationsByArm} buckets belong to the COMPOSITION seat).
     * Incremented together with {@link #irDrivenCount} and {@link #irDrivenDelegatedCount} at that
     * single seat, closing the split's Σ conservation. CONVERTED at #516 (THE DELEGATED-SEAT SERVE):
     * the seat now counts LOWERED on {@link #claimRootServeLoweredCount} with the render unchanged, so
     * NO code path increments this counter any more — the braces-dict token stays printed as the
     * frozen-zero honest-residue meter (a future positive value means a NEW delegated path joined
     * without the serve accounting, self-signaling on the split line).
     */
    private int claimRootDelegatedCount = 0;

    /**
     * #516 — THE DELEGATED-SEAT SERVE's postPin flip receipt: claims the #472 composition router
     * previously counted DELEGATED ({@link #postPinDelegatedCount}), now counted LOWERED at the SAME
     * seat with the SAME render ({@link #compositionRootDelegation} — the caller's own literal
     * {@code super.visitX(expr, ctx)} fallback line, byte-identical BY IDENTITY per the #469/#472
     * per-class proof pack). The accounting move is the #507 {@link #inputNavIdentityLoweredCount}
     * law at the router: the adapter's IR lowering EXISTS for every claim that reaches this seat
     * (the seat sits after the adapt gate — {@code ir.isPresent()} by construction, unlike the four
     * ir-empty relabel belts, which stay DELEGATED), so the serve is an IR-LOWERED claim whose
     * render is served by identity; only the counter moves. The per-arm attribution continues on
     * {@link #postPinServesByArm} (the same first-trip attribution the delegation array carried),
     * so the {@code postPin served by arm} census line's Σ ≡ this receipt per cell BY CONSTRUCTION.
     */
    private int postPinServeLoweredCount = 0;

    /**
     * #516 — the claim-root seat's flip receipt (the same serve accounting at the #471/#473/#474
     * seat): a root call whose accepted evaluate-arg arm previously counted DELEGATED
     * ({@link #claimRootDelegatedCount}) now counts LOWERED with the render unchanged
     * ({@code super.visitSymbolReference} — the decline's own line; the adapter's lowering exists,
     * the seat sits after the adapt gate). Incremented together with {@link #claimRootServesByArm}
     * at the single seat, so the {@code claimRoot served by arm} census line's Σ ≡ this receipt per
     * cell BY CONSTRUCTION — the seat's FIRST per-arm decode (the #493 split had noted the seat
     * carried no sub-census; the conversion instruments it at the same stroke, the instrument-first
     * law).
     */
    private int claimRootServeLoweredCount = 0;

    /**
     * The post-pin coercion guard's decline ARMS — the per-arm attribution buckets behind
     * {@link #postPinCoercionDeclineBreakdown()}, and the share-growth wave's worklist (PR #469): each value is
     * ONE teachable refinement unit of the #467 guard (the sub-arms of the composite #467 arms 6/7 are split out,
     * since each is a separate teach). Teaching the IR route a unit's legacy refinement and narrowing its arm
     * sends that bucket to zero with the ON ring held byte-green. Attribution is FIRST-TRIP in the guard's scan
     * order (a subtree tripping several arms counts under the first one found — the same short-circuit order the
     * boolean guard always had), so the buckets sum exactly to {@link #postPinCoercionDeclinedCount}: both are
     * incremented together at the single guard seat in {@link #tryEmitFromIR}. Since #472 the guard seat is a
     * ROUTER: a trip at a proven composition root counts on the router's own array instead (the
     * DELEGATION array {@link #postPinDelegationsByArm} until #516, the SERVE array
     * {@link #postPinServesByArm} since the delegated-seat serve — same first-trip attribution), so per
     * arm served + declined = tripped, and a decline bucket's explicit ZERO with a positive serve
     * bucket is the taught state.
     */
    enum PostPinArm {
        /** The dispatch-variant context (facet dispatchVariantParamResolution #369) — TAUGHT at #470 (the alias
         *  base-input forwarding + the nested-divide witness; the wholesale context decline retired). Stays as
         *  the explicit-zero taught signal on the meter. */
        DISPATCH_VARIANT("dispatchVariant"),
        /**
         * The dep-receiver collision numbering (≥2 distinct callees sharing one bare lowerCamel name) —
         * TAUGHT at #469: {@link #buildCallReceiverResolver} renders the numbered receivers itself (the
         * FQN-keyed collector map, source-range-correlated), so this bucket's explicit ZERO is the taught
         * state, not an un-fire.
         */
        DEP_NUMBERING("depNumbering"),
        /**
         * An implicit-item call argument (post-pin legacy resolves the BOUND lambda name, not
         * {@code item.get()}) — TAUGHT at #469: the {@link ImplicitItemRenderer} reuses legacy
         * {@code handle(RImplicitVariable)} verbatim for every {@code USER_ITEM} render, so this bucket's
         * explicit ZERO is the taught state (meta-typed item args re-attribute to the meta arms).
         */
        ITEM_CALL_ARG("itemCallArg"),
        /**
         * A chained item-rooted navigation (per-hop lambda vars take deferred-sentinel scope escapes) —
         * ROOT-position TAUGHT at #469: a guarded chain that IS the whole claim is served by the
         * {@link ItemNavRenderer} via the exact legacy fallback render; a NESTED one still declines (the
         * composition boundary — the cpON4 drift receipt), so this bucket now counts nested TRIP SITES only.
         * Note (Seat-1 OBS-6): a 3+-hop chain AT the root also lands here — its interior sub-chain is
         * itself a guarded chain and trips the nested scan — so the taught-at-root set is 2-hop non-meta
         * chains (+ single-hop meta receivers under {@link #META_ITEM_RECEIVER}); the counts are trip-site
         * attribution, not claim-position counts. Since #472 the nested set SERVES WHOLE at proven
         * composition roots ({@link #compositionRootDelegation}; delegate-counted until the #516 serve) —
         * the decline bucket's explicit ZERO is the
         * taught state; the volume reads on {@link #postPinServeBreakdown()}. Since #523 the trip
         * ALSO fires on a DISGUISED item-headed chain at a call-ARG top (the single-arrow
         * {@code a -> b} class — legacy's own {@code synthesizeImplicitItemChain} the predicate BY
         * CALL, see the scan's call-branch probe), and an {@code RSymbolReference}-rooted resolved
         * call whose first trip is this arm serves WHOLE at the guard seat's #523 item-nav
         * claim-root leg (the #469 claim-root law, arm-independent) — the co-design that let the
         * adapter's argNav {@code itemChain.}/{@code itemSrcUnprovable.} decline mirror retire.
         */
        MULTI_HOP_ITEM_NAV("multiHopItemNav"),
        /**
         * A meta-typed implicit item at a navigation base (legacy inserts the "Type coercion" deref step) —
         * ROOT-position TAUGHT at #469 via the same {@link ItemNavRenderer} delegation; the nested set
         * SERVES WHOLE at proven composition roots since #472 ({@link #compositionRootDelegation};
         * delegate-counted until #516) — the
         * decline bucket's explicit ZERO is the taught state. Since #523 an
         * {@code RSymbolReference}-rooted resolved call whose first trip is this arm serves WHOLE at
         * the guard seat's item-nav claim-root leg (the same #469-law seat as
         * {@link #MULTI_HOP_ITEM_NAV} — legacy's own {@code implicitItemArgMeta} oracle decides the
         * trip, so an actually-meta single-hop item arg the adapter now admits under the #523
         * argNav teach routes to the whole-legacy serve, never a native compose).
         */
        META_ITEM_RECEIVER("metaItemReceiver"),
        /**
         * A within-long int literal into a {@code BigDecimal} single param (inline {@code BigDecimal.valueOf},
         * M2 #300) — ROOT-position TAUGHT at #474 via the claim-root delegation seat
         * ({@link #claimRootDelegationArm}): the frozen corpus's whole population is RULE-seam — 10 claim-root
         * decline events across 6 drr rule names (the #473 RULE-seam witness census), every one a
         * {@code RSymbolReference} call at the claim root, delegating via {@code super.visitSymbolReference}
         * (the decline's own render; the #469 claim-root law, arm-independent). The FUNCTION-seam population
         * is ZERO, so this bucket's explicit zero there predates the teach; a NESTED trip would SERVE
         * WHOLE at the seven proven composition roots ({@link #compositionRootDelegation}, the #472 router —
         * the same path the meta pair's nested set takes) and decline only at roots outside the set.
         */
        INT_LITERAL_ARG("intLiteralArg"),
        /**
         * A meta-wrapped argument into a meta-free single param (the hoist+deref family, #237/#285/#340/#346/
         * #364) — ROOT-position TAUGHT at #471 via the claim-root delegation seat in
         * {@link #tryEmitFromIR} ({@link #claimRootDelegationArm}): a call that IS the whole claim renders via
         * {@code super.visitSymbolReference} — the decline's own render, provably identical (the #469
         * claim-root law) — keeping the hoist family's five legacy route facets single-sourced. This bucket
         * now counts NESTED trip sites only (the #471 decode: 5, all drr — exists/equality compositions over
         * the call; the composition boundary), and those SERVE WHOLE at proven composition roots since
         * #472 ({@link #compositionRootDelegation}; delegate-counted until #516) — the decline bucket's
         * explicit ZERO is the taught state.
         */
        META_WRAPPER_SINGLE_ARG("metaWrapperSingleArg"),
        /**
         * A meta-item-typed argument into a multi param (the inline bare Type-coercion map before
         * {@code .getMulti()}, facet multiArgElementwiseWrapperDeref #349 / evaluate-arg arm B2) —
         * ROOT-position TAUGHT at #471 via the same claim-root delegation seat. This bucket now counts
         * NESTED trip sites only (the #471 decode: 4 per cdm cell — the alias-body nav/list-op parents over
         * the call; drr is B2-free), and those SERVE WHOLE at proven composition roots since #472
         * ({@link #compositionRootDelegation}; delegate-counted until #516) — the decline bucket's explicit
         * ZERO is the taught state.
         */
        META_ITEM_MULTI_ARG_B2("metaItemMultiArgB2"),
        /** A single-cardinality call/attribute/enum argument into a multi param (the singletonList family
         *  #368/#436/#269) — ROOT-position TAUGHT at #473 (the FUNCTION-seam endgame teach, menu item C):
         *  the claim-root delegation seat ({@link #claimRootDelegationArm}) accepts this arm since #473, so
         *  the frozen corpus's one FUNCTION claim (drr IsActionTypeMODI, the #472 witness census) serves
         *  at the seat (delegate-counted until #516) — the decline bucket's explicit ZERO is the taught
         *  state, and the FUNCTION-seam
         *  decline meter reads ALL-ZEROS. A nested trip would SERVE WHOLE at the seven proven composition
         *  roots ({@link #compositionRootDelegation}, the #472 router — the same path the meta pair's
         *  nested set takes) and decline only at roots outside the set. */
        SINGLETON_LIST_ARG("singletonListArg"),
        /**
         * A bare shortcut-resolved ALIAS operand of an EQUALITY whose {@code FunctionAliasHelper}
         * signature element the RETYPE oracle names — {@code NavigationHandler
         * .tryAliasReceiverMapperType} non-null, the EXACT lever the equality seat's
         * {@code retypeNullTypedAliasOperand} keys on. The oracle's set is META wrapper items
         * (the #326 {@code aliasOperandMetaCoerce} facet — the retype makes the meta-strip deref
         * fire, {@code .map("Type coercion", …getValue())}, a render the neutral leaf does not
         * reproduce: the byte-divergent subset, the six-carrier drr ring decode) UNION the six
         * basic scalars (the #334 {@code arithOperandWrapperCoerce} widening — that retype is
         * byte-INERT at this seat, {@code coerceNavigationReceiver}'s own meta gate no-ops it,
         * so the basic subset delegates byte-identically; the Seat-1 #491 MF-2 receipt proof:
         * the 74 cdm trips rendered natively byte-GREEN pre-arm). The arm keeps the label's
         * discovery name; the trip set = the retype set, so the guard can never under-trip the
         * divergent subset. Reachable since the #491 alias-operand admission (the adapter's
         * engine-side body read cannot see the wrapper), and routed like every arm: an
         * equality/logical composition ROOT serves the WHOLE claim (legacy renders parent +
         * retype together — byte-identity by the decline's-own-render argument; delegate-counted
         * until #516), other roots
         * decline honestly.
         */
        ALIAS_EQUALITY_OPERAND_META("aliasEqualityOperandMeta"),

        /**
         * #492 — the alias-NAV retype mirror (the alias-nav teach's residue class): a navigation
         * whose ALIAS head/receiver the RETYPE oracle names — {@code NavigationHandler
         * .tryAliasReceiverMapperType} non-null, the EXACT lever the nav seat consults at
         * {@code NavigationHandler} handle-time (the null-then-retype protocol: the alias call
         * renders NULL-typed, the {@code FunctionAliasHelper} signature walk stamps the type, and
         * the {@code coerceNavigationReceiver} meta gate then inserts the {@code "Type coercion"}
         * deref hop the neutral field-access render lacks). The adapter's engine-side body read
         * admits these clean ({@code hasMeta()} is the WEAKER oracle — the #326/#491 class), so
         * the compiler-side guard carries the Java fact at the SAME seat as every post-pin
         * refinement; trip set = retype set, so the divergent subset can never under-trip. Two raw
         * shapes trip: the parsed {@code RFeatureCall} over a bound-shortcut receiver, and the
         * disguised {@code REnumValueRef} whose head names an enclosing shortcut (probed through
         * the same throwaway receiver stub legacy {@code synthesizeFeatureCall} builds — the
         * lever's {@code resolveAliasShortcut} resolves it via the name-match fallback). The
         * router serves the proven composition roots WHOLE (delegate-counted until the #516
         * serve).
         */
        ALIAS_NAV_RECEIVER_META("aliasNavReceiverMeta"),

        /**
         * #527 — the missing-hop arg-nav belt (the ARG_NAV-dict teach's compiler half): an
         * admitted call-argument navigation chain carrying the MISSING type sentinel on a hop
         * (or on its {@code IRVariable} root — the #491 root-belt read). The adapter's #527
         * {@code argNavFacet} recut admits PARAM-rooted missing-typed chains (the
         * {@code typeMissing.param.deep[.multi]} faces — 10 sole at the #526 SOT, the drr RULE
         * seam entire), whose native witness render is unprovable BY CONSTRUCTION (the
         * {@code <X>map("getY", …)} generics read the stamped hop types); the #527 argGapGate
         * census read legacy's OWN channel {@code javaEq}/{@code multiParam} on the whole
         * population — legacy renders the flat raw pass — so the claim serves WHOLE at the
         * {@code RSymbolReference} claim root via {@code super.visitSymbolReference} (the #469
         * claim-root law; the #523 leg's membership widened to this arm). The trip probe
         * ({@link #loweredArgChainCarriesMissingHop}) runs AFTER the standing scan returns
         * null (first-trip attribution frozen — a scan-tripping claim keeps its standing arm)
         * and reads the LOWERED tree only — the trigger set was a 100%-decline face pre-teach
         * (every missing-hop chain was adapter-declined), so no standing claim can trip (the
         * #504 conservation argument). A future missing-hop carrier at a non-servable root
         * declines below — the honest-residue default on the meter.
         */
        ARG_NAV_MISSING_HOP("argNavMissingHop");

        private final String label;

        PostPinArm(String label) {
            this.label = label;
        }

        /** The lowerCamel receipt token this arm prints as in {@link #postPinCoercionDeclineBreakdown()}. */
        String label() {
            return label;
        }
    }

    /**
     * The per-arm decline counts behind {@link #postPinCoercionDeclineBreakdown()} — indexed by
     * {@link PostPinArm#ordinal()}, incremented ONLY at the guard seat in {@link #tryEmitFromIR} (alongside
     * {@link #postPinCoercionDeclinedCount}, so the array totals the scalar by construction).
     */
    private final int[] postPinCoercionDeclinesByArm = new int[PostPinArm.values().length];

    /**
     * The per-arm delegation counts behind {@link #postPinDelegationBreakdown()} — indexed by
     * {@link PostPinArm#ordinal()}, historically incremented ONLY at the guard seat's #472 delegation
     * branch (alongside {@link #postPinDelegatedCount}, so the array totals the scalar by construction;
     * first-trip attribution in the scan's short-circuit order, exactly like the decline array — the two
     * arrays partition the scan's trip set between them). Since the #516 serve conversion NO code path
     * increments it — the line stays printed as the frozen-zero honest-residue meter and the live per-arm
     * attribution moved to {@link #postPinServesByArm} (served + declined = tripped, per arm).
     */
    private final int[] postPinDelegationsByArm = new int[PostPinArm.values().length];

    /**
     * The per-arm SERVE counts behind the D11 {@code postPin served by arm} receipt line (#516 — the
     * delegation array's successor channel: same {@link PostPinArm#ordinal()} indexing, same first-trip
     * attribution, incremented ONLY at the router's serve branch alongside
     * {@link #postPinServeLoweredCount}, so the array totals that receipt by construction and
     * served + declined = tripped per arm).
     */
    private final int[] postPinServesByArm = new int[PostPinArm.values().length];

    /**
     * The claim-root seat's per-arm serve census (#516 — the seat's FIRST decode; the #493 split had
     * left it the one delegated seat with no sub-census). Only the four ACCEPTED evaluate-arg arms of
     * {@link #claimRootDelegationArm} can index it — the other labels print structural zeros (the
     * uniform full-enum vector, the same formatter as the postPin lines). Incremented alongside
     * {@link #claimRootServeLoweredCount} at the single seat, so Σ ≡ the receipt by construction.
     */
    private final int[] claimRootServesByArm = new int[PostPinArm.values().length];

    /**
     * The share-growth wave's per-decline WITNESS channel ({@code -Drosetta.generator.ir.postpinWitness=true},
     * default silent): each guard decline prints one {@code [postPin witness]} line — the tripping arm, the
     * enclosing function (or rule), the claim root's node class, and the trip-site detail
     * {@link #postPinTripDetail} the scan recorded — the arg-arm trip: callee + arg index + arg shape +
     * {@code callAtRoot} (whether the tripping call IS the claim root); the nested-nav trip: feature +
     * receiver shape, its root position stated by the {@code nestedNav} prefix itself (that arm is guarded
     * {@code node != claimRoot}, so it only ever trips NESTED — a root guarded nav never reaches it, being
     * served by the {@link ItemNavRenderer} delegation). A SERVED claim prints its own line (the #516
     * relabel of the pre-conversion {@code -rootDelegated}/{@code -delegated} spellings): the #471
     * claim-root seat as {@code arm=<label>-rootServed in=<fn/rule>} (no trip detail — its probe is not
     * the scan), the #472 composition router as {@code arm=<label>-served in=<fn/rule> claimRoot=<class>
     * <detail>} (the scan's own trip detail, so the decode stays per-claim even with guard declines at
     * ZERO). The decode vehicle for sizing each arm's teach (the #470 law: decode which
     * claims can even REACH the seam before sizing a teach), routed through the guard seat ITSELF — the same
     * seam the gate meters, so the witness set and the meter cannot disagree (the A/B-vehicle law). Prints on
     * whichever seam the compiler serves; the FUNCTION-only D11 selection ({@code -Dtest=
     * "D11CorpusRegressionTest#function_comparison"}) scopes a capture to the meter line's own population.
     */
    private static final boolean POSTPIN_WITNESS =
            Boolean.getBoolean("rosetta.generator.ir.postpinWitness");

    /**
     * The #475 census's per-decline WITNESS channel ({@code -Drosetta.generator.ir.declineWitness=true},
     * default silent): every {@link #recordDecline} prints one {@code [irDecline witness]} line — the
     * decline SITE, the claim root's node class, and the enclosing function (or rule) — so a family's
     * census bucket can be sampled down to its member claims without a debugger (the #471 law: a
     * permanent property-gated witness channel at the seat beats per-wave throwaway probes; witness ≡
     * meter by construction — both fire in the same helper). A SEPARATE property from
     * {@link #POSTPIN_WITNESS} on purpose: the postpin channel's documented captures are small,
     * pinned line-sets (23 lines on the #474 RULE-seam receipt) that a 30k-line decline dump must not
     * flood; a census capture opts into the volume explicitly.
     */
    private static final boolean DECLINE_WITNESS =
            Boolean.getBoolean("rosetta.generator.ir.declineWitness");

    /**
     * The #476 adapterGap blocker-attribution PROBE ({@code -Drosetta.generator.ir.blockerProbe=true},
     * default OFF): at every {@code adapterGap} decline, re-adapt the declined claim's expression
     * subtree bottom-up and record its MINIMAL blockers — the nodes whose own
     * {@link ExpressionToIRAdapter#adapt(RExpression, RWorkspace)} returns empty while none of their
     * nearest expression descendants is itself blocked. The #475 family-of-root census keys an
     * adapterGap decline by the CLAIM ROOT's class, which cannot see WHICH nested family the adapter
     * choked on (a claim declines whole when any subtree node fails to lower); this probe attributes
     * each declined claim to the actual blocking families, so the rune-ir adapter teaches are sized by
     * their REAL unlock BEFORE any is built (the decode-first law — a constructor arm unblocks every
     * claim NESTING a constructor, not just the {@code RConstructorExpr}-rooted ones).
     *
     * <p>Read-only against the render by construction: the probe runs strictly AFTER
     * {@link #recordDecline} on the already-declined path, calls only the adapter's documented
     * stateless, side-effect-free {@code adapt} (never the visitor — the standing counters cannot
     * move), and writes only its own maps/scalars, which no render decision reads. Property-gated
     * because it re-adapts every adapterGap claim's subtree — a census-run cost, not a standing-ring
     * cost; with the probe off the standing flag-on receipts stay byte-identical, meters untouched.
     * Instance-mirrored ({@link #blockerProbeEnabled}) so the unit locks can enable it per-compiler.
     *
     * <p>Since #477 each minimal blocker is additionally attributed to its {@code Family:reason}
     * arm-gate pair via the adapter's own read-only {@link ExpressionToIRAdapter#declineReason}
     * channel (the per-ARM refinement decode — WHY the family's arm declined, not just WHICH
     * family), on the same probe run at no extra flag: the pair rankings ride the same
     * {@code blockerProbe} property.
     */
    private static final boolean BLOCKER_PROBE =
            Boolean.getBoolean("rosetta.generator.ir.blockerProbe");

    /** Instance mirror of {@link #BLOCKER_PROBE} — the class-load static made test-settable ({@link #setBlockerProbeForTest}). */
    private boolean blockerProbeEnabled = BLOCKER_PROBE;

    /** #476 probe: adapterGap-declined claims probed (== the adapterGap decline events seen while the probe was enabled). */
    private int blockerProbedClaimCount = 0;

    /**
     * #476 probe: per-family count of probed claims with at least one minimal blocker of that family —
     * a claim counts ONCE per DISTINCT blocking family, so a multi-family claim appears under each of
     * its families and the map's values can sum past {@link #blockerProbedClaimCount} (deliberate:
     * the unit is "claims this family participates in blocking").
     */
    private final Map<String, Integer> blockerClaimsByFamily = new HashMap<>();

    /**
     * #476 probe: per-family count of probed claims whose minimal blockers are ALL one family — the
     * claims that family's adapter teach FULLY unblocks on its own. THE unlock ranking (the
     * sole-blocker mining law): a family high on {@link #blockerClaimsByFamily} but low here mostly
     * co-blocks with other families and its solo teach moves little. Measured against TODAY'S
     * adapter — an ancestor whose own gate would independently decline is exempted while a
     * descendant blocks, so a landed teach can surface a successor blocker; the probe re-reads
     * after each teach (the unlock figure is an upper bound per reading, exact only claim-by-claim).
     */
    private final Map<String, Integer> soleBlockerClaimsByFamily = new HashMap<>();

    /**
     * #476 probe: probed claims whose root RE-adapt unexpectedly LOWERED (or attributed no blocker) —
     * contradicting the decline that triggered the probe, which the adapter's stateless contract rules
     * out. Expected 0 always; an honest-residue meter, never absorbed into the family maps.
     */
    private int blockerProbeAnomalyCount = 0;

    /**
     * #477 probe: the per-ARM refinement decode — per-{@code Family:reason} count of probed claims
     * with at least one minimal blocker of that pair, where the reason is
     * {@link ExpressionToIRAdapter#declineReason}'s first-failing-gate token for the blocker node
     * (the #476 family ranking re-keyed one level deeper: WHICH gate inside the family's own arm
     * declined). The claim-counting unit mirrors {@link #blockerClaimsByFamily} exactly — a claim
     * counts ONCE per DISTINCT pair, so a claim with two same-family blockers of different reasons
     * counts under each pair (and the pair values can sum past the family's own participation).
     */
    private final Map<String, Integer> blockerClaimsByReason = new HashMap<>();

    /**
     * #477 probe: per-pair count of probed claims whose minimal blockers are ALL one
     * {@code Family:reason} pair — the claims a single ARM-GATE refinement fully unblocks on its
     * own (the sole-blocker mining law, applied at the arm-gate unit; the same upper-bound caveat
     * as {@link #soleBlockerClaimsByFamily} — re-read after each teach). Conservation by
     * construction at the recording seat: for every family,
     * {@code soleBlockerClaimsByFamily[F] == Σ_reasons soleReasonClaimsByReason[F:R] +
     * soleFamilyMultiReasonClaims[F]} — a sole-family claim either has ONE pair (it lands here) or
     * several same-family pairs (it lands on the multi-reason residue), never both, never neither.
     */
    private final Map<String, Integer> soleReasonClaimsByReason = new HashMap<>();

    /**
     * #477 probe: per-family count of probed claims whose minimal blockers are all ONE family but
     * MORE THAN ONE {@code Family:reason} pair — the sole-family claims a single arm-gate
     * refinement does NOT fully unblock (two gates of the same arm must both be taught). The
     * honest residue that makes the family-level sole ranking decompose exactly (see
     * {@link #soleReasonClaimsByReason}'s conservation identity).
     */
    private final Map<String, Integer> soleFamilyMultiReasonClaims = new HashMap<>();

    /**
     * #478 nav-gate shape witness (probe runs only): per-bucket count of {@link REnumValueRef}
     * minimal-blocker NODE occurrences on the two disguised-navigation gates
     * ({@code inputFeatureNav} / {@code attributeChain}), keyed {@code channel:shape:verdict}. The
     * shape names the head/root class in legacy {@code ReferenceHandler}'s OWN resolution order
     * ({@code headInput} / {@code headOutput} / {@code headShortcut} / {@code headCondAttr} /
     * {@code headOther} for the input-feature channel; {@code ruleInput} / {@code twoSegOffRoot} /
     * {@code oneSegClosure} for the attribute-chain channel), and the verdict is the ADAPTER's take
     * on the EXACT equivalent navigation legacy synthesizes for the node
     * ({@code ReferenceHandler.synthesizeFeatureCall} / {@code synthesizeImplicitInputChain} — the
     * widened #478 seams, the L-109e reuse precedent): {@code lowers} = the ADAPTER admits the
     * equivalent (adapter admissibility ONLY — no emitter attempt: a fresh synthesized node types
     * MISSING through the expression-node-keyed cache, so the live nested route is
     * visitFeatureCall → adapter admits → the EMITTER declines → the byte-proven legacy render;
     * an arm claiming this slice therefore pairs the same lowering with the cache-boundary RETYPE,
     * and the claim is proven at population by the post-teach conservation signature, not asserted
     * here) — THE claimable mass;
     * {@code declines:<token>} = the adapter's own first-failing-gate token on the equivalent (a
     * {@code receiverNotExpressible} chain wrapper is descended to the failing BASE's own token,
     * prefixed {@code receiverBase.}); {@code noEquivalent} = the primary legacy branch synthesizes
     * nothing (the node renders via a later fallback branch). The unit is blocker-node OCCURRENCES
     * (a claim with two disguised-nav blockers counts twice — a shape census over nodes, NOT the
     * claim-unit rankings above; no conservation identity against them is implied). Diagnostic-only;
     * the extra equivalent adapts ride the probe's census-run cost envelope (the #471 OBS-4 class).
     */
    private final Map<String, Integer> navGateShapeBuckets = new HashMap<>();

    /**
     * #478 nav-gate position facet (probe runs only): the same blocker-node occurrences split
     * per channel by AST seat — {@code receiverOfChain} (the node is the receiver of an enclosing
     * {@link RFeatureCall}, so a taught lowering also un-gates the chain hop above it) vs
     * {@code directPosition} (operand/argument seats). Same node-occurrence unit as
     * {@link #navGateShapeBuckets}.
     */
    private final Map<String, Integer> navGatePositionBuckets = new HashMap<>();

    /**
     * #478 nav-gate witness samples (probe runs only): the FIRST sampled site per
     * {@link #navGateShapeBuckets} bucket — {@code in=<function|rule> <head>-><feature>} — the
     * decode's qualitative face (which real corpus shapes each bucket holds). {@link TreeMap} for
     * deterministic receipt order; bounded by the bucket key space.
     */
    private final Map<String, String> navGateFirstSamples = new TreeMap<>();

    /**
     * #479 implicit-root shape witness (probe runs only): per-bucket count of minimal-blocker NODE
     * occurrences on the three implicit-ROOT gates the #478 witness proved bottom out at the
     * synthetic input/item base — {@code RFeatureCall:receiverSyntheticItem} (keyed
     * {@code synItemNav:<context>:<verdict>} — the context restates legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)}'s own arm order read-only:
     * {@code thenBody} / {@code namedExtractInputForm} / {@code namedExtractBody} /
     * {@code switchCase} / {@code condTopLevel} / {@code ruleTopLevel} /
     * {@code inLambdaImplicit.filterExtract} / {@code inLambdaImplicit.otherBinder} /
     * {@code inLambdaNamed} / {@code otherTopLevel}; the verdict is the ADAPTER's own lowered-item
     * type facts ({@code typed} / {@code typeMissing} / {@code typeMeta} / {@code notLowered}) —
     * except the {@code filterExtract} slice, which reads the RETYPE-SOURCE channel the arm
     * draws from (the binder ARGUMENT's engine type, the Cat-8 element-type law at the adapter
     * boundary — the #478 cache-boundary class): {@code retypeSourceOk} — expected ZERO on every
     * post-teach probed run, the #479 conservation signature (a nonzero re-read means the arm and
     * this restatement drifted — triage, never absorb) — / {@code retypeSourceMissing} /
     * {@code retypeSourceMeta} / {@code metaSourced} (the direct L-029 gm-coercion decline) /
     * {@code unprovableSource} (outside the arm's element-form allowlist)), {@code RSymbolReference:attrOutsideFunction} (keyed
     * {@code bareAttr:<synthesizer>:<verdict>} — the synthesizer axis walks legacy
     * {@code handle(RSymbolReference)}'s OWN arm chain via the widened seams
     * ({@code synthesizeImplicitInputNavigation} split {@code ruleInputTop}/{@code ruleInputLambda}
     * by the receiver-builder's own lambda test → {@code synthesizeImplicitItemNavigation} →
     * {@code synthesizeConditionInstanceNavigation} → {@code noSynthesizer}), and the verdict is
     * the adapter's take on the EXACT synthesized equivalent — the #478 verdict machinery,
     * {@code receiverNotExpressible} descended to the failing BASE's own {@code receiverBase.}
     * token), and {@code REnumValueRef:attributeChain} whose rule-input chain synthesizer NULLS
     * (keyed {@code chainFall:<arm>:<verdict>} — the fall-through decode in legacy
     * {@code handle(REnumValueRef)}'s own order: {@code itemChain} via the widened
     * {@code synthesizeImplicitItemChain} + adapter verdict / {@code caseNarrowedScope} (the
     * scope-gated #368 F-B arm — AST-classified only, a live subject binding cannot exist under
     * the probe) / {@code genericSynth} via the #478 {@code synthesizeFeatureCall} seam + adapter
     * verdict; the ruleInput population stays on the #478 {@code navGateShapeBuckets} channel
     * BYTE-UNCHANGED — this channel deliberately skips it). The unit is blocker-node OCCURRENCES
     * (the #478 witness's unit); diagnostic-only, riding the probe's census-run cost envelope
     * (the #471 OBS-4 class).
     */
    private final Map<String, Integer> implicitRootShapeBuckets = new HashMap<>();

    /**
     * #479 implicit-root witness samples (probe runs only): the FIRST sampled site per
     * {@link #implicitRootShapeBuckets} bucket — {@code in=<function|rule|?> <detail>} — the
     * decode's qualitative face. {@link TreeMap} for deterministic receipt order; bounded by the
     * bucket key space.
     */
    private final Map<String, String> implicitRootFirstSamples = new TreeMap<>();

    /**
     * #480 implicit-root SOURCE decode (probe runs only): the {@code unprovableSource} residue and
     * the still-declining equivalents' synthetic BASES, decoded by binding source shape — the
     * widening map the #479 allowlist law calls for ("the unprovable residue stays legacy's and
     * its census line sizes the widening"). Four channels, one per population:
     * {@code synItem:unprovableSource.<shape>} (the #479 {@code
     * synItemNav:inLambdaImplicit.filterExtract:unprovableSource} bucket sub-decoded by the SOURCE
     * expression's AST shape — {@code thenPipe} / {@code elidedImplicit} / {@code alias} /
     * {@code listOp.<OP>} / {@code conditional} / {@code featureUnresolved} /
     * {@code listLiteral.<element>} / {@code filter.<inner>} / … — Σ over the sub-buckets equals
     * the standing bucket's count, a receipt-side conservation check), and
     * {@code bareAttr:}/{@code chainBase:}/{@code ruleInputLambda:} (the #479 equivalents whose
     * adapter verdict still names a synthetic-receiver decline, decoded at the equivalent's
     * implicit BASE: {@code binder.<context>} for a non-filter/extract binding context, else the
     * retype-source facet with {@code unprovableSource} expanded by shape;
     * {@code retypeSourceOk} here is contract-impossible — a retype-admissible base would have
     * lowered the equivalent — so a nonzero read is a drift detector, triage never absorb). The
     * standing #479 {@link #implicitRootShapeBuckets} stay BYTE-UNCHANGED (the additive-channel
     * contract); node-occurrence unit; diagnostic-only, riding the probe's census-run cost
     * envelope (the #471 OBS-4 class — no extra adapter calls, the verdicts are reused).
     */
    private final Map<String, Integer> implicitRootSourceBuckets = new HashMap<>();

    /**
     * #480 implicit-root source samples (probe runs only): the FIRST sampled site per
     * {@link #implicitRootSourceBuckets} bucket. {@link TreeMap} for deterministic receipt order;
     * bounded by the bucket key space.
     */
    private final Map<String, String> implicitRootSourceFirstSamples = new TreeMap<>();

    /**
     * #480 arm-admission restatement (probe runs only): the planned bare-attr/chain adapter arms'
     * admission predicates restated at the witness seat (the #479 law — the witness's verdict
     * channel must BE the arm's channel, so the post-teach probe is the drift-proof between the
     * two restatements). Keyed {@code bareAttrArm:<verdict>} (every {@code bareAttr:itemNav}
     * blocker) and {@code chainArm:<verdict>} (every {@code chainFall:itemChain} blocker);
     * {@code <verdict>} = {@code claims} — the arm's full admission passes, EXPECTED ZERO on every
     * post-teach probed run (a claimed node lowers and never reaches the witness; a nonzero
     * re-read means the arm and this restatement drifted) — or {@code declines:<gate>} naming the
     * arm's FIRST failing gate in the arm's own exit order ({@code ruleInputNav} /
     * {@code shortcutCollision} / {@code noFilterExtractBinder} / {@code sourceElementUnresolved}
     * / {@code identityGuard} / {@code equivalent.<sub>} — the already-computed adapter verdict on
     * the legacy-synthesized equivalent — / {@code navTypeMissing}; the chain arm adds
     * {@code nameMissing} / {@code closureParamHead} / {@code scopeHead} / {@code ruleInputChain}
     * / {@code headNotOnElement} / {@code boundHeadMismatch} / {@code headTypeUnresolved} /
     * {@code leafUnresolved}). Node-occurrence unit; the standing channels stay BYTE-UNCHANGED.
     */
    private final Map<String, Integer> implicitRootArmBuckets = new HashMap<>();

    /**
     * #481 elided-PIPE decode (probe runs only): every {@code elidedImplicit} binding source on
     * the #480 source-decode channels — the grammar-elided PIPED implicit that dominated every
     * unprovable channel at the #480 reading (synItem 93.4%) — decoded by the PLANNED widening's
     * admission, restated BEFORE the arm exists (the #480 law). Keyed
     * {@code pipe:<channel>:<facet>} where {@code <channel>} is the #480 channel
     * ({@code synItem} / {@code bareAttr} / {@code chainBase} / {@code ruleInputLambda}) and
     * {@code <facet>} walks the planned resolver's own exit order
     * ({@link #elidedPipeFacetToken}): the non-then faces size the widening's structural residue,
     * {@code thenArg.unprovable.<shape>} maps WHICH pipe arguments stay unprovable (the next
     * widening's sizing), and {@code thenArg.provable.typeOk} is THE claimable slice — post-teach
     * it reads ZERO on every cell (a widened-admissible source lowers its claim and never reaches
     * the witness; a nonzero re-read means the arm and this restatement drifted — triage, never
     * absorb, the witness-as-detector law). Σ over a channel's pipe facets ≡ the channel's
     * standing {@code unprovableSource.elidedImplicit} count (receipt-side conservation — the
     * additive contract, every standing bucket byte-unchanged). Node-occurrence unit;
     * diagnostic-only, riding the probe's census-run cost envelope (the #471 OBS-4 class — no
     * adapter calls, the decode is structural walks + one type-cache read).
     */
    private final Map<String, Integer> elidedPipeBuckets = new HashMap<>();

    /**
     * #481 elided-pipe samples (probe runs only): the FIRST sampled site per
     * {@link #elidedPipeBuckets} bucket. {@link TreeMap} for deterministic receipt order; bounded
     * by the bucket key space.
     */
    private final Map<String, String> elidedPipeFirstSamples = new TreeMap<>();

    /**
     * The last {@link #scanForPostPinCoercion} trip's detail for the witness channel — written at each trip
     * point in the scan (the arg-arm loop + the nested-nav arm), read ONLY at the guard seat's witness print
     * (same call stack, no cross-emission state; stale content is never read because the guard seat prints
     * only when the scan just returned non-null).
     */
    private String postPinTripDetail;

    /**
     * Count of implicit-INPUT navigations this compiler claimed for the IR path — a bare rule-from-type attribute
     * {@code attr} that legacy expands to {@code input -> attr}. The adapter declines the bare reference (the synthetic
     * {@code input} receiver is a gm-aware Java-synthesis decision, the L-029 split); the IR routes it to the legacy
     * synthesizer + visitFeatureCall (bytes unchanged). RULE-seam only (functions have no rule input). A per-cell
     * {@code > 0} assert pins THIS seam (anti-L-042).
     */
    private int implicitInputNavDrivenCount = 0;

    /**
     * Count of INPUT-FEATURE navigations this compiler claimed for the IR path via the RELABEL
     * (L-111) — a disguised {@code <function-input> -> <feature>} {@link REnumValueRef} the fork's
     * {@code head -> feature} cascade fix (PR #279, pin {@code 3c60acec}) newly typed via the
     * typing-only {@code resolvedInputFeature} channel (with {@code resolvedAttributeChain} absent
     * — the precedence winner). The seat's contract since the #496 leg-2 conversion (widened at
     * #497): the disguised branch consults the adapter FIRST through the QUIET claim — the
     * proven {@code FieldAccess} shapes (the IRVariable input-head slice #496; the
     * IRReference{ALIAS} alias-head slice #497) render NATIVELY and count
     * {@link #inputFeatureNavLoweredCount}, and THIS counter reads the RESIDUE that stays on the
     * byte-proven legacy relabel ({@code super.visitEnumValueRef} — the guard-tripped + blocked
     * faces, 422 at the #497 read). The navigation analogue of the meta-feature (L-109d) /
     * implicit-input (L-109e) relabels. §4.2-counted, so the global {@code irDrivenCount}
     * {@code > 0} gate cannot pin THIS seam — a per-cell {@code > 0} assert does (anti-L-042).
     * FUNCTION-seam population (rule bodies carry no function-input feature nav — the census
     * measured drr RULE inputFeature = 0).
     */
    private int inputFeatureNavDrivenCount = 0;

    /** The L-111 input-feature-navigation relabel firing count (disguised {@code <input> -> <feature>} REnumValueRefs claimed). */
    public int inputFeatureNavDrivenCount() {
        return inputFeatureNavDrivenCount;
    }

    /**
     * #496 leg 2 — count of L-111 disguised input-feature navigations this compiler LOWERED
     * through the emitter's own {@code FieldAccess} emission (the honest-series conversion: the
     * #478 {@code adaptDisguisedInputNav} arm lowers the root, the post-pin guard stays LIVE — no
     * literal-fallback exemption identity applies to an emitter-composed render — and the render
     * is the SAME proven emission the nested positions and the RFeatureCall claim roots already
     * ring-prove). The residue (adapter-declined / guard-tripped / emitter-declined) stays on the
     * byte-proven L-111 relabel ({@link #inputFeatureNavDrivenCount}), so the seat's total
     * conserves as lowered + delegated by construction. The standing driven-split receipt prints
     * this counter beside the delegated-seat dict (outside the Σ ≡ delegated braces — it is a
     * LOWERED fact, not a ninth delegated seat).
     */
    private int inputFeatureNavLoweredCount = 0;

    /** The #496 L-111 conversion's lowered-claim count (the relabel seat's flip receipt). */
    public int inputFeatureNavLoweredCount() {
        return inputFeatureNavLoweredCount;
    }

    /**
     * #507 arm-B — count of L-111 disguised input-feature navigations this compiler LOWERED
     * through the seat's IDENTITY serve (the #496 conversion's completion for the seat's
     * LOWERABLE residue: the adapter's arm lowers the root but the quiet emitter claim did not
     * fire — the guard-tripped events, 143 at the #506 SOT's l111Residue census, plus any
     * future emitter-declined event — so the seat renders the LITERAL relabel line
     * {@code super.visitEnumValueRef} and counts the event LOWERED, byte-identical BY IDENTITY:
     * the render call is exactly the relabel's own, only the accounting moves). The
     * adapter-DECLINED residue (279 at the #506 census: the alias-head faces + headTypeMissing
     * + receiverNotExpressible) keeps the relabel and the delegated receipt exactly as
     * pre-conversion. Its own counter (not folded into {@link #inputFeatureNavLoweredCount})
     * so the emitter-composed and identity-served routes stay separately receipted — the
     * driven-split line prints it beside the other outside-the-braces lowered facts.
     */
    private int inputNavIdentityLoweredCount = 0;

    /** The #507 L-111 identity-serve lowered-claim count (the seat residue's flip receipt). */
    public int inputNavIdentityLoweredCount() {
        return inputNavIdentityLoweredCount;
    }

    /**
     * #509 arm-A1 — the parsed-seat alias identity-serve lowered-claim count (the #507
     * {@link #inputNavIdentityLoweredCount} pattern at the {@code RFeatureCall} seat): the
     * guard-held plain alias navs (the #326 weaker-oracle class the strong body allowlist
     * declines to render natively — 143 at the #508 SOT's aliasIdGate census) served through
     * the LITERAL fallback line ({@code super.visitFeatureCall} — byte-identical BY IDENTITY)
     * and counted LOWERED (an IR lowering exists; only the render is served). The census's
     * {@code aliasIdGate.served.identityShape.<shape>} rows Σ-reconcile to this counter per
     * cell by construction (the OBS-4 cross-check law). Its own counter — the driven-split
     * line prints it beside the other outside-the-braces lowered facts.
     */
    private int aliasNavIdentityLoweredCount = 0;

    /** The #509 arm-A1 alias identity-serve lowered-claim count (the belt leg's receipt). */
    public int aliasNavIdentityLoweredCount() {
        return aliasNavIdentityLoweredCount;
    }

    /**
     * #499 — count of L-109d meta-feature navigations this compiler LOWERED through the
     * root-site {@link MetaNavRenderer} (the metaNav conversion: the adapter's meta arm lowers
     * the node to {@link IRMetaAccess} whenever its receiver lowers, and the render is the
     * LITERAL relabel-belt line {@code super.visitFeatureCall} — byte-identical BY IDENTITY).
     * The residue (a receiver that does not lower) stays on the byte-proven L-109d relabel
     * ({@link #metaNavDrivenCount}), so the seat's total conserves as lowered + delegated by
     * construction — the L-111/{@link #inputFeatureNavLoweredCount} conversion pattern at the
     * meta seat. The standing driven-split receipt prints this counter beside the
     * delegated-seat dict (outside the Σ ≡ delegated braces — a LOWERED fact).
     */
    private int metaNavLoweredCount = 0;

    /** The #499 metaNav conversion's lowered-claim count (the relabel seat's flip receipt). */
    public int metaNavLoweredCount() {
        return metaNavLoweredCount;
    }

    /**
     * #515 — THE EMITTER-FRONTIER SERVE's lowered-claim count: claims the adapter LOWERED
     * but the leaf emitter declined (the {@code leafEmitter} decline site — the standing
     * 2,353 at the #514 SOT, decoded by the {@link #leafEmitterGapFacets} census), served at
     * the decline site through the decline's OWN fallback line ({@link #emitterGapServe}'s
     * per-raw-family {@code super.visitX} — every converted caller is
     * {@code tryEmitFromIR(...).orElseGet(() -> super.visitX(...))}, so the serve ≡ the
     * fallback BY IDENTITY; the #499 L-109d conversion argument at the LAST unconverted
     * site) and counted LOWERED (an IR lowering EXISTS — the #507
     * {@link #inputNavIdentityLoweredCount} accounting law; only the counter moves). The
     * census's {@code .served}-suffixed rows Σ-reconcile to this counter per cell BY
     * CONSTRUCTION. The unserved raw families keep the decline + the census names them
     * (the honest residue).
     */
    private int emitterServeLoweredCount = 0;

    /** The #515 emitter-frontier serve's lowered-claim count (the decline site's flip receipt). */
    public int emitterServeLoweredCount() {
        return emitterServeLoweredCount;
    }

    /**
     * #500 — the per-family serve counts of the generic {@link OracleRootRenderer} (the A+B
     * consumer/family cluster): keyed by the builder's family tokens — the arm-B family roots
     * ({@code conversion} / {@code pipe} / {@code listLiteral} / {@code onlyExists}) and the
     * CONTAINMENT-class roots ({@code navChain} / {@code equality} / {@code comparison} /
     * {@code logical} / {@code arithmetic} / {@code existence} / {@code callArgs} — RECUT at
     * #501 from the {@code meta}-prefixed #500 spellings when the consumer admissions widened
     * the contained-leaf set beyond meta to the shallow kinds; the #500 SOT values re-key 1:1)
     * plus the #501 {@code bareAttrMeta} disguised-meta at-root seat. Every serve is an
     * IR-LOWERED claim whose render is the literal {@code super.visitX} legacy line — the
     * standing driven-split receipt prints the Σ beside the other lowered receipts (outside the
     * delegated braces). {@link TreeMap} for deterministic receipt order.
     */
    private final Map<String, Integer> oracleRootLoweredByFamily = new TreeMap<>();

    /** The #500 oracle-root serve receipt: {@code family=count} tokens, ranked (the house format). */
    public String oracleRootLoweredBreakdown() {
        return rankedBreakdown(oracleRootLoweredByFamily);
    }

    /** The #500 oracle-root serve total (Σ {@link #oracleRootLoweredByFamily} — the lowered receipt). */
    public int oracleRootLoweredCount() {
        return oracleRootLoweredByFamily.values().stream().mapToInt(Integer::intValue).sum();
    }

    /**
     * Count of INPUT-FEATURE-RECEIVER navigations this compiler claimed for the IR path (L-112) — a feature-call
     * {@code (<function-input> -> <feature>) -> <next>} whose receiver chain bottoms out in a disguised-input-feature
     * {@link REnumValueRef} the fork's cascade fix typed, and which the adapter still declines
     * ({@code tryEmitFromIR} runs FIRST, so since the #478 arm the adapter-admissible slice of this population
     * lowers whole through the emitter and never reaches this relabel — the counter now reads the RESIDUE whose
     * base or hops the arm's gates defer, e.g. an alias/output head or a meta hop). The IR PATH claims the residue
     * by routing to {@code super.visitFeatureCall} (the EXACT current fallback — bytes provably unchanged) +
     * counting it driven: the multi-hop sibling of the L-111 leaf relabel. A declined→driven move
     * (denominator-neutral). §4.2-counted; a per-cell {@code > 0} assert pins THIS seam (anti-L-042; the pin's
     * tension re-read at the #478 ring — a fully-absorbed cell would retense it by design). FUNCTION-seam
     * population (rule bodies carry no function-input feature nav).
     */
    private int inputFeatureNavReceiverDrivenCount = 0;

    /** The L-112 input-feature-nav-receiver relabel firing count (feature-calls over a disguised input-nav, claimed). */
    public int inputFeatureNavReceiverDrivenCount() {
        return inputFeatureNavReceiverDrivenCount;
    }

    /**
     * Count of IMPLICIT-ATTRIBUTE navigations this compiler claimed for the IR path (L-113) — a bare non-meta
     * attribute reference ({@code foo}) legacy expands to an implicit input/item navigation. The adapter declines it
     * (the synthetic receiver is a gm-aware/AST-derived Java-synthesis decision the neutral IR cannot make — the L-029
     * split); legacy renders it via {@code super.visitSymbolReference}. The IR PATH claims the bare node by routing to
     * that exact fallback (bytes provably unchanged) + counting it driven — the same relabel class as the L-109e
     * rule-input case, the half L-109c WRONGLY dismissed as "already-driven/vacuous": a direct census (L-113 v5) proved
     * the synthetic does NOT drive ({@code drove}=0), so these are genuine un-driven nodes. Excludes meta attrs (a
     * gm-aware {@code FieldWithMetaX} retype — a separate, smaller residual). A declined→driven move (denominator-
     * neutral). §4.2-counted; per-cell {@code > 0} asserts pin THIS seam (anti-L-042). Fires on BOTH seams (the drr
     * RULE population, 741, is the largest — the first RULE-seam lever since L-109e).
     */
    private int implicitAttrNavDrivenCount = 0;

    /** The L-113 implicit-attribute relabel firing count (bare non-meta attribute refs legacy expands to an implicit nav, claimed). */
    public int implicitAttrNavDrivenCount() {
        return implicitAttrNavDrivenCount;
    }

    /**
     * Whether a declined feature-call's receiver chain bottoms out in a disguised-input-feature {@link REnumValueRef}
     * — the firing predicate for the L-112 relabel. Walks the {@link RFeatureCall} receiver hops down to the base; the
     * base (or any intermediate hop) being an {@code REnumValueRef} with {@code resolvedInputFeature} present means the
     * cascade fix typed an input-nav the adapter cannot lower, so the whole feature-call is a TYPED-but-Lab-gated nav.
     */
    private static boolean hasInputFeatureNavInReceiverChain(RFeatureCall fc) {
        RExpression recv = fc.receiver();
        while (true) {
            if (recv instanceof REnumValueRef enr && enr.resolvedInputFeature().isPresent()) {
                return true;
            }
            if (recv instanceof RFeatureCall inner) {
                recv = inner.receiver();
            } else {
                return false;
            }
        }
    }

    public IRExpressionCompiler(GeneratorModel generatorModel, JavaTypeTranslator typeTranslator,
                                JavaTypeUtil typeUtil) {
        super(generatorModel, typeTranslator, typeUtil);
    }

    /**
     * Wave-6 Phase-C expression-compiler "Step 0" (L-064) + slice-4 (L-065): supply the IR-routed
     * {@link IRCollectionHandler}, which drives the OPERAND-position {@code then}-chain {@code thenArg} hoist NAME
     * from the neutral ANF (the SEAM-1 {@code then} reached via {@link #visitThen}). The back-reference is kept so
     * the byte gate can read the slice-4 firing count. Mirrors L-060's renderer factory override. Invoked once
     * during {@code super()} — see the {@link #irCollectionHandler} field note on the init-order discipline.
     */
    @Override
    protected CollectionHandler createCollectionHandler() {
        this.irCollectionHandler = new IRCollectionHandler();
        return this.irCollectionHandler;
    }

    /** The IR-routed collection handler (slice-4 operand-then {@code thenArg} drive); its firing count feeds the byte gate. */
    public IRCollectionHandler irCollectionHandler() {
        return irCollectionHandler;
    }

    /**
     * Wave-6 Phase-C slice-5 (L-068): supply the IR-routed {@link IRLiteralHandler}, which drives the
     * {@code bigInteger} literal-hoist NAME from the neutral ANF (the 4th + last hoist family). The back-reference
     * is kept so the byte gate can read the slice-5 firing count. Mirrors {@link #createCollectionHandler()} (L-064);
     * invoked once during {@code super()} — see the {@link #irLiteralHandler} field note on the init-order
     * discipline (the no-initializer field).
     */
    @Override
    protected LiteralHandler createLiteralHandler() {
        this.irLiteralHandler = new IRLiteralHandler();
        return this.irLiteralHandler;
    }

    /** The IR-routed literal handler (slice-5 {@code bigInteger} drive); its firing count feeds the byte gate. */
    public IRLiteralHandler irLiteralHandler() {
        return irLiteralHandler;
    }

    /**
     * Wave-6 Phase-C slice-6: supply the IR-routed {@link IRControlFlowHandler}, which drives the
     * expression-compiler-reached {@code ifThenElseResult} hoist NAME (the ComparisonResult arm + the ctor
     * item-local arm — the §6 cross-group reorder carriers) from the neutral ANF. The back-reference is kept so the
     * byte gate can read the slice-6 firing count. Mirrors {@link #createCollectionHandler()} (L-064) /
     * {@link #createLiteralHandler()} (L-068); invoked once during {@code super()} — see the
     * {@link #irControlFlowHandler} field note on the init-order discipline (the no-initializer field).
     */
    @Override
    protected ControlFlowHandler createControlFlowHandler() {
        this.irControlFlowHandler = new IRControlFlowHandler();
        return this.irControlFlowHandler;
    }

    /** The IR-routed control-flow handler (slice-6 {@code ifThenElseResult} drive); its firing count feeds the byte gate. */
    public IRControlFlowHandler irControlFlowHandler() {
        return irControlFlowHandler;
    }

    @Override
    public JavaStatementBuilder visitStringLiteral(RStringLiteral expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitStringLiteral(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitBooleanLiteral(RBooleanLiteral expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitBooleanLiteral(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitNumberLiteral(RNumberLiteral expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitNumberLiteral(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitIntLiteral(RIntLiteral expr, ExpressionContext ctx) {
        // v3.1 flip seat 22 (facet intLiteralNumberSeat, LAW 77): the SAME predicate the legacy literal
        // seat reads (HandlerHelper.intLiteralConditionalArmExpectedType) at the IR claim ROOT - an int
        // literal at a null-expected conditional-arm seat whose ladder root infers number takes the
        // BigDecimal expected upstream would have threaded, so the IR leaf (IRJavaLeafEmitter.emitInt)
        // and the legacy fallback render the same MapperS.of(BigDecimal.valueOf(N)) on both routes.
        // tryEmitFromIR itself never reads the expected type; only the leaf emitters consult it.
        ExpressionContext effectiveCtx = ctx;
        if (ctx.expectedType() == null) {
            JavaType numberSeat = HandlerHelper.intLiteralConditionalArmExpectedType(
                    expr, getGeneratorModel(), getTypeTranslator(), getTypeUtil());
            if (numberSeat != null) {
                effectiveCtx = ctx.withExpected(numberSeat);
            }
        }
        final ExpressionContext intCtx = effectiveCtx;
        return tryEmitFromIR(expr, intCtx).orElseGet(() -> super.visitIntLiteral(expr, intCtx));
    }

    @Override
    public JavaStatementBuilder visitEmptyLiteral(REmptyLiteral expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitEmptyLiteral(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitSymbolReference(RSymbolReference expr, ExpressionContext ctx) {
        // References tier: the adapter lowers the bare function-parameter case (scalar →
        // MapperS.of(name), multi → MapperC.<Item>of(name)), the bare enum-value case
        // (→ EnumName.CONSTANT), the alias/shortcut reference (→ aliasName(inputs)), and the slice-1
        // function CALL (→ MapperS.of(receiver.evaluate(args)) for a plain RFunction with scalar args +
        // scalar single output); the remaining call/navigation shapes decline and fall back byte-identically.
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            if (hasNestedCallArg(expr)) {
                callNestedArgDrivenCount++;
            }
            if (hasItemArg(expr)) {
                callItemArgDrivenCount++;
            }
            return ir.get();
        }
        return super.visitSymbolReference(expr, ctx);
    }

    /**
     * Whether {@code ref} is a function call carrying a NESTED function-call argument — an {@link RSymbolReference}
     * argument that itself carries arguments ({@code f(g(x))}). The firing witness for the L-107 call-as-nested-arg
     * slice; a cheap AST check at the realized-drive point (safe as an oracle because it is only consulted on a
     * call that already drove, so the nested arg necessarily lowered to an {@link com.regnosys.rosetta.ir.expr.IRApply}).
     *
     * <p>Since the claim-root delegation (#471, widened to the singletonList arm at #473 and the intLiteral
     * arm at #474), a DELEGATED claim (rendered via legacy, not the adapter) also counts here, so the
     * adapter-lowered premise above no longer holds for every increment (the Seat-1 #471 OBS-1 note; the
     * #473 singletonList delegate — a call whose first arg is itself a call — is a live instance).
     * Diagnostic-only: the counter has no reader/gate consumer today — re-scope the premise (or skip
     * delegated renders) before wiring one.
     */
    private static boolean hasNestedCallArg(RSymbolReference ref) {
        for (RExpression arg : ref.args()) {
            if (arg instanceof RSymbolReference inner && !inner.args().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether {@code ref} is a function call carrying an implicit {@code item} argument ({@code someFunc(item)}) —
     * the firing witness for the L-108 call-as-item-arg slice. A cheap AST check at the realized-drive point (safe
     * because it is only consulted on a call that already drove, so the item arg was the filter/extract-bound,
     * typed, non-meta item the adapter admits).
     *
     * <p>Since the claim-root delegation (#471, widened to the singletonList arm at #473 and the intLiteral
     * arm at #474), a DELEGATED claim also counts here — including a META-typed item arg legacy hoists (the
     * ObservableQualification face), which the adapter never admitted — so the non-meta premise above no
     * longer holds for every increment (the Seat-1 #471 OBS-1 note). Diagnostic-only: no reader/gate
     * consumer today — re-scope the premise (or skip delegated renders) before wiring one.
     */
    private static boolean hasItemArg(RSymbolReference ref) {
        for (RExpression arg : ref.args()) {
            if (arg instanceof RImplicitVariable) {
                return true;
            }
        }
        return false;
    }

    @Override
    public JavaStatementBuilder visitSuperCall(RSuperCall expr, ExpressionContext ctx) {
        // super → super.doEvaluate(), emitted from IRReference{SUPER} (context-free).
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitSuperCall(expr, ctx));
    }

    /**
     * The #491 RImplicitVariable teach — the biggest untargeted family (7,274 = 147/545/877/5,705
     * drr-RULE at the #489 census) moves from the #475 count-then-super meter to the targeted set
     * (its untargeted count going to zero is the teach's own conservation signature, per the meter
     * javadoc). ONE seat, oracle-closed at both exits: the adapter lowers the node UNCONDITIONALLY
     * ({@code USER_ITEM}/{@code SYNTHETIC_ITEM} keyed on the raw {@code isSynthetic()}, the cache
     * type stamped) and the leaf emitter's two item arms render via the per-emission renderers —
     * for a claim whose ROOT is the item itself, {@code buildImplicitItemRenderer}/
     * {@code buildSyntheticItemRenderer} index exactly the claim node, the range correlates
     * identically, and the render is {@code getReferenceHandler().handle(node, ctx, this)} — the
     * LITERAL call {@code super.visitImplicitVariable} makes (legacy {@code ExpressionCompiler}
     * :433 is one line: {@code referenceHandler.handle(expr, ctx, this)}). Same method, same
     * handler instance, same arguments — byte-identical BY CONSTRUCTION at every exit: a driven
     * claim renders the oracle's bytes, a declined claim (the emitter type gates — the #491 probe
     * read the honest residue at 1,608 {@code typeMissing}, 1,607 of them render-time MINTS the
     * fixed point never typed) falls back to the same call. The #491 probe sized the claimable
     * mass at 5,666 typed (77.9% — lambda 3,738 + ruleTop 1,815 + switchTop 113); the facet bump
     * stays at this seat (probe-only) so the post-teach probe reads driven+declined ≡ Σ facets.
     */
    @Override
    public JavaStatementBuilder visitImplicitVariable(RImplicitVariable expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #491: the facet channel rides the visit seat (the #489 one-walk law — Σ facets ≡ the
            // family's whole visit population by construction), probe-only, teach-independent.
            implicitVisitFacets.merge(implicitVisitFacet(expr), 1, Integer::sum);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitImplicitVariable(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitEnumValueRef(REnumValueRef expr, ExpressionContext ctx) {
        // A qualified enum-value reference (SomeEnum -> VALUE) at SEAM-ROOT position (a whole function/rule output)
        // or inside a legacy-fallback parent — positions that bypass the IR until this override exists (L-069). The
        // adapter lowers a GENUINE ref to IRReference{ENUM_VALUE} (adaptEnumValueRef) and the emitter renders the
        // bare, null-typed EnumName.CONSTANT (emitEnumValue, byte-proven as an operand/arg, L-044/L-016); that render
        // is POSITION-INDEPENDENT (the SET/operand wrap is applied downstream, unchanged), so a seam-root ref is
        // byte-identical to the operand case by construction. The five disguised REnumValueRef channels are
        // really navigation/downcast; the L-111 precedence-winner slice carries the #496/#497 quiet
        // CONVERSION (adapter-first, residue on the byte-proven relabel), and since #505 the REMAINING
        // disguised residue is CONVERTED TOO (the L-109c exclusion RETIRED): every disguised root consults
        // tryEmitFromIR — a lowering claim drives (natively or through the #505 enumChain oracle leg), a
        // declining claim records its decline + blocker attribution and falls to the same legacy line, so
        // the whole seat joins the §4.2 population honestly (the L-052 new-family-share guard's premise
        // dissolves — the seam now claims at every disguised sub-channel; the #504 re-entrant law states
        // the denominator movement BOTH ways).
        if (!ExpressionToIRAdapter.isGenuineEnumValueRef(expr)) {
            // L-111 input-feature navigation: a disguised `<function-input> -> <feature>` REnumValueRef the
            // fork's cascade fix (PR #279) newly typed via the typing-only resolvedInputFeature channel (with
            // resolvedAttributeChain absent — the precedence winner, mirroring ExpressionTypeComputer.computeEnumValueRef).
            // Since the #496 leg-2 conversion (widened #497) this branch is adapter-FIRST: the quiet claim
            // renders the proven FieldAccess shapes natively (IR-LOWERED, inputFeatureNavLoweredCount); the
            // residue routes to the EXACT legacy renderer (super.visitEnumValueRef — bytes provably
            // unchanged, driven-DELEGATED on this seat's relabel counter) — the navigation analogue of the
            // meta-feature (L-109d) / implicit-input (L-109e) relabels. The attributeChain (implicit-ITEM)
            // bucket and every other disguised channel take the #505 SEAT CONVERSION below instead (the
            // L-109c exclusion retired — no relabel leg there: lowers drive, declines join counted).
            if (expr.resolvedInputFeature().isPresent() && expr.resolvedAttributeChain().isEmpty()) {
                if (blockerProbeEnabled) {
                    // #496: the leg-2 L-111 root census rides the relabel seat (Σ facets ≡ the
                    // seat's relabel count per cell by construction), probe-only — the
                    // delegated-seat shape read sizing the delegated → IR-LOWERED conversion.
                    inputNavRootFacets.merge(inputNavRootFacet(expr), 1, Integer::sum);
                    // #507: the arm-B residue census — the seat's DELEGATED slice one key deeper
                    // (guard arm + shape / full reason + meta qualifier; see l111ResidueFacets).
                    // Residue-only: the clean emitter-served slice takes no row, so Σ rows ≡ the
                    // seat's delegated count per cell (the cross-check the javadoc names).
                    String residue = l111ResidueFacet(expr);
                    if (residue != null) {
                        l111ResidueFacets.merge(residue, 1, Integer::sum);
                        if (!l111ResidueFirstSamples.containsKey(residue)) {
                            l111ResidueFirstSamples.put(residue, enclosingSampleToken(expr));
                        }
                    }
                }
                // #496 leg 2 — the L-111 CONVERSION (the honest-series flip, the census's
                // adapterLowers slice): QUIET adapter-first claim. The #478 adaptDisguisedInputNav
                // arm lowers the root to the typed single-hop FieldAccess over an IRVariable head
                // (the input-head shape whose emitter render is ring-proven at nested positions
                // AND at RFeatureCall claim roots — the SAME emission both routes share); the
                // emitter renders it natively → the event counts LOWERED, not delegated. The
                // gates, in order: the FieldAccess shape gate (#496 the IRVariable input-head
                // slice; #497 widened to the #492 ALIAS-head leg — the FieldAccess over an
                // IRReference{ALIAS} receiver, whose emitter render the #492 alias machinery
                // ring-proves at nested positions and the frame's alias resolver serves at this
                // root; any future shape stays on the relabel until its own oracle-closing), the
                // LIVE post-pin guard (an emitter-composed render takes NO literal-fallback
                // exemption — the census pre-sized the trip residue), then the emitter's own
                // gates inside emitViaLeafEmitter. EVERY decline falls through to the relabel
                // below WITHOUT any decline recording (the quiet contract — the seat's total
                // conserves as lowered + delegated; a declined-and-relabeled event would
                // double-count), so the residue keeps the byte-proven legacy render and the
                // delegated receipt exactly as pre-conversion.
                GeneratorModel quietGm = getGeneratorModel();
                RWorkspace quietWs = quietGm == null ? null : quietGm.workspace();
                Optional<IRExpr> navIr = quietWs == null ? Optional.empty()
                        : adapter.adapt(expr, quietWs);
                // v3.1 flip seat 21 — facet overrideChainWitness, LAW 77: a disguised input-nav that
                // is a call ARGUMENT whose last hop the override-chain selector base-witnesses
                // (NavigationHandler.disguisedArgIsOverrideBaseWitness — upstream's
                // findProperty(name, expectedType) at the call-argument seat) is a render decision
                // the emitter cannot take (it names the stamped LEAF); it takes the relabel below —
                // the literal legacy line — so both routes render the base through NavigationHandler.
                if (navIr.isPresent()
                        && navIr.get() instanceof FieldAccess fa
                        && (fa.receiver() instanceof IRVariable
                                || (fa.receiver() instanceof IRReference aliasRecv
                                        && aliasRecv.referenceKind() == IRReference.ReferenceKind.ALIAS))
                        // seat 30 law 1 rung 2: a RENDER-deciding seat — live scope, BINDING channel on.
                        && subtreeTripsPostPinCoercion(expr, ctx == null ? null : ctx.scope()) == null
                        && !NavigationHandler.disguisedArgIsOverrideBaseWitness(expr, this)) {
                    Optional<JavaStatementBuilder> emitted = emitViaLeafEmitter(expr, ctx, navIr.get());
                    if (emitted.isPresent()) {
                        irDrivenCount++;
                        inputFeatureNavLoweredCount++;
                        return emitted.get();
                    }
                }
                // #507 arm-B: the IDENTITY-serve leg — the seat's LOWERABLE residue (the
                // adapter's arm lowers the root but the quiet claim above did not fire: the
                // post-pin guard tripped — 143 at the #506 l111Residue census, ONE face:
                // aliasNavReceiverMeta over the alias-head shape — or the emitter declined,
                // zero live) renders the LITERAL relabel line below and counts LOWERED: the
                // render call is byte-identical BY IDENTITY (the same super call the relabel
                // makes), only the accounting moves — the guard exists to protect
                // emitter-COMPOSED renders and no compose happens here. The adapter-DECLINED
                // residue (279: the alias-head faces + headTypeMissing +
                // receiverNotExpressible) falls through to the relabel EXACTLY as
                // pre-conversion.
                if (navIr.isPresent()) {
                    JavaStatementBuilder served = super.visitEnumValueRef(expr, ctx);
                    irDrivenCount++;
                    inputNavIdentityLoweredCount++;
                    return served;
                }
                JavaStatementBuilder rendered = super.visitEnumValueRef(expr, ctx);
                irDrivenCount++;
                irDrivenDelegatedCount++;
                inputFeatureNavDrivenCount++;
                return rendered;
            }
            if (blockerProbeEnabled) {
                // #505: the seat census rides the CONVERTED residue seat (the #489 one-walk
                // law: Σ facets ≡ the seat's whole population by construction), probe-only.
                // Pre-conversion it sized the L-109c EXCLUSION; post-conversion it is the
                // conversion's own receipt (the lowers.<shape> slices = the claimable mass,
                // the blocked.<reason> slices = the joined declines).
                String seatToken = enumSeatRootFacet(expr);
                enumSeatRootFacets.merge(seatToken, 1, Integer::sum);
                if (!enumSeatRootFirstSamples.containsKey(seatToken)) {
                    enumSeatRootFirstSamples.put(seatToken, enclosingSampleToken(expr));
                }
            }
            // #505: THE SEAT CONVERSION — the L-109c exclusion RETIRED for the
            // ORACLE-SERVABLE shapes. The disguised residue consults the adapter first and
            // enters the claim pipeline exactly when the lowering is oracle-shaped
            // (isOracleRootShape — the IRClosureParam/IRSynItemNav/IRChoiceOptionNav/
            // IRDispatchInputRef-bearing chains, the IRSymbolNav roots — or an
            // IRMetaAccess root): the #505 enumChain oracle leg serves the claim through
            // the literal super.visitEnumValueRef line, byte-identical BY IDENTITY, and a
            // declining claim records its decline + blocker attribution inside
            // tryEmitFromIR (the events JOIN the denominator — the honest population law).
            // The ≥2-hop plain spines over synthetic-item/param bases (the seat census's
            // lowers.faFa rows) enter through the isEnumChainSpineRoot serve leg (the
            // emitter's direct spine render DIVERGED at this seat — drr 11+13 mismatches at
            // the first probe — so the serve is the byte truth for them too). The remaining
            // NATIVE-renderable residue (single-hop faVar and any future off-shape
            // lowering) deliberately does NOT enter — it keeps the pre-conversion
            // exclusion, disclosed exactly by the census; its own oracle-closing or a
            // proven native compose is the named next lever.
            GeneratorModel seatGm = getGeneratorModel();
            RWorkspace seatWs = seatGm == null ? null : seatGm.workspace();
            if (seatWs != null) {
                Optional<IRExpr> seatIr = adapter.adapt(expr, seatWs);
                if (seatIr.isPresent()
                        && (isOracleRootShape(seatIr.get())
                                || seatIr.get() instanceof IRMetaAccess
                                || isEnumChainSpineRoot(seatIr.get(), expr))) {
                    Optional<JavaStatementBuilder> chainIr = tryEmitFromIR(expr, ctx);
                    if (chainIr.isPresent()) {
                        enumChainRootDrivenCount++;
                        return chainIr.get();
                    }
                } else if (seatIr.isEmpty()) {
                    // The DECLINING slice joins the counted population (the honest
                    // conversion — no relabel leg: the decline falls to the same legacy
                    // line as pre-conversion; the blocker probe attributes it).
                    Optional<JavaStatementBuilder> declined = tryEmitFromIR(expr, ctx);
                    if (declined.isPresent()) {
                        // Contract-impossible (the adapter just declined and the belts are
                        // family-gated away from ENR) — never discard a render if it fires.
                        enumChainRootDrivenCount++;
                        return declined.get();
                    }
                }
            }
            return super.visitEnumValueRef(expr, ctx);
        }
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            enumValueRefDrivenCount++;
            return ir.get();
        }
        return super.visitEnumValueRef(expr, ctx);
    }

    @Override
    public JavaStatementBuilder visitComparison(RComparisonExpr expr, ExpressionContext ctx) {
        // First structural family: the adapter lowers a param/param ordered comparison to
        // BinaryOp (→ {method}(MapperS.of(l), MapperS.of(r), CardinalityOperator.All));
        // literal/enum/nav operands and explicit all/any modifiers decline to legacy. The
        // bare-item-operand slice also drives a comparison whose operand is the filter/extract item.
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            if (isBareItemOperand(expr.rawLeft()) || isBareItemOperand(expr.rawRight())) {
                itemOperandDrivenCount++;
            }
            if (isCallOperand(expr.rawLeft()) || isCallOperand(expr.rawRight())) {
                callCmpEqOperandDrivenCount++;
            }
            return ir.get();
        }
        return super.visitComparison(expr, ctx);
    }

    /**
     * Whether {@code operand} is the bare implicit {@code item} keyword (a non-synthetic {@link RImplicitVariable})
     * — the firing witness for the bare-item-operand slice. Counted only at a REALIZED present emission (so a
     * then/switch-bound item, which declines, never reaches the increment); the adapter's
     * {@code isFilterExtractBoundItemOperand} gate guarantees a present emission with a bare item operand was
     * filter/extract-bound. Null-safe (a then-body-elided operand is null — the adapter already declined it).
     */
    private static boolean isBareItemOperand(RExpression operand) {
        return operand instanceof RImplicitVariable iv && !iv.isSynthetic();
    }

    /**
     * Whether {@code operand} is a function CALL — an {@link RSymbolReference} carrying arguments — the firing
     * witness for the L-105 call-as-operand slices. A cheap AST check at the realized-drive point; safe as a
     * firing oracle because it is only consulted on a node that already drove (so the call necessarily lowered to
     * an {@link com.regnosys.rosetta.ir.expr.IRApply}, the new admission). Null-safe.
     */
    private static boolean isCallOperand(RExpression operand) {
        return operand instanceof RSymbolReference ref && !ref.args().isEmpty();
    }

    @Override
    public JavaStatementBuilder visitArithmetic(RArithmeticExpr expr, ExpressionContext ctx) {
        // Arithmetic (+ - * /) over two scalar params of the SAME numeric kind lowers to BinaryOp{ADD|SUB|
        // MUL|DIV} (→ MapperMaths.<R,O,O>method(MapperS.of(l), MapperS.of(r))); the emitter classifies the
        // Integer/BigDecimal witnesses and declines a mixed-kind or non-numeric (string-concat) pair. The
        // unary +/- form, literal/nav/count operands and nested arithmetic decline to legacy.
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitArithmetic(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitEquality(REqualityExpr expr, ExpressionContext ctx) {
        // Equality joins the structural tier: a param/param `=`/`<>` lowers to BinaryOp{EQ|NEQ}
        // (→ areEqual(…, CardinalityOperator.All) / notEqual(…, CardinalityOperator.Any), the
        // operator-dependent cardinality default); literal/enum/nav operands and explicit
        // all/any modifiers decline to legacy. The bare-item-operand slice also drives an
        // equality whose operand is the filter/extract item (e.g. `item = SomeEnum -> VALUE`).
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            if (isBareItemOperand(expr.rawLeft()) || isBareItemOperand(expr.rawRight())) {
                itemOperandDrivenCount++;
            }
            if (isCallOperand(expr.rawLeft()) || isCallOperand(expr.rawRight())) {
                callCmpEqOperandDrivenCount++;
            }
            return ir.get();
        }
        return super.visitEquality(expr, ctx);
    }

    @Override
    public JavaStatementBuilder visitLogical(RLogicalExpr expr, ExpressionContext ctx) {
        // Logical completes the boolean binary tier: an and/or whose operands BOTH lower to a
        // ComparisonResult-producing node (comparison/equality/nested logical/existence) lowers to
        // BinaryOp{AND|OR} (→ left.andNullSafe(right) / left.orNullSafe(right)); operands that
        // don't render as a ComparisonResult (bare boolean vars/literals, boolean-function calls,
        // then-chains) decline to legacy.
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitLogical(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitExistence(RExistenceExpr expr, ExpressionContext ctx) {
        // First unary structural family: a scalar-parameter existence check lowers to Existence
        // (→ exists/notExists/singleExists/multipleExists(MapperS.of(name))) — a ComparisonResult,
        // so it also composes through visitLogical. multi/navigation operands and the
        // then/filter-elided null argument decline to legacy; the bare-item-operand slice now drives
        // `item exists` / `item is absent` whose operand is the filter/extract item.
        // v3.2 seat 9 (PR #630, F8 / D47 - the `nothing` render law, LAW 77): a Void-item operand is the EMPTY
        // mapper (`exists(MapperS.<Void>ofNull())` - upstream coerces the operand before the call) where this
        // route's scalar-parameter claim would render `exists(MapperS.of(t))`: the oracle group
        // void-mapping-render-edge's InputExists / InputAbsent / InputSingleExists. The operand DECLINES to the
        // shared handler, whose Void arm is the ONE site on both routes (ExistenceHandler.voidOperandEmptyOrNull -
        // the coercer's own predicate over the front-end type).
        // Round 1 (cq SF-5): the EXPLICIT-argument form alone - the same node the handler reads (identity), no second
        // synthetic implicit minted here; the elided form's synthetic operand has no inference and the handler's arm
        // declines it too (the implicit-operand family never reaches the Void law on either route - BANKED).
        if (expr.argument() != null
                && com.regnosys.rosetta.generator.java.expression.handlers.ExistenceHandler.voidOperandEmptyOrNull(
                        expr.argument(), this) != null) {
            return super.visitExistence(expr, ctx);
        }
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            if (isBareItemOperand(expr.argument())) {
                itemOperandDrivenCount++;
            }
            if (isCallOperand(expr.argument())) {
                callExistenceOperandDrivenCount++;
            }
            return ir.get();
        }
        return super.visitExistence(expr, ctx);
    }

    @Override
    public JavaStatementBuilder visitFeatureCall(RFeatureCall expr, ExpressionContext ctx) {
        // First navigation family: a single-hop `scalarParam -> feature` (resolved, single,
        // non-meta) lowers to FieldAccess (→ receiver.<Witness>map("getX", v -> v.getX())), the
        // lambda var disambiguated against the SAME ctx.scope() the legacy handler uses. Unresolved/
        // record/meta/multi features, navigation chains and non-scalar-param receivers decline to legacy.
        // The extract/filter percolation slice also drives an item-rooted nav chain (the receiver bottoms out
        // in the filter/extract lambda's `item`); count those top-level drives as the seam's firing witness.
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            if (isItemRootedFeatureCall(expr)) {
                itemNavDrivenCount++;
            }
            if (isCallRootedFeatureCall(expr)) {
                callBaseNavDrivenCount++;
            }
            return ir.get();
        }
        return super.visitFeatureCall(expr, ctx);
    }

    /**
     * Whether a feature-call chain bottoms out in a function CALL — its base receiver, after walking through any
     * intervening feature-call hops, is an {@link RSymbolReference} carrying arguments. The firing predicate for the
     * call-as-nav-base slice (a cheap AST check at the realized-drive point).
     */
    private static boolean isCallRootedFeatureCall(RFeatureCall fc) {
        RExpression recv = fc.receiver();
        while (recv instanceof RFeatureCall inner) {
            recv = inner.receiver();
        }
        return recv instanceof RSymbolReference ref && !ref.args().isEmpty();
    }

    /**
     * Whether a feature-call chain bottoms out in the implicit {@code item} — its base receiver, after walking
     * through any intervening feature-call hops, is an {@link RImplicitVariable}. The firing predicate for the
     * extract/filter item-nav slice (a cheap AST check at the realized-drive point — no dependence on speculative
     * adapter state).
     */
    private static boolean isItemRootedFeatureCall(RFeatureCall fc) {
        RExpression recv = fc.receiver();
        while (recv instanceof RFeatureCall inner) {
            recv = inner.receiver();
        }
        return recv instanceof RImplicitVariable;
    }

    @Override
    public JavaStatementBuilder visitListOp(RListOpExpr expr, ExpressionContext ctx) {
        // Flat postfix list ops (distinct/flatten/first/last/reverse, + only-element since the #498
        // teach — its inline `.get()` collapse and selfUnwrapping consumer-marker ride the oracle's
        // own JavaExpression) lower to IRListOp when the receiver subtree itself lowers; the render
        // reuses CollectionHandler verbatim (L-050). Sum (a deeper-diff render) and non-lowering
        // receivers (alias/item, L-032) decline.
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitListOp(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitCount(RCountExpr expr, ExpressionContext ctx) {
        // `<arg> count` lowers to IRListOp{COUNT} (→ <arg>.resultCount()) when the receiver lowers (L-050).
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitCount(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitToString(RToStringExpr expr, ExpressionContext ctx) {
        // `<arg> to-string` lowers to IRToString when the receiver subtree lowers (L-070); the render reuses
        // ConversionHandler.handle(RToStringExpr) verbatim via the installed ToStringRenderer (byte-identical to
        // Path-1 by construction — the same call super.visitToString makes). toStringDrivenCount is the dedicated
        // firing oracle (the global driven>0 gate cannot catch this §4.2-counted seam regressing to zero).
        Optional<JavaStatementBuilder> ir = tryEmitFromIR(expr, ctx);
        if (ir.isPresent()) {
            toStringDrivenCount++;
            return ir.get();
        }
        return super.visitToString(expr, ctx);
    }

    // ------------------------------------------------------------------------------------------------
    // The #475 UNTARGETED census meters: the 2 visit* families with NO IR attempt (21 at birth;
    // moved to the targeted set: RImplicitVariable at #491, RConstructorExpr at #494,
    // RConditionalExpr at #495, RExtractExpr + RFilterExpr at #496, RListLiteral / RThenExpr /
    // ROnlyExistsExpr / RConversionExpr at the #500 arm-B sweep, RDeepFeatureCall at #507,
    // RSwitchExpr / RDefaultExpr / RContainsExpr / RDisjointExpr / RMaxExpr / RMinExpr /
    // RSortExpr at the #513 noAdaptArm sweep, and RWithMetaExpr / RJoinExpr at the #515
    // untargeted close — the conservation signature each time; the
    // remaining meters are RReduceExpr / RCardinalityCheckExpr).
    // Each override is
    // count-then-super — the counter bump plus the EXACT call the un-overridden virtual dispatch would
    // have made — so the render is byte-identical by construction (no IR attempt, no speculative state,
    // no new control flow; the strongest inertness argument, stronger even than the delegation seats'
    // — there is no seat here to un-fire). The point: these families were invisible to the §4.2 dial
    // (never attempted, never counted), so the road to a 100% IR-driven walk had an UNSIZED half; the
    // census makes the whole worklist measurable on one D11 ON run ({@link #untargetedVisitBreakdown}).
    // A family taught later moves from this meter to the targeted set by gaining a tryEmitFromIR
    // override — its count here going to zero is the teach's own conservation signature.
    // ------------------------------------------------------------------------------------------------

    /**
     * The #500 arm-B list-literal claim (seat 4 of 4) — the DEEP, adapter-ready
     * {@link com.regnosys.rosetta.ir.expr.IRListConstruct} (the standing all-or-nothing element
     * recursion; the census read 327 of 398 lowering) served by the generic oracle-root
     * renderer's {@code super.visitListLiteral(site, ctx)} (routing to
     * {@code literalHandler.handle}) — see {@link #visitThen}. The kind has NO native emitter
     * arm, so an interior list construct under a native root keeps declining at the emit
     * chain's end exactly as pre-teach.
     */
    @Override
    public JavaStatementBuilder visitListLiteral(RListLiteral expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #500: the arm-B listLitVisit census — see visitThen.
            bumpVisitCensus(listLitVisitFacets, listLitVisitFirstSamples,
                    listLitVisitFacet(expr), expr);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitListLiteral(expr, ctx));
    }

    /**
     * The #513 noAdaptArm-sweep visit conversions (default here; contains/disjoint below,
     * sort/max/min and switch at their declaration-order seats): each family joins the
     * TARGETED set (tryEmitFromIR first — the untargeted meter's row going to ZERO is the
     * teach's own conservation signature; the #507 visit-conversion precedent). A lowered
     * root (the adapter's shape-gated shallow arm) serves through the frame's per-family
     * oracle leg — the literal {@code super.visitX} line, byte-identical BY IDENTITY, whose
     * legacy re-walk visits every interior node at its own seat (the #504 re-entrant law); a
     * declining root falls to the same super call with the decline counted and
     * blocker-attributed (the honest population law — the events were already
     * attempted-population members via the untargeted meter, so the denominator is
     * unchanged).
     */
    @Override
    public JavaStatementBuilder visitDefault(RDefaultExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitDefault(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitContains(RContainsExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitContains(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitDisjoint(RDisjointExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitDisjoint(expr, ctx));
    }

    // #515: join joins the TARGETED set (the untargeted-close visit conversions — the
    // #513 noAdaptArm-sweep pattern, see visitDefault; the adapter's unconditional
    // IRJoinOp arm serves through the frame's joinOp oracle leg BY IDENTITY).
    @Override
    public JavaStatementBuilder visitJoin(RJoinExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitJoin(expr, ctx));
    }

    /**
     * The #507 arm-A visit conversion: the family joins the TARGETED set (tryEmitFromIR first —
     * the untargeted meter's RDeepFeatureCall row going to ZERO is the teach's own conservation
     * signature; the #500 arm-B family-seat precedent). A lowered root (the adapter's new
     * {@code adaptDeepFeatureCall} arm — the {@link com.regnosys.rosetta.ir.expr.IRDeepFeatureNav}
     * mint) serves through the frame's {@code deepChain} oracle leg — the literal
     * {@code super.visitDeepFeatureCall} line, byte-identical BY IDENTITY; a declining root
     * falls to the same super call with the decline counted and blocker-attributed (the honest
     * population law — the events were already attempted-population members via the untargeted
     * meter, so the denominator is unchanged).
     */
    @Override
    public JavaStatementBuilder visitDeepFeatureCall(RDeepFeatureCall expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #507: the arm-A root-seat census — the root population's shape read (see the
            // deepVisitFacets javadoc); probe-only.
            bumpVisitCensus(deepVisitFacets, deepVisitFirstSamples, deepVisitFacet(expr), expr);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitDeepFeatureCall(expr, ctx));
    }

    /**
     * The #491 facet classifier for {@link #implicitVisitFacets} — see the field javadoc for the
     * grammar. Kind reads the raw node's {@code isSynthetic()} (the adapter's own
     * {@code VariableKind} key), typing reads the fixed-point cache exactly as the leaf emitter's
     * type gates would ({@code getInferredType} returns the non-null MISSING sentinel for any
     * node the fixed point never saw — minted nodes always read {@code typeMissing}), and the
     * context walk names the binding seat. {@code noWorkspace} is the contract-impossible belt
     * (the D11 run always carries a generator model; a standalone-compiler probe run without one
     * still returns a stable token, never an NPE — the declineReason robustness convention).
     */
    private String implicitVisitFacet(RImplicitVariable expr) {
        String kind = expr.isSynthetic() ? "synthetic" : "user";
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "implicitVisit." + kind + ".noWorkspace";
        }
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        return "implicitVisit." + kind + "." + typing + "." + implicitVisitContext(expr);
    }

    /**
     * The context leg of {@link #implicitVisitFacet}: {@code minted} FIRST (the #486 single-hop
     * child-linkage mechanism — an UP-only parented node is a render-time synthesis; every
     * {@code ReferenceHandler}/{@code HandlerHelper} mint seat {@code setParent}s the fresh node
     * without child-linking it, so parse-tree membership is exactly identity-membership in the
     * parent's {@code children()}), then the #485-convention bounded parent walk naming the
     * nearest binding seat: {@code lambda} (an {@link RInlineFunction} below any root — the
     * filter/extract binder context), {@code switchTop} / {@code condTop} on their carriers,
     * {@code ruleTop} / {@code fnTop} at the roots, {@code detached} for a parentless node
     * (fixture shapes; corpus nodes always carry parents), {@code other} on walk-out.
     */
    private static String implicitVisitContext(RImplicitVariable expr) {
        RNode parent = expr.parent();
        if (parent == null) {
            return "detached";
        }
        boolean linked = false;
        for (RNode child : parent.children()) {
            if (child == expr) {
                linked = true;
                break;
            }
        }
        if (!linked) {
            return "minted";
        }
        RNode cur = parent;
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return "lambda";
            }
            if (cur instanceof RSwitchExpr) {
                return "switchTop";
            }
            if (cur instanceof RCondition) {
                return "condTop";
            }
            if (cur instanceof RRule) {
                return "ruleTop";
            }
            if (cur instanceof RFunction) {
                return "fnTop";
            }
            cur = cur.parent();
        }
        return "other";
    }

    /**
     * The #494 facet classifier for {@link #ctorVisitFacets} — see the field javadoc for the
     * grammar. Target mirrors {@code tryTypedBuilderBlock}'s entry reads verbatim
     * ({@code resolveTypeCall} → data/choice, the typed-block decline set), typing reads the
     * fixed-point cache exactly as the leaf emitter's type gates would, and the values leg
     * re-adapts each pair value bottom-up exactly like the #476 blocker probe's
     * {@code collectMinimalBlockers} walk. {@code noWorkspace} is the contract-impossible belt
     * (the D11 run always carries a generator model; a standalone-compiler probe run without one
     * still returns a stable token, never an NPE — the declineReason robustness convention).
     */
    private String ctorVisitFacet(RConstructorExpr expr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "ctorVisit.noWorkspace";
        }
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String facet = "ctorVisit." + ctorVisitTarget(expr, gm) + "." + typing + "."
                + ctorVisitValues(expr, ws);
        return expr.isSpread() ? facet + ".spread" : facet;
    }

    /**
     * The target leg of {@link #ctorVisitFacet}: the SAME resolution legacy
     * {@code ConstructionHandler.tryTypedBuilderBlock} keys its entry on —
     * {@code gm.resolveTypeCall(typeCall)} to {@code RDataTypeRef} ({@code data}) or
     * {@code RChoiceTypeRef} ({@code choice}); any other resolution ({@code otherType}) or a
     * missing type call ({@code noTypeCall}) is the typed block's own decline set (legacy falls
     * to the one-line placeholder there), so the face predicts the golden-form boundary.
     */
    private static String ctorVisitTarget(RConstructorExpr expr, GeneratorModel gm) {
        if (expr.typeCall() == null) {
            return "noTypeCall";
        }
        RType resolved = gm.resolveTypeCall(expr.typeCall());
        if (resolved instanceof RDataTypeRef) {
            return "data";
        }
        if (resolved instanceof RChoiceTypeRef) {
            return "choice";
        }
        return "otherType";
    }

    /**
     * The values leg of {@link #ctorVisitFacet}: {@code valuesLowerable} when EVERY pair value
     * lowers through {@code adapter.adapt} TODAY (the deep-{@code IRConstruct} admission read),
     * else {@code valueBlocked.<Family>} naming the FIRST source-order declining value's node
     * family (one token per visit keeps Σ facets ≡ the flat count; the ranked census over the
     * first-blocker convention mirrors the #476 minimal-blocker channel), {@code noPairs} for a
     * pair-free constructor. A {@code null} pair value (contract-impossible on parsed corpus
     * nodes) reads {@code valueBlocked.nullValue} — the robustness belt, never an NPE.
     */
    private String ctorVisitValues(RConstructorExpr expr, RWorkspace ws) {
        if (expr.pairs().isEmpty()) {
            return "noPairs";
        }
        for (RKeyValuePair pair : expr.pairs()) {
            RExpression value = pair.value();
            if (value == null) {
                return "valueBlocked.nullValue";
            }
            if (adapter.adapt(value, ws).isEmpty()) {
                return "valueBlocked." + value.getClass().getSimpleName();
            }
        }
        return "valuesLowerable";
    }

    /**
     * The #495 facet classifier for {@link #condVisitFacets} — see the field javadoc for the
     * grammar. Shape reads {@link #hasGenuineElseShape}, typing reads the fixed-point cache
     * exactly as the leaf emitter's type gates would, and the lower leg takes the ROOT
     * {@code adapter.adapt} verdict (the drift-proof read: {@code adaptConditional}'s own D1
     * recursion decides, never a probe restatement of it) with the per-slot walk attributing a
     * decline. {@code noWorkspace} is the contract-impossible belt (the D11 run always carries a
     * generator model; a standalone-compiler probe run without one still returns a stable token,
     * never an NPE — the declineReason robustness convention).
     */
    private String condVisitFacet(RConditionalExpr expr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "condVisit.noWorkspace";
        }
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String shape = hasGenuineElseShape(expr) ? "else" : "noElse";
        return "condVisit." + shape + "." + typing + "." + condVisitLower(expr, ws);
    }

    /**
     * The lower leg of {@link #condVisitFacet}: {@code adapterLowers[.guardTrip]} when the WHOLE
     * conditional lowers through {@code adapter.adapt} TODAY (the {@code .guardTrip} suffix when
     * {@link #subtreeTripsPostPinCoercion} would decline the claim — the #494 exemption sizing),
     * else {@code blocked.<slot>.<Family>} naming the first source-order declining slot (the
     * adapter's own recursion order: condition → then → genuine else) by its expression's node
     * family (one token per visit keeps Σ facets ≡ the flat count; the shallow slot-family
     * convention mirrors the ctorVisit values leg).
     *
     * <p>The slot probes re-adapt each slot at the public entry's ROOT position, but the real
     * recursion adapts them at CHILD positions — and the #494 constructor arm is the one
     * position-gated arm ({@code NodeId.path().isEmpty()}), so the two verdicts diverge exactly
     * when a slot IS a constructor: {@code blocked.ctorSlot.<slot>} attributes that class (the
     * first probe cut read it at 320 of 1,133 — a live class pre-arm; the #495 admission then
     * claimed it whole, so post-arm the face reads ZERO and stands as the admission's regression
     * belt). Since #496 the ONE-LEVEL-NESTED admission classes get their own faces
     * ({@code blocked.condCtorSlot.<slot>} / {@code blocked.lambdaCtorSlot.<slot>} — a slot that
     * is itself a ctor-slot conditional or a ctor-body lambda lowers at the slot re-adapt's root
     * via its own claim-root admission yet declines at this conditional's child position; the
     * #495 {@code rootOnly} belt fired its first live event on the #496 arm's first run and
     * decoded to exactly this class — an else-slot nested ctor conditional unmasked when the
     * lambda arm made the then-slot lower). The residue belts:
     * {@code blocked.<slot>.nullSlot} (parse-robustness) and {@code blocked.rootOnly} (a root
     * decline nothing above explains — contract-impossible while those are the only
     * position-gated behaviours; a future position-gated kind lands here VISIBLY rather than
     * mis-attributed). Stable tokens, never an NPE.
     */
    private String condVisitLower(RConditionalExpr expr, RWorkspace ws) {
        if (expr.condition() == null) {
            return "blocked.cond.nullSlot";
        }
        if (expr.thenBranch() == null) {
            return "blocked.then.nullSlot";
        }
        if (adapter.adapt(expr, ws).isPresent()) {
            return subtreeTripsPostPinCoercion(expr) != null ? "adapterLowers.guardTrip" : "adapterLowers";
        }
        if (adapter.adapt(expr.condition(), ws).isEmpty()) {
            return "blocked.cond." + expr.condition().getClass().getSimpleName();
        }
        if (adapter.adapt(expr.thenBranch(), ws).isEmpty()) {
            return "blocked.then." + expr.thenBranch().getClass().getSimpleName();
        }
        if (hasGenuineElseShape(expr) && adapter.adapt(expr.elseBranch().get(), ws).isEmpty()) {
            return "blocked.else." + expr.elseBranch().get().getClass().getSimpleName();
        }
        // Every slot lowers as a ROOT yet the conditional declines: the slot's real adapt runs at
        // a CHILD position, where the #494 ctor arm (the one position-gated arm) declines — so a
        // slot that IS a constructor is the divergence, attributed first-slot-first.
        if (expr.condition() instanceof RConstructorExpr) {
            return "blocked.ctorSlot.cond";
        }
        if (expr.thenBranch() instanceof RConstructorExpr) {
            return "blocked.ctorSlot.then";
        }
        if (hasGenuineElseShape(expr) && expr.elseBranch().get() instanceof RConstructorExpr) {
            return "blocked.ctorSlot.else";
        }
        // #496: the ONE-LEVEL-NESTED admission classes (the #495 rootOnly belt's first live fire,
        // decoded on the #496 arm's first run): a slot that is itself a ctor-slot CONDITIONAL or
        // a ctor-body LAMBDA lowers at the slot re-adapt's root (its own claim-root admission
        // fires there) but declines at this conditional's child position — the cdm6 event
        // (blocked.condCtorSlot.else: an else-slot nested conditional carrying a direct ctor,
        // unmasked when the #496 lambda arm made the then-slot's extract lower).
        if (isCondWithDirectCtorSlot(expr.condition())) {
            return "blocked.condCtorSlot.cond";
        }
        if (isCondWithDirectCtorSlot(expr.thenBranch())) {
            return "blocked.condCtorSlot.then";
        }
        if (hasGenuineElseShape(expr) && isCondWithDirectCtorSlot(expr.elseBranch().get())) {
            return "blocked.condCtorSlot.else";
        }
        if (isLambdaWithDirectCtorBody(expr.condition())) {
            return "blocked.lambdaCtorSlot.cond";
        }
        if (isLambdaWithDirectCtorBody(expr.thenBranch())) {
            return "blocked.lambdaCtorSlot.then";
        }
        if (hasGenuineElseShape(expr) && isLambdaWithDirectCtorBody(expr.elseBranch().get())) {
            return "blocked.lambdaCtorSlot.else";
        }
        return "blocked.rootOnly";
    }

    /**
     * The #496 facet classifier for {@link #lambdaVisitFacets} — see the field javadoc for the
     * grammar. Binder reads the body {@link RInlineFunction}'s own three spellings, typing reads
     * the fixed-point cache exactly as the leaf emitter's type gates would, and the lower leg
     * takes the ROOT {@code adapter.adapt} verdict first (since the #496 arm — the drift-proof
     * read) with the piece walk attributing declines in recursion order (receiver, then lambda
     * body) and the position-divergent classes named exactly as {@link #condVisitLower} names
     * its own. {@code noWorkspace} is the contract-impossible belt (the declineReason
     * robustness convention — a stable token, never an NPE).
     */
    private String lambdaVisitFacet(String family, RExpression expr, RExpression argument,
            RInlineFunction body) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "lambdaVisit." + family + ".noWorkspace";
        }
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        // isImplicit FIRST: the ImplicitVariableRule derived-state pass injects a SYNTHETIC
        // "item" parameter into every bare implicit lambda, so paramNames alone cannot
        // distinguish a genuine named binder from the implicit binding (the probe cut-1 read
        // conflated them — the cut-2 recut).
        String binder = body == null ? "noBody"
                : body.isImplicit() ? "implicitBare"
                        : !body.paramNames().isEmpty() ? "named" : "implicitBracket";
        return "lambdaVisit." + family + "." + binder + "." + typing + "."
                + lambdaVisitLower(expr, argument, body, ws);
    }

    /**
     * The lower leg of {@link #lambdaVisitFacet}: {@code lowers[.guardTrip]} when the WHOLE
     * lambda lowers through the ROOT {@code adapter.adapt} verdict (the drift-proof read —
     * {@code adaptLambdaOp}'s own D1 recursion decides, never a probe restatement; since the
     * #496 arm this includes the claim-root ctor-body admission, so the probe cut-1's
     * {@code blocked.ctorSlot.body} class — 194 events, read BEFORE the arm — now reads
     * {@code lowers} and the ctorSlot faces stand as the admission's regression belts, the #495
     * convention exactly; the {@code .guardTrip} suffix stays the counterfactual
     * {@link #subtreeTripsPostPinCoercion} scan — the #496 LAMBDA_OP exemption claims those
     * roots, the suffix keeps sizing the exemption's standing obligation), else the piece walk
     * attributes the decline in recursion order — {@code blocked.recv.<Family>} /
     * {@code blocked.body.<Family>} name the first declining piece by its expression's node
     * family ({@code nullSlot} the parse-robustness belts), then the position-divergent classes a
     * root re-adapt of the pieces cannot see ({@code blocked.ctorSlot.<piece>} — a direct
     * {@link RConstructorExpr} piece lowers at the probe root while the arm's RECEIVER position
     * keeps the #494 root-only gate; {@code blocked.condCtorSlot.<piece>} — a direct
     * {@link RConditionalExpr} piece with a direct constructor slot, admitted at ITS claim root
     * only), the {@code blocked.multiParamBinder} belt, and {@code blocked.rootOnly} — a root
     * decline nothing above explains, contract-impossible while those are the only
     * position-gated behaviours (a future one lands here VISIBLY). Stable tokens, never an NPE.
     */
    private String lambdaVisitLower(RExpression expr, RExpression argument, RInlineFunction body,
            RWorkspace ws) {
        if (argument == null) {
            return "blocked.recv.nullSlot";
        }
        if (body == null || body.body() == null) {
            return "blocked.body.nullSlot";
        }
        if (adapter.adapt(expr, ws).isPresent()) {
            return subtreeTripsPostPinCoercion(expr) != null ? "lowers.guardTrip" : "lowers";
        }
        if (adapter.adapt(argument, ws).isEmpty()) {
            return "blocked.recv." + argument.getClass().getSimpleName();
        }
        if (adapter.adapt(body.body(), ws).isEmpty()) {
            return "blocked.body." + body.body().getClass().getSimpleName();
        }
        // Every piece lowers as a ROOT yet the lambda declines: the piece's real adapt runs at a
        // CHILD position — name the one-level position-divergent classes (the #494 root-only
        // ctor arm at the RECEIVER position; the #495/#496 claim-root-gated admissions one level
        // down), then the structural belts.
        if (argument instanceof RConstructorExpr) {
            return "blocked.ctorSlot.recv";
        }
        if (body.body() instanceof RConstructorExpr) {
            return "blocked.ctorSlot.body";
        }
        if (isCondWithDirectCtorSlot(argument)) {
            return "blocked.condCtorSlot.recv";
        }
        if (isCondWithDirectCtorSlot(body.body())) {
            return "blocked.condCtorSlot.body";
        }
        if (isLambdaWithDirectCtorBody(argument)) {
            return "blocked.lambdaCtorSlot.recv";
        }
        if (isLambdaWithDirectCtorBody(body.body())) {
            return "blocked.lambdaCtorSlot.body";
        }
        if (!body.isImplicit() && body.paramNames().size() > 1) {
            return "blocked.multiParamBinder";
        }
        return "blocked.rootOnly";
    }

    /**
     * Whether {@code e} is a conditional carrying a DIRECT {@link RConstructorExpr} in any slot —
     * the #495 claim-ROOT ctor-slot admission's beneficiary shape, position-divergent at the
     * probe's root re-adapt (see {@link #lambdaVisitLower}). The synthetic empty else
     * ({@code DefaultElseRule}) is never a constructor, so the raw three-slot check is safe
     * without the genuine-else split.
     */
    private static boolean isCondWithDirectCtorSlot(RExpression e) {
        return e instanceof RConditionalExpr cond
                && (cond.condition() instanceof RConstructorExpr
                        || cond.thenBranch() instanceof RConstructorExpr
                        || cond.elseBranch().map(el -> el instanceof RConstructorExpr).orElse(false));
    }

    /**
     * Whether {@code e} is an extract/filter carrying a DIRECT {@link RConstructorExpr} body —
     * the #496 claim-ROOT body-slot admission's beneficiary shape, position-divergent at a probe
     * re-root exactly like {@link #isCondWithDirectCtorSlot} (the adapter's
     * {@code isCtorBodyLambda} mirror twin).
     */
    private static boolean isLambdaWithDirectCtorBody(RExpression e) {
        RInlineFunction body = e instanceof RExtractExpr extract ? extract.body()
                : e instanceof RFilterExpr filter ? filter.body() : null;
        return body != null && body.body() instanceof RConstructorExpr;
    }

    /**
     * The #496 binder-gate cross-read classifier ({@link #binderGateCrossFacets}) — see the field
     * javadoc for the grammar. Self-gating on the five gate spellings (every other reason token
     * returns without a bump); the composed faces match by suffix (the #478 channel law — an arm's
     * twin composes its own gate onto the channel token, and the cross-read keys the GATE so its
     * buckets stay comparable across any arm landing).
     */
    private void classifyBinderGateBlocker(RExpression expr, String reason) {
        String gate;
        if ("noFilterExtractBinder".equals(reason)) {
            gate = "bareAttr";
        } else if (reason.endsWith(".noFilterExtractBinder")) {
            gate = "attrChain";
        } else if ("itemNotFilterExtractBound".equals(reason)
                || reason.endsWith(".itemNotFilterExtractBound")) {
            gate = "itemBound";
        } else if ("receiverSyntheticItem".equals(reason)) {
            gate = "syntheticItem";
        } else if ("opOnlyElement".equals(reason)) {
            // The knock-on face: the ONLY_ELEMENT operator (deferred until the #498 teach — the
            // reason token no longer mints, so this face reads zero post-#498 and stands as the
            // drift belt), attributed by its RECEIVER family — the extract/filter-receiver slice
            // was the mass whose one REMAINING gate was the operator itself once leg 1 lowered
            // the receiver.
            RExpression arg = expr instanceof RListOpExpr op ? op.argument() : null;
            binderGateCrossFacets.merge("binderGate.onlyElement.arg."
                    + (arg == null ? "nullSlot" : arg.getClass().getSimpleName()), 1, Integer::sum);
            return;
        } else {
            return;
        }
        binderGateCrossFacets.merge("binderGate." + gate + "." + nearestBinderFace(expr),
                1, Integer::sum);
    }

    /**
     * The widening-lever face for a binder-gated blocker: the nearest enclosing
     * {@link RInlineFunction}'s parent family + binder spelling, mirroring the arm walk the gate
     * itself runs ({@code filterExtractSourceOfArmBinder} over
     * {@code nearestInlineFunctionBeforeSwitch}) — a switch cuts the walk ({@code switchCut},
     * legacy's binding-liveness conservatism), no enclosing lambda is {@code noLambda}, and an
     * in-lambda face names the parent family the widening would have to admit
     * ({@code <ParentFamily>.<named|implicitBare|implicitBracket>}).
     */
    private String nearestBinderFace(RExpression expr) {
        RNode cur = expr.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RSwitchExpr) {
                return "switchCut";
            }
            if (cur instanceof RInlineFunction inline) {
                RNode parent = inline.parent();
                String family = parent == null ? "orphanLambda" : parent.getClass().getSimpleName();
                // isImplicit FIRST — the ImplicitVariableRule synthetic "item" parameter (see
                // lambdaVisitFacet's binder leg).
                String binder = inline.isImplicit() ? "implicitBare"
                        : !inline.paramNames().isEmpty() ? "named" : "implicitBracket";
                return family + "." + binder;
            }
            cur = cur.parent();
        }
        return "noLambda";
    }

    /**
     * The #496 facet classifier for {@link #inputNavRootFacets} — see the field javadoc for the
     * grammar. Meta reads the resolved input feature's own annotation set, typing reads the
     * fixed-point cache exactly as the leaf emitter's type gates would, and the lower leg takes
     * the ROOT {@code adapter.adapt} verdict with the adapter's own {@code declineReason} spelling
     * on a decline (the drift-proof read — the #478 arm's gates and its reason twin decide, never
     * a probe restatement). {@code noWorkspace} is the contract-impossible belt (the declineReason
     * robustness convention — a stable token, never an NPE).
     */
    private String inputNavRootFacet(REnumValueRef enr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "inputNavRoot.noWorkspace";
        }
        String meta = enr.resolvedInputFeature()
                .map(f -> isMetaAnnotated(f) ? "meta" : "plain").orElse("plain");
        RMetaAnnotatedType t = ws.getInferredType(enr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        Optional<IRExpr> lowered = adapter.adapt(enr, ws);
        String lower;
        if (lowered.isPresent()) {
            // The recvKind split (the conversion's own shape gate restated): inputHead = the
            // single-hop FieldAccess over an IRVariable head — the ring-proven shape the #496
            // conversion claims; aliasHead = the #492 alias-head leg (a FieldAccess over the
            // alias reference — stays on the relabel until its own oracle-closing);
            // otherShape = the contract-impossible belt.
            String recvKind = lowered.get() instanceof FieldAccess fa
                    ? (fa.receiver() instanceof IRVariable ? "inputHead" : "aliasHead")
                    : "otherShape";
            lower = "adapterLowers." + recvKind
                    + (subtreeTripsPostPinCoercion(enr) != null ? ".guardTrip" : "");
        } else {
            lower = "blocked." + adapter.declineReason(enr, ws)
                    .orElse(ExpressionToIRAdapter.UNATTRIBUTED);
        }
        return "inputNavRoot." + meta + "." + typing + "." + lower;
    }

    /**
     * The #507 arm-B facet classifier for {@link #l111ResidueFacets} — see the field javadoc
     * for the grammar. Returns null for the seat's CLEAN slice (adapter lowers, no guard trip —
     * the emitter-served events), so the census is residue-only and {@code Σ rows ≡ delegated}
     * per cell. Calls the adapter directly (stateless by its documented contract) and the
     * guard's own walk read-only — the standing counters cannot move.
     */
    private String l111ResidueFacet(REnumValueRef enr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "l111Residue.noWorkspace";
        }
        Optional<IRExpr> lowered = adapter.adapt(enr, ws);
        if (lowered.isPresent()) {
            // The identity-serve routing predicate re-read (the seat's own leg order): a
            // guard trip routes the event to the #507 identity serve — the `served` face.
            // #508: the seat's QUIET-claim shape gate re-read too — a lowering OUTSIDE the
            // FieldAccess-over-IRVariable/ALIAS quiet shapes (the arm-A1/A2 IRMetaAccess
            // acceptances) skips the quiet claim and takes the SAME identity serve, so it
            // carries its own served face (served.identityShape.<kind> — restoring the
            // field javadoc's Σ served ≡ inputNavIdentityLowered cross-check, the #507
            // OBS-4 watch); the emitter-clean quiet slice alone takes no row.
            PostPinArm trip = subtreeTripsPostPinCoercion(enr);
            if (trip != null) {
                return "l111Residue.served." + trip.label() + "."
                        + enumSeatLoweredShapeToken(lowered.get());
            }
            boolean quietShape = lowered.get() instanceof FieldAccess fa
                    && (fa.receiver() instanceof IRVariable
                            || (fa.receiver() instanceof IRReference aliasRecv
                                    && aliasRecv.referenceKind()
                                            == IRReference.ReferenceKind.ALIAS));
            return quietShape ? null
                    : "l111Residue.served.identityShape."
                            + enumSeatLoweredShapeToken(lowered.get());
        }
        String qual = enr.resolvedInputFeature()
                .map(IRExpressionCompiler::metaQualifierToken).orElse("noFeature");
        return "l111Residue.blocked."
                + adapter.declineReason(enr, ws).orElse(ExpressionToIRAdapter.UNATTRIBUTED)
                + ".q:" + qual;
    }

    /**
     * The #505 facet classifier for {@link #enumSeatRootFacets} — see the field javadoc for
     * the grammar. Runs at the excluded disguised-residue seat only (probe-gated); calls the
     * adapter directly (stateless by its documented contract) so the standing counters cannot
     * move.
     */
    private String enumSeatRootFacet(REnumValueRef enr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "enumSeatRoot.noWorkspace";
        }
        // The channel, in ExpressionTypeComputer.computeEnumValueRef's own resolution
        // precedence (the adapter twin's reasonForEnumValueRef order restated seat-side).
        String channel;
        if (enr.resolvedAttributeChain().isPresent()) {
            channel = "chain";
        } else if (enr.resolvedInputFeature().isPresent()) {
            channel = "inputLeak";
        } else if (enr.resolvedChoiceOption().isPresent()) {
            channel = "choiceOpt";
        } else if (enr.resolvedTypeRestriction().isPresent()) {
            channel = "typeRestrict";
        } else if (enr.enumeration().isEmpty()) {
            channel = enr.resolvedSymbol().isPresent() ? "headSym" : "noChannel";
        } else if (enr.enumValue().isEmpty()) {
            channel = "valueNameFb";
        } else {
            channel = "genuineLeak";
        }
        RMetaAnnotatedType t = ws.getInferredType(enr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        Optional<IRExpr> lowered = adapter.adapt(enr, ws);
        String verdict;
        if (lowered.isPresent()) {
            verdict = "lowers." + enumSeatLoweredShapeToken(lowered.get());
        } else {
            String r = adapter.declineReason(enr, ws).orElse(ExpressionToIRAdapter.UNATTRIBUTED);
            int d1 = r.indexOf('.');
            int d2 = d1 < 0 ? -1 : r.indexOf('.', d1 + 1);
            verdict = "blocked." + (d2 < 0 ? r : r.substring(0, d2));
        }
        return "enumSeatRoot." + channel + "." + typing + "." + verdict;
    }

    /**
     * The lowered-shape token for {@link #enumSeatRootFacet}'s claimable slice — the
     * conversion's shape gate reads these (the {@link #inputNavRootFacet} recvKind
     * convention): {@code faSynNav} (the #504-minted spine — oracle-leaf-bearing, needs the
     * seat's own oracle-closing), {@code faFa}/{@code faVar}/{@code fa:<Class>} (the plain
     * navigation spines by receiver), {@code symNav} ({@link IRSymbolNav} — the #501 C2
     * kind), or the IR class simple name.
     */
    private static String enumSeatLoweredShapeToken(IRExpr lowered) {
        if (lowered instanceof FieldAccess fa) {
            if (fa.receiver() instanceof IRSynItemNav) {
                return "faSynNav";
            }
            if (fa.receiver() instanceof FieldAccess) {
                return "faFa";
            }
            if (fa.receiver() instanceof IRVariable) {
                return "faVar";
            }
            return "fa:" + fa.receiver().getClass().getSimpleName();
        }
        if (lowered instanceof IRSymbolNav) {
            return "symNav";
        }
        return lowered.getClass().getSimpleName();
    }

    /**
     * The #497 facet classifier for {@link #metaNavRootFacets} — see the field javadoc for the
     * grammar. The feature is present by the belt's own gate; the receiver leg re-adapts at the
     * probe root (the #494 pieces convention — the seat's decline is the adapter's deliberate
     * meta gate, so the ROOT verdict is constant-declined and the PIECES are the read).
     */
    private String metaNavRootFacet(RFeatureCall fc) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "metaNavRoot.noWorkspace";
        }
        String kind = metaQualifierToken(fc.resolvedFeature().get());
        RMetaAnnotatedType t = ws.getInferredType(fc);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        RExpression rawReceiver = fc.receiver();
        String recv;
        if (rawReceiver == null) {
            recv = "recvNull";
        } else if (adapter.adapt(rawReceiver, ws).isPresent()) {
            recv = "recvLowers." + (rawReceiver instanceof RFeatureCall
                    || rawReceiver instanceof REnumValueRef
                    || rawReceiver instanceof RDeepFeatureCall ? "chain" : "leaf");
        } else {
            // #499: the recvBlocked leg refined IN PLACE (the #497 cut-2 ladder-leg precedent) —
            // the receiver's OWN first failing gate (the adapter's declineReason HEAD segment —
            // the composed descents' first token bounds the key space) names what still blocks
            // the seat's residue, and the cascade read sizes the recursive-teach slice (a
            // receiver that is itself a meta chain lowering bottom-up under the planned
            // admission — {@link #metaChainWouldLower}).
            String rreason = adapter.declineReason(rawReceiver, ws).orElse("unattributed");
            int dot = rreason.indexOf('.');
            String head = dot < 0 ? rreason : rreason.substring(0, dot);
            recv = "recvBlocked." + rawReceiver.getClass().getSimpleName() + "." + head
                    + (metaChainWouldLower(rawReceiver, ws, 0) ? ".cascade" : "");
        }
        return "metaNavRoot." + kind + "." + typing + "." + recv;
    }

    /**
     * The metadata-annotation qualifier token for {@link #metaNavRootFacet}'s metaKind leg:
     * the {@code metadata} annotation refs' qualifier names, sorted and {@code +}-joined
     * ({@code bare} = a qualifier-less metadata annotation — never empty, never an NPE).
     */
    private static String metaQualifierToken(RAttribute attr) {
        java.util.TreeSet<String> quals = new java.util.TreeSet<>();
        for (var ar : attr.annotationRefs()) {
            if ("metadata".equals(ar.annotationName())) {
                quals.add(ar.qualifierName().orElse("bare"));
            }
        }
        return quals.isEmpty() ? "bare" : String.join("+", quals);
    }

    /**
     * The #497 facet classifier for {@link #implicitAttrRootFacets} — see the field javadoc for
     * the grammar. Reason is the adapter's own mirror token (the drift-proof read); binderFace
     * the nearest-binder context; the LADDER leg (the cut-2 refinement) reads which legacy
     * {@code handle(RSymbolReference)} branch renders the event — the render a conversion must
     * byte-match — via the PUBLIC synthesizers in ladder order (the L-109e branch is null for
     * every event here BY THE BELT ORDER — that belt claims first): {@code itemNav} = the
     * filter/extract implicit-item nav ({@code synthesizeImplicitItemNavigation}), {@code
     * condInstance} = the type-condition instance nav ({@code
     * synthesizeConditionInstanceNavigation} — the executeDataRule-parameter navigation, the
     * data-rule class), {@code varPath} = neither (the bare-variable render). Read-only: the
     * synthesizers build fresh nodes and resolve names, never compile (the #479 witness
     * precedent).
     */
    private String implicitAttrRootFacet(RSymbolReference ref) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "implicitAttrRoot.noWorkspace";
        }
        String reason = adapter.declineReason(ref, ws).orElse("mirrorLowersOnReread");
        RAttribute attr = (RAttribute) ref.symbol().get(); // present by the belt's own gate
        String ladder;
        if (ReferenceHandler.synthesizeImplicitItemNavigation(ref, attr, this) != null) {
            ladder = "itemNav";
        } else if (getReferenceHandler().synthesizeConditionInstanceNavigation(ref, attr, this) != null) {
            ladder = "condInstance";
        } else {
            ladder = "varPath";
        }
        String token = "implicitAttrRoot." + reason + "." + nearestBinderFace(ref) + "." + ladder;
        // The first-sample witness (the navGateFirstSamples pattern): the qualitative face per
        // bucket — the enclosing rule/function, the attr name, and the declaring type.
        if (!implicitAttrFirstSamples.containsKey(token)) {
            RFunction sampleFn = HandlerHelper.findEnclosingFunction(ref);
            RRule sampleRule = sampleFn == null ? HandlerHelper.findEnclosingRule(ref) : null;
            String declaring = attr.parent() == null ? "<orphan>"
                    : attr.parent().getClass().getSimpleName()
                            + (attr.parent() instanceof RDataType dt ? ":" + dt.name() : "");
            implicitAttrFirstSamples.put(token, "in="
                    + (sampleFn != null ? "func:" + sampleFn.name()
                            : sampleRule != null ? "rule:" + sampleRule.name() : "<unresolved>")
                    + " attr=" + attr.name() + " declaring=" + declaring);
        }
        return token;
    }

    /**
     * The #497 facet classifier for {@link #inputNavReceiverRootFacets} — see the field javadoc
     * for the grammar. Reason is the adapter's own mirror token on the WHOLE call.
     */
    private String inputNavReceiverRootFacet(RFeatureCall fc) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "inputNavReceiverRoot.noWorkspace";
        }
        RMetaAnnotatedType t = ws.getInferredType(fc);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String reason = adapter.declineReason(fc, ws).orElse("mirrorLowersOnReread");
        return "inputNavReceiverRoot." + typing + "." + reason;
    }

    /**
     * The #498 leg-A facet classifier for {@link #onlyElemGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the exact {@code opOnlyElement} spelling (the flat token is the
     * only spelling {@code reasonForListOp} mints for the operator — a composed token names a
     * PARENT's arm gate, not this blocker node).
     */
    private void classifyOnlyElemGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!"opOnlyElement".equals(reason) || !(expr instanceof RListOpExpr op)) {
            return;
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(op);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        RExpression recv = op.argument();
        String recvVerdict;
        if (recv == null) {
            recvVerdict = "recvNull";
        } else if (ws != null && adapter.adapt(recv, ws).isPresent()) {
            recvVerdict = "recvLowers." + recv.getClass().getSimpleName();
        } else {
            recvVerdict = "recvBlocked." + recv.getClass().getSimpleName();
        }
        String token = "onlyElemGate." + positionToken(expr, claimRoot) + "."
                + typing + "." + recvVerdict;
        onlyElemGateFacets.merge(token, 1, Integer::sum);
        if (!onlyElemGateFirstSamples.containsKey(token)) {
            onlyElemGateFirstSamples.put(token, enclosingSampleToken(op));
        }
    }

    /**
     * The #498 leg-B facet classifier for {@link #calleeGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the exact {@code calleeNotFunction} spelling
     * ({@code reasonForApply}'s own token; the callee-symbol read mirrors the adapter's gate
     * order, so the symbol is present whenever the token minted — {@code unresolved} is the
     * drift belt).
     */
    private void classifyCalleeGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!"calleeNotFunction".equals(reason) || !(expr instanceof RSymbolReference ref)) {
            return;
        }
        RNode symbol = ref.symbol().orElse(null);
        String symbolKind = symbol == null ? "unresolved" : symbol.getClass().getSimpleName();
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(ref);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "calleeGate." + positionToken(expr, claimRoot) + "." + symbolKind
                + "." + typing + "." + enclosingContextToken(ref) + ".arg" + ref.args().size();
        calleeGateFacets.merge(token, 1, Integer::sum);
        if (!calleeGateFirstSamples.containsKey(token)) {
            calleeGateFirstSamples.put(token,
                    enclosingSampleToken(ref) + " callee=" + ref.name());
        }
    }

    /**
     * The #499 leg-A facet classifier for {@link #listOpGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the three exact {@code <pos>:IRListOp} spellings (the generic
     * consumer-token fallbacks {@code receiverToken}/{@code operandToken}/{@code argToken} mint
     * for a lowered list-op child that failed the seat's admission gate). The child decode is
     * seat-generic: every nearest expression descendant re-adapts at the probe and each lowered
     * {@link com.regnosys.rosetta.ir.expr.IRListOp} contributes an indexed kind token — the walk
     * PROVES which child(ren) carry the collapse rather than assuming the seat's position.
     */
    private void classifyListOpGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String seat;
        switch (reason) {
            case "receiver:IRListOp" -> seat = "recv";
            case "operand:IRListOp" -> seat = "operand";
            case "arg:IRListOp" -> seat = "arg";
            default -> {
                return;
            }
        }
        StringBuilder opKinds = new StringBuilder();
        Boolean firstListOpTyped = null;
        if (ws != null) {
            int i = 0;
            for (RExpression child : nearestExpressionDescendants(expr)) {
                var lowered = adapter.adapt(child, ws).orElse(null);
                if (lowered instanceof com.regnosys.rosetta.ir.expr.IRListOp listOp) {
                    if (opKinds.length() > 0) {
                        opKinds.append('+');
                    }
                    // Cut-2 (the 48-second-probe law): the collapse's OWN child-shape leg — the
                    // planned admission's byte-safety rests on the L-051 param/nav child
                    // equivalence (emit(child) ≡ legacy compileInterior(argument)); legacy's
                    // collapse render carries child-shape-conditional facets (#334 call wrap,
                    // the item round-trips), so the census names which child shapes are live.
                    opKinds.append('c').append(i).append(':')
                            .append(listOpKindToken(listOp.op()))
                            .append('/').append(listOpChildShapeToken(listOp.child()));
                    if (firstListOpTyped == null) {
                        firstListOpTyped = lowered.type() != null && !lowered.type().isMissing();
                    }
                }
                i++;
            }
        }
        String rt = firstListOpTyped == null ? "rtUnread"
                : firstListOpTyped ? "rt" : "rtMissing";
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "listOpGate." + seat + "." + positionToken(expr, claimRoot) + "."
                + (opKinds.length() == 0 ? "noListOpChild" : opKinds.toString()) + "."
                + rt + "." + typing + "." + expr.getClass().getSimpleName();
        listOpGateFacets.merge(token, 1, Integer::sum);
        if (!listOpGateFirstSamples.containsKey(token)) {
            listOpGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The cut-2 child-shape token for {@link #classifyListOpGateBlocker}: the lowered collapse's
     * own child IR form — {@code param} (an {@code IRVariable} of kind PARAM — the L-051
     * byte-equivalence class) / {@code fieldAccess} (a navigation — same class) / the IR class
     * simple name otherwise (the shapes legacy's collapse render treats facet-conditionally).
     */
    private static String listOpChildShapeToken(com.regnosys.rosetta.ir.expr.IRExpr child) {
        if (child instanceof com.regnosys.rosetta.ir.expr.IRVariable v
                && v.variableKind() == com.regnosys.rosetta.ir.expr.IRVariable.VariableKind.PARAM) {
            return "param";
        }
        if (child instanceof com.regnosys.rosetta.ir.expr.FieldAccess) {
            return "fieldAccess";
        }
        return child.getClass().getSimpleName();
    }

    /** The receipt token for an {@link com.regnosys.rosetta.ir.expr.IRListOp.Kind} constant ({@code ONLY_ELEMENT} → {@code onlyElement}). */
    private static String listOpKindToken(com.regnosys.rosetta.ir.expr.IRListOp.Kind kind) {
        String[] parts = kind.name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            sb.append(Character.toUpperCase(parts[i].charAt(0))).append(parts[i].substring(1));
        }
        return sb.toString();
    }

    /**
     * The #499 leg-B facet classifier for {@link #metaFeatureGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the exact {@code metaFeature} spelling
     * ({@code reasonForFeatureCall}'s own token; the resolved feature is present whenever the
     * token minted — the gate reads it AFTER the feature-unresolved gate).
     */
    private void classifyMetaFeatureGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        // Prefix-matched since the same-PR #499 meta arm landed (the #480 composed-token
        // convention): the arm refines the flat token IN PLACE to `metaFeature.recvBlocked` (the
        // residue face — a lowering receiver now claims through the arm and never mints), so the
        // post-arm channel reads the honest residue; the PRE-ARM decode (1,989 nodes, 100%
        // recvLowers, atRoot ZERO) stands in the #499 receipts.
        if (!reason.startsWith("metaFeature") || !(expr instanceof RFeatureCall fc)) {
            return;
        }
        RAttribute feature = fc.resolvedFeature().orElse(null);
        String kind = feature == null ? "featureMissing" : metaQualifierToken(feature);
        String card = feature != null && isMultiCardinalityShape(feature) ? "multi" : "single";
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(fc);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        RExpression rawReceiver = fc.receiver();
        String recv;
        if (rawReceiver == null) {
            recv = "recvNull";
        } else if (ws != null && adapter.adapt(rawReceiver, ws).isPresent()) {
            recv = "recvLowers." + (rawReceiver instanceof RFeatureCall
                    || rawReceiver instanceof REnumValueRef
                    || rawReceiver instanceof RDeepFeatureCall ? "chain" : "leaf");
        } else if (metaChainWouldLower(rawReceiver, ws, 0)) {
            recv = "recvCascade";
        } else {
            recv = "recvBlocked." + rawReceiver.getClass().getSimpleName();
        }
        String token = "metaFeatureGate." + positionToken(expr, claimRoot) + "." + kind + "."
                + card + "." + typing + "." + recv;
        metaFeatureGateFacets.merge(token, 1, Integer::sum);
        if (!metaFeatureGateFirstSamples.containsKey(token)) {
            metaFeatureGateFirstSamples.put(token, enclosingSampleToken(fc));
        }
    }

    /**
     * The #500 arm-A facet classifier for {@link #metaAccessGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the four exact {@code <seat>:IRMetaAccess} spellings (the
     * generic consumer-token fallbacks {@code receiverToken}/{@code operandToken}/{@code argToken}
     * and the attributeChain-composed twin mint them for a lowered meta hop that failed the
     * seat's admission gate — the #499 distinct-kind law's honest frontier). The child decode is
     * seat-generic: every nearest expression descendant re-adapts at the probe and each lowered
     * {@link com.regnosys.rosetta.ir.expr.IRMetaAccess} contributes an indexed
     * qualifier/receiver-shape token — the walk PROVES which child(ren) carry the meta hop, and
     * the lowering-child count carries the absorption-composition leg.
     */
    private void classifyMetaAccessGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String seat;
        switch (reason) {
            case "receiver:IRMetaAccess" -> seat = "recv";
            case "operand:IRMetaAccess" -> seat = "operand";
            case "arg:IRMetaAccess" -> seat = "arg";
            case "attributeChain.receiver:IRMetaAccess" -> seat = "chainRecv";
            default -> {
                return;
            }
        }
        StringBuilder metaKids = new StringBuilder();
        int lowering = 0;
        if (ws != null) {
            int i = 0;
            for (RExpression child : nearestExpressionDescendants(expr)) {
                var lowered = adapter.adapt(child, ws).orElse(null);
                if (lowered != null) {
                    lowering++;
                }
                if (lowered instanceof com.regnosys.rosetta.ir.expr.IRMetaAccess meta) {
                    if (metaKids.length() > 0) {
                        metaKids.append('+');
                    }
                    metaKids.append('c').append(i).append(':')
                            .append(String.join("-", meta.metaQualifiers()))
                            .append('/').append(metaAccessChildToken(meta.receiver()));
                }
                i++;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "metaAccessGate." + seat + "." + positionToken(expr, claimRoot) + "."
                + (metaKids.length() == 0 ? "noMetaChild" : metaKids.toString()) + ".low"
                + lowering + "." + typing + "." + expr.getClass().getSimpleName();
        metaAccessGateFacets.merge(token, 1, Integer::sum);
        if (!metaAccessGateFirstSamples.containsKey(token)) {
            metaAccessGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The meta hop's OWN receiver-shape token for {@link #classifyMetaAccessGateBlocker}:
     * {@code param} / {@code fieldAccess} (the flat-hop classes) / {@code metaAccess} (the
     * recursive-chain slice — a lowered meta hop whose receiver is itself a meta hop) / the IR
     * class simple name otherwise.
     */
    private static String metaAccessChildToken(com.regnosys.rosetta.ir.expr.IRExpr recv) {
        if (recv instanceof com.regnosys.rosetta.ir.expr.IRVariable v
                && v.variableKind() == com.regnosys.rosetta.ir.expr.IRVariable.VariableKind.PARAM) {
            return "param";
        }
        if (recv instanceof com.regnosys.rosetta.ir.expr.FieldAccess) {
            return "fieldAccess";
        }
        if (recv instanceof com.regnosys.rosetta.ir.expr.IRMetaAccess) {
            return "metaAccess";
        }
        return recv.getClass().getSimpleName();
    }

    /**
     * The #501 arm-A facet classifier for {@link #shallowGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the four exact {@code <seat>:<IRClass>} spellings (the generic
     * consumer-token fallbacks {@code booleanOperandToken}/{@code argToken} mint them for a
     * lowered shallow node that failed the seat's admission gate — the #500 family sweep's honest
     * consumer frontier). The child decode re-adapts every nearest expression descendant at the
     * probe and records which child index(es) lower to the reason's shallow kind plus the
     * lowering-child count (the absorption-composition leg).
     */
    private void classifyShallowGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String seat;
        String kind;
        Class<? extends IRExpr> kindClass;
        switch (reason) {
            case "operand:IROnlyExists" -> {
                seat = "operand";
                kind = "onlyExists";
                kindClass = IROnlyExists.class;
            }
            case "operand:IRPipe" -> {
                seat = "operand";
                kind = "pipe";
                kindClass = IRPipe.class;
            }
            case "arg:IRConversion" -> {
                seat = "arg";
                kind = "conversion";
                kindClass = IRConversion.class;
            }
            case "arg:IRPipe" -> {
                seat = "arg";
                kind = "pipe";
                kindClass = IRPipe.class;
            }
            default -> {
                return;
            }
        }
        // The callee leg on the arg seat: a RULE-apply root is already whole-served by the #498
        // RuleApplyRenderer (super.visitSymbolReference — the same literal line), so the teach
        // outcome splits by callee kind (a FUNCTION apply needs the oracle-root callArgs leg).
        String callee = "";
        if ("arg".equals(seat) && expr instanceof RSymbolReference sym) {
            RNode symbol = sym.symbol().orElse(null);
            callee = symbol instanceof RRule ? ".calleeRule"
                    : symbol instanceof RFunction ? ".calleeFunction" : ".calleeOther";
        }
        StringBuilder kids = new StringBuilder();
        int lowering = 0;
        if (ws != null) {
            int i = 0;
            for (RExpression child : nearestExpressionDescendants(expr)) {
                IRExpr lowered = adapter.adapt(child, ws).orElse(null);
                if (lowered != null) {
                    lowering++;
                }
                if (lowered != null && kindClass.isInstance(lowered)) {
                    if (kids.length() > 0) {
                        kids.append('+');
                    }
                    kids.append('c').append(i);
                }
                i++;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "shallowGate." + seat + "." + kind + callee + "."
                + positionToken(expr, claimRoot) + "."
                + (kids.length() == 0 ? "noShallowChild" : kids.toString()) + ".low" + lowering
                + "." + typing + "." + expr.getClass().getSimpleName();
        shallowGateFacets.merge(token, 1, Integer::sum);
        if (!shallowGateFirstSamples.containsKey(token)) {
            shallowGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #501 arm-C facet classifier for {@link #chainDrainGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the five exact chain-drain reason spellings. Each face
     * adapts the EXACT legacy-synthesized equivalent (the #478 witness convention — legacy's own
     * public synthesizers where they exist, the local single-hop mirror for the bare seat) so the
     * verdict names the deep route's admissibility per node. Read-only against the tree and
     * adapter-only (stateless by its documented contract) — the standing counters cannot move.
     */
    private void classifyChainDrainGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face;
        String detail;
        switch (reason) {
            case "attributeChain.headSymbolNav" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                face = "headSym";
                RNode symbol = enr.resolvedSymbol().orElse(null);
                RFeatureCall equivalent =
                        symbol instanceof RRule rule
                                ? getReferenceHandler().synthesizeRuleReceiverNavigation(enr, rule)
                                : symbol instanceof RFunction fn
                                        ? getReferenceHandler()
                                                .synthesizeFunctionReceiverNavigation(enr, fn)
                                        : null;
                RAttribute leaf = enr.resolvedAttributeChain()
                        .map(REnumValueRef.AttributeChain::feature).orElse(null);
                String leafTok = leaf == null ? "leafUnbound"
                        : isMetaAnnotated(leaf) ? "metaLeaf" : "plainLeaf";
                detail = (symbol == null ? "nullSym" : symbol.getClass().getSimpleName()) + "."
                        + leafTok + "." + verdictWithLoweredKind(equivalent, ws);
            }
            case "attributeChain.ruleInputChain" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                face = "ruleChain";
                REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
                RFeatureCall equivalent = chain == null ? null
                        : getReferenceHandler().synthesizeImplicitInputChain(enr, chain);
                detail = (ruleLambdaInterposed(enr) ? "inLambda" : "topLevel") + "."
                        + verdictWithLoweredKind(equivalent, ws);
            }
            case "attributeChain.sourceElementUnresolved" -> {
                face = "srcElem";
                detail = binderSourceShapeToken(expr);
            }
            case "attributeChain.equivalentMetaAccess" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                face = "chainMeta";
                REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
                RAttribute head = chain == null ? null : chain.attributeOpt().orElse(null);
                RAttribute leaf = chain == null ? null : chain.feature();
                boolean headMeta = head != null && isMetaAnnotated(head);
                boolean leafMeta = leaf != null && isMetaAnnotated(leaf);
                String shape = headMeta && leafMeta ? "metaBoth" : headMeta ? "metaHead" : "metaLeaf";
                RFeatureCall equivalent =
                        getReferenceHandler().synthesizeImplicitItemChain(enr, this);
                detail = shape + "." + verdictWithLoweredKind(equivalent, ws);
            }
            case "attrOutsideFunction.equivalentMetaAccess" -> {
                if (!(expr instanceof RSymbolReference sym)) {
                    return;
                }
                face = "bareMeta";
                RNode symbol = sym.symbol().orElse(null);
                RFeatureCall equivalent = null;
                String qualifier = "symNotAttr";
                if (symbol instanceof RAttribute attr) {
                    qualifier = metaQualifierToken(attr);
                    // The single-hop synthetic-item equivalent — the adapter's
                    // implicitItemNavEquivalent mirror (legacy's synthesizeImplicitItemNavigation
                    // shape): a fresh synthetic implicit + the resolved feature, both parented at
                    // the ref's own parent (throwaway construction — joins no tree).
                    RImplicitVariable item = new RImplicitVariable();
                    item.setSynthetic(true);
                    item.setParent(sym.parent());
                    RFeatureCall fc = new RFeatureCall();
                    fc.setReceiver(item);
                    fc.setFeatureName(attr.name());
                    fc.setResolvedFeature(attr);
                    fc.setParent(sym.parent());
                    equivalent = fc;
                }
                detail = qualifier + "." + verdictWithLoweredKind(equivalent, ws);
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "chainDrainGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + detail + "." + typing;
        chainDrainGateFacets.merge(token, 1, Integer::sum);
        if (!chainDrainGateFirstSamples.containsKey(token)) {
            chainDrainGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #501 verdict leg: {@link #adapterVerdictOnEquivalent}'s spelling with the lowered IR
     * kind appended on admission ({@code lowers:<IRClass>} — the render-seat split the arm-C
     * teach routes on: an {@code IRMetaAccess} root takes the disguised-meta oracle serve, a
     * {@code FieldAccess} chain the native composition).
     */
    private String verdictWithLoweredKind(RFeatureCall equivalent, RWorkspace ws) {
        if (equivalent == null || ws == null) {
            return "noEquivalent";
        }
        IRExpr lowered = adapter.adapt(equivalent, ws).orElse(null);
        if (lowered != null) {
            return "lowers:" + lowered.getClass().getSimpleName();
        }
        return adapterVerdictOnEquivalent(equivalent, ws);
    }

    /**
     * The #502 arm-A facet classifier for {@link #symbolDrainGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the six exact sole-face reason spellings of the
     * RSymbolReference drain (every other token a no-op). Read-only against the tree (the
     * aliasHead equivalent is a FRESH linker-bound reference — the #478 witness convention,
     * exactly the form the adapter's alias arm claims) and adapter-only (stateless by its
     * documented contract) — the standing counters cannot move.
     */
    private void classifySymbolDrainGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference sym)) {
            return;
        }
        String face;
        switch (reason) {
            case "symbolNotAttribute" -> {
                RNode symbol = sym.symbol().orElse(null);
                face = "symNotAttr."
                        + (symbol == null ? "nullSym" : symbol.getClass().getSimpleName());
            }
            case "symbolUnresolved.synthetic.aliasName.bodyTyped" -> {
                RFunction enclosing = HandlerHelper.findEnclosingFunction(sym);
                RShortcut shortcut = shortcutNamed(enclosing, sym.name());
                if (shortcut == null) {
                    face = "aliasHead.noShortcut"; // drift face — the reason implies the match
                } else {
                    RSymbolReference bound = new RSymbolReference();
                    bound.setName(sym.name());
                    bound.setResolvedSymbol(shortcut);
                    bound.setParent(sym.parent());
                    IRExpr lowered = ws == null ? null : adapter.adapt(bound, ws).orElse(null);
                    String verdict = lowered == null ? "declines"
                            : "lowers:" + lowered.getClass().getSimpleName();
                    RMetaAnnotatedType bodyT = shortcut.expression() == null || ws == null
                            ? null : ws.getInferredType(shortcut.expression());
                    String bodyState = bodyT == null || bodyT.isMissing() ? "bodyMissing"
                            : bodyT.hasMeta() ? "bodyMeta" : "bodyTyped";
                    face = "aliasHead." + verdict + "." + bodyState;
                }
            }
            case "symbolUnresolved.synthetic.closureParam" -> {
                RInlineFunction binder = registeringClosureBinder(sym, sym.name());
                if (binder == null) {
                    face = "closureParam.noBinder"; // drift face — the reason implies the match
                } else {
                    RNode parent = binder.parent();
                    String binderOp = parent == null ? "orphanBinder"
                            : parent.getClass().getSimpleName();
                    RExpression src = parent instanceof RFilterExpr filter
                            && filter.body() == binder ? filter.argument()
                                    : parent instanceof RExtractExpr extract
                                            && extract.body() == binder ? extract.argument()
                                            : parent instanceof RThenExpr then
                                                    && then.body().orElse(null) == binder
                                                            ? then.argument() : null;
                    face = "closureParam." + binderOp + "." + sourceWalkVerdicts(src);
                }
            }
            case "argItem" -> {
                // Re-run the reason mirror's per-argument loop: the FIRST item-lowering argument
                // is the one the flat token named.
                String detail = "noItemArg"; // drift face — the reason implies one
                for (RExpression rawArg : sym.args()) {
                    IRExpr lowered = ws == null ? null : adapter.adapt(rawArg, ws).orElse(null);
                    if (lowered instanceof IRVariable var
                            && (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                                    || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM)) {
                        String itemKind =
                                var.variableKind() == IRVariable.VariableKind.USER_ITEM
                                        ? "user" : "syn";
                        String binderCtx = rawArg instanceof RImplicitVariable iv
                                ? (iv.isSynthetic() ? syntheticItemContext(iv)
                                        : filterExtractBindingSourceOf(iv) != null
                                                ? "userBound" : "userUnbound")
                                : "nonImplicitRaw";
                        RNode callee = sym.symbol().orElse(null);
                        String calleeKind = callee instanceof RFunction ? "fn"
                                : callee instanceof RRule ? "rule" : "other";
                        detail = itemKind + "." + binderCtx + "." + calleeKind;
                        break;
                    }
                }
                face = "argItem." + detail;
            }
            case "calleeMetaOutput" -> {
                RNode callee = sym.symbol().orElse(null);
                RFunction fn = callee instanceof RFunction f ? f
                        : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
                RAttribute output = fn == null ? null : fn.output().orElse(null);
                String qualifier = output == null ? "noOutput" : metaQualifierToken(output);
                int lowering = 0;
                for (RExpression rawArg : sym.args()) {
                    if (ws != null && adapter.adapt(rawArg, ws).isPresent()) {
                        lowering++;
                    }
                }
                face = "calleeMetaOut." + qualifier + ".argsLow" + lowering + "of"
                        + sym.args().size();
            }
            case "attrOutsideFunction.sourceElementUnresolved" -> {
                RExpression src = binderSourceOf(sym);
                face = "srcElemBare." + (src == null ? "noSource"
                        : deepSourceShapeToken(src, 0) + "."
                                + (ExpressionToIRAdapter.isProvablyNonMetaElementSource(src)
                                        ? "aw1" : "aw0"));
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(sym);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "symbolDrainGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        symbolDrainGateFacets.merge(token, 1, Integer::sum);
        if (!symbolDrainGateFirstSamples.containsKey(token)) {
            symbolDrainGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #502 arm-B facet classifier for {@link #synItemGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the exact {@code receiverSyntheticItem} spelling (every
     * other token a no-op). Read-only against the tree and adapter-only — the standing counters
     * cannot move.
     */
    private void classifySynItemGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!"receiverSyntheticItem".equals(reason) || !(expr instanceof RFeatureCall fc)
                || !(fc.receiver() instanceof RImplicitVariable iv) || !iv.isSynthetic()) {
            return;
        }
        String context = syntheticItemContext(iv);
        String detail;
        switch (context) {
            case "inLambdaImplicit" -> {
                RExpression bound = filterExtractBindingSourceOf(iv);
                if (bound == null) {
                    detail = "otherBinder.contextOnly"; // the #479 non-filter/extract class
                } else {
                    RExpression source = resolveElidedPipedSourceShape(bound);
                    detail = source == null ? "noSource"
                            : deepSourceShapeToken(source, 0) + "." + threeWalkVerdicts(source, ws);
                }
            }
            case "thenBody" -> {
                RExpression arg = thenArgumentOf(iv);
                if (arg == null) {
                    detail = "noThenArg"; // drift face — the context implies the then
                } else {
                    ExpressionCardinality card = ws == null ? null : ws.getCardinality(arg);
                    String cardTok = card == null ? "cardMiss"
                            : card == ExpressionCardinality.SINGLE ? "card1" : "cardN";
                    detail = deepSourceShapeToken(arg, 0) + "." + threeWalkVerdicts(arg, ws)
                            + "." + cardTok;
                }
            }
            default -> detail = "contextOnly";
        }
        String token = "synItemGate." + context + "." + detail + "."
                + positionToken(expr, claimRoot);
        synItemGateFacets.merge(token, 1, Integer::sum);
        if (!synItemGateFirstSamples.containsKey(token)) {
            synItemGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #503 arm-A facet classifier for {@link #equalityGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the three cluster-face reason spellings (every other
     * token a no-op; the {@code allAnyModifier} spelling is shared by BOTH the equality and
     * comparison mirrors, split by the family leg). Read-only against the tree and
     * adapter-only — the standing counters cannot move.
     */
    private void classifyEqualityGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face;
        switch (reason) {
            case "allAnyModifier" -> {
                String fam;
                String mod;
                String op;
                RExpression rawLeft;
                RExpression rawRight;
                if (expr instanceof REqualityExpr eq) {
                    fam = "eq";
                    mod = eq.mod().map(Enum::name).orElse("noMod");
                    op = eq.op().name();
                    rawLeft = eq.rawLeft();
                    rawRight = eq.rawRight();
                } else if (expr instanceof RComparisonExpr cmp) {
                    fam = "cmp";
                    mod = cmp.mod().map(Enum::name).orElse("noMod");
                    op = cmp.op().name();
                    rawLeft = cmp.rawLeft();
                    rawRight = cmp.rawRight();
                } else {
                    return;
                }
                int lowering = 0;
                if (ws != null) {
                    if (rawLeft != null && adapter.adapt(rawLeft, ws).isPresent()) {
                        lowering++;
                    }
                    if (rawRight != null && adapter.adapt(rawRight, ws).isPresent()) {
                        lowering++;
                    }
                }
                face = "allAny." + fam + "." + mod + "." + op + ".low" + lowering;
            }
            case "operand:IRToString" -> {
                RExpression rawLeft;
                RExpression rawRight;
                if (expr instanceof REqualityExpr eq) {
                    rawLeft = eq.rawLeft();
                    rawRight = eq.rawRight();
                } else if (expr instanceof RComparisonExpr cmp) {
                    rawLeft = cmp.rawLeft();
                    rawRight = cmp.rawRight();
                } else {
                    return;
                }
                IRExpr left = ws == null || rawLeft == null
                        ? null : adapter.adapt(rawLeft, ws).orElse(null);
                IRExpr right = ws == null || rawRight == null
                        ? null : adapter.adapt(rawRight, ws).orElse(null);
                String side = left instanceof IRToString ? "left"
                        : right instanceof IRToString ? "right" : "neither";
                IRExpr sibling = "left".equals(side) ? right : left;
                String sib = sibling == null ? "sibDeclines"
                        : "sib:" + sibling.getClass().getSimpleName();
                int lowering = (left != null ? 1 : 0) + (right != null ? 1 : 0);
                face = "toStr." + side + "." + sib + ".low" + lowering;
            }
            case "operandAlias.clean.enumSibling" -> {
                if (!(expr instanceof REqualityExpr eq)) {
                    return;
                }
                // The alias side: the raw operand whose adapt reads IRReference{ALIAS}; the
                // sibling is the other side (the bare enum-value reference the #491 admission
                // deferred on). The lever decode: the SHORTCUT body's cached type (does it
                // name the data/enum form the emitter's requalifier needs?) + the sibling
                // enum value's own resolution.
                RExpression rawLeft = eq.rawLeft();
                RExpression rawRight = eq.rawRight();
                IRExpr left = ws == null || rawLeft == null
                        ? null : adapter.adapt(rawLeft, ws).orElse(null);
                boolean leftAlias = left instanceof IRReference r
                        && r.referenceKind() == IRReference.ReferenceKind.ALIAS;
                RExpression rawAlias = leftAlias ? rawLeft : rawRight;
                RExpression rawSibling = leftAlias ? rawRight : rawLeft;
                RShortcut shortcut = null;
                if (rawAlias instanceof RSymbolReference sr) {
                    shortcut = sr.symbol().orElse(null) instanceof RShortcut bound ? bound
                            : shortcutNamed(HandlerHelper.findEnclosingFunction(sr), sr.name());
                }
                String bodyType;
                if (shortcut == null || shortcut.expression() == null || ws == null) {
                    bodyType = "bodyUnreadable";
                } else {
                    RMetaAnnotatedType bt = ws.getInferredType(shortcut.expression());
                    bodyType = bt == null || bt.isMissing() ? "bodyMiss"
                            : bt.type() == null ? "bodyNullType"
                                    : "body:" + bt.type().getClass().getSimpleName()
                                            + ":" + bt.type().name();
                }
                String enumResolved = rawSibling instanceof RSymbolReference sibRef
                        && sibRef.symbol().orElse(null) instanceof REnumValue
                                ? "enumResolved" : "enumOther";
                face = "enumSib." + bodyType + "." + enumResolved;
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "equalityGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        equalityGateFacets.merge(token, 1, Integer::sum);
        if (!equalityGateFirstSamples.containsKey(token)) {
            equalityGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #503 arm-B facet classifier for {@link #featureDrainGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the two feature-resolution reason spellings
     * (every other token a no-op). Read-only against the tree and adapter-only — the standing
     * counters cannot move.
     */
    private void classifyFeatureDrainGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RFeatureCall fc)) {
            return;
        }
        String face;
        switch (reason) {
            case "featureUnresolved.nonSymbolReceiver" -> {
                RExpression recv = fc.receiver();
                String recvClass = recv == null ? "nullRecv" : recv.getClass().getSimpleName();
                IRExpr lowered = ws == null || recv == null
                        ? null : adapter.adapt(recv, ws).orElse(null);
                String adaptV = lowered == null ? "declines"
                        : "lowers:" + lowered.getClass().getSimpleName();
                RDataType recvData = null;
                String typeV;
                RMetaAnnotatedType ct = ws == null || recv == null
                        ? null : ws.getInferredType(recv);
                if (ct == null || ct.isMissing()) {
                    typeV = "ctMiss";
                } else if (ct.type() instanceof RDataTypeRef dt) {
                    recvData = dt.astNode();
                    typeV = "ctData:" + dt.name();
                } else if (ct.type() == null) {
                    typeV = "ctNullType";
                } else {
                    // The refined non-data decode (the first probe read the dominant rows
                    // ctOther — the type CLASS is the teach-routing fact).
                    typeV = "ct:" + ct.type().getClass().getSimpleName()
                            + ":" + ct.type().name();
                }
                String member = recvData == null ? "noLookup"
                        : HandlerHelper.findAttributeOnDataType(recvData, fc.featureName()) != null
                                ? "memberHit" : "memberMiss";
                face = "nonSymRecv." + recvClass + "." + adaptV + "." + typeV + "." + member;
            }
            case "featureUnresolved.headUnresolved" -> {
                RExpression recv = fc.receiver();
                if (!(recv instanceof RSymbolReference head)) {
                    return; // contract-impossible (the facet implies a symbol head)
                }
                String headKind = head.name() != null
                        && registeringClosureBinder(head, head.name()) != null
                                ? "closureParamName"
                                : shortcutNamed(HandlerHelper.findEnclosingFunction(head),
                                        head.name()) != null ? "aliasName" : "otherName";
                RDataType elem = null;
                if ("closureParamName".equals(headKind)) {
                    // The #502 walks: the registering binder's SOURCE derives the param's
                    // element type (the Cat-8 law at the closure-param head).
                    RInlineFunction binder = registeringClosureBinder(head, head.name());
                    RNode parent = binder == null ? null : binder.parent();
                    RExpression src = parent instanceof RFilterExpr filter
                            && filter.body() == binder ? filter.argument()
                                    : parent instanceof RExtractExpr extract
                                            && extract.body() == binder ? extract.argument()
                                            : parent instanceof RThenExpr then
                                                    && then.body().orElse(null) == binder
                                                            ? then.argument() : null;
                    elem = src == null ? null
                            : ExpressionToIRAdapter.sourceElementDataType(src);
                }
                String elemV = elem == null ? "twNull" : "twData:" + elem.name();
                String member = elem == null ? "noLookup"
                        : HandlerHelper.findAttributeOnDataType(elem, fc.featureName()) != null
                                ? "memberHit" : "memberMiss";
                face = "headUnres." + headKind + "." + elemV + "." + member;
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(fc);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "featureDrainGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        featureDrainGateFacets.merge(token, 1, Integer::sum);
        if (!featureDrainGateFirstSamples.containsKey(token)) {
            featureDrainGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #504 arm-A facet classifier for {@link #booleanOpGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the two boolean-operand cluster reason spellings
     * ({@code operandBareBooleanCall} — RLogicalExpr only — and {@code operand:IRPointFreeApply}
     * across its three operand seats; every other token a no-op). Read-only against the tree
     * and adapter-only (stateless by its documented contract) — the standing counters cannot
     * move.
     */
    private void classifyBooleanOpGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        boolean bareCall = "operandBareBooleanCall".equals(reason);
        boolean pf = "operand:IRPointFreeApply".equals(reason);
        if (!bareCall && !pf) {
            return;
        }
        String fam;
        RExpression rawLeft;
        RExpression rawRight;
        if (expr instanceof RLogicalExpr log) {
            fam = "log";
            rawLeft = log.rawLeft();
            rawRight = log.rawRight();
        } else if (expr instanceof REqualityExpr eq) {
            fam = "eq";
            rawLeft = eq.rawLeft();
            rawRight = eq.rawRight();
        } else if (expr instanceof RExistenceExpr exist) {
            fam = "exist";
            rawLeft = exist.argument();
            rawRight = null;
        } else {
            return;
        }
        IRExpr left = ws == null || rawLeft == null ? null : adapter.adapt(rawLeft, ws).orElse(null);
        IRExpr right = ws == null || rawRight == null
                ? null : adapter.adapt(rawRight, ws).orElse(null);
        // The reason mirrors check left before right, and neither named kind can pass its
        // seat's gate — so the FIRST operand lowering to the named kind is the token's own.
        boolean leftNamed = bareCall ? left instanceof IRApply : left instanceof IRPointFreeApply;
        IRExpr named = leftNamed ? left : right;
        IRExpr sibling = leftNamed ? right : left;
        RExpression rawNamed = leftNamed ? rawLeft : rawRight;
        if (named == null || (bareCall ? !(named instanceof IRApply)
                : !(named instanceof IRPointFreeApply))) {
            return; // contract-impossible (the reason implies the lowering) — defensive no-op
        }
        String calleeTok;
        if (bareCall) {
            RNode callee = rawNamed instanceof RSymbolReference call
                    ? call.symbol().orElse(null) : null;
            calleeTok = callee instanceof RFunction ? "fn"
                    : callee instanceof RRule ? "rule" : "otherCallee";
        } else {
            calleeTok = "pfCallee";
        }
        RMetaAnnotatedType outT = named.type();
        String outTok = outT == null || outT.isMissing() ? "outMiss"
                : outT.type() == null ? "outNullType" : "out:" + outT.type().name();
        String cardTok = named.cardinality() == ExpressionCardinality.MULTI ? "cardN" : "card1";
        String sibTok = rawRight == null ? "sibNone"
                : sibling == null ? "sibDeclines" : "sib:" + sibling.getClass().getSimpleName();
        String face = (bareCall ? "bareCall." : "pf.") + fam + "."
                + (leftNamed ? "left" : "right") + "." + calleeTok + "." + outTok + "."
                + cardTok + "." + sibTok;
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "booleanOpGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        booleanOpGateFacets.merge(token, 1, Integer::sum);
        if (!booleanOpGateFirstSamples.containsKey(token)) {
            booleanOpGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #504 arm-C facet classifier for {@link #qualNameGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the three qualified-name residue reason spellings (every
     * other token a no-op). Read-only against the tree and adapter-only — the standing
     * counters cannot move.
     */
    private void classifyQualNameGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference sym)) {
            return;
        }
        String face;
        switch (reason) {
            case "symbolNotAttribute" -> {
                RNode symbol = sym.symbol().orElse(null);
                face = "symNot."
                        + (symbol == null ? "nullSym" : symbol.getClass().getSimpleName())
                        + "." + qualNameSeatToken(sym, ws);
            }
            case "argEmpty" -> {
                // Re-run the reason mirror's per-argument loop: the FIRST empty-lowering
                // argument is the one the flat token named.
                RNode callee = sym.symbol().orElse(null);
                RFunction fn = callee instanceof RFunction f ? f
                        : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
                String calleeTok = callee instanceof RFunction ? "fn"
                        : callee instanceof RRule ? "rule" : "otherCallee";
                String paramTok = "noEmptyArg"; // drift face — the reason implies one
                int lowering = 0;
                List<RExpression> args = sym.args();
                for (int i = 0; i < args.size(); i++) {
                    IRExpr lowered = ws == null ? null : adapter.adapt(args.get(i), ws).orElse(null);
                    if (lowered != null) {
                        lowering++;
                    }
                    if ("noEmptyArg".equals(paramTok) && lowered instanceof IREmptyLiteral) {
                        if (fn == null || i >= fn.inputs().size()) {
                            paramTok = "noParam";
                        } else {
                            RAttribute param = fn.inputs().get(i);
                            paramTok = "param:" + (param.typeCall() == null ? "untyped"
                                    : param.typeCall().typeName())
                                    + (isMultiCardinalityShape(param) ? ".pN" : ".p1");
                        }
                    }
                }
                face = "argEmpty." + calleeTok + "." + paramTok
                        + ".low" + lowering + "of" + args.size();
            }
            case "symbolUnresolved.synthetic.absent" -> {
                RNode parent = sym.parent();
                RNode gp = parent == null ? null : parent.parent();
                face = "synAbsent.name:" + (sym.name() == null ? "null" : sym.name()) + "."
                        + (parent == null ? "noParent" : parent.getClass().getSimpleName()) + "."
                        + (gp == null ? "noGp" : gp.getClass().getSimpleName());
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(sym);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "qualNameGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        qualNameGateFacets.merge(token, 1, Integer::sum);
        if (!qualNameGateFirstSamples.containsKey(token)) {
            qualNameGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #504 symNotAttr SEAT decode — which legacy recovery arm would render the
     * non-attribute-bound bare name (the teach-routing fact): the call-ARG seat (legacy
     * {@code ReferenceHandler.tryBareEnumArg} re-qualifies an RBody/RDataType/RCorpus-bound or
     * unresolved bare name against the callee's POSITIONAL declared enum parameter — the scan
     * mirrors its {@code en.values()} own-values walk exactly, no super-enum chain), the
     * equality-sibling seat (the #458 expected-type family), the in-lambda implicit-item seat
     * (legacy {@code synthesizeImplicitItemBareNav} re-resolves the name on the item's element
     * type — the standing #502 walks restated read-only), or the top-level parent class (the
     * residue). Bounded walks; probe-only.
     */
    private String qualNameSeatToken(RSymbolReference sym, RWorkspace ws) {
        RNode parent = sym.parent();
        if (parent instanceof RSymbolReference call && !call.args().isEmpty()) {
            int idx = -1;
            List<RExpression> args = call.args();
            for (int i = 0; i < args.size(); i++) {
                if (args.get(i) == sym) {
                    idx = i;
                    break;
                }
            }
            RNode callee = call.symbol().orElse(null);
            RFunction fn = callee instanceof RFunction f ? f
                    : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
            String calleeTok = callee instanceof RFunction ? "fn"
                    : callee instanceof RRule ? "rule" : "otherCallee";
            String paramTok;
            if (idx < 0) {
                paramTok = "notAnArg"; // contract-impossible — the parent lists the node
            } else if (fn == null || idx >= fn.inputs().size()) {
                paramTok = "noParam";
            } else {
                RTypeCall paramType = fn.inputs().get(idx).typeCall();
                RNode referenced = paramType == null ? null
                        : paramType.referencedType().orElse(null);
                paramTok = referenced instanceof REnumeration en
                        ? (enumOwnValueNamed(en, sym.name()) ? "enumHit:" : "enumMiss:")
                                + en.name()
                        : "paramNotEnum";
            }
            return "argOf." + calleeTok + "." + paramTok;
        }
        if (parent instanceof REqualityExpr eq) {
            RExpression sibRaw = eq.rawLeft() == sym ? eq.rawRight() : eq.rawLeft();
            RMetaAnnotatedType sibT = ws == null || sibRaw == null
                    ? null : ws.getInferredType(sibRaw);
            if (sibT != null && !sibT.isMissing()
                    && sibT.type() instanceof REnumTypeRef enumRef) {
                return "eqSib." + (enumOwnValueNamed(enumRef.astNode(), sym.name())
                        ? "enumHit:" : "enumMiss:") + enumRef.name();
            }
            return "eqSib.sibNotEnum";
        }
        RInlineFunction binder = nearestInlineFn(sym);
        if (binder != null) {
            RNode bp = binder.parent();
            RExpression src = bp instanceof RFilterExpr filter && filter.body() == binder
                    ? filter.argument()
                    : bp instanceof RExtractExpr extract && extract.body() == binder
                            ? extract.argument()
                            : bp instanceof RThenExpr then && then.body().orElse(null) == binder
                                    ? then.argument() : null;
            RDataType elem = src == null ? null : ExpressionToIRAdapter.sourceElementDataType(src);
            if (elem == null) {
                return "inLambda.srcUnprovable";
            }
            return "inLambda." + (HandlerHelper.findAttributeOnDataType(elem, sym.name()) != null
                    ? "itemAttrHit:" : "itemAttrMiss:") + elem.name();
        }
        return "topLevel." + (parent == null ? "noParent" : parent.getClass().getSimpleName());
    }

    /** The #504 enum own-values scan — legacy {@code tryBareEnumArg}'s walk (no super chain). */
    private static boolean enumOwnValueNamed(REnumeration en, String name) {
        if (en == null || name == null) {
            return false;
        }
        for (REnumValue value : en.values()) {
            if (name.equals(value.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * The #507 arm-A census classifier for {@link #deepGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the family's single reason spelling ({@code RDeepFeatureCall}
     * has no adapt arm, so every minimal blocker carries the generic {@code noAdaptArm} token).
     * Read-only against the tree and adapter-only (stateless by its documented contract) — the
     * standing counters cannot move.
     */
    private void classifyDeepGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RDeepFeatureCall deep) || !"noAdaptArm".equals(reason)) {
            return;
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "deepGate." + deepShapeFacet(deep, ws) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "." + typing;
        deepGateFacets.merge(token, 1, Integer::sum);
        if (!deepGateFirstSamples.containsKey(token)) {
            deepGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The shared {@code <feat>.<recv>} core of the two #507 arm-A censuses ({@link
     * #deepGateFacets} / {@link #deepVisitFacets}): the linker's deep-feature resolution fact +
     * the receiver subtree's own adapter verdict (shape on a lowering receiver per
     * {@link #enumSeatLoweredShapeToken}; the first-failing-gate HEAD on a decliner).
     */
    private String deepShapeFacet(RDeepFeatureCall deep, RWorkspace ws) {
        String feat = deep.resolvedFeature()
                .map(f -> isMetaAnnotated(f) ? "featMetaHit" : "featHit")
                .orElse("featMiss");
        RExpression receiver = deep.receiver();
        String recv;
        if (receiver == null) {
            recv = "recvNull";
        } else if (ws == null) {
            recv = "recvNoWorkspace";
        } else {
            Optional<IRExpr> lowered = adapter.adapt(receiver, ws);
            recv = lowered.isPresent()
                    ? "recvLowers." + enumSeatLoweredShapeToken(lowered.get())
                    : "recvBlocked." + reasonHead(receiver, ws);
        }
        return feat + "." + recv;
    }

    /** The #507 arm-A root-seat facet — grammar at {@link #deepVisitFacets}. */
    private String deepVisitFacet(RDeepFeatureCall deep) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "deepVisit.noWorkspace";
        }
        String parent = deep.parent() == null ? "orphan"
                : deep.parent().getClass().getSimpleName();
        RMetaAnnotatedType t = ws.getInferredType(deep);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        return "deepVisit." + deepShapeFacet(deep, ws) + "." + parent + "." + typing;
    }

    /**
     * The #507 arm-B census classifier for {@link #noChanGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the exact {@code noResolutionChannel} reason spelling
     * (every other token a no-op). Read-only against the tree (resolved-model lookups only) —
     * the standing counters cannot move.
     */
    private void classifyNoChanGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        // PREFIX-matched (the #478 channel-witness law): since the #507 record arm the residue
        // composes the arm's own gate onto the channel token (noResolutionChannel.<facet>), and
        // the census keys the CHANNEL so its buckets stay comparable across the teach.
        if (!(expr instanceof REnumValueRef enr) || !reason.startsWith("noResolutionChannel")) {
            return;
        }
        RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        String scope = fn != null && fn.dispatchBase().isPresent() ? "dispatch" : "plain";
        String head = "headMiss";
        RAttribute headAttr = null;
        String headName = enr.enumName();
        if (fn != null && headName != null) {
            for (RAttribute in : fn.inputs()) {
                if (headName.equals(in.name())) {
                    headAttr = in;
                    head = "input." + attrTypeKindToken(in);
                    break;
                }
            }
            if (headAttr == null && fn.output().isPresent()
                    && headName.equals(fn.output().get().name())) {
                headAttr = fn.output().get();
                head = "output." + attrTypeKindToken(headAttr);
            }
            if (headAttr == null) {
                for (RShortcut sc : fn.shortcuts()) {
                    if (headName.equals(sc.name())) {
                        head = "alias";
                        break;
                    }
                }
            }
            if (headAttr == null && "headMiss".equals(head) && fn.dispatchBase().isPresent()) {
                for (RAttribute in : fn.dispatchBase().get().inputs()) {
                    if (headName.equals(in.name())) {
                        headAttr = in;
                        head = "dispatchInput." + attrTypeKindToken(in);
                        break;
                    }
                }
            }
        }
        String leaf = "leafMiss";
        String leafName = enr.valueName();
        if (headAttr != null && leafName != null) {
            RNode headType = headAttr.typeCall() == null ? null
                    : headAttr.typeCall().referencedType().orElse(null);
            if (headType instanceof RRecordType rec) {
                for (RRecordFeature f : rec.features()) {
                    if (leafName.equals(f.name())) {
                        leaf = "leafRecHit";
                        break;
                    }
                }
            } else if (headType instanceof RDataType data) {
                RAttribute leafAttr = HandlerHelper.findAttributeOnDataType(data, leafName);
                if (leafAttr != null) {
                    leaf = isMetaAnnotated(leafAttr) ? "leafAttrMetaHit" : "leafAttrHit";
                }
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "noChanGate." + head + "." + leaf + "." + scope + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "." + typing;
        noChanGateFacets.merge(token, 1, Integer::sum);
        if (!noChanGateFirstSamples.containsKey(token)) {
            noChanGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The declared-type kind token for the #507 noChanGate head ladder: {@code rec:<name>} for
     * the record classes ({@code date}/{@code dateTime}/{@code zonedDateTime} — the #444
     * clear-without-binding population), {@code data}/{@code enumT}/{@code basic} for the rest,
     * {@code typeMiss} when the type call never resolved.
     */
    private static String attrTypeKindToken(RAttribute attr) {
        RNode type = attr.typeCall() == null ? null
                : attr.typeCall().referencedType().orElse(null);
        if (type == null) {
            return "typeMiss";
        }
        if (type instanceof RRecordType rec) {
            return "rec:" + rec.name();
        }
        if (type instanceof RDataType) {
            return "data";
        }
        if (type instanceof REnumeration) {
            return "enumT";
        }
        return "basic";
    }

    /**
     * The #508 arm-A census classifier for {@link #metaHopGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the five exact sole-face reason spellings of the
     * composed meta-facet + alias-nav cluster (every other token a no-op). Read-only against
     * the tree and adapter-only (stateless by its documented contract; the re-adapted
     * equivalents are FRESH throwaway constructions — the #478 witness convention) — the
     * standing counters cannot move.
     */
    private void classifyMetaHopGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face;
        switch (reason) {
            case "inputFeatureNav.metaFeature" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                // The arm's OWN equivalent restated (disguisedInputNavEquivalent — the head via
                // legacy resolveNameInFunction's inputs→output order, the feature on the head's
                // declared data type), re-adapted read-only: the lowered IRMetaAccess's
                // receiver class + qualifiers = the identity-serve widening's shape read.
                RFeatureCall equivalent = disguisedNavEquivalentOf(enr);
                IRExpr lowered = equivalent == null || ws == null ? null
                        : adapter.adapt(equivalent, ws).orElse(null);
                String shape = lowered == null ? "adaptEmpty"
                        : lowered instanceof IRMetaAccess ma
                                ? "recv:" + ma.receiver().getClass().getSimpleName()
                                        + ".q:" + String.join("+", ma.metaQualifiers())
                                : "notMeta:" + lowered.getClass().getSimpleName();
                face = "l111Meta." + shape;
            }
            case "inputFeatureNav.featureUnresolved.aliasHead.bodyTyped.featureOnBodyMeta" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                face = "aliasMeta.enr." + aliasMetaHopFacet(enr, enr.enumName(),
                        enr.valueName(), ws);
            }
            case "featureUnresolved.aliasHead.bodyTyped.featureOnBodyMeta" -> {
                if (!(expr instanceof RFeatureCall fc)
                        || !(fc.receiver() instanceof RSymbolReference head)) {
                    return;
                }
                face = "aliasMeta.fc." + aliasMetaHopFacet(fc, head.name(), fc.featureName(), ws)
                        + (head.symbol().orElse(null) instanceof RShortcut
                                ? ".bound" : ".collision");
            }
            case "featureUnresolved.aliasHead.bodyTyped.featureOnBody" -> {
                if (!(expr instanceof RFeatureCall fc)
                        || !(fc.receiver() instanceof RSymbolReference head)) {
                    return;
                }
                // The #492 arm's own admission axes at the PARSED seat: the head's bind class,
                // the body's engine cardinality (the receiver-retype channel), the resolved
                // member's own step cardinality and the hop type's presence (the arm's last
                // gate) — the RFeatureCall-twin leg's exact viability read.
                RFunction enclosing = HandlerHelper.findEnclosingFunction(fc);
                RShortcut shortcut = head.symbol().orElse(null) instanceof RShortcut bound
                        ? bound : shortcutNamed(enclosing, head.name());
                String detail;
                if (shortcut == null || ws == null) {
                    detail = "shortcutUnresolvable";
                } else {
                    RExpression body = shortcut.expression();
                    ExpressionCardinality bodyCard = body == null ? null
                            : ws.getRuleBodyCardinality(body);
                    RMetaAnnotatedType bodyType = body == null ? null : ws.getInferredType(body);
                    RDataType bodyData = bodyType != null
                            && bodyType.type() instanceof RDataTypeRef dtr ? dtr.astNode() : null;
                    RAttribute member = bodyData == null || fc.featureName() == null ? null
                            : HandlerHelper.findAttributeOnDataType(bodyData, fc.featureName());
                    RMetaAnnotatedType hopType = member == null ? null
                            : ws.getInferredAttributeType(member);
                    detail = (bodyCard == ExpressionCardinality.MULTI ? "bodyM" : "bodyS")
                            + "." + (member == null ? "memberMiss"
                                    : isMultiCardinalityShape(member) ? "hopM" : "hopS")
                            + "." + (hopType == null || hopType.isMissing()
                                    ? "hopUntyped" : "hopTyped");
                }
                face = "aliasPlain." + detail
                        + (head.symbol().orElse(null) instanceof RShortcut
                                ? ".bound" : ".collision");
            }
            case "attributeChain.equivalentMetaAccess" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                // The arm's OWN equivalent restated (implicitItemChainEquivalent — the 2-hop
                // item -> head -> leaf over a fresh synthetic implicit), re-adapted read-only:
                // the FULL lowered spine the 2-link gate excluded — the acceptance-widening's
                // exact shape read (the #501 census read only the LEGACY-synthesized twin).
                REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
                RAttribute head = chain == null ? null : chain.attributeOpt().orElse(null);
                RAttribute leaf = chain == null ? null : chain.feature();
                if (head == null) {
                    RExpression src = binderSourceOf(enr);
                    RDataType elem = src == null ? null
                            : ExpressionToIRAdapter.sourceElementDataType(src);
                    head = elem == null || enr.enumName() == null ? null
                            : HandlerHelper.findAttributeOnDataType(elem, enr.enumName());
                }
                if (leaf == null && head != null && enr.valueName() != null) {
                    RNode headType = head.typeCall() == null ? null
                            : head.typeCall().referencedType().orElse(null);
                    leaf = headType instanceof RDataType hd
                            ? HandlerHelper.findAttributeOnDataType(hd, enr.valueName()) : null;
                }
                String hopMeta = head != null && isMetaAnnotated(head)
                        ? (leaf != null && isMetaAnnotated(leaf) ? "metaBoth" : "metaHead")
                        : "metaLeaf";
                RAttribute metaHop = leaf != null && isMetaAnnotated(leaf) ? leaf
                        : head != null && isMetaAnnotated(head) ? head : leaf;
                String quals = metaHop == null ? "hopUnbound" : metaQualifierToken(metaHop);
                String spine;
                if (head == null || leaf == null || ws == null) {
                    spine = "chainUnbound";
                } else {
                    RImplicitVariable item = new RImplicitVariable();
                    item.setSynthetic(true);
                    item.setParent(enr.parent());
                    RFeatureCall headCall = new RFeatureCall();
                    headCall.setReceiver(item);
                    headCall.setFeatureName(head.name());
                    headCall.setResolvedFeature(head);
                    headCall.setParent(enr.parent());
                    RFeatureCall leafCall = new RFeatureCall();
                    leafCall.setReceiver(headCall);
                    leafCall.setFeatureName(leaf.name());
                    leafCall.setResolvedFeature(leaf);
                    leafCall.setParent(enr.parent());
                    IRExpr lowered = adapter.adapt(leafCall, ws).orElse(null);
                    spine = lowered == null ? "adaptEmpty" : loweredSpineToken(lowered, 0);
                }
                face = "chainMeta." + spine + "." + hopMeta + "." + quals;
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "metaHopGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        metaHopGateFacets.merge(token, 1, Integer::sum);
        if (!metaHopGateFirstSamples.containsKey(token)) {
            metaHopGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #508 arm-A alias-META hop facet (the {@code aliasMeta} face's shared detail): the
     * enclosing function's shortcut by the head name (the disguise/collision walk both seats
     * share), the BODY's engine cardinality (the receiver-retype channel the meta-twin leg
     * would consume) and the resolved meta member's qualifier list.
     */
    private String aliasMetaHopFacet(RExpression at, String headName, String leafName,
            RWorkspace ws) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(at);
        RShortcut shortcut = shortcutNamed(enclosing, headName);
        if (shortcut == null || ws == null) {
            return "shortcutUnresolvable";
        }
        RExpression body = shortcut.expression();
        ExpressionCardinality bodyCard = body == null ? null : ws.getRuleBodyCardinality(body);
        RMetaAnnotatedType bodyType = body == null ? null : ws.getInferredType(body);
        RDataType bodyData = bodyType != null && bodyType.type() instanceof RDataTypeRef dtr
                ? dtr.astNode() : null;
        RAttribute member = bodyData == null || leafName == null ? null
                : HandlerHelper.findAttributeOnDataType(bodyData, leafName);
        return (bodyCard == ExpressionCardinality.MULTI ? "bodyM" : "bodyS")
                + "." + (member == null ? "memberMiss" : "q:" + metaQualifierToken(member));
    }

    /**
     * The #508 arm-A L-111 equivalent restated read-only ({@code disguisedInputNavEquivalent}'s
     * construction: the head via legacy {@code resolveNameInFunction}'s inputs→output order,
     * the feature resolved on the head's DECLARED data type) — a fresh throwaway node pair,
     * joins no tree.
     */
    private static RFeatureCall disguisedNavEquivalentOf(REnumValueRef enr) {
        if (enr.enumName() == null || enr.valueName() == null) {
            return null;
        }
        RFunction fn = HandlerHelper.findEnclosingFunction(enr);
        RAttribute head = null;
        if (fn != null) {
            for (RAttribute in : fn.inputs()) {
                if (enr.enumName().equals(in.name())) {
                    head = in;
                    break;
                }
            }
            if (head == null && fn.output().isPresent()
                    && enr.enumName().equals(fn.output().get().name())) {
                head = fn.output().get();
            }
        }
        RSymbolReference receiver = new RSymbolReference();
        receiver.setName(enr.enumName());
        receiver.setParent(enr.parent());
        RFeatureCall fc = new RFeatureCall();
        fc.setReceiver(receiver);
        fc.setFeatureName(enr.valueName());
        fc.setParent(enr.parent());
        if (head != null) {
            receiver.setResolvedSymbol(head);
            RNode headType = head.typeCall() == null ? null
                    : head.typeCall().referencedType().orElse(null);
            RAttribute feature = headType instanceof RDataType hd
                    ? HandlerHelper.findAttributeOnDataType(hd, enr.valueName()) : null;
            if (feature != null) {
                fc.setResolvedFeature(feature);
            }
        }
        return fc;
    }

    /**
     * The #508 arm-A spine decode: the lowered chain equivalent's link classes top-down
     * ({@code top:<K>.inner:<K>.base:<K>} through {@link FieldAccess}/{@link IRMetaAccess}
     * links; a non-link node terminates the descent as its own class token) — the exact form
     * the 2-link gate excluded.
     */
    private static String loweredSpineToken(IRExpr node, int depth) {
        String label = depth == 0 ? "top" : depth == 1 ? "inner" : "base";
        if (node instanceof FieldAccess fa) {
            return label + ":FieldAccess"
                    + (depth < 2 ? "." + loweredSpineToken(fa.receiver(), depth + 1) : "");
        }
        if (node instanceof IRMetaAccess ma) {
            return label + ":IRMetaAccess"
                    + (depth < 2 ? "." + loweredSpineToken(ma.receiver(), depth + 1) : "");
        }
        return label + ":" + node.getClass().getSimpleName();
    }

    /**
     * The #508 arm-B census classifier for {@link #metaSrcGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the two exact sole-face reason spellings of the L-029
     * meta-sourced lambda pair (every other token a no-op). Read-only against the tree
     * (resolved-model lookups + the adapter's static source walks) — the standing counters
     * cannot move.
     */
    private void classifyMetaSrcGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face;
        switch (reason) {
            case "attributeChain.closureParamHead.twNull" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                String headName = enr.enumName();
                RExpression src = headName == null ? null : closureBinderSourceOf(enr, headName);
                RDataType elem = src == null ? null
                        : ExpressionToIRAdapter.sourceElementDataType(src);
                String leafV;
                if (elem == null) {
                    leafV = "leafNoElem";
                } else {
                    RAttribute leaf = enr.valueName() == null ? null
                            : HandlerHelper.findAttributeOnDataType(elem, enr.valueName());
                    leafV = leaf == null ? "leafMiss"
                            : isMetaAnnotated(leaf) ? "leafMeta" : "leafPlain";
                }
                face = "cpSrc." + bindingSourceShapeToken(src) + "."
                        + (elem == null ? "elemMiss" : "elemHit:" + elem.name()) + "." + leafV;
            }
            case "itemMetaSourced" -> {
                if (!(expr instanceof RFeatureCall fc)
                        || !(fc.receiver() instanceof RImplicitVariable iv)) {
                    return;
                }
                RInlineFunction inline = nearestInlineFn(iv);
                RNode bp = inline == null ? null : inline.parent();
                String binder = bp == null ? "noBinder" : bp.getClass().getSimpleName();
                RExpression src = bp instanceof RFilterExpr filter ? filter.argument()
                        : bp instanceof RExtractExpr extract ? extract.argument()
                                : bp instanceof RThenExpr then ? then.argument()
                                        : bp instanceof RMinExpr min ? min.argument()
                                                : bp instanceof RMaxExpr max ? max.argument()
                                                        : bp instanceof RSortExpr sort
                                                                ? sort.argument() : null;
                String featClass = fc.resolvedFeature()
                        .map(f -> isMetaAnnotated(f) ? "featMeta" : "featPlain")
                        .orElse("featUnresolved");
                face = "itemSrc." + binder + "." + bindingSourceShapeToken(src) + "." + featClass;
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "metaSrcGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        metaSrcGateFacets.merge(token, 1, Integer::sum);
        if (!metaSrcGateFirstSamples.containsKey(token)) {
            metaSrcGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #508 arm-B binding-source shape ladder: the L-029 split's own dominant classes —
     * {@code metaFc}/{@code plainFc} (a feature-call source by its resolved feature's
     * annotation), {@code argsCall} (an args-present reference — the #506 sort-coercion catch's
     * class), {@code alias} (a shortcut-bound or collision head), {@code pipe:<K>} (a
     * then/extract pipe), {@code other:<K>}, {@code noSource}.
     */
    private static String bindingSourceShapeToken(RExpression src) {
        if (src == null) {
            return "noSource";
        }
        if (src instanceof RFeatureCall fc) {
            return fc.resolvedFeature().map(f -> isMetaAnnotated(f) ? "metaFc" : "plainFc")
                    .orElse("unresFc");
        }
        if (src instanceof RSymbolReference sr) {
            if (!sr.args().isEmpty()) {
                return "argsCall";
            }
            if (sr.symbol().orElse(null) instanceof RShortcut) {
                return "alias";
            }
            RFunction fn = HandlerHelper.findEnclosingFunction(sr);
            return shortcutNamed(fn, sr.name()) != null ? "alias"
                    : "bareRef";
        }
        if (src instanceof RThenExpr || src instanceof RExtractExpr) {
            return "pipe:" + src.getClass().getSimpleName();
        }
        return "other:" + src.getClass().getSimpleName();
    }

    /**
     * The #508 arm-C census classifier for {@link #symNotGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the exact {@code symbolNotAttribute} reason spelling
     * (every other token a no-op). Re-runs the STANDING #504 arms' own gate ladders
     * ({@code requalifiedEnumArg}/{@code requalifiedEnumComparand} — restated read-only against
     * the REAL parent context) to name why the taught recovery does not fire on the residue.
     * Read-only against the tree and adapter-only — the standing counters cannot move.
     */
    private void classifySymNotGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference sym) || !"symbolNotAttribute".equals(reason)) {
            return;
        }
        RNode symbol = sym.symbol().orElse(null);
        String symClass = symbol == null ? "nullSym" : symbol.getClass().getSimpleName();
        RNode parent = sym.parent();
        String seatAndVerdict;
        if (parent instanceof RSymbolReference call && !call.args().isEmpty()) {
            int idx = call.args().indexOf(sym);
            RNode callee = call.symbol().orElse(null);
            RFunction fn = callee instanceof RFunction f ? f
                    : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
            String fnKind = callee instanceof RRule ? "ruleCallee"
                    : callee instanceof RFunction ? "fnCallee" : "otherCallee";
            String verdict;
            if (!(symbol == null || symbol instanceof RBody || symbol instanceof RDataType
                    || symbol instanceof RCorpus)) {
                verdict = "classExcluded";
            } else if (fn == null || idx < 0 || idx >= fn.inputs().size()) {
                verdict = "noParam";
            } else {
                RAttribute param = fn.inputs().get(idx);
                RNode referenced = param.typeCall() == null ? null
                        : param.typeCall().referencedType().orElse(null);
                if (!(referenced instanceof REnumeration en)) {
                    verdict = "paramNotEnum";
                } else if (!enumOwnValueNamed(en, sym.name())) {
                    verdict = "valueMiss:" + en.name();
                } else {
                    // The arm WOULD mint in context — the loss is the CONTAINING claim's own
                    // routing; the parent apply's verdict names which side.
                    IRExpr applyLowered = ws == null ? null
                            : adapter.adapt(call, ws).orElse(null);
                    verdict = "argFires:" + en.name() + "."
                            + (applyLowered == null ? "applyDeclines" : "applyLowers");
                }
            }
            seatAndVerdict = "argOf." + fnKind + "." + verdict;
        } else if (parent instanceof REqualityExpr eq) {
            RExpression sibRaw = eq.rawLeft() == sym ? eq.rawRight() : eq.rawLeft();
            String verdict;
            if (!(symbol == null || symbol instanceof RDataType || symbol instanceof RChoice
                    || symbol instanceof RBody)) {
                verdict = "classExcluded";
            } else {
                RMetaAnnotatedType sibType = sibRaw == null || ws == null ? null
                        : ws.getInferredType(sibRaw);
                if (sibType == null || sibType.isMissing()) {
                    verdict = "sibUntyped";
                } else if (!(sibType.type() instanceof REnumTypeRef enumRef)) {
                    verdict = "sibNotEnum";
                } else if (!enumOwnValueNamed(enumRef.astNode(), sym.name())) {
                    verdict = "valueMiss:" + enumRef.name();
                } else {
                    IRExpr eqLowered = ws == null ? null : adapter.adapt(eq, ws).orElse(null);
                    verdict = "eqFires:" + enumRef.name() + "."
                            + (eqLowered == null ? "eqDeclines" : "eqLowers");
                }
            }
            seatAndVerdict = "eqSib." + verdict;
        } else {
            seatAndVerdict = "other:"
                    + (parent == null ? "noParent" : parent.getClass().getSimpleName());
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(sym);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "symNotGate." + symClass + "." + seatAndVerdict + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                + typing;
        symNotGateFacets.merge(token, 1, Integer::sum);
        if (!symNotGateFirstSamples.containsKey(token)) {
            symNotGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The engine-type kind token for the #509 gate censuses: {@code dt:<Name>} /
     * {@code choice:<Name>} / {@code rec:<name>} / {@code enumT:<Name>} /
     * {@code basic:<name>} (the scalar RType variants by their own {@code name()}) /
     * {@code ctMiss} when the engine channel never typed the node.
     */
    private static String inferredTypeKindToken(RMetaAnnotatedType t) {
        if (t == null || t.isMissing()) {
            return "ctMiss";
        }
        RType ty = t.type();
        if (ty instanceof RDataTypeRef dtr) {
            return "dt:" + dtr.name();
        }
        if (ty instanceof RChoiceTypeRef ch) {
            return "choice:" + ch.name();
        }
        if (ty instanceof com.regnosys.rosetta.types.RRecordType rec) {
            return "rec:" + rec.name();
        }
        if (ty instanceof REnumTypeRef en) {
            return "enumT:" + en.name();
        }
        return "basic:" + ty.name();
    }

    /**
     * The feature-membership token for the #509 gate censuses — the by-name resolution of
     * {@code featureName} against an engine type: {@code mHit:plain}/{@code mHit:meta} on a
     * data type ({@link HandlerHelper#findAttributeOnDataType}), {@code mRecHit} on a record
     * type's own feature list (the #507 29th-kind membership rule),
     * {@code mChoiceHit:plain}/{@code mChoiceHit:meta} on a choice's {@code asRDataType}
     * projection (at a RESOLVED-receiver seat a DRIFT face — the #503 arm-B1 claims it; at
     * the alias-body seat the live {@code offBody} class), {@code mMiss} otherwise,
     * {@code noCt} when the type channel is empty.
     */
    private static String memberOnTypeToken(RMetaAnnotatedType t, String featureName) {
        if (t == null || t.isMissing() || featureName == null) {
            return "noCt";
        }
        RType ty = t.type();
        if (ty instanceof RDataTypeRef dtr && dtr.astNode() != null) {
            RAttribute m = HandlerHelper.findAttributeOnDataType(dtr.astNode(), featureName);
            return m == null ? "mMiss" : isMetaAnnotated(m) ? "mHit:meta" : "mHit:plain";
        }
        if (ty instanceof com.regnosys.rosetta.types.RRecordType rec) {
            for (com.regnosys.rosetta.types.RecordFeature f : rec.features()) {
                if (featureName.equals(f.name())) {
                    return "mRecHit";
                }
            }
            return "mMiss";
        }
        if (ty instanceof RChoiceTypeRef ch) {
            RDataType projected = ch.asRDataType();
            RAttribute m = projected == null ? null
                    : HandlerHelper.findAttributeOnDataType(projected, featureName);
            return m == null ? "mMiss"
                    : isMetaAnnotated(m) ? "mChoiceHit:meta" : "mChoiceHit:plain";
        }
        return "mMiss";
    }

    /**
     * The #509 ladder-channel member token — the feature's resolution against a resolved
     * ladder attribute's DECLARED type (the channel the ladder itself names, not the engine
     * read): {@code mRecHit} on a record type's own feature list, {@code mHit:plain}/
     * {@code mHit:meta}/{@code mMiss} on a data type, {@code mOff} otherwise.
     */
    private static String memberOnDeclaredTypeToken(RAttribute holder, String featureName) {
        RNode type = holder == null || holder.typeCall() == null ? null
                : holder.typeCall().referencedType().orElse(null);
        if (type == null || featureName == null) {
            return "mOff";
        }
        if (type instanceof RRecordType rec) {
            for (RRecordFeature f : rec.features()) {
                if (featureName.equals(f.name())) {
                    return "mRecHit";
                }
            }
            return "mMiss";
        }
        if (type instanceof RDataType data) {
            RAttribute m = HandlerHelper.findAttributeOnDataType(data, featureName);
            return m == null ? "mMiss" : isMetaAnnotated(m) ? "mHit:meta" : "mHit:plain";
        }
        return "mOff";
    }

    /**
     * The #509 arm-A census classifier for {@link #aliasIdGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the two exact parsed-seat alias-nav decline spellings
     * (every other token a no-op). Re-derives the #508 arm's own shortcut lookup (the dual
     * head path) and the strong-guard verdict
     * ({@link ExpressionToIRAdapter#isProvablyNonMetaElementSource}) read-only. The standing
     * counters cannot move.
     */
    private void classifyAliasIdGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        boolean onBody = "featureUnresolved.aliasHead.bodyTyped.featureOnBody".equals(reason);
        boolean offBody = "featureUnresolved.aliasHead.bodyTyped.featureOffBody".equals(reason);
        if (!(expr instanceof RFeatureCall fc) || (!onBody && !offBody)) {
            return;
        }
        String face;
        RSymbolReference head = fc.receiver() instanceof RSymbolReference h ? h : null;
        RShortcut shortcut = head == null ? null
                : head.symbol().orElse(null) instanceof RShortcut bound ? bound
                        : shortcutNamed(HandlerHelper.findEnclosingFunction(fc), head.name());
        if (shortcut == null || ws == null) {
            face = (onBody ? "onBody" : "offBody") + ".contractMiss";
        } else {
            RExpression body = shortcut.expression();
            RMetaAnnotatedType bodyType = body == null ? null : ws.getInferredType(body);
            if (onBody) {
                String guard = ExpressionToIRAdapter.isProvablyNonMetaElementSource(body)
                        ? "guardPass" : "guardHeld";
                RDataType bodyData = bodyType != null
                        && bodyType.type() instanceof RDataTypeRef dtr ? dtr.astNode() : null;
                RAttribute member = bodyData == null || fc.featureName() == null ? null
                        : HandlerHelper.findAttributeOnDataType(bodyData, fc.featureName());
                String hop = member == null ? "hopUnbound"
                        : isMultiCardinalityShape(member) ? "hopM" : "hopS";
                face = "onBody." + guard + ".b:"
                        + (body == null ? "null" : body.getClass().getSimpleName())
                        + "." + inferredTypeKindToken(bodyType) + "." + hop;
            } else {
                face = "offBody." + inferredTypeKindToken(bodyType) + "."
                        + memberOnTypeToken(bodyType, fc.featureName());
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(fc);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "aliasIdGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        aliasIdGateFacets.merge(token, 1, Integer::sum);
        if (!aliasIdGateFirstSamples.containsKey(token)) {
            aliasIdGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #509 arm-B census classifier for {@link #headUnGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the exact {@code featureUnresolved.headUnresolved}
     * spelling (every other token a no-op). Re-runs the head-name resolution ladder in the
     * REAL context (enclosing-function inputs/output/dispatch-base per the #507 noChanGate
     * order, then the #503 closure-binder channel) and the adapter's own verdict on the head
     * node, read-only. The standing counters cannot move.
     */
    private void classifyHeadUnGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RFeatureCall fc)
                || !"featureUnresolved.headUnresolved".equals(reason)) {
            return;
        }
        RSymbolReference head = fc.receiver() instanceof RSymbolReference h ? h : null;
        String token;
        if (head == null || head.name() == null) {
            token = "headUnGate.contractMiss." + positionToken(expr, claimRoot) + "."
                    + enclosingContextToken(expr);
        } else {
            String name = head.name();
            RFunction fn = HandlerHelper.findEnclosingFunction(fc);
            String ladder = "noMatch";
            RAttribute ladderAttr = null;
            if (fn != null) {
                for (RAttribute in : fn.inputs()) {
                    if (name.equals(in.name())) {
                        ladderAttr = in;
                        ladder = "fnInput." + attrTypeKindToken(in);
                        break;
                    }
                }
                if ("noMatch".equals(ladder) && fn.output().isPresent()
                        && name.equals(fn.output().get().name())) {
                    ladderAttr = fn.output().get();
                    ladder = "fnOutput." + attrTypeKindToken(ladderAttr);
                }
                if ("noMatch".equals(ladder) && fn.dispatchBase().isPresent()) {
                    for (RAttribute in : fn.dispatchBase().get().inputs()) {
                        if (name.equals(in.name())) {
                            ladderAttr = in;
                            ladder = "dispatchInput." + attrTypeKindToken(in);
                            break;
                        }
                    }
                }
            }
            if (ladderAttr != null) {
                // The ladder-CHANNEL member verdict (the #507 record-nav mint's own
                // provability read — the engine ht below stays ctMiss for these heads).
                ladder += "." + memberOnDeclaredTypeToken(ladderAttr, fc.featureName());
            }
            if ("noMatch".equals(ladder) && registeringClosureBinder(fc, name) != null) {
                // The #503 arm-B2 gate ladder re-run fact-by-fact: the head-symbol state
                // (symInline = the bound flavor / symNull = the name-resolve flavor /
                // symOther:<K> = the identity gate's exclusion), the STRONG source proof the
                // arm's element walk gates on (isProvablyNonMetaElementSource — srcUnproven =
                // the meta-wrapped-source class, the #508 30th-kind sibling), the binding
                // source's shape, the census's own structural element read and the feature's
                // resolution on it.
                RNode sym = head.symbol().orElse(null);
                String symV = sym == null ? "symNull"
                        : RInlineFunction.isClosureParameterBinding(sym) ? "symInline"
                                : "symOther:" + sym.getClass().getSimpleName();
                RExpression src = closureBinderSourceOf(fc, name);
                String proof = src != null
                        && ExpressionToIRAdapter.isProvablyNonMetaElementSource(src)
                        ? "srcProven" : "srcUnproven";
                RDataType elem = src == null ? null
                        : ExpressionToIRAdapter.sourceElementDataType(src);
                String memberV;
                if (elem == null || fc.featureName() == null) {
                    memberV = "mNoElem";
                } else {
                    RAttribute m = HandlerHelper.findAttributeOnDataType(elem,
                            fc.featureName());
                    memberV = m == null ? "mMiss"
                            : isMetaAnnotated(m) ? "mHit:meta" : "mHit:plain";
                }
                ladder = "cpBind." + symV + "." + proof + "." + bindingSourceShapeToken(src)
                        + "." + (elem == null ? "twNull" : "twData:" + elem.name()) + "."
                        + memberV;
            }
            IRExpr lowered = ws == null ? null : adapter.adapt(head, ws).orElse(null);
            String adaptV = lowered == null ? "headBlocked"
                    : "headLowers:" + lowered.getClass().getSimpleName();
            RMetaAnnotatedType ht = ws == null ? null : ws.getInferredType(head);
            RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(fc);
            String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
            token = "headUnGate." + ladder + "." + adaptV + "." + inferredTypeKindToken(ht)
                    + "." + memberOnTypeToken(ht, fc.featureName()) + "."
                    + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                    + typing;
        }
        headUnGateFacets.merge(token, 1, Integer::sum);
        if (!headUnGateFirstSamples.containsKey(token)) {
            headUnGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #509 arm-B census classifier for {@link #nsrGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the exact {@code featureUnresolved.nonSymbolReceiver}
     * spelling (every other token a no-op). Reads the compound receiver's adapter verdict,
     * engine-type kind and feature membership, read-only. The standing counters cannot move.
     */
    private void classifyNsrGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RFeatureCall fc)
                || !"featureUnresolved.nonSymbolReceiver".equals(reason)) {
            return;
        }
        RExpression recv = fc.receiver();
        String token;
        if (recv == null || recv instanceof RSymbolReference) {
            token = "nsrGate.contractMiss." + positionToken(expr, claimRoot) + "."
                    + enclosingContextToken(expr);
        } else {
            IRExpr lowered = ws == null ? null : adapter.adapt(recv, ws).orElse(null);
            String adaptV = lowered == null ? "recvBlocked"
                    : "recvLowers:" + lowered.getClass().getSimpleName();
            RMetaAnnotatedType ct = ws == null ? null : ws.getInferredType(recv);
            String member = memberOnTypeToken(ct, fc.featureName());
            if (recv instanceof RImplicitVariable iv) {
                // The item-receiver detail (the #508 metaSrcGate itemSrc walk re-keyed at
                // THIS face): the binding lambda's op + the binding source's shape + the
                // META-QUALIFIER verdict — whether the nav's feature name is a metadata
                // qualifier the source's terminal resolved feature carries (the `item ->
                // scheme` class — a meta-leaf read over the bound element, the meta routing
                // law's own claim shape).
                RInlineFunction inline = nearestInlineFn(iv);
                RNode bp = inline == null ? null : inline.parent();
                String binder = bp == null ? "noBinder" : bp.getClass().getSimpleName();
                RExpression src = bp instanceof RFilterExpr filter ? filter.argument()
                        : bp instanceof RExtractExpr extract ? extract.argument()
                                : bp instanceof RThenExpr then ? then.argument()
                                        : bp instanceof RMinExpr min ? min.argument()
                                                : bp instanceof RMaxExpr max ? max.argument()
                                                        : bp instanceof RSortExpr sort
                                                                ? sort.argument() : null;
                String qual = "qMiss";
                if (src instanceof RFeatureCall srcFc && fc.featureName() != null
                        && srcFc.resolvedFeature().isPresent()) {
                    for (var ar : srcFc.resolvedFeature().get().annotationRefs()) {
                        if ("metadata".equals(ar.annotationName())
                                && fc.featureName().equals(ar.qualifierName().orElse(null))) {
                            qual = "qHit:" + fc.featureName();
                            break;
                        }
                    }
                }
                member += "." + binder + "." + bindingSourceShapeToken(src) + "." + qual;
            }
            RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(fc);
            String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
            token = "nsrGate." + recv.getClass().getSimpleName() + "." + adaptV + "."
                    + inferredTypeKindToken(ct) + "." + member
                    + "." + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr)
                    + "." + typing;
        }
        nsrGateFacets.merge(token, 1, Integer::sum);
        if (!nsrGateFirstSamples.containsKey(token)) {
            nsrGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #520 arm-A3 facet classifier for {@link #headOtherGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code featureUnresolved.headOther}
     * spelling (every other token a no-op). Read-only against the tree and adapter-only —
     * the standing counters cannot move.
     */
    private void classifyHeadOtherGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RFeatureCall fc)
                || !"featureUnresolved.headOther".equals(reason)) {
            return;
        }
        if (!(fc.receiver() instanceof RSymbolReference head)) {
            return; // contract-impossible (the facet implies a symbol head)
        }
        RNode sym = head.symbol().orElse(null);
        String symClass = sym == null ? "unresolved" : sym.getClass().getSimpleName();
        RMetaAnnotatedType ht = ws == null ? null : ws.getInferredType(head);
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(fc);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "headOtherGate." + symClass + "."
                + memberOnTypeToken(ht, fc.featureName()) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr)
                + "." + typing;
        headOtherGateFacets.merge(token, 1, Integer::sum);
        if (!headOtherGateFirstSamples.containsKey(token)) {
            headOtherGateFirstSamples.put(token, enclosingSampleToken(expr)
                    + " head=" + head.name() + " feature=" + fc.featureName());
        }
    }

    /**
     * The #524 pre-arm census classifier for {@link #itemAttrGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the six exact {@code symbolUnresolved}
     * in-lambda reason spellings (every other token a no-op). Re-runs LEGACY's OWN
     * bare-name ladder BY CALL ({@link ReferenceHandler#synthesizeImplicitItemBareNav} —
     * the #523 C2 by-call pattern at the BARE-symbol seat) and reads the adapter's verdict
     * on the exact synthesized equivalent. Read-only against the tree and adapter-only
     * (stateless by its documented contract) — the standing counters cannot move.
     */
    private void classifyItemAttrGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face = switch (reason) {
            case "symbolUnresolved.synthetic.itemAttrMeta" -> "iaMeta";
            case "symbolUnresolved.synthetic.itemAttr" -> "iaPlain";
            case "symbolUnresolved.lambdaSrcUnprovable.global.type" -> "lsuType";
            case "symbolUnresolved.lambdaSrcUnprovable.global.typeAlias" -> "lsuAlias";
            case "symbolUnresolved.lambda.absent" -> "lAbsent";
            case "symbolUnresolved.synthetic.lambda.global.type" -> "synLgt";
            default -> null;
        };
        if (face == null || !(expr instanceof RSymbolReference sym)) {
            return;
        }
        RFeatureCall equivalent = ReferenceHandler.synthesizeImplicitItemBareNav(sym, this);
        String leg;
        String member;
        if (equivalent == null) {
            leg = "lgNull";
            member = "mNone";
        } else if (equivalent.resolvedFeature().isPresent()) {
            RAttribute fired = equivalent.resolvedFeature().get();
            leg = "lgFires";
            member = isMetaAnnotated(fired) ? "mMeta:" + metaQualsToken(fired) : "mPlain";
        } else {
            // The #348 metaPathShortForm leg — the synthesized read over a META-WRAPPER
            // item deliberately carries NO resolved feature (the render seat resolves).
            leg = "lgMsf";
            member = "mNone";
        }
        String adaptV;
        if (equivalent == null) {
            adaptV = "avNone";
        } else {
            IRExpr lowered = ws == null ? null : adapter.adapt(equivalent, ws).orElse(null);
            adaptV = lowered != null ? "av:" + lowered.getClass().getSimpleName()
                    : adapterVerdictOnEquivalent(equivalent, ws);
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(sym);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "iaGate." + face + ".n:" + sym.name() + "." + leg + "." + member + "."
                + adaptV + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        itemAttrGateFacets.merge(token, 1, Integer::sum);
        if (!itemAttrGateFirstSamples.containsKey(token)) {
            itemAttrGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #526 pre-arm census classifier for {@link #neGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the child-subtree not-expressible {@code family:reason} pairs
     * (the four live faces + the same-predicate sibling seats; every other token a no-op).
     * Resolves the NAMED slot per the adapter's own {@code reasonFor*} order (a list names its
     * FIRST failing element — the adapter's own return — and counts the rest) and classifies it
     * BY CALL against the adapter's own position-divergence predicates
     * ({@code slotNotExpressibleAtChild} / {@code isCtorSlotConditional} /
     * {@code isCtorBodyLambda} — the #524 census BY-CALL law), plus the slot's ROOT re-adapt
     * verdict and the raw parent-chain glue path to the claim root. Read-only against the tree
     * and adapter-only (stateless by its documented contract) — the standing counters cannot
     * move.
     */
    private void classifyNeGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face = null;
        RExpression slot = null;
        String failsFacet = "";
        if (expr instanceof RListLiteral list && "elementNotExpressible".equals(reason)) {
            face = "elem";
            List<RExpression> elems = list.elements();
            int failing = 0;
            for (RExpression e : elems) {
                if (adapter.slotNotExpressibleAtChild(e, ws)) {
                    failing++;
                    if (slot == null) {
                        slot = e; // the FIRST failing element — the reasonFor return's own slot
                    }
                }
            }
            failsFacet = ".fails:" + failing + "of" + elems.size();
        } else if (expr instanceof RConditionalExpr cond) {
            switch (reason) {
                case "conditionNotExpressible" -> {
                    face = "cond";
                    slot = cond.condition();
                }
                case "thenNotExpressible" -> {
                    face = "then";
                    slot = cond.thenBranch();
                }
                case "elseNotExpressible" -> {
                    face = "else";
                    slot = cond.elseBranch().orElse(null);
                }
                default -> {
                }
            }
        } else if (expr instanceof RExtractExpr extract) {
            if ("receiverNotExpressible".equals(reason)) {
                face = "recv.extract";
                slot = extract.argument();
            } else if ("bodyNotExpressible".equals(reason)) {
                face = "body.extract";
                slot = extract.body() == null ? null : extract.body().body();
            }
        } else if (expr instanceof RFilterExpr filter) {
            if ("receiverNotExpressible".equals(reason)) {
                face = "recv.filter";
                slot = filter.argument();
            } else if ("bodyNotExpressible".equals(reason)) {
                face = "body.filter";
                slot = filter.body() == null ? null : filter.body().body();
            }
        } else if (expr instanceof RCountExpr count && "receiverNotExpressible".equals(reason)) {
            face = "recv.count";
            slot = count.argument();
        } else if (expr instanceof RToStringExpr ts && "receiverNotExpressible".equals(reason)) {
            face = "recv.toString";
            slot = ts.argument();
        } else if (expr instanceof RConversionExpr conv && "argNotExpressible".equals(reason)) {
            face = "arg.conv";
            slot = conv.argument();
        }
        if (face == null) {
            return;
        }
        String token;
        if (slot == null) {
            // contract-impossible (the reason token implies the slot) — the defensive belt
            token = "neGate." + face + ".slotMiss." + positionToken(expr, claimRoot) + "."
                    + enclosingContextToken(expr);
        } else {
            String adaptV = ws == null ? "avNoWs"
                    : adapter.adapt(slot, ws).map(l -> "av:" + l.getClass().getSimpleName())
                            .orElse("avBlocked");
            RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
            String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
            token = "neGate." + face + failsFacet + "." + neSlotShapeToken(slot, ws) + "."
                    + adaptV + "." + positionToken(expr, claimRoot) + "."
                    + neGatePathToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                    + typing;
        }
        neGateFacets.merge(token, 1, Integer::sum);
        if (!neGateFirstSamples.containsKey(token)) {
            neGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #527 pre-arm census classifier for {@link #argGapGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the {@code argNav.typeGap.}/{@code argNav.typeMissing.}
     * reason prefixes (every other token a no-op); the blocker node is the CALL whose argument
     * chain the #489 type-agreement gate (or the #488 hop-flag belt) declined — the arg itself
     * lowers, the call's arg gate declines (the #505 argResidueGate convention). Each NAV-shaped
     * argument (its solo re-adapt lowers to a {@link FieldAccess} chain) carries legacy's own
     * coercion-arm verdict BY CALL ({@link #legacyArgCoercionVerdict}), and the claim root
     * carries the standing post-pin scan's verdict ({@link #subtreeTripsPostPinCoercion} — the
     * shared witness detail field is snapshot/restored, so the guard's own trip prints cannot
     * be disturbed). Read-only against the tree and adapter-only — the standing counters cannot
     * move.
     */
    private void classifyArgGapGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if ((!reason.startsWith("argNav.typeGap.") && !reason.startsWith("argNav.typeMissing."))
                || !(expr instanceof RSymbolReference call) || call.args().isEmpty()) {
            return;
        }
        String tail = reason.substring("argNav.".length());
        RNode callee = call.symbol().orElse(null);
        String calleeTok = callee instanceof RFunction ? "fn"
                : callee instanceof RRule ? "rule" : "otherCallee";
        RFunction fnCallee = callee instanceof RFunction f ? f
                : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
        // The per-NAV-arg verdict sequence (the reason mirror scans args in order, so the
        // sequence's first non-javaEq token is the declining seat's own class read).
        StringBuilder verdicts = new StringBuilder();
        List<RExpression> args = call.args();
        for (int i = 0; i < args.size(); i++) {
            RExpression arg = args.get(i);
            IRExpr lowered = ws == null || arg == null ? null
                    : adapter.adapt(arg, ws).orElse(null);
            if (!(lowered instanceof FieldAccess)) {
                continue; // only nav-chain args carry the #489 gate's verdict
            }
            verdicts.append(verdicts.length() == 0 ? "v:" : "+")
                    .append(legacyArgCoercionVerdict(arg, fnCallee, i));
        }
        String verdictTok = verdicts.length() == 0 ? "vNoNavArg" : verdicts.toString();
        String tripDetailSnapshot = postPinTripDetail;
        PostPinArm scanArm = subtreeTripsPostPinCoercion(claimRoot);
        postPinTripDetail = tripDetailSnapshot;
        String scanTok = scanArm == null ? "scanNone" : "scan:" + scanArm.label();
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(call);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "argGapGate." + tail + "." + calleeTok + "." + verdictTok + "." + scanTok
                + "." + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                + typing;
        argGapGateFacets.merge(token, 1, Integer::sum);
        if (!argGapGateFirstSamples.containsKey(token)) {
            argGapGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #527 census's legacy-arm verdict for one nav-chain call argument — legacy
     * {@code ReferenceHandler.tryMetaDerefArg}'s own precondition ladder BY CALL over the SAME
     * oracles ({@code getType(param)}/{@code toJavaReferenceType}/{@code isBigDecimal}/
     * {@code isInteger}/{@code isBigInteger}), in legacy's own gate order: the multi-param and
     * unreadable-side early raw-passes, the Java-equality raw-pass, then the two numeric hoist
     * arms ({@code Integer}/{@code BigInteger} actual into a {@code BigDecimal} param — the #277/
     * #128 statement hoists). A nav arg is never an {@code RIntLiteral} and the argNav chains
     * are meta-free by declaration (the #488 hop flags decline meta features before the
     * agreement gate can spell), so the literal-inline and meta-deref arms cannot apply at this
     * seat; everything that survives the ladder is {@code rawNoArm} — legacy passes the arg RAW
     * and the flat native render is byte-identical by that read.
     */
    private String legacyArgCoercionVerdict(RExpression arg, RFunction callee, int argIndex) {
        if (callee == null || argIndex >= callee.inputs().size()) {
            return "noParam";
        }
        GeneratorModel gm = getGeneratorModel();
        if (gm == null || getTypeTranslator() == null || getTypeUtil() == null) {
            return "noOracles";
        }
        RAttribute param = callee.inputs().get(argIndex);
        if (gm.isMulti(param)) {
            return "multiParam"; // tryMetaDerefArg's isMulti early raw-pass
        }
        RType paramRType = gm.getType(param);
        JavaClass<?> paramJava = paramRType == null || paramRType instanceof RMissingType ? null
                : getTypeTranslator().toJavaReferenceType(paramRType);
        if (paramJava == null) {
            return "paramUnread"; // legacy raw-passes (the paramRType/paramJavaType gates)
        }
        RWorkspace ws = gm.workspace();
        RMetaAnnotatedType argT = ws == null ? null : ws.getInferredType(arg);
        JavaClass<?> actualJava = argT == null || argT.isMissing() || argT.type() == null ? null
                : getTypeTranslator().toJavaReferenceType(argT.type());
        if (actualJava == null) {
            return "actualUnread"; // the inferred-read channel only — see the field javadoc
        }
        if (paramJava.equals(actualJava)) {
            return "javaEq"; // the raw-pass equality precondition
        }
        if (getTypeUtil().isBigInteger(actualJava) && getTypeUtil().isBigDecimal(paramJava)) {
            return "coerceBigIntBD"; // the #128 hoist arm
        }
        if (getTypeUtil().isInteger(actualJava) && getTypeUtil().isBigDecimal(paramJava)) {
            return "coerceIntBD"; // the #277 nav-chain hoist arm
        }
        return "rawNoArm";
    }

    /**
     * The #528 pre-arm census classifier for {@link #opResGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the five operand/arg residue reason spellings (every
     * other token a no-op). The operand faces enumerate every seat that can mint their
     * spelling; at the two LIVE seats (logical / comparison) the failing side is read BY CALL
     * off the adapter's own gate ({@link #opResFailingSide}), and at the zero-carrier negative
     * seats off the face's lowered KIND (left-first — the mirrors' own order, which given the
     * self-gate names the same operand). Read-only against the tree and adapter-only — the
     * standing counters cannot move.
     */
    private void classifyOpResGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face = switch (reason) {
            case "operandBareBooleanNav" -> "logNav";
            case "operand:IRPointFreeApply" -> "pfOperand";
            case "operandItem" -> "itemOperand";
            case "calleeMetaParam" -> "cMetaP";
            case "argItem" -> "argItem";
            default -> null;
        };
        if (face == null) {
            return;
        }
        String detail = "cMetaP".equals(face) || "argItem".equals(face)
                ? opResApplyDetail(face, expr, ws)
                : opResOperandDetail(face, expr, ws);
        if (detail == null) {
            return; // contract-impossible (the reason implies the shape) — the defensive belt
        }
        String tripDetailSnapshot = postPinTripDetail;
        PostPinArm scanArm = subtreeTripsPostPinCoercion(claimRoot);
        postPinTripDetail = tripDetailSnapshot;
        String scanTok = scanArm == null ? "scanNone" : "scan:" + scanArm.label();
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "opResGate." + face + "." + detail + "." + scanTok + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                + typing;
        opResGateFacets.merge(token, 1, Integer::sum);
        if (!opResGateFirstSamples.containsKey(token)) {
            opResGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #529 channel-1 census classifier for {@link #itemHeadGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code noResolutionChannel.headMiss
     * .other} reason spelling (every other token — including the decoded {@code headMiss.cp
     * .*} closure-param refinements — a no-op). Read-only against the tree (the adapter
     * token method is static and resolved-model-lookup only) — the standing counters cannot
     * move.
     */
    private void classifyItemHeadGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof REnumValueRef enr)
                || !"noResolutionChannel.headMiss.other".equals(reason)) {
            return;
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "itemHeadGate." + ExpressionToIRAdapter.itemHeadCensusToken(enr) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                + typing;
        itemHeadGateFacets.merge(token, 1, Integer::sum);
        if (!itemHeadGateFirstSamples.containsKey(token)) {
            itemHeadGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #529 channel-2 census classifier for {@link #headAttrGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code featureUnresolved.headAttr
     * .declMiss} reason spelling (every other token a no-op). Read-only against the tree —
     * the standing counters cannot move.
     */
    private void classifyHeadAttrGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RFeatureCall fc)
                || !"featureUnresolved.headAttr.declMiss".equals(reason)) {
            return;
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "headAttrGate." + ExpressionToIRAdapter.headAttrCensusToken(fc) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                + typing;
        headAttrGateFacets.merge(token, 1, Integer::sum);
        if (!headAttrGateFirstSamples.containsKey(token)) {
            headAttrGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #529 channel-3 census classifier for {@link #eqImplGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code operand:IRImplicitAttrNav}
     * reason spelling at the {@link REqualityExpr} family (every other token a no-op). The
     * side detection re-adapts both operands BY CALL (the #528 operand-detail pattern — the
     * re-adapted equivalents are fresh throwaway constructions). Read-only against the tree
     * — the standing counters cannot move.
     */
    private void classifyEqImplGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof REqualityExpr eq) || !"operand:IRImplicitAttrNav".equals(reason)) {
            return;
        }
        IRExpr left = ws == null || eq.rawLeft() == null ? null
                : adapter.adapt(eq.rawLeft(), ws).orElse(null);
        IRExpr right = ws == null || eq.rawRight() == null ? null
                : adapter.adapt(eq.rawRight(), ws).orElse(null);
        boolean leftImpl = left instanceof com.regnosys.rosetta.ir.expr.IRImplicitAttrNav;
        boolean rightImpl = right instanceof com.regnosys.rosetta.ir.expr.IRImplicitAttrNav;
        if (!leftImpl && !rightImpl) {
            return; // contract-impossible under the reason self-gate — the defensive belt
        }
        IRExpr named = leftImpl ? left : right;
        IRExpr sibling = leftImpl ? right : left;
        String sideTok = "s:" + (leftImpl ? "L" : "R");
        String sibTok = "sib:" + (sibling == null ? "un" : sibling.getClass().getSimpleName());
        String cardTok = "c:" + (named.cardinality() == ExpressionCardinality.MULTI
                ? "cardN" : "card1");
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "eqImplGate." + sideTok + "." + sibTok + "." + cardTok + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                + typing;
        eqImplGateFacets.merge(token, 1, Integer::sum);
        if (!eqImplGateFirstSamples.containsKey(token)) {
            eqImplGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #529 channel-4 census classifier for {@link #itemArgSrcGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code argItem} reason spelling at
     * the {@link RSymbolReference} family (every other token a no-op). The item argument is
     * located by the #528 apply-detail's own loop (the same face-match and admission gates BY
     * CALL), and the source walk is the adapter's own ({@link
     * ExpressionToIRAdapter#itemArgSrcCensusToken}). Read-only against the tree — the
     * standing counters cannot move.
     */
    private void classifyItemArgSrcGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference call) || !"argItem".equals(reason)) {
            return;
        }
        List<RExpression> args = call.args();
        for (int i = 0; i < args.size(); i++) {
            RExpression rawArg = args.get(i);
            IRExpr lowered = ws == null || rawArg == null ? null
                    : adapter.adapt(rawArg, ws).orElse(null);
            if (!opResFaceMatches("itemOperand", lowered)
                    || ExpressionToIRAdapter.isTypedUserItemArg(lowered, rawArg)
                    || ExpressionToIRAdapter.isFilterExtractBoundItemOperand(lowered, rawArg)
                    // the #529 declaration-channel admission joins the skip-list (the
                    // Seat-1 OBS-2 close): a decl-proven-admitted arg at a lower index
                    // must never token for a residue face at a higher one.
                    || ExpressionToIRAdapter.isDeclProvenMetaFreeItemArg(lowered, rawArg)) {
                continue; // an admitted (or non-item) argument — not this face's seat
            }
            RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
            String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
            String token = "itemArgSrcGate.i:" + i + "."
                    + ExpressionToIRAdapter.itemArgSrcCensusToken(rawArg) + "."
                    + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr) + "."
                    + typing;
            itemArgSrcGateFacets.merge(token, 1, Integer::sum);
            if (!itemArgSrcGateFirstSamples.containsKey(token)) {
                itemArgSrcGateFirstSamples.put(token, enclosingSampleToken(expr));
            }
            return;
        }
    }

    /**
     * The #530 channel-1 census classifier for {@link #pipeCondGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the {@code attrOutsideFunction
     * .sourceElementUnresolved} reason PREFIX (seat-complete over the family — only the
     * deep elided-pipe spellings live at the #529 SOT; any other member discloses itself).
     * Read-only against the tree (the adapter token method re-runs the arm's own static
     * derivation walks) — the standing counters cannot move.
     */
    private void classifyPipeCondGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference ref)
                || reason == null
                || !reason.startsWith("attrOutsideFunction.sourceElementUnresolved")) {
            return;
        }
        String token = "pipeCondGate." + ExpressionToIRAdapter.pipeDeepCensusToken(ref) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr);
        pipeCondGateFacets.merge(token, 1, Integer::sum);
        if (!pipeCondGateFirstSamples.containsKey(token)) {
            pipeCondGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #530 channel-2 census classifier for {@link #nsrCtGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code featureUnresolved
     * .nonSymbolReceiver} spelling (the standing nsrGate's own gate — the two channels read
     * the SAME pool at different grains). Model reads only; read-only.
     */
    private void classifyNsrCtGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RFeatureCall fc)
                || !"featureUnresolved.nonSymbolReceiver".equals(reason)) {
            return;
        }
        RExpression recv = fc.receiver();
        String detail;
        if (recv instanceof RFeatureCall) {
            // The receiver CHAIN's first typing break, root-first: the wall's own type
            // name. A refMiss verdict = the declared type the MODEL cannot resolve
            // (upstream-shaped); a ref verdict = declared-resolvable but
            // inference-missed (the engine-typing fork).
            java.util.List<RFeatureCall> links = new java.util.ArrayList<>();
            RExpression c = recv;
            while (c instanceof RFeatureCall rc) {
                links.add(rc);
                c = rc.receiver();
            }
            String breakTok = null;
            if (c != null) {
                RMetaAnnotatedType rt = ws == null ? null : ws.getInferredType(c);
                if (rt == null || rt.isMissing()) {
                    breakTok = "break:root:" + c.getClass().getSimpleName()
                            + (c instanceof RSymbolReference sr && sr.name() != null
                                    ? ":" + sr.name() : "");
                }
            }
            for (int i = links.size() - 1; i >= 0 && breakTok == null; i--) {
                RFeatureCall link = links.get(i);
                RMetaAnnotatedType lt = ws == null ? null : ws.getInferredType(link);
                if (lt == null || lt.isMissing()) {
                    breakTok = "break:" + (link.featureName() == null ? "?"
                            : link.featureName()) + ":" + nsrCtDeclToken(link);
                }
            }
            detail = breakTok == null ? "break:none" : breakTok;
        } else if (recv instanceof RImplicitVariable iv) {
            // The item-receiver terminal read: the binder source's resolved feature and
            // its DECLARED type name (the mMiss faces' member-vs-declared read).
            RInlineFunction inline = nearestInlineFn(iv);
            RNode bp = inline == null ? null : inline.parent();
            RExpression src = bp instanceof RFilterExpr filter ? filter.argument()
                    : bp instanceof RExtractExpr extract ? extract.argument()
                            : bp instanceof RThenExpr then ? then.argument() : null;
            String term;
            if (src instanceof RFeatureCall sfc && sfc.resolvedFeature().isPresent()) {
                RAttribute tf = sfc.resolvedFeature().get();
                RTypeCall tc = tf.typeCall();
                term = "term:" + (tf.name() == null ? "?" : tf.name()) + ":"
                        + (tc == null || tc.typeName() == null ? "?" : tc.typeName());
            } else {
                term = "term:" + (src == null ? "none" : src.getClass().getSimpleName());
            }
            // #530 probe4: the #512-B2 qualifier gate's own sub-verdicts BY CALL — the
            // deep-walk bottom the shallow term read above cannot see.
            detail = "item." + term + "."
                    + ExpressionToIRAdapter.qualItemCensusToken(fc);
        } else {
            detail = recv == null ? "recvNull" : "recv:" + recv.getClass().getSimpleName();
        }
        String token = "nsrCtGate." + detail + ".feat:"
                + (fc.featureName() == null ? "?" : fc.featureName());
        nsrCtGateFacets.merge(token, 1, Integer::sum);
        if (!nsrCtGateFirstSamples.containsKey(token)) {
            nsrCtGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The declared-type verdict for one {@link #classifyNsrCtGateBlocker} chain link:
     * {@code unres} (the feature itself never linker-bound) / {@code tcNull} /
     * {@code refMiss:<written>} (the declared type name the model cannot resolve) /
     * {@code ref:<written>:<Class>} (declared-resolvable — the inference gap).
     */
    private static String nsrCtDeclToken(RFeatureCall link) {
        RAttribute rf = link.resolvedFeature().orElse(null);
        if (rf == null) {
            return "unres";
        }
        RTypeCall tc = rf.typeCall();
        if (tc == null) {
            return "tcNull";
        }
        String written = tc.typeName() == null || tc.typeName().isEmpty() ? "?" : tc.typeName();
        RNode t = tc.referencedType().orElse(null);
        return t == null ? "refMiss:" + written
                : "ref:" + written + ":" + t.getClass().getSimpleName();
    }

    /**
     * The #530 channel-3 census classifier for {@link #kvpGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the {@code symbolNotAttribute} reason PREFIX
     * (seat-complete over the family — the three survivor spellings are its whole #529
     * population; any other member discloses itself). BY CALL through the adapter's own
     * decline ladder; read-only.
     */
    private void classifyKvpGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference ref)
                || reason == null || !reason.startsWith("symbolNotAttribute")) {
            return;
        }
        String token = "kvpGate." + adapter.kvpCensusToken(ref, ws) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr);
        kvpGateFacets.merge(token, 1, Integer::sum);
        if (!kvpGateFirstSamples.containsKey(token)) {
            kvpGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #530 channel-4 census classifier for {@link #metaArgGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the exact {@code arg:IRMetaParamRef} spelling.
     * The scan mirrors {@code reasonForApply}'s own arg order (the FIRST meta-param arg is
     * the face's own mint seat); the {@code dir} verdict restates legacy
     * {@code tryMetaDerefArg}'s preconditions from the declarations. Read-only.
     */
    private void classifyMetaArgGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof RSymbolReference call) || !"arg:IRMetaParamRef".equals(reason)) {
            return;
        }
        RNode symbol = call.symbol().orElse(null);
        RFunction callee = symbol instanceof RFunction fn ? fn
                : symbol instanceof RRule rule ? RFunction.fromRule(rule) : null;
        String detail = null;
        if (callee != null && ws != null) {
            List<RExpression> args = call.args();
            for (int i = 0; i < args.size() && detail == null; i++) {
                RExpression rawArg = args.get(i);
                IRExpr loweredArg = ws == null ? null : adapter.adapt(rawArg, ws).orElse(null);
                if (!(loweredArg instanceof IRMetaParamRef)) {
                    continue;
                }
                String metaTok = "meta:?";
                String valueType = null;
                if (rawArg instanceof RSymbolReference argRef
                        && argRef.symbol().orElse(null) instanceof RAttribute callerParam) {
                    List<String> quals = ExpressionToIRAdapter.metaQualifierNames(callerParam);
                    metaTok = "meta:" + (quals.isEmpty() ? "none" : String.join("+", quals));
                    RTypeCall vtc = callerParam.typeCall();
                    valueType = vtc == null ? null : vtc.typeName();
                }
                String valTok = "val:" + (valueType == null ? "?" : valueType);
                String paramTok;
                String dirTok;
                RAttribute param = i < callee.inputs().size() ? callee.inputs().get(i) : null;
                if (param == null) {
                    paramTok = "param:gone";
                    dirTok = "dir:na";
                } else {
                    RTypeCall ptc = param.typeCall();
                    String pWritten = ptc == null || ptc.typeName() == null
                            ? "?" : ptc.typeName();
                    boolean pMeta = ExpressionToIRAdapter.isMetaAnnotated(param);
                    boolean pSingle = param.cardinality()
                            .map(cd -> !cd.isUnbounded()
                                    && java.math.BigInteger.ONE.equals(cd.sup()))
                            .orElse(true);
                    paramTok = "param:" + pWritten + (pMeta ? ":meta" : ":plain")
                            + (pSingle ? ":one" : ":many");
                    // The dir verdict BY CALL against the arm's own predicate (the
                    // Copilot #530 R1-C1 alignment — instance identity on the referenced
                    // types, exactly the admission gate; the written names above stay
                    // display-only).
                    dirTok = ExpressionToIRAdapter.isDerefProvenMetaParamArg(loweredArg,
                            rawArg, param) ? "dir:deref" : "dir:other";
                }
                detail = "callee:" + callee.name() + ".arg" + i + "." + metaTok + "."
                        + valTok + "." + paramTok + "." + dirTok;
            }
        }
        String token = "metaArgGate." + (detail == null ? "argGone" : detail) + "."
                + positionToken(expr, claimRoot) + "." + enclosingContextToken(expr);
        metaArgGateFacets.merge(token, 1, Integer::sum);
        if (!metaArgGateFirstSamples.containsKey(token)) {
            metaArgGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #528 census's OPERAND-family detail ({@code logNav}/{@code pfOperand}/
     * {@code itemOperand}): the consuming seat, the failing side and that operand's own
     * decode. {@code null} when the node is not an operand-bearing family or no side carries
     * the face's kind (contract-impossible under the reason self-gate — the defensive belt).
     */
    private String opResOperandDetail(String face, RExpression expr, RWorkspace ws) {
        String fam;
        RExpression rawLeft;
        RExpression rawRight;
        if (expr instanceof RLogicalExpr log) {
            fam = "log";
            rawLeft = log.rawLeft();
            rawRight = log.rawRight();
        } else if (expr instanceof RComparisonExpr cmp) {
            fam = "cmp";
            rawLeft = cmp.rawLeft();
            rawRight = cmp.rawRight();
        } else if (expr instanceof REqualityExpr eq) {
            fam = "eq";
            rawLeft = eq.rawLeft();
            rawRight = eq.rawRight();
        } else if (expr instanceof RArithmeticExpr arith) {
            fam = "arith";
            rawLeft = arith.rawLeft();
            rawRight = arith.rawRight();
        } else if (expr instanceof RExistenceExpr exist) {
            fam = "exist";
            rawLeft = exist.argument();
            rawRight = null;
        } else {
            return null;
        }
        IRExpr left = ws == null || rawLeft == null ? null : adapter.adapt(rawLeft, ws).orElse(null);
        IRExpr right = ws == null || rawRight == null
                ? null : adapter.adapt(rawRight, ws).orElse(null);
        int side = opResFailingSide(face, fam, left, right, rawLeft, rawRight);
        if (side < 0) {
            return null;
        }
        IRExpr named = side == 0 ? left : right;
        IRExpr sibling = side == 0 ? right : left;
        String sideTok = "s:" + (side == 0 ? "L" : "R");
        String cardTok = "c:" + (named.cardinality() == ExpressionCardinality.MULTI
                ? "cardN" : "card1");
        String sibTok = rawRight == null && side == 0 ? "sibNone"
                : sibling == null ? "sibDeclines" : "sib:" + sibling.getClass().getSimpleName();
        String own;
        switch (face) {
            case "logNav" -> {
                IRExpr base = named;
                while (base instanceof FieldAccess fa) {
                    base = fa.receiver();
                }
                own = "bt:" + opResTypeClass(named) + ".base:" + opResKindToken(base);
            }
            case "pfOperand" -> own = "out:" + opResTypeClass(named);
            default -> {
                RExpression rawNamed = side == 0 ? rawLeft : rawRight;
                own = "k:" + (named instanceof IRVariable var
                                && var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM
                                        ? "syn" : "user")
                        + ".t:" + opResTypeClass(named)
                        + ".b:" + ExpressionToIRAdapter.itemBindingCensusToken(rawNamed)
                        + ".argGate:"
                        + (ExpressionToIRAdapter.isTypedUserItemArg(named, rawNamed) ? "yes" : "no");
            }
        }
        return "f:" + fam + "." + sideTok + "." + own + "." + cardTok + "." + sibTok;
    }

    /**
     * The #528 census's failing-side read: at the two LIVE seats the adapter's OWN gate BY CALL
     * in the mirror's left-then-right order ({@code producesComparisonResult} at the logical
     * seat; the sibling-aware comparison gate composed with the binder-BLIND item admission at
     * the comparison seat — {@code adaptComparison}'s post-arm-3 gate exactly; the Copilot #528
     * R1 catch recut the stale bound-gate read), elsewhere the face's lowered KIND left-first
     * (the zero-carrier negative seats — the same operand either way under the reason
     * self-gate, since a side failing for a DIFFERENT cause would have minted a different
     * spelling). Returns 0 (left), 1 (right) or -1 (neither — the defensive belt).
     */
    private static int opResFailingSide(String face, String fam, IRExpr left, IRExpr right,
            RExpression rawLeft, RExpression rawRight) {
        if ("log".equals(fam)) {
            if (left != null && !ExpressionToIRAdapter.producesComparisonResult(left)) {
                return 0;
            }
            if (right != null && !ExpressionToIRAdapter.producesComparisonResult(right)) {
                return 1;
            }
            return -1;
        }
        if ("cmp".equals(fam)) {
            if (left != null && !(ExpressionToIRAdapter.isComparisonOperand(left, right)
                    || ExpressionToIRAdapter.isBinderBlindItemOperand(left, rawLeft))) {
                return 0;
            }
            if (right != null && !(ExpressionToIRAdapter.isComparisonOperand(right, left)
                    || ExpressionToIRAdapter.isBinderBlindItemOperand(right, rawRight))) {
                return 1;
            }
            return -1;
        }
        if (opResFaceMatches(face, left)) {
            return 0;
        }
        return opResFaceMatches(face, right) ? 1 : -1;
    }

    /** Whether a lowered operand carries the #528 census face's own kind ({@link #opResFailingSide}). */
    private static boolean opResFaceMatches(String face, IRExpr lowered) {
        return switch (face) {
            case "logNav" -> lowered instanceof FieldAccess;
            case "pfOperand" -> lowered instanceof IRPointFreeApply;
            case "itemOperand" -> lowered instanceof IRVariable var
                    && (var.variableKind() == IRVariable.VariableKind.USER_ITEM
                            || var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM);
            default -> false;
        };
    }

    /**
     * The #528 census's APPLY-family detail ({@code cMetaP}/{@code argItem}): the callee class
     * plus, for the meta-param face, every meta-annotated input with its qualifier set and the
     * per-argument adapter verdicts; for the item-arg face, the first item-shaped argument
     * failing the seat's binder-unbounded gate with its own decode. {@code null} when the node
     * is not a call (contract-impossible under the reason self-gate — the defensive belt).
     */
    private String opResApplyDetail(String face, RExpression expr, RWorkspace ws) {
        if (!(expr instanceof RSymbolReference call)) {
            return null;
        }
        RNode callee = call.symbol().orElse(null);
        String calleeTok = callee instanceof RFunction ? "fn"
                : callee instanceof RRule ? "rule" : "otherCallee";
        RFunction fnCallee = callee instanceof RFunction f ? f
                : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
        List<RExpression> args = call.args();
        if ("cMetaP".equals(face)) {
            StringBuilder metaParams = new StringBuilder();
            if (fnCallee != null) {
                List<RAttribute> inputs = fnCallee.inputs();
                for (int i = 0; i < inputs.size(); i++) {
                    if (ExpressionToIRAdapter.isMetaAnnotated(inputs.get(i))) {
                        metaParams.append(metaParams.length() == 0 ? "mp:" : "+").append(i)
                                .append("q")
                                .append(String.join("_",
                                        ExpressionToIRAdapter.metaQualifierNames(inputs.get(i))));
                    }
                }
            }
            StringBuilder argVerdicts = new StringBuilder();
            for (RExpression arg : args) {
                IRExpr lowered = ws == null || arg == null ? null
                        : adapter.adapt(arg, ws).orElse(null);
                argVerdicts.append(argVerdicts.length() == 0 ? "a:" : "+")
                        .append(lowered == null ? "un" : lowered.getClass().getSimpleName());
            }
            return "f:apply." + calleeTok + "."
                    + (metaParams.length() == 0 ? "mpNone" : metaParams.toString()) + "."
                    + (argVerdicts.length() == 0 ? "aNone" : argVerdicts.toString());
        }
        for (int i = 0; i < args.size(); i++) {
            RExpression rawArg = args.get(i);
            IRExpr lowered = ws == null || rawArg == null ? null
                    : adapter.adapt(rawArg, ws).orElse(null);
            if (!opResFaceMatches("itemOperand", lowered)
                    || ExpressionToIRAdapter.isTypedUserItemArg(lowered, rawArg)
                    || ExpressionToIRAdapter.isFilterExtractBoundItemOperand(lowered, rawArg)) {
                continue; // an admitted (or non-item) argument — not this face's seat
            }
            RAttribute param = fnCallee != null && i < fnCallee.inputs().size()
                    ? fnCallee.inputs().get(i) : null;
            GeneratorModel gm = getGeneratorModel();
            String paramTok = param == null ? "pNone"
                    : gm == null ? "pUnread" : gm.isMulti(param) ? "pMulti" : "pSingle";
            return "f:apply." + calleeTok + ".i:" + i
                    + ".k:" + (lowered instanceof IRVariable var
                            && var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM
                                    ? "syn" : "user")
                    + ".t:" + opResTypeClass(lowered)
                    + ".c:" + (lowered.cardinality() == ExpressionCardinality.MULTI
                            ? "cardN" : "card1")
                    + ".b:" + ExpressionToIRAdapter.itemBindingCensusToken(rawArg)
                    + "." + paramTok;
        }
        return null;
    }

    /**
     * The #528 census's type class of a lowered operand/argument: the resolved type NAME, or
     * {@code tMiss}/{@code tMeta}/{@code tNull} for the unresolved, meta-carrying and
     * null-typed states (the three facts every admission gate at these seats reads).
     */
    private static String opResTypeClass(IRExpr lowered) {
        RMetaAnnotatedType t = lowered == null ? null : lowered.type();
        if (t == null || t.isMissing()) {
            return "tMiss";
        }
        if (t.hasMeta()) {
            return "tMeta";
        }
        return t.type() == null ? "tNull" : t.type().name();
    }

    /** The #528 census's lowered-kind token, decorating an {@link IRVariable} with its kind. */
    private static String opResKindToken(IRExpr lowered) {
        if (lowered == null) {
            return "null";
        }
        return lowered.getClass().getSimpleName()
                + (lowered instanceof IRVariable var ? "_" + var.variableKind() : "");
    }

    /**
     * The #526 census's slot-shape token ({@link #classifyNeGateBlocker}): the named slot's
     * position-divergence class, read BY CALL against the adapter's own predicates — see the
     * {@link #neGateFacets} field javadoc for the vocabulary. The inner constructor's seat
     * follows {@code isCtorSlotConditional}'s own condition-then-else order; the constructed
     * type spells {@code t:<TypeName>} ({@code t:noTypeCall} the parse-robustness class).
     */
    private String neSlotShapeToken(RExpression slot, RWorkspace ws) {
        if (slot instanceof RConstructorExpr ctor) {
            return "ctor." + neCtorTypeToken(ctor);
        }
        if (ExpressionToIRAdapter.isCtorSlotConditional(slot)) {
            RConditionalExpr cond = (RConditionalExpr) slot;
            String seat;
            RConstructorExpr inner;
            if (cond.condition() instanceof RConstructorExpr c) {
                seat = "ic:cond";
                inner = c;
            } else if (cond.thenBranch() instanceof RConstructorExpr c) {
                seat = "ic:then";
                inner = c;
            } else {
                // the predicate's third leg — a genuine else that IS the constructor
                seat = "ic:else";
                inner = (RConstructorExpr) cond.elseBranch().get();
            }
            return "condCtorSlot." + seat + "." + neCtorTypeToken(inner);
        }
        if (ExpressionToIRAdapter.isCtorBodyLambda(slot)) {
            String fam = slot instanceof RExtractExpr ? "extract" : "filter";
            RInlineFunction body = slot instanceof RExtractExpr extract ? extract.body()
                    : ((RFilterExpr) slot).body();
            return "lamCtorBody." + fam + "." + neCtorTypeToken((RConstructorExpr) body.body());
        }
        boolean blocked = ws == null || adapter.adapt(slot, ws).isEmpty();
        return (blocked ? "rootDecline:" : "anomaly:") + slot.getClass().getSimpleName();
    }

    /** The #526 census's constructed-type token: {@code t:<TypeName>} / {@code t:noTypeCall}. */
    private static String neCtorTypeToken(RConstructorExpr ctor) {
        return "t:" + (ctor.typeCall() == null ? "noTypeCall" : ctor.typeCall().typeName());
    }

    /**
     * The #526 census's glue-path token ({@link #classifyNeGateBlocker}): the raw parent-chain
     * from the claim root down to the blocker, root-first, {@code >}-joined — each step keyed
     * by the CHILD's slot relationship on its parent per {@link #neGateStepToken} ({@code cc}/
     * {@code ct}/{@code ce} conditional condition/then/else · {@code eb}/{@code fb}
     * extract/filter body, THROUGH the inline-function wrapper · {@code er}/{@code fr}
     * extract/filter receiver · {@code le} list element · {@code pa}/{@code pb} pipe
     * argument/body · {@code x:<Family>} any other parent — the glue-reachability breaker the
     * teach's coverage question keys on). {@code p:-} at an atRoot blocker;
     * {@code pDetached}/{@code pOverflow} the defensive belts (a chain that never reaches the
     * root / more than 16 steps).
     */
    private static String neGatePathToken(RExpression expr, RExpression claimRoot) {
        if (expr == claimRoot) {
            return "p:-";
        }
        List<String> steps = new ArrayList<>();
        RNode child = expr;
        for (int depth = 0; depth < 16; depth++) {
            RNode parent = child.parent();
            if (parent instanceof RInlineFunction inline) {
                // the lambda wrapper is silent — classify the hop at ITS parent (the op node)
                child = inline;
                parent = inline.parent();
            }
            if (parent == null) {
                return "pDetached";
            }
            steps.add(neGateStepToken(parent, child));
            if (parent == claimRoot) {
                StringBuilder joined = new StringBuilder("p:");
                for (int i = steps.size() - 1; i >= 0; i--) {
                    joined.append(steps.get(i));
                    if (i > 0) {
                        joined.append('>');
                    }
                }
                return joined.toString();
            }
            child = parent;
        }
        return "pOverflow";
    }

    /** One {@link #neGatePathToken} step: the CHILD's slot relationship on {@code parent}. */
    private static String neGateStepToken(RNode parent, RNode child) {
        if (parent instanceof RConditionalExpr cond) {
            if (child == cond.condition()) {
                return "cc";
            }
            if (child == cond.thenBranch()) {
                return "ct";
            }
            return cond.elseBranch().orElse(null) == child ? "ce" : "c?";
        }
        if (parent instanceof RListLiteral) {
            return "le";
        }
        if (parent instanceof RExtractExpr extract) {
            return child == extract.argument() ? "er" : "eb";
        }
        if (parent instanceof RFilterExpr filter) {
            return child == filter.argument() ? "fr" : "fb";
        }
        if (parent instanceof RThenExpr then) {
            return child == then.argument() ? "pa" : "pb";
        }
        return "x:" + parent.getClass().getSimpleName();
    }

    /**
     * The #524 census's meta-qualifier token: the attribute's {@code metadata} annotation
     * qualifiers plus-joined in declaration order ({@code scheme}/{@code reference}/…;
     * {@code bare} for a qualifier-less ref) — the Meta flavor's per-class meta story.
     */
    private static String metaQualsToken(RAttribute attr) {
        StringBuilder quals = new StringBuilder();
        for (var annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())) {
                if (quals.length() > 0) {
                    quals.append('+');
                }
                quals.append(annotationRef.qualifierName().orElse("bare"));
            }
        }
        return quals.length() == 0 ? "noQual" : quals.toString();
    }

    /**
     * The #505 arm-A facet classifier for {@link #enumSeatGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the four exact chain-bucket reason spellings (every
     * other token a no-op). Read-only against the tree and adapter-only (stateless by its
     * documented contract) — the standing counters cannot move.
     */
    private void classifyEnumSeatGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!(expr instanceof REnumValueRef enr)) {
            return;
        }
        String face;
        switch (reason) {
            case "attributeChain.closureParamHead" -> {
                // The #503 arm-B2 walks restated at the CHAIN shape: the head segment names
                // the registering closure binder's param; the binder SOURCE derives the
                // element type (the Cat-8 law) and the VALUE segment resolves by name on it.
                String headName = enr.enumName();
                RInlineFunction binder = headName == null ? null
                        : registeringClosureBinder(enr, headName);
                RExpression src = closureBinderSourceOf(enr, headName);
                RDataType elem = src == null ? null
                        : ExpressionToIRAdapter.sourceElementDataType(src);
                String elemV = binder == null ? "noBinder"
                        : elem == null ? "twNull" : "twData:" + elem.name();
                RAttribute leaf = elem == null || enr.valueName() == null ? null
                        : HandlerHelper.findAttributeOnDataType(elem, enr.valueName());
                String leafV = elem == null ? "noLookup"
                        : leaf == null ? "leafMiss"
                                : isMetaAnnotated(leaf) ? "leafMetaHit" : "leafHit";
                face = "chainCp." + elemV + "." + leafV;
            }
            case "choiceOption" -> {
                RType headT = enr.enumName() == null || getGeneratorModel() == null ? null
                        : getGeneratorModel().resolveTypeByName(enr.enumName()).orElse(null);
                String headV = headT == null ? "headNone"
                        : "head:" + headT.getClass().getSimpleName();
                RNode opt = enr.resolvedChoiceOption().orElse(null);
                String optV = opt == null ? "optNull" : opt.getClass().getSimpleName();
                String member;
                if (headT instanceof RChoiceTypeRef choiceRef) {
                    RDataType projected = choiceRef.asRDataType();
                    member = projected == null ? "noProjection"
                            : enr.valueName() != null && HandlerHelper
                                    .findAttributeOnDataType(projected, enr.valueName()) != null
                                            ? "optOnHead" : "optOffHead";
                } else {
                    member = "noProjection";
                }
                face = "chainChoice." + headV + "." + optV + "." + member;
            }
            case "attributeChain.noFilterExtractBinder" -> {
                // The filterExtractSourceOfArmBinder exits restated in order — WHY the
                // binder walk returned null names the widening lever (or its absence).
                String absV;
                if (nearestSwitchBeforeLambda(enr) != null) {
                    absV = "switchCrossed";
                } else {
                    RInlineFunction inline = nearestInlineFn(enr);
                    if (inline == null) {
                        absV = "noInlineFn";
                    } else if (!(inline.isImplicit() || inline.paramNames().isEmpty())) {
                        absV = "paramBinder";
                    } else {
                        absV = "bodyNotFilterExtract";
                    }
                }
                RType headT = enr.enumName() == null || getGeneratorModel() == null ? null
                        : getGeneratorModel().resolveTypeByName(enr.enumName()).orElse(null);
                String headV = headT == null ? "headNone"
                        : "head:" + headT.getClass().getSimpleName();
                face = "chainNoBinder." + absV + "." + headV;
            }
            case "attributeChain.equivalentSynItemSpine" -> {
                RFeatureCall equivalent = getReferenceHandler().synthesizeImplicitItemChain(enr, this);
                String form;
                if (equivalent == null) {
                    form = "eqNull";
                } else {
                    IRExpr lowered = ws == null ? null
                            : adapter.adapt(equivalent, ws).orElse(null);
                    if (lowered == null) {
                        form = "eqDeclines";
                    } else if (lowered instanceof FieldAccess top
                            && top.receiver() instanceof IRSynItemNav) {
                        form = "faSynNav";
                    } else if (lowered instanceof FieldAccess top
                            && top.receiver() instanceof FieldAccess) {
                        form = "faFaSyn";
                    } else {
                        form = "other:" + lowered.getClass().getSimpleName();
                    }
                }
                face = "chainSpine." + form;
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(enr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "enumSeatGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        enumSeatGateFacets.merge(token, 1, Integer::sum);
        if (!enumSeatGateFirstSamples.containsKey(token)) {
            enumSeatGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #505 arm-B facet classifier for {@link #argResidueGateFacets} — see the field
     * javadoc for the grammar. Self-gating on the {@code argNav.alias.} reason prefix (every
     * other token a no-op); the blocker node is the CALL whose argument lowered to the
     * alias-rooted chain (the arg itself lowers since #492 — the call's arg gate declines).
     * Read-only and adapter-only — the standing counters cannot move.
     */
    private void classifyArgResidueGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!reason.startsWith("argNav.alias.") || !(expr instanceof RSymbolReference call)
                || call.args().isEmpty()) {
            return;
        }
        String tail = reason.substring("argNav.alias.".length());
        RNode callee = call.symbol().orElse(null);
        String calleeTok = callee instanceof RFunction ? "fn"
                : callee instanceof RRule ? "rule" : "otherCallee";
        RFunction fnCallee = callee instanceof RFunction f ? f
                : callee instanceof RRule rule ? RFunction.fromRule(rule) : null;
        // The FIRST alias-rooted-nav argument names the seat (the reason mirror scans args
        // in order); its positional param carries the render-lever type facts and its raw
        // chain's root shortcut carries the body decode.
        int aliasIdx = -1;
        RShortcut shortcut = null;
        List<RExpression> args = call.args();
        for (int i = 0; i < args.size() && aliasIdx < 0; i++) {
            RExpression arg = args.get(i);
            IRExpr lowered = ws == null || arg == null ? null
                    : adapter.adapt(arg, ws).orElse(null);
            IRExpr walk = lowered;
            while (walk instanceof FieldAccess fa) {
                walk = fa.receiver();
            }
            if (walk instanceof IRReference r
                    && r.referenceKind() == IRReference.ReferenceKind.ALIAS) {
                aliasIdx = i;
                RExpression rawRoot = arg;
                while (rawRoot instanceof RFeatureCall rfc) {
                    rawRoot = rfc.receiver();
                }
                if (rawRoot instanceof RSymbolReference sr) {
                    shortcut = sr.symbol().orElse(null) instanceof RShortcut bound ? bound
                            : shortcutNamed(HandlerHelper.findEnclosingFunction(sr), sr.name());
                }
            }
        }
        String paramTok;
        if (aliasIdx < 0) {
            paramTok = "argUnlocated";
        } else if (fnCallee == null || aliasIdx >= fnCallee.inputs().size()) {
            paramTok = "noParam";
        } else {
            RAttribute param = fnCallee.inputs().get(aliasIdx);
            paramTok = "param:" + (param.typeCall() == null ? "untyped"
                    : param.typeCall().typeName())
                    + (isMultiCardinalityShape(param) ? ".pN" : ".p1");
        }
        String bodyV;
        if (shortcut == null || shortcut.expression() == null || ws == null) {
            bodyV = "bodyUnreadable";
        } else {
            RMetaAnnotatedType bt = ws.getInferredType(shortcut.expression());
            bodyV = bt == null || bt.isMissing() ? "bodyMiss"
                    : bt.type() == null ? "bodyNullType"
                            : "body:" + bt.type().getClass().getSimpleName();
        }
        String face = "aliasArg." + tail + "." + calleeTok + "." + paramTok + "." + bodyV;
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(call);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "argResidueGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        argResidueGateFacets.merge(token, 1, Integer::sum);
        if (!argResidueGateFirstSamples.containsKey(token)) {
            argResidueGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #505 arm-C facet classifier for {@link #dispatchGateFacets} — see the field javadoc
     * for the grammar. Self-gating on the exact {@code symbolUnresolved.synthetic.absent}
     * spelling (every other token a no-op). Read-only against the tree — the standing
     * counters cannot move.
     */
    private void classifyDispatchGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        if (!"symbolUnresolved.synthetic.absent".equals(reason)
                || !(expr instanceof RSymbolReference sym)) {
            return;
        }
        RFunction fn = HandlerHelper.findEnclosingFunction(sym);
        String variantV = fn == null ? "noFn"
                : fn.dispatch().isPresent() ? "variant" : "plainFn";
        RFunction base = fn == null ? null : HandlerHelper.dispatchBaseOf(fn);
        String baseV;
        if (base == null) {
            baseV = "noBase";
        } else {
            RAttribute input = null;
            for (RAttribute in : base.inputs()) {
                if (sym.name() != null && sym.name().equals(in.name())) {
                    input = in;
                    break;
                }
            }
            baseV = input == null ? "baseNoInput"
                    : "baseInput:" + (input.typeCall() == null ? "untyped"
                            : input.typeCall().typeName())
                            + (isMultiCardinalityShape(input) ? ".pN" : ".p1");
        }
        String annoV;
        if (fn == null) {
            annoV = "noAnno";
        } else {
            boolean calc = false;
            for (com.regnosys.rosetta.ast.annotations.RAnnotationRef ref : fn.annotationRefs()) {
                if ("calculation".equals(ref.annotationName())) {
                    calc = true;
                    break;
                }
            }
            annoV = calc ? "calc" : "plainAnno";
        }
        String face = "synAbsent." + variantV + "." + baseV + "." + annoV;
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(sym);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "dispatchGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        dispatchGateFacets.merge(token, 1, Integer::sum);
        if (!dispatchGateFirstSamples.containsKey(token)) {
            dispatchGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #506 census classifier for {@link #walkBindGateFacets} — see the field javadoc for
     * the grammar. Self-gating on the six exact sole-face reason spellings of the
     * walk-widening cluster (every other token a no-op). Read-only against the tree —
     * derivation walks + resolved-model lookups only; the standing counters cannot move.
     */
    private void classifyWalkBindGateBlocker(RExpression expr, String reason, RWorkspace ws,
            RExpression claimRoot) {
        String face;
        switch (reason) {
            case "attributeChain.noFilterExtractBinder" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                face = "chainBnfe." + binderAbsenceFacet(enr, enr.enumName(), enr.valueName());
            }
            case "attrOutsideFunction.noFilterExtractBinder" -> {
                if (!(expr instanceof RSymbolReference sym)) {
                    return;
                }
                // The bare-attr seat is single-segment: the head IS the bound attribute's own
                // name and there is no leaf segment — the shared facet's head-only ladder.
                face = "bareBnfe." + binderAbsenceFacet(sym, sym.name(), null);
            }
            case "attributeChain.closureParamHead.twNull" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                String headName = enr.enumName();
                RInlineFunction binder = headName == null ? null
                        : registeringClosureBinder(enr, headName);
                RNode bp = binder == null ? null : binder.parent();
                String binderOp = binder == null ? "noBinder"
                        : bp == null ? "orphanBinder" : bp.getClass().getSimpleName();
                RExpression src = closureBinderSourceOf(enr, headName);
                String read;
                if (src == null) {
                    read = "noSource";
                } else {
                    boolean aw = ExpressionToIRAdapter.isProvablyNonMetaElementSource(src);
                    RDataType elem = ExpressionToIRAdapter.sourceElementDataType(src);
                    if (elem != null) {
                        // the walk derives — only the allowlist (or drift) blocked the arm;
                        // the value segment's own resolution is the remaining viability fact
                        RAttribute leaf = enr.valueName() == null ? null
                                : HandlerHelper.findAttributeOnDataType(elem, enr.valueName());
                        String leafV = leaf == null ? "leafMiss"
                                : isMetaAnnotated(leaf) ? "leafMetaHit" : "leafHit";
                        read = (aw ? "aw1tw1" : "aw0tw1") + "." + leafV;
                    } else {
                        read = (aw ? "aw1tw0." : "aw0tw0.") + deepSourceOpShapeToken(src, 0);
                    }
                }
                face = "cpTw." + binderOp + "." + read;
            }
            case "itemNotFilterExtractBound" -> {
                if (!(expr instanceof RFeatureCall fc)
                        || !(fc.receiver() instanceof RImplicitVariable iv)) {
                    return;
                }
                String leafV = fc.resolvedFeature()
                        .map(f -> isMetaAnnotated(f) ? "leafMeta" : "leafPlain")
                        .orElse("leafUnresolved");
                face = "itemBind." + itemBinderFacet(iv) + "." + leafV;
            }
            case "attributeChain.sourceElementUnresolved" -> {
                if (!(expr instanceof REnumValueRef enr)) {
                    return;
                }
                RExpression src = binderSourceOf(enr);
                face = "chainSrcDeep." + (src == null ? "noSource"
                        : deepSourceOpShapeToken(src, 0) + "." + sourceWalkVerdicts(src));
            }
            case "attrOutsideFunction.sourceElementUnresolved" -> {
                if (!(expr instanceof RSymbolReference sym)) {
                    return;
                }
                RExpression src = binderSourceOf(sym);
                face = "bareSrcDeep." + (src == null ? "noSource"
                        : deepSourceOpShapeToken(src, 0) + "." + sourceWalkVerdicts(src));
            }
            default -> {
                return;
            }
        }
        RMetaAnnotatedType t = ws == null ? null : ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String token = "walkBindGate." + face + "." + positionToken(expr, claimRoot) + "."
                + enclosingContextToken(expr) + "." + typing;
        walkBindGateFacets.merge(token, 1, Integer::sum);
        if (!walkBindGateFirstSamples.containsKey(token)) {
            walkBindGateFirstSamples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The #506 binder-absence decode ({@code chainBnfe}/{@code bareBnfe}): WHY
     * {@code filterExtractSourceOfArmBinder} returned null, with the #505 chainNoBinder census's
     * {@code bodyNotFilterExtract} dominant class refined by the binder's PARENT OP — and, for
     * the then-body candidate, the widening's full viability cross-read: the then-ARGUMENT's
     * element derivation ({@code aw×tw}) and the head(+leaf) resolution on the derived element
     * (the chain twin's own downstream ladder restated — head by name on the element, leaf by
     * name on the head's declared type; {@code leafName == null} = the single-segment bare-attr
     * seat, head-only).
     */
    private String binderAbsenceFacet(RExpression node, String headName, String leafName) {
        if (nearestSwitchBeforeLambda(node) != null) {
            return "switchCrossed";
        }
        RInlineFunction inline = nearestInlineFn(node);
        if (inline == null) {
            return "noInlineFn";
        }
        RNode parent = inline.parent();
        String op = parent == null ? "orphanBinder" : parent.getClass().getSimpleName();
        if (!(inline.isImplicit() || inline.paramNames().isEmpty())) {
            return "named." + op;
        }
        if (parent instanceof RThenExpr then && then.body().orElse(null) == inline) {
            RExpression arg = then.argument();
            if (arg == null) {
                return "thenBody.nullArg";
            }
            String verdicts = sourceWalkVerdicts(arg);
            RDataType elem = ExpressionToIRAdapter.sourceElementDataType(arg);
            if (elem == null) {
                return "thenBody." + verdicts + "." + deepSourceOpShapeToken(arg, 0);
            }
            RAttribute head = headName == null ? null
                    : HandlerHelper.findAttributeOnDataType(elem, headName);
            if (head == null) {
                return "thenBody." + verdicts + ".headMiss";
            }
            String headV = isMetaAnnotated(head) ? "headMetaHit" : "headHit";
            if (leafName == null) {
                return "thenBody." + verdicts + "." + headV;
            }
            RDataType headT = declaredDataTypeOf(head);
            if (headT == null) {
                return "thenBody." + verdicts + "." + headV + ".headTypeNull";
            }
            RAttribute leaf = HandlerHelper.findAttributeOnDataType(headT, leafName);
            String leafV = leaf == null ? "leafMiss"
                    : isMetaAnnotated(leaf) ? "leafMetaHit" : "leafHit";
            return "thenBody." + verdicts + "." + headV + "." + leafV;
        }
        return "other." + op;
    }

    /**
     * The #506 item-binder decode ({@code itemBind}): the USER-item's ACTUAL binding context —
     * the {@code isFilterOrExtractBound} gate said "not filter/extract", so name what the walk
     * finds instead. The {@code thenBody} class carries the meta-sourced analogue on the then
     * argument ({@code msrc<0|1>} — the meta-sourced-item law restated at the then seat: a
     * meta-annotated feature-call argument means the item's Java VALUE is a FieldWithMeta
     * wrapper, the L-029 split).
     */
    private String itemBinderFacet(RImplicitVariable iv) {
        if (nearestSwitchBeforeLambda(iv) != null) {
            return "switchCrossed";
        }
        RInlineFunction inline = nearestInlineFn(iv);
        if (inline == null) {
            return "noLambda";
        }
        RNode parent = inline.parent();
        String op = parent == null ? "orphanBinder" : parent.getClass().getSimpleName();
        if (!(inline.isImplicit() || inline.paramNames().isEmpty())) {
            return "named." + op;
        }
        if (parent instanceof RThenExpr then && then.body().orElse(null) == inline) {
            RExpression arg = then.argument();
            boolean metaSourced = arg instanceof RFeatureCall afc
                    && afc.resolvedFeature().isPresent()
                    && isMetaAnnotated(afc.resolvedFeature().get());
            return "thenBody.msrc" + (metaSourced ? "1" : "0");
        }
        return "implicit." + op;
    }

    /**
     * The #506 OP-NAMING deep source-shape walk: {@link #deepSourceShapeToken}'s structure with
     * the census-decisive stops refined — the flat {@code listOp} stop names its OP (136 rows at
     * the #505 SOT without it), the {@code conditional} stop names the #487 join's failing arm
     * ({@code thenNull}/{@code elseNull}/{@code mixed}), the closure-param SYMBOL stop
     * ({@code symbol:RInlineFunction} — a named param as the source) carries the
     * {@code cpSym.<aw×tw>} derivability cross-read on the param's OWN binder source, and a
     * NAMED extract body descends ({@code extractPipe.named>…} — the element form is the body's
     * result regardless of the binding name; the standing walk's conservatism is the widening
     * question this token sizes). Every other node delegates to the standing flat namers. The
     * standing {@link #deepSourceShapeToken} stays BYTE-UNCHANGED (its rows are
     * movement-inertness anchors) — the duplication is the standing OBS-4 probe-walk class.
     * The Copilot #506 R1 recut: the bound and the list-op pass-through set READ the live
     * adapter's own ({@code SOURCE_WALK_DEPTH_LIMIT} public + the five preserving ops), so
     * this walker names each residue's TRUE bottom and cannot drift when the live walks move
     * again (the standing #502 walk above deliberately keeps its frozen 8/three-op shape —
     * its rows are prior-census anchors).
     */
    private String deepSourceOpShapeToken(RExpression source, int depth) {
        if (source == null) {
            return "nullSource";
        }
        if (depth >= ExpressionToIRAdapter.SOURCE_WALK_DEPTH_LIMIT) {
            return "deepNest";
        }
        if (source instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return "thenPipe>"
                    + (body == null ? "bodyAbsent" : deepSourceOpShapeToken(body, depth + 1));
        }
        if (source instanceof RExtractExpr extract) {
            RInlineFunction bodyFn = extract.body();
            RExpression body = bodyFn == null ? null : bodyFn.body();
            if (bodyFn != null && !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return "extractPipe.named>"
                        + (body == null ? "bodyAbsent" : deepSourceOpShapeToken(body, depth + 1));
            }
            return "extractPipe>"
                    + (body == null ? "bodyAbsent" : deepSourceOpShapeToken(body, depth + 1));
        }
        if (source instanceof RFilterExpr filter) {
            return "filter>" + deepSourceOpShapeToken(filter.argument(), depth + 1);
        }
        if (source instanceof RImplicitVariable) {
            RExpression resolved = resolveElidedPipedSourceShape(source);
            if (resolved != null && resolved != source
                    && !(resolved instanceof RImplicitVariable)) {
                return "elided>" + deepSourceOpShapeToken(resolved, depth + 1);
            }
            return unprovableSourceShapeToken(source, depth);
        }
        if (source instanceof com.regnosys.rosetta.ast.expressions.unary.RListOpExpr listOp) {
            // the recursion set ≡ the live walks' element-preserving ops (DISTINCT/LAST joined
            // at #506 — the R1 drift-class recut), so a flat op-named stop is a TRUE bottom
            com.regnosys.rosetta.ast.enums.ListOp op = listOp.op();
            if (op == com.regnosys.rosetta.ast.enums.ListOp.ONLY_ELEMENT
                    || op == com.regnosys.rosetta.ast.enums.ListOp.FIRST
                    || op == com.regnosys.rosetta.ast.enums.ListOp.FLATTEN
                    || op == com.regnosys.rosetta.ast.enums.ListOp.DISTINCT
                    || op == com.regnosys.rosetta.ast.enums.ListOp.LAST) {
                return "listOp." + op.name() + ">"
                        + deepSourceOpShapeToken(listOp.argument(), depth + 1);
            }
            return "listOp." + op.name(); // the op-NAMED stop — a genuinely non-preserving op
        }
        if (source instanceof RConditionalExpr cond) {
            RDataType thenT = ExpressionToIRAdapter.sourceElementDataType(cond.thenBranch());
            if (thenT == null) {
                return "cond.thenNull>"
                        + deepSourceOpShapeToken(cond.thenBranch(), depth + 1);
            }
            RExpression elseB = cond.elseBranch().orElse(null);
            RDataType elseT = ExpressionToIRAdapter.sourceElementDataType(elseB);
            if (elseT == null) {
                return "cond.elseNull>" + deepSourceOpShapeToken(elseB, depth + 1);
            }
            return thenT == elseT ? "cond.drift" : "cond.mixed";
        }
        if (source instanceof RSymbolReference sym && sym.args().isEmpty()
                && RInlineFunction.isClosureParameterBinding(sym.symbol().orElse(null))) {
            // a NAMED closure param as the source — would the param's own binder source derive?
            RExpression cpSrc = closureBinderSourceOf(sym, sym.name());
            return "cpSym." + (cpSrc == null ? "noSource" : sourceWalkVerdicts(cpSrc));
        }
        if (source instanceof REnumValueRef evr && evr.enumeration().isEmpty()
                && evr.enumName() != null) {
            // #506 probe4 refinement: an ALIAS-HEAD disguised source's TRUE bottom is inside
            // the alias BODY (the taught adapter leg derives there — a persisting aw1tw0 row
            // means the body's own walk fails), so descend it; a non-alias head keeps the
            // flat namer (the fn-scope/rule ladders' own faces).
            RFunction enc = HandlerHelper.findEnclosingFunction(evr);
            RShortcut aliasHead = enc == null ? null : shortcutNamed(enc, evr.enumName());
            if (aliasHead != null) {
                return "enrAliasHead>" + deepSourceOpShapeToken(aliasHead.expression(), depth + 1);
            }
        }
        return unprovableSourceShapeToken(source, depth);
    }

    /** The #502 aliasHead lookup: the enclosing function's shortcut carrying the name. */
    private static RShortcut shortcutNamed(RFunction enclosing, String name) {
        if (enclosing == null || name == null) {
            return null;
        }
        for (RShortcut shortcut : enclosing.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return shortcut;
            }
        }
        return null;
    }

    /**
     * #505: the registering closure binder's SOURCE expression for a param name — the #503
     * derivation shared by the enumSeatGate census and the chainArmVerdict restatement (the
     * binder's parent filter/extract/then argument; {@code null} when the registration walk
     * misses or the parent shape is outside the derivable set).
     */
    private static RExpression closureBinderSourceOf(RNode start, String name) {
        RInlineFunction binder = name == null ? null : registeringClosureBinder(start, name);
        RNode bp = binder == null ? null : binder.parent();
        return bp instanceof RFilterExpr filter && filter.body() == binder
                ? filter.argument()
                : bp instanceof RExtractExpr extract && extract.body() == binder
                        ? extract.argument()
                        : bp instanceof RThenExpr then && then.body().orElse(null) == binder
                                ? then.argument() : null;
    }

    /**
     * The #502 closureParam lookup: the nearest enclosing inline function whose DECLARED param
     * names carry the head name (pass-5's closure-param registration surface, restated
     * read-only). Bounded like every witness walk.
     */
    private static RInlineFunction registeringClosureBinder(RNode start, String name) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline && inline.paramNames().contains(name)) {
                return inline;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * The #502 two-walk cross-verdict ({@code aw<0|1>tw<0|1>}): the #479 element-form allowlist
     * ({@code isProvablyNonMetaElementSource}) × the #480 structural typing walk
     * ({@code sourceElementDataType}) on the same source — the walk-unification map's cell.
     * Both adapter walks are public for exactly this probe-only cross-read (the #501
     * probe-visibility precedent).
     */
    private static String sourceWalkVerdicts(RExpression source) {
        if (source == null) {
            return "noSource";
        }
        return (ExpressionToIRAdapter.isProvablyNonMetaElementSource(source) ? "aw1" : "aw0")
                + (ExpressionToIRAdapter.sourceElementDataType(source) != null ? "tw1" : "tw0");
    }

    /** The #502 three-walk verdict: {@link #sourceWalkVerdicts} + the cached engine type. */
    private static String threeWalkVerdicts(RExpression source, RWorkspace ws) {
        RMetaAnnotatedType t = ws == null || source == null ? null : ws.getInferredType(source);
        String ct = t == null || t.isMissing() ? "ctMiss" : t.hasMeta() ? "ctMeta" : "ctOk";
        return sourceWalkVerdicts(source) + ct;
    }

    /**
     * The #502 DEEP source-shape token: {@link #unprovableSourceShapeToken}'s namer with the
     * flat {@code thenPipe}/{@code extractPipe} stops RECURSED through their bodies (and a
     * filter through its argument, an elided implicit through the pipe resolver) so the token
     * names the TRUE proof bottom — the #501 srcElem census read the flat stops as its
     * dominant rows and the bareAttr thenArg channel proved the bottoms are list-op collapses.
     * {@code >}-joined links; bounded like every witness walk.
     */
    private static String deepSourceShapeToken(RExpression source, int depth) {
        if (source == null) {
            return "nullSource";
        }
        if (depth >= 8) {
            return "deepNest";
        }
        if (source instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return "thenPipe>"
                    + (body == null ? "bodyAbsent" : deepSourceShapeToken(body, depth + 1));
        }
        if (source instanceof RExtractExpr extract) {
            RInlineFunction bodyFn = extract.body();
            if (bodyFn != null && !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return "extractPipe>namedBody";
            }
            RExpression body = bodyFn == null ? null : bodyFn.body();
            return "extractPipe>"
                    + (body == null ? "bodyAbsent" : deepSourceShapeToken(body, depth + 1));
        }
        if (source instanceof RFilterExpr filter) {
            return "filter>" + deepSourceShapeToken(filter.argument(), depth + 1);
        }
        if (source instanceof RImplicitVariable) {
            RExpression resolved = resolveElidedPipedSourceShape(source);
            if (resolved != null && resolved != source
                    && !(resolved instanceof RImplicitVariable)) {
                return "elided>" + deepSourceShapeToken(resolved, depth + 1);
            }
            return unprovableSourceShapeToken(source, depth); // the elidedImplicit stop facets
        }
        if (source instanceof com.regnosys.rosetta.ast.expressions.unary.RListOpExpr listOp) {
            // #503: the element-form-preserving collapses recurse INTO the argument (the
            // #502 flat `listOp.<OP>` stop was the residue's own naming gap — the taught
            // walks prove THROUGH these ops, so a still-unprovable collapse bottoms INSIDE
            // its argument and the token must name that inner bottom).
            com.regnosys.rosetta.ast.enums.ListOp op = listOp.op();
            if (op == com.regnosys.rosetta.ast.enums.ListOp.ONLY_ELEMENT
                    || op == com.regnosys.rosetta.ast.enums.ListOp.FIRST
                    || op == com.regnosys.rosetta.ast.enums.ListOp.FLATTEN) {
                return "listOp." + op.name() + ">"
                        + deepSourceShapeToken(listOp.argument(), depth + 1);
            }
            return unprovableSourceShapeToken(source, depth); // the non-preserving ops stay flat
        }
        return unprovableSourceShapeToken(source, depth); // the flat leaf namers
    }

    /**
     * The #502 thenBody argument: the nearest inline fn's enclosing {@link RThenExpr}'s argument
     * (the value the then-body implicit takes — the #481 value identity). {@code null} when the
     * walk finds no then-bodied binder (the context token implies one — drift only).
     */
    private static RExpression thenArgumentOf(RImplicitVariable iv) {
        RInlineFunction binder = nearestInlineFn(iv);
        return binder != null && binder.parent() instanceof RThenExpr then
                && then.body().orElse(null) == binder ? then.argument() : null;
    }

    /**
     * The #502 srcElemBare binder-source walk: the {@link #binderSourceShapeToken} walk's
     * source-finding half as a value (that token keeps its own inline walk BYTE-UNCHANGED — the
     * #501 census lines are movement-inertness anchors; the duplication is the standing OBS-4
     * probe-walk class). {@code null} for no binder, a switch crossing, or a non-filter/extract
     * binder alike — the caller's {@code noSource} face.
     */
    private static RExpression binderSourceOf(RExpression expr) {
        RNode cur = expr.parent();
        int depth = 0;
        RInlineFunction binder = null;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RSwitchExpr) {
                return null;
            }
            if (cur instanceof RInlineFunction inline) {
                binder = inline;
                break;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                break;
            }
            cur = cur.parent();
        }
        if (binder == null) {
            return null;
        }
        RNode parent = binder.parent();
        return parent instanceof RFilterExpr filter && filter.body() == binder
                ? filter.argument()
                : parent instanceof RExtractExpr extract && extract.body() == binder
                        ? extract.argument() : null;
    }

    /**
     * Compiler-local mirror of legacy {@code ReferenceHandler.hasEnclosingRuleLambda} (the
     * {@code buildImplicitInputReceiver} receiver split, restated for the #501 ruleChain census):
     * an inline function between the node and the enclosing {@link RRule}/{@link RFunction} means
     * the synthesized input-chain receiver is the lambda's synthetic IMPLICIT, not the
     * {@code MapperS.of(input)} root. Bounded like the source walk.
     */
    private static boolean ruleLambdaInterposed(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < 64) {
            if (cur instanceof RInlineFunction) {
                return true;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * The #501 srcElem decode: the filter/extract binder SOURCE's shape token (the compiler-side
     * mirror of the adapter's {@code filterExtractSourceOfArmBinder} context — the nearest inline
     * function walked with SWITCH precedence, its parent filter/extract's argument). Names which
     * {@code sourceElementDataType} leg a widening must teach: the source class plus one decisive
     * sub-level ({@code declaredTypeToken} of a feature-call source's resolved last hop / a
     * symbol source's symbol class + declared type / a pipe-or-extract source's body class). The
     * {@code noBinder}/{@code noFilterExtract} tokens are drift detectors — the reason spelling
     * implies the adapter FOUND the binder and source.
     */
    private static String binderSourceShapeToken(RExpression expr) {
        RNode cur = expr.parent();
        int depth = 0;
        RInlineFunction binder = null;
        while (cur != null && depth++ < 64) {
            if (cur instanceof RSwitchExpr) {
                return "noBinder"; // the adapter's switch-precedence walk stops here
            }
            if (cur instanceof RInlineFunction inline) {
                binder = inline;
                break;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                break;
            }
            cur = cur.parent();
        }
        if (binder == null) {
            return "noBinder";
        }
        RNode parent = binder.parent();
        RExpression source = parent instanceof RFilterExpr filter && filter.body() == binder
                ? filter.argument()
                : parent instanceof RExtractExpr extract && extract.body() == binder
                        ? extract.argument() : null;
        if (source == null) {
            return "noFilterExtract";
        }
        String cls = source.getClass().getSimpleName();
        if (source instanceof RFeatureCall fc) {
            RAttribute feat = fc.resolvedFeature().orElse(null);
            return cls + "." + (feat == null ? "featureUnresolved" : declaredTypeToken(feat));
        }
        if (source instanceof RSymbolReference sr) {
            RNode symbol = sr.symbol().orElse(null);
            return cls + "." + (symbol == null ? "symUnresolved"
                    : symbol.getClass().getSimpleName()
                            + (symbol instanceof RAttribute a ? "." + declaredTypeToken(a) : ""));
        }
        if (source instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return cls + ".body:" + (body == null ? "absent" : body.getClass().getSimpleName());
        }
        if (source instanceof RExtractExpr extract) {
            RExpression body = extract.body() == null ? null : extract.body().body();
            return cls + ".body:" + (body == null ? "absent" : body.getClass().getSimpleName());
        }
        return cls;
    }

    /**
     * The declared-type token for {@link #binderSourceShapeToken}: why the adapter's
     * {@code declaredDataTypeOf} channel nulls — {@code noTypeCall} (an untyped attribute) /
     * {@code typeUnresolved} (the call resolves nothing) / the referenced node's class simple
     * name ({@code RDataType} = the walk SHOULD have served it, a drift face; an enum/basic
     * carrier names the honest non-data leg).
     */
    private static String declaredTypeToken(RAttribute attr) {
        if (attr.typeCall() == null) {
            return "noTypeCall";
        }
        RNode referenced = attr.typeCall().referencedType().orElse(null);
        return referenced == null ? "typeUnresolved" : referenced.getClass().getSimpleName();
    }

    /** Defensive bound on the {@link #metaChainWouldLower} receiver walk (chains are shallow; cycles impossible). */
    private static final int META_CHAIN_WALK_LIMIT = 32;

    /**
     * Whether an expression would lower under the PLANNED recursive meta-navigation admission —
     * it lowers today, OR it is a meta-annotated feature call whose receiver (recursively) would:
     * the #499 census's cascade projection (a meta hop over a meta hop lowers bottom-up once the
     * adapter's meta gate becomes an admission). Read-only: adapts at the probe, never compiles.
     */
    private boolean metaChainWouldLower(RExpression expr, RWorkspace ws, int depth) {
        if (expr == null || ws == null || depth > META_CHAIN_WALK_LIMIT) {
            return false;
        }
        if (adapter.adapt(expr, ws).isPresent()) {
            return true;
        }
        return expr instanceof RFeatureCall fc
                && fc.resolvedFeature().filter(IRExpressionCompiler::isMetaAnnotated).isPresent()
                && metaChainWouldLower(fc.receiver(), ws, depth + 1);
    }

    /**
     * The #498 post-pin-guard exemption's kind gate: an args-present rule invocation root
     * ({@link IRApply} with callee kind {@code RULE} and non-empty args — see the exemption
     * comment in {@code tryEmitFromIR}). Deliberately narrow: a FUNCTION apply and the BARE rule
     * delegation both keep the live scan.
     */
    private static boolean isArgsPresentRuleApply(IRExpr ir) {
        return ir instanceof IRApply apply && !apply.args().isEmpty()
                && apply.callee() instanceof IRReference callee
                && callee.referenceKind() == IRReference.ReferenceKind.RULE;
    }

    /**
     * The position leg for the #498 gate censuses: {@code atRoot} when the blocker node IS the
     * probed claim root (the teach lowers it at its own visit seat), else
     * {@code interior.<RootFamily>} — the claim root's class simple name, because the post-teach
     * outcome splits BY THE ROOT'S RENDER MODE: a whole-subtree oracle-rendered root (a list op /
     * count / to-string / constructor / conditional / lambda claim) covers its interior through
     * the oracle's own recursion and LOWERS, while a natively-composed root (navigation / binary /
     * existence / apply…) re-declines at the interior node's null-renderer frame-slot gate (the
     * leafEmitter move — the whole-oracle renderers install only for their own claim-root kinds).
     */
    private static String positionToken(RExpression expr, RExpression claimRoot) {
        return expr == claimRoot ? "atRoot"
                : "interior." + (claimRoot == null ? "nullRoot" : claimRoot.getClass().getSimpleName());
    }

    /**
     * The enclosing-container token for the #498 gate censuses: {@code inFunction}/{@code inRule}/
     * {@code orphan} — the container split that names which render machinery serves the site.
     */
    private static String enclosingContextToken(RExpression expr) {
        if (HandlerHelper.findEnclosingFunction(expr) != null) {
            return "inFunction";
        }
        return HandlerHelper.findEnclosingRule(expr) != null ? "inRule" : "orphan";
    }

    /**
     * The first-sample site token for the #498 gate censuses ({@code in=func:<n>} /
     * {@code in=rule:<n>} / {@code in=<unresolved>} — the {@link #implicitAttrFirstSamples}
     * spelling).
     */
    private static String enclosingSampleToken(RExpression expr) {
        RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        RRule rule = fn == null ? HandlerHelper.findEnclosingRule(expr) : null;
        return "in=" + (fn != null ? "func:" + fn.name()
                : rule != null ? "rule:" + rule.name() : "<unresolved>");
    }

    @Override
    public JavaStatementBuilder visitFilter(RFilterExpr expr, ExpressionContext ctx) {
        // The #496 lambda claim (the monster wave's leg 1 — the two biggest untargeted families,
        // decoded by the lambdaVisit probe BEFORE the arm landed). ONE seat per family,
        // oracle-closed at both exits: the adapter lowers the ROOT lambda to the fully childed
        // IRLambdaOp (adaptLambdaOp's D1 all-or-nothing recursion — receiver + body must BOTH
        // lower; the probe read filter lowers 1,304 of 2,014 — plus the claim-root ctor-body
        // admission on the extract twin), and the leaf emitter's lambda arm renders via the
        // range-correlated LambdaOpRenderer — super.visitFilter(site, ctx), the LITERAL fallback
        // call (routing to collectionHandler.handle), same method, same handler instance, same
        // arguments, same virtual dispatch for the receiver/body compiles — byte-identical BY
        // CONSTRUCTION at every exit: a driven claim renders the oracle's bytes, a declined claim
        // (D1-blocked / typeMissing / uncorrelated) falls back to the same call. Interior nodes
        // re-enter this seat through the oracle's own dispatch and claim at their own roots. The
        // facet bump stays at this seat (probe-only, the #491 law) so the post-teach probe reads
        // driven+declined ≡ Σ facets.
        if (blockerProbeEnabled) {
            // #496: the leg-1 facet channel rides the visit seat (the #489 one-walk law — Σ facets
            // ≡ the family's whole visit population by construction), probe-only.
            lambdaVisitFacets.merge(lambdaVisitFacet("filter", expr, expr.argument(), expr.body()),
                    1, Integer::sum);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitFilter(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitExtract(RExtractExpr expr, ExpressionContext ctx) {
        // The #496 lambda claim at the extract seat — see visitFilter (the probe read extract
        // lowers 1,498 of 3,140 + the 194-event claim-root ctor-body admission).
        if (blockerProbeEnabled) {
            // #496: the same leg-1 facet channel at the extract seat — see visitFilter.
            lambdaVisitFacets.merge(lambdaVisitFacet("extract", expr, expr.argument(), expr.body()),
                    1, Integer::sum);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitExtract(expr, ctx));
    }

    // #513: sort/max/min join the TARGETED set (the noAdaptArm-sweep visit conversions —
    // see visitDefault); reduce keeps the count-then-super meter (unarmed).
    @Override
    public JavaStatementBuilder visitSort(RSortExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitSort(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitReduce(RReduceExpr expr, ExpressionContext ctx) {
        recordUntargetedVisit(expr);
        return super.visitReduce(expr, ctx);
    }

    @Override
    public JavaStatementBuilder visitMax(RMaxExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitMax(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitMin(RMinExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitMin(expr, ctx));
    }

    /**
     * The #500 arm-B pipe claim (the untargeted-family sweep, seat 1 of 4 — the #494/#495
     * conversion pattern): the adapter lowers the ROOT then-chain to the deliberately-shallow
     * {@link com.regnosys.rosetta.ir.expr.IRPipe} (census-narrow on the implicit-bare binder —
     * 100% of the 582-event population), and the emitter's entry consult serves it via the
     * generic {@link OracleRootRenderer} — {@code super.visitThen(site, ctx)}, the LITERAL
     * legacy fallback (routing to {@code collectionHandler.handle}), byte-identical BY
     * CONSTRUCTION at every exit: a driven claim renders the oracle's bytes, a declined claim
     * (binder-form / uncorrelated) falls back to the same call. The family moves from the
     * untargeted meter to the targeted set (its untargeted count going to zero is the teach's
     * own conservation signature — the #491 law); the census bump stays probe-only, so the
     * post-teach probe reads driven+declined ≡ Σ facets.
     */
    @Override
    public JavaStatementBuilder visitThen(RThenExpr expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #500: the arm-B thenVisit census rides the visit seat (the #489 one-walk law — Σ
            // facets ≡ the family's whole visit population by construction), probe-only.
            bumpVisitCensus(thenVisitFacets, thenVisitFirstSamples, thenVisitFacet(expr), expr);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitThen(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitConditional(RConditionalExpr expr, ExpressionContext ctx) {
        // The #495 conditional claim (the A menu pick — the census-marked adapter-ready family,
        // 1,133 at the #494 census, decoded by the condVisit probe BEFORE the arm landed). ONE
        // seat, oracle-closed at both exits: the adapter lowers the ROOT conditional to the fully
        // childed IRConditional (adaptConditional's D1 all-or-nothing recursion — 470 of 1,133
        // lower outright, +320 via the #495 claim-root ctor-slot admission), and the leaf
        // emitter's conditional arm renders via the range-correlated ConditionalRenderer —
        // super.visitConditional(site, ctx), the LITERAL fallback call (routing to
        // controlFlowHandler.handle(expr, ctx, this)), same method, same handler instance, same
        // arguments, same virtual dispatch for the condition/branch compiles — byte-identical BY
        // CONSTRUCTION at every exit: a driven claim renders the oracle's bytes, a declined claim
        // (interior-blocked / typeMissing / uncorrelated) falls back to the same call. Interior
        // nodes re-enter this seat through the oracle's own dispatch and claim at their own
        // roots (a #495-admitted ctor slot claims at its own visitConstructor visit). The facet
        // bump stays at this seat (probe-only, the #491 law) so the post-teach probe reads
        // driven+declined ≡ Σ facets.
        if (blockerProbeEnabled) {
            // #495: the facet channel rides the visit seat (the #489 one-walk law — Σ facets ≡ the
            // family's whole visit population by construction), probe-only, teach-independent.
            condVisitFacets.merge(condVisitFacet(expr), 1, Integer::sum);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitConditional(expr, ctx));
    }

    // #513: switch joins the TARGETED set (the noAdaptArm-sweep visit conversions — see
    // visitDefault).
    @Override
    public JavaStatementBuilder visitSwitch(RSwitchExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitSwitch(expr, ctx));
    }

    /**
     * The #500 arm-B only-exists claim (seat 2 of 4) — the shallow
     * {@link com.regnosys.rosetta.ir.expr.IROnlyExists} served by the generic oracle-root
     * renderer's {@code super.visitOnlyExists(site, ctx)} (routing to
     * {@code existenceHandler.handle}) — see {@link #visitThen}.
     */
    @Override
    public JavaStatementBuilder visitOnlyExists(ROnlyExistsExpr expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #500: the arm-B onlyExistsVisit census — see visitThen.
            bumpVisitCensus(onlyExistsVisitFacets, onlyExistsVisitFirstSamples,
                    onlyExistsVisitFacet(expr), expr);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitOnlyExists(expr, ctx));
    }

    @Override
    public JavaStatementBuilder visitCardinalityCheck(RCardinalityCheckExpr expr, ExpressionContext ctx) {
        recordUntargetedVisit(expr);
        return super.visitCardinalityCheck(expr, ctx);
    }

    /**
     * The #494 constructor claim (the D menu pick — the biggest untargeted family, 3,501 at the
     * #493 census, decoded by the ctorVisit probe BEFORE the arm landed). ONE seat, oracle-closed
     * at both exits: the adapter lowers the ROOT constructor to the deliberately-shallow
     * {@link com.regnosys.rosetta.ir.expr.IRConstruct} (typed 3,499 of 3,501 — the emitter's
     * typeMissing gate declines the 2), and the leaf emitter's construct arm renders via the
     * range-correlated {@link ConstructRenderer} — {@code super.visitConstructor(site, ctx)}, the
     * LITERAL fallback call (legacy {@code ExpressionCompiler.visitConstructor} is one line:
     * {@code constructionHandler.handle(expr, ctx, this)}), same method, same handler instance,
     * same arguments, same virtual dispatch for the per-pair value compiles — byte-identical BY
     * CONSTRUCTION at every exit: a driven claim renders the oracle's bytes, a declined claim
     * (typeMissing / uncorrelated / a post-pin guard trip) falls back to the same call. Nested
     * constructors re-enter this seat through the oracle's own value compiles and claim at their
     * own roots (the adapter's root-only law — no child-position allow-list moved). The facet bump
     * stays at this seat (probe-only, the #491 law) so the post-teach probe reads
     * driven+declined ≡ Σ facets.
     */
    @Override
    public JavaStatementBuilder visitConstructor(RConstructorExpr expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #494: the facet channel rides the visit seat (the #489 one-walk law — Σ facets ≡ the
            // family's whole visit population by construction), probe-only, teach-independent.
            ctorVisitFacets.merge(ctorVisitFacet(expr), 1, Integer::sum);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitConstructor(expr, ctx));
    }

    // #515: with-meta joins the TARGETED set (the untargeted-close visit conversions —
    // the #513 noAdaptArm-sweep pattern, see visitDefault; the adapter's unconditional
    // IRWithMetaOp arm serves through the frame's withMetaOp oracle leg BY IDENTITY —
    // legacy's FieldWithMetaX builder threading renders inside the oracle's own re-walk).
    @Override
    public JavaStatementBuilder visitWithMeta(RWithMetaExpr expr, ExpressionContext ctx) {
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitWithMeta(expr, ctx));
    }

    /**
     * The #500 arm-B conversion claim (seat 3 of 4) — the DEEP
     * {@link com.regnosys.rosetta.ir.expr.IRConversion} (all-or-nothing argument; the census
     * read args lowering in 948 of 956) served by the generic oracle-root renderer's
     * {@code super.visitConversion(site, ctx)} (routing to {@code conversionHandler.handle}) —
     * see {@link #visitThen}.
     */
    @Override
    public JavaStatementBuilder visitConversion(RConversionExpr expr, ExpressionContext ctx) {
        if (blockerProbeEnabled) {
            // #500: the arm-B conversionVisit census — see visitThen.
            bumpVisitCensus(conversionVisitFacets, conversionVisitFirstSamples,
                    conversionVisitFacet(expr), expr);
        }
        return tryEmitFromIR(expr, ctx).orElseGet(() -> super.visitConversion(expr, ctx));
    }

    /**
     * The single bump site for the untargeted census ({@link #untargetedVisitsByFamily}): keyed by the
     * node's class simple name — the same key convention as {@link #irDeclinedByFamily}, so the two
     * breakdowns read as one worklist. Deliberately no witness print here: an untargeted visit has no
     * decline site or claim shape to report (there was no attempt), and the per-family totals are the
     * census datum; the {@link #DECLINE_WITNESS} channel samples the ATTEMPTED-and-declined population.
     */
    private void recordUntargetedVisit(RExpression expr) {
        untargetedVisitsByFamily.merge(expr.getClass().getSimpleName(), 1, Integer::sum);
    }

    /**
     * The #500 arm-B channel bump: facet count + first-sample witness in one call (the callers
     * gate on {@link #blockerProbeEnabled} — probe-only by construction).
     */
    private void bumpVisitCensus(Map<String, Integer> facets, Map<String, String> samples,
            String token, RExpression expr) {
        facets.merge(token, 1, Integer::sum);
        if (!samples.containsKey(token)) {
            samples.put(token, enclosingSampleToken(expr));
        }
    }

    /**
     * The adapter's first-failing-gate HEAD (the first dot-segment of
     * {@code declineReason}) for the #500 arm-B verdicts — compact enough for ranked lines
     * while still naming the real gate ({@code closureParam}, {@code attributeChain}, …).
     */
    private String reasonHead(RExpression expr, RWorkspace ws) {
        String reason = adapter.declineReason(expr, ws)
                .orElse(ExpressionToIRAdapter.UNATTRIBUTED);
        int dot = reason.indexOf('.');
        return dot < 0 ? reason : reason.substring(0, dot);
    }

    /**
     * The #500 arm-B thenVisit facet — grammar at {@link #thenVisitFacets}. The verdicts are
     * adapter-probe reads (stateless by its documented contract): the piped source at
     * {@code argument()} and the body EXPRESSION at {@code body().body()} probe separately — a
     * {@code body:closureParam} head names the binder-scope slice a THEN arm must serve.
     */
    private String thenVisitFacet(RThenExpr expr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "thenVisit.noWorkspace";
        }
        int n = 1;
        RExpression cur = expr.argument();
        while (cur instanceof RThenExpr t) {
            n++;
            cur = t.argument();
        }
        String spine = n == 1 ? "n1" : n == 2 ? "n2" : "n3plus";
        RInlineFunction body = expr.body().orElse(null);
        String binder = body == null ? "noBody"
                : body.isImplicit() ? "implicitBare"
                        : !body.paramNames().isEmpty() ? "named" : "implicitBracket";
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String argVerdict = expr.argument() == null ? "arg:nullSlot"
                : adapter.adapt(expr.argument(), ws).isPresent() ? "argLowers"
                        : "arg:" + reasonHead(expr.argument(), ws);
        String bodyVerdict = body == null || body.body() == null ? "body:nullSlot"
                : adapter.adapt(body.body(), ws).isPresent() ? "bodyLowers"
                        : "body:" + reasonHead(body.body(), ws);
        return "thenVisit." + spine + "." + binder + "." + typing + "." + argVerdict + "."
                + bodyVerdict;
    }

    /**
     * The #500 arm-B conversionVisit facet — grammar at {@link #thenVisitFacets}. The
     * {@code to-string} form is the separate {@code RToStringExpr} family (already targeted at
     * its own seat) and never reaches this seat.
     */
    private String conversionVisitFacet(RConversionExpr expr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "conversionVisit.noWorkspace";
        }
        RExpression arg = expr.argument();
        String argFam = arg == null ? "nullSlot" : arg.getClass().getSimpleName();
        String argVerdict = arg == null ? "arg:nullSlot"
                : adapter.adapt(arg, ws).isPresent() ? "argLowers" : "arg:" + reasonHead(arg, ws);
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        return "conversionVisit." + conversionKindToken(expr) + "." + argFam + "." + argVerdict
                + "." + typing;
    }

    /**
     * The {@link com.regnosys.rosetta.ast.enums.ConversionKind} receipt token for
     * {@link #conversionVisitFacet}: the constant lower-camel with the {@code to} prefix
     * ({@code ZONED_DATE_TIME} → {@code toZonedDateTime}); the {@code ENUM} kind splits on
     * target-enum presence ({@code toEnumUnresolved} the unresolved face).
     */
    private static String conversionKindToken(RConversionExpr expr) {
        if (expr.kind() == null) {
            return "toUnknown"; // parse-robustness: a stable token, never an NPE
        }
        String[] parts = expr.kind().name().toLowerCase(java.util.Locale.ROOT).split("_");
        StringBuilder sb = new StringBuilder("to");
        for (String p : parts) {
            sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1));
        }
        if (expr.kind() == com.regnosys.rosetta.ast.enums.ConversionKind.ENUM
                && expr.targetEnum().isEmpty()) {
            sb.append("Unresolved");
        }
        return sb.toString();
    }

    /** The #500 arm-B listLitVisit facet — grammar at {@link #thenVisitFacets}. */
    private String listLitVisitFacet(RListLiteral expr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "listLitVisit.noWorkspace";
        }
        int n = expr.elements().size();
        String bucket = n <= 2 ? "n" + n : "n3plus";
        String verdict = adapter.adapt(expr, ws).isPresent() ? "lowers"
                : "declined." + reasonHead(expr, ws);
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        String parent = expr.parent() == null ? "orphan"
                : expr.parent().getClass().getSimpleName();
        return "listLitVisit." + bucket + "." + verdict + "." + typing + "." + parent;
    }

    /** The #500 arm-B onlyExistsVisit facet — grammar at {@link #thenVisitFacets}. */
    private String onlyExistsVisitFacet(ROnlyExistsExpr expr) {
        GeneratorModel gm = getGeneratorModel();
        RWorkspace ws = gm == null ? null : gm.workspace();
        if (ws == null) {
            return "onlyExistsVisit.noWorkspace";
        }
        var elements = expr.elements();
        String bucket = elements.size() <= 2 ? "n" + elements.size() : "n3plus";
        StringBuilder roots = new StringBuilder();
        for (int i = 0; i < elements.size() && i < 3; i++) {
            var el = elements.get(i);
            if (roots.length() > 0) {
                roots.append('+');
            }
            roots.append(el.isRootItem() ? "item"
                    : el.receiverExpression() != null ? "recvExpr" : "named");
        }
        if (elements.size() > 3) {
            roots.append("+more");
        }
        RMetaAnnotatedType t = ws.getInferredType(expr);
        String typing = t == null || t.isMissing() ? "typeMissing" : "typed";
        return "onlyExistsVisit." + bucket + "."
                + (roots.length() == 0 ? "noElements" : roots.toString()) + "." + typing;
    }

    /**
     * The single decline bump site (#475): increments the scalar {@link #irDeclinedCount}, the
     * per-family map and the per-site map together — the three conserve by construction (the #469
     * per-arm-meter law: the breakdown and the scalar move at the same seat, so the tokens sum to the
     * total and the receipts cannot disagree). Replaces the five bare {@code irDeclinedCount++} bumps
     * inside {@link #tryEmitFromIR}; each caller passes its seat's site tag (the
     * {@link #irDeclinedBySite} javadoc lists the five). The {@link #DECLINE_WITNESS} channel prints
     * the per-claim sample line here — same seat as the meters, so witness ≡ meter by construction.
     */
    private void recordDecline(String site, RExpression expr) {
        irDeclinedCount++;
        irDeclinedByFamily.merge(expr.getClass().getSimpleName(), 1, Integer::sum);
        irDeclinedBySite.merge(site, 1, Integer::sum);
        if (DECLINE_WITNESS) {
            RFunction witnessFn = HandlerHelper.findEnclosingFunction(expr);
            RRule witnessRule = witnessFn == null ? HandlerHelper.findEnclosingRule(expr) : null;
            System.out.println("[irDecline witness] site=" + site
                    + " family=" + expr.getClass().getSimpleName()
                    + " in=" + (witnessFn != null ? witnessFn.name()
                            : witnessRule != null ? witnessRule.name() : "<unresolved>"));
        }
    }

    /**
     * The #476 blocker probe's per-claim body: attributes ONE adapterGap-declined claim to its
     * minimal-blocker families — and, since #477, to their {@code Family:reason} arm-gate pairs.
     * {@link #collectMinimalBlockers} re-adapts the subtree bottom-up recording one pair per minimal
     * blocker; the families derived from the pairs land on {@link #blockerClaimsByFamily} (one count
     * per family per claim — byte-identical to the pre-#477 probe), a single-family claim
     * additionally counts on {@link #soleBlockerClaimsByFamily} — the claims that family's adapter
     * teach fully unblocks — and the pairs land on the #477 reason maps
     * ({@link #blockerClaimsByReason} / {@link #soleReasonClaimsByReason} /
     * {@link #soleFamilyMultiReasonClaims}, conserving against the family maps at this single
     * seat). A failing subtree always yields at least one minimal blocker (a failing node with no
     * blocked descendants records itself, leaves included), so a lowering root re-adapt — the only
     * route to an empty pair set — is counted on {@link #blockerProbeAnomalyCount} instead (it
     * contradicts the decline that got us here).
     */
    private void probeMinimalBlockers(RExpression claimRoot, RWorkspace workspace) {
        blockerProbedClaimCount++;
        Set<String> pairs = new TreeSet<>();
        boolean rootLowered = collectMinimalBlockers(claimRoot, claimRoot, workspace, pairs);
        if (rootLowered || pairs.isEmpty()) {
            blockerProbeAnomalyCount++;
            return;
        }
        // The family set is DERIVED from the pairs (every minimal blocker contributes exactly one
        // "Family:reason" pair whose prefix is its class simple name — a class name never contains
        // ':'), so the #476 family-level counters below read byte-identically to the pre-#477 probe.
        Set<String> families = new TreeSet<>();
        for (String pair : pairs) {
            families.add(pair.substring(0, pair.indexOf(':')));
        }
        for (String family : families) {
            blockerClaimsByFamily.merge(family, 1, Integer::sum);
        }
        if (families.size() == 1) {
            soleBlockerClaimsByFamily.merge(families.iterator().next(), 1, Integer::sum);
        }
        // #477 — the per-ARM refinement decode: the same claim units re-keyed by Family:reason. The
        // three merges below and the two above share this single seat, so the conservation identity
        // (soleFamily == Σ soleReason + soleFamilyMultiReason, per family) holds by construction.
        for (String pair : pairs) {
            blockerClaimsByReason.merge(pair, 1, Integer::sum);
        }
        if (pairs.size() == 1) {
            soleReasonClaimsByReason.merge(pairs.iterator().next(), 1, Integer::sum);
        } else if (families.size() == 1) {
            soleFamilyMultiReasonClaims.merge(families.iterator().next(), 1, Integer::sum);
        }
    }

    /**
     * The #476 probe's recursive arm. Returns true iff {@code expr} lowers
     * ({@code adapter.adapt} non-empty — a lowering subtree contains no blockers and is not
     * descended into). When it does not lower, recurses into the nearest expression descendants
     * ({@link #collectNearestExpressions} — THROUGH non-expression supporting nodes); iff none of
     * them is blocked, {@code expr} itself is the minimal blocker and its {@code Family:reason}
     * pair joins {@code out} (#477 — the reason is the adapter's own first-failing-gate token).
     * Calls the adapter directly (stateless by its documented contract), never the visitor — the
     * standing driven/declined/untargeted counters cannot move under the probe. {@code claimRoot}
     * is threaded down UNCHANGED (the #498 position leg: a blocker node that IS the claim root
     * lowers at the claim's own visit seat once its gate is taught, while an interior blocker
     * under a natively-composed root re-declines at the EMITTER's null-renderer frame-slot gate
     * — the two teach outcomes differ, so the gate censuses read the split).
     */
    private boolean collectMinimalBlockers(RExpression expr, RExpression claimRoot, RWorkspace workspace,
            Set<String> out) {
        if (adapter.adapt(expr, workspace).isPresent()) {
            return true;
        }
        boolean anyChildBlocked = false;
        for (RExpression child : nearestExpressionDescendants(expr)) {
            if (!collectMinimalBlockers(child, claimRoot, workspace, out)) {
                anyChildBlocked = true;
            }
        }
        if (!anyChildBlocked) {
            // #477: attribute the minimal blocker to its arm gate — the adapter's own decline-reason
            // channel (empty ⟺ lowers by its contract; the adapt above just declined and the adapter
            // is stateless, so the reason is always present — the orElse arm is the adapter's OWN
            // shared sentinel, the honest "cannot name the gate" residue, and a nonzero
            // unattributed count in the ranked lines is triaged, never absorbed).
            String reason = adapter.declineReason(expr, workspace)
                    .orElse(ExpressionToIRAdapter.UNATTRIBUTED);
            out.add(expr.getClass().getSimpleName() + ":" + reason);
            // #478: the two disguised-navigation gates get a SHAPE sub-decode — the head/root class
            // plus the adapter's verdict on the exact legacy-synthesized equivalent (the
            // teach-sizing witness; see classifyNavGateBlocker). Node-occurrence unit, so it runs
            // per minimal blocker even when the deduped pair set already holds the token. The
            // input-feature channel matches by PREFIX: since the #478 arm, a decliner composes the
            // equivalent's own gate onto the channel token (`inputFeatureNav.<gate>`), and the
            // witness keys the CHANNEL so its buckets stay comparable across the teach.
            if (expr instanceof REnumValueRef enr
                    && (reason.startsWith("inputFeatureNav") || reason.startsWith("attributeChain"))) {
                // #480: the attributeChain channel matches by PREFIX too — since the chain arm, a
                // decliner composes the arm's own gate onto the channel token
                // (`attributeChain.<gate>`), and the witness keys the CHANNEL (the #478 law).
                classifyNavGateBlocker(enr,
                        reason.startsWith("attributeChain") ? "attributeChain" : "inputFeatureNav",
                        workspace);
            }
            // #479: the implicit-ROOT cluster gates get their own SHAPE sub-decode — the #478
            // witness proved all three bottom out at the synthetic input/item base; this witness
            // classifies each blocker by the LEGACY BINDING/SYNTHESIZER arm that would render its
            // root (see classifyImplicitRootBlocker). The attributeChain hook fires ADDITIONALLY to
            // the #478 channel above (which keeps its ruleInput/twoSegOffRoot/oneSegClosure buckets
            // byte-unchanged); the classifier itself skips the ruleInput population.
            if ((expr instanceof RFeatureCall && "receiverSyntheticItem".equals(reason))
                    || (expr instanceof RSymbolReference && reason.startsWith("attrOutsideFunction"))
                    || (expr instanceof REnumValueRef && reason.startsWith("attributeChain"))) {
                // #480: both cluster channels match by PREFIX — the arms' twins compose the first
                // failing gate onto the channel tokens (`attrOutsideFunction.<gate>` /
                // `attributeChain.<gate>`), and the witness keys the CHANNEL (the #478 law).
                classifyImplicitRootBlocker(expr, reason, workspace);
            }
            // #496: the binder-gate cross-read (the monster wave's leg-1 second channel) — the
            // lambda-machinery gate faces attributed by the exact widening lever that would move
            // each event (the classifier self-gates on the five gate spellings; every other
            // reason token is a no-op). Fires ADDITIONALLY to the #478/#479 witnesses above,
            // which keep their own buckets byte-unchanged.
            classifyBinderGateBlocker(expr, reason);
            // #498: the composed A+B cluster's gate censuses — the two top sole gates
            // (opOnlyElement / calleeNotFunction) read at the blocker node with first-sample
            // witnesses (node-occurrence unit; both classifiers self-gate on their exact reason
            // spellings, every other token a no-op). Fire ADDITIONALLY to the channels above,
            // which keep their own buckets byte-unchanged (incl. the #496 binderGate
            // onlyElement.arg face — the receiver-family cross-check this census refines).
            classifyOnlyElemGateBlocker(expr, reason, workspace, claimRoot);
            classifyCalleeGateBlocker(expr, reason, workspace, claimRoot);
            // #499: the composed A+B admission cluster's gate censuses — the #498-exposed
            // IRListOp consumer faces (recv/operand/arg) and the metaFeature interior population,
            // read at the blocker node with first-sample witnesses (node-occurrence unit; both
            // classifiers self-gate on their exact reason spellings, every other token a no-op).
            // Fire ADDITIONALLY to the channels above, which keep their buckets byte-unchanged.
            classifyListOpGateBlocker(expr, reason, workspace, claimRoot);
            classifyMetaFeatureGateBlocker(expr, reason, workspace, claimRoot);
            // #500: the composed A+B cluster's arm-A census — the four #499-exposed
            // IRMetaAccess consumer faces (recv/operand/arg/chainRecv) read at the blocker node
            // with first-sample witnesses (node-occurrence unit; the classifier self-gates on
            // its exact reason spellings, every other token a no-op). Fires ADDITIONALLY to the
            // channels above, which keep their buckets byte-unchanged.
            classifyMetaAccessGateBlocker(expr, reason, workspace, claimRoot);
            // #501: the composed A+C cluster's censuses — the four #500-exposed shallow-kind
            // consumer faces (operand:IROnlyExists/IRPipe + arg:IRConversion/IRPipe) and the
            // five REnumValueRef chain-drain faces (headSymbolNav / ruleInputChain /
            // sourceElementUnresolved / the two equivalentMetaAccess seats), read at the blocker
            // node with first-sample witnesses (node-occurrence unit; both classifiers self-gate
            // on their exact reason spellings, every other token a no-op). Fire ADDITIONALLY to
            // the channels above, which keep their buckets byte-unchanged.
            classifyShallowGateBlocker(expr, reason, workspace, claimRoot);
            classifyChainDrainGateBlocker(expr, reason, workspace, claimRoot);
            // #502: the composed A+B cluster's censuses — the six RSymbolReference-drain sole
            // faces (symbolNotAttribute / synthetic.aliasName.bodyTyped / synthetic.closureParam /
            // argItem / calleeMetaOutput / attrOutsideFunction.sourceElementUnresolved) and the
            // receiverSyntheticItem residue's deep-source + three-walk cross-read, at the blocker
            // node with first-sample witnesses (node-occurrence unit; both classifiers self-gate
            // on their exact reason spellings, every other token a no-op). Fire ADDITIONALLY to
            // the channels above, which keep their buckets byte-unchanged.
            classifySymbolDrainGateBlocker(expr, reason, workspace, claimRoot);
            classifySynItemGateBlocker(expr, reason, workspace, claimRoot);
            // #503: the composed A+B cluster's censuses — the three REqualityExpr-cluster
            // faces (allAnyModifier [both binary mirrors] / operand:IRToString /
            // operandAlias.clean.enumSibling) and the two RFeatureCall feature-resolution
            // faces (featureUnresolved.nonSymbolReceiver / .headUnresolved — the
            // #502-exposed parent-gate class), at the blocker node with first-sample
            // witnesses (node-occurrence unit; both classifiers self-gate on their exact
            // reason spellings, every other token a no-op). Fire ADDITIONALLY to the
            // channels above, which keep their buckets byte-unchanged.
            classifyEqualityGateBlocker(expr, reason, workspace, claimRoot);
            classifyFeatureDrainGateBlocker(expr, reason, workspace, claimRoot);
            // #504: the composed A+B+C cluster's censuses — the boolean-operand cluster's
            // three faces (operandBareBooleanCall [RLogicalExpr] / operand:IRPointFreeApply
            // [its three operand seats]) and the RSymbolReference qualified-name residue's
            // three faces (symbolNotAttribute [+ the legacy-recovery SEAT decode] / argEmpty /
            // symbolUnresolved.synthetic.absent), at the blocker node with first-sample
            // witnesses (node-occurrence unit; both classifiers self-gate on their exact
            // reason spellings, every other token a no-op). Fire ADDITIONALLY to the
            // channels above, which keep their buckets byte-unchanged. Arm B's decode rides
            // the standing #502 synItemGate census (the same probe output) — no new channel.
            classifyBooleanOpGateBlocker(expr, reason, workspace, claimRoot);
            classifyQualNameGateBlocker(expr, reason, workspace, claimRoot);
            // #505: the composed A+B+C cluster's censuses — the REnumValueRef chain-bucket
            // faces (attributeChain.closureParamHead / choiceOption / attributeChain
            // .noFilterExtractBinder / attributeChain.equivalentSynItemSpine), the
            // RSymbolReference alias-rooted argument residue (the argNav.alias.* prefix —
            // the #492-named follow-up's own decode), and the DISPATCH-class design census
            // (symbolUnresolved.synthetic.absent — the #504-decoded base-signature
            // scope-join class), at the blocker node with first-sample witnesses
            // (node-occurrence unit; the classifiers self-gate on their exact reason
            // spellings, every other token a no-op). Fire ADDITIONALLY to the channels
            // above, which keep their buckets byte-unchanged; the arg:IRListOp and
            // srcElemBare decodes ride the standing #499/#502 censuses (the same probe
            // output) and the chain-bucket sourceElementUnresolved/equivalentMetaAccess
            // faces ride the standing #501 chainDrainGate census — no new channels there.
            classifyEnumSeatGateBlocker(expr, reason, workspace, claimRoot);
            classifyArgResidueGateBlocker(expr, reason, workspace, claimRoot);
            classifyDispatchGateBlocker(expr, reason, workspace, claimRoot);
            // #506: the walk-widening cluster's census — the six binder/element-derivation
            // faces (the two noFilterExtractBinder seats / closureParamHead.twNull /
            // itemNotFilterExtractBound / the two sourceElementUnresolved seats — ONE shared
            // walk family), at the blocker node with first-sample witnesses (node-occurrence
            // unit; the classifier self-gates on its exact reason spellings, every other token
            // a no-op). Fires ADDITIONALLY to the channels above, which keep their buckets
            // byte-unchanged (incl. the #505 enumSeatGate chainNoBinder rows and the #501/#502
            // srcElem/srcElemBare rows this census refines at a finer grain).
            classifyWalkBindGateBlocker(expr, reason, workspace, claimRoot);
            // #507: the composed A+B cluster's censuses — the RDeepFeatureCall noAdaptArm face
            // (the board lead: the family has no adapt arm, so the shape read sizes the arm's
            // provable serve population) and the REnumValueRef noResolutionChannel face (the
            // no-channel decode: record-feature disguised navs / direct meta reads / genuinely
            // unresolved, by the head's own scope-resolution fact), at the blocker node with
            // first-sample witnesses (node-occurrence unit; both classifiers self-gate on their
            // exact reason spellings, every other token a no-op). Fire ADDITIONALLY to the
            // channels above, which keep their buckets byte-unchanged. Arm B's L-111 residue
            // decode rides its own SEAT census (l111ResidueFacets — the relabel seat, not this
            // attribution walk).
            classifyDeepGateBlocker(expr, reason, workspace, claimRoot);
            classifyNoChanGateBlocker(expr, reason, workspace, claimRoot);
            // #508: the composed A+B cluster's censuses — the meta-facet + alias-nav cluster
            // (the L-111 meta single-hop / the alias-head meta-feature navs at BOTH seats /
            // the parsed-seat plain alias nav / the meta-bearing implicit-item chains, with
            // the arms' OWN equivalents re-adapted for the exact excluded shapes), the L-029
            // meta-sourced lambda pair (the closureParamHead.twNull binder-source decode +
            // the itemMetaSourced binding shapes), and the symbolNotAttribute recovery
            // residue (the STANDING #504 arm ladders re-run in the REAL parent context —
            // argFires/eqFires = the context-loss slice, the gate tokens the uncovered
            // classes), at the blocker node with first-sample witnesses (node-occurrence
            // unit; the classifiers self-gate on their exact reason spellings, every other
            // token a no-op). Fire ADDITIONALLY to the channels above, which keep their
            // buckets byte-unchanged (incl. the #501 chainDrainGate chainMeta rows, the #504
            // qualNameGate seat rows and the #506 walkBindGate cpTw/itemBind rows these
            // censuses refine at a finer grain).
            classifyMetaHopGateBlocker(expr, reason, workspace, claimRoot);
            classifyMetaSrcGateBlocker(expr, reason, workspace, claimRoot);
            classifySymNotGateBlocker(expr, reason, workspace, claimRoot);
            // #509: the composed A+B cluster's censuses — the parsed-seat alias-nav decline
            // pair (the guard-held featureOnBody 143 + the featureOffBody 56 residue — the
            // identity-serve leg's sizing read) and the feature-resolution pair
            // (headUnresolved 112 decoded by the head's own channels re-run in the REAL
            // context · nonSymbolReceiver 111 decoded by the compound receiver's adapter
            // verdict + engine type + feature membership), at the blocker node with
            // first-sample witnesses (node-occurrence unit; the classifiers self-gate on
            // their exact reason spellings, every other token a no-op). Fire ADDITIONALLY to
            // the channels above, which keep their buckets byte-unchanged.
            classifyAliasIdGateBlocker(expr, reason, workspace, claimRoot);
            classifyHeadUnGateBlocker(expr, reason, workspace, claimRoot);
            classifyNsrGateBlocker(expr, reason, workspace, claimRoot);
            // #520 arm-A3: the featureUnresolved.headOther face's own decode (the face had
            // NO standing walker decode — the calleeGate/nsrGate pattern at the third
            // feature-resolution face; the classifier self-gates on its exact reason
            // spelling, every other token a no-op). Fires ADDITIONALLY to the channels
            // above, which keep their buckets byte-unchanged.
            classifyHeadOtherGateBlocker(expr, reason, workspace, claimRoot);
            // #524: the B-cluster pre-arm census — the six in-lambda symbolUnresolved
            // faces (the synthetic item-attr pair + the lambdaSrc compose) decoded by
            // legacy's OWN bare-name ladder BY CALL + the adapter's verdict on the exact
            // synthesized equivalent (the classifier self-gates on its exact reason
            // spellings, every other token a no-op). Fires ADDITIONALLY to the channels
            // above, which keep their buckets byte-unchanged.
            classifyItemAttrGateBlocker(expr, reason, workspace, claimRoot);
            // #526: the NotExpressible-cluster pre-arm census — the child-subtree faces
            // (the list-element / conditional-branch / lambda-body seats + the
            // same-predicate sibling seats as sweep-boundary negatives) decoded by the
            // adapter's OWN position-divergence predicates BY CALL + the named slot's root
            // re-adapt verdict + the raw-parent glue path to the claim root (the
            // classifier self-gates on its exact family:reason pairs, every other token a
            // no-op). Fires ADDITIONALLY to the channels above, which keep their buckets
            // byte-unchanged.
            classifyNeGateBlocker(expr, reason, workspace, claimRoot);
            // #527: the ARG_NAV-dict pre-arm census — the typeGap/typeMissing residue faces
            // decoded by legacy's OWN evaluate-arg coercion-arm preconditions BY CALL (the
            // numeric hoist arms the adapter's #489 R-equality mirror conservatively walls)
            // + the standing post-pin scan's claim-root verdict (the serve-route fact). The
            // classifier self-gates on its exact reason prefixes, every other token a no-op.
            classifyArgGapGateBlocker(expr, reason, workspace, claimRoot);
            // #528: the operand/arg residue pre-arm census — the five surviving operand and
            // call-seat faces decoded by the ADAPTER's own gate predicates BY CALL (the
            // widened producesComparisonResult / isComparisonOperand / the two item gates /
            // isMetaAnnotated), seat-complete across every family that can mint each spelling
            // (the zero-carrier seats stand as sweep-boundary negatives — the pf face's
            // standing #504 booleanOpGate channel is family-blind to the COMPARISON seat,
            // which is exactly where its residue survives). The classifier self-gates on its
            // exact reason spellings, every other token a no-op. Fires ADDITIONALLY to the
            // channels above, which keep their buckets byte-unchanged.
            classifyOpResGateBlocker(expr, reason, workspace, claimRoot);
            // #529: the ingest-wall pricing censuses + the A-tail censuses — the headMiss
            // .other pool re-read against the IMPLICIT-ITEM channel (the binder ladder the
            // #507 record arm never searches), the declMiss pool's head type-class +
            // qualifier verdict, the #528 mint's equality-seat frontier, and the argItem
            // residue's provable binder-source channels (all BY CALL through the adapter's
            // own gates; the classifiers self-gate on their exact reason spellings, every
            // other token a no-op). Fire ADDITIONALLY to the channels above, which keep
            // their buckets byte-unchanged.
            classifyItemHeadGateBlocker(expr, reason, workspace, claimRoot);
            classifyHeadAttrGateBlocker(expr, reason, workspace, claimRoot);
            classifyEqImplGateBlocker(expr, reason, workspace, claimRoot);
            classifyItemArgSrcGateBlocker(expr, reason, workspace, claimRoot);
            // #530: the endgame-residue pre-arm censuses — the four surviving clusters
            // (the elided-pipe deep bottoms · the nsr missing-type naming · the
            // symbolNotAttribute survivor seats · the meta-param call-arg render
            // preconditions), all BY CALL through the adapter's own walks/gates or
            // model-declaration reads (the classifiers self-gate on their exact reason
            // spellings/prefixes, every other token a no-op). Fire ADDITIONALLY to the
            // channels above, which keep their buckets byte-unchanged.
            classifyPipeCondGateBlocker(expr, reason, workspace, claimRoot);
            classifyNsrCtGateBlocker(expr, reason, workspace, claimRoot);
            classifyKvpGateBlocker(expr, reason, workspace, claimRoot);
            classifyMetaArgGateBlocker(expr, reason, workspace, claimRoot);
        }
        return false;
    }

    /**
     * The #478 nav-gate shape witness: classifies ONE {@link REnumValueRef} minimal blocker on the
     * {@code inputFeatureNav}/{@code attributeChain} gates by (1) the head/root class in legacy
     * {@code ReferenceHandler}'s own resolution order ({@link #navHeadClass}; the attribute-chain
     * shapes follow {@code synthesizeImplicitInputChain}'s own null contract) and (2) the adapter's
     * verdict on the EXACT equivalent navigation legacy synthesizes for the node — recording into
     * {@link #navGateShapeBuckets} / {@link #navGatePositionBuckets} / {@link #navGateFirstSamples}.
     * Read-only against the tree (the synthesized equivalent is a fresh node — exactly the one the
     * live legacy render builds on this same path) and adapter-only (stateless by its documented
     * contract), so the standing driven/declined/untargeted counters cannot move.
     */
    private void classifyNavGateBlocker(REnumValueRef enr, String reason, RWorkspace workspace) {
        String shape;
        RFeatureCall equivalent;
        if ("attributeChain".equals(reason)) {
            REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
            if (chain == null) {
                return; // contract-impossible (the reason token implies the bind) — defensive no-op
            }
            if (chain.attributeOpt().isEmpty()) {
                shape = "oneSegClosure";
                equivalent = null; // the chain synthesizer's own null case — a lambda-local head
            } else {
                equivalent = getReferenceHandler().synthesizeImplicitInputChain(enr, chain);
                shape = equivalent != null ? "ruleInput" : "twoSegOffRoot";
            }
        } else {
            shape = navHeadClass(enr);
            equivalent = getReferenceHandler().synthesizeFeatureCall(enr, this);
        }
        String verdict = adapterVerdictOnEquivalent(equivalent, workspace);
        String bucket = reason + ":" + shape + ":" + verdict;
        navGateShapeBuckets.merge(bucket, 1, Integer::sum);
        boolean chained = enr.parent() instanceof RFeatureCall pfc && pfc.receiver() == enr;
        navGatePositionBuckets.merge(reason + ":" + (chained ? "receiverOfChain" : "directPosition"),
                1, Integer::sum);
        navGateFirstSamples.putIfAbsent(bucket, describeNavGateSite(enr));
    }

    /**
     * The head class of a disguised {@code <head> -> <feature>} input-feature navigation, in legacy
     * {@code ReferenceHandler.resolveNameInFunction}'s OWN resolution order (inputs → output →
     * shortcuts; then the data-type-condition head the no-function branch of
     * {@code synthesizeFeatureCall} resolves) — the classes decide which receiver the synthesized
     * equivalent carries, hence which adapter verdict the shape can reach ({@code headInput} is the
     * param-receiver slice; a {@code headShortcut} head nulls to an UNRESOLVED receiver by the
     * legacy resolver's own contract, so the equivalent's FEATURE gate fires before the receiver
     * ever adapts and the verdict reads {@code declines:featureUnresolved.*} — the #492 recut: the
     * old {@code receiverBase.symbolUnresolved} prediction was refuted at source, the
     * {@code receiverBase.} prefix wraps only a {@code receiverNotExpressible} token and a
     * feature-less equivalent never reaches the receiver recursion; {@code headOther} produces the
     * SAME null-head equivalent and the same verdict family, the two separating only on this
     * bucket key's shape axis. Since the #492 alias-nav teach the verdict spelling is faceted
     * ({@code featureUnresolved.aliasHead.<face>}) and the ADMITTED faces lower instead — the
     * {@code headShortcut} bucket reads the arm's residue).
     */
    private String navHeadClass(REnumValueRef enr) {
        String head = enr.enumName();
        if (head == null || head.isEmpty()) {
            return "headOther";
        }
        RFunction fn = HandlerHelper.findEnclosingFunction(enr);
        if (fn != null) {
            for (RAttribute input : fn.inputs()) {
                if (head.equals(input.name())) {
                    return "headInput";
                }
            }
            if (fn.output().isPresent() && head.equals(fn.output().get().name())) {
                return "headOutput";
            }
            for (RShortcut shortcut : fn.shortcuts()) {
                if (head.equals(shortcut.name())) {
                    return "headShortcut";
                }
            }
            return "headOther";
        }
        RCondition condition = HandlerHelper.findEnclosingTypeCondition(enr);
        if (condition != null && condition.parent() instanceof RDataType declaringType
                && HandlerHelper.findAttributeOnDataType(declaringType, head) != null) {
            return "headCondAttr";
        }
        return "headOther";
    }

    /** The #478 witness sample form: {@code in=<function|rule|?> <head>-><feature>}. */
    private String describeNavGateSite(REnumValueRef enr) {
        RFunction fn = HandlerHelper.findEnclosingFunction(enr);
        RRule rule = fn == null ? HandlerHelper.findEnclosingRule(enr) : null;
        String in = fn != null ? fn.name() : rule != null ? rule.name() : "<unresolved>";
        return "in=" + in + " " + enr.enumName() + "->" + enr.valueName();
    }

    /**
     * The ADAPTER's verdict on a legacy-synthesized equivalent — the #478 witness's shared verdict
     * machinery ({@code lowers} = adapter admissibility ONLY, see {@link #navGateShapeBuckets};
     * {@code declines:<token>} with a {@code receiverNotExpressible} chain wrapper descended to the
     * failing BASE's own {@code receiverBase.}-prefixed token; {@code noEquivalent} for a null
     * synthesis). Factored out at #479 so the implicit-root witness shares the single
     * implementation (the #477 cross-module-drift lesson applied module-locally).
     */
    private String adapterVerdictOnEquivalent(RFeatureCall equivalent, RWorkspace workspace) {
        if (equivalent == null) {
            return "noEquivalent";
        }
        if (adapter.adapt(equivalent, workspace).isPresent()) {
            return "lowers";
        }
        String token = adapter.declineReason(equivalent, workspace)
                .orElse(ExpressionToIRAdapter.UNATTRIBUTED);
        if ("receiverNotExpressible".equals(token)) {
            // Name the failing BASE, not the coarse chain wrapper: descend the equivalent's
            // receiver spine to the deepest non-expressible hop and prefix its own token.
            RExpression at = equivalent.receiver();
            int guard = 0;
            while (at != null && guard++ < 16) {
                String sub = adapter.declineReason(at, workspace).orElse(null);
                if (sub == null) {
                    break; // the receiver lowers — the failure was the outer hop's own gate
                }
                if ("receiverNotExpressible".equals(sub) && at instanceof RFeatureCall inner) {
                    at = inner.receiver();
                    continue;
                }
                token = "receiverBase." + sub;
                break;
            }
        }
        return "declines:" + token;
    }

    /**
     * The #479 implicit-root shape witness: classifies ONE minimal blocker on the three
     * implicit-ROOT cluster gates by the LEGACY machinery that renders its root — recording into
     * {@link #implicitRootShapeBuckets} / {@link #implicitRootFirstSamples}. Per gate:
     * <ul>
     *   <li>{@code RFeatureCall:receiverSyntheticItem} — the raw synthetic receiver's BINDING
     *       context in legacy {@code ReferenceHandler.handle(RImplicitVariable)}'s own arm order
     *       ({@link #syntheticItemContext}, restated read-only — the scope-LIVE arms classify by
     *       their AST boundary; the render decision stays legacy's at emit time) × the lowered
     *       item's own type facts through the adapter's channel ({@link #syntheticItemVerdict});</li>
     *   <li>{@code RSymbolReference:attrOutsideFunction} — legacy {@code handle(RSymbolReference)}'s
     *       OWN synthesizer chain in source order ({@code synthesizeImplicitInputNavigation} split
     *       top-level/in-lambda by the receiver-builder's own test → the widened
     *       {@code synthesizeImplicitItemNavigation} → the widened
     *       {@code synthesizeConditionInstanceNavigation}) × the adapter's verdict on the EXACT
     *       synthesized equivalent ({@link #adapterVerdictOnEquivalent});</li>
     *   <li>{@code REnumValueRef:attributeChain} whose rule-input chain synthesizer NULLS — the
     *       fall-through decode in legacy {@code handle(REnumValueRef)}'s own order (the widened
     *       {@code synthesizeImplicitItemChain} → the scope-gated case-narrowed arm, AST-classified
     *       → the #478 {@code synthesizeFeatureCall} seam); the ruleInput population stays on the
     *       #478 channel byte-unchanged.</li>
     * </ul>
     * Read-only against the tree (fresh synthesized nodes only — exactly the ones the live legacy
     * render builds on these same paths) and adapter-only (stateless by its documented contract),
     * so the standing driven/declined/untargeted counters cannot move.
     */
    private void classifyImplicitRootBlocker(RExpression expr, String reason, RWorkspace workspace) {
        String bucket;
        String sample;
        if (expr instanceof RFeatureCall fc && "receiverSyntheticItem".equals(reason)) {
            if (!(fc.receiver() instanceof RImplicitVariable iv) || !iv.isSynthetic()) {
                return; // contract-impossible (SYNTHETIC_ITEM lowers only from a synthetic implicit)
            }
            String context = syntheticItemContext(iv);
            String verdict;
            if ("inLambdaImplicit".equals(context)) {
                // The candidate-arm facet: split the dominant context by the BINDER op (the
                // adapter's own filter/extract-bound contract — the L-029-proven render family)
                // and read the RETYPE-SOURCE channel — the binder ARGUMENT's engine type, the
                // Cat-8 element-type law an adapter-boundary retype would apply (the #478
                // cache-boundary class: the synthetic node itself always reads MISSING).
                RExpression source = filterExtractBindingSourceOf(iv);
                if (source == null) {
                    context = "inLambdaImplicit.otherBinder";
                    verdict = syntheticItemVerdict(iv, workspace);
                } else {
                    context = "inLambdaImplicit.filterExtract";
                    verdict = retypeSourceFacet(source, workspace);
                    if ("unprovableSource".equals(verdict)) {
                        // #480: the SOURCE decode — sub-classify the allowlist residue by the
                        // source expression's AST shape (the widening map; Σ over the sub-buckets
                        // ≡ this standing bucket's count, a receipt-side conservation check).
                        recordImplicitRootSource(
                                "synItem:unprovableSource." + unprovableSourceShapeToken(source, 0),
                                describeImplicitRootSite(fc, "item->" + fc.featureName()));
                        if (source instanceof RImplicitVariable elided) {
                            // #481: the elided-PIPE decode — the planned widening's admission
                            // restated per occurrence (the flat elidedImplicit token's own facet;
                            // Σ over the pipe facets ≡ that token's count by construction).
                            recordElidedPipeDecode("synItem", elided,
                                    describeImplicitRootSite(fc, "item->" + fc.featureName()),
                                    workspace);
                        }
                    }
                }
            } else {
                verdict = syntheticItemVerdict(iv, workspace);
            }
            bucket = "synItemNav:" + context + ":" + verdict;
            sample = describeImplicitRootSite(fc, "item->" + fc.featureName());
        } else if (expr instanceof RSymbolReference ref && reason.startsWith("attrOutsideFunction")) {
            RAttribute attr = ref.symbol().filter(RAttribute.class::isInstance)
                    .map(RAttribute.class::cast).orElse(null);
            if (attr == null) {
                return; // contract-impossible (the reason token implies the attribute bind)
            }
            RFeatureCall equivalent = getReferenceHandler().synthesizeImplicitInputNavigation(ref, attr);
            String shape;
            if (equivalent != null) {
                // buildImplicitInputReceiver's own split: an in-lambda chain roots at the synthetic
                // item, a top-level one at the synthetic `input` reference.
                shape = hasEnclosingRuleLambda(ref) ? "ruleInputLambda" : "ruleInputTop";
            } else if ((equivalent = ReferenceHandler.synthesizeImplicitItemNavigation(
                    ref, attr, this)) != null) {
                shape = "itemNav";
            } else if ((equivalent = getReferenceHandler().synthesizeConditionInstanceNavigation(
                    ref, attr, this)) != null) {
                shape = "condInstance";
            } else {
                shape = "noSynthesizer";
            }
            String verdict = adapterVerdictOnEquivalent(equivalent, workspace);
            bucket = "bareAttr:" + shape + ":" + verdict;
            sample = describeImplicitRootSite(ref, ref.name());
            if ("itemNav".equals(shape)) {
                // #480: the bare-attr arm's admission, restated per blocker (the pre-teach
                // pre-sizing; post-teach `claims` reads ZERO — the conservation signature).
                implicitRootArmBuckets.merge(
                        "bareAttrArm:" + bareAttrArmVerdict(ref, attr, verdict, workspace),
                        1, Integer::sum);
            }
            if (("itemNav".equals(shape) || "ruleInputLambda".equals(shape))
                    && verdict.endsWith("receiverSyntheticItem")) {
                // #480: the still-declining equivalent's implicit BASE, decoded (the widening map).
                recordEquivalentBaseDecode("itemNav".equals(shape) ? "bareAttr" : "ruleInputLambda",
                        equivalent, sample, workspace);
            }
        } else if (expr instanceof REnumValueRef enr && reason.startsWith("attributeChain")) {
            REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
            if (chain == null) {
                return; // contract-impossible (the reason token implies the bind)
            }
            if (chain.attributeOpt().isPresent()
                    && getReferenceHandler().synthesizeImplicitInputChain(enr, chain) != null) {
                return; // the ruleInput population — the #478 channel's buckets carry it
            }
            RFeatureCall equivalent = getReferenceHandler().synthesizeImplicitItemChain(enr, this);
            String shape;
            String verdict;
            if (equivalent != null) {
                shape = "itemChain";
                verdict = adapterVerdictOnEquivalent(equivalent, workspace);
                // #480: the chain arm's admission, restated per blocker (the pre-teach
                // pre-sizing; post-teach `claims` reads ZERO — the conservation signature) +
                // the still-declining equivalent's implicit BASE decode (the widening map).
                implicitRootArmBuckets.merge(
                        "chainArm:" + chainArmVerdict(enr, verdict, workspace), 1, Integer::sum);
                if (verdict.endsWith("receiverSyntheticItem")) {
                    recordEquivalentBaseDecode("chainBase", equivalent,
                            describeImplicitRootSite(enr, enr.enumName() + "->" + enr.valueName()),
                            workspace);
                }
            } else if (underSwitchCaseBeforeLambda(enr)) {
                // The #368 F-B case-narrowed arm binds only under a rendering instanceof ladder
                // (a live subject binding cannot exist under the probe) — AST-classified only.
                shape = "caseNarrowedScope";
                verdict = "scopeGated";
            } else {
                shape = "genericSynth";
                verdict = adapterVerdictOnEquivalent(
                        getReferenceHandler().synthesizeFeatureCall(enr, this), workspace);
            }
            bucket = "chainFall:" + shape + ":" + verdict;
            sample = describeImplicitRootSite(enr, enr.enumName() + "->" + enr.valueName());
        } else {
            return;
        }
        implicitRootShapeBuckets.merge(bucket, 1, Integer::sum);
        implicitRootFirstSamples.putIfAbsent(bucket, sample);
    }

    /**
     * The synthetic item's BINDING context, restating legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)}'s own arm order READ-ONLY (no scope
     * access — the scope-LIVE arms classify by their AST boundary; at emit time the render
     * decision is always legacy's own): {@code thenBody} (the thenArg re-root family — the
     * nearest inline fn is a then-step body) → {@code namedExtractInputForm} (the #357/#364
     * named-top-level-extract-over-bare-input shapes, rendering {@code MapperS.of(input)}) /
     * {@code namedExtractBody} (the #367/#368/#375 walk-out family — a live wrapper binding
     * decides) → {@code switchCase} (the #221 subject binding — a bare cast var whose
     * {@code MapperS.of} wrap is consumer-side) → {@code condTopLevel} (the condition-instance
     * render) → {@code ruleTopLevel} (the elided operand at rule-body top level —
     * {@code MapperS.of(input)}, the fixed render) → {@code inLambdaImplicit} /
     * {@code inLambdaNamed} ({@code enclosingLambdaBinding}'s own split: the bound lambda param) →
     * {@code otherTopLevel}.
     *
     * <p>Two DEEP nestings bucket by their outer boundary rather than legacy's live-scope
     * precedence (a switch under a then-body lambda reads {@code thenBody}; a named-extract body
     * under a switch reads {@code namedExtract*} where legacy consults the subject binding
     * first) — both sit wholly inside the non-claimable residue, and for the load-bearing case
     * (a switch inside an implicit filter/extract lambda) this classifier and the arm's
     * switch-precedence walk decline as exact complements (the Seat-1 #479 OBS-1 precision).
     */
    private static String syntheticItemContext(RImplicitVariable iv) {
        RInlineFunction nearest = nearestInlineFn(iv);
        if (nearest != null && nearest.parent() instanceof RThenExpr then
                && then.body().orElse(null) == nearest) {
            return "thenBody";
        }
        if (nearest != null && !nearest.isImplicit() && !nearest.paramNames().isEmpty()
                && nearest.parent() instanceof RExtractExpr ext && ext.body() == nearest) {
            return isNamedTopLevelExtractOverBareInput(nearest, ext)
                    ? "namedExtractInputForm" : "namedExtractBody";
        }
        if (nearestSwitchBeforeLambda(iv) != null) {
            return "switchCase";
        }
        if (nearest == null && HandlerHelper.findEnclosingTypeCondition(iv) != null) {
            return "condTopLevel";
        }
        if (isElidedOperandAtRuleTopLevel(iv)) {
            return "ruleTopLevel";
        }
        if (nearest != null) {
            return (nearest.isImplicit() || nearest.paramNames().isEmpty())
                    ? "inLambdaImplicit" : "inLambdaNamed";
        }
        return "otherTopLevel";
    }

    /**
     * The lowered synthetic item's own type facts through the ADAPTER's channel (self-checking —
     * the same lowering the receiver gate judged): {@code typed} (resolved, non-meta — the
     * teachable upper bound; an arm's admission still applies its own meta-sourced refinements) /
     * {@code typeMissing} / {@code typeMeta} / {@code notLowered} (contract-impossible — the
     * receiver token implies the lowering).
     */
    private String syntheticItemVerdict(RImplicitVariable iv, RWorkspace workspace) {
        Optional<IRExpr> lowered = adapter.adapt(iv, workspace);
        if (lowered.isEmpty() || !(lowered.get() instanceof IRVariable var)
                || var.variableKind() != IRVariable.VariableKind.SYNTHETIC_ITEM) {
            return "notLowered";
        }
        if (var.type() == null || var.type().isMissing()) {
            return "typeMissing";
        }
        if (var.type().hasMeta()) {
            return "typeMeta";
        }
        return "typed";
    }

    /** The #479 witness sample form: {@code in=<function|rule|?> <detail>}. */
    private String describeImplicitRootSite(RExpression site, String detail) {
        RFunction fn = HandlerHelper.findEnclosingFunction(site);
        RRule rule = fn == null ? HandlerHelper.findEnclosingRule(site) : null;
        String in = fn != null ? fn.name() : rule != null ? rule.name() : "<unresolved>";
        return "in=" + in + " " + detail;
    }

    /**
     * Defensive bound on the #479 witness's parent walks — the generator's
     * {@code HandlerHelper.PARENT_WALK_LIMIT} CONSULTED (64, the same limit the adapter's own walks
     * use; public since v3.2 seat 7 round 1, and consulted rather than mirrored since round 2 — the
     * rule-6 review's SF-2: the mirror's stated reason, "package-private there", was no longer true). A
     * compile-time constant, so javac INLINES the value here: a change to the generator's limit reaches
     * this module only when it is recompiled - the chain installs every module before each run (round
     * 3, the code-quality review's NIT-4).
     */
    private static final int PARENT_WALK_LIMIT =
            com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.PARENT_WALK_LIMIT;

    /**
     * The SOURCE (argument) of the nearest enclosing filter/extract whose body binds {@code start} —
     * restating the adapter's own {@code filterExtractBindingSource} walk (the nearest-lambda
     * contract): {@code null} when the nearest inline fn is not a filter/extract body.
     */
    private static RExpression filterExtractBindingSourceOf(RNode start) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline) {
                RNode p = inline.parent();
                if (p instanceof RFilterExpr filter && filter.body() == inline) {
                    return filter.argument();
                }
                if (p instanceof RExtractExpr extract && extract.body() == inline) {
                    return extract.argument();
                }
                return null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /** Restates the adapter's {@code isMetaAnnotated}: the attribute carries a {@code [metadata …]} annotation. */
    private static boolean isMetaAnnotatedAttr(RAttribute attr) {
        for (com.regnosys.rosetta.ast.annotations.RAnnotationRef annotationRef : attr.annotationRefs()) {
            if ("metadata".equals(annotationRef.annotationName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Restates the adapter's {@code isProvablyNonMetaElementSource} — the #479 arm's element-form
     * allowlist (last-hop feature call / non-meta bare attribute / since #484 a non-meta-OUTPUT
     * bare function reference (the callable-output identity, shared with the live arm exactly) /
     * bound disguised-chain leaf / all-provable list literal / filter recursion; everything else
     * NOT provable). The post-teach
     * conservation signature ({@code retypeSourceOk} ZERO on every cell) is the drift check
     * between this restatement and the arm's own predicate.
     */
    private static boolean isProvablyNonMetaElementSourceShape(RExpression source) {
        return isProvablyNonMetaElementSourceShape(source, false);
    }

    /**
     * The 2-arg form: {@code widened} restates the LIVE arm's allowlist — the #481 arm (a
     * grammar-elided PIPED implicit resolves to the enclosing then's argument,
     * {@link #resolveElidedPipedSourceShape}) plus, since #482, the {@link RThenExpr} arm (a
     * nested pipe proves through its BODY result — the element form of {@code A then B} is B's
     * body's own, the #481 value identity one structural seat deeper); the recursing shapes
     * (list literal / filter) recurse through the widened variant, so a nested source proves
     * exactly where the live arm proves it (the witness's verdict channel IS the arm's
     * channel — at #482 commit 2 the then arm was a separate PLANNED 3-arg tier read only by
     * {@link #thenPipeBodyFacet}; the commit-3 teach folded it into the live form, the
     * planned/live distinction dissolving exactly as #481's did; the #483 {@link RExtractExpr}
     * arm followed the same two-step — a PLANNED 3-arg {@code extractWidened} tier at the
     * witness commit, folded here at the teach: an extract-bodied source with an
     * IMPLICIT-or-paramless body proves through its BODY result, the element form of
     * {@code A extract B} being B's OWN result per element — every legacy map route (the
     * single/item routes and the LoL routes alike) produces result elements that ARE the
     * body's values, and the checker types the extract off its
     * BODY; a NAMED extract declines whole, the scope-live walk-out class, and the identity
     * {@code extract item} face declines through the resolver arm; the #484 callable-output
     * arm followed the same two-step — a PLANNED 3-arg {@code bareFnWidened} tier at the
     * witness commit, folded into the SHARED {@link RSymbolReference} case at the teach: a
     * bare no-arg function reference proves on its OUTPUT attribute, the aliasShadow
     * precedence declining first — the leg is cardinality-blind and recursion-free, so it
     * lives in the shared body exactly like the bare-attribute leg, mirroring the live arm;
     * the #485 rule-input arm followed the same two-step — a PLANNED 3-arg
     * {@code ruleTopWidened} tier at the witness commit, folded into the
     * {@link RImplicitVariable} leg's UNRESOLVED branch at the teach: a rule-top noBinder
     * implicit proves as the RULE INPUT on the declaration read,
     * {@link #ruleTopInputElementTypeShape} — recursion-free like the #484 leg; the #487
     * conditional arm followed the same two-step — a PLANNED 3-arg {@code conditionalJoin}
     * tier at the witness commit, folded here at the teach: a conditional proves under the
     * JOIN law (the then arm provable; an empty else — {@link #hasGenuineElseShape} —
     * admitting on the then proof alone, a genuine else proving too — the list-literal
     * all-provable law lifted to the two-armed choice; the arms are strict subtrees, the
     * #482 termination measure decreasing).
     * The 1-arg standing form
     * keeps the #479/#480 channels byte-stable. The {@code thenPipe} body facet branches
     * BEFORE this predicate in {@link #elidedPipeFacetToken}, so a then-resolving pipe source
     * classifies under its own {@code thenPipe.*} facets, never the #481 {@code thenArg.*}
     * verdicts (and the {@code extractPipe} facet branches the same way at both seats).
     */
    private static boolean isProvablyNonMetaElementSourceShape(RExpression source, boolean widened) {
        if (widened && source instanceof RThenExpr then) {
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return body != null && isProvablyNonMetaElementSourceShape(body, true);
        }
        if (widened && source instanceof RExtractExpr extract) {
            RInlineFunction bodyFn = extract.body();
            if (bodyFn == null || !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return false;
            }
            RExpression body = bodyFn.body();
            return body != null && isProvablyNonMetaElementSourceShape(body, true);
        }
        if (widened && source instanceof RImplicitVariable) {
            RExpression resolved = resolveElidedPipedSourceShape(source);
            if (resolved != null && !(resolved instanceof RImplicitVariable)) {
                return isProvablyNonMetaElementSourceShape(resolved, true);
            }
            // #485 (the teach fold — the planned 3-arg ruleTopWidened tier dissolved): the
            // rule-input face — an UNRESOLVED implicit whose walk stops noBinder at RULE top
            // proves as the RULE INPUT (legacy's isElidedOperandTopLevel → MapperS.of(input)
            // value identity; the declaration read, prove-or-decline).
            return resolved instanceof RImplicitVariable iv
                    && ruleTopInputElementTypeShape(iv) != null;
        }
        if (widened && source instanceof RConditionalExpr cond) {
            // #487 (the teach fold — the planned 3-arg conditionalJoin tier dissolved): the
            // conditional JOIN law — the THEN arm must prove; an EMPTY else (absent, `empty`,
            // or the DefaultElseRule synthetic empty list — hasGenuineElseShape's
            // structural-emptiness discriminator) contributes no elements and admits on the
            // then-proof alone; a genuine else must prove too (the list-literal all-provable
            // law lifted to the two-armed choice — the arm's mirror, restated).
            RExpression thenB = cond.thenBranch();
            if (thenB == null || !isProvablyNonMetaElementSourceShape(thenB, true)) {
                return false;
            }
            if (!hasGenuineElseShape(cond)) {
                return true;
            }
            RExpression elseB = cond.elseBranch().orElse(null);
            return elseB != null && isProvablyNonMetaElementSourceShape(elseB, true);
        }
        if (source instanceof RFeatureCall fc) {
            return fc.resolvedFeature().isPresent() && !isMetaAnnotatedAttr(fc.resolvedFeature().get());
        }
        if (source instanceof RSymbolReference sym) {
            if (sym.args().isEmpty() && sym.symbol().orElse(null) instanceof RAttribute attr) {
                return !isMetaAnnotatedAttr(attr);
            }
            if (sym.args().isEmpty() && sym.symbol().orElse(null) instanceof RFunction calleeFn) {
                // #484 (the teach fold — the planned 3-arg tier dissolved): the callable-output
                // admission, the live arm's leg restated verbatim — the aliasShadow precedence
                // declines first (legacy renders the colliding name as an alias invocation),
                // then the OUTPUT attribute must exist and be non-meta.
                RFunction enclosing = HandlerHelper.findEnclosingFunction(sym);
                if (enclosing != null && nameMatchesShortcutW(enclosing, sym.name())) {
                    return false;
                }
                RAttribute out = calleeFn.output().orElse(null);
                return out != null && !isMetaAnnotatedAttr(out);
            }
            return false;
        }
        if (source instanceof REnumValueRef evr) {
            if (evr.resolvedAttributeChain().isPresent()) {
                RAttribute leaf = evr.resolvedAttributeChain().get().feature();
                return leaf != null && !isMetaAnnotatedAttr(leaf);
            }
            return evr.resolvedInputFeature().isPresent()
                    && !isMetaAnnotatedAttr(evr.resolvedInputFeature().get());
        }
        if (source instanceof RListLiteral list) {
            if (list.elements().isEmpty()) {
                return false;
            }
            for (RExpression element : list.elements()) {
                if (!isProvablyNonMetaElementSourceShape(element, widened)) {
                    return false;
                }
            }
            return true;
        }
        if (source instanceof RFilterExpr filter) {
            return filter.argument() != null
                    && isProvablyNonMetaElementSourceShape(filter.argument(), widened);
        }
        return false;
    }

    /**
     * #487: restates the adapter's {@code hasGenuineElse} verbatim — whether a conditional has a
     * genuine, value-bearing {@code else} branch. {@code DefaultElseRule} stamps a SYNTHETIC
     * empty-list else onto every {@code if X then Y} that omits {@code else}, so presence alone
     * proves nothing; the discriminator is structural emptiness ({@code empty} / an empty list
     * literal), a user-written {@code else empty} reading identically to the injected default.
     * An empty else contributes NO elements, so the JOIN law's empty-else face admits on the
     * then-arm alone (the checker's own algebra: the empty faces type {@code NOTHING} — the
     * join's bottom — so {@code join(thenT, NOTHING) = thenT}, {@code TypeJoin}'s bottom rule).
     */
    private static boolean hasGenuineElseShape(RConditionalExpr conditional) {
        return conditional.elseBranch()
                .map(e -> !(e instanceof REmptyLiteral
                        || (e instanceof RListLiteral list && list.elements().isEmpty())))
                .orElse(false);
    }

    /**
     * #481: the planned resolver, restated — walk a grammar-elided PIPED implicit to the
     * enclosing THEN's ARGUMENT (legacy {@code handle(RImplicitVariable)}'s thenBody face: the
     * implicit takes the pipe input's value through both render channels, so the element form is
     * the argument's own — the value identity). The walk mirrors the #479 retype's conservatism
     * exactly: a switch before the lambda (legacy's thenArg arm walks past switches when its
     * scope binding is live and only the binding-dead face reaches the #221 subject machinery —
     * liveness is unknowable statically, so the face declines whole, the safe direction — the
     * Seat-1 #481 OBS-2 precision), a missing/named binder, or a non-then implicit binder
     * stops the resolution (the implicit returns UNRESOLVED and the caller declines it —
     * prove-or-decline); chained elided hops resolve iteratively under the same guard the shape
     * token's depth bound uses. Returns the resolved (non-elided) expression, the unresolved
     * implicit itself, or {@code null} for a null pipe argument.
     */
    private static RExpression resolveElidedPipedSourceShape(RExpression source) {
        RExpression cur = source;
        int hops = 0;
        while (cur instanceof RImplicitVariable iv && iv.isSynthetic() && hops++ < 8) {
            if (nearestSwitchBeforeLambda(iv) != null) {
                return cur;
            }
            RInlineFunction binder = nearestInlineFn(iv);
            if (binder == null || !(binder.isImplicit() || binder.paramNames().isEmpty())) {
                return cur;
            }
            if (!(binder.parent() instanceof RThenExpr then) || then.body().orElse(null) != binder) {
                return cur;
            }
            cur = then.argument();
        }
        return cur;
    }

    /**
     * The #479 retype-source facet of a filter/extract binding SOURCE, factored out at #480 so the
     * equivalent-base decode shares the single token set with the standing
     * {@code synItemNav:inLambdaImplicit.filterExtract} channel (byte-stability: the extraction is
     * token-preserving — {@code metaSourced} / {@code unprovableSource} /
     * {@code retypeSourceMissing} / {@code retypeSourceMeta} / {@code retypeSourceOk}).
     */
    private static String retypeSourceFacet(RExpression source, RWorkspace workspace) {
        if (source instanceof RFeatureCall srcFc && srcFc.resolvedFeature().isPresent()
                && isMetaAnnotatedAttr(srcFc.resolvedFeature().get())) {
            return "metaSourced"; // the L-029 gm coercion — never claimable neutrally
        }
        if (!isProvablyNonMetaElementSourceShape(source, true)) {
            // The arm's element-form allowlist, restated (the adapter's
            // isProvablyNonMetaElementSource — the first #479 ring's GetIsin class; since #481
            // the WIDENED form: a grammar-elided PIPED source resolves to the then's argument,
            // matching the live arm — a still-classified elided source is exactly the
            // unresolvable/unprovable residue, so today's tokens are byte-stable):
            // a source whose element form is not PROVABLY plain (a meta-valued list
            // literal, an alias, a then-pipe, a collapse …) declines at the arm.
            return "unprovableSource";
        }
        // #481: the type facts read the RESOLVED source — the arm's own channel (an elided
        // node is cache-invisible; for every non-elided source the resolver is the identity).
        RExpression resolvedSource = resolveElidedPipedSourceShape(source);
        RMetaAnnotatedType sourceType = resolvedSource == null
                ? null : workspace.getInferredType(resolvedSource);
        if (sourceType == null || sourceType.isMissing()) {
            return "retypeSourceMissing";
        }
        if (sourceType.hasMeta()) {
            return "retypeSourceMeta";
        }
        return "retypeSourceOk"; // THE claimable slice for a retype arm
    }

    /**
     * #480: the AST shape of a binding source OUTSIDE the #479 element-form allowlist — the
     * {@code unprovableSource} sub-decode token (the widening map's key). Named shapes for the
     * classes the allowlist deliberately declines (a then-pipe, the grammar-elided piped implicit,
     * an alias head, a list-op collapse, a conditional, an unresolved feature/symbol …); the
     * allowlist-recursing shapes ({@code listLiteral} / {@code filter}) name their FIRST failing
     * inner shape dot-suffixed; everything else falls back to {@code other:<Class>}. Since #484
     * the {@code symbol:RFunction} face refines IN PLACE to {@code symbol:RFunction.<facet>}
     * ({@link #calleeOutputFacet} — the PLANNED callable-output arm's admission restated at the
     * token's own seat, so every composing channel carries the facets at once); since #485 the
     * {@code elidedImplicit} face refines the same way to {@code elidedImplicit.<facet>}
     * ({@link #elidedImplicitStopFacet} — the resolver's stopping face, the {@code noBinder}
     * stop carrying the PLANNED rule-input arm's admission); since #487 the {@code conditional}
     * face refines the same way to {@code conditional.<facet>}
     * ({@link #conditionalJoinShapeFacet} — the PLANNED JOIN-law arm's admission, the three
     * pipe seats intercepting with the carrier-typed {@link #conditionalJoinFacet} variant).
     * Bounded like every witness walk.
     */
    private static String unprovableSourceShapeToken(RExpression source, int depth) {
        if (source == null) {
            return "nullSource";
        }
        if (depth >= 8) {
            return "deepNest";
        }
        if (source instanceof RThenExpr) {
            return "thenPipe";
        }
        if (source instanceof RImplicitVariable iv) {
            // #485: the piped/elided operand — the flat token refined IN PLACE by the
            // resolver's stopping face, the noBinder face carrying the PLANNED rule-input
            // arm's admission (Σ facets ≡ the flat count per channel BY CONSTRUCTION).
            return "elidedImplicit." + elidedImplicitStopFacet(iv, depth);
        }
        if (source instanceof RSymbolReference sym) {
            if (!sym.args().isEmpty()) {
                return "call";
            }
            RNode symbol = sym.symbol().orElse(null);
            if (symbol == null) {
                return "symbolUnresolved";
            }
            if (symbol instanceof RShortcut) {
                return "alias";
            }
            if (symbol instanceof RAttribute attr) {
                // a non-meta resolved attribute is allowlist-provable — only the meta face lands here
                return isMetaAnnotatedAttr(attr) ? "metaAttr" : "attrOther";
            }
            if (symbol instanceof RFunction calleeFn) {
                // #484: the bare-function-application decode — the PLANNED callable-output
                // arm's admission restated IN PLACE (the #482/#483 law): the flat
                // `symbol:RFunction` token refines to `symbol:RFunction.<facet>` at its own
                // seat, so EVERY channel composing this token (the extractPipe/thenArg pipe
                // seats, the listLiteral/filter recursions, the #480 source channels) carries
                // the facets at once and Σ facets ≡ each channel's flat count BY CONSTRUCTION
                // (RFunction is the concrete class — this instanceof branch and the
                // getSimpleName() spelling below pick out the same population).
                return "symbol:RFunction." + calleeOutputFacet(sym, calleeFn);
            }
            return "symbol:" + symbol.getClass().getSimpleName();
        }
        if (source instanceof RFeatureCall fc) {
            // a resolved non-meta last hop is provable; the meta face routes to metaSourced at the
            // direct seat — inside a recursing shape it lands here as metaFeature
            RAttribute feature = fc.resolvedFeature().orElse(null);
            if (feature == null) {
                return "featureUnresolved";
            }
            return isMetaAnnotatedAttr(feature) ? "metaFeature" : "fcOther";
        }
        if (source instanceof REnumValueRef evr) {
            if (evr.resolvedAttributeChain().isPresent()) {
                return evr.resolvedAttributeChain().get().feature() == null
                        ? "chainLeafNull" : "metaLeaf"; // a non-meta bound leaf is provable
            }
            return evr.resolvedInputFeature().isPresent() ? "metaLeaf" : "chainUnbound";
        }
        if (source instanceof RListLiteral list) {
            if (list.elements().isEmpty()) {
                return "listLiteral.empty";
            }
            for (RExpression element : list.elements()) {
                // #481 Seat-1 OBS-1: the WIDENED form — the first FAILING inner shape must be
                // judged by the live arm's own allowlist, else a resolvable-elided element
                // would be misnamed as the failing shape in a mixed list (zero live carriers
                // today — the pre/post readings prove it — but the token is the next
                // widening's sizing input, so the naming rides the arm's world).
                if (!isProvablyNonMetaElementSourceShape(element, true)) {
                    return "listLiteral." + unprovableSourceShapeToken(element, depth + 1);
                }
            }
            return "listLiteral.other"; // contract-impossible (all-provable lists are provable)
        }
        if (source instanceof RFilterExpr filter) {
            return "filter." + unprovableSourceShapeToken(filter.argument(), depth + 1);
        }
        if (source instanceof RListOpExpr listOp) {
            return "listOp." + (listOp.op() == null ? "null" : listOp.op().name());
        }
        if (source instanceof RConditionalExpr cond) {
            // #487: the conditional decode — the PLANNED JOIN-law arm's admission restated IN
            // PLACE (the #484/#485/#486 law): the flat `conditional` token refines to
            // `conditional.<facet>` at its own seat, so EVERY channel composing this token
            // (the pipe compositions, the `resolves.` stop-facet channel, the listLiteral/
            // filter recursions, the #480 source channels) carries the facets at once and
            // Σ facets ≡ each channel's flat count BY CONSTRUCTION. (The three pipe seats
            // intercept ABOVE their allowlist checks with the carrier-typed variant —
            // conditionalJoinFacet — so this static face serves the workspace-less contexts.)
            return "conditional." + conditionalJoinShapeFacet(cond, depth);
        }
        if (source instanceof RExtractExpr) {
            return "extractPipe";
        }
        if (source instanceof RConversionExpr) {
            return "conversion";
        }
        if (source instanceof RDeepFeatureCall) {
            return "deepFeature";
        }
        return "other:" + source.getClass().getSimpleName();
    }

    /**
     * #484: the callable-output facet — the PLANNED bare-function-application widening's
     * admission restated per occurrence (the #480 law: restate the planned arm in the witness
     * BEFORE building it; the arm is LIVE since the teach fold, so post-teach only the decline
     * faces reach this token). A bare (no-arg) reference to an {@link RFunction} in element-source
     * position is the L-109 point-free application: legacy renders it
     * {@code <callee>.evaluate(<binding>.get())}
     * ({@code ReferenceHandler.renderImplicitFunctionInvocation} — the applied value IS the
     * callee's OUTPUT, and per its contract "the invocation carries the function's output
     * type"), and the checker types the bare reference off {@code fn.output()}
     * ({@code ExpressionTypeComputer.inferTypeOfNode}'s RFunction leg) — so the element form
     * of an extract/pipe whose body is a bare function is the OUTPUT attribute's own: the
     * CALLABLE-OUTPUT identity (the value identity's function face — the element values are
     * the callee's outputs, NOT a navigation form). Facets, in the planned arm's exit order:
     * <ul>
     *   <li>{@code aliasShadow} — the reference name matches an enclosing function's shortcut
     *       (legacy's {@code isAliasReference} name-match fallback: the bare name renders as
     *       an ALIAS invocation, not a function call — the admission mirrors legacy's
     *       PRECEDENCE, the #480 MF-1 class). The restated gate is the adapter's standing
     *       {@code collidesWithShortcut} form — coarser than legacy's fallback (no #453
     *       self-shortcut exemption), so it declines a SUPERSET: the safe direction, the rare
     *       self-name face staying sized under this facet;</li>
     *   <li>{@code outputMissing} — no output attribute (defensive; the grammar requires
     *       one);</li>
     *   <li>{@code outputMeta} — a {@code [metadata …]}-annotated output: the applied value
     *       carries meta, the allowlist's non-meta gate fails (DECLINED — stays
     *       post-teach);</li>
     *   <li>{@code outputData} / {@code outputDataMulti} — a non-meta output with a declared
     *       {@link RDataType}: the FULL claim face (the allowlist proves non-meta AND the
     *       structural derivation proves the element data type — the output attribute's
     *       declared type, cardinality-agnostic like the bare-attribute leg: a MULTI output's
     *       applied values are the output's own elements, the #483 route-grid flatten faces;
     *       the multi face sized separately for the disclosure);</li>
     *   <li>{@code outputNonData} / {@code outputNonDataMulti} — a non-meta output WITHOUT a
     *       declared data type (a basic/enum/unresolved-typed output): the allowlist ADMITS
     *       (the non-meta proof) but the derivation seats decline (no {@link RDataType}) —
     *       the SPLIT face, sized separately.</li>
     * </ul>
     * Post-teach the admit faces ({@code outputData*} / {@code outputNonData*}) read ZERO on
     * every channel (an admitted occurrence proves BEFORE reaching the residue token) and the
     * decline faces stay byte-stable — the conservation signature.
     */
    private static String calleeOutputFacet(RSymbolReference ref, RFunction callee) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(ref);
        if (enclosing != null && nameMatchesShortcutW(enclosing, ref.name())) {
            return "aliasShadow";
        }
        RAttribute out = callee.output().orElse(null);
        if (out == null) {
            return "outputMissing";
        }
        if (isMetaAnnotatedAttr(out)) {
            return "outputMeta";
        }
        boolean multi = isMultiCardinalityShape(out);
        if (declaredDataTypeOf(out) != null) {
            return multi ? "outputDataMulti" : "outputData";
        }
        return multi ? "outputNonDataMulti" : "outputNonData";
    }

    /**
     * #484: restates the adapter's {@code isMultiCardinality} — declared multi-cardinality
     * (unbounded {@code *} or an upper bound &gt; 1; an absent cardinality is the implicit
     * {@code (1..1)} ⇒ single).
     */
    private static boolean isMultiCardinalityShape(RAttribute attr) {
        return attr.cardinality()
                .map(c -> c.isUnbounded()
                        || (c.sup() != null && c.sup().compareTo(java.math.BigInteger.ONE) > 0))
                .orElse(false);
    }

    /**
     * #485: the rule-top implicit facet — the PLANNED rule-input widening's admission restated
     * per occurrence (the #480 law: restate the planned arm in the witness BEFORE building it;
     * the arm is LIVE since the teach fold, so post-teach only the decline faces reach this
     * token). A synthetic elided implicit whose binder walk found NEITHER a lambda NOR a switch (the
     * {@code noBinder} face — both facet seats establish that before calling here) takes the
     * enclosing RULE's INPUT value: legacy {@code ReferenceHandler.handle(RImplicitVariable)}'s
     * {@code isElidedOperandTopLevel} route renders {@code MapperS.of(input)} — the
     * {@code RFunction.fromRule}-synthesized input parameter, named literally {@code "input"}
     * and typed by the rule's {@code from} type — and the checker types the same face off the
     * rule's from-type ({@code TypeInferenceEngine.computeImplicitItemType}'s top-level branch
     * → {@code inferRuleFromType}, always {@code withNoMeta}): THE RULE-INPUT identity — the
     * element form is the rule's declared from-type, a DECLARATION read (no cached-type
     * channel, like the #484 output-attribute read). The ladder mirrors legacy's
     * {@code isElidedOperandTopLevel} admission gate-for-gate. Facets, in the planned arm's
     * exit order:
     * <ul>
     *   <li>{@code parentNotElidedOp} — the implicit is not the ARGUMENT of one of legacy's
     *       ten without-left ops ({@code isElidedOperandTopLevel}'s slot gate; a parsed
     *       synthetic implicit always sits in that slot — AstBuilder only mints them there —
     *       so a nonzero read is a drift detector);</li>
     *   <li>{@code lambdaAbove} / {@code switchAbove} — the walk crosses a lambda/switch
     *       (contract-impossible here: both seats mint {@code noBinder} only after the
     *       binder/switch walks returned empty — drift detectors, the witness-as-detector
     *       law);</li>
     *   <li>{@code functionTop} — the walk hits an {@link RFunction} before any rule: legacy's
     *       gate declines (a function body's input is not named {@code input} — the
     *       {@code isElidedOperandTopLevel} javadoc's own scoping), DECLINED whole;</li>
     *   <li>{@code conditionTop} — the walk hits an {@link RCondition} (the data-type
     *       condition-instance face: legacy renders {@code MapperS.of(<instance>)} — a
     *       DIFFERENT value identity, its own future widening), DECLINED here;</li>
     *   <li>{@code noRoot} — the walk exhausts without any recognized root (defensive);</li>
     *   <li>{@code fromMissing} / {@code fromUnresolved} — the rule carries no {@code from}
     *       type / its type reference did not resolve (the report-scoped and unresolved
     *       faces the charter predicted — DECLINED, prove-or-decline);</li>
     *   <li>{@code fromNonData} — the from-type resolves to a non-{@link RDataType} (an
     *       enum/basic/choice/alias face: legacy still renders {@code input}, but the
     *       derivation seats cannot prove an element DATA type — the #484
     *       {@code outputNonData} split face's rule-seat twin), DECLINED;</li>
     *   <li>{@code fromData} — the from-type resolves to an {@link RDataType}: the FULL claim
     *       face (the allowlist proves non-meta — the fromRule-synthesized input carries no
     *       annotations and the checker's read is {@code withNoMeta} by construction — and
     *       the derivation proves the element data type = the from-type, the same
     *       declared-typeCall channel).</li>
     * </ul>
     * Post-teach the {@code fromData} face reads ZERO on every channel (an admitted occurrence
     * proves before reaching the residue token) and the decline faces stay byte-stable — the
     * conservation signature.
     */
    private static String ruleTopImplicitFacet(RImplicitVariable iv) {
        if (!isElidedOperandArgumentSlot(iv)) {
            return "parentNotElidedOp";
        }
        RNode cur = iv.parent().parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return "lambdaAbove";
            }
            if (cur instanceof RSwitchExpr) {
                return "switchAbove";
            }
            if (cur instanceof RFunction) {
                return "functionTop";
            }
            if (cur instanceof RCondition) {
                return "conditionTop";
            }
            if (cur instanceof RRule rule) {
                if (rule.fromType().isEmpty()) {
                    return "fromMissing";
                }
                RNode ref = rule.fromType().get().referencedType().orElse(null);
                if (ref == null) {
                    return "fromUnresolved";
                }
                return ref instanceof RDataType ? "fromData" : "fromNonData";
            }
            cur = cur.parent();
        }
        return "noRoot";
    }

    /**
     * #487: the conditional JOIN decode — the PLANNED {@link RConditionalExpr} widening's
     * admission restated per occurrence (the #480–#485 law: restate the planned arm in the
     * witness BEFORE building it). The element form of {@code if c then A else B} is the arms'
     * COMMON form: the runtime elements are the EXECUTED arm's (legacy
     * {@code ControlFlowHandler.handle(RConditionalExpr)} — every route, the hoisted locals and
     * the inline ternary alike, yields ONE arm's value), the checker types the node
     * {@code withNoMeta(typeJoin.join(thenT, elseT))} ({@code ExpressionTypeComputer
     * .computeConditional}; an absent/empty else types {@code NOTHING} — the join's bottom — so
     * the empty-else face types as the THEN arm alone), and legacy's item machinery reads that
     * SAME cache, so each arm's own non-meta proof is the ONLY meta protection (the
     * {@code withNoMeta} wrap strips an arm's meta from the cache channel — the L-029 law
     * demands the per-arm allowlist proofs). Faces, in the planned arm's exit order:
     * <ul>
     *   <li>{@code noThen} — no then branch (defensive; the grammar requires it);</li>
     *   <li>{@code thenUnprovable.<shape>} / {@code elseUnprovable.<shape>} — an arm fails the
     *       planned allowlist ({@link #unprovableSourceShapeToken} names the failing shape —
     *       the residue map; a nested conditional arm names its own {@code conditional.<facet>}
     *       recursively), DECLINED whole (prove-or-decline);</li>
     *   <li>{@code elseEmpty[.formGap]} — no genuine else ({@link #hasGenuineElseShape}: the
     *       {@code DefaultElseRule} synthetic empty list and a user-written {@code else empty}
     *       read identically): the then arm alone carries the form — ADMITTED under the
     *       shape law; {@code .formGap} marks the then form underivable as a DATA type (the
     *       derivation channel's reach — basic-typed arms and mixed inners derive null,
     *       exactly the list-literal precedent);</li>
     *   <li>{@code sameType} — both arms derive the SAME {@link RDataType} (instance identity —
     *       the list-literal all-IDENTICAL law): the FULL claim face, the trivial join;</li>
     *   <li>{@code formGap} — both arms prove the allowlist but at least one derives no single
     *       data type (basic-typed arms, mixed-inner lists …): retype-channel claimable (the
     *       cached join), nav-derivation-channel not;</li>
     *   <li>{@code typeDiffers.thenSuper} / {@code .elseSuper} — the arms derive DIFFERENT data
     *       types, one the other's ancestor ({@code TypeJoin}'s subtype shortcut: the join is
     *       the supertype);</li>
     *   <li>{@code typeDiffers.commonAncestor} / {@code .disjoint} — distinct types with/without
     *       a common ancestor ({@code TypeJoin.joinDataTypes}'s walk: the join is the first
     *       common ancestor, or {@code ANY}). The {@code typeDiffers.*} faces size a future
     *       join-widening; the planned #487 arm's derivation declines them (the list-literal
     *       mixed-decline law).</li>
     * </ul>
     */
    private static String conditionalJoinShapeFacet(RConditionalExpr cond, int depth) {
        RExpression thenB = cond.thenBranch();
        if (thenB == null) {
            return "noThen";
        }
        if (!isProvablyNonMetaElementSourceShape(thenB, true)) {
            return "thenUnprovable." + unprovableSourceShapeToken(thenB, depth + 1);
        }
        boolean genuineElse = hasGenuineElseShape(cond);
        RExpression elseB = genuineElse ? cond.elseBranch().orElse(null) : null;
        if (genuineElse && !isProvablyNonMetaElementSourceShape(elseB, true)) {
            return "elseUnprovable." + unprovableSourceShapeToken(elseB, depth + 1);
        }
        RDataType thenT = sourceElementDataTypeShape(thenB, depth + 1);
        if (!genuineElse) {
            return thenT == null ? "elseEmpty.formGap" : "elseEmpty";
        }
        RDataType elseT = sourceElementDataTypeShape(elseB, depth + 1);
        if (thenT == null || elseT == null) {
            return "formGap";
        }
        if (thenT == elseT) {
            return "sameType";
        }
        if (isAncestorDataTypeShape(elseT, thenT)) {
            return "typeDiffers.elseSuper";
        }
        if (isAncestorDataTypeShape(thenT, elseT)) {
            return "typeDiffers.thenSuper";
        }
        return haveCommonDataAncestorShape(thenT, elseT)
                ? "typeDiffers.commonAncestor" : "typeDiffers.disjoint";
    }

    /**
     * #487: the carrier-typed variant for the three PIPE seats (the #483 terminal-triple
     * pattern): the shape face ({@link #conditionalJoinShapeFacet}) composed with the SEAT's
     * resolved-source cached-type verdict on every allowlist-ADMITTED face — the retype
     * channel's own gate ({@code typeOk} = the claimable read; the conditional's checker type
     * is {@code withNoMeta} by construction, so {@code typeMeta} on the FLAT seat is a drift
     * detector; the nested seats read the then/extract carrier, the #482/#483 channels). The
     * decline faces carry no verdict — the arm never reads a type it will not use.
     */
    private String conditionalJoinFacet(RConditionalExpr cond, RExpression typeCarrier,
            RWorkspace workspace) {
        String face = conditionalJoinShapeFacet(cond, 0);
        if (face.startsWith("noThen") || face.startsWith("thenUnprovable.")
                || face.startsWith("elseUnprovable.")) {
            return face;
        }
        RMetaAnnotatedType carrierType = workspace.getInferredType(typeCarrier);
        if (carrierType == null || carrierType.isMissing()) {
            return face + ".typeMissing";
        }
        if (carrierType.hasMeta()) {
            return face + ".typeMeta";
        }
        return face + ".typeOk";
    }

    /**
     * #487: whether {@code candidate} sits on {@code start}'s supertype chain (inclusive) —
     * instance identity per hop, mirroring {@code TypeJoin.ancestorChain}'s
     * {@code IdentityHashMap} walk; bounded like every witness walk.
     */
    private static boolean isAncestorDataTypeShape(RDataType candidate, RDataType start) {
        RDataType cur = start;
        int guard = 0;
        while (cur != null && guard++ < PARENT_WALK_LIMIT) {
            if (cur == candidate) {
                return true;
            }
            cur = cur.superType().orElse(null);
        }
        return false;
    }

    /** #487: whether the two types share ANY common ancestor ({@code TypeJoin.joinDataTypes}'s question). */
    private static boolean haveCommonDataAncestorShape(RDataType a, RDataType b) {
        RDataType cur = a;
        int guard = 0;
        while (cur != null && guard++ < PARENT_WALK_LIMIT) {
            if (isAncestorDataTypeShape(cur, b)) {
                return true;
            }
            cur = cur.superType().orElse(null);
        }
        return false;
    }

    /**
     * #485: restates legacy {@code ReferenceHandler.isElidedOperandTopLevel}'s slot gate — the
     * implicit must be the ARGUMENT of one of the ten without-left ops (the elided-operand
     * synthesis seats; AstBuilder mints synthetic implicits only there, so the gate is a
     * legacy-precedence restatement + drift detector, never a live filter).
     */
    private static boolean isElidedOperandArgumentSlot(RImplicitVariable iv) {
        RNode parent = iv.parent();
        return (parent instanceof RListOpExpr op && op.argument() == iv)
                || (parent instanceof RConversionExpr conv && conv.argument() == iv)
                || (parent instanceof RToStringExpr ts && ts.argument() == iv)
                || (parent instanceof RExtractExpr ext && ext.argument() == iv)
                || (parent instanceof RFilterExpr filt && filt.argument() == iv)
                || (parent instanceof RCountExpr count && count.argument() == iv)
                || (parent instanceof RSortExpr sort && sort.argument() == iv)
                || (parent instanceof RMinExpr min && min.argument() == iv)
                || (parent instanceof RMaxExpr max && max.argument() == iv)
                || (parent instanceof RReduceExpr reduce && reduce.argument() == iv);
    }

    /**
     * #485: the rule-input arm's admission (the adapter's
     * {@code provableRuleInputElementType}, restated) — the element data type of a
     * TRUE-noBinder rule-top elided implicit, or {@code null} outside the claim slice
     * (prove-or-decline). Non-null EXACTLY when {@link #ruleTopImplicitFacet} reads
     * {@code fromData}: the same gates in the same order (synthetic + the
     * {@code isElidedOperandTopLevel} slot gate + a clean walk to the {@link RRule} root
     * crossing no lambda/switch/function/condition + the {@code from} type resolving to an
     * {@link RDataType}). The type is the rule's declared from-type — a DECLARATION read (the
     * checker's own channel: {@code inferRuleFromType} wraps it {@code withNoMeta}), no
     * cached-type read, no recursion — the termination measure untouched (the #484 pattern).
     */
    private static RDataType ruleTopInputElementTypeShape(RImplicitVariable iv) {
        if (!iv.isSynthetic() || !isElidedOperandArgumentSlot(iv)) {
            return null;
        }
        RNode cur = iv.parent().parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction || cur instanceof RSwitchExpr
                    || cur instanceof RFunction || cur instanceof RCondition) {
                return null;
            }
            if (cur instanceof RRule rule) {
                RNode ref = rule.fromType().flatMap(tc -> tc.referencedType()).orElse(null);
                return ref instanceof RDataType data ? data : null;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * #485: the stop-face decode for an elided implicit reached as a RECURSING shape's inner
     * (the {@code filter.}/{@code listLiteral.} legs of {@link #unprovableSourceShapeToken}) —
     * the flat {@code elidedImplicit} token refined IN PLACE at its own seat (the #482/#483/#484
     * law), so every channel composing it (the flat {@code filter.elidedImplicit} pipe face,
     * the nested {@code thenPipe.unprovable.filter.elidedImplicit} face, the #480 source
     * channels' {@code unprovableSource.elidedImplicit} flats) carries the facets at once and
     * Σ facets ≡ each channel's flat count BY CONSTRUCTION. The ladder re-derives the
     * resolver's ({@link #resolveElidedPipedSourceShape}) stopping face in its own gate order —
     * pure walks, so the re-test reproduces the stop deterministically (the
     * {@link #elidedPipeFacetToken} unresolved branch's exact ladder) — with the
     * {@code noBinder} face refined by the PLANNED rule-input arm's admission
     * ({@link #ruleTopImplicitFacet}); a source that RESOLVES to a non-implicit but still
     * failed the allowlist names the resolved shape ({@code resolves.<shape>} — the composing
     * residue).
     */
    private static String elidedImplicitStopFacet(RImplicitVariable elided, int depth) {
        RExpression resolved = resolveElidedPipedSourceShape(elided);
        if (resolved == null) {
            return "nullSource";
        }
        if (!(resolved instanceof RImplicitVariable stop)) {
            return "resolves." + unprovableSourceShapeToken(resolved, depth + 1);
        }
        if (!stop.isSynthetic()) {
            return "literalItem";
        }
        if (nearestSwitchBeforeLambda(stop) != null) {
            return "switchBoundary";
        }
        RInlineFunction binder = nearestInlineFn(stop);
        if (binder == null) {
            return "noBinder." + ruleTopImplicitFacet(stop);
        }
        if (!(binder.isImplicit() || binder.paramNames().isEmpty())) {
            return "namedBinder";
        }
        RNode parent = binder.parent();
        if (!(parent instanceof RThenExpr then) || then.body().orElse(null) != binder) {
            return "nonThenBinder."
                    + (parent == null ? "null" : parent.getClass().getSimpleName());
        }
        return "deepPipe";
    }

    /** #480: record one source-decode observation (bucket + first sample). */
    private void recordImplicitRootSource(String bucket, String sample) {
        implicitRootSourceBuckets.merge(bucket, 1, Integer::sum);
        implicitRootSourceFirstSamples.putIfAbsent(bucket, sample);
    }

    /**
     * #480: decode a still-declining equivalent's synthetic implicit BASE — walk the equivalent's
     * receiver spine to its base; when it is a synthetic implicit, classify its binding context
     * and (for the filter/extract context) the retype-source facet with {@code unprovableSource}
     * expanded by shape. {@code <channel>:retypeSourceOk} is contract-impossible (a
     * retype-admissible base would have lowered the equivalent) — a nonzero read is a drift
     * detector. A non-implicit base (the {@code ruleInputTop} synthetic {@code input} root) is
     * outside this decode's population and records nothing.
     */
    private void recordEquivalentBaseDecode(String channel, RFeatureCall equivalent, String sample,
            RWorkspace workspace) {
        RExpression base = equivalent.receiver();
        int guard = 0;
        while (base instanceof RFeatureCall fc && guard++ < 16) {
            base = fc.receiver();
        }
        if (!(base instanceof RImplicitVariable iv) || !iv.isSynthetic()) {
            return;
        }
        String facet;
        RExpression source = filterExtractBindingSourceOf(iv);
        if (source == null) {
            facet = "binder." + syntheticItemContext(iv);
        } else {
            facet = retypeSourceFacet(source, workspace);
            if ("unprovableSource".equals(facet)) {
                facet = "unprovableSource." + unprovableSourceShapeToken(source, 0);
                if (source instanceof RImplicitVariable elided) {
                    // #481: the elided-PIPE decode (see the synItem seat — the same facet
                    // channel; Σ over a channel's pipe facets ≡ its flat elidedImplicit count).
                    recordElidedPipeDecode(channel, elided, sample, workspace);
                }
            }
        }
        recordImplicitRootSource(channel + ":" + facet, sample);
    }

    /** #481: record one elided-pipe decode observation (bucket + first sample). */
    private void recordElidedPipeDecode(String channel, RImplicitVariable elided, String sample,
            RWorkspace workspace) {
        String bucket = "pipe:" + channel + ":" + elidedPipeFacetToken(elided, workspace);
        elidedPipeBuckets.merge(bucket, 1, Integer::sum);
        elidedPipeFirstSamples.putIfAbsent(bucket, sample);
    }

    /**
     * #481: the elided-PIPE facet of an {@code elidedImplicit} binding source — the PLANNED
     * widening's admission restated in the resolver's own exit order (the #480 law: restate the
     * planned arm in the witness BEFORE building it). The grammar-elided piped implicit takes the
     * value of the enclosing THEN's ARGUMENT — legacy {@code handle(RImplicitVariable)}'s
     * thenBody face serves the pipe input's value through BOTH render channels (the hoisted
     * {@code thenArg} local and the runtime lambda binding — the render decision stays legacy's
     * at emit time; only the ELEMENT FORM recurses, and it is channel-independent by the value
     * identity) — so the planned resolver recurses to that argument; every other face stays
     * legacy's, prove-or-decline. Faces, in exit order:
     * <ul>
     *   <li>{@code literalItem} — a non-synthetic {@code item} keyword source: the lambda's own
     *       binding, not the pipe class;</li>
     *   <li>{@code switchBoundary} — a switch between the implicit and its lambda: legacy's
     *       thenArg arm walks PAST switches when its scope binding is live, and only the
     *       binding-DEAD face reaches the #221 case-narrowed subject machinery — the liveness
     *       is unknowable statically, so the face declines whole (the safe direction; the #479
     *       retype's own conservatism, restated — the Seat-1 #481 OBS-2 precision);</li>
     *   <li>{@code noBinder.<facet>} — no enclosing inline fn (the rule-input /
     *       condition-instance top-level faces); since #485 refined IN PLACE by the PLANNED
     *       rule-input arm's admission ({@link #ruleTopImplicitFacet} — the elided implicit at
     *       RULE top level takes the RULE INPUT's value, legacy's
     *       {@code isElidedOperandTopLevel} → {@code MapperS.of(input)} route: the value
     *       identity at the rule seat; {@code fromData} = THE claim face);</li>
     *   <li>{@code namedBinder} — an explicit-param lambda (the #357/#364/#367/#375 walk-out
     *       class — scope-live re-roots the structural walk cannot prove);</li>
     *   <li>{@code nonThenBinder.<Construct>} — an implicit binder that is not a THEN body (the
     *       filter/extract item piggy-back class — a future widening, sized here);</li>
     *   <li>{@code deepPipe} — the hop guard (defensive; a pipe chain strictly ascends, so the
     *       walk is finite — the guard mirrors the shape token's depth bound);</li>
     *   <li>{@code thenArg.provable.typeOk} / {@code .typeMissing} / {@code .typeMeta} — the
     *       resolved pipe argument ({@link #resolveElidedPipedSourceShape}) proves under the
     *       WIDENED element-form allowlist (the 2-arg
     *       {@link #isProvablyNonMetaElementSourceShape(RExpression, boolean)} — the planned
     *       arm's own predicate, nested elided sources resolving recursively) with the argument's
     *       own cached-type facts read ({@code typeOk} = THE claimable slice; post-teach
     *       ZERO);</li>
     *   <li>{@code thenArg.unprovable.<shape>} — the resolved argument's own AST shape
     *       ({@link #unprovableSourceShapeToken}) — the residue map: WHICH pipe arguments stay
     *       unprovable, the next widening's sizing. Since #482 the dominant {@code thenPipe}
     *       shape refines IN PLACE to {@code thenPipe.<facet>}
     *       ({@link #thenPipeBodyFacet} — the PLANNED RThenExpr arm's admission restated; Σ over
     *       the {@code thenPipe.*} facets ≡ the #481 flat {@code thenPipe} count by construction,
     *       the same seat dot-suffixed); since #483 the dominant flat {@code extractPipe} shape
     *       refines the same way to {@code extractPipe.<facet>} ({@link #extractPipeBodyFacet} —
     *       the PLANNED RExtractExpr arm's admission, the type carrier the extract itself);
     *       since #487 the flat {@code conditional} shape refines the same way to
     *       {@code conditional.<facet>} ({@link #conditionalJoinFacet} — the PLANNED JOIN-law
     *       arm's admission with the carrier verdicts composed, the type carrier the
     *       conditional itself).</li>
     * </ul>
     */
    private String elidedPipeFacetToken(RImplicitVariable elided, RWorkspace workspace) {
        RExpression resolved = resolveElidedPipedSourceShape(elided);
        if (resolved == null) {
            return "thenArg.unprovable.nullSource";
        }
        if (resolved instanceof RImplicitVariable iv) {
            // Unresolved — re-derive the stopping face by the resolver's own gate order (the
            // gates are pure walks, so the re-test reproduces the stop deterministically).
            if (!iv.isSynthetic()) {
                return iv == elided ? "literalItem" : "thenArg.unprovable.literalItem";
            }
            if (nearestSwitchBeforeLambda(iv) != null) {
                return "switchBoundary";
            }
            RInlineFunction binder = nearestInlineFn(iv);
            if (binder == null) {
                // #485: the rule-input face — the flat noBinder token refined IN PLACE by the
                // PLANNED rule-input arm's admission (the #482/#483/#484 in-place law).
                return "noBinder." + ruleTopImplicitFacet(iv);
            }
            if (!(binder.isImplicit() || binder.paramNames().isEmpty())) {
                return "namedBinder";
            }
            RNode parent = binder.parent();
            if (!(parent instanceof RThenExpr then) || then.body().orElse(null) != binder) {
                return "nonThenBinder."
                        + (parent == null ? "null" : parent.getClass().getSimpleName());
            }
            return "deepPipe"; // every gate passes — only the hop guard stops a then-bound synthetic
        }
        if (resolved instanceof RThenExpr then) {
            // #482: the nested-thenPipe sub-decode — the resolved pipe argument is ITSELF a
            // pipe (the #481 residue map's dominant class); the PLANNED RThenExpr arm's
            // admission restates per occurrence BEFORE the arm exists (the #480/#481 law),
            // refining the #481 flat `thenArg.unprovable.thenPipe` token in place. The branch
            // sits ABOVE the live-allowlist check so the #481 verdict tokens keep their exact
            // meaning (a then never proves under the live 2-arg form — hoisting is
            // behavior-preserving for every other shape).
            return "thenArg.unprovable.thenPipe." + thenPipeBodyFacet(then, workspace);
        }
        if (resolved instanceof RExtractExpr extract) {
            // #483: the extract-bodied pipe sub-decode — the resolved pipe argument is ITSELF
            // an EXTRACT (the #482 residue map's dominant flat face, 753 at population); the
            // PLANNED RExtractExpr arm's admission restates per occurrence BEFORE the arm
            // exists (the #480/#481/#482 law), refining the #481 flat
            // `thenArg.unprovable.extractPipe` token in place. The branch sits ABOVE the
            // live-allowlist check so the standing verdicts keep their exact meaning (an
            // extract never proves under the live form — hoisting is behavior-preserving for
            // every other shape). The type carrier is the extract itself — the claim seat's
            // resolved source.
            return "thenArg.unprovable.extractPipe." + extractPipeBodyFacet(extract, extract, workspace);
        }
        if (resolved instanceof RConditionalExpr cond) {
            // #487: the conditional-bodied pipe sub-decode — the resolved pipe argument is a
            // CONDITIONAL (the residue map's direct face); the PLANNED JOIN-law arm's
            // admission restates per occurrence BEFORE the arm exists, refining the flat
            // `thenArg.unprovable.conditional` token IN PLACE with the carrier verdicts
            // composed (the #483 terminal-triple pattern — the branch sits ABOVE the
            // live-allowlist check, which a conditional never passes today, so it intercepts
            // exactly the standing population). The type carrier is the conditional itself —
            // the claim seat's resolved source, a parsed cache-visible node.
            return "thenArg.unprovable.conditional." + conditionalJoinFacet(cond, cond, workspace);
        }
        if (!isProvablyNonMetaElementSourceShape(resolved, true)) {
            // #484: the callable-output arm rides the live widened form (the teach fold — a
            // bare-F argument's claimable slice pre-read provable.typeOk under the planned tier).
            // #485: the rule-input arm rides the same fold — the filter/pipe compositions over
            // the rule input pre-read provable.* under the planned tier, claimed live now.
            return "thenArg.unprovable." + unprovableSourceShapeToken(resolved, 0);
        }
        RMetaAnnotatedType argType = workspace.getInferredType(resolved);
        if (argType == null || argType.isMissing()) {
            return "thenArg.provable.typeMissing";
        }
        if (argType.hasMeta()) {
            return "thenArg.provable.typeMeta";
        }
        return "thenArg.provable.typeOk";
    }

    /**
     * #482: the nested-thenPipe BODY facet — the PLANNED {@link RThenExpr} widening's admission
     * restated per pipe occurrence (the #480 law: restate the planned arm in the witness BEFORE
     * building it). The element form of {@code A then B} is B's BODY result — the pipe's value
     * IS the body's value on the piped input (the #481 value identity one structural seat
     * deeper), so the arm proves the BODY expression under the widened
     * allowlist ({@link #isProvablyNonMetaElementSourceShape(RExpression, boolean)} — at
     * commit 2 the PLANNED 3-arg tier, folded into the live form at the commit-3 teach)
     * and reads the type facts on the THEN node itself — a parsed node, cache-visible (the
     * resolver's argument hop already lands here; the arm needs no new type channel). Facets,
     * in the arm's exit order:
     * <ul>
     *   <li>{@code noBody} — a bodyless then (defensive; the grammar always builds the body
     *       lambda);</li>
     *   <li>{@code unprovable.<shape>} — the body's element form stays unprovable under the
     *       planned allowlist ({@link #unprovableSourceShapeToken} on the body — the residue
     *       map, the NEXT widening's sizing; a body that is itself a then names
     *       {@code thenPipe}: its OWN body failed under the planned recursion, the depth-2
     *       residue; since #483 an EXTRACT body refines in place to
     *       {@code extractPipe.<facet>} ({@link #extractPipeBodyFacet} — the branch above this
     *       check, the type carrier staying the then node);</li>
     *   <li>{@code provable.typeOk} / {@code .typeMissing} / {@code .typeMeta} — the body
     *       proves and the then node's own cached-type facts read ({@code typeOk} = THE
     *       claimable slice; post-teach ZERO).</li>
     * </ul>
     */
    private String thenPipeBodyFacet(RThenExpr then, RWorkspace workspace) {
        RExpression body = then.body().map(RInlineFunction::body).orElse(null);
        if (body == null) {
            return "noBody";
        }
        if (body instanceof RExtractExpr extract) {
            // #483: the nested extract-bodied face — the then's body is an EXTRACT (the #482
            // residue map's dominant class, 1,435 at population); the flat
            // `unprovable.extractPipe` token refines in place per the planned arm's admission
            // (the branch sits ABOVE the allowlist check — an extract never proves under the
            // live form, so it intercepts exactly the standing population). The type carrier
            // stays the THEN node — the claim seat reads the resolved source's type, and for
            // the nested face that is the enclosing then (the #482 channel).
            return "unprovable.extractPipe." + extractPipeBodyFacet(extract, then, workspace);
        }
        if (body instanceof RConditionalExpr cond) {
            // #487: the conditional-bodied then face — the then's body is a CONDITIONAL; the
            // flat `unprovable.conditional` token refines in place with the carrier verdicts
            // composed (the type carrier stays the THEN node — the #482 channel's claim-seat
            // read; the branch sits above the allowlist check it never passes today).
            return "unprovable.conditional." + conditionalJoinFacet(cond, then, workspace);
        }
        if (!isProvablyNonMetaElementSourceShape(body, true)) {
            // #484: the callable-output arm rides the live widened form (the teach fold).
            // #485: the rule-input arm rides the same fold through the body descent.
            return "unprovable." + unprovableSourceShapeToken(body, 0);
        }
        RMetaAnnotatedType thenType = workspace.getInferredType(then);
        if (thenType == null || thenType.isMissing()) {
            return "provable.typeMissing";
        }
        if (thenType.hasMeta()) {
            return "provable.typeMeta";
        }
        return "provable.typeOk";
    }

    /**
     * #483: the extract-bodied pipe BODY facet — the PLANNED {@link RExtractExpr} widening's
     * admission restated per occurrence (the #480 law: restate the planned arm in the witness
     * BEFORE building it). The element form of {@code A extract B} is B's OWN result per
     * element — the extract re-shapes each piped element to its body's value (legacy
     * {@code CollectionHandler.handle(RExtractExpr)}: EVERY receiver/body-cardinality map
     * route — the single/item routes and the LoL routes alike — produces result elements
     * that ARE the body's values, the MULTI/LoL-body routes flattening to the body's own;
     * the switch-block lambda routes are unreachable under the arm (a switch body never
     * proves); the checker types the extract off its BODY —
     * {@code ExpressionTypeComputer}'s {@code RExtractExpr} case), so the arm proves the BODY
     * expression under the widened allowlist
     * ({@link #isProvablyNonMetaElementSourceShape(RExpression, boolean)} — at the witness
     * commit the PLANNED 3-arg tier, folded into the live form at the teach) and
     * reads the type facts on the SEAT's resolved source — the {@code typeCarrier}: the
     * extract node itself at the flat seat ({@link #elidedPipeFacetToken}), the enclosing THEN
     * node at the nested seat ({@link #thenPipeBodyFacet}) — the claim seat's own read, both
     * parsed cache-visible nodes. Facets, in the arm's exit order:
     * <ul>
     *   <li>{@code noBody} — a bodyless extract (defensive; the grammar requires the body
     *       lambda);</li>
     *   <li>{@code namedBinder} — an explicit-param extract body (the #357/#364/#367/#375
     *       scope-live walk-out class — references inside the body can resolve through
     *       scope-live bindings the structural walk cannot see, the #389 self-shadow face
     *       included; DECLINED whole, the safe direction);</li>
     *   <li>{@code literalItem} — the body IS the written non-synthetic {@code item} (the
     *       identity extract: the element form is the ARGUMENT's own — an argument-recursion
     *       face, a FUTURE widening; sized here, declined by the body-descent arm);</li>
     *   <li>{@code unprovable.<shape>} — the body's element form stays unprovable under the
     *       planned allowlist ({@link #unprovableSourceShapeToken} on the body — the residue
     *       map, the NEXT widening's sizing);</li>
     *   <li>{@code provable.typeOk} / {@code .typeMissing} / {@code .typeMeta} — the body
     *       proves and the type carrier's own cached-type facts read ({@code typeOk} = THE
     *       claimable slice; post-teach ZERO).</li>
     * </ul>
     */
    private String extractPipeBodyFacet(RExtractExpr extract, RExpression typeCarrier,
            RWorkspace workspace) {
        RInlineFunction bodyFn = extract.body();
        if (bodyFn == null) {
            return "noBody";
        }
        if (!(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
            return "namedBinder";
        }
        RExpression body = bodyFn.body();
        if (body == null) {
            return "noBody";
        }
        if (body instanceof RImplicitVariable bodyItem && !bodyItem.isSynthetic()) {
            return "literalItem";
        }
        if (body instanceof RConditionalExpr cond) {
            // #487: the conditional-bodied extract face — THE dominant decoded pipe class
            // (the #483 residue map's conditional-bodied 435); the flat
            // `unprovable.conditional` token refines in place with the carrier verdicts
            // composed (the type carrier stays the SEAT's — the extract at the flat seat, the
            // enclosing then at the nested seat; the branch sits above the allowlist check it
            // never passes today).
            return "unprovable.conditional." + conditionalJoinFacet(cond, typeCarrier, workspace);
        }
        if (!isProvablyNonMetaElementSourceShape(body, true)) {
            // #484: the callable-output arm rides the live widened form (the teach fold).
            // #485: the rule-input arm rides the same fold through the body descent.
            return "unprovable." + unprovableSourceShapeToken(body, 0);
        }
        RMetaAnnotatedType carrierType = workspace.getInferredType(typeCarrier);
        if (carrierType == null || carrierType.isMissing()) {
            return "provable.typeMissing";
        }
        if (carrierType.hasMeta()) {
            return "provable.typeMeta";
        }
        return "provable.typeOk";
    }

    /**
     * #480: the planned BARE-ATTR arm's admission, restated per {@code bareAttr:itemNav} blocker in
     * the arm's own exit order (the #479 law — the witness's verdict channel IS the arm's channel;
     * post-teach {@code claims} must read ZERO). The legacy-precedence gates use legacy's OWN
     * helpers ({@code HandlerHelper}); the structural element derivation restates the arm's
     * planned {@code sourceElementDataType} channel; the equivalent's verdict is the
     * already-computed adapter take on the legacy-synthesized equivalent (no extra adapts — the
     * #471 OBS-4 cost class).
     */
    private String bareAttrArmVerdict(RSymbolReference ref, RAttribute attr, String equivalentVerdict,
            RWorkspace workspace) {
        // (1) the rule-input precedence (legacy's ladder renders the INPUT nav where it fires;
        //     contract-impossible in the itemNav shape bucket — the shape axis already proved the
        //     input synthesizer nulls; kept as a drift detector). #485: refined to the TOP-LEVEL
        //     slice only (the arm's own gate — the in-lambda slice proceeds through the source
        //     gates, legacy's receiver there being the synthetic implicit: the item ≡ input
        //     value identity).
        if (wouldSynthesizeRuleInputNav(ref, attr) && !hasEnclosingRuleLambda(ref)) {
            return "declines:ruleInputNav";
        }
        // (2) the shortcut name-match precedence (legacy's isAliasReference fallback renders an
        //     alias invocation — the adapter's standing collidesWithShortcut class)
        RFunction fn = HandlerHelper.findEnclosingFunction(ref);
        if (fn != null && nameMatchesShortcutW(fn, ref.name())) {
            return "declines:shortcutCollision";
        }
        // (3) the binder gates (the #479 retype's own: before-switch, implicit-or-paramless,
        //     filter/extract body)
        RExpression source = filterExtractSourceOfArmBinder(ref);
        if (source == null) {
            return "declines:noFilterExtractBinder";
        }
        // (4) the structural element-type derivation over the allowlist shapes
        RDataType elementType = sourceElementDataTypeShape(source, 0);
        if (elementType == null) {
            return "declines:sourceElementUnresolved";
        }
        // (5) the identity guard (legacy synthesizeImplicitItemNavigation's own)
        if (HandlerHelper.findAttributeOnDataType(elementType, attr.name()) != attr) {
            return "declines:identityGuard";
        }
        // (6) the equivalent's own gates (the delegation)
        if (!"lowers".equals(equivalentVerdict)) {
            return equivalentDeclineToken(equivalentVerdict);
        }
        // (6b) #499 — the arm's SHAPE gate restated (the adapter twin's equivalentMetaAccess
        // exit): a meta-annotated attribute's equivalent lowers through the meta arm as
        // IRMetaAccess, which the bare-attr arm's FieldAccess-over-synthetic-item contract
        // excludes — the bare-META-attr slice's own face, a named future lever.
        if (isMetaAnnotated(attr)) {
            return "declines:equivalentMetaAccess";
        }
        // (7) the raw node's cached type (the cache-boundary rebuild's channel)
        RMetaAnnotatedType navType = workspace.getInferredType(ref);
        if (navType == null || navType.isMissing()) {
            return "declines:navTypeMissing";
        }
        return "claims";
    }

    /**
     * #480: the planned CHAIN arm's admission, restated per {@code chainFall:itemChain} blocker in
     * the arm's own exit order (legacy {@code synthesizeImplicitItemChain}'s decline ladder + the
     * arm's narrower prove-or-decline gates). Post-teach {@code claims} must read ZERO.
     */
    private String chainArmVerdict(REnumValueRef enr, String equivalentVerdict, RWorkspace workspace) {
        if (enr.resolvedSymbol().isPresent()) {
            // #480 MF-1: legacy renders the rule/function-receiver navigation whenever
            // resolvedSymbol is present — before the item-chain arm (the head-name collision
            // shape); the arm yields, restated here first.
            return "declines:headSymbolNav";
        }
        String headName = enr.enumName();
        String leafName = enr.valueName();
        if (headName == null || headName.isEmpty() || leafName == null || leafName.isEmpty()) {
            return "declines:nameMissing"; // the OBS-2 recut — empty names decline like the arm's own gate
        }
        // (1) a closure-param head is a real Java lambda var — never re-root (legacy's
        //     ladder). #505 arm-A1: the class CLAIMS through adaptClosureParamHeadChain when
        //     the binder-source walk proves the element, the VALUE segment resolves plain
        //     and the node types (this verdict channel restates the arm's gates; the
        //     composed decline faces mirror the adapter twin's).
        if (isEnclosingClosureParamNameW(enr, headName)) {
            RExpression cpSrc = closureBinderSourceOf(enr, headName);
            RDataType cpElem = cpSrc == null ? null
                    : ExpressionToIRAdapter.sourceElementDataType(cpSrc);
            if (cpElem == null) {
                return "declines:closureParamHead.twNull";
            }
            RAttribute cpLeaf = leafName == null ? null
                    : HandlerHelper.findAttributeOnDataType(cpElem, leafName);
            if (cpLeaf == null) {
                return "declines:closureParamHead.leafMiss";
            }
            if (isMetaAnnotated(cpLeaf)) {
                return "declines:closureParamHead.metaLeaf";
            }
            return "claims";
        }
        // (2) a function-scope head roots at the variable/alias render (the #389 self-shadow
        //     shortcut exemption restated)
        RFunction fn = HandlerHelper.findEnclosingFunction(enr);
        RShortcut selfShadow = HandlerHelper.findEnclosingShortcut(enr);
        boolean headIsSelfShortcut = selfShadow != null && headName.equals(selfShadow.name());
        if (fn != null && !headIsSelfShortcut && nameInFunctionScopeW(fn, headName)) {
            return "declines:scopeHead";
        }
        // (3) the rule-input chain precedence (contract-impossible here — the chainFall channel
        //     gate already skipped the ruleInput population; kept as a drift detector via the
        //     REAL legacy synthesizer)
        REnumValueRef.AttributeChain chain = enr.resolvedAttributeChain().orElse(null);
        if (chain != null && chain.attributeOpt().isPresent()
                && getReferenceHandler().synthesizeImplicitInputChain(enr, chain) != null) {
            return "declines:ruleInputChain";
        }
        // (4) the binder gates (the arm's prove-or-decline narrowing of legacy's
        //     bindsImplicitItem: before-switch, implicit-or-paramless, filter/extract body)
        RExpression source = filterExtractSourceOfArmBinder(enr);
        if (source == null) {
            return "declines:noFilterExtractBinder";
        }
        // (5) the structural element-type derivation
        RDataType elementType = sourceElementDataTypeShape(source, 0);
        if (elementType == null) {
            return "declines:sourceElementUnresolved";
        }
        // (6) the head resolves by name on the element type (legacy's own gate)
        RAttribute headAttr = HandlerHelper.findAttributeOnDataType(elementType, headName);
        if (headAttr == null) {
            return "declines:headNotOnElement";
        }
        // (7) the bound-chain identity guard / the by-name leaf resolution (legacy's split)
        RAttribute boundHead = chain == null ? null : chain.attributeOpt().orElse(null);
        RAttribute leafAttr;
        if (boundHead != null) {
            if (boundHead != headAttr) {
                return "declines:boundHeadMismatch";
            }
            leafAttr = chain.feature();
        } else {
            RDataType headType = declaredDataTypeOf(headAttr);
            if (headType == null) {
                return "declines:headTypeUnresolved"; // incl. the choice-projection class — prove-or-decline
            }
            leafAttr = HandlerHelper.findAttributeOnDataType(headType, leafName);
        }
        if (leafAttr == null) {
            return "declines:leafUnresolved"; // incl. legacy's record-leaf fall-through — stays legacy
        }
        // (8) the equivalent's own gates (the delegation)
        if (!"lowers".equals(equivalentVerdict)) {
            return equivalentDeclineToken(equivalentVerdict);
        }
        // (9) the raw node's cached type (the cache-boundary rebuild's channel)
        RMetaAnnotatedType navType = workspace.getInferredType(enr);
        if (navType == null || navType.isMissing()) {
            return "declines:navTypeMissing";
        }
        return "claims";
    }

    /** #480: compose the equivalent's own verdict onto the arm token ({@code declines:equivalent.<sub>}). */
    private static String equivalentDeclineToken(String equivalentVerdict) {
        String sub = equivalentVerdict.startsWith("declines:")
                ? equivalentVerdict.substring("declines:".length())
                : equivalentVerdict;
        return "declines:equivalent." + sub;
    }

    /**
     * #480: the filter/extract SOURCE the planned arms derive from — the raw node's nearest inline
     * fn walked with SWITCH precedence (the #479 retype's own walk), admitted only when the binder
     * is an IMPLICIT-or-paramless filter/extract BODY; the returned expression is that
     * filter/extract's argument. {@code null} outside the arm's binder slice.
     */
    private static RExpression filterExtractSourceOfArmBinder(RNode raw) {
        if (nearestSwitchBeforeLambda(raw) != null) {
            return null; // a switch between the node and the lambda — the #221 subject machinery
        }
        RInlineFunction binder = nearestInlineFn(raw);
        if (binder == null || !(binder.isImplicit() || binder.paramNames().isEmpty())) {
            return null;
        }
        RNode parent = binder.parent();
        if (parent instanceof RFilterExpr filter && filter.body() == binder) {
            return filter.argument();
        }
        if (parent instanceof RExtractExpr extract && extract.body() == binder) {
            return extract.argument();
        }
        return null;
    }

    /**
     * #480: the STRUCTURAL element data type of an allowlist-shaped source — the defining
     * feature/attribute's DECLARED type (the same declared-{@code typeCall} channel legacy's
     * structural walk reads for these shapes): a feature call's resolved last hop, a bare
     * symbol's attribute, a disguised {@link REnumValueRef} via legacy's FUNCTION-rooted by-name
     * ladder (the Seat-1 #480 MF-2 recut — rule-scoped disguised sources decline), an
     * all-IDENTICAL list literal,
     * a filter's inner (element-preserving), the #481 widening — a grammar-elided PIPED
     * implicit resolved to the enclosing then's argument
     * ({@link #resolveElidedPipedSourceShape}) and derived THERE — the #482 widening: a
     * NESTED {@link RThenExpr} pipe derived on its BODY result (the element form of
     * {@code A then B} is B's body's own) — the #483 widening: an {@link RExtractExpr}
     * with an implicit-or-paramless body derived on its BODY result (the same body-descent
     * law one construct wider; a named extract declines) — the #484 widening: a bare
     * function reference derived on the callee's OUTPUT attribute's declared type (the
     * callable-output identity; the aliasShadow precedence declines first) — and the #487
     * widening: an {@link RConditionalExpr} derived as the arms' COMMON form (instance
     * identity — the list-literal law lifted to the two-armed choice; an empty else derives
     * the then form alone; mixed forms decline — the arm's mirror, restated). {@code null}
     * where the
     * derivation cannot PROVE a
     * single data type (the arm declines — prove-or-decline).
     */
    private static RDataType sourceElementDataTypeShape(RExpression source, int depth) {
        if (source == null || depth >= 8) {
            return null;
        }
        if (source instanceof RImplicitVariable) {
            // #481: the elided-pipe widening — derive on the resolved pipe argument (the value
            // identity). #485: an UNRESOLVED implicit derives through the rule-input arm (the
            // rule's declared from-type — the declaration read; the arm's mirror, restated).
            RExpression resolved = resolveElidedPipedSourceShape(source);
            if (resolved instanceof RImplicitVariable iv) {
                return ruleTopInputElementTypeShape(iv);
            }
            return resolved == null ? null : sourceElementDataTypeShape(resolved, depth + 1);
        }
        if (source instanceof RThenExpr then) {
            // #482: the nested-pipe widening — derive on the BODY result (the value identity
            // one seat deeper); a bodyless then declines (the arm's recursion, restated).
            RExpression body = then.body().map(RInlineFunction::body).orElse(null);
            return body == null ? null : sourceElementDataTypeShape(body, depth + 1);
        }
        if (source instanceof RExtractExpr extract) {
            // #483: the extract-bodied widening — derive on the BODY result (the element form
            // of `A extract B` is B's own result per element); implicit-or-paramless bodies
            // only, a NAMED extract declines (the arm's gates, restated).
            RInlineFunction bodyFn = extract.body();
            if (bodyFn == null || !(bodyFn.isImplicit() || bodyFn.paramNames().isEmpty())) {
                return null;
            }
            RExpression body = bodyFn.body();
            return body == null ? null : sourceElementDataTypeShape(body, depth + 1);
        }
        if (source instanceof RConditionalExpr cond) {
            // #487 (the teach fold — the planned 3-arg tier dissolved): the conditional JOIN
            // derivation — the arms' COMMON form (instance identity, the list-literal law);
            // an empty else derives the then form (the arm's mirror, restated).
            RDataType thenT = sourceElementDataTypeShape(cond.thenBranch(), depth + 1);
            if (thenT == null) {
                return null;
            }
            if (!hasGenuineElseShape(cond)) {
                return thenT;
            }
            RDataType elseT = sourceElementDataTypeShape(cond.elseBranch().orElse(null), depth + 1);
            return elseT == thenT ? thenT : null;
        }
        if (source instanceof RFeatureCall fc) {
            return declaredDataTypeOf(fc.resolvedFeature().orElse(null));
        }
        if (source instanceof RSymbolReference sym) {
            if (!sym.args().isEmpty()) {
                return null;
            }
            RNode symbol = sym.symbol().orElse(null);
            if (symbol instanceof RAttribute attr) {
                return declaredDataTypeOf(attr);
            }
            if (symbol instanceof RFunction calleeFn) {
                // #484: the callable-output widening — the element data type is the callee's
                // OUTPUT attribute's declared type (the aliasShadow precedence declines first;
                // the arm's derivation leg, restated).
                RFunction enclosing = HandlerHelper.findEnclosingFunction(sym);
                if (enclosing != null && nameMatchesShortcutW(enclosing, sym.name())) {
                    return null;
                }
                return declaredDataTypeOf(calleeFn.output().orElse(null));
            }
            return null;
        }
        if (source instanceof REnumValueRef evr) {
            // #480 MF-2: legacy's derivation for a disguised source is never the linker's bind —
            // the FUNCTION-rooted by-name resolveDisguisedFeature arm, then legacy's #358
            // RULE-gated in-lambda fallback (the head by NAME on the enclosing-lambda item type);
            // everything else declines (prove-or-decline). Restates the arm's recut exactly.
            if (evr.enumeration().isPresent() || evr.enumName() == null || evr.valueName() == null) {
                return null; // legacy's disguised-receiver arm gates on enumeration-EMPTY
            }
            RNode headSymbol = evr.resolvedSymbol().orElse(null);
            if (headSymbol instanceof RRule || headSymbol instanceof RFunction) {
                // the rule/function-invocation-rooted source (legacy's own callable-receiver
                // channel): the element form is the bound nav LEAF's declared type.
                if (evr.resolvedAttributeChain().isPresent()) {
                    return declaredDataTypeOf(evr.resolvedAttributeChain().get().feature());
                }
                return declaredDataTypeOf(evr.resolvedInputFeature().orElse(null));
            }
            RFunction sourceFn = HandlerHelper.findEnclosingFunction(evr);
            if (sourceFn != null) {
                RAttribute head = null;
                for (RAttribute input : sourceFn.inputs()) {
                    if (evr.enumName().equals(input.name())) {
                        head = input;
                        break;
                    }
                }
                if (head == null && sourceFn.output().isPresent()
                        && evr.enumName().equals(sourceFn.output().get().name())) {
                    head = sourceFn.output().get();
                }
                RDataType headType = declaredDataTypeOf(head);
                return declaredDataTypeOf(headType == null ? null
                        : HandlerHelper.findAttributeOnDataType(headType, evr.valueName()));
            }
            RRule sourceRule = HandlerHelper.findEnclosingRule(evr);
            if (sourceRule != null) {
                REnumValueRef.AttributeChain evrChain = evr.resolvedAttributeChain().orElse(null);
                RAttribute boundHead = evrChain == null ? null
                        : evrChain.attributeOpt().orElse(null);
                if (boundHead != null && sourceRule.fromType().isPresent()
                        && sourceRule.fromType().get().referencedType().orElse(null)
                                instanceof RDataType fromType
                        && HandlerHelper.findAttributeOnDataType(fromType, boundHead.name())
                                == boundHead) {
                    // the rule-INPUT chain source (legacy's own synthesizeImplicitInputChain
                    // channel — the bound head identity-proven the from-type's feature): the
                    // element form is the bound LEAF's declared type.
                    return declaredDataTypeOf(evrChain.feature());
                }
                if (nearestInlineFn(evr) != null) {
                    // legacy's #358 RULE-gated in-lambda fallback resolves here — the bound
                    // LEAF's declared type is the byte-proven element form.
                    if (evrChain != null) {
                        return declaredDataTypeOf(evrChain.feature());
                    }
                    return declaredDataTypeOf(evr.resolvedInputFeature().orElse(null));
                }
                // the Seat-1 #480 MF-2 face: a rule-TOP-LEVEL disguised source — every leg of
                // legacy's ladder is structurally null there (prove-or-decline: decline).
                return null;
            }
            return null;
        }
        if (source instanceof RListLiteral list) {
            if (list.elements().isEmpty()) {
                return null;
            }
            RDataType common = null;
            for (RExpression element : list.elements()) {
                RDataType elementType = sourceElementDataTypeShape(element, depth + 1);
                if (elementType == null || (common != null && elementType != common)) {
                    return null; // unprovable or mixed element types — decline
                }
                common = elementType;
            }
            return common;
        }
        if (source instanceof RFilterExpr filter) {
            return sourceElementDataTypeShape(filter.argument(), depth + 1);
        }
        return null;
    }

    /** #480: an attribute's DECLARED data type ({@code typeCall}'s referenced {@link RDataType}), else null. */
    private static RDataType declaredDataTypeOf(RAttribute attr) {
        if (attr == null || attr.typeCall() == null) {
            return null;
        }
        return attr.typeCall().referencedType().orElse(null) instanceof RDataType dataType
                ? dataType : null;
    }

    /**
     * #480: restates legacy {@code synthesizeImplicitInputNavigation}'s admission (the rule-input
     * precedence the bare-attr arm must yield to) via legacy's own {@code HandlerHelper} walks:
     * an enclosing rule with a data-type from-type whose identity-matched feature IS the bound
     * attribute.
     */
    private static boolean wouldSynthesizeRuleInputNav(RSymbolReference ref, RAttribute attr) {
        RRule rule = HandlerHelper.findEnclosingRule(ref);
        if (rule == null || rule.fromType().isEmpty()) {
            return false;
        }
        return rule.fromType().get().referencedType().orElse(null) instanceof RDataType fromType
                && HandlerHelper.findAttributeOnDataType(fromType, attr.name()) == attr;
    }

    /** #480: restates the adapter's {@code collidesWithShortcut} (name-match on the enclosing function's shortcuts). */
    private static boolean nameMatchesShortcutW(RFunction fn, String name) {
        if (name == null) {
            return false;
        }
        for (RShortcut shortcut : fn.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return true;
            }
        }
        return false;
    }

    /** #480: restates legacy {@code isEnclosingClosureParam} (bounded walk, stops at rule/function). */
    private static boolean isEnclosingClosureParamNameW(RNode start, String name) {
        RNode cur = start == null ? null : start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction inline && inline.paramNames().contains(name)) {
                return true;
            }
            if (cur instanceof RRule || cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /** #480: restates legacy {@code nameResolvesInFunctionScope} (inputs, output, shortcuts). */
    private static boolean nameInFunctionScopeW(RFunction fn, String name) {
        for (RAttribute input : fn.inputs()) {
            if (name.equals(input.name())) {
                return true;
            }
        }
        if (fn.output().isPresent() && name.equals(fn.output().get().name())) {
            return true;
        }
        return nameMatchesShortcutW(fn, name);
    }

    /** The nearest enclosing inline function (lambda) — ReferenceHandler's own bounded walk, restated. */
    private static RInlineFunction nearestInlineFn(RNode node) {
        RNode cur = node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction fn) {
                return fn;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Restates {@code ReferenceHandler.nearestEnclosingSwitchSubject}: the nearest enclosing
     * switch, {@code null} if an inline function intervenes (a closer lambda owns {@code item}).
     */
    private static RSwitchExpr nearestSwitchBeforeLambda(RNode node) {
        RNode cur = node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return null;
            }
            if (cur instanceof RSwitchExpr sw) {
                return sw;
            }
            cur = cur.parent();
        }
        return null;
    }

    /**
     * Restates {@code synthesizeCaseNarrowedChain}'s boundary: the node sits under a switch CASE
     * with no inline function between (the case-scope shape; the arm itself additionally requires
     * the LIVE #221 subject binding, which cannot exist under the probe).
     */
    private static boolean underSwitchCaseBeforeLambda(RNode node) {
        RNode cur = node.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RSwitchCase) {
                return true;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * Restates {@code ReferenceHandler.isElidedOperandTopLevel}: a synthetic elided operand of a
     * without-left op sitting directly at an {@link RRule} body's top (no wrapping lambda — the
     * {@code MapperS.of(input)} render; a function body never qualifies, its inputs are not named
     * {@code input}).
     */
    private static boolean isElidedOperandAtRuleTopLevel(RImplicitVariable iv) {
        if (!iv.isSynthetic()) {
            return false;
        }
        RNode parent = iv.parent();
        boolean elidedOperand = (parent instanceof RListOpExpr op && op.argument() == iv)
                || (parent instanceof RConversionExpr conv && conv.argument() == iv)
                || (parent instanceof RToStringExpr ts && ts.argument() == iv)
                || (parent instanceof RExtractExpr ext && ext.argument() == iv)
                || (parent instanceof RFilterExpr filt && filt.argument() == iv)
                || (parent instanceof RCountExpr count && count.argument() == iv)
                || (parent instanceof RSortExpr sort && sort.argument() == iv)
                || (parent instanceof RMinExpr min && min.argument() == iv)
                || (parent instanceof RMaxExpr max && max.argument() == iv)
                || (parent instanceof RReduceExpr reduce && reduce.argument() == iv);
        if (!elidedOperand) {
            return false;
        }
        RNode cur = parent.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RRule) {
                return true;
            }
            if (cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * Restates {@code ReferenceHandler.isInsideNamedTopLevelExtractOverBareInput} for a synthetic
     * item whose nearest inline fn is a NAMED extract body: the extract maps the BARE implicit
     * input and sits at rule-body top level — the #357/#364 shape whose implicit renders
     * {@code MapperS.of(input)}.
     */
    private static boolean isNamedTopLevelExtractOverBareInput(RInlineFunction fn, RExtractExpr ext) {
        if (fn.isImplicit() || fn.paramNames().isEmpty()) {
            return false;
        }
        if (!(ext.argument() instanceof RImplicitVariable)) {
            return false;
        }
        RNode cur = ext.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return false;
            }
            if (cur instanceof RRule) {
                return true;
            }
            if (cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * Restates {@code ReferenceHandler.hasEnclosingRuleLambda} (the {@code buildImplicitInputReceiver}
     * split): an inline function lies between the node and the enclosing rule/function root.
     */
    private static boolean hasEnclosingRuleLambda(RNode start) {
        RNode cur = start.parent();
        int depth = 0;
        while (cur != null && depth++ < PARENT_WALK_LIMIT) {
            if (cur instanceof RInlineFunction) {
                return true;
            }
            if (cur instanceof RRule) {
                return false;
            }
            if (cur instanceof RFunction) {
                return false;
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * The nearest {@link RExpression} descendants of {@code node}: its {@link RNode#children()},
     * descending THROUGH non-expression supporting nodes (constructor key-value pairs, only-exists
     * elements, …) until an expression is met — the child unit the adapter's own recursion lowers.
     */
    private static List<RExpression> nearestExpressionDescendants(RNode node) {
        List<RExpression> out = new ArrayList<>();
        collectNearestExpressions(node, out);
        return out;
    }

    private static void collectNearestExpressions(RNode node, List<RExpression> out) {
        for (RNode child : node.children()) {
            if (child instanceof RExpression childExpr) {
                out.add(childExpr);
            } else if (child != null) {
                collectNearestExpressions(child, out);
            }
        }
    }

    /**
     * Attempt IR-driven emission: build the IR for {@code expr} (requires the workspace,
     * reached via the generator model) and lower it through {@link IRJavaLeafEmitter}.
     * Returns empty — signalling the caller to fall back to the legacy handler — when no
     * workspace is available, when the node is not (yet) IR-expressible, or when the leaf
     * emitter does not handle its kind. A genuine emission increments {@link #irDrivenCount}.
     */
    private Optional<JavaStatementBuilder> tryEmitFromIR(RExpression expr, ExpressionContext ctx) {
        GeneratorModel generatorModel = getGeneratorModel();
        if (generatorModel == null) {
            return Optional.empty();
        }
        RWorkspace workspace = generatorModel.workspace();
        if (workspace == null) {
            return Optional.empty();
        }
        // v3.1 flip seat 21 — facet overrideChainWitness, LAW 77: the override-chain base witness
        // (NavigationHandler.calleeArgBaseAttributeOrNull — upstream's findProperty(name,
        // expectedType) at a call-argument seat) is a render decision the IR emitter cannot take:
        // IRJavaLeafEmitter.emitFieldAccess names the IR node's stamped LEAF type and carries no
        // expected type. The guard holds at every claim ROOT — five seats: the four branches below
        // (an apply carrying such an argument — its arg navs render inside the IR emitter; such a
        // nav itself, a driven nav root at a legacy-rendered call, resolved through the SAME leaf
        // ladder the legacy seat reads (NavigationHandler.navLeafAttrOrNull); a disguised
        // `<input> -> <feature>` root; a BARE attribute symbol that is a call arg) and the L-111
        // visitEnumValueRef quiet FieldAccess claim — each on the predicate legacy evaluates, on
        // the node legacy renders, so BOTH routes render the selection through NavigationHandler
        // and the rings stay converged. The seat-21 ON probe: the emitter renders 3,959
        // reportableInformation/periodicPayment navs with the leaf witness; only the call-argument
        // seats the shared predicate selects a parent for are kept legacy-side.
        //
        // THE NESTED-COMPOSITION RESIDUE (declared, 0 live carriers — seat-21 review SF-1): the guard
        // tests the node when it IS the claim root. A base-witnessed arg whose containing call is NOT
        // the root but an operand of a NATIVELY-COMPOSED root — the L-105 call-as-operand family
        // (isCallOperand: visitComparison / visitEquality / visitExistence) — or a call nested as
        // another call's arg, is unguarded: emitExistence / emitBinaryOp / emitApply recurse inside
        // IRJavaLeafEmitter and emitFieldAccess names the stamped LEAF. The DELEGATING roots cannot
        // carry it — IRConstruct / IRConditional / IRLambdaOp / RULE-APPLY / META-ACCESS / ORACLE-ROOT
        // render the literal super.visitX line, so every interior node re-enters this compiler and
        // runs this guard at its own claim root, and a CHILD-position conditional / lambda declines
        // the whole claim. Measured: the [PROBE21X] enumeration reads 132 base-witness sites across
        // the 275 rows (72 periodicPayment + 56 ReportableInformationBase + 4 Group C) and every one
        // sits under a delegating root (conditional arms, `then extract` lambda bodies, rule `extract`
        // bodies); the chain-C receipt is matrix d0b8a0ad on BOTH routes with the 275 rows
        // byte-identical AND the drr 7.0.0 band file lists identical per FILE. Standing obligation:
        // a future PR that teaches a native compose for any shape that can contain a call carrying a
        // base-witnessed argument must MOVE this guard from the root branches to a
        // scanForPostPinCoercion arm in the same PR.
        if (expr instanceof RSymbolReference callRoot
                && NavigationHandler.callCarriesOverrideBaseWitnessArg(callRoot, this)) {
            return Optional.empty();
        }
        if (expr instanceof RFeatureCall navRoot) {
            // the SAME three-rung leaf ladder the legacy nav seat reads (review SF-1: the first rung
            // alone let a fallback-resolved override nav render the LEAF here — one body now)
            RAttribute navLeaf = NavigationHandler.navLeafAttrOrNull(navRoot, this);
            if (navLeaf != null
                    && NavigationHandler.calleeArgBaseAttributeOrNull(navRoot, navLeaf, this) != null) {
                return Optional.empty();
            }
        }
        if (expr instanceof REnumValueRef disguisedRoot
                && NavigationHandler.disguisedArgIsOverrideBaseWitness(disguisedRoot, this)) {
            return Optional.empty();
        }
        if (expr instanceof RSymbolReference bareRoot
                && NavigationHandler.bareArgIsOverrideBaseWitness(bareRoot, this)) {
            return Optional.empty(); // the bare implicit-item feature as a call arg (the rule-body `extract Fn(attr)` seat)
        }
        Optional<IRExpr> ir = adapter.adapt(expr, workspace);
        if (ir.isEmpty()) {
            // Point-free function application (L-109 → the #492 teach): the adapter now LOWERS a bare
            // no-arg function reference to IRPointFreeApply (its own kind — the IRApply gates stay
            // byte-frozen), so a top-level point-free node normally takes the EMITTER path above,
            // where the range-correlated PointFreeRenderer reuses the SAME oracle
            // (renderImplicitFunctionInvocation) — byte-identical either way. This seat remains as
            // the RESIDUE BELT for any adapter-decline face (today only the shortcut-collision
            // face, which the isAliasReference guard below deliberately SKIPS — a colliding name
            // renders as an alias invocation on the legacy path), so the branch is expected ~ZERO
            // post-#492 and its counter reads the belt, not the family.
            if (expr instanceof RSymbolReference fnRef
                    && fnRef.args().isEmpty()
                    && fnRef.symbol().orElse(null) instanceof RFunction fnSym
                    && !ReferenceHandler.isAliasReference(fnRef)) {
                JavaStatementBuilder rendered =
                        getReferenceHandler().renderImplicitFunctionInvocation(fnRef, fnSym, ctx, this);
                irDrivenCount++;
                irDrivenDelegatedCount++;
                pointFreeFnDrivenCount++;
                return Optional.of(rendered);
            }
            // Implicit-INPUT navigation (L-109e): a bare attribute reference inside a RULE body that names a feature
            // of the rule's from-type — legacy expands it to `input -> attr` by synthesizing an RFeatureCall and
            // routing it through visitFeatureCall (handle's attr branch). The synthetic `input` receiver is a Java-
            // synthesis decision the neutral IR cannot represent (gm-aware, the L-029 split), so the IR claims the bare
            // node by reusing the EXACT legacy synthesizer + the same visitFeatureCall — byte-identical to the fallback.
            // GATED on synthesizeImplicitInputNavigation != null, which selects ONLY the rule-input case (a feature of
            // the from-type); the implicit-ITEM case (`item -> attr` in a filter/extract lambda) returns null here and
            // stays on the fallback, where its synthetic item-nav is ALREADY genuinely IR-driven (a FieldAccess off the
            // L-080 item) — so relabeling it would be vacuous. The isAliasReference guard skips the shortcut-collision
            // case (handle's alias branch). Falls through to decline (the variable path) when not a from-type feature.
            if (expr instanceof RSymbolReference attrRef
                    && attrRef.args().isEmpty()
                    && attrRef.symbol().orElse(null) instanceof RAttribute
                    && !ReferenceHandler.isAliasReference(attrRef)) {
                RAttribute inputAttr = (RAttribute) attrRef.symbol().get();
                RFeatureCall inputNav = getReferenceHandler().synthesizeImplicitInputNavigation(attrRef, inputAttr);
                if (inputNav != null) {
                    JavaStatementBuilder rendered = this.visitFeatureCall(inputNav, ctx);
                    irDrivenCount++;
                    irDrivenDelegatedCount++;
                    implicitInputNavDrivenCount++;
                    return Optional.of(rendered);
                }
            }
            // Meta-feature navigation (L-109d): a `receiver -> metaFeature` whose feature carries a `metadata`
            // annotation. The adapter deliberately declines it (the with-meta `MapperS<FieldWithMetaX>` retype +
            // coercion is a gm-aware Java-emission decision the neutral IR cannot make — the L-029 split,
            // adaptFeatureCall's meta gate). The IR PATH claims the node by routing it to the meta-aware legacy
            // renderer: `super.visitFeatureCall` IS the exact fallback the decline would take (visitFeatureCall does
            // `tryEmitFromIR; else super.visitFeatureCall`), so the bytes are PROVABLY unchanged — only the §4.2
            // counter moves (declined → driven). This is the established oracle-reuse closure (the IR routes the node
            // + delegates the gm-aware Java decision to the legacy handler), the navigation analogue of the rule/
            // to-string/point-free delegations. No byte risk by construction.
            // POST-#499 STATE: the meta arm now LOWERS the seat's convertible slice (a receiver
            // that lowers → IRMetaAccess, served by the root-site MetaNavRenderer — the SAME
            // super.visitFeatureCall line, byte-identical BY IDENTITY, counted
            // metaNavLoweredCount), so THIS belt keeps only the RESIDUE (a receiver that does
            // not lower — the recvBlocked-non-cascade faces of the #497/#499 censuses); the
            // seat's total conserves as lowered + delegated by construction (the L-111
            // conversion pattern at the meta seat).
            if (expr instanceof RFeatureCall metaFc
                    && metaFc.resolvedFeature().isPresent()
                    && isMetaAnnotated(metaFc.resolvedFeature().get())) {
                if (blockerProbeEnabled) {
                    // #497: the metaNav census rides the relabel belt (Σ ≡ the seat's count per
                    // cell by construction), probe-only — the conversion cluster's seat-1 decode.
                    metaNavRootFacets.merge(metaNavRootFacet(metaFc), 1, Integer::sum);
                }
                JavaStatementBuilder rendered = super.visitFeatureCall(metaFc, ctx);
                irDrivenCount++;
                irDrivenDelegatedCount++;
                metaNavDrivenCount++;
                return Optional.of(rendered);
            }
            // Input-feature-RECEIVER navigation (L-112): a feature-call `(<input> -> <feature>) -> <next>` whose
            // receiver chain bottoms out in a disguised-input-feature REnumValueRef the cascade fix typed. The adapter
            // declines the whole feature-call because adaptEnumValueRef lowers only GENUINE enum values, so the
            // disguised REnumValueRef receiver never lowers (adaptFeatureCall's adapt(receiver) returns empty). The IR
            // PATH claims it by routing to `super.visitFeatureCall` (THE exact fallback — bytes provably unchanged) +
            // counting it driven: the multi-hop sibling of the L-111 leaf relabel (the same TYPED-but-Lab-gated nav,
            // one hop up — NOT the fork-gated MISSING case). Placed AFTER the meta gate so a meta-annotated such nav is
            // claimed by L-109d (matching the census, which counted this bucket after meta). A declined→driven move.
            if (expr instanceof RFeatureCall fcOverInputNav
                    && hasInputFeatureNavInReceiverChain(fcOverInputNav)) {
                if (blockerProbeEnabled) {
                    // #497: the L-112 census rides the relabel belt (Σ ≡ the seat's count per
                    // cell by construction), probe-only — the conversion cluster's seat-3 decode.
                    inputNavReceiverRootFacets.merge(inputNavReceiverRootFacet(fcOverInputNav),
                            1, Integer::sum);
                }
                JavaStatementBuilder rendered = super.visitFeatureCall(fcOverInputNav, ctx);
                irDrivenCount++;
                irDrivenDelegatedCount++;
                inputFeatureNavReceiverDrivenCount++;
                return Optional.of(rendered);
            }
            // #509 arm-A1 — the parsed-seat alias IDENTITY-serve leg (the #507 L-111
            // identity-serve pattern at the RFeatureCall seat): the adapter's alias arm WOULD
            // lower the nav (the shared #492/#508 core admits — the aliasIdGate census read
            // the face 143/143 guard-held, 100% atRoot, all bodies data-typed with resolving
            // hops) but the STRONG body allowlist cannot prove the body's element form (the
            // #326 weaker-oracle class — the #508 probe2 catch), so the claim site declines
            // it to THIS belt. The serve renders the LITERAL fallback line (the same
            // super.visitFeatureCall the decline takes — byte-identical BY IDENTITY; no
            // compose happens, so the guard's protection class is untouched) and counts the
            // event LOWERED on its own receipt (aliasNavIdentityLoweredCount — an IR lowering
            // EXISTS for the shape, only the render is served; the #507 accounting law). The
            // census serve face is named AT BIRTH (the OBS-4 law): Σ served.identityShape
            // rows ≡ aliasNavIdentityLoweredCount per cell by construction.
            if (expr instanceof RFeatureCall guardHeldAliasNav
                    && guardHeldAliasNav.resolvedFeature().isEmpty()
                    && adapter.isGuardHeldParsedAliasNav(guardHeldAliasNav, workspace)) {
                if (blockerProbeEnabled) {
                    String servedToken = "aliasIdGate.served.identityShape.FieldAccess";
                    aliasIdGateFacets.merge(servedToken, 1, Integer::sum);
                    if (!aliasIdGateFirstSamples.containsKey(servedToken)) {
                        aliasIdGateFirstSamples.put(servedToken,
                                enclosingSampleToken(guardHeldAliasNav));
                    }
                }
                JavaStatementBuilder served = super.visitFeatureCall(guardHeldAliasNav, ctx);
                irDrivenCount++;
                aliasNavIdentityLoweredCount++;
                return Optional.of(served);
            }
            // Implicit-ATTRIBUTE navigation (L-113): a bare non-meta attribute reference `foo` legacy expands to an
            // implicit input/item navigation (the synthetic receiver is a gm-aware/AST-derived Java-synthesis decision
            // the neutral IR cannot make — the L-029 split). The IR PATH claims it by routing to
            // `super.visitSymbolReference` (THE exact fallback — bytes provably unchanged) + counting it driven. The
            // same relabel class as the L-109e rule-input case (the half L-109c dismissed as "already-driven/vacuous"):
            // an L-113 census MEASURED the synthetic does NOT drive (drove=0), so these are genuine un-driven nodes.
            // Placed AFTER the L-109e rule-input gate (which claims the synthesizeImplicitInputNavigation != null case
            // via its own renderer) so L-113 claims the remaining bare non-meta attrs. Excludes meta attrs (a gm-aware
            // FieldWithMetaX retype — a separate residual). A declined→driven move; fires on BOTH seams (drr RULE largest).
            // #528 arm-5 — THE L-113 CONVERSION: the adapter now MINTS IRImplicitAttrNav for
            // this belt's whole population (the 45th kind — the shallow synthesized-receiver
            // bare attribute), so a claim reaching this seat has an IR lowering and never
            // enters the ir-EMPTY branch: the belt is UNREACHABLE for the converted class and
            // its claims count LOWERED through the oracle-root bareSymbolRef serve — the SAME
            // super.visitSymbolReference render this belt performed, byte-identical BY
            // IDENTITY. The seat is KEPT as the frozen-zero honest-residue meter (the #516
            // claimRootDelegatedCount precedent): a future positive value means a bare
            // non-meta attribute reached the ir-EMPTY branch again — a mint regression that
            // self-signals on the split line rather than silently re-delegating.
            if (expr instanceof RSymbolReference implicitAttrRef
                    && implicitAttrRef.args().isEmpty()
                    && implicitAttrRef.symbol().orElse(null) instanceof RAttribute implicitAttr
                    && !isMetaAnnotated(implicitAttr)) {
                if (blockerProbeEnabled) {
                    // #497: the implicitAttrNav census rides the relabel belt (Σ ≡ the seat's
                    // count per cell by construction), probe-only — the conversion cluster's
                    // seat-2 decode.
                    implicitAttrRootFacets.merge(implicitAttrRootFacet(implicitAttrRef),
                            1, Integer::sum);
                }
                JavaStatementBuilder rendered = super.visitSymbolReference(implicitAttrRef, ctx);
                irDrivenCount++;
                irDrivenDelegatedCount++;
                implicitAttrNavDrivenCount++;
                return Optional.of(rendered);
            }
            recordDecline("adapterGap", expr); // the adapter could not lower it; no relabel gate claimed it
            if (blockerProbeEnabled) {
                probeMinimalBlockers(expr, workspace); // #476: attribute the decline to its blocking families (read-only)
            }
            return Optional.empty();
        }
        // The claim-root SERVE seat (the #471 teach, widened at #473 and #474; the accounting CONVERTED
        // delegated → IR-LOWERED at #516 — THE DELEGATED-SEAT SERVE): a call that IS the whole
        // claim, whose FIRST-TRIP arg coercion is one of the four ACCEPTED evaluate-arg arms (the #471 meta
        // pair + the #473 singletonList endgame arm + the #474 intLiteral RULE-seam arm), renders via
        // {@code super.visitSymbolReference} — literally the
        // decline's own render (the #469 claim-root law: oracle-reuse is provably byte-identical
        // ONLY at the claim root), so the hoist family's five legacy route facets (BLOCK / STATEMENT_SINK /
        // LAMBDA_CHANNEL / armDeref / flat — #237/#285/#340/#346/#364), the multi elementwise-deref splice
        // (#349) and the singletonList coercion family (#368/#436/#269) stay single-sourced in legacy rather
        // than being re-derived here. The #516 conversion moves ONLY the counters (the #507 identity-serve
        // accounting law: the adapter's IR lowering EXISTS — this seat sits after the adapt gate, ir is
        // present by construction — so the claim counts LOWERED on the seat's flip receipt + per-arm census;
        // the render call is character-identical to the pre-conversion delegation, bytes CANNOT move).
        // Runs BEFORE the guard, so if this seat ever un-fires the guard
        // re-declines the claim and the taught arm's explicit ZERO breaks on the meter — self-signaling, not
        // silent drift (since #472 an un-fire routes to the guard seat, where a proven composition root would
        // serve — but a ROOT call is RSymbolReference, outside the router's set, so it still re-DECLINES
        // and breaks the meter zero + the carrier-cell declined==0 locks, ALL THREE cells since the #473
        // retense: the self-signal survives the router). A NESTED coercing call falls to the guard seat
        // below — a proven composition root serves it whole (delegated-counted before #516); other roots
        // decline (the honest-residue default). See {@link #claimRootDelegationArm}.
        // seat 30 law 1 rung 2: a RENDER-deciding seat — pass the live scope so the BINDING channel is on.
        PostPinArm rootServeArm = claimRootDelegationArm(expr, ctx == null ? null : ctx.scope());
        if (rootServeArm != null) {
            JavaStatementBuilder rendered = super.visitSymbolReference((RSymbolReference) expr, ctx);
            irDrivenCount++;
            claimRootServeLoweredCount++;
            claimRootServesByArm[rootServeArm.ordinal()]++;
            if (POSTPIN_WITNESS) {
                RFunction serveFn = HandlerHelper.findEnclosingFunction(expr);
                RRule serveRule = serveFn == null ? HandlerHelper.findEnclosingRule(expr) : null;
                System.out.println("[postPin witness] arm=" + rootServeArm.label()
                        + "-rootServed in=" + (serveFn != null ? serveFn.name()
                                : serveRule != null ? serveRule.name() : "<unresolved>"));
            }
            return Optional.of(rendered);
        }
        // The post-pin coercion guard (the #467 drift wave): DECLINE the whole claim to legacy when the claimed
        // subtree contains a shape whose LEGACY render gained a post-pin evaluate-arg / item-navigation coercion
        // refinement the IR route does not reproduce — the strangler pattern's honest fallback, byte-identical by
        // construction (legacy renders the declined node in full, refinements included). Supersedes the #466
        // top-level-only alias-arg guard (aliasArgRequiresMetaDeref): the drifted carriers nest the call INSIDE a
        // claimed exists/comparison, which a top-level probe never saw — this scan walks the WHOLE claimed subtree,
        // so nested applies are covered. See {@link #subtreeTripsPostPinCoercion}.
        //
        // #494 — a CONSTRUCT claim root is EXEMPT from the scan, provably: its emission IS the guard's own
        // fallback (the range-correlated ConstructRenderer calls super.visitConstructor(site, ctx) — the
        // literal legacy render, post-pin refinements included BY IDENTITY), and every pair value re-enters
        // this compiler through the oracle's own per-pair compiles, where the value's OWN claim runs this
        // guard at its own root. There is no IR-composed render to protect — the guard's premise ("the IR
        // route does not reproduce the refinement") is false by construction for this root class. Without
        // the exemption the FUNCTION cells' taught-zero decline meters break on trip shapes INSIDE pair
        // values (the first #494 ring read: 11/47/34 declines, all byte-identical either way). THE STANDING
        // OBLIGATION (the Seat-1 #494 OBS-4 pin): the exemption keys on the node KIND but its proof rests on
        // the SHALLOW oracle-closed render — the named deep-enrichment wave (field-value IR children, an
        // IR-composed render) must REMOVE or RE-PROVE this exemption in the same PR that lands the deep render.
        //
        // #495 — a CONDITIONAL claim root joins the exemption BY THE SAME IDENTITY: its emission is the
        // range-correlated ConditionalRenderer's super.visitConditional(site, ctx) — the literal legacy
        // render — and every condition/branch node re-enters this compiler through the oracle's own
        // dispatch, running this guard at its own claim root. The #495 probe read 40 adapterLowers roots
        // whose subtrees WOULD trip (the .guardTrip faces, all drr FUNCTION) — without the exemption those
        // break the taught-zero meters exactly like the first #494 ring read. The SAME standing obligation
        // extends: a future wave replacing the wholesale oracle render with an IR-composed conditional
        // render must REMOVE or RE-PROVE this exemption in the same PR.
        //
        // #496 — a LAMBDA_OP claim root joins BY THE SAME IDENTITY once more: its emission is the
        // range-correlated LambdaOpRenderer's super.visitExtract/super.visitFilter(site, ctx) — the
        // literal legacy fallbacks — and every receiver/body node re-enters this compiler through the
        // oracle's own dispatch, running this guard at its own claim root. The #496 lambdaVisit probe
        // read 35 lowers.guardTrip roots across the two families. The SAME standing obligation extends
        // to any future IR-composed lambda render. (The #496 L-111 conversion claims take NO exemption —
        // their render is the emitter's OWN FieldAccess emission, so the guard stays live at that seat
        // by the same premise this exemption negates.)
        //
        // #498 — an ARGS-PRESENT RULE-APPLY claim root joins BY THE SAME IDENTITY: its emission is
        // the root-site RuleApplyRenderer's getReferenceHandler().handle(ref, ctx, this) — the one
        // line super.visitSymbolReference IS — so every post-pin refinement is included BY IDENTITY,
        // and the argument re-enters this compiler through the oracle's own compile, running this
        // guard at its own claim root. The SAME standing obligation extends to any future
        // IR-composed rule-apply render. Deliberately NARROW: kind RULE with args() non-empty ONLY —
        // a FUNCTION apply is the emitter's OWN composed render (the guard's live protection class,
        // the #466 carriers), and the BARE rule delegation's L-045 leg composes
        // `receiver.evaluate(input)` emitter-side — both stay scanned. (An IRListOp{ONLY_ELEMENT}
        // root — the #498 leg-A teach — takes NO exemption, consistent with its already-taught
        // flat-list-op siblings: the whole family renders via the CollectionOpRenderer oracle and
        // has always run under the live scan.)
        //
        // #499 — a META-ACCESS claim root joins BY THE SAME IDENTITY (the FIFTH kind): its
        // emission is the root-site MetaNavRenderer's super.visitFeatureCall(site, ctx) — the
        // LITERAL L-109d relabel-belt line this conversion replaces, which itself never ran the
        // scan (a relabel delegation renders BEFORE the guard by construction) — so the
        // conversion keeps the seat's guard posture EXACTLY while every receiver/interior node
        // re-enters this compiler through the oracle's own recursion, running this guard at its
        // own claim root. The SAME standing obligation extends to any future IR-composed meta
        // render (the remove-or-re-prove list now spans FIVE kinds: CONSTRUCT / CONDITIONAL /
        // LAMBDA_OP / RULE-APPLY / META-ACCESS). The #499 nav teach's IRListOp RECEIVERS take NO
        // exemption — the nav claim's render is the emitter's OWN FieldAccess composition, the
        // guard's live protection class, so those roots stay scanned.
        //
        // #500 — the ORACLE-ROOT shapes join BY THE SAME IDENTITY (the SIXTH entry, ONE
        // predicate): every root the generic OracleRootRenderer serves — the arm-B family roots
        // (conversion / pipe / only-exists / root list-literal, kinds with NO native emitter
        // arm) and the arm-A meta-bearing roots (nav / binary / existence / non-rule apply with
        // an interior meta hop) — renders the literal super.visitX legacy line, so every
        // post-pin refinement is included BY IDENTITY and every interior node re-enters this
        // compiler through the oracle's own recursion, running this guard at its own claim
        // root. The exemption keys on the SHARED isOracleRootShape predicate — the SAME gate
        // that installs the slot — so the exempted set and the oracle-served set cannot drift.
        // The guard's live protection class is untouched: a meta-FREE FieldAccess / BinaryOp /
        // Existence / FUNCTION-apply root fails the predicate and stays scanned (the emitter's
        // OWN composed renders — the #466 carriers). The SAME standing obligation extends: any
        // future IR-composed render for a served shape must REMOVE or RE-PROVE its exemption in
        // the same PR (the remove-or-re-prove list now spans CONSTRUCT / CONDITIONAL /
        // LAMBDA_OP / RULE-APPLY / META-ACCESS / the ORACLE-ROOT shapes).
        //
        // #501 — the SAME predicate widened, the SAME identity: the oracle-leaf set grew from
        // IRMetaAccess alone to the shallow/consumer-admitted kinds (IROnlyExists / IRPipe /
        // IRConversion in operand/arg positions + the new IRSymbolNav — see containsOracleLeaf),
        // and IRSymbolNav joined the kind screens, so every #501-widened root's serve is still
        // the literal super.visitX legacy line (byte-identical BY IDENTITY — every post-pin
        // refinement included). The disguised-meta at-root class (the bareMeta face) is covered
        // by the standing #499 kind-wide IRMetaAccess entry — no new exemption kind. The guard's
        // live protection class is again untouched: an oracle-leaf-FREE native root stays
        // scanned. The remove-or-re-prove obligation carries forward unchanged (SIX kinds).
        //
        // #502 — the SAME predicate widened again, the SAME identity: the oracle-leaf set adds
        // IRClosureParam + IRMetaOutputApply (the arm-2/arm-4 shallow kinds — see
        // containsOracleLeaf) and both join the kind screens, so every #502-widened root's
        // serve is still the literal super.visitX legacy line (byte-identical BY IDENTITY —
        // the closure-param binding and the meta-output call threading are legacy's own inside
        // that line). The guard's live protection class is again untouched: an
        // oracle-leaf-FREE native root stays scanned. The remove-or-re-prove obligation
        // carries forward unchanged (SIX kinds).
        //
        // #505 — the converted disguised-ENR seat's claims arrive SHAPE-PRE-GATED (only
        // isOracleRootShape / IRMetaAccess-rooted lowerings enter tryEmitFromIR — the seat's
        // own gate), so the standing shape-keyed exemptions cover them: the serve is the
        // literal super.visitEnumValueRef line, every post-pin refinement included BY
        // IDENTITY, every re-entrant interior visit running this guard at its own root. (A
        // SITE-keyed exemption leg was tried and REVERTED with its frame-gate sibling — the
        // L-111 hijack, the first #505 probe loop's catch.)
        boolean postPinExempt = ir.get() instanceof IRConstruct || ir.get() instanceof IRConditional
                || ir.get() instanceof IRLambdaOp
                || isArgsPresentRuleApply(ir.get())
                || ir.get() instanceof IRMetaAccess
                || isOracleRootShape(ir.get())
                // #505: the plain-spine ENR serves join by the same identity (the shared
                // isEnumChainSpineRoot gate — served, exempted and installed as one set).
                || isEnumChainSpineRoot(ir.get(), expr);
        // seat 30 law 1 rung 2: THE render-deciding seat — pass the live scope so the BINDING channel is on.
        PostPinArm postPinArm = postPinExempt ? null
                : subtreeTripsPostPinCoercion(expr, ctx == null ? null : ctx.scope());
        // #527: the missing-hop arg-nav probe — AFTER the standing scan (a scan-tripping claim
        // keeps its standing first-trip attribution), and only for non-exempt roots (an exempt
        // root already serves whole-legacy through its own standing slot, missing hops
        // included BY IDENTITY). The probe reads the LOWERED tree only (cheap, no re-adapts):
        // its trigger set was a 100%-decline face pre-teach, so no standing claim can trip
        // (see {@link PostPinArm#ARG_NAV_MISSING_HOP}).
        if (postPinArm == null && !postPinExempt
                && loweredArgChainCarriesMissingHop(ir.get())) {
            postPinArm = PostPinArm.ARG_NAV_MISSING_HOP;
            postPinTripDetail = "loweredArgChain missing-typed hop";
        }
        if (postPinArm != null) {
            // Snapshot the scan's trip detail BEFORE any render: a delegated render re-enters
            // tryEmitFromIR, and a nested trip's scan overwrites the shared field — the local pins THIS
            // claim's own detail (the #469 per-emission-state corollary; Seat-1 #472 OBS-1). The decline
            // path prints before any render and reads the same local for uniformity.
            String tripDetail = postPinTripDetail;
            // The #472 composition-boundary widening (the accounting CONVERTED delegated → IR-LOWERED at
            // #516 — THE DELEGATED-SEAT SERVE): a tripped claim whose ROOT class is one of the seven
            // PROVEN composition classes serves the WHOLE claim via the caller's own fallback render
            // ({@link #compositionRootDelegation} — the #469 claim-root law widened per-class), so the
            // legacy-rendered subtree is never wrapped in IR-rendered parents (the cpON4 drift class stays
            // impossible — the whole claim is legacy's render, exactly the decline's bytes) and only the
            // accounting moves. The #516 conversion moves ONLY the counters (the #507 identity-serve
            // accounting law: the adapter's IR lowering EXISTS — ir is present at this seat by
            // construction — so the claim counts LOWERED on the router's flip receipt + the served-by-arm
            // census; the render call is character-identical to the pre-conversion delegation, bytes
            // CANNOT move). Roots outside the proven set (an RSymbolReference root the seat above did
            // not accept — a nested trip under a call root or a future NEW evaluate-arg arm, the int-literal
            // first trip being ACCEPTED since #474 — RComparisonExpr, any future shape) still DECLINE below
            // — the honest-residue default on the meter (the frozen corpus reads ZERO declines on BOTH the
            // FUNCTION seam [the #473 endgame teach] and the RULE seam [the #474 intLiteral teach]) —
            // EXCEPT, since #523, an RSymbolReference-rooted resolved call whose first trip is one of the
            // two ITEM-NAV arms: that pair serves at the leg below (explicit membership — the #469
            // claim-root law, arm-independent; every other arm keeps the honest-residue decline).
            JavaStatementBuilder served = compositionRootDelegation(expr, ctx);
            // #523 — THE ITEM-NAV CLAIM-ROOT SERVE (the guard co-design's compiler half): a
            // tripped claim whose ROOT is a resolved-function call and whose first-trip arm is
            // one of the two ITEM-NAVIGATION arms serves the WHOLE claim via
            // {@code super.visitSymbolReference} — the decline's own render, so bytes CANNOT
            // move (the #469 claim-root law is arm-INDEPENDENT: it binds the render seat, not
            // the tripping arm — the {@link #claimRootDelegationArm} javadoc's own clause).
            // Membership stays EXPLICIT (the honest-residue default): only the item-nav pair
            // is accepted here — a future NEW arm under a call root still declines below and
            // keeps its own guard attribution. This seat is what lets the adapter's #523
            // argNav admission (the itemChain./itemSrcUnprovable. retirement) land with the
            // meter clean: every guarded item-rooted arg shape the adapter now admits either
            // trips here and serves whole-legacy, or renders the #469-taught natives.
            if (served == null
                    && (postPinArm == PostPinArm.MULTI_HOP_ITEM_NAV
                            || postPinArm == PostPinArm.META_ITEM_RECEIVER
                            // #527: the missing-hop arg-nav arm joins the claim-root serve —
                            // the same #469-law seat (the render is the decline's own line,
                            // arm-independent); membership stays EXPLICIT per the
                            // honest-residue default.
                            || postPinArm == PostPinArm.ARG_NAV_MISSING_HOP)
                    && expr instanceof RSymbolReference rootCall
                    && !rootCall.args().isEmpty()
                    && rootCall.symbol().orElse(null) instanceof RFunction) {
                served = super.visitSymbolReference(rootCall, ctx);
            }
            if (served != null) {
                irDrivenCount++;
                postPinServeLoweredCount++;
                postPinServesByArm[postPinArm.ordinal()]++;
                if (POSTPIN_WITNESS) {
                    RFunction serveFn = HandlerHelper.findEnclosingFunction(expr);
                    RRule serveRule = serveFn == null ? HandlerHelper.findEnclosingRule(expr) : null;
                    System.out.println("[postPin witness] arm=" + postPinArm.label()
                            + "-served in=" + (serveFn != null ? serveFn.name()
                                    : serveRule != null ? serveRule.name() : "<unresolved>")
                            + " claimRoot=" + expr.getClass().getSimpleName()
                            + " " + tripDetail);
                }
                return Optional.of(served);
            }
            recordDecline("postPinGuard", expr);
            postPinCoercionDeclinedCount++;
            postPinCoercionDeclinesByArm[postPinArm.ordinal()]++;
            if (POSTPIN_WITNESS) {
                RFunction witnessFn = HandlerHelper.findEnclosingFunction(expr);
                RRule witnessRule = witnessFn == null ? HandlerHelper.findEnclosingRule(expr) : null;
                System.out.println("[postPin witness] arm=" + postPinArm.label()
                        + " in=" + (witnessFn != null ? witnessFn.name()
                                : witnessRule != null ? witnessRule.name() : "<unresolved>")
                        + " claimRoot=" + expr.getClass().getSimpleName()
                        + " " + tripDetail);
            }
            return Optional.empty();
        }
        // Bare RULE delegation gate (the L-029/L-043 split: the context decision is Java-emission-side, reusing legacy's
        // own parent-walk oracles so the IR claim cannot disagree with legacy's dispatch). The adapter lowers EVERY bare
        // RRule reference to an IRApply{RULE}; the IR now drives BOTH shapes legacy's renderImplicitRuleInvocation renders
        // (L-049 extended the L-045 top-level-only slice to the in-lambda population), so the IR-drives set is exactly
        // legacy's full dispatch: an enclosing rule (findEnclosingRule != null) AND either (a) the reference sits inside an
        // extract/filter/then lambda (nearestEnclosingInlineFunction != null — legacy pipes the lambda `item.get()`/
        // `thenArg.get()` binding, rendered by reusing the oracle verbatim via the RuleDelegationRenderer), or (b) at rule-
        // body TOP LEVEL the enclosing rule declares a from-type (which types the synthetic `input` — the L-045 receiver-
        // resolver render). Only two shapes DECLINE to legacy: a rule reference in a plain FUNCTION body, and a
        // from-type-less rule at TOP LEVEL (legacy falls through to the variable path rather than emit an untyped
        // `input`). NOTE (v3.1 flip seat 33, law B.3): the function-body decline is a ROUTE-OWNERSHIP decision, not an
        // emission gap — the collector injects the @Inject rule field on BOTH host paths (the fork's own drr 7.x
        // QuantityUnitOfMeasure carries `@Inject protected UnitOfMeasureFromQuantityRule unitOfMeasureFromQuantityRule;`
        // at a FUNCTION host) and legacy's caseSymbolReference gate now RENDERS the in-lambda function-host shape. The
        // decline is KEPT because the top-level arm's own rebuild carries the #274 band regression below; the legacy
        // fall-through renders both shapes byte-identically on either route.
        // #498: the dispatch gate above is BARE-shape-only — an ARGS-PRESENT rule invocation
        // (`<Rule>(<arg>)`, the adaptApply rule leg) claims regardless of host kind, because its
        // legacy render is the injected-field explicit-args path with NO host gate (upstream
        // renders the RosettaRule case identically to the Function case — the #332 law; the
        // collector injects the @Inject field on both paths), served whole by the root-site
        // RuleApplyRenderer. The args().isEmpty() conjunct keeps ruleDelegationDeclines serving
        // exactly the bare-delegation dispatch it mirrors.
        if (ir.get() instanceof IRApply ruleApply
                && ruleApply.callee() instanceof IRReference ruleCallee
                && ruleCallee.referenceKind() == IRReference.ReferenceKind.RULE
                && ruleApply.args().isEmpty()
                && ruleDelegationDeclines(expr)) {
            recordDecline("ruleDelegation", expr);
            return Optional.empty();
        }
        // Alias/shortcut reference: the Java render — the bare aliasName(enclosingInputs) or, for the
        // #381 W-facet class, the MapperS.of(aliasName(outputName.toBuilder(), …).build()) wrap (the
        // #531 teach) — needs facts the neutral node lacks: the enclosing function's inputs, the
        // dependency-collision name, the W output name. Computed HERE (the L-029 split: Java-emission
        // facts assembled compiler-side, where the AST node lives) and handed to the emitter, NOT
        // carried on the neutral IR. Handled before the generic emit (no witness/collision).
        if (ir.get() instanceof IRReference alias && alias.referenceKind() == IRReference.ReferenceKind.ALIAS) {
            Optional<JavaStatementBuilder> aliasEmitted = tryEmitAlias(alias, expr);
            if (aliasEmitted.isPresent()) {
                irDrivenCount++;
            } else {
                // enclosing function not resolved (→ legacy's empty-arg-list path) or the
                // corpus-absent escape belt — the W-facet class renders since #531.
                recordDecline("aliasResolution", expr);
            }
            return aliasEmitted;
        }
        Optional<JavaStatementBuilder> emitted = emitViaLeafEmitter(expr, ctx, ir.get());
        if (emitted.isPresent()) {
            irDrivenCount++;
        } else {
            // #515 THE EMITTER-FRONTIER SERVE: before recording the decline, serve the
            // decline's OWN fallback for the gap-census raw families — the per-family
            // literal super.visitX line, exactly what the converted caller's orElseGet
            // would run on the empty return (the frame was already restored by
            // emitViaLeafEmitter's finally — same environment, same bytes BY IDENTITY;
            // the #499 L-109d conversion argument at the LAST unconverted site). A serve
            // counts LOWERED (irDrivenCount ONLY — an IR lowering EXISTS, the #507
            // accounting law) with its own flip receipt; an unserved family keeps the
            // decline + the census names it (the honest residue).
            JavaStatementBuilder served = emitterGapServe(expr, ctx);
            if (served != null) {
                irDrivenCount++;
                emitterServeLoweredCount++;
                if (blockerProbeEnabled) {
                    // The served census rows carry the .served suffix — Σ served rows ≡
                    // emitterServeLoweredCount per cell BY CONSTRUCTION.
                    leafEmitterGapFacets.merge(
                            expr.getClass().getSimpleName() + ":root:" + ir.get().kind()
                                    + ".served",
                            1, Integer::sum);
                }
                return Optional.of(served);
            }
            recordDecline("leafEmitter", expr); // no leaf-emitter arm for some IR kind in the tree → legacy fallback
            if (blockerProbeEnabled) {
                // #514: the emitter-gap decode (probe-only) — see leafEmitterGapFacets.
                leafEmitterGapFacets.merge(
                        expr.getClass().getSimpleName() + ":root:" + ir.get().kind(),
                        1, Integer::sum);
            }
        }
        return emitted;
    }

    /** #514: the leafEmitter-gap census reader ({@link #leafEmitterGapFacets}) — probe-only. */
    public String leafEmitterGapBreakdown() {
        return rankedBreakdown(leafEmitterGapFacets);
    }

    /**
     * #515 THE EMITTER-FRONTIER SERVE — the per-raw-family switch over the gap-census
     * families (the #514 {@link #leafEmitterGapFacets} decode: RImplicitVariable:root:
     * VARIABLE 959 · RFeatureCall:root:FIELD_ACCESS 771 · root:META_ACCESS 141 the
     * leaders), each leg the LITERAL {@code super.visitX(expr, ctx)} line the family's
     * converted visit seat would run as its own {@code orElseGet} fallback on the empty
     * return — byte-identical BY IDENTITY (the caller-fallback audit: every converted
     * seat for these eleven families falls back to exactly this call; the raw families
     * outside the switch — the enum-chain and to-string seats' own classes among them —
     * return null and keep the decline). Null on an unserved family.
     */
    private JavaStatementBuilder emitterGapServe(RExpression expr, ExpressionContext ctx) {
        if (expr instanceof RImplicitVariable implicitVar) {
            return super.visitImplicitVariable(implicitVar, ctx);
        }
        if (expr instanceof RFeatureCall fc) {
            return super.visitFeatureCall(fc, ctx);
        }
        if (expr instanceof REqualityExpr eq) {
            return super.visitEquality(eq, ctx);
        }
        if (expr instanceof RSymbolReference symRef) {
            return super.visitSymbolReference(symRef, ctx);
        }
        if (expr instanceof RIntLiteral intLit) {
            return super.visitIntLiteral(intLit, ctx);
        }
        if (expr instanceof RLogicalExpr logical) {
            return super.visitLogical(logical, ctx);
        }
        if (expr instanceof RComparisonExpr cmp) {
            return super.visitComparison(cmp, ctx);
        }
        if (expr instanceof RArithmeticExpr arith) {
            return super.visitArithmetic(arith, ctx);
        }
        if (expr instanceof RExistenceExpr exists) {
            return super.visitExistence(exists, ctx);
        }
        if (expr instanceof RConstructorExpr ctor) {
            return super.visitConstructor(ctor, ctx);
        }
        if (expr instanceof RConditionalExpr cond) {
            return super.visitConditional(cond, ctx);
        }
        return null; // unserved raw family — the decline stands + the census names it
    }

    /**
     * The frame-install + emission core shared by {@link #tryEmitFromIR} and the #496 L-111
     * conversion's QUIET claim path ({@link #visitEnumValueRef}'s disguised branch) — counters are
     * deliberately NOT applied here: {@code tryEmitFromIR} records driven/declined at its own
     * seat, while the quiet path applies its lowered counters on success and falls back to the
     * byte-proven relabel on a decline WITHOUT any decline recording (the seat's total stays
     * conserved as lowered + delegated — a declined-and-then-relabeled event would double-count).
     * Collision-render attribution IS applied here (attributed on a USED emission, discarded on a
     * decline — identical semantics at both callers).
     *
     * <p>The enclosing function's OUTPUT Java type is the one fact a witnessed leaf needs to
     * reproduce legacy's witness/output simple-name collision render (FQN-inline +
     * import-suppress) — a Java-emission decision, computed HERE (where the AST node + translator
     * are available), not in the language-neutral adapter (L-029); null for a non-function seam /
     * no output / meta output → no collision. The COMPLETE per-emission resolver/renderer frame
     * installs in one swap (the #469 re-entrancy fix: an oracle render can recurse back through
     * this compiler — the CollectionOpRenderer's NORMAL-recursion contract — and the nested claim
     * must RESTORE this frame on exit, never null it; a nulled ImplicitItemRenderer would
     * downgrade a later USER_ITEM render in THIS emission to the bare form, and a nulled resolver
     * would turn later calls into spurious declines). The seams, each documented on its builder:
     * the alias-operand resolver (nested ALIAS operands), the call-receiver resolver (@Inject
     * field names — since #469 incl. the dep-collision numbering), the rule-receiver resolver
     * (&lt;Name&gt;Rule fields), the in-lambda rule-delegation renderer (L-049), the
     * call-param-cardinality resolver (tailMulti), the collection-op renderer (L-050), the
     * to-string renderer (L-070), the implicit-item renderer (the #469 live scope binding), the
     * item-nav renderer (the #469 guarded chained/meta item navs — the exact legacy fallback
     * render), the five kind-gated root-site renderers (#494 construct / #495 conditional /
     * #496 lambda-op / #498 rule-apply / #499 meta-nav — each buildable work exclusively on its
     * own claim-root kind, the Copilot #494-R3 gate law; every other claim skips the builds
     * outright, and a child-position node of those kinds inside a lowered tree finds no
     * renderer — declining to legacy exactly as pre-teach) and the #500 GENERIC oracle-root
     * renderer (ONE slot for the arm-B family roots + the arm-A meta-bearing roots, gated by
     * the shared {@link #isOracleRootShape} predicate — the same gate law, one slot).
     */
    private Optional<JavaStatementBuilder> emitViaLeafEmitter(RExpression expr, ExpressionContext ctx,
            IRExpr ir) {
        JavaClass<?> outputType = enclosingFunctionOutputJavaType(expr);
        IRJavaLeafEmitter.Resolvers previousFrame = leafEmitter.swapResolvers(new IRJavaLeafEmitter.Resolvers(
                buildAliasResolver(expr),
                buildCallReceiverResolver(expr),
                buildRuleReceiverResolver(expr),
                buildRuleDelegationRenderer(expr, ctx),
                buildCallParamMultiResolver(expr),
                buildCollectionOpRenderer(expr, ctx),
                buildToStringRenderer(expr, ctx),
                buildImplicitItemRenderer(expr, ctx),
                buildItemNavRenderer(expr, ctx),
                buildSyntheticItemRenderer(expr, ctx),
                buildPointFreeRenderer(expr, ctx),
                ir instanceof IRConstruct ? buildConstructRenderer(expr, ctx) : null,
                ir instanceof IRConditional ? buildConditionalRenderer(expr, ctx) : null,
                ir instanceof IRLambdaOp ? buildLambdaOpRenderer(expr, ctx) : null,
                ir instanceof IRApply ? buildRuleApplyRenderer(expr, ctx) : null,
                ir instanceof IRMetaAccess ? buildMetaNavRenderer(expr, ctx) : null,
                // #501: the disguised-meta OR-leg installs the oracle slot for the bareMeta
                // at-root class (RSymbolReference raw — MetaNav's builder is RFeatureCall-gated
                // and returns null there); an RFeatureCall meta root fails the disguised gate
                // and keeps the #499 MetaNav serve + receipt exactly (see isDisguisedMetaRoot).
                // #505: the converted disguised-ENR seat's claims arrive SHAPE-PRE-GATED
                // (the seat's own oracle-shape gate — see visitEnumValueRef: only
                // isOracleRootShape / IRMetaAccess-rooted lowerings enter, so the standing
                // shape-keyed gates here serve them via the enumChain leg; a SITE-keyed leg
                // was tried and REVERTED — it hijacked the L-111 quiet claims' proven
                // native renders into serves whose legacy re-entry cascaded ~3,480 new
                // declining interior visits, the first #505 probe loop's catch).
                isOracleRootShape(ir) || isDisguisedMetaRoot(ir, expr)
                        || isEnumChainSpineRoot(ir, expr)
                        ? buildOracleRootRenderer(expr, ctx) : null,
                // law E.2: the function-INPUT escape table — null for every function with no
                // escaping input, and the identity outside the escape population (today's bytes).
                buildInputEscape(expr)));
        // Snapshot the emitter's collision-render counter so we attribute collision renders ONLY to a
        // top-level emission that is actually USED (present); if it declines, the speculative renders of
        // the would-be subtree are discarded (§4.2 instrumentation; see IRJavaLeafEmitter.collisionFqnRenders).
        int collisionBefore = leafEmitter.collisionFqnRenderCount();
        Optional<JavaStatementBuilder> emitted;
        try {
            emitted = leafEmitter.emit(ir, ctx, getTypeUtil(), getTypeTranslator(), outputType);
        } finally {
            // Copilot #469 C-1: restore in a finally so a throwing emit cannot leak THIS emission's frame
            // into subsequent emissions on the same compiler instance (a caught-and-continue caller would
            // otherwise see stale resolvers — captured against the WRONG enclosing function — serving
            // later claims; wrong facts, not just spurious declines).
            leafEmitter.swapResolvers(previousFrame);
        }
        if (emitted.isPresent()) {
            collisionRenderCount += leafEmitter.collisionFqnRenderCount() - collisionBefore;
        } else {
            leafEmitter.setCollisionFqnRenderCount(collisionBefore); // discard the declined subtree's speculative renders
        }
        return emitted;
    }

    /**
     * The post-pin coercion guard (the #467 drift wave): whether the claimed subtree contains a shape whose LEGACY
     * render fires a POST-PIN coercion refinement the IR route does not reproduce. The #466 ON ring's first honest
     * reading decoded the 34-file drift to ONE family — the ported handlers reproduce PIN-ERA render decisions where
     * Path-1 gained post-pin refinements ({@code target-466-ab-samples.txt}) — and every arm here keys on the SAME
     * oracles the legacy refinement keys on, so the decline set tracks the refinement set:
     * <ol>
     *   <li><strong>the int-literal evaluate-arg</strong> (legacy facet {@code numericCoercionCompose} M2, PR #300,
     *       inside {@code ReferenceHandler.tryMetaDerefArg}): a within-long {@link RIntLiteral} into a
     *       {@code BigDecimal} parameter renders the inline {@code BigDecimal.valueOf(<lit>)} (the beyond-long
     *       sibling takes the {@code new BigDecimal(new BigInteger("…"))} hoist) — the IR rendered the bare literal
     *       (drr rule-family {@code IndexFactorRule} {@code evaluate(1)});</li>
     *   <li><strong>the meta-wrapper evaluate-arg</strong> (legacy {@code tryMetaDerefArg}'s hoist family — PR
     *       #237/#285/#340/#346/#364): a meta-wrapped argument into a meta-FREE single parameter hoists
     *       {@code final <Wrapper> v = …;} + derefs {@code (v == null ? null : v.getValue())} — the IR rendered the
     *       flat un-derefed form (drr {@code Create_AnnaDsbUpiRequestUnderlyingForCredit}). The argument's meta-ness
     *       is probed through the SAME typing channels legacy cascades: the alias walk
     *       ({@link NavigationHandler#tryAliasReceiverMapperType}), the list-op terminal walk
     *       ({@link NavigationHandler#tryTerminalMetaMapperType}), the implicit-item walk
     *       ({@link NavigationHandler#implicitItemArgMeta}) and the direct meta-annotated terminal feature;</li>
     *   <li><strong>the meta-typed implicit ITEM at a navigation base</strong> (the receiver-coercion family — the
     *       {@code .map("Type coercion", …getValue())} deref step legacy inserts ahead of the feature hop): the IR
     *       rendered the direct single-hop map (cdm {@code ExtractTradePurchasePrice}).</li>
     * </ol>
     * A meta-annotated callee parameter EXPECTS its wrapper (legacy facet {@code inverseN7CalleeParamMeta}, #347),
     * so it never declines; a multi parameter is outside the hoist family's gate, same as legacy. Supersedes the
     * #466 top-level-only {@code aliasArgRequiresMetaDeref} guard — the drifted carriers nest the call inside a
     * claimed exists/comparison the top-level probe never saw. OVER-declining is byte-safe by construction (legacy
     * renders a declined node identically wherever no refinement fires — e.g. a sink-less position keeps legacy's
     * own flat form); UNDER-declining is exactly what the flag-on D11 ring catches. A Java-emission decision kept
     * compiler-side (the L-029 split: the language-neutral adapter cannot name Java types).
     *
     * <p>Returns the FIRST tripping {@link PostPinArm} in scan order ({@code null} = the subtree is clean, the
     * claim drives) — the per-arm attribution feeding the share-growth wave's meter (PR #469). The short-circuit
     * order is exactly the boolean guard's, so the decline DECISION is unchanged; only the attribution is new.
     *
     * <p>Since #471 the claim-root delegation seat ({@link #claimRootDelegationArm} — the meta pair;
     * widened to the singletonList arm at #473 and the intLiteral arm at #474) runs BEFORE this scan, so a
     * ROOT-position call whose first-trip arg coercion is an ACCEPTED arm never reaches it (delegated,
     * driven); the meta + singletonList + intLiteral buckets here read NESTED trip sites only. The seat
     * re-probes the same {@link #evaluateArgWouldCoerce} in the same order, so the two cannot disagree on
     * attribution.
     */
    private PostPinArm subtreeTripsPostPinCoercion(RExpression root) {
        // The oracle guard lives ONCE, on the scope-carrying impl below; this form delegates so the
        // two can never drift apart (the sibling pattern the other four overload pairs already follow).
        // The dispatch-variant context gate (the #467 wholesale arm) was TAUGHT at #470 and retired. Inside a
        // dispatch VARIANT the enclosing function's own inputs are the single `__synthesized_input__` placeholder,
        // and the post-pin legacy render family re-derives EVERYTHING positional from the dispatch BASE (facet
        // dispatchVariantParamResolution PR #369). The #470 teaches close the two decoded faces: (1) alias-call
        // arg lists forward the BASE's declared inputs ({@link #aliasCallInputNames}' base swap — the same oracle
        // legacy renderEnclosingInputs consults; the #466 A/B's `daysInPeriod(__synthesized_input__)` face,
        // decoded at #467) and
        // (2) the nested-divide witness classifies NUMBER before the engine arm
        // ({@code IRJavaLeafEmitter.nestedArithmeticWitness}, mirroring legacy numericOperandKind's post-pin #369
        // `earlyDiv` arm — the A/B's `<Integer,…>`-where-legacy-renders-`<BigDecimal,…>` face). Every other
        // variant channel is context-generic and taught elsewhere: a call's receiver resolves through the
        // variant's OWN dependency numbering (the same legacy oracle both sides), arg renders recurse the taught
        // alias/enum/literal channels, arg accessors are callee-side, and the generic arg-coercion probes below
        // run on variant call args exactly as anywhere else. Base-input references adapt since the #505 arm-C
        // mint (IRDispatchInputRef — the fork AST still leaves them unresolved; the adapter's dispatchBase
        // scope-join mirrors the #369 generator-side resolution), and the kind is an ORACLE LEAF: every
        // containing claim root renders whole-legacy through the oracle serves, so those claims never reach a
        // native compose — byte-safe by routing. {@link PostPinArm#DISPATCH_VARIANT} stays as the
        // explicit-zero taught signal on the meter.
        // The dep-receiver collision-numbering gate (the #467 A/B face R-G) was TAUGHT at #469 and its
        // wholesale decline retired: {@link #buildCallReceiverResolver} now rebuilds legacy
        // disambiguateDependencyReceiver's full precedence (the FQN-keyed numbering map first, then the
        // dep-vs-shortcut "0", then the bare name), correlating each IR callee back to its AST call site by
        // SOURCE RANGE — so claims inside a numbered function render the numbered receivers themselves.
        // {@link PostPinArm#DEP_NUMBERING} stays as the explicit-zero success signal on the meter.
        return subtreeTripsPostPinCoercion(root, null);
    }

    /**
     * v3.1 flip seat 30, law 1 rung 2 — the scope-carrying overload of the standing post-pin scan. The three
     * RENDER-deciding seats call THIS one so {@link #argItemTypeIsMeta}'s BINDING channel is live where bytes
     * are decided; the diagnostic/label seats keep the no-scope form (see that method's javadoc).
     */
    private PostPinArm subtreeTripsPostPinCoercion(RExpression root,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope) {
        if (getTypeUtil() == null || getTypeTranslator() == null || getGeneratorModel() == null) {
            return null; // no type oracles — legacy's refinements gate on the same oracles and cannot fire either
        }
        return scanForPostPinCoercion(root, root, scope);
    }

    /**
     * The claim-root delegation probe (the #471 teach, widened at #473 and #474 — {@code metaArgClaimRootArm}
     * pre-#473): when the claim ROOT is itself a resolved function call whose FIRST-TRIP evaluate-arg
     * coercion (probing the call's own args in order, exactly the scan's short-circuit order — so the
     * first-trip attribution and this seat cannot disagree) is one of the four ACCEPTED arms, return that
     * arm; else {@code null}. The accepted arms:
     * <ul>
     *   <li>{@link PostPinArm#META_WRAPPER_SINGLE_ARG} — the hoist+deref family
     *       ({@code final <Wrapper> v = …;} + the null-guarded {@code .getValue()}, legacy
     *       {@code tryMetaDerefArg} #237/#285/#340/#346/#364): the corpus census (the #471 decode,
     *       {@code target-471-witness1.log}) reads 51 of 56 declines at the claim root;</li>
     *   <li>{@link PostPinArm#META_ITEM_MULTI_ARG_B2} — the multi-param elementwise wrapper deref
     *       (the inline bare {@code .map("Type coercion", …)} before {@code .getMulti()}, legacy facet
     *       multiArgElementwiseWrapperDeref #349): 16 of 24 at the claim root;</li>
     *   <li>{@link PostPinArm#SINGLETON_LIST_ARG} — the single-into-multi coercion family (#368/#436/#269),
     *       ACCEPTED since #473 (the FUNCTION-seam endgame teach, menu item C): the frozen corpus's one
     *       FUNCTION claim (drr {@code IsActionTypeMODI} calling {@code FilterOpenTradeStates}, argIndex 0,
     *       the #472 witness census) roots at the call itself — the same proven {@code RSymbolReference}
     *       claim-root shape as the meta pair;</li>
     *   <li>{@link PostPinArm#INT_LITERAL_ARG} — the int-literal → {@code BigDecimal} evaluate-arg family
     *       (M2 #300 / #128), ACCEPTED since #474 (the RULE-seam teach): the frozen corpus's whole
     *       population is 10 claim-root decline events across 6 drr rule names (the #473 RULE-seam witness
     *       census — NotionalAmountOfLeg1/2 ×3 each · TotalNotionalQuantityOfLeg2 · PutAmount · CallAmount
     *       · IndexFactor; callees at argIndex 1 · {@code FormatToBaseOne18Rate} argIndex 0), EVERY one
     *       {@code callAtRoot=true} with {@code claimRoot=RSymbolReference} — the same proven shape; the
     *       arm's FUNCTION-seam population is zero, so the FUNCTION decline meter stays all-zeros with no
     *       pin movement.</li>
     * </ul>
     * Every arm {@link #evaluateArgWouldCoerce} can return today is accepted, but membership stays explicit
     * — a future NEW arm keeps its own guard attribution (the honest-residue default; see the return-site
     * comment). Oracle-absent compilers return {@code null} (the guard's own gate — the
     * probes cannot run, and legacy's refinements gate on the same oracles). The delegated render is
     * {@code super.visitSymbolReference} — the decline's own call, so over-approximation in the probe is
     * byte-safe by construction (legacy renders the claim in full, refinement or no refinement); the
     * byte-identity argument is arm-INDEPENDENT (the #469 claim-root law binds the render seat, not the
     * tripping arm — which is why widening the accepted set is a one-membership change).
     *
     * <p><b>v3.1 flip seat 30, law 1 rung 2 — the {@code scope} parameter.</b> Threading the live scope here
     * is what keeps this probe and {@link #scanForPostPinCoercion} on the SAME predicate (the invariant
     * {@link #subtreeTripsPostPinCoercion}'s javadoc names): both re-probe
     * {@link #evaluateArgWouldCoerce} in the same order with the same inputs. The sole caller is the
     * render-deciding claim-root seat in {@code tryEmitFromIR}, which passes {@code ctx.scope()}. The IR
     * twin first shipped a no-scope 1-arg delegator beside this one; it never had a caller and was deleted
     * at the seat-30 review, in the same sweep as the seat's three legacy-side dead overloads.
     */
    private PostPinArm claimRootDelegationArm(RExpression expr,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope) {
        if (getTypeUtil() == null || getTypeTranslator() == null || getGeneratorModel() == null) {
            return null;
        }
        if (!(expr instanceof RSymbolReference call)
                || call.args().isEmpty()
                || !(call.symbol().orElse(null) instanceof RFunction callee)) {
            return null;
        }
        List<RExpression> args = call.args();
        for (int i = 0; i < args.size() && i < callee.inputs().size(); i++) {
            PostPinArm argArm = evaluateArgWouldCoerce(args.get(i), callee.inputs().get(i), scope);
            if (argArm != null) {
                return argArm == PostPinArm.META_WRAPPER_SINGLE_ARG
                        || argArm == PostPinArm.META_ITEM_MULTI_ARG_B2
                        || argArm == PostPinArm.SINGLETON_LIST_ARG
                        || argArm == PostPinArm.INT_LITERAL_ARG
                        ? argArm
                        // Every arm the probe can return TODAY is accepted (since #474), but membership
                        // stays EXPLICIT: a future NEW evaluate-arg arm falls through to null and keeps
                        // its own guard attribution — self-signaling on the meter, never silently
                        // delegated (the honest-residue default, same law as the router's proven-class set).
                        : null;
            }
        }
        return null;
    }

    /**
     * The #472 composition-boundary whole-parent render (the router; delegate-counted until the #516
     * serve conversion — the seat now counts its claims IR-LOWERED on
     * {@link #postPinServeLoweredCount} with this render unchanged): render a guard-tripped claim via the CALLER's
     * own legacy fallback when the claim ROOT's class is one of the SEVEN PROVEN composition classes (six
     * at #472; {@code RToStringExpr} the seventh at #506 — the widened item-receiver admission's own
     * new-family signal, see the case note) — the
     * exact call the decline path would reach ({@code tryEmitFromIR} empty ⇒ the caller's
     * {@code super.visitX(expr, ctx)}), so the render is the decline's own code path and byte-identity holds
     * by construction (the #469 claim-root law widened per-class). The per-class proof pack (each class
     * verified at source, the #472 recon; re-run for the seventh at #506):
     * <ul>
     *   <li>every proven-class node enters {@code tryEmitFromIR} ONLY from its own visit override (each
     *       call site passes its own typed node — no cross-class site);</li>
     *   <li>each caller's decline fallback is exactly the super call this switch reproduces
     *       ({@code visitFeatureCall} / {@code visitExistence} / {@code visitEquality} / {@code visitLogical}
     *       / {@code visitListOp} / {@code visitCount} / {@code visitToString});</li>
     *   <li>both exits sit at the SAME guard position — everything before the guard ran identically, and the
     *       emission-path state installs after it are reached by neither;</li>
     *   <li>the seven classes have ZERO subclasses repo-wide (the leaf-class audit, same as the #469
     *       {@code RSymbolReference} proof);</li>
     *   <li>the served render re-enters child visits through the same virtual dispatch as the decline's
     *       render — every downstream sub-attempt still occurs, so ATTEMPTED is byte-unchanged (the #471
     *       delegation signature, zero absorption).</li>
     * </ul>
     * The caller's present-path diagnostic counters (item/call operand + nav witnesses) also fire for a
     * served claim when their predicates hold — the #471 OBS-1 re-scope class, annotated on each counter.
     * Returns {@code null} for any root class outside the proven set — the guard declines it (the
     * honest-residue default; ZERO on the frozen corpus's FUNCTION seam since the #473 singletonList
     * endgame teach and on its RULE seam since the #474 intLiteral teach — both delegated their last root
     * calls at the {@link #claimRootDelegationArm} seat, so a decline here now means a NEW family
     * self-signaling on the meter, on either seam).
     */
    private JavaStatementBuilder compositionRootDelegation(RExpression expr, ExpressionContext ctx) {
        return switch (expr) {
            case RFeatureCall fc -> super.visitFeatureCall(fc, ctx);
            case RExistenceExpr ex -> super.visitExistence(ex, ctx);
            case REqualityExpr eq -> super.visitEquality(eq, ctx);
            case RLogicalExpr lg -> super.visitLogical(lg, ctx);
            case RListOpExpr lo -> super.visitListOp(lo, ctx);
            case RCountExpr ct -> super.visitCount(ct, ctx);
            // #506: the SEVENTH proven class — the widened item-receiver admission surfaced
            // two drr to-string roots over nested guarded item chains (the probe3 witness:
            // InterestRateLeg{1,2}Basis, claimRoot=RToStringExpr — the NEW-family
            // self-signal the honest-residue default exists for). The same proof pack holds
            // at source: visitToString enters tryEmitFromIR only with its own typed node,
            // its decline fallback is exactly this super call, RToStringExpr is a leaf
            // class repo-wide, and the served render re-enters child visits through the
            // same virtual dispatch (the per-family toStringDrivenCount counts the
            // served claim as driven — the router's own irDrivenCount law).
            case RToStringExpr ts -> super.visitToString(ts, ctx);
            default -> null;
        };
    }

    /**
     * The recursive walk behind {@link #subtreeTripsPostPinCoercion} — probes function-call args and
     * item-rooted navigation chains against the post-pin legacy refinement predicates. Returns the first
     * tripping {@link PostPinArm} ({@code null} = clean), preserving the boolean walk's short-circuit order.
     * {@code claimRoot} threads the claim's top-level node: a guarded item-rooted nav AT the root does not
     * trip (the {@link ItemNavRenderer} serves it with the exact legacy fallback render — provably identical,
     * it is literally the decline's own call), while a NESTED one still trips on the whole claim (the #469
     * composition boundary: a nested delegation would mix an IR-rendered parent around a legacy-rendered
     * chain — a composition legacy never produced; the cpON4 drift receipt's numbered coercion params are
     * the witness class). The scan only REPORTS the trip; since #472 the guard seat routes it — a proven
     * composition root serves the whole claim ({@link #compositionRootDelegation}, the mixed composition
     * stays impossible either way; IR-LOWERED-counted since the #516 serve), other roots decline.
     *
     * <p><b>v3.1 flip seat 30, law 1 rung 2 — the {@code scope} parameter</b> threads to
     * {@link #evaluateArgWouldCoerce} and on to {@link #argItemTypeIsMeta}'s BINDING channel. It is
     * {@code null} on the diagnostic path {@link #subtreeTripsPostPinCoercion} takes and live on the
     * render-deciding one; the recursion below carries whichever it was given. (The no-scope 2-arg
     * delegator the twin commit added had no callers and was deleted at the seat-30 review.)
     */
    private PostPinArm scanForPostPinCoercion(RNode node, RExpression claimRoot,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope) {
        if (node instanceof RSymbolReference call
                && !call.args().isEmpty()
                && call.symbol().orElse(null) instanceof RFunction callee) {
            // The implicit-item ARG channel (the #467 A/B faces R-D/R-F) was TAUGHT at #469 and its decline
            // retired: the {@link ImplicitItemRenderer} now supplies the LIVE scope binding (named extract
            // param / collision-escaped `_item` / plain `item`) by reusing legacy handle(RImplicitVariable)
            // verbatim on the source-range-correlated node — so a call's implicit-item argument renders the
            // bound name exactly as post-pin legacy does. A META-typed item argument still declines below
            // (the evaluateArgWouldCoerce meta arms — it previously first-tripped here and re-attributes).
            // {@link PostPinArm#ITEM_CALL_ARG} stays as the explicit-zero taught signal on the meter.
            List<RExpression> args = call.args();
            for (int i = 0; i < args.size() && i < callee.inputs().size(); i++) {
                PostPinArm argArm = evaluateArgWouldCoerce(args.get(i), callee.inputs().get(i), scope);
                if (argArm != null) {
                    if (POSTPIN_WITNESS) {
                        postPinTripDetail = "callee=" + callee.name() + " argIndex=" + i
                                + " argShape=" + args.get(i).getClass().getSimpleName()
                                + " callAtRoot=" + (node == claimRoot);
                    }
                    return argArm;
                }
            }
            // #523 — the DISGUISED item-headed chain ARG probe (the guard co-design's
            // recognizer half): a call argument whose chain HEAD (walking explicit
            // RFeatureCall links) is a single-arrow {@code a -> b} disguise that legacy's OWN
            // {@code synthesizeImplicitItemChain} resolves item-headed renders through the
            // SAME multi-hop item machinery the explicit recognizer below trips on (legacy
            // synthesizes the equivalent {@code item -> a -> b} chain and re-enters
            // {@code visitFeatureCall} — the B2 disguised-chain arm), so it takes the SAME
            // trip. The predicate is legacy's synthesizer BY CALL — prove-or-decline with
            // zero mirror drift; a genuine enum-value argument resolves {@code null} there
            // (no item attribute carries an enum type's name) and never trips. ARG-TOP
            // scoped BY CONSTRUCTION (this probe reads only {@code call.args()} elements):
            // disguised chains at operand seats keep their standing native/oracle routes —
            // no reroute of currently-lowered claims.
            ReferenceHandler refHandler = getReferenceHandler();
            if (refHandler != null) {
                for (int i = 0; i < args.size(); i++) {
                    RExpression head = args.get(i);
                    while (head instanceof RFeatureCall fcHop) {
                        head = fcHop.receiver();
                    }
                    if (head instanceof REnumValueRef enr
                            && enr.enumName() != null && enr.valueName() != null
                            && refHandler.synthesizeImplicitItemChain(enr, this) != null) {
                        if (POSTPIN_WITNESS) {
                            postPinTripDetail = "callee=" + callee.name() + " argIndex=" + i
                                    + " argShape=disguisedItemChain"
                                    + " callAtRoot=" + (node == claimRoot);
                        }
                        return PostPinArm.MULTI_HOP_ITEM_NAV;
                    }
                }
            }
        }
        // The item-rooted navigation faces (the #467 A/B faces R-E + the meta-item receiver): TAUGHT at #469
        // for the ROOT position only — a guarded nav that IS the whole claim is served by the
        // {@link ItemNavRenderer} via the exact legacy fallback render ({@code super.visitFeatureCall} — the
        // decline's own call, provably identical), so it does not trip here. A NESTED guarded nav still
        // trips on the whole claim: a NATIVE nested render would wrap a legacy-rendered chain in IR-rendered
        // parents (exists/logical/equality compositions with scope-group-NUMBERED coercion params) — a
        // composition legacy never produced; the cpON4 drift receipt (QuantityFrequencyOrCalculationPeriod)
        // is the witness. Since #472 the guard seat routes the trip: a proven composition root serves the
        // WHOLE claim (legacy renders parent + chain together — the mixed composition stays impossible),
        // other roots decline; the decline buckets read the residue only.
        if (node != claimRoot && node instanceof RFeatureCall fc && isItemRootedFeatureCall(fc)
                && isGuardedItemNav(fc)) {
            if (POSTPIN_WITNESS) {
                postPinTripDetail = "nestedNav feature="
                        + fc.resolvedFeature().map(f -> f instanceof RAttribute a ? a.name() : f.toString())
                                .orElse("<unresolved>")
                        + " receiverShape=" + (fc.receiver() == null ? "<none>"
                                : fc.receiver().getClass().getSimpleName());
            }
            return fc.receiver() instanceof RFeatureCall
                    ? PostPinArm.MULTI_HOP_ITEM_NAV
                    : PostPinArm.META_ITEM_RECEIVER;
        }
        // #491 — the equality-seat alias-RETYPE mirror (the alias-operand admission's residue class):
        // a bare shortcut-resolved ALIAS operand of an EQUALITY whose FunctionAliasHelper signature
        // element the retype oracle names (tryAliasReceiverMapperType — the EXACT lever the equality
        // seat's retypeNullTypedAliasOperand keys on: META wrapper items, the #326 byte-divergent
        // subset whose meta-strip deref the neutral leaf cannot reproduce — the six-carrier drr ring
        // decode — UNION the #334 basic scalars, whose retype is byte-inert here and delegates
        // byte-identically). The adapter's engine-side body read cannot see the wrapper, so the
        // compiler-side guard carries the decision at the SAME seat as every post-pin refinement;
        // trip set = retype set, so the divergent subset can never under-trip. Fires at ANY position
        // incl. the claim root (unlike the item-nav arm, no root-position teach exists here); the
        // router then delegates equality/logical roots WHOLE.
        if (node instanceof REqualityExpr aliasEq) {
            RSymbolReference metaAlias = firstMetaAliasEqualityOperand(aliasEq);
            if (metaAlias != null) {
                if (POSTPIN_WITNESS) {
                    postPinTripDetail = "aliasEqualityOperandMeta alias=" + metaAlias.name();
                }
                return PostPinArm.ALIAS_EQUALITY_OPERAND_META;
            }
        }
        // #492 — the alias-NAV retype mirror (see PostPinArm.ALIAS_NAV_RECEIVER_META): the same
        // lever at the nav seat, two raw shapes. Order after the equality arm is immaterial (the
        // shapes are disjoint: a bare alias ref is not an RFeatureCall/REnumValueRef).
        if (node instanceof RFeatureCall aliasNav
                && aliasNav.receiver() instanceof RSymbolReference navRecv
                && navRecv.args().isEmpty()
                && navRecv.symbol().orElse(null) instanceof RShortcut
                && NavigationHandler.tryAliasReceiverMapperType(navRecv, this) != null) {
            if (POSTPIN_WITNESS) {
                postPinTripDetail = "aliasNavReceiverMeta alias=" + navRecv.name();
            }
            return PostPinArm.ALIAS_NAV_RECEIVER_META;
        }
        if (node instanceof REnumValueRef disguise
                && disguise.resolvedInputFeature().isPresent()
                && disguise.resolvedAttributeChain().isEmpty()) {
            RSymbolReference stub = aliasHeadReceiverStub(disguise);
            if (stub != null && NavigationHandler.tryAliasReceiverMapperType(stub, this) != null) {
                if (POSTPIN_WITNESS) {
                    postPinTripDetail = "aliasNavReceiverMeta alias=" + stub.name()
                            + " disguised=true";
                }
                return PostPinArm.ALIAS_NAV_RECEIVER_META;
            }
        }
        for (RNode child : node.children()) {
            PostPinArm childArm = scanForPostPinCoercion(child, claimRoot, scope);
            if (childArm != null) {
                return childArm;
            }
        }
        return null;
    }

    /**
     * The #491 {@link PostPinArm#ALIAS_EQUALITY_OPERAND_META} probe: the first raw operand of the
     * equality that is a bare shortcut-resolved alias reference whose
     * {@code NavigationHandler.tryAliasReceiverMapperType} walk returns non-null (the RETYPE-oracle
     * set — META wrapper items [the #326 byte-divergent subset] union the #334 basic scalars
     * [byte-inert here] — legacy's own #178 lever, consulted verbatim so the trip set tracks the
     * retype set exactly), else {@code null}.
     */
    private RSymbolReference firstMetaAliasEqualityOperand(REqualityExpr eq) {
        for (RExpression operand : new RExpression[] {eq.rawLeft(), eq.rawRight()}) {
            if (operand instanceof RSymbolReference ref
                    && ref.args().isEmpty()
                    && ref.symbol().orElse(null) instanceof RShortcut
                    && NavigationHandler.tryAliasReceiverMapperType(operand, this) != null) {
                return ref;
            }
        }
        return null;
    }

    /**
     * #492 — the {@link PostPinArm#ALIAS_NAV_RECEIVER_META} probe stub for the DISGUISED shape: a
     * throwaway receiver carrying the disguise's head name, parented to the ref's own parent —
     * byte-for-byte the construction legacy {@code ReferenceHandler.synthesizeFeatureCall}
     * performs on this exact path at render time, so {@code NavigationHandler
     * .resolveAliasShortcut}'s name-match fallback resolves it identically. Built ONLY when the
     * head names an enclosing shortcut (the cheap collision pre-check keeps the standing-path
     * scan allocation-free on the overwhelming non-alias population); null otherwise.
     */
    private static RSymbolReference aliasHeadReceiverStub(REnumValueRef disguise) {
        String headName = disguise.enumName();
        if (headName == null || headName.isEmpty()) {
            return null;
        }
        RFunction enclosing = HandlerHelper.findEnclosingFunction(disguise);
        if (enclosing == null) {
            return null;
        }
        boolean collides = false;
        for (RShortcut shortcut : enclosing.shortcuts()) {
            if (headName.equals(shortcut.name())) {
                collides = true;
                break;
            }
        }
        if (!collides) {
            return null;
        }
        RSymbolReference stub = new RSymbolReference();
        stub.setName(headName);
        stub.setParent(disguise.parent());
        return stub;
    }

    /**
     * This compiler's OWN owner-chain meta probe for a lambda-bound implicit item: walk UP from the item to the
     * nearest owning extract/filter/sort lambda, take the owner's list ARGUMENT, thread then-pipes (an implicit
     * argument recurses to ITS owner), and report whether the chain's terminal navigation feature is
     * meta-annotated — i.e. the extracted elements are meta wrappers, so legacy coerces the item receiver with the
     * {@code .map("Type coercion", …getValue())} step before any feature hop. Deliberately INDEPENDENT of
     * {@code NavigationHandler.implicitItemArgMeta} (whose recovery walk nulls on shapes legacy still coerces via
     * the live compiled-receiver type — the CompareTradeLotToAmount catch); over-reporting is byte-safe (a decline
     * renders via legacy either way), under-reporting is what the flag-on D11 ring catches.
     */
    private boolean itemOwnerChainTerminalIsMeta(RNode start) {
        RNode cur = start;
        for (int guard = 0; cur != null && guard < 64; guard++) {
            if (cur instanceof RFunction || cur instanceof RRule) {
                return false; // reached the declaration root without a meta-terminal owner chain
            }
            RExpression ownerArg = null;
            if (cur instanceof RExtractExpr extract) {
                ownerArg = extract.argument();
            } else if (cur instanceof RFilterExpr filter) {
                ownerArg = filter.argument();
            } else if (cur instanceof RSortExpr sort) {
                ownerArg = sort.argument();
            } else if (cur instanceof RThenExpr then) {
                ownerArg = then.argument();
            }
            if (ownerArg != null && ownerArg != start) {
                // Resolve the owner argument's LEFT-CHAIN terminal (a chained list-op argument's element
                // type comes from ITS chain source).
                RExpression terminal = ownerArg;
                for (int leftGuard = 0; terminal != null && leftGuard < 64; leftGuard++) {
                    if (terminal instanceof RExtractExpr e) {
                        terminal = e.argument();
                    } else if (terminal instanceof RFilterExpr f) {
                        terminal = f.argument();
                    } else if (terminal instanceof RSortExpr s) {
                        terminal = s.argument();
                    } else if (terminal instanceof RThenExpr t) {
                        terminal = t.argument();
                    } else {
                        break;
                    }
                }
                if (terminal instanceof RFeatureCall terminalNav
                        && terminalNav.resolvedFeature().isPresent()
                        && isMetaAnnotated(terminalNav.resolvedFeature().get())) {
                    return true;
                }
                // An implicit or unresolvable terminal (a then-arg pipe, an elided receiver): the chain
                // source sits FURTHER UP the pipe — keep walking, never conclude non-meta here (the
                // CompareTradeLotToAmount catch: `nav filter … then extract [f(item -> value, …)]` reaches
                // the extract's item through a then whose own argument is the piped implicit variable).
            }
            cur = cur.parent();
        }
        return false;
    }

    /**
     * The {@link PostPinArm} under which legacy's evaluate-arg pipeline would COERCE this (arg, param) pair
     * ({@code null} = no coercion, the pair is clean) — the per-arg arms of
     * {@link #subtreeTripsPostPinCoercion}. Mirrors the legacy gates per parameter cardinality:
     * <ul>
     *   <li><strong>SINGLE param</strong> ({@code ReferenceHandler.tryMetaDerefArg}'s family): the int-literal →
     *       {@code BigDecimal} coercion (within-long inline {@code BigDecimal.valueOf}, M2 PR #300; beyond-long
     *       the BigInteger hoist, PR #128) + the meta-wrapper → value hoist+deref (PR #237/#285/#340/#346/#364),
     *       probed through the same public typing oracles legacy cascades;</li>
     *   <li><strong>MULTI param</strong>: the meta-item-typed argument's inline bare Type-coercion map (facet
     *       evaluate_arg_consumption arm B2 — the Qualify_StockSplit face: the coercion composes BEFORE the
     *       {@code .getMulti()} unwrap) + the single-cardinality argument's singletonList coercion family (a
     *       direct fn-call arg hoists + null-guards, PR #368 — the IsActionTypeMODI golden; a bare single input
     *       var coerces in place, W42 #27 PR #436; a present enum constant hoists rule-side, PR #269). A
     *       multi-cardinality Mapper-chain arg keeps driving — its {@code .getMulti()} accessor is the
     *       resolver-supplied tailMulti lever the IR already reproduces.</li>
     * </ul>
     * A meta-annotated callee parameter EXPECTS its wrapper (facet inverseN7CalleeParamMeta, #347) — no coercion,
     * no decline, on either cardinality.
     *
     * <p><b>v3.1 flip seat 30, law 1 rung 2 — the {@code scope} parameter</b> reaches
     * {@link #argItemTypeIsMeta}'s BINDING channel; see that method's javadoc for why a
     * {@code null} scope is byte-safe (diagnostic seats only). (The no-scope 2-arg delegator the twin
     * commit added had no callers and was deleted at the seat-30 review.)
     */
    private PostPinArm evaluateArgWouldCoerce(RExpression arg, RAttribute param,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope) {
        GeneratorModel gm = getGeneratorModel();
        boolean paramMeta = MetaFieldGenerator.detectMetaKind(param) != MetaFieldGenerator.MetaKind.NONE;
        if (gm.isMulti(param)) {
            if (paramMeta) {
                return null; // the wrapper passes through bare — no legacy coercion on a meta multi param
            }
            if (argItemTypeIsMeta(arg, scope)) {
                return PostPinArm.META_ITEM_MULTI_ARG_B2; // the inline bare Type-coercion map before .getMulti()
            }
            // The singletonList family: a SINGLE-cardinality arg into a MULTI param takes a post-pin item→list
            // coercion legacy renders (#368 fn-call hoist / #436 var in-place / #269 enum hoist). The shapes those
            // facets key on are exactly a call, a bare attribute reference, or an enum constant.
            boolean singletonListShape = (arg instanceof RSymbolReference callArg && !callArg.args().isEmpty())
                    || (arg instanceof RSymbolReference bareRef && bareRef.args().isEmpty()
                            && bareRef.symbol().orElse(null) instanceof RAttribute)
                    || arg instanceof REnumValueRef;
            if (singletonListShape) {
                RWorkspace ws = gm.workspace();
                ExpressionCardinality argCard = ws == null ? null : ws.getCardinality(arg);
                return argCard != ExpressionCardinality.MULTI ? PostPinArm.SINGLETON_LIST_ARG : null;
            }
            return null;
        }
        if (arg instanceof RIntLiteral) {
            // Arm 1: within-long → the inline BigDecimal.valueOf(<lit>) (M2, PR #300); beyond-long → the
            // BigInteger→BigDecimal hoist (b, PR #128). Both fire on a BigDecimal param; the IR renders neither.
            RType paramRType = gm.getType(param);
            if (paramRType == null || paramRType instanceof RMissingType) {
                return null;
            }
            JavaClass<?> paramJavaType = getTypeTranslator().toJavaReferenceType(paramRType);
            return paramJavaType != null && getTypeUtil().isBigDecimal(paramJavaType)
                    ? PostPinArm.INT_LITERAL_ARG : null;
        }
        // Arm 2: the meta-wrapper hoist family (single param). A meta-annotated callee param EXPECTS the wrapper —
        // legacy passes it bare (facet inverseN7CalleeParamMeta, #347), so only a meta-FREE param can trigger it.
        if (paramMeta) {
            return null;
        }
        return argItemTypeIsMeta(arg, scope) ? PostPinArm.META_WRAPPER_SINGLE_ARG : null;
    }

    /**
     * The argument-side meta probe shared by the single-param hoist arm and the multi-param B2 arm: the arg's item
     * type resolves to a meta wrapper through the SAME typing channels legacy cascades when the compiled type is
     * absent — the alias walk, the list-op terminal walk, the implicit-item walk, the then-pipe BINDING channel
     * — plus the direct meta-annotated
     * terminal feature (legacy's compiled-type channel surfaces that wrapper via {@code metaNavResultType}; the
     * adapter's meta gate declines those args from lowering today, so that arm is a belt for future admissions).
     *
     * <p><b>v3.1 flip seat 30, law 1 rung 2 — the LAW-77 twin.</b> The fourth channel is the one legacy's
     * cascade gained at {@code ReferenceHandler.tryMetaDerefArg}: when the three AST walks decline, read the
     * then-step BINDING via {@link HandlerHelper#bareItemThenPipeMetaType} (the #362 render-truth channel).
     * Without it this mirror answered "not meta" for a then-arg piped from a META ALIAS — all three walks
     * bottom out in {@code recoverMetaFromExpr}, which has no alias-SIGNATURE arm — so the post-pin guard did
     * not trip, the IR rendered the call itself, and the ON route emitted the INLINE lambda while OFF emitted
     * golden's block (drr {@code ReportablePricePeriod} ×4: OFF healed WHOLE, ON healed the DECL hunk only).
     * LAW 77 is that the SAME predicate lives at EVERY claim seat; this is that predicate, not a route fork.
     *
     * <p>{@code scope} is {@code null} only at the DIAGNOSTIC seats ({@code condVisitLower},
     * {@code lambdaVisitLower}, {@code inputNavRootFacet}, {@code l111ResidueFacet},
     * {@code classifyArgGapGateBlocker}, {@code classifyOpResGateBlocker}), which build facet LABELS and census
     * rows and never decide a render — a null scope there can only UNDER-count this arm on the meter, never
     * move a byte. All three RENDER-deciding seats ({@code tryEmitFromIR}'s claim-root probe and its post-pin
     * guard, {@code visitEnumValueRef}'s nav-lower gate) pass the live {@code ctx.scope()}, so the invariant
     * {@link #subtreeTripsPostPinCoercion}'s javadoc names — that the claim-root probe and the scan "cannot
     * disagree" — holds exactly where bytes are decided. (The twin commit also shipped a no-scope 1-arg
     * delegator carrying this javadoc; it never had a caller, and the paragraph above documented a
     * {@code scope} parameter it did not declare, so it was deleted at the seat-30 review and the whole
     * javadoc moved here, onto the method that actually takes {@code scope}.)
     */
    private boolean argItemTypeIsMeta(RExpression arg,
            com.regnosys.rosetta.generator.java.scoping.JavaStatementScope scope) {
        JavaType argMapperType = NavigationHandler.tryAliasReceiverMapperType(arg, this);
        if (argMapperType == null) {
            argMapperType = NavigationHandler.tryTerminalMetaMapperType(arg, this);
        }
        if (argMapperType == null) {
            argMapperType = NavigationHandler.implicitItemArgMeta(arg, this);
        }
        if (argMapperType == null && scope != null && getTypeUtil() != null) {
            // seat 30 law 1 rung 2's twin — the BINDING channel, in legacy's own cascade position
            // (behind implicitItemArgMeta, ahead of the belts below). MapperS-wrapped to match the
            // sibling it falls back from; only getItemType is read here, so the kind is presentational.
            RJavaWithMetaValue pipedItemMeta =
                    HandlerHelper.bareItemThenPipeMetaType(arg, scope, this);
            if (pipedItemMeta != null) {
                argMapperType = getTypeUtil().wrap(getTypeUtil().MAPPER_S, pipedItemMeta);
            }
        }
        if (argMapperType != null && getTypeUtil().getItemType(argMapperType) instanceof RJavaWithMetaValue) {
            return true;
        }
        // The #469 Seat-1 OBS-1 belt: a bare implicit-item ARG also probes this compiler's OWN
        // then-pipe-threading owner-chain walk — the recovery walk above can null on shapes legacy still
        // coerces via its live compiled type (the #467 CompareTradeLotToAmount class), and since the
        // itemCallArg teach retired the wholesale implicit-var-arg decline this position is newly exposed
        // to that gap (the #467 retro law: never conclude non-meta at the first unresolvable terminal).
        // Over-declining is byte-safe by construction; corpus-absent today (the cpON receipts), so this is
        // a belt against future corpus movement, not a live carrier.
        if (arg instanceof RImplicitVariable item && itemOwnerChainTerminalIsMeta(item)) {
            return true;
        }
        return arg instanceof RFeatureCall fcArg
                && fcArg.resolvedFeature().isPresent()
                && isMetaAnnotated(fcArg.resolvedFeature().get());
    }

    /**
     * Compute the Java-emission facts an {@code ALIAS} reference needs and render it via
     * {@link IRJavaLeafEmitter#emitAlias} — {@code aliasName(input1, input2, …)} — or, for the
     * #381 W-facet class ({@link #aliasTakesOutputBuilderNavRender}), via
     * {@link IRJavaLeafEmitter#emitAliasOutputBuilderNav} — the
     * {@code MapperS.of(aliasName(outputName.toBuilder(), …).build())} wrap (the #531 teach).
     * The enclosing function supplies the inputs (declaration order, the alias method
     * signature's parameter order), the dependency-collision check
     * ({@code disambiguateAliasInvocation}'s {@code "1"} suffix) and — for the W-facet — the
     * output name (the builder receiver). Declines (→ legacy, which logs + emits an empty arg
     * list) when the enclosing function cannot be resolved, and for the corpus-absent
     * {@link #aliasTakesStaticOperatorEscape} belt.
     */
    private Optional<JavaStatementBuilder> tryEmitAlias(IRReference alias, RExpression expr) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing == null) {
            wAliasCensus("wAlias.enclosingNull.declined");
            return Optional.empty();
        }
        List<String> inputNames = aliasCallInputNames(enclosing);
        boolean collidesWithDependency =
                FunctionDependencyCollector.hasFunctionDependencyNamed(enclosing, alias.target());
        if (aliasTakesStaticOperatorEscape(enclosing, alias.target(), collidesWithDependency)) {
            wAliasCensus("wAlias.escape.declined");
            return Optional.empty();
        }
        if (aliasTakesOutputBuilderNavRender(enclosing, alias.target())) {
            // The #531 teach — the LAST decline taught, the walk's 100% close: render the #381
            // W-facet wrap natively from the SAME three facts the census names (the output name
            // via the predicate's own non-null-proven read, the collision bit, the input list)
            // — byte-identical to legacy's aliasOutputBuilderNav arm, which composes from
            // exactly these (ReferenceHandler's W-arm string, the MAPPER_S ref, the null
            // expression type). The census fate segment reads lowered at the same mint seat
            // the pre-teach probe read declined — the flip IS the meter movement.
            String outputName = enclosing.output().map(RAttribute::name).orElse(null);
            wAliasCensus("wAlias.w.lowered.out:" + outputName
                    + ".collide:" + (collidesWithDependency ? 1 : 0)
                    + ".inputs:" + inputNames.size());
            return Optional.of(leafEmitter.emitAliasOutputBuilderNav(
                    alias.target(), collidesWithDependency, outputName, inputNames));
        }
        wAliasCensus("wAlias.bare.lowered.inputs:" + inputNames.size());
        return Optional.of(leafEmitter.emitAlias(alias.target(), collidesWithDependency, inputNames));
    }

    /** The #531 W-facet belt census mint — probe-gated at the single seat (the map field's javadoc). */
    private void wAliasCensus(String token) {
        if (blockerProbeEnabled) {
            wAliasGateFacets.merge(token, 1, Integer::sum);
        }
    }

    /**
     * The #381 W-facet gate (the #502 probe2 {@code NewEquitySwapProduct} catch): an
     * OUTPUT-ROOTED disguised-chain alias renders through legacy's aliasOutputBuilderNav arm
     * (PR #381 — {@code MapperS.of(payout(product.toBuilder(), …).build())}: the output builder
     * threaded as the FIRST argument and the Builder-returning call wrapped back into the Mapper
     * world), gated by the SAME-WALK {@code FunctionAliasHelper.isOutputBuilderNavAlias}
     * predicate at the declaration AND every call site. From #502 to #530 this was a
     * DECLINE belt at both alias render channels (the IR's {@code emitAlias} renders only the
     * bare {@code aliasName(inputs)} form); <strong>since the #531 teach the TOP-LEVEL channel
     * renders the wrap natively</strong> ({@link IRJavaLeafEmitter#emitAliasOutputBuilderNav}
     * — the walk's LAST decline taught, closing the 100% walk), while the OPERAND channel
     * ({@link #buildAliasResolver}) keeps the decline: corpus-absent there (the
     * {@code wAliasOperand} census token reads ZERO — outer claims over a W-alias are
     * frontier-served whole before any operand recursion reaches the resolver), and while
     * legacy's W-arm fires at operand seats too, an IR operand mirror would be
     * golden-unwitnessable today (no corpus carrier exercises one — the Seat-1 #531 OBS-3
     * precision), so the belt declines to legacy — byte-safe either way — and guards future
     * corpus movement. Live since the #502 resolve-by-name arm: the W-facet carriers' call-site
     * references are symbol-EMPTY (legacy's own name-based fallback lookup serves them — the
     * {@code ReferenceHandler} W-arm's null-target branch), so the class entered the
     * IR-attempted set exactly with that arm.
     */
    private static boolean aliasTakesOutputBuilderNavRender(RFunction enclosing, String target) {
        String outputName = enclosing.output()
                .map(RAttribute::name).orElse(null);
        if (outputName == null || target == null) {
            return false;
        }
        for (RShortcut shortcut : enclosing.shortcuts()) {
            if (target.equals(shortcut.name())) {
                return com.regnosys.rosetta.generator.java.function.FunctionAliasHelper
                        .isOutputBuilderNavAlias(shortcut.expression(), outputName);
            }
        }
        return false;
    }

    /**
     * The Seat-1 #470 OBS-1 belt: legacy {@code disambiguateAliasInvocation} applies a SECOND post-pin
     * refinement beyond the dep-collision {@code "1"} suffix — facet aliasStaticImportEscape (PR #420,
     * post-lab-pin): when the invocation name (post-suffix) collides with an
     * {@code ExpressionOperatorsNullSafe} member the rendered class references through its static wildcard
     * import, BOTH the alias method declaration AND every call render {@code _<name>}. The IR's
     * {@code emitAlias} has no mirror for the escape, so a colliding alias must DECLINE to legacy (which
     * renders the escape at both seats). Corpus-absent on the IR-attempted set today (the 55/55 ≡ SOT rings;
     * a hit would also be non-compiling — the legacy-rendered declaration carries the underscore); the belt
     * guards future corpus movement, not a live carrier. Checked at BOTH alias render channels
     * ({@link #tryEmitAlias} + {@link #buildAliasResolver}) — a seat-local render decline, not a guard arm.
     */
    private static boolean aliasTakesStaticOperatorEscape(RFunction enclosing, String target,
            boolean collidesWithDependency) {
        String invocationName = collidesWithDependency ? target + "1" : target;
        return HandlerHelper.staticOperatorMembersUsed(enclosing).contains(invocationName);
    }

    /**
     * The alias-invocation ARG-NAME list for an enclosing function — the input OWNER's declared inputs MINUS
     * the {@code __synthesized_input__} placeholder the AstBuilder adds to an input-less function (legacy facet
     * zeroInputAliasCallArgs, W42 #14 PR #426: the alias METHOD signature already filters it, so the CALL must
     * render the same empty list). Inside a dispatch VARIANT the input owner is the dispatch BASE (facet
     * dispatchVariantParamResolution #369, taught at #470): the variant's own {@code inputs()} is the
     * placeholder, while the alias METHOD signature takes the BASE's declared inputs (legacy
     * {@code FunctionGenerator} threads the base as {@code signatureSource}) — so the call forwards the base's
     * names, mirroring legacy {@code ReferenceHandler.renderEnclosingInputs}'s base swap byte-for-byte (golden
     * {@code endDate(calculationPeriod, priorCalculationPeriod, calculateRelativeTo, resetDates)}). Every
     * non-variant caller keeps the enclosing function's own list ({@code dispatchBaseOf} is {@code null} there
     * without walking — the universal fast path stays allocation-light).
     *
     * <p>facet aliasCallInputEscape (v3.1 flip seat 32, law E.2): the names are the EMITTED render
     * names, so an input the declaration seat {@code "_"}-escapes is forwarded escaped — golden
     * {@code party1(counterparties, _partyLei)} against our {@code party1(counterparties, partyLei)}
     * (drr 7.0.0-7.3.0 {@code CounterpartyRoleFromLEI}, whose {@code @Inject protected PartyLei
     * partyLei} takes the raw name). This method is the IR TWIN of legacy
     * {@code ReferenceHandler.renderEnclosingInputs} — a seat the IR route RE-IMPLEMENTS, so a
     * handler-side fix does NOT reach it by inheritance (LAW 77) — and it reads the SAME table
     * legacy reads: {@code FunctionGenerator.escapedFunctionInputName} (facet
     * fnInputDepCollisionEscape, PR #436) against the PRE-swap {@code enclosing}. The two owners
     * are deliberately different: the arg NAMES come from the dispatch BASE, the escape is decided
     * against the EMITTED function, exactly as the declaration seat decides it
     * ({@code FunctionGenerator:970} passes {@code func}, not the signature source).
     * Off-collision the helper returns the raw name, so every non-escaping caller is
     * byte-identical.
     */
    private static List<String> aliasCallInputNames(RFunction enclosing) {
        RFunction dispatchBase = HandlerHelper.dispatchBaseOf(enclosing);
        RFunction inputOwner = dispatchBase != null ? dispatchBase : enclosing;
        return inputOwner.inputs().stream()
                .filter(input -> !com.regnosys.rosetta.generator.java.function.FunctionGenerator
                        .isSynthesizedInput(input))
                .map(input -> com.regnosys.rosetta.generator.java.function.FunctionGenerator
                        .escapedFunctionInputName(enclosing, input.name(), enclosing.name()))
                .toList();
    }

    /**
     * The enclosing function's INPUT-name escape table for one emission — the THIRD consumer of
     * facet aliasCallInputEscape (v3.1 flip seat 32, law E.2), and the fact
     * {@code IRJavaLeafEmitter.emitVariable}'s bare {@code PARAM} arm cannot compute for itself
     * (the emitter holds no {@link RFunction}; the escape is a function-scope decision). Built from
     * the SAME source of truth the other two seats read
     * ({@code FunctionGenerator.escapedFunctionInputName}), over the SAME input owner
     * {@link #aliasCallInputNames} uses (the dispatch BASE inside a variant) and against the SAME
     * escape owner (the PRE-swap enclosing function — the EMITTED class).
     *
     * <p>Returns {@code null} — no hook installed — for every function whose inputs all keep their
     * raw names, which is the overwhelming majority; and the table is the identity outside the
     * escape population. So a null hook and an installed one render the same bytes everywhere the
     * law does not apply, and the reach is exactly the escape population. Its only corpus carrier
     * at this head is {@code CounterpartyRoleFromLEI} x drr 7.0.0-7.3.0: golden
     * {@code MapperS.of(_partyLei)} against our {@code MapperS.of(partyLei)} — a render that
     * COMPILES (upstream {@code areEqual} takes {@code Mapper<T>, Mapper<U>} and the bare name
     * binds the {@code @Inject} field), so it is invisible to javac and visible only to the byte
     * gate.
     */
    private static java.util.function.UnaryOperator<String> buildInputEscape(RExpression expr) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing == null) {
            return null;
        }
        RFunction dispatchBase = HandlerHelper.dispatchBaseOf(enclosing);
        RFunction inputOwner = dispatchBase != null ? dispatchBase : enclosing;
        Map<String, String> escaped = new HashMap<>();
        for (RAttribute input : inputOwner.inputs()) {
            if (com.regnosys.rosetta.generator.java.function.FunctionGenerator
                    .isSynthesizedInput(input)) {
                continue;
            }
            String raw = input.name();
            String esc = com.regnosys.rosetta.generator.java.function.FunctionGenerator
                    .escapedFunctionInputName(enclosing, raw, enclosing.name());
            if (!esc.equals(raw)) {
                escaped.put(raw, esc);
            }
        }
        return escaped.isEmpty() ? null : name -> escaped.getOrDefault(name, name);
    }

    /**
     * Build the {@link AliasOperandResolver} for a top-level emission — the Java-emission facts a nested
     * {@code ALIAS} operand needs (an alias reaches the generic emit recursion ONLY as an arithmetic operand).
     * Captures the enclosing function (the alias helper-method's inputs + the dependency-collision check) and
     * classifies each alias's numeric kind by recursing the shortcut body through the legacy
     * {@code HandlerHelper.numericOperandKind} — the oracle's own classifier, NOT a re-implemented inference
     * (so it is byte-faithful and sidesteps the unimplemented shortcut type inference). Returns {@code null}
     * when there is no enclosing function (a non-function seam), so every alias declines to legacy.
     */
    private AliasOperandResolver buildAliasResolver(RExpression expr) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        if (enclosing == null) {
            return null;
        }
        List<String> inputNames = aliasCallInputNames(enclosing);
        return alias -> {
            RShortcut shortcut = enclosing.shortcuts().stream()
                    .filter(s -> s.name().equals(alias.target()))
                    .findFirst()
                    .orElse(null);
            if (shortcut == null) {
                return null; // the enclosing function does not declare this alias → decline
            }
            boolean collidesWithDependency =
                    FunctionDependencyCollector.hasFunctionDependencyNamed(enclosing, alias.target());
            if (aliasTakesStaticOperatorEscape(enclosing, alias.target(), collidesWithDependency)) {
                return null; // the #420 static-import escape has no IR mirror — decline (Seat-1 #470 OBS-1)
            }
            if (aliasTakesOutputBuilderNavRender(enclosing, alias.target())) {
                wAliasCensus("wAliasOperand.w.declined"); // the operand-channel mirror token (#531)
                // The #502 belt, kept at the OPERAND channel after the #531 top-level teach:
                // corpus-absent here (the census token reads ZERO — outer claims over a
                // W-alias are frontier-served whole before any operand recursion), and an IR
                // operand mirror would be golden-unwitnessable today (legacy's W-arm fires at
                // operand seats too, but no corpus carrier exercises one — the Seat-1 #531
                // OBS-3 precision), so the decline falls whole to legacy, byte-safe.
                return null;
            }
            return new AliasOperandResolver.Facts(
                    collidesWithDependency, inputNames, aliasNumericWitness(shortcut.expression()));
        };
    }

    /**
     * Build the {@link CallReceiverResolver} for a top-level emission — the {@code @Inject} receiver field name a
     * function CALL ({@link com.regnosys.rosetta.ir.expr.IRApply}) needs to render {@code <receiver>.evaluate(...)}.
     * Byte-identical to legacy {@code ReferenceHandler.disambiguateDependencyReceiver}'s FULL precedence since the
     * #469 dep-numbering teach: (1) the same-simple-name FUNCTION-dependency collision numbering (facet
     * injectDepCollision #364 — {@code commodityLeg10}/{@code commodityLeg11}, keyed by the callee's
     * generated-class FQN via the collector's own
     * {@link FunctionDependencyCollector#collidingFunctionDependencyNumbering} map), then (2) the dep-vs-shortcut
     * {@code "0"} suffix, then (3) the bare lowerCamelCase of the callee's simple name.
     *
     * <p><strong>The numbering correlation (the #469 teach):</strong> the numbering map is FQN-keyed, but the
     * neutral {@link IRReference} carries only the callee's SIMPLE name — the colliding callees are
     * indistinguishable from the IR node alone. The compiler correlates by SOURCE RANGE instead: the adapter
     * stamps the call site's own {@code sourceRange()} onto the callee reference, so a pre-walk of the claimed
     * AST subtree indexes each args-present call site's resolved {@link RFunction} by that exact range (a
     * value-equal record, unique per physical call site), and the resolver recovers the precise callee. The
     * range-indexed path is built ONLY when the enclosing function's numbering map is non-empty — the universal
     * no-collision function keeps the allocation-light closure (legacy's own cost discipline, mirroring the
     * memoized oracle) — and an uncorrelated callee inside a numbered function returns {@code null} (→ the call
     * declines to legacy; never guess a receiver where two same-named candidates exist).
     *
     * <p>When there is no enclosing function (a non-function seam) no shortcut or dependency group can collide,
     * so the bare lowerCamelCase name is returned (still byte-correct).
     */
    private CallReceiverResolver buildCallReceiverResolver(RExpression expr) {
        RFunction enclosing = HandlerHelper.findEnclosingFunction(expr);
        Map<String, FunctionDependencyCollector.NumberedDependency> numbering = enclosing == null
                ? Map.of()
                : FunctionDependencyCollector.collidingFunctionDependencyNumbering(
                        enclosing, getGeneratorModel(), getTypeTranslator());
        if (!numbering.isEmpty()) {
            Map<SourceRange, RFunction> calleesByRange = new HashMap<>();
            indexFunctionCallSites(expr, calleesByRange);
            return callee -> {
                RFunction fn = calleesByRange.get(callee.sourceRange());
                if (fn == null) {
                    return null; // an uncorrelated callee inside a numbered function — decline, never guess
                }
                String fqn = getTypeTranslator()
                        .toFunctionJavaClass(getGeneratorModel().symbolId(fn))
                        .getCanonicalName().withDots();
                FunctionDependencyCollector.NumberedDependency numbered = numbering.get(fqn);
                if (numbered != null) {
                    return numbered.fieldName();
                }
                String bare = FunctionDependencyCollector.lowerCamelCase(fn.name());
                if (FunctionDependencyCollector.hasShortcutNamed(enclosing, bare)) {
                    return bare + "0";
                }
                return bare;
            };
        }
        return callee -> {
            String bare = FunctionDependencyCollector.lowerCamelCase(callee.target());
            if (enclosing != null && FunctionDependencyCollector.hasShortcutNamed(enclosing, bare)) {
                return bare + "0";
            }
            return bare;
        };
    }

    /**
     * Index every args-present function call site in the claimed subtree by its own {@link SourceRange} — the
     * correlation key the adapter stamps on the callee {@link IRReference} (the #469 dep-numbering teach). The
     * gate ({@code RSymbolReference} + args + resolved {@link RFunction}) is a SUPERSET of the adapter's claim
     * gate (which additionally requires {@code Origin.FUNCTION} etc.), so every callee reference the emitter can
     * ask about is indexed; extra entries are never queried.
     */
    private static void indexFunctionCallSites(RNode node, Map<SourceRange, RFunction> out) {
        if (node instanceof RSymbolReference call
                && !call.args().isEmpty()
                && call.symbol().orElse(null) instanceof RFunction callee) {
            // The point-free twin's ambiguity poisoning, swept to the class (the Copilot
            // #492 catch): two DISTINCT callees sharing one range key (the materialized-node
            // SourceRange.NONE default being the realistic collider) poison to null — the
            // consumers' null-check declines, never guesses. Same-callee re-keys stay benign.
            SourceRange key = call.sourceRange();
            if (out.containsKey(key) && out.get(key) != callee) {
                out.put(key, null); // ambiguous key — poisoned; lookups decline, never guess
            } else {
                out.put(key, callee);
            }
        }
        for (RNode child : node.children()) {
            indexFunctionCallSites(child, out);
        }
    }

    /**
     * #494 — build the construct renderer for this emission: under the adapter's ROOT-ONLY law the
     * only lowered {@link com.regnosys.rosetta.ir.expr.IRConstruct} is the claim root itself, so
     * the renderer is the root-site fast path (the Copilot R4 suppressed-note fix — no subtree
     * pre-walk, no site map): serve the node ONLY when its {@link SourceRange} equals the claim
     * root's own (the correlate-or-decline honesty kept — a foreign node, impossible this wave,
     * declines rather than guesses), by calling {@code super.visitConstructor(site, ctx)} — the
     * LITERAL legacy fallback (one line: {@code constructionHandler.handle(expr, ctx, this)}),
     * byte-identical to the decline path by the strongest argument. Null for a non-constructor
     * claim root (the frame slot additionally gates on the lowered kind). The deep-enrichment
     * wave (CONSTRUCT nodes away from the claim root) must reintroduce the #479
     * range-collision-poisoning subtree index — {@link #indexPointFreeSites} is the living
     * template — alongside the guard-exemption re-proof it already owes.
     */
    private ConstructRenderer buildConstructRenderer(RExpression expr, ExpressionContext ctx) {
        if (!(expr instanceof RConstructorExpr rootSite)) {
            return null;
        }
        return construct -> {
            if (!rootSite.sourceRange().equals(construct.sourceRange())) {
                return null; // not the claim root — decline, never guess
            }
            return super.visitConstructor(rootSite, ctx);
        };
    }

    /**
     * #495 — build the conditional renderer for this emission: the root-site fast path, the #494
     * construct pattern EXACTLY (no subtree pre-walk, no site map — a lowered
     * {@link com.regnosys.rosetta.ir.expr.IRConditional} is only ever consumed at its own claim
     * root, the emitter's kind-gated frame slot enforcing it): serve the node ONLY when its
     * {@link SourceRange} equals the claim root's own (correlate-or-decline — a foreign node
     * declines rather than guesses), by calling {@code super.visitConditional(site, ctx)} — the
     * LITERAL legacy fallback (routing to {@code controlFlowHandler.handle(expr, ctx, this)}),
     * byte-identical to the decline path by the strongest argument, statement hoists included (the
     * oracle registers them itself; the condition/branch compiles re-enter this compiler through
     * the oracle's own dispatch, so interior nodes claim at their own roots — a #495-admitted
     * ctor slot is render-inert here). Null for a non-conditional claim root (the frame slot
     * additionally gates on the lowered kind). A future wave consuming conditionals AWAY from the
     * claim root must reintroduce the #479 range-collision-poisoning subtree index —
     * {@link #indexPointFreeSites} is the living template — alongside the guard-exemption
     * re-proof it already owes (the same standing obligation the #494 construct wave pinned).
     */
    private ConditionalRenderer buildConditionalRenderer(RExpression expr, ExpressionContext ctx) {
        if (!(expr instanceof RConditionalExpr rootSite)) {
            return null;
        }
        return conditional -> {
            if (!rootSite.sourceRange().equals(conditional.sourceRange())) {
                return null; // not the claim root — decline, never guess
            }
            return super.visitConditional(rootSite, ctx);
        };
    }

    /**
     * #496 — build the lambda-op renderer for this emission: the ROOT-SITE FAST PATH exactly as
     * {@link #buildConditionalRenderer} (no subtree pre-walk, no site map — under the frame-slot
     * kind gate an {@link IRLambdaOp} renderer is only ever built on a lambda claim root). The
     * builder captures the claim root's own {@link RExtractExpr}/{@link RFilterExpr}; the
     * renderer serves the node only when its range equals that root's (correlate-or-decline) and
     * the render is the LITERAL legacy fallback for the site's own family —
     * {@code super.visitExtract(site, ctx)} / {@code super.visitFilter(site, ctx)} — byte-identical
     * at every exit by the strongest argument (see {@link LambdaOpRenderer}).
     */
    private LambdaOpRenderer buildLambdaOpRenderer(RExpression expr, ExpressionContext ctx) {
        if (!(expr instanceof RExtractExpr) && !(expr instanceof RFilterExpr)) {
            return null;
        }
        return lambdaOp -> {
            if (!expr.sourceRange().equals(lambdaOp.sourceRange())) {
                return null; // not the claim root — decline, never guess
            }
            return expr instanceof RExtractExpr extractSite
                    ? super.visitExtract(extractSite, ctx)
                    : super.visitFilter((RFilterExpr) expr, ctx);
        };
    }

    /**
     * #492 — build the point-free renderer for this emission: a pre-walk indexes every bare no-arg
     * function reference in the claimed subtree by its own {@link SourceRange} (the correlation key
     * the adapter stamps on the {@link com.regnosys.rosetta.ir.expr.IRPointFreeApply} node — the
     * same #469 dep-numbering correlation), and the renderer serves the node by calling legacy
     * {@code ReferenceHandler.renderImplicitFunctionInvocation(rawNode, callee, ctx, this)}
     * VERBATIM — the same public D43-seam oracle the top-level point-free claim (in
     * {@code tryEmitFromIR}) reuses, so the implicit-argument derivation, the nav-receiver
     * {@code MapperS.of} wrap (the oracle reads the RAW node's own parent) and the
     * zero-real-input {@code .evaluate()} form are byte-identical to the decline path by the
     * strongest argument. Null when the subtree carries no point-free site (the universal case —
     * allocation-light); an uncorrelated node inside an indexed subtree returns {@code null} from
     * the renderer (→ the claim declines to legacy; never guess an implicit binding).
     */
    private PointFreeRenderer buildPointFreeRenderer(RExpression expr, ExpressionContext ctx) {
        Map<SourceRange, RSymbolReference> sitesByRange = new HashMap<>();
        indexPointFreeSites(expr, sitesByRange);
        if (sitesByRange.isEmpty()) {
            return null;
        }
        return pointFree -> {
            RSymbolReference site = sitesByRange.get(pointFree.sourceRange());
            if (site == null || !(site.symbol().orElse(null) instanceof RFunction callee)) {
                pointFreeRenderFaces[0]++; // uncorrelated — decline, never guess
                return null;
            }
            // #492: the META-item implicit-arg face declines at NESTED positions BEFORE rendering
            // — the oracle's in-lambda item compile inserts the meta deref and REGISTERS a
            // numbered scope hoist (`final ReferenceWithMetaX xN = item.get();`), a side effect a
            // SPECULATIVE emission must never leak: a later-gate decline leaves the registration
            // behind and legacy's re-render mints its own number (the leg-2 ring's one residue
            // carrier, CountryAndProvinceOrTerritoryOfIndividualRule, read hoists 0/1/2 where
            // golden reads 0/1 — the leaked speculative registration EMITTED unused). At the
            // CLAIM ROOT the render is NEVER speculative — emitPointFree is the last gate, the
            // returned render is used unconditionally (exactly the pre-#492 L-109 belt's
            // byte-green behavior on these same sites) — so the guard scopes to nested positions
            // only (the first cut's root-wide gate flipped the belt's driven mass to leafEmitter
            // declines: -3,281 driven, +4,657 leafEmitter on the cpON6 pre-fix reading). Two
            // probes OR-composed, prove-or-decline: the list-op owner-chain walk + the site-side
            // pipe-element walk (a then-pipe's element is its BODY's leaf, which the first walk's
            // chain-source heuristic cannot see; an UNRESOLVABLE element declines — the leak is
            // invisible to the bytes until it fires). Over-declining is byte-safe (the claim
            // falls back to legacy WHOLE, where the hoist numbering is legacy's own). The leg-2
            // refinement widens the site walk's FALSE-provable set (rule/function/alias-bodied
            // elements, value-producing shapes — the arm inventory at elementTerminalMeta) and
            // decomposes every decline by probe face: the chain probe runs even when the site
            // walk decides, so the `chainMeta.siteFalse` face counts the conflict class (the
            // chain heuristic's extract-argument over-read vs a site-proven plain element —
            // still DECLINED, the verdict is unchanged; the face sizes a possible reorder).
            if (site != expr) {
                String[] unresolvedFace = new String[1];
                Boolean elementMeta = siteImplicitItemMeta(site, unresolvedFace);
                boolean chainMeta = itemOwnerChainTerminalIsMeta(site);
                if (chainMeta || !Boolean.FALSE.equals(elementMeta)) {
                    pointFreeRenderFaces[1]++; // metaGate — the nested hoist-leak guard
                    String siteFace = Boolean.TRUE.equals(elementMeta) ? "elementMeta"
                            : Boolean.FALSE.equals(elementMeta) ? "siteFalse"
                            : "unres." + (unresolvedFace[0] == null ? "walk" : unresolvedFace[0]);
                    metaGateFaces.merge(chainMeta ? "chainMeta." + siteFace : siteFace,
                            1, Integer::sum);
                    return null;
                }
            }
            JavaStatementBuilder rendered =
                    getReferenceHandler().renderImplicitFunctionInvocation(site, callee, ctx, this);
            // The belt behind the meta gate: a STATEMENT-COMPOSITE render (any residual
            // hoist-carrying face) declines on shape — a plain-expression render carries no
            // cross-statement scope state.
            if (!(rendered instanceof JavaExpression)) {
                pointFreeRenderFaces[2]++; // nonExpression — the composite belt
                return null;
            }
            pointFreeRenderFaces[3]++; // rendered — the oracle expression served
            return rendered;
        };
    }

    /** #492 — the point-free renderer's face meter (probe channel). */
    private final int[] pointFreeRenderFaces = new int[4];

    /**
     * #492 leg-2 refinement — the metaGate declines decomposed by probe face (sorted at the
     * breakdown print): {@code elementMeta} (site walk proved the element meta), {@code
     * unres.<shape>} (the walk bottomed unresolvable at that shape — the recoverable residue),
     * each optionally {@code chainMeta.}-prefixed when the owner-chain probe ALSO fired;
     * {@code chainMeta.siteFalse} is the conflict class (chain-declined, site-proven plain).
     */
    private final Map<String, Integer> metaGateFaces = new TreeMap<>();

    /**
     * #492 — the point-free renderer's face breakdown for the probe channel:
     * {@code uncorrelated} (no range-correlated raw site — decline) / {@code metaGate} (the
     * nested hoist-leak guard declined; the brace block decomposes the declines by probe face
     * — {@link #metaGateFaces}) / {@code nonExpression} (the composite belt declined) /
     * {@code rendered} (the oracle expression served). The consuming-position split of the
     * post-teach residue — the leafEmitter site channel counts the CLAIMS these declines fall
     * under; this meter counts the renderer's own exits (multiple per claim possible).
     */
    public String pointFreeRenderBreakdown() {
        StringBuilder gateDecomp = new StringBuilder();
        for (Map.Entry<String, Integer> gateFace : metaGateFaces.entrySet()) {
            gateDecomp.append(gateDecomp.length() == 0 ? "{" : ",")
                    .append(gateFace.getKey()).append('=').append(gateFace.getValue());
        }
        return "uncorrelated=" + pointFreeRenderFaces[0]
                + " metaGate=" + pointFreeRenderFaces[1]
                + (gateDecomp.length() == 0 ? "" : gateDecomp.append('}').toString())
                + " nonExpression=" + pointFreeRenderFaces[2]
                + " rendered=" + pointFreeRenderFaces[3];
    }

    /**
     * Index every bare no-arg function-reference site in the claimed subtree by its own
     * {@link SourceRange} — the {@link #indexFunctionCallSites} twin for the point-free family
     * (#492). The gate ({@code RSymbolReference} + empty args + resolved {@link RFunction}) is a
     * SUPERSET of the adapter's claim gate (which additionally declines the shortcut-collision
     * face), so every point-free node the emitter can ask about is indexed; extra entries are
     * never queried. An AMBIGUOUS key — two DISTINCT sites sharing one range, the
     * materialized-node {@link SourceRange#NONE} default being the realistic collider — is
     * POISONED to a {@code null} value (the Copilot #492 catch, closed with the #479
     * range-collision-poisoning pattern): a poisoned lookup reads as uncorrelated
     * and declines to legacy (never guess a binding), while a claim whose subtree carries ONE
     * sentinel-ranged site keeps its benign single-entry correlation — the corpus's
     * parser-materialized point-free population (the probe10 reading: ~834 renders) rides
     * exactly that face.
     */
    private static void indexPointFreeSites(RNode node, Map<SourceRange, RSymbolReference> out) {
        if (node instanceof RSymbolReference ref
                && ref.args().isEmpty()
                && ref.symbol().orElse(null) instanceof RFunction) {
            SourceRange key = ref.sourceRange();
            if (out.containsKey(key) && out.get(key) != ref) {
                out.put(key, null); // ambiguous key — poisoned; lookups decline, never guess
            } else {
                out.put(key, ref);
            }
        }
        for (RNode child : node.children()) {
            indexPointFreeSites(child, out);
        }
    }

    /**
     * #492 — the point-free renderer's site-side META-item probe (the
     * {@link #itemOwnerChainTerminalIsMeta} twin for pipe shapes its chain-source heuristic
     * cannot see): walks up from the SITE to the nearest inline function and probes the
     * ELEMENT source's terminal feature meta-ness through the AST channels — a then-pipe's
     * element is its left pipe's RESULT (the pipe body's leaf feature), filter/sort preserve
     * their argument's element, an extract's element is its body's result. Three-state:
     * {@code TRUE} means the oracle's implicit-item compile would take the meta deref (and
     * register the scope hoist this renderer must never speculatively leak), {@code FALSE} is
     * the provably-non-meta admission, {@code null} is unresolvable ({@code face[0]} carries
     * the bottoming shape for the metaGate decomposition; the caller treats unknown as meta —
     * prove-or-decline). The no-item exits (rule/condition top, no enclosing inline function)
     * read {@code FALSE}: legacy's top-level branches pass {@code evaluate(input)} — no item
     * compile, no hoist (the #261 law).
     */
    private static Boolean siteImplicitItemMeta(RNode site, String[] face) {
        RNode cur = site == null ? null : site.parent();
        for (int guard = 0; cur != null; guard++) {
            if (guard >= 64) {
                // Cap exhaustion is UNKNOWN, not a no-item proof — decline (prove-or-decline;
                // the walked-off-root exit below stays FALSE: no enclosing lambda, no item).
                return unknownFace(face, "climbCap");
            }
            if (cur instanceof RFunction || cur instanceof RRule) {
                // No enclosing inline function — the rule/condition-top branches, no item.
                return Boolean.FALSE;
            }
            if (cur instanceof RInlineFunction inline) {
                RNode owner = inline.parent();
                RExpression element = owner instanceof RExtractExpr extract ? extract.argument()
                        : owner instanceof RFilterExpr filter ? filter.argument()
                        : owner instanceof RSortExpr sort ? sort.argument()
                        : owner instanceof RThenExpr then ? then.argument()
                        : null;
                if ((element == null || element instanceof RImplicitVariable)
                        && owner instanceof RNode ownerNode) {
                    // An ELIDED or IMPLICIT owner argument (the piped `then extract`/`then filter`
                    // forms — incl. the #358 parser-MATERIALIZED implicit variable) — the element
                    // comes from the ENCLOSING pipe: keep climbing.
                    cur = ownerNode;
                    continue;
                }
                // PROVE-OR-DECLINE: only a provably NON-meta element admits (FALSE); a meta or
                // UNRESOLVABLE element declines — the hoist leak is invisible to the bytes until
                // a later-gate decline leaves the registration behind, so unknown must never pass.
                return elementTerminalMeta(element, 0, new HashSet<>(), face);
            }
            cur = cur.parent();
        }
        return Boolean.FALSE;
    }

    /**
     * The element-terminal meta walk behind {@link #siteImplicitItemMeta} — three-state:
     * {@code TRUE} = the element's terminal is meta (the oracle would hoist), {@code FALSE} =
     * provably non-meta through BOTH oracle typing channels, {@code null} = unresolvable (the
     * caller treats unknown as meta — prove-or-decline; {@code face[0]} records the FIRST
     * bottoming shape for the metaGate decomposition, set-once).
     *
     * <p>The leg-2 refinement widens the FALSE-provable set to mirror the ORACLE's own two
     * typing channels seat-by-seat — the live thenArg BINDING (legacy's compiled element java
     * type, {@code ReferenceHandler.enclosingThenArgType}) and the no-binding recovery walk
     * ({@code NavigationHandler.implicitItemArgMeta} → {@code recoverMetaFromExpr}) — so a
     * FALSE guarantees neither channel types the item as a meta wrapper:
     * <ul>
     *   <li><b>bare RULE reference</b> — recurses the delegated rule's BODY exactly like the
     *       recovery's case (c) (cycle-guarded by {@code visitedRules}, mirroring the oracle's
     *       own {@code visited} set). The BINDING channel is plain by construction —
     *       {@code RFunction.fromRule}'s synthetic output carries no meta annotations and the
     *       RuleGenerator back-fill strips the {@code RMetaAnnotatedType} envelope, so every
     *       generated {@code <Name>Rule.evaluate()} returns the bare type (0 wrapper outputs
     *       across the 2,340 drr golden rule classes) — but the recovery channel FOLLOWS the
     *       body, so the proof must too (the bug-compatible law).</li>
     *   <li><b>FUNCTION reference</b> (bare or with args — {@code evaluate()}'s return does
     *       not depend on arity) — proves on the callee's OUTPUT attribute (the return IS the
     *       output's {@code toMetaJavaType}); a bare name colliding with an enclosing
     *       SHORTCUT resolves the shortcut's BODY instead (legacy renders the colliding name
     *       as the alias invocation — the #484 aliasShadow precedence, body-recursed at this
     *       seat because the element question is the alias BINDING's type).</li>
     *   <li><b>alias (shortcut) reference</b> — recurses the alias BODY (the binding compiles
     *       the body with wrappers preserved — the #326/#491 retype family's lever).</li>
     *   <li><b>resolved ENUM constant / literals / value-producing operators / constructor</b>
     *       — plain computed values: no recovery case exists for any of them and the binding
     *       compiles the bare type ({@code with-meta} is the provably-TRUE inverse — the
     *       explicit wrapper constructor).</li>
     *   <li><b>conditional / default</b> — the JOIN law: every value-bearing arm must prove
     *       FALSE (a no-value else — {@link #hasGenuineElseShape} — is neutral, the recovery
     *       case (d) elseless mirror; a MIXED join declines conservatively although rune
     *       joins it to the bare base — over-declining is byte-safe).</li>
     *   <li><b>element-preserving shapes</b> — filter/sort/max/min and the FIRST/LAST/
     *       ONLY_ELEMENT/DISTINCT/REVERSE/FLATTEN list ops flow their argument's element
     *       ({@code sum} is the plain numeric aggregate); a PIPED implicit argument resolves
     *       through {@link #resolveElidedPipedSourceShape} to the enclosing then's argument
     *       (the #481 resolver reused verbatim — the identity {@code extract item} face stays
     *       declined exactly like the sibling predicate's resolver arm).</li>
     * </ul>
     * {@link #isProvablyNonMetaElementSourceShape} is the adapter-side sibling, deliberately
     * NOT reused: its verdicts restate the #479-arm family and are pinned by the adapter's
     * conservation signature — widening it would move those seats; the borrowed details
     * (aliasShadow precedence, the join law, the elided-pipe resolver) are cross-referenced
     * arm by arm instead.
     */
    private static Boolean elementTerminalMeta(RExpression e, int depth, Set<RRule> visitedRules,
            String[] face) {
        if (e == null) {
            return unknownFace(face, "absent");
        }
        if (depth > 32) { // mirrors the oracle's META_RECOVERY_DEPTH_LIMIT budget
            return unknownFace(face, "depthCap");
        }
        // The four ternaries below each pair a PRIMITIVE `boolean` arm (isMetaAnnotatedAttr) with
        // the tri-state `Boolean` `unknownFace` arm (null = unknown); a mixed conditional's type is
        // the primitive, so the null arm was UNBOXED at the return — an NPE that surfaced as a
        // whole-body `/* TODO: expression compilation error … unknownFace … is null */` stub on
        // the IR route (drr 7.x DTCC_UnderlyingAssetReportRule; exposed by seat 20, PR #592). The
        // arms are boxed so the null reaches the caller as the unknown verdict it was designed
        // to be (prove-or-decline; the face recorder still fires).
        if (e instanceof RFeatureCall fc) {
            return fc.resolvedFeature().orElse(null) instanceof RAttribute a
                    ? Boolean.valueOf(isMetaAnnotatedAttr(a)) : unknownFace(face, "fcUnresolved");
        }
        if (e instanceof RDeepFeatureCall deep) {
            // The `->>` leaf: resolved → the leaf attribute's own meta-ness (a meta leaf
            // over-declines if the deep-path getter unwraps — the safe direction).
            return deep.resolvedFeature().orElse(null) instanceof RAttribute a
                    ? Boolean.valueOf(isMetaAnnotatedAttr(a)) : unknownFace(face, "deepFcUnresolved");
        }
        if (e instanceof REnumValueRef evr) {
            if (evr.enumValue().isPresent()) {
                return Boolean.FALSE; // a resolved ENUM constant — plain, never a wrapper
            }
            if (evr.resolvedAttributeChain().isPresent()) {
                RAttribute leaf = evr.resolvedAttributeChain().get().feature();
                return leaf == null ? unknownFace(face, "evrChainLeafless")
                        : Boolean.valueOf(isMetaAnnotatedAttr(leaf));
            }
            RAttribute inputFeature = evr.resolvedInputFeature().orElse(null);
            if (inputFeature != null) {
                return isMetaAnnotatedAttr(inputFeature);
            }
            return unknownFace(face, evr.resolvedSymbol().isPresent() ? "evrSymbol"
                    : evr.resolvedTypeRestriction().isPresent() ? "evrRestrict"
                    : evr.resolvedChoiceOption().isPresent() ? "evrChoice" : "evrBare");
        }
        if (e instanceof RThenExpr then) {
            // The pipe's element = its BODY's result; an implicit/elided body pipes the left arm.
            if (then.body().orElse(null) instanceof RInlineFunction inline && inline.body() != null) {
                return elementTerminalMeta(inline.body(), depth + 1, visitedRules, face);
            }
            return elementTerminalMeta(then.argument(), depth + 1, visitedRules, face);
        }
        if (e instanceof RFilterExpr filter) {
            return elementTerminalMeta(filter.argument(), depth + 1, visitedRules, face);
        }
        if (e instanceof RSortExpr sort) {
            return elementTerminalMeta(sort.argument(), depth + 1, visitedRules, face);
        }
        if (e instanceof RExtractExpr extract) {
            if (extract.body() instanceof RInlineFunction inline && inline.body() != null) {
                // The extracted element — the body's result.
                return elementTerminalMeta(inline.body(), depth + 1, visitedRules, face);
            }
            return unknownFace(face, "extractOpaque");
        }
        if (e instanceof RImplicitVariable) {
            // A PIPED implicit inside the element recursion (a filter/sort/list-op body's own
            // argument) resolves to the enclosing then's argument — the #481 resolver verbatim.
            RExpression resolved = resolveElidedPipedSourceShape(e);
            if (resolved != null && !(resolved instanceof RImplicitVariable)) {
                return elementTerminalMeta(resolved, depth + 1, visitedRules, face);
            }
            if (resolved instanceof RImplicitVariable unresolvedIv
                    && ruleTopInputElementTypeShape(unresolvedIv) != null) {
                // The #485 mirror: a TRUE-noBinder rule-top implicit IS the rule input — the
                // declared from-type instance (the checker's own inferRuleFromType read wraps
                // it withNoMeta; legacy's isElidedOperandTopLevel passes MapperS.of(input)) —
                // a bare data-type instance, never a wrapper on either oracle channel.
                return Boolean.FALSE;
            }
            return unknownFace(face, "implicitUnresolved");
        }
        if (e instanceof RSymbolReference ref) {
            Object sym = ref.symbol().orElse(null);
            if (sym instanceof RAttribute a) {
                return isMetaAnnotatedAttr(a);
            }
            if (sym instanceof RRule rule) {
                if (!visitedRules.add(rule)) {
                    return unknownFace(face, "ruleRevisit");
                }
                return elementTerminalMeta(rule.expression().orElse(null),
                        depth + 1, visitedRules, face);
            }
            if (sym instanceof RFunction calleeFn) {
                if (ref.args().isEmpty()) {
                    RFunction enclosing = HandlerHelper.findEnclosingFunction(ref);
                    if (enclosing != null && nameMatchesShortcutW(enclosing, ref.name())) {
                        for (RShortcut shadow : enclosing.shortcuts()) {
                            if (ref.name().equals(shadow.name())) {
                                return elementTerminalMeta(shadow.expression(),
                                        depth + 1, visitedRules, face);
                            }
                        }
                    }
                }
                RAttribute out = calleeFn.output().orElse(null);
                return out == null ? unknownFace(face, "fnNoOutput")
                        : Boolean.valueOf(isMetaAnnotatedAttr(out));
            }
            if (sym instanceof RShortcut alias) {
                return elementTerminalMeta(alias.expression(), depth + 1, visitedRules, face);
            }
            return unknownFace(face, sym == null ? "symEmpty" : "symOther");
        }
        if (e instanceof RConditionalExpr cond) {
            Boolean thenVerdict =
                    elementTerminalMeta(cond.thenBranch(), depth + 1, visitedRules, face);
            if (!Boolean.FALSE.equals(thenVerdict)) {
                return thenVerdict; // TRUE or unknown — the then arm decides/declines
            }
            if (!hasGenuineElseShape(cond)) {
                return Boolean.FALSE; // no-value else is neutral — the join keeps the then arm
            }
            return elementTerminalMeta(cond.elseBranch().orElse(null),
                    depth + 1, visitedRules, face);
        }
        if (e instanceof RDefaultExpr dft) {
            Boolean leftVerdict =
                    elementTerminalMeta(dft.left().orElse(null), depth + 1, visitedRules, face);
            if (!Boolean.FALSE.equals(leftVerdict)) {
                return leftVerdict;
            }
            return elementTerminalMeta(dft.right().orElse(null), depth + 1, visitedRules, face);
        }
        if (e instanceof RMaxExpr max) {
            return elementTerminalMeta(max.argument(), depth + 1, visitedRules, face);
        }
        if (e instanceof RMinExpr min) {
            return elementTerminalMeta(min.argument(), depth + 1, visitedRules, face);
        }
        if (e instanceof RListOpExpr listOp) {
            if (listOp.op() == ListOp.SUM) {
                return Boolean.FALSE; // the numeric aggregate — plain
            }
            // ONLY_ELEMENT/FLATTEN/DISTINCT/REVERSE/FIRST/LAST — element-preserving picks.
            return elementTerminalMeta(listOp.argument(), depth + 1, visitedRules, face);
        }
        if (e instanceof RListLiteral list) {
            for (RExpression element : list.elements()) {
                Boolean elementVerdict =
                        elementTerminalMeta(element, depth + 1, visitedRules, face);
                if (!Boolean.FALSE.equals(elementVerdict)) {
                    return elementVerdict;
                }
            }
            return Boolean.FALSE; // all elements provably plain (an empty literal has no items)
        }
        if (e instanceof RWithMetaExpr) {
            return Boolean.TRUE; // the explicit wrapper constructor
        }
        if (e instanceof RConstructorExpr || e instanceof REmptyLiteral
                || e instanceof RBooleanLiteral || e instanceof RIntLiteral
                || e instanceof RNumberLiteral || e instanceof RStringLiteral
                || e instanceof RArithmeticExpr || e instanceof RComparisonExpr
                || e instanceof REqualityExpr || e instanceof RLogicalExpr
                || e instanceof RContainsExpr || e instanceof RDisjointExpr
                || e instanceof RJoinExpr || e instanceof RExistenceExpr
                || e instanceof ROnlyExistsExpr || e instanceof RCardinalityCheckExpr
                || e instanceof RCountExpr || e instanceof RToStringExpr
                || e instanceof RConversionExpr) {
            // Value-producing shapes: a constructed instance / literal / boolean-numeric-string
            // operator result is never a meta wrapper — no recovery case exists for any of
            // these and the binding compiles the bare computed type.
            return Boolean.FALSE;
        }
        return unknownFace(face, e.getClass().getSimpleName());
    }

    /** Set-once face recorder for the metaGate decomposition (the FIRST bottoming shape wins). */
    private static Boolean unknownFace(String[] face, String bottomedShape) {
        if (face[0] == null) {
            face[0] = bottomedShape;
        }
        return null;
    }

    /**
     * Build the rule-receiver resolver for a bare RULE delegation reached during this emission — the {@code <Name>Rule}
     * {@code @Inject} field name. Reuses legacy {@code ReferenceHandler.ruleInvocationReceiver} (the
     * {@code RFunction.fromRule} → {@code toFunctionJavaClass} → {@code lowerCamelCase} bridge) to stay byte-identical
     * and to sidestep re-deriving the rule class name; the callee {@link RRule} is captured from the top-level
     * expression's resolved symbol. For the pure-delegation slice the rule-body top-level expression IS the bare rule
     * reference, so {@code expr.symbol()} is exactly the callee rule (a nested/then-chained rule reference is declined
     * by {@link #ruleDelegationDeclines}, so it never reaches the resolver). Returns {@code null} (→ the delegation
     * declines) when the top-level expression is not itself a bare rule reference — keeping a single, unambiguous rule.
     */
    private CallReceiverResolver buildRuleReceiverResolver(RExpression expr) {
        // The args-empty gate is the #498 bare-contract made explicit — the L-045 `input`
        // receiver form serves ONLY the bare delegation (an args-present rule reference renders
        // whole through the RuleApplyRenderer, which derives its own receiver legacy-side).
        if (expr instanceof RSymbolReference ref && ref.args().isEmpty()
                && ref.symbol().orElse(null) instanceof RRule rule) {
            return callee -> ReferenceHandler.ruleInvocationReceiver(rule, this);
        }
        return null;
    }

    /**
     * Build the IN-LAMBDA rule-delegation renderer for a bare RULE delegation reached during this emission (L-049) — the
     * full {@code MapperS.of(<Name>Rule.evaluate(<binding>))} render for a {@code … then SomeRule} reference inside an
     * extract/filter/then lambda, where the polymorphic implicit-input {@code <binding>} ({@code item.get()} /
     * {@code thenArg.get()}) follows the lambda's scope. Rather than re-derive that binding precedence lab-side (a parity
     * risk the recon flagged), the renderer calls legacy {@code ReferenceHandler.renderImplicitRuleInvocation} VERBATIM
     * on this compiler's own {@link ReferenceHandler} (the exact instance the legacy fallback would use), capturing the
     * AST node + the live {@link ExpressionContext} — so the in-lambda delegation is byte-identical to Path-1 by
     * construction. The oracle's in-lambda branch derives the binding via {@code handle(RImplicitVariable)} (a DIRECT
     * legacy call, NOT {@code compiler.compile} — so it never re-enters this IR compiler and adds no spurious
     * &sect;4.2 decline).
     *
     * <p>Returns {@code null} (→ the emitter falls to the {@link #buildRuleReceiverResolver} top-level {@code input}
     * path, undisturbed) for the rule-body TOP-LEVEL case ({@code nearestEnclosingInlineFunction == null}), and for a
     * non-rule top-level expression / a reference with no enclosing rule (both already declined by
     * {@link #ruleDelegationDeclines} before this renderer is consulted). The BARE rule reference IS the top-level
     * expression compiled here — a bare {@code IRApply{RULE}} is never an operand/argument, and since #498 the
     * ARGS-PRESENT shape (which CAN sit in an argument slot) routes to its own {@link RuleApplyRenderer} — so
     * {@code expr.symbol()} is the callee.
     */
    private RuleDelegationRenderer buildRuleDelegationRenderer(RExpression expr, ExpressionContext ctx) {
        if (!(expr instanceof RSymbolReference ref) || !ref.args().isEmpty()
                || !(ref.symbol().orElse(null) instanceof RRule rule)) {
            // Not a BARE rule reference → no in-lambda render. The args-empty gate is the #498
            // bare-contract made explicit: an args-present rule reference takes the
            // RuleApplyRenderer route (its render is the whole-reference oracle, never the
            // renderImplicitRuleInvocation bare form this closure captures).
            return null;
        }
        if (ReferenceHandler.nearestEnclosingInlineFunction(expr) == null) {
            return null; // rule-body top level → the L-045 receiver-resolver `input` render (no oracle re-entry)
        }
        RRule enclosingRule = HandlerHelper.findEnclosingRule(expr);
        if (enclosingRule == null) {
            return null; // also declined by ruleDelegationDeclines (no enclosing rule)
        }
        return () -> Optional.of(
                getReferenceHandler().renderImplicitRuleInvocation(ref, rule, enclosingRule, ctx, this));
    }

    /**
     * Build the {@link RuleApplyRenderer} for an args-present rule invocation claim (the #498 teach)
     * — the ROOT-SITE FAST PATH (the #494 root-only law: no subtree pre-walk, no site map): the
     * builder captures the claim root's own args-present rule {@link RSymbolReference}; the renderer
     * serves the node ONLY when its source range {@link Objects#equals} the root's — NULL-TOLERANT
     * deliberately, because the class's dominant face is the {@code fromRule}/{@code fromReport}
     * factories' SYNTHESIZED wrapper-body calls (range-less, outside any source file). The
     * null≡null match is sound BY ROUTING, not by shape-impossibility: this builder returns
     * non-null only when the claim ROOT is the args-present rule reference, and at that root the
     * emitter whole-renders through the oracle before its walk could reach any interior node —
     * so the renderer is only ever invoked with the root's own node (the range equality trivially
     * true; the check is defense-in-depth for any future non-root consult). A NESTED args-present
     * rule invocation CAN lower into an argument slot ({@code isSimpleCallArg} admits any
     * {@code IRApply}; the #498 census read the interior {@code calleeGate.interior.*} faces
     * live) — under any other claim root the slot is null and that claim declines at the
     * emitter's null-renderer gate, the leafEmitter seat. The
     * render is {@code getReferenceHandler().handle(ref, ctx, this)} — the LITERAL legacy fallback
     * ({@code super.visitSymbolReference} is that one line), same method, same handler instance,
     * same arguments, same virtual dispatch for the arg compiles (the arg re-enters this IR
     * compiler through the oracle's own {@code compiler.compile} — a NORMAL recursion,
     * byte-transparent), so the render is byte-identical to the decline path by the strongest
     * argument. Returns {@code null} for a non-rule-apply top-level expression (→ the frame slot
     * stays empty and a child-position rule call declines at the emitter's null-renderer gate —
     * the leafEmitter seat, the #498 census's interior residue).
     */
    private RuleApplyRenderer buildRuleApplyRenderer(RExpression expr, ExpressionContext ctx) {
        if (!(expr instanceof RSymbolReference ref) || ref.args().isEmpty()
                || !(ref.symbol().orElse(null) instanceof RRule)) {
            return null;
        }
        return apply -> {
            if (!Objects.equals(ref.sourceRange(), apply.sourceRange())) {
                return null; // uncorrelated — never render a site the correlation cannot prove
            }
            return getReferenceHandler().handle(ref, ctx, this);
        };
    }

    /**
     * The #499 metaNav root-site renderer builder (the {@link #buildRuleApplyRenderer} pattern —
     * the #494 root-only law: no subtree pre-walk, no site map). Buildable ONLY when the claim
     * root is itself a meta-annotated feature call (the frame construction's
     * {@code ir instanceof IRMetaAccess} kind gate makes any other build unreachable — belt and
     * braces here); the renderer serves the node only on source-range correlation
     * ({@code Objects.equals} — null-tolerant, sound BY ROUTING exactly as the rule-apply slot:
     * installed only at a meta-access claim root, where the emitter whole-renders through this
     * oracle before its walk could reach any interior node). The render is
     * {@code super.visitFeatureCall(site, ctx)} — the LITERAL L-109d relabel-belt line, so the
     * conversion cannot move a byte; interior nodes re-enter this compiler through the oracle's
     * own recursion and claim at their own roots. On a served render the lowered receipt
     * ({@link #metaNavLoweredCount}) increments — the conversion's own counter, distinct from the
     * belt's {@link #metaNavDrivenCount} residue seat.
     */
    private MetaNavRenderer buildMetaNavRenderer(RExpression expr, ExpressionContext ctx) {
        if (!(expr instanceof RFeatureCall fc)
                || fc.resolvedFeature().filter(IRExpressionCompiler::isMetaAnnotated).isEmpty()) {
            return null;
        }
        return metaAccess -> {
            if (!Objects.equals(fc.sourceRange(), metaAccess.sourceRange())) {
                return null; // uncorrelated — never render a site the correlation cannot prove
            }
            JavaStatementBuilder rendered = super.visitFeatureCall(fc, ctx);
            metaNavLoweredCount++;
            return rendered;
        };
    }

    /**
     * The #500 oracle-root shape gate — the SINGLE predicate driving BOTH the frame construction
     * (install the {@link OracleRootRenderer} slot) and the post-pin guard exemption (the served
     * render IS the guard's own fallback line), so the two can never drift:
     * <ul>
     *   <li>the arm-B family roots — {@link IRConversion} / {@link IRPipe} /
     *       {@link IROnlyExists} / {@link com.regnosys.rosetta.ir.expr.IRListConstruct} (O(1)
     *       kind screens; these kinds have NO native emitter arm, so every root serve is the
     *       oracle and every interior position declines at the emit chain's end) — joined at
     *       #501 by {@link IRSymbolNav} (the arm-C disguised symbol-receiver navigation, the
     *       same no-native-arm property; the census read the kind 100% INTERIOR, so a root-kind
     *       serve is the drift belt) and at #502 by {@link IRClosureParam} (at-root bare
     *       closure-param claims — the args-empty bareSymbolRef dispatch leg) +
     *       {@link IRMetaOutputApply} (382 atRoot / 33 interior at the census — the callArgs
     *       leg);</li>
     *   <li>the oracle-leaf-bearing roots — a {@link FieldAccess} / {@code BinaryOp} /
     *       {@code Existence} / non-rule {@link IRApply} whose lowered subtree contains an
     *       ORACLE LEAF (the O(1) family screen FIRST, then the {@link #containsOracleLeaf}
     *       walk — the #495-R4 root-site fast-path law; the rule callee is EXCLUDED so the #498
     *       {@link RuleApplyRenderer} slot keeps serving its whole class and its receipt
     *       attribution — that render is the same literal line). The #500 leaf was
     *       {@link IRMetaAccess} alone; #501 widens the leaf set to the shallow kinds the
     *       consumer admissions now lower into interior positions ({@link IROnlyExists} /
     *       {@link IRPipe} logical operands, {@link IRConversion}/{@link IRPipe} call args,
     *       {@link IRSymbolNav} receivers/operands/args) — none has a native emitter arm, so
     *       the containing root MUST render whole-legacy or the claim declines.</li>
     * </ul>
     * An oracle-leaf-bearing root OUTSIDE the served families (an {@code IRListOp} collapse
     * over a meta-bearing nav, an {@code IRToString}, an {@code IRLambdaOp}/{@code IRConstruct}/
     * {@code IRConditional} body) is NOT gated here: those roots already render whole-oracle
     * through their own standing slots (the L-050 {@code CollectionOpRenderer} /
     * {@code ToStringRenderer} / the kind-gated root-site renderers), whose handler calls
     * re-walk the raw subtree identically.
     */
    private static boolean isOracleRootShape(IRExpr ir) {
        if (ir instanceof IRConversion || ir instanceof IRPipe || ir instanceof IROnlyExists
                || ir instanceof com.regnosys.rosetta.ir.expr.IRListConstruct
                || ir instanceof IRSymbolNav
                // #502: the closure-param reference (an at-root bare name naming a lambda
                // param — the args-empty RSymbolReference dispatch leg serves it) and the
                // meta-output call (382 atRoot / 33 interior at the census — the callArgs
                // dispatch leg serves the roots; the interiors route through containsOracleLeaf
                // or their standing oracle slots) join the O(1) kind screens by the same
                // no-native-arm property.
                || ir instanceof IRClosureParam || ir instanceof IRMetaOutputApply
                // #503: the ALL/ANY-modified comparison joins the same screens (the at-root
                // claims serve through the standing equality/comparison dispatch legs —
                // super.visitEquality/super.visitComparison, the raw family keying the leg).
                // IRToString deliberately does NOT join the root screens: a top-level
                // to-string keeps its standing ToStringRenderer slot; only the LEAF walk
                // (containsOracleLeaf) carries it for the containing-root routing.
                || ir instanceof IRAllAnyCompare
                // #504 arm-B: the un-retypeable synthetic-item nav joins the O(1) screens
                // (at-root claims — the census's atRoot majority — serve through the
                // standing navChain/bareSymbolRef dispatch legs by raw family; the
                // interior chained hops route through containsOracleLeaf).
                || ir instanceof IRSynItemNav
                // #505: the choice-option nav and the dispatch-base input join the O(1)
                // screens by the same no-native-arm property (at-root claims serve through
                // the new enumChain leg / the standing bareSymbolRef leg by raw family;
                // interiors route through containsOracleLeaf).
                || ir instanceof IRChoiceOptionNav || ir instanceof IRDispatchInputRef
                // #507: the deep-path nav and the record-feature read join the O(1) screens
                // by the same no-native-arm property (deep roots serve through the new
                // deepChain leg, record-read roots through the standing enumChain leg by raw
                // family; interiors route through containsOracleLeaf).
                || ir instanceof IRDeepFeatureNav || ir instanceof IRRecordFeatureNav
                // #508 arm-A5: the meta-sourced USER-item nav joins the O(1) screens by the
                // same no-native-arm property (at-root claims serve through the standing
                // dispatch legs by raw family; interiors route through containsOracleLeaf).
                || ir instanceof IRMetaItemNav
                // #512 arm-B: the receiver-carrying record read and the item-qualifier read
                // join the O(1) screens by the same no-native-arm property (at-root claims
                // — both classes' census atRoot rows — serve through the standing navChain
                // dispatch legs by raw family; interiors route through containsOracleLeaf).
                || ir instanceof IRRecordReceiverNav || ir instanceof IRQualifierItemNav
                // #640 (v3.3 seat 4): the data-rule condition's implicit instance at ROOT (legacy's re-entered
                // parameter reference) - no native arm; the standing bare-ref serve is legacy's own render
                || ir instanceof com.regnosys.rosetta.ir.expr.IRConditionInstance
                // #525: the receiver-carrying qualifier read joins the O(1) screens by the
                // same no-native-arm property (at-root claims — the headUnGate census's
                // 100%-atRoot rows — serve through the standing navChain dispatch leg by
                // raw family; interiors route through containsOracleLeaf).
                || ir instanceof IRQualifierReceiverNav
                // #529: the receiver-carrying choice-option read joins the O(1) screens by
                // the same no-native-arm property (at-root claims — the headAttrGate
                // census's 100%-atRoot rows — serve through the standing navChain dispatch
                // leg by raw family; interiors route through containsOracleLeaf).
                || ir instanceof com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav
                // #513: the noAdaptArm-sweep kinds join the O(1) screens by the same
                // no-native-arm property (at-root claims serve through the new per-family
                // dispatch legs — switchOp/defaultOp/contains/disjoint/maxOp/minOp/sortOp
                // — by raw family; interiors route through containsOracleLeaf).
                || isNoAdaptArmSweepKind(ir)
                // #514: the function-OUTPUT reference, the META-input reference and the
                // top-level rule-input nav join the O(1) screens by the same no-native-arm
                // property (at-root claims serve through the standing bareSymbolRef/
                // enumChain dispatch legs by raw family; interiors route through
                // containsOracleLeaf).
                || ir instanceof IROutputRef || ir instanceof IRMetaParamRef
                || ir instanceof IRRuleInputNav
                // #515: the with-meta annotation and the join concatenation join the O(1)
                // screens by the same no-native-arm property (at-root claims — the
                // untargeted census's whole mass — serve through the new withMetaOp/joinOp
                // dispatch legs by raw family; interiors route through containsOracleLeaf).
                || ir instanceof com.regnosys.rosetta.ir.expr.IRWithMetaOp
                || ir instanceof com.regnosys.rosetta.ir.expr.IRJoinOp
                // #518 arm-B2: the output-consuming alias-head navigation joins the O(1)
                // screens by the same no-native-arm property (at-root claims serve through
                // the standing enumChain/navChain dispatch legs by raw family — at the
                // L-111 relabel seat the #507 identity-serve leg carries the root;
                // interiors route through containsOracleLeaf).
                || ir instanceof com.regnosys.rosetta.ir.expr.IROutputAliasNav
                // #520: the library-function application joins the O(1) screens by the
                // same no-native-arm property (the #502 IRMetaOutputApply pattern
                // verbatim: at-root claims — the #519 calleeGate census's whole mass,
                // 100% atRoot — serve through the standing callArgs dispatch leg by raw
                // family, super.visitSymbolReference with legacy's own external-function
                // threading inside; interiors route through containsOracleLeaf).
                || ir instanceof com.regnosys.rosetta.ir.expr.IRLibraryApply) {
            return true;
        }
        if (ir instanceof FieldAccess || ir instanceof com.regnosys.rosetta.ir.expr.BinaryOp
                || ir instanceof com.regnosys.rosetta.ir.expr.Existence
                || (ir instanceof IRApply apply
                        && !(apply.callee() instanceof IRReference callee
                                && callee.referenceKind() == IRReference.ReferenceKind.RULE))) {
            return containsOracleLeaf(ir);
        }
        return false;
    }

    /**
     * #527 — the {@link PostPinArm#ARG_NAV_MISSING_HOP} trip probe: whether any {@link IRApply}
     * in the lowered tree carries a {@link FieldAccess}-chain argument with the MISSING type
     * sentinel on a hop, or on its {@link IRVariable} chain root (the #491 root-belt read —
     * scoped to variable roots: the missing-BY-CONSTRUCTION oracle-leaf root kinds
     * [{@code IRSynItemNav} and family] never reach this probe, their claims being exempt
     * oracle-root shapes). Read-only, IR-only (no re-adapts, no raw walks): the adapter's #527
     * {@code argNavFacet} recut is the ONLY source of missing-typed admitted arg chains (every
     * such chain was adapter-declined pre-teach — the {@code typeMissing.param.*} faces), so
     * the probe fires exactly on the taught class plus any future carrier of the same shape.
     */
    private static boolean loweredArgChainCarriesMissingHop(IRExpr node) {
        return loweredArgChainCarriesMissingHop(node, false);
    }

    /**
     * The single-pass walk behind {@link #loweredArgChainCarriesMissingHop(IRExpr)} (Copilot
     * #527 R1: the first cut spine-walked each {@link IRApply} argument AND re-descended the
     * same subtrees through {@code children()} — the arg-seat context now threads through the
     * one recursion instead). {@code argSeat} is true exactly on the SPINE of a
     * {@link FieldAccess}-chain call argument: it turns on at a chain-shaped arg, propagates
     * receiver-to-receiver, reaches the {@link IRVariable} chain root (the #491 root-belt
     * read), and resets everywhere else — a nested {@link IRApply} inside a chain re-runs its
     * own args with a fresh seat, so the trigger set is IDENTICAL to the two-pass form.
     */
    private static boolean loweredArgChainCarriesMissingHop(IRExpr node, boolean argSeat) {
        if (argSeat && node instanceof FieldAccess hop
                && (hop.type() == null || hop.type().isMissing())) {
            return true;
        }
        if (argSeat && node instanceof IRVariable var
                && (var.type() == null || var.type().isMissing())) {
            return true; // the chain root — reachable only through a FieldAccess spine
        }
        if (node instanceof IRApply apply) {
            if (loweredArgChainCarriesMissingHop(apply.callee(), false)) {
                return true;
            }
            for (IRExpr arg : apply.args()) {
                if (loweredArgChainCarriesMissingHop(arg, arg instanceof FieldAccess)) {
                    return true;
                }
            }
            return false;
        }
        if (node instanceof FieldAccess fa) {
            // the receiver keeps the spine context; a FieldAccess has no other child
            return loweredArgChainCarriesMissingHop(fa.receiver(), argSeat);
        }
        for (IRExpr child : node.children()) {
            if (loweredArgChainCarriesMissingHop(child, false)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Whether any ORACLE-LEAF node — an {@link IRMetaAccess} (the #500 walk), a #501
     * shallow/consumer-admitted kind with no native emitter arm ({@link IROnlyExists} /
     * {@link IRPipe} / {@link IRConversion} / {@link IRSymbolNav}), or a #502 shallow kind
     * ({@link IRClosureParam} / {@link IRMetaOutputApply}) — appears in the lowered tree
     * (read-only walk). A root containing one MUST render whole-legacy (the oracle-root
     * serve): the leaf emitter has no arm for these kinds, so a native compose would decline
     * the whole claim at the kind-dispatch end — the walk keeps the render routed.
     */
    private static boolean containsOracleLeaf(IRExpr node) {
        if (node instanceof IRMetaAccess || node instanceof IROnlyExists
                || node instanceof IRPipe || node instanceof IRConversion
                || node instanceof IRSymbolNav
                // #502: the closure-param reference and the meta-output call join the leaf set
                // (the arm-2/arm-4 shallow kinds — no native emitter arm exists for either, so
                // a containing root MUST render whole-legacy or the claim declines whole).
                || node instanceof IRClosureParam || node instanceof IRMetaOutputApply
                // #503: the ALL/ANY-modified comparison (the arm-A1 shallow kind) and the
                // to-string conversion (the arm-A2 operand admission — the emitter's nested
                // IRToString decline is the standing choke point, so a containing root MUST
                // render whole-legacy; a TOP-LEVEL to-string root keeps its own standing
                // ToStringRenderer slot, which the O(1) root screens do not re-route).
                || node instanceof IRAllAnyCompare || node instanceof IRToString
                // #504 arm-B: the un-retypeable synthetic-item nav (the shallow kind — no
                // native emitter arm, so a containing root MUST render whole-legacy).
                || node instanceof IRSynItemNav
                // #505: the choice-option nav and the dispatch-base input join the leaf set
                // (the same no-native-arm property — a containing root MUST render
                // whole-legacy or the claim declines).
                || node instanceof IRChoiceOptionNav || node instanceof IRDispatchInputRef
                // #507: the deep-path nav and the record-feature read join the leaf set
                // (the same no-native-arm property — a containing root MUST render
                // whole-legacy or the claim declines).
                || node instanceof IRDeepFeatureNav || node instanceof IRRecordFeatureNav
                // #508 arm-A5: the meta-sourced USER-item nav joins the leaf set (the same
                // no-native-arm property — a containing root MUST render whole-legacy or
                // the claim declines).
                || node instanceof IRMetaItemNav
                // #512 arm-B: the receiver-carrying record read and the item-qualifier
                // read join the leaf set (the same no-native-arm property — a containing
                // root MUST render whole-legacy or the claim declines; the recursive child
                // walk below carries both kinds to EVERY position — operand, arg, list-op
                // child, conditional branch, deeper hop).
                || node instanceof IRRecordReceiverNav
                || node instanceof IRQualifierItemNav
                // #525: the receiver-carrying qualifier read joins the leaf set (the same
                // no-native-arm property — a containing root MUST render whole-legacy or
                // the claim declines; the recursive child walk carries the kind to EVERY
                // position, twin-exact with the adapter's equality/existence admissions).
                || node instanceof IRQualifierReceiverNav
                // #529: the receiver-carrying choice-option read joins the leaf set (the
                // same no-native-arm property — a containing root MUST render whole-legacy
                // or the claim declines; the recursive child walk carries the kind to
                // EVERY position, twin-exact with the adapter's equality/existence
                // admissions).
                || node instanceof com.regnosys.rosetta.ir.expr.IRChoiceReceiverNav
                // #513: the noAdaptArm-sweep kinds join the leaf set (the same
                // no-native-arm property — a containing root MUST render whole-legacy or
                // the claim declines; the recursive child walk carries all four to EVERY
                // position, twin-exact with the adapter's isNoAdaptArmSweepKind
                // admissions).
                || isNoAdaptArmSweepKind(node)
                // #514: the function-OUTPUT reference, the META-input reference and the
                // top-level rule-input nav join the leaf set (the same no-native-arm
                // property — the recursive child walk carries all three kinds to EVERY
                // position: equality/existence/comparison/arithmetic operands, call args,
                // nav bases — twin-exact with the adapter's admissions).
                || node instanceof IROutputRef || node instanceof IRMetaParamRef
                || node instanceof IRRuleInputNav
                // #515: the with-meta annotation and the join concatenation join the leaf
                // set (the same no-native-arm property — the adapter arms are UNCONDITIONAL,
                // so an interior with-meta/join lowers wherever a parent arm's child
                // recursion admits it; a containing root MUST render whole-legacy or the
                // claim declines at the leaf emitter — the #514 emitter-leak law).
                || node instanceof com.regnosys.rosetta.ir.expr.IRWithMetaOp
                || node instanceof com.regnosys.rosetta.ir.expr.IRJoinOp
                // #518 arm-B2: the output-consuming alias-head navigation joins the leaf
                // set (the same no-native-arm property — legacy renders the BUILDER walk,
                // a compose no Mapper-shaped native arm may reproduce; a containing root
                // MUST render whole-legacy or the claim declines).
                || node instanceof com.regnosys.rosetta.ir.expr.IROutputAliasNav
                // #520: the library-function application joins the leaf set (the #502
                // IRMetaOutputApply pattern verbatim — the render is legacy's own
                // external-function threading, a compose no native arm reproduces; a
                // containing root MUST render whole-legacy or the claim declines at the
                // kind-dispatch end).
                || node instanceof com.regnosys.rosetta.ir.expr.IRLibraryApply
                // #528 arm-5: the synthesized-receiver bare attribute joins the leaf set (the
                // L-113 conversion's routing half — the #514 IRRuleInputNav pattern one seat
                // over): the render is legacy's own receiver synthesis inside
                // ReferenceHandler.handle, a compose no native arm reproduces, so a containing
                // root MUST render whole-legacy or the claim declines at the kind-dispatch
                // end. An AT-ROOT claim serves through the standing bareSymbolRef dispatch leg
                // — literally the render the L-113 belt itself performed, byte-identical BY
                // IDENTITY; the kind was 100% delegated-or-declined pre-teach, so no standing
                // native compose reroutes (the #504 conservation argument).
                || node instanceof com.regnosys.rosetta.ir.expr.IRImplicitAttrNav
                // #640 (v3.3 seat 4): the data-rule condition's implicit instance - legacy's synthesized parameter
                // reference; an oracle leaf, so a root containing it (`<instance> -> attr`) renders whole-legacy
                || node instanceof com.regnosys.rosetta.ir.expr.IRConditionInstance) {
            return true;
        }
        // #519: a SUM list-op joins the leaf set OP-SCOPED (the numeric aggregate — legacy's
        // sum render derives the element-type method inside its own composition, so the kind
        // has NO native emitter arm and a containing root MUST render whole-legacy; a
        // TOP-LEVEL sum keeps the standing CollectionOpRenderer slot, which the O(1) root
        // screens do not re-route — the IRToString precedent). The op scoping is the #511
        // COUNT-carve-out pattern: every COUNT/collapse list-op keeps its standing native
        // compose exactly where it is — the leg keys the NEWLY-taught operator only, so no
        // standing native compose reroutes (the #504 conservation argument: opSum was a
        // 100%-decline face pre-teach).
        if (node instanceof com.regnosys.rosetta.ir.expr.IRListOp sumOp
                && sumOp.op() == com.regnosys.rosetta.ir.expr.IRListOp.Kind.SUM) {
            return true;
        }
        // #504 SHAPE legs — the arm-A/arm-C admissions whose OPERAND kinds keep their standing
        // NATIVE composes elsewhere (IRApply/IRPointFreeApply args and nav receivers, the
        // future native enum-value arg): the leaf is the COMPOSED SHAPE, not the bare kind, so
        // exactly the newly-admitted roots re-route and every pre-#504 native compose keeps its
        // bytes (the conservation argument — each shape was a 100%-decline face pre-teach).
        if (node instanceof com.regnosys.rosetta.ir.expr.BinaryOp bin) {
            boolean logical = bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.AND
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.OR;
            boolean equality = bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.EQ
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.NEQ;
            // arm-A1: a logical with a ComparisonResult.ofNullSafe-coerced operand (a bare
            // boolean call or a point-free reference) — legacy's LogicalHandler composes the
            // coercion inside its own render; the emitter's andNullSafe/orNullSafe chain
            // cannot.
            if (logical && (isCoercedBooleanOperand(bin.left())
                    || isCoercedBooleanOperand(bin.right()))) {
                return true;
            }
            // arm-A2: an equality with a point-free operand — legacy's implicit-invocation +
            // wrap composition (areEqual_operand_wrap) is the ComparisonHandler's own.
            if (equality && (bin.left() instanceof IRPointFreeApply
                    || bin.right() instanceof IRPointFreeApply)) {
                return true;
            }
            // #510 arm-A2 SHAPE leg: an equality with a MULTI-cardinality nav operand —
            // legacy's ComparisonHandler derives the cardinality-modifier default (All for
            // =, Any for <>) and the MapperC join inside its own composition; the emitter's
            // native equality arm was only ever proven SINGLE. The leaf is the COMPOSED
            // shape (twin-exact with the adapter's isMultiNavOperand admission — the same
            // accumulated-cardinality fact off the same node); single navs keep every
            // standing native compose. The shape was a 100%-decline face pre-teach
            // (operandMultiNav — the #504 conservation argument).
            if (equality && (isMultiNavOperand(bin.left()) || isMultiNavOperand(bin.right()))) {
                return true;
            }
            // #511 SHAPE legs: an equality with a nested-BinaryOp operand (operand:BinaryOp
            // — 58 equality sole at the #510 SOT: the `a + b = c` / `a + b = 0` class) or a
            // non-COUNT list-op operand (operand:IRListOp — 34 equality sole) — legacy's
            // ComparisonHandler composes the nested operand render + the coercion/witness
            // threading inside its own line. Twin-exact with the adapter's isEqualityOperand
            // admissions (the same kind facts off the same nodes). COUNT stays OUT of the
            // list-op test — `xs count = 1` is the standing NATIVE compose (the emitter's
            // resultCount wrap), preserved by the exclusion; both new shapes were
            // 100%-decline faces pre-teach (the #504 conservation argument).
            if (equality && (bin.left() instanceof com.regnosys.rosetta.ir.expr.BinaryOp
                    || bin.right() instanceof com.regnosys.rosetta.ir.expr.BinaryOp
                    || isNonCountListOp(bin.left()) || isNonCountListOp(bin.right()))) {
                return true;
            }
            // #518 SHAPE legs — the EQUALITY operand cluster (the #517 arithmetic legs'
            // siblings, twin-exact with the adapter's isEqualityOperand admissions): an
            // equality with an implicit-item variable (operandItem — the `then if item =
            // DAIL`-class then-bound shapes; the leg is KIND-WIDE and deliberately
            // REROUTES the standing filter/extract-bound native composes to the oracle
            // serve BY IDENTITY — the #504 arm-C1a/C2 precedent: the bound/unbound split
            // is a RAW-side fact the lowered tree cannot key, the serve IS legacy's own
            // super.visitEquality line, and the oracleRootLowered equality receipt carries
            // the moved mass), an existence-check operand (operand:Existence — the
            // `(x exists) = flag` class), a list-literal operand (operand:IRListConstruct),
            // a MULTI-cardinality call operand (operandMultiCall via isMultiCallOperand —
            // the MULTI slice ONLY: a SINGLE call is the standing isScalarOperand native
            // admission, and a kind-wide leg would hijack that class into the oracle
            // exemption and migrate the postPinServeLowered receipt — the #516 router
            // witness caught exactly that at arm time), a MULTI-cardinality PARAM operand
            // (operandMultiParam — the #510 multi-nav leg's PARAM sibling via
            // isMultiParamOperand) or a lambda-op operand (operand:IRLambdaOp). Legacy's
            // ComparisonHandler composes every operand render + the modifier default + the
            // coercion/witness threading inside its own line; every shape except the item
            // was a 100%-decline face pre-teach (the #504 conservation argument). The
            // IRConversion operand needs NO leg — the kind is a standing #501 entry in the
            // walk above.
            if (equality && (isItemVariableOperand(bin.left())
                    || isItemVariableOperand(bin.right())
                    || bin.left() instanceof com.regnosys.rosetta.ir.expr.Existence
                    || bin.right() instanceof com.regnosys.rosetta.ir.expr.Existence
                    || bin.left() instanceof com.regnosys.rosetta.ir.expr.IRListConstruct
                    || bin.right() instanceof com.regnosys.rosetta.ir.expr.IRListConstruct
                    || isMultiCallOperand(bin.left()) || isMultiCallOperand(bin.right())
                    || isMultiParamOperand(bin.left()) || isMultiParamOperand(bin.right())
                    || bin.left() instanceof IRLambdaOp
                    || bin.right() instanceof IRLambdaOp)) {
                return true;
            }
            // #510 arm-A4c SHAPE leg: an inequality comparison with an alias operand —
            // legacy's #372 comparisonIntWiden body walk (a numeric-alias inequality firing
            // a widen hop on its sibling) runs inside the whole-legacy render. The shape
            // was a 100%-decline face pre-teach (the operandAlias.* comparison facets).
            boolean comparison = bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.LT
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.GT
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.LTE
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.GTE;
            if (comparison && (isAliasOperand(bin.left()) || isAliasOperand(bin.right())
                    // #511: a non-COUNT list-op comparison operand joins the leg
                    // (operand:IRListOp — 2 comparison sole at the #510 SOT), twin-exact
                    // with the adapter's isComparisonOperand admission; `xs count >= 1`
                    // keeps its standing NATIVE compose via the COUNT exclusion.
                    || isNonCountListOp(bin.left()) || isNonCountListOp(bin.right())
                    // #528 arm-2 SHAPE leg: a comparison with a POINT-FREE operand
                    // (operand:IRPointFreeApply — 4 comparison sole at the #527 SOT) —
                    // legacy's ComparisonHandler composes the implicit-invocation render
                    // inside its own line; twin-exact with the adapter's isComparisonOperand
                    // admission. A 100%-decline face pre-teach, so no standing native
                    // compose reroutes (the #504 conservation argument).
                    || bin.left() instanceof IRPointFreeApply
                    || bin.right() instanceof IRPointFreeApply
                    // #528 arm-3 SHAPE leg: a comparison with an implicit-item operand
                    // (operandItem — 2 comparison sole at the #527 SOT) — the #518 EQUALITY
                    // leg at its sibling seat, and KIND-WIDE for the same forced reason:
                    // the bound/unbound split is a RAW-side fact the lowered tree cannot
                    // key, so this leg DELIBERATELY reroutes the standing filter/extract-
                    // bound native composes at the comparison seat to the oracle serve BY
                    // IDENTITY (the serve IS legacy's own super.visitComparison line, and
                    // the oracleRootLowered comparison receipt carries the moved mass).
                    || isItemVariableOperand(bin.left())
                    || isItemVariableOperand(bin.right()))) {
                return true;
            }
            // #510 arm-A5 SHAPE legs: an arithmetic with a nav or conditional operand —
            // legacy's ArithmeticHandler derives the witness/join/coercion for both forms
            // inside its own MapperMaths composition; the emitter's native arithmetic arm
            // was only ever proven for the scalar-param/literal/alias/nested set. Both
            // shapes were 100%-decline faces pre-teach (operandNav / operand:IRConditional
            // at the arithmetic seat); bare navs and conditionals keep their standing
            // native composes elsewhere. The meta-access arithmetic operand needs no leg —
            // the kind walk above already carries IRMetaAccess kind-wide.
            boolean arithmetic = bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.ADD
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.SUB
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.MUL
                    || bin.op() == com.regnosys.rosetta.ir.expr.BinaryOp.BinOp.DIV;
            if (arithmetic && (bin.left() instanceof FieldAccess
                    || bin.right() instanceof FieldAccess
                    || bin.left() instanceof IRConditional
                    || bin.right() instanceof IRConditional)) {
                return true;
            }
            // #517 SHAPE legs — the ARITHMETIC operand cluster (the #510 arm-A5 legs'
            // siblings, twin-exact with the adapter's isArithmeticOperand admissions):
            // an arithmetic with a call (operandCall — legacy's evaluate-slot render
            // composes inside its own MapperMaths line), a list-op of ANY kind
            // (operandCount — the live mass is 100% COUNT; the arithmetic seat has NO
            // native list-op compose to protect, so the leg is kind-wide, UNLIKE the
            // equality/comparison COUNT carve-outs above, which this leg does not touch
            // — different operators), an implicit-item variable (operandItem — the
            // `item * 2` lambda-body shapes) or a NON-numeric literal (the drr-RULE
            // string-concat class — operand:IRLiteral) operand. The numeric-literal
            // EXCLUSION in isNonNumericLiteralOperand keeps every standing `a + 1`-class
            // native compose exactly where it is (the #511 COUNT-carve-out pattern: the
            // leg keys the NEWLY-admitted subset only). Every shape was a 100%-decline
            // face pre-teach (the #504 conservation argument — no standing native
            // compose reroutes); the to-string / synthetic-item-nav / record-read
            // operands need NO leg — all three are standing KINDS in the walk above
            // (#503/#504/#507), so the recursive child walk already routes every
            // containing root.
            if (arithmetic && (bin.left() instanceof IRApply || bin.right() instanceof IRApply
                    || bin.left() instanceof com.regnosys.rosetta.ir.expr.IRListOp
                    || bin.right() instanceof com.regnosys.rosetta.ir.expr.IRListOp
                    || isItemVariableOperand(bin.left()) || isItemVariableOperand(bin.right())
                    || isNonNumericLiteralOperand(bin.left())
                    || isNonNumericLiteralOperand(bin.right()))) {
                return true;
            }
        }
        // #511 receiver SHAPE leg: a navigation over a list-op receiver OUTSIDE the #499
        // census-narrow collapse class (the receiver:IRListOp admission — 86 sole at the
        // #510 SOT). The leg keys on the emitter's OWN native boundary
        // (IRJavaLeafEmitter.isCollapseNavBase — twin-exact BY IDENTITY: the same predicate
        // that gates the #499 native nav_after_get_rewrap / FIRST member-call composes), so
        // every collapse-class nav keeps its standing NATIVE compose and every excluded
        // kind/child routes the containing root whole-legacy. The recursive child walk
        // below carries this leg to a nav-over-list-op in EVERY position (operand, arg,
        // deeper hop, claim root); the excluded shapes were 100%-decline faces pre-teach,
        // so no standing native compose reroutes (the #504 conservation argument).
        if (node instanceof FieldAccess navOverListOp
                && navOverListOp.receiver() instanceof com.regnosys.rosetta.ir.expr.IRListOp lo
                && !IRJavaLeafEmitter.isCollapseNavBase(lo)) {
            return true;
        }
        // #520 receiver SHAPE leg: a navigation over a lambda-bodied collection-op receiver
        // (the receiver:IRLambdaOp admission — 9 sole at the #519 SOT: the `(xs
        // extract/filter […]) -> feature` chains). The #511 nav-over-list-op leg's
        // lambda-bodied sibling with NO native carve-out: no nav-over-lambda-op native
        // compose exists (the shape was a 100%-decline face pre-teach — the #504
        // conservation argument), so EVERY containing claim root routes whole-legacy
        // (legacy NavigationHandler's own receiver handling inside the serve; a drift
        // arrival at the leaf emitter's frame-slot-gated lambda arm declines, never a
        // native compose). Twin-exact with the adapter's isNavigableReceiver admission.
        if (node instanceof FieldAccess navOverLambdaOp
                && navOverLambdaOp.receiver() instanceof IRLambdaOp) {
            return true;
        }
        // arm-A3: an existence over a point-free operand — the same implicit-invocation wrap.
        // #511: an existence over a list-op operand joins (operand:IRListOp — 18 existence
        // sole at the #510 SOT; kind-wide — no list-op existence operand had a native
        // compose pre-teach), twin-exact with the adapter's isExistenceOperand admission.
        // #519 SHAPE legs — the EXISTENCE operand cluster (the #518 equality legs' siblings,
        // twin-exact with the adapter's isExistenceOperand admissions): an existence over an
        // implicit-item variable (operandItem — the then/lambda-bound `item exists` shapes
        // AND the adapter-synthesized elided implicits; the leg is KIND-WIDE and deliberately
        // REROUTES the standing filter/extract-bound native existence composes to the oracle
        // serve BY IDENTITY — the #518 equality-item precedent: the bound/unbound split is a
        // RAW-side fact the lowered tree cannot key, the serve IS legacy's own
        // super.visitExistence line, and the oracleRootLowered existence receipt carries the
        // moved mass) or a list-literal operand (operand:IRListConstruct — the `[a, b]
        // exists` class; the kind keeps its standing native root render elsewhere, the leaf
        // is the COMPOSED shape). The IRConversion / IROutputAliasNav existence operands need
        // NO leg — both are standing KINDS in the walk above (#501/#518), so the recursive
        // child walk already routes every containing root.
        if (node instanceof com.regnosys.rosetta.ir.expr.Existence exist
                && (exist.arg() instanceof IRPointFreeApply
                        || exist.arg() instanceof com.regnosys.rosetta.ir.expr.IRListOp
                        || isItemVariableOperand(exist.arg())
                        || exist.arg() instanceof com.regnosys.rosetta.ir.expr.IRListConstruct)) {
            return true;
        }
        // arm-C1a/C2: a call carrying an enum-value arg (the tryBareEnumArg-mirror mints AND
        // the standing qualified-enum-arg natives — the leg cannot key the requalified subset
        // from the lowered tree, so it REROUTES the pre-#504 native enum-arg composes to the
        // callArgs oracle serve too: byte-identical BY IDENTITY [the serve IS legacy's own
        // line; the #504 Seat-1 MF-4 disclosure — the callArgs oracleRootLowered receipt
        // carries the moved mass]) or an explicit `empty` arg (the argEmpty admission —
        // legacy's null-threading render).
        if (node instanceof IRApply apply) {
            for (IRExpr arg : apply.args()) {
                if (arg instanceof IREmptyLiteral
                        || (arg instanceof IRReference ref
                                && ref.referenceKind() == IRReference.ReferenceKind.ENUM_VALUE)) {
                    return true;
                }
                // #505 arm-B SHAPE legs: a call carrying a LIST-OP arg (the arg:IRListOp
                // admission — legacy's collapse render + evaluate-slot unwrap compose inside
                // its own line) or an ALIAS-rooted nav arg (the argNav.alias admission —
                // legacy's #360 neutral-compile + evaluate-arg unwrap). Both were
                // 100%-decline faces pre-teach, so no standing native compose reroutes (the
                // #504 conservation argument); the leaf is the COMPOSED shape — bare list
                // ops and alias navs keep their standing native composes elsewhere.
                if (arg instanceof com.regnosys.rosetta.ir.expr.IRListOp) {
                    return true;
                }
                // #511: the BINARY argument joins the callArgs legs (the arg:BinaryOp
                // admission — 47 sole at the #510 SOT; legacy's operand render +
                // evaluate-slot unwrap compose inside its own line). A 100%-decline face
                // pre-teach — no standing native compose reroutes.
                if (arg instanceof com.regnosys.rosetta.ir.expr.BinaryOp) {
                    return true;
                }
                // #512 — the ARG-KIND SWEEP legs (the #511 arg:BinaryOp leg's siblings):
                // a call carrying a list-literal (arg:IRListConstruct — 28 sole at the
                // #511 SOT), lambda-op (arg:IRLambdaOp 26), construct (arg:IRConstruct 22)
                // or conditional (arg:IRConditional 16) argument — legacy's argument
                // render + evaluate-slot unwrap compose inside its own line. The leaf is
                // the COMPOSED shape, not the bare kind (each kind keeps its standing
                // native root render elsewhere — the list-literal/conditional arms, the
                // LambdaOpRenderer/ConstructRenderer slots); every shape was a
                // 100%-decline face pre-teach, so no standing native compose reroutes
                // (the #504 conservation argument). The arg:IRToString sibling needs NO
                // leg — IRToString is a standing KIND in the walk above (#503), so the
                // recursive child walk already routes every containing root.
                if (arg instanceof com.regnosys.rosetta.ir.expr.IRListConstruct
                        || arg instanceof IRLambdaOp || arg instanceof IRConstruct
                        || arg instanceof IRConditional) {
                    return true;
                }
                IRExpr argRoot = arg;
                while (argRoot instanceof FieldAccess fa) {
                    argRoot = fa.receiver();
                }
                if (arg instanceof FieldAccess && (argRoot instanceof IRReference aliasRoot
                        && aliasRoot.referenceKind() == IRReference.ReferenceKind.ALIAS
                        // #511: the LIST-OP-rooted nav arg joins the walk (the
                        // argNav.root:IRListOp admission, every depth — the #505
                        // alias-root leg's sibling). In arg position even a
                        // collapse-class-rooted nav was a 100%-decline face pre-teach
                        // (the #489 argNav root gate never admitted list-op roots), so
                        // the leg reroutes nothing; the nav-over-list-op receiver leg
                        // below independently carries the non-collapse receivers in
                        // every other position.
                        || argRoot instanceof com.regnosys.rosetta.ir.expr.IRListOp)) {
                    return true;
                }
            }
        }
        for (IRExpr child : node.children()) {
            if (containsOracleLeaf(child)) {
                return true;
            }
        }
        return false;
    }

    /**
     * #504 arm-A1 — a logical operand legacy coerces {@code ComparisonResult.ofNullSafe(...)}
     * inside {@code LogicalHandler}'s own composition: a lowered boolean CALL ({@link IRApply}
     * — the {@code operandBareBooleanCall} face, 829 sole at the #503 SOT) or a lowered
     * point-free reference ({@link IRPointFreeApply} — the logical {@code
     * operand:IRPointFreeApply} face, 332 sole). The SHAPE leg in {@link #containsOracleLeaf}
     * keys on this predicate at the logical seat only — args and nav receivers keep their
     * standing native composes.
     */
    private static boolean isCoercedBooleanOperand(IRExpr operand) {
        // #510 arms A3/A4: the conditional and the alias reference join the coerced family
        // (operand:IRConditional 86 · operandAlias FLAT 73 logical sole at the #509 SOT) —
        // legacy LogicalHandler coerces both ComparisonResult.ofNullSafe(<render>) inside
        // its own composition, for ANY alias body and any branch form; bare conditionals
        // and alias refs keep their standing native composes elsewhere (the leaf is the
        // COMPOSED logical shape, twin-exact with the adapter's producesComparisonResult
        // admissions).
        return operand instanceof IRApply || operand instanceof IRPointFreeApply
                || operand instanceof IRConditional || isAliasOperand(operand)
                // #511: the list-op operand joins the coerced family (operand:IRListOp — 6
                // logical sole at the #510 SOT) — legacy LogicalHandler coerces the render
                // ComparisonResult.ofNullSafe(...) inside its own composition; twin-exact
                // with the adapter's producesComparisonResult admission (kind-wide — no
                // logical had a list-op operand's native compose pre-teach).
                || operand instanceof com.regnosys.rosetta.ir.expr.IRListOp
                // #513: the membership test joins the coerced family (a contains/disjoint
                // IS a boolean producer) — twin-exact with the adapter's
                // producesComparisonResult admission; probe2 adds the default fallback
                // (operand:IRDefaultOp 6 logical sole at probe1 — the boolean
                // `(a default b) and …` class, the same coerced composition).
                || operand instanceof com.regnosys.rosetta.ir.expr.IRMembershipOp
                || operand instanceof com.regnosys.rosetta.ir.expr.IRDefaultOp
                // #520: the lambda-bodied collection op joins the coerced family
                // (operand:IRLambdaOp — 10 logical sole at the #519 SOT) — legacy
                // LogicalHandler coerces the render ComparisonResult.ofNullSafe(...)
                // inside its own composition for ANY lambda-op render; twin-exact with
                // the adapter's producesComparisonResult admission (no logical had a
                // lambda-op operand's native compose pre-teach — the #504 conservation
                // argument).
                || operand instanceof IRLambdaOp
                // #528 arm-1: the bare boolean NAVIGATION joins the coerced family (the
                // operandBareBooleanNav face — 4 sole at the #527 SOT) — legacy
                // LogicalHandler coerces the render ComparisonResult.ofNullSafe(...) inside
                // its own composition for ANY nav render; twin-exact with the adapter's
                // producesComparisonResult admission (kind-wide — no logical had a nav
                // operand's native compose pre-teach, so no standing compose reroutes; navs
                // keep every native compose at the other seats, which this logical-scoped
                // leg does not touch).
                || operand instanceof FieldAccess;
    }

    /**
     * #513: the noAdaptArm-sweep kinds ({@code IRSwitchOp}/{@code IRDefaultOp}/
     * {@code IRMembershipOp}/{@code IRCollectOp}) — the leaf/screen shape test, twin-exact
     * with the adapter's {@code isNoAdaptArmSweepKind} admissions (the same kind facts off
     * the same nodes). All four are SHALLOW oracle leaves with no emitter arm.
     */
    private static boolean isNoAdaptArmSweepKind(IRExpr node) {
        return node instanceof com.regnosys.rosetta.ir.expr.IRSwitchOp
                || node instanceof com.regnosys.rosetta.ir.expr.IRDefaultOp
                || node instanceof com.regnosys.rosetta.ir.expr.IRMembershipOp
                || node instanceof com.regnosys.rosetta.ir.expr.IRCollectOp;
    }

    /**
     * #510: a lowered alias/shortcut reference ({@link IRReference.ReferenceKind#ALIAS}) — the
     * comparison/logical shape legs' operand test, twin-exact with the adapter's
     * {@code isAliasReference} admission.
     */
    private static boolean isAliasOperand(IRExpr operand) {
        return operand instanceof IRReference ref
                && ref.referenceKind() == IRReference.ReferenceKind.ALIAS;
    }

    /**
     * #511: a lowered list op of any kind EXCEPT {@code COUNT} — the equality/comparison
     * shape legs' operand test, twin-exact with the adapter's {@code isNonCountListOp}
     * admission (the same kind fact off the same node). The {@code COUNT} carve-out keeps
     * {@code xs count = 1} / {@code xs count >= 1} on their standing NATIVE composes (the
     * emitter's {@code resultCount} wrap).
     */
    private static boolean isNonCountListOp(IRExpr operand) {
        return operand instanceof com.regnosys.rosetta.ir.expr.IRListOp listOp
                && listOp.op() != com.regnosys.rosetta.ir.expr.IRListOp.Kind.COUNT;
    }

    /**
     * #510: a lowered MULTI-cardinality plain navigation — the equality shape leg's operand
     * test, twin-exact with the adapter's {@code isMultiNavOperand} admission (the same
     * accumulated-cardinality fact off the same node).
     */
    private static boolean isMultiNavOperand(IRExpr operand) {
        return operand instanceof FieldAccess fa
                && fa.cardinality() == ExpressionCardinality.MULTI;
    }

    /**
     * #517: the implicit item variable ({@link IRVariable} of {@code USER_ITEM} /
     * {@code SYNTHETIC_ITEM}) — the arithmetic shape leg's item test, twin-exact with the
     * adapter's {@code isItemOperand} admission (the same kind facts off the same node).
     * PARAM variables stay out: the scalar-param operand is the emitter's standing native
     * compose.
     */
    private static boolean isItemVariableOperand(IRExpr operand) {
        return operand instanceof com.regnosys.rosetta.ir.expr.IRVariable var
                && (var.variableKind()
                        == com.regnosys.rosetta.ir.expr.IRVariable.VariableKind.USER_ITEM
                        || var.variableKind()
                                == com.regnosys.rosetta.ir.expr.IRVariable.VariableKind.SYNTHETIC_ITEM);
    }

    /**
     * #518: a MULTI-cardinality PARAM variable — the equality shape leg's MULTI-param test,
     * twin-exact with the adapter's {@code isMultiParamOperand} admission (the same kind +
     * accumulated-cardinality facts off the same node — the #510 multi-nav pattern at the
     * PARAM kind). SINGLE params stay out: the scalar-param operand is the emitter's
     * standing native compose.
     */
    private static boolean isMultiParamOperand(IRExpr operand) {
        return operand instanceof com.regnosys.rosetta.ir.expr.IRVariable var
                && var.variableKind() == com.regnosys.rosetta.ir.expr.IRVariable.VariableKind.PARAM
                && var.cardinality() == com.regnosys.rosetta.types.ExpressionCardinality.MULTI;
    }

    /**
     * #518: a MULTI-cardinality call — the equality shape leg's MULTI-call test, twin-exact
     * with the adapter's {@code isMultiCallOperand} admission (the same kind +
     * accumulated-cardinality facts off the same node — the #510 multi-nav pattern at the
     * CALL kind). SINGLE calls stay out: a SINGLE call is the adapter's standing
     * isScalarOperand admission whose claims ride the proven native compose + the postPin
     * scan (the #471–#474/#516 serve arms) — a kind-wide leg would hijack that class into
     * the oracle exemption and migrate the postPinServeLowered receipt.
     */
    private static boolean isMultiCallOperand(IRExpr operand) {
        return operand instanceof IRApply apply
                && apply.cardinality() == com.regnosys.rosetta.types.ExpressionCardinality.MULTI;
    }

    /**
     * #517: a lowered NON-numeric literal (an {@link com.regnosys.rosetta.ir.expr.IRLiteral}
     * whose kind is neither {@code INT} nor {@code NUMBER} — the drr-RULE string-concat
     * operands) — the arithmetic shape leg's literal test, twin-exact with the NEWLY-admitted
     * subset of the adapter's literal widening. The numeric EXCLUSION is the leg's whole
     * point: a numeric literal beside the standing operand set is the emitter's ring-proven
     * native compose (`a + 1` → MapperMaths with the re-rendered literal), and this leg must
     * never reroute it (the #511 COUNT-carve-out pattern).
     */
    private static boolean isNonNumericLiteralOperand(IRExpr operand) {
        return operand instanceof com.regnosys.rosetta.ir.expr.IRLiteral lit
                && lit.literalKind() != com.regnosys.rosetta.ir.expr.IRLiteral.LiteralKind.INT
                && lit.literalKind() != com.regnosys.rosetta.ir.expr.IRLiteral.LiteralKind.NUMBER;
    }

    /**
     * The #501 disguised-meta root gate — an {@link IRMetaAccess} CLAIM ROOT whose raw node is
     * a DISGUISED navigation ({@code RSymbolReference} args-empty — the bareMeta face's 180
     * at-root claims; since #505 the bound-chain {@code REnumValueRef} family JOINS — the seat
     * conversion retired the L-109c exclusion, so the family's meta-lowered chain roots reach
     * the frame gate for real [the #505 enumSeatRoot census read lowers.IRMetaAccess 344] and
     * serve through the new {@code enumChain} leg). Installs the {@link OracleRootRenderer}
     * slot ALONGSIDE the
     * {@code ir instanceof IRMetaAccess} MetaNav slot line: for these raw families
     * {@code buildMetaNavRenderer} returns null (RFeatureCall-gated), so the oracle slot is
     * the only live serve — while an {@code RFeatureCall} meta root fails THIS gate and keeps
     * the #499 MetaNavRenderer serve and its {@code metaNavLowered} receipt EXACTLY (the
     * receipt-stability constraint: the seat conservation gate pins that receipt). The guard
     * exemption for the whole class is the standing #499 kind-wide {@code IRMetaAccess} entry.
     */
    private static boolean isDisguisedMetaRoot(IRExpr ir, RExpression expr) {
        return ir instanceof IRMetaAccess
                && ((expr instanceof RSymbolReference ref && ref.args().isEmpty())
                        || expr instanceof REnumValueRef);
    }

    /**
     * #505: the converted disguised-REnumValueRef IDENTITY — true exactly when an
     * {@code REnumValueRef} claim is the DISGUISED residue (single-arrow navigation the
     * grammar parses as an enum-value ref), not a genuine enum-value constant. TWO
     * consumers: the {@code buildOracleRootRenderer} enumChain dispatch leg — the serve
     * routes the claim through the literal {@code super.visitEnumValueRef} line, the EXACT
     * pre-conversion fallback, byte-identical BY IDENTITY (the first #505 probe read the
     * native faFa slice DIVERGING at the converted roots, drr 11 FUNCTION + 13 POJO
     * mismatches — the emitter's direct spine render vs legacy's synthesize-and-re-enter
     * route) — and the {@link #isEnumChainSpineRoot} identity conjunct. The one-set
     * discipline (served ≡ exempted ≡ slot-installed) is carried by the SHAPE trio
     * (isOracleRootShape / IRMetaAccess-rooted / isEnumChainSpineRoot) at the seat's entry
     * gate, the frame-slot install and the post-pin exemption; this predicate is
     * deliberately NOT keyed at those three sites (a SITE-keyed exemption/install leg was
     * tried and REVERTED — the L-111 hijack, the #505 probe loop's second catch). A GENUINE
     * enum-value ref keeps its standing native path (the L-069 claim —
     * {@code emitEnumValue}'s byte-proven constant render).
     */
    private static boolean isConvertedEnumChainSite(RExpression expr) {
        return expr instanceof REnumValueRef enr
                && !ExpressionToIRAdapter.isGenuineEnumValueRef(enr);
    }

    /**
     * #505: the converted seat's PLAIN-SPINE serve gate — a disguised-ENR site whose lowering
     * is a ≥2-hop {@link FieldAccess} spine over a {@code SYNTHETIC_ITEM} base (the #480
     * item-chain arm's plain form — the seat census's {@code lowers.faFa} rows) or a
     * {@code PARAM} base (the #501 C1 rule-input chains over the #497 orphan input). These
     * shapes carry NO oracle leaf, so without this leg the emitter would compose them
     * natively at the seat — the first #505 probe read that render DIVERGING (drr 11+13
     * mismatches: the synthesize-and-re-enter route is the byte truth) — so the leg routes
     * them to the enumChain serve instead (the literal {@code super.visitEnumValueRef} line,
     * byte-identical BY IDENTITY). The ≥2-hop bound EXCLUDES the single-hop L-111 quiet
     * claims (FieldAccess over the input/alias head — the #496/#497 conversion's own
     * ring-proven NATIVE renders; serving those was tried and REVERTED, the L-111-hijack
     * cascade). THE shared spine predicate: the seat's entry gate, the frame-slot install
     * and the post-pin exemption all key on THIS method (the one-set law — served, exempted
     * and installed cannot drift); {@link #isConvertedEnumChainSite} is its identity
     * conjunct, not a gate of its own at those three sites.
     */
    private static boolean isEnumChainSpineRoot(IRExpr ir, RExpression expr) {
        if (!isConvertedEnumChainSite(expr) || !(ir instanceof FieldAccess)) {
            return false;
        }
        int hops = 0;
        IRExpr cur = ir;
        while (cur instanceof FieldAccess fa) {
            hops++;
            cur = fa.receiver();
        }
        return hops >= 2 && cur instanceof IRVariable var
                && (var.variableKind() == IRVariable.VariableKind.SYNTHETIC_ITEM
                        || var.variableKind() == IRVariable.VariableKind.PARAM);
    }

    /**
     * The #500 generic oracle-root renderer builder (the {@link #buildRuleApplyRenderer} /
     * {@link #buildMetaNavRenderer} pattern — the #494 root-only law: no site map; the ONE
     * subtree walk is the {@link #isOracleRootShape} oracle-leaf screen at the frame gate).
     * Captures the claim root's own raw node and dispatches ONE literal {@code super.visitX}
     * legacy line by its family — the #500 arm-B family seats (conversion / then / list-literal /
     * only-exists) and the CONTAINMENT-class consumer seats (nav chain / equality / comparison /
     * logical / arithmetic / existence / args-present call — at #500 these served the
     * meta-bearing roots; the #501 consumer admissions widened the contained-leaf set to the
     * shallow kinds, so the seven tokens were RECUT from the {@code meta}-prefixed spellings to
     * the family-plain forms [{@code metaLogical} → {@code logical} …] — the #497 MF-2
     * stale-narration law applied ahead of the drift; the #500 SOT values re-key 1:1), plus the
     * #501 {@code bareAttrMeta} seat (an args-EMPTY {@code RSymbolReference} whose claim
     * lowered to a disguised at-root {@link IRMetaAccess} — the {@link #isDisguisedMetaRoot}
     * frame leg; the bound-chain {@code REnumValueRef} family stays un-dispatched — the tail
     * note below).
     * The renderer serves the node only on source-range correlation ({@code Objects.equals} —
     * null-tolerant, sound BY ROUTING exactly as the #498/#499 slots: installed only at a
     * served claim root, where the emitter's entry consult whole-renders through this oracle
     * before its walk could reach any interior node). Interior nodes re-enter this compiler
     * through the oracle's own recursion and claim at their own roots. On a served render the
     * {@link #oracleRootLoweredByFamily} receipt increments under the family token. Returns
     * {@code null} for any other raw family — the ROUTING form of the two-leg narrowing: the
     * frame gate's IR-shape screen admits only the served kinds, and every construction site of
     * those kinds stamps a raw claim site's own sourceRange (the Seat-1 #500 construction-site
     * audit; the #501 arms' re-range stamps extend it); a future arm minting a served IR shape
     * from a NEW raw family finds the null slot here and that claim declines at the emitter —
     * bytes safe, the serve honestly absent.
     */
    private OracleRootRenderer buildOracleRootRenderer(RExpression expr, ExpressionContext ctx) {
        if (expr instanceof RConversionExpr conv) {
            return root -> correlatedOracleServe(conv, root, "conversion",
                    () -> super.visitConversion(conv, ctx));
        }
        if (expr instanceof RThenExpr then) {
            return root -> correlatedOracleServe(then, root, "pipe",
                    () -> super.visitThen(then, ctx));
        }
        if (expr instanceof RListLiteral list) {
            return root -> correlatedOracleServe(list, root, "listLiteral",
                    () -> super.visitListLiteral(list, ctx));
        }
        if (expr instanceof ROnlyExistsExpr onlyExists) {
            return root -> correlatedOracleServe(onlyExists, root, "onlyExists",
                    () -> super.visitOnlyExists(onlyExists, ctx));
        }
        if (expr instanceof RFeatureCall fc) {
            return root -> correlatedOracleServe(fc, root, "navChain",
                    () -> super.visitFeatureCall(fc, ctx));
        }
        if (expr instanceof REqualityExpr eq) {
            return root -> correlatedOracleServe(eq, root, "equality",
                    () -> super.visitEquality(eq, ctx));
        }
        if (expr instanceof RComparisonExpr cmp) {
            return root -> correlatedOracleServe(cmp, root, "comparison",
                    () -> super.visitComparison(cmp, ctx));
        }
        if (expr instanceof RLogicalExpr log) {
            return root -> correlatedOracleServe(log, root, "logical",
                    () -> super.visitLogical(log, ctx));
        }
        if (expr instanceof RArithmeticExpr arith) {
            return root -> correlatedOracleServe(arith, root, "arithmetic",
                    () -> super.visitArithmetic(arith, ctx));
        }
        if (expr instanceof RExistenceExpr exist) {
            return root -> correlatedOracleServe(exist, root, "existence",
                    () -> super.visitExistence(exist, ctx));
        }
        if (expr instanceof RSymbolReference ref && !ref.args().isEmpty()) {
            return root -> correlatedOracleServe(ref, root, "callArgs",
                    () -> super.visitSymbolReference(ref, ctx));
        }
        // #501: the bareMeta at-root seat — the args-EMPTY RSymbolReference sibling of the
        // callArgs leg above (disjoint by the args() guard): the bare meta-annotated attr's
        // claim lowered to IRMetaAccess at root (the adaptBareAttrItemNav meta leg), and the
        // serve is the literal legacy ladder (super.visitSymbolReference routes to
        // ReferenceHandler.handle — the item-nav synthesis + the meta machinery, legacy's own).
        // #502: the at-root CLOSURE-PARAM claims join the SAME leg (an args-empty bare name
        // naming a lambda param — the identical super.visitSymbolReference serve, where
        // legacy's own scope machinery renders the binding), so the family token RECUT
        // bareAttrMeta → bareSymbolRef AHEAD of the drift (the #497 MF-2 stale-narration law;
        // the #501 SOT re-keys 1:1 — bareAttrMeta 193 was the seat's whole population).
        if (expr instanceof RSymbolReference bareRef) {
            return root -> correlatedOracleServe(bareRef, root, "bareSymbolRef",
                    () -> super.visitSymbolReference(bareRef, ctx));
        }
        // #505: the REnumValueRef dispatch leg INSTALLED — the seat conversion retired the
        // L-109c exclusion (visitEnumValueRef's disguised residue consults tryEmitFromIR
        // first), so the enum family's disguised claim roots reach this builder: the
        // oracle-leaf-bearing shapes (the closure-param-head chains, the IRSynItemNav-based
        // spines, the choice-option navs, the dispatch-input refs, the IRSymbolNav and
        // IRMetaAccess roots) serve through the literal super.visitEnumValueRef line —
        // legacy's whole disguised ladder (the item-chain synthesis, the choice projection,
        // the dispatch scope-join) runs inside it, byte-identical BY IDENTITY; every
        // re-entrant interior visit claims at its own seat (the #504 re-entrant law).
        if (expr instanceof REnumValueRef enumChain && isConvertedEnumChainSite(enumChain)) {
            return root -> correlatedOracleServe(enumChain, root, "enumChain",
                    () -> super.visitEnumValueRef(enumChain, ctx));
        }
        // #507: the RDeepFeatureCall dispatch leg INSTALLED — the visit conversion targets the
        // family (tryEmitFromIR first; the untargeted meter's RDeepFeatureCall row going to
        // zero is the teach's own conservation signature), so the family's claim roots reach
        // this builder: the IRDeepFeatureNav-rooted lowerings serve through the literal
        // super.visitDeepFeatureCall line — legacy's whole deep-path ladder (the generated
        // DeepPathUtil.choose<Feature> routing) runs inside it, byte-identical BY IDENTITY;
        // every re-entrant interior visit claims at its own seat (the #504 re-entrant law).
        if (expr instanceof RDeepFeatureCall deepChain) {
            return root -> correlatedOracleServe(deepChain, root, "deepChain",
                    () -> super.visitDeepFeatureCall(deepChain, ctx));
        }
        // #513: the noAdaptArm-sweep dispatch legs INSTALLED — the seven visit conversions
        // target the families (tryEmitFromIR first; the untargeted meter's rows going to
        // zero are the teach's own conservation signature), so their claim roots reach this
        // builder: each serves through its literal super.visitX line — legacy's whole
        // family render (the switch ladder, the null-safe default composition, the
        // contains/disjoint runtime wraps, the CollectionHandler comparator forms) runs
        // inside it, byte-identical BY IDENTITY; every re-entrant interior visit claims at
        // its own seat (the #504 re-entrant law).
        if (expr instanceof RSwitchExpr switchOp) {
            return root -> correlatedOracleServe(switchOp, root, "switchOp",
                    () -> super.visitSwitch(switchOp, ctx));
        }
        if (expr instanceof RDefaultExpr defaultOp) {
            return root -> correlatedOracleServe(defaultOp, root, "defaultOp",
                    () -> super.visitDefault(defaultOp, ctx));
        }
        if (expr instanceof RContainsExpr containsOp) {
            return root -> correlatedOracleServe(containsOp, root, "contains",
                    () -> super.visitContains(containsOp, ctx));
        }
        if (expr instanceof RDisjointExpr disjointOp) {
            return root -> correlatedOracleServe(disjointOp, root, "disjoint",
                    () -> super.visitDisjoint(disjointOp, ctx));
        }
        if (expr instanceof RMaxExpr maxOp) {
            return root -> correlatedOracleServe(maxOp, root, "maxOp",
                    () -> super.visitMax(maxOp, ctx));
        }
        if (expr instanceof RMinExpr minOp) {
            return root -> correlatedOracleServe(minOp, root, "minOp",
                    () -> super.visitMin(minOp, ctx));
        }
        if (expr instanceof RSortExpr sortOp) {
            return root -> correlatedOracleServe(sortOp, root, "sortOp",
                    () -> super.visitSort(sortOp, ctx));
        }
        // #515: the untargeted-close dispatch legs INSTALLED — the two visit conversions
        // target the last big no-IR-attempt families (tryEmitFromIR first; the untargeted
        // meter's RWithMetaExpr/RJoinExpr rows going to zero are the teach's own
        // conservation signature), so their claim roots reach this builder: each serves
        // through its literal super.visitX line — legacy's whole family render (the
        // FieldWithMetaX builder threading + scheme/reference/id entry setters; the
        // join(...) runtime wrap with its optional separator) runs inside it,
        // byte-identical BY IDENTITY; every re-entrant interior visit claims at its own
        // seat (the #504 re-entrant law).
        if (expr instanceof RWithMetaExpr withMetaOp) {
            return root -> correlatedOracleServe(withMetaOp, root, "withMetaOp",
                    () -> super.visitWithMeta(withMetaOp, ctx));
        }
        if (expr instanceof RJoinExpr joinOp) {
            return root -> correlatedOracleServe(joinOp, root, "joinOp",
                    () -> super.visitJoin(joinOp, ctx));
        }
        return null;
    }

    /**
     * The shared serve step for {@link #buildOracleRootRenderer}'s dispatch lambdas: range-
     * correlate the IR node against the captured raw claim root (null-tolerant
     * {@code Objects.equals} — the routing-soundness convention), render the literal legacy line,
     * bump the {@link #oracleRootLoweredByFamily} receipt under {@code familyToken}.
     */
    private JavaStatementBuilder correlatedOracleServe(RExpression site, IRExpr root,
            String familyToken, java.util.function.Supplier<JavaStatementBuilder> render) {
        if (!Objects.equals(site.sourceRange(), root.sourceRange())) {
            return null; // uncorrelated — never render a site the correlation cannot prove
        }
        JavaStatementBuilder rendered = render.get();
        oracleRootLoweredByFamily.merge(familyToken, 1, Integer::sum);
        return rendered;
    }

    /**
     * Build the collection-op renderer for a flat list op ({@link RListOpExpr} distinct/flatten/first/last/reverse
     * — + only-element since the #498 teach — or
     * {@link RCountExpr} count) reached during this emission (L-050) — the full
     * {@code distinct(<arg>)} / {@code <arg>.first()} / {@code <arg>.resultCount()} /
     * {@code <arg>.get()} render. Reproducing the flat wrap
     * lab-side would mean re-deriving the op→method mapping, the chain link and the {@code distinct} static-wildcard
     * import (a parity risk the L-029 split forbids), so the renderer calls legacy {@code CollectionHandler.handle(...)}
     * VERBATIM on this compiler's own {@link com.regnosys.rosetta.generator.java.expression.handlers.CollectionHandler}
     * (the exact instance the legacy fallback would use), capturing the AST node + the live {@link ExpressionContext} —
     * byte-identical to Path-1 by construction. Reached only when the adapter admitted the {@link com.regnosys.rosetta.ir.expr.IRListOp}
     * (i.e. the receiver subtree lowered), so the IR genuinely represents a fully-lowered collection subtree; the
     * argument is re-compiled through {@code compileInterior} inside the oracle (a NORMAL recursion through this IR
     * compiler, byte-transparent — no synthetic re-entry). Returns {@code null} for a non-list-op top-level expression.
     */
    private CollectionOpRenderer buildCollectionOpRenderer(RExpression expr, ExpressionContext ctx) {
        if (expr instanceof RListOpExpr listOp) {
            return () -> Optional.of(getCollectionHandler().handle(listOp, ctx, this));
        }
        if (expr instanceof RCountExpr count) {
            return () -> Optional.of(getCollectionHandler().handle(count, ctx, this));
        }
        return null;
    }

    /**
     * Build the to-string renderer for a {@code to-string} conversion reached during this emission — the COMPLETE
     * render by reusing legacy {@code ConversionHandler.handle(RToStringExpr)} VERBATIM on the compiler's own handler
     * instance (the exact instance the legacy fallback would use), capturing the AST node + the live
     * {@link ExpressionContext} — byte-identical to Path-1 by construction (the same call {@code super.visitToString}
     * makes). The argument is re-compiled through {@code compiler.compile} inside the oracle (a NORMAL recursion
     * through this IR compiler, byte-transparent). Returns {@code null} for a non-to-string top-level expression.
     */
    private ToStringRenderer buildToStringRenderer(RExpression expr, ExpressionContext ctx) {
        if (expr instanceof RToStringExpr toString) {
            return () -> Optional.of(getConversionHandler().handle(toString, ctx, this));
        }
        return null;
    }

    /**
     * Build the {@link ImplicitItemRenderer} for a top-level emission (the #469 itemCallArg teach) — the LIVE
     * scope binding of every lambda-bound implicit {@code item} in the claimed subtree, supplied by calling
     * legacy {@code ReferenceHandler.handle(RImplicitVariable)} VERBATIM on this compiler's own handler with
     * the live {@link ExpressionContext} (the established oracle-reuse closure — the binding decision is
     * always legacy's own: a named extract parameter, a collision-escaped {@code _item}, a piped binding, or
     * the plain {@code item}). The {@link com.regnosys.rosetta.ir.expr.IRVariable} is correlated back to its
     * AST node by the SOURCE RANGE the adapter stamps at lowering; only NON-synthetic items are indexed (the
     * adapter's {@code USER_ITEM} kind is exactly the non-synthetic sub-family, and a synthesized node can
     * share a range with the construct that synthesized it). Returns {@code null} for an item-free subtree
     * (→ the emitter's historical bare render, vacuously unreached); an installed renderer returning
     * {@code null} for an uncorrelated variable declines the claim — never guess a binding.
     */
    private ImplicitItemRenderer buildImplicitItemRenderer(RExpression expr, ExpressionContext ctx) {
        Map<SourceRange, RImplicitVariable> itemsByRange = new HashMap<>();
        indexImplicitItems(expr, itemsByRange);
        if (itemsByRange.isEmpty()) {
            return null;
        }
        return item -> {
            RImplicitVariable node = itemsByRange.get(item.sourceRange());
            if (node == null) {
                return null;
            }
            return getReferenceHandler().handle(node, ctx, this);
        };
    }

    /**
     * Index every NON-synthetic implicit {@code item} in the claimed subtree by its own {@link SourceRange} —
     * the correlation key the adapter stamps on the lowered {@code USER_ITEM} variable (the #469 itemCallArg
     * teach). Synthetic implicits are deliberately excluded: the adapter lowers them as
     * {@code SYNTHETIC_ITEM} (served by the SEPARATE #479 {@link #buildSyntheticItemRenderer} channel —
     * synthetic ranges can collide with their synthesizing construct, so that index carries its own
     * collision poisoning), and a synthesized node can share a source range with its synthesizing construct.
     */
    private static void indexImplicitItems(RNode node, Map<SourceRange, RImplicitVariable> out) {
        if (node instanceof RImplicitVariable iv && !iv.isSynthetic()) {
            out.put(iv.sourceRange(), iv);
        }
        for (RNode child : node.children()) {
            indexImplicitItems(child, out);
        }
    }

    /**
     * Build the SYNTHETIC-item renderer for a top-level emission (the #479 filter/extract
     * synthetic-item receiver teach) — the same oracle-reuse closure as
     * {@link #buildImplicitItemRenderer} over the SYNTHETIC sub-family: the compiler correlates the
     * lowered {@link com.regnosys.rosetta.ir.expr.IRVariable} back to its synthetic AST node by the
     * SOURCE RANGE the adapter stamps at lowering and calls legacy
     * {@code ReferenceHandler.handle(RImplicitVariable)} VERBATIM with the live
     * {@link ExpressionContext} — the binding decision is always legacy's own (the lambda's
     * {@code item}, a depth-escaped {@code _item}, or a named parameter). A synthesized node can
     * SHARE a range with its synthesizing construct, so the index poisons any range claimed by two
     * DISTINCT synthetic nodes ({@code null}ed — the renderer then declines the claim rather than
     * guessing which binding legacy resolves; the #469 never-guess law). Returns {@code null} for a
     * subtree with no synthetic items (→ the emitter declines any synthetic render, vacuously
     * unreached — the adapter only admits shapes this renderer serves).
     */
    private ImplicitItemRenderer buildSyntheticItemRenderer(RExpression expr, ExpressionContext ctx) {
        Map<SourceRange, RNode> itemsByRange = new HashMap<>();
        indexSyntheticItems(expr, itemsByRange);
        if (itemsByRange.isEmpty()) {
            return null;
        }
        return item -> {
            RNode node = itemsByRange.get(item.sourceRange());
            if (node == null) {
                return null; // uncorrelated OR poisoned (ambiguous) range — decline, never guess
            }
            if (node instanceof RImplicitVariable iv) {
                return getReferenceHandler().handle(iv, ctx, this);
            }
            // #480: a raw bare-attr/chain claim node (the arms stamp the RAW node's range on the
            // lowered synthetic base) — build the fresh synthetic implicit legacy ITSELF builds on
            // this exact render path (synthesizeImplicitItemNavigation/-Chain: parented at the
            // node's own parent, so the binding walk is the raw node's) and hand it to the oracle
            // verbatim — the binding decision stays legacy's own.
            RImplicitVariable fresh = new RImplicitVariable();
            fresh.setSynthetic(true);
            fresh.setParent(node.parent());
            return getReferenceHandler().handle(fresh, ctx, this);
        };
    }

    /**
     * Index every SYNTHETIC implicit item in the claimed subtree by its {@link SourceRange} — plus,
     * since #480, every raw bare-attr/chain CLAIM node (an argument-less attribute-bound
     * {@link RSymbolReference} / a bound-chain {@link REnumValueRef} — the #480 arms stamp these
     * nodes' ranges on their lowered synthetic bases, and the renderer resolves the lambda binding
     * through a fresh implicit parented at the raw node) — poisoning (nulling) any range two
     * DISTINCT nodes share — the #479 correlation map with the ambiguity hazard the #469 USER-item
     * index documented made EXPLICIT: a synthesized node's range is its synthesizing construct's,
     * so two elided operands in one construct can collide; a poisoned range makes the renderer
     * decline instead of serving the wrong node's binding. EVERY candidate-shaped raw node in the
     * subtree indexes (not only arm-claimed ones), so an unrelated same-range candidate can
     * over-poison and forfeit a claim — decline-only, byte-safe (the Seat-1 #480 OBS-3 surface).
     * The synthetic-implicit candidates index
     * at ANY range including {@link SourceRange#NONE} (the pre-#480 behavior — load-bearing for
     * the legacy-synthesized RE-ENTRANT equivalents, whose fresh implicits carry NONE and
     * correlate NONE≡NONE, poisoned when ambiguous); the #480 raw-node candidates skip NONE (a
     * parsed claim node always carries a real range, and an unrelated NONE key would only widen
     * the poison surface).
     */
    private static void indexSyntheticItems(RNode node, Map<SourceRange, RNode> out) {
        boolean candidate = (node instanceof RImplicitVariable iv && iv.isSynthetic())
                || (!SourceRange.NONE.equals(node.sourceRange())
                        && ((node instanceof RSymbolReference sym && sym.args().isEmpty()
                                && sym.symbol().orElse(null) instanceof RAttribute)
                            || (node instanceof REnumValueRef enr
                                && enr.resolvedAttributeChain().isPresent())));
        if (candidate) {
            SourceRange range = node.sourceRange();
            if (out.containsKey(range) && out.get(range) != node) {
                out.put(range, null); // poison: ambiguous correlation
            } else {
                out.put(range, node);
            }
        }
        for (RNode child : node.children()) {
            indexSyntheticItems(child, out);
        }
    }

    /**
     * Build the {@link ItemNavRenderer} for a top-level emission (the #469 multiHopItemNav + metaItemReceiver
     * teach) — the complete legacy render of a guarded item-rooted navigation that IS the whole claim (the
     * ROOT position ONLY — Copilot #469 C-2 contract precision): the chained family (per-hop
     * deferred-sentinel lambda naming the emitter's node-local {@code disambiguate} cannot reproduce —
     * {@code registerDeferredLambdaParam} resolves escapes at method finalization) and the
     * meta-typed-item-receiver family (legacy's {@code .map("Type coercion", …getValue())} deref ahead of the
     * hop). The root nav is served by {@code super.visitFeatureCall} — the EXACT legacy fallback render, so
     * the bytes are identical to the decline path by the strongest argument; only the §4.2 counters move. A
     * NESTED guarded nav is never served: its whole claim declines at the guard scan instead (the
     * composition boundary — the cpON4 drift receipt). The oracle-presence gate mirrors the guard arms' own
     * ({@code null} oracles → nothing was ever declined → the emitter's hop-by-hop render proceeds
     * unchanged). Returns {@code null} for a claim whose root is not a guarded nav; a non-root node at
     * consult time returns {@code null} → the emitter's own render.
     */
    private ItemNavRenderer buildItemNavRenderer(RExpression expr, ExpressionContext ctx) {
        if (getTypeUtil() == null || getTypeTranslator() == null || getGeneratorModel() == null) {
            return null; // mirrors the guard arms' oracle-presence gate
        }
        // ROOT position ONLY (the #469 composition boundary): the renderer serves a guarded nav only when
        // it IS the whole claim — there super.visitFeatureCall is literally the decline's own call, so the
        // bytes are identical by construction. A nested guarded nav never reaches the emitter (the guard
        // declines its whole claim — see scanForPostPinCoercion's nested-only item-nav arm; the cpON4 drift
        // receipt is why the delegation must not reach inside IR-rendered parents).
        if (!(expr instanceof RFeatureCall rootNav) || !isItemRootedFeatureCall(rootNav)
                || !isGuardedItemNav(rootNav)) {
            return null;
        }
        SourceRange rootRange = rootNav.sourceRange();
        return nav -> {
            if (!rootRange.equals(nav.sourceRange())) {
                return null; // not the root nav — the emitter's own hop-by-hop render proceeds
            }
            return super.visitFeatureCall(rootNav, ctx);
        };
    }

    /**
     * Whether an item-rooted navigation belongs to the legacy-delegated family — EXACTLY the predicates the
     * retired MULTI_HOP_ITEM_NAV / META_ITEM_RECEIVER guard arms declined on: a chained receiver, or a
     * single-hop whose implicit-item base is meta-typed (both probes, belt + braces — the legacy recovery
     * walk plus this compiler's own then-pipe-threading owner-chain walk).
     */
    private boolean isGuardedItemNav(RFeatureCall fc) {
        if (fc.receiver() instanceof RFeatureCall) {
            return true; // the chained family
        }
        RExpression base = fc.receiver();
        JavaType itemMapperType = NavigationHandler.implicitItemArgMeta(base, this);
        if (itemMapperType != null
                && getTypeUtil().getItemType(itemMapperType) instanceof RJavaWithMetaValue) {
            return true;
        }
        return base instanceof RImplicitVariable item && itemOwnerChainTerminalIsMeta(item);
    }

    /**
     * Build the call-param-cardinality resolver for the function CALLS reached during this emission — for each
     * (callee, argument position), whether the callee parameter is declared MULTI (the {@code .getMulti()} vs
     * {@code .get()} arg-unwrap decision, legacy facet {@code tailMulti}). Reuses legacy
     * {@code ReferenceHandler.evaluateParamIsMulti} (a {@code GeneratorModel.isMulti} of the callee's positional
     * input) as the oracle, so the accessor matches Path-1's {@code paramAcceptsMulti} byte-for-byte and the
     * cardinality is not re-derived.
     *
     * <p>#489: PER-CALL via {@link #indexFunctionCallSites} range correlation (the
     * {@link #buildCallReceiverResolver} mechanism verbatim) — the pre-#489 form captured the callee from the
     * top-level expression only, on the "an IR-driven {@link IRApply} is only ever the top-level expression"
     * invariant, which the #489 nav-arg admission broke twice over, both byte-visible on the ring's drr decode:
     * a claimed call nested under an operator ({@code … exists} / a filter lambda) read a NULL resolver, so every
     * accessor defaulted to {@code .get()} where golden reads {@code .getMulti()} (Beneficiary1IdentifierType-
     * IndicatorRule / FirstExerciseDateRule); and a NESTED call's args resolved against the OUTER callee's
     * positions (Direction1BuyerParty — {@code .getMulti()} where golden reads {@code .get()}). An uncorrelated
     * callee reads {@code false} (the scalar default), mirroring the receiver resolver's decline-never-guess.
     * Returns {@code null} when the claimed subtree contains no resolved call site.
     */
    private CallParamMultiResolver buildCallParamMultiResolver(RExpression expr) {
        Map<SourceRange, RFunction> calleesByRange = new HashMap<>();
        indexFunctionCallSites(expr, calleesByRange);
        if (calleesByRange.isEmpty()) {
            return null;
        }
        return (callee, argIndex) -> {
            RFunction fn = calleesByRange.get(callee.sourceRange());
            return fn != null && ReferenceHandler.evaluateParamIsMulti(fn, argIndex, this);
        };
    }

    /**
     * Whether a bare RULE delegation must DECLINE to legacy. The IR-drives set (the negation of this predicate —
     * {@code enclosingRule != null && (nearestEnclosingInlineFunction != null || fromType present)}) is EXACTLY legacy's
     * full {@code renderImplicitRuleInvocation} dispatch (the {@code caseSymbolReference} gate at
     * {@code ReferenceHandler}): both the in-lambda {@code item.get()}/{@code thenArg.get()} sub-case (L-049, rendered by
     * reusing the oracle verbatim) and the top-level {@code input} sub-case (L-045). It reuses legacy's own parent-walk
     * oracles on the same AST node, so the claim cannot disagree with legacy's branch selection. Declines ONLY when:
     * there is no enclosing rule ({@code findEnclosingRule == null} — a bare rule reference in a plain function
     * body; v3.1 flip seat 33 law B.3 measured that the {@code @Inject} rule field IS generated there and taught
     * LEGACY to render the in-lambda shape, so this arm declines by ROUTE OWNERSHIP, not because the emission is
     * missing); or the reference is at rule-body top level
     * ({@code nearestEnclosingInlineFunction == null}) AND the enclosing rule declares no from-type
     * ({@code fromType().isEmpty()} — legacy falls through to the variable path rather than emit an untyped
     * {@code input}). Every in-lambda, in-rule reference drives regardless of from-type (the lambda binding does not
     * need it); every from-typed top-level, in-rule reference drives.
     */
    private boolean ruleDelegationDeclines(RExpression expr) {
        RRule enclosingRule = HandlerHelper.findEnclosingRule(expr);
        if (enclosingRule == null) {
            return true; // no enclosing rule (a plain function body) → no @Inject rule field is generated
        }
        // v3.1 phase C, C0 item 2b — THE ONE IR-ONLY BAND REGRESSION.
        //
        // A rule-body TOP-LEVEL delegation is the one rule-delegation shape this route
        // renders itself rather than through the legacy oracle: the in-lambda arm calls
        // ReferenceHandler.renderImplicitRuleInvocation VERBATIM (buildRuleDelegationRenderer),
        // but the top-level arm rebuilds the render in IRJavaLeafEmitter#emitRuleDelegation as
        // MapperS.of(<Name>Rule.evaluate(input)) — unconditionally SINGLE. Legacy's own
        // renderer carries an arm that rebuild does not: facet bodyMultiCardinality (PR #274),
        // which wraps MapperC.<Elem>of(…) when the INVOKED sub-rule's body is rule-aware MULTI,
        // because evaluate() then returns a List and the single wrap assigns a List to a
        // MapperS<Elem> — code that does not compile.
        //
        // The band's three-route survey found exactly ONE file where the IR route was worse
        // than Path-1, and this is it: drr 5.61.0
        // techsprint/g20/mas/reports/PriorUniqueTransactionIdentifierRule — `PriorVersion then
        // extract … then flatten then last`, whose PriorVersion sub-rule navigates to a multi
        // `trade`. Golden and Path-1: `MapperC.<Trade>of(priorVersionRule.evaluate(input))`;
        // this route emitted `MapperS.of(…)` plus a spurious MapperS import.
        //
        // The fix DECLINES the shape rather than reimplementing #274's witness/collision logic
        // a second time — the route's standing posture for anything the leaf emitter cannot
        // render byte-exactly, and byte-identity with Path-1 then holds by construction rather
        // than by a second implementation agreeing with the first. Gated to the top-level arm
        // only: the in-lambda arm already inherits #274 through the verbatim oracle call.
        // Re-claiming this natively is C2 work, once the typed layer carries call-result
        // cardinality (the E3 hole) — not a C0 reimplementation.
        if (ReferenceHandler.nearestEnclosingInlineFunction(expr) == null
                && getGeneratorModel() != null
                && getGeneratorModel().workspace() != null
                && getGeneratorModel().workspace().getRuleBodyCardinality(expr)
                        == ExpressionCardinality.MULTI) {
            return true;
        }
        // In-lambda references drive via the oracle (binding from the lambda); top-level references drive only when a
        // from-type types the synthetic `input`. The disjunction is legacy's exact dispatch condition.
        return ReferenceHandler.nearestEnclosingInlineFunction(expr) == null
                && enclosingRule.fromType().isEmpty();
    }

    /**
     * The alias body's int/number witness for the {@code MapperMaths<R,O,O>} type parameters — classified by
     * recursing the shortcut body through legacy {@code HandlerHelper.numericOperandKind} (the same classifier
     * {@code ArithmeticHandler} feeds the operand-type join, which resolves an alias operand by recursing its
     * body). {@code "Integer"} / {@code "BigDecimal"} for a numeric body; {@code null} (→ the arithmetic
     * declines) for a non-numeric or unresolvable body.
     *
     * <p><strong>Literal-bodied alias declines</strong> — mirroring the explicit guard in legacy
     * {@code HandlerHelper.numericOperandKind}'s alias arm ("a literal must not type its consumption site
     * through an alias", returning UNKNOWN for an {@code RIntLiteral}/{@code RNumberLiteral} body). Legacy
     * classifies the alias OPERAND (the symbol reference), whose alias arm carries that guard; classifying the
     * body directly would MISS it and over-classify. The divergence is byte-visible only for {@code divide}: a
     * literal-bodied-alias DIVIDE sends legacy to its heuristic fallback (which omits the divide-result =
     * {@code BigDecimal} rule), so the typed path's {@code <BigDecimal, Integer, Integer>divide} would differ
     * from legacy's {@code <Integer, Integer, Integer>divide}. Declining keeps it byte-identical (legacy renders
     * it). [Found by adversarial review; not corpus-exercised — no function binds an alias to a bare literal then
     * divides by it.]
     */
    String aliasNumericWitness(RExpression aliasBody) {
        if (aliasBody instanceof RIntLiteral || aliasBody instanceof RNumberLiteral) {
            return null;
        }
        return switch (HandlerHelper.numericOperandKind(aliasBody, this)) {
            case INT -> "Integer";
            case NUMBER -> "BigDecimal";
            case UNKNOWN -> null;
        };
    }

    /**
     * The enclosing function OUTPUT's Java type, for the emitter's witness/output simple-name collision
     * check — byte-identical to the output resolution in legacy
     * {@code ReferenceHandler.mapperCWitnessOutputCollisionFqn} / {@code NavigationHandler.witnessOutputCollisionFqn}:
     * walk to the enclosing {@link RFunction}, take its declared output, and map the resolved type through the
     * translator. Returns {@code null} (⇒ no collision, bare witness) for a non-function seam (rule / data-type
     * condition), a function with no output, a META-annotated output (whose emitted type is the meta wrapper,
     * which never shares the always-plain witness's simple name), or an unresolved output type.
     */
    private JavaClass<?> enclosingFunctionOutputJavaType(RExpression expr) {
        RFunction fn = HandlerHelper.findEnclosingFunction(expr);
        if (fn == null) {
            return null;
        }
        RAttribute out = fn.output().orElse(null);
        if (out == null || out.typeCall() == null) {
            return null;
        }
        if (MetaFieldGenerator.detectMetaKind(out) != MetaFieldGenerator.MetaKind.NONE) {
            return null;
        }
        GeneratorModel gm = getGeneratorModel();
        JavaTypeTranslator tt = getTypeTranslator();
        if (gm == null || tt == null) {
            return null;
        }
        RType outputRt = gm.resolveTypeCall(out.typeCall());
        if (outputRt == null || outputRt instanceof RMissingType) {
            return null;
        }
        return tt.toJavaReferenceType(outputRt);
    }

    /** Number of expressions emitted from the IR (not legacy fallback) by this compiler. */
    public int irDrivenCount() {
        return irDrivenCount;
    }

    /**
     * The #493 driven-metric split total — see {@link #irDrivenDelegatedCount}: the delegated share
     * of {@link #irDrivenCount()} (claim-root renders that are wholesale legacy fallback/oracle
     * renders, counted driven at the eight relabel/belt/delegation seats). The D11 ring prints
     * {@code lowered = irDrivenCount() − irDrivenDelegatedCount()} next to it and HARD-asserts the
     * two-channel conservation ({@code == Σ} of the eight per-seat sub-counters).
     */
    public int irDrivenDelegatedCount() {
        return irDrivenDelegatedCount;
    }

    /** Number of top-level genuine enum-value references driven from the IR (slice L-069 firing oracle). */
    public int enumValueRefDrivenCount() {
        return enumValueRefDrivenCount;
    }

    /**
     * #505: the converted disguised-REnumValueRef seat's driven receipt — see
     * {@link #enumChainRootDrivenCount}.
     */
    public int enumChainRootDrivenCount() {
        return enumChainRootDrivenCount;
    }

    /** Number of {@code to-string} conversions driven from the IR (slice L-070 firing oracle). */
    public int toStringDrivenCount() {
        return toStringDrivenCount;
    }

    /** Number of top-level item-rooted nav chains driven from the IR (the extract/filter slice firing oracle). */
    public int itemNavDrivenCount() {
        return itemNavDrivenCount;
    }

    /** Number of checks driven from the IR whose operand is the bare filter/extract {@code item} (the firing oracle). */
    public int itemOperandDrivenCount() {
        return itemOperandDrivenCount;
    }

    /** Firing oracle for the call-as-nav-base slice — driven navigations whose chain base is a function call. */
    public int callBaseNavDrivenCount() {
        return callBaseNavDrivenCount;
    }

    /** Firing oracle for the L-105 call-as-existence-operand slice — driven `exists` checks whose operand is a call. */
    public int callExistenceOperandDrivenCount() {
        return callExistenceOperandDrivenCount;
    }

    /** Firing oracle for the L-106 call-as-cmp/eq-operand slice — driven comparison/equality with a call operand. */
    public int callCmpEqOperandDrivenCount() {
        return callCmpEqOperandDrivenCount;
    }

    /** Firing oracle for the L-107 call-as-nested-arg slice — driven calls carrying a nested function-call argument. */
    public int callNestedArgDrivenCount() {
        return callNestedArgDrivenCount;
    }

    /** Firing oracle for the L-108 call-as-item-arg slice — driven calls carrying a filter/extract-bound item argument. */
    public int callItemArgDrivenCount() {
        return callItemArgDrivenCount;
    }

    /** L-109 point-free function-application firing oracle (see {@link #pointFreeFnDrivenCount}). */
    public int pointFreeFnDrivenCount() {
        return pointFreeFnDrivenCount;
    }

    /** L-109d meta-feature navigation firing oracle (see {@link #metaNavDrivenCount}). */
    public int metaNavDrivenCount() {
        return metaNavDrivenCount;
    }

    /** L-109e implicit-input navigation firing oracle (see {@link #implicitInputNavDrivenCount}). */
    public int implicitInputNavDrivenCount() {
        return implicitInputNavDrivenCount;
    }

    /**
     * Mirror of the adapter's {@code isMetaAnnotated} (the {@code metadata}-annotation test) — whether an attribute
     * carries the {@code metadata} annotation, so a navigation to it needs the gm-aware with-meta render the neutral
     * IR delegates to the legacy handler (L-109d).
     */
    private static boolean isMetaAnnotated(RAttribute attr) {
        for (var ar : attr.annotationRefs()) {
            if ("metadata".equals(ar.annotationName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Number of expressions in an IR-targeted family that fell back to legacy. The denominator of the
     * §4.2 progress metric is {@code irDrivenCount() + irDeclinedCount()} (nodes the IR attempted).
     */
    public int irDeclinedCount() {
        return irDeclinedCount;
    }

    /**
     * Number of claims declined to legacy by the #467 post-pin coercion guard
     * ({@link #subtreeTripsPostPinCoercion}). Since the #472 router this reads the honest RESIDUE only
     * (tripped claims whose root class is outside the proven delegation set — ZERO on the frozen corpus's
     * FUNCTION seam since the #473 endgame teach AND on its RULE seam since the #474 intLiteral teach),
     * and the D11 ON-gate's retensed locks read it INVERTED — {@code == 0} is the pinned fully-taught
     * state (a new decline is a claim-root-seat un-fire or a NEW decline family, triaged never absorbed):
     * on ALL THREE FUNCTION carrier cells (the drr pin joined at #473) and on EVERY cell of the RULE-seam
     * reader block (the #474 pojo-pass funcGen readers — that seam's whole trip population is drr), while
     * the guard seat's firing witness lives on {@link #postPinServeLoweredCount()} {@code > 0} (the
     * #470→#472→#516 lock lineage: guard-decline > 0 → delegated > 0 → served > 0; drr-only on the
     * RULE seam, where the router's 13 claims are
     * the population). Every guard decline also increments {@link #irDeclinedCount()}, so this counter is
     * a subset witness, not extra denominator.
     */
    public int postPinCoercionDeclinedCount() {
        return postPinCoercionDeclinedCount;
    }

    /**
     * The per-arm breakdown of {@link #postPinCoercionDeclinedCount()} — the share-growth wave's meter
     * (PR #469), read reflectively by the D11 flag-on ring's ON-gate reader and printed under each cell's
     * share line. One {@code label=count} token per {@link PostPinArm} in declaration order, EVERY arm
     * printed (a taught arm's explicit ZERO is the wave's per-arm success signal). Attribution is
     * first-trip in the guard's scan order, so the counts sum exactly to the scalar total (both are
     * incremented together at the single guard seat).
     */
    public String postPinCoercionDeclineBreakdown() {
        StringBuilder sb = new StringBuilder();
        for (PostPinArm arm : PostPinArm.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(arm.label()).append('=').append(postPinCoercionDeclinesByArm[arm.ordinal()]);
        }
        return sb.toString();
    }

    /**
     * The #472 router's pre-conversion firing total — claims the guard seat delegated WHOLE to the
     * caller's own legacy fallback ({@link #compositionRootDelegation}). The FROZEN ZERO since the #516
     * serve conversion (no code path increments it — the seat counts LOWERED on
     * {@link #postPinServeLoweredCount} with the render unchanged); the D11 ON-gate carrier locks that
     * pinned {@code delegated > 0} RETENSED to the serve receipt at #516 (declined {@code == 0} stays
     * pinned unchanged — the #473/#474 fully-taught state).
     */
    public int postPinDelegatedCount() {
        return postPinDelegatedCount;
    }

    /** The claim-root seat's pre-#516 firing sub-counter (see {@link #claimRootDelegatedCount}). */
    public int claimRootDelegatedCount() {
        return claimRootDelegatedCount;
    }

    /**
     * The #516 delegated-seat serve's postPin flip receipt (see {@link #postPinServeLoweredCount}) —
     * the D11 ON-gate's retensed firing witness on all three FUNCTION carrier cells ({@code > 0};
     * the #472→#516 lock lineage: guard-decline {@code > 0} → delegated {@code > 0} → served
     * {@code > 0}, the scan's un-fire witness at every step) and on the pojo-pass RULE-seam
     * reader's drr cell.
     */
    public int postPinServeLoweredCount() {
        return postPinServeLoweredCount;
    }

    /** The #516 delegated-seat serve's claim-root flip receipt (see {@link #claimRootServeLoweredCount}). */
    public int claimRootServeLoweredCount() {
        return claimRootServeLoweredCount;
    }

    /**
     * The per-arm delegation meter behind the D11 {@code postPin delegated by arm} receipt line — the same
     * token format as {@link #postPinCoercionDeclineBreakdown()}. The frozen-zero honest-residue meter
     * since the #516 serve conversion (the live attribution moved to {@link #postPinServeBreakdown()};
     * served + declined = tripped, per arm, by construction).
     */
    public String postPinDelegationBreakdown() {
        StringBuilder sb = new StringBuilder();
        for (PostPinArm arm : PostPinArm.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(arm.label()).append('=').append(postPinDelegationsByArm[arm.ordinal()]);
        }
        return sb.toString();
    }

    /**
     * The per-arm serve meter behind the D11 {@code postPin served by arm} receipt line (#516 — the
     * delegation meter's successor channel): the same token format as
     * {@link #postPinCoercionDeclineBreakdown()}, over {@link #postPinServesByArm}; Σ ≡
     * {@link #postPinServeLoweredCount()} per cell by construction.
     */
    public String postPinServeBreakdown() {
        StringBuilder sb = new StringBuilder();
        for (PostPinArm arm : PostPinArm.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(arm.label()).append('=').append(postPinServesByArm[arm.ordinal()]);
        }
        return sb.toString();
    }

    /**
     * The claim-root seat's per-arm serve census line ({@code claimRoot served by arm}) — the seat's
     * FIRST per-arm decode (#516). The same full-enum token format (the four ACCEPTED evaluate-arg
     * arms are the only labels that can read non-zero — see {@link #claimRootDelegationArm}); Σ ≡
     * {@link #claimRootServeLoweredCount()} per cell by construction.
     */
    public String claimRootServeBreakdown() {
        StringBuilder sb = new StringBuilder();
        for (PostPinArm arm : PostPinArm.values()) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(arm.label()).append('=').append(claimRootServesByArm[arm.ordinal()]);
        }
        return sb.toString();
    }

    /**
     * The #475 census: {@link #irDeclinedCount()}'s per-family breakdown ({@link #irDeclinedByFamily}),
     * read reflectively by the D11 ON-gate readers and printed under each cell's share line. Same
     * {@code key=count} token format as {@link #postPinCoercionDeclineBreakdown()}, but over an
     * OPEN key set (claim-root class simple names), so tokens are ranked count-DESCENDING (ties by key,
     * ascending) — a census wants its worklist pre-sorted — and only non-zero families print
     * ({@code none} when the map is empty, e.g. a cell with zero attempted claims). The tokens sum to
     * the scalar by construction (the {@link #recordDecline} single-seat law).
     */
    public String irDeclineFamilyBreakdown() {
        return rankedBreakdown(irDeclinedByFamily);
    }

    /**
     * The #475 census's per-site companion ({@link #irDeclinedBySite}): the same ranked token format as
     * {@link #irDeclineFamilyBreakdown()} over the five decline seats — {@code adapterGap} /
     * {@code postPinGuard} / {@code ruleDelegation} / {@code aliasResolution} / {@code leafEmitter}.
     * Splits the worklist by owner: an {@code adapterGap} count is rune-ir teach work (no adapt arm),
     * a {@code leafEmitter} count is rune-ir-java teach work (no emitter arm).
     */
    public String irDeclineSiteBreakdown() {
        return rankedBreakdown(irDeclinedBySite);
    }

    /**
     * The #531 W-facet belt census reader ({@link #wAliasGateFacets}): ranked {@code key=count}
     * tokens over the {@link #tryEmitAlias} seat's per-exit population ({@code none} when the
     * probe is off — the standing receipts' state). Printed by the D11 reader on its own
     * non-empty guard OUTSIDE the {@code blockerProbed > 0} gate (the closed-board census
     * channel — the map field's javadoc narrates why it cannot ride the adapterGap gate).
     */
    public String wAliasGateBreakdown() {
        return rankedBreakdown(wAliasGateFacets);
    }

    /**
     * The #475 census's UNTARGETED axis ({@link #untargetedVisitsByFamily}): ranked {@code key=count}
     * tokens for the 20 families with no IR attempt at all — the population outside the §4.2
     * denominator that the share line could never see. {@link #untargetedVisitCount()} is its total;
     * {@code irDeclined + untargetedVisits} together are the whole not-IR-driven node population the
     * 100% goal must burn down.
     */
    public String untargetedVisitBreakdown() {
        return rankedBreakdown(untargetedVisitsByFamily);
    }

    /**
     * The #491 implicitVisit facet probe's reader ({@link #implicitVisitFacets}): ranked
     * {@code implicitVisit.<kind>.<typing>.<context>=count} tokens ({@code none} when the probe
     * is off — the standing receipts' state). Σ tokens ≡ the {@code RImplicitVariable} WHOLE
     * visit population per cell BY CONSTRUCTION (the single-seat bump; driven+declined since the
     * #491 teach — pre-arm it equalled the untargeted flat count); printed by the D11 reader
     * blocks inside the probe gate, next to the census lines it decodes.
     */
    public String implicitVisitFacetBreakdown() {
        return rankedBreakdown(implicitVisitFacets);
    }

    /**
     * The #494 ctorVisit facet probe's reader ({@link #ctorVisitFacets}): ranked
     * {@code ctorVisit.<target>.<typing>.<values>[.spread]=count} tokens ({@code none} when the
     * probe is off — the standing receipts' state). Σ tokens ≡ the {@code RConstructorExpr}
     * WHOLE visit population per cell BY CONSTRUCTION (the single-seat bump; driven+declined
     * since the #494 teach — pre-arm it equalled the untargeted flat count); printed by the D11
     * reader blocks inside the probe gate, next to the census lines it decodes.
     */
    public String ctorVisitFacetBreakdown() {
        return rankedBreakdown(ctorVisitFacets);
    }

    /**
     * The #495 condVisit facet probe's reader ({@link #condVisitFacets}): ranked
     * {@code condVisit.<shape>.<typing>.<lower>[.guardTrip]=count} tokens ({@code none} when the
     * probe is off — the standing receipts' state). Σ tokens ≡ the {@code RConditionalExpr}
     * WHOLE visit population per cell BY CONSTRUCTION (the single-seat bump; driven+declined
     * since the #495 teach — pre-arm it equalled the untargeted flat count); printed by the D11
     * reader blocks inside the probe gate, next to the census lines it decodes.
     */
    public String condVisitFacetBreakdown() {
        return rankedBreakdown(condVisitFacets);
    }

    /**
     * The #496 lambdaVisit facet probe's reader ({@link #lambdaVisitFacets}): ranked
     * {@code lambdaVisit.<family>.<binder>.<typing>.<lower>[.guardTrip]=count} tokens
     * ({@code none} when the probe is off — the standing receipts' state). Σ tokens ≡ the
     * {@code RExtractExpr} + {@code RFilterExpr} WHOLE visit populations per cell BY CONSTRUCTION
     * (the two-seat bump; each family's own Σ ≡ its untargeted flat count pre-arm); printed by
     * the D11 reader blocks inside the probe gate, next to the census lines it decodes.
     */
    public String lambdaVisitFacetBreakdown() {
        return rankedBreakdown(lambdaVisitFacets);
    }

    /**
     * The #496 binder-gate cross-read's reader ({@link #binderGateCrossFacets}): ranked
     * {@code binderGate.<gate>.<face>=count} tokens ({@code none} when the probe is off).
     * Node-occurrence unit over the five gate spellings (the #478 witness convention — no
     * conservation identity against the claim-unit rankings is implied); printed by the D11
     * reader blocks inside the probe gate.
     */
    public String binderGateCrossBreakdown() {
        return rankedBreakdown(binderGateCrossFacets);
    }

    /**
     * The #496 L-111 root census's reader ({@link #inputNavRootFacets}): ranked
     * {@code inputNavRoot.<meta>.<typing>.<lower>[.guardTrip]=count} tokens ({@code none} when
     * the probe is off). Σ tokens ≡ the disguised branch's WHOLE population per cell BY
     * CONSTRUCTION (bumped before the claim/relabel split — since the conversion that is the
     * lowered count + the relabel count together, and the {@code adapterLowers.inputHead} facet
     * ≡ {@link #inputFeatureNavLoweredCount} per cell by the same verdict); printed by the D11
     * reader blocks inside the probe gate.
     */
    public String inputNavRootBreakdown() {
        return rankedBreakdown(inputNavRootFacets);
    }

    /**
     * The #497 metaNav census's reader ({@link #metaNavRootFacets}): ranked
     * {@code metaNavRoot.<metaKind>.<typing>.<recv>=count} tokens ({@code none} when the probe is
     * off). Σ tokens ≡ the L-109d relabel count per cell BY CONSTRUCTION; printed by the D11
     * reader blocks inside the probe gate.
     */
    public String metaNavRootBreakdown() {
        return rankedBreakdown(metaNavRootFacets);
    }

    /**
     * The #497 implicitAttrNav census's reader ({@link #implicitAttrRootFacets}): ranked
     * {@code implicitAttrRoot.<reason>.<binderFace>.<ladder>=count} tokens ({@code none} when the
     * probe is off — the ladder leg is the cut-2 refinement, see the field javadoc). Σ tokens ≡
     * the L-113 relabel count per cell BY CONSTRUCTION (the shipped belt reads the post-arm
     * residue); printed by the D11 reader blocks inside the probe gate.
     */
    public String implicitAttrRootBreakdown() {
        return rankedBreakdown(implicitAttrRootFacets);
    }

    /**
     * The #497 L-112 census's reader ({@link #inputNavReceiverRootFacets}): ranked
     * {@code inputNavReceiverRoot.<typing>.<reason>=count} tokens ({@code none} when the probe is
     * off). Σ tokens ≡ the L-112 relabel count per cell BY CONSTRUCTION; printed by the D11
     * reader blocks inside the probe gate.
     */
    public String inputNavReceiverRootBreakdown() {
        return rankedBreakdown(inputNavReceiverRootFacets);
    }

    /**
     * The #497 implicitAttrNav first-sample witness's reader ({@link #implicitAttrFirstSamples}):
     * {@code bucket e.g. site} pairs, bucket-key order, {@code " | "}-joined (the
     * {@link #navGateWitnessSamples} format); {@code none} when the probe is off.
     */
    public String implicitAttrWitnessSamples() {
        if (implicitAttrFirstSamples.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : implicitAttrFirstSamples.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(" e.g. ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * The #498 leg-A census reader ({@link #onlyElemGateFacets}): ranked
     * {@code onlyElemGate.<pos>.<typing>.<recvVerdict>=count} tokens ({@code none} probe-off).
     */
    public String onlyElemGateBreakdown() {
        return rankedBreakdown(onlyElemGateFacets);
    }

    /**
     * The #498 leg-A first-sample witness reader ({@link #onlyElemGateFirstSamples}):
     * {@code bucket e.g. site} pairs, bucket-key order, {@code " | "}-joined (the
     * {@link #implicitAttrWitnessSamples} format); {@code none} when the probe is off.
     */
    public String onlyElemGateWitnessSamples() {
        return witnessSamplesJoin(onlyElemGateFirstSamples);
    }

    /**
     * The #498 leg-B census reader ({@link #calleeGateFacets}): ranked
     * {@code calleeGate.<pos>.<symbolKind>.<typing>.<enclosing>.<argN>=count} tokens
     * ({@code none} probe-off).
     */
    public String calleeGateBreakdown() {
        return rankedBreakdown(calleeGateFacets);
    }

    /**
     * The #498 leg-B first-sample witness reader ({@link #calleeGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String calleeGateWitnessSamples() {
        return witnessSamplesJoin(calleeGateFirstSamples);
    }

    /**
     * The #499 leg-A census reader ({@link #listOpGateFacets}): ranked
     * {@code listOpGate.<seat>.<pos>.<opKinds>.<rt>.<typing>.<family>=count} tokens
     * ({@code none} probe-off).
     */
    public String listOpGateBreakdown() {
        return rankedBreakdown(listOpGateFacets);
    }

    /**
     * The #499 leg-A first-sample witness reader ({@link #listOpGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String listOpGateWitnessSamples() {
        return witnessSamplesJoin(listOpGateFirstSamples);
    }

    /**
     * The #499 leg-B census reader ({@link #metaFeatureGateFacets}): ranked
     * {@code metaFeatureGate.<pos>.<kind>.<card>.<typing>.<recvVerdict>=count} tokens
     * ({@code none} probe-off).
     */
    public String metaFeatureGateBreakdown() {
        return rankedBreakdown(metaFeatureGateFacets);
    }

    /**
     * The #499 leg-B first-sample witness reader ({@link #metaFeatureGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String metaFeatureGateWitnessSamples() {
        return witnessSamplesJoin(metaFeatureGateFirstSamples);
    }

    /**
     * The #500 arm-A census reader ({@link #metaAccessGateFacets}): ranked
     * {@code metaAccessGate.<seat>.<pos>.<metaKids>.<low#>.<typing>.<family>=count} tokens.
     */
    public String metaAccessGateBreakdown() {
        return rankedBreakdown(metaAccessGateFacets);
    }

    /**
     * The #501 arm-A census reader ({@link #shallowGateFacets}): ranked
     * {@code shallowGate.<seat>.<kind>[.<callee>].<pos>.<kids>.<low#>.<typing>.<family>=count}
     * tokens.
     */
    public String shallowGateBreakdown() {
        return rankedBreakdown(shallowGateFacets);
    }

    /**
     * The #501 arm-A first-sample witness reader ({@link #shallowGateFirstSamples}): the
     * per-bucket site samples.
     */
    public String shallowGateWitnessSamples() {
        return witnessSamplesJoin(shallowGateFirstSamples);
    }

    /**
     * The #501 arm-C census reader ({@link #chainDrainGateFacets}): ranked
     * {@code chainDrainGate.<face>.<pos>.<ctx>.<detail>.<typing>=count} tokens.
     */
    public String chainDrainGateBreakdown() {
        return rankedBreakdown(chainDrainGateFacets);
    }

    /**
     * The #501 arm-C first-sample witness reader ({@link #chainDrainGateFirstSamples}): the
     * per-bucket site samples.
     */
    public String chainDrainGateWitnessSamples() {
        return witnessSamplesJoin(chainDrainGateFirstSamples);
    }

    /**
     * The #502 arm-A census reader ({@link #symbolDrainGateFacets}): ranked
     * {@code symbolDrainGate.<face>.<detail>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String symbolDrainGateBreakdown() {
        return rankedBreakdown(symbolDrainGateFacets);
    }

    /**
     * The #502 arm-A first-sample witness reader ({@link #symbolDrainGateFirstSamples}): the
     * per-bucket site samples.
     */
    public String symbolDrainGateWitnessSamples() {
        return witnessSamplesJoin(symbolDrainGateFirstSamples);
    }

    /**
     * The #502 arm-B census reader ({@link #synItemGateFacets}): ranked
     * {@code synItemGate.<context>.<detail>.<pos>=count} tokens.
     */
    public String synItemGateBreakdown() {
        return rankedBreakdown(synItemGateFacets);
    }

    /**
     * The #502 arm-B first-sample witness reader ({@link #synItemGateFirstSamples}): the
     * per-bucket site samples.
     */
    public String synItemGateWitnessSamples() {
        return witnessSamplesJoin(synItemGateFirstSamples);
    }

    /**
     * The #503 arm-A census reader ({@link #equalityGateFacets}): ranked
     * {@code equalityGate.<face>.<detail>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String equalityGateBreakdown() {
        return rankedBreakdown(equalityGateFacets);
    }

    /**
     * The #503 arm-A first-sample witness reader ({@link #equalityGateFirstSamples}): the
     * per-bucket site samples.
     */
    public String equalityGateWitnessSamples() {
        return witnessSamplesJoin(equalityGateFirstSamples);
    }

    /**
     * The #503 arm-B census reader ({@link #featureDrainGateFacets}): ranked
     * {@code featureDrainGate.<face>.<detail>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String featureDrainGateBreakdown() {
        return rankedBreakdown(featureDrainGateFacets);
    }

    /**
     * The #503 arm-B first-sample witness reader ({@link #featureDrainGateFirstSamples}): the
     * per-bucket site samples.
     */
    public String featureDrainGateWitnessSamples() {
        return witnessSamplesJoin(featureDrainGateFirstSamples);
    }

    /**
     * The #504 arm-A census reader ({@link #booleanOpGateFacets}): ranked
     * {@code booleanOpGate.<face>.<fam>.<side>.<callee>.<out>.<card>.<sib>.<pos>.<ctx>.<typing>=count}
     * tokens.
     */
    public String booleanOpGateBreakdown() {
        return rankedBreakdown(booleanOpGateFacets);
    }

    /**
     * The #504 arm-A first-sample witness reader ({@link #booleanOpGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String booleanOpGateWitnessSamples() {
        return witnessSamplesJoin(booleanOpGateFirstSamples);
    }

    /**
     * The #504 arm-C census reader ({@link #qualNameGateFacets}): ranked
     * {@code qualNameGate.<face>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String qualNameGateBreakdown() {
        return rankedBreakdown(qualNameGateFacets);
    }

    /**
     * The #504 arm-C first-sample witness reader ({@link #qualNameGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String qualNameGateWitnessSamples() {
        return witnessSamplesJoin(qualNameGateFirstSamples);
    }

    /**
     * The #505 arm-A census reader ({@link #enumSeatGateFacets}): ranked
     * {@code enumSeatGate.<face>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String enumSeatGateBreakdown() {
        return rankedBreakdown(enumSeatGateFacets);
    }

    /**
     * The #505 arm-A first-sample witness reader ({@link #enumSeatGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String enumSeatGateWitnessSamples() {
        return witnessSamplesJoin(enumSeatGateFirstSamples);
    }

    /**
     * The #505 arm-A SEAT census reader ({@link #enumSeatRootFacets}): ranked
     * {@code enumSeatRoot.<channel>.<typing>.<verdict>=count} tokens — the excluded
     * disguised-residue population the seat conversion would join to the denominator.
     */
    public String enumSeatRootBreakdown() {
        return rankedBreakdown(enumSeatRootFacets);
    }

    /**
     * The #505 arm-A seat-census first-sample witness reader
     * ({@link #enumSeatRootFirstSamples}): the {@link #onlyElemGateWitnessSamples} format.
     */
    public String enumSeatRootWitnessSamples() {
        return witnessSamplesJoin(enumSeatRootFirstSamples);
    }

    /**
     * The #505 arm-B census reader ({@link #argResidueGateFacets}): ranked
     * {@code argResidueGate.aliasArg.<tail>.<callee>.<param>.<body>.<pos>.<ctx>.<typing>=count}
     * tokens.
     */
    public String argResidueGateBreakdown() {
        return rankedBreakdown(argResidueGateFacets);
    }

    /**
     * The #505 arm-B first-sample witness reader ({@link #argResidueGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String argResidueGateWitnessSamples() {
        return witnessSamplesJoin(argResidueGateFirstSamples);
    }

    /**
     * The #505 arm-C census reader ({@link #dispatchGateFacets}): ranked
     * {@code dispatchGate.synAbsent.<variant>.<base>.<anno>.<pos>.<ctx>.<typing>=count}
     * tokens.
     */
    public String dispatchGateBreakdown() {
        return rankedBreakdown(dispatchGateFacets);
    }

    /**
     * The #505 arm-C first-sample witness reader ({@link #dispatchGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String dispatchGateWitnessSamples() {
        return witnessSamplesJoin(dispatchGateFirstSamples);
    }

    /**
     * The #506 wave-census reader ({@link #walkBindGateFacets}): ranked
     * {@code walkBindGate.<face>.<detail>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String walkBindGateBreakdown() {
        return rankedBreakdown(walkBindGateFacets);
    }

    /**
     * The #507 arm-A census reader ({@link #deepGateFacets}): ranked
     * {@code deepGate.<feat>.<recv>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String deepGateBreakdown() {
        return rankedBreakdown(deepGateFacets);
    }

    /**
     * The #507 arm-A first-sample witness reader ({@link #deepGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String deepGateWitnessSamples() {
        return witnessSamplesJoin(deepGateFirstSamples);
    }

    /**
     * The #507 arm-A root-seat census reader ({@link #deepVisitFacets}): ranked
     * {@code deepVisit.<feat>.<recv>.<parent>.<typing>=count} tokens.
     */
    public String deepVisitBreakdown() {
        return rankedBreakdown(deepVisitFacets);
    }

    /**
     * The #507 arm-A root-seat first-sample witness reader ({@link #deepVisitFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String deepVisitWitnessSamples() {
        return witnessSamplesJoin(deepVisitFirstSamples);
    }

    /**
     * The #507 arm-B census reader ({@link #noChanGateFacets}): ranked
     * {@code noChanGate.<head>.<leaf>.<scope>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String noChanGateBreakdown() {
        return rankedBreakdown(noChanGateFacets);
    }

    /**
     * The #507 arm-B first-sample witness reader ({@link #noChanGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String noChanGateWitnessSamples() {
        return witnessSamplesJoin(noChanGateFirstSamples);
    }

    /**
     * The #507 arm-B seat-census reader ({@link #l111ResidueFacets}): ranked
     * {@code l111Residue.<face>=count} tokens ({@code Σ ≡ the seat's delegated count} per cell).
     */
    public String l111ResidueBreakdown() {
        return rankedBreakdown(l111ResidueFacets);
    }

    /**
     * The #507 arm-B seat-census first-sample witness reader
     * ({@link #l111ResidueFirstSamples}): the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String l111ResidueWitnessSamples() {
        return witnessSamplesJoin(l111ResidueFirstSamples);
    }

    /**
     * The #508 arm-A census reader ({@link #metaHopGateFacets}): ranked
     * {@code metaHopGate.<face>.<detail>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String metaHopGateBreakdown() {
        return rankedBreakdown(metaHopGateFacets);
    }

    /**
     * The #508 arm-A first-sample witness reader ({@link #metaHopGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String metaHopGateWitnessSamples() {
        return witnessSamplesJoin(metaHopGateFirstSamples);
    }

    /**
     * The #508 arm-B census reader ({@link #metaSrcGateFacets}): ranked
     * {@code metaSrcGate.<face>.<detail>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String metaSrcGateBreakdown() {
        return rankedBreakdown(metaSrcGateFacets);
    }

    /**
     * The #508 arm-B first-sample witness reader ({@link #metaSrcGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String metaSrcGateWitnessSamples() {
        return witnessSamplesJoin(metaSrcGateFirstSamples);
    }

    /**
     * The #508 arm-C census reader ({@link #symNotGateFacets}): ranked
     * {@code symNotGate.<SymClass>.<seat>.<verdict>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String symNotGateBreakdown() {
        return rankedBreakdown(symNotGateFacets);
    }

    /**
     * The #508 arm-C first-sample witness reader ({@link #symNotGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String symNotGateWitnessSamples() {
        return witnessSamplesJoin(symNotGateFirstSamples);
    }

    /**
     * The #509 arm-A census reader ({@link #aliasIdGateFacets}): ranked
     * {@code aliasIdGate.<face>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String aliasIdGateBreakdown() {
        return rankedBreakdown(aliasIdGateFacets);
    }

    /**
     * The #509 arm-A first-sample witness reader ({@link #aliasIdGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String aliasIdGateWitnessSamples() {
        return witnessSamplesJoin(aliasIdGateFirstSamples);
    }

    /**
     * The #509 arm-B census reader ({@link #headUnGateFacets}): ranked
     * {@code headUnGate.<ladder>.<adaptV>.<ht>.<member>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String headUnGateBreakdown() {
        return rankedBreakdown(headUnGateFacets);
    }

    /**
     * The #509 arm-B first-sample witness reader ({@link #headUnGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String headUnGateWitnessSamples() {
        return witnessSamplesJoin(headUnGateFirstSamples);
    }

    /**
     * The #509 arm-B census reader ({@link #nsrGateFacets}): ranked
     * {@code nsrGate.<RecvClass>.<recvAdapt>.<ct>.<member>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String nsrGateBreakdown() {
        return rankedBreakdown(nsrGateFacets);
    }

    /**
     * The #509 arm-B first-sample witness reader ({@link #nsrGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String nsrGateWitnessSamples() {
        return witnessSamplesJoin(nsrGateFirstSamples);
    }

    /**
     * The #520 arm-A3 census reader ({@link #headOtherGateFacets}): ranked
     * {@code headOtherGate.<SymClass>.<member>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String headOtherGateBreakdown() {
        return rankedBreakdown(headOtherGateFacets);
    }

    /**
     * The #520 arm-A3 first-sample witness reader ({@link #headOtherGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String headOtherGateWitnessSamples() {
        return witnessSamplesJoin(headOtherGateFirstSamples);
    }

    /**
     * The #524 pre-arm census reader ({@link #itemAttrGateFacets}): ranked
     * {@code iaGate.<face>.n:<name>.<legacyLeg>.<member>.<adaptV>.<pos>.<ctx>.<typing>=count}
     * tokens.
     */
    public String itemAttrGateBreakdown() {
        return rankedBreakdown(itemAttrGateFacets);
    }

    /**
     * The #524 pre-arm census first-sample witness reader ({@link #itemAttrGateFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String itemAttrGateWitnessSamples() {
        return witnessSamplesJoin(itemAttrGateFirstSamples);
    }

    /**
     * The #526 pre-arm census reader ({@link #neGateFacets}): ranked
     * {@code neGate.<face>[.fails:<n>of<N>].<shape>.<adaptV>.<pos>.<path>.<ctx>.<typing>=count}
     * tokens.
     */
    public String neGateBreakdown() {
        return rankedBreakdown(neGateFacets);
    }

    /**
     * The #526 pre-arm census first-sample witness reader ({@link #neGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String neGateWitnessSamples() {
        return witnessSamplesJoin(neGateFirstSamples);
    }

    /**
     * The #527 pre-arm census reader ({@link #argGapGateFacets}): ranked
     * {@code argGapGate.<tail>.<callee>.<verdicts>.<scan>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String argGapGateBreakdown() {
        return rankedBreakdown(argGapGateFacets);
    }

    /**
     * The #527 pre-arm census first-sample witness reader ({@link #argGapGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String argGapGateWitnessSamples() {
        return witnessSamplesJoin(argGapGateFirstSamples);
    }

    /**
     * The #528 pre-arm census reader ({@link #opResGateFacets}): ranked
     * {@code opResGate.<face>.f:<fam>.<detail>.<scan>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String opResGateBreakdown() {
        return rankedBreakdown(opResGateFacets);
    }

    /**
     * The #528 pre-arm census first-sample witness reader ({@link #opResGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String opResGateWitnessSamples() {
        return witnessSamplesJoin(opResGateFirstSamples);
    }

    /**
     * The #529 channel-1 census reader ({@link #itemHeadGateFacets}): ranked
     * {@code itemHeadGate.<ladder>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String itemHeadGateBreakdown() {
        return rankedBreakdown(itemHeadGateFacets);
    }

    /**
     * The #529 channel-1 first-sample witness reader ({@link #itemHeadGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String itemHeadGateWitnessSamples() {
        return witnessSamplesJoin(itemHeadGateFirstSamples);
    }

    /**
     * The #529 channel-2 census reader ({@link #headAttrGateFacets}): ranked
     * {@code headAttrGate.<head>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String headAttrGateBreakdown() {
        return rankedBreakdown(headAttrGateFacets);
    }

    /**
     * The #529 channel-2 first-sample witness reader ({@link #headAttrGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String headAttrGateWitnessSamples() {
        return witnessSamplesJoin(headAttrGateFirstSamples);
    }

    /**
     * The #529 channel-3 census reader ({@link #eqImplGateFacets}): ranked
     * {@code eqImplGate.s:<L|R>.sib:<Class>.c:<card>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String eqImplGateBreakdown() {
        return rankedBreakdown(eqImplGateFacets);
    }

    /**
     * The #529 channel-3 first-sample witness reader ({@link #eqImplGateFirstSamples}): the
     * {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String eqImplGateWitnessSamples() {
        return witnessSamplesJoin(eqImplGateFirstSamples);
    }

    /**
     * The #529 channel-4 census reader ({@link #itemArgSrcGateFacets}): ranked
     * {@code itemArgSrcGate.i:<idx>.<src>.<pos>.<ctx>.<typing>=count} tokens.
     */
    public String itemArgSrcGateBreakdown() {
        return rankedBreakdown(itemArgSrcGateFacets);
    }

    /**
     * The #529 channel-4 first-sample witness reader ({@link #itemArgSrcGateFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String itemArgSrcGateWitnessSamples() {
        return witnessSamplesJoin(itemArgSrcGateFirstSamples);
    }

    /**
     * The #530 channel-1 census reader ({@link #pipeCondGateFacets}): ranked
     * {@code pipeCondGate.<read>.<pos>.<ctx>=count} tokens.
     */
    public String pipeCondGateBreakdown() {
        return rankedBreakdown(pipeCondGateFacets);
    }

    /**
     * The #530 channel-1 first-sample witness reader ({@link #pipeCondGateFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String pipeCondGateWitnessSamples() {
        return witnessSamplesJoin(pipeCondGateFirstSamples);
    }

    /**
     * The #530 channel-2 census reader ({@link #nsrCtGateFacets}): ranked
     * {@code nsrCtGate.<detail>.feat:<name>=count} tokens.
     */
    public String nsrCtGateBreakdown() {
        return rankedBreakdown(nsrCtGateFacets);
    }

    /**
     * The #530 channel-2 first-sample witness reader ({@link #nsrCtGateFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String nsrCtGateWitnessSamples() {
        return witnessSamplesJoin(nsrCtGateFirstSamples);
    }

    /**
     * The #530 channel-3 census reader ({@link #kvpGateFacets}): ranked
     * {@code kvpGate.<read>.<pos>.<ctx>=count} tokens.
     */
    public String kvpGateBreakdown() {
        return rankedBreakdown(kvpGateFacets);
    }

    /**
     * The #530 channel-3 first-sample witness reader ({@link #kvpGateFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String kvpGateWitnessSamples() {
        return witnessSamplesJoin(kvpGateFirstSamples);
    }

    /**
     * The #530 channel-4 census reader ({@link #metaArgGateFacets}): ranked
     * {@code metaArgGate.<detail>.<pos>.<ctx>=count} tokens.
     */
    public String metaArgGateBreakdown() {
        return rankedBreakdown(metaArgGateFacets);
    }

    /**
     * The #530 channel-4 first-sample witness reader ({@link #metaArgGateFirstSamples}):
     * the {@code <bucket> e.g. <site>} pairs, pipe-joined.
     */
    public String metaArgGateWitnessSamples() {
        return witnessSamplesJoin(metaArgGateFirstSamples);
    }

    /**
     * The #506 wave-census first-sample witness reader ({@link #walkBindGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String walkBindGateWitnessSamples() {
        return witnessSamplesJoin(walkBindGateFirstSamples);
    }

    /**
     * The #500 arm-A first-sample witness reader ({@link #metaAccessGateFirstSamples}): the
     * {@link #onlyElemGateWitnessSamples} format.
     */
    public String metaAccessGateWitnessSamples() {
        return witnessSamplesJoin(metaAccessGateFirstSamples);
    }

    /** The #500 arm-B thenVisit census reader ({@link #thenVisitFacets}): ranked tokens. */
    public String thenVisitBreakdown() {
        return rankedBreakdown(thenVisitFacets);
    }

    /** The #500 arm-B thenVisit first-sample witness reader ({@link #thenVisitFirstSamples}). */
    public String thenVisitWitnessSamples() {
        return witnessSamplesJoin(thenVisitFirstSamples);
    }

    /** The #500 arm-B conversionVisit census reader ({@link #conversionVisitFacets}): ranked tokens. */
    public String conversionVisitBreakdown() {
        return rankedBreakdown(conversionVisitFacets);
    }

    /** The #500 arm-B conversionVisit first-sample witness reader ({@link #conversionVisitFirstSamples}). */
    public String conversionVisitWitnessSamples() {
        return witnessSamplesJoin(conversionVisitFirstSamples);
    }

    /** The #500 arm-B listLitVisit census reader ({@link #listLitVisitFacets}): ranked tokens. */
    public String listLitVisitBreakdown() {
        return rankedBreakdown(listLitVisitFacets);
    }

    /** The #500 arm-B listLitVisit first-sample witness reader ({@link #listLitVisitFirstSamples}). */
    public String listLitVisitWitnessSamples() {
        return witnessSamplesJoin(listLitVisitFirstSamples);
    }

    /** The #500 arm-B onlyExistsVisit census reader ({@link #onlyExistsVisitFacets}): ranked tokens. */
    public String onlyExistsVisitBreakdown() {
        return rankedBreakdown(onlyExistsVisitFacets);
    }

    /** The #500 arm-B onlyExistsVisit first-sample witness reader ({@link #onlyExistsVisitFirstSamples}). */
    public String onlyExistsVisitWitnessSamples() {
        return witnessSamplesJoin(onlyExistsVisitFirstSamples);
    }

    /**
     * The shared {@code bucket e.g. sample} join for the #498 witness readers (the
     * {@link #implicitAttrWitnessSamples} display convention, single-sourced for the new
     * channels); {@code none} when empty — the probe-off state.
     */
    private static String witnessSamplesJoin(Map<String, String> samples) {
        if (samples.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : samples.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(" e.g. ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Total of {@link #untargetedVisitsByFamily} — the untargeted half of the not-IR-driven population
     * (the counted half being {@link #irDeclinedCount()}). Printed on the D11 census line next to its
     * breakdown so the two halves read together.
     */
    public int untargetedVisitCount() {
        int total = 0;
        for (int v : untargetedVisitsByFamily.values()) {
            total += v;
        }
        return total;
    }

    /**
     * The #476 blocker probe's claim scalar: adapterGap-declined claims probed. 0 whenever the probe
     * is off ({@code -Drosetta.generator.ir.blockerProbe} unset — the default; the standing receipts'
     * state), so the D11 reader blocks print the probe lines only on an explicit probe run.
     */
    public int blockerProbedClaimCount() {
        return blockerProbedClaimCount;
    }

    /** The #476 probe's honest-residue meter: root re-adapts that contradicted their decline. Expected 0 always. */
    public int blockerProbeAnomalyCount() {
        return blockerProbeAnomalyCount;
    }

    /**
     * The #476 probe's participation ranking: {@code family=claims} tokens ranked count-descending
     * ({@code none} when the probe is off / no claim probed) — probed claims each family participates
     * in blocking. A claim with blockers in several families counts under EACH (the values can sum
     * past {@link #blockerProbedClaimCount}); read the SOLE ranking for the per-teach unlock.
     */
    public String blockerClaimsBreakdown() {
        return rankedBreakdown(blockerClaimsByFamily);
    }

    /**
     * The #476 probe's UNLOCK ranking: {@code family=claims} tokens ranked count-descending
     * ({@code none} when empty) — probed claims whose minimal blockers are all ONE family, i.e. the
     * claims that family's adapter teach fully unblocks on its own (the sole-blocker mining law; an
     * upper bound vs today's adapter — a landed teach can surface a successor blocker, so re-read
     * after each teach).
     */
    public String soleBlockerClaimsBreakdown() {
        return rankedBreakdown(soleBlockerClaimsByFamily);
    }

    /**
     * The #477 per-ARM decode's participation ranking: {@code Family:reason=claims} tokens ranked
     * count-descending ({@code none} when the probe is off / no claim probed) — probed claims each
     * arm-gate pair participates in blocking. The same claim unit as
     * {@link #blockerClaimsBreakdown()} one key deeper; read the sole-reason ranking for the
     * per-gate unlock.
     */
    public String blockerReasonBreakdown() {
        return rankedBreakdown(blockerClaimsByReason);
    }

    /**
     * The #477 per-ARM decode's UNLOCK ranking: {@code Family:reason=claims} tokens ranked
     * count-descending ({@code none} when empty) — probed claims whose minimal blockers are all
     * ONE arm-gate pair, i.e. the claims a single gate refinement fully unblocks on its own (the
     * sole-blocker mining law at the arm-gate unit; the same upper-bound-per-reading caveat as the
     * family ranking — re-read after each teach).
     */
    public String soleReasonClaimsBreakdown() {
        return rankedBreakdown(soleReasonClaimsByReason);
    }

    /**
     * The #477 per-ARM decode's honest residue: {@code family=claims} tokens ({@code none} when
     * empty) — sole-FAMILY claims blocked by MORE THAN ONE gate of that family's arm, which no
     * single gate refinement unblocks. Closes the conservation identity per family:
     * {@code soleBlocker == Σ soleReason + soleFamilyMultiReason} (checkable receipt-side against
     * {@link #soleBlockerClaimsBreakdown()} + {@link #soleReasonClaimsBreakdown()}).
     */
    public String soleFamilyMultiReasonBreakdown() {
        return rankedBreakdown(soleFamilyMultiReasonClaims);
    }

    /**
     * The #477 mirror-fidelity meter: the {@code unattributed} pair participations — one count
     * per probed claim per DISTINCT unattributed pair (a claim with unattributed blockers in two
     * families counts twice, the participation map's own unit) — a minimal blocker whose arm
     * mirror named no failing gate (an adapter-arm edit whose {@code reasonFor*} twin was missed,
     * or a — contract-impossible — re-adapt contradiction). Expected 0 always; derived from the
     * participation map, no separate state.
     */
    public int blockerReasonUnattributedCount() {
        int total = 0;
        for (Map.Entry<String, Integer> e : blockerClaimsByReason.entrySet()) {
            if (e.getKey().endsWith(":" + ExpressionToIRAdapter.UNATTRIBUTED)) {
                total += e.getValue();
            }
        }
        return total;
    }

    /**
     * #478: ranked nav-gate shape buckets ({@code channel:shape:verdict=count}, blocker-node
     * occurrences); see {@link #navGateShapeBuckets}.
     */
    public String navGateShapeBreakdown() {
        return rankedBreakdown(navGateShapeBuckets);
    }

    /**
     * #478: ranked nav-gate position facet ({@code channel:position=count}); see
     * {@link #navGatePositionBuckets}.
     */
    public String navGatePositionBreakdown() {
        return rankedBreakdown(navGatePositionBuckets);
    }

    /**
     * #478: the first sampled corpus site per nav-gate shape bucket ({@code bucket e.g. site},
     * bucket-key order, {@code " | "}-joined); {@code none} when the probe saw no nav-gate
     * blockers. See {@link #navGateFirstSamples}.
     */
    public String navGateWitnessSamples() {
        if (navGateFirstSamples.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : navGateFirstSamples.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(" e.g. ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Ranked {@code bucket=count} view of the #479 implicit-root shape witness (blocker-node
     * occurrences; {@code none} when the probe is off / no cluster blocker probed); see
     * {@link #implicitRootShapeBuckets}.
     */
    public String implicitRootShapeBreakdown() {
        return rankedBreakdown(implicitRootShapeBuckets);
    }

    /**
     * One pinned corpus site per #479 implicit-root bucket — the qualitative face of the cluster
     * blockers. See {@link #implicitRootFirstSamples}.
     */
    public String implicitRootWitnessSamples() {
        if (implicitRootFirstSamples.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : implicitRootFirstSamples.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(" e.g. ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Ranked {@code bucket=count} view of the #480 implicit-root SOURCE decode (the
     * {@code unprovableSource} sub-shapes + the still-declining equivalents' base facets;
     * {@code none} when the probe is off / nothing decoded); see
     * {@link #implicitRootSourceBuckets}.
     */
    public String implicitRootSourceBreakdown() {
        return rankedBreakdown(implicitRootSourceBuckets);
    }

    /**
     * One pinned corpus site per #480 source-decode bucket — the widening map's qualitative face.
     * See {@link #implicitRootSourceFirstSamples}.
     */
    public String implicitRootSourceWitnessSamples() {
        if (implicitRootSourceFirstSamples.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : implicitRootSourceFirstSamples.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(" e.g. ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Ranked {@code bucket=count} view of the #480 arm-admission restatement
     * ({@code bareAttrArm:}/{@code chainArm:} claims vs first-failing-gate declines; post-teach
     * {@code claims} reads ZERO — the conservation signature; {@code none} when the probe is off);
     * see {@link #implicitRootArmBuckets}.
     */
    public String implicitRootArmBreakdown() {
        return rankedBreakdown(implicitRootArmBuckets);
    }

    /**
     * Ranked {@code bucket=count} view of the #481 elided-PIPE decode (the planned widening's
     * admission restated per {@code elidedImplicit} source; {@code thenArg.provable.typeOk} = the
     * claimable slice, post-teach ZERO; {@code none} when the probe is off / nothing decoded);
     * see {@link #elidedPipeBuckets}.
     */
    public String implicitRootPipeBreakdown() {
        return rankedBreakdown(elidedPipeBuckets);
    }

    /**
     * One pinned corpus site per #481 elided-pipe bucket — the widening's qualitative face. See
     * {@link #elidedPipeFirstSamples}.
     */
    public String implicitRootPipeWitnessSamples() {
        if (elidedPipeFirstSamples.isEmpty()) {
            return "none";
        }
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> e : elidedPipeFirstSamples.entrySet()) {
            if (sb.length() > 0) {
                sb.append(" | ");
            }
            sb.append(e.getKey()).append(" e.g. ").append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Test seam for the #476 blocker probe: {@link #BLOCKER_PROBE} is a class-load static the unit
     * locks cannot set per-test, so the locks flip the instance mirror directly. Package-private on
     * purpose — production code enables the probe only via the property.
     */
    void setBlockerProbeForTest(boolean enabled) {
        this.blockerProbeEnabled = enabled;
    }

    /**
     * Shared formatter for the #475 census breakdowns: {@code key=count} tokens ranked count-DESCENDING,
     * ties broken by key ascending (deterministic receipts — the D11 census lines are diffed run-to-run),
     * {@code none} for an empty map (a zero-activity cell prints a stable literal rather than an empty
     * tail the log diff could mistake for truncation).
     */
    private static String rankedBreakdown(Map<String, Integer> counts) {
        if (counts.isEmpty()) {
            return "none";
        }
        List<Map.Entry<String, Integer>> entries = new ArrayList<>(counts.entrySet());
        entries.sort((a, b) -> {
            int byCount = Integer.compare(b.getValue(), a.getValue());
            return byCount != 0 ? byCount : a.getKey().compareTo(b.getKey());
        });
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, Integer> e : entries) {
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        return sb.toString();
    }

    /**
     * Number of witness/output collision FQN-inline renders produced inside IR-driven emissions — the
     * decisive measure of whether the IR's own {@code collisionFqn} path (L-029) is exercised by the corpus.
     */
    public int collisionRenderCount() {
        return collisionRenderCount;
    }
}
