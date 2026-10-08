package com.regnosys.rosetta.generator.java.function;

import com.regnosys.rosetta.generator.java.SilentDegradation;
import com.regnosys.rosetta.ast.RExpression;
import com.regnosys.rosetta.ast.RNode;
import com.regnosys.rosetta.ast.enums.ArithOp;
import com.regnosys.rosetta.ast.enums.ConversionKind;
import com.regnosys.rosetta.ast.enums.ListOp;
import com.regnosys.rosetta.ast.enums.SwitchGuardKind;
import com.regnosys.rosetta.ast.enums.SwitchGuardLiteralKind;
import com.regnosys.rosetta.ast.expressions.binary.*;
import com.regnosys.rosetta.ast.expressions.constructors.RConditionalExpr;
import com.regnosys.rosetta.ast.expressions.constructors.RConstructorExpr;
import com.regnosys.rosetta.ast.expressions.literals.*;
import com.regnosys.rosetta.ast.expressions.references.RDeepFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.REmptyLiteral;
import com.regnosys.rosetta.ast.expressions.references.REnumValueRef;
import com.regnosys.rosetta.ast.expressions.references.RFeatureCall;
import com.regnosys.rosetta.ast.expressions.references.RImplicitVariable;
import com.regnosys.rosetta.ast.expressions.references.RSymbolReference;
import com.regnosys.rosetta.ast.expressions.supporting.RInlineFunction;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCase;
import com.regnosys.rosetta.ast.expressions.supporting.RSwitchCaseGuard;
import com.regnosys.rosetta.ast.expressions.unary.*;
import com.regnosys.rosetta.ast.functions.RFunction;
import com.regnosys.rosetta.ast.functions.RShortcut;
import com.regnosys.rosetta.ast.supporting.RAttribute;
import com.regnosys.rosetta.ast.types.RDataType;
import com.regnosys.rosetta.generator.java.GeneratorModel;
import com.regnosys.rosetta.generator.java.expression.handlers.ReferenceHandler;
import com.regnosys.rosetta.generator.java.object.MetaFieldGenerator;
import com.regnosys.rosetta.generator.java.template.ImportCollisionResolver;
import com.regnosys.rosetta.generator.java.types.JavaTypeTranslator;
import com.regnosys.rosetta.generator.java.types.JavaTypeUtil;
import com.regnosys.rosetta.generator.java.types.RJavaWithMetaValue;
import com.regnosys.rosetta.types.*;
import com.rosetta.util.types.JavaClass;
import com.rosetta.util.types.JavaType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Analyses all aliases ({@link RShortcut}) declared inside a function and
 * produces an {@link FunctionTemplateModel.AliasModel} for each one.
 *
 * <p>For each alias the helper determines:
 * <ol>
 *   <li><strong>usesOutput</strong> — whether the alias expression tree contains
 *       any {@link RSymbolReference} whose {@link RSymbolReference#name()} matches
 *       the function's output attribute name.</li>
 *   <li><strong>returnType</strong> — derived from the alias expression's own type
 *       by walking the expression AST and resolving types through
 *       {@link GeneratorModel} and {@link JavaTypeTranslator}:
 *       <ul>
 *         <li>When usesOutput, the return type is the output type's builder form.</li>
 *         <li>Otherwise, the return type is:
 *             {@code MapperS<? extends T>} or {@code MapperC<? extends T>} for
 *             model types, {@code MapperS<T>} or {@code MapperC<T>} for primitives
 *             and enums.</li>
 *       </ul></li>
 *   <li><strong>params</strong> — the parameter list of the generated helper method.</li>
 * </ol>
 */
public class FunctionAliasHelper {

    private final GeneratorModel generatorModel;
    private final JavaTypeTranslator typeTranslator;
    private final JavaTypeUtil typeUtil;

    /**
     * Full constructor with type infrastructure for proper alias type resolution.
     */
    public FunctionAliasHelper(GeneratorModel generatorModel,
                               JavaTypeTranslator typeTranslator,
                               JavaTypeUtil typeUtil) {
        this.generatorModel = generatorModel;
        this.typeTranslator = typeTranslator;
        this.typeUtil = typeUtil;
    }

    /**
     * Legacy zero-arg constructor for tests that don't need type resolution.
     * Falls back to output-based type inference when type infrastructure is unavailable.
     */
    public FunctionAliasHelper() {
        this.generatorModel = null;
        this.typeTranslator = null;
        this.typeUtil = null;
    }

    /**
     * v3.2 seat 2: the #252 element-preserving then-body ops the alias SIGNATURE walk types
     * by the receiver — FIRST, LAST, ONLY_ELEMENT, FLATTEN, DISTINCT and, since seat 2
     * (witnessed by the chaos {@code C20Spread} rows), REVERSE.
     */
    static final Set<ListOp> SIGNATURE_ELEMENT_PRESERVING_OPS = Collections.unmodifiableSet(
            EnumSet.of(ListOp.FIRST, ListOp.LAST, ListOp.ONLY_ELEMENT, ListOp.FLATTEN,
                    ListOp.DISTINCT, ListOp.REVERSE));

    /**
     * The two MIRRORS' op set — the RType twin ({@code inferRTypeFromExpr}) and the render-side
     * hoist gate ({@code FunctionExpressionRenderer.isBareItemCardinalityThenBody}): the pre-seat
     * five. REVERSE is deliberately NOT mirrored (lanes O and P stayed green with it, withdrawn
     * under the #614 law at the witness sweep; re-measured with both seat suites at the final head by the
     * P-OP probe); the relation SIGNATURE = MIRROR + {REVERSE} is pinned by
     * {@code FunctionAliasHelperTest}.
     */
    static final Set<ListOp> MIRROR_ELEMENT_PRESERVING_OPS = Collections.unmodifiableSet(
            EnumSet.of(ListOp.FIRST, ListOp.LAST, ListOp.ONLY_ELEMENT, ListOp.FLATTEN,
                    ListOp.DISTINCT));

    /**
     * The function currently being analyzed. Set at the start of {@link #analyze}
     * and cleared at the end. Used by deep inference methods for parameter/output
     * lookup. NOT thread-safe — this class must be called sequentially per function.
     */
    private RFunction currentFunc;

    /**
     * facet aliasThenSigRenderTruth (PR #351, i2): the walk's ITEM binding while typing a
     * then-BODY — the receiver's ELEMENT (single form), so implicit-item references inside
     * the body (an {@code item -> value} nav, an arith over item navs, a conditional's
     * arms) type against the piped element exactly as the render binds them (the
     * bindThenArg render-truth). Push/pop around the body walk (the {@code currentFunc}
     * field pattern — this helper is single-threaded per generation); {@code null} =
     * unbound (every pre-facet walk shape unchanged: implicit references previously
     * returned null and fell to the fn-output fallback).
     */
    private ExpressionTypeInfo thenItemBinding;
    /**
     * v3.2 seat 2, PR #623 (round-1 cq review, SF-5): true iff {@link #thenItemBinding} was bound by
     * a THEN arm (the pipe), false when the EXTRACT arm bound it (the element); read by
     * {@link #thenPipedWholeOrNull}, which names the PIPE and must not answer inside an extract lambda.
     */
    private boolean thenItemBindingFromThen;

    /**
     * Per-alias signature-witness refs that are already Java classes rather than
     * {@link RType}s — today only the concrete {@link RJavaWithMetaValue} meta
     * wrapper (facet alias_method_signature_typing): {@code FieldWithMetaX}/
     * {@code ReferenceWithMetaX} is generator-built, so it has no {@code RType}
     * to drain into the {@code collectedRefs} channel that
     * {@code FunctionGenerator.unionExpressionRefs} translates. Reset per
     * shortcut in {@link #doAnalyze}; drained into the structural
     * {@code AliasModel.refs}, which {@code compileAliases} unions with the
     * rendered body's refs (idempotent where the body already registered the
     * wrapper). Same sequential-use contract as {@link #currentFunc}.
     *
     * <p>Like {@code inferredRefs}, this collects from EVERY
     * {@code resolveAttributeTypeInfo} hit during the alias's walk — including
     * sub-walks whose type result is later declined — so it can over-register
     * an unused (harmless, dedup'd) import; precise need-only collection would
     * require threading consumption context through the walk for no byte-level
     * gain (the byte-oracle is exact with this design).
     */
    private Set<JavaClass<?>> signatureJavaRefs = new HashSet<>();

    /**
     * Analyse all aliases in {@code func} and return a list of
     * {@link FunctionTemplateModel.AliasModel} in declaration order.
     */
    public List<FunctionTemplateModel.AliasModel> analyze(RFunction func) {
        this.currentFunc = func;
        try {
            return doAnalyze(func);
        } finally {
            this.currentFunc = null; // clear to prevent stale references
        }
    }

    private List<FunctionTemplateModel.AliasModel> doAnalyze(RFunction func) {
        List<FunctionTemplateModel.AliasModel> result = new ArrayList<>();

        Optional<RAttribute> outputOpt = func.output();
        String outputName = outputOpt.map(RAttribute::name).orElse(null);

        // facet importCollisionFqn return-type (PR #247): the signature simple-name -> canonical(s)
        // map (output + inputs) — built ONCE per function — for gating the return-type sentinel to
        // the actual-collision case below, so a non-colliding model-type alias return keeps the
        // import-collision fast-path (Copilot R1). super/alias-vs-alias collisions are deliberately
        // NOT checked here (an unchecked collision only leaves an exotic carrier waivered as before
        // — its bare same-simple-name reference is a duplicate import = a compile error = already
        // waivered — never a green regression).
        Map<String, Set<String>> sigCanonicalsBySimple = buildSignatureCanonicals(func, outputOpt);

        for (RShortcut shortcut : func.shortcuts()) {
            RExpression expr = shortcut.expression();

            // facet aliasOutputBuilderNav (PR #381, W): the OUTPUT-ROOTED
            // disguised-chain alias joins the usesOutput class — the resolved-symbol
            // walk cannot see it (the parser cannot root a nav at the function
            // OUTPUT, so `product -> contractualProduct -> …` parses as a
            // fully-unresolved REnumValueRef; the #346 disguised-nav class), and the
            // dormancy is corpus-proven: the resolved walk fires on ZERO aliases
            // across all five cells (the 381-W tracer census).
            boolean outputBuilderNav = outputName != null
                    && isOutputBuilderNavAlias(expr, outputName);
            boolean usesOutput = outputName != null
                    && expr != null
                    && (usesOutput(expr, outputName) || outputBuilderNav);

            // PR-A §9.1 A3-D2-02 — single expression walk per alias.
            // Walks once when type infrastructure is available, collecting
            // BOTH the ExpressionTypeInfo (consumed by computeReturnType in
            // the !usesOutput branch) AND the refs (drained into
            // AliasModel.inferredRefs for FunctionGenerator.compileAliases
            // L486). Prior to Copilot round 4 on 39fdb5a this was two
            // separate walks (computeReturnType + aliasRefs collection)
            // that could drift under behaviour changes to inferExpressionType.
            Set<RType> aliasRefs = new HashSet<>();
            signatureJavaRefs = new HashSet<>();
            ExpressionTypeInfo typeInfo = null;
            if (expr != null && generatorModel != null
                    && typeTranslator != null && typeUtil != null) {
                typeInfo = inferExpressionType(expr, new HashSet<>(), aliasRefs);
            }

            // facet importCollisionFqn (PR #245): the signature return-type ELEMENT
            // canonical, for the FunctionGenerator import-collision SEED. Only the
            // non-usesOutput Mapper-element model type (buildMapperReturnType's
            // `MapperS/C<? extends T>`) — a usesOutput builder return type's element IS
            // the function output, already seeded; primitives/unresolved carry none.
            // ALWAYS populated (the seed needs it so a BODY witness/ctor can lose the
            // first-claim to a winning alias return type, the #245 direction), independent
            // of whether the return-type STRING below sentinel-izes.
            String returnTypeElementFqn = (!usesOutput && typeInfo != null && typeInfo.isRosettaModelType)
                    ? typeInfo.javaItemCanonical
                    : null;

            // facet importCollisionFqn return-type (PR #247): emit the return-type sentinel
            // (FQN the loser) ONLY when the return element actually COLLIDES with a signature
            // type — a same-simple-name output/input of a DIFFERENT canonical, seeded first.
            // Off-collision the return type renders bare directly (no sentinel), so a
            // non-colliding model-type alias keeps the import-collision fast-path. (The #245
            // WINNER direction — return wins, a body witness loses — needs NO return-type
            // sentinel: the return renders bare and the body-witness sentinel + the always-on
            // returnTypeElementFqn seed handle the loser.)
            boolean returnElementCollides = returnTypeElementFqn != null
                    && signatureSimpleNameCollides(
                            sigCanonicalsBySimple, typeInfo.javaTypeName, returnTypeElementFqn);

            // facet aliasSeamSignature (PR #612): the seam string and the typed facts it is rendered from
            // come from the SAME branch (one SeamRender per producer), so a consumer reading the facts and
            // the template reading the string can never disagree (LAW 69).
            SeamRender seamRender = outputBuilderNav && typeInfo != null
                    ? buildOutputNavBuilderReturnType(typeInfo)
                    : computeReturnType(usesOutput, outputOpt, typeInfo, returnElementCollides);
            String returnType = seamRender.text();

            List<String> params = buildParams(usesOutput, outputOpt, func.inputs());

            result.add(new FunctionTemplateModel.AliasModel(
                    shortcut.name(),
                    returnType,
                    params,
                    "",
                    usesOutput,
                    // refs — the walk's signature-witness Java classes (meta
                    // wrappers); FunctionGenerator.compileAliases unions the
                    // rendered body's refs in on top
                    Set.copyOf(signatureJavaRefs),
                    aliasRefs)  // inferredRefs — populated by the single walk above
                    .withReturnTypeElementFqn(returnTypeElementFqn)
                    .withSeam(seamRender.seam()));
        }

        return result;
    }

    /**
     * facet meta_coercion_numbering (arm A1): the alias's typed Mapper result —
     * {@code MapperS/MapperC<ConcreteMetaWrapper>} — when the alias expression
     * walks to a META-annotated attribute; {@code null} for every other alias
     * (non-meta, {@code usesOutput} builder-form, unresolvable, or missing type
     * infrastructure).
     *
     * <p>Runs the SAME single walk ({@link #inferExpressionType}) that produced
     * the alias method's declared signature at {@link #analyze} — wrapper kind
     * from the same {@code isMulti}, item witness from the same meta arm — so
     * the receiver type a caller stamps on an alias-call rendering cannot
     * disagree with the emitted {@code protected abstract MapperS<? extends
     * ReferenceWithMetaX> aliasName(...)} declaration. Consumed by
     * {@code NavigationHandler} to type an alias-call navigation receiver,
     * activating the dormant {@code ExpressionCompiler.coerceNavigationReceiver}
     * meta-unwrap (the gate itself is byte-faithful for both guard kinds; this
     * method only supplies the type it declines without). The {@code usesOutput}
     * barrier mirrors {@link #computeReturnType}: such aliases return
     * {@code List}/builder forms, never Mappers.
     *
     * <p>Walk results beyond the meta arm carry no {@code javaItemType}, so a
     * non-meta alias returns {@code null} here and the caller's gate stays
     * dormant — byte-flat for every alias the facet does not claim.
     */
    public JavaType inferShortcutMapperJavaType(RShortcut shortcut, RFunction enclosingFunc) {
        if (shortcut == null || enclosingFunc == null || shortcut.expression() == null
                || generatorModel == null || typeTranslator == null || typeUtil == null) {
            return null;
        }
        String outputName = enclosingFunc.output().map(RAttribute::name).orElse(null);
        if (outputName != null && usesOutput(shortcut.expression(), outputName)) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            ExpressionTypeInfo info =
                    inferExpressionType(shortcut.expression(), new HashSet<>(), new HashSet<>());
            if (info == null) {
                return null;
            }
            // facet arithOperandWrapperCoerce (PR #334): walk results beyond the meta
            // arm carry the item type only as the signature NAME string; recover the
            // unambiguous java.lang/java.math basics from it so a basic-typed alias
            // (the fn-call-body `diff` = `DateDifference(...)` → `MapperS<Integer>`)
            // types its call sites from the SAME walk that rendered that signature.
            // Every pre-existing consumer acts only on RJavaWithMetaValue items
            // (coerceNavigationReceiver / tryMetaDerefArg meta gates), so the widened
            // non-null return is byte-neutral for them by their own gates.
            JavaType item = info.javaItemType;
            if (item == null) {
                item = basicJavaTypeForName(info.javaTypeName);
            }
            if (item == null) {
                return null;
            }
            // Concrete wrapper per branch — mirrors NavigationHandler.metaNavResultType's
            // construction (a MAPPER_C/MAPPER_S ternary would capture a wildcard
            // declaration and defeat wrap's inference).
            return info.isMulti
                    ? typeUtil.wrap(typeUtil.MAPPER_C, item)
                    : typeUtil.wrap(typeUtil.MAPPER_S, item);
        } finally {
            this.currentFunc = null;
        }
    }

    /**
     * facet caseSwitchAliasSubjectStmtArms (PR #370): the alias's bare MODEL item Java
     * class — the walk's {@code isRosettaModelType} leaf resolved through
     * {@link #resolveModelTypeByName} (canonical-disambiguated, the #349 law) — for the
     * choice-type switch SUBJECT declaration ({@code final Product switchArgument0 =
     * fpmlProduct(fpmlTrade).get();}, golden MapTransferStateList). Runs the SAME single
     * walk ({@link #inferExpressionType}) that produced the alias method's declared
     * signature ({@code MapperS<? extends Product>}), so the hoisted subject type and the
     * signature element cannot disagree (the #178 same-walk law). Returns {@code null}
     * for usesOutput/meta/basic/unresolvable aliases — the meta/basic arms stay on
     * {@link #inferShortcutMapperJavaType}, whose deliberate model-type null (the #352
     * boundary) its navigation-seat consumers rely on; this separate entry point serves
     * exactly the switch-subject seat that needs the bare model item.
     */
    public JavaClass<?> inferShortcutModelItemJavaClass(RShortcut shortcut, RFunction enclosingFunc) {
        if (shortcut == null || enclosingFunc == null || shortcut.expression() == null
                || generatorModel == null || typeTranslator == null || typeUtil == null) {
            return null;
        }
        String outputName = enclosingFunc.output().map(RAttribute::name).orElse(null);
        if (outputName != null && usesOutput(shortcut.expression(), outputName)) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            ExpressionTypeInfo info =
                    inferExpressionType(shortcut.expression(), new HashSet<>(), new HashSet<>());
            // javaItemType non-null = the meta-wrapper arm — not a bare model item.
            if (info == null || !info.isRosettaModelType || info.javaItemType != null) {
                return null;
            }
            RDataType dt = resolveModelTypeByName(info.javaTypeName, info.javaItemCanonical);
            if (dt == null) {
                return null;
            }
            return typeTranslator.toJavaReferenceType(new RDataTypeRef(dt));
        } finally {
            this.currentFunc = null;
        }
    }

    /**
     * facet switchBaseItemCase (PR #353): the JOINED case-guard {@link RType} of a
     * CHOICE/TYPE-keyed switch whose EVERY non-default case body is the bare implicit
     * ITEM and whose default (if any) is empty — or {@code null} for any other shape
     * (function-call cases, literal guards, unresolvable names, disagreeing guards).
     * The guard resolves exactly like {@code ChoiceSwitchSupport.resolveCaseType}
     * (the linker's {@code resolvedGuard} first — PR #460 — then the simple-name
     * fallback against the loaded workspace for guards the linker left unresolved).
     */
    /**
     * facet aliasSwitchExtendsJoin (PR #396): the public accessor — the receiver-type
     * walk ({@code NavigationHandler.resolveReceiverDataType0}'s alias-body recursion)
     * reads the SAME item-case join the signature and the RETURN ladder read, so the
     * nav witness/cardinality/lambda-var cascade and the declared
     * {@code MapperS<? extends T>} element cannot disagree (the #178/#353 lockstep).
     */
    public RType switchItemCaseJoinedGuardTypeOrNull(RSwitchExpr sw) {
        return switchItemCaseJoinedGuardType(sw);
    }

    private RType switchItemCaseJoinedGuardType(RSwitchExpr sw) {
        if (generatorModel == null) {
            return null;
        }
        RType joined = null;
        String joinedName = null;
        boolean anyCase = false;
        for (RSwitchCase sc : sw.cases()) {
            if (sc.isDefault()) {
                RExpression defExpr = sc.expression();
                if (defExpr != null && !(defExpr instanceof REmptyLiteral)
                        && !(defExpr instanceof RListLiteral dl && dl.elements().isEmpty())) {
                    return null;
                }
                continue;
            }
            if (!(sc.expression() instanceof RImplicitVariable)) {
                return null;
            }
            RSwitchCaseGuard guard = sc.guard().orElse(null);
            if (guard == null || guard.kind() != SwitchGuardKind.NAME) {
                return null;
            }
            String qn = guard.qualifiedName().orElse(null);
            if (qn == null) {
                return null;
            }
            String simple = qn.substring(qn.lastIndexOf('.') + 1);
            // facet caseGuardResolvedBinding (PR #460): the linker's resolvedGuard first
            // (the authoritative import/alias-aware binding), then the #396
            // namespace-consistency heuristic, then the raw first-wins lookup.
            RType t = com.regnosys.rosetta.generator.java.expression.handlers
                    .ChoiceSwitchSupport.resolvedGuardRType(guard);
            if (t == null) {
                // facet guardNamespaceAwareResolve (PR #396, Copilot R1): the LUB join
                // raises the stakes on guard resolution — prefer a namespace-consistent
                // candidate (the guard's qualifier segments, then the switch's own model
                // namespace) over the raw first-wins lookup; a unique candidate resolves
                // identically (the carriers are corpus-unique by simple name).
                t = com.regnosys.rosetta.generator.java.expression.handlers
                        .ChoiceSwitchSupport.resolveGuardRTypeNamespaceAware(
                                qn, sc, generatorModel);
            }
            if (t == null) {
                t = generatorModel.resolveTypeByName(simple).orElse(null);
            }
            if (t == null || t.name() == null) {
                return null;
            }
            if (joined == null) {
                joined = t;
                joinedName = t.name();
            } else if (!t.name().equals(joinedName)) {
                // facet aliasSwitchExtendsJoin (PR #396): DISAGREEING guards join at
                // the nearest common EXTENDS supertype — upstream's case-result join
                // over the narrowed items (golden cdm6 ExtractNotionalAdjustmentByLeg
                // fpmlProduct: EquitySwapTransactionSupplement + ReturnSwap →
                // MapperS<? extends ReturnSwapBase>). No common ancestor keeps the
                // null decline (since v3.2 seat 12 the R3 refusal at the alias SIGNATURE for a
                // builtin-output function and the raw-name fallback for a model type; today's
                // bytes before it).
                RType lub = nearestCommonExtendsSupertype(joined, t);
                if (lub == null) {
                    return null;
                }
                joined = lub;
                joinedName = lub.name();
            }
            anyCase = true;
        }
        return anyCase ? joined : null;
    }

    /**
     * facet aliasSwitchExtendsJoin (PR #396): the nearest common EXTENDS supertype of
     * two resolved data types — IDENTITY-compared over the {@code superType()} chains
     * (the same walk {@code collectAttributesWithSupertypes} bounds), or {@code null}
     * when either side is not a data type or no common ancestor exists.
     * (facet fnNotionalTogetherRestructure, PR #398: opened {@code public} for the
     * ControlFlowHandler deep-then ladder's elseless ctor-arm LUB join — the
     * Measure + NonNegativeQuantitySchedule → MeasureBase read; the handlers live in
     * a sibling package, so package-private does not reach.)
     */
    public static RType nearestCommonExtendsSupertype(RType a, RType b) {
        com.regnosys.rosetta.ast.types.RDataType da =
                a instanceof RDataTypeRef ar ? ar.astNode() : null;
        com.regnosys.rosetta.ast.types.RDataType db =
                b instanceof RDataTypeRef br ? br.astNode() : null;
        if (da == null || db == null) {
            return null;
        }
        java.util.List<com.regnosys.rosetta.ast.types.RDataType> aChain = new ArrayList<>();
        for (com.regnosys.rosetta.ast.types.RDataType t = da;
                t != null && aChain.size() < 64; t = t.superType().orElse(null)) {
            aChain.add(t);
        }
        int guard = 0;
        for (com.regnosys.rosetta.ast.types.RDataType t = db;
                t != null && guard++ < 64; t = t.superType().orElse(null)) {
            for (com.regnosys.rosetta.ast.types.RDataType cand : aChain) {
                if (cand == t) {
                    return new RDataTypeRef(t);
                }
            }
        }
        return null;
    }

    /**
     * facet fnNotionalTogetherRestructure (PR #398): TRUE iff two RTypes name the SAME
     * data type -- {@code equals()} first, then the {@code RDataTypeRef} AST-node identity
     * rescue (the workspace mints fresh refs per query, so an instance-based equals must
     * not read same-type arms as a disagreeing join -- the false-positive wildcard trap).
     *
     * <p>Moved here from {@code ControlFlowHandler} at v3.1 flip seat 33, law D.12, with
     * the fold it serves; the predicate keeps its one call site - the FOLD it serves gained a
     * second consumer (the D.12 wildcard decl).
     */
    private static boolean sameLadderDataType(RType a, RType b) {
        if (a.equals(b)) {
            return true;
        }
        return a instanceof RDataTypeRef ar && b instanceof RDataTypeRef br
                && ar.astNode() == br.astNode();
    }

    /**
     * The per-arm RTYPE fold of a conditional LADDER's arms, folded pairwise at the nearest
     * common EXTENDS supertype: a ctor arm contributes its constructed type (the workspace
     * snapshot), a nav/chain arm its recovered element. Returns the join ONLY when the walk
     * resolved EVERY arm AND at least one pair genuinely DISAGREED -- i.e. exactly the
     * {@code walkOk && folded != null && anyDisagree} verdict its first consumer has always
     * taken -- and {@code null} for an all-agreeing ladder, an unresolvable arm, a
     * non-data-type pair, or an empty list.
     *
     * <p>EXTRACTED at v3.1 flip seat 33 (law D.12) from
     * {@code ControlFlowHandler.hoistAsDeepThenMapperLocalOrNull}, which keeps its own
     * COMPILE-driven arm-node collection ({@code armNodes} + {@code innerElseNodes}) and now
     * calls this for the fold alone. The second consumer is
     * {@code FunctionExpressionRenderer.appendIteHoistChainCore}'s DECL seat, reached through
     * {@link #ladderArmsProperSupertypeJoinOrNull} -- the two seats must not disagree about
     * when a ladder's arms join at a proper supertype (LAW 69, the #178 same-walk law).
     * A pure extraction: the returned verdict is byte-for-byte the one the original inline
     * fold produced for the same node list.
     */
    public static RType properSupertypeJoinOrNull(List<RExpression> armNodes,
            GeneratorModel generatorModel,
            com.regnosys.rosetta.generator.java.expression.ExpressionCompiler compiler) {
        return foldLadderArms(armNodes, generatorModel, compiler).properSupertypeJoinOrNull();
    }

    /**
     * The fold's FULL verdict -- the three facts {@code ControlFlowHandler}'s inline fold
     * used to leave in scope ({@code walkOk}, {@code anyDisagree}, {@code folded}), because
     * its seat-31 {@code aliasSigWildcardDecl} arm reads {@code !anyDisagree && walkOk}
     * ("every arm resolved AND all agree") AFTER the join verdict. Extracting only the join
     * would have collapsed "walk failed", "all agree" and "disagree without a join" into one
     * {@code null} and moved that arm -- the first GREEN run of law D.12 caught the join-only
     * draft as a compile error at that read. {@link #properSupertypeJoinOrNull()} is the join
     * under exactly the original verdict.
     */
    public record LadderArmsFold(boolean walkOk, boolean anyDisagree, RType folded) {
        /** The join iff the walk resolved every arm AND at least one pair disagreed. */
        public RType properSupertypeJoinOrNull() {
            return walkOk && folded != null && anyDisagree ? folded : null;
        }
    }

    /**
     * The walk behind {@link #properSupertypeJoinOrNull(List, GeneratorModel,
     * com.regnosys.rosetta.generator.java.expression.ExpressionCompiler)}, verdict and all:
     * {@code walkOk} is false for an empty list, an unresolvable arm, or a disagreeing pair
     * with no EXTENDS supertype -- exactly where the original inline fold set it false and
     * broke -- and {@code anyDisagree} is whatever the walk had established when it stopped,
     * as before.
     */
    public static LadderArmsFold foldLadderArms(List<RExpression> armNodes,
            GeneratorModel generatorModel,
            com.regnosys.rosetta.generator.java.expression.ExpressionCompiler compiler) {
        if (armNodes == null || armNodes.isEmpty() || generatorModel == null) {
            return new LadderArmsFold(false, false, null);
        }
        RType folded = null;
        boolean anyDisagree = false;
        for (RExpression armNode : armNodes) {
            RMetaAnnotatedType nodeInferred = generatorModel.workspace().getInferredType(armNode);
            RType armT = nodeInferred == null || nodeInferred.isMissing() ? null
                    : nodeInferred.type();
            if (armT == null) {
                armT = com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler
                        .recoverThenArgItemRType(armNode, compiler);
            }
            if (armT == null) {
                return new LadderArmsFold(false, anyDisagree, folded);
            }
            if (folded == null) {
                folded = armT;
            } else if (!sameLadderDataType(folded, armT)) {
                RType lub = nearestCommonExtendsSupertype(folded, armT);
                if (lub == null) {
                    return new LadderArmsFold(false, anyDisagree, folded);
                }
                folded = lub;
                anyDisagree = true;
            }
        }
        return new LadderArmsFold(true, anyDisagree, folded);
    }

    /**
     * facet iteDeclWildcardFromDisagreeingArms (v3.1 flip seat 33, law D.12): the AST-only
     * entry point to {@link #properSupertypeJoinOrNull} for a seat that has no compiled arm
     * list -- {@code FunctionExpressionRenderer.appendIteHoistChainCore}'s ite-hoist DECL,
     * which runs BEFORE the arms are compiled.
     *
     * <p>The walk MIRRORS the spine of {@code ControlFlowHandler}'s ladder walk: the
     * {@code thenBranch} of every rung, then the REAL terminal else. An ABSENT terminal, an
     * {@link REmptyLiteral} and an {@link RListLiteral} contribute NOTHING -- the first two
     * because {@code ControlFlowHandler}'s own walk breaks before adding them (an elseless
     * ladder's synthetic empty-list else denotes "no value", and folding its {@code Void}
     * would abort every ladder), the third because that is the EXCLUSION the [P33-F6B] probe
     * measured: golden StrikePrice's terminal prints {@code elseKind=RListLiteral} on all 40
     * firing rows, and this predicate is shipped exactly as it was measured. That makes this
     * walk STRICTLY NARROWER than {@code ControlFlowHandler}'s, which folds a NON-empty list
     * literal terminal; the difference is disclosed rather than silently widened, because a
     * non-empty list-literal terminal could only ADD un-measured firing rows.
     *
     * <p>Bounded at 64 rungs (the {@link #nearestCommonExtendsSupertype} chain bound): the
     * else-chain of a finite AST terminates on its own, so the bound can only be reached by a
     * malformed tree.
     */
    public static RType ladderArmsProperSupertypeJoinOrNull(RConditionalExpr cond,
            GeneratorModel generatorModel,
            com.regnosys.rosetta.generator.java.expression.ExpressionCompiler compiler) {
        if (cond == null || generatorModel == null) {
            return null;
        }
        List<RExpression> armNodes = new ArrayList<>();
        RConditionalExpr cur = cond;
        int hops = 0;
        while (cur != null && hops++ < 64) {
            armNodes.add(cur.thenBranch());
            RExpression els = cur.elseBranch().orElse(null);
            if (els == null) {
                break;
            }
            if (els instanceof RConditionalExpr nested) {
                cur = nested;
                continue;
            }
            if (!(els instanceof REmptyLiteral) && !(els instanceof RListLiteral)) {
                armNodes.add(els);
            }
            break;
        }
        return properSupertypeJoinOrNull(armNodes, generatorModel, compiler);
    }

    /**
     * facet aliasSwitchValueLadder (PR #365, F-A): the JOINED case-RESULT type of a
     * CHOICE/TYPE-keyed switch over a BARE symbol whose EVERY non-default case result
     * is a VALUE-class shape — a resolved enum value ref ({@code FinancialUnitEnum ->
     * Share}) or a boolean literal ({@code True}) — and whose default (if any) is
     * empty or the same value class. Guards must be NAME guards that resolve against
     * the loaded workspace (the {@code ChoiceSwitchSupport.resolveCaseType} law) so
     * the signature fires exactly when the RETURN-ladder renderer can render the
     * {@code instanceof} arms — the two reads stay in lockstep (the #353 agreement
     * law) — or, since v3.2 seat 2, STRING-literal guards over int-literal results
     * (an elided subject, every non-default guard a STRING literal, an int-literal or
     * absent default — the deep-then ladder's one witnessed class, {@code CollectionHandler
     * .isDeepThenLiteralSwitchConsumer}; every other shape declines here exactly as the
     * ladder declines it). Function-call case results keep the {@code null} fall-through:
     * the green #226 carriers' signatures come from the callee output / the legacy
     * fallback (bytes untouched), exactly as the item-case join above documents. Returns
     * {@code null} for any other shape.
     */
    private ExpressionTypeInfo switchValueCaseJoin(RSwitchExpr sw, Set<RNode> visited,
            Set<RType> collectedRefs) {
        if (generatorModel == null) {
            return null;
        }
        // v3.2 seat 2 (Law 1): STRING-literal-guarded cases (`"a" then 1, default 0`) admit INT-
        // literal results — the case-result join is the switch's type (Integer), exactly as the
        // enum/boolean value class below; golden C5Forms `MapperS<Integer> graded`. That is the ONE
        // witnessed class: number/string results and non-string literal guards stayed green when
        // narrowed away (the witness sweep's probes), so they are banked under the #614 law, not
        // admitted. A NAME-guarded switch keeps its pre-seat admission set unchanged (its render
        // ladders are the #365 family's; widening them is not this seat's).
        boolean literalGuarded = sw.cases().stream()
                .anyMatch(c -> c.guard().map(g -> g.kind() == SwitchGuardKind.LITERAL
                        && g.literalKind().orElse(null) == SwitchGuardLiteralKind.STRING).orElse(false));
        // PR #623 (round-3 cq review, SF-1; round-4 cq SF-1 / spec MF-3): under STRING-literal guards
        // the admission is the deep-then ladder's EXACTLY - each term of isDeepThenLiteralSwitchConsumer
        // is read here too: the subject ELIDED (a bare or typed subject under string guards is a class
        // no renderer carries: the RETURN ladder wants NAME guards, the deep-then ladder the piped
        // item); and, in the loop below, EVERY non-default guard a STRING literal carrying its value
        // (a NAME guard mixed in would move the signature while BOTH ladders decline the body), an
        // EMPTY default declined and every case result an int literal - never a superset of it, for
        // every case the loop reaches: the #396 itemJoin early return above the loop is unchanged by
        // this seat and, for a parser-built tree, cannot fire under literalGuarded (it requires a NAME
        // guard on every non-default case; only a hand-built DEFAULT case carrying a STRING guard
        // could reach it - round-5 cq NIT-4). The subject term sits ABOVE the subject block (round-4
        // cq NIT-2): a non-elided subject under string guards declines before the block's
        // inferExpressionType walk. That walk's RESULT was discarded either way; its one surviving
        // side effect - a signatureJavaRefs over-registration from a declined sub-walk, the contract
        // that field's javadoc states - is dropped with it (round-5 cq SF-1), measured byte-neutral
        // at 3b1706224 on all 26 cells and both routes (the s2e chain == the s2d chain on every digest).
        // v3.2 seat 7 (facet literalSwitchLambdaBlock): a LITERAL-guarded switch whose subject is the extract lambda's
        // ITEM (`[raw] extract (item switch "r" then "Red", …)` — the chaos C18ToKind `switched` alias) is the lambda
        // seat's class: the subject is the #361 extract binding (typable and single, or the walk declines), exactly as the
        // elided then-piped subject below — the block renderer (CollectionHandler.compileLiteralSwitchBlockLambda) renders
        // it and the signature must agree (the #353 lockstep law). Pre-seat the walk declined here and the signature fell to
        // the output-type fallback's raw rune name (`MapperC<? extends string>` — non-compiling). The result class under a
        // lambda-item subject admits STRING literals beside the int literals (both witnessed by the ControlLiteral / Chaos
        // goldens); the deep-then subject keeps the seat-2 int-only class.
        boolean lambdaItemSubject = thenItemBinding != null && !thenItemBindingFromThen
                && sw.argument() instanceof RImplicitVariable;
        if (literalGuarded && sw.argument() != null && !lambdaItemSubject) {
            return null;
        }
        // The subject must be a BARE symbol (an input param/local): the ladder renders
        // `if (<subject> == null)` and `(<Case>) <subject>` casts on the raw name.
        // facet aliasTypeSwitchFnArm (PR #382, E-iii): OR a NON-bare subject this walk
        // itself types to a single model element — the renderer then hoists
        // `final <T> switchArgument = <arg>.get();` (the #370 assignment-seat law at
        // the alias RETURN seat) and reads the SAME walk for the decl type
        // (switchSubjectTypeInfoOrNull — the #178/#353 lockstep law). Golden cdm6
        // MapExecutionDetails: subject `fpmlTrade -> product` → `final Product
        // switchArgument = MapperS.of(fpmlTrade).<Product>map("getProduct", …).get();`.
        // v3.2 seat 2 (Law 1, the chaos C5Forms `graded` rows): a `… then switch …` body ELIDES
        // its subject (argument() is null — the piped item, HandlerHelper.orSyntheticImplicit at
        // the render seat). The subject is the then-binding the #351 arm pushed: typable and
        // single, or the walk declines exactly as for an untypable bare subject.
        if (sw.argument() == null) {
            if (!isTypableName(thenItemBinding) || thenItemBinding.isMulti) {
                return null;
            }
        } else if (lambdaItemSubject) {
            // v3.2 seat 7: the #361 extract binding carries the RECEIVER's cardinality; the lambda ITEM it binds is
            // SINGLE by construction (the `mapItem` lambda receives one element), so only typability is required - the
            // NAME-guarded chaos `pulled` alias over `eths Either (0..*)` and the literal-guarded `switched` over
            // `[raw]` both read their subject here (fix1: the pre-seat `inferExpressionType(item)` walk returned the
            // binding's MULTI and declined, and the signature fell to the output-type fallback).
            if (!isTypableName(thenItemBinding)) {
                return null;
            }
        } else if (!(sw.argument() instanceof RSymbolReference sref) || !sref.args().isEmpty()) {
            ExpressionTypeInfo subjInfo =
                    inferExpressionType(sw.argument(), new HashSet<>(), new HashSet<>());
            if (!isTypableName(subjInfo) || subjInfo.isMulti) {
                return null;
            }
        }
        // facet aliasSwitchExtendsJoin (PR #396): a BARE-ITEM case-result switch joins
        // at the case GUARD types (the narrowed item IS the guard type — the #353
        // item-case law at the VALUE-join seat), disagreeing guards LUB-ed at the
        // nearest common EXTENDS supertype through the SAME
        // switchItemCaseJoinedGuardType walk the signature reads (the #178/#353
        // lockstep law) — so the RETURN-ladder renderer now fires for the item-case
        // class too (golden cdm6 ExtractNotionalAdjustmentByLeg fpmlProduct:
        // `return MapperS.of(equitySwapTransactionSupplement);` arms under the
        // `MapperS<? extends ReturnSwapBase>` signature).
        RType itemJoin = switchItemCaseJoinedGuardType(sw);
        if (itemJoin != null && typeTranslator != null
                && typeTranslator.toJavaReferenceType(itemJoin) instanceof JavaClass<?> itemJc
                && !"Object".equals(itemJc.getSimpleName())) {
            collectedRefs.add(itemJoin);
            return new ExpressionTypeInfo(itemJc.getSimpleName(), false, true, null,
                    itemJc.getCanonicalName().withDots());
        }
        ExpressionTypeInfo join = null;
        boolean anyCase = false;
        for (RSwitchCase sc : sw.cases()) {
            RExpression ce = sc.expression();
            if (sc.isDefault()) {
                if (ce == null || ce instanceof REmptyLiteral
                        || (ce instanceof RListLiteral dl && dl.elements().isEmpty())) {
                    if (literalGuarded) {
                        // PR #623 (round-3 cq review, SF-1): the ladder wants an int-literal default
                        return null;
                    }
                    continue; // empty default contributes no type (the ofNull terminal)
                }
                // PR #623 (round-2 cq review, MF-1): under STRING-literal guards the default's RESULT
                // must be an int literal too - the ONE witnessed class, the same admission the
                // deep-then predicate makes (isDeepThenLiteralSwitchConsumer).
                // v3.2 seat 7 round 1 (LAW 69): the lambda-item kinds through the ONE declaration the literal-guard
                // block consults (ChoiceSwitchSupport.isLiteralGuardedLambdaArmLiteral - string and int; a number /
                // boolean literal declines at both halves, unwitnessed - the code-quality review's SF-6).
                if (literalGuarded && !(ce instanceof RIntLiteral
                        || (lambdaItemSubject
                                && com.regnosys.rosetta.generator.java.expression.handlers.ChoiceSwitchSupport
                                        .isLiteralGuardedLambdaArmLiteral(ce)))) {
                    return null;
                }
            } else {
                RSwitchCaseGuard guard = sc.guard().orElse(null);
                if (guard == null) {
                    return null;
                }
                // PR #623 (round-4 cq review, SF-1 / spec MF-3): under STRING guards EVERY non-default
                // guard is one - isDeepThenLiteralSwitchConsumer requires it of every case - so a switch
                // MIXING a STRING-literal guard with a NAME guard (`switch "a" then 1, F4Item then 2,
                // default 0`) declines here exactly as both ladders decline its body (the NAME branch
                // below carries no result-class term and would have joined Integer). Pre-seat the
                // mixed switch declined at the NAME-only gate, so nothing this seat admits is lost.
                if (literalGuarded && guard.kind() != SwitchGuardKind.LITERAL) {
                    return null;
                }
                if (guard.kind() == SwitchGuardKind.LITERAL) {
                    // v3.2 seat 2 (Law 1): a literal guard names no type — nothing to resolve.
                    // STRING literals only (PR #623, round-1 cq review, MF-1): the one witnessed
                    // class, in lockstep with literalGuarded above and the deep-then predicate - an
                    // INT / BOOLEAN / NUMBER guard over a value-class result would move the SIGNATURE
                    // while the RETURN-ladder renderer (ChoiceSwitchSupport.isChoiceTypeSwitch, NAME
                    // guards only) still declines the body: the #353 agreement law, kept.
                    // PR #623 (round-2 cq review, MF-1): the RESULT class too - a STRING-guarded
                    // case whose result is an enum ref / boolean / ctor / conditional / fn-call would
                    // pass valueShaped below and move the signature while BOTH renderers decline the
                    // body (the RETURN ladder wants NAME guards, the deep-then ladder int literals).
                    // The value's PRESENCE too (PR #623, round-4 cq review, SF-1): the predicate and
                    // HandlerHelper.literalGuardMapper read the same four conjuncts - kind, value
                    // present, STRING kind, int result (a parsed STRING guard always carries its value;
                    // the term is for a hand-built node, RSwitchCaseGuard's own documented path).
                    if (guard.literalKind().orElse(null) != SwitchGuardLiteralKind.STRING
                            || guard.literalValue().isEmpty()
                            || !(ce instanceof RIntLiteral
                                    || (lambdaItemSubject
                                            && com.regnosys.rosetta.generator.java.expression.handlers.ChoiceSwitchSupport
                                                    .isLiteralGuardedLambdaArmLiteral(ce)))) {
                        return null; // the lambda-item kinds: the ONE declaration (round 1, SF-6), as at the default above
                    }
                } else if (guard.kind() != SwitchGuardKind.NAME) {
                    return null;
                } else {
                    String qn = guard.qualifiedName().orElse(null);
                    if (qn == null) {
                        return null;
                    }
                    String simple = qn.substring(qn.lastIndexOf('.') + 1);
                    // facet caseGuardResolvedBinding (PR #460): a linker-resolved guard
                    // satisfies the existence gate directly; unresolved guards keep the
                    // pre-#460 simple-name existence check.
                    if (com.regnosys.rosetta.generator.java.expression.handlers
                                    .ChoiceSwitchSupport.resolvedGuardRType(guard) == null
                            && generatorModel.resolveTypeByName(simple).isEmpty()) {
                        return null;
                    }
                }
            }
            // facet aliasTypeSwitchFnArm (PR #382, E-iii): a FUNCTION-CALL case result
            // joins by the callee's declared output through the SAME inferExpressionType
            // walk that renders fn-call alias signatures (META-aware — MapCurrency's
            // `string [metadata scheme]` output joins FieldWithMetaString; golden cdm6
            // MapExecutionDetails `MapperS<? extends FieldWithMetaString>
            // swapStreamNotionalCurrency`). The pre-#382 all-fn decline kept the #226
            // green carriers' signatures on the callee-output/legacy fallback — those
            // route through the EXTRACT-seat block lambda, never this alias-body seat.
            // facet aliasTypeSwitchCtorCondArm (PR #388): a CTOR case result joins by
            // the CONSTRUCTED type (the #358 law via inferConstructorType — golden cdm6
            // MapAssetToObservableWithLocation: Observable ctor arms + an Observable
            // ctor default → MapperS<? extends Observable>); a CONDITIONAL case result
            // joins by its ARM join (the inferExpressionType RConditionalExpr arm —
            // golden cdm6 GetUnitTypeForUnderlyingAsset: the elseless `if instrumentId
            // exists then FinancialUnitEnum -> IndexUnit` arm + the enum default →
            // MapperS<FinancialUnitEnum>). Both render-side arms land in the SAME PR
            // (the #353 signature/renderer lockstep law).
            boolean valueShaped =
                    (ce instanceof REnumValueRef evr && evr.enumeration().isPresent())
                            || ce instanceof RBooleanLiteral
                            || (ce instanceof RSymbolReference fnArm
                                    && fnArm.symbol().orElse(null) instanceof RFunction)
                            || ce instanceof RConstructorExpr
                            || ce instanceof RConditionalExpr
                            // v3.2 seat 2 (Law 1): int-literal results under STRING-literal guards
                            || (literalGuarded && ce instanceof RIntLiteral)
                            // v3.2 seat 7: literal results (a string / int / number literal - the arms and the
                            // defaults the block seats admit) under a lambda-item subject, NAME- or literal-guarded
                            || (lambdaItemSubject
                                    && (ce instanceof RStringLiteral || ce instanceof RIntLiteral
                                            || ce instanceof RNumberLiteral));
            ExpressionTypeInfo t;
            if (valueShaped) {
                t = inferExpressionType(ce, visited, collectedRefs);
            } else {
                // facet aliasSwitchNavLadder (PR #369, cluster A): a case result that is
                // a DISGUISED 2-name nav over the NARROWED case type
                // (`floatingRateModel -> spreadSchedule` inside `case
                // FloatingRateCalculation` — the #368 caseNarrowedDisguisedNav class at
                // the ALIAS-body RETURN seat) types as the leaf attribute resolved
                // through the case guard's type, carrying the leaf's CARDINALITY
                // (golden `MapperS<? extends Schedule>` Multipler / `MapperC<? extends
                // SpreadSchedule>` Spread). Any other non-value shape keeps the decline.
                t = sc.isDefault() ? null : caseNarrowedNavTypeOrNull(sc, ce, collectedRefs, lambdaItemSubject);
            }
            if (!isTypableName(t)) {
                return null;
            }
            join = join == null ? t : joinArmTypes(join, t, collectedRefs);
            if (join == null) {
                return null;
            }
            if (!sc.isDefault()) {
                anyCase = true;
            }
        }
        return anyCase ? join : null;
    }

    /**
     * facet aliasSwitchNavLadder (PR #369, cluster A): type a switch CASE result that
     * is a disguised {@code head -> feature} nav whose head is an attribute of the
     * case guard's NARROWED type. Single-cardinality heads only (the carrier
     * population); {@code null} declines. The SAME resolution feeds the signature
     * join and (via the renderer's bound-subject compile) the rendered chain — the
     * #178 same-walk law.
     *
     * <p>facet aliasSwitchBareCaseNav (seat 23, law A1): …and its BARE 1-name sibling — a
     * case result that is a bare {@code RSymbolReference} (no args; the linker binds it to the
     * narrowed type's OWN attribute — cdm 6.21.0 flattened {@code floatingRateModel ->
     * floatingRateMultiplierSchedule} onto {@code FloatingRateCalculation} itself, so the same
     * source navigation stopped being the 2-name chain above) types as that attribute resolved
     * BY NAME on the case type through the SAME guard resolution, carrying the leaf's OWN
     * cardinality (golden {@code MapperS<? extends Schedule>} Multipler / {@code MapperC<?
     * extends SpreadSchedule>} Spread). IDENTITY-guarded against the linker's binding when one
     * is present (the #590 law — this corpus declares a carrier name twice with different
     * cardinality): a binding that is NOT the case type's own attribute (an input, a shadowing
     * declaration) declines rather than guesses. A function reference never enters (it is
     * value-shaped, the #365/#382 arm). The class has 21 members corpus-wide — 2 functions × 7
     * cells (cdm 6.21.0 / 6.22.0 / 6.23.0 + drr 7.0–7.3): the Multipler's one case and the
     * Spread's TWO ({@code FloatingRateCalculation} and {@code InflationRateCalculation}, both
     * the bare {@code spreadSchedule}). The seat-23 runtime probe at the join seat recorded 28
     * LINES over 14 of them (the Multipler case and the Spread's FloatingRateCalculation case,
     * two lines each — lines, not cases), every one identity=true; the Spread's
     * InflationRateCalculation case did not surface at the instrumented join line and is
     * covered instead by the whole-file lock (the A1 suite's c2/c3) and control0's two-arm pin
     * on the Spread ladder. The healed files are byte-identical to golden.
     */
    private ExpressionTypeInfo caseNarrowedNavTypeOrNull(RSwitchCase sc, RExpression ce,
            Set<RType> collectedRefs, boolean lambdaItemSubject) {
        RSymbolReference bare = ce instanceof RSymbolReference sr && sr.args().isEmpty()
                && sr.symbol().map(s -> s instanceof RAttribute).orElse(true) ? sr : null;
        REnumValueRef nav = ce instanceof REnumValueRef evr && evr.enumeration().isEmpty()
                && evr.enumName() != null && evr.valueName() != null ? evr : null;
        // v3.2 seat 7 (F11, facet switchLambdaArmAdmit): under a LAMBDA-ITEM subject, an item-rooted feature-call chain
        // (`item -> av`, `item -> a -> b`) or a to-string over one - the chaos C18ToKind `pulled` arms - types through the
        // case guard's NARROWED type hop by hop, the SAME resolution the block seats' bound compile renders off the case
        // local (`optA.<String>map("getAv", ...)`), so the signature (`MapperC<String>`) and the block agree (the #353
        // lockstep law). Gated to the lambda-item subject: the alias-body RETURN ladder does not render this class.
        boolean itemToString = lambdaItemSubject
                && ce instanceof com.regnosys.rosetta.ast.expressions.unary.RToStringExpr;
        RExpression itemNavExpr = itemToString
                ? ((com.regnosys.rosetta.ast.expressions.unary.RToStringExpr) ce).argument() : ce;
        List<String> itemHops = new ArrayList<>();
        if (lambdaItemSubject) {
            RExpression root = itemNavExpr;
            int depth = 0;
            // round 1 (the code-quality review's NIT-3): the SAME bound as the admission half's walk
            // (CollectionHandler.admissibleBlockSwitchNav) - HandlerHelper.PARENT_WALK_LIMIT, not a second literal
            while (root instanceof RFeatureCall fc
                    && depth++ < com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.PARENT_WALK_LIMIT) {
                itemHops.add(0, fc.featureName());
                root = fc.receiver();
            }
            if (!(root instanceof RImplicitVariable)) {
                itemHops.clear();
            }
        }
        if (bare == null && nav == null && itemHops.isEmpty()) {
            return null;
        }
        RSwitchCaseGuard guard = sc.guard().orElse(null);
        String qn = guard == null ? null : guard.qualifiedName().orElse(null);
        if (qn == null) {
            return null;
        }
        // facet caseGuardResolvedBinding (PR #460): resolvedGuard first, then the
        // pre-#460 simple-name fallback.
        RType caseType = com.regnosys.rosetta.generator.java.expression.handlers
                .ChoiceSwitchSupport.resolvedGuardRType(guard);
        if (caseType == null) {
            String simple = qn.substring(qn.lastIndexOf('.') + 1);
            caseType = generatorModel.resolveTypeByName(simple).orElse(null);
        }
        if (caseType == null) {
            return null;
        }
        if (!itemHops.isEmpty()) {
            // v3.2 seat 7: the item-rooted chain walked from the case type; a MULTI hop makes the value multi.
            RType cur = caseType;
            RAttribute leaf = null;
            boolean multi = false;
            for (int i = 0; i < itemHops.size(); i++) {
                if (cur == null) {
                    return null;
                }
                Optional<RAttribute> hopAttr = findAttribute(cur, itemHops.get(i));
                if (hopAttr.isEmpty()) {
                    return null;
                }
                leaf = hopAttr.get();
                multi |= generatorModel.isMulti(leaf);
                cur = i + 1 < itemHops.size() ? resolveAttrRType(leaf) : cur;
            }
            if (itemToString) {
                return new ExpressionTypeInfo("String", multi, false);
            }
            ExpressionTypeInfo leafInfo = resolveAttributeTypeInfo(leaf, itemNavExpr, collectedRefs);
            if (leafInfo == null || !multi || leafInfo.isMulti) {
                return leafInfo;
            }
            return new ExpressionTypeInfo(leafInfo.javaTypeName, true, leafInfo.isRosettaModelType,
                    leafInfo.javaItemType, leafInfo.javaItemCanonical);
        }
        if (bare != null) {
            // facet aliasSwitchBareCaseNav (seat 23, law A1): the bare 1-name case — the case
            // type's own attribute, identity-guarded against the linker's binding.
            Optional<RAttribute> own = findAttribute(caseType, bare.name());
            if (own.isEmpty()) {
                return null;
            }
            if (bare.symbol().isPresent() && bare.symbol().get() != own.get()) {
                return null;
            }
            return resolveAttributeTypeInfo(own.get(), bare, collectedRefs);
        }
        Optional<RAttribute> head = findAttribute(caseType, nav.enumName());
        if (head.isEmpty() || generatorModel.isMulti(head.get())) {
            return null;
        }
        RType headType = resolveAttrRType(head.get());
        if (headType == null) {
            return null;
        }
        Optional<RAttribute> leaf = findAttribute(headType, nav.valueName());
        if (leaf.isEmpty()) {
            return null;
        }
        return resolveAttributeTypeInfo(leaf.get(), nav, collectedRefs);
    }

    /**
     * facet aliasSwitchValueLadder (PR #365, F-A): the renderer-side consult —
     * the joined case-result SIMPLE name for a value-class choice-type switch alias
     * body, or {@code null} when the join declines (the alias body then falls to the residual
     * seat's refusal — {@code TYPE_SWITCH_TERNARY_STUB} per resolvable case since v3.1 C0, R1 and R3 (the
     * signature's, for a builtin output) since v3.2 seat 12; the plain {@code return <ternary>;} before them).
     * Same walk as the signature's
     * {@link #switchValueCaseJoin} arm, so the {@code MapperS.<T>ofNull()} arms the
     * ladder renders can never disagree with the emitted
     * {@code protected abstract MapperS<T>} declaration (the #178 same-walk law).
     */
    public String switchValueCaseJoinedSimpleNameOrNull(RShortcut shortcut, RFunction enclosingFunc) {
        ExpressionTypeInfo info = switchValueCaseJoinInfoOrNull(shortcut, enclosingFunc);
        return info == null ? null : info.javaTypeName;
    }

    /**
     * facet aliasSwitchNavLadder (PR #369, cluster A): the FULL case-result join —
     * element simple name AND cardinality — for the renderer's {@code MapperS}/
     * {@code MapperC} terminal selection (golden {@code MapperC.<SpreadSchedule>
     * ofNull()} for the multi-leaf Spread join). Same walk as the signature's
     * {@link #switchValueCaseJoin} arm (the #178 same-walk law).
     */
    public ExpressionTypeInfo switchValueCaseJoinInfoOrNull(RShortcut shortcut,
            RFunction enclosingFunc) {
        if (shortcut == null || shortcut.expression() == null
                || !(shortcut.expression() instanceof RSwitchExpr sw)) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            return switchValueCaseJoin(sw, new HashSet<>(), new HashSet<>());
        } finally {
            this.currentFunc = null;
        }
    }

    /**
     * facet aliasTypeSwitchFnArm (PR #382, E-iii): the switch SUBJECT's type through
     * the SAME {@code inferExpressionType} walk {@link #switchValueCaseJoin}'s
     * non-bare-subject gate consulted — the renderer's hoisted
     * {@code final <T> switchArgument} declaration reads THIS (the #178/#353
     * lockstep law), so the admit and the decl type cannot disagree. {@code null}
     * when the subject does not type (the renderer then declines the ladder).
     */
    public ExpressionTypeInfo switchSubjectTypeInfoOrNull(RSwitchExpr sw,
            RFunction enclosingFunc) {
        if (sw == null || sw.argument() == null) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            ExpressionTypeInfo info =
                    inferExpressionType(sw.argument(), new HashSet<>(), new HashSet<>());
            return isTypableName(info) && !info.isMulti ? info : null;
        } finally {
            this.currentFunc = null;
        }
    }

    /**
     * v3.2 seat 5 (PR #626, F3): the Java simple name of an int literal - the translator's digit law
     * ({@code JavaTypeTranslator.caseNumberType}) over the parser's inferred {@code RNumberType} (its digits are
     * the literal's digit count, {@code ExpressionTypeComputer.computeIntLiteral}), boxed: {@code Integer} /
     * {@code Long} / {@code BigInteger}. {@code Integer} (the pre-seat constant) when the workspace or the
     * translator is absent or answers anything else.
     *
     * <p>WITNESSES (round 1, the spec review's SF-2): the {@code BigInteger} rung is witnessed by the oracle group
     * {@code conv-bigint-statement}'s ViaAlias ({@code MapperS<BigInteger>}, the seat suite's d4 and lane D8); the
     * {@code Integer} rung is the pre-seat constant every vendored cell's alias locks carry. The {@code Long} rung
     * (10-18 digits) is corpus- and oracle-UNWITNESSED - no vendored cell, no chaos file and no oracle group declares
     * a 10-18-digit literal alias - and is BANKED: the digit law is the vendored translator's own, but the signature
     * it yields has not been read from the released plugin, and the conversion seat DECLINES a {@code Long} item
     * (its lower-cased name is the keyword {@code long}). The group to pin first: {@code conv-bigint-alias-long}.
     */
    private String intLiteralJavaSimpleName(RIntLiteral lit) {
        if (generatorModel == null || generatorModel.workspace() == null || typeTranslator == null) {
            return "Integer";
        }
        var inferred = generatorModel.workspace().getInferredType(lit);
        if (inferred == null || inferred.isMissing()) {
            return "Integer";
        }
        JavaType java = typeTranslator.toJavaReferenceType(inferred.type());
        if (java instanceof JavaClass<?> cls) {
            String simple = cls.getSimpleName();
            if ("Integer".equals(simple) || "Long".equals(simple) || "BigInteger".equals(simple)) {
                return simple;
            }
        }
        return "Integer";
    }

    /**
     * facet arithOperandWrapperCoerce (PR #334): the {@link JavaType} for an
     * unambiguous basic signature name — the java.lang/java.math scalars the
     * walk reports by NAME without a {@code javaItemType}. Data-model /
     * date-time / meta names return {@code null} (unchanged decline).
     */
    private JavaType basicJavaTypeForName(String javaTypeName) {
        if (javaTypeName == null || typeUtil == null) {
            return null;
        }
        return switch (javaTypeName) {
            case "Integer" -> typeUtil.INTEGER;
            case "Long" -> typeUtil.LONG;
            case "BigInteger" -> typeUtil.BIG_INTEGER;
            case "BigDecimal" -> typeUtil.BIG_DECIMAL;
            case "String" -> typeUtil.STRING;
            case "Boolean" -> typeUtil.BOOLEAN;
            default -> null;
        };
    }

    /**
     * facet interior_position_coercion: the alias's WRAPPER KIND — {@code true}
     * for a {@code MapperC}-signatured alias, {@code false} for {@code MapperS},
     * {@code null} when the walk declines (no expression, {@code usesOutput}
     * builder-form, unresolvable, or missing type infrastructure).
     *
     * <p>The meta-arm sibling above returns {@code null} for every non-meta
     * alias because its consumers need the concrete meta ITEM witness; the
     * wrapper-kind read has no such need — {@code chainRendersMapperC}'s
     * alias-head arm only asks whether the chain RIDES a {@code MapperC}, so a
     * non-meta multi alias (e.g. {@code interestRatePayouts} riding into a
     * downstream meta step's coercion) must classify here. Same single walk
     * ({@link #inferExpressionType}) that renders the alias method's declared
     * {@code MapperS/MapperC} signature, so the kind a caller reads cannot
     * disagree with the wrapper in the generated bytes. Same decline ladder as
     * {@link #inferShortcutMapperJavaType} (including the {@code usesOutput}
     * barrier — such aliases return {@code List}/builder forms, never Mappers),
     * so a declined alias keeps the caller's conservative-single read — the
     * pre-facet bytes.
     */
    public Boolean inferShortcutIsMulti(RShortcut shortcut, RFunction enclosingFunc) {
        if (shortcut == null || enclosingFunc == null || shortcut.expression() == null
                || generatorModel == null || typeTranslator == null || typeUtil == null) {
            return null;
        }
        String outputName = enclosingFunc.output().map(RAttribute::name).orElse(null);
        if (outputName != null && usesOutput(shortcut.expression(), outputName)) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            ExpressionTypeInfo info =
                    inferExpressionType(shortcut.expression(), new HashSet<>(), new HashSet<>());
            return info == null ? null : info.isMulti;
        } finally {
            this.currentFunc = null;
        }
    }

    /**
     * facet aliasSumChainRoot (PR #385): the RENDER-side read for a sum whose operand the
     * engine cannot type (the workspace reports MISSING for a chain rooted at an
     * extract-constructor alias, and the compiled argument is unstamped on the
     * synthesized-nav route) — the SAME {@link #inferExpressionType} walk that renders the
     * alias METHOD's {@code MapperS<BigDecimal>} signature (the #178 same-walk law: the
     * body's {@code sumBigDecimal()} dispatch cannot disagree with the signature).
     * Returns the sum's element simple name ({@code BigDecimal}/{@code BigInteger}/
     * {@code Integer}/{@code Long} — the walk's own SUM arm applies the numeric gate) or
     * null. Scoped to a sum that IS the alias body's ROOT — directly, or as the LAST
     * then-body of a body-root then-chain (GetNetInitialMarginFromExposure's
     * {@code … filter … then value sum}) — so a sum nested anywhere else (an arithmetic
     * operand, a conditional arm) can never adopt the whole-body type.
     */
    public String aliasSumBodyElementOrNull(RShortcut shortcut, RFunction enclosingFunc,
            RListOpExpr sumExpr) {
        if (shortcut == null || enclosingFunc == null || sumExpr == null
                || shortcut.expression() == null
                || generatorModel == null || typeTranslator == null || typeUtil == null) {
            return null;
        }
        RExpression body = shortcut.expression();
        boolean sumIsBodyRoot = body == sumExpr;
        boolean sumIsThenLastBody = body instanceof RThenExpr t
                && t.body().map(f -> f.body() == sumExpr).orElse(false);
        if (!sumIsBodyRoot && !sumIsThenLastBody) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            ExpressionTypeInfo info =
                    inferExpressionType(body, new HashSet<>(), new HashSet<>());
            return info == null ? null : info.javaTypeName;
        } finally {
            this.currentFunc = null;
        }
    }

    // -------------------------------------------------------------------------
    // Output-usage detection
    // -------------------------------------------------------------------------

    static boolean usesOutput(RExpression expr, String outputName) {
        return walkForOutput(expr, outputName, new HashSet<>());
    }

    /**
     * facet aliasOutputBuilderNav (PR #381, W): the OUTPUT-ROOTED disguised-chain
     * discriminator — the alias body's receiver-most root is a FULLY-unresolved
     * {@code REnumValueRef} (no enumeration, no resolved symbol — the #346
     * disguised-nav class: the parser cannot root a nav at the function OUTPUT, so
     * {@code product -> contractualProduct -> …} parses disguised) whose enumName IS
     * the output name. Such an alias navigates FROM the output: golden threads the
     * output BUILDER as the alias's first param, returns the NAV ELEMENT's builder,
     * wraps the body {@code return toBuilder(<nav>.get());} and every call site
     * {@code MapperS.of(<alias>(<out>.toBuilder(), …).build())} (cdm5
     * NewEquitySwapProduct — the single corpus carrier; the corpus rosetta scan
     * finds exactly one output-rooted alias). Root-only by design: a non-root
     * disguised output usage keeps today's route. Consumed by {@code analyze}, the
     * {@code createScope} output seed, the {@code compileAliases} body wrap and the
     * {@code ReferenceHandler} call-site wrap — the #178 same-walk invariant. The
     * {@code inferShortcut*} gates deliberately keep the RESOLVED-only
     * {@code usesOutput} read, so a W alias falls through them and its call-site
     * receiver typing (witnesses + lambda naming) stays exactly today's.
     */
    public static boolean isOutputBuilderNavAlias(RExpression expr, String outputName) {
        if (expr == null || outputName == null) {
            return false;
        }
        RExpression root = expr;
        while (root instanceof com.regnosys.rosetta.ast.expressions.references.RFeatureCall fc) {
            root = fc.receiver();
        }
        return root instanceof com.regnosys.rosetta.ast.expressions.references.REnumValueRef evr
                && evr.enumeration().isEmpty()
                && evr.resolvedSymbol().isEmpty()
                && outputName.equals(evr.enumName());
    }

    /**
     * facet aliasOutputBuilderNav (PR #381, W): the W-class return type — the NAV
     * ELEMENT's builder form ({@code Payout.PayoutBuilder} from the walked element
     * {@code Payout}), NOT the function output's builder (the existing usesOutput
     * arm's law — that class returns the output itself). Mirrors the existing arm's
     * multi law ({@code List<…>} wrap).
     */
    private SeamRender buildOutputNavBuilderReturnType(ExpressionTypeInfo typeInfo) {
        String bt = typeInfo.javaTypeName + "." + typeInfo.javaTypeName + "Builder";
        return new SeamRender(typeInfo.isMulti ? "List<" + bt + ">" : bt,
                FunctionTemplateModel.AliasSeam.builder(typeInfo.isMulti));
    }

    /**
     * facet aliasSeamSignature (PR #612): an alias seam string together with the typed facts it was rendered
     * from — produced by ONE branch per form ({@link #buildMapperReturnType}, {@link #computeReturnType},
     * {@link #buildOutputNavBuilderReturnType}), never derived from the text.
     */
    record SeamRender(String text, FunctionTemplateModel.AliasSeam seam) { }

    static boolean walkForOutput(RNode node, String outputName, Set<RNode> visited) {
        if (node == null || !visited.add(node)) {
            return false;
        }

        if (node instanceof RSymbolReference ref) {
            if (outputName.equals(ref.name())) {
                return true;
            }
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RShortcut shortcut) {
                if (walkForOutput(shortcut.expression(), outputName, visited)) {
                    return true;
                }
            }
        }

        for (RNode child : node.children()) {
            if (walkForOutput(child, outputName, visited)) {
                return true;
            }
        }

        return false;
    }

    // -------------------------------------------------------------------------
    // Return-type computation
    // -------------------------------------------------------------------------

    /**
     * Pure return-type computation from pre-walked type info. No expression
     * walk — callers pass {@code typeInfo} from a single upstream walk
     * (typically {@code doAnalyze}'s per-shortcut walk) so walk cost stays
     * O(1) per alias and type/refs stay in lockstep.
     *
     * <p>Branches:
     * <ol>
     *   <li>{@code usesOutput} → builder-form of the function's output.</li>
     *   <li>{@code typeInfo != null} → {@code MapperS/MapperC<? extends T>}
     *       from the walked expression type.</li>
     *   <li>Fallback → function output's raw type in a mapper wrapper
     *       (legacy zero-arg-constructor behaviour).</li>
     * </ol>
     */
    private SeamRender computeReturnType(boolean usesOutput, Optional<RAttribute> outputOpt,
                                     ExpressionTypeInfo typeInfo, boolean returnElementCollides) {
        if (usesOutput) {
            return outputOpt
                    .map(out -> {
                        String bt = builderType(out);
                        boolean multi = isMultiValued(out);
                        return new SeamRender(multi ? "List<" + bt + ">" : bt,
                                FunctionTemplateModel.AliasSeam.builder(multi));
                    })
                    .orElse(new SeamRender("Object", FunctionTemplateModel.AliasSeam.UNKNOWN));
        }

        if (typeInfo != null) {
            return buildMapperReturnType(typeInfo, returnElementCollides);
        }

        // Fallback: use function output type (legacy behaviour for zero-arg constructor)
        // v3.2 seat 7 (F11), round 1 (the code-quality review's NIT-4): this fallback renders the RAW rune type name
        // (`MapperC<? extends string>` - a Rune name in Java, a non-compiling signature) whenever the signature walk
        // declines; the seat measured it at the literal-keyed control (ControlLiteral's `switched` alias, the pre-fix
        // compile-gate pin `cannot find symbol: class string`) and routed the lambda-item switch around it (the walk's
        // lambdaItemSubject arms). The fallback itself is BANKED - a typed fallback or a refusal, with the oracle group
        // that reaches it - and the literal-guard block admits only the kinds the walk signs (LAW 69, SF-6).
        return outputOpt
                .map(out -> {
                    String typeName = rawTypeName(out);
                    boolean multi = isMultiValued(out);
                    String wrapper = multi ? "MapperC" : "MapperS";
                    // v3.2 seat 12 (D52, COUNTERS FIRST - the LOUD register's alias-signature site): a BUILTIN output
                    // type's raw rune name (`string`, `number`, an alias of one) is never a Java type - the seam this
                    // leg rendered (`MapperS<? extends number>`, the chaos M6b rows; the `? extends string` seams of
                    // C26AliasArms / C26Nested) is non-compiling and was SILENT. Refused by name; a MODEL-typed output
                    // keeps the fallback (its raw name is the Java simple name - the rings decide whether any vendored
                    // alias rests on it). The walk's typing at the declining seats is seat 13's (D52 decision 3).
                    if (outputRawNameIsBuiltin(out)) {
                        throw SilentDegradation.refuse(SilentDegradation.Site.ALIAS_SIGNATURE_RAW_TYPE,
                                "alias signature fell to the function output's raw rune type name '" + typeName
                                        + "' (a builtin, never a Java type) because the signature walk declined -"
                                        + " the pre-seat render was " + wrapper + "<? extends " + typeName + ">",
                                out);
                    }
                    return new SeamRender(wrapper + "<? extends " + typeName + ">",
                            FunctionTemplateModel.AliasSeam.mapper(multi, true, typeName));
                })
                .orElse(new SeamRender("MapperS<?>",
                        FunctionTemplateModel.AliasSeam.mapper(false, true, null)));
    }

    /**
     * facet aliasSigWildcardDecl (seat 28, law D): THE wildcard predicate of the alias
     * signature, extracted so the DECLARATION seats that must agree with the signature
     * consult the very expression that emits it instead of re-deriving one (LAW 69 - a
     * call site sharing a law with a declaration CONSULTS it). A Rosetta MODEL element
     * renders upper-bounded {@code Mapper*<? extends T>}; a basic/enum/record element
     * renders INVARIANT {@code Mapper*<T>}. Byte-inert by construction:
     * {@link #buildMapperReturnType} now calls this instead of testing the field inline,
     * and it tests the identical field.
     */
    static boolean aliasReturnIsWildcarded(ExpressionTypeInfo typeInfo) {
        return typeInfo != null && typeInfo.isRosettaModelType;
    }

    /**
     * facet aliasSigWildcardDecl (seat 28, law D): the alias's SIGNATURE verdict for a
     * hoisted local's declared element - the model item Java class when (and only when)
     * {@link #aliasReturnIsWildcarded} says the emitted signature is
     * {@code Mapper*<? extends T>}; {@code null} otherwise.
     *
     * <p>Runs the IDENTICAL single {@link #inferExpressionType} walk that produced the
     * alias method's declared return type (the #178 same-walk law), with the same
     * {@code usesOutput} barrier and the same {@link #resolveModelTypeByName} canonical
     * disambiguation as its sibling {@link #inferShortcutModelItemJavaClass}. The
     * difference is only the RETURN CONTRACT: the sibling deliberately answers
     * {@code null} for the META arm (its navigation-seat consumers depend on that #352
     * boundary), whereas this entry answers for every wildcarded element kind - meta
     * wrappers included - because a local assigned from a meta alias call owes the
     * wildcard exactly as much as one assigned from a bare-model alias call.
     *
     * <p>Consumed by {@code NavigationHandler.aliasSignatureWildcardItemType} and,
     * through it, by the then-arg decl seat and the conditional-ladder hoist decl seat,
     * so a declared local and the alias method it is assigned from cannot disagree.
     */
    public JavaClass<?> aliasSignatureWildcardElementOrNull(RShortcut shortcut,
            RFunction enclosingFunc) {
        if (shortcut == null || enclosingFunc == null || shortcut.expression() == null
                || generatorModel == null || typeTranslator == null || typeUtil == null) {
            return null;
        }
        String outputName = enclosingFunc.output().map(RAttribute::name).orElse(null);
        if (outputName != null && usesOutput(shortcut.expression(), outputName)) {
            return null;
        }
        this.currentFunc = enclosingFunc;
        try {
            ExpressionTypeInfo info =
                    inferExpressionType(shortcut.expression(), new HashSet<>(), new HashSet<>());
            if (!aliasReturnIsWildcarded(info)) {
                return null;
            }
            // The META arm carries its concrete wrapper directly (the same javaItemType
            // inferShortcutMapperJavaType reads); every other model element resolves by
            // name + canonical exactly as inferShortcutModelItemJavaClass does.
            if (info.javaItemType instanceof JavaClass<?> metaItem) {
                return metaItem;
            }
            RDataType dt = resolveModelTypeByName(info.javaTypeName, info.javaItemCanonical);
            return dt == null ? null : typeTranslator.toJavaReferenceType(new RDataTypeRef(dt));
        } finally {
            this.currentFunc = null;
        }
    }

    private SeamRender buildMapperReturnType(ExpressionTypeInfo typeInfo, boolean returnElementCollides) {
        String wrapper = typeInfo.isMulti ? "MapperC" : "MapperS";
        String typeName = typeInfo.javaTypeName;

        if (aliasReturnIsWildcarded(typeInfo)) {
            // facet importCollisionFqn return-type (PR #247) — the #245 return-type-as-LOSER
            // completion. When the return element COLLIDES with a signature type seeded first
            // (a fpml input param whose simple name equals the cdm return element, a DIFFERENT
            // canonical), emit the model-type element as an ImportCollisionResolver typeRef
            // sentinel carrying the CANONICAL (javaItemCanonical, set at the model-type leaf via
            // getCanonicalName().withDots()), so FunctionGenerator renders THIS loser
            // FULLY-QUALIFIED inline and suppresses its import. The sentinel is resolved against
            // the import-collision seed across BOTH the abstract + impl method declarations (this
            // string) AND the body's typed empty else `MapperS.<Item>ofNull()` — which
            // typedEmptyElseOrNull lifts verbatim from this same string, so it rides along for
            // free. OFF-collision (the overwhelming majority — gated by returnElementCollides) the
            // element renders BARE directly, so the function stays off the import-collision
            // resolution path (the fast path; Copilot R1). A null canonical (the structurally-bare
            // model-type leaf) likewise stays bare — it can never be FQN-ed, so it never loses.
            // Sentinel-safety is STRUCTURAL since PR #612 (facet aliasSeamSignature): the seam's
            // FORM is recorded independently of its ELEMENT, so the form readers (the
            // MapperS/MapperC gates in FunctionExpressionRenderer / FunctionGenerator / the
            // optimised policy) never see the sentinel, and the element readers —
            // typedEmptyElseOrNull, typedEmptyAnyMapperOrNull, aliasReturnElementOrNull,
            // AliasValueSeamPolicy.valueReturnTypeOrNull / multiElementText — carry it
            // verbatim into the ofNull terminals and the value re-seam, which is exactly
            // where it must ride.
            String element = (returnElementCollides && typeInfo.javaItemCanonical != null)
                    ? ImportCollisionResolver.typeRef(typeInfo.javaItemCanonical)
                    : typeName;
            return new SeamRender(wrapper + "<? extends " + element + ">",
                    FunctionTemplateModel.AliasSeam.mapper(typeInfo.isMulti, true, element));
        } else {
            return new SeamRender(wrapper + "<" + typeName + ">",
                    FunctionTemplateModel.AliasSeam.mapper(typeInfo.isMulti, false, typeName));
        }
    }

    /**
     * facet importCollisionFqn return-type (PR #247): the signature simple-name &rarr; canonical(s)
     * map (function output + inputs) used to gate the return-type sentinel to the actual-collision
     * case. Built once per function. A signature type whose Java reference type cannot be resolved
     * is simply omitted (conservative — the resolver still renders any emitted sentinel bare
     * off-collision, so an omission can only miss a flip, never cause a regression).
     */
    private Map<String, Set<String>> buildSignatureCanonicals(RFunction func, Optional<RAttribute> outputOpt) {
        Map<String, Set<String>> map = new HashMap<>();
        outputOpt.ifPresent(out -> addSignatureType(map, out));
        for (RAttribute in : func.inputs()) {
            addSignatureType(map, in);
        }
        return map;
    }

    private void addSignatureType(Map<String, Set<String>> map, RAttribute attr) {
        if (attr == null || generatorModel == null || typeTranslator == null) {
            return;
        }
        try {
            JavaClass<?> jc = typeTranslator.toJavaReferenceType(generatorModel.getType(attr));
            if (jc != null) {
                map.computeIfAbsent(jc.getSimpleName(), k -> new HashSet<>())
                        .add(jc.getCanonicalName().withDots());
            }
        } catch (Exception e) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            // conservative: a signature type we cannot resolve simply does not gate.
        }
    }

    /**
     * facet importCollisionFqn return-type (PR #247): true when a signature type shares the return
     * element's Java simple name but a DIFFERENT canonical (so the return element loses the
     * first-claim to a seeded signature type and must render FULLY-QUALIFIED).
     */
    private static boolean signatureSimpleNameCollides(Map<String, Set<String>> sigCanonicalsBySimple,
            String returnElementSimpleName, String returnElementCanonical) {
        Set<String> canonicals = sigCanonicalsBySimple.get(returnElementSimpleName);
        return canonicals != null && canonicals.stream().anyMatch(c -> !c.equals(returnElementCanonical));
    }

    // -------------------------------------------------------------------------
    // Expression type inference (inline, walking the AST)
    // -------------------------------------------------------------------------

    /**
     * Holds inferred type information for an alias expression.
     */
    static class ExpressionTypeInfo {
        final String javaTypeName;
        final boolean isMulti;
        final boolean isRosettaModelType;
        /**
         * The concrete item {@link JavaClass} when the walk resolved one beyond
         * the simple name — today ONLY the {@link RJavaWithMetaValue} meta
         * wrapper built at the meta arm of {@code resolveAttributeTypeInfo}
         * (facet meta_coercion_numbering arm A1:
         * {@link #inferShortcutMapperJavaType} re-types an alias-call
         * navigation receiver from it so the dormant
         * {@code ExpressionCompiler.coerceNavigationReceiver} gate fires);
         * {@code null} everywhere else.
         */
        final JavaClass<?> javaItemType;
        /**
         * facet importCollisionFqn (PR #245): the dotted CANONICAL name of the element
         * type when the walk resolved a concrete Java class for it — the fully-qualified
         * form of {@link #javaTypeName} (a model type, or the meta wrapper). {@code null}
         * for primitives/enums/unresolved walks. Purely INFORMATIONAL — unlike
         * {@link #javaItemType} (which {@code inferShortcutMapperJavaType} consumes to
         * activate the meta-coercion gate) this field is read ONLY by {@link #analyze} to
         * surface the alias signature return-type canonical for the
         * {@code FunctionGenerator} import-collision SEED, so populating it on non-meta
         * model types carries no behavioural coupling.
         */
        final String javaItemCanonical;

        ExpressionTypeInfo(String javaTypeName, boolean isMulti, boolean isRosettaModelType) {
            this(javaTypeName, isMulti, isRosettaModelType, null, null);
        }

        ExpressionTypeInfo(String javaTypeName, boolean isMulti, boolean isRosettaModelType,
                JavaClass<?> javaItemType) {
            this(javaTypeName, isMulti, isRosettaModelType, javaItemType, null);
        }

        ExpressionTypeInfo(String javaTypeName, boolean isMulti, boolean isRosettaModelType,
                JavaClass<?> javaItemType, String javaItemCanonical) {
            this.javaTypeName = javaTypeName;
            this.isMulti = isMulti;
            this.isRosettaModelType = isRosettaModelType;
            this.javaItemType = javaItemType;
            this.javaItemCanonical = javaItemCanonical;
        }
    }

    /**
     * facet aliasLadderJoin (PR #349, C): join two arm {@link ExpressionTypeInfo}s per
     * upstream's conditional/default/arithmetic typing ({@code RosettaTypeProvider}
     * {@code caseConditionalExpression}/{@code caseDefaultOperation} →
     * {@code typeSystem.joinMetaAnnotatedTypes}; {@code CardinalityProvider}: any arm
     * multi → multi; {@code SubtypeRelation.join}):
     * <ul>
     *   <li>one side null (untypable — the {@code empty} arm) → the OTHER
     *       ({@code join(⊥,T)=T});</li>
     *   <li>equal type names → keep, cardinality OR-ed;</li>
     *   <li>both numeric → widen ({@code Integer < BigInteger < BigDecimal} —
     *       {@code RNumberType.join});</li>
     *   <li>wrapper-vs-value ({@code FieldWithMetaX}/{@code ReferenceWithMetaX} item
     *       against the bare {@code X}) → the VALUE (the meta INTERSECT — golden
     *       Create_Exercise cdm5 {@code MapperS<? extends OptionPayout>});</li>
     *   <li>both model types → the nearest common Rosetta ancestor by
     *       supertype-chain walk ({@code joinByTraversingAncestorsAndAliases} —
     *       golden MapReferenceObligation {@code UnderlyingAsset});</li>
     *   <li>otherwise UNJOINABLE → the FIRST arm (the pre-facet then/left read —
     *       today's bytes, strictly conservative).</li>
     * </ul>
     */
    private ExpressionTypeInfo joinArmTypes(ExpressionTypeInfo a, ExpressionTypeInfo b,
            Set<RType> collectedRefs) {
        // Sanitize BEFORE the null-substitution: some walk arms echo unresolved
        // SYMBOL names as type names (`endDate`, `multiplier`, `commodityAsianModel`
        // — the cp4b junk-mover catch); the pre-facet then-only read never surfaced
        // them, and the join must not either. Every legit Java type name here is
        // Capitalized (Integer/BigDecimal/Bond/FieldWithMetaX/…), so a
        // lowercase-initial name marks the arm UNTYPABLE.
        a = sanitizeArm(a);
        b = sanitizeArm(b);
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        boolean multi = a.isMulti || b.isMulti;
        if (a.javaTypeName != null && a.javaTypeName.equals(b.javaTypeName)) {
            return new ExpressionTypeInfo(a.javaTypeName, multi, a.isRosettaModelType,
                    a.javaItemType != null ? a.javaItemType : b.javaItemType,
                    a.javaItemCanonical != null ? a.javaItemCanonical : b.javaItemCanonical);
        }
        Integer na = numericRank(a.javaTypeName);
        Integer nb = numericRank(b.javaTypeName);
        if (na != null && nb != null) {
            ExpressionTypeInfo wider = na >= nb ? a : b;
            return new ExpressionTypeInfo(wider.javaTypeName, multi, false);
        }
        ExpressionTypeInfo metaJoin = joinWrapperValue(a, b, multi);
        if (metaJoin == null) {
            metaJoin = joinWrapperValue(b, a, multi);
        }
        if (metaJoin != null) {
            return metaJoin;
        }
        if (a.isRosettaModelType && b.isRosettaModelType) {
            RDataType lub = nearestCommonModelAncestor(
                    resolveModelTypeByName(a.javaTypeName, a.javaItemCanonical),
                    resolveModelTypeByName(b.javaTypeName, b.javaItemCanonical));
            if (lub != null) {
                RType lubRef = recordRef(new RDataTypeRef(lub), collectedRefs);
                JavaClass<?> lubClass = typeTranslator == null ? null
                        : typeTranslator.toJavaReferenceType(lubRef);
                return new ExpressionTypeInfo(lub.name(), multi, true, null,
                        lubClass == null ? null : lubClass.getCanonicalName().withDots());
            }
        }
        return a;
    }

    /**
     * v3.2 seat 2 (Law 4): the WHOLE piped value of the enclosing then-body — the then-receiver
     * at its own cardinality — for an operator that elides its left operand (`then default [x]`).
     * The {@code RImplicitVariable} arm types the ELEMENT (single) because a bare `item` names
     * one; an elided operand names the pipe itself. {@code null} outside a bound then-body (the
     * binding is the NEAREST enclosing THEN whose receiver this walk could type; an untypable
     * inner receiver keeps the outer binding), and {@code null} when the live binding was pushed
     * by the EXTRACT arm (PR #623, round-1 cq review, SF-5), whose binding is the element and
     * whose {@code isMulti} is the receiver's: inside an extract lambda the pipe is not what an
     * elided operand names. No corpus carrier reaches that case; the flag is the fail-closed
     * guard this javadoc promised.
     */
    private ExpressionTypeInfo thenPipedWholeOrNull() {
        if (thenItemBinding == null || !thenItemBindingFromThen) {
            return null;
        }
        return new ExpressionTypeInfo(thenItemBinding.javaTypeName, thenItemBinding.isMulti,
                thenItemBinding.isRosettaModelType, thenItemBinding.javaItemType,
                thenItemBinding.javaItemCanonical);
    }

    /** Junk-name guard for {@link #joinArmTypes} — see the sanitize note there. */
    private static ExpressionTypeInfo sanitizeArm(ExpressionTypeInfo info) {
        if (info == null || info.javaTypeName == null || info.javaTypeName.isEmpty()
                || !Character.isUpperCase(info.javaTypeName.charAt(0))) {
            return null;
        }
        return info;
    }

    /** Numeric widen rank per {@code RNumberType.join}; null for non-numeric names. */
    private static Integer numericRank(String javaTypeName) {
        if ("Integer".equals(javaTypeName)) {
            return 0;
        }
        if ("BigInteger".equals(javaTypeName)) {
            return 1;
        }
        if ("BigDecimal".equals(javaTypeName)) {
            return 2;
        }
        return null;
    }

    /**
     * The wrapper-vs-value half of the meta INTERSECT: {@code w}'s item is a meta
     * WRAPPER whose value type is {@code v}'s type → the join is the bare value
     * ({@code SubtypeRelation.join} keeps the type and intersects the meta lists —
     * a bare arm has none). Read from the RESOLVED {@link RJavaWithMetaValue} item alone.
     *
     * <p>facet aliasSeamSignature (PR #612): the wrapper NAMING-convention fallback
     * ({@code FieldWithMetaX} / {@code ReferenceWithMetaX} name prefixes) is GONE. The C2c
     * census measured every wrapper-vs-value join on all three routes — 572 (default) /
     * 700 (IR) / 934 (optimised) arrivals — and the two name arms took ZERO of them:
     * every join reaches the typed item read below. Null when not this shape.
     */
    private static ExpressionTypeInfo joinWrapperValue(ExpressionTypeInfo w,
            ExpressionTypeInfo v, boolean multi) {
        String valueName = null;
        if (w.javaItemType instanceof RJavaWithMetaValue meta) {
            valueName = meta.getValueType().getSimpleName();
        }
        if (valueName == null || !valueName.equals(v.javaTypeName)) {
            return null;
        }
        return new ExpressionTypeInfo(v.javaTypeName, multi, v.isRosettaModelType,
                null, v.javaItemCanonical);
    }

    /**
     * The nearest common Rosetta MODEL ancestor of two type names — both chains
     * resolve BY NAME through fresh detached {@link RTypeCall}s (the #347-F8
     * deep-copy law) and walk {@code RDataType.superType()}; the first name of
     * {@code b}'s chain contained in {@code a}'s chain wins. Null when either side
     * does not resolve to a data type or no common ancestor exists.
     */
    private RDataType nearestCommonModelAncestor(RDataType aRoot, RDataType bRoot) {
        if (aRoot == null || bRoot == null) {
            return null;
        }
        java.util.Set<String> aChain = new java.util.LinkedHashSet<>();
        for (RDataType dt = aRoot; dt != null; dt = dt.superType().orElse(null)) {
            if (!aChain.add(dt.name())) {
                break;
            }
        }
        java.util.Set<RDataType> seen = java.util.Collections.newSetFromMap(
                new java.util.IdentityHashMap<>());
        for (RDataType dt = bRoot; dt != null && seen.add(dt);
                dt = dt.superType().orElse(null)) {
            if (aChain.contains(dt.name())) {
                return dt;
            }
        }
        return null;
    }

    /**
     * facet aliasThenSigRenderTruth (PR #351, i2): the then-item binding's MODEL type
     * for feature resolution. A META-WRAPPER element resolves through its VALUE type
     * (render-truth: the rendered arms deref via "Type coercion" and nav the value —
     * ReferenceWithMetaX carries no model attributes and is not a model root element).
     * Null when unbound, non-model, or unresolvable.
     */
    private RDataType thenItemBindingModelType() {
        if (thenItemBinding == null || !thenItemBinding.isRosettaModelType) {
            return null;
        }
        String lookupName = thenItemBinding.javaTypeName;
        String lookupCanonical = thenItemBinding.javaItemCanonical;
        if (thenItemBinding.javaItemType instanceof RJavaWithMetaValue metaItem
                && metaItem.getValueType() instanceof JavaClass<?> valueClass) {
            lookupName = valueClass.getSimpleName();
            lookupCanonical = valueClass.getCanonicalName().withDots();
        }
        return resolveModelTypeByName(lookupName, lookupCanonical);
    }

    /**
     * facet aliasThenSigRenderTruth (PR #351): a walk result is only consumable as a
     * NEW signature source when its name is a REAL type — walk arms echo unresolved
     * SYMBOL names, which are always lowercase-initial (the joinArmTypes sanitizeArm
     * law: a legit basic or model type is always Capitalized). Guards the pipe-through
     * and item-bound recoveries; the legacy body-walk return keeps its pre-facet
     * behavior either way.
     */
    private static boolean isTypableName(ExpressionTypeInfo info) {
        return info != null && info.javaTypeName != null && !info.javaTypeName.isEmpty()
                && Character.isUpperCase(info.javaTypeName.charAt(0));
    }

    /**
     * facet aliasThenSigRenderTruth (PR #351): type a then-BODY whose bare root names
     * a FEATURE of the receiver ELEMENT (the implicit item) — the stateless walk has
     * no item binding, so an unresolved root symbol nulls the whole body walk and the
     * signature leaks the fn-output fallback. The render binds the same item to the
     * hoisted thenArg (the #350-F4 general-value hoist), so resolving the root by
     * NAME against the receiver's model type is the render-truth signature. Handles
     * the bare root and its filter/sort wrappers (both preserve the attribute's type
     * and per-item cardinality). Returns null — the legacy fallback bytes — when the
     * receiver element is not a resolvable model type or the name is not one of its
     * attributes.
     */
    private ExpressionTypeInfo inferItemBoundBodyType(RExpression body, ExpressionTypeInfo recv,
            Set<RType> collectedRefs) {
        // Null-guarded locally (Seat-1 #351 OBS-4): the sole caller pre-checks via
        // isTypableName(recv), but a future second caller must not NPE here.
        if (recv == null || !recv.isRosettaModelType) {
            return null;
        }
        RExpression root = body;
        while ((root instanceof RFilterExpr || root instanceof RSortExpr)
                && root.left().isPresent()) {
            root = root.left().get();
        }
        if (!(root instanceof RSymbolReference bare) || bare.symbol().isPresent()) {
            return null;
        }
        RDataType elemType = resolveModelTypeByName(recv.javaTypeName, recv.javaItemCanonical);
        if (elemType == null) {
            return null;
        }
        return findAttribute(new RDataTypeRef(elemType), bare.name())
                .map(attr -> resolveAttributeTypeInfo(attr, bare, collectedRefs))
                .orElse(null);
    }

    /**
     * Resolve a MODEL data type by simple name, disambiguating same-named types
     * across loaded models by the arm's translated CANONICAL when available (the
     * namespace trap: the cdm6 cell loads cdm AND fpml.consolidated — "Bond" exists
     * in both, and a first-wins simple-name lookup can pick the wrong model, rooting
     * the ancestor chain in the wrong hierarchy).
     */
    private RDataType resolveModelTypeByName(String simpleName, String canonicalOrNull) {
        if (generatorModel == null || simpleName == null) {
            return null;
        }
        // Fast path (Copilot #349 R1): with no canonical to disambiguate, the existing
        // first-wins by-name resolution answers without the O(files) scan below — same
        // first-match semantics over the same root-element universe. In practice model
        // arms always carry canonicals (resolveAttributeTypeInfo populates them), so
        // the scan runs only for genuine cross-model LUB joins.
        if (canonicalOrNull == null) {
            return generatorModel.resolveTypeByName(simpleName)
                    .filter(RDataTypeRef.class::isInstance)
                    .map(t -> ((RDataTypeRef) t).astNode())
                    .orElse(null);
        }
        RDataType firstMatch = null;
        for (var file : generatorModel.workspace().files()) {
            for (var elt : file.rootElements()) {
                if (elt instanceof RDataType dt && simpleName.equals(dt.name())) {
                    if (firstMatch == null) {
                        firstMatch = dt;
                    }
                    JavaClass<?> jc = typeTranslator == null ? null
                            : typeTranslator.toJavaReferenceType(new RDataTypeRef(dt));
                    if (jc != null && canonicalOrNull.equals(jc.getCanonicalName().withDots())) {
                        return dt;
                    }
                }
            }
        }
        return firstMatch;
    }

    /**
     * Infer the return type and cardinality of an alias expression by walking
     * its AST structure, and drain every resolved {@link RType} into
     * {@code collectedRefs} for downstream import collection. This is a
     * focused walker that resolves types inline using the GeneratorModel's
     * type resolution, without depending on the workspace's M4 type
     * inference engine (which has cascading MISSING issues for function
     * body expressions).
     *
     * <p>For feature call chains like {@code businessEvent -> instruction -> transfer},
     * this walker:
     * <ol>
     *   <li>Resolves the receiver's type (e.g., BusinessEvent from the input parameter)</li>
     *   <li>Finds the named feature on that type (e.g., 'instruction' on BusinessEvent)</li>
     *   <li>Gets the feature's type and cardinality</li>
     *   <li>Repeats for the next feature in the chain</li>
     * </ol>
     *
     * <p>PR-A §9.1 A3-D2-02: the caller ({@link #analyze}) translates
     * {@code collectedRefs} to Java classes via
     * {@link JavaTypeTranslator#toJavaReferenceType(RType)} and feeds them
     * to the import collector.
     *
     * <p>Package-private so the same-package {@code FunctionAliasHelperTest}
     * can invoke directly; reflection is used in the pin test to fail-loudly
     * if the declared signature changes.
     */
    ExpressionTypeInfo inferExpressionType(RExpression expr, Set<RNode> visited,
                                           Set<RType> collectedRefs) {
        if (expr == null || !visited.add(expr)) return null;

        // facet aliasThenSigRenderTruth (PR #351, i2): the implicit item inside a bound
        // then-BODY types as the receiver ELEMENT (single). Unbound walks keep the
        // pre-facet null (the fn-output fallback).
        if (expr instanceof RImplicitVariable && thenItemBinding != null) {
            return new ExpressionTypeInfo(thenItemBinding.javaTypeName, false,
                    thenItemBinding.isRosettaModelType, thenItemBinding.javaItemType,
                    thenItemBinding.javaItemCanonical);
        }

        // Feature call: resolve through receiver type → feature attribute
        if (expr instanceof RFeatureCall fc) {
            return inferFeatureCallType(fc, visited, collectedRefs);
        }

        // Deep feature call: same approach
        if (expr instanceof RDeepFeatureCall dfc) {
            return inferDeepFeatureCallType(dfc, visited, collectedRefs);
        }

        // Symbol reference: resolve to attribute/function/shortcut
        if (expr instanceof RSymbolReference ref) {
            return inferSymbolRefType(ref, visited, collectedRefs);
        }

        // Enum value reference — may be actual enum value or ambiguous parse of
        // "paramName -> featureName" (the grammar can't distinguish at parse time)
        if (expr instanceof REnumValueRef evr) {
            return inferEnumValueRefType(evr, visited, collectedRefs);
        }

        // Comparison / equality / logical → Boolean (single)
        if (expr instanceof RComparisonExpr
                || expr instanceof REqualityExpr
                || expr instanceof RLogicalExpr
                || expr instanceof RExistenceExpr
                || expr instanceof RCardinalityCheckExpr
                || expr instanceof ROnlyExistsExpr
                || expr instanceof RContainsExpr
                || expr instanceof RDisjointExpr) {
            return new ExpressionTypeInfo("Boolean", false, false);
        }

        // Count → Integer (single)
        if (expr instanceof RCountExpr) {
            return new ExpressionTypeInfo("Integer", false, false);
        }

        // Arithmetic — facet aliasLadderJoin (PR #349, C): DIVISION always types
        // BigDecimal (upstream caseArithmeticOperation — golden
        // ResolveSecurityFinanceBillingAmount `MapperS<BigDecimal> billingQuantity`
        // for `divide(1, …)`; the pre-facet left-literal read leaked Integer, which
        // never compiled against the body — every carrier waivered); the other ops
        // JOIN the operand types (numeric widen Integer < BigInteger < BigDecimal —
        // RNumberType.join; equal names keep — the String-add left=String case is
        // join(String,String)=String, byte-identical to the old left read).
        if (expr instanceof RArithmeticExpr arith) {
            if (arith.op() == ArithOp.DIVIDE) {
                return new ExpressionTypeInfo("BigDecimal", false, false);
            }
            ExpressionTypeInfo arithLeft = arith.left().isPresent()
                    ? inferExpressionType(arith.left().get(), visited, collectedRefs)
                    : null;
            ExpressionTypeInfo arithRight = arith.right().isPresent()
                    ? inferExpressionType(arith.right().get(), visited, collectedRefs)
                    : null;
            return joinArmTypes(arithLeft, arithRight, collectedRefs);
        }

        // Then expression → infer from body expression
        if (expr instanceof RThenExpr then) {
            // facet aliasThenSignatureTyping (PR #252): a then-body that is a bare-item
            // CARDINALITY list-op (`then [ item first/last/only-element/flatten ]`) types
            // by applying the op to the then-RECEIVER's type. The stateless body walk
            // cannot type the implicit `item` (it has no enclosing-receiver binding), so
            // `inferExpressionType(body)` nulled and computeReturnType leaked the function
            // OUTPUT type — emitting e.g. `MapperC<? extends Counterparty>` where golden's
            // `.first()` yields `MapperS<? extends PayerReceiverModel>`. Green-safe by
            // construction: ON CLEAN MAIN an alias whose expression is an RThenExpr renders the
            // broken runtime `.then(item -> …)` form (no such method), so every such alias file
            // is ALREADY a waivered, non-compiling mismatch — this arm can only flip a waivered
            // file toward golden (e.g. MapCommoditySwapCounterpartyList), never turn a
            // pre-existing green file red. Only the
            // cardinality ops are handled here (first/last/only-element collapse to single;
            // flatten preserves multi); a feature-extract / value-transform then-body keeps
            // the legacy fall-through (today's bytes). The op set MIRRORS
            // FunctionExpressionRenderer.isBareItemCardinalityThenBody (the
            // renderAliasThenHoistOrNull VALUE gate) — keep the two in lockstep so the signature
            // recovery and the body hoist fire on exactly the same shape.
            RExpression thenBody = then.body().map(b -> b.body()).orElse(null);
            if (thenBody instanceof RListOpExpr lop && lop.argument() instanceof RImplicitVariable) {
                ListOp op = lop.op();
                // facet aliasSigReturnTypeLeak (PR #342): DISTINCT joins the op set —
                // `then distinct` PRESERVES the receiver's cardinality (upstream
                // caseDistinctOperation wraps the operand's own Mapper kind), so a
                // `… then distinct then only-element` alias chain types through to the
                // extract element (golden CashPriceQuantityNoOfUnitsTriangulation
                // `MapperS<BigDecimal> notional`; the pre-facet NULL leaked the function
                // output `MapperS<? extends boolean>` — never compiled against the body,
                // so every carrier is waivered). Same lockstep note as below: the op set
                // MIRRORS FunctionExpressionRenderer.isBareItemCardinalityThenBody (less REVERSE,
                // below).
                // v3.2 seat 2 (Law 1, the chaos C20Spread rows — F17): REVERSE joins the set. It is
                // the same shape as DISTINCT (cardinality-preserving, element-preserving: upstream's
                // caseReverseOperation returns the operand's own MapperC), and it was the ONE op of
                // `ordered then reverse then first` outside the set — so the inner then nulled, the
                // outer FIRST arm saw no receiver, and computeReturnType leaked the function OUTPUT
                // type (`MapperS<? extends number>` where golden says `MapperS<? extends C20Leaf>`).
                // REVERSE is deliberately NOT mirrored into isBareItemCardinalityThenBody or
                // inferRTypeFromExpr's twin (PR #623's witness sweep): both mirrors stayed green
                // with REVERSE off (lanes O, P), so they were withdrawn under the #614 law - the
                // mirrors' op sets are the pre-seat five until a carrier witnesses REVERSE there.
                // The `multi` below keeps the operand's cardinality for REVERSE (multi in, multi
                // out) - the cardinality half of this witnessed arm; its corpus carrier (`ordered
                // then reverse then first`) collapses through FIRST, so its witness is the seat
                // fixture F4Back's bare `alias revAll: leaves then reverse`, asserted
                // `MapperC<? extends F4Leaf>` by LambdaTypeJoinSeatTest.a1 (lane AG goes red with
                // the sub-arm off).
                if (SIGNATURE_ELEMENT_PRESERVING_OPS.contains(op)) {
                    ExpressionTypeInfo recv = inferExpressionType(then.argument(), visited, collectedRefs);
                    if (recv != null) {
                        boolean multi = op == ListOp.FLATTEN
                                || ((op == ListOp.DISTINCT || op == ListOp.REVERSE) && recv.isMulti);
                        return new ExpressionTypeInfo(recv.javaTypeName, multi,
                                recv.isRosettaModelType, recv.javaItemType, recv.javaItemCanonical);
                    }
                }
            }
            if (then.body().isPresent() && then.body().get().body() != null) {
                // facet aliasThenSigRenderTruth (PR #351): the #350-F4 GENERAL-VALUE
                // admission renders alias then-chain BODIES golden (hoisted thenArgN
                // levels), but this walk still leaked on three shapes, emitting a
                // signature the rendered body contradicts (a non-compiling, waivered
                // mismatch — green-safe by the same #252 argument):
                //   (i)  a bare `then filter <cond>` / `then sort` body (absent left)
                //        pipes the WHOLE receiver through unchanged — the walk nulled
                //        and computeReturnType leaked the fn-output type (the
                //        GetRegimeSpecificIdentifiers lowercase-`string` leak);
                //   (ii) a body whose bare root names a FEATURE of the receiver
                //        ELEMENT (`then transactionIdentifier filter …`) — the
                //        stateless walk cannot bind the implicit item, so resolve the
                //        root by NAME against the receiver's model type (render-truth:
                //        the body render binds the same item);
                //   (iii) a PER-ITEM single body over a MULTI receiver is elementwise
                //        MULTI (`… filter … then transfer` = MapperC<? extends
                //        Transfer>, Get_OptionPremiumOnEventDate) — composition gated
                //        to ATTRIBUTE-rooted bodies (a pipe-consuming body like
                //        `then count` / `then only-element` keeps its own cardinality;
                //        the collapse ops are the #252 arm above).
                RExpression thenBodyExpr = then.body().get().body();
                ExpressionTypeInfo recv = then.argument() != null
                        ? inferExpressionType(then.argument(), visited, collectedRefs)
                        : null;
                // A bare `then filter/sort <cond>` body parses with a SYNTHETIC implicit
                // input as its left (AstBuilder.visitFilterWithoutLeftExpr) — never an
                // empty left — so the pipe-through test is implicit-left-or-empty. The
                // receiver must be a REAL type: walk arms can echo unresolved SYMBOL
                // names (lowercase-initial — the sanitizeArm law), and piping such an
                // echo through REPLACED the legacy fallback with a junk signature
                // (MapIntent `MapperS<tradeHeader>`, the cp3c catch) — untypable
                // receivers keep the legacy route.
                if ((thenBodyExpr instanceof RFilterExpr || thenBodyExpr instanceof RSortExpr)
                        && (thenBodyExpr.left().isEmpty()
                                || thenBodyExpr.left().filter(RImplicitVariable.class::isInstance)
                                        .isPresent())
                        && isTypableName(recv)) {
                    return recv;
                }
                RExpression bodyRoot = thenBodyExpr;
                while ((bodyRoot instanceof RFilterExpr || bodyRoot instanceof RSortExpr)
                        && bodyRoot.left().isPresent()) {
                    bodyRoot = bodyRoot.left().get();
                }
                // Attribute-rooted = a resolved-attr root OR an unresolved root under a
                // typable binding (the i2 symbol arm resolves those against the binding's
                // element — the only route an unresolved symbol types through, so a
                // non-null body walk implies the attr lookup fired).
                boolean attrRoot = bodyRoot instanceof RSymbolReference rootRef
                        && (rootRef.symbol().filter(RAttribute.class::isInstance).isPresent()
                                || (rootRef.symbol().isEmpty() && isTypableName(recv)));
                // The body walks with the receiver ELEMENT bound as the implicit item
                // (push/pop — nested thens re-bind their own receiver), so item-rooted
                // navs / ariths / conditional arms type render-true (the
                // StandardizedSchedule `then if … then item->value*… else item->value`
                // signature = MapperS<BigDecimal>, not the `? extends number` fallback).
                ExpressionTypeInfo body;
                ExpressionTypeInfo prevBinding = this.thenItemBinding;
                boolean prevFromThen = this.thenItemBindingFromThen;
                this.thenItemBinding = isTypableName(recv) ? recv : prevBinding;
                this.thenItemBindingFromThen = isTypableName(recv) || prevFromThen;
                try {
                    body = inferExpressionType(thenBodyExpr, visited, collectedRefs);
                } finally {
                    this.thenItemBinding = prevBinding;
                    this.thenItemBindingFromThen = prevFromThen;
                }
                if (body == null && isTypableName(recv)
                        && bodyRoot instanceof RSymbolReference bareRoot
                        && bareRoot.symbol().isEmpty()) {
                    body = inferItemBoundBodyType(thenBodyExpr, recv, collectedRefs);
                    attrRoot = body != null;
                }
                if (body != null && attrRoot && recv != null
                        && recv.isMulti && !body.isMulti) {
                    return new ExpressionTypeInfo(body.javaTypeName, true,
                            body.isRosettaModelType, body.javaItemType,
                            body.javaItemCanonical);
                }
                // facet listOfListsAliasSig (PR #384): a `then extract <body>` step
                // over a MULTI receiver is elementwise MULTI regardless of the body's
                // own cardinality (upstream extract PRESERVES receiver cardinality —
                // mapItem and mapItemToList both yield MapperC), so the signature
                // types MapperC: golden `protected abstract MapperC<BigDecimal>
                // beforePriceQuantityRateOnly` — cdm Qualify_OnDemandRateChange,
                // `… extract price then flatten then filter … then extract value`.
                // The (iii) composition above deliberately gates to ATTRIBUTE-rooted
                // bodies; this arm admits exactly the extract-with-implicit-left
                // shape (an extract is per-item by construction, never
                // pipe-consuming — the collapse ops are the #252 arm). Green-safe by
                // the mirror law: upstream renders MapperC for an extract over a
                // multi receiver, so a green MapperS-signatured alias cannot carry
                // one.
                if (body != null && !body.isMulti && recv != null && recv.isMulti
                        && thenBodyExpr instanceof RExtractExpr extThenBody
                        && extThenBody.left()
                                .filter(RImplicitVariable.class::isInstance).isPresent()) {
                    return new ExpressionTypeInfo(body.javaTypeName, true,
                            body.isRosettaModelType, body.javaItemType,
                            body.javaItemCanonical);
                }
                return body;
            }
            if (then.argument() != null) {
                return inferExpressionType(then.argument(), visited, collectedRefs);
            }
        }

        // Default expression — facet aliasLadderJoin (PR #349, C): JOIN left and
        // right (upstream caseDefaultOperation joins identically to the conditional —
        // golden MapCommodityOptionToObservationTerms; a right-only-typable default
        // previously leaked the function-output fallback).
        if (expr instanceof RDefaultExpr def) {
            // v3.2 seat 2 (Law 4, the chaos C5Forms `fallback` rows): `vals then default [0]`
            // parses with NO left child (the piped value is elided — HandlerHelper
            // .orSyntheticImplicit at the render seat), so the join saw only the literal arm
            // and typed `MapperC<Integer>`. The elided left is the WHOLE then-receiver (its
            // own cardinality, not the element): golden `MapperC<BigDecimal> fallback`.
            ExpressionTypeInfo defLeft = def.left().isPresent()
                    ? inferExpressionType(def.left().get(), visited, collectedRefs)
                    : thenPipedWholeOrNull();
            ExpressionTypeInfo defRight = def.right().isPresent()
                    ? inferExpressionType(def.right().get(), visited, collectedRefs)
                    : null;
            return joinArmTypes(defLeft, defRight, collectedRefs);
        }

        // Filter/sort → same type as receiver
        if (expr instanceof RFilterExpr || expr instanceof RSortExpr) {
            if (expr.left().isPresent()) {
                return inferExpressionType(expr.left().get(), visited, collectedRefs);
            }
        }

        // facet switchBaseItemCase (PR #353): a CHOICE/TYPE-keyed switch whose EVERY
        // non-default case body is the bare implicit ITEM (`switch fpml.ReturnLeg then
        // item, default empty` — the MapReturnSwap*/MapEquitySwap* alias extract bodies)
        // types as the JOIN of the case GUARD types: the #226/#353 block lambda returns
        // the narrowed CAST var (`MapperS.of(_returnLeg)`) with a
        // `MapperS.<ReturnLeg>ofNull()` witness, so this walk must agree with those
        // rendered bytes (render truth). SINGLE per item — the enclosing extract arm
        // lifts the receiver's cardinality. Function-call-case switches keep the
        // pre-facet null fall-through (their green #226 carriers' signatures come from
        // the callee output / the legacy fallback — bytes untouched).
        if (expr instanceof RSwitchExpr sw) {
            RType joinedCase = switchItemCaseJoinedGuardType(sw);
            if (joinedCase != null && typeTranslator != null) {
                JavaType jt = typeTranslator.toJavaReferenceType(joinedCase);
                if (jt instanceof JavaClass<?> jc && !"Object".equals(jc.getSimpleName())) {
                    collectedRefs.add(joinedCase);
                    return new ExpressionTypeInfo(jc.getSimpleName(), false, true, null,
                            jc.getCanonicalName().withDots());
                }
            }
            // facet aliasSwitchValueLadder (PR #365, F-A): a value-class case-result
            // switch (enum-value / boolean-literal results under resolvable NAME guards
            // over a bare subject) types as the case-result JOIN — the RETURN-ladder
            // renderer reads the SAME join (switchValueCaseJoinedSimpleNameOrNull), so
            // the `MapperS<FinancialUnitEnum>` / `MapperS<Boolean>` signature and the
            // ladder's `MapperS.<T>ofNull()` arms agree by construction (golden cdm6
            // GetMultiplerUnitTypeUnderlyingAsset + MapRateOptionWithAddress).
            // Function-call / item-case results keep the null fall-through (green
            // #226/#353 carriers byte-untouched).
            ExpressionTypeInfo valueJoin = switchValueCaseJoin(sw, visited, collectedRefs);
            if (valueJoin != null) {
                return valueJoin;
            }
        }

        // Extract → infer from body (changes type but keeps cardinality from receiver)
        if (expr instanceof RExtractExpr ext) {
            if (ext.left().isPresent()) {
                // facet aliasExtractItemBinding (PR #361): the body walks with the
                // receiver ELEMENT bound as the implicit item — the #351 THEN-arm
                // binding law mirrored at the EXTRACT arm, so an item-FEATURE-rooted
                // body (`extract commodityOptionFeaturesModelSequence -> … ->
                // calculationPeriodsSchedule default …`) types render-true instead
                // of nulling to computeReturnType's fn-OUTPUT leak (golden cdm6
                // MapCommodityOptionToObservationTerms `MapperS<? extends
                // CommodityCalculationPeriodsSchedule>` where the leak emitted
                // `? extends ObservationTerms` — never compiled against the body,
                // so every carrier is waivered; the #252 argument).
                ExpressionTypeInfo extRecv =
                        inferExpressionType(ext.left().get(), visited, collectedRefs);
                ExpressionTypeInfo prevExtBinding = this.thenItemBinding;
                boolean prevExtFromThen = this.thenItemBindingFromThen;
                this.thenItemBinding = isTypableName(extRecv) ? extRecv : prevExtBinding;
                this.thenItemBindingFromThen = isTypableName(extRecv) ? false : prevExtFromThen;
                ExpressionTypeInfo bodyType;
                try {
                    bodyType = ext.body() != null && ext.body().body() != null
                        ? inferExpressionType(ext.body().body(), visited, collectedRefs)
                        : extRecv;
                } finally {
                    this.thenItemBinding = prevExtBinding;
                    this.thenItemBindingFromThen = prevExtFromThen;
                }
                // facet aliasReturnCardinality (PR #248) — an extract/map PRESERVES the
                // receiver's cardinality (this arm's stated intent): a single-valued
                // body mapped over a MULTI receiver yields a MULTI result. The pre-#248
                // code returned ONLY the body's cardinality, so an extract over a multi
                // alias receiver (`openTradeStates extract [ item -> Fn(item) ]`) leaked
                // single -> the alias signature emitted MapperS over a MapperC body (a
                // non-compiling, already-waivered mismatch). isReceiverMultiInner is the
                // pure (ref-free) cardinality probe; it resolves an alias-call left
                // through its expression. A receiver-collapsing list op (only-element/
                // first/last) tops the body as RListOpExpr, not RExtractExpr, so this
                // arm is not reached for collapsed chains.
                if (bodyType != null && !bodyType.isMulti
                        && isReceiverMultiInner(ext.left().get())) {
                    return new ExpressionTypeInfo(bodyType.javaTypeName, true,
                            bodyType.isRosettaModelType, bodyType.javaItemType,
                            bodyType.javaItemCanonical);
                }
                return bodyType;
            }
        }

        // Reduce → infer from body but force single
        if (expr instanceof RReduceExpr red) {
            if (red.body() != null && red.body().body() != null) {
                var inner = inferExpressionType(red.body().body(), visited, collectedRefs);
                if (inner != null) {
                    // javaItemType stays NULL (this site did not forward it pre-facet, unlike
                    // only-element/first/last/distinct below) — javaItemType drives
                    // inferShortcutMapperJavaType's meta-coercion gate, so forwarding it would
                    // spuriously activate the deref. Only the seed-only canonical is carried.
                    return new ExpressionTypeInfo(inner.javaTypeName, false, inner.isRosettaModelType,
                            null, inner.javaItemCanonical);
                }
            }
        }

        // Min/Max → infer from operand but force single
        if (expr instanceof RMinExpr || expr instanceof RMaxExpr) {
            if (expr.left().isPresent()) {
                var inner = inferExpressionType(expr.left().get(), visited, collectedRefs);
                if (inner != null) {
                    // javaItemType NULL (see Reduce above — seed-only canonical, no meta-coercion).
                    return new ExpressionTypeInfo(inner.javaTypeName, false, inner.isRosettaModelType,
                            null, inner.javaItemCanonical);
                }
            }
        }

        // facet alias_method_signature_typing — only-element/first/last keep the
        // operand's ITEM type and force single (upstream caseOnlyElementOperation
        // is a hard single barrier). facet distinct_prefix_rendering — DISTINCT
        // walks through to its operand's item type with multi PRESERVED (upstream
        // caseDistinctOperation wraps MapperC over the operand item type); a
        // consuming ONLY_ELEMENT stamps single, yielding e.g. the golden
        // MapperS<Date> for drr IsActionTypePositionMODI's
        // `... -> openDateTime -> date distinct only-element` alias. The
        // operand's built-in `date` record-feature leaf (no RAttribute — the
        // regular walk nulls) is typed HERE and only here — an unscoped
        // record-feature arm could re-type aliases in currently-green files
        // whose signatures come from the byte-frozen fallback; every
        // DISTINCT-bearing alias file is waivered by construction (its body
        // rendered the non-compiling postfix .distinct() pre-facet). Remaining
        // list ops (flatten/reverse/sum) decline to the legacy output-type
        // fallback — no corpus witness needs them.
        if (expr instanceof RListOpExpr listOp) {
            ListOp op = listOp.op();
            // Cardinality-only re-stamps PRESERVE the operand's javaItemType
            // (facet meta_coercion_numbering arm A1): only-element over a
            // meta-walked chain keeps the concrete wrapper witness — the
            // Qualify_Novation alias `businessEvent -> instruction -> before
            // only-element` types MapperS<ReferenceWithMetaTradeState>.
            if (op == ListOp.ONLY_ELEMENT || op == ListOp.FIRST || op == ListOp.LAST) {
                var inner = inferExpressionType(listOp.argument(), visited, collectedRefs);
                if (inner != null) {
                    return new ExpressionTypeInfo(inner.javaTypeName, false,
                            inner.isRosettaModelType, inner.javaItemType, inner.javaItemCanonical);
                }
            }
            if (op == ListOp.DISTINCT) {
                var inner = inferExpressionType(listOp.argument(), visited, collectedRefs);
                if (inner != null) {
                    return new ExpressionTypeInfo(inner.javaTypeName, true,
                            inner.isRosettaModelType, inner.javaItemType, inner.javaItemCanonical);
                }
                if (listOp.argument() instanceof RFeatureCall fc
                        && isDateRecordFeatureLeaf(fc, collectedRefs)) {
                    // The operand chain's leaf is the built-in `date` record
                    // feature on dateTime/zonedDateTime — no RAttribute exists
                    // (the regular walk above returns null), the same gap the
                    // body-side NavigationHandler.tryRecordFeatureNav arm
                    // (facet date_record_feature_nav) renders as
                    // `.<Date>map("Date", «v» -> Date.of(«v».toLocalDate()))`.
                    // Witness `Date` = com.rosetta.model.lib.records.Date (a
                    // runtime record class, never wildcarded); ref recorded for
                    // the signature import (idempotent — the body's record-nav
                    // render already registered it).
                    signatureJavaRefs.add(typeUtil.DATE);
                    return new ExpressionTypeInfo("Date", true, false);
                }
            }
            // facet sumTypedFunctionPath (PR #328, F3): SUM types from the operand's
            // NUMERIC element type, SINGLE — upstream caseSumOperation's result is the
            // item type (golden ApplyCompoundingFormula `MapperS<BigDecimal>
            // totalWeight`; the fork previously declined to the legacy output-type
            // fallback, leaking `MapperS<? extends CalculatedRateDetails>` — which
            // never compiled against the body's sum, so every carrier is waivered).
            // Restricted to the four MapperC typed-sum element types; a non-numeric /
            // unresolved operand keeps the fallback (today's bytes).
            if (op == ListOp.SUM) {
                var inner = inferExpressionType(listOp.argument(), visited, collectedRefs);
                if (inner != null && inner.javaTypeName != null
                        && switch (inner.javaTypeName) {
                            case "BigDecimal", "BigInteger", "Integer", "Long" -> true;
                            default -> false;
                        }) {
                    return new ExpressionTypeInfo(inner.javaTypeName, false,
                            inner.isRosettaModelType, inner.javaItemType, inner.javaItemCanonical);
                }
            }
            return null;
        }

        // ToString → String single
        if (expr instanceof RToStringExpr) {
            return new ExpressionTypeInfo("String", false, false);
        }

        // v3.2 seat 2 (Law 1, the chaos C5Preds rows): a JOIN is String, single — upstream's
        // caseJoinOperation returns `MapperS<String>` whatever the receiver. The walk had no arm,
        // so `alias joined: ones then join ", "` nulled and computeReturnType leaked the function
        // OUTPUT type (`MapperS<? extends boolean>`); the body already rendered golden
        // (`thenArg.join(MapperS.of(", "))`), so only the signature moves.
        if (expr instanceof RJoinExpr) {
            return new ExpressionTypeInfo("String", false, false);
        }

        // facet alias_method_signature_typing — to-enum types from the TARGET enum:
        // exact-wrapped bare-enum witness (golden `MapperS<PeriodEnum>`; a bare enum
        // is not a generated POJO interface, so upstream wrapExtendsIfNotFinal never
        // wildcards it), single per the corpus shapes, enum ref recorded for the
        // import. Unresolved targets and every other conversion kind decline to the
        // legacy output-type fallback.
        if (expr instanceof RConversionExpr conv) {
            if (conv.kind() == ConversionKind.ENUM && conv.targetEnum().isPresent()) {
                var targetEnum = conv.targetEnum().get();
                recordRef(new REnumTypeRef(targetEnum), collectedRefs);
                return new ExpressionTypeInfo(targetEnum.name(), false, false);
            }
            // facet aliasSigReturnTypeLeak (PR #342): `to-int` types Integer single —
            // upstream's ToIntOperation result (golden MapOtherAgreements
            // `MapperS<Integer> vintage`; the pre-facet null leaked the function output
            // `MapperS<? extends LegalAgreement>`, which never compiled against the
            // body's to-int chain — every carrier waivered).
            if (conv.kind() == ConversionKind.INT) {
                return new ExpressionTypeInfo("Integer", false, false);
            }
            // facet conversionAliasSigTypes (PR #419): the remaining five conversion
            // kinds type their RESULT — oracle golden expr-conversions-valid
            // (`MapperS<BigDecimal> asNum` / `MapperS<Date> asDate` /
            // `MapperS<LocalDateTime> asDateTime` / `MapperS<ZonedDateTime> asZoned`
            // / `MapperS<LocalTime> asTime`; the parser's
            // ExpressionTypeComputer.computeConversion already types these — the
            // #418-pinned leak was THIS arm's null → computeReturnType's legacy
            // output-type fallback rendering the raw rune name
            // `MapperS<? extends string>`, non-compiling). Exact-wrapped like the
            // ENUM/INT arms (single, non-model — the corpus-shape law); the imports
            // ride the BODY emissions' refs (ConversionHandler is the sole feeder —
            // the red diff showed zero import divergence). Green-safe by the #342
            // argument: the pre-facet fallback never compiled against the conversion
            // body, so every carrier is an already-waivered mismatch (divergent=1 →
            // zero corpus carriers; D11 proves by measurement).
            return switch (conv.kind()) {
                case NUMBER -> new ExpressionTypeInfo("BigDecimal", false, false);
                case DATE -> new ExpressionTypeInfo("Date", false, false);
                case DATE_TIME -> new ExpressionTypeInfo("LocalDateTime", false, false);
                case ZONED_DATE_TIME -> new ExpressionTypeInfo("ZonedDateTime", false, false);
                case TIME -> new ExpressionTypeInfo("LocalTime", false, false);
                default -> null;
            };
        }

        // Conditional — facet aliasLadderJoin (PR #349, C): JOIN the then and else
        // arms (upstream RosettaTypeProvider caseConditionalExpression →
        // typeSystem.joinMetaAnnotatedTypes; CardinalityProvider: any arm multi →
        // multi). A nested else-if chain joins pairwise bottom-up through the
        // recursion. An untypable arm (`empty` → null) joins to the OTHER arm —
        // killing the function-output fallback leak (golden Create_Valuation
        // `MapperC<? extends Valuation>`; the pre-facet then-only read left null →
        // the TradeState leak, never compiled — every carrier waivered).
        if (expr instanceof RConditionalExpr cond) {
            ExpressionTypeInfo thenInfo = cond.thenBranch() != null
                    ? inferExpressionType(cond.thenBranch(), visited, collectedRefs)
                    : null;
            ExpressionTypeInfo elseInfo = cond.elseBranch().isPresent()
                    ? inferExpressionType(cond.elseBranch().get(), visited, collectedRefs)
                    : null;
            return joinArmTypes(thenInfo, elseInfo, collectedRefs);
        }

        // Constructor → type from the constructor type
        if (expr instanceof RConstructorExpr ctor) {
            return inferConstructorType(ctor, collectedRefs);
        }

        // Literals
        if (expr instanceof RBooleanLiteral) return new ExpressionTypeInfo("Boolean", false, false);
        // v3.2 seat 5 (PR #626, F3 - the ALIAS seat): an int literal types by the translator's digit law over the
        // parser's inferred type (digits <= 9 Integer, <= 18 Long, beyond BigInteger - the SAME
        // JavaTypeTranslator.caseNumberType every number attribute takes), so a beyond-long literal alias
        // declares `MapperS<BigInteger>` (golden conv-bigint-statement ViaAlias) where the fork's fixed "Integer"
        // rendered a signature its own body (`MapperS.of(new BigInteger("..."))`) could not satisfy.
        if (expr instanceof RIntLiteral intLit) return new ExpressionTypeInfo(intLiteralJavaSimpleName(intLit), false, false);
        if (expr instanceof RNumberLiteral) return new ExpressionTypeInfo("BigDecimal", false, false);
        if (expr instanceof RStringLiteral) return new ExpressionTypeInfo("String", false, false);
        if (expr instanceof RListLiteral list) {
            // Infer from first element
            if (!list.elements().isEmpty()) {
                var elemType = inferExpressionType(list.elements().get(0), visited, collectedRefs);
                // facet fnAliasListLiteralJoinSanitize (PR #362): a lowercase-initial
                // first-element name is a SYMBOL ECHO (the #349 sanitizeArm law — walk arms
                // echo unresolved symbol names; a legit Java type is always Capitalized) that
                // must never render into a signature. Fall back to the parser-side element
                // JOIN (getInferredType on the literal), whose meta-stripped bare value IS
                // golden's signature element for the mixed meta-collapse/to-string literal
                // (drr SortIdentifiers `comparison` → `MapperC<String>`; the elements deref
                // in-body via the #361 metaValueDerefHoist, so the bare join is the render
                // truth). BASIC joins only (the java.lang scalars basicJavaTypeForName
                // recovers) — a model-typed or unresolvable join KEEPS the pre-#362 echo
                // (today's bytes; no carrier).
                if (elemType != null && elemType.javaTypeName != null
                        && !elemType.javaTypeName.isEmpty()
                        && Character.isLowerCase(elemType.javaTypeName.charAt(0))
                        && generatorModel != null && typeTranslator != null) {
                    RMetaAnnotatedType joined =
                            generatorModel.workspace().getInferredType(list);
                    if (joined != null && !joined.isMissing()) {
                        JavaClass<?> joinedClass =
                                typeTranslator.toJavaReferenceType(joined.type());
                        if (joinedClass != null
                                && basicJavaTypeForName(joinedClass.getSimpleName()) != null) {
                            return new ExpressionTypeInfo(
                                    joinedClass.getSimpleName(), true, false);
                        }
                    }
                }
                if (elemType != null) {
                    // facet aliasListLiteralMetaItemForward (PR #390): forward the
                    // META-arm javaItemType when EVERY element's walk agrees on it —
                    // a `[<then-chain>, <then-chain>] only-element` alias body then
                    // types MapperS<FieldWithMetaString> through
                    // inferShortcutMapperJavaType, activating the meta-gated
                    // consumers (the evaluate-arg deref: golden drr
                    // IsCommodityBullion/Metal `IsCRPBullion(commodityReferencePrice)`
                    // → `fieldWithMetaString1 == null ? null : …getValue()`). A
                    // disagreeing / walk-null sibling keeps the pre-#390 null
                    // (seed-only canonical, no meta-coercion activation — the
                    // original Reduce-mirror choice, which pre-dated a golden
                    // carrier).
                    JavaClass<?> agreedItem = elemType.javaItemType;
                    for (int i = 1; agreedItem != null && i < list.elements().size(); i++) {
                        var sibling = inferExpressionType(list.elements().get(i), visited, collectedRefs);
                        if (sibling == null || !agreedItem.equals(sibling.javaItemType)) {
                            agreedItem = null;
                        }
                    }
                    return new ExpressionTypeInfo(elemType.javaTypeName, true, elemType.isRosettaModelType,
                            agreedItem, elemType.javaItemCanonical);
                }
            }
        }

        return null;
    }

    /**
     * Infer type from a feature call chain. Resolves the receiver's type,
     * then finds the named feature on that type.
     */
    private ExpressionTypeInfo inferFeatureCallType(RFeatureCall fc, Set<RNode> visited,
                                                    Set<RType> collectedRefs) {
        String featureName = fc.featureName();

        // v3.1 flip seat 3 — authority-first (the R9 feature binding): the legacy
        // resolvedFeature slot is typed RAttribute and structurally cannot hold a
        // CHOICE OPTION, so an option hop always fell to the structural walk below,
        // whose id-less projected copies resolve through the shouldGenerate-filtered
        // name-search and die in every cell navigating vendored-CDM option types
        // (the seat-1 mechanism at the analyzer — the LAW-65 content dump's
        // raw-echo/element/cardinality signature classes, 33 of the 85 drr 7.0.0
        // FUNCTION residue files). A ChoiceOption authority is claimed through
        // seat-1's projectedOptionAttribute (the id+attach pair, LAW 62). A null
        // walk falls through to every pre-seat rung unchanged.
        RAttribute fcAuthFeature = authorityFeatureAttr(fc.resolvedFeatureNode());
        if (fcAuthFeature != null) {
            ExpressionTypeInfo authInfo = resolveAttributeTypeInfo(fcAuthFeature, fc, collectedRefs);
            if (authInfo != null) {
                return authInfo;
            }
        }

        // First check if the feature was resolved during M4 type-directed resolution
        if (fc.resolvedFeature().isPresent()) {
            return resolveAttributeTypeInfo(fc.resolvedFeature().get(), fc, collectedRefs);
        }

        // Otherwise, resolve manually by walking the receiver chain
        RType receiverRType = inferReceiverRType(fc.receiver(), visited, collectedRefs);
        if (receiverRType == null) {
            return null;
        }

        // Find the feature on the receiver type
        Optional<RAttribute> attr = findAttribute(receiverRType, featureName);
        if (attr.isEmpty()) {
            // seat-21 review (the H a6 fixture, LAW 67): the built-in `date` record feature on a
            // dateTime/zonedDateTime receiver — no RAttribute exists, so the walk above cannot type
            // it and the alias signature fell to the output-type fallback, which names the RAW Rune
            // type (`MapperS<? extends date>` — never compiled; zero corpus carriers). The SAME
            // substitution the DISTINCT walk-through already makes: witness `Date`
            // (com.rosetta.model.lib.records.Date, a runtime record class, never wildcarded), the
            // ref recorded for the signature import (idempotent with the body's record-nav render).
            if ("date".equals(featureName) && receiverRType instanceof RRecordType rt
                    && (rt.kind() == RecordKind.DATE_TIME || rt.kind() == RecordKind.ZONED_DATE_TIME)) {
                signatureJavaRefs.add(typeUtil.DATE);
                return new ExpressionTypeInfo("Date", isReceiverMulti(fc), false);
            }
            return null;
        }

        return resolveAttributeTypeInfo(attr.get(), fc, collectedRefs);
    }

    /**
     * True iff a feature-call leaf is the built-in {@code date} record feature
     * on a {@code dateTime}/{@code zonedDateTime} receiver — the sole record
     * feature the 9.83.0 corpus exercises (the body-side gate is
     * {@code NavigationHandler.tryRecordFeatureNav}, facet
     * {@code date_record_feature_nav}). A record feature has no
     * {@link RAttribute}, so {@link #inferFeatureCallType} cannot type it; the
     * caller (facet {@code distinct_prefix_rendering}'s DISTINCT walk-through)
     * substitutes the {@code Date} witness. The receiver probe runs with a
     * fresh visited set — the failed regular walk may already have consumed
     * shortcut nodes from the shared one.
     */
    private boolean isDateRecordFeatureLeaf(RFeatureCall fc, Set<RType> collectedRefs) {
        if (!"date".equals(fc.featureName())) {
            return false;
        }
        RType recvType = inferReceiverRType(fc.receiver(), new HashSet<>(), collectedRefs);
        return recvType instanceof RRecordType rt
                && (rt.kind() == RecordKind.DATE_TIME || rt.kind() == RecordKind.ZONED_DATE_TIME);
    }

    /**
     * Infer type from a deep feature call.
     */
    private ExpressionTypeInfo inferDeepFeatureCallType(RDeepFeatureCall dfc, Set<RNode> visited,
                                                        Set<RType> collectedRefs) {
        if (dfc.resolvedFeature().isPresent()) {
            return resolveAttributeTypeInfo(dfc.resolvedFeature().get(), dfc, collectedRefs);
        }

        RType receiverRType = inferReceiverRType(dfc.receiver(), visited, collectedRefs);
        if (receiverRType == null) return null;

        Optional<RAttribute> attr = findAttributeDeep(receiverRType, dfc.featureName(), new HashSet<>());
        if (attr.isEmpty()) {
            // facet alias_signature_deep_arrow_typing (PR #210) — choice-aware deep search.
            // The parser leaves a deep call's resolvedFeature() empty when its receiver
            // navigates through a one-of/choice type (gm-aware probe: resolvedFeature=false,
            // receiverRType=RChoiceTypeRef), and findAttributeDeep above only RECURSES for an
            // RDataTypeRef receiver — for a choice receiver it tries the direct option lookup
            // ONLY. So a `<choice> ->> feature` alias whose deep feature lives BELOW the
            // choice options lost its type, and the signature fell back to the function
            // output type (the `is_product boolean` → `MapperS<? extends boolean>` of the 10
            // cdm6 Qualify_EquityOption_ParameterReturn* carriers, whose `economicTerms` deep
            // feature lives several levels below the `Product` choice's options). Recover it
            // through the SAME choice-aware deep walk the body-side
            // NavigationHandler.findDeepFeatureAttr / upstream
            // DeepFeatureCallUtil.findDeepFeaturePaths use, so the alias method's
            // `MapperS<? extends EconomicTerms>` signature matches the body the
            // deep_path_util_resolution (#205) NavigationHandler already renders correctly.
            RDataType receiverDt = toRDataTypeForm(receiverRType);
            attr = findDeepFeatureAttrChoiceAware(receiverDt, dfc.featureName(),
                    Collections.newSetFromMap(new IdentityHashMap<>()));
        }
        if (attr.isEmpty()) return null;

        return resolveAttributeTypeInfo(attr.get(), dfc, collectedRefs);
    }

    /**
     * facet alias_signature_deep_arrow_typing (PR #210) — project an {@link RType}
     * to the {@link RDataType} form whose attributes a deep search enumerates: a
     * choice narrows to its {@code asRDataType()} options projection (mirroring
     * upstream's choice→asRDataType narrowing), an alias unwraps to its target, a
     * data-type ref yields its node. {@code null} for any other (primitive / enum /
     * record / missing) type — the deep search then declines (the alias keeps its
     * byte-frozen output-type fallback, still waivered).
     */
    private RDataType toRDataTypeForm(RType type) {
        if (type instanceof RDataTypeRef dtRef) return dtRef.astNode();
        if (type instanceof RChoiceTypeRef choice) return choice.asRDataType();
        if (type instanceof RAliasType alias) return toRDataTypeForm(alias.refersTo());
        return null;
    }

    /**
     * facet alias_signature_deep_arrow_typing (PR #210) — choice-aware
     * deep-attribute search, the alias-signature sibling of
     * {@code NavigationHandler.findDeepFeatureAttr} (the body-side
     * {@code deep_path_util_resolution} worker) and upstream
     * {@code DeepFeatureCallUtil.findDeepFeaturePaths}. Finds an attribute named
     * {@code featureName} reachable from {@code type} by recursing into each
     * attribute's data type (an {@link RChoiceTypeRef} projected to its
     * {@code asRDataType()} options form via {@link #attributeToDataTypeForm}).
     * The {@code visited} identity set bounds the walk to each data type once
     * (cycle + revisit guard). Matches by NAME only — the same deliberate
     * weakening as the body worker: byte-verified correct for every shipped
     * carrier (Product→EconomicTerms single-card), and regression-safe because a
     * deep-call alias is waivered by construction (there are zero green deep-call
     * files, so a wrong pick can only affect an already-divergent file).
     */
    private Optional<RAttribute> findDeepFeatureAttrChoiceAware(RDataType type, String featureName,
                                                                Set<RDataType> visited) {
        if (type == null || !visited.add(type)) return Optional.empty();
        Optional<RAttribute> direct = findAttributeOnDataType(type, featureName, new HashSet<>());
        if (direct.isPresent()) return direct;
        for (RAttribute attr : collectAttributesWithSupertypes(type)) {
            RDataType nested = attributeToDataTypeForm(attr);
            if (nested != null) {
                Optional<RAttribute> found = findDeepFeatureAttrChoiceAware(nested, featureName, visited);
                if (found.isPresent()) return found;
            }
        }
        return Optional.empty();
    }

    /** All attributes of a data type, including those inherited from its supertype chain. */
    private List<RAttribute> collectAttributesWithSupertypes(RDataType type) {
        List<RAttribute> result = new ArrayList<>();
        Set<RDataType> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (RDataType t = type; t != null && seen.add(t); t = t.superType().orElse(null)) {
            result.addAll(t.attributes());
        }
        return result;
    }

    /**
     * facet alias_signature_deep_arrow_typing (PR #210) — gm-aware projection of an
     * attribute's declared type to its {@link RDataType} form (the alias-signature
     * counterpart of {@code NavigationHandler.attributeToDataType}'s compiler-aware
     * overload). Tries the AST {@code referencedType()} fast-path, then resolves the
     * type call via {@link GeneratorModel#resolveTypeCall} (by-name workspace
     * resolution, so a detached/projected option {@code typeCall} still resolves)
     * and narrows an {@link RChoiceTypeRef} to its {@code asRDataType()} projection
     * so the choice's options are reachable. {@code null} when the type does not
     * project to a data type (primitive / enum / record / missing) or resolution
     * fails.
     */
    private RDataType attributeToDataTypeForm(RAttribute attr) {
        if (attr == null || attr.typeCall() == null) return null;
        Optional<RNode> referenced = attr.typeCall().referencedType();
        if (referenced.isPresent() && referenced.get() instanceof RDataType dt) {
            return dt;
        }
        // Defensive decline for the legacy zero-arg constructor (generatorModel == null):
        // the gm-aware projection below needs the generator model, and the body-side
        // sibling NavigationHandler.attributeToDataType(attr, compiler) guards its compiler
        // the same way. In production this is unreachable (the doAnalyze walk that reaches
        // here is gated on generatorModel != null, and inferReceiverRType returns null
        // first under the legacy ctor), but a direct unit call via the package-private
        // inferExpressionType would otherwise NPE instead of cleanly declining.
        if (generatorModel == null) {
            return null;
        }
        try {
            RType rt = generatorModel.resolveTypeCall(attr.typeCall());
            if (rt instanceof RDataTypeRef dtRef) return dtRef.astNode();
            if (rt instanceof RChoiceTypeRef choice) return choice.asRDataType();
        } catch (Exception e) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            // unresolvable type call declines (keeps the stub — still waivered)
        }
        return null;
    }

    /**
     * Infer the RType of a receiver expression. This walks the chain to
     * determine the M4 type at each step.
     */
    private RType inferReceiverRType(RExpression receiver, Set<RNode> visited,
                                     Set<RType> collectedRefs) {
        if (receiver == null) return null;

        // facet aliasThenSigRenderTruth (PR #351, i2): an implicit-item RECEIVER inside a
        // bound then-BODY resolves to the receiver element's model type (nav chains over
        // `item` type through — the StandardizedSchedule conditional arms).
        if (receiver instanceof RImplicitVariable) {
            RDataType bound = thenItemBindingModelType();
            if (bound != null) {
                return recordRef(new RDataTypeRef(bound), collectedRefs);
            }
        }

        if (receiver instanceof RSymbolReference ref) {
            // v3.1 flip seat 3 — authority-first: a bare option name over the
            // implicit item (the a2 SYM class — legacy SYMBOL mis-binds the
            // same-named GLOBAL type, for which no arm below exists) types
            // through the R9 slot's claimed projection; a genuine symbol whose
            // authority agrees types identically (inert). Empty slot → every
            // arm below unchanged.
            RAttribute symAuthFeature = authorityFeatureAttr(ref.resolvedFeatureNode());
            if (symAuthFeature != null) {
                RType symAuthType = resolveAttrRType(symAuthFeature);
                if (symAuthType != null) {
                    return recordRef(symAuthType, collectedRefs);
                }
            }
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RAttribute attr) {
                return recordRef(resolveAttrRType(attr), collectedRefs);
            }
            if (resolved instanceof RFunction func) {
                return recordRef(
                        func.output().map(this::resolveAttrRType).orElse(null),
                        collectedRefs);
            }
            // v3.1 flip seat 21 (LAW 67): a RULE receiver in an alias nav chain types as the
            // rule's inferred output — the same read NavigationHandler.resolveReceiverRType's
            // H1 arm consults; this walk had the RFunction arm above and no RRule sibling.
            if (resolved instanceof com.regnosys.rosetta.ast.functions.RRule rule) {
                return recordRef(
                        com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper
                                .ruleInferredOutputRType(rule, generatorModel),
                        collectedRefs);
            }
            if (resolved instanceof RShortcut shortcut) {
                // For shortcut, recursively infer type of its expression
                ExpressionTypeInfo info = inferExpressionType(shortcut.expression(), visited, collectedRefs);
                if (info != null) {
                    // We need the RType, not just the Java type name. Try to reverse-lookup.
                    // This is imperfect, but for shortcut references in alias chains it suffices.
                    return recordRef(
                            inferRTypeFromExpr(shortcut.expression(), visited, collectedRefs),
                            collectedRefs);
                }
                return null;
            }
            return null;
        }

        if (receiver instanceof RFeatureCall fc) {
            // v3.1 flip seat 3 — authority-first (the seat's FC rung at the
            // receiver walk): an option hop's type comes from the claimed
            // projection instead of the id-less structural copy.
            RAttribute fcAuthFeature = authorityFeatureAttr(fc.resolvedFeatureNode());
            if (fcAuthFeature != null) {
                RType fcAuthType = resolveAttrRType(fcAuthFeature);
                if (fcAuthType != null) {
                    return recordRef(fcAuthType, collectedRefs);
                }
            }
            // If already resolved, use that
            if (fc.resolvedFeature().isPresent()) {
                return recordRef(resolveAttrRType(fc.resolvedFeature().get()), collectedRefs);
            }
            // Otherwise resolve the chain
            RType recType = inferReceiverRType(fc.receiver(), visited, collectedRefs);
            if (recType == null) return null;
            return recordRef(
                    findAttribute(recType, fc.featureName())
                            .map(this::resolveAttrRType)
                            .orElse(null),
                    collectedRefs);
        }

        if (receiver instanceof RDeepFeatureCall dfc) {
            if (dfc.resolvedFeature().isPresent()) {
                return recordRef(resolveAttrRType(dfc.resolvedFeature().get()), collectedRefs);
            }
            RType recType = inferReceiverRType(dfc.receiver(), visited, collectedRefs);
            if (recType == null) return null;
            // facet aliasSigDeepChoiceReceiver (PR #346): the #210 choice-aware recovery
            // mirrored from inferDeepFeatureCallType — a deep step through a CHOICE
            // receiver (`… -> product ->> economicTerms` mid-chain: Product is a choice,
            // economicTerms lives below the options) nulls the plain findAttributeDeep,
            // losing the whole chain's type and falling back to the function OUTPUT type
            // (Create_AssetPayoutTradeStateWithObservations cdm6 `MapperS<? extends
            // TradeState>` where golden types the leaf `AssetPayout`). The SAME
            // findDeepFeatureAttrChoiceAware walk keeps the signature and the
            // NavigationHandler-rendered body (#205/#207) in lockstep.
            Optional<RAttribute> deepAttr =
                    findAttributeDeep(recType, dfc.featureName(), new HashSet<>());
            if (deepAttr.isEmpty()) {
                deepAttr = findDeepFeatureAttrChoiceAware(toRDataTypeForm(recType),
                        dfc.featureName(), Collections.newSetFromMap(new IdentityHashMap<>()));
            }
            return recordRef(
                    deepAttr.map(this::resolveAttrRType).orElse(null),
                    collectedRefs);
        }

        // facet alias_method_signature_typing — an only-element/first/last receiver
        // keeps the operand's ITEM type (`businessEvent -> instruction only-element
        // -> before -> …` walks on through with type Instruction; cardinality is
        // handled separately by isReceiverMultiInner's hard single barrier). Other
        // list ops decline (null → legacy output-type fallback).
        if (receiver instanceof RListOpExpr listOp) {
            ListOp op = listOp.op();
            if (op == ListOp.ONLY_ELEMENT || op == ListOp.FIRST || op == ListOp.LAST) {
                return inferReceiverRType(listOp.argument(), visited, collectedRefs);
            }
            return null;
        }

        // REnumValueRef treated as "paramName -> featureName" when not a real enum
        if (receiver instanceof REnumValueRef evr) {
            if (evr.enumeration().isEmpty() && evr.enumName() != null && evr.valueName() != null) {
                // v3.1 flip seat 3 — authority-first: the R9 feature binding is
                // the disguised chain's leaf; the name rungs below stand as the
                // fallback.
                RAttribute evrAuthFeature = authorityFeatureAttr(evr.resolvedFeatureNode());
                if (evrAuthFeature != null) {
                    RType evrAuthType = resolveAttrRType(evrAuthFeature);
                    if (evrAuthType != null) {
                        return recordRef(evrAuthType, collectedRefs);
                    }
                }
                RType paramType = lookupParameterType(evr.enumName(), collectedRefs);
                if (paramType != null) {
                    recordRef(paramType, collectedRefs);
                    return recordRef(
                            findAttribute(paramType, evr.valueName())
                                    .map(this::resolveAttrRType)
                                    .orElse(null),
                            collectedRefs);
                }
            }
        }

        return null;
    }

    /**
     * Helper: add a non-null RType to the collector (skip nulls and missing
     * types). Returns the input unchanged for chaining. Centralising the
     * null/missing-type filter keeps the caller sites declarative.
     */
    private static RType recordRef(RType rType, Set<RType> collectedRefs) {
        if (rType != null && !(rType instanceof RMissingType)) {
            collectedRefs.add(rType);
        }
        return rType;
    }

    /**
     * Infer the RType of an expression (for shortcut references in chains).
     */
    private RType inferRTypeFromExpr(RExpression expr, Set<RNode> visited,
                                     Set<RType> collectedRefs) {
        if (expr instanceof RFeatureCall fc) {
            // v3.1 flip seat 3 — authority-first (the seat's FC rung at the
            // RType twin): an option hop types through the claimed projection
            // (the id+attach pair) instead of dying in the id-less structural
            // walk; empty authority → every pre-seat rung unchanged.
            RAttribute fcAuthFeature = authorityFeatureAttr(fc.resolvedFeatureNode());
            if (fcAuthFeature != null) {
                RType fcAuthType = resolveAttrRType(fcAuthFeature);
                if (fcAuthType != null) {
                    return recordRef(fcAuthType, collectedRefs);
                }
            }
            if (fc.resolvedFeature().isPresent()) {
                return recordRef(resolveAttrRType(fc.resolvedFeature().get()), collectedRefs);
            }
            RType recType = inferReceiverRType(fc.receiver(), visited, collectedRefs);
            if (recType == null) return null;
            return recordRef(
                    findAttribute(recType, fc.featureName())
                            .map(this::resolveAttrRType)
                            .orElse(null),
                    collectedRefs);
        }
        if (expr instanceof RSymbolReference ref) {
            // v3.1 flip seat 3 — authority-first (the seat's SYM rung at the
            // RType twin — the a2 bare-option class).
            RAttribute symAuthFeature = authorityFeatureAttr(ref.resolvedFeatureNode());
            if (symAuthFeature != null) {
                RType symAuthType = resolveAttrRType(symAuthFeature);
                if (symAuthType != null) {
                    return recordRef(symAuthType, collectedRefs);
                }
            }
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RAttribute attr) {
                return recordRef(resolveAttrRType(attr), collectedRefs);
            }
            if (resolved instanceof RFunction func) {
                return recordRef(
                        func.output().map(this::resolveAttrRType).orElse(null),
                        collectedRefs);
            }
        }
        // facet aliasSigDisguisedChainReceiver (PR #228): complete this shortcut-
        // expression-to-RType walker for the remaining chain shapes the sibling
        // {@link #inferReceiverRType} already handles, so a chained alias whose
        // receiver is a shortcut bottoming out in a deep call or an
        // only-element/first/last list op resolves instead of leaking the receiver
        // name. A deep `head ->> feature` call (e.g. an alias `economicTerms ->>
        // optionPayout`); mirrors inferReceiverRType's RDeepFeatureCall arm.
        if (expr instanceof RDeepFeatureCall dfc) {
            if (dfc.resolvedFeature().isPresent()) {
                return recordRef(resolveAttrRType(dfc.resolvedFeature().get()), collectedRefs);
            }
            RType recType = inferReceiverRType(dfc.receiver(), visited, collectedRefs);
            if (recType == null) return null;
            // facet aliasSigDeepChoiceReceiver (PR #346): the #210 choice-aware recovery —
            // see the inferReceiverRType RDeepFeatureCall arm (this walker mirrors it, the
            // #228 law).
            Optional<RAttribute> deepAttr =
                    findAttributeDeep(recType, dfc.featureName(), new HashSet<>());
            if (deepAttr.isEmpty()) {
                deepAttr = findDeepFeatureAttrChoiceAware(toRDataTypeForm(recType),
                        dfc.featureName(), Collections.newSetFromMap(new IdentityHashMap<>()));
            }
            return recordRef(
                    deepAttr.map(this::resolveAttrRType).orElse(null),
                    collectedRefs);
        }
        // An only-element / first / last receiver keeps the operand's ITEM type
        // (the cdm `optionPayout` alias `economicTerms -> payout -> optionPayout
        // only-element`, navigated by `optionPayout -> underlier`); mirrors
        // inferReceiverRType's RListOpExpr arm. Other list ops decline (null →
        // legacy output-type fallback, still waivered).
        if (expr instanceof RListOpExpr listOp) {
            ListOp op = listOp.op();
            if (op == ListOp.ONLY_ELEMENT || op == ListOp.FIRST || op == ListOp.LAST) {
                return inferReceiverRType(listOp.argument(), visited, collectedRefs);
            }
            return null;
        }
        // facet aliasSigReturnTypeLeak (PR #342): three more RType-preserving shapes
        // complete this walker exactly like the #228 arms above — each a P342A
        // probe-verified carrier gap, each a pure decline extension (an unresolvable
        // inner keeps the null → the caller's legacy fallback bytes):
        //  - a FILTER keeps its receiver's type (drr PrimeBrokerageTransactionIndicatorFunc's
        //    `executingBrokerPartyInfo` filter alias, navigated onward by the
        //    `executingBrokerIsUSPerson` chain — the ExpressionTypeInfo walk already has
        //    this arm; the RType walk lacked it);
        //  - a CONDITIONAL types from its THEN branch (the drr
        //    ExtractCommodityClassification `commodityUnderlier` if/else-nav alias,
        //    consumed as the disguised-chain receiver `commodityUnderlier ->
        //    productTaxonomy` — mirrors inferExpressionType's RConditionalExpr arm);
        //  - a THEN types from its bare-item CARDINALITY body's receiver
        //    (first/last/only-element/flatten/distinct over the implicit item — the #252
        //    signature arm's RType twin, less REVERSE since v3.2 seat 2
        //    (MIRROR_ELEMENT_PRESERVING_OPS); cdm6 EquityCashSettlementAmount's `payout`
        //    then-alias consumed by the disguised choice-option chain `payout ->
        //    PerformancePayout`, which findAttribute resolves via the RChoiceTypeRef
        //    projection), else from the generic then body.
        if (expr instanceof RFilterExpr filter && filter.left().isPresent()) {
            return inferRTypeFromExpr(filter.left().get(), visited, collectedRefs);
        }
        // (v3.2 seat 2, PR #623's witness sweep: a SORT pipe-through twin stood here at the fix
        // commit; the `_backFirst` token pair is healed by NavigationHandler.resolveReceiverDataType's
        // sort arm alone - lane Q stayed green with this arm off, so it was withdrawn under the #614
        // law.)
        if (expr instanceof RConditionalExpr cond && cond.thenBranch() != null) {
            return inferRTypeFromExpr(cond.thenBranch(), visited, collectedRefs);
        }
        if (expr instanceof RThenExpr then) {
            RExpression thenBody = then.body().map(b -> b.body()).orElse(null);
            if (thenBody instanceof RListOpExpr lop && lop.argument() instanceof RImplicitVariable) {
                ListOp op = lop.op();
                // v3.2 seat 2 (PR #623's witness sweep): REVERSE is NOT mirrored here - the signature
                // walk's REVERSE arm is witnessed (C20Spread) but this twin's was not (lane O stayed
                // green: withdrawn, the #614 law).
                if (MIRROR_ELEMENT_PRESERVING_OPS.contains(op)) {
                    return inferRTypeFromExpr(then.argument(), visited, collectedRefs);
                }
            }
            if (thenBody != null) {
                return inferRTypeFromExpr(thenBody, visited, collectedRefs);
            }
            return null;
        }
        // facet aliasSigDisguisedChainReceiver (PR #228): a disguised 2-name
        // `head -> feature` chain (an {@link REnumValueRef} the grammar can't
        // disambiguate at parse time) used as a chain RECEIVER. The top-level
        // {@link #inferEnumValueRefType} already resolves this shape, but this
        // receiver-type walker (which {@link #lookupParameterType} /
        // {@link #inferReceiverRType} drive for a shortcut receiver) did NOT, so a
        // chain whose receiver is itself a disguised-chain alias lost its type and
        // the consuming alias signature leaked the receiver NAME as a bogus type
        // arg — e.g. the cdm `economicTerms` alias body `product -> economicTerms`
        // (where `product` is the alias `trade -> product`) rendered the abstract
        // signature `MapperS<product>` instead of `MapperS<? extends EconomicTerms>`.
        // Resolve enumName as a param/output/shortcut, then findAttribute valueName
        // on it — the SAME resolution inferEnumValueRefType's paramName->featureName
        // arm performs (which is reached only after that method's early-return for a
        // genuine resolved enum). The {@code enumeration().isEmpty()} guard mirrors the
        // sibling {@link #inferReceiverRType} arm so a genuine enum reference declines
        // here too (defensive — an enum's PascalCase name cannot collide with a
        // lowerCamel param/shortcut name, so {@code lookupParameterType} already returns
        // null). Returns null (the prior behaviour) when enumName does not resolve to a
        // navigable receiver, so a declined walk keeps the byte-frozen fallback.
        if (expr instanceof REnumValueRef evr && evr.enumeration().isEmpty()
                && evr.enumName() != null && evr.valueName() != null && currentFunc != null) {
            // v3.1 flip seat 3 — authority-first (the seat's disguised-EVR rung
            // at the RType twin).
            RAttribute evrAuthFeature = authorityFeatureAttr(evr.resolvedFeatureNode());
            if (evrAuthFeature != null) {
                RType evrAuthType = resolveAttrRType(evrAuthFeature);
                if (evrAuthType != null) {
                    return recordRef(evrAuthType, collectedRefs);
                }
            }
            RType recvType = lookupParameterType(evr.enumName(), collectedRefs);
            if (recvType != null) {
                return recordRef(
                        findAttribute(recvType, evr.valueName())
                                .map(this::resolveAttrRType)
                                .orElse(null),
                        collectedRefs);
            }
        }
        // facet aliasSumChainRoot (PR #385): an EXTRACT keeps its BODY's type (the
        // element the extract projects), and a CONSTRUCTOR types as its constructed
        // type (the #358 F-B receiver arm's RType twin) — together they type an
        // extract-constructor alias consumed as a disguised chain ROOT
        // (GetNetInitialMarginFromExposure's `positions extract
        // StandardizedScheduleTradeInfo {…}` alias, navigated by `tradeInitialMargin
        // -> grossInitialMargin -> value sum`; the T384F/T384G evidence: both walks
        // nulled exactly here). Strictly additive — both shapes previously returned
        // null, so a declined walk keeps the legacy fallback bytes.
        if (expr instanceof RExtractExpr ext && ext.body() != null && ext.body().body() != null) {
            return inferRTypeFromExpr(ext.body().body(), visited, collectedRefs);
        }
        if (expr instanceof RConstructorExpr ctor && ctor.typeCall() != null) {
            try {
                RType rt = generatorModel.resolveTypeCall(ctor.typeCall());
                if (rt != null && !(rt instanceof RMissingType)) {
                    return recordRef(rt, collectedRefs);
                }
            } catch (Exception e) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // unresolvable constructor type declines (null → the legacy fallback)
            }
        }
        return null;
    }

    /**
     * v3.1 flip seat 3 — the R9 authority claim for the typing walk: the
     * linker's authoritative feature binding as a typable attribute. An
     * {@link RAttribute} authority is the answer directly; a ChoiceOption
     * authority is claimed through seat-1's {@code projectedOptionAttribute}
     * (the id+attach pair, LAW 62) via
     * {@code NavigationHandler.authorityChoiceOptionAttr}'s workspace-direct
     * overload — the analyzer has no {@code ExpressionCompiler}, but its
     * {@link GeneratorModel} carries the same workspace. Null when the slot is
     * empty or holds any other node — every legacy rung runs unchanged (the
     * fallback contract, the seat-1/2 doctrine).
     */
    private RAttribute authorityFeatureAttr(Optional<RNode> authoritySlot) {
        RNode auth = authoritySlot.orElse(null);
        if (auth == null) {
            return null;
        }
        if (auth instanceof RAttribute attr) {
            return attr;
        }
        // The stateless unit path (the zero-arg test constructor — the
        // attributeToDataTypeForm decline convention) has no GeneratorModel:
        // decline instead of NPE, exactly like the compiler-less render claim.
        if (generatorModel == null) {
            return null;
        }
        return com.regnosys.rosetta.generator.java.expression.handlers.NavigationHandler
                .authorityChoiceOptionAttr(auth, generatorModel.workspace());
    }

    /**
     * Resolve the RType of an attribute using GeneratorModel.
     */
    private RType resolveAttrRType(RAttribute attr) {
        if (attr.typeCall() == null) return null;
        try {
            RType rType = generatorModel.getType(attr);
            return (rType instanceof RMissingType) ? null : rType;
        } catch (Exception e) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            return null;
        }
    }

    /**
     * Build ExpressionTypeInfo from a resolved attribute, determining
     * cardinality by combining the attribute's cardinality with the
     * receiver chain's cardinality.
     */
    private ExpressionTypeInfo resolveAttributeTypeInfo(RAttribute attr, RExpression containingExpr,
                                                        Set<RType> collectedRefs) {
        RType rType = resolveAttrRType(attr);
        if (rType == null) return null;
        recordRef(rType, collectedRefs);

        try {
            // Determine cardinality
            boolean isMulti = generatorModel.isMulti(attr);
            if (!isMulti) {
                isMulti = isReceiverMulti(containingExpr);
            }

            // facet alias_method_signature_typing — a meta-annotated attribute's
            // witness is the CONCRETE FieldWithMetaX/ReferenceWithMetaX wrapper
            // (upstream getRMetaAnnotatedType keeps the meta annotation, and the
            // wrapper is a generated POJO interface, so wrapExtendsIfNotFinal
            // wildcards it: golden `MapperC<? extends FieldWithMetaString>`, meta
            // wrappers 156 wildcard / 0 non-wildcard corpus-wide). Built exactly
            // like NavigationHandler.metaNavResultType — detectMetaKind +
            // RJavaWithMetaValue.create — NOT JavaTypeTranslator.toMetaJavaType,
            // which returns the generic runtime `FieldWithMeta<T>` form and whose
            // gate misses the scheme/reference qualifiers. The wrapper is recorded
            // into the Java-class ref channel for the signature import (idempotent
            // where the body already registered it).
            MetaFieldGenerator.MetaKind metaKind = MetaFieldGenerator.detectMetaKind(attr);
            if (metaKind != MetaFieldGenerator.MetaKind.NONE) {
                // facet convertNullSafe (slice 2, import rider) — a meta-annotated
                // leaf's SIGNATURE is the concrete FieldWithMetaX/ReferenceWithMetaX
                // wrapper (added to signatureJavaRefs below), NEVER the bare value
                // type recordRef() optimistically added to collectedRefs (the
                // inferredRefs import channel) above. The bare value type is not
                // NAMED anywhere the wrapper signature renders — the body navigates
                // through the wrapper — so leaving it in inferredRefs emits a
                // spurious bare-value import the upstream goldens omit (e.g.
                // CalculateFloatingCashFlow's bare DayCountFractionEnum beside the
                // FieldWithMetaDayCountFractionEnum the dcf alias actually uses).
                // Drop it; the wrapper covers the signature import. Where the bare
                // value IS genuinely used elsewhere in the function it rides that
                // site's rendered-body refs, so this only removes the spurious case.
                // The drop is membership-based (Set.remove), not per-site
                // ref-counted: even were the same bare value type also recorded by a
                // receiver step earlier in this walk, a meta receiver itself renders
                // through the wrapper (importing nothing), so its bare-value entry is
                // equally spurious — there is no legitimately-needed bare-value
                // inferredRef at a meta site to lose. Green-safe: a green file's
                // imports match golden, which never carries the bare-value import for
                // a meta leaf — so no green file currently carries it (carrying it
                // would already be a mismatch).
                collectedRefs.remove(rType);
                RJavaWithMetaValue metaWrapper = RJavaWithMetaValue.create(
                        metaKind == MetaFieldGenerator.MetaKind.REFERENCE_WITH_META,
                        typeTranslator.toJavaReferenceType(rType), typeUtil);
                signatureJavaRefs.add(metaWrapper);
                // The concrete wrapper rides javaItemType so arm A1
                // (inferShortcutMapperJavaType) can re-type an alias-call
                // receiver with the SAME walk result the signature renders.
                return new ExpressionTypeInfo(metaWrapper.getSimpleName(), isMulti, true, metaWrapper,
                        metaWrapper.getCanonicalName().withDots());
            }

            JavaType javaType = typeTranslator.toJavaType(rType);
            JavaClass<?> refType = typeTranslator.toJavaReferenceType(rType);
            String typeName = refType.getSimpleName();
            boolean isRosettaModel = typeUtil.isRosettaModelObject(javaType);

            return new ExpressionTypeInfo(typeName, isMulti, isRosettaModel, null,
                    isRosettaModel ? refType.getCanonicalName().withDots() : null);
        } catch (Exception e) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            return null;
        }
    }

    /**
     * Check if any step in the receiver chain is multi-valued.
     */
    private boolean isReceiverMulti(RExpression expr) {
        if (expr instanceof RFeatureCall fc && fc.receiver() != null) {
            // Check this feature call's receiver chain
            return isReceiverMultiInner(fc.receiver());
        }
        if (expr instanceof RDeepFeatureCall dfc && dfc.receiver() != null) {
            return isReceiverMultiInner(dfc.receiver());
        }
        // v3.1 flip seat 3 — the disguised `head -> feature` container: the EVR
        // has no receiver NODE, so its receiver-side multiplicity is the HEAD's
        // (pre-seat this fell through to false and the item's multiplicity never
        // reached the arm walk — the 27-file MapperS-for-MapperC class).
        if (expr instanceof REnumValueRef evr && evr.enumeration().isEmpty()
                && evr.enumName() != null && evr.valueName() != null) {
            return evrHeadMulti(evr);
        }
        return false;
    }

    /**
     * v3.1 flip seat 3 — the disguised-EVR head's multiplicity, authority-first
     * (the {@code resolvedHead} slot; seat-2's {@code evrChainHead} read at the
     * dependency collector, here at the cardinality walk):
     * <ul>
     *   <li>a ChoiceOption head navigates the bound then-item elementwise, so
     *       the receiver side carries the ITEM's multiplicity
     *       ({@code PerformancePayout -> underlier} over {@code payout (1..*)}
     *       — upstream CardinalityProvider flows the implicit item; pre-seat
     *       every arm read single);</li>
     *   <li>an RAttribute head carries its own declared multiplicity, plus the
     *       bound item's when it is item-rooted (a head that is a declared
     *       function input/output keeps its own cardinality only);</li>
     *   <li>every other head shape falls to the legacy rungs — the
     *       {@link #isReceiverMultiInner} EVR block's shortcut/param name
     *       resolution, unchanged.</li>
     * </ul>
     *
     * <p>Known residual (matrix-bounded, zero new mismatches at the seat's
     * rings): when a nested walk's receiver is untypable the #351 binding
     * discipline keeps the OUTER binding visible ({@code : prevBinding}), and
     * this read consumes that visibility — a theoretical over-multi for a
     * shape whose inner hops type via authority under an outer-scoped
     * binding; every such carrier is pre-seat waivered.
     */
    private boolean evrHeadMulti(REnumValueRef evr) {
        RNode head = evr.resolvedHead().orElse(null);
        if (head instanceof com.regnosys.rosetta.ast.supporting.RChoiceOption) {
            return thenItemBinding != null && thenItemBinding.isMulti;
        }
        if (head instanceof RAttribute headAttr) {
            if (generatorModel.isMulti(headAttr)) {
                return true;
            }
            return !isFunctionParamAttr(headAttr)
                    && thenItemBinding != null && thenItemBinding.isMulti;
        }
        return isReceiverMultiInner(evr);
    }

    /** Identity membership in {@link #currentFunc}'s declared inputs/output. */
    private boolean isFunctionParamAttr(RAttribute attr) {
        if (currentFunc == null) {
            return false;
        }
        for (RAttribute input : currentFunc.inputs()) {
            if (input == attr) {
                return true;
            }
        }
        return currentFunc.output().filter(out -> out == attr).isPresent();
    }

    private boolean isReceiverMultiInner(RExpression receiver) {
        return isReceiverMultiInner(receiver, null);
    }

    /**
     * @param visitedShortcuts identity set guarding the alias-of-alias recursion
     *        (facet alias_method_signature_typing — mirrors
     *        {@code CardinalityComputer.computeSymbolRefCardinality}); lazily
     *        created at the first {@link RShortcut} hop, {@code null} until then
     */
    private boolean isReceiverMultiInner(RExpression receiver, Set<RShortcut> visitedShortcuts) {
        // facet alias_method_signature_typing — EVERY list-op receiver reads single
        // here, and the walk never recurses past it. For only-element/first/last
        // that is the upstream law (caseOnlyElementOperation returns false) made
        // explicit: without the barrier, an upstream multi step (`instruction
        // (0..*)` in `businessEvent -> instruction only-element -> …`, or a multi
        // fn-call output in `Fn(...) only-element -> x`) would leak multi through
        // the collapse now that the RFunction/RShortcut arms below can report
        // multi. For the other ops (flatten/distinct/reverse/sum) it preserves
        // the false the unhandled-node fall-through always produced — recursing
        // could flip green files whose byte-identity depends on it.
        if (receiver instanceof RListOpExpr) {
            return false;
        }
        // facet aliasFilterCardinality (PR #420): a FILTER receiver PRESERVES its
        // argument's cardinality — upstream CardinalityProvider treats filter as a
        // pass-through (a filter over a multi list is multi). Reached when an alias
        // resolves through the RShortcut arm below to a filter-topped body
        // (`alias activeAccounts: accounts filter [...]` consumed by
        // `activeAccounts extract [...]` — the extract arm's single→multi upgrade
        // consults THIS walk, so the signature types golden's
        // `MapperC<BigDecimal>` instead of leaking the body's single; oracle
        // golden expr-nested-complex ComplexExpression.balances). Green-safe by
        // the #248 argument: a MapperS signature over a MapperC-rendering body
        // (`.mapItem(...)`) never compiled, so every carrier is an
        // already-waivered mismatch; a filter over a SINGLE argument keeps
        // reading single (the recursion, not a hard true).
        if (receiver instanceof RFilterExpr filterRecv) {
            return isReceiverMultiInner(filterRecv.argument(), visitedShortcuts);
        }
        // v3.1 flip seat 3 — the implicit-item receiver inside a bound
        // then-body: elementwise navigation over a MULTI receiver is multi
        // (upstream CardinalityProvider flows the item's cardinality; pre-seat
        // this fell through to false). Unbound walks keep the fall-through.
        if (receiver instanceof RImplicitVariable) {
            return thenItemBinding != null && thenItemBinding.isMulti;
        }
        // facet aliasSigListLiteralMulti (PR #348): a LIST-LITERAL receiver with more
        // than one element is MULTI — upstream's ListLiteral cardinality (the element
        // join), the SAME law the main walk's RListLiteral arm already applies when the
        // literal is the alias expression's TOP node. Reached when the literal roots an
        // extract chain (`[<6 aliases>] extract CreateQuantityWithLocation(...)` — the
        // cdm6 MapPriceQuantity `q`): the extract arm's single→multi upgrade consults
        // THIS walk, so the signature types MapperC (golden) instead of leaking the
        // body's single. Green-safe by the render-truth law: golden signatures come
        // from upstream's own cardinality read, so no green alias renders MapperS over
        // a list-literal root.
        // v3.2 seat 7 (F11): the #348 rule's 1-element clause ("a 1-element literal carries its element's own
        // cardinality") was UNWITNESSED and is REFUTED by the oracle: the released plugin renders EVERY list literal as
        // a MapperC (`MapperC.<String>of(MapperS.of(raw))` - the fork's own render already does) and an extract over it
        // `mapItem`, so the alias `switched: [raw] extract (item switch "r" then "Red", ...)` signs `MapperC<String>`
        // (goldens ControlLiteral / Chaos - the chaos C18ToKind rows' last diff line at the seat's second cut). A
        // list-literal receiver reads MULTI for any element count; the empty literal keeps its own arm. REACH: every
        // alias whose receiver is a list literal, on any cell - outside the parse-tree SWITCH census's domain; its
        // vendored inertness is the 25-cell digest invariant (UNMOVED on both routes at every chain of record), not
        // the census (round 1, the spec review's NIT-5).
        if (receiver instanceof RListLiteral literal) {
            return !literal.elements().isEmpty();
        }
        if (receiver instanceof RFeatureCall fc) {
            // Check this step's attribute
            RAttribute attr = resolveFeatureCallAttr(fc);
            if (attr != null && generatorModel.isMulti(attr)) return true;
            // Recurse into the receiver's own receiver
            if (fc.receiver() != null) return isReceiverMultiInner(fc.receiver(), visitedShortcuts);
        }
        if (receiver instanceof RDeepFeatureCall dfc) {
            RAttribute attr = dfc.resolvedFeature().orElse(null);
            if (attr != null && generatorModel.isMulti(attr)) return true;
            if (dfc.receiver() != null) return isReceiverMultiInner(dfc.receiver(), visitedShortcuts);
        }
        if (receiver instanceof RSymbolReference ref) {
            var resolved = ref.symbol().orElse(null);
            if (resolved instanceof RAttribute attr) {
                return generatorModel.isMulti(attr);
            }
            // facet alias_method_signature_typing — a function-call receiver carries
            // the callee OUTPUT's cardinality (upstream CardinalityProvider's symbol
            // call to a Function: `FilterOpenTradeStates(...) -> trade` is multi
            // because openTradeStates is (0..*)). The fork represents calls as
            // RSymbolReference WITH args — there is no RFunctionCall node.
            if (resolved instanceof RFunction func) {
                return func.output().map(generatorModel::isMulti).orElse(false);
            }
            // facet alias_method_signature_typing — an alias receiver carries its
            // EXPRESSION's cardinality (upstream ShortcutDeclaration case). The
            // identity-set guard makes a malformed self-/mutually-referential alias
            // cycle read single instead of overflowing the stack.
            if (resolved instanceof RShortcut shortcut && shortcut.expression() != null) {
                Set<RShortcut> visited = visitedShortcuts != null
                        ? visitedShortcuts
                        : Collections.newSetFromMap(new IdentityHashMap<>());
                if (!visited.add(shortcut)) {
                    return false;
                }
                return isReceiverMultiInner(shortcut.expression(), visited);
            }
        }
        // REnumValueRef as "head -> featureName"
        if (receiver instanceof REnumValueRef evr) {
            if (evr.enumeration().isEmpty() && evr.enumName() != null && evr.valueName() != null) {
                // facet aliasReturnCardinality (PR #248) — the cardinality
                // counterpart of #228's disguised-chain-receiver TYPE fix. The
                // disguised `head -> feature` head may itself be a MULTI alias
                // (e.g. `openTradeState -> trade` where the `openTradeState` alias
                // is MapperC<TradeState>) — when the head is multi the whole
                // sub-chain is multi regardless of the feature's own cardinality.
                // The pre-#248 branch resolved the head ONLY as a single parameter:
                // lookupParameterType returns the alias ELEMENT type (TradeState),
                // discarding the alias's multi-ness, and the feature check below
                // (`trade` is single) then read single — so the alias signature
                // emitted MapperS over a MapperC body, a non-compiling (already
                // waivered) mismatch. Mirrors the RShortcut arm of the bare-symbol
                // receiver above (its own cycle guard); a single-result head (or a
                // cycle) falls through to the feature-cardinality check below.
                RShortcut headShortcut = findShortcutByName(evr.enumName());
                if (headShortcut != null && headShortcut.expression() != null) {
                    Set<RShortcut> visited = visitedShortcuts != null
                            ? visitedShortcuts
                            : Collections.newSetFromMap(new IdentityHashMap<>());
                    if (visited.add(headShortcut)
                            && isReceiverMultiInner(headShortcut.expression(), visited)) {
                        return true;
                    }
                }
                // Cardinality helper — not the main inference walker, so ref
                // collection is intentionally skipped here (mirrors the
                // resolveFeatureCallAttr comment above). Throwaway is correct.
                RType paramType = lookupParameterType(evr.enumName(), new HashSet<>());
                if (paramType != null) {
                    var attr = findAttribute(paramType, evr.valueName());
                    if (attr.isPresent()) {
                        return generatorModel.isMulti(attr.get());
                    }
                }
                // Also check the parameter itself
                for (RAttribute input : currentFunc.inputs()) {
                    if (evr.enumName().equals(input.name())) {
                        return generatorModel.isMulti(input);
                    }
                }
            }
        }
        return false;
    }

    /**
     * Find a shortcut (alias) declared in {@link #currentFunc} by name, or
     * {@code null} when no alias of that name exists. Used by
     * {@link #isReceiverMultiInner} to resolve a disguised
     * {@code REnumValueRef} head ({@code head -> feature}) whose head is an
     * alias rather than a function parameter (facet aliasReturnCardinality).
     */
    private RShortcut findShortcutByName(String name) {
        if (currentFunc == null || name == null) {
            return null;
        }
        for (RShortcut shortcut : currentFunc.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return shortcut;
            }
        }
        return null;
    }

    /**
     * Resolve the attribute for a feature call, either from the pre-resolved
     * field or by manual lookup.
     */
    private RAttribute resolveFeatureCallAttr(RFeatureCall fc) {
        if (fc.resolvedFeature().isPresent()) {
            return fc.resolvedFeature().get();
        }
        // Manual lookup — called by cardinality helpers, not from the main
        // inference walker, so ref collection is not needed here.
        RType recType = inferReceiverRType(fc.receiver(), new HashSet<>(), new HashSet<>());
        if (recType == null) return null;
        return findAttribute(recType, fc.featureName()).orElse(null);
    }

    /**
     * Infer type from a symbol reference.
     */
    private ExpressionTypeInfo inferSymbolRefType(RSymbolReference ref, Set<RNode> visited,
                                                  Set<RType> collectedRefs) {
        // v3.1 flip seat 3 deliberately claims NO authority rung here: the
        // seat's measured SYM carriers are RECEIVER-position bare options
        // (inferReceiverRType / inferRTypeFromExpr — wired there); a
        // value-position bare symbol keeps the legacy reads until a measured
        // carrier says otherwise (the minimal-seat doctrine).
        var resolved = ref.symbol().orElse(null);

        if (resolved instanceof RAttribute attr) {
            return resolveAttributeTypeInfo(attr, ref, collectedRefs);
        }

        if (resolved instanceof RShortcut shortcut) {
            return inferExpressionType(shortcut.expression(), visited, collectedRefs);
        }

        if (resolved instanceof RFunction func) {
            var funcOutput = func.output();
            if (funcOutput.isPresent()) {
                return resolveAttributeTypeInfo(funcOutput.get(), ref, collectedRefs);
            }
        }

        // facet aliasRuleRefTyping (PR #332): a reporting-RULE reference types the
        // alias from the rule BODY's inferred type — the synthetic
        // RFunction.fromRule output attribute carries NO typeCall (a rule's output
        // type is inference-derived, not declared), so the RFunction-arm path above
        // cannot serve it; the workspace inference is the SAME source RuleGenerator
        // types the generated `<Name>Rule` evaluate() from, so the alias signature
        // and the rendered `<name>Rule.evaluate(...)` value type cannot disagree
        // (golden cde Direction2 `reportingParty` = `MapperS<String>` from the
        // Counterparty1 rule's String output; the pre-#332 null leaked the enclosing
        // function's output type — the non-compiling `MapperS<? extends
        // Direction2Enum>`, so every carrier is a waivered mismatch → green-safe by
        // construction). An unresolvable body type returns null (the pre-#332
        // fallback bytes).
        if (resolved instanceof com.regnosys.rosetta.ast.functions.RRule rule
                && generatorModel != null && typeTranslator != null && typeUtil != null) {
            RExpression body = rule.expression().orElse(null);
            // seat 21: the ONE rule-output read (HandlerHelper.ruleInferredOutputRType — it declines a
            // body-less rule itself, so a non-null read implies a body); this consumer keeps its own
            // translation + cardinality read over the body.
            RType outType = com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper
                    .ruleInferredOutputRType(rule, generatorModel);
            if (outType != null) {
                try {
                    JavaType javaType = typeTranslator.toJavaType(outType);
                    JavaClass<?> refType = typeTranslator.toJavaReferenceType(outType);
                    boolean isRosettaModel = typeUtil.isRosettaModelObject(javaType);
                    boolean isMulti = generatorModel.workspace().getCardinality(body)
                            == com.regnosys.rosetta.types.ExpressionCardinality.MULTI;
                    // recordRef AFTER every translation succeeded — a throw above must
                    // not leave a phantom ref in collectedRefs (Seat-1 #332 nit: the
                    // broad catch would otherwise keep the leaked import candidate).
                    recordRef(outType, collectedRefs);
                    return new ExpressionTypeInfo(refType.getSimpleName(), isMulti,
                            isRosettaModel, null,
                            isRosettaModel ? refType.getCanonicalName().withDots() : null);
                } catch (Exception e) {
                    // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                    // boundary (JavaClassGenerator), which attaches the target path and reports
                    // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                    if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                    return null;
                }
            }
        }

        // facet aliasThenSigRenderTruth (PR #351, i2): an UNRESOLVED bare symbol inside a
        // bound then-BODY names a feature of the piped element (the render's
        // implicit-item law — `then (if multiplier exists then value * multiplier ->
        // value else value)`, the StandardizedSchedule* arms). Resolve by name against
        // the binding's model type; anything else keeps the pre-facet null.
        if (resolved == null && thenItemBinding != null) {
            RDataType bound = thenItemBindingModelType();
            if (bound != null) {
                Optional<RAttribute> bodyAttr =
                        findAttribute(new RDataTypeRef(bound), ref.name());
                if (bodyAttr.isPresent()) {
                    return resolveAttributeTypeInfo(bodyAttr.get(), ref, collectedRefs);
                }
            }
        }

        // facet dispatchVariantParamResolution (PR #369): an UNRESOLVED bare symbol in a
        // dispatch VARIANT body names a declared input of the dispatch BASE (the variant's
        // own inputs are the `__synthesized_input__` placeholder). Typing it here lets a
        // conditional alias over two base params join its arms (`MapperS<? extends
        // CalculationPeriodBase> calcPd` instead of the untypable `MapperS<?>`). Null for
        // every non-variant seat.
        if (resolved == null) {
            RAttribute baseInput = com.regnosys.rosetta.generator.java.expression.handlers
                    .HandlerHelper.dispatchBaseInput(ref, ref.name());
            if (baseInput != null) {
                return resolveAttributeTypeInfo(baseInput, ref, collectedRefs);
            }
        }

        // facet dispatchVariantParamResolution (PR #369): an explicit-args call bound
        // to an RLibraryFunction (the rune-dsl builtin basicfunctions — no model import
        // in scope) types like upstream: IsLeapYear → Boolean; the generic Min/Max →
        // the JOIN of the argument types (all-Integer → Integer, any BigDecimal →
        // BigDecimal; an untypable argument declines). Feeds the alias signatures
        // (`MapperS<Integer> endDay` / `MapperS<Boolean> startDateIsInLeapYear`,
        // YearFraction).
        if (resolved instanceof com.regnosys.rosetta.ast.types.RLibraryFunction
                && !ref.args().isEmpty()) {
            if ("IsLeapYear".equals(ref.name())) {
                return new ExpressionTypeInfo("Boolean", false, false);
            }
            if ("Min".equals(ref.name()) || "Max".equals(ref.name())) {
                boolean anyNumber = false;
                boolean allTyped = true;
                for (var arg : ref.args()) {
                    ExpressionTypeInfo ai = inferExpressionType(arg, visited, collectedRefs);
                    if (ai == null || ai.javaTypeName == null) {
                        allTyped = false;
                        break;
                    }
                    if ("BigDecimal".equals(ai.javaTypeName)) {
                        anyNumber = true;
                    } else if (!"Integer".equals(ai.javaTypeName)) {
                        allTyped = false;
                        break;
                    }
                }
                if (allTyped) {
                    return new ExpressionTypeInfo(
                            anyNumber ? "BigDecimal" : "Integer", false, false);
                }
            }
        }

        return null;
    }

    /**
     * Infer type from an enum value reference.
     *
     * <p>Due to grammar ambiguity, {@code paramName -> featureName} is parsed as
     * {@code REnumValueRef(enumName=paramName, valueName=featureName)} when it
     * cannot be disambiguated at parse time. If the "enum" doesn't actually
     * resolve to an enumeration, we treat it as a feature call on a parameter.
     */
    private ExpressionTypeInfo inferEnumValueRefType(REnumValueRef evr, Set<RNode> visited,
                                                     Set<RType> collectedRefs) {
        // If it actually resolved to an enum, return the enum type
        try {
            var resolvedEnum = evr.enumeration();
            if (resolvedEnum.isPresent()) {
                String enumName = resolvedEnum.get().name();
                // Emit the enum's RType ref — it's a real referenced type that
                // needs an import. Build an REnumTypeRef from the resolved enum.
                recordRef(new REnumTypeRef(resolvedEnum.get()), collectedRefs);
                return new ExpressionTypeInfo(enumName, false, false);
            }
        } catch (Exception e) {
            // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
            // boundary (JavaClassGenerator), which attaches the target path and reports
            // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
            if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            // fall through
        }

        // v3.1 flip seat 3 — authority-first: the R9 feature binding types the
        // disguised `head -> feature` directly (an RAttribute for a genuine leaf,
        // a ChoiceOption for an option leaf — the census EVR.right slot), so a
        // chain whose head or hops die in the legacy name walk no longer reaches
        // the terminal raw-name echo below. A null walk falls through unchanged.
        RAttribute evrAuthFeature = authorityFeatureAttr(evr.resolvedFeatureNode());
        if (evrAuthFeature != null) {
            ExpressionTypeInfo authInfo = resolveAttributeTypeInfo(evrAuthFeature, evr, collectedRefs);
            if (authInfo != null) {
                return authInfo;
            }
        }

        // Not a real enum — try treating as "paramName -> featureName"
        if (evr.enumName() != null && evr.valueName() != null && currentFunc != null) {
            // Look up enumName as a function parameter or shortcut
            RType paramType = lookupParameterType(evr.enumName(), collectedRefs);
            if (paramType != null) {
                recordRef(paramType, collectedRefs);
                // Look up valueName as a feature on that type
                Optional<RAttribute> attr = findAttribute(paramType, evr.valueName());
                if (attr.isPresent()) {
                    return resolveAttributeTypeInfo(attr.get(), evr, collectedRefs);
                }
                // facet dispatchVariantParamResolution (PR #369): the year/month/day
                // accessors on a DATE-record param have no RAttribute (a record is not
                // a data type — findAttribute above cannot serve them) and type Integer,
                // the SAME resolution the #366 record method-ref nav renders
                // (`.<Integer>map("Year", Date::getYear)`) — so the alias signature
                // stops echoing the raw head (`MapperS<startDate>` → `MapperS<Integer>`,
                // YearFraction _30_360/_30E_360/ACT_365L).
                if (isDateRecordAccessor(paramType, evr.valueName())) {
                    return new ExpressionTypeInfo("Integer", false, false);
                }
            }
        }

        // facet aliasThenSigRenderTruth (PR #351, i2): the "rootName -> featureName"
        // mis-parse inside a bound then-BODY roots at a feature of the piped element
        // (`multiplier -> value` in the StandardizedSchedule* arms). Resolve rootName on
        // the binding's model type, then featureName on the root attribute's type.
        if (evr.enumName() != null && evr.valueName() != null && thenItemBinding != null) {
            RDataType bound = thenItemBindingModelType();
            if (bound != null) {
                Optional<RAttribute> rootAttr =
                        findAttribute(new RDataTypeRef(bound), evr.enumName());
                if (rootAttr.isPresent()) {
                    RType rootType = resolveAttrRType(rootAttr.get());
                    if (rootType != null) {
                        Optional<RAttribute> leafAttr =
                                findAttribute(rootType, evr.valueName());
                        if (leafAttr.isPresent()) {
                            return resolveAttributeTypeInfo(leafAttr.get(), evr, collectedRefs);
                        }
                    }
                }
            }
        }

        // facet aliasCallElementType (PR #358, F-B): the "rootName -> featureName"
        // mis-parse inside an EXTRACT-item lambda roots at a feature of the extract
        // RECEIVER's element (`tradeState extract trade -> tradeIdentifier only-element`
        // — the Create_PairOffInstruction componentId alias, whose pre-facet signature
        // leaked the raw root echo `MapperC<trade>`). Resolve rootName on the extract
        // left's element type (a function param/shortcut symbol — the lookupParameterType
        // route this walk already owns), then featureName on the root attribute's type —
        // the extract-item sibling of the #351 then-binding arm above. A nav-left /
        // non-symbol extract keeps the fallback (today's bytes).
        if (evr.enumName() != null && evr.valueName() != null && currentFunc != null) {
            RNode cur = evr.parent();
            int depth = 0;
            RExtractExpr ownerExt = null;
            while (cur != null && depth++ < 40) {
                if (cur instanceof RInlineFunction inline
                        && inline.parent() instanceof RExtractExpr pExt
                        && pExt.body() == inline) {
                    ownerExt = pExt;
                    break;
                }
                if (cur instanceof RFunction
                        || cur instanceof com.regnosys.rosetta.ast.functions.RRule) {
                    break;
                }
                cur = cur.parent();
            }
            if (ownerExt != null && ownerExt.left().orElse(null) instanceof RSymbolReference extLeft
                    && extLeft.args().isEmpty()) {
                RType extElemType = lookupParameterType(extLeft.name(), collectedRefs);
                if (extElemType != null) {
                    // v3.2 seat 2 (Law 4, the chaos C5Forms `lambdaIte` rows): the 2-name root IS
                    // the extract's EXPLICIT lambda parameter (`items extract x [ … x -> opt … ]`),
                    // not a feature of the element — `x` is bound to the element itself, so the
                    // leaf resolves on the element directly (the render binds `x` the same way:
                    // `x.<BigDecimal>map("getOpt", …)`). Pre-seat the walk looked `x` up as an
                    // attribute, nulled, and the conditional join saw only the literal arm
                    // (`MapperC<Integer>` where golden says `MapperC<BigDecimal>`).
                    if (ownerExt.body() != null
                            && ownerExt.body().paramNames().contains(evr.enumName())) {
                        Optional<RAttribute> paramLeaf = findAttribute(extElemType, evr.valueName());
                        if (paramLeaf.isPresent()) {
                            return resolveAttributeTypeInfo(paramLeaf.get(), evr, collectedRefs);
                        }
                    }
                    Optional<RAttribute> rootAttr = findAttribute(extElemType, evr.enumName());
                    if (rootAttr.isPresent()) {
                        RType rootType = resolveAttrRType(rootAttr.get());
                        if (rootType != null) {
                            Optional<RAttribute> leafAttr =
                                    findAttribute(rootType, evr.valueName());
                            if (leafAttr.isPresent()) {
                                return resolveAttributeTypeInfo(leafAttr.get(), evr, collectedRefs);
                            }
                        }
                    }
                }
            }
        }

        // Fallback: use the enum name (assuming it IS an enum just not resolved)
        if (evr.enumName() != null) {
            // v3.2 seat 13 (D53, THE CLOSING SEAT - the LOUD register's alias-walk site R4, the chaos M7c rows
            // `C29Piped`): the parser carries `l2 -> v` inside `then extract l2 [ l2 -> v ]` as this 2-name ref, and
            // when the owner extract has no bare-symbol LEFT (a then-piped extract) every arm above declines, so this
            // fallback returned the CLOSURE PARAMETER's name as the alias element type - `MapperC<l2>`, a signature
            // javac refuses, SILENT. A name bound as a lambda parameter by an enclosing inline function is never a
            // Java type: refused by name (the walk's own typing of the then-piped extract's element is the v3.3
            // heal, D53 decision 2). Every other unresolved 2-name root keeps the fallback's bytes.
            String closureOwner = enclosingClosureParameterOwner(evr, evr.enumName());
            if (closureOwner != null) {
                throw SilentDegradation.refuse(SilentDegradation.Site.ALIAS_SIGNATURE_CLOSURE_PARAM_TYPE,
                        "alias signature walk fell to the 2-name root '" + evr.enumName() + " -> " + evr.valueName()
                                + "' whose root is the closure parameter '" + evr.enumName() + "' of the enclosing "
                                + closureOwner + " - a lambda parameter is never a Java type; the pre-seat render was "
                                + "MapperC<" + evr.enumName() + "> / MapperS<" + evr.enumName() + ">",
                        evr);
            }
            return new ExpressionTypeInfo(evr.enumName(), false, false);
        }
        return null;
    }

    /**
     * v3.2 seat 13 (D53, site R4): the closure-parameter binding of {@code name} at {@code node} — the parent walk up
     * to the owning function or rule, answering the enclosing {@link RInlineFunction} that declares {@code name} as
     * one of its parameters (rendered as {@code "<owner kind> at <file>:<line>"} for the refusal's witness), or
     * {@code null} when no enclosing lambda binds it. ONE walk for the whole generator (LAW 69): the binding is
     * {@link ReferenceHandler#enclosingClosureParamOwner} - the closure-parameter owner walk the reference, the
     * set-operation and the collection seats already share, bounded by {@code HandlerHelper.PARENT_WALK_LIMIT} - and
     * this method renders only the WITNESS STRING from its answer. Until #634 round 1 (the code-quality seat's SF-1)
     * it re-derived the walk here with a literal {@code 64}: two copies of one walk under two spellings of one bound.
     */
    static String enclosingClosureParameterOwner(RNode node, String name) {
        RInlineFunction inline = ReferenceHandler.enclosingClosureParamOwner(node, name);
        if (inline == null) {
            return null;
        }
        RNode owner = inline.parent();
        String kind = owner == null ? "inline function" : owner.getClass().getSimpleName();
        return kind + (inline.sourceRange() == null ? "" : " at " + inline.sourceRange().file() + ":"
                + inline.sourceRange().startLine());
    }

    /**
     * facet dispatchVariantParamResolution (PR #369): true when {@code paramType}
     * (alias-unwrapped) is the DATE builtin record and {@code featureName} is one of
     * its {@code year}/{@code month}/{@code day} Integer accessors — the alias-walk
     * mirror of {@code NavigationHandler.isDateAccessorFeature}'s render gate, kept
     * separately scoped exactly like the render side (the #366 nav-seat-only law).
     */
    private static boolean isDateRecordAccessor(RType paramType, String featureName) {
        RType rt = paramType;
        while (rt instanceof com.regnosys.rosetta.types.RAliasType alias) {
            rt = alias.refersTo();
        }
        return rt instanceof com.regnosys.rosetta.types.RRecordType rec
                && rec.kind() == com.regnosys.rosetta.types.RecordKind.DATE
                && ("year".equals(featureName) || "month".equals(featureName)
                        || "day".equals(featureName));
    }

    /**
     * Look up a name as a function parameter (input/output) or shortcut,
     * returning its RType. When the name resolves to a shortcut, every
     * {@link RType} discovered inside the shortcut's expression is drained
     * into {@code collectedRefs} so that alias-of-alias chains produce
     * correct imports (PR-A §9.1 A3-D2-02 Finding R1#1).
     */
    private RType lookupParameterType(String name, Set<RType> collectedRefs) {
        if (currentFunc == null) return null;

        // Check inputs
        for (RAttribute input : currentFunc.inputs()) {
            if (name.equals(input.name())) {
                return resolveAttrRType(input);
            }
        }

        // Check output
        if (currentFunc.output().isPresent() && name.equals(currentFunc.output().get().name())) {
            return resolveAttrRType(currentFunc.output().get());
        }

        // Check shortcuts (aliases) — forward the caller's collector so
        // RTypes discovered inside nested shortcut-of-shortcut expressions
        // are captured and not dropped on the floor.
        for (RShortcut shortcut : currentFunc.shortcuts()) {
            if (name.equals(shortcut.name())) {
                return inferRTypeFromExpr(shortcut.expression(), new HashSet<>(), collectedRefs);
            }
        }

        // facet dispatchVariantParamResolution (PR #369): a dispatch VARIANT's own
        // inputs are the `__synthesized_input__` placeholder — its body's parameter
        // references resolve against the dispatch BASE's declared inputs (the SAME
        // attributes the generated signatures take via signatureSource), so the alias
        // return types stop echoing the raw symbol name (`MapperS<resetDates>` →
        // `MapperS<ResetRelativeToEnum>`). Null for every non-variant currentFunc.
        RFunction dispatchBase = com.regnosys.rosetta.generator.java.expression.handlers
                .HandlerHelper.dispatchBaseOf(currentFunc);
        if (dispatchBase != null) {
            for (RAttribute input : dispatchBase.inputs()) {
                if (name.equals(input.name())) {
                    return resolveAttrRType(input);
                }
            }
        }

        return null;
    }

    /**
     * Infer type from a constructor expression.
     */
    private ExpressionTypeInfo inferConstructorType(RConstructorExpr ctor, Set<RType> collectedRefs) {
        if (ctor.typeCall() != null) {
            String rawName = ctor.typeCall().typeName();
            try {
                RType rType = generatorModel.resolveTypeCall(ctor.typeCall());
                if (!(rType instanceof RMissingType)) {
                    recordRef(rType, collectedRefs);
                    JavaType javaType = typeTranslator.toJavaType(rType);
                    JavaClass<?> refType = typeTranslator.toJavaReferenceType(rType);
                    String typeName = refType.getSimpleName();
                    boolean isRosettaModel = typeUtil.isRosettaModelObject(javaType);
                    return new ExpressionTypeInfo(typeName, false, isRosettaModel, null,
                            isRosettaModel ? refType.getCanonicalName().withDots() : null);
                }
            } catch (Exception e) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // fall through
            }
            if (rawName != null) {
                return new ExpressionTypeInfo(rawName, false, true);
            }
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Attribute lookup on RType (mirrors TypeDirectedResolver.findAttribute)
    // -------------------------------------------------------------------------

    /**
     * Find an attribute by name on a type. Handles data types, alias types,
     * and choice types.
     */
    private Optional<RAttribute> findAttribute(RType type, String name) {
        if (type instanceof RDataTypeRef dtRef) {
            return findAttributeOnDataType(dtRef.astNode(), name, new HashSet<>());
        }
        if (type instanceof RAliasType alias) {
            return findAttribute(alias.refersTo(), name);
        }
        if (type instanceof RChoiceTypeRef choice) {
            // facet alias_method_signature_typing — try the data-type projection
            // FIRST: a navigation step can name a choice OPTION itself
            // (`rateSpecification -> FloatingRateSpecification -> rounding`), and
            // asRDataType projects each option as an attribute named after its type
            // (the same RChoiceTypeRef.asRDataType narrowing the PR #160 body
            // machinery rides — upstream narrows a choice receiver to its data-type
            // projection). Name spaces are disjoint (option names are capitalized
            // type names, attributes lowercase), so the projection-first order is
            // effectively additive over the option loop below.
            RDataType projection = choice.asRDataType();
            if (projection != null) {
                var viaProjection = findAttributeOnDataType(projection, name, new HashSet<>());
                if (viaProjection.isPresent()) return viaProjection;
            }
            // For choice types, find on any option
            for (RType option : choice.options()) {
                var attr = findAttribute(option, name);
                if (attr.isPresent()) return attr;
            }
        }
        return Optional.empty();
    }

    private Optional<RAttribute> findAttributeOnDataType(RDataType dt, String name, Set<RDataType> visited) {
        if (!visited.add(dt)) return Optional.empty();
        for (RAttribute attr : dt.attributes()) {
            if (name.equals(attr.name())) return Optional.of(attr);
        }
        if (dt.superType().isPresent()) {
            return findAttributeOnDataType(dt.superType().get(), name, visited);
        }
        return Optional.empty();
    }

    private Optional<RAttribute> findAttributeDeep(RType type, String name, Set<RDataType> visited) {
        var direct = findAttribute(type, name);
        if (direct.isPresent()) return direct;

        if (type instanceof RDataTypeRef dtRef) {
            if (!visited.add(dtRef.astNode())) return Optional.empty();
            for (RAttribute attr : dtRef.astNode().attributes()) {
                var tc = attr.typeCall();
                if (tc != null && tc.referencedType().isPresent()) {
                    var attrType = tc.referencedType().get();
                    if (attrType instanceof RDataType nested) {
                        var found = findAttributeDeep(new RDataTypeRef(nested), name, visited);
                        if (found.isPresent()) return found;
                    }
                }
            }
        }
        return Optional.empty();
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private String builderType(RAttribute attr) {
        String typeName = rawTypeName(attr);
        return typeName + "." + typeName + "Builder";
    }

    private String rawTypeName(RAttribute attr) {
        if (attr.typeCall() == null) return "Object";
        String name = attr.typeCall().typeName();
        return name != null ? name : "Object";
    }

    /**
     * v3.2 seat 12 (D52, the {@code ALIAS_SIGNATURE_RAW_TYPE} gate): whether the output attribute's type - resolved
     * through the generator model with its typeAliases stripped, the SAME read the walk's attribute arms make - is a
     * BUILTIN ({@code string} / {@code number} / a basic / a record type), so that its raw rune name could never be a
     * Java type. A model type (data / choice / enum), an unresolved type or a helper built without a generator model
     * answers {@code false} and keeps the fallback's bytes.
     */
    private boolean outputRawNameIsBuiltin(RAttribute out) {
        if (generatorModel == null || out.typeCall() == null) {
            return false;
        }
        RType resolved;
        try {
            resolved = com.regnosys.rosetta.generator.java.expression.handlers.HandlerHelper.stripAliases(
                    generatorModel.resolveTypeCall(out.typeCall()));
        } catch (RuntimeException ex) {
            // v3.1 C0: a REFUSAL is never recovery - it must reach the per-element boundary (JavaClassGenerator).
            if (ex instanceof SilentDegradation.Refusal __refusal) throw __refusal;
            return false;
        }
        return resolved instanceof com.regnosys.rosetta.types.RStringType
                || resolved instanceof com.regnosys.rosetta.types.RNumberType
                || resolved instanceof com.regnosys.rosetta.types.RBasicType
                || resolved instanceof com.regnosys.rosetta.types.RRecordType;
    }

    private boolean isMultiValued(RAttribute attr) {
        return attr.cardinality()
                .map(c -> c.isUnbounded() || c.sup().compareTo(java.math.BigInteger.ONE) > 0)
                .orElse(false);
    }

    // -------------------------------------------------------------------------
    // Parameter list construction
    // -------------------------------------------------------------------------

    private List<String> buildParams(boolean usesOutput,
                                     Optional<RAttribute> outputOpt,
                                     List<RAttribute> inputs) {
        List<String> params = new ArrayList<>();

        if (usesOutput) {
            outputOpt.ifPresent(out -> {
                String builderTypeName = builderType(out);
                String paramName = out.name();
                params.add(builderTypeName + " " + paramName);
            });
        }

        for (RAttribute input : inputs) {
            String typeName = resolveInputTypeName(input);
            String paramName = input.name();
            params.add(typeName + " " + paramName);
        }

        return params;
    }

    private String resolveInputTypeName(RAttribute input) {
        if (generatorModel != null && typeTranslator != null) {
            try {
                RType rType = generatorModel.getType(input);
                JavaClass<?> refType = typeTranslator.toJavaReferenceType(rType);
                return refType.getSimpleName();
            } catch (Exception e) {
                // v3.1 C0: a REFUSAL is never recovery — it must reach the per-element
                // boundary (JavaClassGenerator), which attaches the target path and reports
                // it as a generation error. Locked by scripts/ci/refusal-propagation-lint.py.
                if (e instanceof SilentDegradation.Refusal __refusal) throw __refusal;
                // fall through to raw name
            }
        }
        return rawTypeName(input);
    }
}
